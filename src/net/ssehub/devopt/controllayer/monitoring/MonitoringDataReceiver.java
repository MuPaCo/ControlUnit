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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.ssehub.devopt.controllayer.model.EntityInfo;
import net.ssehub.devopt.controllayer.model.ModelManager;

/**
 * This class realizes the singleton receiver for monitoring/runtime data from individual entities of the local layer.
 * In order to monitor these entities, a valid model describing them and their properties must be available to the
 * {@link ModelManager}, which includes the necessary {@link EntityInfo}. Hence, adding new or removing existing
 * observables (local entities) is done by the {@link ModelManager}, while individual instances can register as
 * {@link MonitoringDataReceptionCallback} to receive all monitoring data.
 * 
 * @author kroeher
 *
 */
public class MonitoringDataReceiver {
    
    /**
     * The singleton instance of this class.
     */
    public static final MonitoringDataReceiver INSTANCE = new MonitoringDataReceiver();
    
    /**
     * The identifier of this class, e.g. for printing messages.
     */
    private static final String ID = MonitoringDataReceiver.class.getSimpleName();
    
    private List<MonitoringDataReceptionCallback> callbacks;
    
    /*
     * TODO Define and use a data structure that enables decoupling the reception of monitoring data from observables
     * from propagating such data to callbacks. Otherwise propagation will block further reception of new data.
     * One option is the use of a queue.
     * Keep in mind that such asynchronous access (add new data while others read older data) needs synchronization.
     */
    
    /**
     * Constructs a new {@link MonitoringDataReceiver} instance.
     */
    private MonitoringDataReceiver() {
        callbacks = new ArrayList<MonitoringDataReceptionCallback>();
    }
    
    /**
     * Adds the given callback to the list of all callbacks of this receiver. Elements in this list will be informed
     * about any runtime data received from all observables managed by this receiver.
     * 
     * @param callback the callback to add to the list all callbacks
     * @return <code>true</code> (as specified by {@link Collection#add(Object)})
     */
    public boolean addCallback(MonitoringDataReceptionCallback callback) {
        return callbacks.add(callback);
    }
    
    /**
     * Removes the given callback from the list of all callbacks of this receiver. Hence, the given callback will not be
     * called anymore about runtime data received from observables managed by this receiver.
     * 
     * @param callback the callback to remove from the list all callbacks
     * @return <code>true</code>, if the list of callbacks contained the specified element; <code>false</code> otherwise
     */
    public boolean removeCallback(MonitoringDataReceptionCallback callback) {
        return callbacks.remove(callback);
    }
    
    // TODO write correct JavaDoc
    public boolean addObservable(EntityInfo observable) {
        /*
         * TODO Start monitoring the component represented by the information in the given ObservableInfo instance.
         * This includes:
         *     - Creating a new network connection to listen to runtime data produced by the component
         *     - Adding this network connection to the general listening loop of all active monitoring connections of
         *       this class
         *     - Manage this network connection until it is actively removed by calling removeObservable below
         * 
         * Return true, if starting monitoring was successful; return false otherwise
         */
        return true;
    }
    
    // TODO write correct JavaDoc
    public boolean removeObservable(EntityInfo observable) {
        /*
         * TODO Stop monitoring the component represented by the information in the given ObservableInfo instance.
         * This includes:
         *     - Removing the network connection created for the component from the general listening loop of all active
         *       monitoring connections of this class
         *     - Close the network connection created for the component (release the object in the end)
         * 
         * Return true, if stopping monitoring was successful; return false otherwise
         */
        return true;
    }
    
    /**
     * Sends the given source and its monitoring data to all {@link #callbacks}.
     * 
     * @param source the information about the component that created the monitoring data 
     * @param data the monitoring data of the source
     */
    private void propagateMonitoringData(EntityInfo source, String data) {
        for (MonitoringDataReceptionCallback callback : callbacks) {
            callback.monitoringDataReceived(source, data);
        }
    }
    
}
