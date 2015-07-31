/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoftopentechnologies.tooling.msservices.serviceexplorer.azure;

import com.microsoftopentechnologies.tooling.msservices.components.DefaultLoader;
import com.microsoftopentechnologies.tooling.msservices.helpers.NotNull;
import com.microsoftopentechnologies.tooling.msservices.serviceexplorer.Node;

import javax.swing.*;
import java.util.concurrent.Callable;

public abstract class AzureNodeActionPromptListener extends AzureNodeActionListener {
    private String promptMessage;
    private int optionDialog;

    public AzureNodeActionPromptListener(@NotNull Node azureNode,
                                         @NotNull String promptMessage,
                                         @NotNull String progressMessage) {
        super(azureNode, progressMessage);
        this.promptMessage = promptMessage;
    }

    @NotNull
    @Override
    protected Callable<Boolean> beforeAsyncActionPerfomed() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                DefaultLoader.getIdeHelper().invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        optionDialog = JOptionPane.showOptionDialog(null,
                                promptMessage,
                                "Service explorer",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                new String[]{"Yes", "No"},
                                null);
                    }
                });

                return (optionDialog == JOptionPane.YES_OPTION);
            }
        };
    }
}