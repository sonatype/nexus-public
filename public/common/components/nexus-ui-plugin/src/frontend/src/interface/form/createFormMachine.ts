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
import { equals, lensPath, set } from 'ramda';

import type { FormContext, FormEvent } from './types';
import { hasValidationErrors, extractErrorMessage, toPathArray } from './utils';

/**
 * Configuration for creating a form machine.
 * Mirrors XState's config structure with sensible defaults for forms.
 */
export interface FormMachineConfig<TData> {
  /** Unique identifier for this machine */
  id: string;

  /**
   * Initial context. Will be merged with default form context.
   * At minimum, provide `data` with your form's initial values.
   */
  context: {
    data: TData;
  } & Partial<Omit<FormContext<TData>, 'data'>>;

  /**
   * Actions object. Must include `validate` action using XState's assign().
   * The validate action should update `validationErrors` in context.
   * Other actions will be merged with defaults.
   *
   * @example
   * ```ts
   * import { assign } from 'xstate';
   *
   * actions: {
   *   validate: assign((ctx) => ({
   *     validationErrors: {
   *       name: !ctx.data.name?.trim() ? 'Name is required' : null,
   *     },
   *   })),
   * }
   * ```
   */
  actions: {
    /** Validation action - must use assign() to update validationErrors */
    validate: unknown;
    [key: string]: unknown;
  };

  /**
   * Services for async operations.
   */
  services?: {
    /** Save the form data. Throws error if not implemented. */
    save?: (context: FormContext<TData>) => Promise<unknown>;
    /** Load initial data. If not provided, starts in 'editing' state with initial context. */
    load?: (context: FormContext<TData>) => Promise<{ data: TData } & Record<string, unknown>>;
    /** Delete the entity. If not provided, delete functionality is disabled. Throws error if not implemented but called. */
    delete?: (context: FormContext<TData>) => Promise<unknown>;
  };

  /**
   * Optional custom guards. Will be merged with defaults.
   */
  guards?: Record<string, (context: FormContext<TData>) => boolean>;

  /**
   * Optional custom event handlers for the editing state.
   * Allows adding domain-specific events beyond the standard form events.
   */
  on?: Record<string, { actions: string | string[] } | Array<{ target: string; cond?: string; actions: string | string[] }>>;

  /**
   * When true, transitions directly from saving → editing, bypassing the saved state.
   * Use this for forms that should remain editable after save (no post-save navigation).
   * Default: false (saves to 'saved' state for post-save navigation flows).
   */
  stayEditableAfterSave?: boolean;

  /**
   * Optional configuration for type variant sub-states within the editing state.
   * When provided, the editing state becomes a compound state with sub-states.
   *
   * This enables model-based testing where each variant is a distinct machine state,
   * and the machine can declare field metadata per variant.
   *
   * @example
   * ```ts
   * editingConfig: {
   *   defaultState: 'application',
   *   typeField: 'type',
   *   states: {
   *     application: { meta: { fields: ['domain', 'actions'] } },
   *     wildcard: { meta: { fields: ['pattern'] } },
   *   },
   * }
   * ```
   */
  editingConfig?: {
    /** Default sub-state name (used for create mode / initial state) */
    defaultState: string;
    /** Field path in data that determines the active sub-state (default: 'type') */
    typeField?: string;
    /** Sub-state definitions. Keys are state names, values contain metadata. */
    states: Record<string, { meta?: Record<string, unknown> }>;
  };

  /**
   * When true, the saved state transitions back to editing after a 0ms delay,
   * allowing the form to be edited again after a successful save (settings pages).
   * When false (default), saved is a terminal state — consumers use isComplete to
   * detect save completion and navigate away (creation/edit dialogs).
   */
  resetAfterSave?: boolean;
}

/**
 * Create default context values for a form machine
 */
function getDefaultContext<TData>(data: TData): FormContext<TData> {
  return {
    data,
    pristineData: data,
    isPristine: true,
    touched: {},
    validationErrors: {},
    saveError: null,
    loadError: null,
    deleteError: null,
  };
}

/**
 * Create a form machine with standard states and transitions.
 *
 * The machine handles common form patterns:
 * - Loading initial data
 * - Field updates with validation
 * - Pristine state tracking
 * - Save with error handling
 * - Delete with confirmation
 * - Reset to pristine state
 *
 * @example
 * ```ts
 * const machine = createFormMachine({
 *   id: 'user-form',
 *   context: {
 *     data: { name: '', email: '' },
 *   },
 *   actions: {
 *     validate: (ctx) => ({
 *       name: !ctx.data.name?.trim() ? 'Name is required' : null,
 *       email: !ctx.data.email?.includes('@') ? 'Invalid email' : null,
 *     }),
 *   },
 *   services: {
 *     save: async (ctx) => api.saveUser(ctx.data),
 *     delete: async (ctx) => api.deleteUser(ctx.data.id),
 *   },
 * });
 * ```
 */
export function createFormMachine<TData>(config: FormMachineConfig<TData>) {
  const { id, context: userContext, actions: userActions, services = {}, guards: userGuards, on: customEvents, editingConfig, stayEditableAfterSave = false, resetAfterSave = false } = config;

  const hasLoad = Boolean(services.load);
  const hasDelete = Boolean(services.delete);

  // Merge user context with defaults
  const initialContext: FormContext<TData> = {
    ...getDefaultContext(userContext.data),
    ...userContext,
    pristineData: userContext.pristineData ?? userContext.data,
  };

  return createMachine<FormContext<TData>, FormEvent>(
    {
      id,
      initial: hasLoad ? 'loading' : 'editing',
      context: initialContext,

      states: {
        // ============================================
        // Loading State (optional)
        // ============================================
        ...(hasLoad && {
          loading: {
            invoke: {
              src: 'load',
              onDone: {
                target: 'editing',
                actions: ['clearLoadError', 'setData', 'validate'],
              },
              onError: {
                target: 'loadError',
                actions: 'setLoadError',
              },
            },
          },

          loadError: {
            on: {
              RETRY: 'loading',
            },
          },
        }),

        // ============================================
        // Editing State (main state)
        // When editingConfig is provided, becomes compound
        // with sub-states for each type variant.
        // ============================================
        editing: {
          // When editingConfig is provided, make this a compound state
          // with a 'determining' initial state that routes to the correct sub-state
          ...(editingConfig ? {
            initial: 'determining',
            states: {
              // Transient state that routes to the correct sub-state based on data
              determining: {
                always: [
                  // Generate transitions for each sub-state
                  ...Object.keys(editingConfig.states).map((stateName) => ({
                    target: stateName,
                    cond: `isType_${stateName}`,
                  })),
                  // Default fallback
                  { target: editingConfig.defaultState },
                ],
              },
              // Spread the user-defined sub-states
              ...editingConfig.states,
            },
          } : {}),
          entry: ['validate', 'computePristine'],
          on: {
            UPDATE: {
              actions: ['updateField', 'validate', 'computePristine'],
            },
            BLUR: {
              actions: 'markTouched',
            },
            SUBMIT: {
              target: 'validating',
            },
            RESET: {
              actions: ['resetForm', 'validate', 'computePristine'],
            },
            CANCEL: [
              // If pristine, go directly to cancelled
              { target: 'cancelled', cond: 'isPristine' },
              // Otherwise, show confirmation
              { target: 'confirmingCancel' },
            ],
            ...(hasDelete && {
              DELETE: {
                target: 'confirmingDelete',
                cond: 'canDelete',
              },
            }),
            // Custom events from config
            ...customEvents,
          },
        },

        // ============================================
        // Validation State (transient)
        // ============================================
        validating: {
          entry: ['validate', 'markAllTouched'],
          always: [
            { target: 'editing', cond: 'hasValidationErrors' },
            { target: 'saving' },
          ],
        },

        // ============================================
        // Saving State
        // ============================================
        saving: {
          entry: 'clearSaveError',
          invoke: {
            src: 'save',
            onDone: {
              target: stayEditableAfterSave ? 'editing' : 'saved',
              actions: 'onSaveSuccess',
            },
            onError: {
              target: 'editing',
              actions: 'setSaveError',
            },
          },
        },

        saved: {
          ...(resetAfterSave ? { after: { 0: 'editing' } } : { type: 'final' as const }),
        },

        // ============================================
        // Cancel Confirmation State
        // ============================================
        confirmingCancel: {
          on: {
            CONFIRM_CANCEL: 'cancelled',
            STAY: 'editing',
          },
        },

        cancelled: {
          type: 'final',
          entry: 'onCancel',
        },

        // ============================================
        // Delete States (optional)
        // ============================================
        ...(hasDelete && {
          confirmingDelete: {
            entry: 'clearDeleteError',
            on: {
              CONFIRM_DELETE: 'deleting',
              CANCEL_DELETE: 'editing',
            },
          },

          deleting: {
            invoke: {
              src: 'delete',
              onDone: {
                target: 'deleted',
              },
              onError: {
                target: 'editing',
                actions: 'setDeleteError',
              },
            },
          },

          deleted: {
            type: 'final',
          },
        }),
      },
    },
    {
      // ============================================
      // Actions
      // ============================================
      actions: {
        // Note: XState invoke onDone/onError events have shape { type: 'done.invoke...', data: ... }
        // We use `as unknown as` to handle the type mismatch between FormEvent and invoke events
        setData: assign((_, event) => {
          // The invoke event wraps the service result in event.data
          const invokeEvent = event as unknown as { data: { data: TData } & Record<string, unknown> };
          const payload = invokeEvent.data; // This is what the load service returned
          const { data, ...rest } = payload;
          return {
            data,
            pristineData: data,
            ...rest, // Includes privilege, privilegeTypes, etc.
          };
        }),

        setLoadError: assign({
          loadError: (_, event) => {
            const invokeEvent = event as unknown as { data: unknown };
            return extractErrorMessage(invokeEvent.data);
          },
        }),

        clearLoadError: assign({ loadError: null }),

        setSaveError: assign({
          saveError: (_, event) => {
            const invokeEvent = event as unknown as { data: unknown };
            return extractErrorMessage(invokeEvent.data);
          },
        }),

        clearSaveError: assign({ saveError: null }),

        setDeleteError: assign({
          deleteError: (_, event) => {
            const invokeEvent = event as unknown as { data: unknown };
            return extractErrorMessage(invokeEvent.data);
          },
        }),

        clearDeleteError: assign({ deleteError: null }),

        updateField: assign((context, event) => {
          const updateEvent = event as { type: 'UPDATE'; name: string; value: unknown };
          const pathArray = toPathArray(updateEvent.name);
          const newData = set(lensPath(pathArray), updateEvent.value, context.data);

          return {
            data: newData,
            touched: set(lensPath(pathArray), true, context.touched),
          };
        }),

        markTouched: assign((context, event) => {
          const blurEvent = event as { type: 'BLUR'; name: string };
          const pathArray = toPathArray(blurEvent.name);
          return {
            touched: set(lensPath(pathArray), true, context.touched),
          };
        }),

        computePristine: assign((context) => ({
          isPristine: equals(context.data, context.pristineData),
        })),

        resetForm: assign((context) => ({
          data: context.pristineData,
          touched: {},
          validationErrors: {},
          saveError: null,
        })),

        markAllTouched: assign((context) => {
          const allTouched: Record<string, boolean> = {};
          Object.keys(context.validationErrors).forEach((key) => {
            if (context.validationErrors[key]) {
              allTouched[key] = true;
            }
          });
          return { touched: { ...context.touched, ...allTouched } };
        }),

        onSaveSuccess: assign((context) => ({
          pristineData: context.data,
          isPristine: true,
          touched: {},
        })),

        // Placeholder - override via useMachine options to provide actual implementation
        onCancel: () => {},

        // Allow user actions to override defaults (cast to any for XState compatibility)
        ...(userActions as Record<string, unknown>),
      },

      // ============================================
      // Guards
      // ============================================
      guards: {
        canSave: (context) => {
          return !(context.isPristine || hasValidationErrors(context.validationErrors));
        },

        hasValidationErrors: (context) => {
          return hasValidationErrors(context.validationErrors);
        },

        isPristine: (context) => context.isPristine,

        canDelete: () => hasDelete,

        // Auto-generated guards for editingConfig sub-state routing
        ...(editingConfig ? Object.keys(editingConfig.states).reduce((guards, stateName) => {
          const typeField = editingConfig.typeField || 'type';
          guards[`isType_${stateName}`] = (context: FormContext<TData>) => {
            const data = context.data as Record<string, unknown>;
            return data[typeField] === stateName;
          };
          return guards;
        }, {} as Record<string, (context: FormContext<TData>) => boolean>) : {}),

        // Allow user guards to override defaults
        ...userGuards,
      },

      // ============================================
      // Services
      // ============================================
      services: {
        load: services.load ?? (() => Promise.resolve({ data: initialContext.data })),
        save: services.save ?? (() => Promise.reject(new Error('Save service not configured'))),
        delete: services.delete ?? (() => Promise.reject(new Error('Delete service not configured'))),
      },
    }
  );
}
