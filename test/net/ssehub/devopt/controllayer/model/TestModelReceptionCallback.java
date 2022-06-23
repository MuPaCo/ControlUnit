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
package net.ssehub.devopt.controllayer.model;

/**
 * This class realizes a specific {@link ModelReceptionCallback}. An instance of this class is used to test the
 * {@link ModelReceiver} in the {@link ModelReceiverMqttTests}.
 *  
 * @author kroeher
 *
 */
public class TestModelReceptionCallback implements ModelReceptionCallback {
    
    /**
     * The definition of whether this instance was called (<code>true</code>) or not (<code>false</code>). The initial
     * and default value is <code>false</code>;
     */
    private boolean wasCalled;
    
    /**
     * The received IVML model file name. The initial and default value is <code>null</code>;
     */
    private String ivmlFileName;
    
    /**
     * The received IVML project name. The initial and default value is <code>null</code>;
     */
    private String ivmlProjectName;
    
    /**
     * Constructs a new {@link TestModelReceptionCallback} instance.<br>
     * <br>
     * This constructor call {@link #reset()} once to initialize the class attributes. 
     */
    protected TestModelReceptionCallback() {
        reset();
    }

    @Override
    public void modelReceived(String ivmlFileName, String ivmlProjectName) {
        wasCalled = true;
        this.ivmlFileName = ivmlFileName;
        this.ivmlProjectName = ivmlProjectName;
    }
    
    /**
     * Resets the attributes of this instances to their default values. These are:
     * <ul>
     * <li>The definition of whether this instance was called: <code>false</code></li>
     * <li>The received IVML model file name: <code>null</code></li>
     * <li>The received IVML project name: <code>null</code></li>
     * </ul>
     */
    public void reset() {
        wasCalled = false;
        ivmlFileName = null;
        ivmlProjectName = null;
    }
    
    /**
     * Returns the current state of this instance.
     * 
     * @return <code>true</code>, if this instance was called, or <code>false</code>, if it was not called yet or was
     *         reseted by calling {@link #reset()}
     */
    public boolean wasCalled() {
        return wasCalled;
    }
    
    /**
     * Returns the received IVML model file name.
     * 
     * @return the name of the file or <code>null</code>, if no file name was received or this instance was reseted by
     *         calling {@link #reset()}
     */
    public String getReceivedIvmlFileName() {
        return ivmlFileName;
    }
    
    /**
     * Returns the received IVML project name.
     * 
     * @return the name of the project or <code>null</code>, if no project name was received or this instance was
     *         reseted by calling {@link #reset()}
     */
    public String getReceivedIvmlProjectName() {
        return ivmlProjectName;
    }
    
}