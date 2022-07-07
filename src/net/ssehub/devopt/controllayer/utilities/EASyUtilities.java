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
import java.net.URISyntaxException;
import java.util.List;

import de.uni_hildesheim.sse.easy.loader.ListLoader;
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
     * The singleton instance of this class.
     */
    public static final EASyUtilities INSTANCE = new EASyUtilities();
    
    /**
     * The identifier of this class, e.g. for printing messages.
     */
    private static final String ID = EASyUtilities.class.getSimpleName();
    
    /**
     * The {@link ProgressObserver} used to track the progress of EASy-Producer tasks.
     */
    private static final ProgressObserver EASY_PROGRESS_OBSERVER = ProgressObserver.NO_OBSERVER;
    
    /**
     * The loader for the EASy-Producer components.
     */
    private ListLoader easyLoader;
    
    /**
     * The local reference to the global {@link VarModel} of EASy-Producer.
     */
    private VarModel varModel;
    
    /**
     * The definition of whether the EASy-Producer components are loaded (<code>true</code>) or not
     * (<code>false</code>). This includes the successful loading of the DevOpt meta model.
     */
    private boolean easyComponentsLoaded;
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * Constructs a new {@link EASyUtilities} instance.
     */
    private EASyUtilities() {
        easyComponentsLoaded = false;
    }
    
    /**
     * Starts the components of EASy-Producer, defines the {@link #varModel} reference, and adds the DevOpt meta model
     * directory as model location to the {@link #varModel}. This method must be called exactly once before this
     * instance is used the first time. Calling this method multiple times without stopping the components in between
     * has no effect.
     *
     * @throws EASyUtilitiesException if starting the components, adding the directory, or retrieving necessary
     *                                information for these tasks fails
     * @see #stopEASyComponents()
     */
    public void startEASyComponents() throws EASyUtilitiesException {        
        if (!easyComponentsLoaded) {
            // Retrieve class loader for identifying EASy-Producer startup file and DevOpt meta model resources
            Class<?> thisClass = this.getClass();
            ClassLoader thisClassLoader = null;
            try {                
                thisClassLoader = thisClass.getClassLoader();
            } catch (SecurityException e) {
                throw new EASyUtilitiesException("Retrieving class loader for resource identification failed", e);
            }
            if (thisClassLoader != null) {
                File resourceFile;
                // Start EASy-Producer components using the respective startup file resource
                try {
                    resourceFile = new File(thisClassLoader.getResource(".easy-producer").toURI());
                    easyLoader = new ListLoader(resourceFile);
                    easyLoader.startup();
                } catch (NullPointerException | URISyntaxException | IllegalArgumentException | IOException e) {
                    throw new EASyUtilitiesException("Loading EASy-Producer components failed", e);
                }
                // If starting EASy-Producer components was successful, set reference to its variability model and...
                varModel = VarModel.INSTANCE;
                // ... add DevOpt meta model directory as model location to the EASy-Producer variability model
                try {
                    resourceFile = new File(thisClassLoader.getResource("DevOpt_System.ivml").toURI());
                    if (!addModelLocation(resourceFile.getParentFile())) {
                        throw new EASyUtilitiesException("Adding internal DevOpt meta model location \""
                                + resourceFile.getParentFile() + "\" failed");
                    }
                } catch (NullPointerException | URISyntaxException | IllegalArgumentException
                        | EASyUtilitiesException e) {
                    throw new EASyUtilitiesException("Adding DevOpt meta model directory failed", e);
                }
                // Track successful loading
                easyComponentsLoaded = true;
                logger.logDebug(ID, "EASy-Producer components started");
            } else {
                throw new EASyUtilitiesException("No EASyUtilities class loader for resource identification available");
            }
        }
    }
    
    /**
     * Removes all model locations from the {@link #varModel} (including the DevOpt meta model directory)  and stops the
     * components of EASy-Producer. This method must be called exactly once after this instance is used the last time.
     * Calling this method multiple times without starting the components in between has no effect.
     * 
     * @throws EASyUtilitiesException if removing a model location fails
     * @see #startEASyComponents()
     */
    public void stopEASyComponents() throws EASyUtilitiesException {
        if (easyComponentsLoaded) {
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
            easyLoader.shutdown();
            easyComponentsLoaded = false;
            logger.logDebug(ID, "EASy-Producer components stopped");
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
                if (projectName != null) {
                    List<ModelInfo<Project>> models = availableModels.getModelInfo(projectName);
                    if (models != null) {                        
                        Project loadedIvmlProject = varModel.load(models.get(0));
                        if (loadedIvmlProject != null) {
                            configuration = new Configuration(loadedIvmlProject);
                        } else {
                            throw new EASyUtilitiesException("No project \"" + projectName + "\"");
                        }
                    } else {
                        throw new EASyUtilitiesException("No model information for project \"" + projectName + "\"");
                    }
                } else {
                    throw new EASyUtilitiesException("No project name for model information from  URI \""
                            + ivmlFile.toURI() + "\"");
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
