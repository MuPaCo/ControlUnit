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

/**
 * This class represents a complex data structure to consistently manage all information of a component relevant for its
 * correct monitoring by the {@link MonitoringDataReceiver}. The current assumption is that monitoring is realized via
 * MQTT only. Hence, this class focuses on MQTT-specific parameters. However, in future, this class may be extended to
 * support other protocol information, like HTTP.
 * 
 * @author kroeher
 *
 */
public class ObservableInfo {

    /**
     * The (symbolic) name of the component represented by the information of this instance.
     */
    private String name;
    
    /**
     * The URL (IP) of the MQTT broker to which the component represented by the information of this instance sends its
     * monitoring data.
     */
    private String url;
    
    /**
     * The port number of the MQTT broker to which the component represented by the information of this instance sends
     * its monitoring data (see {@link #channel} for the specific MQTT topic).
     */
    private int port;
    
    /**
     * The name of the MQTT topic available at the MQTT broker defined by the {@link #url} and {@link #port}.
     * This is the topic to which the component represented by the information of this instance sends its
     * monitoring data.
     */
    private String channel;
    
    /**
     * Constructs a new {@link ObservableInfo} instance containing the relevant information of a component for its
     * monitoring by the {@link MonitoringDataReceiver}.
     * 
     * @param name the (symbolic) name of the component represented by the information of this instance; must not be
     *        <code>null</code>
     * @param url the URL (IP) of the MQTT broker to which the component represented by the information of this instance
     *        sends its monitoring data; must not be <code>null</code> and a valid URL
     * @param port the port number of the MQTT broker to which the component represented by the information of this
     *        instance sends its monitoring data; must be between 0 and 65535
     * @param channel the name of the MQTT topic to which the component represented by the information of this instance
     *        sends its monitoring data; must not be <code>null</code> nor <i>empty</i> 
     */
    public ObservableInfo(String name, String url, int port, String channel) {
        this.name = name;
        this.url = url;
        this.port = port;
        this.channel = channel;
    }
    
    // TODO write correct JavaDoc
    public String getName() {
        return name;
    }
    
    // TODO write correct JavaDoc
    public String getUrl() {
        return url;
    }
    
    // TODO write correct JavaDoc
    public int getPort() {
        return port;
    }
    
    // TODO write correct JavaDoc
    public String getChannel() {
        return channel;
    }
    
    @Override
    public String toString() {
        // TODO return a proper textual representation of an instance of this class
        return "";
    }
    
}
