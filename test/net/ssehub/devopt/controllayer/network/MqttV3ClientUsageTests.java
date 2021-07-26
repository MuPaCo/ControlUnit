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

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * This class contains unit tests for the usage of a {@link MqttV3Client} instance. In particular, these tests focus on
 * publishing messages and subscribing to a broker. For testing the creation of client instances, see
 * {@link MqttV3ClientCreationTests}.<br>
 * <br>
 * <b>Note</b> that sometimes these tests fail for any obvious reason, but due to network traffic and some strange, not
 * reproducible problems with closing/disconnecting a client instance.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class MqttV3ClientUsageTests implements MqttCallback {
    
    /**
     * The identifier of this class, e.g., for exception messages. 
     */
    private static final String ID = MqttV3ClientUsageTests.class.getSimpleName();
    
    /**
     * The constant URL of the MQTT broker used in these tests.
     */
    private static final String TEST_BROKER_URL = "tcp://broker.hivemq.com";
    
    /**
     * The constant number defining the port of the MQTT broker used in these tests.
     */
    private static final int TEST_BROKER_PORT = 1883;
    
    /**
     * The constant MQTT topic the client will use to publish and subscribe. This topic is the {@link #ID} of this
     * class and is used as main test parameter.
     */
    private static final String TEST_GENERIC_TOPIC = ID;
    
    /**
     * The constant message the client will publish. This message is used as main test parameter.
     */
    private static final String TEST_GENERIC_MESSAGE = "Test message.";
    
    /**
     * The constant number defining the time in milliseconds to wait for a message to arrive asynchronously.
     */
    private static final long DEFAULT_ASYNC_MESSAGE_ARRIVAL_TIMEOUT = 5000;
    
    /**
     * The constant number defining the time in milliseconds to pause this thread while waiting for a message to arrive
     * asynchronously or waiting for network resources to be released.
     */
    private static final long DEFAULT_SLEEP_MILLIS = 500;
    
    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent the expected attribute
     * values of a client instance, the published message to a specific topic, and the expected values received by
     * subscription. For each subset, the values represent:
     * <ul>
     * <li>The user name for creating a new client instance
     * <li>The password for creating a new client instance
     * <li>The topic the client instance publishes and subscribes to
     * <li>The quality of message delivery the client instance defines for publishing and subscribing
     * <li>The message (MQTT payload) the client publishes
     * <li>The expected MQTT topic the client will publish and subscribe to 
     * <li>The expected message (MQTT payload) the client will publish
     * <li>The expected detail message of the top-level exception thrown during during client execution
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {null, null, null, 2, null, null, null, "Invalid MQTT topic: \"null\""},
            
            {null, null, TEST_GENERIC_TOPIC, -1, null, null, null, "Invalid MQTT Quality-of-Service: -1"},
            
            {null, null, TEST_GENERIC_TOPIC, 2, null, null, null, "Invalid MQTT payload: \"null\""},
            
            {null, null, TEST_GENERIC_TOPIC, 2, "", TEST_GENERIC_TOPIC, "", null},
            
            {null, null, TEST_GENERIC_TOPIC, 2, TEST_GENERIC_MESSAGE, TEST_GENERIC_TOPIC, TEST_GENERIC_MESSAGE, null},
            
            {"u", null, TEST_GENERIC_TOPIC, 2, TEST_GENERIC_MESSAGE, TEST_GENERIC_TOPIC, TEST_GENERIC_MESSAGE, null},
            
            {null, "p", TEST_GENERIC_TOPIC, 2, TEST_GENERIC_MESSAGE, TEST_GENERIC_TOPIC, TEST_GENERIC_MESSAGE, null},
            
            {"u", "p", TEST_GENERIC_TOPIC, 2, TEST_GENERIC_MESSAGE, TEST_GENERIC_TOPIC, TEST_GENERIC_MESSAGE, null},
    };
    
    /**
     * The definition of whether a message arrived asynchronously (<code>true</code>) or not (<code>false</code>).
     */
    private boolean asyncMessageArrived;
    
    /**
     * The expected MQTT topic the client will publish and subscribe to.
     */
    private String expectedTopic;
    
    /**
     * The expected message (MQTT payload) the client will publish.
     */
    private String expectedMessage;
    
    /**
     * The expected detail message of the top-level exception thrown during client execution. 
     */
    private String expectedExceptionMessage;
    
    /**
     * The actual MQTT topic at which the client received a message.
     */
    private String actualTopic;
    
    /**
     * The actual message (MQTT payload) the client received.
     */
    private String actualMessage;
    
    /**
     * The actual exception thrown during client execution. 
     */
    private NetworkException actualException;
    
    /**
     * Constructs a new {@link MqttV3ClientUsageTests} instance for publishing and subscribing to a MQTT broker based on
     * the given parameters. Further, the creation of the required {@link MqttV3Client} instance uses the
     * {@link #TEST_BROKER_URL} and {@link #TEST_BROKER_PORT}. 
     * 
     * @param username the user name for creating a new client instance
     * @param password the password for creating a new client instance
     * @param topic the topic the client instance publishes and subscribes to
     * @param qos the quality of message delivery the client instance defines for publishing and subscribing
     * @param message the message (MQTT payload) the client publishes
     * @param expectedTopic the expected MQTT topic the client will publish and subscribe to 
     * @param expectedMessage the expected message (MQTT payload) the client will publish
     * @param expectedExceptionMessage the expected detail message of the top-level exception thrown during during
     *        client execution
     */
    //checkstyle: stop parameter number check
    public MqttV3ClientUsageTests(String username, String password, String topic, int qos, String message,
            String expectedTopic, String expectedMessage, String expectedExceptionMessage) {
        asyncMessageArrived = false;
        this.expectedTopic = expectedTopic;
        this.expectedMessage = expectedMessage;
        this.expectedExceptionMessage = expectedExceptionMessage;
        
        MqttV3Client subscriber = null;
        MqttV3Client publisher = null;
        try {
            subscriber = new MqttV3Client("TestSubscriber", TEST_BROKER_URL, TEST_BROKER_PORT, username, password);
            subscriber.subscribe(topic, qos, this);
            
            publisher = new MqttV3Client("TestPublisher", TEST_BROKER_URL, TEST_BROKER_PORT, username, password);
            byte[] payload = null;
            if (message != null) {
                payload = message.getBytes();
            }
            publisher.publish(topic, qos, payload);
            waitForAsyncMessageArrival();
        } catch (NetworkException e) {
            actualException = e;
        } finally {
            if (subscriber != null) {
                try {
                    subscriber.close();
                } catch (NetworkException e) {
                    e.printStackTrace();
                    fail("Closing MQTT subscriber client for " + ID + " failed: see printed stack trace");
                }
            }
            if (publisher != null) {
                try {
                    publisher.close();
                } catch (NetworkException e) {
                    e.printStackTrace();
                    fail("Closing MQTT publisher client for " + ID + " failed: see printed stack trace");
                }
            }
        }
    }
    //checkstyle: resume parameter number check
    
    /**
     * Calls {@link Thread#sleep(long)} with {@link #DEFAULT_SLEEP_MILLIS} as long as {@link #asyncMessageArrived} is
     * <code>false</code> and {@link #DEFAULT_ASYNC_MESSAGE_ARRIVAL_TIMEOUT} is not reached. This method is used to
     * postpone the execution of any tests until the subscription callback has received a published message, which
     * may take some time due to network traffic. 
     */
    private void waitForAsyncMessageArrival() {
        long currentTimeMillis = System.currentTimeMillis();
        long endTimeMillis = currentTimeMillis + DEFAULT_ASYNC_MESSAGE_ARRIVAL_TIMEOUT;
        while (!asyncMessageArrived && currentTimeMillis < endTimeMillis) {
            try {
                Thread.sleep(DEFAULT_SLEEP_MILLIS);
                currentTimeMillis = System.currentTimeMillis();
            } catch (InterruptedException e) {
                System.err.println("Waiting for async response failed: " + e.getMessage());
            }
        }
        if (!asyncMessageArrived) {
            System.err.println("Async response did not arrive in time");
        }
    }
    
    /**
     * Calls {@link Thread#sleep(long)} with {@link #DEFAULT_SLEEP_MILLIS} exactly once after each test of this class.
     * For each test, the constructor of this class is called, which results in creating a new {@link MqttV3Client}
     * instance frequently. In some cases, this yields false-negative test results as releasing the network resources 
     * by an old client instance is not finished before the new instance is created. This method adds a delay, which
     * solves this problem.
     */
    @After
    public void waitForResourceRelease() {
        try {
            Thread.sleep(DEFAULT_SLEEP_MILLIS);
        } catch (InterruptedException e) {
            System.err.println("Waiting for resource release failed: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        asyncMessageArrived = true;
        fail("MQTT client instance for " + ID + " lost connection: see printed stack trace");
        cause.printStackTrace();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // nothing to do here during tests
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        actualTopic = topic;
        actualMessage = null;
        if (message != null) {
            byte[] payload = message.getPayload();
            if (payload != null) {                
                actualMessage = new String(payload);
            }
        }
        asyncMessageArrived = true;
    }
    
    /**
     * Prints a warning before executing these tasks. See note in class description for more information.
     */
    @BeforeClass
    public static void printWarning() {
        System.out.println("Starting unstable " + ID);
        System.out.println("Sometimes these tests fail for any obvious reason, but due to network traffic and some "
                + "strange, not reproducible problems with closing/disconnecting a client instance");
    }
    
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
     * Tests whether the {@link #expectedTopic} is equal to the {@link #actualTopic}.
     */
    @Test
    public void testCorrectTopic() {
        assertEquals(expectedTopic, actualTopic, "Wrong topic");
    }
    
    /**
     * Tests whether the {@link #expectedMessage} is equal to the {@link #actualMessage}.
     */
    @Test
    public void testCorrectMessage() {
        assertEquals(expectedMessage, actualMessage, "Wrong message");
    }
    
    /**
     * Tests the correctness of the {@link #actualException}. This exception is correct, if:
     * <ul>
     * <li>the {@link #expectedExceptionMessage} and the actual exception are both <code>null</code></li>
     * <li>the {@link #expectedExceptionMessage} is not <code>null</code> and the detail message of the actual exception
     *     are equal</li>
     * </ul>
     */
    @Test
    public void testCorrectException() {
        if (expectedExceptionMessage == null) {
            assertEquals(expectedExceptionMessage, actualException, "Expected no exception");
        } else {
            try {
                assertEquals(expectedExceptionMessage, actualException.getMessage(), "Wrong exception message");
            } catch (NullPointerException e) {
                assertNotNull(actualException, "Actual exception must not be null");
            }
        }
    }

}
