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
import { ENDPOINTS, restClient } from '../../../../../../interface/api';
import { createFormMachine, type FormContext, type ValidationErrors } from '../../../../../../interface/form';

import { HttpConfiguration, DEFAULT_HTTP_CONFIGURATION } from './types';

/**
 * Validates a proxy hostname or IP address.
 * Accepts: single-label hostnames (localhost), FQDNs (proxy.example.com),
 *          IPv4 (192.168.1.1), IPv6 in brackets ([::1]).
 * Rejects: URLs with scheme (http://...), whitespace, paths.
 * Mirrors legacy ValidationUtils.validateHost.
 * Note: IPv4 validation checks digit count only (1-3 digits per octet),
 * not value range — invalid IPs like 999.0.0.1 pass regex but are rejected by the server.
 */
const HOST_REGEX = /^(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*|\[[0-9A-Fa-f:.]+\]|(?:\d{1,3}\.){3}\d{1,3})$/;

function validateHttpConfig(data: HttpConfiguration): ValidationErrors {
  const errors: ValidationErrors = {};

  // Timeout: optional; if provided, integer in [1, 3600]
  if (data.timeout != null) {
    if (!Number.isInteger(Number(data.timeout)) || data.timeout < 1 || data.timeout > 3600) {
      errors.timeout = 'Timeout must be between 1 and 3600 seconds';
    }
  }

  // Retries: optional; if provided, integer in [0, 10]
  if (data.retries != null) {
    if (!Number.isInteger(Number(data.retries)) || data.retries < 0 || data.retries > 10) {
      errors.retries = 'Retries must be between 0 and 10';
    }
  }

  // HTTP proxy validation
  if (data.httpEnabled) {
    if (!data.httpHost?.trim()) {
      errors.httpHost = 'HTTP proxy host is required';
    } else if (!HOST_REGEX.test(data.httpHost.trim())) {
      errors.httpHost = 'Provide a hostname or IP address (no scheme, no path)';
    }
    if (data.httpPort == null) {
      errors.httpPort = 'HTTP proxy port is required';
    } else if (!Number.isInteger(Number(data.httpPort)) || data.httpPort < 1 || data.httpPort > 65535) {
      errors.httpPort = 'Port must be between 1 and 65535';
    }
    if (data.httpAuthType === 'username' && !data.httpAuthUsername?.trim()) {
      errors.httpAuthUsername = 'Username is required when authentication is enabled';
    }
  }

  // HTTPS proxy validation
  if (data.httpsEnabled) {
    if (!data.httpsHost?.trim()) {
      errors.httpsHost = 'HTTPS proxy host is required';
    } else if (!HOST_REGEX.test(data.httpsHost.trim())) {
      errors.httpsHost = 'Provide a hostname or IP address (no scheme, no path)';
    }
    if (data.httpsPort == null) {
      errors.httpsPort = 'HTTPS proxy port is required';
    } else if (!Number.isInteger(Number(data.httpsPort)) || data.httpsPort < 1 || data.httpsPort > 65535) {
      errors.httpsPort = 'Port must be between 1 and 65535';
    }
    if (data.httpsAuthType === 'username' && !data.httpsAuthUsername?.trim()) {
      errors.httpsAuthUsername = 'Username is required when authentication is enabled';
    }
  }

  return errors;
}

/**
 * REST API HTTP settings shape — mirrors HttpSettingsXo / ProxySettingsXo / AuthSettingsXo
 */
interface RestAuthSettings {
  enabled: boolean;
  username: string;
  password: string;
  ntlmHost: string;
  ntlmDomain: string;
}

interface RestProxySettings {
  enabled: boolean;
  host: string;
  port: string;
  authInfo: RestAuthSettings;
}

interface RestHttpSettings {
  userAgent: string;
  timeout: number | null;
  retries: number | null;
  httpProxy: RestProxySettings;
  httpsProxy: RestProxySettings;
  nonProxyHosts: string[];
}

const DEFAULT_AUTH: RestAuthSettings = {
  enabled: false,
  username: '',
  password: '',
  ntlmHost: '',
  ntlmDomain: '',
};

/**
 * Transform REST response to UI model
 */
function restToHttpConfig(data: RestHttpSettings): HttpConfiguration {
  return {
    ...DEFAULT_HTTP_CONFIGURATION,
    userAgentSuffix: data.userAgent ?? '',
    timeout: data.timeout ?? null,
    retries: data.retries ?? null,
    httpEnabled: data.httpProxy?.enabled ?? false,
    httpHost: data.httpProxy?.host ?? '',
    httpPort: data.httpProxy?.port ? parseInt(data.httpProxy.port, 10) || null : null,
    httpAuthType: (data.httpProxy?.authInfo?.enabled ?? false) ? 'username' : '',
    httpAuthUsername: data.httpProxy?.authInfo?.username ?? '',
    httpAuthPassword: data.httpProxy?.authInfo?.password ?? '',
    httpAuthNtlmHost: data.httpProxy?.authInfo?.ntlmHost ?? '',
    httpAuthNtlmDomain: data.httpProxy?.authInfo?.ntlmDomain ?? '',
    httpsEnabled: data.httpsProxy?.enabled ?? false,
    httpsHost: data.httpsProxy?.host ?? '',
    httpsPort: data.httpsProxy?.port ? parseInt(data.httpsProxy.port, 10) || null : null,
    httpsAuthType: (data.httpsProxy?.authInfo?.enabled ?? false) ? 'username' : '',
    httpsAuthUsername: data.httpsProxy?.authInfo?.username ?? '',
    httpsAuthPassword: data.httpsProxy?.authInfo?.password ?? '',
    httpsAuthNtlmHost: data.httpsProxy?.authInfo?.ntlmHost ?? '',
    httpsAuthNtlmDomain: data.httpsProxy?.authInfo?.ntlmDomain ?? '',
    nonProxyHosts: data.nonProxyHosts ?? [],
  };
}

/**
 * Transform UI model to REST format — always sends the nested httpProxy/httpsProxy objects
 * required by HttpSettingsXo's @NotNull constraints.
 */
function httpConfigToRest(config: HttpConfiguration): RestHttpSettings {
  const httpAuth: RestAuthSettings = config.httpEnabled && config.httpAuthType === 'username'
    ? {
        enabled: true,
        username: config.httpAuthUsername || '',
        password: config.httpAuthPassword || '',
        ntlmHost: config.httpAuthNtlmHost || '',
        ntlmDomain: config.httpAuthNtlmDomain || '',
      }
    : DEFAULT_AUTH;

  const httpsAuth: RestAuthSettings = config.httpsEnabled && config.httpsAuthType === 'username'
    ? {
        enabled: true,
        username: config.httpsAuthUsername || '',
        password: config.httpsAuthPassword || '',
        ntlmHost: config.httpsAuthNtlmHost || '',
        ntlmDomain: config.httpsAuthNtlmDomain || '',
      }
    : DEFAULT_AUTH;

  return {
    userAgent: config.userAgentSuffix || '',
    timeout: config.timeout ?? null,
    retries: config.retries ?? null,
    httpProxy: {
      enabled: config.httpEnabled,
      host: config.httpEnabled ? (config.httpHost || '') : '',
      port: config.httpEnabled && config.httpPort != null ? String(config.httpPort) : '',
      authInfo: httpAuth,
    },
    httpsProxy: {
      enabled: config.httpsEnabled,
      host: config.httpsEnabled ? (config.httpsHost || '') : '',
      port: config.httpsEnabled && config.httpsPort != null ? String(config.httpsPort) : '',
      authInfo: httpsAuth,
    },
    nonProxyHosts: !config.httpEnabled && !config.httpsEnabled ? [] : (config.nonProxyHosts || []),
  };
}

/**
 * Create the HTTP settings form machine.
 *
 * Simple settings form - loads current config, allows editing, saves via PUT.
 * No type variants or editingConfig needed.
 */
export function createHttpFormMachine() {
  return createFormMachine<HttpConfiguration>({
    id: 'http-form',
    stayEditableAfterSave: true,
    context: {
      data: { ...DEFAULT_HTTP_CONFIGURATION },
    },
    actions: {
      validate: assign((ctx: FormContext<HttpConfiguration>) => ({
        validationErrors: validateHttpConfig(ctx.data),
      })),
    },
    services: {
      load: async () => {
        const rest = await restClient.get<RestHttpSettings>(ENDPOINTS.HTTP);
        const data = restToHttpConfig(rest);
        return { data };
      },
      save: async (ctx: FormContext<HttpConfiguration>) => {
        const payload = httpConfigToRest(ctx.data);
        await restClient.put(ENDPOINTS.HTTP, payload);
      },
    },
  });
}

export { validateHttpConfig, restToHttpConfig, httpConfigToRest };
