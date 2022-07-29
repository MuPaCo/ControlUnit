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
 * This class contains units tests for the {@link MonitoringDataReceiver#addObservable(String, String, String, int)}
 * method.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class MonitoringDataReceiverObservableAdditionTests extends AbstractMonitoringDataReceiverTest {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = MonitoringDataReceiverObservableAdditionTests.class.getSimpleName();
    
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
     * <li>The <b>unique</b> identifier of the observable to add</li>
     * <li>The channel for which to add the observable</li>
     * <li>The URL to connect the observable to</li>
     * <li>The port number to connect the observable to</li>
     * <li>The expected return value, if {@link MonitoringDataReceiver#addObservable(String, String, String, int)} is
     *     called with the values defined above for the current test</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
        {null, null, null, -1, false},
        {"TestObservable1", TEST_CHANNELS[0], TEST_BROKER_URL, TEST_BROKER_PORT, true},
        {"TestObservable2", TEST_CHANNELS[1], TEST_BROKER_URL, TEST_BROKER_PORT, true},
    };
    
    /**
     * The constant test message to send for testing correct addition of observables.
     */
    private static final String TEST_MESSAGE = "Test message for " + ID;
    
    /**
     * The constant {@link TestMonitoringDataReceptionCallback} instance used to be informed about the reception of the
     * {@link #TEST_MESSAGE} from the current observable added as part of the tests in this class.
     */
    private static final TestMonitoringDataReceptionCallback TEST_CALLBACK = 
            new TestMonitoringDataReceptionCallback("MonitoringDataReceiverObservableAdditionTests callback");
    
    /**
     * The <b>unique</b> identifier of the observable to add.
     */
    private String testIdentifier;
    
    /**
     * The channel for which to add the observable.
     */
    private String testChannel;
    
    /**
     * The URL to connect the observable to.
     */
    private String testUrl;
    
    /**
     * The port number to connect the observable to.
     */
    private int testPort;

    /**
     * The expected return value, if {@link MonitoringDataReceiver#addObservable(String, String, String, int)} is
     * called with the current values of {@link #testIdentifier}, {@link #testChannel}, {@link #testUrl}, and
     * {@link #testPort}.
     */
    private boolean expectedAdditionReturnValue;
    
    /**
     * Constructs a new {@link MonitoringDataReceiverObservableAdditionTests} instance for testing one particular subset
     * of the {@link #TEST_DATA}.
     * 
     * @param identifier the <b>unique</b> identifier of the observable to add
     * @param channel the channel for which to add the observable
     * @param url the URL to connect the observable to
     * @param port the port number to connect the observable to
     * @param expectedAdditionReturnValue the expected return value, if
     *        {@link MonitoringDataReceiver#addObservable(String, String, String, int)} is called with the given values
     *        above
     */
    public MonitoringDataReceiverObservableAdditionTests(String identifier, String channel, String url, int port,
            boolean expectedAdditionReturnValue) {
        this.testIdentifier = identifier;
        this.testChannel = channel;
        this.testUrl = url;
        this.testPort = port;
        this.expectedAdditionReturnValue = expectedAdditionReturnValue;
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
     * Adds the {@link #TEST_CALLBACK} and checks for each of the {@link #TEST_CHANNELS} that no monitoring is already
     * established.
     */
    @BeforeClass
    public static void setUp() {
        if (!testMonitoringDataReceiverInstance.addCallback(TEST_CALLBACK)) {
            fail("Adding test callback \"" + TEST_CALLBACK + "\" failed");
        }
        for (int i = 0; i < TEST_CHANNELS.length; i++) {
            if (isMonitoringEstablished(TEST_CHANNELS[i], TEST_BROKER_URL, TEST_BROKER_PORT)) {
                fail("Monitoring at channel \"" + TEST_CHANNELS[i] + "\" must not be available yet");                
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
     * Removes the {@link #TEST_CALLBACK} and all observables at the {@link #TEST_CHANNELS} from the
     * {@link MonitoringDataReceiver}.
     */
    @AfterClass
    public static void tearDown() {
        if (!testMonitoringDataReceiverInstance.removeCallback(TEST_CALLBACK)) {
            fail("Removing test callback \"" + TEST_CALLBACK + "\" failed");
        }
        for (int i = 0; i < TEST_CHANNELS.length; i++) {
            if (!testMonitoringDataReceiverInstance.removeObservable(TEST_CHANNELS[i])) {
                fail("Removing test observable at channel \"" + TEST_CHANNELS[i] + "\" failed");
            }
        }
    }
    
    /**
     * Tests whether calling {@link MonitoringDataReceiver#addObservable(String, String, String, int)} with the current
     * values of {@link #testIdentifier}, {@link #testChannel}, {@link #testUrl}, and {@link #testPort} returns the
     * {@link #expectedAdditionReturnValue}.<br>
     * <br>
     * As double check, this method also tests whether {@link #isMonitoringEstablished(String, String, int)} with the
     * same values returns the {@link #expectedAdditionReturnValue}. This test checks that monitoring data is only
     * received, if the addition is expected to be successful. This test will fail otherwise as there must not be an
     * established monitoring, if the corresponding observable addition is expected to fail.
     */
    @Test
    public void testAddObservable() {
        assertEquals(expectedAdditionReturnValue,
                testMonitoringDataReceiverInstance.addObservable(testIdentifier, testChannel, testUrl, testPort),
                "Wrong observable addition result");
        assertEquals(expectedAdditionReturnValue, isMonitoringEstablished(testChannel, testUrl, testPort),
                "Monitoring must follow observable addition result");
    }
    
}
