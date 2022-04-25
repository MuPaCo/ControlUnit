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
 * This interface needs to be implemented by any class that needs to be informed about new monitoring data from any of
 * the registered and observed external element.
 * 
 * @author kroeher
 *
 */
public interface MonitoringDataReceptionCallback {
    
    /**
     * Receives the information about source and its (latest) runtime data as received by the
     * {@link MonitoringDataReceiver}.
     *  
     * @param source the information about the component that created the received runtime data 
     * @param data the (latest) runtime data received by the source
     */
    public abstract void monitoringDataReceived(ObservableInfo source, String data);
    
}
