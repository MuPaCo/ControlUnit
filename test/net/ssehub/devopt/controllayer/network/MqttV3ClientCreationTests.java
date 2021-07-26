/*
 * Copyright 2021 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.devopt.controllayer.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class contains unit tests for the creation of new {@link MqttV3Client} instances.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class MqttV3ClientCreationTests {
    
    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent the expected attribute
     * values of a client instance created for the respective test iteration as well as the input for creating that
     * client instance. For each subset, the values represent:
     * <ul>
     * <li>The identifier of the client instance</li>
     * <li>The URL or IP of the MQTT broker the client instance must use</li>
     * <li>The port of the MQTT broker the client instance must use</li>
     * <li>The user name for authentication of the client instance</li>
     * <li>The password for authentication of the client instance</li>
     * <li>The expected creation result in terms of:</li>
     *     <ul>
     *     <li>The client instance string as provided by {@link MqttV3Client#toString()}, if creating the instance with
     *         the associated parameters above must be successful</li>
     *     <li>The detail message string of the top-level exception thrown during client instance creation, if creating
     *         the client instance must fail</li>
     *     </ul>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {null, null, -1, null, null, "Invalid network element ID: \"null\""},
            
            {"#TestClient#", null, -1, null, null,
                "Invalid network element ID: must contain only [0..9], [a..z], [A..Z], but is \"#TestClient#\""},
            
            {"TestClient", null, -1, null, null, "Invalid MQTT client broker URL: \"null\""},
            
            {"TestClient", "127.0.0.1", -1, null, null, "Invalid MQTT client broker port: -1"},
            
            {"TestClient", "127.0.0.1", 1883, null, null,
                "Creating MQTT client \"TestClient\" for broker \"127.0.0.1:1883\" failed"},
            
            {"TestClient", "tcp://127.0.0.1", 1883, null, null,
                "MqttV3Client{id=TestClient;broker=tcp://127.0.0.1:1883;username=null;password=null;connected=false;"
                        + "closed=false;}"},
            
            {"TestClient", "tcp://127.0.0.1", 1883, "u", null,
                "MqttV3Client{id=TestClient;broker=tcp://127.0.0.1:1883;username=u;password=null;connected=false;"
                        + "closed=false;}"},
            
            {"TestClient", "tcp://127.0.0.1", 1883, "u", "p",
                "MqttV3Client{id=TestClient;broker=tcp://127.0.0.1:1883;username=u;password=p;connected=false;"
                        + "closed=false;}"}
    };
    
    /**
     * The expected creation result. This string contains:
     * <ul>
     * <li>The client instance string as provided by {@link MqttV3Client#toString()}, if creating the instance must be
     *     successful</li>
     * <li>The detail message string of the top-level exception thrown during client instance creation, if creating
     *     the client instance must fail</li>
     * </ul>
     */
    private String expectedCreationResult;
    
    /**
     * The actual creation result. This string contains:
     * <ul>
     * <li>The client instance string provided by {@link MqttV3Client#toString()} after successful creation of the
     *     client instance</li>
     * <li>The detail message string of the top-level exception thrown during client instance creation, if creating
     *     the client instance failed</li>
     * </ul>
     */
    private String actualCreationResult;

    /**
     * Constructs a new {@link MqttV3Client} instance for connection to a MQTT broker.
     * 
     * @param id the identifier for creating a new client instance
     * @param brokerUrl the URL or IP of the MQTT broker for creating a new client instance
     * @param brokerPort the port of the MQTT broker for creating a new client instance
     * @param username the user name for creating a new client instance
     * @param password the password for creating a new client instance
     * @param expectedCreationResult either the client information as provided by {@link MqttV3Client#toString()}, if
     *        creating the instance must be successful, or the detail exception message, if creating the instance must
     *        fail
     */
    //checkstyle: stop parameter number check
    public MqttV3ClientCreationTests(String id, String brokerUrl, int brokerPort, String username, String password,
            String expectedCreationResult) {
        this.expectedCreationResult = expectedCreationResult;
        actualCreationResult = null;
        try {
            MqttV3Client client = new MqttV3Client(id, brokerUrl, brokerPort, username, password);
            actualCreationResult = client.toString();
        } catch (NetworkException e) {
            actualCreationResult = e.getMessage();
        }
    }
    //checkstyle: resume parameter number check
    
    /**
     * Returns the {@link #TEST_DATA} for the following tests in this class.
     * 
     * @return the {@link #TEST_DATA}
     */
    @Parameters
    public static Object[][] getTestData() {
        return TEST_DATA;
    }
    
    /**
     * Tests whether the {@link #expectedCreationResult} defined in the {@link #TEST_DATA} for a specific test iteration
     * is equal to the {@link #actualCreationResult} of creating the client instance of that iteration.  
     */
    @Test
    public void testEqualCreationResults() {
        assertEquals(expectedCreationResult, actualCreationResult, "Wrong creation result");
    }
    
}
