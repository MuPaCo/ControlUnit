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

import java.util.HashMap;
import java.util.Set;

import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class realizes an IVML model management.
 * 
 * @author kroeher
 *
 */
public class ModelManager implements ModelReceptionCallback {
    
    /**
     * The identifier of this class, e.g., for logging messages during instance creation. 
     */
    private static final String ID = ModelManager.class.getSimpleName();
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The {@link ModelReceiver} which manages the network connection and the incoming requests via that connection for
     * registration. It calls this instance back, if the addition of a new IVML model describing an external element
     * and, hence, its registration was successful. 
     */
    private ModelReceiver modelReceiver;
    
    /**
     * The mapping between the IVML project name (key) and the IVML model file name (value) in which this project is
     * defined. Note that the file name does not include the <code>.ivml</code>extension.
     */
    private HashMap<String, String> ivmlProjectFileMap;
    
    /**
     * Constructs a new {@link ModelManager} instance for managing the IVML models of registered external elements.
     *  
     * @param setup the {@link Setup} of the entire control node for configuring the {@link ModelReceiver}
     * @throws ModelException if creating the instance fails
     */
    public ModelManager(Setup setup) throws ModelException {
        String receptionProtocol = setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_PROTOCOL);
        String receptionUrl = setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_URL);
        int receptionPort = Integer.parseInt(setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_PORT));
        String receptionChannel = setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_CHANNEL);
        modelReceiver = new ModelReceiver(receptionProtocol, receptionUrl, receptionPort, receptionChannel, null, null,
                this);
        ivmlProjectFileMap = new HashMap<String, String>();
    }
    
    /**
     * Starts the internal {@link ModelReceiver}.
     * 
     * @throws ModelException if activating the network connection of the model receiver fails 
     */
    public void run() throws ModelException {
        modelReceiver.start();
    }

    @Override
    public void modelReceived(String ivmlFileName, String ivmlProjectName) {
        logger.logInfo(ID, "New IVML project \"" + ivmlProjectName + "\" received", "Source IVML file: \""
                + ivmlFileName + "\"");
        ivmlProjectFileMap.put(ivmlProjectName, ivmlFileName);
        logMapping();
    }
    
    /**
     * Logs the current entries in the {@link #ivmlProjectFileMap} via the {@link #logger} as generla information.
     */
    private void logMapping() {
        Set<String> mappingKeySet = ivmlProjectFileMap.keySet();
        String[] mappingLogLines = new String[mappingKeySet.size() + 1];
        mappingLogLines[0] = "Available IVML projects:";
        int mappingLogLinesCounter = 1;
        for (String mappingKey : mappingKeySet) {
            mappingLogLines[mappingLogLinesCounter] = mappingKey + "[" + ivmlProjectFileMap.get(mappingKey) + ".ivml]";
            mappingLogLinesCounter++;
        }
        logger.logInfo(ID, mappingLogLines);
    }
    
}
