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

import { createListMachine, restClient, ENDPOINTS } from '@sonatype/nexus-ui-plugin';
import type { ListMachineContext } from '@sonatype/nexus-ui-plugin';
import type { Privilege } from './types';

export interface PrivilegeFilters {
  filter: string;
  typeFilter: string[];
  /** 'locked' = read-only only, 'unlocked' = editable only, empty = all */
  readOnlyFilter: string[];
}

/**
 * REST API privilege shape
 */
interface RestPrivilege {
  type: string;
  name: string;
  description?: string;
  readOnly: boolean;
  // Type-specific fields
  domain?: string;
  actions?: string[];
  pattern?: string;
  format?: string;
  repository?: string;
  contentSelector?: string;
  scriptName?: string;
}

/**
 * Compute permission string from REST privilege
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
 * Transform REST privilege to UI model with computed permission field
 */
function transformPrivilege(rest: RestPrivilege): Privilege {
  const properties: Record<string, string> = {};

  // Map type-specific fields to properties
  switch (rest.type) {
    case 'application':
      if (rest.domain) properties.domain = rest.domain;
      if (rest.actions) properties.actions = rest.actions.join(',');
      break;
    case 'wildcard':
      if (rest.pattern) properties.pattern = rest.pattern;
      break;
    case 'repository-view':
    case 'repository-admin':
      if (rest.format) properties.format = rest.format;
      if (rest.repository) properties.repository = rest.repository;
      if (rest.actions) properties.actions = rest.actions.join(',');
      break;
    case 'repository-content-selector':
      if (rest.format) properties.format = rest.format;
      if (rest.repository) properties.repository = rest.repository;
      if (rest.contentSelector) properties.contentSelector = rest.contentSelector;
      if (rest.actions) properties.actions = rest.actions.join(',');
      break;
    case 'script':
      if (rest.scriptName) properties.name = rest.scriptName;
      if (rest.actions) properties.actions = rest.actions.join(',');
      break;
  }

  return {
    id: rest.name,
    version: '1',
    name: rest.name,
    description: rest.description || '',
    type: rest.type,
    readOnly: rest.readOnly,
    properties,
    permission: computePermission(rest),
  };
}

/**
 * Fetch all privileges and transform to UI model
 */
async function fetchPrivileges(): Promise<Privilege[]> {
  try {
    const data = await restClient.get<RestPrivilege[]>(ENDPOINTS.PRIVILEGES);
    return Array.isArray(data) ? data.map(transformPrivilege) : [];
  } catch (err) {
    console.error('Failed to fetch privileges:', err);
    throw err;
  }
}

/**
 * Create privileges list machine
 */
export function createPrivilegeListMachine() {
  return createListMachine<Privilege, PrivilegeFilters>({
    id: 'privileges-list',
    context: {
      sortField: 'name',
      sortDirection: 'asc',
      filters: {
        filter: '',
        typeFilter: [],
        readOnlyFilter: [],
      },
    },
  }).withConfig({
    services: {
      fetchData: async () => {
        const data = await fetchPrivileges();
        return data;
      },
    },
    actions: {
      filterData: (context: ListMachineContext<Privilege, PrivilegeFilters>) => {
        const { pristineData, filters } = context;

        return pristineData.filter((priv) => {
          // Text filter - search name, description, permission
          if (filters.filter) {
            const searchLower = filters.filter.toLowerCase();
            const matchesName = priv.name?.toLowerCase().includes(searchLower);
            const matchesDescription = priv.description?.toLowerCase().includes(searchLower);
            const matchesPermission = priv.permission?.toLowerCase().includes(searchLower);
            if (!matchesName && !matchesDescription && !matchesPermission) {
              return false;
            }
          }

          // Type filter
          if (filters.typeFilter.length > 0 && !filters.typeFilter.includes(priv.type)) {
            return false;
          }

          // Read-only / locked filter: 'locked' = readOnly only, 'unlocked' = editable only
          if (filters.readOnlyFilter && filters.readOnlyFilter.length > 0) {
            const isReadOnly = priv.readOnly === true;
            if (filters.readOnlyFilter.includes('locked') && !filters.readOnlyFilter.includes('unlocked')) {
              if (!isReadOnly) return false;
            } else if (filters.readOnlyFilter.includes('unlocked') && !filters.readOnlyFilter.includes('locked')) {
              if (isReadOnly) return false;
            }
          }

          return true;
        });
      },
    },
  });
}
