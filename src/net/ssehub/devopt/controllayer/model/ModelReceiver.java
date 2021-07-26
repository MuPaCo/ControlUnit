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
package net.ssehub.devopt.controllayer.model;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import net.ssehub.devopt.controllayer.network.HttpRequest;
import net.ssehub.devopt.controllayer.network.HttpRequestCallback;
import net.ssehub.devopt.controllayer.network.HttpResponse;
import net.ssehub.devopt.controllayer.network.HttpServer;
import net.ssehub.devopt.controllayer.network.MqttV3Client;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * TODO .
 * @author kroeher
 *
 */
public class ModelReceiver implements MqttCallback, HttpRequestCallback {
    
    /**
     * The identifier of this class, e.g., for logging messages during instance creation. 
     */
    private static final String ID = ModelReceiver.class.getSimpleName();

    /*
     * Steht unter der Kontrolle des ModelManagers, den es noch nicht gibt.
     * 
     * Erstellt die entsprechende Netzwerkverbindung, auf der für mögliche Registrierungen gelauscht wird:
     *  HTTP oder MQTT (oder beides?)
     *  
     * 
     *  
     *  Es gibt nur eine ModelReceiver-Instanz für den Manager
     *  ModelReceiver selbst kein eigener Thread, kann aber durch Manager in separaten Thread ausgeführt werden?!
     *  Dazu:
     *      ModelManager kennt ModelReceiver (erstellt diesen auch) und bekommt geparstes Modell zur Ablage
     *      Wichtig: ModelManager arbeitet parallel als Versorger bzgl. Modellinformationen an andere Komponenten und
     *               Verwaltung neuer Modelle, die vom Empfänger kommen
     *  
     */
    
    private Logger logger = Logger.INSTANCE;
    
    private ModelReceptionCallback callback;
    
    private String receptionChannel;
    
    private MqttV3Client mqttClient;
    
    private HttpServer httpServer;
    
    /**
     * Constructs a new {@link ModelReceiver} instance for receiving models of local elements as a registration for
     * supervision by the parent controller of this instance. This construction includes creating either a
     * {@link MqttV3Client} or a {@link HttpServer} instance depending on the given protocol. Based on this protocol,
     * the reception*-parameters are used as follows:
     * <ul>
     * <li>If the protocol is <code>MQTT</code>, a {@link MqttV3Client} will be instantiate to subscribe to a
     *     registration topic of an external MQTT broker:</li>
     *     <ul>
     *     <li>The reception URL defines the URL of the MQTT broker to subscribe to</li>
     *     <li>The reception port defines the port of the MQTT broker to subscribe to</li>
     *     <li>The reception channel defines the MQTT topic to subscribe to receive models for registration</li>
     *     </ul>
     * <li>If the protocol is <code>HTTP</code>, an internal {@link HttpServer} will be instantiate to receive
     *     models:</li>
     *     <ul>
     *     <li>The reception URL defines the address of the HTTP server to create</li>
     *     <li>The reception port defines the port of the HTTP server to create</li>
     *     <li>The reception channel defines the context of the HTTP server to which models are send for
     *         registration</li>
     *     </ul>
     * </ul> 
     * 
     * @param protocol the protocol to use for receiving models
     * @param receptionUrl the URL of the MQTT broker or the HTTP server
     * @param receptionPort the port of the MQTT broker or the HTTP server
     * @param receptionChannel the MQTT topic to subscribe to or the HTTP server context to create
     * @param username the <i>optional</i>, non-blank user name required by some external MQTT brokers or to use for
     *        establishing the HTTP server; may be <code>null</code>, if no user name is required
     * @param password the <i>optional</i>, non-blank password required by some external MQTT brokers or to use for
     *        establishing the HTTP server; may be <code>null</code>, if no password is required
     * @param callback the instance to call back, if a new model is received
     * @throws ModelException if the given protocol or the given reception callback is <code>null</code>, or creating
     *         the required network connection fails
     */
    //checkstyle: stop parameter number check
    public ModelReceiver(String protocol, String receptionUrl, int receptionPort, String receptionChannel,
            String username, String password, ModelReceptionCallback callback) throws ModelException {
        if (callback == null) {
            throw new ModelException("Invalid model reception callback: \"null\"");
        }
        this.callback = callback;
        this.receptionChannel = receptionChannel;
        createReceiver(protocol, receptionUrl, receptionPort, username, password);
    }
    //checkstyle: resume parameter number check
    
    /**
     * Creates the basic receiver, which is either the internal {@link #mqttClient} or the internal {@link #httpServer}.
     * 
     * @param protocol the protocol to use for receiving models
     * @param receptionUrl the URL of the MQTT broker or the HTTP server
     * @param receptionPort the port of the MQTT broker or the HTTP server
     * @param username the <i>optional</i>, non-blank user name required by some external MQTT brokers or to use for
     *        establishing the HTTP server; may be <code>null</code>, if no user name is required
     * @param password the <i>optional</i>, non-blank password required by some external MQTT brokers or to use for
     *        establishing the HTTP server; may be <code>null</code>, if no password is required
     * @throws ModelException if the given protocol is <code>null</code> or unknown, or creating the client or the
     *         server fails
     */
    private void createReceiver(String protocol, String receptionUrl, int receptionPort, String username,
            String password) throws ModelException {
        if (protocol == null) {
            throw new ModelException("Invalid model reception protocol: \"null\"");
        }
        if (protocol.equalsIgnoreCase("MQTT")) {
            try {
                mqttClient = new MqttV3Client(ID + "Client", receptionUrl, receptionPort, username, password);
                logger.logDebug(ID, "MQTT client created", mqttClient.toString());
            } catch (NetworkException e) {
                throw new ModelException("Creating model receiver client failed", e);
            }            
        } else if (protocol.equalsIgnoreCase("HTTP")) {            
            try {
                httpServer = new HttpServer(ID + "Server", receptionUrl, receptionPort, 0);
                logger.logDebug(ID, "HTTP server created", httpServer.toString());
            } catch (NetworkException e) {
                throw new ModelException("Creating model receiver server failed", e);
            }
        } else {
            throw new ModelException("Invalid model reception protocol: \"" + protocol + "\"");
        }
    }
    
    /**
     * Starts this instance by either subscribing to the MQTT broker and its registration topic or starting the local
     * HTTP server with its registration context. Which alternative is executed depends on the protocol given to the
     * constructor of this instance.
     *   
     * @throws ModelException if subscribing or starting the server fails
     */
    public void start() throws ModelException {
        if (mqttClient != null) {
            try {
                mqttClient.subscribe(receptionChannel, 2, this);
                logger.logInfo(ID, "ModelReceiver started", mqttClient.toString());
            } catch (NetworkException e) {
                throw new ModelException("Starting model receiver client failed", e);
            }
        } else if (httpServer != null) {
            try {
                httpServer.addContext(receptionChannel, this);
                httpServer.start();
                logger.logInfo(ID, "ModelReceiver started", httpServer.toString());
            } catch (NetworkException e) {
                throw new ModelException("Starting model receiver server failed", e);
            }
        } else {
            // Should never be reached
            throw new ModelException("Starting model receiver failed: missing connection instance");
        }
    }
    
    // Return null if everything is ok!
    private String processRegistration(String receivedMessage) {
        String response = null;
        if (receivedMessage == null || receivedMessage.isBlank()) {
            response = "Received message is empty";
        } else {
            /*
             * Workflow:
             *  1. Registration-Anfrage wird empfangen
             *  2. Body/Message wird an EASy/IVML zum parsen übergeben
             *  3a. Parsen schlägt fehl -> Response = Entsprechende Fehlermdeldung "Parsing failed"
             *  3b. Parsen erfolgreich, dann weiter mit 4.
             *  4. Geparstes Modell in DB oder sowas ablegen: entweder neu hinzufügen oder altes überschreiben
             *  5a. Ablage schlägt fehl -> Response = Entsprechende Fehlermdeldung "Ablage failed (warum?)"
             *  5b. Ablage erfolgreich -> Response = null
             */
            
            callback.modelReceived(receivedMessage); // TODO add parsed model as parameter
        }
        return response;
    }

    @Override
    public void connectionLost(Throwable cause) {
        logger.logException(ID, new ModelException(mqttClient + " lost connection", cause));
        logger.logInfo(ID, "Trying to reestablish client connection");
        try {
            start();
        } catch (ModelException e) {
            logger.logError(ID, "Reestablishing client connection failed");
            logger.logException(ID, e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        /* not required here */
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (message != null) {
            byte[] payload = message.getPayload();
            if (payload != null) {
                /*
                 * TODO the following call returns "null" (if registration successful) or a problem description (if
                 * registration was not successful) depending on the result of processing the registration.
                 * However, it is unclear how such a reply should be delivered: again via an additional
                 * MQTT client for publishing (which would mean that the sender waits for that reply and disconnects
                 * later and we need another clent for publishing) or via HTTP, but thats a completely different
                 * protocol
                 * 
                 * For now there is no reply
                 */
                processRegistration(new String(payload));
            }
        }
    }

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
            logger.logInfo(ID, "HTTP request arrived");
            int responseCode = 400; // "Bad request" as default
            String responseBody = processRegistration(request.getBody());
            if (responseBody == null) {
                // Registration ok, change response code to "OK"
                responseCode = 200;
                responseBody = "Registration successful";
            }
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
