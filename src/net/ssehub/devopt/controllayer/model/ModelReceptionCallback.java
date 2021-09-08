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

import net.ssehub.devopt.controllayer.utilities.EASyUtilities;

/**
 * This interface needs to be implemented by any class that needs to be informed about and react to a new registration
 * of an external element at this control node. A successful registration and, hence, a callback via the method defined
 * by this interface occurs, if the external element send a message containing a valid IVML model. This model describes
 * the element and will be added to the model management for use of other components of a control node.
 * 
 * @author kroeher
 *
 */
public interface ModelReceptionCallback {

    /**
     * Receives the name of the added IVML model file and the name of the loaded IVML project in that file, if the
     * registration of an external element with this information at the {@link ModelReceiver} was successful.
     * 
     * @param ivmlFileName the name of the IVML file in the model directory, which contains the IVML project definition
     *        of the registered element; never <code>null</code> nor <i>blank</i>
     * @param ivmlProjectName the name of the IVML project available via the {@link EASyUtilities} defined in the given
     *        IVML file; never <code>null</code> nor <i>blank</i>
     */
    public abstract void modelReceived(String ivmlFileName, String ivmlProjectName);
    
}
