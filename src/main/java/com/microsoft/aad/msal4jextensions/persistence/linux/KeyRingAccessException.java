// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.linux;

public class KeyRingAccessException extends RuntimeException {
    KeyRingAccessException(String message) {
        super(message);
    }

    KeyRingAccessException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
