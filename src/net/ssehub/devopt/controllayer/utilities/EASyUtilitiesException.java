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
package net.ssehub.devopt.controllayer.utilities;

/**
 * This class represents the specific type of exception, which will be thrown by the {@link EASyUtilities}, if their
 * execution fails.
 * 
 * @author kroeher
 *
 */
public class EASyUtilitiesException extends Exception {

    /**
     * The serial version UID of this class required by the extended {@link Exception}.
     */
    private static final long serialVersionUID = 8593279287326733721L;

    /**
     * Constructs a new {@link EASyUtilitiesException} instance with the given message.
     * 
     * @param message the description of the problem causing this exception
     */
    public EASyUtilitiesException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link EASyUtilitiesException} instance with the given message and the underlying cause of this
     * exception.
     * 
     * @param message the description of the problem causing this exception
     * @param cause the exception causing this exception
     */
    public EASyUtilitiesException(String message, Throwable cause) {
        super(message, cause);
    }

}
