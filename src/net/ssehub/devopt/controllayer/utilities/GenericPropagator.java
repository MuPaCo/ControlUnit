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
package net.ssehub.devopt.controllayer.utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * This class realizes a parallelized and generic propagation mechanism. An instance of this class is a
 * {@link Runnable}, which constantly checks a given {@link GenericQueue} for available elements. If an element is
 * available, it removes it from the queue and informs each of the {@link GenericCallback} instances about it.
 * 
 * @author kroeher
 *
 * @param <T> the type of the elements this mechanism propagates
 */
public class GenericPropagator<T> implements Runnable {
    
    /**
     * The {@link GenericQueue} to check for available elements. 
     */
    private GenericQueue<T> queue;
    
    /**
     * The list of {@link GenericCallback} instance to inform about elements in the {@link #queue}.
     */
    private List<GenericCallback<T>> callbacks;
    
    /**
     * Constructs a new {@link GenericPropagator} instance.
     * 
     * @param queue the queue to check for available elements
     * @throws if the given queue is <code>null</code>
     */
    public GenericPropagator(GenericQueue<T> queue) throws NullPointerException {
        if (queue == null) {
            throw new NullPointerException("Generic queue is not defined");
        }
        this.queue = queue;
        callbacks = new ArrayList<GenericCallback<T>>();
    }
    
    /**
     * Adds the given callback to the list of callbacks to inform about elements in the queue, which this instance
     * received during construction.
     * 
     * @param callback the callback to add
     * @return <code>true</code>, if adding the callback was successful; <code>false</code> otherwise
     */
    public synchronized boolean addCallback(GenericCallback<T> callback) {
        boolean callbackAdded = false;
        if (callback != null && !callbacks.contains(callback)) {
            callbackAdded = callbacks.add(callback);
        }
        return callbackAdded;
    }
    
    /**
     * Removes the given callback from the list of callbacks to inform about elements in the queue, which this instance
     * received during construction.
     * 
     * @param callback the callback to remove
     * @return <code>true</code>, if removing the callback was successful; <code>false</code> otherwise
     */
    public synchronized boolean removeCallback(GenericCallback<T> callback) {
        boolean callbackRemoved = false;
        if (callback != null && callbacks.contains(callback)) {
            callbackRemoved = callbacks.remove(callback);
        }
        return callbackRemoved;
    }

    /**
     * Starts constantly checking the internal queue for elements to inform the callbacks about. This method cannot be
     * stopped directly, but requires the queue to be closed instead.
     */
    @Override
    public void run() {
        T message;
        while (queue.isOpen()) {
            message = queue.getElement();
            if (message != null) {
                for (GenericCallback<T> callback : callbacks) {                    
                    callback.inform(message);
                }
            }
        }
    }

}
