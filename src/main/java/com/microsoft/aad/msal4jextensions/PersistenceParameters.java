// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

public class PersistenceParameters {

    private String cacheFilePath;
    private String cacheLockFilePath;

    private String keychainService;
    private String keychainAccount;

    private String keyringCollection;
    private String keyringSchemaName;
    private String keyringSecretLabel;
    private String keyringAttributeKey1;
    private String keyringAttributeValue1;
    private String keyringAttributeKey2;
    private String keyringAttributeValue2;

    public PersistenceParameters(String cacheFilePath,
                                 String cacheLockFilePath,
                                 String keychainService,
                                 String keychainAccount,
                                 String keyringCollection,
                                 String keyringSchemaName,
                                 String keyringSecretLabel,
                                 String keyringAttributeKey1,
                                 String keyringAttributeValue1,
                                 String keyringAttributeKey2,
                                 String keyringAttributeValue2) {

        this.cacheFilePath = cacheFilePath;
        this.cacheLockFilePath = cacheLockFilePath;
        this.keychainService = keychainService;
        this.keychainAccount = keychainAccount;
        this.keyringCollection = keyringCollection;
        this.keyringSchemaName = keyringSchemaName;
        this.keyringSecretLabel = keyringSecretLabel;
        this.keyringAttributeKey1 = keyringAttributeKey1;
        this.keyringAttributeValue1 = keyringAttributeValue1;
        this.keyringAttributeKey2 = keyringAttributeKey2;
        this.keyringAttributeValue2 = keyringAttributeValue2;
    }

    public String getCacheFilePath() {
        return cacheFilePath;
    }

    public String getCacheLockFilePath() {
        return cacheLockFilePath;
    }

    public String getKeychainService() {
        return keychainService;
    }

    public String getKeychainAccount() {
        return keychainAccount;
    }

    public String getKeyringCollection() {
        return keyringCollection;
    }

    public String getKeyringSchemaName() {
        return keyringSchemaName;
    }

    public String getKeyringSecretLabel() {
        return keyringSecretLabel;
    }

    public String getKeyringAttributeKey1() {
        return keyringAttributeKey1;
    }

    public String getKeyringAttributeValue1() {
        return keyringAttributeValue1;
    }

    public String getKeyringAttributeKey2() {
        return keyringAttributeKey2;
    }

    public String getKeyringAttributeValue2() {
        return keyringAttributeValue2;
    }

    private static void validateArgument(String parameter, String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(parameter + " null or Empty");
        }
    }

    public static class Builder {

        private String cacheFilePath;
        private String cacheLockFilePath;

        private String keychainService;
        private String keychainAccount;

        private String keyringCollection;
        private String keyringSchemaName;
        private String keyringSecretLabel;
        private String keyringAttributeKey1;
        private String keyringAttributeValue1;
        private String keyringAttributeKey2;
        private String keyringAttributeValue2;

        public Builder(String cacheFilePath, String cacheLockFilePath) {
            validateArgument("cacheFilePath", cacheFilePath);
            validateArgument("cacheLockFilePath", cacheLockFilePath);

            this.cacheFilePath = cacheFilePath;
            this.cacheLockFilePath = cacheLockFilePath;
        }

        public Builder setKeychainService(String keychainService) {
            validateArgument("keychainService", keychainService);

            this.keychainService = keychainService;
            return this;
        }

        public Builder setKeychainAccount(String keychainAccount) {
            validateArgument("keychainAccount", keychainAccount);

            this.keychainAccount = keychainAccount;
            return this;
        }

        public Builder setKeyringCollection(String keyringCollection) {
            validateArgument("keyringCollection", keyringCollection);

            this.keyringCollection = keyringCollection;
            return this;
        }

        public Builder setKeyringSchemaName(String keyringSchemaName) {
            validateArgument("keyringSchemaName", keyringSchemaName);

            this.keyringSchemaName = keyringSchemaName;
            return this;
        }

        public Builder setKeyringSecretLabel(String keyringSecretLabel) {
            validateArgument("keyringSecretLabel", keyringSecretLabel);

            this.keyringSecretLabel = keyringSecretLabel;
            return this;
        }

        public Builder setKeyringAttributeKey1(String keyringAttributeKey1) {
            validateArgument("keyringAttributeKey1", keyringAttributeKey1);

            this.keyringAttributeKey1 = keyringAttributeKey1;
            return this;
        }

        public Builder setKeyringAttributeValue1(String keyringAttributeValue1) {
            validateArgument("keyringAttributeValue1", keyringAttributeValue1);

            this.keyringAttributeValue1 = keyringAttributeValue1;
            return this;
        }

        public Builder setKeyringAttributeKey2(String keyringAttributeKey2) {
            validateArgument("keyringAttributeKey2", keyringAttributeKey2);

            this.keyringAttributeKey2 = keyringAttributeKey2;
            return this;
        }

        public Builder setKeyringAttributeValue2(String keyringAttributeValue2) {
            validateArgument("keyringAttributeValue2", keyringAttributeValue2);

            this.keyringAttributeValue2 = keyringAttributeValue2;
            return this;
        }

        public PersistenceParameters build() {
            PersistenceParameters persistenceParameters = new PersistenceParameters(
                    cacheFilePath,
                    cacheLockFilePath,
                    keychainService,
                    keychainAccount,
                    keyringCollection,
                    keyringSchemaName,
                    keyringSecretLabel,
                    keyringAttributeKey1,
                    keyringAttributeValue1,
                    keyringAttributeKey2,
                    keyringAttributeValue2);

            return persistenceParameters;
        }
    }
}
