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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.utilities.EASyUtilities;
import net.ssehub.devopt.controllayer.utilities.EASyUtilitiesException;
import net.ssehub.devopt.controllayer.utilities.FileUtilities;
import net.ssehub.devopt.controllayer.utilities.FileUtilities.WriteOption;
import net.ssehub.devopt.controllayer.utilities.FileUtilitiesException;
import net.ssehub.devopt.controllayer.utilities.Logger;
import net.ssehub.easy.varModel.confModel.Configuration;

/**
 * This class realizes the internal model management.
 * 
 * @author kroeher
 *
 */
public class ModelManager implements ModelReceptionCallback {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = ModelManager.class.getSimpleName();
    
    /**
     * The singleton instance of this class.
     */
    private static ModelManager instance;
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The local reference to the global {@link EASyUtilities}.
     */
    private EASyUtilities easyUtilities = EASyUtilities.INSTANCE;
    
    /**
     * The local reference to the global {@link FileUtilities}.
     */
    private FileUtilities fileUtilities = FileUtilities.INSTANCE;
    
    /**
     * The {@link ModelReceiver} which manages the network connection and the incoming requests via that connection for
     * registration. It calls this instance back, if a new message arrives.
     */
    private ModelReceiver modelReceiver;
    
    /**
     * The model directory as defined by the {@link Setup} instance provided for {@link #setUp(Setup)}. This directory
     * may already contain models to be loaded during {@link #setUp(Setup)} and will be used to store new models, if
     * the arrive at the {@link #modelReceiver}.
     */
    private File modelDirectory;
    
    /**
     * The list of all {@link EntityInfo} instances loaded from the received models in the {@link #modelDirectory}.
     */
    private List<EntityInfo> entityInformation;
    /*
     * TODO Can we shutdown EASy and release IVML models/resources again after model addition?
     * This manager and, hence, the remaining controller components withh use EntityInfo instances internally.
     */
    
    /**
     * Constructs a new {@link ModelManager} instance.
     */
    private ModelManager() {
        entityInformation = new ArrayList<EntityInfo>();        
    }
    
    /**
     * Returns the singleton instance of this class. <b>Note</b> that it is required to call {@link #setUp(Setup)}
     * exactly once before calling this method successfully.
     * 
     * @return the singleton instance of this class, or <code>null</code>, if it was not set up yet
     */
    public static ModelManager getInstance() {
        return instance;
    }
    
    /**
     * Creates the singleton instance of this class based on the given setup.
     * 
     * @param setup the configuration properties to use for setting up the singleton instance of this class
     * @throws ModelException if setting up the instance failed or the instance is already set up
     */
    public static void setUp(Setup setup) throws ModelException {
        if (instance == null) {
            if (setup != null && instance == null) {
                instance = new ModelManager();
                instance.loadAvailableModels(setup);
                instance.createModelReceiver(setup, instance);
            } else {
                throw new ModelException("Setup is \"null\"");
            }
        } else {
            throw new ModelException("Model manager instance already set up");
        }
    }
    
    /**
     * Sets the internal {@link #modelDirectory} in accordance to the corresponding configuration property defined in
     * the given setup before loading all IVML models from the <code>*.ivml</code> files in that directory. This loading
     * results in the addition of the respective {@link EntityInfo} instances to the {@link #entityInformation}.
     *   
     * @param setup the setup containing the configuration property defining the model directory to use
     * @throws ModelException if loading the available models from the model directory fails
     */
    private void loadAvailableModels(Setup setup) throws ModelException {
        String modelDirectoryPath = setup.getModelConfiguration(Setup.KEY_MODEL_DIRECTORY);
        if (modelDirectoryPath != null && !modelDirectoryPath.isBlank()) {
            modelDirectory = new File(modelDirectoryPath);
            try {
                if (easyUtilities.addModelLocation(modelDirectory)) {
                    File[] availableModelFiles = modelDirectory.listFiles(new FilenameFilter() {
                        
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".ivml");
                        }
                        
                    });
                    if (availableModelFiles != null) {
                        for (int i = 0; i < availableModelFiles.length; i++) {
                            if (!addModelInformation(availableModelFiles[i])) {
                                logger.logWarning(ID, "Adding model information from \"" + availableModelFiles[i]
                                        + "\" not successful");
                            }
                        }
                    } else {
                        logger.logWarning(ID, "Retrieving *.ivml files from \"" + modelDirectory
                                + "\" not possible as it does not denote a directory, or an I/O error occured");
                    }
                } else {
                    logger.logWarning(ID, "Adding directory \"" + modelDirectory
                            + "\" as model location was not successful");
                }
            } catch (EASyUtilitiesException e) {
                throw new ModelException("Adding directory \"" + modelDirectory + "\" as model location failed", e);
            }
        } else {
            throw new ModelException("Setup does not provide a model directory");
        }
    }
    
    /**
     * Adds the {@link EntityInfo} defined by the {@link Configuration} of the model in the given file to the
     * {@link #entityInformation}.
     * 
     * @param ivmlModelFile the IVML model file for which the {@link Configuration} will be retrieved from the
     *        {@link #easyUtilities} to create the corresponding {@link EntityInfo} to add
     * @return <code>true</code>, if the addition was successful; <code>false</code> otherwise
     */
    private boolean addModelInformation(File ivmlModelFile) {
        boolean modelInformationAdded = false;
        try {
            Configuration modelConfiguration = easyUtilities.loadConfiguration(ivmlModelFile);
            if (easyUtilities.isValid(modelConfiguration)) {
                EntityInfo entityInfo = new EntityInfo(modelConfiguration, ivmlModelFile.getAbsolutePath());
                if (!isEntityInfoKnown(entityInfo)) {                    
                    modelInformationAdded = entityInformation.add(entityInfo);
                } else {
                    logger.logWarning(ID, "Entity information already known", "No addition of: "
                            + entityInfo.toString());
                }
            } else {
                logger.logWarning(ID, "Model information addition aborted", "Model configuration from \""
                        + ivmlModelFile + "\" is not valid");
            }
        } catch (EASyUtilitiesException | ModelException e) {
            logger.logException(ID, e);
        }
        return modelInformationAdded;
    }
    
    /**
     * Checks whether the given {@link EntityInfo} is equal to one of the instances in the {@link #entityInformation}.
     *   
     * @param entityInfo the instance to check equality for
     * @return <code>true</code>, if the first equal instance in the {@link #entityInformation} is found;
     *         <code>false</code> otherwise
     */
    private boolean isEntityInfoKnown(EntityInfo entityInfo) {
        boolean entityInfoKnown = false;
        if (entityInfo != null) {
            int entityInformationCounter = 0;
            while (!entityInfoKnown && entityInformationCounter < entityInformation.size()) {
                if (entityInfo.equals(entityInformation.get(entityInformationCounter))) {
                    entityInfoKnown = true;
                }
                entityInformationCounter++;
            }            
        }
        return entityInfoKnown;
    }
    
    /**
     * Creates the {@link #modelReceiver} in accordance to the corresponding configuration property defined in the given
     * setup and with the given callback.
     *  
     * @param setup the setup containing the configuration property defining the registration values to use
     * @param callback the instance to be called back by the model receiver for each received message
     * @throws ModelException if creating the model receiver instance fails
     */
    private void createModelReceiver(Setup setup, ModelReceptionCallback callback) throws ModelException {
        String receptionProtocol = setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_PROTOCOL);
        String receptionUrl = setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_URL);
        int receptionPort = Integer.parseInt(setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_PORT));
        String receptionChannel = setup.getRegistrationConfiguration(Setup.KEY_REGISTRATION_CHANNEL);
        modelReceiver = new ModelReceiver(receptionProtocol, receptionUrl, receptionPort, receptionChannel, null, null,
                callback);
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
    public void modelReceived(String receivedContent) {
        if (receivedContent != null && !receivedContent.isBlank()) {
            addNewModel(receivedContent);
        }
    }
    
    /**
     * Adds the given model to the {@link #entityInformation}. For this purpose, the given string is written to a new
     * file in the {@link #modelDirectory}. That file is used to load an IVML, create the respective configuration, and,
     * hence, the corresponding {@link EntityInfo} instance. If one of these steps fail for any reason, this method logs
     * these errors and warnings only (no further propagation).
     * 
     * @param model the string to interpret as IVML model; must no be <code>null</code>
     */
    private void addNewModel(String model) {
        String modelFileName = System.currentTimeMillis() + ".ivml";
        logger.logInfo(ID, "Adding new IVML model \"" + modelFileName + "\"");
        try {
            fileUtilities.writeFile(modelDirectory.getAbsolutePath(), modelFileName, model, WriteOption.CREATE);
            File newModelFile = new File(modelDirectory, modelFileName);
            easyUtilities.updateModelLocation(modelDirectory);
            if (!addModelInformation(newModelFile)) {
                logger.logWarning(ID, "Adding information for IVML model \"" + modelFileName + "\" failed",
                        "Deleting model file again");
                fileUtilities.delete(newModelFile);
            }
        } catch (FileUtilitiesException | EASyUtilitiesException e) {
            logger.logException(ID, e);
        }
    }
    
    /**
     * Returns the number of {@link EntityInfo} instances loaded from the received models in the
     * {@link #modelDirectory}.
     * 
     * @return zero-based number of available {@link EntityInfo} instance 
     */
    public synchronized int getEntityInfoCount() {
        return entityInformation.size();
    }
    
    /**
     * Returns the {@link EntityInfo} instance at the given index.
     * 
     * @param index the index of the instance to return
     * @return the instance at the given index, or <code>null</code>, if the given index is negative or larger than the
     *         largest index of the {@link #entityInformation}
     */
    public synchronized EntityInfo getEntityInfo(int index) {
        EntityInfo entityInfo = null;
        if (index >= 0 && index < entityInformation.size()) {
            entityInfo = entityInformation.get(index);
        }
        return entityInfo;
    }

}
