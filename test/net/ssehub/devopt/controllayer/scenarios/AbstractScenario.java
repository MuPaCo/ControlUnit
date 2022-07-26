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

import java.io.File;

import org.junit.Test;

import net.ssehub.devopt.controllayer.AllTests;
import net.ssehub.devopt.controllayer.Controller;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This abstract class provides basic attributes and auxiliary methods commonly used by specific scenario tests.
 * 
 * @author kroeher
 *
 */
public abstract class AbstractScenario {
    
    /**
     * The constant string denoting the path to the configuration file used for scenario tests.
     */
    protected static final String CONFIGURATION_FILE_PATH =
            AllTests.TEST_CONFIGURATION_FILES_DIRECTORY.getAbsolutePath() + File.separator + "scenarios.cfg";
    
    /**
     * The constant string denoting the path to the directory in which the controller will save received IVML models
     * during entity registration. This value always matches the respective definition in the configuration file
     * identified by the {@link #CONFIGURATION_FILE_PATH}.
     */
    protected static final String CONFIGURED_MODELS_DIRECTORY_PATH = "./models";
    
    /**
     * The constant string defining the URL of the MQTT broker to use for receiving registrations and sending aggregated
     * data. This value always matches the URLs for the registration and aggregation in the configuration file
     * identified by the {@link #CONFIGURATION_FILE_PATH}.
     */
    protected static final String MQTT_BROKER_URL = "tcp://broker.hivemq.com";
    
    /**
     * The constant port number of the MQTT broker to use for receiving registrations and sending aggregated data.
     * This value always matches the port numbers for the registration and aggregation in the configuration file
     * identified by the {@link #CONFIGURATION_FILE_PATH}.
     */
    protected static final int MQTT_BROKER_PORT = 1883;
    
    /**
     * The constant string denoting the MQTT topic to use for receiving registrations. This value always matches the
     * respective definition in the configuration file identified by the {@link #CONFIGURATION_FILE_PATH}.
     */
    protected static final String REGISTRATION_CHANNEL = "devoptregistration";
    
    /**
     * The constant string denoting the MQTT topic to use for sending aggregated data. This value always matches the
     * respective definition in the configuration file identified by the {@link #CONFIGURATION_FILE_PATH}.
     */
    protected static final String AGGREGATION_CHANNEL = "devoptaggregation";
    
    /**
     * The local reference to the global {@link Logger}.
     */
    protected Logger logger = Logger.INSTANCE;
    
    /**
     * The {@link Controller} instance running in a parallel thread to simulate practical usage of the entire
     * controller.
     * 
     * @see #startController(String)
     * @see #stopController()
     */
    private Controller controller;
    
    /**
     * Tests the scenario result. This test (or multiple ones internally) are subject to the specific scenario.
     * However, this method guarantees execution of the tests by JUnit and, hence, must be implemented by all scenarios.
     * Note that this method should only execute tests. Executing the scenario to produce the necessary results should
     * be done before this method is called, e.g. using <code>@Before</code>.
     */
    @Test
    public abstract void testScenarioResult();
    
    /**
     * Returns the identifier of the specific scenario.
     * 
     * @return the identifier of the specific scenario
     */
    protected abstract String getId();
    
    /**
     * Starts the {@link Controller} instance in a new thread using the given configuration file path as single start
     * argument. This method simulates the typical call of the controller with command line arguments
     * (<code>String[]</code>). Scenarios can interact only with that instance using its public interfaces as in usual
     * practical scenarios.
     * 
     * @param configurationFilePath the path to the configuration file to use as start argument
     * @return <code>true</code>, if starting the controller was successful; <code>false</code> otherwise
     * @see #stopController()
     */
    protected boolean startController(String configurationFilePath) {
        boolean controllerStarted = false;
        if (configurationFilePath != null && !configurationFilePath.isBlank()) {
            Runnable controllerRunnable = new Runnable() {
                
                @Override
                public void run() {
                    String[] args = {configurationFilePath};
                    controller = new Controller(args);
                }
                
            };
            controllerRunnable.run();
            controllerStarted = true;
        } else {
            logger.logError(getId(), "Starting controller failed", "No configuration file path");
        }
        return controllerStarted;
    }
    
    /**
     * Stops the {@link Controller} instance and, hence, terminates its parallel thread.
     *  
     * @return <code>true</code>, if stopping the controller was successful; <code>false</code> otherwise
     */
    protected boolean stopController() {
        boolean controllerStopped = false;
        if (controller != null) {
            controllerStopped = controller.stop();
        } else {
            logger.logError(getId(), "Stopping controller failed", "No controller instance");
        }
        return controllerStopped;
    }
    
    /**
     * Generates an IVML model string, which uses the given parameters to customize that model. While the model uses
     * some static elements for any model, its project name and entity identifier can be defined as desired. Hence, the
     * caller of this method must guarantee their uniqueness in order to not cause errors or rejections by the
     * {@link Controller} and its components. Further, this method relies on the {@link #MQTT_BROKER_URL} and the
     * {@link #MQTT_BROKER_PORT} to define the monitoring information in the model using the given channel.
     * 
     * @param projectName the name of the IVML project to generate; must not be <code>null</code> nor <i>blank</i>
     * @param entityIdentifier the identifier of the entity for which the model is generated; must not be
     *        <code>null</code> nor <i>blank</i>
     * @param monitoringChannel the channel (MQTT topic) on which the entity, for which the model is generated, sends
     *        its monitoring data; must not be <code>null</code> nor <i>blank</i>
     * @return the custom IVML model
     */
    protected String getModel(String projectName, String entityIdentifier, String monitoringChannel) {
        String model = "project " + projectName + " {\r\n"
                + "\r\n"
                + "    import DevOpt_System;\r\n"
                + "    \r\n"
                + "    Identification entityIdentification = {\r\n"
                + "        identifier = \"" + entityIdentifier + "\",\r\n"
                + "        host = \"192.168.1.11\",\r\n"
                + "        port = 1883\r\n"
                + "    };\r\n"
                + "\r\n"
                + "    IdentificationDescription identificationDescription = {\r\n"
                + "        description = entityIdentification\r\n"
                + "    };\r\n"
                + "    \r\n"
                + "    IntegerParameter entityPowerBalance = {\r\n"
                + "        name = \"Power Balance\",\r\n"
                + "        value = null\r\n"
                + "    };\r\n"
                + "    \r\n"
                + "    RuntimeDate entityMonitoring = {\r\n"
                + "        timestamp = null,\r\n"
                + "        monitoringScope = \"" + monitoringChannel + "@" + MQTT_BROKER_URL + ":" + MQTT_BROKER_PORT
                                + "\",\r\n"
                + "        value = entityPowerBalance,\r\n"
                + "        expressions = null\r\n"
                + "    };\r\n"
                + "    \r\n"
                + "    RuntimeDescription runtimeDescription = {\r\n"
                + "        description = entityMonitoring\r\n"
                + "    };\r\n"
                + "    \r\n"
                + "    EntityDescription observableDescription = {\r\n"
                + "        identificationDescription,\r\n"
                + "        runtimeDescription\r\n"
                + "    };\r\n"
                + "    \r\n"
                + "    Entity observable = {\r\n"
                + "        name = \"Observable\",\r\n"
                + "        description = observableDescription\r\n"
                + "    };\r\n"
                + "    \r\n"
                + "}";
        return model;
    }
    
}
