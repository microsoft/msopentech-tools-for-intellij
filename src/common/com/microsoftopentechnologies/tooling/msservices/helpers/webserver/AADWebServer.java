/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoftopentechnologies.tooling.msservices.helpers.webserver;

import com.google.common.io.ByteStreams;
import com.microsoftopentechnologies.tooling.msservices.helpers.EncodingHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.ServiceCodeReferenceHelper;
import com.microsoftopentechnologies.tooling.msservices.helpers.aadauth.AuthCodeCallback;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class AADWebServer extends WebServer {
    private static final String STATUS_SUCCESS = "success";
    private static final String AUTH_PATH = "/auth";
    private static final String CLOSED_PATH = "/closed";
    private static final String STATUS_PARAM_NAME = "status";
    private static final String DATA_PARAM_NAME = "data";
    private AuthCodeCallback authCodeCallback;
    private ClosedCallback closedCallback;
    private ReentrantLock requestHandlerLock = new ReentrantLock();

    public AADWebServer() throws IOException {
        super(-1);

        // setup request handlers
        get(AUTH_PATH, new AuthRequestHandler());
        get(CLOSED_PATH, new ClosedRequestHandler());
    }

    @Override
    public URL getBaseURL() throws MalformedURLException {
        return new URL(super.getBaseURL(), AUTH_PATH);
    }

    public void setAuthCodeCallback(AuthCodeCallback authCodeCallback) {
        this.authCodeCallback = authCodeCallback;
    }

    public void setClosedCallback(ClosedCallback closedCallback) {
        this.closedCallback = closedCallback;
    }

    class AuthRequestHandler implements HttpHandler {
        private String code;

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            requestHandlerLock.lock();

            try {
                Map<String, String> params = EncodingHelper.parseKeyValueList(
                        httpExchange.getRequestURI().getQuery(), '&', true);

                if(params == null ||
                   !params.containsKey(STATUS_PARAM_NAME) ||
                   !params.containsKey(DATA_PARAM_NAME) ||
                   !params.get(STATUS_PARAM_NAME).equals(STATUS_SUCCESS)) {

                    httpExchange.sendResponseHeaders(400, -1);
                    if(authCodeCallback != null) {
                        authCodeCallback.onAuthCodeReceived(null, params);
                    }
                    return;
                }

                code = params.get(DATA_PARAM_NAME);

                // setup response headers
                Headers headers = httpExchange.getResponseHeaders();
                headers.add("Content-Type", "text/html");
                httpExchange.sendResponseHeaders(200, 0);

                // send browser response
                try {
                    InputStream input = ServiceCodeReferenceHelper.getTemplateResource("auth-response.html");
                    OutputStream output = httpExchange.getResponseBody();
                    ByteStreams.copy(input, output);
                    output.close();
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // raise the notification for the code
                if(authCodeCallback != null) {
                    authCodeCallback.onAuthCodeReceived(code, params);
                }
            } finally {
                requestHandlerLock.unlock();
            }
        }

        public String getCode() {
            return code;
        }
    }

    class ClosedRequestHandler implements  HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            requestHandlerLock.lock();

            try {
                if(closedCallback != null) {
                    closedCallback.onClosed();
                }

                httpExchange.sendResponseHeaders(200, -1);
            } finally {
                requestHandlerLock.unlock();
            }
        }
    }
}
