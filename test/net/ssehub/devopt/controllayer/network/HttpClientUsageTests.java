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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.opentest4j.TestAbortedException;

/**
 * This class contains unit tests for the usage of a {@link HttpClient} instance. In particular, these tests focus on
 * sending messages to a server and receiving its response. For testing the creation of client instances, see
 * {@link HttpClientCreationTests}.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class HttpClientUsageTests implements HttpResponseCallback {
    
    /**
     * The constant identifier for the client used in these tests.
     */
    private static final String TEST_CLIENT_ID = "TestClient";
    
    /**
     * The constant host URI to send test HTTP requests to.
     */
    private static final String TEST_HOST_URI = "https://httpbin.org/";
    
    /**
     * The constant host URI to send test HTTP GET requests to.
     */
    private static final String TEST_GET_HOST_URI = TEST_HOST_URI + "get";
    
    /**
     * The constant host URI to send test HTTP POST requests to.
     */
    private static final String TEST_POST_HOST_URI = TEST_HOST_URI + "post";
    
    /**
     * The constant number defining that no value for a timeout is available.
     */
    private static final long NO_TIMEOUT = -1;
    
    /**
     * The constant number defining that no value for a HTTP response code is available.
     */
    private static final int NO_RESPONSE_CODE = -1;
    
    /**
     * The constant number defining that no value for a HTTP body length is available.
     */
    private static final int NO_BODY_LENGTH = -1;
    
    /**
     * The constant number defining the time in milliseconds to wait for a response of an asynchronous request.
     */
    private static final long DEFAULT_ASYNC_RESPONSE_TIMEOUT = 2000;
    
    /**
     * The constant number defining the time in milliseconds to pause this thread while waiting for a response of an
     * asynchronous request.
     */
    private static final long DEFAULT_ASYNC_RESPONSE_SLEEP = 500;
    
    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent the expected attribute
     * values of a client instance, the send request, and received response created during the respective test iteration
     * For each subset, the values represent:
     * <ul>
     * <li>The preferred HTTP protocol version for creating a new client instance; this value is also set as
     *     {@link #expectedResponseVersion}</li>
     * <li>The redirect policy for creating a new client instance</li>
     * <li>The URL or IP of the proxy for creating a new client instance</li>
     * <li>The port of the proxy for creating a new client instance</li>
     * <li>The user name for creating a new client instance</li>
     * <li>The password for creating a new client instance</li>
     * <li>The HTTP request method to use for sending the request for testing; this value is also set as
     *     {@link #expectedRequestMethod}</li>
     * <li>The definition of whether to send the request synchronously (<code>true</code>) or asynchronously
     *     (<code>false</code>)</li>
     * <li>The headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}</li>
     * <li>The body to add to the request; the length of this value is also set as {@link #expectedRequestBodyLength},
     *     if the value is not <code>null</code></li>
     * <li>The timeout in milliseconds before a timeout exception is thrown, if the server does not respond; this value
     *     is also set as {@link #expectedRequestTimeout}</li>
     * <li>The expected HTTP response code of the received response</li>
     * <li>The expected content of the response body; in case of large content, this string may only contain a subset of
     *     the expected body</li>
     * <li>The expected detail message of the top-level exception thrown during sending a request</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {Version.HTTP_2, null, null, -1, null, null, HttpRequestMethod.GET, true, null, null, 1000, 200,
                "\"url\": \"https://httpbin.org/get\"", null},
            
            {Version.HTTP_1_1, null, null, -1, null, null, HttpRequestMethod.GET, true, null, null, 1000, 200,
                "\"url\": \"https://httpbin.org/get\"", null},
            
            {Version.HTTP_2, null, null, -1, null, null, HttpRequestMethod.GET, false, null, null, 1000, 200,
                "\"url\": \"https://httpbin.org/get\"", null},
            
            {Version.HTTP_2, null, null, -1, null, null, HttpRequestMethod.POST, true,
                new String[] {"Content-Type", "application/x-www-form-urlencoded"}, "username=abc&password=123",
                1000, 200, "\"url\": \"https://httpbin.org/post\"", null},
            
            {Version.HTTP_2, null, null, -1, null, null, HttpRequestMethod.POST, false,
                new String[] {"Content-Type", "application/x-www-form-urlencoded"}, "username=abc&password=123",
                1000, 200, "\"url\": \"https://httpbin.org/post\"", null}
    };
    
    /**
     * The definition of whether a response of an asynchronous request has arrived (<code>true</code>) or not
     * (<code>false</code>).
     */
    private boolean asyncResponseArrived;
    
    /**
     * The expected host URI set for the send request.
     */
    private String expectedRequestUri;
    
    /**
     * The expected HTTP request method used to send the request, e.g. <code>GET</code>, <code>POST</code>. 
     */
    private String expectedRequestMethod;
    
    /**
     * The expected number defining the time in milliseconds to wait for accepting the request.
     */
    private long expectedRequestTimeout;
    
    /**
     * The expected number defining the length of the request body. As there is no direct access to the content of the
     * request body via the received response, the only check regarding the request body content is to compare the
     * expected and its actual length.
     */
    private long expectedRequestBodyLength;
    
    /**
     * The expected host URI set for the received response.
     */
    private String expectedResponseUri;
    
    /**
     * The expected HTTP version of the received response, e.g. <code>.HTTP_2</code> or <code>HTTP_1_1</code>.
     */
    private String expectedResponseVersion;
    
    /**
     * The expected HTTP response code of the received response.
     */
    private int expectedResponseCode;
    
    /**
     * The expected content of the response body. In case of large content, this string may only contain a subset of the
     * expected body. Hence, tests using this attribute must accept partial equivalence with an actual response body.
     */
    private String expectedResponseBody;

    /**
     * The expected detail message of the top-level exception thrown during sending a request. 
     */
    private String expectedExceptionMessage;
    
    /**
     * The actual host URI set for the send request.
     */
    private String actualRequestUri;
    
    /**
     * The actual HTTP request method used to send the request, e.g. <code>GET</code>, <code>POST</code>. 
     */
    private String actualRequestMethod;
    
    /**
     * The actual number defining the time in milliseconds to wait for accepting the request.
     */
    private long actualRequestTimeout;
    
    /**
     * The actual number defining the length of the request body. As there is no direct access to the content of the
     * request body via the received response, the only check regarding the request body content is to compare the
     * expected and its actual length.
     */
    private long actualRequestBodyLength; 
    
    /**
     * The actual host URI set for the received response.
     */
    private String actualResponseUri;
    
    /**
     * The actual HTTP version of the received response, e.g. <code>.HTTP_2</code> or <code>HTTP_1_1</code>.
     */
    private String actualResponseVersion;
    
    /**
     * The actual HTTP response code of the received response.
     */
    private int actualResponseCode;
    
    /**
     * The actual content of the response body as received from the server.
     */
    private String actualResponseBody;
    
    /**
     * The actual exception thrown during sending a request. 
     */
    private NetworkException actualException;
    
    /**
     * Constructs a new {@link HttpClientUsageTests} instance for sending HTTP request to a server based on the given
     * parameters.
     * 
     * @param httpVersion the preferred HTTP protocol version for creating a new client instance; this value is also set
     *        as {@link #expectedResponseVersion}
     * @param httpRedirect the redirect policy for creating a new client instance
     * @param proxyUrl the URL or IP of the proxy for creating a new client instance
     * @param proxyPort the port of the proxy for creating a new client instance
     * @param username the user name for creating a new client instance
     * @param password the password for creating a new client instance
     * @param httpRequestMethod the HTTP request method to use for sending the request for testing; this value is also
     *        set as {@link #expectedRequestMethod}
     * @param sendSync the definition of whether to send the request synchronously (<code>true</code>) or asynchronously
     *        (<code>false</code>)
     * @param requestHeaders the headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}
     * @param requestBody the body to add to the request; the length of this value is also set as
     *        {@link #expectedRequestBodyLength}, if the value is not <code>null</code>
     * @param requestTimeout the timeout in milliseconds before a timeout exception is thrown, if the server does not
     *        respond; this value is also set as {@link #expectedRequestTimeout}
     * @param expectedResponseCode the expected HTTP response code of the received response
     * @param expectedResponseBody the expected content of the response body; in case of large content, this string may
     *        only contain a subset of the expected body
     * @param expectedExceptionMessage the expected detail message of the top-level exception thrown during sending a
     *        request
     */
    //checkstyle: stop parameter number check
    public HttpClientUsageTests(Version httpVersion, Redirect httpRedirect, String proxyUrl, int proxyPort,
            String username, String password, HttpRequestMethod httpRequestMethod, boolean sendSync,
            String[] requestHeaders, String requestBody, long requestTimeout, int expectedResponseCode,
            String expectedResponseBody, String expectedExceptionMessage) {
        asyncResponseArrived = false;
        expectedResponseVersion = httpVersion.name();
        this.expectedResponseCode = expectedResponseCode;
        this.expectedResponseBody = expectedResponseBody;
        this.expectedExceptionMessage = expectedExceptionMessage;
        try {
            HttpClient client = new HttpClient(TEST_CLIENT_ID, httpVersion, httpRedirect, proxyUrl, proxyPort, username,
                    password);
            sendRequest(client, httpRequestMethod, sendSync, requestHeaders, requestBody, requestTimeout);
        } catch (NetworkException e) {
            actualException = e;
        }
    }
    //checkstyle: resume parameter number check
    
    /**
     * Sends a HTTP request using the given client and parameters.
     * 
     * @param client the client instance to use for sending the request
     * @param httpRequestMethod the HTTP request method to use for sending the request for testing; this value is also
     *        set as {@link #expectedRequestMethod}
     * @param sendSync the definition of whether to send the request synchronously (<code>true</code>) or asynchronously
     *        (<code>false</code>)
     * @param requestHeaders the headers to add to the request (see {@link HttpRequest.Builder#headers(String...)}
     * @param requestBody the body to add to the request; the length of this value is also set as
     *        {@link #expectedRequestBodyLength}, if the value is not <code>null</code>
     * @param requestTimeout the timeout in milliseconds before a timeout exception is thrown, if the server does not
     *        respond; this value is also set as {@link #expectedRequestTimeout}
     * @throws NetworkException if sending the request fails
     */
    //checkstyle: stop parameter number check
    private void sendRequest(HttpClient client, HttpRequestMethod httpRequestMethod, boolean sendSync,
            String[] requestHeaders, String requestBody, long requestTimeout) throws NetworkException {
        switch(httpRequestMethod) {
        case GET:
            expectedRequestUri = TEST_GET_HOST_URI;
            if (sendSync) {
                setActualResponseValues(client.sendGetSync(expectedRequestUri, requestHeaders, requestTimeout));
            } else {
                // callback (this) sets actual response values in responseArrived(HttpResponse, NetworkException
                client.sendGetAsync(expectedRequestUri, requestHeaders, requestTimeout, this);
                waitForAsyncResponse();
            }
            break;
        case POST:
            expectedRequestUri = TEST_POST_HOST_URI;
            if (sendSync) {
                setActualResponseValues(client.sendPostSync(expectedRequestUri, requestHeaders, requestBody,
                        requestTimeout));
            } else {
                // callback (this) sets actual response values in responseArrived(HttpResponse, NetworkException
                client.sendPostAsync(expectedRequestUri, requestHeaders, requestBody, requestTimeout, this);
                waitForAsyncResponse();
            }
            break;
        default:
            throw new TestAbortedException("Unsupported HTTP request method \"" + httpRequestMethod.name() + "\"");
        }
        expectedRequestMethod = httpRequestMethod.name();
        expectedRequestTimeout = requestTimeout;
        if (requestBody != null) {            
            expectedRequestBodyLength = requestBody.length();
        } else {
            expectedRequestBodyLength = NO_BODY_LENGTH;
        }
        expectedResponseUri = expectedRequestUri;
    }
    //checkstyle: resume parameter number check

    /**
     * Calls {@link Thread#sleep(long)} with {@link #DEFAULT_ASYNC_RESPONSE_SLEEP} as long as
     * {@link #asyncResponseArrived} is <code>false</code> and {@link #DEFAULT_ASYNC_RESPONSE_TIMEOUT} is not reached.
     * This method is used to postpone the execution of any tests, if a request is send asynchronously. In this case,
     * the required response for testing arrives later via {@link #responseArrived(HttpResponse, NetworkException)}. 
     */
    private void waitForAsyncResponse() {
        long currentTimeMillis = System.currentTimeMillis();
        long endTimeMillis = currentTimeMillis + DEFAULT_ASYNC_RESPONSE_TIMEOUT;
        while (!asyncResponseArrived && currentTimeMillis < endTimeMillis) {
            try {
                Thread.sleep(DEFAULT_ASYNC_RESPONSE_SLEEP);
                currentTimeMillis = System.currentTimeMillis();
            } catch (InterruptedException e) {
                System.err.println("Waiting for async response failed: " + e.getMessage());
            }
        }
    }

    /**
     * Sets the actual request and response data as received from a server as part of the given response.
     * 
     * @param actualResponse the response of the send request as received by a server
     */
    private void setActualResponseValues(HttpResponse<String> actualResponse) {
        if (actualResponse == null) {
            actualRequestUri = null;
            actualRequestMethod = null;
            actualRequestTimeout = NO_TIMEOUT;
            actualRequestBodyLength = NO_BODY_LENGTH;
            
            actualResponseUri = null;
            actualResponseVersion = null;
            actualResponseCode = NO_RESPONSE_CODE;
            actualResponseBody = null;
        } else {
            java.net.http.HttpRequest request = actualResponse.request();
            actualRequestUri = request.uri().toString();
            actualRequestMethod = request.method();
            try {                
                actualRequestTimeout = request.timeout().get().getSeconds() * 1000; // Timeout is set in milliseconds
            } catch (NoSuchElementException e) {
                actualRequestTimeout = NO_TIMEOUT;
                System.err.println("Setting actual request timeout failed: " + e.getMessage());
            }
            try {                
                actualRequestBodyLength = request.bodyPublisher().get().contentLength();
            } catch (NoSuchElementException e) {
                actualRequestBodyLength = NO_BODY_LENGTH;
                System.err.println("Setting actual request body failed: " + e.getMessage());
            }
            
            actualResponseUri = actualResponse.uri().toString();
            actualResponseVersion = actualResponse.version().name();            
            actualResponseCode = actualResponse.statusCode();
            actualResponseBody = actualResponse.body();
        }
    }
    
    /**
     * Receives the server response to an asynchronous HTTP request send via an instance of a {@link HttpClient}.
     * 
     * @param response the string-based response provided by the server or <code>null</code>, if the server does not
     *        provide a response and also does not cause an exception 
     * @param exception the error occurred during processing the request by the server; may be <code>null</code> 
     */
    @Override
    public void responseArrived(HttpResponse<String> response, NetworkException exception) {
        asyncResponseArrived = true;
        setActualResponseValues(response);
        actualException = exception;
    }
    
    /**
     * Returns the {@link #TEST_DATA} for the following tests in this class.
     * 
     * @return the {@link #TEST_DATA}
     */
    @Parameters
    public static Object[][] getTestData() {
        return TEST_DATA;
    }
    
    /**
     * Tests whether the {@link #expectedRequestUri} is equal to the {@link #actualRequestUri}.
     */
    @Test
    public void testCorrectRequestUri() {
        assertEquals(expectedRequestUri, actualRequestUri, "Wrong request URI");
    }
    
    /**
     * Tests whether the {@link #expectedRequestMethod} is equal to the {@link #actualRequestMethod}.
     */
    @Test
    public void testCorrectRequestMethod() {
        assertEquals(expectedRequestMethod, actualRequestMethod, "Wrong request method");
    }
    
    /**
     * Tests whether the {@link #expectedRequestTimeout} is equal to the {@link #actualRequestTimeout}.
     */
    @Test
    public void testCorrectRequestTimeout() {
        assertEquals(expectedRequestTimeout, actualRequestTimeout, "Wrong request timeout");
    }
    
    /**
     * Tests whether the {@link #expectedRequestBodyLength} is equal to the {@link #actualRequestBodyLength}.
     */
    @Test
    public void testCorrectRequestBodyLength() {
        assertEquals(expectedRequestBodyLength, actualRequestBodyLength, "Wrong request body length");
    }

    /**
     * Tests whether the {@link #expectedResponseUri} is equal to the {@link #actualResponseUri}.
     */
    @Test
    public void testCorrectResponseUri() {
        assertEquals(expectedResponseUri, actualResponseUri, "Wrong response URI");
    }
    
    /**
     * Tests whether the {@link #expectedResponseVersion} is equal to the {@link #actualResponseVersion}.
     */
    @Test
    public void testCorrectResponseVersion() {
        assertEquals(expectedResponseVersion, actualResponseVersion, "Wrong response version");
    }
    
    /**
     * Tests whether the {@link #expectedResponseCode} is equal to the {@link #actualResponseCode}.
     */
    @Test
    public void testCorrectResponseCode() {
        assertEquals(expectedResponseCode, actualResponseCode, "Wrong response code");
    }
    
    /**
     * Tests the correctness of the {@link #actualResponseBody}. This body is correct, if:
     * <ul>
     * <li>the {@link #expectedResponseBody} and the actual one are both <code>null</code></li>
     * <li>the {@link #expectedResponseBody} is not <code>null</code> and:</li>
     *     <ul>
     *     <li>either the actual one is completely equal</li>
     *     <li>or the actual one contains the expected one (partial equivalence in case of large content)</li>
     *     </ul>
     * </ul>
     */
    @Test
    public void testCorrectResponseBody() {
        if (expectedResponseBody == null) {            
            assertEquals(expectedResponseBody, actualResponseBody, "Expected no response body");
        } else {
            try {
                if (!actualResponseBody.equals(expectedResponseBody)
                        && !actualResponseBody.contains(expectedResponseBody)) {
                    assertEquals(expectedResponseBody, actualResponseBody,
                            "Actual response body neither equals nor contains expected one");
                }
            } catch (NullPointerException e) {
                assertNotNull(actualResponseBody, "Actual response body must not be null but: \"" 
                        + expectedResponseBody + "\"");
            }
        }
            
    }
    
    /**
     * Tests the correctness of the {@link #actualException}. This exception is correct, if:
     * <ul>
     * <li>the {@link #expectedExceptionMessage} and the actual exception are both <code>null</code></li>
     * <li>the {@link #expectedExceptionMessage} is not <code>null</code> and the detail message of the actual exception
     *     are equal</li>
     * </ul>
     */
    @Test
    public void testCorrectException() {
        if (expectedExceptionMessage == null) {
            assertEquals(expectedExceptionMessage, actualException, "Expected no exception");
        } else {
            try {
                assertEquals(expectedExceptionMessage, actualException.getMessage(), "Wrong exception message");
            } catch (NullPointerException e) {
                assertNotNull(actualException, "Actual exception must not be null");
            }
        }
    }
    
}
