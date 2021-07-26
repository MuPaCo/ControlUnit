/*
 * Copyright 2021 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.devopt.controllayer;

/**
 * This class realizes a task-specific exception type, which is thrown by the {@link Setup} class in case of errors
 * related to creating the internal setup of this tool.
 * 
 * @author kroeher
 *
 */
public class SetupException extends Exception {

    /**
     * The generated serial version UID of this exception type.
     */
    private static final long serialVersionUID = -1567704534435828453L;
    
    /**
     * Constructs a new {@link SetupException} instance with the given message.
     * 
     * @param message the error message of this exception
     */
    public SetupException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link SetupException} instance with the given message and the underlying cause of this
     * exception.
     * 
     * @param message the error message of this exception
     * @param cause the underlying cause of this exception
     */
    public SetupException(String message, Throwable cause) {
        super(message, cause);
    }

}
