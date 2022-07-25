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
import java.util.HashMap;
import java.util.Iterator;

import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.monitoring.MonitoringDataReceiver;
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
     * The mapping of all {@link EntityInfo} instances loaded from the received models in the {@link #modelDirectory}.
     * The keys for these mapping are the respective IVML model file names from which the {@link EntityInfo} instances
     * were created.
     */
    private HashMap<String, EntityInfo> entityInformation;
    /*
     * TODO Can we shutdown EASy and release IVML models/resources again after model addition?
     * This manager and, hence, the remaining controller components will use EntityInfo instances internally.
     * 
     * Partially done as addModelInformation() unloads the source configuration again. However, EASy is still running.
     */
    
    /**
     * Constructs a new {@link ModelManager} instance.
     */
    private ModelManager() {
        entityInformation = new HashMap<String, EntityInfo>();        
    }
    
    /**
     * Returns the singleton instance of this class. <b>Note</b> that it is required to call {@link #setUp(Setup)}
     * exactly once before calling this method successfully.
     * 
     * @return the singleton instance of this class, or <code>null</code>, if it was not set up yet or it was stopped
     *         already
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
                        String availableModelFileName;
                        for (int i = 0; i < availableModelFiles.length; i++) {
                            availableModelFileName = availableModelFiles[i].getName();
                            try {                                
                                availableModelFileName = availableModelFileName.substring(0,
                                        availableModelFileName.indexOf("."));
                                if (addModelInformation(availableModelFileName, availableModelFiles[i]) == null) {
                                    logger.logWarning(ID, "Adding model information from \"" + availableModelFiles[i]
                                            + "\" not successful");
                                }
                            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                                throw new ModelException("Parsing model identifier from file name \""
                                        + availableModelFileName + "\" failed", null);
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
     * {@link #entityInformation} with the given key.
     * 
     * @param ivmlModelIdentifier the identifier to use as key for the addition of the created {@link EntityInfo};
     *        must not be <code>null</code> nor <i>blank</i>
     * @param ivmlModelFile the IVML model file for which the {@link Configuration} will be retrieved from the
     *        {@link #easyUtilities} to create the corresponding {@link EntityInfo} to add; must not be
     *        <code>null</code>
     * @return the created {@link EntityInfo} instance, if its addition was successful; <code>null</code> otherwise 
     */
    private EntityInfo addModelInformation(String ivmlModelIdentifier, File ivmlModelFile) {
        EntityInfo addedEntityInformation = null;
        Configuration modelConfiguration = null;
        try {
            modelConfiguration = easyUtilities.loadConfiguration(ivmlModelFile);
            if (easyUtilities.isValid(modelConfiguration)) {
                EntityInfo entityInfo = new EntityInfo(modelConfiguration, ivmlModelFile.getAbsolutePath());
                if (!isEntityInfoKnown(entityInfo) && entityInformation.put(ivmlModelIdentifier, entityInfo) == null) {
                    addedEntityInformation = entityInfo;
                } else {
                    logger.logWarning(ID, "Entity information already known", "No addition of: "
                            + entityInfo.toString());
                }
            } else {
                logger.logWarning(ID, "Model information addition aborted", "Model configuration from \""
                        + ivmlModelFile + "\" is not valid");
                // TODO also log the exact problem(s) here - the above warning alone is not sufficient
            }
        } catch (EASyUtilitiesException | ModelException e) {
            logger.logException(ID, e);
        } finally {
            if (modelConfiguration != null) {
                logger.logDebug(ID, "Unloading model configuration source for \"" + ivmlModelIdentifier + "\"");
                try {
                    if (!easyUtilities.unloadConfiguration(modelConfiguration)) {
                        logger.logWarning(ID, "Unloading model configuration source for \"" + ivmlModelIdentifier
                                + "\" failed");
                    }
                } catch (EASyUtilitiesException e) {
                    logger.logException(ID, e);
                }
            }
        }
        return addedEntityInformation;
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
            Iterator<String> entityInformationKeysIterator = entityInformation.keySet().iterator();
            while (!entityInfoKnown && entityInformationKeysIterator.hasNext()) {
                if (entityInfo.equals(entityInformation.get(entityInformationKeysIterator.next()))) {
                    entityInfoKnown = true;
                }
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
     * Starts the internal {@link ModelReceiver} after establish monitoring for all available models already loaded
     * during setup.
     * 
     * @throws ModelException if activating the network connection of the model receiver fails
     */
    public void start() throws ModelException {
        // Establish monitoring for all available models already loaded during setup
        Iterator<String> entityInformationKeysIterator = entityInformation.keySet().iterator();
        EntityInfo entityInfo;
        while (entityInformationKeysIterator.hasNext()) {
            entityInfo = entityInformation.get(entityInformationKeysIterator.next());
            if (!establishMonitoring(entityInfo)) {
                logger.logWarning(ID, "Establishing entity monitroring failed", entityInfo.toString());
            }
        }
        // Start model receiver to receive registration requests
        modelReceiver.start();
    }
    
    /**
     * Stops the internal {@link ModelReceiver}, the monitoring of all known entities, and releases all resources
     * including this instances.
     * 
     * @throws ModelException if stopping the model receiver fails
     */
    public void stop() throws ModelException {
        // Stop model receiver to reject any further incoming registration requests
        modelReceiver.stop();
        modelReceiver = null;
        // Stop monitoring of all entities
        Iterator<String> entityInformationKeysIterator = entityInformation.keySet().iterator();
        EntityInfo currentEntityInfo;
        while (entityInformationKeysIterator.hasNext()) {
            currentEntityInfo = entityInformation.get(entityInformationKeysIterator.next());
            if (!MonitoringDataReceiver.INSTANCE.removeObservable(currentEntityInfo.getMonitoringChannel())) {
                logger.logError(ID, "Stopping monitoring of " + currentEntityInfo.toString() + " failed",
                        "No further actions possible");
            }
        }
        // Release remaining resources and delete references
        entityInformation.clear();
        entityInformation = null;
        modelDirectory = null;
        fileUtilities = null;
        logger = null;
        instance = null;
    }

    @Override
    public void modelReceived(String receivedContent) {
        if (receivedContent != null && !receivedContent.isBlank()) {
            String modelIdentifier = "" + System.currentTimeMillis();
            EntityInfo addedEntityInfo = addReceivedModel(modelIdentifier, receivedContent);
            if (addedEntityInfo != null) {
                if (!establishMonitoring(addedEntityInfo)) {
                    logger.logWarning(ID, "Establishing entity monitoring failed",
                            "Removing entity information including sources: " + addedEntityInfo.toString());
                    /*
                     * At this point, the source configuration is already unloaded from EASy-Producer as part of
                     * addModelInformation(). Hence, it is only necessary to delete the IVML file, update the model
                     * location for EASy, and remove the EntityInfo from the map.
                     */
                    try {
                        fileUtilities.delete(new File(addedEntityInfo.getSourceFilePath()));
                        easyUtilities.updateModelLocation(modelDirectory);
                        entityInformation.remove(modelIdentifier);
                    } catch (FileUtilitiesException | EASyUtilitiesException e) {
                        logger.logException(ID, e);
                    }
                }
            } else {
                logger.logWarning(ID, "Adding entity information failed", "Received information: " + receivedContent);
            }
        } else {
            logger.logDebug(ID, "Receiving empty message content");
        }
    }
    
    /**
     * Adds the given model to the {@link #entityInformation} using the given model identifier as key. For this purpose,
     * the given string is written to a new file in the {@link #modelDirectory}. This file is used to load the contained
     * IVML model, create the respective configuration, and, hence, the corresponding {@link EntityInfo} instance. If
     * one of these steps fail for any reason, this method logs these errors and warnings only (no further propagation).
     * 
     * @param modelIdentifier the identifier to use as key for the addition of the created {@link EntityInfo};must not
     *        be <code>null</code> nor <i>blank</i>
     * @param model the string to interpret as IVML model; must no be <code>null</code>
     * @return the created {@link EntityInfo} instance, if its addition was successful; <code>null</code> otherwise 
     */
    private EntityInfo addReceivedModel(String modelIdentifier, String model) {
        EntityInfo addedEntityInfo = null;
        String modelFileName = modelIdentifier + ".ivml";
        logger.logInfo(ID, "Adding new IVML model \"" + modelFileName + "\"");
        try {
            fileUtilities.writeFile(modelDirectory.getAbsolutePath(), modelFileName, model, WriteOption.CREATE);
            File newModelFile = new File(modelDirectory, modelFileName);
            easyUtilities.updateModelLocation(modelDirectory);
            addedEntityInfo = addModelInformation(modelIdentifier, newModelFile);
            if (addedEntityInfo == null) {
                logger.logWarning(ID, "Adding information for IVML model \"" + modelFileName + "\" failed",
                        "Deleting model file again");
                fileUtilities.delete(newModelFile);
                easyUtilities.updateModelLocation(modelDirectory);
            }
        } catch (FileUtilitiesException | EASyUtilitiesException e) {
            logger.logException(ID, e);
        }
        return addedEntityInfo;
    }
    
    /**
     * Calls the {@link MonitoringDataReceiver} instance using the given entity information to add a new observable. If
     * this addition is successful, a network connection is active that allows monitoring the respective entity via its
     * parameters defined for monitoring in its model. The monitoring data receiver manages this connection and informs
     * registered callbacks about received monitoring messages.
     * 
     * @param observableInfo the information about the entity to add as observable
     * @return <code>true</code>, if the addition was successful; <code>false</code> otherwise
     */
    private boolean establishMonitoring(EntityInfo observableInfo) {
        return MonitoringDataReceiver.INSTANCE.addObservable(observableInfo.getIdentifier(),
                observableInfo.getMonitoringChannel(), observableInfo.getMonitoringUrl(),
                observableInfo.getMonitoringPort());
    }
    
    /**
     * Returns the number of {@link EntityInfo} instances loaded from the received models in the
     * {@link #modelDirectory}.
     * 
     * @return zero-based number of available {@link EntityInfo} instances
     */
    public synchronized int getEntityInfoCount() {
        return entityInformation.size();
    }
    
    /**
     * Returns the {@link EntityInfo} instance for the given key.
     * 
     * @param key the key of the instance to return
     * @return the instance for the given key, or <code>null</code>, if the given key maps to <code>null</code> or the
     *         key is not known
     */
    public synchronized EntityInfo getByKey(String key) {
        return entityInformation.get(key);
    }
    
    /**
     * Returns the {@link EntityInfo} instance with the given monitoring channel definition. If multiple instances with
     * the same monitoring channel definition exist, the first match will be returned only.
     * 
     * @param channel the channel (MQTT topic name or HTTP server context name) defined as monitoring scope of the
     *        instance to return
     * @return the instance for the given channel, or <code>null</code>, if the given channel is <code>null</code> or
     *         <i>blank</i>, or, if no such instance is available
     */
    public synchronized EntityInfo getByChannel(String channel) {
        EntityInfo entityInfo = null;
        if (channel != null && !channel.isBlank()) {
            Iterator<String> entityInformationKeysIterator = entityInformation.keySet().iterator();
            EntityInfo currentEntityInfo;
            while (entityInfo == null && entityInformationKeysIterator.hasNext()) {
                currentEntityInfo = entityInformation.get(entityInformationKeysIterator.next());
                if (channel.equals(currentEntityInfo.getMonitoringChannel())) {
                    entityInfo = currentEntityInfo;
                }
            }
        }
        return entityInfo;
    }

}
