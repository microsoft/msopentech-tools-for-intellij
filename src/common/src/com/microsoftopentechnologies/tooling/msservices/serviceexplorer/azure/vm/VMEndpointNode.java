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

package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure.vm;

import com.microsoftopentechnologies.tooling.msservices.helpers.azure.AzureCmdException;
import com.microsoftopentechnologies.tooling.msservices.model.vm.Endpoint;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;

public class VMEndpointNode extends Node {
    public static final String ICON_PATH = "endpoint.png";
    protected Endpoint endpoint;

    public VMEndpointNode(Node parent, Endpoint endpoint) {
        super(endpoint.getName(), endpoint.getName(), parent, ICON_PATH, false);
        this.endpoint = endpoint;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // create child nodes for the protocol, public and private ports
        addChildNode(new Node(
                getName() + "_protocol",
                "Protocol: " + endpoint.getProtocol(),
                this, null, false));
        addChildNode(new Node(
                getName() + "_public_port",
                "Public Port: " + endpoint.getPublicPort(),
                this, null, false));
        addChildNode(new Node(
                getName() + "_private_port",
                "Private Port: " + endpoint.getPrivatePort(),
                this, null, false));
    }
}
