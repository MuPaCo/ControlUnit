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

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sun.net.httpserver.HttpContext;

/**
 * This class contains unit tests for the usage of a {@link HttpServer} instance. In particular, these tests focus on
 * receiving messages from a client and sending appropriated responses. For testing the creation of server instances,
 * see {@link HttpServerCreationTests}.
 * 
 * @author kroeher
 *
 */
@RunWith(Parameterized.class)
public class HttpServerUsageTests {
    
    /**
     * The identifier of this class, e.g., for exception messages. 
     */
    private static final String ID = HttpServerUsageTests.class.getSimpleName();
    
    /**
     * The constant identifier of the server used in these tests.
     */
    private static final String TEST_SERVER_ID = "TestServer";
    
    /**
     * The constant address of the server used in these tests.
     */
    private static final String TEST_SERVER_ADDRESS = "127.0.0.1";
    
    /**
     * The constant URI of the server used in these tests. This is the {@link #TEST_SERVER_ADDRESS} prefixed by
     * "http://". 
     */
    private static final String TEST_SERVER_URI = "http://" + TEST_SERVER_ADDRESS;
    
    /**
     * The constant number defining the port to bind the server instance socket to.
     */
    private static final int TEST_SERVER_PORT = 80;
    
    /**
     * The constant number defining the backlog of the server used in these test. The backlog is the maximum number of
     * incoming connections stored before the server instance refuses incoming connections.
     */
    private static final int TEST_SERVER_BACKLOG = 1;
    
    /**
     * The constant set of paths defining the different contexts the server instance supports. For each of these paths,
     * the {@link #setup()} creates a new {@link HttpContext} and associates a new {@link HttpRequestCallbackHandler}
     * with it, which handles requests send to the respective path.
     */
    private static final String[] TEST_SERVER_CONTEXT_PATHS = {"/ctx1", "/ctx2", "/ctx3-1/ctx3-2"};
    
    /**
     * The constant number defining the HTTP "Not Found" response code.
     */
    private static final int NO_CONTEXT_RESPONSE_CODE = 404;
    
    /**
     * The constant response body send in case of a HTTP "Not Found" client error.
     */
    private static final String NO_CONTEXT_RESPONSE_BODY = "<h1>404 Not Found</h1>No context found for request";
    
    /**
     * The set of test value sets used to execute the tests in this class. Each subset is input to the constructor of
     * this class (exactly once and performed by JUnit). Hence, the values of a subset represent the client's request
     * configuration as well as the expected server response. For each subset, the values represent:
     * <ul>
     * <li>The {@link HttpRequestMethod} to use for the request to send</li>
     * <li>The URI identifying the target (server) to send the request to</li>
     * <li>The body to add to the request to send</li>
     * <li>The timeout of the request to send in milliseconds before a timeout exception is thrown, if the server does
     *     not respond</li>
     * <li>The expected HTTP response code send by the server for the build request</li>
     * <li>The expected response body send by the server for the build request</li>
     * <li>The expected detail message of the top-level exception thrown during sending the build request</li>
     * </ul>
     */
    private static final Object[][] TEST_DATA = {
            {HttpRequestMethod.GET, TEST_SERVER_URI, null, 1000,
                NO_CONTEXT_RESPONSE_CODE, NO_CONTEXT_RESPONSE_BODY, null},

            {HttpRequestMethod.GET, TEST_SERVER_URI + TEST_SERVER_CONTEXT_PATHS[0], null, 1000,
                200, "method=GET;uri=/ctx1;body=;", null},
            
            {HttpRequestMethod.GET, TEST_SERVER_URI + TEST_SERVER_CONTEXT_PATHS[1], null, 1000,
                200, "method=GET;uri=/ctx2;body=;", null},
            
            {HttpRequestMethod.GET, TEST_SERVER_URI + TEST_SERVER_CONTEXT_PATHS[2], null, 1000,
                200, "method=GET;uri=/ctx3-1/ctx3-2;body=;", null},
            
            {HttpRequestMethod.POST, TEST_SERVER_URI, null, 1000,
                NO_CONTEXT_RESPONSE_CODE, NO_CONTEXT_RESPONSE_BODY, null},

            {HttpRequestMethod.POST, TEST_SERVER_URI + TEST_SERVER_CONTEXT_PATHS[0], "post-body-content", 1000,
                200, "method=GET;uri=/ctx1;body=post-body-content;", null},
            
            {HttpRequestMethod.POST, TEST_SERVER_URI + TEST_SERVER_CONTEXT_PATHS[1], "post-body-content", 1000,
                200, "method=GET;uri=/ctx2;body=post-body-content;", null},
            
            {HttpRequestMethod.POST, TEST_SERVER_URI + TEST_SERVER_CONTEXT_PATHS[2], "post-body-content", 1000,
                200, "method=GET;uri=/ctx3-1/ctx3-2;body=post-body-content;", null},            
    };
    
    /**
     * The server instance used in these tests. This instance is created and started during {@link #setup()} before any
     * tests are executed. It remains active until {@link #teardown()} is called after all tests are executed.
     */
    private static HttpServer server;
    
    /**
     * The client instance used in these tests. This instance is created during {@link #setup()} before any tests are
     * executed. It is used to send requests to the {@link #server} during instantiation of a new 
     * {@link #HttpServerUsageTests(HttpRequestMethod, String, String, int, int, String, String)} instance.
     */
    private static HttpClient client;

    /**
     * The expected HTTP response code send by the server for a specific request.
     */
    private int expectedServerResponseCode;
    
    /**
     * The expected response body send by the server for a specific request.
     */
    private String expectedServerResponseBody;
    
    /**
     * The expected detail message of the top-level exception thrown during sending a request. 
     */
    private String expectedExceptionMessage;
    
    /**
     * The actual HTTP response code received from the server for a specific request.
     */
    private int actualServerResponseCode;
    
    /**
     * The actual response body received from the server for a specific request.
     */
    private String actualServerResponseBody;
    
    /**
     * The actual detail message of the top-level exception thrown during sending a request. 
     */
    private String actualExceptionMessage;
    
    /**
     * Constructs a new {@link HttpServerUsageTests} instance for receiving HTTP request build from the given parameters
     * and responding to them.
     * 
     * @param requestMethod the {@link HttpRequestMethod} to use for the request to send
     * @param hostUri the URI identifying the target (server) to send the request to
     * @param requestBody the body to add to the request to send
     * @param requestTimeout the timeout of the request to send in milliseconds before a timeout exception is thrown, if
     *        the server does not respond
     * @param expectedServerResponseCode the expected HTTP response code send by the server for the build request
     * @param expectedServerResponseBody the expected response body send by the server for the build request
     * @param expectedExceptionMessage the expected detail message of the top-level exception thrown during sending the
     *        build request
     */
    //checkstyle: stop parameter number check
    public HttpServerUsageTests(HttpRequestMethod requestMethod, String hostUri, String requestBody, int requestTimeout,
            int expectedServerResponseCode, String expectedServerResponseBody, String expectedExceptionMessage) {
        this.expectedServerResponseCode = expectedServerResponseCode;
        this.expectedServerResponseBody = expectedServerResponseBody;
        this.expectedExceptionMessage = expectedExceptionMessage;
        
        java.net.http.HttpResponse<String> response = null;
        try {            
            switch(requestMethod) {
            case GET:
                response = client.sendGetSync(hostUri, null, requestTimeout);
                break;
            case POST:
                response = client.sendPostSync(hostUri, null, requestBody, requestTimeout);
                break;
            default:
                throw new NetworkException("Unsupported request method: " + requestMethod);
            }
        } catch (NetworkException e) {
            actualExceptionMessage = e.getMessage();
        }
        
        if (response != null) {
            actualServerResponseCode = response.statusCode();
            actualServerResponseBody = response.body();
        } else {
            actualServerResponseCode = -1;
            actualServerResponseBody = null;
        }
    }
    //checkstyle: resume parameter number check
    
    /**
     * Creates the {@link #server} and {@link #client} instances used in these tests. For the server, it also creates a
     * new {@link HttpContext} for each path in {@link #TEST_SERVER_CONTEXT_PATHS} and associates a new
     * {@link HttpRequestCallbackHandler} with it, which handles requests send to the respective path. Further, it
     * starts the server. 
     */
    @BeforeClass
    public static void setup() {
        if (server == null) {
            try {
                server = new HttpServer(TEST_SERVER_ID, TEST_SERVER_ADDRESS, TEST_SERVER_PORT, TEST_SERVER_BACKLOG);
                for (int i = 0; i < TEST_SERVER_CONTEXT_PATHS.length; i++) {
                    server.addContext(TEST_SERVER_CONTEXT_PATHS[i],
                            new HttpRequestCallbackHandler(TEST_SERVER_CONTEXT_PATHS[i]));
                }
                if (!server.start()) {
                    fail("Starting HTTP server instance for " + ID + " failed");
                }
            } catch (NetworkException e) {
                e.printStackTrace();
                fail("Creating HTTP server instance for " + ID + " failed: see printed stack trace");
            }
        }
        if (client == null) {
            try {
                client = new HttpClient("TestClient", null, null, null, -1, null, null);
            } catch (NetworkException e) {
                e.printStackTrace();
                fail("Creating HTTP client instance for " + ID + " failed: see printed stack trace");
            }
        }
    }
    
    /**
     * Stops the {@link #server}.
     */
    @AfterClass
    public static void teardown() {
        if (server != null) {
            if (!server.stop(5)) {
                fail("Stopping HTTP server instance for " + ID + " failed");
            }
        }
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
     * Tests whether the {@link #expectedServerResponseCode} is equal to the {@link #actualServerResponseCode}.
     */
    @Test
    public void testCorrectResponseCode() {
        assertEquals(expectedServerResponseCode, actualServerResponseCode, "Wrong response code");
    }
    
    /**
     * Tests whether the {@link #expectedServerResponseBody} is equal to the {@link #actualServerResponseBody}.
     */
    @Test
    public void testCorrectResponseBody() {
        assertEquals(expectedServerResponseBody, actualServerResponseBody, "Wrong response body");
    }
    
    /**
     * Tests whether the {@link #expectedExceptionMessage} is equal to the {@link #actualExceptionMessage}.
     */
    @Test
    public void testCorrectExceptionMessage() {
        assertEquals(expectedExceptionMessage, actualExceptionMessage, "Wrong excpetion message");
    }
    
}
