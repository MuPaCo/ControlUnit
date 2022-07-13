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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import net.ssehub.devopt.controllayer.model.EntityInfo;
import net.ssehub.devopt.controllayer.model.ModelException;
import net.ssehub.devopt.controllayer.model.ModelManager;
import net.ssehub.devopt.controllayer.network.HttpRequest;
import net.ssehub.devopt.controllayer.network.HttpRequestCallback;
import net.ssehub.devopt.controllayer.network.HttpResponse;
import net.ssehub.devopt.controllayer.network.MqttV3Client;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class realizes the singleton receiver for monitoring/runtime data from individual entities of the local layer.
 * In order to monitor these entities, a valid model describing them and their properties must be available to the
 * {@link ModelManager}, which includes the necessary {@link EntityInfo}. Hence, adding new or removing existing
 * observables (local entities) is done by the {@link ModelManager}, while individual instances can register as
 * {@link MonitoringDataReceptionCallback} to receive all monitoring data.
 * 
 * @author kroeher
 *
 */
public class MonitoringDataReceiver implements MqttCallback, HttpRequestCallback {
    
    /**
     * The singleton instance of this class.
     */
    public static final MonitoringDataReceiver INSTANCE = new MonitoringDataReceiver();
    
    /**
     * The identifier of this class, e.g. for printing messages.
     */
    private static final String ID = MonitoringDataReceiver.class.getSimpleName();
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The list of other components to propagate each received message from all observables to.  
     */
    private List<MonitoringDataReceptionCallback> callbacks;
    
    /**
     * The mapping of all {@link MqttV3Client} instances created for receiving monitoring data from observables.
     * The keys for these mappings are the channels (MQTT topic name or HTTP server context name) of the respective
     * entity's monitoring scope as defined by their {@link EntityInfo} instances.
     * 
     * TODO Currently, MQTT support is enough, but to also support other channels as expected, the value type of this
     * map and, hence, its general access must be extended.
     */
    private HashMap<String, MqttV3Client> observableInformation; 
    
    /*
     * TODO Define and use a data structure that enables decoupling the reception of monitoring data from observables
     * from propagating such data to callbacks. Otherwise propagation will block further reception of new data.
     * One option is the use of a queue.
     * Keep in mind that such asynchronous access (add new data while others read older data) needs synchronization.
     */
    
    /**
     * Constructs a new {@link MonitoringDataReceiver} instance.
     */
    private MonitoringDataReceiver() {
        callbacks = new ArrayList<MonitoringDataReceptionCallback>();
        observableInformation = new HashMap<String, MqttV3Client>();
    }
    
    /**
     * Adds the given callback to the list of all callbacks of this receiver. Elements in this list will be informed
     * about any runtime data received from all observables managed by this receiver.
     * 
     * @param callback the callback to add to the list all callbacks
     * @return <code>true</code>, if the addition was successful; <code>false</code>, if the given callback is
     *         <code>null</code>, the list already contains an equal object, or {@link Collection#add(Object)} returned
     *         <code>false</code>
     * @see #removeCallback(MonitoringDataReceptionCallback)
     */
    public boolean addCallback(MonitoringDataReceptionCallback callback) {
        boolean callbackAdded = false;
        if (callback != null && !callbacks.contains(callback)) {
            callbackAdded = callbacks.add(callback);
        }
        return callbackAdded;
    }
    
    /**
     * Removes the given callback from the list of all callbacks of this receiver. Hence, the given callback will not be
     * called anymore about runtime data received from observables managed by this receiver.
     * 
     * @param callback the callback to remove from the list all callbacks
     * @return <code>true</code>, if the removal was successful; <code>false</code>, if the given callback is
     *         <code>null</code>, the list does not contain an equal object, or {@link Collection#remove(Object)}
     *         returned <code>false</code>
     * @see #addCallback(MonitoringDataReceptionCallback)
     */
    public boolean removeCallback(MonitoringDataReceptionCallback callback) {
        boolean callbackRemoved = false;
        if (callback != null && callbacks.contains(callback)) {
            callbackRemoved = callbacks.remove(callback);
        }
        return callbackRemoved;
    }
    
    /**
     * Uses the given channel, URL, and port number to establish and maintain a corresponding network connection to the
     * entity with the given identifier. The data received via that network connection is interpreted as an entity's
     * runtime data, which will be propagated to all {@link #callbacks} of this receiver.
     *
     * @param identifier the identifier of the entity as defined by its {@link EntityInfo} instance
     * @param channel the channel (MQTT topic name or HTTP server context name) of the entity's monitoring scope as
     *        defined by its {@link EntityInfo} instance
     * @param url the URL of the entity's monitoring scope as defined by its {@link EntityInfo} instance
     * @param port the port number of the entity's monitoring scope as defined by its {@link EntityInfo} instance
     * @return <code>true</code>, if this addition was successful; <code>false otherwise</code>, e.g. if the network
     *         connection could not be established
     * @see #removeObservable(String)
     */
    public boolean addObservable(String identifier, String channel, String url, int port) {
        logger.logInfo(ID, "Adding observable \"" + identifier + "\"");
        boolean observableAdded = false;
        try {
            if (!observableInformation.containsKey(channel)) {                
                MqttV3Client monitoringClient = new MqttV3Client(identifier, url, port, null, null);
                monitoringClient.subscribe(channel, 2, this);
                if (observableInformation.put(channel, monitoringClient) == null) {
                    observableAdded = true;
                } else {
                    // Should never be reached
                    logger.logWarning(ID, "Adding observable \"" + identifier 
                            + "\" replaced existing monitoring without key", "This should not happen");
                }
            } else {
                logger.logWarning(ID, "Monitoring of \"" + identifier + "\" already established",
                        "No addition of observable again");
            }
        } catch (NetworkException e) {
            logger.logException(ID, e);
        }
        return observableAdded;
    }
    
    /**
     * Closes the network connection for the given channel removes all related date from this receiver.
     * 
     * @param channel the channel (MQTT topic name or HTTP server context name) of the entity's monitoring scope as
     *        defined by its {@link EntityInfo} instance
     * @return <code>true</code>, if this removal was successful; <code>false otherwise</code>, e.g. if the network
     *         connection could not be closed
     * @see #addObservable(String, String, String, int)
     */
    public boolean removeObservable(String channel) {
        logger.logInfo(ID, "Removing observable for channel \"" + channel + "\"");
        boolean observableRemoved = false;
        if (channel != null && !channel.isBlank()) {
            if (observableInformation.containsKey(channel)) {
                MqttV3Client monitoringClient = observableInformation.remove(channel);
                if (monitoringClient != null) {
                    try {
                        monitoringClient.close();
                        observableRemoved = true;
                    } catch (NetworkException e) {
                        logger.logException(ID, e);
                    }
                } else {
                    logger.logWarning(ID, "Removing observable failed",
                            "No network client available for channel \"" + channel + "\"");
                }
            } else {
                logger.logWarning(ID, "Removing observable failed", "Unknown channel \"" + channel + "\"");
            }
        } else {
            logger.logWarning(ID, "Removing observable without channel definition is not possible");
        }
        return observableRemoved;
    }
    
    /**
     * Sends the given (monitoring) data received via the given channel to all {@link #callbacks}.
     * 
     * @param channel the channel on which the monitoring data was received; this is the MQTT topic name or HTTP server
     *        context name of the entity's monitoring scope as defined by its {@link EntityInfo} instance
     * @param data the data to propagate
     */
    private void propagateMonitoringData(String channel, String data) {
        // TODO this blocks further data reception - decouple this method calls from basic data reception
        if (channel != null && !channel.isBlank()) {
            if (data != null && !data.isBlank()) {                
                for (MonitoringDataReceptionCallback callback : callbacks) {
                    callback.monitoringDataReceived(channel, data);
                }
            } else {
                logger.logDebug(ID, "Monitoring data not propagated", "No data to propagate");
            }
        } else {
            logger.logDebug(ID, "Monitoring data not propagated", "No reception channel available");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectionLost(Throwable cause) {
        logger.logException(ID, new ModelException("Monitoring connection lost", cause));
        // TODO realize re-connect
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        /* not required here */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (message != null) {
            byte[] payload = message.getPayload();
            if (payload != null) {
                propagateMonitoringData(topic, new String(payload));
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public HttpResponse requestArrived(HttpRequest request, NetworkException exception) {
        HttpResponse response = null;
        if (request == null) {
            if (exception == null)  {
                logger.logWarning(ID, "HTTP request arrived with \"null\"-parameters");
            } else {
                logger.logError(ID, "HTTP request arrived exceptional");
                logger.logException(ID, exception);
            }
        } else {
            propagateMonitoringData(request.getUri().getPath(), request.getBody());
            int responseCode = 200; // "OK"
            String responseBody = "Monitoring data received";
            // TODO add correct response headers
            response = new HttpResponse(null, responseCode, responseBody);
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return ID;
    }

}
