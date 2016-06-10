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

package org.wso2.carbon.security.caas.user.core.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.security.caas.internal.CarbonSecurityDataHolder;
import org.wso2.carbon.security.caas.user.core.bean.Group;
import org.wso2.carbon.security.caas.user.core.bean.User;
import org.wso2.carbon.security.caas.user.core.config.CacheConfig;
import org.wso2.carbon.security.caas.user.core.config.IdentityConnectorConfig;
import org.wso2.carbon.security.caas.user.core.constant.CacheNames;
import org.wso2.carbon.security.caas.user.core.exception.GroupNotFoundException;
import org.wso2.carbon.security.caas.user.core.exception.IdentityStoreException;
import org.wso2.carbon.security.caas.user.core.exception.StoreException;
import org.wso2.carbon.security.caas.user.core.exception.UserNotFoundException;
import org.wso2.carbon.security.caas.user.core.service.RealmService;
import org.wso2.carbon.security.caas.user.core.util.CacheHelper;

import java.util.List;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.security.auth.callback.Callback;

/**
 * Virtual identity store with the caching.
 * @since 1.0.0
 */
public class CacheBackedIdentityStore implements IdentityStore {

    private static Logger log = LoggerFactory.getLogger(CacheBackedIdentityStore.class);

    private IdentityStore identityStore = new IdentityStoreImpl();
    private CacheManager cacheManager;
    private Map<String, CacheConfig> cacheConfigs;

    public CacheBackedIdentityStore(Map<String, CacheConfig> cacheConfigs) {
        this.cacheConfigs = cacheConfigs;
    }

    @Override
    public void init(RealmService realmService, Map<String, IdentityConnectorConfig> identityConnectorConfigs)
            throws IdentityStoreException {

        if (CarbonSecurityDataHolder.getInstance().getCarbonCachingService() == null) {
            throw new StoreException("Caching service is not available.");
        }

        cacheManager = CarbonSecurityDataHolder.getInstance().getCarbonCachingService().getCachingProvider()
                .getCacheManager();
        identityStore.init(realmService, identityConnectorConfigs);

        // Initialize all caches.
        CacheHelper.createCache(CacheNames.USER_USERNAME, String.class, User.class, CacheHelper.MEDIUM_EXPIRE_TIME,
                cacheConfigs, cacheManager);
        CacheHelper.createCache(CacheNames.USER_USERID, String.class, User.class, CacheHelper.MEDIUM_EXPIRE_TIME,
                cacheConfigs, cacheManager);
        CacheHelper.createCache(CacheNames.GROUP_GROUPNAME, String.class, Group.class, CacheHelper.MEDIUM_EXPIRE_TIME,
                cacheConfigs, cacheManager);
        CacheHelper.createCache(CacheNames.GROUP_GROUPID, String.class, Group.class, CacheHelper.MEDIUM_EXPIRE_TIME,
                cacheConfigs, cacheManager);
        CacheHelper.createCache(CacheNames.GROUPS_USERID_IDENTITYSTOREID, String.class, List.class,
                CacheHelper.MEDIUM_EXPIRE_TIME, cacheConfigs, cacheManager);

        if (log.isDebugEnabled()) {
            log.debug("Cache backed identity store successfully initialized.");
        }
    }

    @Override
    public User getUser(String username) throws IdentityStoreException, UserNotFoundException {

        if (CacheHelper.isCacheDisabled(cacheConfigs, CacheNames.USER_USERNAME)) {
            return identityStore.getUser(username);
        }

        Cache<String, User> cache = cacheManager.getCache(CacheNames.USER_USERNAME, String.class, User.class);
        User user = cache.get(username);

        if (user == null) {
            user = identityStore.getUser(username);
            cache.put(username, user);
            if (log.isDebugEnabled()) {
                log.debug("User cached for username: {}.", username);
            }
        }

        return user;
    }

    @Override
    public User getUser(Callback[] callbacks) throws IdentityStoreException, UserNotFoundException {

        // We are using this method mostly internally and for to aid the authenticate() method. I see no use of
        // caching in here.
        return identityStore.getUser(callbacks);
    }

    @Override
    public User getUserFromId(String userId, String identityStoreId) throws IdentityStoreException {

        if (CacheHelper.isCacheDisabled(cacheConfigs, CacheNames.USER_USERID)) {
            return identityStore.getUserFromId(userId, identityStoreId);
        }

        Cache<String, User> cache = cacheManager.getCache(CacheNames.USER_USERID, String.class, User.class);
        User user = cache.get(userId + identityStoreId);

        if (user == null) {
            user = identityStore.getUserFromId(userId, identityStoreId);
            cache.put(userId + identityStoreId, user);
            if (log.isDebugEnabled()) {
                log.debug("User cached for user id: {} and identity store id: {}.", user, identityStoreId);
            }
        }

        return user;
    }

    @Override
    public List<User> listUsers(String filterPattern, int offset, int length) throws IdentityStoreException {
        return identityStore.listUsers(filterPattern, offset, length);
    }

    @Override
    public Map<String, String> getUserAttributeValues(String userID, String userStoreId) throws IdentityStoreException {
        return identityStore.getUserAttributeValues(userID, userStoreId);
    }

    @Override
    public Map<String, String> getUserAttributeValues(String userID, List<String> attributeNames, String userStoreId)
            throws IdentityStoreException {
        return identityStore.getUserAttributeValues(userID, attributeNames, userStoreId);
    }

    @Override
    public Group getGroup(String groupName) throws IdentityStoreException, GroupNotFoundException {

        if (CacheHelper.isCacheDisabled(cacheConfigs, CacheNames.GROUP_GROUPNAME)) {
            return identityStore.getGroup(groupName);
        }

        Cache<String, Group> cache = cacheManager.getCache(CacheNames.GROUP_GROUPNAME, String.class, Group.class);
        Group group = cache.get(groupName);

        if (group == null) {
            group = identityStore.getGroup(groupName);
            cache.put(groupName, group);
            if (log.isDebugEnabled()) {
                log.debug("Group cached for group name: {}.", groupName);
            }
        }

        return group;
    }

    @Override
    public Group getGroupFromId(String groupId, String identityStoreId) throws IdentityStoreException {

        if (CacheHelper.isCacheDisabled(cacheConfigs, CacheNames.GROUP_GROUPID)) {
            return identityStore.getGroupFromId(groupId, identityStoreId);
        }

        Cache<String, Group> cache = cacheManager.getCache(CacheNames.GROUP_GROUPID, String.class, Group.class);
        Group group = cache.get(groupId + identityStoreId);

        if (group == null) {
            group = identityStore.getGroupFromId(groupId, identityStoreId);
            cache.put(groupId + identityStoreId, group);
            if (log.isDebugEnabled()) {
                log.debug("Group cached for group id: {} and for identity store id: {}.", groupId, identityStoreId);
            }
        }

        return group;
    }

    @Override
    public List<Group> listGroups(String filterPattern, int offset, int length) throws IdentityStoreException {
        return identityStore.listGroups(filterPattern, offset, length);
    }

    @Override
    public List<Group> getGroupsOfUser(String userId, String identityStoreId) throws IdentityStoreException {

        if (CacheHelper.isCacheDisabled(cacheConfigs, CacheNames.GROUPS_USERID_IDENTITYSTOREID)) {
            return identityStore.getGroupsOfUser(userId, identityStoreId);
        }

        Cache<String, List> cache = cacheManager.getCache(CacheNames.GROUPS_USERID_IDENTITYSTOREID, String.class,
                List.class);

        List<Group> groups = cache.get(userId + identityStoreId);

        if (groups == null) {
            groups = identityStore.getGroupsOfUser(userId, identityStoreId);
            cache.put(userId + identityStoreId, groups);
            if (log.isDebugEnabled()) {
                log.debug("Groups cached for user id: {} and identity store id: {}.", userId, identityStoreId);
            }
        }

        return groups;
    }

    @Override
    public List<User> getUsersOfGroup(String groupID, String identityStoreId) throws IdentityStoreException {
        return identityStore.getUsersOfGroup(groupID, identityStoreId);
    }

    @Override
    public boolean isUserInGroup(String userId, String groupId, String identityStoreId) throws IdentityStoreException {

        if (CacheHelper.isCacheDisabled(cacheConfigs, CacheNames.GROUPS_USERID_IDENTITYSTOREID)) {
            return identityStore.isUserInGroup(userId, groupId, identityStoreId);
        }

        Cache<String, List> cache = cacheManager.getCache(CacheNames.GROUPS_USERID_IDENTITYSTOREID, String.class,
                List.class);

        boolean isUserInGroup = false;
        List<Group> groups = cache.get(userId + identityStoreId);

        if (groups == null) {
            isUserInGroup = identityStore.isUserInGroup(userId, groupId, identityStoreId);
        } else {
            // If there are groups for this user id and identity store id in the cache,
            // do the validation logic here.
            for (Group group : groups) {
                if (group.getGroupId().equals(groupId)) {
                    isUserInGroup = true;
                    break;
                }
            }
        }
        return isUserInGroup;
    }
}
