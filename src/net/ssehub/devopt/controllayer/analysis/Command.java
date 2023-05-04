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

import java.net.http.HttpResponse;

import net.ssehub.devopt.controllayer.model.EntityInfo;
import net.ssehub.devopt.controllayer.network.HttpClient;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.update.UpdateException;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class realized the control action type "command".
 * 
 * @author kroeher
 *
 */
public class Command extends ControlActionType {
    
    /**
     * The identifier of this class, e.g., for logging messages during instance creation. 
     */
    private static final String ID = Command.class.getSimpleName();
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The command to send to the {@link #targetEntity}.
     */
    private String command;
    
    /**
     * The target entity to send the {@link #command} to.
     */
    private EntityInfo targetEntity;

    /**
     * Constructs a new {@link Command} instance.
     * 
     * @param command the command to send to the given target entity
     * @param targetEntity the target entity to send the given command to
     */
    public Command(String command, EntityInfo targetEntity) {
        this.command = command;
        this.targetEntity = targetEntity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeSelf() {
        boolean executionSuccessful = false;
        if (command != null && !command.isBlank() && targetEntity != null) {
            try {
                HttpClient commandClient = new HttpClient("CommandClient", null, null, null, -1, null, null);
                HttpResponse<String> commandResponse = commandClient.sendPostSync(targetEntity.getHost(), null,
                        command, 1000);
                if (commandResponse != null) {
                    logger.logInfo(ID, "Command response of \"" + targetEntity.getIdentifier() + "\": "
                            + commandResponse.body());
                }
            } catch (NetworkException e) {
                logger.logException(ID, new UpdateException("Sending command to \"" + targetEntity.getIdentifier()
                        + "\" failed", e));
            }
            
        }
        return executionSuccessful;
    }

}
