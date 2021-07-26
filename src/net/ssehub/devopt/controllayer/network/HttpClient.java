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
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * This class realizes a HTTP client for communication with a HTTP server.
 * 
 * @author kroeher
 *
 */
public class HttpClient extends AbstractNetworkClient {
    
    /**
     * The identifier of this class, e.g., for logging messages. 
     */
    private static final String ID = HttpClient.class.getSimpleName();

    /**
     * The actual HTTP client instance.
     */
    private java.net.http.HttpClient client;
    
    /**
     * The information about the proxy used by this client. The format of this information is "proxyURL:proxyPort". May
     * be <code>null</code>, if this client does not use a proxy. 
     */
    private String proxyInfo;
    
    /**
     * Constructs a new {@link HttpClient} instance for connection to a HTTP server.
     * 
     * @param id the identifier of this client consisting of 1 to 23 UTF-8 encoded bytes representing the characters
     *        [0..9], [a..z], [A..Z] only
     * @param httpVersion the preferred HTTP protocol version or <code>null</code>, if HTTP/2 should be used
     * @param httpRedirect the redirect policy or <code>null</code>, if the policy "never redirect" should be used
     * @param proxyUrl the <i>optional</i> URL or IP of the proxy to use for this client; may be <code>null</code>, if
     *        no proxy should be used
     * @param proxyPort the <i>optional</i> port of the proxy to use for this client; must be between 0 and 65535 or
     *        exactly -1, if no proxy should be used
     * @param username the <i>optional</i> user name required for this client to communicate with its host or
     *        <code>null</code>, if no user name is required
     * @param password the <i>optional</i> password required for this client to communicate with its host or
     *        <code>null</code>, if no user name is required
     * @throws NetworkException if the given identifier is <code>null</code> or does not match the expected format, the
     *         given proxy URL is not <code>null</code>, but blank, the give proxy port is not -1 and not in range, the
     *         proxy could not be created, or creating the client failed
     */
    //checkstyle: stop parameter number check
    public HttpClient(String id, Version httpVersion, Redirect httpRedirect, String proxyUrl, int proxyPort,
            String username, String password) throws NetworkException {
        super(id, username, password);
        createClient(httpVersion, httpRedirect, proxyUrl, proxyPort);
    }
    //checkstyle: resume parameter number check
    
    /**
     * Creates the actual HTTP {@link #client} for connection to a HTTP server.
     * 
     * @param httpVersion the preferred HTTP protocol version or <code>null</code>, if HTTP/2 should be used
     * @param httpRedirect the redirect policy or <code>null</code>, if the policy "never redirect" should be used
     * @param proxyUrl the <i>optional</i> URL or IP of the proxy to use for this client; may be <code>null</code>, if
     *        no proxy should be used
     * @param proxyPort the <i>optional</i> port of the proxy to use for this client; must be between 0 and 65535 or
     *        exactly -1, if no proxy should be used
     * @throws NetworkException if the given proxy URL is not <code>null</code>, but blank, the give proxy port is not
     *         -1 and not in range, the proxy could not be created, or creating the client failed
     */
    private void createClient(Version httpVersion, Redirect httpRedirect, String proxyUrl, int proxyPort)
            throws NetworkException {
        if (httpVersion == null) {
            httpVersion = Version.HTTP_2;
        }
        if (httpRedirect == null) {
            httpRedirect = Redirect.NEVER;
        }
        if (proxyUrl != null && proxyUrl.isBlank()) {
            throw new NetworkException("Invalid HTTP client proxy URL: URL is blank");
        }
        if (proxyPort != -1 && (proxyPort < 0 || proxyPort > 65535)) {
            throw new NetworkException("Invalid HTTP client proxy port: " + proxyPort);
        }
        // Create proxy information, if proxy URL and port are available
        ProxySelector clientProxySelector = null;
        if (proxyUrl != null && proxyPort != -1) {
            try {                
                InetSocketAddress clientProxySocketAddress = new InetSocketAddress(proxyUrl, proxyPort);
                if (clientProxySocketAddress.isUnresolved()) {
                    throw new NetworkException("Proxy URL \"" + proxyUrl + "\" cannot be resolved");
                }
                clientProxySelector = ProxySelector.of(clientProxySocketAddress);
                proxyInfo = proxyUrl + ":" + proxyPort;
            } catch (IllegalArgumentException | SecurityException | NetworkException e) {
                throw new NetworkException("Creating proxy for HTTP client \"" + getId() + "\" failed", e);
            }
        }
        // Create authenticator, if user name and password are available
        Authenticator clientAuthenticator = null;
        if (getUsername() != null && getPassword() != null) {
            clientAuthenticator = new Authenticator() {
                
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(getUsername(), getPassword().toCharArray());
                }
                
            };
        }
        // Create actual client depending on available of proxy and authenticator
        Builder clientBuilder = java.net.http.HttpClient.newBuilder();
        clientBuilder = clientBuilder.version(httpVersion);
        clientBuilder = clientBuilder.followRedirects(httpRedirect);
        if (clientProxySelector != null) {
            clientBuilder = clientBuilder.proxy(clientProxySelector);
        }
        if (clientAuthenticator != null) {
            clientBuilder = clientBuilder.authenticator(clientAuthenticator);
        }
        client = clientBuilder.build();
        setIsClosed(false);
        logger.logDebug(ID, "\"" + getId() + "\" created");
    }
    
    /**
     * Synchronously sends a GET request based on the given parameters. This method blocks its caller until a response
     * is received or the action terminates exceptionally.
     * 
     * @param requestUriString the URI identifying the target (server) to send the request to; must always start with
     *        "http://" and must not be <code>null</code>
     * @param requestHeaders the headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}; may
     *        be <code>null</code>, if no headers are required
     * @param requestTimeout the timeout in milliseconds before a timeout exception is thrown, if the server does not
     *        respond
     * @return the string-based response provided by the server or <code>null</code>, if the server does not provide a
     *         response and also does not cause an exception
     * @throws NetworkException if the given URI is <code>null</code> or invalid, the timeout is non-positive, or 
     *         sending the request fails
     */
    public HttpResponse<String> sendGetSync(String requestUriString, String[] requestHeaders, long requestTimeout)
            throws NetworkException {
        return sendSync(HttpRequestMethod.GET, requestUriString, requestHeaders, null, requestTimeout);
    }

    /**
     * Asynchronously sends a GET request based on the given parameters. This method does not block its caller, but
     * informs the given {@link HttpResponseCallback} when a response is received or the action terminates
     * exceptionally.
     * 
     * @param requestUriString the URI identifying the target (server) to send the request to; must not be
     *        <code>null</code>
     * @param requestHeaders the headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}; may
     *        be <code>null</code>, if no headers are required
     * @param requestTimeout the timeout in milliseconds before a timeout exception is thrown, if the server does not
     *        respond
     * @param callback the instance to call back, if the target server responds or sending terminates exceptionally;
     *        must not be <code>null</code>
     * @throws NetworkException if the given URI is <code>null</code> or invalid, the timeout is non-positive, the
     *         callback is <code>null</code>, or sending the request fails (errors after sending and while waiting for a
     *         response are delivered to the given callback instance)
     */
    public void sendGetAsync(String requestUriString, String[] requestHeaders, long requestTimeout,
            HttpResponseCallback callback) throws NetworkException {
        sendAsync(HttpRequestMethod.GET, requestUriString, requestHeaders, null, requestTimeout, callback);
    }
    
    /**
     * Synchronously sends a POST request based on the given parameters. This method blocks its caller until a response
     * is received or the action terminates exceptionally.
     * 
     * @param requestUriString the URI identifying the target (server) to send the request to; must not be
     *        <code>null</code>
     * @param requestHeaders the headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}; may
     *        be <code>null</code>, if no headers are required
     * @param requestBody the body to add to the request; may be <code>null</code>, if no body is required
     * @param requestTimeout the timeout in milliseconds before a timeout exception is thrown, if the server does not
     *        respond
     * @return the string-based response provided by the server or <code>null</code>, if the server does not provide a
     *         response and also does not cause an exception
     * @throws NetworkException if the given URI is <code>null</code> or invalid, the timeout is non-positive, or 
     *         sending the request fails
     */
    public HttpResponse<String> sendPostSync(String requestUriString, String[] requestHeaders, String requestBody,
            long requestTimeout) throws NetworkException {
        return sendSync(HttpRequestMethod.POST, requestUriString, requestHeaders, requestBody, requestTimeout);
    }

    /**
     * Asynchronously sends a POST request based on the given parameters. This method does not block its caller, but
     * informs the given {@link HttpResponseCallback} when a response is received or the action terminates
     * exceptionally.
     * 
     * @param requestUriString the URI identifying the target (server) to send the request to; must not be
     *        <code>null</code>
     * @param requestHeaders the headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}; may
     *        be <code>null</code>, if no headers are required
     * @param requestBody the body to add to the request; may be <code>null</code>, if no body is required
     * @param requestTimeout the timeout in milliseconds before a timeout exception is thrown, if the server does not
     *        respond
     * @param callback the instance to call back, if the target server responds or sending terminates exceptionally;
     *        must not be <code>null</code>
     * @throws NetworkException if the given URI is <code>null</code> or invalid, the timeout is non-positive, the
     *         callback is <code>null</code>, or sending the request fails (errors after sending and while waiting for a
     *         response are delivered to the given callback instance)
     */
    public void sendPostAsync(String requestUriString, String[] requestHeaders, String requestBody, long requestTimeout,
            HttpResponseCallback callback) throws NetworkException {
        sendAsync(HttpRequestMethod.POST, requestUriString, requestHeaders, requestBody, requestTimeout, callback);
    }
    
    /**
     * Synchronously sends a request based on the given parameters. This method blocks its caller until a response is
     * received or the action terminates exceptionally.
     * 
     * @param requestMethod the {@link HttpRequestMethod} to use for the request; must not be <code>null</code>
     * @param requestUriString the URI identifying the target (server) to send the request to; must not be
     *        <code>null</code>
     * @param requestHeaders the headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}; may
     *        be <code>null</code>, if no headers are required
     * @param requestBody the body to add to the request; may be <code>null</code>, if no body is required
     * @param requestTimeout the timeout in milliseconds before a timeout exception is thrown, if the server does not
     *        respond
     * @return the string-based response provided by the server or <code>null</code>, if the server does not provide a
     *         response and also does not cause an exception
     * @throws NetworkException if the request method is unknown, the given URI is <code>null</code> or invalid, the
     *         headers are not <code>null</code> and invalid, the timeout is non-positive, or sending the request fails
     */
    private HttpResponse<String> sendSync(HttpRequestMethod requestMethod, String requestUriString,
            String[] requestHeaders, String requestBody, long requestTimeout) throws NetworkException {
        HttpResponse<String> response = null;
        HttpRequest request = createHttpRequest(requestMethod, requestUriString, requestHeaders, requestBody,
                requestTimeout);
        try {
            setIsConnected(true);
            response = client.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new NetworkException("Sending synchronously by HTTP client \"" + getId() + "\" to host \""
                    + requestUriString + "\" failed", e);
        } finally {
            setIsConnected(false);
        }
        return response;
    }
    
    /**
     * Asynchronously sends a request based on the given parameters. This method does not block its caller, but informs
     * the given {@link HttpResponseCallback} when a response is received or the action terminates exceptionally.
     * 
     * @param requestMethod the {@link HttpRequestMethod} to use for the request; must not be <code>null</code>
     * @param requestUriString the URI identifying the target (server) to send the request to; must not be
     *        <code>null</code>
     * @param requestHeaders the headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}; may
     *        be <code>null</code>, if no headers are required
     * @param requestBody the body to add to the request; may be <code>null</code>, if no body is required
     * @param requestTimeout the timeout in milliseconds before a timeout exception is thrown, if the server does not
     *        respond
     * @param callback the instance to call back, if the target server responds or sending terminates exceptionally;
     *        must not be <code>null</code>
     * @throws NetworkException if the request method is unknown, the given URI is <code>null</code> or invalid, the
     *         headers are not <code>null</code> and invalid, the timeout is non-positive, the callback is
     *         <code>null</code>, or sending the request fails (errors after sending and while waiting for a response
     *         delivered to the given callback instance)
     */
    //checkstyle: stop parameter number check
    private void sendAsync(HttpRequestMethod requestMethod, String requestUriString, String[] requestHeaders,
            String requestBody, long requestTimeout, HttpResponseCallback callback) throws NetworkException {
        if (callback == null) {
            throw new NetworkException("Sending asynchronously by HTTP client \"" + getId() + "\" to host \""
                    + requestUriString + "\" failed: callback is \"null\"");
        }
        HttpRequest request = createHttpRequest(requestMethod, requestUriString, requestHeaders, requestBody,
                requestTimeout);
        try {
            setIsConnected(true);
            CompletableFuture<HttpResponse<String>> futureResponse = client.sendAsync(request, BodyHandlers.ofString());
            futureResponse.whenComplete((response, error) -> {
                NetworkException exception = null;
                if (error != null) {
                    exception = new NetworkException("Sending asynchronously by HTTP client \"" + getId()
                            + "\" to host \"" + requestUriString + "\" failed", error);
                }
                callback.responseArrived(response, exception);
                setIsConnected(false);
            });
        } catch (IllegalArgumentException e) {
            setIsConnected(false);
            throw new NetworkException("Sending asynchronously by HTTP client \"" + getId() + "\" to host \""
                    + requestUriString + "\" failed", e);
        }
    }
    //checkstyle: resume parameter number check
    
    /**
     * Creates a {@link HttpRequest} based on the given parameters.
     * 
     * @param requestMethod the {@link HttpRequestMethod} to use for the request; must not be <code>null</code>
     * @param requestUriString the URI identifying the target (server) to send the request to; must not be
     *        <code>null</code>
     * @param requestHeaders the headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}; may
     *        be <code>null</code>, if no headers are required
     * @param requestBody the body to add to the request; may be <code>null</code>, if no body is required
     * @param requestTimeout the timeout in milliseconds before a timeout exception is thrown, if the server does not
     *        respond
     * @return the created request
     * @throws NetworkException if the request method is unknown, the given URI is <code>null</code> or invalid, the
     *         headers are not <code>null</code> and invalid, the timeout is non-positive
     */
    private HttpRequest createHttpRequest(HttpRequestMethod requestMethod, String requestUriString,
            String[] requestHeaders, String requestBody, long requestTimeout) throws NetworkException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        // Set request URI (target server)
        try {            
            requestBuilder = requestBuilder.uri(URI.create(requestUriString));
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new NetworkException("Creating HTTP request URI from \"" + requestUriString + "\" failed", e);
        }
        // Set optional request headers
        if (requestHeaders != null) {
            try {            
                requestBuilder = requestBuilder.headers(requestHeaders);
            } catch (IllegalArgumentException e) {
                throw new NetworkException("Creating HTTP request header failed", e);
            }
        }
        // Create optional request body to be used for the specific request below
        BodyPublisher requestBodyPublisher = BodyPublishers.noBody();
        if (requestBody != null && !requestBody.isBlank()) {
            requestBodyPublisher = BodyPublishers.ofString(requestBody);
        }
        // Set request timeout
        try {
            requestBuilder = requestBuilder.timeout(Duration.ofMillis(requestTimeout));
        } catch (IllegalArgumentException e) {
            throw new NetworkException("Creating HTTP request timeout from \"" + requestTimeout + "\" failed", e);
        }
        // Set desired request method
        switch(requestMethod) {
        case GET:
            requestBuilder = requestBuilder.GET();
            break;
        case POST:
            requestBuilder = requestBuilder.POST(requestBodyPublisher);
            break;
        default:
            throw new NetworkException("Creating HTTP request failed: unknown request method \"" + requestMethod.name()
                + "\"");
        }
        // Build and return request
        return requestBuilder.build();
    }

    /**
     * The internal {@link java.net.http.HttpClient} cannot be closed explicitly. It automatically closes its
     * connection and releases associated resources after sending a request and receiving the corresponding response.
     * Hence, this method is deprecated as calling it has no effect.<br>
     * <br>
     * Calling {@link #isClosed()} for instances if this class always return <code>false</code>.
     */
    @Deprecated
    @Override
    public void close() throws NetworkException {}
    
    /**
     * Returns the information about this instance to be included in the textual representation provided by
     * {@link #toString()}. This information contains:
     * <ul>
     * <li>The identifier of this instance's parent class (simple class name)</li>
     * <li>The identifier of this instance</li>
     * <li>The HTTP version used by this instance</li>
     * <li>The redirect policy of this instance (how to follow redirects)</li>
     * <li>The information about the proxy used by this instance, if any</li>
     * <li>The user name used by this instance for authentication, if any</li>
     * <li>The password used by this instance for authentication, if any</li>
     * <li>The connection state of this instance</li>
     * <li>The open/close state of this instance</li>
     * </ul>
     */
    @Override
    protected String[][] getElementInfo() {
        String[][] clientInfo = {
            {ID},
            {"id", getId()},
            {"version", client.version().name()},
            {"redirect", client.followRedirects().name()},
            {"proxy", proxyInfo},
            {"username", getUsername()},
            {"password", getPassword()},
            {"connected", "" + isConnected()},
            {"closed", "" + isClosed()}
        };
        return clientInfo;
    }
    
}
