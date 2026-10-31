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
import { UserTokenSettings, DEFAULT_USER_TOKEN_SETTINGS, RESET_CONFIRMATION_STRING } from './types';

export interface UserTokensMachineContext {
  data: UserTokenSettings;
  pristineData: UserTokenSettings;
  isPristine: boolean;
  validationErrors: { expirationDays?: string | null };
  loadError: string | null;
  saveError: string | null;
  resetError: string | null;
  resetConfirmationInput: string;
  resetConfirmationError: string | null;
}

export type UserTokensEvent =
  | { type: 'UPDATE'; field: keyof UserTokenSettings; value: boolean | number }
  | { type: 'SUBMIT' }
  | { type: 'DISCARD' }
  | { type: 'CONFIRM_SAVE' }
  | { type: 'CANCEL_SAVE' }
  | { type: 'REQUEST_RESET' }
  | { type: 'UPDATE_RESET_CONFIRMATION'; value: string }
  | { type: 'CONFIRM_RESET' }
  | { type: 'CANCEL_RESET' }
  | { type: 'CLEAR_ERROR' };

/**
 * Validate expirationDays. Ported from UserTokensPage.tsx expirationDaysError memo.
 */
export function validateExpirationDays(data: UserTokenSettings): string | null {
  if (!data.expirationEnabled) return null;
  if (!data.expirationDays) return 'Expiration days is required';
  if (data.expirationDays < 1 || data.expirationDays > 999) return 'Must be between 1 and 999 days';
  if (!Number.isInteger(Number(data.expirationDays))) return 'Must be a whole number';
  return null;
}

/**
 * The injected services throw Error(apiError.message), so this unwraps to the
 * API message; falls back to a string coercion for non-Error rejections.
 */
function extractMessage(errData: unknown): string {
  return errData instanceof Error ? errData.message : String(errData ?? 'Error');
}

/**
 * Create the User Tokens settings form machine.
 *
 * Bespoke createMachine (not createFormMachine): the pre-save expiration-change
 * warning and the typed-confirmation reset-all-tokens flow (with its own
 * destructive invoke) don't fit createFormMachine's fixed
 * SUBMIT -> validating -> saving path. Precedent: realmsFormMachine.
 *
 * load/save/resetAllTokens are declared as failing stubs here and provided by
 * the integration hook via useMachine's services option.
 */
export function createUserTokensMachine() {
  return createMachine<UserTokensMachineContext, UserTokensEvent>(
    {
      id: 'user-tokens-form',
      initial: 'loading',
      context: {
        data: DEFAULT_USER_TOKEN_SETTINGS,
        pristineData: DEFAULT_USER_TOKEN_SETTINGS,
        isPristine: true,
        validationErrors: {},
        loadError: null,
        saveError: null,
        resetError: null,
        resetConfirmationInput: '',
        resetConfirmationError: null,
      },
      states: {
        loading: {
          invoke: {
            src: 'load',
            onDone: { target: 'editing', actions: 'setData' },
            onError: { target: 'editing', actions: 'setLoadError' },
          },
        },
        editing: {
          on: {
            UPDATE: { actions: ['updateField', 'validate', 'computePristine'] },
            DISCARD: { actions: ['resetForm', 'clearError'] },
            CLEAR_ERROR: { actions: 'clearError' },
            REQUEST_RESET: { target: 'confirmingResetAllTokens' },
            SUBMIT: [
              { cond: 'hasValidationErrors', target: 'editing' },
              { cond: 'expirationChanged', target: 'confirmingSaveWithExpirationWarning' },
              { target: 'saving' },
            ],
          },
        },
        confirmingSaveWithExpirationWarning: {
          on: {
            CONFIRM_SAVE: 'saving',
            CANCEL_SAVE: 'editing',
          },
        },
        saving: {
          entry: 'clearError',
          invoke: {
            src: 'save',
            onDone: { target: 'editing', actions: 'onSaveSuccess' },
            onError: { target: 'editing', actions: 'setSaveError' },
          },
        },
        confirmingResetAllTokens: {
          entry: 'clearResetConfirmation',
          on: {
            UPDATE_RESET_CONFIRMATION: { actions: 'setResetConfirmation' },
            CONFIRM_RESET: [
              { cond: 'resetConfirmationValid', target: 'resettingAllTokens' },
              { actions: 'setResetConfirmationError' },
            ],
            CANCEL_RESET: { target: 'editing', actions: 'clearResetConfirmation' },
          },
        },
        resettingAllTokens: {
          entry: assign({ resetError: null }),
          invoke: {
            src: 'resetAllTokens',
            onDone: { target: 'editing', actions: 'clearResetConfirmation' },
            onError: { target: 'confirmingResetAllTokens', actions: 'setResetError' },
          },
        },
      },
    },
    {
      guards: {
        hasValidationErrors: (ctx) => Boolean(validateExpirationDays(ctx.data)),
        expirationChanged: (ctx) => ctx.data.expirationEnabled !== ctx.pristineData.expirationEnabled,
        resetConfirmationValid: (ctx) => ctx.resetConfirmationInput === RESET_CONFIRMATION_STRING,
      },
      actions: {
        setData: assign((_, event: any) => {
          const data = event.data as UserTokenSettings;
          return { data, pristineData: data, isPristine: true, loadError: null };
        }),
        setLoadError: assign((_, event: any) => ({ loadError: extractMessage(event.data) })),
        updateField: assign((ctx, event) => {
          if (event.type !== 'UPDATE') return {};
          const { field, value } = event;
          if (field === 'enabled' && value === false) {
            return {
              data: {
                ...ctx.data,
                enabled: false,
                protectContent: false,
                expirationEnabled: false,
                expirationDays: 30,
              },
            };
          }
          return { data: { ...ctx.data, [field]: value } };
        }),
        validate: assign((ctx) => ({
          validationErrors: { expirationDays: validateExpirationDays(ctx.data) },
        })),
        computePristine: assign((ctx) => ({ isPristine: equals(ctx.data, ctx.pristineData) })),
        resetForm: assign((ctx) => ({
          data: ctx.pristineData,
          isPristine: true,
          validationErrors: { expirationDays: validateExpirationDays(ctx.pristineData) },
        })),
        onSaveSuccess: assign((ctx) => ({ pristineData: ctx.data, isPristine: true, saveError: null })),
        setSaveError: assign((_, event: any) => ({ saveError: extractMessage(event.data) })),
        clearError: assign({ loadError: null, saveError: null, resetError: null }),
        setResetConfirmation: assign((_, event) => {
          if (event.type !== 'UPDATE_RESET_CONFIRMATION') return {};
          return { resetConfirmationInput: event.value, resetConfirmationError: null };
        }),
        setResetConfirmationError: assign({
          resetConfirmationError: `Please type "${RESET_CONFIRMATION_STRING}" to confirm`,
        }),
        clearResetConfirmation: assign({ resetConfirmationInput: '', resetConfirmationError: null }),
        setResetError: assign((_, event: any) => ({ resetError: extractMessage(event.data) })),
      },
      services: {
        load: () => Promise.reject(new Error('load not configured')),
        save: () => Promise.reject(new Error('save not configured')),
        resetAllTokens: () => Promise.reject(new Error('resetAllTokens not configured')),
      },
    }
  );
}
