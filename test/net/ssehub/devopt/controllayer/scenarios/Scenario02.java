/*
 * Copyright 2022 University of Hildesheim, Software Systems Engineering
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.ssehub.devopt.controllayer.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import net.ssehub.devopt.controllayer.network.MqttV3Client;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.utilities.FileUtilities;
import net.ssehub.devopt.controllayer.utilities.FileUtilitiesException;

/**
 * This class realizes a scenario of the usage of the controller. In this scenario, three entities register itself and
 * send nine monitoring data items each to the controller. The entities execute these actions in parallel, but with some
 * delay to ensure that the controller establishes monitoring without missing a monitoring data item and to not send all
 * of this data almost at once. Receiving all possible monitoring data items is necessary as the actual tests checks the
 * number of received aggregated data. However, the controller must handle multiple requests (in particular, the
 * reception of monitoring data) in parallel.<br>
 * <br>
 * The actual test checks whether the number of aggregated data items received by the controller equals the expected,
 * number, which is the total aggregated data items send by all entities.Due to concurrent execution of the entities, it
 * is not possible to determine (and test) the aggregated data for each single monitoring data item per entity.
 * 
 * @author kroeher
 *
 */
public class Scenario02 extends AbstractScenario {

    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = Scenario02.class.getSimpleName();
    
    /**
     * The number of entities executed in parallel in this scenario.
     */
    private static final int PARALLEL_ENTITIES_COUNT = 3;
    
    /**
     * The set of monitoring data to send by the entities in the scenario. Note that this scenario creates three
     * parallel entities of which each sends each monitoring data item exactly once sequentially.
     */
    private static final String[] TEST_MONITORING_DATA = {"1", "2", "-3", "-7", "32", "0", "0", "0", "18"};
    
    /**
     * The instance to receive aggregated data from the controller.
     */
    private AggregationReceiver aggregationReceiver;
    
    /**
     * This class realizes an entity, which can be executed in its own {@link Thread}. Its execution consists of sending
     * an registration request and each {@link Scenario02#TEST_MONITORING_DATA} item once.
     * 
     * @author kroeher
     *
     */
    private class ThreadedEntity implements Runnable {
        
        /**
         * The identifier of this instance.
         */
        private String entityIdentifier;
        
        /**
         * The channel (MQTT topic) this instances uses to send its monitoring data.
         */
        private String entityMonitoringChannel;
        
        /**
         * The client this instance uses to sends its MQTT messages (registration and monitoring data).
         */
        private MqttV3Client entityClient;
        
        /**
         * The IVML model describing the entity, which this instances represents.
         */
        private String entityModel;
        
        /**
         * The set of monitoring data items, which this entity must send after its registration. Typically, these are
         * the {@link Scenario02#TEST_MONITORING_DATA} items.
         * 
         */
        private String[] monitoringData;
        
        /**
         * Constructs a new {@link ThreadedEntity} instance.
         * 
         * @param entityNumber the unique number of this instances used to create its identifier
         * @param monitoringData the set of monitoring data items this instance must send
         * @throws NetworkException if creating its internal network client fails
         */
        private ThreadedEntity(int entityNumber, String[] monitoringData) throws NetworkException {
            entityIdentifier = "Entity" + entityNumber;
            entityMonitoringChannel = entityIdentifier + "Channel";
            entityClient = new MqttV3Client(entityIdentifier + "Client", MQTT_BROKER_URL, MQTT_BROKER_PORT, null, null);
            entityModel = getModel(entityIdentifier, entityMonitoringChannel);
            this.monitoringData = monitoringData;
        }

        /**
         * Executes this instance, which will connect its internal client, register itself, and send its monitoring data
         * items.
         */
        public void run() {
            // Connect client and register as entity
            try {
                logger.logInfo(entityIdentifier, "Registering");
                entityClient.connect();
                entityClient.publish(REGISTRATION_CHANNEL, 2, entityModel.getBytes());
                logger.logInfo(entityIdentifier, "Registration done");
                Thread.sleep(3000); // Allow controller to process the model and establish monitoring
            } catch (NetworkException | InterruptedException e) {
                logger.logException(entityIdentifier, e);
            }
            // Send monitoring data
            for (int i = 0; i < monitoringData.length; i++) {
                try {
                    entityClient.publish(entityMonitoringChannel, 0, monitoringData[i].getBytes());
                    Thread.sleep(500); // Publishing too fast results in missing receptions although sending was ok
                } catch (NetworkException | InterruptedException e) {
                    logger.logException(entityIdentifier, e);
                }
            }
            // Clean-up
            try {
                entityClient.close();
            } catch (NetworkException e) {
                logger.logException(entityIdentifier, e);
            } finally {
                entityClient = null;
                entityModel = null;
                monitoringData = null;
            }
        }
        
    }
    
    /**
     * Executes the scenario.
     */
    @Before
    public void executeScenario() {
        // Create and start the receiver of aggregated data; before starting the controller it should not receive data 
        try {
            aggregationReceiver = new AggregationReceiver(MQTT_BROKER_URL, MQTT_BROKER_PORT, AGGREGATION_CHANNEL);
            aggregationReceiver.start();
        } catch (NetworkException e) {
            fail("Starting the aggregation receiver failed unexpectedly", e);
        }
        // Start the controller in a separate thread using the configuration file for this scenario
        if (!startController(CONFIGURATION_FILE_PATH)) {
            fail("Starting the controller failed unexpectedly");
        }
        // Create and start three threads (entities), which register and send monitoring data in parallel
        Thread[] entityThreads = new Thread[PARALLEL_ENTITIES_COUNT];
        int entityNumber;
        for (int i = 0; i < PARALLEL_ENTITIES_COUNT; i++) {
            entityNumber = i + 1;
            try {
                entityThreads[i] = new Thread(new ThreadedEntity(entityNumber, TEST_MONITORING_DATA),
                        "EntityThread" + entityNumber);
                entityThreads[i].start();
            } catch (NetworkException e) {
                fail("Starting threads (entities) failed unexpectedly", e);
            }
            
        }
        // Wait until all threads (entities) terminate
        for (int i = 0; i < PARALLEL_ENTITIES_COUNT; i++) {            
            try {
                entityThreads[i].join();
            } catch (InterruptedException e) {
                fail("Waiting for threads (entities) failed unexpectedly", e);
            }
        }
        /*
         * Stopping the controller at this point already includes an indirect test: if stop is called, the controller 
         * must ensure that all data is processed before the participating components are stopped or deleted. Hence,
         * calling stop here also introduces a delay until all data is processed before testing their correct reception.
         */
        if (!stopController()) {
            fail("Stopping the controller failed unexpectedly");
        }
    }

    /**
     * Stops the {@link #aggregationReceiver} and deletes the model directory created by the controller at startup.
     */
    @After
    public void tearDown() {
        try {
            aggregationReceiver.stop();
        } catch (NetworkException | InterruptedException e) {
            fail("Stopping the aggregation receiver failed unexpectedly", e);
        }
        File toDelete = new File(CONFIGURED_MODELS_DIRECTORY_PATH);
        if (!toDelete.exists()) {
            fail("Deleting directory \"" + toDelete.getAbsolutePath()
                    + "\" failed unexpectedly: directory does not exist");
        }
        try {
            FileUtilities.INSTANCE.delete(toDelete);
        } catch (FileUtilitiesException e) {
            fail("Deleting directory \"" + toDelete.getAbsolutePath() + "\" failed unexpectedly", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testScenarioResult() {
        List<String> actualAggregationResults = aggregationReceiver.getReceivedMessages();
        int expectedAggregationResultsCount = PARALLEL_ENTITIES_COUNT * TEST_MONITORING_DATA.length;
        assertEquals(expectedAggregationResultsCount, actualAggregationResults.size(),
                "Wrong number of received aggregation results");
        /*
         * Due to parallelization, further checks regarding the reception of the correct aggregation results per test
         * monitoring data is not possible. Which entity (thread) sends which data is random such that the resulting
         * aggregated data changes for each run of this scenario. 
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getId() {
        return ID;
    }
    
}
