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
package net.ssehub.devopt.controllayer;

import java.io.OutputStream;

import net.ssehub.devopt.controllayer.model.ModelException;
import net.ssehub.devopt.controllayer.model.ModelManager;
import net.ssehub.devopt.controllayer.utilities.Logger;
import net.ssehub.devopt.controllayer.utilities.Logger.LogLevel;
import net.ssehub.devopt.controllayer.utilities.StringUtilities;

/**
 * This is the main class of this tool. It provides the mandatory {@link #main(String[])} method as the starting point
 * of the internal workflow. As part of this workflow, this class triggers the loading of a configuration, instantiates 
 * the main components of this tool based on a configuration's parameter values, and starts these components.
 * 
 * @author kroeher
 *
 */
public class Controller {
    
    /**
     * The identifier of this class, e.g., for logging messages during instance creation. 
     */
    private static final String ID = Controller.class.getSimpleName();
    
    private Logger logger = Logger.INSTANCE;
    
    private ModelManager modelManager;
    
    /**
     * Constructs a new {@link Controller} instance.
     * 
     * @param args the command-line arguments as supplied to {@link #main(String[])}
     */
    private Controller(String[] args) {
        /*
         * The very first call must always be loading the setup, which includes configuring the logger.
         * Hence, do not call the logger before loading the setup.
         */
        Setup setup = loadSetup(args);
        if (setup == null) {
            logger.logInfo(ID, "Execution aborted");
            System.exit(1);
        }
        
        /*
         * TODO this is a singleton. Hence, instance is created at startup, but the actual instantiation of the EASy-
         * Producer components must be done here before any other components that rely on its capabilities.
         * In particular, the setup must provide the model directory and (optionally) the .easy-producer startup file.
         * 
         * Setup details:
         *    model.directory = DIR - where the save models received from local elements as part of the registration at
         *                            the controller; default is a new folder called "models" next to the location of
         *                            the controller bundle
         *    easy.startup = FILE - where to find the easy-startup file defining which EASy-Producer components to load
         *                          and how (see DevDoc of EASy); default is the internal file, which loads ...
         *                          <describe components and style (DS here)>
         */
        setupEASyUtilities();
        
        try {
            modelManager = new ModelManager(setup);
            modelManager.run();
        } catch (ModelException e) {
            logger.logException(ID, e);
            System.exit(1);
        }
    }
    
    /**
     * Returns a new {@link Setup} instance, which loaded the configuration properties of the file denoted by the file
     * path at index 0 of the given command-line arguments.<br>
     * This method also calls {@link #setupLogger(Setup)} to immediately enable the usage of the global {@link Logger},
     * if creating the setup instance was successful.
     *  
     * @param args the command-line arguments as supplied to {@link #main(String[])}
     * @return the setup instance of this controller or <code>null</code>, if the command-line arguments are empty or
     *         creating the setup instance fails
     */
    private Setup loadSetup(String[] args) {
        Setup setup = null;
        try {
            // Do not reorder as the logger must be configured before any messages are logged
            setup = new Setup(args[0]);
            setupLogger(setup);
            logger.logInfo(ID, "Loading configuration:", "Configurtion file \"" + args[0] + "\"");
            logger.logInfo(ID, StringUtilities.INSTANCE.prepend("Configuration loaded:", setup.toLogLines()));
            if (setup.hasPostponedWarnings()) {                
                logger.logWarning(ID,
                        StringUtilities.INSTANCE.prepend("Setup warnings:", setup.getPostponedWarnings()));
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            /*
             * Use System.out as fall-back output streams, if there are no command-line arguments and, hence, no
             * configuration file path for creating the setup.
             */
            logger.addOutputStream(System.out, LogLevel.STANDARD);
            logger.addOutputStream(System.out, LogLevel.DEBUG);
            logger.logError(ID, "Missing configuration file path");
        } catch (SetupException e) {
            /*
             * Use System.out as fall-back output streams, if creating the setup, which typically provides the desired
             * streams, fails.
             */
            logger.addOutputStream(System.out, LogLevel.STANDARD);
            logger.addOutputStream(System.out, LogLevel.DEBUG);
            logger.logInfo(ID, "Loading configuration:", "Configurtion file \"" + args[0] + "\"");
            logger.logException(ID, e);
        }
        return setup;
    }
    
    /**
     * Configures the global {@link Logger} by adding the output streams for standard and debug messages as defined in
     * the given {@link Setup}.
     * 
     * @param setup the setup instance providing the user-defined configuration properties of this controller instance
     */
    private void setupLogger(Setup setup) {
        logger.addOutputStream(getLogStream(setup.getLoggingConfiguration(Setup.KEY_LOGGING_STANDARD)),
                LogLevel.STANDARD);
        logger.addOutputStream(getLogStream(setup.getLoggingConfiguration(Setup.KEY_LOGGING_DEBUG)),
                LogLevel.DEBUG);
    }
    
    /**
     * Determines the {@link OutputStream} associated with the given setup value (configuration property) for logging.
     * 
     * @param setupValue the value of the configuration property identified by either {@link Setup#KEY_LOGGING_STANDARD}
     *        or {@link Setup#KEY_LOGGING_DEBUG}
     * @return the {@link OutputStream} associated with the given value or <code>null</code>, if no such association is
     *         defined
     */
    private OutputStream getLogStream(String setupValue) {
        OutputStream stream = null;
        if (setupValue.equalsIgnoreCase("s")) {
            stream = System.out;
        }
        return stream;
    }

    /**
     * Starts the internal workflow of this tool.
     * 
     * @param args the supplied command-line arguments
     */
    public static void main(String[] args) {
        new Controller(args); 
    }

}
