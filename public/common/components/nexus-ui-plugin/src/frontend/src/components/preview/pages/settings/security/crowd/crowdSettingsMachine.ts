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

import { createMachine, assign } from 'xstate';
import { equals } from 'ramda';
import { extractErrorMessage } from '../../../../../../interface/form';
import { CrowdConfig, DEFAULT_CROWD_CONFIG } from './types';

/**
 * Crowd validation errors keyed by field name.
 */
export type CrowdValidationErrors = Record<string, string>;

/** URL required/format error, or undefined when valid. */
function getUrlError(url: string): string | undefined {
  if (!url?.trim()) return 'Crowd server URL is required';
  if (!/^https?:\/\/.+/.test(url)) return 'URL is not valid';
  return undefined;
}

/**
 * Validate Crowd configuration. Mirrors the legacy synchronous validation so
 * the same error messages are produced (URL, required credentials, timeout).
 */
export function validateCrowd(cfg: CrowdConfig): CrowdValidationErrors {
  const errors: CrowdValidationErrors = {};

  const urlError = getUrlError(cfg.url);
  if (urlError) {
    errors.url = urlError;
  }

  if (!cfg.applicationName?.trim()) {
    errors.applicationName = 'Application name is required';
  }
  if (!cfg.applicationPassword?.trim()) {
    errors.applicationPassword = 'Application password is required';
  }

  if (cfg.timeout !== undefined && cfg.timeout !== null) {
    const timeout = Number(cfg.timeout);
    if (Number.isNaN(timeout)) {
      errors.timeout = 'Timeout must be a number';
    } else if (timeout < 1 || timeout > 3600) {
      errors.timeout = 'Timeout must be between 1 and 3600 seconds';
    }
  }

  return errors;
}

export interface CrowdMachineContext {
  data: CrowdConfig;
  pristineData: CrowdConfig;
  validationErrors: CrowdValidationErrors;
  isPristine: boolean;
  /** Shared operation error (save/verify/clear-cache), shown in the alert banner. */
  error: string | null;
}

export type CrowdEvent =
  | { type: 'UPDATE'; field: keyof CrowdConfig; value: string | boolean | number | undefined }
  | { type: 'SUBMIT' }
  | { type: 'DISCARD' }
  | { type: 'VERIFY_CONNECTION' }
  | { type: 'CLEAR_CACHE' }
  | { type: 'CLEAR_ERROR' };

/**
 * Bespoke Crowd settings machine.
 *
 * Unlike a standard form, Crowd has two extra invoked operations —
 * verify-connection and clear-cache — each with its own busy state. Modeling
 * them as explicit states (rather than forcing them into createFormMachine)
 * keeps a single source of truth for the shared busy/error state and gives
 * every async operation automatic cancellation on state exit.
 *
 * Load/save/verify/clear services are injected by the integration hook.
 */
export function createCrowdSettingsMachine() {
  return createMachine<CrowdMachineContext, CrowdEvent>(
    {
      id: 'crowd-settings',
      initial: 'loading',
      context: {
        data: { ...DEFAULT_CROWD_CONFIG },
        pristineData: { ...DEFAULT_CROWD_CONFIG },
        validationErrors: {},
        isPristine: true,
        error: null,
      },
      states: {
        loading: {
          invoke: {
            src: 'load',
            onDone: { target: 'editing', actions: 'setData' },
            // Parity with legacy: a load failure is swallowed — the form shows
            // defaults with reactive validation, no error banner.
            onError: { target: 'editing' },
          },
        },

        editing: {
          entry: ['validate', 'computePristine'],
          on: {
            UPDATE: { actions: ['updateField', 'validate', 'computePristine'] },
            SUBMIT: { target: 'saving', cond: 'canSave' },
            DISCARD: { actions: ['resetForm', 'validate', 'computePristine'] },
            VERIFY_CONNECTION: { target: 'verifyingConnection', cond: 'canVerify' },
            CLEAR_CACHE: { target: 'clearingCache' },
            CLEAR_ERROR: { actions: 'clearError' },
          },
        },

        saving: {
          entry: 'clearError',
          invoke: {
            src: 'save',
            onDone: { target: 'editing', actions: 'onSaveSuccess' },
            onError: { target: 'editing', actions: 'setError' },
          },
        },

        verifyingConnection: {
          entry: 'clearError',
          invoke: {
            src: 'verifyConnection',
            onDone: { target: 'editing' },
            onError: { target: 'editing', actions: 'setError' },
          },
        },

        clearingCache: {
          entry: 'clearError',
          invoke: {
            src: 'clearCache',
            onDone: { target: 'editing' },
            onError: { target: 'editing', actions: 'setError' },
          },
        },
      },
    },
    {
      actions: {
        setData: assign((_, event) => {
          const data = (event as unknown as { data: CrowdConfig }).data;
          return { data, pristineData: data };
        }),
        updateField: assign((context, event) => {
          const e = event as Extract<CrowdEvent, { type: 'UPDATE' }>;
          return { data: { ...context.data, [e.field]: e.value } };
        }),
        validate: assign((context) => ({ validationErrors: validateCrowd(context.data) })),
        computePristine: assign((context) => ({
          isPristine: equals(context.data, context.pristineData),
        })),
        resetForm: assign((context) => ({ data: context.pristineData, error: null })),
        onSaveSuccess: assign((context) => ({
          pristineData: context.data,
          isPristine: true,
        })),
        setError: assign({
          error: (_, event) => extractErrorMessage((event as unknown as { data: unknown }).data),
        }),
        clearError: assign({ error: null }),
      },
      guards: {
        canSave: (context) =>
          !context.isPristine && Object.keys(context.validationErrors).length === 0,
        canVerify: (context) => Object.keys(context.validationErrors).length === 0,
      },
      // Placeholder services — always overridden by useCrowdSettings (and by
      // withConfig in unit tests). The reject bodies are a developer-facing
      // invariant: they only surface if a consumer forgets to wire a service.
      services: {
        load: () => Promise.resolve({ ...DEFAULT_CROWD_CONFIG }),
        save: () => Promise.reject(new Error('Save service not configured')),
        verifyConnection: () => Promise.reject(new Error('Verify service not configured')),
        clearCache: () => Promise.reject(new Error('Clear cache service not configured')),
      },
    }
  );
}
