 /* Copyright 2021 University of Hildesheim, Software Systems Engineering
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

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * This class realizes a MQTTv3 client for publishing and subscribing to a MQTT broker.
 * 
 * @author kroeher
 *
 */
public class MqttV3Client extends AbstractNetworkClient {
    
    /**
     * The identifier of this class, e.g., for logging messages during instance creation. 
     */
    private static final String ID = MqttV3Client.class.getSimpleName();
    
    /**
     * The actual MQTT client instance provided by the Eclipse Paho project (https://www.eclipse.org/paho/).
     */
    private MqttClient client;
    
    /**
     * The MQTT connection options required by a client to subscribe to the desired broker. These options are set during
     * {@link #createClient(String, int)}.
     */
    private MqttConnectOptions connectionOptions;
    
    /**
     * The textual representation of the MQTT broker this client is connected to. This representation contains the URL
     * and port of the broker, e.g. "tcp://broker.hivemq.com:1883"
     */
    private String broker;

    /**
     * Constructs a new {@link MqttV3Client} instance for connection to a MQTT broker.
     * 
     * @param id the identifier of this client consisting of 1 to 23 UTF-8 encoded bytes representing the characters
     *        [0..9], [a..z], [A..Z] only
     * @param brokerUrl the URL or IP of the MQTT broker this client connects to; must always start with "tcp://"
     * @param brokerPort the port of the MQTT broker this client connects to; must be between 0 and 65535
     * @param username the <i>optional</i>, non-blank user name required for this client to communicate with its broker;
     *        may be <code>null</code>, if no user name is required
     * @param password the <i>optional</i>, non-blank password required for this client to communicate with its broker;
     *        may be <code>null</code>, if no password is required
     * @throws NetworkException if the given identifier is <code>null</code> or does not match the expected format, the
     *         given broker URL is <code>null</code> or blank, or the given broker port is not in range
     */
    public MqttV3Client(String id, String brokerUrl, int brokerPort, String username, String password)
            throws NetworkException {
        super(id, username, password);
        createClient(brokerUrl, brokerPort);
    }

    /**
     * Creates the actual MQTT {@link #client} for the given broker (information).
     * 
     * @param brokerUrl the URL or IP of the MQTT broker this client connects to; must always start with "tcp://"
     * @param brokerPort the port of the MQTT broker this client connects to; must be between 0 and 65535
     * @throws NetworkException if the given broker URL is <code>null</code> or blank, the given broker port is not in
     *         range, or a MQTT exception occurred during client creation
     */
    private void createClient(String brokerUrl, int brokerPort) throws NetworkException {
        if (brokerUrl == null) {
            throw new NetworkException("Invalid MQTT client broker URL: \"null\"");
        }
        if (brokerUrl.isBlank()) {
            throw new NetworkException("Invalid MQTT client broker URL: URL is blank");
        }
        if (brokerPort < 0 || brokerPort > 65535) {
            throw new NetworkException("Invalid MQTT client broker port: " + brokerPort);
        }
        this.broker = brokerUrl + ":" + brokerPort;
        
        /*
         * Using the MqttDefaultFilePersistence for storing message results in runtime warnings produced by the client
         * library. It does not seem to be problematic, but annoying. Hence, use the MemoryPersistence for now.
         * 
         * More information on the client persistence:
         * https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClientPersistence.html
         * 
         * MqttDefaultFilePersistence dataStore = null;
         * if (dataStoreDirectoryPath != null) {
         *    dataStore = new MqttDefaultFilePersistence(dataStoreDirectoryPath);
         * } else {
         *    dataStore = new MqttDefaultFilePersistence(DEFAULT_DATA_STORE_DIRECTORY);
         * }
         */
        MemoryPersistence dataStore = new MemoryPersistence();
        
        connectionOptions = new MqttConnectOptions();
        connectionOptions.setCleanSession(true); // default value; maybe configurable via constructor in future
        // This default re-connection behavior from the paho mqtt client package
        // setAutomaticReconnect re-connects to the client after every 2 seconds and increases 
        // the delay to 2 minutes by an increment of 1 second and keeps re-connecting in intervals
        connectionOptions.setAutomaticReconnect(true); 
        if (getUsername() != null) {
            connectionOptions.setUserName(getUsername());
        }
        if (getPassword() != null) {
            connectionOptions.setPassword(getPassword().toCharArray());
        }
        
        try {
            client = new MqttClient(broker, getId(), dataStore);
            logger.logDebug(ID, "\"" + getId() + "\" for \"" + brokerUrl + "\" created");
        } catch (MqttException | IllegalArgumentException e) {
            throw new NetworkException("Creating MQTT client \"" + getId() + "\" for broker \"" + broker + "\" failed",
                    e);
        }
    }

    /**
     * Publishes the given message (payload) under the given topic to the MQTT broker defined by the parameters given to
     * this clients constructor. For this purpose, this client first connects to its MQTT broker, then publishes the
     * message, and, finally, disconnects again from the broker.<br>
     * <br>
     * <b>Note</b> that the client is not closed and, hence, can be reused for future publishing to its broker.
     * 
     * @param topic the MQTT topic under which the given message will be published; must neither be <code>null</code>
     *        nor blank
     * @param qos the quality of message delivery; must be <i>at most once</i> (0), <i>at least once</i> (1), or
     *        <i>exactly once</i> (2)
     * @param payload the message to publish
     * @throws NetworkException if connecting or disconnecting this client, or publishing the message fails
     * @see #close()
     */
    public void publish(String topic, int qos, byte[] payload) throws NetworkException {
        if (topic == null) {
            throw new NetworkException("Invalid MQTT topic: \"null\"");
        }
        if (qos < 0 || qos > 2) {
            throw new NetworkException("Invalid MQTT Quality-of-Service: " + qos);
        }
        if (payload == null) {
            throw new NetworkException("Invalid MQTT payload: \"null\"");
        }
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);        
        try {
            connect();
            client.publish(topic, message);
            logger.logDebug(ID, "\"" + getId() + "\" published to \"" + topic + "\"@\"" + broker + "\"");
        } catch (NetworkException | MqttException e) {
            throw new NetworkException("Publishing by MQTT client \"" + getId() + "\" to topic \"" + topic 
                    + "\" at broker \"" + broker + "\" failed", e);
        } finally {            
            disconnect();
        }
    }
    
    /**
     * Subscribes this client to the given topic of the MQTT broker defined by the parameters given to this clients
     * constructor. The given callback is set as the element to inform about new messages published to the subscribed
     * topic.<br>
     * <br>
     * <b>Note</b> that calling this method for an already connected client triggers its disconnection before connecting
     * and subscribing again.
     *  
     * @param topic the topic to which this client will be subscribed
     * @param qos the quality of message delivery ranging between <i>at most once</i> (0), <i>at least once</i> (1), and
     *        <i>exactly once</i> (2)
     * @param callback the instance to call back, if this client receives a new message on the subscribed topic
     * @throws NetworkException if connecting or disconnecting this client, or subscribing to the topic fails
     * @see #isConnected()
     * @see #disconnect()
     */
    public void subscribe(String topic, int qos, MqttCallback callback) throws NetworkException {
        if (topic == null) {
            throw new NetworkException("Invalid MQTT topic: \"null\"");
        }
        if (qos < 0 || qos > 2) {
            throw new NetworkException("Invalid MQTT Quality-of-Service: " + qos);
        }
        if (callback == null) {
            throw new NetworkException("Invalid MQTT callback: \"null\"");
        }
        try {
            // Restart the connection, e.g., if this method is called multiple times
            if (isConnected()) {
                disconnect();
            }
            client.setCallback(callback);
            connect();
            client.subscribe(topic, qos);
            logger.logDebug(ID, "\"" + getId() + "\" subscribed to \"" + topic + "\"@\"" + broker + "\"");
        } catch (NetworkException | MqttException e) {
            throw new NetworkException("Subscribing MQTT client \"" + getId() + "\" to topic \"" + topic 
                    + "\" at broker \"" + broker + "\" failed", e);
        }
    }
    
    /**
     * Connects this client to its MQTT broker. If this client is already connected, calling this method has no effect.
     * 
     * @throws NetworkException if connecting this client fails
     * @see #isConnected()
     */
    public void connect() throws NetworkException { //Can we make this protected void connect()
        if (!isConnected()) {
            try {
                client.connect(connectionOptions);
                setIsConnected(client.isConnected());
                logger.logDebug(ID, "\"" + getId() + "\" connected to \"" + broker + "\"");
            } catch (MqttException e) {
                throw new NetworkException("Connecting MQTT client \"" + getId() + "\" to broker \"" + broker
                        + "\" failed", e);
            }
        }
    }
    
    /**
     * Disconnects this client from its MQTT broker. If this client is already disconnected, calling this method has no
     * effect.<br>
     * <br>
     * <b>Note</b> that disconnecting a client does not include closing the client or releasing its associated
     * resources.
     * 
     * @throws NetworkException if disconnecting this client fails
     * @see #isConnected()
     * @see #close()
     */
    public void disconnect() throws NetworkException {
        if (isConnected()) {            
            try {
                client.disconnect();
                setIsConnected(client.isConnected());
                logger.logDebug(ID, "\"" + getId() + "\" disconnected from \"" + broker + "\"");
            } catch (MqttException e) {
                throw new NetworkException("Disconnecting MQTT client \"" + getId() + "\" from broker \"" + broker
                        + "\" failed", e);
            }
        }
    }
    
    /**
     * Closes this client and releases all resource associated with it. After the client has been closed it cannot be
     * reused. For instance attempts to publish or subscribe will fail. If this client is already closed, calling this
     * method has no effect.
     * 
     * @throws NetworkException if this client is already closed or closing the client fails
     * @see #isClosed()
     */
    @Override
    public void close() throws NetworkException {
        if (!isClosed()) {            
            try {
                disconnect(); // If not disconnected, close throws exception
                client.close();
                setIsClosed(true);
                logger.logDebug(ID, "\"" + getId() + "\" for \"" + broker + "\" closed");
            } catch (NetworkException | MqttException e) {
                throw new NetworkException("Closing MQTT client \"" + getId() + "\" for broker \"" + broker
                        + "\" failed", e);
            }
        }
    }

    /**
     * Returns the information about this instance to be included in the textual representation provided by
     * {@link #toString()}. This information contains:
     * <ul>
     * <li>The identifier of this instance's parent class (simple class name)</li>
     * <li>The identifier of this instance</li>
     * <li>The broker this instance connects to</li>
     * <li>The user name used by this instance for authentication, if any</li>
     * <li>The password used by this instance for authentication, if any</li>
     * <li>The connection state of this instance</li>
     * <li>The open/close state of this instance</li>
     * </ul>
     */
    @Override
    protected String[][] getElementInfo() {
        String[][] clientInfo = {
            {ID},
            {"id", getId()},
            {"broker", broker},
            {"username", getUsername()},
            {"password", getPassword()},
            {"connected", "" + isConnected()},
            {"closed", "" + isClosed()}
        };
        return clientInfo;
    }
    
}
