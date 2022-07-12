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

import net.ssehub.devopt.controllayer.model.EntityInfo;

/**
 * This interface needs to be implemented by any class that needs to be informed about new monitoring data from any of
 * the registered and observed entities.
 * 
 * @author kroeher
 *
 */
public interface MonitoringDataReceptionCallback {
    
    /**
     * Receives the data from the given channel. The subsequent processing of this data depends on the specific
     * receiver.
     * 
     * @param channel the channel on which the monitoring data was received; this is the MQTT topic name or HTTP server
     *        context name of the entity's monitoring scope as defined by its {@link EntityInfo} instance; must not be
     *        <code>null</code> nor <i>blank</i>
     * @param data the data received via the channel; must not be <code>null</code> nor <i>blank</i> 
     */
    public abstract void monitoringDataReceived(String channel, String data);
    
}
