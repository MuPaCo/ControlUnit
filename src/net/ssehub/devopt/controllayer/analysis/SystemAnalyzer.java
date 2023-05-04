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

import net.ssehub.devopt.controllayer.model.EntityInfo;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class realizes the analysis features to detect problems, select the appropriate {@link ControlActionType} and,
 * if required, the target entity to apply that action to.
 * 
 * @author kroeher
 *
 */
public class SystemAnalyzer {
    
    /**
     * The identifier of this class, e.g., for logging messages during instance creation. 
     */
    private static final String ID = SystemAnalyzer.class.getSimpleName();
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;

    /**
     * Constructs a new {@link SystemAnalyzer} instance.
     */
    public SystemAnalyzer() {
        analyze();
    }

    /**
     * Starts this instance in its separate thread.
     */
    public void start() {
        // TODO start this instance in a separate thread
        logger.logInfo(ID, "Starting instance (thread)");
    }
    
    /**
     * Stops this instance and its separate thread.
     */
    public void stop() {
        // TODO stop this instance and its separate thread
        logger.logInfo(ID, "Stopping instance (thread)");
    }
    
    /**
     * Analyzes the system to to create a {@link SystemState} instance describing the current state of the system.
     * This instance is the basis for determining appropriate CATs and potential target entities.
     */
    private void analyze() {
        // TODO analyze system to create a SystemState for determining appropriate CATs and potential target entities
        SystemState currentState = new SystemState();
        EntityInfo targetEntity = getEntity(currentState);
        ControlActionType cat = getAction(targetEntity, currentState);
        if (cat != null) {
            cat.execute();
        }
    }

    /**
     * Determine the target entity to correct, if the given system state requires its correction.
     * 
     * @param state the current {@link SystemState}
     * @return the {@link EntityInfo} describing the target entity to correct or <code>null</code>, if no such entity
     *         can be determined
     */
    private EntityInfo getEntity(SystemState state) {
        // TODO determine target entity for correction from analyzed situation
        return null;
    }
    
    /**
     * Determines the appropriate control action type to execute to react to the given system state. The target entity
     * is optional, if there cannot be determined a single target for improving the current system state.
     *  
     * @param targetEntity the <i>optional</i> {@link EntityInfo} instance describing the target entity to correct or
     *        <code>null</code>, if no such entity can be determined
     * @param state the current {@link SystemState}
     * @return the {@link ControlActionType} instance to execute to correct or improve the current system state or
     *         <code>null</code>, if no action is required
     */
    private ControlActionType getAction(EntityInfo targetEntity, SystemState state) {
        // TODO determine appropriate CAT for target entity and analyzed situation
        return null;
    }

}
