/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.security.caas.user.core.bean;

import org.wso2.carbon.security.caas.user.core.exception.StoreException;

/**
 * Represents a resource.
 */
public class Resource {

    public static final String DELIMITER = ":";

    private String resourceNamespace;
    private String resourceId;

    private String authorizationStore;

    private String userId;
    private Domain domain;

    public Resource(String resourceString) {

        if (!resourceString.contains(DELIMITER)) {
            throw new StoreException("Invalid or cannot find the delimiter.");
        }

        resourceNamespace = resourceString.substring(0, resourceString.indexOf(DELIMITER));
        resourceId = resourceString.substring(resourceString.indexOf(DELIMITER) + 1, resourceString.length());
    }

    public Resource(String resourceNamespace, String resourceId) {

        this.resourceNamespace = resourceNamespace;
        this.resourceId = resourceId;
    }

    public Resource(String resourceNamespace, String resourceId, String userId, Domain domain) {

        this.resourceNamespace = resourceNamespace;
        this.resourceId = resourceId;
        this.userId = userId;
        this.domain = domain;
    }

    private Resource(String resourceNamespace, String resourceId, String userId, Domain domain,
                     String authorizationStore) {

        this.resourceNamespace = resourceNamespace;
        this.resourceId = resourceId;
        this.userId = userId;
        this.domain = domain;
        this.authorizationStore = authorizationStore;
    }

    public static Resource getUniversalResource() {
        return new Resource("*", "*");
    }

    public String getResourceNamespace() {
        return resourceNamespace;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceString() {
        return resourceNamespace + DELIMITER + resourceId;
    }

    public void setAuthorizationStore(String authorizationStore) {
        this.authorizationStore = authorizationStore;
    }

    public String getAuthorizationStore() {
        return authorizationStore;
    }

    public User.UserBuilder getOwner() {
        return new User.UserBuilder()
                .setUserId(userId)
                .setDomain(domain);
    }

    /**
     * Builder for the resource bean.
     */
    public static class ResourceBuilder {

        private String resourceNamespace;
        private String resourceId;
        private String authorizationStore;
        private String userId;
        private Domain domain;

        public ResourceBuilder setResourceNamespace(String resourceNamespace) {
            this.resourceNamespace = resourceNamespace;
            return this;
        }

        public ResourceBuilder setResourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public ResourceBuilder setAuthorizationStore(String authorizationStore) {
            this.authorizationStore = authorizationStore;
            return this;
        }

        public ResourceBuilder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public ResourceBuilder setDomain(Domain domain) {
            this.domain = domain;
            return this;
        }

        public Resource build() {

            if (resourceNamespace == null || resourceId == null || authorizationStore == null || userId == null ||
                    domain == null) {
                throw new StoreException("Required data is missing to build the resource.");
            }

            return new Resource(resourceNamespace, resourceId, userId, domain, authorizationStore);
        }
    }
}
