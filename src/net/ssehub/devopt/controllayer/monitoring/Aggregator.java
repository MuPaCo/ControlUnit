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

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Iterator;

import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.model.EntityInfo;
import net.ssehub.devopt.controllayer.network.HttpClient;
import net.ssehub.devopt.controllayer.network.HttpResponseCallback;
import net.ssehub.devopt.controllayer.network.MqttV3Client;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class manages the aggregation of received monitoring data to provide runtime data of higher value to other
 * internal components and, optionally, to external elements via a dedicated network connection.
 * 
 * @author kroeher
 *
 */
public class Aggregator implements MonitoringDataReceptionCallback, HttpResponseCallback {
    
    /**
     * The singleton instance of this class.
     */
    protected static Aggregator instance; // Keep 'protected' to enable usage tests
    
    /**
     * The identifier of this class, e.g. for printing messages.
     */
    private static final String ID = Aggregator.class.getSimpleName();    
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;

    /**
     * The definition of whether aggregation results must be distributed via the {@link #mqttClient} or the
     * {@link #httpClient} (<code>true</code>) or not (<code>false</code>).
     */
    private boolean distributeAggregationResult;
    
    /**
     * The {@link MqttV3Client} instance for distributing aggregation results. Distribution via MQTT uses this client
     * instance (instantiated exactly once) to publish aggregation results on a topic defined by the
     * {@link #distributionChannel} at a particular MQTT broker. This instance may be <code>null</code>, if the
     * {@link #httpClient} must be used or no distribution is desired.
     */
    private MqttV3Client mqttClient;
    
    /**
     * The {@link HttpClient} instance for distributing aggregation results. Distribution via HTTP uses this client
     * instance (a new instance per distribution) to send an asynchronous POST-request to the server defined by the
     * {@link #distributionHttpRequestUriString} with the {@link #distributionHttpRequestHeaders}. This instance may be
     * <code>null</code>, if the {@link #mqttClient} must be used or no distribution is desired.
     */
    private HttpClient httpClient;
    
    /**
     * The network connection channel to use for distributing aggregation results. This string either denotes the MQTT
     * topic or the HTTP context depending on the protocol and, hence, client instance ({@link #mqttClient} or 
     * {@link #httpClient}) to use. This channel may be <code>null</code>, if no distribution is desired.
     */
    private String distributionChannel;
    
    /**
     * The URI string identifying the HTTP server (including the specific context as defined by the
     * {@link #distributionChannel}) to send the aggregation results to. This string may be <code>null</code>, if the
     * {@link #mqttClient} must be used or no distribution is desired.
     */
    private String distributionHttpRequestUriString;
    
    /**
     * The headers (name-value-sequence) to add to the HTTP POST-request that distributes aggregation results. This
     * instance may be <code>null</code>, if the {@link #mqttClient} must be used or no distribution is desired.
     */
    private String[] distributionHttpRequestHeaders;
    
    /**
     * The mapping of the recent entity messages received via monitoring. The keys for these mappings are the channels
     * on which the respective message arrived. This mapping will be updated as part of
     * {@link #aggregate(String, String)}, which is called for each {@link #monitoringDataReceived(String, String)}.
     * Hence, this mapping always contains all recent messages of all entities under monitoring. 
     */
    private HashMap<String, String> entityMessages;
   
    /**
     * Constructs a new {@link Aggregator} instance.
     */
    private Aggregator() {
        distributeAggregationResult = false;
        mqttClient = null;
        httpClient = null;
        distributionChannel = null;
        distributionHttpRequestUriString = null;
        distributionHttpRequestHeaders = null;
        entityMessages = new HashMap<String, String>();
    }
    
    /**
     * Creates the singleton instance of this class based on the given setup.
     * 
     * @param setup the configuration properties to use for setting up the singleton instance of this class
     * @throws MonitoringException if setting up the instance failed or the instance is already set up
     * @see #tearDown()
     */
    public static void setUp(Setup setup) throws MonitoringException {
        if (instance == null) {
            if (setup != null) {
                instance = new Aggregator();
                instance.prepareDistribution(setup);
                if (!MonitoringDataReceiver.INSTANCE.addCallback(instance)) {
                    throw new MonitoringException("Adding aggregator as monitoring data receiver callback failed");
                }
            } else {
                throw new MonitoringException("Setup is \"null\"");
            }
        } else {
            throw new MonitoringException("Aggregator instance already set up");
        }
    }
    
    /**
     * Deletes the singleton instance of this class and releases associated resources.
     *  
     * @throws MonitoringException if tearing down the instance failed or the instance is already teared down
     * @see #setUp(Setup)
     */
    public static void tearDown() throws MonitoringException {
        if (instance != null) {
            if (MonitoringDataReceiver.INSTANCE.removeCallback(instance)) {                
                instance.destruct();
                instance = null;
            } else {
                throw new MonitoringException("Removing aggregator as monitoring data receiver callback failed");
            }
        } else {
            throw new MonitoringException("Aggregator instance already teared down");
        }
    }
    
    /**
     * Sets the internal attributes related to the distribution of aggregated data and creates the internal
     * {@link #mqttClient} instance, if MQTT is the defined distribution protocol. If HTTP is the desired protocol, the
     * internal {@link #httpClient} instance will be created for each call to {@link #distribute(String)} later.<br>
     * <br>
     * However, these actions are only performed, if the given setup provides the necessary configuration properties. If
     * these optional properties are not defined, this method does nothing, but logging a corresponding warning.
     * 
     * @param setup the configuration properties to use for preparing the distribution of aggregated data
     * @throws MonitoringException if creating the client fails
     */
    private void prepareDistribution(Setup setup) throws MonitoringException {
        if (setup.hasAggregationConfiguration()) {            
            distributionChannel = setup.getAggregationConfiguration(Setup.KEY_AGGREGATION_CHANNEL);
            String url = setup.getAggregationConfiguration(Setup.KEY_AGGREGATION_URL);
            int port = Integer.parseInt(setup.getAggregationConfiguration(Setup.KEY_AGGREGATION_PORT));
            String protocol = setup.getAggregationConfiguration(Setup.KEY_AGGREGATION_PROTOCOL);
            if (protocol.equalsIgnoreCase("MQTT")) {
                try {
                    mqttClient = new MqttV3Client(ID + "Client", url, port, null, null);
                    distributeAggregationResult = true;
                    logger.logDebug(ID, "Aggregator MQTT client created", mqttClient.toString());
                } catch (NetworkException e) {
                    throw new MonitoringException("Creating aggregator distribution client failed", e);
                } 
            } else if (protocol.equalsIgnoreCase("HTTP")) {
                distributionHttpRequestUriString = url + distributionChannel;
                distributionHttpRequestHeaders = null; // TODO Add correct headers, if required
                distributeAggregationResult = true;
                logger.logDebug(ID, "Aggregator HTTP client properties defined",
                        "Request URI: " + distributionHttpRequestUriString,
                        "Request headers: " + distributionHttpRequestHeaders);
            } else {
                throw new MonitoringException("Invalid aggregation distribution protocol: \"" + protocol + "\"");
            }
        } else {
            logger.logWarning(ID, "No aggregation configuration properties defined",
                    "No distribution of aggregated data possible");
        }
    }
    
    /**
     * Closes any open network connection (if available) and releases all references to internal resources.
     * 
     * @throws MonitoringException if closing the network connection fails
     */
    private void destruct() throws MonitoringException {
        if (mqttClient != null) {                
            try {
                mqttClient.close();
                mqttClient = null;
            } catch (NetworkException e) {
                throw new MonitoringException("Deleting aggregator distribution client failed", e);
            }
        } else if (httpClient != null) {
            // Cannot actively close a HttpClient instance; see #close() in corresponding class definition
            httpClient = null;
        }
        distributionChannel = null;
        distributionHttpRequestUriString = null;
        distributionHttpRequestHeaders = null;
        entityMessages.clear();
        entityMessages = null;
    }
    
    /**
     * Aggregates the data of the {@link #entityMessages}. For this purpose, this method first updates these messages by
     * the given reception channel (key) and the given data (value).<br>
     * <br>
     * TODO the current implementation is specific to the usage in the DevOpt physical demonstrator. A different
     *      mechanism to easily exchange aggregation algorithms (maybe also combine them) in a generic way is desired
     *      in future.
     *   
     * @param receptionChannel the channel on which the monitoring data was received; this is the MQTT topic name or
     *        HTTP server context name of the entity's monitoring scope as defined by its {@link EntityInfo} instance 
     * @param receivedData the data received via the channel
     */
    private void aggregate(String receptionChannel, String receivedData) {
        entityMessages.put(receptionChannel, receivedData);
        String aggregationResult = null;
        
        Iterator<String> entityMessagesKeysIterator = entityMessages.keySet().iterator();
        String entityData;
        int totalBalance = 0;
        while (entityMessagesKeysIterator.hasNext()) {
            entityData = entityMessages.get(entityMessagesKeysIterator.next());
            if (entityData != null && !entityData.isBlank()) {
                try {                    
                    totalBalance += Integer.parseInt(entityData.trim());
                } catch (NumberFormatException e) {
                    logger.logError(ID, "Aggregation failed", "\"" + entityData + "\" is not a number");
                }
            }
        }
        
        aggregationResult = "" + totalBalance;
        distribute(aggregationResult);
    }
    
    /**
     * Distributes the given aggregated data either via the {@link #mqttClient} or a new {@link #httpClient}, if
     * {@link #distributeAggregationResult} is <code>true</code> and the given data is not <code>null</code>.
     * 
     * @param aggregatedData the data to distribute as aggregation result
     */
    private void distribute(String aggregatedData) {
        if (distributeAggregationResult && aggregatedData != null) {
            try {
                if (mqttClient != null) {                    
                    mqttClient.publish(distributionChannel, 2, aggregatedData.getBytes());
                } else if (distributionHttpRequestUriString != null) {
                    httpClient = new HttpClient(ID + "Client", null, null, null, -1, null, null);
                    httpClient.sendPostAsync(distributionHttpRequestUriString, distributionHttpRequestHeaders,
                            aggregatedData, 1000, this);
                } else {
                    logger.logWarning(ID, "Distributing aggregated data failed", "No network client available");
                }
            } catch (NetworkException e) {
                logger.logException(ID, e);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void monitoringDataReceived(String channel, String data) {
        logger.logDebug(ID, "Monitoring data received", "Channel: " + channel, "Data: " + data);
        aggregate(channel, data); // TODO this will block the caller of this method until aggregation is done
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void responseArrived(HttpResponse<String> response, NetworkException exception) {
        logger.logDebug(ID, "Aggregated data distribution response received", "Response: " + response,
                "Excpetion: " + exception);
        // Nothing to do here
    }
    
}
