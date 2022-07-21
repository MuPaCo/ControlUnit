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
 * This class realizes a package-specific exception type, which is thrown by the classes of this package in case of
 * errors related to the monitoring features.
 * 
 * @author kroeher
 *
 */
public class MonitoringException extends Exception {

    /**
     * The generated serial version UID of this exception type.
     */
    private static final long serialVersionUID = 6381335702954711734L;

    /**
     * Constructs a new {@link MonitoringException} instance with the given message.
     * 
     * @param message the error message of this exception
     */
    public MonitoringException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link MonitoringException} instance with the given message and the underlying cause of this
     * exception.
     * 
     * @param message the error message of this exception
     * @param cause the underlying cause of this exception
     */
    public MonitoringException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
