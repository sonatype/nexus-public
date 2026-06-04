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
import { restClient, parseApiError, ENDPOINTS } from '../../../../../../interface/api';
import {
  Repository,
  RepositoryFormData,
  RepositoryReference,
  Recipe,
  BlobStore,
  RoutingRule,
  CleanupPolicy,
  HealthCheckStatus,
} from './types';

// =============================================================================
// API Endpoints - REST only, NO ExtDirect
// =============================================================================

const REPOSITORIES_REST_URL = '/service/rest/v1/repositories';
const REPOSITORIES_DETAILS_URL = '/service/rest/internal/ui/repositories/details/';
const REPOSITORIES_LIST_URL = '/service/rest/internal/ui/repositories';
const RECIPES_URL = '/service/rest/internal/ui/repositories/recipes';
const BLOB_STORES_URL = '/service/rest/v1/blobstores';
const ROUTING_RULES_URL = '/service/rest/v1/routing-rules';
const CLEANUP_POLICIES_URL = '/service/rest/v1/cleanup-policies';

/**
 * Maps format names to REST API endpoint paths.
 * Some formats have different names in the UI vs the REST API:
 * - maven2 -> maven (UI uses maven2, API uses maven)
 */
function getApiFormatPath(format: string): string {
  const formatMapping: Record<string, string> = {
    'maven2': 'maven',
  };
  return formatMapping[format] || format;
}

/**
 * Custom hook for Repository API operations
 * Uses REST APIs only - NO ExtDirect/ExtJS
 */
export function useRepositoriesApi() {
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

  // ==========================================================================
  // Repository Operations
  // ==========================================================================

  /**
   * Fetch all repositories
   * Uses REST API: GET /service/rest/internal/ui/repositories/details/
   */
  const fetchRepositories = useCallback(async (): Promise<Repository[]> => {
    try {
      const data = await restClient.get<Repository[]>(REPOSITORIES_DETAILS_URL);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      console.error('Failed to fetch repositories:', err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch a single repository by name
   * Uses REST API: GET /service/rest/v1/repositories/{name}
   */
  const fetchRepository = useCallback(async (name: string): Promise<Repository | null> => {
    try {
      const basic = await restClient.get<Repository>(`${REPOSITORIES_REST_URL}/${encodeURIComponent(name)}`);
      if (!basic) return null;

      const apiFormat = getApiFormatPath(basic.format);
      const detailUrl = `${REPOSITORIES_REST_URL}/${encodeURIComponent(apiFormat)}/${encodeURIComponent(basic.type)}/${encodeURIComponent(name)}`;
      try {
        const detail = await restClient.get<Repository>(detailUrl);
        const repo = { ...basic, ...detail } || basic;

        // Fetch signing passphrase from internal endpoint for apt/yum repos
        if (repo.format === 'apt' || repo.format === 'yum') {
          try {
            const signing = await restClient.get<{ passphrase: string | null }>(
              `${REPOSITORIES_LIST_URL}/repository/${encodeURIComponent(name)}/signing-passphrase`
            );
            if (signing?.passphrase != null) {
              if (repo.format === 'apt') {
                repo.aptSigning = { ...repo.aptSigning, passphrase: signing.passphrase };
              } else {
                repo.yumSigning = { ...repo.yumSigning, passphrase: signing.passphrase };
              }
            }
          } catch {
            // Non-critical — passphrase field will just be empty
          }
        }

        return repo;
      } catch {
        return basic;
      }
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      if (apiError.status === 404) {
        return null;
      }
      console.error('Failed to fetch repository:', err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Create a new repository
   * Uses REST API: POST /service/rest/v1/repositories/{format}/{type}
   */
  const createRepository = useCallback(async (data: RepositoryFormData): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const repoConfig = buildRepositoryConfig(data);
      const apiFormat = getApiFormatPath(data.format);
      const url = `${REPOSITORIES_REST_URL}/${encodeURIComponent(apiFormat)}/${encodeURIComponent(data.type)}`;
      await restClient.post(url, repoConfig);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update an existing repository
   * Uses REST API: PUT /service/rest/v1/repositories/{format}/{type}/{name}
   */
  const updateRepository = useCallback(async (name: string, data: RepositoryFormData): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const repoConfig = buildRepositoryConfig(data);
      const apiFormat = getApiFormatPath(data.format);
      const url = `${REPOSITORIES_REST_URL}/${encodeURIComponent(apiFormat)}/${encodeURIComponent(data.type)}/${encodeURIComponent(name)}`;
      await restClient.put(url, repoConfig);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete a repository
   * Uses REST API: DELETE /service/rest/v1/repositories/{name}
   */
  const deleteRepository = useCallback(async (name: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.delete(`${REPOSITORIES_REST_URL}/${encodeURIComponent(name)}`);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Invalidate cache for a proxy repository
   * Uses REST API: POST /service/rest/v1/repositories/{name}/invalidate-cache
   */
  const invalidateCache = useCallback(async (name: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.post(`${REPOSITORIES_REST_URL}/${encodeURIComponent(name)}/invalidate-cache`);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Rebuild index for a repository
   * Uses REST API: POST /service/rest/v1/repositories/{name}/rebuild-index
   */
  const rebuildIndex = useCallback(async (name: string): Promise<string> => {
    setLoading(true);
    setError(null);
    try {
      await restClient.post(`${REPOSITORIES_REST_URL}/${encodeURIComponent(name)}/rebuild-index`);
      return 'Index rebuild started';
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // ==========================================================================
  // Reference Data
  // ==========================================================================

  /**
   * Fetch available recipes
   * Uses REST API: GET /service/rest/internal/ui/repositories/recipes
   */
  const fetchRecipes = useCallback(async (): Promise<Recipe[]> => {
    try {
      const data = await restClient.get<Array<{ format: string; type: string }>>(RECIPES_URL);
      const rawRecipes = Array.isArray(data) ? data : [];
      if (rawRecipes.length === 0) {
        throw new Error('No repository recipes available');
      }
      // API returns {format, type} - add name field for compatibility
      return rawRecipes.map((r) => ({
        ...r,
        name: `${r.format}-${r.type}`,
      }));
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      console.error('Failed to fetch recipes:', err);
      throw new Error(apiError.message);
    }
  }, []);

  /**
   * Fetch available blob stores
   * Uses REST API: GET /service/rest/v1/blobstores
   */
  const fetchBlobStores = useCallback(async (): Promise<BlobStore[]> => {
    try {
      const data = await restClient.get<Array<{ name: string; type?: string }>>(BLOB_STORES_URL);
      const stores = Array.isArray(data) ? data : [];
      return stores.map((s) => ({
        name: s.name,
        type: s.type,
      }));
    } catch (err: unknown) {
      console.error('Failed to fetch blob stores:', err);
      return [];
    }
  }, []);

  /**
   * Fetch repository references (for group members)
   * Uses REST API: GET /service/rest/internal/ui/repositories
   */
  const fetchRepositoryReferences = useCallback(async (format?: string): Promise<RepositoryReference[]> => {
    try {
      const url = format 
        ? `${REPOSITORIES_LIST_URL}?format=${encodeURIComponent(format)}`
        : REPOSITORIES_LIST_URL;
      const data = await restClient.get<Array<{ name: string; format: string; type: string }>>(url);
      const repos = Array.isArray(data) ? data : [];
      // Map to RepositoryReference format
      return repos.map((r) => ({
        id: r.name,
        name: r.name,
        format: r.format,
        type: r.type,
      }));
    } catch (err: unknown) {
      console.error('Failed to fetch repository references:', err);
      return [];
    }
  }, []);

  /**
   * Fetch routing rules
   * Uses REST API: GET /service/rest/v1/routing-rules
   */
  const fetchRoutingRules = useCallback(async (): Promise<RoutingRule[]> => {
    try {
      const data = await restClient.get<RoutingRule[]>(ROUTING_RULES_URL);
      return Array.isArray(data) ? data : [];
    } catch (err: unknown) {
      console.error('Failed to fetch routing rules:', err);
      return [];
    }
  }, []);

  /**
   * Fetch cleanup policies for a format
   * Uses REST API: GET /service/rest/v1/cleanup-policies
   */
  const fetchCleanupPolicies = useCallback(async (format?: string): Promise<CleanupPolicy[]> => {
    try {
      const data = await restClient.get<CleanupPolicy[]>(CLEANUP_POLICIES_URL);
      const policies = Array.isArray(data) ? data : [];
      
      // Filter by format client-side if format is specified
      if (format) {
        return policies.filter((p) => p.format === format);
      }
      return policies;
    } catch (err: unknown) {
      console.error('Failed to fetch cleanup policies:', err);
      return [];
    }
  }, []);

  // ==========================================================================
  // Health Check Operations
  // Note: Health check REST APIs may not be available in all versions
  // ==========================================================================

  /**
   * Fetch health check status
   * Uses REST API: GET /service/rest/internal/ui/healthcheck
   */
  const fetchHealthCheckStatus = useCallback(async (): Promise<Record<string, HealthCheckStatus>> => {
    try {
      const data = await restClient.get<Array<HealthCheckStatus & { repositoryName: string }>>(ENDPOINTS.HEALTH_CHECK);
      return (data || []).reduce<Record<string, HealthCheckStatus>>((acc, item) => {
        acc[item.repositoryName] = item;
        return acc;
      }, {});
    } catch (err) {
      console.error('Failed to fetch health check status:', err);
      return {};
    }
  }, []);

  /**
   * Enable health check for a repository
   * Uses REST API: POST /service/rest/v1/repositories/{name}/health-check
   */
  const enableHealthCheck = useCallback(async (repositoryName: string): Promise<void> => {
    try {
      await restClient.post(ENDPOINTS.HEALTH_CHECK_ANALYZE(repositoryName));
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      throw new Error(apiError.message);
    }
  }, []);

  return {
    loading,
    error,
    setError,
    // Repository operations
    fetchRepositories,
    fetchRepository,
    createRepository,
    updateRepository,
    deleteRepository,
    invalidateCache,
    rebuildIndex,
    // Reference data
    fetchRecipes,
    fetchBlobStores,
    fetchRepositoryReferences,
    fetchRoutingRules,
    fetchCleanupPolicies,
    // Health check
    fetchHealthCheckStatus,
    enableHealthCheck,
  };
}

// =============================================================================
// Helper Functions
// =============================================================================

/**
 * Build repository configuration object for REST API calls
 * REST API expects a different format than ExtDirect
 */
function buildRepositoryConfig(data: RepositoryFormData): Record<string, unknown> {
  const config: Record<string, unknown> = {
    name: data.name,
    online: data.online ?? true,
  };

  // Add routing rule if present
  if (data.routingRuleId) {
    config.routingRule = data.routingRuleId;
  }

  // Storage configuration
  if (data.storage) {
    config.storage = {
      blobStoreName: data.storage.blobStoreName || 'default',
      strictContentTypeValidation: data.storage.strictContentTypeValidation ?? true,
      writePolicy: data.storage.writePolicy,
    };
  }

  // Cleanup configuration
  if (data.cleanup?.policyNames && data.cleanup.policyNames.length > 0) {
    config.cleanup = {
      policyNames: data.cleanup.policyNames,
    };
  }

  // Proxy-specific configuration
  if (data.type === 'proxy') {
    if (data.proxy) {
      config.proxy = {
        remoteUrl: data.proxy.remoteUrl,
        contentMaxAge: data.proxy.contentMaxAge ?? 1440,
        metadataMaxAge: data.proxy.metadataMaxAge ?? 1440,
      };
    }
    if (data.negativeCache) {
      config.negativeCache = {
        enabled: data.negativeCache.enabled ?? true,
        timeToLive: data.negativeCache.timeToLive ?? 1440,
      };
    }
    if (data.httpClient) {
      config.httpClient = {
        blocked: data.httpClient.blocked ?? false,
        autoBlock: data.httpClient.autoBlock ?? true,
        connection: data.httpClient.connection,
        authentication: data.httpClient.authentication,
      };
    }
    if (data.replication) {
      config.replication = {
        preemptivePullEnabled: data.replication.preemptivePullEnabled ?? false,
        assetPathRegex: data.replication.assetPathRegex || null,
      };
    }
  }

  // Hosted-specific configuration
  if (data.type === 'hosted') {
    if (data.component) {
      config.component = {
        proprietaryComponents: data.component.proprietaryComponents ?? false,
      };
    }
  }

  // Group-specific configuration
  if (data.type === 'group') {
    if (data.group) {
      config.group = {
        memberNames: data.group.memberNames || [],
        ...(data.group.writableMember ? { writableMember: data.group.writableMember } : {}),
      };
    }
  }

  // Format-specific configurations - always include for matching formats
  // even if user didn't change defaults (API requires them)
  if (data.format === 'maven2') {
    config.maven = {
      versionPolicy: data.maven?.versionPolicy ?? 'RELEASE',
      layoutPolicy: data.maven?.layoutPolicy ?? 'STRICT',
      contentDisposition: data.maven?.contentDisposition ?? 'ATTACHMENT',
    };
  }

  if (data.format === 'docker' || data.docker) {
    config.docker = {
      v1Enabled: data.docker?.v1Enabled ?? false,
      forceBasicAuth: data.docker?.forceBasicAuth ?? false,
      httpPort: data.docker?.httpPort || null,
      httpsPort: data.docker?.httpsPort || null,
      subdomain: data.docker?.subdomain || null,
      pathEnabled: data.docker?.pathEnabled ?? false,
    };
  }

  if ((data.format === 'docker' && data.type === 'proxy') || data.dockerProxy) {
    config.dockerProxy = {
      indexType: data.dockerProxy?.indexType ?? 'HUB',
      indexUrl: data.dockerProxy?.indexUrl || null,
      cacheForeignLayers: data.dockerProxy?.cacheForeignLayers ?? false,
      foreignLayerUrlWhitelist: data.dockerProxy?.foreignLayerUrlWhitelist || [],
    };
  }

  if (data.format === 'apt' || data.apt) {
    config.apt = {
      distribution: data.apt?.distribution || '',
      flat: data.apt?.flat ?? false,
      enforceDistribution: data.apt?.enforceDistribution ?? false,
    };
  }

  if (data.format === 'apt' && (data.type === 'hosted' || data.type === 'proxy')) {
    const aptSigningConfig: Record<string, string | null> = {};
    if (data.aptSigning?.keypair?.trim()) {
      aptSigningConfig.keypair = data.aptSigning.keypair.trim();
    }
    aptSigningConfig.passphrase = data.aptSigning?.passphrase?.trim() || null;
    if (aptSigningConfig.keypair) {
      config.aptSigning = aptSigningConfig;
    }
  }

  // Always include alpineSigning for alpine group repositories (signing is mandatory).
  // For other alpine types, include only when a keypair is provided.
  if (data.format === 'alpine' && data.type === 'group') {
    config.alpineSigning = {
      keypair: data.alpineSigning?.keypair || '',
      passphrase: data.alpineSigning?.passphrase || '',
    };
  } else {
    const alpineKeypair = data.alpineSigning?.keypair?.trim();
    if (alpineKeypair) {
      config.alpineSigning = {
        keypair: alpineKeypair,
        passphrase: data.alpineSigning?.passphrase || '',
      };
    }
  }

  if (data.format === 'yum' || data.yum) {
    config.yum = {
      repodataDepth: data.yum?.repodataDepth ?? 0,
      deployPolicy: data.yum?.deployPolicy ?? 'STRICT',
    };
  }

  if (data.format === 'yum' && (data.type === 'proxy' || data.type === 'group')) {
    const yumSigningConfig: Record<string, string | null> = {};
    if (data.yumSigning?.keypair?.trim()) {
      yumSigningConfig.keypair = data.yumSigning.keypair.trim();
    }
    yumSigningConfig.passphrase = data.yumSigning?.passphrase?.trim() || null;
    if (yumSigningConfig.keypair) {
      config.yumSigning = yumSigningConfig;
    }
  }

  if (data.raw) {
    config.raw = {
      contentDisposition: data.raw.contentDisposition ?? 'ATTACHMENT',
    };
  }

  if (data.format === 'npm' && data.type === 'proxy' && data.npm) {
    config.npm = {
      removeQuarantinedVersions: data.npm.removeQuarantinedVersions ?? false,
    };
  }

  if (data.format === 'nuget' && data.type === 'proxy') {
    config.nugetProxy = {
      queryCacheItemMaxAge: data.nugetProxy?.queryCacheItemMaxAge ?? 3600,
      nugetVersion: data.nugetProxy?.nugetVersion ?? 'V3',
    };
  }

  if (data.format === 'pypi' && data.type === 'proxy' && data.pypi) {
    config.pypi = {
      indexPath: data.pypi.indexPath,
      removeQuarantinedVersions: data.pypi.removeQuarantinedVersions ?? false,
    };
  }

  return config;
}

export default useRepositoriesApi;
