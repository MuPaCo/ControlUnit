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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.ssehub.devopt.controllayer.network.MqttV3Client;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class contains units tests for the {@link MonitoringDataReceiver#removeObservable(String)} method.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class MonitoringDataReceiverObservableRemovalTests {

    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = MonitoringDataReceiverObservableRemovalTests.class.getSimpleName();
    
    /**
     * The constant URL of the MQTT broker used in these tests.
     */
    private static final String TEST_BROKER_URL = "tcp://broker.hivemq.com";
    
    /**
     * The constant number defining the port of the MQTT broker used in these tests.
     */
    private static final int TEST_BROKER_PORT = 1883;
    
    /**
     * The channels (MQTT topics) used in the tests of this class.
     */
    private static final String[] TEST_CHANNELS = {ID + "_1", ID + "_2"};

    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent:
     * <ul>
     * <li>The channel for which to remove the observable</li>
     * <li>The expected return value, if {@link MonitoringDataReceiver#removeObservable(String)} is called with the
     *     channel defined above for the current test</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
        {null, false},
        {ID + "_0", false}, // Channel not managed by setup and teardown; hence, no observable and no removal
        {TEST_CHANNELS[0], true},
        {TEST_CHANNELS[1], true},
    };
    
    /**
     * The constant test message to send for testing correct removal of observables.
     */
    private static final String TEST_MESSAGE = "Test message for " + ID;
    
    /**
     * The constant {@link TestMonitoringDataReceptionCallback} instance used to be informed about the reception of the
     * {@link #TEST_MESSAGE} from the current observable removed as part of the tests in this class. Of course, this
     * message will not arrive, if removal is successful.
     */
    private static final TestMonitoringDataReceptionCallback TEST_CALLBACK = 
            new TestMonitoringDataReceptionCallback("MonitoringDataReceiverObservableRemovalTests callback");
    
    /**
     * The channel from which to remove the observable.
     */
    private String testChannel;

    /**
     * The expected return value, if {@link MonitoringDataReceiver#removeObservable(String)} is called with the current
     * value of {@link #testChannel}.
     */
    private boolean expectedRemovalReturnValue;
    
    /**
     * Constructs a new {@link MonitoringDataReceiverObservableRemovalTests} instance for testing one particular subset
     * of the {@link #TEST_DATA}.
     * 
     * @param channel the channel from which to remove the observable
     * @param expectedRemovalReturnValue the expected return value, if
     *        {@link MonitoringDataReceiver#removeObservable(String)} is called with the given channel
     */
    public MonitoringDataReceiverObservableRemovalTests(String channel, boolean expectedRemovalReturnValue) {
        this.testChannel = channel;
        this.expectedRemovalReturnValue = expectedRemovalReturnValue;
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
     * Adds the {@link #TEST_CALLBACK} and one observable for each of the {@link #TEST_CHANNELS} to the
     * {@link MonitoringDataReceiver}.
     */
    @BeforeClass
    public static void setUp() {
        if (!MonitoringDataReceiver.INSTANCE.addCallback(TEST_CALLBACK)) {
            fail("Adding test callback \"" + TEST_CALLBACK + "\" failed");
        }
        for (int i = 0; i < TEST_CHANNELS.length; i++) {
            if (!MonitoringDataReceiver.INSTANCE
                    .addObservable("TestObservable" + (i + 1), TEST_CHANNELS[i], TEST_BROKER_URL, TEST_BROKER_PORT)
                    || !isMonitoringEstablished(TEST_CHANNELS[i], TEST_BROKER_URL, TEST_BROKER_PORT)) {
                fail("Adding test observable at channel \"" + TEST_CHANNELS[i] + "\" failed");                
            }
        }
    }
    
    /**
     * Checks whether monitoring at the given channel, URL, and port is established. This method therefore creates
     * another network client to send the {@link #TEST_MESSAGE} on the given channel to the given URL and port. If the
     * {@link #TEST_CALLBACK} receives that message, monitoring for this channel is established.
     * 
     * @param channel the channel to check for being monitored
     * @param url the URL that provides the channel
     * @param port the port number for the URL
     * @return <code>true</code>, if monitoring for the given channel at the given URL and port is established;
     *         <code>false</code> otherwise
     */
    private static boolean isMonitoringEstablished(String channel, String url, int port) {
        boolean monitoringEstablished = false;
        try {
            String clientId = "" + System.currentTimeMillis();
            MqttV3Client testMessagePublisher = new MqttV3Client(clientId, url, port, null, null);
            testMessagePublisher.publish(channel, 2, TEST_MESSAGE.getBytes());
            testMessagePublisher.close();
            Thread.sleep(2000); // Leave some time for message transmission
            Logger.INSTANCE.logDebug(ID, "Monitoring data reception callback information:",
                    "Channel: " + TEST_CALLBACK.getChannel(), "Data: " + TEST_CALLBACK.getData());
            monitoringEstablished = TEST_MESSAGE.equals(TEST_CALLBACK.getData());
        } catch (NetworkException | InterruptedException e) {
            Logger.INSTANCE.logException(ID, e);
        } finally {
            TEST_CALLBACK.reset();
        }
        return monitoringEstablished;
    }
    
    /**
     * Removes the {@link #TEST_CALLBACK} from the {@link MonitoringDataReceiver} and checks for each of the
     * {@link #TEST_CHANNELS} that no observable is available and no monitoring is established.
     */
    @AfterClass
    public static void tearDown() {
        if (!MonitoringDataReceiver.INSTANCE.removeCallback(TEST_CALLBACK)) {
            fail("Removing test callback \"" + TEST_CALLBACK + "\" failed");
        }
        for (int i = 0; i < TEST_CHANNELS.length; i++) {
            if (MonitoringDataReceiver.INSTANCE.removeObservable(TEST_CHANNELS[i]) 
                    || isMonitoringEstablished(TEST_CHANNELS[i], TEST_BROKER_URL, TEST_BROKER_PORT)) {
                fail("Monitoring  at channel \"" + TEST_CHANNELS[i] + "\" must not be available");
            }
        }
    }
    
    /**
     * Tests whether calling {@link MonitoringDataReceiver#removeObservable(String)} with the current value of
     * {@link #testChannel} returns the {@link #expectedRemovalReturnValue}.<br>
     * <br>
     * If a removal is expected ({@link #expectedRemovalReturnValue} is <code>true</code>), this method also tests
     * whether {@link #isMonitoringEstablished(String, String, int)} with the same value as well as the
     * {@link #TEST_BROKER_URL} and the {@link #TEST_BROKER_PORT} returns the negation of the
     * {@link #expectedRemovalReturnValue} (<code>false</code>). This test checks that monitoring data is not received
     * (anymore), if the removal is expected to be successful. If the removal is expected to fail, the respective test
     * channel is <code>null</code> or unknown, which makes such an additional check unnecessary.
     */
    @Test
    public void testRemoveObservable() {
        assertEquals(expectedRemovalReturnValue,
                MonitoringDataReceiver.INSTANCE.removeObservable(testChannel),
                "Wrong observable removal result");
        if (expectedRemovalReturnValue) {
            // Removal for the current channel expected; hence, there must not be any monitoring for that channel
            assertEquals(!expectedRemovalReturnValue,
                    isMonitoringEstablished(testChannel, TEST_BROKER_URL, TEST_BROKER_PORT),
                    "Monitoring must follow observable removal result");
        }
    }
    
}
