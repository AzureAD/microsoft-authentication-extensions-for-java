// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

public class KeyChainWriter {

    public static void main(String[] args) throws Exception {
        String filePath;
        String lockFilePath;
        String executionId;

        executionId = args[0];
        lockFilePath = args[1];
        filePath = args[2];

        String serviceName = args[3];
        String accountName = args[4];

        try {
            KeyChainWriterRunnable keyChainWriterRunnable =
                    new KeyChainWriterRunnable(executionId, lockFilePath, filePath, serviceName, accountName);

            keyChainWriterRunnable.run();
            System.out.println("executionId - " + executionId + " SUCCESS");
        }
        catch (Throwable e){
            System.out.println("executionId - " + executionId + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> FAILURE <<<<<<<<<<<<<<<<<<<<<<<<<");
            System.out.println(e.getMessage());
        }
    }
}
