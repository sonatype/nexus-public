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
import { notifySessionExpiredFromRest } from '@sonatype/nexus-ui-plugin';
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

  // Repositories
  REPOSITORIES: `${API_V1}/repositories`,
  COMPONENTS: `${API_V1}/components`,
  ASSETS: `${API_V1}/assets`,
  ROUTING_RULES: `${API_V1}/routing-rules`,
  
  // Browse (REST API for tree browsing)
  REPOSITORY_BROWSE: (repositoryName: string) => `${API_V1}/repositories/${encodeURIComponent(repositoryName)}/browse`,

  // Search
  SEARCH: `${API_V1}/search`,
  SEARCH_ASSETS: `${API_V1}/search/assets`,

  // System Settings
  CAPABILITIES: `${API_V1}/capabilities`,
  CAPABILITIES_TYPES: `${API_V1}/capabilities/types`,
  TASKS: `${API_V1}/tasks`,
  EMAIL: `${API_V1}/email`,
  EMAIL_VERIFY: `${API_V1}/email/verify`,
  HTTP_SETTINGS: `${API_V1}/http`,

  // Status
  STATUS: `${API_V1}/status`,

  // Tags
  TAGS: `${API_V1}/tags`,
  TAGS_FILTERED: `${API_INTERNAL_UI}/tags/filtered`,

  // Upload
  UPLOAD_DEFINITIONS: `${API_INTERNAL_UI}/upload/definitions`,

  // Internal UI endpoints
  /** Merged JAX-RS + manual permission map for Preview UI API hub */
  API_PERMISSIONS: `${API_INTERNAL_UI}/api/permissions`,
  /** POST body: userId?, roleId?, endpoint, method — see ApiAccessCheckXo */
  SECURITY_ACCESS_CHECK: `${API_INTERNAL_UI}/security/access-check`,
  PRIVILEGE_TYPES: `${API_INTERNAL_UI}/privileges/types`,
  ROLE_SOURCES: `${API_INTERNAL_UI}/roles/sources`,
  USER_SOURCES: `${API_V1}/security/user-sources`,
  REPOSITORIES_DETAILS: `${API_INTERNAL}/ui/repositories/details`,
  BROWSE: `${API_INTERNAL_UI}/browse`,
  NODES: `${API_INTERNAL_UI}/nodes`,
  HEALTH_CHECK: `${API_INTERNAL_UI}/healthcheck`,
  /** Same repo list as HEALTH_CHECK without per-repo details HTML reads (faster for many proxies, e.g. cloud) */
  HEALTH_CHECK_SUMMARY: `${API_INTERNAL_UI}/healthcheck/summary`,
  HEALTH_CHECK_ANALYZE: (repoName: string) => `${API_V1}/repositories/${encodeURIComponent(repoName)}/health-check`,
  /** Enable/disable Repository Health Check (POST enable / DELETE disable) */
  REPOSITORY_HEALTH_CHECK: (repoName: string) =>
    `${API_V1}/repositories/${encodeURIComponent(repoName)}/health-check`,
  FIREWALL_STATUS: `${API_INTERNAL_UI}/firewall/status`,
  FIREWALL_STATUS_SUMMARY: `${API_INTERNAL_UI}/firewall/status/summary`,
  FIREWALL_STATUS_REPO: (repoName: string) => `${API_INTERNAL_UI}/firewall/status/repo/${encodeURIComponent(repoName)}`,

  // IQ Server connection status
  IQ_CONNECTION: `${API_V1}/iq`,

  // IQ component evaluation (policy violations for a specific component version)
  IQ_COMPONENT_EVALUATION: `${API_V1}/iq/component-evaluation`,

  // IQ Firewall audit (enable Audit or Quarantine per repo)
  IQ_AUDIT: `${API_V1}/iq/audit`,
  IQ_AUDIT_REPO: (repositoryName: string) => `${API_V1}/iq/audit/${encodeURIComponent(repositoryName)}`,
  /** IQ Server capabilities: hasFirewall, connected, url, deploymentId */
  IQ_CAPABILITIES: `${API_V1}/iq/capabilities`,

  // Security report artifact table (Health Check / Firewall)
  SECURITY_REPORT_ARTIFACTS: `${API_INTERNAL_UI}/security-report/artifacts`,

  // Malicious risk
  MALICIOUS_RISK_ON_DISK: `${API_V1}/malicious-risk/risk-on-disk`,
  MALICIOUS_RISK_COMPONENTS: `${API_V1}/malicious-risk/components`,
  MALICIOUS_RISK_CSV: `${API_V1}/malicious-risk/malware-components-csv`,
  MALWARE_COUNTS: `${API_INTERNAL_UI}/malware/counts`,
  MALICIOUS_RISK_ACTIVE_FINDINGS: `${API_V1}/malicious-risk/active-findings`,
  MALICIOUS_RISK_HISTORY: `${API_V1}/malicious-risk/history`,
  MALICIOUS_RISK_ACKNOWLEDGE: `${API_V1}/malicious-risk/acknowledge`,
  MALICIOUS_RISK_DELETE_FINDING: `${API_V1}/malicious-risk/delete-finding`,
  MALICIOUS_RISK_REMEDIATE: `${API_V1}/malicious-risk/remediate`,

  IQ: `${API_INTERNAL_UI}/iq`,

  /** Protect module change history (audit_events subset) */
  PROTECT_AUDIT_EVENTS: `${API_INTERNAL_UI}/protect/audit-events`,
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
        config.headers['Pragma'] = 'no-cache';
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

// Singleton client instance
const axiosClient = createClient();

/**
 * REST Client with typed methods
 */
export const restClient = {
  /**
   * GET request
   */
  async get<T>(url: string, config?: RequestConfig): Promise<T> {
    const response = await axiosClient.get<T>(url, toAxiosConfig(config));
    return response.data;
  },

  /**
   * POST request
   */
  async post<T, D = unknown>(url: string, data?: D, config?: RequestConfig): Promise<T> {
    const response = await axiosClient.post<T>(url, data, toAxiosConfig(config));
    return response.data;
  },

  /**
   * PUT request
   */
  async put<T, D = unknown>(url: string, data?: D, config?: RequestConfig): Promise<T> {
    const response = await axiosClient.put<T>(url, data, toAxiosConfig(config));
    return response.data;
  },

  /**
   * PATCH request
   */
  async patch<T, D = unknown>(url: string, data?: D, config?: RequestConfig): Promise<T> {
    const response = await axiosClient.patch<T>(url, data, toAxiosConfig(config));
    return response.data;
  },

  /**
   * DELETE request
   */
  async delete<T = void>(url: string, config?: RequestConfig): Promise<T> {
    const response = await axiosClient.delete<T>(url, toAxiosConfig(config));
    return response.data;
  },

  /**
   * HEAD request (for checking existence)
   */
  async head(url: string, config?: RequestConfig): Promise<boolean> {
    try {
      await axiosClient.head(url, toAxiosConfig(config));
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
 * The REST API expects URL-safe base64(repositoryName:rawId) format without padding.
 * Uses URL-safe encoding: + becomes -, / becomes _, and removes padding =.
 *
 * @param repositoryName - Repository name
 * @param rawId - Raw database ID
 * @returns URL-safe Base64-encoded ID for REST API
 */
export function encodeRepositoryItemId(repositoryName: string, rawId: string): string {
  // Use standard btoa, then convert to URL-safe format (matching Java's Base64.getUrlEncoder().withoutPadding())
  const standardBase64 = btoa(`${repositoryName}:${rawId}`);
  return standardBase64
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, ''); // Remove padding
}

/**
 * Decode a repository item ID from REST API format.
 * Handles URL-safe base64 where + becomes - and / becomes _.
 *
 * @param encodedId - URL-safe Base64-encoded ID
 * @returns Object with repositoryName and rawId
 */
export function decodeRepositoryItemId(encodedId: string): { repositoryName: string; rawId: string } {
  // Convert URL-safe back to standard base64
  let standardBase64 = encodedId
    .replace(/-/g, '+')
    .replace(/_/g, '/');
  // Add padding if needed
  while (standardBase64.length % 4 !== 0) {
    standardBase64 += '=';
  }
  const decoded = atob(standardBase64);
  // Use indexOf to handle colons in rawId (e.g., Maven GAV: group:artifact:version)
  const colonIndex = decoded.indexOf(':');
  const repositoryName = decoded.substring(0, colonIndex);
  const rawId = decoded.substring(colonIndex + 1);
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
};

export default restClient;
