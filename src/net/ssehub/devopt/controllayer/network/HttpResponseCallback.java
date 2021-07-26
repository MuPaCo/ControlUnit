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
package net.ssehub.devopt.controllayer.network;

import java.net.http.HttpResponse;

/**
 * This interface needs to be implemented by any class that uses asynchronous send methods of the {@link HttpClient}. An
 * instance of this client will inform the implementing class about arrived server responses using the method defined by
 * this interface.
 * 
 * @author kroeher
 *
 */
public interface HttpResponseCallback {

    /**
     * Receives the server response to an asynchronous HTTP request send via an instance of a {@link HttpClient}.
     * 
     * @param response the string-based response provided by the server or <code>null</code>, if the server does not
     *        provide a response and also does not cause an exception 
     * @param exception the error occurred during processing the request by the server; may be <code>null</code> 
     */
    public abstract void responseArrived(HttpResponse<String> response, NetworkException exception);
    
}
