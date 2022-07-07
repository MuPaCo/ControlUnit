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
package net.ssehub.devopt.controllayer.model;

/**
 * This interface has to be implemented by any class that needs to be informed about and react to a new registration of
 * an external entity at this control unit.
 * 
 * @author kroeher
 *
 */
public interface ModelReceptionCallback {

    /**
     * Receives the content of a message send to the {@link ModelReceiver}. This content is interpreted as an IVML model
     * definition, which describes an external entity that wants to register at this control unit.
     *  
     * @param receivedContent the content of the registration message
     */
    public abstract void modelReceived(String receivedContent);
    
}
