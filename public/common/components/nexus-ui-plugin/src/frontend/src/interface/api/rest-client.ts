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

/**
 * REST API Client
 *
 * Typed wrapper around Axios for REST API calls.
 * Provides consistent error handling and request/response transformation.
 *
 * Usage:
 *   import { restClient } from '@/utils/api';
 *
 *   // GET request
 *   const privileges = await restClient.get<Privilege[]>('/v1/security/privileges');
 *
 *   // POST request
 *   await restClient.post('/v1/security/privileges/application', newPrivilege);
 *
 *   // With error handling
 *   try {
 *     await restClient.post('/v1/security/privileges/application', data);
 *   } catch (err) {
 *     const apiError = parseApiError(err);
 *     // Handle structured error
 *   }
 */

import Axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
import { notifySessionExpiredFromRest } from './sessionExpiryBroadcast';
import { RequestConfig } from './types';

// API base paths
export const API_BASE = '/service/rest';
export const API_V1 = `${API_BASE}/v1`;
export const API_INTERNAL = `${API_BASE}/internal`;
export const API_INTERNAL_UI = `${API_INTERNAL}/ui`;

// Common endpoint paths
export const ENDPOINTS = {
  // Security
  PRIVILEGES: `${API_V1}/security/privileges`,
  ROLES: `${API_V1}/security/roles`,
  USERS: `${API_V1}/security/users`,
  REALMS: `${API_V1}/security/realms`,
  ANONYMOUS: `${API_V1}/security/anonymous`,
  LDAP: `${API_V1}/security/ldap`,
  CONTENT_SELECTORS: `${API_V1}/security/content-selectors`,
  USER_TOKENS: `${API_V1}/security/user-tokens`,

  // Scripts
  SCRIPTS: `${API_V1}/script`,

  // Repositories
  REPOSITORIES: `${API_V1}/repositories`,
  COMPONENTS: `${API_V1}/components`,
  ASSETS: `${API_V1}/assets`,
  ROUTING_RULES: `${API_V1}/routing-rules`,
  PROPRIETARY_CONTENT: `${API_INTERNAL}/proprietary-content`,

  // IQ Server (Firewall audit/quarantine)
  IQ_AUDIT: `${API_V1}/iq/audit`,
  IQ_AUDIT_REPO: (repositoryName: string) =>
    `${API_V1}/iq/audit/${encodeURIComponent(repositoryName)}`,
  /**
   * IQ Server connectivity and entitlements. Returns
   * `{hasFirewall, hasLifecycle, connected, url, deploymentId}` with the same shape on
   * self-hosted (pro/community) and cloud, unlike `GET /v1/iq` whose response type differs
   * per edition. Prefer this as the cross-edition source of truth for "is IQ usable here".
   */
  IQ_CAPABILITIES: `${API_V1}/iq/capabilities`,

  // Browse (REST API for tree browsing)
  REPOSITORY_BROWSE: (repositoryName: string) => `${API_V1}/repositories/${encodeURIComponent(repositoryName)}/browse`,

  // Search
  SEARCH: `${API_V1}/search`,
  SEARCH_ASSETS: `${API_V1}/search/assets`,
  SEARCH_REPOSITORIES: `${API_V1}/search/repositories`,

  // System Settings
  BLOBSTORES: `${API_V1}/blobstores`,
  CAPABILITIES: `${API_V1}/capabilities`,
  CAPABILITIES_TYPES: `${API_V1}/capabilities/types`,
  TASKS: `${API_V1}/tasks`,
  EMAIL: `${API_V1}/email`,
  EMAIL_VERIFY: `${API_V1}/email/verify`,
  HTTP: `${API_V1}/http`,

  // Status
  STATUS: `${API_V1}/status`,

  // Tags
  TAGS: `${API_V1}/tags`,
  TAGS_FILTERED: `${API_INTERNAL_UI}/tags/filtered`,

  // Upload
  UPLOAD_DEFINITIONS: `${API_INTERNAL_UI}/upload/definitions`,

  // User Account
  USER_ACCOUNT: `${API_INTERNAL_UI}/user`,

  // Docker
  DOCKER_SUGGEST_PORT: `${API_INTERNAL_UI}/docker/suggest-port`,

  // Internal UI endpoints
  PRIVILEGE_TYPES: `${API_INTERNAL_UI}/privileges/types`,
  ROLE_SOURCES: `${API_INTERNAL_UI}/roles/sources`,
  USER_SOURCES: `${API_V1}/security/user-sources`,
  REPOSITORIES_DETAILS: `${API_INTERNAL}/ui/repositories/details`,
  BROWSE: `${API_INTERNAL_UI}/browse`,
  NODES: `${API_INTERNAL_UI}/nodes`,
  HEALTH_CHECK: `${API_INTERNAL_UI}/healthcheck`,
  HEALTH_CHECK_ANALYZE: (repoName: string) => `${API_V1}/repositories/${encodeURIComponent(repoName)}/health-check`,
  /** Enable/disable Repository Health Check (POST enable / DELETE disable) */
  REPOSITORY_HEALTH_CHECK: (repoName: string) =>
    `${API_V1}/repositories/${encodeURIComponent(repoName)}/health-check`,
  HEALTH_CHECK_SUMMARY: `${API_INTERNAL_UI}/healthcheck/summary`,
  FIREWALL_STATUS: `${API_INTERNAL_UI}/firewall/status`,
  FIREWALL_STATUS_SUMMARY: `${API_INTERNAL_UI}/firewall/status/summary`,
  FIREWALL_STATUS_REPO: (repoName: string) =>
    `${API_INTERNAL_UI}/firewall/status/repo/${encodeURIComponent(repoName)}`,
  BLOBSTORES_TYPES: `${API_INTERNAL_UI}/blobstores/types`,
  BLOBSTORES_QUOTA_TYPES: `${API_INTERNAL_UI}/blobstores/quotaTypes`,
  DATASTORE: `${API_INTERNAL_UI}/datastore`,

  // Malicious risk (mirrors nexus-coreui-plugin's utils/api/rest-client.ts)
  MALICIOUS_RISK_ACTIVE_FINDINGS: `${API_V1}/malicious-risk/active-findings`,
  MALICIOUS_RISK_HISTORY: `${API_V1}/malicious-risk/history`,
  MALICIOUS_RISK_ACKNOWLEDGE: `${API_V1}/malicious-risk/acknowledge`,
  MALICIOUS_RISK_DELETE_FINDING: `${API_V1}/malicious-risk/delete-finding`,
  MALICIOUS_RISK_REMEDIATE: `${API_V1}/malicious-risk/remediate`,
} as const;

/**
 * Create a configured Axios instance
 */
function createClient(): AxiosInstance {
  const client = Axios.create({
    timeout: 30000, // 30 second default timeout
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
  });

  if (!client?.interceptors) {
    return client as AxiosInstance;
  }

  // Request interceptor - add CSRF token, cache-busting
  client.interceptors.request.use(
    (config) => {
      // Add CSRF token if available (for non-GET requests)
      if (config.method !== 'get') {
        const csrfToken = getCsrfToken();
        if (csrfToken) {
          config.headers['NX-ANTI-CSRF-TOKEN'] = csrfToken;
        }
      }

      // Prevent browser caching of GET requests
      if (config.method === 'get') {
        config.headers['Cache-Control'] = 'no-cache';
        config.headers.Pragma = 'no-cache';
      }

      // Let Axios set Content-Type automatically for FormData (multipart/form-data with boundary)
      if (config.data instanceof FormData) {
        delete config.headers['Content-Type'];
      }

      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor - handle common errors
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      // Handle 401 - redirect to login
      if (error.response?.status === 401) {
        console.warn('[REST] Authentication required');
        notifySessionExpiredFromRest();
      }
      return Promise.reject(error);
    }
  );

  return client;
}

/**
 * Get CSRF token from cookie or meta tag
 */
function getCsrfToken(): string | null {
  // Try meta tag first
  const metaTag = document.querySelector('meta[name="NX-ANTI-CSRF-TOKEN"]');
  if (metaTag) {
    return metaTag.getAttribute('content');
  }

  // Try cookie
  const cookies = document.cookie.split(';');
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split('=');
    if (name === 'NX-ANTI-CSRF-TOKEN') {
      return decodeURIComponent(value);
    }
  }

  return null;
}

// Lazy singleton client instance - created on first use to avoid
// crashing module loading when Axios is not available (test environments)
let _client: AxiosInstance | null = null;
function getClient(): AxiosInstance {
  if (!_client) {
    _client = createClient();
  }
  return _client;
}

/**
 * REST Client with typed methods
 */
export const restClient = {
  /**
   * GET request
   */
  async get<T>(url: string, config?: RequestConfig): Promise<T> {
    const response = await getClient().get<T>(url, toAxiosConfig(config));
    return response.data;
  },

  /**
   * POST request
   */
  async post<T, D = unknown>(url: string, data?: D, config?: RequestConfig): Promise<T> {
    const response = await getClient().post<T>(url, data, toAxiosConfig(config));
    return response.data;
  },

  /**
   * PUT request
   */
  async put<T, D = unknown>(url: string, data?: D, config?: RequestConfig): Promise<T> {
    const response = await getClient().put<T>(url, data, toAxiosConfig(config));
    return response.data;
  },

  /**
   * PATCH request
   */
  async patch<T, D = unknown>(url: string, data?: D, config?: RequestConfig): Promise<T> {
    const response = await getClient().patch<T>(url, data, toAxiosConfig(config));
    return response.data;
  },

  /**
   * DELETE request
   */
  async delete<T = void>(url: string, config?: RequestConfig): Promise<T> {
    const response = await getClient().delete<T>(url, toAxiosConfig(config));
    return response.data;
  },

  /**
   * HEAD request (for checking existence)
   */
  async head(url: string, config?: RequestConfig): Promise<boolean> {
    try {
      await getClient().head(url, toAxiosConfig(config));
      return true;
    } catch {
      return false;
    }
  },
};

/**
 * Convert RequestConfig to AxiosRequestConfig
 */
function toAxiosConfig(config?: RequestConfig): AxiosRequestConfig | undefined {
  if (!config) return undefined;

  return {
    timeout: config.timeout,
    headers: config.headers,
    params: config.params,
    signal: config.signal,
  };
}

/**
 * Encode a repository item ID for REST API calls.
 * The REST API expects base64(repositoryName:rawId) format.
 *
 * @param repositoryName - Repository name
 * @param rawId - Raw database ID
 * @returns Base64-encoded ID for REST API
 */
export function encodeRepositoryItemId(repositoryName: string, rawId: string): string {
  return btoa(`${repositoryName}:${rawId}`);
}

/**
 * Decode a repository item ID from REST API format.
 *
 * @param encodedId - Base64-encoded ID
 * @returns Object with repositoryName and rawId
 */
export function decodeRepositoryItemId(encodedId: string): { repositoryName: string; rawId: string } {
  const decoded = atob(encodedId);
  const [repositoryName, rawId] = decoded.split(':');
  return { repositoryName, rawId };
}

/**
 * URL builder helpers
 */
export const urlBuilder = {
  /**
   * Build URL with path parameters
   * Example: urlBuilder.path('/v1/users/{id}', { id: '123' }) => '/v1/users/123'
   */
  path(template: string, params: Record<string, string | number>): string {
    let url = template;
    for (const [key, value] of Object.entries(params)) {
      url = url.replace(`{${key}}`, encodeURIComponent(String(value)));
    }
    return url;
  },

  /**
   * Build URL with query parameters
   * Example: urlBuilder.query('/v1/search', { q: 'test', limit: 10 }) => '/v1/search?q=test&limit=10'
   */
  query(baseUrl: string, params: Record<string, string | number | boolean | undefined>): string {
    const searchParams = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined) {
        searchParams.append(key, String(value));
      }
    }
    const queryString = searchParams.toString();
    return queryString ? `${baseUrl}?${queryString}` : baseUrl;
  },

  /**
   * Privilege endpoints
   */
  privileges: {
    list: () => ENDPOINTS.PRIVILEGES,
    get: (name: string) => `${ENDPOINTS.PRIVILEGES}/${encodeURIComponent(name)}`,
    createByType: (type: string) => `${ENDPOINTS.PRIVILEGES}/${encodeURIComponent(type)}`,
    update: (type: string, name: string) =>
      `${ENDPOINTS.PRIVILEGES}/${encodeURIComponent(type)}/${encodeURIComponent(name)}`,
    delete: (name: string) => `${ENDPOINTS.PRIVILEGES}/${encodeURIComponent(name)}`,
  },

  /**
   * Role endpoints
   */
  roles: {
    list: () => ENDPOINTS.ROLES,
    get: (id: string) => `${ENDPOINTS.ROLES}/${encodeURIComponent(id)}`,
    create: () => ENDPOINTS.ROLES,
    update: (id: string) => `${ENDPOINTS.ROLES}/${encodeURIComponent(id)}`,
    delete: (id: string) => `${ENDPOINTS.ROLES}/${encodeURIComponent(id)}`,
  },

  /**
   * User endpoints
   */
  users: {
    list: () => ENDPOINTS.USERS,
    get: (id: string) => `${ENDPOINTS.USERS}/${encodeURIComponent(id)}`,
    create: () => ENDPOINTS.USERS,
    update: (id: string) => `${ENDPOINTS.USERS}/${encodeURIComponent(id)}`,
    delete: (id: string) => `${ENDPOINTS.USERS}/${encodeURIComponent(id)}`,
    changePassword: (id: string) => `${ENDPOINTS.USERS}/${encodeURIComponent(id)}/change-password`,
    invite: () => `${ENDPOINTS.USERS}/invite`,
    updateRoles: (id: string) => `${ENDPOINTS.USERS}/${encodeURIComponent(id)}/roles`,
  },

  /**
   * Content Selector endpoints
   */
  selectors: {
    list: () => ENDPOINTS.CONTENT_SELECTORS,
    get: (name: string) => `${ENDPOINTS.CONTENT_SELECTORS}/${encodeURIComponent(name)}`,
    create: () => ENDPOINTS.CONTENT_SELECTORS,
    update: (name: string) => `${ENDPOINTS.CONTENT_SELECTORS}/${encodeURIComponent(name)}`,
    delete: (name: string) => `${ENDPOINTS.CONTENT_SELECTORS}/${encodeURIComponent(name)}`,
  },

  /**
   * LDAP endpoints
   */
  ldap: {
    list: () => ENDPOINTS.LDAP,
    get: (name: string) => `${ENDPOINTS.LDAP}/${encodeURIComponent(name)}`,
    create: () => ENDPOINTS.LDAP,
    update: (name: string) => `${ENDPOINTS.LDAP}/${encodeURIComponent(name)}`,
    delete: (name: string) => `${ENDPOINTS.LDAP}/${encodeURIComponent(name)}`,
    changeOrder: () => `${ENDPOINTS.LDAP}/change-order`,
    templates: () => `${ENDPOINTS.LDAP}/templates`,
    clearCache: () => `${ENDPOINTS.LDAP}/cache`,
    verifyConnection: () => `${ENDPOINTS.LDAP}/verify-connection`,
    verifyUserMapping: () => `${ENDPOINTS.LDAP}/verify-user-mapping`,
    verifyLogin: () => `${ENDPOINTS.LDAP}/verify-login`,
  },

  /**
   * User Tokens endpoints
   */
  userTokens: {
    get: () => ENDPOINTS.USER_TOKENS,
    update: () => ENDPOINTS.USER_TOKENS,
    reset: () => ENDPOINTS.USER_TOKENS,
  },

  /**
   * Component endpoints
   */
  components: {
    get: (id: string) => `${ENDPOINTS.COMPONENTS}/${encodeURIComponent(id)}`,
    delete: (id: string) => `${ENDPOINTS.COMPONENTS}/${encodeURIComponent(id)}`,
  },

  /**
   * Asset endpoints
   */
  assets: {
    get: (id: string) => `${ENDPOINTS.ASSETS}/${encodeURIComponent(id)}`,
    delete: (id: string) => `${ENDPOINTS.ASSETS}/${encodeURIComponent(id)}`,
  },

  /**
   * Capabilities endpoints
   */
  capabilities: {
    list: () => ENDPOINTS.CAPABILITIES,
    types: () => ENDPOINTS.CAPABILITIES_TYPES,
    get: (id: string) => `${ENDPOINTS.CAPABILITIES}/${encodeURIComponent(id)}`,
    create: () => ENDPOINTS.CAPABILITIES,
    update: (id: string) => `${ENDPOINTS.CAPABILITIES}/${encodeURIComponent(id)}`,
    delete: (id: string) => `${ENDPOINTS.CAPABILITIES}/${encodeURIComponent(id)}`,
  },

  /**
   * Tasks endpoints
   */
  tasks: {
    list: () => ENDPOINTS.TASKS,
    get: (id: string) => `${ENDPOINTS.TASKS}/${encodeURIComponent(id)}`,
    create: () => ENDPOINTS.TASKS,
    update: (id: string) => `${ENDPOINTS.TASKS}/${encodeURIComponent(id)}`,
    delete: (id: string) => `${ENDPOINTS.TASKS}/${encodeURIComponent(id)}`,
    run: (id: string) => `${ENDPOINTS.TASKS}/${encodeURIComponent(id)}/run`,
    stop: (id: string) => `${ENDPOINTS.TASKS}/${encodeURIComponent(id)}/stop`,
    templates: () => `${ENDPOINTS.TASKS}/templates`,
    template: (typeId: string) => `${ENDPOINTS.TASKS}/templates/${encodeURIComponent(typeId)}`,
  },

  /**
   * Email configuration endpoints
   */
  email: {
    get: () => ENDPOINTS.EMAIL,
    update: () => ENDPOINTS.EMAIL,
    delete: () => ENDPOINTS.EMAIL,
    verify: () => ENDPOINTS.EMAIL_VERIFY,
  },

  /**
   * HTTP settings endpoints
   */
  http: {
    get: () => ENDPOINTS.HTTP,
    update: () => ENDPOINTS.HTTP,
    reset: () => ENDPOINTS.HTTP,
  },

  /**
   * Proprietary content endpoints
   */
  proprietaryContent: {
    get: () => ENDPOINTS.PROPRIETARY_CONTENT,
    update: () => ENDPOINTS.PROPRIETARY_CONTENT,
  },

  /**
   * DataStore endpoints
   */
  datastore: {
    get: () => ENDPOINTS.DATASTORE,
    update: () => ENDPOINTS.DATASTORE,
  },

  /**
   * Tags endpoints
   */
  tags: {
    list: () => ENDPOINTS.TAGS,
    get: (name: string) => `${ENDPOINTS.TAGS}/${encodeURIComponent(name)}`,
    create: () => ENDPOINTS.TAGS,
    update: (name: string) => `${ENDPOINTS.TAGS}/${encodeURIComponent(name)}`,
    delete: (name: string) => `${ENDPOINTS.TAGS}/${encodeURIComponent(name)}`,
    filtered: () => ENDPOINTS.TAGS_FILTERED,
  },

  /**
   * Routing rules endpoints
   */
  routingRules: {
    list: () => ENDPOINTS.ROUTING_RULES,
    get: (name: string) => `${ENDPOINTS.ROUTING_RULES}/${encodeURIComponent(name)}`,
    create: () => ENDPOINTS.ROUTING_RULES,
    update: (name: string) => `${ENDPOINTS.ROUTING_RULES}/${encodeURIComponent(name)}`,
    delete: (name: string) => `${ENDPOINTS.ROUTING_RULES}/${encodeURIComponent(name)}`,
  },

  /**
   * Blob stores endpoints
   */
  blobstores: {
    list: () => ENDPOINTS.BLOBSTORES,
    types: () => ENDPOINTS.BLOBSTORES_TYPES,
    quotaTypes: () => ENDPOINTS.BLOBSTORES_QUOTA_TYPES,
    get: (name: string) => `${ENDPOINTS.BLOBSTORES}/${encodeURIComponent(name)}`,
    // Create URL includes type in path: POST /v1/blobstores/{type}
    create: (type: string) => `${ENDPOINTS.BLOBSTORES}/${encodeURIComponent(type.toLowerCase())}`,
    // Update URL includes type and name: PUT /v1/blobstores/{type}/{name}
    update: (type: string, name: string) =>
      `${ENDPOINTS.BLOBSTORES}/${encodeURIComponent(type.toLowerCase())}/${encodeURIComponent(name)}`,
    delete: (name: string) => `${ENDPOINTS.BLOBSTORES}/${encodeURIComponent(name)}`,
    quotaStatus: (name: string) => `${ENDPOINTS.BLOBSTORES}/${encodeURIComponent(name)}/quota-status`,
    // Type-specific get endpoints
    getFile: (name: string) => `${ENDPOINTS.BLOBSTORES}/file/${encodeURIComponent(name)}`,
    getS3: (name: string) => `${ENDPOINTS.BLOBSTORES}/s3/${encodeURIComponent(name)}`,
    getAzure: (name: string) => `${ENDPOINTS.BLOBSTORES}/azure/${encodeURIComponent(name)}`,
    getGoogle: (name: string) => `${ENDPOINTS.BLOBSTORES}/google/${encodeURIComponent(name)}`,
  },
};

export default restClient;
