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

import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * @author kroeher
 *
 */
public class ModelManager implements ModelReceptionCallback {
    
    /**
     * The identifier of this class, e.g., for logging messages during instance creation. 
     */
    private static final String ID = ModelManager.class.getSimpleName();
    
    private Logger logger = Logger.INSTANCE;
    
    private ModelReceiver modelReceiver;
    
    public ModelManager(Setup setup) throws ModelException {
        String receptionProtocol = setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_PROTOCOL);
        String receptionUrl = setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_URL);
        int receptionPort = Integer.parseInt(setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_PORT));
        String receptionChannel = setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_CHANNEL);
        
        modelReceiver = new ModelReceiver(receptionProtocol, receptionUrl, receptionPort, receptionChannel, null, null,
                this);
    }
    
    public void run() throws ModelException {
        modelReceiver.start();
    }

    @Override
    public void modelReceived(String test) {
        // TODO implement correctly
        
        logger.logInfo(ID, "Model received:", test);
    }
    
}
