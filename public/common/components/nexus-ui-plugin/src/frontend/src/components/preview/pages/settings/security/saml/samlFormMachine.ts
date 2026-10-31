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

import { createMachine, assign, type StateMachine } from 'xstate';
import { equals } from 'ramda';
import { type FormContext, type FormEvent, type ValidationErrors, extractErrorMessage } from '../../../../../../interface/form';
import { SamlConfiguration } from './types';

/**
 * Form context extended with whether a SAML configuration currently exists on
 * the server (drives the Configured/Not-Configured badge and delete button).
 */
export interface SamlFormContext extends FormContext<SamlConfiguration> {
  isConfigured: boolean;
}

/** SAML-specific event (banner dismissal) on top of the standard form events. */
type SamlEvent = FormEvent | { type: 'CLEAR_ERROR' };

export const DEFAULT_CONFIG: SamlConfiguration = {
  entityId: '',
  idpMetadata: '',
  usernameAttribute: 'username',
  firstNameAttribute: 'firstName',
  lastNameAttribute: 'lastName',
  emailAttribute: 'email',
  groupsAttribute: 'groups',
  // Tri-state: null = Default (backend decides), true = Force enabled, false = Force disabled
  validateResponseSignature: null,
  validateAssertionSignature: null,
};

/**
 * Convert tri-state signature value to API format (null | boolean). Handles the
 * string values from the select ('default'/'true'/'false') and boolean values
 * from API/state.
 */
export function parseSignatureValidation(value: string | boolean | null | undefined): boolean | null {
  if (value === 'default' || value === null || value === undefined) return null;
  return value === 'true' || value === true;
}

/**
 * Validate SAML form data. Mirrors the legacy validation messages.
 */
export function validateSaml(data: SamlConfiguration): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.idpMetadata || data.idpMetadata.trim() === '') {
    errors.idpMetadata = 'Identity Provider Metadata is required';
  }

  if (data.entityId && data.entityId.trim() !== '') {
    const URI_REGEX = /^[a-zA-Z][a-zA-Z0-9+\-.]*:.+$/;
    if (!URI_REGEX.test(data.entityId.trim())) {
      errors.entityId = 'Entity ID must be a URI';
    }
  }

  if (!data.usernameAttribute || data.usernameAttribute.trim() === '') {
    errors.usernameAttribute = 'Username Attribute is required';
  }

  return errors;
}

/**
 * Build the API payload from form data: trim attribute strings and convert the
 * tri-state signature fields to null | boolean (parity with the legacy page).
 */
export function toSamlPayload(data: SamlConfiguration): SamlConfiguration {
  return {
    ...data,
    entityId: data.entityId?.trim() || '',
    usernameAttribute: data.usernameAttribute?.trim() || '',
    firstNameAttribute: data.firstNameAttribute?.trim() || '',
    lastNameAttribute: data.lastNameAttribute?.trim() || '',
    emailAttribute: data.emailAttribute?.trim() || '',
    groupsAttribute: data.groupsAttribute?.trim() || '',
    validateResponseSignature: parseSignatureValidation(data.validateResponseSignature),
    validateAssertionSignature: parseSignatureValidation(data.validateAssertionSignature),
  };
}

function getDefaultContext(): SamlFormContext {
  return {
    data: { ...DEFAULT_CONFIG },
    pristineData: { ...DEFAULT_CONFIG },
    isPristine: true,
    touched: {},
    validationErrors: {},
    loadError: null,
    saveError: null,
    deleteError: null,
    isConfigured: false,
  };
}

/**
 * SAML settings form machine.
 *
 * A bespoke machine (like the Crowd settings machine) rather than
 * createFormMachine, because SAML's delete flow must keep the page open and
 * reset in place, and a failed delete must return to the confirmation dialog
 * with the error visible. Modeling those transitions directly is fully
 * supported XState; the previous approach mutated createFormMachine's built
 * config with mergeDeepRight, which relied on its internal shape.
 *
 * The context/state/event shape matches createFormMachine so the standard
 * `useForm` hook (field helpers, dirty tracking, status flags) still drives it.
 *
 * Notable transitions:
 * - `loading.onError` -> `editing` (NOT a dead loadError state) with the error
 *   set, so a real load failure is surfaced while the form stays editable.
 * - `saving` -> `editing` (stay editable after save) and flip isConfigured.
 * - `deleting.onDone` -> `editing` resetting to the fresh default returned by
 *   the delete service and flipping isConfigured false (page stays open).
 * - `deleting.onError` -> `confirmingDelete` with deleteError set, so the dialog
 *   stays open showing the error and the delete is retriable.
 *
 * Load/save/delete services are injected by the integration hook.
 */
export function createSamlFormMachine(): StateMachine<SamlFormContext, any, FormEvent> {
  const machine = createMachine<SamlFormContext, SamlEvent>(
    {
      id: 'saml-form',
      initial: 'loading',
      context: getDefaultContext(),
      states: {
        loading: {
          invoke: {
            src: 'load',
            onDone: { target: 'editing', actions: ['setData', 'validate', 'computePristine'] },
            // Surface a real load failure but keep the form editable.
            onError: { target: 'editing', actions: 'setLoadError' },
          },
        },

        editing: {
          on: {
            UPDATE: { actions: ['updateField', 'validate', 'computePristine'] },
            BLUR: { actions: 'markTouched' },
            SUBMIT: { target: 'validating' },
            RESET: { actions: ['resetForm', 'validate', 'computePristine'] },
            DELETE: { target: 'confirmingDelete', actions: 'clearDeleteError' },
            CLEAR_ERROR: { actions: 'clearErrors' },
          },
        },

        validating: {
          entry: ['validate', 'markAllTouched'],
          always: [{ target: 'editing', cond: 'hasValidationErrors' }, { target: 'saving' }],
        },

        saving: {
          entry: 'clearSaveError',
          invoke: {
            src: 'save',
            onDone: { target: 'editing', actions: 'onSaveSuccess' },
            onError: { target: 'editing', actions: 'setSaveError' },
          },
        },

        confirmingDelete: {
          on: {
            CONFIRM_DELETE: 'deleting',
            CANCEL_DELETE: { target: 'editing', actions: 'clearDeleteError' },
          },
        },

        deleting: {
          invoke: {
            src: 'delete',
            onDone: { target: 'editing', actions: 'resetAfterDelete' },
            // Keep the dialog open with the error shown; delete is retriable.
            onError: { target: 'confirmingDelete', actions: 'setDeleteError' },
          },
        },
      },
    },
    {
      actions: {
        setData: assign((_, event) => {
          const payload = (event as unknown as { data: { data: SamlConfiguration; isConfigured: boolean } }).data;
          return {
            data: payload.data,
            pristineData: payload.data,
            isConfigured: payload.isConfigured,
            loadError: null,
          };
        }),
        setLoadError: assign({
          loadError: (_, event) => extractErrorMessage((event as unknown as { data: unknown }).data),
        }),
        updateField: assign((context, event) => {
          const e = event as Extract<FormEvent, { type: 'UPDATE' }>;
          return {
            data: { ...context.data, [e.name]: e.value },
            touched: { ...context.touched, [e.name]: true },
          };
        }),
        markTouched: assign((context, event) => {
          const e = event as Extract<FormEvent, { type: 'BLUR' }>;
          return { touched: { ...context.touched, [e.name]: true } };
        }),
        markAllTouched: assign((context) => {
          const touched = { ...context.touched };
          Object.keys(context.validationErrors).forEach((key) => {
            if (context.validationErrors[key]) touched[key] = true;
          });
          return { touched };
        }),
        validate: assign((context) => ({ validationErrors: validateSaml(context.data) })),
        computePristine: assign((context) => ({ isPristine: equals(context.data, context.pristineData) })),
        resetForm: assign((context) => ({
          data: context.pristineData,
          touched: {},
          validationErrors: {},
          saveError: null,
        })),
        onSaveSuccess: assign((context) => ({
          pristineData: context.data,
          isPristine: true,
          touched: {},
          isConfigured: true,
          saveError: null,
        })),
        setSaveError: assign({
          saveError: (_, event) => extractErrorMessage((event as unknown as { data: unknown }).data),
        }),
        clearSaveError: assign({ saveError: null }),
        resetAfterDelete: assign((_, event) => {
          const data = (event as unknown as { data: SamlConfiguration }).data;
          return {
            data,
            pristineData: data,
            isPristine: true,
            touched: {},
            validationErrors: {},
            isConfigured: false,
            deleteError: null,
          };
        }),
        setDeleteError: assign({
          deleteError: (_, event) => extractErrorMessage((event as unknown as { data: unknown }).data),
        }),
        clearDeleteError: assign({ deleteError: null }),
        clearErrors: assign({ loadError: null, saveError: null, deleteError: null }),
      },
      guards: {
        hasValidationErrors: (context) => Object.values(context.validationErrors).some(Boolean),
      },
      // Placeholder services; real implementations are injected by the hook.
      services: {
        load: () => Promise.resolve({ data: { ...DEFAULT_CONFIG }, isConfigured: false }),
        save: () => Promise.resolve(),
        delete: () => Promise.resolve({ ...DEFAULT_CONFIG }),
      },
    }
  );

  // The machine is FormContext-shaped so useForm can drive it; the extra
  // CLEAR_ERROR event is not part of the base FormEvent union.
  return machine as unknown as StateMachine<SamlFormContext, any, FormEvent>;
}
