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
 * send three monitoring data items each to the controller. All steps of this scenario are executed in sequence. Hence,
 * the controller must handle one request (either registration or reception of monitoring data) at a time.<br>
 * <br>
 * The actual test checks whether the number of aggregated data published by the controller equals the expected number
 * and, if each of this data equals the expected value.
 * 
 * @author kroeher
 *
 */
public class Scenario01 extends AbstractScenario {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = Scenario01.class.getSimpleName();
    
    /**
     * The set of monitoring data to send by the entities in the scenario. Note that this scenario creates three
     * entities of which each sends exactly three monitoring data items sequentially. Hence, the first element in this
     * set is the first data item send by the first entity, the second element is the first data item send by the second
     * entity, etc.
     */
    private static final String[] TEST_MONITORING_DATA = {"1", "2", "-3", "-7", "32", "0", "0", "0", "18"};
    
    /**
     * The set of expected aggregated data published by the controller for each received {@link #TEST_MONITORING_DATA}.
     */
    private static final String[] EXPECTED_AGGREGATION_RESULTS = {"1", "3", "0", "-8", "22", "25", "32", "0", "18"};
    
    /**
     * The instance to receive aggregated data from the controller.
     */
    private AggregationReceiver aggregationReceiver;
    
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
        // Create three clients, which simulate the three entities in this scenario                
        MqttV3Client[] entityClients = new MqttV3Client[3];
        try {
            entityClients[0] = new MqttV3Client("EntityClient1", MQTT_BROKER_URL, MQTT_BROKER_PORT, null, null);
            entityClients[1] = new MqttV3Client("EntityClient2", MQTT_BROKER_URL, MQTT_BROKER_PORT, null, null);
            entityClients[2] = new MqttV3Client("EntityClient3", MQTT_BROKER_URL, MQTT_BROKER_PORT, null, null);
        } catch (NetworkException e) {
            fail("Creating all clients (entities) failed unexpectedly", e);
        }
        // Register the entities at the controller using generated models for each of them
        String entityIdentifier;
        for (int i = 0; i < entityClients.length; i++) {
            entityIdentifier = "Entity" + (i + 1);
            String entityModel = getModel(entityIdentifier, entityIdentifier + "Channel");
            try {
                entityClients[i].publish(REGISTRATION_CHANNEL, 2, entityModel.getBytes());
                Thread.sleep(1000); // Add a delay to support traceability during execution
            } catch (NetworkException | InterruptedException e) {
                fail("Registering the clients (entities) failed unexpectedly", e);
            }
        }
        /*
         * Send monitoring data. In this scenario, each entity sends three data items sequentially. Hence, the first
         * entity sends its first data item, the second entity sends its first data item, and the third entity sends its
         * first data item. Then, each entity sends its second and third data item in the same order. 
         */
        int entityNumber;
        for (int i = 0; i < TEST_MONITORING_DATA.length; i++) {
            entityNumber = i % 3;
            try {
                entityClients[entityNumber].publish("Entity" + (entityNumber + 1) + "Channel", 2,
                        TEST_MONITORING_DATA[i].getBytes());
                Thread.sleep(1000); // Add a delay to support traceability during execution
            } catch (NetworkException | InterruptedException e) {
                fail("Sending monitoring data failed unexpectedly", e);
            }
        }
    }

    /**
     * Stops the controller and the {@link #aggregationReceiver} and deletes the model directory created by the
     * controller at startup.
     */
    @After
    public void tearDown() {
        if (!stopController()) {
            fail("Stopping the controller failed unexpectedly");
        }
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
        assertEquals(EXPECTED_AGGREGATION_RESULTS.length, actualAggregationResults.size(),
                "Wrong number of received aggregation results");
        for (int i = 0; i < EXPECTED_AGGREGATION_RESULTS.length; i++) {
            assertEquals(EXPECTED_AGGREGATION_RESULTS[i], actualAggregationResults.get(i),
                    "Wrong received aggregation result for iteration " + i);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getId() {
        return ID;
    }

}
