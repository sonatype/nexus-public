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
import { createFormMachine } from '@sonatype/nexus-ui-plugin';
import type { FormContext, ValidationErrors } from '@sonatype/nexus-ui-plugin';
import { CertificateSource, SslCertificate, CERTIFICATE_SOURCES } from './types';

/**
 * Form data shape for SSL certificate add form
 */
export interface SslFormData {
  source: CertificateSource;
  remoteHostUrl: string;
  pemContent: string;
}

/**
 * Extended form context with certificate preview data
 */
interface SslFormContext extends FormContext<SslFormData> {
  certificateDetails: SslCertificate | null;
}

/**
 * Default form data
 */
const DEFAULT_SSL_FORM_DATA: SslFormData = {
  source: CERTIFICATE_SOURCES.REMOTE_HOST,
  remoteHostUrl: '',
  pemContent: '',
};

/**
 * Guard factory: creates a guard that checks if a SOURCE_CHANGE event targets a specific source
 */
const isSourceGuard = (targetSource: CertificateSource) =>
  (_context: unknown, event: { type: string; value?: string }) => event.value === targetSource;

/**
 * Validate SSL certificate form data based on the active source.
 * - In remoteHost mode: remoteHostUrl is required
 * - In PEM mode: pemContent is required
 */
function validateSslForm(data: SslFormData): ValidationErrors {
  const errors: ValidationErrors = {};

  if (data.source === CERTIFICATE_SOURCES.REMOTE_HOST) {
    if (!data.remoteHostUrl?.trim()) {
      errors.remoteHostUrl = 'Hostname or URL is required';
    }
  } else if (data.source === CERTIFICATE_SOURCES.PEM) {
    if (!data.pemContent?.trim()) {
      errors.pemContent = 'PEM content is required';
    }
  }

  return errors;
}

/**
 * Create an SSL certificate form machine.
 *
 * This form uses editingConfig with two sub-states:
 * - remoteHost: load certificate from a remote server (host + optional port)
 * - PEM: paste PEM-encoded certificate text
 *
 * The SOURCE_CHANGE event switches between sub-states and resets
 * the irrelevant fields. No load service is used since this is a
 * create-only form.
 */
export function createSslFormMachine() {
  return createFormMachine({
    id: 'ssl-certificate-form',
    context: {
      data: { ...DEFAULT_SSL_FORM_DATA } as SslFormData,
      certificateDetails: null as SslCertificate | null,
    } as SslFormContext,
    actions: {
      validate: assign((ctx: SslFormContext) => ({
        validationErrors: validateSslForm(ctx.data),
      })),
      // Custom action: switch source and reset the other source's fields
      changeSource: assign((context: any, event: any) => ({
        data: {
          ...context.data,
          source: event.value,
          // Clear the field that doesn't belong to the new source
          remoteHostUrl: event.value === CERTIFICATE_SOURCES.REMOTE_HOST ? context.data.remoteHostUrl : '',
          pemContent: event.value === CERTIFICATE_SOURCES.PEM ? context.data.pemContent : '',
        },
        touched: {},
        certificateDetails: null,
      })),
      // Clear certificate preview when fields change
      clearPreview: assign({
        certificateDetails: null,
      }),
    },
    guards: {
      isSourceRemoteHost: isSourceGuard(CERTIFICATE_SOURCES.REMOTE_HOST) as any,
      isSourcePem: isSourceGuard(CERTIFICATE_SOURCES.PEM) as any,
    },
    // Custom events for source switching
    on: {
      SOURCE_CHANGE: [
        {
          target: '.remoteHost',
          cond: 'isSourceRemoteHost',
          actions: ['changeSource', 'validate', 'computePristine'],
        },
        {
          target: '.PEM',
          cond: 'isSourcePem',
          actions: ['changeSource', 'validate', 'computePristine'],
        },
      ],
    },
    // Sub-states for source type variants within the editing state
    editingConfig: {
      defaultState: CERTIFICATE_SOURCES.REMOTE_HOST,
      typeField: 'source',
      states: {
        [CERTIFICATE_SOURCES.REMOTE_HOST]: {
          meta: {
            typeLabel: 'Load from Server',
            fields: ['remoteHostUrl'],
            requiredFields: ['remoteHostUrl'],
            fieldConfig: {
              remoteHostUrl: {
                label: 'Hostname or URL',
                type: 'text',
                placeholder: 'example.com or example.com:443 or https://example.com',
                helpText: 'Enter a hostname, hostname:port, or URL to fetch an SSL certificate from',
              },
            },
          },
        },
        [CERTIFICATE_SOURCES.PEM]: {
          meta: {
            typeLabel: 'Paste PEM',
            fields: ['pemContent'],
            requiredFields: ['pemContent'],
            fieldConfig: {
              pemContent: {
                label: 'Paste Certificate as PEM',
                type: 'textarea',
                placeholder: '-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----',
                helpText: 'Paste certificate content in PEM format',
              },
            },
          },
        },
      },
    },
  });
}
