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
 * This class represents a monitoring data structure. This structure consists of three attributes:
 * <ul>
 * <li>The <i>channel</i> on which the monitoring data was received</li>
 * <li>The monitoring <i>data</i> received on that channel</li>
 * <li>The <i>time</i> (as provided by {@link System#currentTimeMillis()}) at which the monitoring data was
 *     received</li>
 * </ul> 
 * @author kroeher
 *
 */
public class MonitoringData {
    
    /**
     * The channel on which the {@link #data} was received.
     */
    private String channel;
    
    /**
     * The monitoring data received on the {@link #channel}.
     */
    private String data;
    
    /**
     * The time as provided by {@link System#currentTimeMillis()} at which the {@link #data} was received.
     */
    private long time;

    /**
     * Constructs a new {@link MonitoringData} instance. This construction also sets the internal time accessible via
     * {@link #getTime()}.
     * 
     * @param channel the channel (MQTT topic, HTTP server context) on which the data was received that this instance
     *        will represent
     * @param data the monitoring data, which this instance will represent
     * @param time the time as provided by {@link System#currentTimeMillis()} at which the monitoring data was received
     *        that this instance will represent
     */
    public MonitoringData(String channel, String data, long time) {
        this.channel = channel;
        this.data = data;
        this.time = time;
    }
    
    /**
     * Returns the channel (MQTT topic, HTTP server context) on which the data was received that this instance
     * represents.
     * 
     * @return the monitoring data reception channel
     */
    public String getChannel() {
        return channel;
    }
    
    /**
     * Returns the actual monitoring data that this instance represents.
     * 
     * @return the monitoring data
     */
    public String getData() {
        return data;
    }
    
    /**
     * Returns the time as provided by {@link System#currentTimeMillis()} at which the monitoring data was received that
     * this instance represents.
     * 
     * @return the monitoring data reception time
     */
    public long getTime() {
        return time;
    }
    
}
