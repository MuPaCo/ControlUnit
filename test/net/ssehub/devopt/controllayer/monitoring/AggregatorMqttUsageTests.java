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
package net.ssehub.devopt.controllayer.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.devopt.controllayer.AllTests;
import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.SetupException;
import net.ssehub.devopt.controllayer.network.MqttV3Client;
import net.ssehub.devopt.controllayer.network.NetworkException;

/**
 * This class contains unit tests for the usage of the {@link Aggregator} instance. In particular, these tests focus on
 * sending messages to this instance and receiving the corresponding aggregation results using the MQTT protocol.
 * 
 * @author kroeher
 *
 */
public class AggregatorMqttUsageTests implements MqttCallback {

    /**
     * The constant string denoting the path to the test configuration file used to create the {@link Setup} instance.
     * This instance will be used to create the {@link Aggregator} instance for the tests in this class.
     */
    private static final String TEST_CONFIGURATION_FILE_PATH =
            AllTests.TEST_CONFIGURATION_FILES_DIRECTORY.getAbsolutePath() + File.separator
            + "aggregationProperties_mqtt.cfg";
    
    /**
     * The string defining the URL to use for distributing aggregated data. This string must match the value of the 
     * configuration property <code>aggregation.url</code> (excluding the leading <code>http://</code>) as defined in
     * the file denoted by the {@link #TEST_CONFIGURATION_FILE_PATH}.
     */
    private static final String AGGREGATION_DISTRIBUTION_URL = "tcp://broker.hivemq.com";
    
    /**
     * The port number to use for distributing aggregated data. This number must match the value of the configuration
     * property <code>aggregation.port</code> as defined in the file denoted by the
     * {@link #TEST_CONFIGURATION_FILE_PATH}.
     */
    private static final int AGGREGATION_DISTRIBUTION_PORT = 1883;
    
    /**
     * The channel to use for distributing aggregated data. This string must match the value of the configuration
     * property <code>aggregation.channel</code> as defined in the file denoted by the
     * {@link #TEST_CONFIGURATION_FILE_PATH}.
     */
    private static final String AGGREGATION_DISTRIBUTION_CHANNEL = "devoptaggregation";
    
    /**
     * The constant number defining the time in milliseconds to wait for a MQTT message to arrive.
     */
    private static final long DEFAULT_MESSAGE_ARRIVAL_TIMEOUT = 2000;
    
    /**
     * The constant number defining the time in milliseconds to pause this thread while waiting for a MQTT message to
     * arrive.
     */
    private static final long DEFAULT_MESSAGE_ARRIVAL_SLEEP = 500;
    
    /**
     * The {@link MqttV3Client} instance to subscribe to the topic on which the {@link Aggregator} instance will publish
     * the aggregated data. For this purpose, this instance uses the same properties as defined in the configuration
     * file denoted by the {@link #TEST_CONFIGURATION_FILE_PATH}.
     */
    private static MqttV3Client aggregationSubscriptionClient;
    
    /**
     * The content (MQTT payload) as received via {@link #messageArrived(String, MqttMessage)}. This attribute also
     * serves as a lock to {@link #waitForAsyncResponse()} after monitoring data was send to the {@link Aggregator}
     * instance. 
     */
    private String aggregationReceptionContent;
    
    /**
     * Creates the {@link Setup} instance using the {@link #TEST_CONFIGURATION_FILE_PATH} to set up the
     * {@link Aggregator} instance for the tests in this class. Further, it creates the
     * {@link #aggregationSubscriptionClient} for receiving aggregated data from that instance.
     */
    @BeforeClass
    public static void setUp() {
        try {
            Setup testSetup = new Setup(TEST_CONFIGURATION_FILE_PATH);
            Aggregator.setUp(testSetup);
            aggregationSubscriptionClient = new MqttV3Client("AggregationSubscriber", AGGREGATION_DISTRIBUTION_URL,
                    AGGREGATION_DISTRIBUTION_PORT, null, null);
        } catch (SetupException | MonitoringException | NetworkException e) {
            fail("Setting up the aggregator MQTT usage tests failed unexpectedly", e);
        }
    }
    
    /**
     * Closes the {@link #aggregationSubscriptionClient} and tears the {@link Aggregator} instance down.
     */
    @AfterClass
    public static void tearDown() {
        try {
            if (aggregationSubscriptionClient != null) {                
                aggregationSubscriptionClient.close();
                aggregationSubscriptionClient = null;
            }
            Aggregator.tearDown();
        } catch (NetworkException | MonitoringException e) {
            fail("Tearing down the aggregator MQTT usage tests failed unexpectedly", e);
        }
    }
    
    /**
     * Tests the correct distribution of aggregated data via MQTT using the {@link #aggregationSubscriptionClient} as
     * receiver.
     */
    @Test
    public void testAggregatorMqttUsage() {
        try {
            aggregationSubscriptionClient.subscribe(AGGREGATION_DISTRIBUTION_CHANNEL, 2, this);
        } catch (NetworkException e) {
            fail("Subscribing to aggregation distribution channel failed unexpectedly", e);
        }
        
        String[] testMonitoringData = {"1", "1", "5", "-6", "10", "-2"};
        String[] expectedAggregationRresults = {"1", "2", "6", "-1", "4", "8"};
        String testMonitoringChannelPrefix = "Monitoring";
        int testMonitoringChannelNumber = 0;
        for (int i = 0; i < testMonitoringData.length; i++) {
            aggregationReceptionContent = null;
            testMonitoringChannelNumber = i % 2; // Tests with two different channel sending monitoring data
            Aggregator.instance.monitoringDataReceived(testMonitoringChannelPrefix + testMonitoringChannelNumber,
                    testMonitoringData[i]);
            waitForAsyncResponse();
            assertEquals(expectedAggregationRresults[i], aggregationReceptionContent, "Wrong aggregation result");
        }
    }
    
    /**
     * Calls {@link Thread#sleep(long)} with {@link #DEFAULT_MESSAGE_ARRIVAL_SLEEP} as long as
     * {@link #aggregationReceptionContent} is <code>null</code> and {@link #DEFAULT_MESSAGE_ARRIVAL_TIMEOUT} is not
     * reached. This method is used to postpone the execution of any further actions, if the arrival of a MQTT message
     * takes some time, e.g., due to network traffic. In this case, the required message (content) for testing arrives
     * later via {@link #messageArrived(String, MqttMessage)}. 
     */
    private void waitForAsyncResponse() {
        long currentTimeMillis = System.currentTimeMillis();
        long endTimeMillis = currentTimeMillis + DEFAULT_MESSAGE_ARRIVAL_TIMEOUT;
        while (aggregationReceptionContent == null && currentTimeMillis < endTimeMillis) {
            try {
                Thread.sleep(DEFAULT_MESSAGE_ARRIVAL_SLEEP);
                currentTimeMillis = System.currentTimeMillis();
            } catch (InterruptedException e) {
                System.err.println("Waiting for async response failed: " + e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectionLost(Throwable cause) {
        fail("Lost connection during tests", cause);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { /* not used here */ }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        aggregationReceptionContent = new String(message.getPayload());
    }
    
}
