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

import { EmailConfiguration, DEFAULT_EMAIL_CONFIGURATION } from './types';

/**
 * Email address regex (simple but sufficient for UI validation)
 */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Validate email configuration form data.
 * Host, port, and fromAddress are only required when email is enabled.
 */
function validateEmailConfig(data: EmailConfiguration): ValidationErrors {
  const errors: ValidationErrors = {};

  if (data.enabled) {
    if (!data.host?.trim()) {
      errors.host = 'SMTP host is required';
    }

    if (data.port == null || data.port === 0) {
      errors.port = 'Port is required';
    } else if (!Number.isInteger(Number(data.port)) || data.port < 1 || data.port > 65535) {
      errors.port = 'Port must be between 1 and 65535';
    }

    if (!data.fromAddress?.trim()) {
      errors.fromAddress = 'From address is required';
    } else if (!EMAIL_REGEX.test(data.fromAddress)) {
      errors.fromAddress = 'Invalid email address format';
    }
  }

  return errors;
}

/**
 * REST API email configuration shape (matches ApiEmailConfiguration.java)
 */
interface RestEmailConfiguration {
  enabled: boolean;
  host?: string;
  port?: number;
  username?: string;
  password?: string;
  fromAddress?: string;
  subjectPrefix?: string;
  startTlsEnabled?: boolean;
  startTlsRequired?: boolean;
  sslOnConnectEnabled?: boolean;
  sslServerIdentityCheckEnabled?: boolean;
  nexusTrustStoreEnabled?: boolean;
}

/**
 * Transform REST response to UI model
 */
function restToEmailConfig(rest: RestEmailConfiguration): EmailConfiguration {
  return {
    enabled: rest.enabled ?? false,
    host: rest.host ?? '',
    port: rest.port ?? 25,
    username: rest.username ?? '',
    password: rest.password ?? '',
    fromAddress: rest.fromAddress ?? '',
    subjectPrefix: rest.subjectPrefix ?? '',
    startTlsEnabled: rest.startTlsEnabled ?? false,
    startTlsRequired: rest.startTlsRequired ?? false,
    sslOnConnectEnabled: rest.sslOnConnectEnabled ?? false,
    sslCheckServerIdentityEnabled: rest.sslServerIdentityCheckEnabled ?? false,
    nexusTrustStoreEnabled: rest.nexusTrustStoreEnabled ?? false,
  };
}

/**
 * Transform UI model to REST format for saving
 */
function emailConfigToRest(config: EmailConfiguration): RestEmailConfiguration {
  return {
    enabled: config.enabled,
    host: config.host,
    port: config.port,
    username: config.username,
    password: config.password,
    fromAddress: config.fromAddress,
    subjectPrefix: config.subjectPrefix,
    startTlsEnabled: config.startTlsEnabled,
    startTlsRequired: config.startTlsRequired,
    sslOnConnectEnabled: config.sslOnConnectEnabled,
    sslServerIdentityCheckEnabled: config.sslCheckServerIdentityEnabled,
    nexusTrustStoreEnabled: config.nexusTrustStoreEnabled,
  };
}

/**
 * Create the email settings form machine.
 *
 * Simple settings form - loads current config, allows editing, saves via PUT.
 * No type variants or editingConfig needed.
 */
export function createEmailFormMachine() {
  return createFormMachine<EmailConfiguration>({
    id: 'email-form',
    context: {
      data: { ...DEFAULT_EMAIL_CONFIGURATION },
    },
    actions: {
      validate: assign((ctx: FormContext<EmailConfiguration>) => ({
        validationErrors: validateEmailConfig(ctx.data),
      })),
    },
    services: {
      load: async () => {
        const rest = await restClient.get<RestEmailConfiguration>(ENDPOINTS.EMAIL);
        const data = restToEmailConfig(rest);
        return { data };
      },
      save: async (ctx: FormContext<EmailConfiguration>) => {
        const payload = emailConfigToRest(ctx.data);
        await restClient.put(ENDPOINTS.EMAIL, payload);
      },
    },
  });
}

export { validateEmailConfig, restToEmailConfig, emailConfigToRest };
