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
import { restClient, parseApiError, ENDPOINTS, urlBuilder } from '../../../../../../interface/api';
import { Privilege, PrivilegeReference, PrivilegeType, PrivilegeFormData } from './types';

// Internal UI endpoint for privilege types
const PRIVILEGE_TYPES_URL = '/service/rest/internal/ui/privileges/types';

/**
 * REST API privilege shape (from ApiPrivilege.java)
 * Type-specific properties are flattened at the top level
 */
interface RestPrivilege {
  type: string;
  name: string;
  description: string;
  readOnly: boolean;
  // Application privilege
  domain?: string;
  actions?: string[];
  // Wildcard privilege
  pattern?: string;
  // Repository privileges
  format?: string;
  repository?: string;
  contentSelector?: string;
  // Script privilege
  scriptName?: string;
}

/**
 * REST API privilege type shape (from PrivilegesTypesUIResponse.java)
 */
interface RestPrivilegeType {
  id: string;
  name: string;
  formFields: Array<{
    id: string;
    type: string;
    label: string;
    helpText?: string;
    required?: boolean;
    regexValidation?: string;
    initialValue?: string;
    storeApi?: string;
    storeFilters?: Record<string, string>;
    idMapping?: string;
    nameMapping?: string;
    allowAutocomplete?: boolean;
  }> | null;
}

/**
 * Action name mapping from UI/ExtDirect to REST API
 * REST API uses different names for some actions:
 * - UPDATE -> EDIT
 * - CREATE -> ADD
 */
const ACTION_TO_REST: Record<string, string> = {
  UPDATE: 'EDIT',
  CREATE: 'ADD',
};

const ACTION_FROM_REST: Record<string, string> = {
  EDIT: 'UPDATE',
  ADD: 'CREATE',
};

/**
 * Convert REST API action to UI format
 * e.g., "EDIT" -> "update"
 */
function formatActionFromApi(action: string): string {
  const mapped = ACTION_FROM_REST[action] || action;
  return mapped.toLowerCase();
}

/**
 * Convert actions string to array format expected by REST API
 * e.g., "read,update" -> ["READ", "EDIT"]
 */
function formatActionsForApi(actions: string | undefined): string[] {
  if (!actions) return [];
  return actions
    .split(',')
    .map((a) => {
      const upper = a.trim().toUpperCase();
      return ACTION_TO_REST[upper] || upper;
    })
    .filter(Boolean);
}

/**
 * Convert REST privilege to ExtDirect-compatible Privilege shape
 * This maintains backward compatibility with existing UI components
 */
function restToPrivilege(rest: RestPrivilege): Privilege {
  const properties: Record<string, string> = {};

  // Map type-specific REST fields to properties object
  // Use formatActionFromApi to convert REST action names (EDIT, ADD) to UI names (update, create)
  switch (rest.type) {
    case 'application':
      if (rest.domain) properties.domain = rest.domain;
      if (rest.actions) properties.actions = rest.actions.map(formatActionFromApi).join(',');
      break;
    case 'wildcard':
      if (rest.pattern) properties.pattern = rest.pattern;
      break;
    case 'repository-view':
    case 'repository-admin':
      if (rest.format) properties.format = rest.format;
      if (rest.repository) properties.repository = rest.repository;
      if (rest.actions) properties.actions = rest.actions.map(formatActionFromApi).join(',');
      break;
    case 'repository-content-selector':
      if (rest.format) properties.format = rest.format;
      if (rest.repository) properties.repository = rest.repository;
      if (rest.contentSelector) properties.contentSelector = rest.contentSelector;
      if (rest.actions) properties.actions = rest.actions.map(formatActionFromApi).join(',');
      break;
    case 'script':
      if (rest.scriptName) properties.name = rest.scriptName;
      if (rest.actions) properties.actions = rest.actions.map(formatActionFromApi).join(',');
      break;
  }

  // Compute permission string (matches PrivilegeComponent.java behavior)
  const permission = computePermission(rest);

  return {
    id: rest.name, // REST uses name as identifier
    version: '1', // REST doesn't return version, default to '1'
    name: rest.name,
    description: rest.description || '',
    type: rest.type,
    readOnly: rest.readOnly,
    properties,
    permission,
  };
}

/**
 * Compute permission string from REST privilege
 * Matches the format returned by ExtDirect (e.g., "nexus:users:read,update")
 */
function computePermission(rest: RestPrivilege): string {
  switch (rest.type) {
    case 'application':
      return `nexus:${rest.domain || '*'}:${(rest.actions || []).join(',').toLowerCase() || '*'}`;
    case 'wildcard':
      return rest.pattern || '*';
    case 'repository-view':
      return `nexus:repository-view:${rest.format || '*'}:${rest.repository || '*'}:${(rest.actions || []).join(',').toLowerCase() || '*'}`;
    case 'repository-admin':
      return `nexus:repository-admin:${rest.format || '*'}:${rest.repository || '*'}:${(rest.actions || []).join(',').toLowerCase() || '*'}`;
    case 'repository-content-selector':
      return `nexus:repository-content-selector:${rest.format || '*'}:${rest.repository || '*'}:${rest.contentSelector || '*'}:${(rest.actions || []).join(',').toLowerCase() || '*'}`;
    case 'script':
      return `nexus:script:${rest.scriptName || '*'}:${(rest.actions || []).join(',').toLowerCase() || '*'}`;
    default:
      return rest.type;
  }
}

/**
 * Apply client-side filtering to privileges
 */
function applyFilter(privileges: Privilege[], filter: string): Privilege[] {
  if (!filter) return privileges;
  const lowerFilter = filter.toLowerCase();
  return privileges.filter(
    (p) =>
      p.name.toLowerCase().includes(lowerFilter) ||
      p.description.toLowerCase().includes(lowerFilter) ||
      p.type.toLowerCase().includes(lowerFilter) ||
      p.permission.toLowerCase().includes(lowerFilter)
  );
}

/**
 * Apply client-side sorting to privileges
 */
function applySort(
  privileges: Privilege[],
  sortField?: string,
  sortDirection?: 'ASC' | 'DESC'
): Privilege[] {
  if (!sortField) return privileges;

  const sorted = [...privileges];
  const dir = sortDirection === 'DESC' ? -1 : 1;

  sorted.sort((a, b) => {
    const aVal = (a as Record<string, unknown>)[sortField] as string || '';
    const bVal = (b as Record<string, unknown>)[sortField] as string || '';
    return aVal.localeCompare(bVal) * dir;
  });

  return sorted;
}

/**
 * Custom hook for Privileges API operations
 *
 * MIGRATION NOTE: All methods now use REST API instead of ExtDirect.
 * - fetchPrivileges: GET /v1/security/privileges
 * - fetchPrivilegeTypes: GET /internal/ui/privileges/types
 * - fetchPrivilegeReferences: GET /v1/security/privileges (mapped to references)
 * - createPrivilege: POST /v1/security/privileges/{type}
 * - updatePrivilege: PUT /v1/security/privileges/{type}/{name}
 * - deletePrivilege: DELETE /v1/security/privileges/{name}
 */
export function usePrivilegesApi() {
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
   * Fetch all privileges using REST API
   * Applies client-side filtering, sorting, and pagination since REST returns all privileges
   */
  const fetchPrivileges = useCallback(async (
    filter?: string,
    sortField?: string,
    sortDirection?: 'ASC' | 'DESC',
    start?: number,
    limit?: number
  ): Promise<{ data: Privilege[]; total: number }> => {
    try {
      const restPrivileges = await restClient.get<RestPrivilege[]>(ENDPOINTS.PRIVILEGES);
      let privileges = restPrivileges.map(restToPrivilege);

      // Apply client-side filtering
      privileges = applyFilter(privileges, filter || '');

      // Store total before pagination
      const total = privileges.length;

      // Apply client-side sorting
      privileges = applySort(privileges, sortField, sortDirection);

      // Apply client-side pagination
      const startIndex = start || 0;
      const endIndex = limit ? startIndex + limit : privileges.length;
      const paginatedData = privileges.slice(startIndex, endIndex);

      return {
        data: paginatedData,
        total,
      };
    } catch (err: unknown) {
      console.error('Failed to fetch privileges:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch privilege references (id/name pairs) using REST API
   * Maps from full privileges list to reference format
   */
  const fetchPrivilegeReferences = useCallback(async (): Promise<PrivilegeReference[]> => {
    try {
      const restPrivileges = await restClient.get<RestPrivilege[]>(ENDPOINTS.PRIVILEGES);

      // Map to reference format (id/name pairs)
      // In REST, name is used as the identifier
      return restPrivileges.map((p) => ({
        id: p.name,
        name: p.name,
      }));
    } catch (err: unknown) {
      console.error('Failed to fetch privilege references:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch privilege types using REST API
   */
  const fetchPrivilegeTypes = useCallback(async (): Promise<PrivilegeType[]> => {
    try {
      const types = await restClient.get<RestPrivilegeType[]>(PRIVILEGE_TYPES_URL);
      return types;
    } catch (err: unknown) {
      console.error('Failed to fetch privilege types:', err);
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Find a privilege by ID using REST API
   */
  const findPrivilege = useCallback(async (privilegeId: string): Promise<Privilege | null> => {
    try {
      // Fetch specific privilege by name from REST API
      const restPrivilege = await restClient.get<RestPrivilege>(
        urlBuilder.privileges.get(privilegeId)
      );
      return restToPrivilege(restPrivilege);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      // Return null for 404 (not found) instead of throwing
      if (apiError.status === 404) {
        return null;
      }
      console.error('Failed to find privilege:', err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Create a new privilege using REST API
   */
  const createPrivilege = useCallback(async (data: PrivilegeFormData): Promise<Privilege> => {
    setLoading(true);
    setError(null);
    try {
      // Build payload for REST API - format varies by type
      const payload: Record<string, unknown> = {
        name: data.name,
        description: data.description || '',
      };

      // Add type-specific properties
      const props = data.properties || {};

      if (data.type === 'application') {
        payload.domain = props.domain || '';
        payload.actions = formatActionsForApi(props.actions);
      } else if (data.type === 'wildcard') {
        payload.pattern = props.pattern || '';
      } else if (data.type === 'repository-view' || data.type === 'repository-admin') {
        payload.format = props.format || '*';
        payload.repository = props.repository || '*';
        payload.actions = formatActionsForApi(props.actions);
      } else if (data.type === 'repository-content-selector') {
        payload.format = props.format || '*';
        payload.repository = props.repository || '*';
        payload.contentSelector = props.contentSelector || '';
        payload.actions = formatActionsForApi(props.actions);
      } else if (data.type === 'script') {
        payload.scriptName = props.name || '';
        payload.actions = formatActionsForApi(props.actions);
      }

      const url = urlBuilder.privileges.createByType(data.type);
      await restClient.post(url, payload);
      // REST create returns 201 with no body, fetch the created privilege
      const created = await restClient.get<RestPrivilege>(urlBuilder.privileges.get(data.name));
      return restToPrivilege(created);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update an existing privilege using REST API
   */
  const updatePrivilege = useCallback(async (data: PrivilegeFormData): Promise<Privilege> => {
    setLoading(true);
    setError(null);
    try {
      // Build payload for REST API - format varies by type
      const payload: Record<string, unknown> = {
        name: data.name,
        description: data.description || '',
      };

      // Add type-specific properties
      const props = data.properties || {};

      if (data.type === 'application') {
        payload.domain = props.domain || '';
        payload.actions = formatActionsForApi(props.actions);
      } else if (data.type === 'wildcard') {
        payload.pattern = props.pattern || '';
      } else if (data.type === 'repository-view' || data.type === 'repository-admin') {
        payload.format = props.format || '*';
        payload.repository = props.repository || '*';
        payload.actions = formatActionsForApi(props.actions);
      } else if (data.type === 'repository-content-selector') {
        payload.format = props.format || '*';
        payload.repository = props.repository || '*';
        payload.contentSelector = props.contentSelector || '';
        payload.actions = formatActionsForApi(props.actions);
      } else if (data.type === 'script') {
        payload.scriptName = props.name || '';
        payload.actions = formatActionsForApi(props.actions);
      }

      const url = urlBuilder.privileges.update(data.type, data.name);
      await restClient.put(url, payload);
      // REST update returns 204 with no body, fetch the updated privilege
      const updated = await restClient.get<RestPrivilege>(urlBuilder.privileges.get(data.name));
      return restToPrivilege(updated);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete a privilege using REST API
   */
  const deletePrivilege = useCallback(async (privilegeName: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const url = urlBuilder.privileges.delete(privilegeName);
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
    fetchPrivileges,
    fetchPrivilegeReferences,
    fetchPrivilegeTypes,
    findPrivilege,
    createPrivilege,
    updatePrivilege,
    deletePrivilege,
  };
}

export default usePrivilegesApi;



