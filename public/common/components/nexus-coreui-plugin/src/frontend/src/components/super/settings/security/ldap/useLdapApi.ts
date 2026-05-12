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

import { useState, useCallback } from 'react';
import { restClient, parseApiError, urlBuilder } from '@/utils/api';
import { LdapServer, LdapSchemaTemplate, LdapUser, LdapFormData } from './types';

/**
 * REST API LDAP server shape (from ReadLdapServerXo.java)
 *
 * FIELD NAME DIFFERENCES from ExtDirect:
 * - connectionTimeoutSeconds (REST) vs connectionTimeout (ExtDirect)
 * - connectionRetryDelaySeconds (REST) vs connectionRetryDelay (ExtDirect)
 * - authScheme is UPPERCASE in REST: NONE, SIMPLE, DIGEST_MD5, CRAM_MD5
 */
interface RestLdapServer {
  id: string;
  order: number;
  name: string;
  protocol: 'ldap' | 'ldaps';
  useTrustStore: boolean;
  host: string;
  port: number;
  searchBase: string;
  authScheme: string;  // UPPERCASE: NONE, SIMPLE, DIGEST_MD5, CRAM_MD5
  authRealm?: string;
  authUsername?: string;
  connectionTimeoutSeconds: number;
  connectionRetryDelaySeconds: number;
  maxIncidentsCount: number;
  userBaseDn?: string;
  userSubtree: boolean;
  userObjectClass: string;
  userLdapFilter?: string;
  userIdAttribute: string;
  userRealNameAttribute: string;
  userEmailAddressAttribute: string;
  userPasswordAttribute?: string;
  ldapGroupsAsRoles: boolean;
  groupType?: 'static' | 'dynamic';
  groupBaseDn?: string;
  groupSubtree: boolean;
  groupObjectClass?: string;
  groupIdAttribute?: string;
  groupMemberAttribute?: string;
  groupMemberFormat?: string;
  userMemberOfAttribute?: string;
}

/**
 * REST API LDAP user shape from verify-user-mapping
 */
interface RestLdapUser {
  username: string;
  realName?: string;
  email?: string;
  membership?: string[];
}

/**
 * Convert REST authScheme to UI format
 * REST: SIMPLE, DIGEST_MD5, CRAM_MD5, NONE
 * UI: simple, DIGEST-MD5, CRAM-MD5, none
 */
function restAuthSchemeToUi(scheme: string): string {
  const map: Record<string, string> = {
    'NONE': 'none',
    'SIMPLE': 'simple',
    'DIGEST_MD5': 'DIGEST-MD5',
    'CRAM_MD5': 'CRAM-MD5',
  };
  return map[scheme] || scheme.toLowerCase();
}

/**
 * Convert UI authScheme to REST format
 */
function uiAuthSchemeToRest(scheme: string): string {
  const map: Record<string, string> = {
    'none': 'NONE',
    'simple': 'SIMPLE',
    'DIGEST-MD5': 'DIGEST_MD5',
    'CRAM-MD5': 'CRAM_MD5',
  };
  return map[scheme] || scheme.toUpperCase().replace(/-/g, '_');
}

/**
 * Convert REST LDAP server to UI-compatible LdapServer shape
 */
function restToLdapServer(rest: RestLdapServer): LdapServer {
  return {
    id: rest.id,
    order: rest.order,
    name: rest.name,
    protocol: rest.protocol.toLowerCase() as 'ldap' | 'ldaps',
    useTrustStore: rest.useTrustStore,
    host: rest.host,
    port: rest.port,
    searchBase: rest.searchBase,
    authScheme: restAuthSchemeToUi(rest.authScheme),
    authRealm: rest.authRealm,
    authUsername: rest.authUsername,
    // Note: REST never returns authPassword
    connectionTimeout: rest.connectionTimeoutSeconds,
    connectionRetryDelay: rest.connectionRetryDelaySeconds,
    maxIncidentsCount: rest.maxIncidentsCount,
    userBaseDn: rest.userBaseDn,
    userSubtree: rest.userSubtree,
    userObjectClass: rest.userObjectClass,
    userLdapFilter: rest.userLdapFilter,
    userIdAttribute: rest.userIdAttribute,
    userRealNameAttribute: rest.userRealNameAttribute,
    userEmailAddressAttribute: rest.userEmailAddressAttribute,
    userPasswordAttribute: rest.userPasswordAttribute,
    ldapGroupsAsRoles: rest.ldapGroupsAsRoles,
    groupType: rest.groupType,
    groupBaseDn: rest.groupBaseDn,
    groupSubtree: rest.groupSubtree,
    groupObjectClass: rest.groupObjectClass,
    groupIdAttribute: rest.groupIdAttribute,
    groupMemberAttribute: rest.groupMemberAttribute,
    groupMemberFormat: rest.groupMemberFormat,
    userMemberOfAttribute: rest.userMemberOfAttribute,
  };
}

/**
 * Convert LdapFormData to REST API request format
 */
function ldapFormDataToRestRequest(data: LdapFormData): Record<string, unknown> {
  return {
    id: data.id || undefined,
    name: data.name,
    protocol: data.protocol,
    useTrustStore: data.useTrustStore || false,
    host: data.host,
    port: data.port,
    searchBase: data.searchBase,
    authScheme: uiAuthSchemeToRest(data.authScheme),
    authRealm: data.authRealm || undefined,
    authUsername: data.authUsername || undefined,
    authPassword: data.authPassword || undefined,
    connectionTimeoutSeconds: data.connectionTimeout || 30,
    connectionRetryDelaySeconds: data.connectionRetryDelay || 300,
    maxIncidentsCount: data.maxIncidentsCount || 3,
    userBaseDn: data.userBaseDn || undefined,
    userSubtree: data.userSubtree || false,
    userObjectClass: data.userObjectClass,
    userLdapFilter: data.userLdapFilter || undefined,
    userIdAttribute: data.userIdAttribute,
    userRealNameAttribute: data.userRealNameAttribute,
    userEmailAddressAttribute: data.userEmailAddressAttribute,
    userPasswordAttribute: data.userPasswordAttribute || undefined,
    ldapGroupsAsRoles: data.ldapGroupsAsRoles,
    groupType: data.ldapGroupsAsRoles ? data.groupType : undefined,
    groupBaseDn: data.groupBaseDn || undefined,
    groupSubtree: data.groupSubtree || false,
    groupObjectClass: data.groupObjectClass || undefined,
    groupIdAttribute: data.groupIdAttribute || undefined,
    groupMemberAttribute: data.groupMemberAttribute || undefined,
    groupMemberFormat: data.groupMemberFormat || undefined,
    userMemberOfAttribute: data.userMemberOfAttribute || undefined,
  };
}

/**
 * Custom hook for LDAP API operations
 *
 * ALL METHODS NOW USE REST API:
 * - fetchServers: GET /v1/security/ldap
 * - fetchServer: GET /v1/security/ldap/{name}
 * - fetchTemplates: GET /v1/security/ldap/templates
 * - createServer: POST /v1/security/ldap
 * - updateServer: PUT /v1/security/ldap/{name}
 * - deleteServer: DELETE /v1/security/ldap/{name}
 * - changeOrder: POST /v1/security/ldap/change-order
 * - clearCache: DELETE /v1/security/ldap/cache
 * - verifyConnection: POST /v1/security/ldap/verify-connection
 * - verifyUserMapping: POST /v1/security/ldap/verify-user-mapping
 * - verifyLogin: POST /v1/security/ldap/verify-login
 */
export function useLdapApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch all LDAP servers using REST API
   */
  const fetchServers = useCallback(async (): Promise<LdapServer[]> => {
    try {
      const restServers = await restClient.get<RestLdapServer[]>(urlBuilder.ldap.list());
      return restServers.map(restToLdapServer);
    } catch (err: unknown) {
      console.error('Failed to fetch LDAP servers:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch a single LDAP server by name using REST API
   */
  const fetchServer = useCallback(async (serverName: string): Promise<LdapServer | null> => {
    try {
      const restServer = await restClient.get<RestLdapServer>(urlBuilder.ldap.get(serverName));
      return restToLdapServer(restServer);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      // Return null for 404 (not found)
      if (apiError.status === 404) {
        return null;
      }
      console.error('Failed to fetch LDAP server:', err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch LDAP schema templates using REST API
   */
  const fetchTemplates = useCallback(async (): Promise<LdapSchemaTemplate[]> => {
    try {
      const templates = await restClient.get<LdapSchemaTemplate[]>(urlBuilder.ldap.templates());
      return templates;
    } catch (err: unknown) {
      console.error('Failed to fetch LDAP templates:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Create a new LDAP server using REST API
   */
  const createServer = useCallback(async (data: LdapFormData): Promise<LdapServer> => {
    setLoading(true);
    setError(null);
    try {
      const payload = ldapFormDataToRestRequest(data);
      await restClient.post(urlBuilder.ldap.create(), payload);
      // REST create returns 201 with no body, fetch the created server
      const created = await restClient.get<RestLdapServer>(urlBuilder.ldap.get(data.name));
      return restToLdapServer(created);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update an existing LDAP server using REST API
   */
  const updateServer = useCallback(async (data: LdapFormData): Promise<LdapServer> => {
    setLoading(true);
    setError(null);
    try {
      const payload = ldapFormDataToRestRequest(data);
      await restClient.put(urlBuilder.ldap.update(data.name), payload);
      // REST update returns 204 with no body, fetch the updated server
      const updated = await restClient.get<RestLdapServer>(urlBuilder.ldap.get(data.name));
      return restToLdapServer(updated);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete an LDAP server using REST API
   */
  const deleteServer = useCallback(async (serverName: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.delete(urlBuilder.ldap.delete(serverName));
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Change the order of LDAP servers using REST API
   * NOTE: REST uses server NAMES, not IDs
   */
  const changeOrder = useCallback(async (serverNames: string[]): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.post(urlBuilder.ldap.changeOrder(), serverNames);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Clear the LDAP cache using REST API
   */
  const clearCache = useCallback(async (): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.delete(urlBuilder.ldap.clearCache());
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Verify LDAP connection using REST API.
   * The REST endpoint validates the full CreateLdapServerXo (including user mapping fields),
   * unlike the ExtDirect endpoint which only validates connection fields.
   */
  const verifyConnection = useCallback(async (data: LdapFormData, existingServerName?: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const payload = ldapFormDataToRestRequest(data);
      const url = existingServerName
        ? `${urlBuilder.ldap.verifyConnection()}?existingServerName=${encodeURIComponent(existingServerName)}`
        : urlBuilder.ldap.verifyConnection();
      await restClient.post(url, payload);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Verify user mapping configuration using REST API
   */
  const verifyUserMapping = useCallback(async (data: LdapFormData, existingServerName?: string): Promise<LdapUser[]> => {
    setLoading(true);
    setError(null);
    try {
      const payload = ldapFormDataToRestRequest(data);
      const url = existingServerName
        ? `${urlBuilder.ldap.verifyUserMapping()}?existingServerName=${encodeURIComponent(existingServerName)}`
        : urlBuilder.ldap.verifyUserMapping();
      const users = await restClient.post<RestLdapUser[]>(url, payload);
      return users.map(u => ({
        username: u.username,
        realName: u.realName,
        email: u.email,
        membership: u.membership,
      }));
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Verify login with test credentials using REST API
   */
  const verifyLogin = useCallback(async (
    data: LdapFormData,
    username: string,
    password: string
  ): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const payload = {
        ...ldapFormDataToRestRequest(data),
        // REST API expects plain text credentials (not base64)
        testUsername: username,
        testPassword: password,
      };
      await restClient.post(urlBuilder.ldap.verifyLogin(), payload);
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
    fetchServers,
    fetchServer,
    fetchTemplates,
    createServer,
    updateServer,
    deleteServer,
    changeOrder,
    clearCache,
    verifyConnection,
    verifyUserMapping,
    verifyLogin,
  };
}

export default useLdapApi;
