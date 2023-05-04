/*
 * Copyright 2023 University of Hildesheim, Software Systems Engineering
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
package net.ssehub.devopt.controllayer.update;

import java.net.http.HttpResponse;
import java.util.Set;

import net.ssehub.devopt.controllayer.Setup;
import net.ssehub.devopt.controllayer.model.EntityInfo;
import net.ssehub.devopt.controllayer.model.ModelManager;
import net.ssehub.devopt.controllayer.network.HttpClient;
import net.ssehub.devopt.controllayer.network.NetworkException;
import net.ssehub.devopt.controllayer.utilities.GenericCallback;
import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class realizes the propagation of software update information received from external sources for local elements
 * supervised by this control unit.
 * 
 * @author kroeher
 *
 */
public class SoftwareUpdater implements Runnable, GenericCallback<String> {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = SoftwareUpdater.class.getSimpleName();
    
    /**
     * The singleton instance of this class.
     */
    private static SoftwareUpdater instance;
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The local reference to the global {@link ModelManager}.
     */
    private ModelManager modelManager = ModelManager.getInstance();
    
    /**
     * The {@link Thread} in which this instance is executed.
     */
    private Thread instanceThread;
    
    /**
     * The {@link UpdateException} thrown during {@link #startInstance()}. May be <code>null</code>, if this instance is
     * not started yet (see also {@link #updateReceiverEstablished}) or starting this instance was successful.<br>
     * <br>
     * Declaring this attribute as <code>volatile</code> is mandatory as it is a shared variable between the thread,
     * which creates the instance of this class, and the {@link #instanceThread}. It enables propagating exceptions
     * thrown during starting the instance and the caller of {@link #start()}, which spans the two threads.
     */
    private volatile UpdateException instanceStartException;
    
    /**
     * The definition of whether {@link #startInstance()} successfully established the {@link #updateReceiver}
     * (<code>true</code>) or not (<code>false</code>). The default value is <code>false</code>.<br>
     * <br>
     * Declaring this attribute as <code>volatile</code> is mandatory as it is a shared variable between the thread,
     * which creates the instance of this class, and the {@link #instanceThread}. It enables blocking the caller of
     * {@link #start()} until establishing the mode receiver is finished, which spans the two threads. Blocking the
     * caller is necessary to inform about potential fail of this start via propagating the
     * {@link #instanceStartException}.
     */
    private volatile boolean updateReceiverEstablished;
    
    /**
     * The {@link UpdateReceiver} which manages the network connection and the incoming requests via that connection for
     * software update information. It calls this instance back, if a new message arrives.
     */
    private UpdateReceiver updateReceiver;
    
    /**
     * Constructs a new {@link SoftwareUpdater} instance.
     */
    private SoftwareUpdater() {
        instanceThread = null;
        instanceStartException = null;
        updateReceiverEstablished = false;        
    }
    
    /**
     * Returns the singleton instance of this class. <b>Note</b> that it is required to call {@link #setUp(Setup)}
     * exactly once before calling this method successfully.
     * 
     * @return the singleton instance of this class, or <code>null</code>, if it was not set up yet or it was stopped
     *         already
     */
    public static SoftwareUpdater getInstance() {
        return instance;
    }
    
    /**
     * Creates the singleton instance of this class based on the given setup.
     * 
     * @param setup the configuration properties to use for setting up the singleton instance of this class
     * @throws UpdateException if setting up the instance failed or the instance is already set up
     */
    public static void setUp(Setup setup) throws UpdateException {
        if (instance == null) {
            if (setup != null && instance == null) {
                instance = new SoftwareUpdater();
                instance.createUpdateReceiver(setup, instance);
            } else {
                throw new UpdateException("Setup is \"null\"");
            }
        } else {
            throw new UpdateException("Model manager instance already set up");
        }
    }
    
    /**
     * Creates the {@link #updateReceiver} in accordance to the corresponding configuration property defined in the
     * given setup and with the given callback.
     *  
     * @param setup the setup containing the configuration property defining the software update values to use
     * @param callback the instance to be called back by the update receiver for each received message
     * @throws UpdateException if creating the update receiver instance fails
     */
    private void createUpdateReceiver(Setup setup, GenericCallback<String> callback) throws UpdateException {
        String updateProtocol = setup.getUpdateConfiguration(Setup.KEY_UPDATE_PROTOCOL);
        String updateUrl = setup.getUpdateConfiguration(Setup.KEY_UPDATE_URL);
        int updatePort = Integer.parseInt(setup.getUpdateConfiguration(Setup.KEY_UPDATE_PORT));
        String updateChannel = setup.getUpdateConfiguration(Setup.KEY_UPDATE_CHANNEL);
        updateReceiver = new UpdateReceiver(updateProtocol, updateUrl, updatePort, updateChannel, null, null, callback);
    }
    
    /**
     * Starts the {@link SoftwareUpdater} instance in a new {@link Thread}. Hence, the usage of this method must be
     * equal to calling {@link Thread#start()}.
     * 
     * @throws UpdateException if establishing the internal {@link UpdateReceiver} instance fails; this will also
     *         terminate the thread in which this instance is executed
     */
    public synchronized void start() throws UpdateException {
        if (instanceThread == null) {
            logger.logInfo(ID, "Starting instance (thread)");
            instanceThread = new Thread(this, ID);
            instanceThread.start(); // This calls run() of this runnable, which in turn calls startInstance()
            /*
             * Calling 'start()' above triggers calling 'run()' of this instance, which in turn calls 'startInstance()'.
             * The latter method sets 'modelReceiverEstablished' to 'true', if starting the model receiver instance
             * was successful. If starting the model receiver instance fails, 'startInstance()' sets
             * 'instanceStartExecption' with a new exception describing the cause of the fail. Hence, the loop below
             * will terminate in either case, but ensures that the caller of this method is blocked until this instance
             * is completely started.
             */
            while (!updateReceiverEstablished && instanceStartException == null) {
                /* Block caller until 'startInstance()' is done */
            }
            if (instanceStartException != null) {
                instanceThread = null;
                throw instanceStartException;
            }
            logger.logInfo(ID, "Instance (thread) started");
        }
    }
    
    /**
     * Stops the {@link SoftwareUpdater} instance and its {@link Thread}.
     * 
     * @throws UpdateException if stopping the internal {@link UpdateReceiver} instance or the thread fails
     */
    public synchronized void stop() throws UpdateException {
        if (instanceThread != null) {
            logger.logInfo(ID, "Stopping instance (thread)");
            stopInstance();
            try {
                instanceThread.join();
                instanceThread = null;
                logger.logInfo(ID, "Instance (thread) stopped");
            } catch (InterruptedException e) {
                throw new UpdateException("Waiting for software updater thread to join failed", e);
            } finally {
                logger = null;
            }
        }
    }
    
    /**
     * Starts the internal {@link UpdateReceiver}.<br>
     * <br>
     * If starting the instance was successful can be checked via {@link #updateReceiverEstablished} and 
     * {@link #instanceStartException}.
     * 
     */
    private void startInstance() {
        try {
            updateReceiver.start();
            updateReceiverEstablished = true;
        } catch (UpdateException e) {
            instanceStartException = e;
        }
    }
    
    /**
     * Stops the internal {@link UpdateReceiver} and releases all resources including this instances.
     * 
     * @throws UpdateException if stopping the update receiver fails
     */
    private void stopInstance() throws UpdateException {
        // Stop model receiver to reject any further incoming registration requests
        updateReceiver.stop();
        updateReceiver = null;
        instance = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inform(String receivedContent) {
        if (receivedContent != null && !receivedContent.isBlank()) {
            propagateUpdateInformation(receivedContent);
        } else {
            logger.logDebug(ID, "Receiving empty message content");
        }
    }
    
    /**
     * Sends the given software update information to each local element known by its available {@link EntityInfo}
     * instance of the {@link ModelManager}.
     * 
     * @param updateInformation the software update information to propagate to all local elements
     */
    private void propagateUpdateInformation(String updateInformation) {
        Set<String> entityInformationKeys = modelManager.getEntityInfoKeys();
        EntityInfo entityInformation = null;
        HttpClient updatePropagationClient = null;
        for (String entityInformationKey : entityInformationKeys) {
            entityInformation = modelManager.getByKey(entityInformationKey);
            if (entityInformation != null) {
                try {
                    updatePropagationClient = new HttpClient("SoftwareUpdaterClient", null, null, null, -1,
                            null, null);
                    HttpResponse<String> updateResponse = updatePropagationClient.sendPostSync(
                            entityInformation.getHost(), null, updateInformation, 1000);
                    if (updateResponse != null) {
                        logger.logInfo(ID, "Software update information response of \""
                                + entityInformation.getIdentifier() + "\": " + updateResponse.body());
                    }
                } catch (NetworkException e) {
                    logger.logException(ID, new UpdateException("Propagating software update information to \""
                            + entityInformation.getIdentifier() + "\" failed", e));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (instanceThread != null) { // ensure that this call is executed in instance thread
            startInstance();
        }
    }

}
