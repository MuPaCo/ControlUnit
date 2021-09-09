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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.uni_hildesheim.sse.easy.loader.ListLoader;
import net.ssehub.devopt.controllayer.utilities.FileUtilities.WriteOption;
import net.ssehub.easy.basics.modelManagement.AvailableModels;
import net.ssehub.easy.basics.modelManagement.ModelInfo;
import net.ssehub.easy.basics.modelManagement.ModelManagementException;
import net.ssehub.easy.basics.progress.ProgressObserver;
import net.ssehub.easy.varModel.management.VarModel;
import net.ssehub.easy.varModel.model.Project;

/*
 * TODO Loading internal files must be re-implemented
 * 
 * As it is, the EASyUtilties loads the EASy components based on the .easy-producer file and the DevOpt meta-model as a
 * prerequisite to resolve imports of received models of the local elements.
 * 
 * However, this does not work in an executable jar-File, because then we cannot use File-objects, but need to load
 * these jar-internal resources. In turn, this breaks the current implementation:
 * 
 * - For the .easy-producer file: it must be a File-object due to the ListLoader(EASY_STARTUP_FILE) in
 *   startEASyComponents
 * - For the meta-model: there is currently the assumption that these files are loaded prior to any received external
 *   models, which is not completely correct. Loading the meta-model and loading additional models must be decoupled
 *   completely to use the internal model directory for the meta-model once, and the externally defined directory for
 *   received models independently. However, for the latter again resources instead of files must be used, which is
 *   questionable with respect to the EASy components.
 *   
 * For now, we package the executable jar with all files and directory necessary for its proper execution. Hence, the
 * configuration option for the model directory must always be the directory in that package. Changing that path must
 * include copying the meta-model files to that directory as well.
 * 
 */

/**
 * This class provides thread-safe access to the standalone version of EASy-Producer. In particular, the methods of this
 * class provide access to the modeling and reasoning capabilities of EASy-Producer as well as additional utilities to
 * simplify working with data elements, like IVML models or the reasoner. 
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
     * The identifier if this class, e.g. for printing messages.
     */
    private static final String ID = EASyUtilities.class.getSimpleName();
    
    /**
     * The start-up file required to load the EASy-Producer components.
     */
    private static final File EASY_STARTUP_FILE = new File(".easy-producer");
    
    /**
     * The {@link ProgressObserver} used to track the progress of EASy-Producer tasks.
     */
    private static final ProgressObserver EASY_PROGRESS_OBSERVER = ProgressObserver.NO_OBSERVER;
    
    /**
     * The directory in which the DevOpt meta model (IVML) files are located.
     */
    private static final File DEVOPT_META_MODEL_DIRECTORY = new File("./model");
    
    /**
     * The directory containing the model (IVML) files of registered elements.
     */
    private File modelDirectory;
    
    /**
     * The loader for the EASy-Producer components.
     */
    private ListLoader easyLoader;
    
    /**
     * The local reference to the global {@link VarModel}.
     */
    private VarModel varModel;
    
    /**
     * The definition of whether the EASy-Producer components are loaded (<code>true</code>) or not
     * (<code>false</code>). This includes the successful loading of the DevOpt meta model and those models available in
     * the {@link #modelDirectory}.
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
     * Starts the components of EASy-Producer, loads the DevOpt meta model as well as all models located in the 
     * directory denoted by the given path. This method must be called exactly once before this instance is used the
     * first time. Calling this method multiple times without stopping the components in between has no effect.
     *  
     * @param modelDirectoryPath the path to the directory in which the IVML models are saved; must not be
     *        <code>null</code> and always an existing directory
     * @throws EASyUtilitiesException if loading the EASy-Producer components or loading the models fails
     * @see #stopEASyComponents()
     */
    public void startEASyComponents(String modelDirectoryPath) throws EASyUtilitiesException {
        if (!easyComponentsLoaded) {            
            try {
                easyLoader = new ListLoader(EASY_STARTUP_FILE);
                easyLoader.startup();
            } catch (IOException e) {
                throw new EASyUtilitiesException("Loading EASy-Producer components failed", e);
            }
            varModel = VarModel.INSTANCE;
            // Load the meta model as part of start-up; otherwise loading any models relying on it will fail 
            try {
                varModel.locations().addLocation(DEVOPT_META_MODEL_DIRECTORY, EASY_PROGRESS_OBSERVER);
            } catch (ModelManagementException e) {
                throw new EASyUtilitiesException("Loading DevOpt meta model from \"" + DEVOPT_META_MODEL_DIRECTORY 
                        + "\" failed", e);
            }
            // Load already known models from model directory specified in the configuration file
            addModelDirectory(modelDirectoryPath);
            easyComponentsLoaded = true;
            
            // Some debug logging information 
            logger.logDebug(ID, "EASy-Producer components started");
            List<String> loadedProjects = getProjectNames();
            if (loadedProjects.size() == 0) {
                logger.logDebug(ID, "Loaded models: <none>");
            } else {
                logger.logDebug(ID, StringUtilities.INSTANCE.prepend("Loaded models:", loadedProjects));
            }
        }
    }
    
    /**
     * Adds the directory denoted by the given path as a model location to the {@link #varModel}. Further, a successful
     * addition of this directory triggers loading the individual model (IVML) files in that directory.
     * 
     * @param modelDirectoryPath the path to the directory in which the IVML models are saved; must not be
     *        <code>null</code> and always an existing directory
     * @throws EASyUtilitiesException if adding the directory or loading the models fails
     * @see {@link #loadModels(File)}
     */
    private void addModelDirectory(String modelDirectoryPath) throws EASyUtilitiesException {
        modelDirectory = new File(modelDirectoryPath);
        try {
            varModel.locations().addLocation(modelDirectory, EASY_PROGRESS_OBSERVER);
            loadModels(modelDirectory);
        } catch (ModelManagementException e) {
            throw new EASyUtilitiesException("Adding IVML model directory \"" + modelDirectory.getAbsolutePath()
                    + "\" failed", e);
        }
    }
    
    /**
     * Loads all model (IVML) files in the given directory. This method applies a file name filter reducing the set of
     * files to be considered to those having a file name, which ends with the <code>*.ivml</code>-extension.
     * 
     * @param modelDirectory the directory in which the IVML models are saved; must not be <code>null</code> and always
     *        an existing directory
     * @throws EASyUtilitiesException if loading a model file in the given directory fails
     */
    private void loadModels(File modelDirectory) throws EASyUtilitiesException {
        File[] modelFiles = modelDirectory.listFiles(new FilenameFilter() {
            
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".ivml");
            }
        });
        
        if (modelFiles != null) {
            File modelFile;
            String projectName;
            for (int i = 0; i < modelFiles.length; i++) {
                modelFile = modelFiles[i];
                try {
                    // TODO should be more elegant to avoid "." within paths to match correct URI in varModel
                    modelFile = new File(modelFile.getCanonicalPath());
                    projectName = getProjectName(modelFile.toURI());
                    if (projectName != null) {                    
                        List<ModelInfo<Project>> models = varModel.availableModels().getModelInfo(projectName);
                        if (models != null) {                        
                            try {
                                varModel.load(models.get(0));
                            } catch (ModelManagementException e) {
                                throw new EASyUtilitiesException("Loading model file \"" + modelFile + "\" failed", e);
                            }
                        }
                    }
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Stops the components of EASy-Producer and removes the DevOpt meta model directory as well as the
     * {@link #modelDirectory} from the {@link #varModel}. This method must be called exactly once after this instance
     * is used the last time. Calling this method multiple times without starting the components in between has no
     * effect.
     * 
     * @throws EASyUtilitiesException if removing the model directories fails
     * @see #startEASyComponents(String)
     */
    public void stopEASyComponents() throws EASyUtilitiesException {
        if (easyComponentsLoaded) {
            // Unload already known models from model directory specified in the configuration file
            removeModelDirectory(modelDirectory.getAbsolutePath());
            // Unload the meta model as part of tear-down after the models relying on it are removed
            try {
                varModel.locations().removeLocation(DEVOPT_META_MODEL_DIRECTORY, EASY_PROGRESS_OBSERVER);
            } catch (ModelManagementException e) {
                throw new EASyUtilitiesException("Unloading DevOpt meta model from \"" + DEVOPT_META_MODEL_DIRECTORY 
                        + "\" failed", e);
            }
            // Finally, shutdown the EASy-Producer components
            easyLoader.shutdown();
            easyComponentsLoaded = false;
            logger.logDebug(ID, "EASy-Producer components stopped");
        }
    }
    
    /**
     * Removes the directory denoted by the given path as a model location from the {@link #varModel}.
     * 
     * @param modelDirectoryPath the path to the directory in which the IVML models are saved; must not be
     *        <code>null</code> and always an existing directory
     * @throws EASyUtilitiesException if removing the directory fails
     * @see {@link #loadModels(File)}
     */
    private void removeModelDirectory(String modelDirectoryPath) throws EASyUtilitiesException {
        File modelDirectory = new File(modelDirectoryPath);
        try {
            // TODO unload the respective models first
            varModel.locations().removeLocation(modelDirectory, EASY_PROGRESS_OBSERVER);
        } catch (ModelManagementException e) {
            throw new EASyUtilitiesException("Removing IVML model directory \"" + modelDirectory.getAbsolutePath()
                    + "\" failed", e);
        }
    }
    
    /**
     * Adds the given model to the {@link #varModel}. For this purpose, this method first created a new model file in
     * the {@link #modelDirectory} with the given model file name and write the given model to that file. Second, it
     * reloads the EASy-Producer components to consider these changes.
     * 
     * @param model the {@link String} representing a complete IVML model
     * @param modelFileName the name of the model (IVML) file to write the give model to; the correct file extension
     *        will be added to this file name automatically by this method
     * @return the name of the IVML project as defined in the given model or <code>null</code>, if loading the model
     *         fails
     * @throws EASyUtilitiesException if adding the new model fails
     */
    public synchronized String addModel(String model, String modelFileName) throws EASyUtilitiesException {
        logger.logDebug(ID, "Adding new model file \"" + modelFileName + "\"");
        String addedProjectName = null;
        if (model != null && model.length() > 0) {
            if (modelFileName != null && modelFileName.length() > 0) {
                modelFileName = modelFileName + ".ivml";
                try {
                    FileUtilities.INSTANCE.writeFile(modelDirectory.getAbsolutePath(), modelFileName, model,
                            WriteOption.CREATE);
                    removeModelDirectory(modelDirectory.getAbsolutePath());
                    addModelDirectory(modelDirectory.getAbsolutePath());
                    addedProjectName = getProjectName(modelDirectory, modelFileName);
                    // TODO validate new model
                } catch (FileUtilitiesException e) {
                    throw new EASyUtilitiesException("Creating model file \"" + modelFileName + "\" failed", e);
                }
            } else {
                throw new EASyUtilitiesException("Adding a model without a model file name is not supported");
            }
        } else {
            throw new EASyUtilitiesException("Adding an empty model is not supported");
        }
        return addedProjectName;
    }
    
    /**
     * TODO .
     * @param ivmlProject TODO
     * @return TODO
     */
    public synchronized boolean isValid(Project ivmlProject) {
        // TODO implement validation using the reasoner
        return false;
    }
    
    /**
     * Returns the name of the IVML project defined in the file denoted by the given model file name. This file must be
     * available in the {@link #modelDirectory}.
     * 
     * @param modelFileName the name of the model file for which the name of the defined project shall be returned; this
     *        name must <b>not</b> include the <code>.ivml</code>-extension as it will be added automatically by this
     *        method
     * @return the name of the IVML project or <code>null</code>, if no project for the given model file is known
     */
    public String getProjectName(String modelFileName) {
        modelFileName = modelFileName + ".ivml";
        return getProjectName(modelDirectory, modelFileName);
    }
    
    /**
     * Returns the name of the IVML project defined in the file denoted by the given model file name and located in the
     * given model directory.
     * 
     * @param modelDirectory the {@link File} denoting the directory in which the model file is located
     * @param modelFileName the name of the model file for which the name of the defined project shall be returned; this
     *        name must include the <code>.ivml</code>-extension
     * @return the name of the IVML project or <code>null</code>, if no project for the given model file is known
     */
    private String getProjectName(File modelDirectory, String modelFileName) {
        String projectName = null;
        File modelFile;
        try {
            modelFile = new File(modelDirectory.getCanonicalPath(), modelFileName);
            URI modelFileUri = modelFile.toURI();
            projectName = getProjectName(modelFileUri);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return projectName;
    }
    
    /**
     * Returns the name of the IVML project defined in the file denoted by the given {@link URI}.
     * 
     * @param modelFileUri the {@link URI} of the model file for which the name of the defined project shall be returned
     * @return the name of the IVML project or <code>null</code>, if no project for the given URI is known
     */
    private String getProjectName(URI modelFileUri) {
        String projectName = null;
        AvailableModels<Project> availableModels = varModel.availableModels();
        if (availableModels != null) {            
            ModelInfo<Project> modelInfo = availableModels.getInfo(modelFileUri);
            if (modelInfo != null) {                
                projectName = modelInfo.getName();
            }
        }
        return projectName;
    }
    
    /**
     * TODO Removes the IVML project denoted by the give name from the {@link #varModel}.
     * 
     * @param projectName the name of the IVML project to remove
     */
    public synchronized void removeModel(String projectName) {
        // TODO implement model removal at runtime including its unloading first
    }
    
    /**
     * Returns the current list of names of all IVML projects loaded in the {@link #varModel}.
     * 
     * @return the list names of of all loaded IVML projects; never <code>null</code>, but may be <i>empty</i>
     */
    public synchronized List<String> getProjectNames() {
        List<String> projectNames = new ArrayList<String>();
        int modelCount = varModel.getModelCount();
        for (int i = 0; i < modelCount; i++) {
            Project project = varModel.getModel(i);
            projectNames.add(project.getName());
        }
        return projectNames;
    }
    
}
