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

import net.ssehub.devopt.controllayer.utilities.GenericCallback;

/**
 * This class implements the {@link GenericCallback} interface with {@link MonitoringData} as specific type to act as a
 * monitoring data callback, which provides access to received monitoring data. Hence, instances of this class will be
 * used as callbacks for tests requiring them.
 * 
 * @author kroeher
 *
 */
public class TestMonitoringDataReceptionCallback implements GenericCallback<MonitoringData> {
    
    /**
     * The name of this instance.
     */
    private String name;
    
    /**
     * The channel on which the data was received.
     */
    private String channel;
    
    /**
     * The received data.
     */
    private String data;
    
    /**
     * Constructs a new {@link TestMonitoringDataReceptionCallback} instance.
     * 
     * @param name the name of this instance used as return value of {@link #toString()}; must not be <code>null</code>
     */
    public TestMonitoringDataReceptionCallback(String name) {
        this.name = name;
        reset();
    }
    
    /**
     * Sets the channel and the data to <code>null</code>.
     */
    public void reset() {
        channel = null;
        data = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inform(MonitoringData data) {
        this.channel = data.getChannel();
        this.data = data.getData();
    }
    
    /**
     * Returns the channel as received via the latest {@link #inform(MonitoringData)} call.
     * 
     * @return the channel or <code>null</code>, if no such information was received so far
     */
    public String getChannel() {
        return channel;
    }
    
    /**
     * Returns the data as received via the latest {@link #inform(MonitoringData)} call.
     * 
     * @return the data or <code>null</code>, if no such information was received so far
     */
    public String getData() {
        return data;
    }
    
    /**
     * Returns the name of this instance given as constructor parameter.
     * 
     * @return the name of this instance
     */
    @Override
    public String toString() {
        return name; 
    }

}
