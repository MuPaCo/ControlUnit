/*
 * Copyright 2023 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.devopt.controllayer.analysis;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an abstract control action type as a way top interact with a distributed controlled emergent
 * system and its entities.
 * 
 * @author kroeher
 *
 */
public abstract class ControlActionType {

    private List<ControlActionType> controlActions;
    
    /**
     * Creates a new {@link ControlActionType} instance.
     */
    protected ControlActionType() {
        controlActions = new ArrayList<>();
    }
    
    /**
     * Adds the given {@link ControlActionType} instance at the end of this instances' list of actions. Actions in that
     * list will be executed in the sequence they were added unless they are removed again.
     * 
     * @param action the instance to add to this (complex) {@link ControlActionType}
     * @return <code>true</code>, if the addition was successful; <code>false</code>, if the given action is
     *         <code>null</code> or the addition failed for other reasons
     */
    protected boolean addAction(ControlActionType action) {
        boolean additionSuccessful = false;
        if (action != null) {
            additionSuccessful = controlActions.add(action);
        }
        return additionSuccessful;
    }
    
    /**
     * Removes the given {@link ControlActionType} instance at from the this instances' list of actions. Actions in that
     * list will be executed in the sequence they were added unless they are removed again.
     * 
     * @param action the instance to remove from this (complex) {@link ControlActionType}
     * @return <code>true</code>, if the removal was successful; <code>false</code>, if the given action is
     *         <code>null</code> or the removal failed for other reasons
     */
    protected boolean removeAction(ControlActionType action) {
        boolean removalSuccessful = false;
        if (action != null) {
            removalSuccessful = controlActions.remove(action);
        }
        return removalSuccessful;
    }
    
    /**
     * Executes this {@link ControlActionType} instance. This execution first calls {@link #executeSelf()} before all
     * instances in the internal list of instances are called.
     *  
     * @return <code>true</code>, if execution was successful; <code>false</code> otherwise
     */
    public boolean execute() {
        boolean executionSuccesful = false;
        if (executeSelf()) {
            if (!controlActions.isEmpty()) {
                int controlActionCounter = 0;
                boolean subActionFailed = false;
                while (!subActionFailed && controlActionCounter < controlActions.size()) {
                    subActionFailed = !controlActions.get(controlActionCounter).execute();
                    controlActionCounter++;
                }
                executionSuccesful = !subActionFailed;
            } else {
                executionSuccesful = true;
            }
        }
        return executionSuccesful;
    }
    
    /**
     * Executes this {@link ControlActionType} instance. This execution only calls the sole action of this instance
     * without its inherent sub-actions.
     *  
     * @return <code>true</code>, if execution was successful; <code>false</code> otherwise
     */
    public abstract boolean executeSelf();
    
}
