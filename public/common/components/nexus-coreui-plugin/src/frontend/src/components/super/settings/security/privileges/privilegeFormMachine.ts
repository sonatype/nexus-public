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

import { assign } from 'xstate';
import { createFormMachine, ENDPOINTS, restClient, API_INTERNAL_UI } from '@sonatype/nexus-ui-plugin';
import type { FormContext, ValidationErrors } from '@sonatype/nexus-ui-plugin';

/**
 * Guard factory: creates a guard that checks if a TYPE_CHANGE event targets a specific type,
 * or if the current data type matches (for initial load).
 */
const isTypeGuard = (targetType: string) =>
  (context: any, event: any) => {
    if (event.type === 'TYPE_CHANGE') {
      return event.value === targetType;
    }
    return context.data?.type === targetType;
  };
import {
  Privilege,
  PrivilegeType,
  PrivilegeFormData,
  PRIVILEGE_TYPES,
  ContentSelector,
} from './types';
import { CONTENT_SELECTOR_API } from '../../repository/selectors/types';

// Internal UI endpoints for privilege types
const PRIVILEGE_TYPES_URL = `${API_INTERNAL_UI}/privileges/types`;

/**
 * Validate privilege form data
 * Returns an object with field names as keys and error messages as values
 */
function validatePrivilege(data: PrivilegeFormData): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.name?.trim()) {
    errors.name = 'Privilege name is required';
  } else if (!/^[a-zA-Z0-9_-]+$/.test(data.name)) {
    errors.name = 'Name can only contain letters, numbers, underscores, and hyphens';
  }

  if (!data.type) {
    errors.type = 'Privilege type is required';
  }

  // Type-specific validation using nested property paths
  if (data.type === PRIVILEGE_TYPES.WILDCARD) {
    if (!data.properties?.pattern?.trim()) {
      errors['properties.pattern'] = 'Pattern is required for wildcard privileges';
    }
  }

  if (data.type === PRIVILEGE_TYPES.APPLICATION) {
    if (!data.properties?.domain?.trim()) {
      errors['properties.domain'] = 'Domain is required';
    }
    if (!data.properties?.actions?.trim()) {
      errors['properties.actions'] = 'Actions are required';
    }
  }

  if (
    data.type === PRIVILEGE_TYPES.REPOSITORY_VIEW ||
    data.type === PRIVILEGE_TYPES.REPOSITORY_ADMIN
  ) {
    if (!data.properties?.format?.trim()) {
      errors['properties.format'] = 'Repository format is required';
    }
    if (!data.properties?.repository?.trim()) {
      errors['properties.repository'] = 'Repository is required';
    }
    if (!data.properties?.actions?.trim()) {
      errors['properties.actions'] = 'Actions are required';
    }
  }

  if (data.type === PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR) {
    if (!data.properties?.repository?.trim()) {
      errors['properties.repository'] = 'Repository is required';
    }
    if (!data.properties?.contentSelector?.trim()) {
      errors['properties.contentSelector'] = 'Content selector is required';
    }
    if (!data.properties?.actions?.trim()) {
      errors['properties.actions'] = 'Actions are required';
    }
  }

  if (data.type === PRIVILEGE_TYPES.SCRIPT) {
    if (!data.properties?.name?.trim()) {
      errors['properties.name'] = 'Script name is required';
    }
    if (!data.properties?.actions?.trim()) {
      errors['properties.actions'] = 'Actions are required';
    }
  }

  return errors;
}

/**
 * REST API privilege shape
 */
interface RestPrivilege {
  type: string;
  name: string;
  description?: string;
  readOnly: boolean;
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
 * Find privilege by ID and transform to UI model
 */
async function findPrivilege(id: string): Promise<Privilege | null> {
  try {
    const data = await restClient.get<RestPrivilege>(`${ENDPOINTS.PRIVILEGES}/${id}`);
    return transformPrivilege(data);
  } catch (err) {
    console.error('Failed to load privilege:', err);
    throw err;
  }
}

/**
 * Fetch privilege types from internal UI endpoint
 */
async function fetchPrivilegeTypes(): Promise<PrivilegeType[]> {
  try {
    const data = await restClient.get(PRIVILEGE_TYPES_URL);
    return data as PrivilegeType[];
  } catch (err) {
    console.error('Failed to load privilege types:', err);
    return [];
  }
}

/**
 * Create a privilege form machine with XState
 * This factory allows creating a machine configured for create or edit mode
 */
export function createPrivilegeFormMachine(
  privilegeId: string | undefined,
  preloadedPrivilege?: Privilege,
  initialTypeId?: string
) {
  return createFormMachine({
    id: `privilege-form-${privilegeId ?? 'new'}`,
    context: {
      data: {
        name: '',
        description: '',
        type: initialTypeId || preloadedPrivilege?.type || PRIVILEGE_TYPES.APPLICATION,
        properties: {},
      } as PrivilegeFormData,
      // Initialize reference data as empty - will be populated by load service
      privilege: preloadedPrivilege ?? (null as Privilege | null),
      privilegeTypes: [] as PrivilegeType[],
      repositories: [] as { name: string; format: string; type: string; status: { online: boolean } }[],
      formats: [] as string[],
      contentSelectors: [] as ContentSelector[],
      scripts: [] as { name: string }[],
    },
    actions: {
      validate: assign((ctx: FormContext<PrivilegeFormData>) => ({
        validationErrors: validatePrivilege(ctx.data),
      })),
      changeType: assign((context: any, event: any) => ({
        data: { ...context.data, type: event.value, properties: {} },
        touched: { ...context.touched, type: true },
      })),
    },
    // Guards for TYPE_CHANGE transitions between sub-states
    guards: {
      isTypeApplication: isTypeGuard(PRIVILEGE_TYPES.APPLICATION) as any,
      isTypeWildcard: isTypeGuard(PRIVILEGE_TYPES.WILDCARD) as any,
      isTypeRepositoryView: isTypeGuard(PRIVILEGE_TYPES.REPOSITORY_VIEW) as any,
      isTypeRepositoryAdmin: isTypeGuard(PRIVILEGE_TYPES.REPOSITORY_ADMIN) as any,
      isTypeRepositoryContentSelector: isTypeGuard(PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR) as any,
      isTypeScript: isTypeGuard(PRIVILEGE_TYPES.SCRIPT) as any,
    },
    services: {
      load: async () => {
        // Load privilege and reference data in parallel
        const [privilege, privilegeTypes, repos, selectors, scriptsData] = await Promise.all([
          // If privilege is preloaded, use it; otherwise fetch if privilegeId is provided
          preloadedPrivilege
            ? Promise.resolve(preloadedPrivilege)
            : privilegeId
            ? findPrivilege(privilegeId).catch((err: unknown) => {
                console.error('Failed to load privilege:', err);
                throw err; // Re-throw so form machine catches it
              })
            : Promise.resolve(null),
          fetchPrivilegeTypes().catch((err: unknown) => {
            console.error('Failed to load privilege types:', err);
            return [] as PrivilegeType[];
          }),
          restClient
            .get('/service/rest/v1/repositories')
            .then((data: unknown) => data as { name: string; format: string; type: string; online: boolean }[])
            .then((repos) => repos.map(r => ({
              name: r.name,
              format: r.format,
              type: r.type,
              status: { online: r.online }
            })))
            .catch((err: unknown) => {
              console.warn('Could not load repositories for privilege form:', err);
              return [] as { name: string; format: string; type: string; status: { online: boolean } }[];
            }),
          restClient
            .get(CONTENT_SELECTOR_API.BASE_URL)
            .then((data: unknown) => data as ContentSelector[])
            .catch((err: unknown) => {
              console.warn('Could not load content selectors:', err);
              return [] as ContentSelector[];
            }),
          restClient
            .get(ENDPOINTS.SCRIPTS)
            .then((data: unknown) => data as { name: string }[])
            .catch((err: unknown) => {
              console.warn('Could not load scripts:', err);
              return [] as { name: string }[];
            }),
        ]);

        // Return raw data arrays (no UI formatting)
        const repositories = Array.isArray(repos) ? repos : [];
        const formats = Array.isArray(repos) ? [...new Set(repos.map((r) => r.format))].sort() : [];
        const contentSelectors = Array.isArray(selectors) ? selectors : [];
        const scripts = Array.isArray(scriptsData) ? scriptsData : [];

        // Build initial form data from loaded privilege or use defaults
        const initialData: PrivilegeFormData = privilege
          ? {
              name: privilege.name,
              description: privilege.description,
              type: privilege.type,
              properties: privilege.properties,
              version: privilege.version,
            }
          : {
              name: '',
              description: '',
              type: initialTypeId || PRIVILEGE_TYPES.APPLICATION,
              properties: {},
            };

        return {
          data: initialData,
          privilege,
          privilegeTypes,
          repositories,
          formats,
          contentSelectors,
          scripts,
        };
      },
      // save service is provided via useForm options
    },
    // Custom event for privilege type changes (transitions to correct sub-state)
    on: {
      TYPE_CHANGE: [
        { target: '.application', cond: 'isTypeApplication', actions: ['changeType', 'validate', 'computePristine'] },
        { target: '.wildcard', cond: 'isTypeWildcard', actions: ['changeType', 'validate', 'computePristine'] },
        { target: '.repository-view', cond: 'isTypeRepositoryView', actions: ['changeType', 'validate', 'computePristine'] },
        { target: '.repository-admin', cond: 'isTypeRepositoryAdmin', actions: ['changeType', 'validate', 'computePristine'] },
        { target: '.repository-content-selector', cond: 'isTypeRepositoryContentSelector', actions: ['changeType', 'validate', 'computePristine'] },
        { target: '.script', cond: 'isTypeScript', actions: ['changeType', 'validate', 'computePristine'] },
      ],
    },
    // Privilege type variant sub-states within the editing state.
    // Each sub-state declares metadata about which fields are visible and required
    // for that type variant. This enables:
    // 1. The component to read field config from machine state (no switch/case)
    // 2. Model-based testing that auto-generates paths through every variant
    // 3. Single source of truth for form structure
    editingConfig: {
      defaultState: PRIVILEGE_TYPES.APPLICATION,
      typeField: 'type',
      states: {
        [PRIVILEGE_TYPES.APPLICATION]: {
          always: [
            { target: PRIVILEGE_TYPES.WILDCARD, cond: 'isTypeWildcard' },
            { target: PRIVILEGE_TYPES.REPOSITORY_VIEW, cond: 'isTypeRepositoryView' },
            { target: PRIVILEGE_TYPES.REPOSITORY_ADMIN, cond: 'isTypeRepositoryAdmin' },
            { target: PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR, cond: 'isTypeRepositoryContentSelector' },
            { target: PRIVILEGE_TYPES.SCRIPT, cond: 'isTypeScript' },
          ],
          meta: {
            typeLabel: 'Application',
            fields: ['properties.domain', 'properties.actions'],
            requiredFields: ['properties.domain', 'properties.actions'],
            fieldConfig: {
              'properties.domain': { label: 'Domain', type: 'combobox', allowCustom: true },
              'properties.actions': { label: 'Actions', type: 'checkboxGroup' },
            },
          },
        },
        [PRIVILEGE_TYPES.WILDCARD]: {
          meta: {
            typeLabel: 'Wildcard',
            fields: ['properties.pattern'],
            requiredFields: ['properties.pattern'],
            fieldConfig: {
              'properties.pattern': { label: 'Pattern', type: 'text', helpText: 'Wildcard pattern (e.g., nexus:*)' },
            },
          },
        },
        [PRIVILEGE_TYPES.REPOSITORY_VIEW]: {
          meta: {
            typeLabel: 'Repository View',
            fields: ['properties.format', 'properties.repository', 'properties.actions'],
            requiredFields: ['properties.format', 'properties.repository', 'properties.actions'],
            fieldConfig: {
              'properties.format': { label: 'Repository Format', type: 'combobox', allowCustom: true },
              'properties.repository': { label: 'Repository', type: 'combobox', allowCustom: true },
              'properties.actions': { label: 'Actions', type: 'checkboxGroup' },
            },
          },
        },
        [PRIVILEGE_TYPES.REPOSITORY_ADMIN]: {
          meta: {
            typeLabel: 'Repository Admin',
            fields: ['properties.format', 'properties.repository', 'properties.actions'],
            requiredFields: ['properties.format', 'properties.repository', 'properties.actions'],
            fieldConfig: {
              'properties.format': { label: 'Repository Format', type: 'combobox', allowCustom: true },
              'properties.repository': { label: 'Repository', type: 'combobox', allowCustom: true },
              'properties.actions': { label: 'Actions', type: 'checkboxGroup' },
            },
          },
        },
        [PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR]: {
          meta: {
            typeLabel: 'Repository Content Selector',
            fields: ['properties.format', 'properties.repository', 'properties.contentSelector', 'properties.actions'],
            requiredFields: ['properties.format', 'properties.repository', 'properties.contentSelector', 'properties.actions'],
            fieldConfig: {
              'properties.format': { label: 'Repository Format', type: 'combobox', allowCustom: true },
              'properties.repository': { label: 'Repository', type: 'combobox', allowCustom: true },
              'properties.contentSelector': { label: 'Content Selector', type: 'combobox' },
              'properties.actions': { label: 'Actions', type: 'checkboxGroup' },
            },
          },
        },
        [PRIVILEGE_TYPES.SCRIPT]: {
          meta: {
            typeLabel: 'Script',
            fields: ['properties.name', 'properties.actions'],
            requiredFields: ['properties.name', 'properties.actions'],
            fieldConfig: {
              'properties.name': { label: 'Script Name', type: 'combobox', allowCustom: true },
              'properties.actions': { label: 'Actions', type: 'checkboxGroup' },
            },
          },
        },
      },
    },
  });
}
