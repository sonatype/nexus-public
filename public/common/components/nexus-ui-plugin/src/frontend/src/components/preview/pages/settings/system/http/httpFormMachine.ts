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
 * Validate HTTP configuration form data.
 * Proxy host/port are required when the respective proxy is enabled.
 * Timeout and retries must be non-negative integers when provided.
 */
function validateHttpConfig(data: HttpConfiguration): ValidationErrors {
  const errors: ValidationErrors = {};

  // Timeout validation
  if (data.timeout != null && data.timeout !== 0) {
    if (!Number.isInteger(Number(data.timeout)) || data.timeout < 0) {
      errors.timeout = 'Timeout must be a non-negative integer (seconds)';
    }
  }

  // Retries validation
  if (data.retries != null && data.retries !== 0) {
    if (!Number.isInteger(Number(data.retries)) || data.retries < 0) {
      errors.retries = 'Retries must be a non-negative integer';
    }
  }

  // HTTP proxy validation
  if (data.httpEnabled) {
    if (!data.httpHost?.trim()) {
      errors.httpHost = 'HTTP proxy host is required';
    }
    if (data.httpPort == null || data.httpPort === 0) {
      errors.httpPort = 'HTTP proxy port is required';
    } else if (!Number.isInteger(Number(data.httpPort)) || data.httpPort < 1 || data.httpPort > 65535) {
      errors.httpPort = 'Port must be between 1 and 65535';
    }
    if (data.httpAuthEnabled && !data.httpAuthUsername?.trim()) {
      errors.httpAuthUsername = 'Username is required when authentication is enabled';
    }
  }

  // HTTPS proxy validation
  if (data.httpsEnabled) {
    if (!data.httpsHost?.trim()) {
      errors.httpsHost = 'HTTPS proxy host is required';
    }
    if (data.httpsPort == null || data.httpsPort === 0) {
      errors.httpsPort = 'HTTPS proxy port is required';
    } else if (!Number.isInteger(Number(data.httpsPort)) || data.httpsPort < 1 || data.httpsPort > 65535) {
      errors.httpsPort = 'Port must be between 1 and 65535';
    }
    if (data.httpsAuthEnabled && !data.httpsAuthUsername?.trim()) {
      errors.httpsAuthUsername = 'Username is required when authentication is enabled';
    }
  }

  return errors;
}

/**
 * REST API HTTP settings shape
 */
interface RestHttpSettings {
  userAgentSuffix?: string | null;
  timeout?: number | null;
  retries?: number | null;
  httpEnabled?: boolean;
  httpHost?: string | null;
  httpPort?: number | null;
  httpAuthEnabled?: boolean;
  httpAuthUsername?: string | null;
  httpAuthPassword?: string | null;
  httpAuthNtlmHost?: string | null;
  httpAuthNtlmDomain?: string | null;
  httpsEnabled?: boolean;
  httpsHost?: string | null;
  httpsPort?: number | null;
  httpsAuthEnabled?: boolean;
  httpsAuthUsername?: string | null;
  httpsAuthPassword?: string | null;
  httpsAuthNtlmHost?: string | null;
  httpsAuthNtlmDomain?: string | null;
  nonProxyHosts?: string[];
}

/**
 * Transform REST response to UI model
 */
function restToHttpConfig(data: RestHttpSettings): HttpConfiguration {
  return {
    ...DEFAULT_HTTP_CONFIGURATION,
    ...data,
    userAgentSuffix: data.userAgentSuffix ?? '',
    timeout: data.timeout ?? null,
    retries: data.retries ?? null,
    httpEnabled: data.httpEnabled ?? false,
    httpHost: data.httpHost ?? '',
    httpPort: data.httpPort ?? null,
    httpAuthEnabled: data.httpAuthEnabled ?? false,
    httpAuthUsername: data.httpAuthUsername ?? '',
    httpAuthPassword: data.httpAuthPassword ?? '',
    httpAuthNtlmHost: data.httpAuthNtlmHost ?? '',
    httpAuthNtlmDomain: data.httpAuthNtlmDomain ?? '',
    httpsEnabled: data.httpsEnabled ?? false,
    httpsHost: data.httpsHost ?? '',
    httpsPort: data.httpsPort ?? null,
    httpsAuthEnabled: data.httpsAuthEnabled ?? false,
    httpsAuthUsername: data.httpsAuthUsername ?? '',
    httpsAuthPassword: data.httpsAuthPassword ?? '',
    httpsAuthNtlmHost: data.httpsAuthNtlmHost ?? '',
    httpsAuthNtlmDomain: data.httpsAuthNtlmDomain ?? '',
    nonProxyHosts: data.nonProxyHosts ?? [],
  };
}

/**
 * Transform UI model to REST format for saving.
 * Only includes proxy fields when the respective proxy is enabled.
 */
function httpConfigToRest(config: HttpConfiguration): RestHttpSettings {
  const saveData: RestHttpSettings = {
    userAgentSuffix: config.userAgentSuffix || null,
    timeout: config.timeout || null,
    retries: config.retries || null,
    httpEnabled: config.httpEnabled,
    httpsEnabled: config.httpsEnabled,
    nonProxyHosts: config.nonProxyHosts || [],
  };

  if (config.httpEnabled) {
    saveData.httpHost = config.httpHost || null;
    saveData.httpPort = config.httpPort || null;
    saveData.httpAuthEnabled = config.httpAuthEnabled || false;
    if (config.httpAuthEnabled) {
      saveData.httpAuthUsername = config.httpAuthUsername || null;
      saveData.httpAuthPassword = config.httpAuthPassword || null;
      saveData.httpAuthNtlmHost = config.httpAuthNtlmHost || null;
      saveData.httpAuthNtlmDomain = config.httpAuthNtlmDomain || null;
    }
  }

  if (config.httpsEnabled) {
    saveData.httpsHost = config.httpsHost || null;
    saveData.httpsPort = config.httpsPort || null;
    saveData.httpsAuthEnabled = config.httpsAuthEnabled || false;
    if (config.httpsAuthEnabled) {
      saveData.httpsAuthUsername = config.httpsAuthUsername || null;
      saveData.httpsAuthPassword = config.httpsAuthPassword || null;
      saveData.httpsAuthNtlmHost = config.httpsAuthNtlmHost || null;
      saveData.httpsAuthNtlmDomain = config.httpsAuthNtlmDomain || null;
    }
  }

  if (!config.httpEnabled && !config.httpsEnabled) {
    saveData.nonProxyHosts = [];
  }

  return saveData;
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
