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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import net.ssehub.devopt.controllayer.utilities.Logger;

/**
 * This class realizes a handler for processing {@link HttpExchange}s for a specific
 * {@link com.sun.net.httpserver.HttpContext} of a {@link HttpServer}. This handling consists of informing the handler's
 * {@link HttpRequestCallback} about incoming requests and sending a response created by that callback to the requesting
 * client.
 * 
 * @author kroeher
 *
 */
public class HttpRequestHandler implements HttpHandler {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = HttpRequestHandler.class.getSimpleName();
    
    /**
     * The local reference to the global {@link Logger}.
     */
    private Logger logger = Logger.INSTANCE;
    
    /**
     * The instance to call back, if a request for the {@link com.sun.net.httpserver.HttpContext} arrived to which this
     * request handler instance is assigned.
     */
    private HttpRequestCallback callback;
    
    /**
     * Constructs a new {@link HttpRequestHandler} instance.
     * 
     * @param callback the instance to call back, if a request for the {@link com.sun.net.httpserver.HttpContext}
     *        arrived to which this request handler instance is assigned.
     * @throws NetworkException if the given callback is <code>null</code>
     */
    protected HttpRequestHandler(HttpRequestCallback callback) throws NetworkException {
        if (callback == null) {
            throw new NetworkException("Creating HTTP request handler failed: callback is \"null\"");
        }
        this.callback = callback;
    }

    /**
     * Handles the given exchange by sending the inherent request information to the {@link #callback} of this handler
     * and responding via the given exchange based on the received response of the callback.
     * 
     * @param exchange the exchange to handle; must never be <code>null</code>
     */
    @Override
    public void handle(HttpExchange exchange) {
        HttpRequest exchangeRequest = null;
        NetworkException exchangeExcpetion = null;
        try {
            exchangeRequest = createHttpRequest(exchange);
        } catch (NetworkException e) {
            exchangeExcpetion = e;
        } finally {
            HttpResponse exchangeResponse = callback.requestArrived(exchangeRequest, exchangeExcpetion);
            if (exchangeResponse == null) {
                exchangeResponse = new HttpResponse(null, 500, "Internal callback returned null");
                logger.logWarning(ID, "Callback \"" + callback.getName() + "\" returned \"null\"-response",
                        "Using default " + exchangeResponse);
            }
            try {
                respond(exchange, exchangeResponse);
            } catch (NetworkException e) {
                logger.logException(ID, e);
            }
        }
    }
    
    /**
     * Creates a simplified HTTP request for sending the request information to the {@link #callback} of this handler.
     * 
     * @param exchange the exchange containing the request information to include in the HTTP request
     * @return the simplified HTTP request or <code>null</code>, if the given exchange is <code>null</code>
     * @throws NetworkException if the HTTP request method of the given exchange is unsupported or retrieving the
     *         request body of the given exchange fails
     */
    private HttpRequest createHttpRequest(HttpExchange exchange) throws NetworkException {
        HttpRequest request = null;
        if (exchange != null) {
            request = new HttpRequest(getRequestMethod(exchange.getRequestMethod()), exchange.getRequestURI(),
                    getRequestBody(exchange.getRequestBody()));
        }
        return request;
    }
    
    /**
     * Returns a {@link HttpRequestMethod} described by the given string. This string must be a textual representation
     * of the desired request method only, e.g. "GET".
     *  
     * @param methodString the string describing a supported HTTP request method
     * @return the HTTP request method described by the given string; never <code>null</code>
     * @throws NetworkException if the given string does not describe a supported HTTP request method
     */
    private HttpRequestMethod getRequestMethod(String methodString) throws NetworkException {
        HttpRequestMethod method = null;
        if (methodString.equalsIgnoreCase(HttpRequestMethod.GET.name())) {
            method = HttpRequestMethod.GET;
        } else if (methodString.equalsIgnoreCase(HttpRequestMethod.POST.name())) {
            method = HttpRequestMethod.GET;
        } else {
            throw new NetworkException("Unsupported HTTP request method: " + methodString);
        }
        return method;
    }

    /**
     * Returns the content of the given input stream as a single string.
     * 
     * @param bodyInputStream the input stream to read
     * @return the string representing the content of the given input stream or <code>null</code>, if the given input
     *         stream is <code>null</code>
     * @throws NetworkException if reading or closing the given input stream fails
     */
    private String getRequestBody(InputStream bodyInputStream) throws NetworkException {
        String body = null;
        if (bodyInputStream != null) {
            /*
             * This may change in future, if reading all bytes at once becomes problematic.
             * In that case, use ByteArrayOutputStream as a buffer, for example. 
             */
            try {
                byte[] bodyBytes = bodyInputStream.readAllBytes();
                body = new String(bodyBytes);
            } catch (IOException | OutOfMemoryError e) {
                throw new NetworkException("Reading HTTP request body bytes failed", e);
            } finally {
                try {
                    bodyInputStream.close();
                } catch (IOException e) {
                    throw new NetworkException("Closing HTTP request body input stream failed", e);
                }
            }
            
        }
        return body;
    }
    
    /**
     * Responds to the request of the given exchange by sending the data provided by the given response.
     *  
     * @param exchange the exchange to respond to; must not be <code>null</code>
     * @param response the response providing the information to send; must not be <code>null</code>
     * @throws NetworkException if sending the response headers, writing the response body, or closing the response body
     *         output writer fails
     */
    private void respond(HttpExchange exchange, HttpResponse response) throws NetworkException {
        String[] responseHeadersStrings = response.getHeaders();
        if (responseHeadersStrings != null) {
            if (responseHeadersStrings.length % 2 == 0) {                
                for (int i = 0; i < responseHeadersStrings.length; i = (i + 2)) {
                    exchange.getResponseHeaders().add(responseHeadersStrings[i], responseHeadersStrings[i + 1]);
                }
            } else {
                logger.logWarning(ID, "Invalid response headers size: " + responseHeadersStrings.length,
                        "Responding without header adaptations");
            }
        } else {
            logger.logWarning(ID, "Missing response headers", "Responding without header adaptations");
        }
        
        String responseBody = response.getBody();
        if (responseBody == null) {
            responseBody = "null";
        }
        
        try {            
            exchange.sendResponseHeaders(response.getCode(), responseBody.getBytes().length);
        } catch (NullPointerException | IOException e) {
            throw new NetworkException("Sending HTTP response headers failed", e);
        }
        
        OutputStream bodyOutputStream = null;
        try {
            bodyOutputStream = exchange.getResponseBody();
            bodyOutputStream.write(responseBody.getBytes());
        } catch (IOException e) {
            throw new NetworkException("Writing HTTP response body bytes failed", e);
        } finally {
            if (bodyOutputStream != null) {                
                try {
                    bodyOutputStream.close();
                } catch (IOException e) {
                    throw new NetworkException("Closing HTTP response body output stream failed", e);
                }
            }
        }
    }

}
