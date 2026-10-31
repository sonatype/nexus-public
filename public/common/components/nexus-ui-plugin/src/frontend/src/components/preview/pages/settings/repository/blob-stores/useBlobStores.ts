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

import { useState, useEffect, useCallback } from 'react';
import { restClient, parseApiError } from '../../../../../../interface/api';
import type {
  BlobStore,
  BlobStoreTypeDescriptor,
  QuotaType,
  BlobStoreFormData,
  S3DropdownValues
} from './types';

// API URLs - matching Default UI endpoints (all with leading slash for restClient)
const URLS = {
  BLOB_STORES_LIST: '/service/rest/internal/ui/blobstores',
  BLOB_STORES_TYPES: '/service/rest/internal/ui/blobstores/types',
  BLOB_STORES_QUOTA_TYPES: '/service/rest/internal/ui/blobstores/quotaTypes',
  BLOB_STORES_USAGE: (name: string) => `/service/rest/internal/ui/blobstores/usage/${encodeURIComponent(name)}`,
  BLOB_STORE_SINGLE: (type: string, name: string) => `/service/rest/v1/blobstores/${encodeURIComponent(type)}/${encodeURIComponent(name)}`,
  BLOB_STORE_CREATE: (type: string) => `/service/rest/v1/blobstores/${encodeURIComponent(type)}`,
  BLOB_STORE_DELETE: (name: string) => `/service/rest/v1/blobstores/${encodeURIComponent(name)}`,
  BLOB_STORE_CONVERT_TO_GROUP: (name: string, newName: string) => `/service/rest/v1/blobstores/group/convert/${encodeURIComponent(name)}/${encodeURIComponent(newName)}`,
  AZURE_TEST_CONNECTION: '/service/rest/internal/ui/azureblobstore/test-connection',
};

// Convert MB to bytes (matching UnitUtil.megaBytesToBytes from Default UI)
const megaBytesToBytes = (mb: number): number => mb * 1024 * 1024;

// Convert bytes to MB — inverse of megaBytesToBytes, used when loading API responses
const bytesToMegaBytes = (bytes: number): number => bytes / (1024 * 1024);

// Deep trim all string values in an object
const _trimStrings = (obj: Record<string, unknown>): Record<string, unknown> => {
  const result: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(obj)) {
    if (typeof value === 'string') {
      result[key] = value.trim();
    } else if (value && typeof value === 'object' && !Array.isArray(value)) {
      result[key] = _trimStrings(value as Record<string, unknown>);
    } else if (Array.isArray(value)) {
      result[key] = value.map(item => 
        typeof item === 'object' && item !== null 
          ? _trimStrings(item as Record<string, unknown>) 
          : item
      );
    } else {
      result[key] = value;
    }
  }
  return result;
};

/**
 * Format blob store data for API submission
 * Matches Default UI behavior from BlobStoresFormMachine.js:
 * - Removes softQuota if not enabled
 * - Trims string values
 * - Converts softQuota.limit from MB to bytes
 */
const formatBlobStoreData = (data: BlobStoreFormData): Record<string, unknown> => {
  const result: Record<string, unknown> = {};

  result.name = (data.name || '').trim();

  // Type-specific fields only
  const blobType = data.type?.toLowerCase();
  if (blobType === 'file') {
    if (data.path) result.path = data.path.trim();
  } else if (blobType === 's3' || blobType === 'azure' || blobType === 'google') {
    if (data.bucketConfiguration) result.bucketConfiguration = data.bucketConfiguration;
  } else if (blobType === 'group') {
    if (data.members) result.members = data.members;
    if (data.fillPolicy) result.fillPolicy = data.fillPolicy;
  }

  // Soft quota: only include when enabled, strip UI-only 'enabled' field
  if (data.softQuota?.enabled && data.softQuota.type) {
    let limit = data.softQuota.limit;
    if (typeof limit === 'string') limit = parseFloat(limit);
    if (typeof limit === 'number' && limit > 0) {
      limit = megaBytesToBytes(limit);
    }
    result.softQuota = {
      type: data.softQuota.type,
      limit,
    };
  }

  return result;
};

interface UseBlobStoresListResult {
  blobStores: BlobStore[];
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

interface UseBlobStoreTypesResult {
  types: BlobStoreTypeDescriptor[];
  quotaTypes: QuotaType[];
  loading: boolean;
  error: string | null;
}

interface UseBlobStoreResult {
  blobStore: BlobStoreFormData | null;
  blobStoreUsage: number;
  repositoryUsage: number;
  loading: boolean;
  error: string | null;
  save: (data: BlobStoreFormData) => Promise<void>;
  remove: () => Promise<void>;
}

/**
 * Hook for fetching blob stores list
 */
export function useBlobStoresList(): UseBlobStoresListResult {
  const [blobStores, setBlobStores] = useState<BlobStore[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchBlobStores = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // restClient returns data directly, not response.data
      const responseData = await restClient.get<{ data?: BlobStore[] } | BlobStore[]>(URLS.BLOB_STORES_LIST);
      const data = (responseData as { data?: BlobStore[] })?.data || responseData || [];
      // Transform data to match expected format
      // API returns 'typeName' but UI expects 'type' for display
      const transformed = (Array.isArray(data) ? data : []).map((blobStore: Record<string, unknown>) => ({
        name: blobStore.name,
        type: blobStore.typeName as string,
        typeId: blobStore.typeId as string,
        path: blobStore.path as string | undefined,
        available: !blobStore.unavailable,
        unavailable: blobStore.unavailable as boolean | undefined,
        blobCount: blobStore.unavailable ? -1 : (blobStore.blobCount as number),
        totalSizeInBytes: blobStore.unavailable ? -1 : (blobStore.totalSizeInBytes as number),
        availableSpaceInBytes: (blobStore.unlimited as boolean) ? Infinity : (blobStore.availableSpaceInBytes as number),
        unlimited: blobStore.unlimited as boolean | undefined
      }));
      setBlobStores(transformed);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load blob stores');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBlobStores();
  }, [fetchBlobStores]);

  return { blobStores, loading, error, refetch: fetchBlobStores };
}

/**
 * Hook for fetching blob store types and quota types
 * Uses REST endpoints matching Default UI: /service/rest/internal/ui/blobstores/types
 */
export function useBlobStoreTypes(): UseBlobStoreTypesResult {
  const [types, setTypes] = useState<BlobStoreTypeDescriptor[]>([]);
  const [quotaTypes, setQuotaTypes] = useState<QuotaType[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchTypes = async () => {
      setLoading(true);
      setError(null);
      try {
        // restClient returns data directly
        const [typesData, quotaData] = await Promise.all([
          restClient.get<BlobStoreTypeDescriptor[]>(URLS.BLOB_STORES_TYPES),
          restClient.get<QuotaType[]>(URLS.BLOB_STORES_QUOTA_TYPES)
        ]);

        // Ensure we have arrays
        if (Array.isArray(typesData)) {
          setTypes(typesData);
        } else {
          console.warn('Blob store types response is not an array:', typesData);
          setTypes([]);
        }

        if (Array.isArray(quotaData)) {
          setQuotaTypes(quotaData);
        } else {
          console.warn('Quota types response is not an array:', quotaData);
          setQuotaTypes([]);
        }
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to load blob store types';
        setError(errorMessage);
        console.error('Failed to load blob store types:', err);
        // Set empty arrays on error so UI can still render
        setTypes([]);
        setQuotaTypes([]);
      } finally {
        setLoading(false);
      }
    };

    fetchTypes();
  }, []);

  return { types, quotaTypes, loading, error };
}

/**
 * Hook for fetching a single blob store by name and type
 * Uses REST endpoints matching Default UI:
 * - GET service/rest/v1/blobstores/{type}/{name}
 * - GET /service/rest/internal/ui/blobstores/usage/{name}
 */
export function useBlobStore(name?: string, type?: string): UseBlobStoreResult {
  const [blobStore, setBlobStore] = useState<BlobStoreFormData | null>(null);
  const [blobStoreUsage, setBlobStoreUsage] = useState(0);
  const [repositoryUsage, setRepositoryUsage] = useState(0);
  const [loading, setLoading] = useState(!!name);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!name || !type) {
      setBlobStore(null);
      setLoading(false);
      return;
    }

    const fetchBlobStore = async () => {
      setLoading(true);
      setError(null);
      try {
        // The REST API URL requires a lowercase type segment (e.g. "file", not "File")
        const rawData = await restClient.get<BlobStoreFormData>(URLS.BLOB_STORE_SINGLE(type.toLowerCase(), name));
        if (rawData) {
          // Transform the raw API response into the form data shape:
          //  - softQuota.enabled is not returned by the API; derive it from presence of softQuota
          //  - softQuota.limit is in bytes from the API; the form field works in MB
          const transformed: BlobStoreFormData = {
            ...rawData,
            softQuota: rawData.softQuota
              ? {
                  ...rawData.softQuota,
                  enabled: true,
                  limit: rawData.softQuota.limit != null
                    ? bytesToMegaBytes(rawData.softQuota.limit as unknown as number)
                    : undefined,
                }
              : undefined,
          };
          setBlobStore(transformed);
        } else {
          setError('Failed to load blob store');
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load blob store');
      } finally {
        setLoading(false);
      }

      // Fetch usage separately - 404 is expected if endpoint not deployed
      try {
        const usageData = await restClient.get<{ blobStoreUsage?: number; repositoryUsage?: number }>(URLS.BLOB_STORES_USAGE(name));
        if (usageData) {
          setBlobStoreUsage(usageData.blobStoreUsage || 0);
          setRepositoryUsage(usageData.repositoryUsage || 0);
        }
      } catch {
        // Usage endpoint may not exist - default to 0 (deletable)
        setBlobStoreUsage(0);
        setRepositoryUsage(0);
      }
    };

    fetchBlobStore();
  }, [name, type]);

  const save = useCallback(async (data: BlobStoreFormData) => {
    if (!data.type) {
      throw new Error('Blob store type is required');
    }
    
    // Format data to match backend expectations (matching Default UI behavior)
    const saveData = formatBlobStoreData(data);
    
    try {
      // REST API URL segments require lowercase type (e.g. "file", not "File")
      const typeSegment = data.type.toLowerCase();
      if (name) {
        // Update existing blob store
        await restClient.put(URLS.BLOB_STORE_SINGLE(typeSegment, name), saveData);
      } else {
        // Create new blob store
        await restClient.post(URLS.BLOB_STORE_CREATE(typeSegment), saveData);
      }
    } catch (err: unknown) {
      // Parse the API error to extract meaningful message
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, [name]);

  const remove = useCallback(async () => {
    if (!name) return;
    await restClient.delete(URLS.BLOB_STORE_DELETE(name));
  }, [name]);

  return { blobStore, blobStoreUsage, repositoryUsage, loading, error, save, remove };
}

/**
 * Hook for testing Azure blob store connection
 * Uses REST endpoint matching Default UI: service/rest/internal/ui/azureblobstore/test-connection
 */
export function useAzureConnectionTest() {
  const [testing, setTesting] = useState(false);
  const [result, setResult] = useState<'success' | 'error' | null>(null);

  const testConnection = useCallback(async (config: {
    blobStoreName?: string;
    accountName: string;
    containerName: string;
    authenticationMethod: string;
    accountKey?: string;
  }) => {
    setTesting(true);
    setResult(null);
    try {
      const url = config.blobStoreName 
        ? `${URLS.AZURE_TEST_CONNECTION}/${config.blobStoreName}`
        : URLS.AZURE_TEST_CONNECTION;
      await restClient.post(url, config);
      setResult('success');
    } catch {
      setResult('error');
    } finally {
      setTesting(false);
    }
  }, []);

  const reset = useCallback(() => {
    setResult(null);
  }, []);

  return { testing, result, testConnection, reset };
}

/**
 * Hook for S3 dropdown values
 * Extracts dropDownValues from the S3 blob store type (returned by types endpoint)
 */
export function useS3DropdownValues(): { values: S3DropdownValues | null; loading: boolean } {
  const [values, setValues] = useState<S3DropdownValues | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchValues = async () => {
      try {
        // restClient returns data directly
        const types = await restClient.get<BlobStoreTypeDescriptor[]>(URLS.BLOB_STORES_TYPES);
        if (Array.isArray(types)) {
          // Find the S3 type and extract its dropDownValues
          const s3Type = types.find((t: BlobStoreTypeDescriptor) => t.id === 's3' || t.id === 'S3');
          if (s3Type?.dropDownValues) {
            setValues(s3Type.dropDownValues);
          }
        }
      } catch {
        // Silently fail, will use defaults
      } finally {
        setLoading(false);
      }
    };

    fetchValues();
  }, []);

  return { values, loading };
}

/**
 * Hook for promoting blob store to group
 * Uses REST endpoint matching Default UI: service/rest/v1/blobstores/group/convert/{name}/{newName}
 */
export function useBlobStorePromote() {
  const [promoting, setPromoting] = useState(false);

  const promote = useCallback(async (originalName: string, newGroupName: string) => {
    setPromoting(true);
    try {
      await restClient.post(URLS.BLOB_STORE_CONVERT_TO_GROUP(originalName, newGroupName));
    } finally {
      setPromoting(false);
    }
  }, []);

  return { promoting, promote };
}

/**
 * Hook for fetching groupable blob stores (for Group blob store members)
 * Uses REST API for getting groupable blob store names
 */
export function useGroupableBlobStores() {
  const [blobStores, setBlobStores] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchGroupable = async () => {
      setLoading(true);
      try {
        // restClient returns data directly
        const responseData = await restClient.get<{ data?: BlobStore[] } | BlobStore[]>(URLS.BLOB_STORES_LIST);
        const data = (responseData as { data?: BlobStore[] })?.data || responseData || [];
        // Filter to only non-group blob stores and extract names
        const groupable = (Array.isArray(data) ? data : [])
          .filter((store: BlobStore) => store.typeId?.toLowerCase() !== 'group' && !store.unavailable)
          .map((store: BlobStore) => store.name);
        setBlobStores(groupable);
      } catch {
        setBlobStores([]);
      } finally {
        setLoading(false);
      }
    };

    fetchGroupable();
  }, []);

  return { blobStores, loading };
}

