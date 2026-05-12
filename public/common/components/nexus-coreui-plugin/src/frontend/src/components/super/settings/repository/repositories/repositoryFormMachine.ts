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
import { createFormMachine, ENDPOINTS, restClient, API_INTERNAL_UI, ExtJS } from '@sonatype/nexus-ui-plugin';
import type { FormContext, ValidationErrors } from '@sonatype/nexus-ui-plugin';

import {
  Repository,
  RepositoryFormData,
  RepositoryType,
  BlobStore,
  RoutingRule,
  CleanupPolicy,
  RepositoryReference,
  Recipe,
  NAME_PATTERN,
  NAME_PATTERN_MESSAGE,
  DEFAULT_HOSTED_VALUES,
  DEFAULT_PROXY_VALUES,
  DEFAULT_GROUP_VALUES,
} from './types';

// =============================================================================
// Constants
// =============================================================================

/** Repository type constants for machine sub-states */
export const REPOSITORY_TYPES = {
  HOSTED: 'hosted' as const,
  PROXY: 'proxy' as const,
  GROUP: 'group' as const,
};

// Internal UI endpoints for reference data
const RECIPES_URL = `${API_INTERNAL_UI}/repositories/recipes`;
const REPOSITORIES_LIST_URL = `${API_INTERNAL_UI}/repositories`;
const CLEANUP_POLICIES_URL = '/service/rest/v1/cleanup-policies';

// =============================================================================
// Guards
// =============================================================================

/**
 * Guard factory: creates a guard that checks if a TYPE_CHANGE event targets a specific type.
 */
const isTypeGuard = (targetType: string) =>
  (_context: unknown, event: { type: string; value?: string }) => event.value === targetType;

// =============================================================================
// Validation
// =============================================================================

/**
 * Validate repository form data.
 * Returns flat validation errors using dot notation for nested fields,
 * compatible with the createFormMachine validation contract.
 */
export function validateRepository(data: RepositoryFormData): ValidationErrors {
  const errors: ValidationErrors = {};

  // Name is always required
  if (!data.name?.trim()) {
    errors.name = 'Name is required';
  } else if (!NAME_PATTERN.test(data.name)) {
    errors.name = NAME_PATTERN_MESSAGE;
  }

  // Blob store is always required
  if (!data.storage?.blobStoreName?.trim()) {
    errors['storage.blobStoreName'] = 'Blob store is required';
  }

  // Proxy-specific validation
  if (data.type === 'proxy') {
    if (!data.proxy?.remoteUrl?.trim()) {
      errors['proxy.remoteUrl'] = 'Remote URL is required';
    } else {
      try {
        new URL(data.proxy.remoteUrl);
      } catch {
        errors['proxy.remoteUrl'] = 'Invalid URL format';
      }
    }
  }

  // Group-specific validation
  if (data.type === 'group') {
    if (!data.group?.memberNames || data.group.memberNames.length === 0) {
      errors['group.memberNames'] = 'At least one member repository is required';
    }
  }

  // Format-specific validation
  if (data.format === 'apt' && data.type === 'hosted') {
    if (!data.aptSigning?.keypair?.trim()) {
      errors['aptSigning.keypair'] = 'GPG signing key is required for APT hosted repositories';
    }
  }

  return errors;
}

// =============================================================================
// API Helpers
// =============================================================================

/**
 * Fetch a single repository by name from the REST API
 */
async function findRepository(name: string): Promise<Repository | null> {
  try {
    const data = await restClient.get<Repository>(
      `${ENDPOINTS.REPOSITORIES}/${encodeURIComponent(name)}`
    );
    return data || null;
  } catch (err) {
    console.error('Failed to load repository:', err);
    throw err;
  }
}

/**
 * Build form data from a loaded repository (handles both flat and nested API response formats)
 */
function buildFormDataFromRepository(repo: Repository): RepositoryFormData {
  const repoData = repo as Repository & Record<string, unknown>;

  const getConfig = <T>(key: string): T | undefined => {
    return (repoData[key] as T) ?? (repo.attributes as Record<string, T> | undefined)?.[key];
  };

  const storageConfig = getConfig<{
    blobStoreName?: string;
    strictContentTypeValidation?: boolean;
    writePolicy?: string;
  }>('storage');

  const blobStoreName =
    storageConfig?.blobStoreName ||
    (repoData.blobStoreName as string | undefined) ||
    repo.attributes?.storage?.blobStoreName ||
    '';

  return {
    name: repo.name,
    type: repo.type,
    format: repo.format,
    recipe: repo.recipe || `${repo.format}-${repo.type}`,
    online: repo.online ?? true,
    routingRuleId: repo.routingRuleId ?? (repoData.routingRuleName as string | null | undefined),
    storage: {
      blobStoreName,
      strictContentTypeValidation: storageConfig?.strictContentTypeValidation ?? true,
      writePolicy: storageConfig?.writePolicy as 'ALLOW' | 'ALLOW_ONCE' | 'DENY' | undefined,
    },
    proxy: getConfig('proxy'),
    negativeCache: getConfig('negativeCache'),
    httpClient: getConfig('httpClient'),
    group: getConfig('group'),
    cleanup: getConfig('cleanup'),
    component: getConfig('component'),
    replication: getConfig('replication'),
    maven: getConfig('maven'),
    docker: getConfig('docker'),
    dockerProxy: getConfig('dockerProxy'),
    npm: getConfig('npm'),
    nugetProxy: getConfig('nugetProxy'),
    apt: getConfig('apt'),
    aptSigning: getConfig('aptSigning'),
    yum: getConfig('yum'),
    raw: getConfig('raw'),
    terraformSigning: getConfig('terraformSigning'),
  };
}

/**
 * Build default form data for a new repository based on format and type
 */
function buildDefaultFormData(format: string, type: RepositoryType): RepositoryFormData {
  const baseValues: RepositoryFormData = {
    name: '',
    type,
    format,
    recipe: `${format}-${type}`,
    online: true,
    storage: {
      blobStoreName: '',
      strictContentTypeValidation: true,
    },
  };

  switch (type) {
    case 'hosted':
      return { ...baseValues, ...DEFAULT_HOSTED_VALUES } as RepositoryFormData;
    case 'proxy':
      return { ...baseValues, ...DEFAULT_PROXY_VALUES } as RepositoryFormData;
    case 'group':
      return { ...baseValues, ...DEFAULT_GROUP_VALUES } as RepositoryFormData;
    default:
      return baseValues;
  }
}

// =============================================================================
// Machine Factory
// =============================================================================

export interface RepositoryFormMachineOptions {
  /** Repository name for edit mode; undefined for create mode */
  repositoryName?: string;
  /** Pre-loaded repository to avoid re-fetching */
  preloadedRepository?: Repository;
  /** Format for the repository (e.g., 'maven2', 'npm', 'docker') */
  format: string;
  /** Repository type for create mode (defaults to 'hosted') */
  repositoryType?: RepositoryType;
}

/**
 * Create a repository form machine with XState.
 *
 * The machine models repository types (hosted/proxy/group) as sub-states
 * within the editing state. Each sub-state declares field metadata for
 * the UI component to consume without switch/case logic.
 *
 * Format is a context field that affects validation, not a sub-state.
 * Format-specific fields (maven, docker, apt, etc.) are rendered based
 * on the format value in context.data.format.
 *
 * Supports TYPE_CHANGE transitions between hosted/proxy/group, which:
 * 1. Transition to the correct sub-state
 * 2. Reset type-specific fields
 * 3. Apply default values for the new type
 * 4. Preserve common fields (name, format, blobStoreName)
 */
export function createRepositoryFormMachine(options: RepositoryFormMachineOptions) {
  const { repositoryName, preloadedRepository, format, repositoryType = 'hosted' } = options;

  const initialType: RepositoryType = preloadedRepository?.type ?? repositoryType;
  const initialData = preloadedRepository
    ? buildFormDataFromRepository(preloadedRepository)
    : buildDefaultFormData(format, initialType);

  return createFormMachine({
    id: `repository-form-${repositoryName ?? 'new'}`,
    context: {
      data: initialData,
      // Reference data - populated by the load service
      repository: preloadedRepository ?? (null as Repository | null),
      blobStores: [] as BlobStore[],
      routingRules: [] as RoutingRule[],
      cleanupPolicies: [] as CleanupPolicy[],
      memberRepositories: [] as RepositoryReference[],
      recipes: [] as Recipe[],
    },
    actions: {
      validate: assign((ctx: FormContext<RepositoryFormData>) => ({
        validationErrors: validateRepository(ctx.data),
      })),
      // Custom action: update type, reset type-specific fields, apply defaults.
      // Preserves common fields (name, format, online, blobStoreName).
      changeType: assign((context: any, event: any) => {
        const newType = event.value as RepositoryType;
        const currentData = context.data as RepositoryFormData;

        // Preserve fields common to all types
        const preserved = {
          name: currentData.name,
          format: currentData.format,
          online: currentData.online,
        };

        // Get default values for the new type
        let typeDefaults: Partial<RepositoryFormData>;
        switch (newType) {
          case 'hosted':
            typeDefaults = { ...DEFAULT_HOSTED_VALUES };
            break;
          case 'proxy':
            typeDefaults = { ...DEFAULT_PROXY_VALUES };
            break;
          case 'group':
            typeDefaults = { ...DEFAULT_GROUP_VALUES };
            break;
          default:
            typeDefaults = {};
        }

        // Build new data: type defaults + preserved common fields + explicit overrides
        const newData: RepositoryFormData = {
          ...typeDefaults,
          ...preserved,
          type: newType,
          recipe: `${currentData.format}-${newType}`,
          storage: {
            ...(typeDefaults.storage || { blobStoreName: '', strictContentTypeValidation: true }),
            blobStoreName: currentData.storage?.blobStoreName || '',
          },
        } as RepositoryFormData;

        return {
          data: newData,
          touched: { ...context.touched, type: true },
        };
      }),
    },
    // Guards for TYPE_CHANGE transitions between sub-states
    guards: {
      isTypeHosted: isTypeGuard(REPOSITORY_TYPES.HOSTED) as any,
      isTypeProxy: isTypeGuard(REPOSITORY_TYPES.PROXY) as any,
      isTypeGroup: isTypeGuard(REPOSITORY_TYPES.GROUP) as any,
    },
    services: {
      load: async () => {
        // Load repository and reference data in parallel
        const [repository, blobStoresData, routingRulesData, cleanupData, recipesData, memberData] =
          await Promise.all([
            // Load repository if editing (use preloaded if available)
            preloadedRepository
              ? Promise.resolve(preloadedRepository)
              : repositoryName
                ? findRepository(repositoryName).catch((err: unknown) => {
                    console.error('Failed to load repository:', err);
                    throw err;
                  })
                : Promise.resolve(null),
            // Load blob stores
            (ExtJS.state()?.getValue('isCloud', false)
              ? Promise.resolve([] as BlobStore[])
              : restClient
                  .get(ENDPOINTS.BLOBSTORES)
                  .then((data: unknown) => data as BlobStore[])
                  .catch((err: unknown) => {
                    console.warn('Could not load blob stores:', err);
                    return [] as BlobStore[];
                  })),
            // Load routing rules
            restClient
              .get(ENDPOINTS.ROUTING_RULES)
              .then((data: unknown) => data as RoutingRule[])
              .catch((err: unknown) => {
                console.warn('Could not load routing rules:', err);
                return [] as RoutingRule[];
              }),
            // Load cleanup policies
            restClient
              .get(CLEANUP_POLICIES_URL)
              .then((data: unknown) => data as CleanupPolicy[])
              .catch((err: unknown) => {
                console.warn('Could not load cleanup policies:', err);
                return [] as CleanupPolicy[];
              }),
            // Load recipes
            restClient
              .get(RECIPES_URL)
              .then((data: unknown) => {
                const rawRecipes = data as Array<{ format: string; type: string }>;
                return Array.isArray(rawRecipes)
                  ? rawRecipes.map((r) => ({ ...r, name: `${r.format}-${r.type}` }))
                  : [];
              })
              .catch((err: unknown) => {
                console.warn('Could not load recipes:', err);
                return [] as Recipe[];
              }),
            // Load member repositories (always load - may be needed if type changes to group)
            restClient
              .get(REPOSITORIES_LIST_URL)
              .then((data: unknown) => {
                const repos = data as Array<{ name: string; format: string; type: string }>;
                return Array.isArray(repos)
                  ? repos.map((r) => ({
                      id: r.name,
                      name: r.name,
                      format: r.format,
                      type: r.type as RepositoryType,
                    }))
                  : [];
              })
              .catch((err: unknown) => {
                console.warn('Could not load repository references:', err);
                return [] as RepositoryReference[];
              }),
          ]);

        // Process reference data
        const blobStores = Array.isArray(blobStoresData) ? blobStoresData : [];
        const routingRules = Array.isArray(routingRulesData) ? routingRulesData : [];
        const cleanupPolicies = Array.isArray(cleanupData) ? cleanupData : [];
        const recipes = Array.isArray(recipesData) ? recipesData : [];
        const memberRepositories = Array.isArray(memberData)
          ? memberData.filter((r) => r.name !== repositoryName && r.type !== 'group')
          : [];

        // Build form data from loaded repository or use defaults
        const formData: RepositoryFormData = repository
          ? buildFormDataFromRepository(repository as Repository)
          : buildDefaultFormData(format, initialType);

        return {
          data: formData,
          repository: repository as Repository | null,
          blobStores,
          routingRules,
          cleanupPolicies,
          recipes,
          memberRepositories,
        };
      },
      // save service is provided via useForm options
    },
    // Custom event for repository type changes (transitions to correct sub-state)
    on: {
      TYPE_CHANGE: [
        {
          target: '.hosted',
          cond: 'isTypeHosted',
          actions: ['changeType', 'validate', 'computePristine'],
        },
        {
          target: '.proxy',
          cond: 'isTypeProxy',
          actions: ['changeType', 'validate', 'computePristine'],
        },
        {
          target: '.group',
          cond: 'isTypeGroup',
          actions: ['changeType', 'validate', 'computePristine'],
        },
      ],
    },
    // Repository type variant sub-states within the editing state.
    // Each sub-state declares metadata about which fields are visible and required
    // for that type variant. This enables:
    // 1. The component to read field config from machine state (no switch/case)
    // 2. Model-based testing that auto-generates paths through every variant
    // 3. Single source of truth for form structure per repository type
    editingConfig: {
      defaultState: initialType,
      typeField: 'type',
      states: {
        [REPOSITORY_TYPES.HOSTED]: {
          meta: {
            typeLabel: 'Hosted',
            fields: [
              'storage.blobStoreName',
              'storage.strictContentTypeValidation',
              'storage.writePolicy',
              'component.proprietaryComponents',
              'cleanup.policyNames',
            ],
            requiredFields: ['storage.blobStoreName'],
            fieldConfig: {
              'storage.blobStoreName': { label: 'Blob Store', type: 'select' },
              'storage.strictContentTypeValidation': {
                label: 'Strict Content Type Validation',
                type: 'checkbox',
              },
              'storage.writePolicy': { label: 'Deployment Policy', type: 'select' },
              'component.proprietaryComponents': {
                label: 'Proprietary Components',
                type: 'checkbox',
              },
              'cleanup.policyNames': { label: 'Cleanup Policies', type: 'multiselect' },
            },
          },
        },
        [REPOSITORY_TYPES.PROXY]: {
          meta: {
            typeLabel: 'Proxy',
            fields: [
              'storage.blobStoreName',
              'storage.strictContentTypeValidation',
              'proxy.remoteUrl',
              'proxy.contentMaxAge',
              'proxy.metadataMaxAge',
              'negativeCache.enabled',
              'negativeCache.timeToLive',
              'httpClient.blocked',
              'httpClient.autoBlock',
              'httpClient.authentication',
              'routingRuleId',
              'cleanup.policyNames',
              'replication.preemptivePullEnabled',
            ],
            requiredFields: ['storage.blobStoreName', 'proxy.remoteUrl'],
            fieldConfig: {
              'storage.blobStoreName': { label: 'Blob Store', type: 'select' },
              'storage.strictContentTypeValidation': {
                label: 'Strict Content Type Validation',
                type: 'checkbox',
              },
              'proxy.remoteUrl': {
                label: 'Remote Storage URL',
                type: 'text',
                helpText: 'Location of the remote repository being proxied',
              },
              'proxy.contentMaxAge': { label: 'Maximum Component Age', type: 'number' },
              'proxy.metadataMaxAge': { label: 'Maximum Metadata Age', type: 'number' },
              'negativeCache.enabled': { label: 'Not Found Cache Enabled', type: 'checkbox' },
              'negativeCache.timeToLive': { label: 'Not Found Cache TTL', type: 'number' },
              'httpClient.blocked': { label: 'Block Outbound Connections', type: 'checkbox' },
              'httpClient.autoBlock': {
                label: 'Auto-block Outbound Connections',
                type: 'checkbox',
              },
              'httpClient.authentication': { label: 'Authentication', type: 'fieldset' },
              'routingRuleId': { label: 'Routing Rule', type: 'select' },
              'cleanup.policyNames': { label: 'Cleanup Policies', type: 'multiselect' },
              'replication.preemptivePullEnabled': { label: 'Preemptive Pull', type: 'checkbox' },
            },
          },
        },
        [REPOSITORY_TYPES.GROUP]: {
          meta: {
            typeLabel: 'Group',
            fields: [
              'storage.blobStoreName',
              'storage.strictContentTypeValidation',
              'group.memberNames',
              'group.writableMember',
              'routingRuleId',
            ],
            requiredFields: ['storage.blobStoreName', 'group.memberNames'],
            fieldConfig: {
              'storage.blobStoreName': { label: 'Blob Store', type: 'select' },
              'storage.strictContentTypeValidation': {
                label: 'Strict Content Type Validation',
                type: 'checkbox',
              },
              'group.memberNames': {
                label: 'Member Repositories',
                type: 'transfer',
                helpText: 'Select repositories to include in this group',
              },
              'group.writableMember': {
                label: 'Writable Member',
                type: 'select',
                helpText: 'Select a hosted member for deploy',
              },
              'routingRuleId': { label: 'Routing Rule', type: 'select' },
            },
          },
        },
      },
    },
  });
}
