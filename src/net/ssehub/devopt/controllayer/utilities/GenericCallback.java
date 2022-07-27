/*
 * Copyright 2022 University of Hildesheim, Software Systems Engineering
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

/**
 * This interface has to be implemented by any class that needs to be informed about a certain type of element. Due to
 * its generic nature, the type of the element and the purpose why to be informed about depends on the implementing
 * class.
 * 
 * @author kroeher
 *
 * @param <T> the type of the element this callback will be informed about
 */
public interface GenericCallback<T> {

    /**
     * Informs this instance about the given element.
     * 
     * @param element the element this instance needs to be informed about
     */
    public abstract void inform(T element);
    
}
