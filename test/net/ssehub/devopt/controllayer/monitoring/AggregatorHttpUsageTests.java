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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.ssehub.devopt.controllayer.AllTests;
import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.SetupException;
import net.ssehub.devopt.controllayer.network.HttpRequest;
import net.ssehub.devopt.controllayer.network.HttpRequestCallback;
import net.ssehub.devopt.controllayer.network.HttpResponse;
import net.ssehub.devopt.controllayer.network.HttpServer;
import net.ssehub.devopt.controllayer.network.NetworkException;

/**
 * This class contains unit tests for the usage of the {@link Aggregator} instance. In particular, these tests focus on
 * sending messages to this instance and receiving the corresponding aggregation results using the HTTP protocol.
 * 
 * @author kroeher
 *
 */
public class AggregatorHttpUsageTests implements HttpRequestCallback {
    
    /**
     * The constant string denoting the path to the test configuration file used to create the {@link Setup} instance.
     * This instance will be used to create the {@link Aggregator} instance for the tests in this class.
     */
    private static final String TEST_CONFIGURATION_FILE_PATH =
            AllTests.TEST_CONFIGURATION_FILES_DIRECTORY.getAbsolutePath() + File.separator
            + "aggregationProperties_http.cfg";
    
    /**
     * The string defining the URL to use for distributing aggregated data. This string must match the value of the 
     * configuration property <code>aggregation.url</code> (excluding the leading <code>http://</code>) as defined in
     * the file denoted by the {@link #TEST_CONFIGURATION_FILE_PATH}.
     */
    private static final String AGGREGATION_DISTRIBUTION_URL = "127.0.0.2";
    
    /**
     * The port number to use for distributing aggregated data. This number must match the value of the configuration
     * property <code>aggregation.port</code> as defined in the file denoted by the
     * {@link #TEST_CONFIGURATION_FILE_PATH}.
     */
    private static final int AGGREGATION_DISTRIBUTION_PORT = 80;
    
    /**
     * The channel to use for distributing aggregated data. This string must match the value of the configuration
     * property <code>aggregation.channel</code> as defined in the file denoted by the
     * {@link #TEST_CONFIGURATION_FILE_PATH}.
     */
    private static final String AGGREGATION_DISTRIBUTION_CHANNEL = "/devoptaggregation";
    
    /**
     * The constant number defining the time in milliseconds to wait for a response of an asynchronous HTTP request.
     */
    private static final long DEFAULT_ASYNC_RESPONSE_TIMEOUT = 2000;
    
    /**
     * The constant number defining the time in milliseconds to pause this thread while waiting for a response of an
     * asynchronous HTTP request.
     */
    private static final long DEFAULT_ASYNC_RESPONSE_SLEEP = 500;
    
    /**
     * The {@link HttpServer} instance representing the target to distribute aggregated data to. This instance uses the
     * same properties as defined in the configuration file denoted by the {@link #TEST_CONFIGURATION_FILE_PATH} to
     * set up a server, which then receives the aggregated data distributed by the {@link Aggregator} instance to test.
     */
    private static HttpServer aggregationReceptionServer;
    
    /**
     * The content (HTTP request body) as received via {@link #requestArrived(HttpRequest, NetworkException)}. This
     * attribute also serves as a lock to {@link #waitForAsyncResponse()} after monitoring data was send to the
     * {@link Aggregator} instance. 
     */
    private String aggregationReceptionContent;
    
    /**
     * Creates the {@link Setup} instance using the {@link #TEST_CONFIGURATION_FILE_PATH} to set up the
     * {@link Aggregator} instance for the tests in this class. Further, it creates the
     * {@link #aggregationReceptionServer} for receiving aggregated data from that instance.
     */
    @BeforeClass
    public static void setUp() {
        try {
            Setup testSetup = new Setup(TEST_CONFIGURATION_FILE_PATH);
            Aggregator.setUp(testSetup);
            aggregationReceptionServer = new HttpServer("AggregationServer", AGGREGATION_DISTRIBUTION_URL,
                    AGGREGATION_DISTRIBUTION_PORT, 0);
        } catch (SetupException | MonitoringException | NetworkException e) {
            fail("Setting up the aggregator HTTP usage tests failed unexpectedly", e);
        }
    }
    
    /**
     * Stops the {@link #aggregationReceptionServer} and tears the {@link Aggregator} instance down.
     */
    @AfterClass
    public static void tearDown() {
        if (aggregationReceptionServer != null) {
            aggregationReceptionServer.stop(2);
            aggregationReceptionServer = null;
        }
        try {
            Aggregator.tearDown();
        } catch (MonitoringException e) {
            fail("Tearing down the aggregator HTTP usage tests failed unexpectedly", e);
        }
    }
    
    /**
     * Tests the correct distribution of aggregated data via HTTP using the {@link #aggregationReceptionServer} as
     * receiver.
     */
    @Test
    public void testAggregatorHttpUsage() {
        try {
            aggregationReceptionServer.addContext(AGGREGATION_DISTRIBUTION_CHANNEL, this);
            if (!aggregationReceptionServer.start()) {
                throw new NetworkException("Starting the server was not successful");
            }
        } catch (NetworkException e) {
            fail("Starting server for aggregator HTTP usage tests failed unexpectedly", e);
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
     * Calls {@link Thread#sleep(long)} with {@link #DEFAULT_ASYNC_RESPONSE_SLEEP} as long as
     * {@link #aggregationReceptionContent} is <code>null</code> and {@link #DEFAULT_ASYNC_RESPONSE_TIMEOUT} is not
     * reached. This method is used to postpone the execution of any further actions, if a HTTP request is send
     * asynchronously. In this case, the required response for testing arrives later via
     * {@link #responseArrived(HttpResponse, NetworkException)}. 
     */
    private void waitForAsyncResponse() {
        long currentTimeMillis = System.currentTimeMillis();
        long endTimeMillis = currentTimeMillis + DEFAULT_ASYNC_RESPONSE_TIMEOUT;
        while (aggregationReceptionContent == null && currentTimeMillis < endTimeMillis) {
            try {
                Thread.sleep(DEFAULT_ASYNC_RESPONSE_SLEEP);
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
    public HttpResponse requestArrived(HttpRequest request, NetworkException exception) {
        HttpResponse requestResponse = null;
        if (request == null || request.getBody().isBlank()) {
            requestResponse = new HttpResponse(null, 500, "No request body");
        } else if (exception != null) {
            requestResponse = new HttpResponse(null, 500, "Exception arrived: " + exception.getMessage());
        } else {
            aggregationReceptionContent = request.getBody();
            requestResponse = new HttpResponse(null, 200, "Request arrived: " + aggregationReceptionContent);
        }
        return requestResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
    
}
