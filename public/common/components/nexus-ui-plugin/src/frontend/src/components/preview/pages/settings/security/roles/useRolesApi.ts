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
import { restClient, parseApiError, ENDPOINTS, urlBuilder, API_INTERNAL_UI } from '../../../../../../interface/api';
import { Role, RoleReference, PrivilegeReference, RoleSource, RoleFormData, NEXUS_SOURCE, DEFAULT_SOURCE } from './types';

/**
 * REST API role source shape (from RoleSourceUIResponse.java)
 */
interface RestRoleSource {
  id: string;
  name: string;
}

/**
 * REST API role shape (from RoleXOResponse.java)
 */
interface RestRole {
  id: string;
  source: string;
  name: string;
  description: string;
  readOnly: boolean;
  privileges: string[];
  roles: string[];
}

/**
 * Convert REST role to ExtDirect-compatible Role shape
 * Maintains backward compatibility with existing UI components
 */
function restToRole(rest: RestRole): Role {
  return {
    id: rest.id,
    version: '1',
    source: (rest.source === DEFAULT_SOURCE || rest.source === 'Nexus') ? NEXUS_SOURCE : rest.source,
    name: rest.name,
    description: rest.description || '',
    readOnly: rest.readOnly ?? false,
    privileges: rest.privileges || [],
    roles: rest.roles || [],
  };
}

/**
 * Convert UI source value to REST source value
 */
function sourceToRest(source: string | undefined): string {
  if (!source || source === NEXUS_SOURCE) {
    return DEFAULT_SOURCE;
  }
  return source;
}

/**
 * Custom hook for Roles API operations
 *
 * MIGRATION NOTE: Most methods now use REST API instead of ExtDirect.
 * - fetchRoles: GET /v1/security/roles
 * - fetchRoleReferences: GET /v1/security/roles (mapped to references)
 * - fetchRolesFromSource: GET /v1/security/roles?source={source}
 * - fetchPrivilegeReferences: GET /v1/security/privileges (mapped to references)
 * - findRole: GET /v1/security/roles/{id}
 * - createRole: POST /v1/security/roles
 * - updateRole: PUT /v1/security/roles/{id}
 * - deleteRole: DELETE /v1/security/roles/{id}
 *
 * STILL USES EXTDIRECT (blocked, waiting for backend):
 * - fetchRoleSources: No REST endpoint exists
 */
export function useRolesApi() {
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
   * Fetch all Nexus-managed roles using REST API.
   * Uses ?source=default to match the old UI behavior (only fetches roles from DEFAULT_SOURCE).
   * This excludes raw external roles (LDAP/SAML/Crowd) that haven't been mapped in Nexus.
   */
  const fetchRoles = useCallback(async (): Promise<Role[]> => {
    try {
      const url = urlBuilder.query(ENDPOINTS.ROLES, { source: DEFAULT_SOURCE });
      const restRoles = await restClient.get<RestRole[]>(url);
      return restRoles.map(restToRole);
    } catch (err: unknown) {
      console.error('Failed to fetch roles:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch role references (id/name pairs) using REST API.
   * Uses ?source=default to only fetch Nexus-managed roles.
   */
  const fetchRoleReferences = useCallback(async (): Promise<RoleReference[]> => {
    try {
      const url = urlBuilder.query(ENDPOINTS.ROLES, { source: DEFAULT_SOURCE });
      const restRoles = await restClient.get<RestRole[]>(url);
      return restRoles.map((r) => ({ id: r.id, name: r.name }));
    } catch (err: unknown) {
      console.error('Failed to fetch role references:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch role sources (realms) using REST API
   */
  const fetchRoleSources = useCallback(async (): Promise<RoleSource[]> => {
    try {
      const restSources = await restClient.get<RestRoleSource[]>(ENDPOINTS.ROLE_SOURCES);
      // Add Default as the default source (always first)
      const sources: RoleSource[] = [{ id: NEXUS_SOURCE, name: NEXUS_SOURCE }];
      if (Array.isArray(restSources)) {
        sources.push(...restSources);
      }
      return sources;
    } catch (err: unknown) {
      console.error('Failed to fetch role sources:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch roles from a specific source using REST API
   */
  const fetchRolesFromSource = useCallback(async (source: string): Promise<Role[]> => {
    try {
      const restSource = sourceToRest(source);
      const url = urlBuilder.query(ENDPOINTS.ROLES, { source: restSource });
      const restRoles = await restClient.get<RestRole[]>(url);
      return restRoles.map(restToRole);
    } catch (err: unknown) {
      console.error('Failed to fetch roles from source:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Search roles from a specific source using REST API internal endpoint
   * @param source - Authorization source (LDAP, SAML, Crowd, etc.)
   * @param search - Search query string
   */
  const searchRoles = useCallback(async (source: string, search: string): Promise<Role[]> => {
    try {
      // Use the internal UI endpoint: /service/rest/internal/ui/roles
      const url = urlBuilder.query(`${API_INTERNAL_UI}/roles`, { source, search });
      const restRoles = await restClient.get<RestRole[]>(url);
      return restRoles.map(restToRole);
    } catch (err: unknown) {
      console.error('Failed to search roles:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch privilege references (id/name pairs) using REST API
   */
  const fetchPrivilegeReferences = useCallback(async (): Promise<PrivilegeReference[]> => {
    try {
      const restPrivileges = await restClient.get<Array<{ name: string; description?: string }>>(ENDPOINTS.PRIVILEGES);
      return restPrivileges.map((p) => ({ id: p.name, name: p.name, description: p.description }));
    } catch (err: unknown) {
      console.error('Failed to fetch privilege references:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Find a role by ID using REST API
   */
  const findRole = useCallback(async (roleId: string): Promise<Role | null> => {
    try {
      const url = urlBuilder.roles.get(roleId);
      const restRole = await restClient.get<RestRole>(url);
      return restToRole(restRole);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      // Return null for 404 (not found) instead of throwing
      if (apiError.status === 404) {
        return null;
      }
      console.error('Failed to find role:', err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Create a new role using REST API
   */
  const createRole = useCallback(async (data: RoleFormData): Promise<Role> => {
    setLoading(true);
    setError(null);
    try {
      const payload = {
        id: data.id,
        name: data.name,
        description: data.description || '',
        privileges: data.privileges || [],
        roles: data.roles || [],
      };
      const url = urlBuilder.roles.create();
      const restRole = await restClient.post<RestRole>(url, payload);
      return restToRole(restRole);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update an existing role using REST API
   */
  const updateRole = useCallback(async (data: RoleFormData): Promise<Role> => {
    setLoading(true);
    setError(null);
    try {
      const payload = {
        id: data.id,
        name: data.name,
        description: data.description || '',
        privileges: data.privileges || [],
        roles: data.roles || [],
      };
      const url = urlBuilder.roles.update(data.id);
      const restRole = await restClient.put<RestRole>(url, payload);
      return restToRole(restRole);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete a role using REST API
   */
  const deleteRole = useCallback(async (roleId: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const url = urlBuilder.roles.delete(roleId);
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
    fetchRoles,
    fetchRoleReferences,
    fetchRoleSources,
    fetchRolesFromSource,
    searchRoles,
    fetchPrivilegeReferences,
    findRole,
    createRole,
    updateRole,
    deleteRole,
  };
}

export default useRolesApi;
