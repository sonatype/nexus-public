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
import { API_INTERNAL_UI, ENDPOINTS, restClient } from '../../../../../../interface/api';
import { createFormMachine, type FormContext, type ValidationErrors } from '../../../../../../interface/form';

import type {
  BlobStoreTypeDescriptor as BlobStoreTypeInfo,
  QuotaType,
} from './types';

/**
 * Blob store type IDs (lowercase, matching the REST API type path segments).
 */
export const BLOB_STORE_TYPE_IDS = {
  FILE: 'file',
  S3: 's3',
  AZURE: 'azure',
  GOOGLE: 'google',
  GROUP: 'group',
} as const;

export type BlobStoreTypeId = (typeof BLOB_STORE_TYPE_IDS)[keyof typeof BLOB_STORE_TYPE_IDS];

/**
 * Internal UI endpoints for blob store reference data
 */
const BLOB_STORES_TYPES_URL = `${API_INTERNAL_UI}/blobstores/types`;
const BLOB_STORES_QUOTA_TYPES_URL = `${API_INTERNAL_UI}/blobstores/quotaTypes`;
const BLOB_STORES_USAGE_URL = (name: string) =>
  `${API_INTERNAL_UI}/blobstores/usage/${encodeURIComponent(name)}`;

/**
 * Unified form data for blob store creation/update.
 * Covers all type variants; unused fields default to empty values.
 */
export interface BlobStoreFormData {
  name: string;
  type: string;
  softQuota: {
    enabled: boolean;
    type?: string;
    limit?: number;
  };
  // File-specific
  path: string;
  // S3/Azure/Google-specific (polymorphic nested config)
  bucketConfiguration: Record<string, unknown>;
  // Group-specific
  members: string[];
  fillPolicy: string;
}

/**
 * REST API blob store shape (from GET /v1/blobstores/{type}/{name})
 */
interface RestBlobStore {
  name: string;
  type?: string;
  path?: string;
  softQuota?: { type: string; limit: number };
  bucketConfiguration?: Record<string, unknown>;
  members?: string[];
  fillPolicy?: string;
  [key: string]: unknown;
}

/**
 * Usage response from internal UI
 */
interface BlobStoreUsage {
  blobStoreUsage: number;
  repositoryUsage: number;
}

/**
 * Guard factory: checks if a TYPE_CHANGE event targets a specific type.
 */
const isTypeGuard = (targetType: string) =>
  (_context: unknown, event: { type: string; value?: string }) => event.value === targetType;

/**
 * Used to extract token strings "${name}" => "name"
 */
const tokenMatcher = /\$\{(.*?)\}/g;
function stripTokenCharacters(match: string): string {
  return match.replace('${', '').replace('}', '');
}

/**
 * Replace tokens in a string with values from a data object.
 * Example: replaceTokens("${name}/path", { name: "my-store" }) => "my-store/path"
 */
function replaceTokens(template: string, data: Record<string, unknown>): string {
  return template.replace(tokenMatcher, (match) => {
    const key = stripTokenCharacters(match);
    return String(data[key] ?? '');
  });
}

/**
 * Find the tokenReplacement attribute from blob store type fields.
 * Returns the tokenReplacement string for the path field (file blob store).
 */
function findPathTokenReplacement(blobStoreTypes: BlobStoreTypeInfo[]): string | undefined {
  const fileType = blobStoreTypes.find(t => t.id.toLowerCase() === 'file');
  if (!fileType?.fields) return undefined;

  const pathField = fileType.fields.find(f => f.id === 'path');
  return pathField?.attributes?.tokenReplacement;
}

/**
 * Build the default form data for a given blob store type.
 * Resets all type-specific fields and initializes the target type's defaults.
 */
function buildDefaultsForType(
  type: string,
  base: Partial<BlobStoreFormData>,
  blobStoreTypes?: BlobStoreTypeInfo[]
): BlobStoreFormData {
  const defaults: BlobStoreFormData = {
    name: base.name ?? '',
    type,
    softQuota: base.softQuota ?? { enabled: false },
    path: '',
    bucketConfiguration: {},
    members: [],
    fillPolicy: '',
  };

  switch (type) {
    case BLOB_STORE_TYPE_IDS.FILE:
      // If we have blob store types with tokenReplacement, use it to set default path
      if (blobStoreTypes && base.name) {
        const tokenReplacement = findPathTokenReplacement(blobStoreTypes);
        if (tokenReplacement) {
          defaults.path = replaceTokens(tokenReplacement, { name: base.name });
        }
      }
      break;
    case BLOB_STORE_TYPE_IDS.S3:
      defaults.bucketConfiguration = {
        bucket: { region: 'DEFAULT', name: '', prefix: '' },
        bucketSecurity: {},
        encryption: null,
        advancedBucketConnection: {},
        failoverBuckets: [],
      };
      break;
    case BLOB_STORE_TYPE_IDS.AZURE:
      defaults.bucketConfiguration = {
        accountName: '',
        containerName: '',
        authentication: { authenticationMethod: 'ENVIRONMENTVARIABLE' },
      };
      break;
    case BLOB_STORE_TYPE_IDS.GOOGLE:
      defaults.bucketConfiguration = {
        bucket: { name: '', prefix: '' },
        bucketSecurity: { authenticationMethod: 'applicationDefault' },
        encryption: { encryptionType: 'default' },
      };
      break;
    case BLOB_STORE_TYPE_IDS.GROUP:
      defaults.members = [];
      defaults.fillPolicy = '';
      break;
  }

  return defaults;
}

/**
 * Validate blob store form data.
 * Returns an object with field names as keys and error messages as values.
 */
function validateBlobStore(data: BlobStoreFormData): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.name?.trim()) {
    errors.name = 'Blob store name is required';
  } else if (!/^[a-zA-Z0-9_-]+$/.test(data.name)) {
    errors.name = 'Name can only contain letters, numbers, underscores, and hyphens';
  }

  if (!data.type) {
    errors.type = 'Blob store type is required';
  }

  const type = data.type?.toLowerCase();

  // Type-specific validation
  if (type === BLOB_STORE_TYPE_IDS.FILE) {
    if (!data.path?.trim()) {
      errors.path = 'Path is required for File blob stores';
    }
  }

  if (type === BLOB_STORE_TYPE_IDS.S3) {
    const bucket = (data.bucketConfiguration?.bucket as Record<string, string>) ?? {};
    const bucketName = bucket.name?.trim() ?? '';
    if (!bucketName) {
      errors['bucketConfiguration.bucket.name'] = 'Bucket name is required';
    } else if (bucketName.length < 3 || bucketName.length > 63) {
      errors['bucketConfiguration.bucket.name'] = 'Bucket name must be between 3 and 63 characters';
    }
  }

  if (type === BLOB_STORE_TYPE_IDS.AZURE) {
    const config = data.bucketConfiguration ?? {};
    // Azure storage account names: 3-24 characters (per Azure naming rules).
    const accountName = (config.accountName as string)?.trim() ?? '';
    if (!accountName) {
      errors['bucketConfiguration.accountName'] = 'Account name is required';
    } else if (accountName.length < 3 || accountName.length > 24) {
      errors['bucketConfiguration.accountName'] = 'Account name must be between 3 and 24 characters';
    }
    // Azure container names: 3-63 characters (per Azure naming rules).
    const containerName = (config.containerName as string)?.trim() ?? '';
    if (!containerName) {
      errors['bucketConfiguration.containerName'] = 'Container name is required';
    } else if (containerName.length < 3 || containerName.length > 63) {
      errors['bucketConfiguration.containerName'] = 'Container name must be between 3 and 63 characters';
    }
  }

  if (type === BLOB_STORE_TYPE_IDS.GOOGLE) {
    const bucket = (data.bucketConfiguration?.bucket as Record<string, string>) ?? {};
    const bucketName = bucket.name?.trim() ?? '';
    if (!bucketName) {
      errors['bucketConfiguration.bucket.name'] = 'Bucket name is required';
    } else if (bucketName.length < 3 || bucketName.length > 63) {
      errors['bucketConfiguration.bucket.name'] = 'Bucket name must be between 3 and 63 characters';
    }
  }

  if (type === BLOB_STORE_TYPE_IDS.GROUP) {
    if (!data.members || data.members.length === 0) {
      errors.members = 'At least one member blob store is required';
    }
    if (!data.fillPolicy) {
      errors.fillPolicy = 'Fill policy is required';
    }
  }

  // Soft quota validation
  if (data.softQuota?.enabled) {
    if (!data.softQuota.type) {
      errors['softQuota.type'] = 'Quota type is required when quota is enabled';
    }
    if (!data.softQuota.limit || data.softQuota.limit <= 0) {
      errors['softQuota.limit'] = 'Quota limit must be greater than 0';
    }
  }

  return errors;
}

/**
 * Fetch a blob store by name and type from the REST API.
 * Type is lowercased to match the REST API path segment convention (e.g. "file", not "File").
 */
async function findBlobStore(name: string, type: string): Promise<RestBlobStore | null> {
  try {
    const url = `${ENDPOINTS.BLOBSTORES}/${encodeURIComponent(type.toLowerCase())}/${encodeURIComponent(name)}`;
    const data = await restClient.get<RestBlobStore>(url);
    return data;
  } catch (err) {
    console.error('Failed to load blob store:', err);
    throw err;
  }
}

/**
 * Fetch blob store usage (how many repositories/blob stores reference it).
 */
async function fetchBlobStoreUsage(name: string): Promise<BlobStoreUsage> {
  try {
    const data = await restClient.get<BlobStoreUsage>(BLOB_STORES_USAGE_URL(name));
    return data;
  } catch {
    return { blobStoreUsage: 0, repositoryUsage: 0 };
  }
}

/**
 * Convert bytes to megabytes, matching the save-side conversion in useBlobStores.ts.
 * The REST API stores/returns softQuota.limit in bytes; the form field works in MB.
 */
function bytesToMegaBytes(bytes: number): number {
  return bytes / (1024 * 1024);
}

/**
 * Transform a REST blob store response into the machine's form data shape.
 */
function transformRestToFormData(rest: RestBlobStore, type: string): BlobStoreFormData {
  const formData = buildDefaultsForType(type, { name: rest.name });

  // Soft quota: API returns limit in bytes; form field works in MB.
  if (rest.softQuota) {
    formData.softQuota = {
      enabled: true,
      type: rest.softQuota.type,
      limit: bytesToMegaBytes(rest.softQuota.limit),
    };
  }

  switch (type) {
    case BLOB_STORE_TYPE_IDS.FILE:
      formData.path = rest.path || '';
      break;
    case BLOB_STORE_TYPE_IDS.S3:
    case BLOB_STORE_TYPE_IDS.AZURE:
    case BLOB_STORE_TYPE_IDS.GOOGLE:
      formData.bucketConfiguration = rest.bucketConfiguration || {};
      break;
    case BLOB_STORE_TYPE_IDS.GROUP:
      formData.members = rest.members || [];
      formData.fillPolicy = rest.fillPolicy || '';
      break;
  }

  return formData;
}

/**
 * Create a blob store form machine with XState.
 *
 * Uses createFormMachine() with editingConfig for type variant sub-states.
 * Each blob store type (File, S3, Azure, Google Cloud, Group) has its own
 * sub-state with metadata declaring the type-specific fields, required fields,
 * and field configuration.
 *
 * @param blobStoreName - Name of the blob store to edit, or undefined for create mode.
 * @param blobStoreType - Type ID of the blob store (for edit mode URL construction).
 * @param preloadedBlobStore - Optional pre-loaded blob store data to avoid re-fetching.
 */
export function createBlobStoreFormMachine(
  blobStoreName: string | undefined,
  blobStoreType?: string,
  preloadedBlobStore?: RestBlobStore
) {
  const initialType = blobStoreType?.toLowerCase() || BLOB_STORE_TYPE_IDS.FILE;

  return createFormMachine({
    id: `blob-store-form-${blobStoreName ?? 'new'}`,
    context: {
      data: buildDefaultsForType(initialType, {}) as BlobStoreFormData,
      // Reference data - populated by load service
      blobStore: null as RestBlobStore | null,
      blobStoreTypes: [] as BlobStoreTypeInfo[],
      quotaTypes: [] as QuotaType[],
      usage: { blobStoreUsage: 0, repositoryUsage: 0 } as BlobStoreUsage,
      showDeleteModal: false,
    },
    actions: {
      validate: assign((ctx: FormContext<BlobStoreFormData>) => ({
        validationErrors: validateBlobStore(ctx.data),
      })),
      // Custom action: update type and reset type-specific configuration
      changeType: assign((context: any, event: any) => ({
        data: buildDefaultsForType(event.value, {
          name: context.data.name,
          softQuota: context.data.softQuota,
        }, context.blobStoreTypes),
        touched: { ...context.touched, type: true },
      })),
      // Update path field when name changes (using tokenReplacement for file blob stores)
      updatePathFromName: assign((context: any, event: any) => {
        const { blobStoreTypes, data, touched } = context;
        const type = data.type?.toLowerCase();

        // Only apply token replacement for file blob stores in create mode
        if (type !== BLOB_STORE_TYPE_IDS.FILE) {
          return {};
        }

        // Don't update path if it has been touched (user manually edited it)
        if (touched?.path) {
          return {};
        }

        const tokenReplacement = findPathTokenReplacement(blobStoreTypes);
        if (!tokenReplacement) {
          return {};
        }

        // Update path with new name
        const newPath = replaceTokens(tokenReplacement, { name: event.value });
        return {
          data: { ...data, path: newPath },
        };
      }),
    },
    // Guards for TYPE_CHANGE transitions between sub-states
    guards: {
      isTypeFile: isTypeGuard(BLOB_STORE_TYPE_IDS.FILE) as any,
      isTypeS3: isTypeGuard(BLOB_STORE_TYPE_IDS.S3) as any,
      isTypeAzure: isTypeGuard(BLOB_STORE_TYPE_IDS.AZURE) as any,
      isTypeGoogle: isTypeGuard(BLOB_STORE_TYPE_IDS.GOOGLE) as any,
      isTypeGroup: isTypeGuard(BLOB_STORE_TYPE_IDS.GROUP) as any,
    },
    services: {
      load: async () => {
        // Load reference data in parallel
        const [blobStoreTypes, quotaTypes, blobStore, usage] = await Promise.all([
          restClient
            .get(BLOB_STORES_TYPES_URL)
            .then((data: unknown) => data as BlobStoreTypeInfo[])
            .catch((err: unknown) => {
              console.error('Failed to load blob store types:', err);
              return [] as BlobStoreTypeInfo[];
            }),
          restClient
            .get(BLOB_STORES_QUOTA_TYPES_URL)
            .then((data: unknown) => data as QuotaType[])
            .catch((err: unknown) => {
              console.error('Failed to load quota types:', err);
              return [] as QuotaType[];
            }),
          preloadedBlobStore
            ? Promise.resolve(preloadedBlobStore)
            : blobStoreName && blobStoreType
            ? findBlobStore(blobStoreName, blobStoreType).catch((err: unknown) => {
                console.error('Failed to load blob store:', err);
                throw err;
              })
            : Promise.resolve(null),
          blobStoreName
            ? fetchBlobStoreUsage(blobStoreName)
            : Promise.resolve({ blobStoreUsage: 0, repositoryUsage: 0 } as BlobStoreUsage),
        ]);

        // Build initial form data
        const type = blobStore
          ? (blobStore.type?.toLowerCase() || blobStoreType?.toLowerCase() || initialType)
          : initialType;

        const initialData: BlobStoreFormData = blobStore
          ? transformRestToFormData(blobStore, type)
          : buildDefaultsForType(type, {}, Array.isArray(blobStoreTypes) ? blobStoreTypes : []);

        return {
          data: initialData,
          blobStore,
          blobStoreTypes: Array.isArray(blobStoreTypes) ? blobStoreTypes : [],
          quotaTypes: Array.isArray(quotaTypes) ? quotaTypes : [],
          usage,
        };
      },
      // save service is provided via useForm options
    },
    // Custom event for blob store type changes (transitions to correct sub-state)
    on: {
      TYPE_CHANGE: [
        { target: '.file', cond: 'isTypeFile', actions: ['changeType', 'validate', 'computePristine'] },
        { target: '.s3', cond: 'isTypeS3', actions: ['changeType', 'validate', 'computePristine'] },
        { target: '.azure', cond: 'isTypeAzure', actions: ['changeType', 'validate', 'computePristine'] },
        { target: '.google', cond: 'isTypeGoogle', actions: ['changeType', 'validate', 'computePristine'] },
        { target: '.group', cond: 'isTypeGroup', actions: ['changeType', 'validate', 'computePristine'] },
      ],
      // Handle name changes to update path field (file blob stores only)
      NAME_CHANGE: {
        actions: ['updatePathFromName'],
      },
      CONFIRM_DELETE: {
        actions: assign({ showDeleteModal: true } as any),
      },
      HIDE_DELETE_MODAL: {
        actions: assign({ showDeleteModal: false } as any),
      },
    },
    // Blob store type variant sub-states within the editing state.
    // Each sub-state declares metadata about which fields are visible and required
    // for that type variant. This enables:
    // 1. The component to read field config from machine state (no switch/case)
    // 2. Model-based testing that auto-generates paths through every variant
    // 3. Single source of truth for form structure
    editingConfig: {
      defaultState: BLOB_STORE_TYPE_IDS.FILE,
      typeField: 'type',
      states: {
        [BLOB_STORE_TYPE_IDS.FILE]: {
          meta: {
            typeLabel: 'File',
            fields: ['path'],
            requiredFields: ['path'],
            fieldConfig: {
              path: {
                label: 'Path',
                type: 'text',
                helpText: 'An absolute path or a path relative to <data-directory>/blobs',
              },
            },
          },
        },
        [BLOB_STORE_TYPE_IDS.S3]: {
          meta: {
            typeLabel: 'S3',
            fields: [
              'bucketConfiguration.bucket.region',
              'bucketConfiguration.bucket.name',
              'bucketConfiguration.bucket.prefix',
              'bucketConfiguration.bucketSecurity.accessKeyId',
              'bucketConfiguration.bucketSecurity.secretAccessKey',
              'bucketConfiguration.bucketSecurity.role',
              'bucketConfiguration.bucketSecurity.sessionToken',
              'bucketConfiguration.encryption.encryptionType',
              'bucketConfiguration.encryption.encryptionKey',
              'bucketConfiguration.advancedBucketConnection.endpoint',
              'bucketConfiguration.advancedBucketConnection.maxConnectionPoolSize',
              'bucketConfiguration.advancedBucketConnection.forcePathStyle',
              'bucketConfiguration.preSignedUrlEnabled',
            ],
            requiredFields: ['bucketConfiguration.bucket.name'],
            fieldConfig: {
              'bucketConfiguration.bucket.region': { label: 'Region', type: 'select' },
              'bucketConfiguration.bucket.name': { label: 'Bucket', type: 'text', helpText: 'S3 Bucket Name (must be between 3 and 63 characters)' },
              'bucketConfiguration.bucket.prefix': { label: 'Prefix', type: 'text', helpText: 'S3 Path prefix (optional)' },
              'bucketConfiguration.bucketSecurity.accessKeyId': { label: 'Access Key ID', type: 'text' },
              'bucketConfiguration.bucketSecurity.secretAccessKey': { label: 'Secret Access Key', type: 'password' },
              'bucketConfiguration.bucketSecurity.role': { label: 'Assume Role ARN', type: 'text' },
              'bucketConfiguration.bucketSecurity.sessionToken': { label: 'Session Token', type: 'password' },
              'bucketConfiguration.encryption.encryptionType': { label: 'Encryption Type', type: 'select' },
              'bucketConfiguration.encryption.encryptionKey': { label: 'KMS Key ID', type: 'text' },
              'bucketConfiguration.advancedBucketConnection.endpoint': { label: 'Endpoint URL', type: 'text' },
              'bucketConfiguration.advancedBucketConnection.maxConnectionPoolSize': { label: 'Max Connection Pool Size', type: 'number' },
              'bucketConfiguration.advancedBucketConnection.forcePathStyle': { label: 'Use path-style access', type: 'checkbox' },
              'bucketConfiguration.preSignedUrlEnabled': { label: 'Pre-Signed URL', type: 'checkbox' },
            },
          },
        },
        [BLOB_STORE_TYPE_IDS.AZURE]: {
          meta: {
            typeLabel: 'Azure',
            fields: [
              'bucketConfiguration.accountName',
              'bucketConfiguration.containerName',
              'bucketConfiguration.authentication.authenticationMethod',
              'bucketConfiguration.authentication.accountKey',
              'bucketConfiguration.preSignedUrlEnabled',
            ],
            requiredFields: [
              'bucketConfiguration.accountName',
              'bucketConfiguration.containerName',
            ],
            fieldConfig: {
              'bucketConfiguration.accountName': { label: 'Account Name', type: 'text', helpText: 'The name of the Azure storage account' },
              'bucketConfiguration.containerName': { label: 'Container Name', type: 'text', helpText: 'The name of the container for storage' },
              'bucketConfiguration.authentication.authenticationMethod': { label: 'Authentication Method', type: 'select' },
              'bucketConfiguration.authentication.accountKey': { label: 'Account Key', type: 'password' },
              'bucketConfiguration.preSignedUrlEnabled': { label: 'Direct Download (SAS URLs)', type: 'checkbox' },
            },
          },
        },
        [BLOB_STORE_TYPE_IDS.GOOGLE]: {
          meta: {
            typeLabel: 'Google Cloud',
            fields: [
              'bucketConfiguration.bucket.projectId',
              'bucketConfiguration.bucket.name',
              'bucketConfiguration.bucket.prefix',
              'bucketConfiguration.bucketSecurity.authenticationMethod',
              'bucketConfiguration.encryption.encryptionType',
              'bucketConfiguration.encryption.encryptionKey',
            ],
            requiredFields: ['bucketConfiguration.bucket.name'],
            fieldConfig: {
              'bucketConfiguration.bucket.projectId': { label: 'Project ID', type: 'text', helpText: 'Your GCP Project ID' },
              'bucketConfiguration.bucket.name': { label: 'Bucket', type: 'text', helpText: 'Google Cloud Platform bucket name (must be between 3 and 63 characters)' },
              'bucketConfiguration.bucket.prefix': { label: 'Prefix', type: 'text', helpText: 'Google Cloud Storage path prefix' },
              'bucketConfiguration.bucketSecurity.authenticationMethod': { label: 'Authentication Method', type: 'select' },
              'bucketConfiguration.encryption.encryptionType': { label: 'Encryption Type', type: 'select' },
              'bucketConfiguration.encryption.encryptionKey': { label: 'KMS Key ID', type: 'text' },
            },
          },
        },
        [BLOB_STORE_TYPE_IDS.GROUP]: {
          meta: {
            typeLabel: 'Group',
            fields: ['members', 'fillPolicy'],
            requiredFields: ['members', 'fillPolicy'],
            fieldConfig: {
              members: { label: 'Members', type: 'transferList', helpText: 'Select the blob stores to include in this group' },
              fillPolicy: { label: 'Fill Policy', type: 'select', helpText: 'Determines how blobs are distributed across group members' },
            },
          },
        },
      },
    },
  });
}
