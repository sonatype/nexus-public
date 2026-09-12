/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import { useState, useCallback, useRef, useEffect } from 'react';
// Sprint 15: ExtAPIUtils and APIConstants removed - no longer using ExtDirect
import { restClient, parseApiError, ENDPOINTS, urlBuilder } from '../../../../../../interface/api';
import { ExtJS } from '../../../../../../interface/ExtJS';
import {
  User,
  UserSource,
  Role,
  UserFormData,
  DEFAULT_SOURCE,
  isExternalUser,
  RestUser,
  restToUser,
} from './types';

export interface UserInviteData {
  firstName: string;
  lastName: string;
  email: string;
}

// Sprint 15: All ExtDirect usage eliminated - using REST API for all operations

/**
 * Custom hook for Users API operations
 *
 * MIGRATION STATUS:
 * - REST: fetchSources, fetchUsers, fetchUser, fetchRoles, createUser, updateUser (local), deleteUser, changePassword, resetUserToken
 * - REST: updateUser (all users including external role mappings) - Sprint 15 migration
 */
export function useUsersApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  /**
   * Fetch user sources (realms) using REST API
   */
  const fetchSources = useCallback(async (): Promise<UserSource[]> => {
    try {
      const sources = await restClient.get<UserSource[]>(ENDPOINTS.USER_SOURCES);
      return Array.isArray(sources) ? sources : [];
    } catch (err: unknown) {
      console.error('Failed to fetch user sources:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch users list with optional filtering using REST API
   */
  const fetchUsers = useCallback(async (
    filter: string = '',
    sourceFilter?: string
  ): Promise<User[]> => {
    try {
      const params: Record<string, string> = {};
      if (sourceFilter) {
        params.source = sourceFilter;
      }
      if (filter) {
        params.userId = filter;
      }
      const url = urlBuilder.query(ENDPOINTS.USERS, params);
      const restUsers = await restClient.get<RestUser[]>(url);
      return restUsers.map(restToUser);
    } catch (err: unknown) {
      console.error('Failed to fetch users:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch a single user by ID and source using REST API
   */
  const fetchUser = useCallback(async (
    userId: string,
    source: string = DEFAULT_SOURCE
  ): Promise<User | null> => {
    try {
      const url = urlBuilder.query(ENDPOINTS.USERS, {
        source,
        userId,
      });
      const restUsers = await restClient.get<RestUser[]>(url);
      const found = restUsers.find((u) => u.userId === userId && u.source === source);
      return found ? restToUser(found) : null;
    } catch (err: unknown) {
      console.error('Failed to fetch user:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch all available roles using REST API
   */
  const fetchRoles = useCallback(async (source: string = DEFAULT_SOURCE): Promise<Role[]> => {
    try {
      const url = urlBuilder.query(ENDPOINTS.ROLES, { source });
      const roles = await restClient.get<Role[]>(url);
      return roles;
    } catch (err: unknown) {
      console.error('Failed to fetch roles:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Create a new user using REST API
   */
  const createUser = useCallback(async (data: UserFormData): Promise<User> => {
    setLoading(true);
    setError(null);
    try {
      const payload = {
        userId: data.userId,
        firstName: data.firstName,
        lastName: data.lastName,
        emailAddress: data.emailAddress,
        password: data.password,
        status: data.status ? 'active' : 'disabled',
        roles: data.roles,
      };
      const restUser = await restClient.post<RestUser>(urlBuilder.users.create(), payload);
      return restToUser(restUser);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update an existing user using REST API
   * - Local users: Full update via PUT /v1/security/users/{userId}
   * - External users: Role mapping update via PUT /v1/security/users/{userId}
   *
   * Sprint 15: Migrated from ExtDirect to REST for all user types.
   */
  const updateUser = useCallback(async (
    userId: string,
    data: UserFormData,
    source: string = DEFAULT_SOURCE
  ): Promise<User> => {
    setLoading(true);
    setError(null);
    try {
      if (isExternalUser(source)) {
        const isCloud = ExtJS.state?.().getValue?.('isCloud', false) ?? false;
        if (isCloud) {
          // Cloud: dedicated role sub-resource (PUT /v1/security/users/{userId}/roles)
          await restClient.put(urlBuilder.users.updateRoles(userId), {
            roles: data.roles ?? [],
          });
          return {
            ...data,
            userId,
            source,
            realm: source,
            status: data.status ? 'active' : 'disabled',
          } as User;
        }
        // Self-hosted: update role mappings via PUT /v1/security/users/{userId}?source=
        const payload = {
          userId: userId,
          firstName: data.firstName || userId,
          lastName: data.lastName || '',
          emailAddress: data.emailAddress || '',
          source: source,
          roles: data.roles ?? [],
          status: data.status ? 'active' : 'disabled',
        };
        const restUser = await restClient.put<RestUser>(
          `${urlBuilder.users.update(userId)}?source=${encodeURIComponent(source)}`,
          payload
        );
        return restUser ? restToUser(restUser) : {
          ...data,
          userId,
          source,
          realm: source,
          status: data.status ? 'active' : 'disabled',
        } as User;
      } else {
        const payload = {
          userId: data.userId,
          firstName: data.firstName,
          lastName: data.lastName,
          emailAddress: data.emailAddress,
          source: source,
          status: data.status ? 'active' : 'disabled',
          roles: data.roles ?? [],
        };
        const restUser = await restClient.put<RestUser>(urlBuilder.users.update(userId), payload);
        return restToUser(restUser);
      }
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete a user using REST API
   */
  const deleteUser = useCallback(async (userId: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.delete(urlBuilder.users.delete(userId));
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Change user password using REST API
   */
  const changePassword = useCallback(async (
    userId: string,
    newPassword: string
  ): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const url = urlBuilder.users.changePassword(userId);
      // Password change requires plain text content type
      await restClient.put(url, newPassword, {
        headers: { 'Content-Type': 'text/plain' },
      });
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Invite a user via the cloud Auth0 invite endpoint (POST /v1/security/users/invite)
   * Only available in cloud distribution.
   */
  const inviteUser = useCallback(async (data: UserInviteData): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.post(urlBuilder.users.invite(), data);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Status-only update. Reuses PUT /v1/security/users/{userId} by reconstructing
   * the full payload from the server-truth user with only `status` overridden;
   * preserves other fields against concurrent in-progress form edits.
   */
  const patchUserStatus = useCallback(async (
    currentUser: User,
    active: boolean,
  ): Promise<User> => {
    const data: UserFormData = {
      userId: currentUser.userId,
      firstName: currentUser.firstName ?? '',
      lastName: currentUser.lastName ?? '',
      emailAddress: currentUser.emailAddress || currentUser.email || '',
      status: active,
      roles: currentUser.roles ?? [],
      source: currentUser.source,
    };
    return updateUser(currentUser.userId, data, currentUser.source);
  }, [updateUser]);

  /**
   * Reset user token using REST API
   */
  const resetUserToken = useCallback(async (
    userId: string,
    realm: string
  ): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const url = `${ENDPOINTS.USERS}/${encodeURIComponent(userId)}/${encodeURIComponent(realm)}/user-token-reset`;
      await restClient.delete(url);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchSources,
    fetchUsers,
    fetchUser,
    fetchRoles,
    createUser,
    updateUser,
    patchUserStatus,
    deleteUser,
    changePassword,
    resetUserToken,
    inviteUser,
  };
}

export default useUsersApi;
