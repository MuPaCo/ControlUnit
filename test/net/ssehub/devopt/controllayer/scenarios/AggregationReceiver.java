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
package net.ssehub.devopt.controllayer.scenarios;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import net.ssehub.devopt.controllayer.network.MqttV3Client;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class realizes a simple receiver for aggregated data produced by the controller during scenario tests. An
 * instance of this class runs in its own {@link Thread} and can connect to the respective aggregation channel to save
 * all aggregated data (received messages) in a list, which it provides for tests after execution of a scenario. 
 * 
 * @author kroeher
 *
 */
public class AggregationReceiver implements Runnable, MqttCallback {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = AggregationReceiver.class.getSimpleName();
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The client to use for receiving aggregated data.
     */
    private MqttV3Client client;
    
    /**
     * The channel (MQTT topic) on which the controller will publish its aggregated data.
     */
    private String aggregationChannel;
    
    /**
     * The {@link Thread} in which this instance is executed.
     */
    private Thread instanceThread;
    
    /**
     * The list of all messages arrived at the {@link #aggregationChannel} during the runtime of this instance.
     */
    private List<String> arrivedMessages;
    
    /**
     * Constructs a new {@link AggregationReceiver} instance.
     * 
     * @param mqttBrokerUrl the URL of the MQTT broker at which the controller publishes its aggregated data
     * @param mqttBrokerPort the port number of the MQTT broker
     * @param aggregationChannel the channel (MQTT topic) on which the controller publishes its aggregated data
     * @throws NetworkException if creating the client for receiving aggregated data fails
     */
    public AggregationReceiver(String mqttBrokerUrl, int mqttBrokerPort, String aggregationChannel)
            throws NetworkException {
        client = new MqttV3Client(ID, mqttBrokerUrl, mqttBrokerPort, null, null);
        this.aggregationChannel = aggregationChannel;
        arrivedMessages = new ArrayList<String>();
        instanceThread = null;
    }
    
    /**
     * Starts this instance in its own {@link Thread}, which in turn starts receiving aggregated data by subscribing the
     * internal client to the topic defined via the constructor of this instance.
     *  
     * @throws NetworkException if subscribing to the defined topic fails
     */
    public void start() throws NetworkException {
        instanceThread = new Thread(this, ID);
        instanceThread.start();
    }
    
    /**
     * Stops this instance from receiving aggregated data by closing the internal client. This method blocks until the
     * {@link Thread} of this instance has joined.
     *  
     * @throws NetworkException if closing the client fails
     * @throws InterruptedException if joining the thread fails
     */
    public void stop() throws NetworkException, InterruptedException {
        client.close();
        instanceThread.join();
    }
    
    /**
     * Returns the list of all messages arrived at the aggregation channel during the runtime of this instance.
     * 
     * @return the list of messages; never <code>null</code>, but may be <i>empty</i>
     */
    public List<String> getReceivedMessages() {
        return arrivedMessages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connectionLost(Throwable cause) {
        logger.logError(ID, "Connection lost", cause.getMessage());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) { }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String messageString = new String(message.getPayload());
        arrivedMessages.add(messageString);
        logger.logInfo(ID, "Message arrived", "Topic: " + topic, "Message: " + messageString,
                "Total message count: " + arrivedMessages.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            client.subscribe(aggregationChannel, 2, this);
        } catch (NetworkException e) {
            logger.logException(ID, e);
        }
    }
    
}
