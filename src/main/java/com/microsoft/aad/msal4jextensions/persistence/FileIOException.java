// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence;

public class FileIOException extends RuntimeException {

    FileIOException(String message) {
        super(message);
    }

    FileIOException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
