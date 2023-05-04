/*
 * Copyright 2023 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.devopt.controllayer.update;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import net.ssehub.devopt.controllayer.network.HttpRequest;
import net.ssehub.devopt.controllayer.network.HttpRequestCallback;
import net.ssehub.devopt.controllayer.network.HttpResponse;
import net.ssehub.devopt.controllayer.network.HttpServer;
import net.ssehub.devopt.controllayer.network.HttpServer.ServerState;
import net.ssehub.devopt.controllayer.network.MqttV3Client;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.utilities.GenericCallback;
import net.ssehub.devopt.controllayer.utilities.GenericPropagator;
import net.ssehub.devopt.controllayer.utilities.GenericQueue;
import net.ssehub.devopt.controllayer.utilities.GenericQueue.QueueState;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class realizes a threaded receiver for incoming software update information. This information allows the control
 * unit to inform supervised local elements about updates, which trigger their update routine, potentially using a cache
 * instead of the original update location.<br>
 * <br>
 * Instances of this class always are under control of the {@link SoftwareUpdater}. Typically, there is only a single
 * instance, which calls the updater back, if a new message arrives. 
 * 
 * @author kroeher
 *
 */
public class UpdateReceiver implements Runnable, MqttCallback, HttpRequestCallback {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = UpdateReceiver.class.getSimpleName();
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The {@link Thread} in which this instance is executed.
     */
    private Thread instanceThread;
    
    /**
     * The {@link UpdateException} thrown during {@link #startInstance()}. May be <code>null</code>, if this instance is
     * not started yet (see also {@link #networkConnectionEstablished}) or starting this instance was successful.<br>
     * <br>
     * Declaring this attribute as <code>volatile</code> is mandatory as it is a shared variable between the thread,
     * which creates the instance of this class, and the {@link #instanceThread}. It enables propagating exceptions
     * thrown during starting the instance and the caller of {@link #start()}, which spans the two threads.
     */
    private volatile UpdateException instanceStartException;
    
    /**
     * The definition of whether {@link #startInstance()} successfully established the desired network connection
     * (<code>true</code>) or not (<code>false</code>). The default value is <code>false</code>.<br>
     * <br>
     * Declaring this attribute as <code>volatile</code> is mandatory as it is a shared variable between the thread,
     * which creates the instance of this class, and the {@link #instanceThread}. It enables blocking the caller of
     * {@link #start()} until establishing the network connection is finished, which spans the two threads. Blocking the
     * caller is necessary to inform about potential fail of this start via propagating the
     * {@link #instanceStartException}.
     */
    private volatile boolean networkConnectionEstablished;
    
    /**
     * The MQTT topic to subscribe to or the HTTP server context to create for incoming software update information.
     */
    private String updateChannel;
    
    /**
     * The queue for storing received messages. Removing stored messages is exclusive to the {@link #messagePropagator}.
     */
    private GenericQueue<String> messageQueue;
    
    /**
     * The propagator for removing messages from the {@link #messageQueue} and passing them to the callback given during
     * construction of this instance.
     */
    private GenericPropagator<String> messagePropagator;
    
    /**
     * The {@link Thread} in which the {@link #messagePropagator} is executed. Hence, receiving new messages and adding
     * them to the {@link #messageQueue} occurs in the {@link #instanceThread}, while their removal and further
     * processing happens in this thread without blocking the reception.
     */
    private Thread messagePropagatorThread;
    
    /**
     * The {@link MqttV3Client} instance to use for receiving incoming software update information. May be
     * <code>null</code>, if HTTP is used as update channel, see
     * {@link #createReceiver(String, String, int, String, String)}.
     */
    private MqttV3Client mqttClient;
    
    /**
     * The {@link HttpServer} instance to use for receiving incoming software update information. May be
     * <code>null</code>, if MQTT is used as update channel, see
     * {@link #createReceiver(String, String, int, String, String)}.
     */
    private HttpServer httpServer;
    
    /**
     * Constructs a new {@link UpdateReceiver} instance for receiving software update information for local elements
     * supervised by this control unit. This construction includes creating either a {@link MqttV3Client} or a
     * {@link HttpServer} instance depending on the given protocol. Based on this protocol, the update parameters are
     * used as follows:
     * <ul>
     * <li>If the protocol is <code>MQTT</code>, a {@link MqttV3Client} will be instantiate to subscribe to a software
     *     update topic of an external MQTT broker:</li>
     *     <ul>
     *     <li>The update URL defines the URL of the MQTT broker to subscribe to</li>
     *     <li>The update port defines the port of the MQTT broker to subscribe to</li>
     *     <li>The update channel defines the MQTT topic to subscribe to receive software update information</li>
     *     </ul>
     * <li>If the protocol is <code>HTTP</code>, an internal {@link HttpServer} will be instantiate to receive software
     *     update information:</li>
     *     <ul>
     *     <li>The update URL defines the address of the HTTP server to create</li>
     *     <li>The update port defines the port of the HTTP server to create</li>
     *     <li>The update channel defines the context of the HTTP server to which software update information is send
     *         to</li>
     *     </ul>
     * </ul> 
     * 
     * @param protocol the protocol to use for receiving software update information
     * @param updateUrl the URL of the MQTT broker or the HTTP server
     * @param updatePort the port of the MQTT broker or the HTTP server
     * @param updateChannel the MQTT topic to subscribe to or the HTTP server context to create
     * @param username the <i>optional</i>, non-blank user name required by some external MQTT brokers or to use for
     *        establishing the HTTP server; may be <code>null</code>, if no user name is required
     * @param password the <i>optional</i>, non-blank password required by some external MQTT brokers or to use for
     *        establishing the HTTP server; may be <code>null</code>, if no password is required
     * @param callback the instance to call back, if a new message is received
     * @throws UpdateException if the given protocol or the given reception callback is <code>null</code>, or creating
     *         the required network connection fails
     */
    //checkstyle: stop parameter number check
    public UpdateReceiver(String protocol, String updateUrl, int updatePort, String updateChannel, String username,
            String password, GenericCallback<String> callback) throws UpdateException {
        if (callback == null) {
            throw new UpdateException("Invalid software update information reception callback: \"null\"");
        }
        instanceThread = null;
        instanceStartException = null;
        networkConnectionEstablished = false;
        messageQueue = new GenericQueue<String>(100);
        messagePropagator = new GenericPropagator<String>(messageQueue);
        messagePropagator.addCallback(callback);
        this.updateChannel = updateChannel;
        createReceiver(protocol, updateUrl, updatePort, username, password);
    }
    //checkstyle: resume parameter number check
    
    /**
     * Creates the basic receiver, which is either the internal {@link #mqttClient} or the internal {@link #httpServer}.
     * 
     * @param protocol the protocol to use for receiving software update information
     * @param updateUrl the URL of the MQTT broker or the HTTP server
     * @param updatePort the port of the MQTT broker or the HTTP server
     * @param username the <i>optional</i>, non-blank user name required by some external MQTT brokers or to use for
     *        establishing the HTTP server; may be <code>null</code>, if no user name is required
     * @param password the <i>optional</i>, non-blank password required by some external MQTT brokers or to use for
     *        establishing the HTTP server; may be <code>null</code>, if no password is required
     * @throws UpdateException if the given protocol is <code>null</code> or unknown, or creating the client or the
     *         server fails
     */
    private void createReceiver(String protocol, String updateUrl, int updatePort, String username, String password)
            throws UpdateException {
        if (protocol == null) {
            throw new UpdateException("Invalid software update information protocol: \"null\"");
        }
        if (protocol.equalsIgnoreCase("MQTT")) {
            try {
                mqttClient = new MqttV3Client(ID + "Client", updateUrl, updatePort, username, password);
                logger.logDebug(ID, "MQTT client created", mqttClient.toString());
            } catch (NetworkException e) {
                throw new UpdateException("Creating software update information receiver client failed", e);
            }            
        } else if (protocol.equalsIgnoreCase("HTTP")) {            
            try {
                httpServer = new HttpServer(ID + "Server", updateUrl, updatePort, 0);
                logger.logDebug(ID, "HTTP server created", httpServer.toString());
            } catch (NetworkException e) {
                throw new UpdateException("Creating software update information server failed", e);
            }
        } else {
            throw new UpdateException("Invalid software update information protocol: \"" + protocol + "\"");
        }
    }
    
    /**
     * Starts the {@link UpdateReceiver} instance in a new {@link Thread}. Hence, the usage of this method must be equal
     * to calling {@link Thread#start()}.
     * 
     * @throws UpdateException if establishing the network connection for receiving software update information fails;
     *         this will also terminate the thread in which this instance is executed
     */
    public synchronized void start() throws UpdateException {
        if (instanceThread == null) {
            logger.logInfo(ID, "Starting instance (thread)");
            instanceThread = new Thread(this, ID);
            instanceThread.start(); // This calls run() of this runnable, which in turn calls startInstance()
            /*
             * Calling 'start()' above triggers calling 'run()' of this instance, which in turn calls 'startInstance()'.
             * The latter method sets 'networkConnectionEstablished' to 'true', if subscribing the MQTT client or
             * starting the HTTP server was successful. If establishing the network connection fails, 'startInstance()'
             * sets 'instanceStartExecption' with a new exception describing the cause of the fail. Hence, the loop
             * below will terminate in either case, but ensures that the caller of this method is blocked until this
             * instance is completely started.
             */
            while (!networkConnectionEstablished && instanceStartException == null) {
                /* Block caller until 'startInstance()' is done */
            }
            if (instanceStartException != null) {
                instanceThread = null;
                throw instanceStartException;
            }
            logger.logInfo(ID, "Instance (thread) started");
        }
    }
    
    /**
     * Stops the {@link UpdateReceiver} instance and its {@link Thread}.
     * 
     * @throws UpdateException if closing the network connection for receiving software update information or stopping
     *         the thread fails
     */
    public synchronized void stop() throws UpdateException {
        if (instanceThread != null) {
            logger.logInfo(ID, "Stopping instance (thread)");
            stopInstance();
            try {
                instanceThread.join();
                instanceThread = null;
                logger.logInfo(ID, "Instance (thread) stopped");
            } catch (InterruptedException e) {
                throw new UpdateException("Waiting for update receiver thread to join failed", e);
            }
        }
    }
    
    /**
     * Starts this instance by either subscribing to the MQTT broker and its software update information topic or
     * starting the local HTTP server with its software update information context. Which alternative is executed
     * depends on the protocol given to the constructor of this instance. Further, this method sets the
     * {@link #messageQueue} state to {@link QueueState#OPEN} and starts the {@link #messagePropagator} in its
     * {@link #messagePropagatorThread}.<br>
     * <br>
     * If starting the instance was successful can be checked via {@link #networkConnectionEstablished} and 
     * {@link #instanceStartException}.
     */
    private void startInstance() {
        if (mqttClient != null) {
            try {
                mqttClient.subscribe(updateChannel, 2, this);
                networkConnectionEstablished = true;
                logger.logInfo(ID, "Update receiver started", mqttClient.toString());
            } catch (NetworkException e) {
                instanceStartException = new UpdateException("Starting update receiver client failed", e);
            }
        } else if (httpServer != null && httpServer.getState() == ServerState.INITIALIZED) {
            try {
                httpServer.addContext(updateChannel, this);
                networkConnectionEstablished = httpServer.start();
                logger.logInfo(ID, "Update receiver started", httpServer.toString());
            } catch (NetworkException e) {
                instanceStartException =  new UpdateException("Starting update receiver server failed", e);
            }
        } else {
            // Should never be reached
            networkConnectionEstablished = false;
            instanceStartException =  new UpdateException("Starting update receiver failed:"
                    + " missing connection instance");
        }
        messageQueue.setState(QueueState.OPEN);
        messagePropagatorThread = new Thread(messagePropagator, messagePropagator.getClass().getSimpleName());
        messagePropagatorThread.start();
    }
    
    /**
     * Stops this instance by either closing the MQTT client or stopping the local HTTP server. Which alternative is
     * executed depends on the protocol given to the constructor of this instance. Further, this method sets the
     * {@link #messageQueue} state to {@link QueueState#CLOSED} and waits for the {@link #messagePropagatorThread} to
     * join.
     *   
     * @throws UpdateException if closing the MQTT client waiting for the thread to join fails; stopping the server will
     *         always be successful
     */
    private void stopInstance() throws UpdateException {
        if (mqttClient != null) {
            try {
                mqttClient.close();
                logger.logInfo(ID, "Update receiver stopped", mqttClient.toString());
            } catch (NetworkException e) {
                throw new UpdateException("Stopping update receiver client failed", e);
            }
        } else if (httpServer != null && httpServer.getState() == ServerState.RUNNING) {
            httpServer.stop(10);
            logger.logInfo(ID, "Update receiver stopped", httpServer.toString());
        } else {
            // Should never be reached
            throw new UpdateException("Stopping update receiver failed: missing connection instance");
        }
        
        messageQueue.setState(QueueState.CLOSED); // Closing the queue also stops message propagator
        try {
            messagePropagatorThread.join();
        } catch (InterruptedException e) {
            throw new UpdateException("Waiting for message propagator thread to join failed", e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void connectionLost(Throwable cause) {
        logger.logException(ID, new UpdateException(mqttClient + " lost connection", cause));
        instanceStartException = null;
        networkConnectionEstablished = false;
        logger.logInfo(ID, "Trying to reestablish client connection");
        startInstance(); // sets 'networkConnectionEstablished' to 'true', if start was successful
        if (!networkConnectionEstablished) {            
            logger.logError(ID, "Reestablishing client connection failed");
            logger.logException(ID, instanceStartException);
        }
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
        logger.logDebug(ID, "New MQTT message arrived");
        if (message != null) {
            byte[] payload = message.getPayload();
            if (payload != null) {
                messageQueue.addElement(new String(payload));
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
            logger.logDebug(ID, "New HTTP request arrived");
            messageQueue.addElement(request.getBody());
            int responseCode = 200; // "OK"
            String responseBody = "Software update information received";
            // TODO add correct response headers
            response = new HttpResponse(null, responseCode, responseBody);
            logger.logDebug(ID, "HTTP response", response.toString());
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
        if (instanceThread != null) { // ensure that this call is executed in instance thread
            startInstance();
        }
    }
    
}
