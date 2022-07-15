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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.uni_hildesheim.sse.easy.loader.ListLoader;
import net.ssehub.devopt.controllayer.utilities.FileUtilities.WriteOption;
import net.ssehub.easy.basics.modelManagement.AvailableModels;
import net.ssehub.easy.basics.modelManagement.ModelInfo;
import net.ssehub.easy.basics.modelManagement.ModelManagementException;
import net.ssehub.easy.basics.progress.ProgressObserver;
import net.ssehub.easy.reasoning.core.frontend.ReasonerFrontend;
import net.ssehub.easy.reasoning.core.reasoner.ReasonerConfiguration;
import net.ssehub.easy.reasoning.core.reasoner.ReasoningResult;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.management.VarModel;
import net.ssehub.easy.varModel.model.Project;

/**
 * This class provides thread-safe access to the stand-alone version of EASy-Producer. In particular, the methods of
 * this class provide access to the model management and reasoning capabilities.
 * 
 * @author kroeher
 *
 */
public class EASyUtilities {
    
    /**
     * The identifier of this class, e.g. for printing messages.
     */
    private static final String ID = EASyUtilities.class.getSimpleName();
    
    /**
     * The {@link ProgressObserver} used to track the progress of EASy-Producer tasks.
     */
    private static final ProgressObserver EASY_PROGRESS_OBSERVER = ProgressObserver.NO_OBSERVER;
    
    /**
     * The name of the directory, which will contain the runtime files necessary to start the EASy-Producer components
     * and to load IVML models based on the DevOpt meta model. The method {@link #createRuntimeResources()} creates the
     * corresponding directory and extracts the internal resources denoted by the {@link #EASY_RUNTIME_FILES_NAMES} to
     * it.
     */
    private static final String EASY_RUNTIME_DIRECTORY_NAME = "easy-producer-runtime";
    
    /**
     * The two parts of the path to the directory  which will contain the runtime files necessary to start the
     * EASy-Producer components and to load IVML models based on the DevOpt meta model. The first part defines the
     * parent path (directory), while the second part is the {@link #EASY_RUNTIME_DIRECTORY_NAME}.
     */
    private static final String[] EASY_RUNTIME_DIRECTORY_PATH_PARTS = {"./", EASY_RUNTIME_DIRECTORY_NAME};
    
    /**
     * The name of the EASy-Producer startup file.
     */
    private static final String EASY_STARTUP_FILE_NAME = ".easy-producer";
    
    /**
     * The names of the EASy-Producer runtime files. These files are:
     * <ul>
     * <li>The EASy-Producer startup file as defined by {@link #EASY_STARTUP_FILE_NAME}</li>
     * <li>The IVML files defining the DevOpt meta model</li>
     * </ul>
     */
    private static final String[] EASY_RUNTIME_FILES_NAMES = {EASY_STARTUP_FILE_NAME,
        "DevOpt_Analysis.ivml",
        "DevOpt_Basics.ivml",
        "DevOpt_Configuration.ivml",
        "DevOpt_Control.ivml",
        "DevOpt_Description.ivml",
        "DevOpt_Identification.ivml",
        "DevOpt_Optimization.ivml",
        "DevOpt_Runtime.ivml",
        "DevOpt_Software.ivml",
        "DevOpt_System.ivml"
    };
    
    /**
     * The singleton instance of this class.
     */
    /*
     * checkstyle: stop declaration order check
     * 
     * Construction requires EASY_RUNTIME_DIRECTORY_PATH_PARTS. Hence, different declaration order required
     */
    public static final EASyUtilities INSTANCE = new EASyUtilities();
    //checkstyle: resume declaration order check
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The local reference to the global {@link FileUtilities}.
     */
    private FileUtilities fileUtilities = FileUtilities.INSTANCE;
    
    /**
     * The directory containing the runtime files necessary to start the EASy-Producer components and to load IVML
     * models based on the DevOpt meta model. The method {@link #createRuntimeResources()} creates this directory and
     * extracts the internal resources denoted by the {@link #EASY_RUNTIME_FILES_NAMES} to it. The method
     * {@link #deleteRuntimeResources()} deletes this directory and its content again.
     */
    private File easyRuntimeDirectory;
    
    /**
     * The loader for the EASy-Producer components.
     */
    private ListLoader easyLoader;
    
    /**
     * The local reference to the global {@link VarModel} of EASy-Producer, if starting the EASy-Producer components was
     * successful; <code>null</code> otherwise.
     */
    private VarModel varModel;
    
    /**
     * The definition of whether the EASy-Producer components are loaded (<code>true</code>) or not
     * (<code>false</code>). This includes the successful loading of the DevOpt meta model.
     */
    private boolean easyComponentsLoaded;
    
    /**
     * Constructs a new {@link EASyUtilities} instance.
     */
    private EASyUtilities() {
        /*
         * Creating file instance at this point already is necessary to support checks in areRuntimeResourcesCreated()
         * before any directory will be created conditionally upon its existence
         */
        easyRuntimeDirectory = new File(EASY_RUNTIME_DIRECTORY_PATH_PARTS[0], EASY_RUNTIME_DIRECTORY_PATH_PARTS[1]);
        easyLoader = null;
        varModel = null;
        easyComponentsLoaded = false;
    }
    
    /**
     * Starts the components of EASy-Producer, defines the {@link #varModel} reference, and adds the DevOpt meta model
     * directory as model location to the {@link #varModel}. For this purpose, this method extracts the required runtime
     * resources first via {@link #createRuntimeResources()}.<br>
     * <br>
     * This method must be called exactly once before this instance is used the first time. Calling this method multiple
     * times without stopping the components in between has no effect.
     *
     * @throws EASyUtilitiesException if extracting the runtime resources, starting the components, adding the
     *                                directory, or retrieving necessary information for these tasks fails
     * @see #stopEASyComponents()
     */
    public void startEASyComponents() throws EASyUtilitiesException {
        logger.logDebug(ID, "Starting EASy-Producer components");
        if (!easyComponentsLoaded) {
            logger.logDebug(ID, "Creating runtime resources");
            createRuntimeResources();
            
            logger.logDebug(ID, "Loading core components");
            // Start EASy-Producer components using the startup file in the runtime directory
            try {
                easyLoader = new ListLoader(new File(easyRuntimeDirectory, EASY_STARTUP_FILE_NAME));
                easyLoader.startup();
            } catch (NullPointerException | IOException e) {
                throw new EASyUtilitiesException("Loading EASy-Producer components failed", e);
            }
            // If starting EASy-Producer components was successful, set reference to its variability model and...
            varModel = VarModel.INSTANCE;
            
            logger.logDebug(ID, "Adding runtime resources directory as model location");
            // ... add runtime directory (contains DevOpt meta model) as model location to the variability model
            try {
                if (!addModelLocation(easyRuntimeDirectory)) {
                    throw new EASyUtilitiesException("Adding DevOpt meta model location \""
                            + easyRuntimeDirectory.getParentFile() + "\" failed");
                }
            } catch (NullPointerException | EASyUtilitiesException e) {
                throw new EASyUtilitiesException("Adding DevOpt meta model directory failed", e);
            }
            
            // Track successful start
            easyComponentsLoaded = true;
            logger.logDebug(ID, "EASy-Producer components started");
        } else {
            logger.logDebug(ID, "EASy-Producer components already started");
        }
    }
    
    /**
     * Creates the required runtime resources for the EASy-Producer components by creating the
     * {@link #easyRuntimeDirectory} and extracting the internal resources denoted by the
     * {@link #EASY_RUNTIME_FILES_NAMES} as files to that directory.<br>
     * <br>
     * This method must only be called, if {@link #easyComponentsLoaded} is <code>false</code> and before any
     * EASy-Producer component is started, e.g. at the beginning of {@link #startEASyComponents()} only.
     *  
     * @throws EASyUtilitiesException if creating the directory, retrieving the necessary class loader, or writing the
     *         files denoting runtime resources fails
     * @see #deleteRuntimeResources()
     */
    private void createRuntimeResources() throws EASyUtilitiesException {
        if (!areRuntimeResourcesCreated()) {
            // Create root runtime directory at the current location of this tool
            try {
                easyRuntimeDirectory = fileUtilities.createDirectory(EASY_RUNTIME_DIRECTORY_PATH_PARTS[0],
                        EASY_RUNTIME_DIRECTORY_PATH_PARTS[1]);
            } catch (FileUtilitiesException e) {
                throw new EASyUtilitiesException("Creating runtime resources directory failed", e);
            }
            // Retrieve class loader for identifying EASy-Producer resources
            Class<?> thisClass = this.getClass();
            ClassLoader thisClassLoader = null;
            try {                
                thisClassLoader = thisClass.getClassLoader();
            } catch (SecurityException e) {
                throw new EASyUtilitiesException("Retrieving class loader for resource identification failed", e);
            }
            if (thisClassLoader != null) {
                // Extract all runtime file contents from internal resources to files in runtime directory
                String resourceName;
                InputStream resourceStream;
                for (int i = 0; i < EASY_RUNTIME_FILES_NAMES.length; i++) {                
                    resourceName = EASY_RUNTIME_FILES_NAMES[i];
                    resourceStream = thisClassLoader.getResourceAsStream(resourceName);
                    if (resourceStream == null) {
                        throw new EASyUtilitiesException("No input stream available for resource \"" + resourceName
                                + "\"");
                    }
                    try {
                        fileUtilities.writeFile(easyRuntimeDirectory.getAbsolutePath(), resourceName, resourceStream,
                                WriteOption.CREATE);
                    } catch (FileUtilitiesException e) {
                        throw new EASyUtilitiesException("Writing runtime resouce \"" + resourceName + "\" to \""
                                + easyRuntimeDirectory.getAbsolutePath() + "\"failed", e);
                    }
                }
            } else {
                // Should never happen
                throw new EASyUtilitiesException("No class loader for resource identification available");
            }
        } else {
            logger.logDebug(ID, "Runtime resources already created at \"" + easyRuntimeDirectory + "\"");
        }
    }
    
    /**
     * Checks whether the {@link #easyRuntimeDirectory} exists and contains all files denoted by the
     * {@link #EASY_RUNTIME_FILES_NAMES}.
     * 
     * @return <code>true</code>, if runtime resource exist; <code>false</code> otherwise
     */
    private boolean areRuntimeResourcesCreated() {
        boolean runtimeResourcesCreated = false;
        if (easyRuntimeDirectory != null && easyRuntimeDirectory.exists()) {
            String[] easyRuntimeDirectoryFileNames = easyRuntimeDirectory.list();
            if (easyRuntimeDirectoryFileNames != null
                    && easyRuntimeDirectoryFileNames.length == EASY_RUNTIME_FILES_NAMES.length) {
                runtimeResourcesCreated = true;
                int easyRuntimeFilesNamesCounter = 0;
                int easyRuntimeDirectoryFileNamesCounter;
                boolean runtimeResourceFound;
                while (runtimeResourcesCreated && easyRuntimeFilesNamesCounter < EASY_RUNTIME_FILES_NAMES.length) {
                    easyRuntimeDirectoryFileNamesCounter = 0;
                    runtimeResourceFound = false;
                    while (!runtimeResourceFound
                            && easyRuntimeDirectoryFileNamesCounter < easyRuntimeDirectoryFileNames.length) {
                        runtimeResourceFound = EASY_RUNTIME_FILES_NAMES[easyRuntimeFilesNamesCounter]
                                .equals(easyRuntimeDirectoryFileNames[easyRuntimeDirectoryFileNamesCounter]);
                        easyRuntimeDirectoryFileNamesCounter++;
                    }
                    runtimeResourcesCreated = runtimeResourceFound;
                    easyRuntimeFilesNamesCounter++;
                }
            } else {
                logger.logDebug(ID, "Runtime resources directory contains wrong number of files");
            }
        } else {
            logger.logDebug(ID, "Runtime resources directory not created yet");
        }
        return runtimeResourcesCreated;
    }
    
    /**
     * Removes all model locations from the {@link #varModel} (including the DevOpt meta model directory) and stops the
     * components of EASy-Producer. Finally, this method deletes the required runtime resources via
     * {@link #deleteRuntimeResources()}.<br>
     * <br>
     * This method must be called exactly once after this instance is used the last time. Calling this method multiple
     * times without starting the components in between has no effect.
     * 
     * @throws EASyUtilitiesException if removing a model location or deleting the runtime resources fails
     * @see #startEASyComponents()
     */
    public void stopEASyComponents() throws EASyUtilitiesException {
        logger.logDebug(ID, "Stopping EASy-Producer components");
        if (easyComponentsLoaded) {
            logger.logDebug(ID, "Removing all model locations");
            try {
                // Remove all model locations before actual shutdown
                File[] locationFiles = new File [varModel.locations().getLocationCount()];
                for (int i = 0; i < locationFiles.length; i++) {
                    locationFiles[i] = varModel.locations().getLocation(i).getLocation();
                }
                for (int i = 0; i < locationFiles.length; i++) {
                    removeModelLocation(locationFiles[i]);
                }
            } catch (EASyUtilitiesException e) {
                throw new EASyUtilitiesException("Removing model locations failed", e);
            }
            
            logger.logDebug(ID, "Unloading core components");
            easyLoader.shutdown();
            
            logger.logDebug(ID, "Deletin runtime resources");
            deleteRuntimeResources();
            
            // Track successful stop
            easyComponentsLoaded = false;
            logger.logDebug(ID, "EASy-Producer components stopped");
        } else {
            logger.logDebug(ID, "EASy-Producer components already stopped");
        }
    }
    
    /**
     * Deletes the {@link #easyRuntimeDirectory} and its entire content.<br>
     * <br>
     * This method must only be called, if {@link #easyComponentsLoaded} is <code>true</code> and after all
     * EASy-Producer component are stopped, e.g. at the end of {@link #stopEASyComponents()} only.
     *  
     * @throws EASyUtilitiesException if deleting the directory or one of its files fails
     * @see #createRuntimeResources()
     */
    private void deleteRuntimeResources() throws EASyUtilitiesException {
        if (easyRuntimeDirectory != null && easyRuntimeDirectory.exists()) {
            try {
                fileUtilities.delete(easyRuntimeDirectory);
            } catch (FileUtilitiesException e) {
                throw new EASyUtilitiesException("Deleting runtime resources failed", e);
            }
        } else {
            logger.logDebug(ID, "Runtime resources directory not available", "No deletion of runtime resources");
        }
    }
    
    /**
     * Checks whether the given file denotes a directory previously added as model location to this instance.
     * 
     * @param modelLocationFile the file (directory) to check for being known as model location
     * @return <code>true</code>, if the given file is known as model location; <code>false</code> otherwise, which
     *         includes that the given file is <code>null</code>, it does not exist, or is not a directory
     */
    public synchronized boolean isModelLocationKnown(File modelLocationFile) {
        boolean modelLocationKnown = false;
        if (isValid(modelLocationFile)) {
            // Model locations are never null; safe direct call of getLocationCount() and others here
            int locationCount = varModel.locations().getLocationCount();
            int locationCounter = 0;
            while (!modelLocationKnown && locationCounter < locationCount) {
                try {                
                    if (varModel.locations().getLocation(locationCounter).getLocation().getCanonicalPath()
                            .equals(modelLocationFile.getCanonicalPath())) {
                        modelLocationKnown = true;
                    }
                } catch (IOException e) {
                    logger.logException(ID, e);
                }
                locationCounter++;
            }
        }
        return modelLocationKnown;
    }
    
    /**
     * Adds the given file as IVML model location to this instance. An IVML model location is a directory, which
     * contains IVML model files currently, or will contain some in future. Any sub-directories in the directory denoted
     * by the given file will also be considered as IVML model locations.
     * 
     * @param modelLocationFile the file to add as IVML model location
     * @return <code>true</code>, if adding the given file as model location was successful; <code>false</code>
     *         otherwise, which includes that the given file is already known as model location or that file is 
     *         <code>null</code>, it does not exist, or it is not a directory
     * @throws EASyUtilitiesException if adding the location fails unexpectedly
     */
    public synchronized boolean addModelLocation(File modelLocationFile) throws EASyUtilitiesException {
        boolean modelLocationAdded = false;
        if (!isModelLocationKnown(modelLocationFile) && isValid(modelLocationFile)) {
            try {
                // Model locations are never null; safe direct call of addLocation() here
                if (varModel.locations().addLocation(modelLocationFile, EASY_PROGRESS_OBSERVER) != null) {
                    modelLocationAdded = true;
                }
            } catch (ModelManagementException e) {
                throw new EASyUtilitiesException("Adding model location \"" + modelLocationFile.getAbsolutePath()
                        + "\" failed", e);
            }
        }
        return modelLocationAdded;
    }
    
    /**
     * Updates the IVML model location denoted by the given file. If the content of the model location changes, e.g., by
     * changes to individual files, their addition, or removal, this instance needs to update its internal model states
     * accordingly. Hence, this method must be called to ensure consistency between runtime data of this instance and
     * the persisted one.  
     * 
     * @param modelLocationFile the file denoting the IVML model location to update
     * @return <code>true</code>, if updating the model location denoted by the given file was successful;
     *         <code>false</code> otherwise, which includes that the location denoted by the given file is not known as
     *         model location
     * @throws EASyUtilitiesException if updating the location fails unexpectedly 
     */
    public synchronized boolean updateModelLocation(File modelLocationFile) throws EASyUtilitiesException {
        boolean modelLocationUpdated = false;
        if (isModelLocationKnown(modelLocationFile)) {
            try {
                // Model locations are never null; safe direct call of updateLocation() here
                varModel.locations().updateLocation(modelLocationFile, EASY_PROGRESS_OBSERVER);
                modelLocationUpdated = true;
            } catch (ModelManagementException e) {
                throw new EASyUtilitiesException("Updating model location \"" + modelLocationFile.getAbsolutePath()
                        + "\" failed", e);
            }
        }
        return modelLocationUpdated;
    }
    
    /**
     * Removes the IVML model location denoted by the given file.
     * 
     * @param modelLocationFile the file denoting the IVML model location to remove
     * @return <code>true</code>, if removing the model location denoted by the given file was successful;
     *         <code>false</code> otherwise, which includes that the location denoted by the given file is not known as
     *         model location
     * @throws EASyUtilitiesException if removing the location fails unexpectedly
     */
    public synchronized boolean removeModelLocation(File modelLocationFile) throws EASyUtilitiesException {
        boolean modelLocationRemoved = false;
        if (isModelLocationKnown(modelLocationFile)) {
            try {
                // Model locations are never null; safe direct call of removeLocation() here
                varModel.locations().removeLocation(modelLocationFile, EASY_PROGRESS_OBSERVER);
                modelLocationRemoved = true;
            } catch (ModelManagementException e) {
                throw new EASyUtilitiesException("Removing model location \"" + modelLocationFile.getAbsolutePath()
                        + "\" failed", e);
            }
        }
        return modelLocationRemoved;
    }
    
    /**
     * Checks whether the given file denotes a valid IVML model location. A file is considered valid, if it is not
     * <code>null</code>, it exists, and it is a directory.
     * 
     * @param modelLocationFile the file to check
     * @return <code>true</code>, if the given file is a valid IVML model location; <code>false</code> otherwise
     */
    private boolean isValid(File modelLocationFile) {
        boolean isValid = false;
        if (modelLocationFile != null && modelLocationFile.exists() && modelLocationFile.isDirectory()) {
            isValid = true;
        }
        return isValid;
    }
    
    /**
     * Loads the {@link Configuration} of the IVML model (project) defined in the given IVML file. For successful
     * loading, the given file must be located in one of the IVML model locations known to this instance. If this not
     * the case, loading will fail due to missing model information.
     * 
     * @param ivmlFile the file containing the IVML model (project) definition to use for configuration loading
     * @return the loaded {@link Configuration} or <code>null</code>, if loading fails
     * @throws EASyUtilitiesException if the given file is invalid (<code>null</code>, does not exist, is not a file) or
     *         the loading process fails to retrieve required model information 
     */
    public synchronized Configuration loadConfiguration(File ivmlFile) throws EASyUtilitiesException {
        if (ivmlFile == null) {
            throw new EASyUtilitiesException("Source IVML file is \"null\"");
        }
        if (!ivmlFile.isFile()) {
            throw new EASyUtilitiesException("Source IVML file \"" + ivmlFile.getAbsolutePath() + "\" is not a file");
        }
        if (!ivmlFile.exists()) {
            throw new EASyUtilitiesException("Source IVML file \"" + ivmlFile.getAbsolutePath() + "\" does not exist");
        }
        
        logger.logDebug(ID, "Adding project from file \"" + ivmlFile.getAbsolutePath()  + "\"");
        Configuration configuration = null;
        try {
            ivmlFile = new File(ivmlFile.getCanonicalPath()); // Avoid "." within path to match correct URI in varModel
            AvailableModels<Project> availableModels = varModel.availableModels(); // Available models are never null
            ModelInfo<Project> modelInfo = availableModels.getInfo(ivmlFile.toURI());
            if (modelInfo != null) {                
                String projectName = modelInfo.getName();
                List<ModelInfo<Project>> models = availableModels.getModelInfo(projectName);
                if (models != null) {
                    int modelInfoCounter = 0;
                    while (configuration == null && modelInfoCounter < models.size()) {
                        modelInfo = models.get(modelInfoCounter);
                        if (modelInfo.getLocation().equals(ivmlFile.toURI())) {                                
                            Project loadedIvmlProject = varModel.load(modelInfo);
                            if (loadedIvmlProject != null) {
                                configuration = new Configuration(loadedIvmlProject);
                            } else {
                                throw new EASyUtilitiesException("No project \"" + projectName + "\"");
                            }
                        }
                        modelInfoCounter++;
                    }
                } else {
                    throw new EASyUtilitiesException("No model information for project \"" + projectName + "\"");
                }
            } else {
                throw new EASyUtilitiesException("No model information for URI \"" + ivmlFile.toURI() + "\"");
            }
        } catch (EASyUtilitiesException | ModelManagementException e) {
            throw new EASyUtilitiesException("Loading configuration from file \"" + ivmlFile.getAbsolutePath()
                    + "\" failed", e);
        } catch (IOException e) {
            throw new EASyUtilitiesException("Creating canonical path for file \"" + ivmlFile.getAbsolutePath()
                    + "\" failed", e);
        }
        return configuration;
    }
    
    /**
     * Unloads the given {@link Configuration}. If unloading is successful, all references to the given configuration
     * must be released. This method does not care for such references other than the one managed by this instance.
     * 
     * @param configuration the configuration to unload
     * @return <code>true</code>, if unloading the given configuration was successful; <code>false</code> otherwise,
     *         which includes that the given configuration is <code>null</code> or no model was unloaded
     * @throws EASyUtilitiesException if unloading the configuration fails unexpectedly
     */
    public synchronized boolean unloadConfiguration(Configuration configuration) throws EASyUtilitiesException {
        boolean configurationUnloaded = false;
        if (configuration != null) {
            try {
                if (varModel.unload(configuration.getProject(), EASY_PROGRESS_OBSERVER) >= 1) {
                    /*
                     * Unloading returns the number of unloaded models.
                     * This must be at least 1 due to the given configuration.
                     * If this given configuration is the only one loaded so far, unloading will also unload the models
                     * imported by that configuration to use the DevOpt meta model. 
                     */
                    configurationUnloaded = true;
                }
            } catch (ModelManagementException e) {
                throw new EASyUtilitiesException("Unloading configuration \"" + configuration.getName() + "\" failed",
                        e);
            }
        }
        return configurationUnloaded;
    }
    
    /**
     * Checks the given {@link Configuration} for validity using the IVML reasoner provided by the EASy-Producer
     * components.
     * 
     * @param configuration the configuration to check
     * @return <code>true</code>, if the given configuration is valid; <code>false</code> otherwise, which includes that
     *         the given configuration is <code>null</code>
     * @throws EASyUtilitiesException if validating the configuration fails unexpectedly
     */
    public synchronized boolean isValid(Configuration configuration) throws EASyUtilitiesException {
        boolean isValid = false;
        if (configuration != null) {
            ReasonerConfiguration reasonerConfiguration = new ReasonerConfiguration();
            try {                
                ReasoningResult reasoningResult = ReasonerFrontend.getInstance()
                        .propagate(configuration, reasonerConfiguration, ProgressObserver.NO_OBSERVER);
                isValid = !reasoningResult.hasConflict();
            } catch (NullPointerException e) {
                // Not documented, but was thrown by some tests
                throw new EASyUtilitiesException("Checking validity of configuration \"" + configuration.getName()
                        + "\" failed", e);
            }
        }
        return isValid;
    }
    
}
