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

import java.util.LinkedList;
import java.util.List;

/**
 * This class realizes a generic queue with <code>synchronized</code> access. Hence, a single instance of this class can
 * be accessed by multiple threads safely.
 * 
 * @author kroeher
 *
 * @param <T> the type of the elements this queue stores
 */
public class GenericQueue<T> {

    /**
     * This enumeration defines the different states of a {@link GenericQueue} instance.
     * 
     * @author kroeher
     *
     */
    public enum QueueState {
        /**
         * Indicates that the queue is ready for use, but not yet {@link QueueState#OPEN}. Further, a ready queue is
         * always empty. 
         */
        INIT,
        /**
         * Indicates that the queue is open, which allows adding and removing messages.
         */
        OPEN,
        /**
         * Indicates that the queue is closed, which rejects any addition or removal of messages. Further, a closed
         * queue is always empty.
         */
        CLOSED
    };
    
    /**
     * The maximum number of elements this instance manages simultaneously. This value limits the addition of elements,
     * e.g., to prevent memory problems. Hence, further calls to {@link #addElement(Object)} block the caller until
     * stored elements are removed again.
     */
    private int maxQueueElements;

    /**
     * The current {@link QueueState} of this instance. 
     */
    private QueueState state;
    
    /**
     * Defines whether this instance will be closed, if all elements are removed (<code>true</code>) or not
     * (<code>false</code>). This flag is needed in case this instance should close, while it still contains elements.
     * This instance is then waiting for removal of the remaining elements, but blocks the addition of new ones, before
     * it finally closes completely.
     */
    private boolean prepareClose;
    
    /**
     * The list of elements this instance holds currently.
     */
    private List<T> elements;
    
    /**
     * Constructs a new {@link GenericQueue} instance.
     * 
     * @param maxElements the maximum number of elements this instance manages simultaneously; must be a positive,
     *        non-zero integer value
     * @throws NumberFormatException if the given maximum number of elements is less than or equal to zero
     *         (<code>maxElements &lt= 0</code>) 
     */
    public GenericQueue(int maxElements) throws NumberFormatException {
        if (maxElements <= 0) {
            throw new NumberFormatException("Maximum number of generic queue elements must be greater than zero");
        }
        state = QueueState.INIT;
        maxQueueElements = maxElements;
        prepareClose = false;
        elements = new LinkedList<T>();
    }
    
    /**
     * Returns whether this instance is {@link QueueState#OPEN}.
     * 
     * @return <code>true</code>, if the current state of this instance is {@link QueueState#OPEN}; <code>false</code>
     *         otherwise
     */
    public synchronized boolean isOpen() {
        return state == QueueState.OPEN;
    }
    
    /**
     * Sets the state of this instance to the given new state.<br>
     * <br>
     * Note that setting the state to {@link QueueState#CLOSED} may not become effective immediately. In case that this
     * instance still contains messages, this new state is set effectively after the remaining messages are removed. In
     * the meantime, the addition of new messages to this instance is rejected.
     * 
     * @param newState the new state for this instance
     */
    public synchronized void setState(QueueState newState) {
        if (newState == QueueState.CLOSED) {
            if (elements.isEmpty()) {
                // If there are no more elements in this queue, close it immediately 
                state = newState;
            } else {
                // If there are elements in this queue, wait until they are removed, but block further additions
                prepareClose = true;
            }
        } else {
            state = newState;
        }
    }

    /**
     * Removes the first element from this instance and returns it.
     * 
     * @return the first element or <code>null</code>, if this instance is not in state {@link QueueState#OPEN}, it does
     *         does not contain any elements currently, or the element itself is <code>null</code>
     * @see #isOpen()
     */
    public synchronized T getElement() {
        T element  = null;
        if (state == QueueState.OPEN && !elements.isEmpty()) {
            element = elements.remove(0);
            if (elements.isEmpty() && prepareClose) {
                setState(QueueState.CLOSED);
            }
        }
        return element;
    }

    /**
     * Adds the given element to this instance.
     * 
     * @param element the element to add to this instance
     * @return <code>true</code>, if adding the element was successful; <code>false</code> otherwise, which is the case,
     *         if this instance is not in state {@link QueueState#OPEN}, closing this instance was already initiated, or
     *         the maximum number of elements this instance manages simultaneously is reached 
     */
    public synchronized boolean addElement(T element) {
        boolean additionSuccessful = false;
        if (state == QueueState.OPEN && !prepareClose && elements.size() < maxQueueElements) {
            additionSuccessful = elements.add(element);
        }
        return additionSuccessful;
    }
    
}
