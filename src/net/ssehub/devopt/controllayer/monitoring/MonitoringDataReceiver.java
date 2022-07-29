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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import net.ssehub.devopt.controllayer.model.EntityInfo;
import net.ssehub.devopt.controllayer.model.ModelManager;
import net.ssehub.devopt.controllayer.network.HttpRequest;
import net.ssehub.devopt.controllayer.network.HttpRequestCallback;
import net.ssehub.devopt.controllayer.network.HttpResponse;
import net.ssehub.devopt.controllayer.network.MqttV3Client;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.utilities.GenericCallback;
import net.ssehub.devopt.controllayer.utilities.GenericPropagator;
import net.ssehub.devopt.controllayer.utilities.GenericQueue;
import net.ssehub.devopt.controllayer.utilities.GenericQueue.QueueState;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class realizes the threaded singleton receiver for monitoring/runtime data from individual entities. In order to
 * monitor these entities, a valid model describing them and their properties must be available to the
 * {@link ModelManager}. Hence, adding new or removing existing observables (entities) is done by the
 * {@link ModelManager}, while individual components can register as callbacks to receive all monitoring data.
 * 
 * @author kroeher
 *
 */
public class MonitoringDataReceiver implements Runnable, MqttCallback, HttpRequestCallback {
    
    /**
     * The identifier of this class, e.g. for printing messages.
     */
    private static final String ID = MonitoringDataReceiver.class.getSimpleName();
    
    /**
     * The singleton instance of this class.
     */
    private static MonitoringDataReceiver instance;
    
    /**
     * The {@link Thread} in which the {@link #instance} is executed.
     */
    private static Thread instanceThread;
    
    /**
     * The definition of whether {@link #run()} successfully established this instance (<code>true</code>) or not
     * (<code>false</code>). The default value is <code>false</code>.<br>
     * <br>
     * Declaring this attribute as <code>volatile</code> is mandatory as it is a shared variable between the thread,
     * which creates the instance of this class, and the {@link #instanceThread}. It enables blocking the caller of
     * {@link #MonitoringDataReceiver()} until establishing the instance is finished, which spans the two threads.
     * Blocking the caller is necessary to ensure adding or removing callbacks or observables does not yield a
     * {@link NullPointerException} as instantiation is not completed.
     */
    private volatile boolean instanceRunning;
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The mapping of all {@link MqttV3Client} instances created for receiving monitoring data from observables.
     * The keys for these mappings are the channels (MQTT topic name or HTTP server context name) of the respective
     * entity's monitoring scope as defined by their {@link EntityInfo} instances.
     * 
     * TODO Currently, MQTT support is enough, but to also support other channels as expected, the value type of this
     * map and, hence, its general access must be extended.
     */
    private HashMap<String, MqttV3Client> observableInformation;
    
    /**
     * The queue for storing received monitoring data. Removing stored monitoring data is exclusive to the
     * {@link #monitoringDataPropagator}.
     */
    private GenericQueue<MonitoringData> monitoringDataQueue;
    
    /**
     * The propagator for removing monitoring data from the {@link #monitoringDataQueue} and passing them to the
     * callbacks added via {@link #addCallback(GenericCallback)}.
     */
    private GenericPropagator<MonitoringData> monitoringDataPropagator;
    
    /**
     * The {@link Thread} in which the {@link #monitoringDataPropagator} is executed. Hence, receiving new monitoring
     * data and adding them to the {@link #monitoringDataQueue} occurs in the {@link #instanceThread}, while their
     * removal and further processing happens in this thread without blocking the reception.
     */
    private Thread monitoringDataPropagatorThread;
    
    /**
     * Constructs a new {@link MonitoringDataReceiver} instance and starts it in its own {@link Thread}.
     */
    private MonitoringDataReceiver() {
        Logger.INSTANCE.logInfo(ID, "Starting instance (thread)");
        instanceRunning = false; // set to 'true' at the end of 'run()' (see loop below)
        instanceThread = new Thread(this, ID);
        instanceThread.start();
        while (!instanceRunning) {
            /*
             * Block finalizing construction of the singleton instance until it is actually running. This instances
             * runs, if 'run()' is executed successfully. Hence, 'instanceRunning' is set to 'true' at the end of that
             * method. 
             */
        }
        Logger.INSTANCE.logInfo(ID, "Instance (thread) started");
    }
    
    /**
     * Returns the singleton instance of this class. <b>Note</b> that it is required to call {@link #createInstance()}
     * exactly once before calling this method successfully.
     * 
     * @return the singleton instance of this class, or <code>null</code>, if it was not created yet or it was stopped
     *         already
     */
    public static MonitoringDataReceiver getInstance() {
        return instance;
    }
    
    /**
     * Creates the singleton instance of this class in its own {@link Thread}.
     */
    public static void createInstance() {
        if (instance == null) {
            instance = new MonitoringDataReceiver();
        }
    }
    
    /**
     * Stops the {@link MonitoringDataReceiver} and its {@link Thread}. A successful stop also deletes the singleton
     * instance of this class.
     * 
     * @throws MonitoringException if stopping internal threads fails
     */
    public synchronized void stop() throws MonitoringException {
        if (instanceThread != null) {
            logger.logInfo(ID, "Stopping monitoring data receiver");
            // Remove all observables, which includes stopping the respective monitoring clients
            Iterator<String> observableInformationKeysIterator = observableInformation.keySet().iterator();
            while (observableInformationKeysIterator.hasNext()) {
                removeObservable(observableInformationKeysIterator.next());
            }
            // Close queue, which also stops the monitoring data propagator
            monitoringDataQueue.setState(QueueState.CLOSED);
            String thread = null;
            try {
                thread = "monitoring data propagator";
                monitoringDataPropagatorThread.join();
                thread = "monitoring data receiver";
                instanceThread.join();
            } catch (InterruptedException e) {
                throw new MonitoringException("Waiting for " + thread + " thread to join failed", e);
            } finally {
                monitoringDataPropagatorThread = null;                
                instanceThread = null;
                logger = null;
                observableInformation.clear(); // Should be empty already, but ensure all elements are removed
                observableInformation = null;
                monitoringDataQueue = null;
                monitoringDataPropagator.removeCallbacks();
                monitoringDataPropagator = null;
                instance = null;
            }
            // Global access to logger due to setting local reference to 'null' above
            Logger.INSTANCE.logInfo(ID, "Monitoring data receiver stopped");
        }
    }
    
    /**
     * Adds the given callback such that it will be informed about all monitoring data received from all observables
     * managed by this receiver.
     * 
     * @param callback the callback to add
     * @return <code>true</code>, if the addition was successful; <code>false</code>, if the given callback is
     *         <code>null</code>, this receiver already contains an equal object, or {@link Collection#add(Object)}
     *         returned <code>false</code>
     * @see #removeCallback(MonitoringDataReceptionCallback)
     */
    public synchronized boolean addCallback(GenericCallback<MonitoringData> callback) {
        return monitoringDataPropagator.addCallback(callback);
    }
    
    /**
     * Removes the given callback such that it will not be informed anymore about monitoring data received from
     * observables managed by this receiver.
     * 
     * @param callback the callback to remove
     * @return <code>true</code>, if the removal was successful; <code>false</code>, if the given callback is
     *         <code>null</code>, this receiver does not contain an equal object, or {@link Collection#remove(Object)}
     *         returned <code>false</code>
     * @see #addCallback(MonitoringDataReceptionCallback)
     */
    public synchronized boolean removeCallback(GenericCallback<MonitoringData> callback) {
        return monitoringDataPropagator.removeCallback(callback);
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
    public synchronized boolean addObservable(String identifier, String channel, String url, int port) {
        logger.logInfo(ID, "Adding observable \"" + identifier + "\"");
        boolean observableAdded = false;
        try {
            if (!observableInformation.containsKey(channel)) {
                if (url != null && url.startsWith("tcp://")) {                    
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
                    logger.logWarning(ID, "Monitoring of \"" + identifier + "\" not possible",
                            "Protocol not support: " + url);
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
     * Closes the network connection for the given channel and removes all related date from this receiver.
     * 
     * @param channel the channel (MQTT topic name or HTTP server context name) of the entity's monitoring scope as
     *        defined by its {@link EntityInfo} instance
     * @return <code>true</code>, if this removal was successful; <code>false otherwise</code>, e.g. if the network
     *         connection could not be closed
     * @see #addObservable(String, String, String, int)
     */
    public synchronized boolean removeObservable(String channel) {
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
     * {@inheritDoc}
     */
    @Override
    public void connectionLost(Throwable cause) {
        logger.logException(ID, new MonitoringException("Monitoring connection lost", cause));
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
                monitoringDataQueue.addElement(new MonitoringData(topic, new String(payload),
                        System.currentTimeMillis()));
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
            monitoringDataQueue.addElement(new MonitoringData(request.getUri().getPath(), request.getBody(),
                    System.currentTimeMillis()));
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        observableInformation = new HashMap<String, MqttV3Client>();
        monitoringDataQueue = new GenericQueue<MonitoringData>(100);
        monitoringDataPropagator = new GenericPropagator<MonitoringData>(monitoringDataQueue);
        monitoringDataQueue.setState(QueueState.OPEN);
        monitoringDataPropagatorThread = new Thread(monitoringDataPropagator,
                monitoringDataPropagator.getClass().getSimpleName());
        monitoringDataPropagatorThread.start();
        instanceRunning = true;
    }

}
