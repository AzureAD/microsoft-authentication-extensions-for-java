// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.mac;

public class KeyChainAccessException extends RuntimeException {
    KeyChainAccessException(String message) {
        super(message);
    }
}
