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

import { Realm } from './types';

/**
 * Context for the realms form machine.
 *
 * Unlike traditional form machines, this manages two lists:
 * - availableRealms: all realms the system knows about
 * - activeRealms: the ordered list of currently active realms
 */
export interface RealmsFormContext {
  /** All available realms from the server */
  availableRealms: Realm[];
  /** Currently active (selected) realms in order */
  activeRealms: Realm[];
  /** Pristine active realms for dirty state comparison */
  pristineActiveRealms: Realm[];
  /** Whether the form has unsaved changes */
  isPristine: boolean;
  /** Validation error message */
  validationError: string | null;
  /** Save error message */
  saveError: string | null;
  /** Load error message */
  loadError: string | null;
}

/**
 * Events for the realms form machine
 */
export type RealmsFormEvent =
  | { type: 'RETRY' }
  | { type: 'SUBMIT' }
  | { type: 'DISCARD' }
  | { type: 'ADD_REALM'; realm: Realm }
  | { type: 'REMOVE_REALM'; realmId: string }
  | { type: 'REORDER'; activeRealms: Realm[] }
  | { type: 'MOVE_UP'; realmId: string }
  | { type: 'MOVE_DOWN'; realmId: string };

/**
 * Validate that at least one realm is active
 */
function validateRealms(activeRealms: Realm[]): string | null {
  if (activeRealms.length === 0) {
    return 'At least one active realm is required';
  }
  return null;
}

/**
 * Create the realms form machine.
 *
 * This is a custom machine (not using createFormMachine) because realms
 * is a reorderable list rather than a traditional key-value form.
 *
 * States: loading -> editing <-> saving
 * The machine manages: load available/active realms, add/remove/reorder, save.
 */
export function createRealmsFormMachine() {
  return createMachine<RealmsFormContext, RealmsFormEvent>(
    {
      id: 'realms-form',
      initial: 'loading',
      context: {
        availableRealms: [],
        activeRealms: [],
        pristineActiveRealms: [],
        isPristine: true,
        validationError: null,
        saveError: null,
        loadError: null,
      },
      states: {
        loading: {
          invoke: {
            src: 'load',
            onDone: {
              target: 'editing',
              actions: 'setLoadedData',
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

        editing: {
          on: {
            ADD_REALM: {
              actions: ['addRealm', 'validate', 'computePristine'],
            },
            REMOVE_REALM: {
              actions: ['removeRealm', 'validate', 'computePristine'],
            },
            REORDER: {
              actions: ['reorder', 'validate', 'computePristine'],
            },
            MOVE_UP: {
              actions: ['moveUp', 'validate', 'computePristine'],
            },
            MOVE_DOWN: {
              actions: ['moveDown', 'validate', 'computePristine'],
            },
            SUBMIT: [
              { target: 'editing', cond: 'hasValidationError', actions: 'validate' },
              { target: 'saving' },
            ],
            DISCARD: {
              actions: ['discard', 'validate', 'computePristine'],
            },
          },
        },

        saving: {
          entry: 'clearSaveError',
          invoke: {
            src: 'save',
            onDone: {
              target: 'editing',
              actions: 'setSaved',
            },
            onError: {
              target: 'editing',
              actions: 'setSaveError',
            },
          },
        },
      },
    },
    {
      actions: {
        setLoadedData: assign((_, event: any) => {
          const { availableRealms, activeRealmIds } = event.data;
          const activeRealms = (activeRealmIds as string[])
            .map((id: string) => (availableRealms as Realm[]).find((r) => r.id === id))
            .filter((r): r is Realm => r !== undefined);

          return {
            availableRealms,
            activeRealms,
            pristineActiveRealms: activeRealms,
            isPristine: true,
            validationError: null,
            loadError: null,
          };
        }),

        setLoadError: assign((_, event: any) => ({
          loadError: event.data?.message || 'Failed to load realms',
        })),

        setSaveError: assign((_, event: any) => ({
          saveError: event.data?.message || 'Failed to save realms',
        })),

        clearSaveError: assign({ saveError: null }),

        setSaved: assign((ctx) => ({
          pristineActiveRealms: ctx.activeRealms,
          isPristine: true,
          saveError: null,
        })),

        addRealm: assign((ctx, event) => {
          if (event.type !== 'ADD_REALM') return {};
          const { realm } = event;
          if (ctx.activeRealms.some((r) => r.id === realm.id)) return {};
          return {
            activeRealms: [...ctx.activeRealms, realm],
          };
        }),

        removeRealm: assign((ctx, event) => {
          if (event.type !== 'REMOVE_REALM') return {};
          return {
            activeRealms: ctx.activeRealms.filter((r) => r.id !== event.realmId),
          };
        }),

        reorder: assign((_, event) => {
          if (event.type !== 'REORDER') return {};
          return {
            activeRealms: event.activeRealms,
          };
        }),

        moveUp: assign((ctx, event) => {
          if (event.type !== 'MOVE_UP') return {};
          const index = ctx.activeRealms.findIndex((r) => r.id === event.realmId);
          if (index <= 0) return {};

          const newRealms = [...ctx.activeRealms];
          [newRealms[index - 1], newRealms[index]] = [newRealms[index], newRealms[index - 1]];
          return { activeRealms: newRealms };
        }),

        moveDown: assign((ctx, event) => {
          if (event.type !== 'MOVE_DOWN') return {};
          const index = ctx.activeRealms.findIndex((r) => r.id === event.realmId);
          if (index < 0 || index >= ctx.activeRealms.length - 1) return {};

          const newRealms = [...ctx.activeRealms];
          [newRealms[index], newRealms[index + 1]] = [newRealms[index + 1], newRealms[index]];
          return { activeRealms: newRealms };
        }),

        validate: assign((ctx) => ({
          validationError: validateRealms(ctx.activeRealms),
        })),

        computePristine: assign((ctx) => ({
          isPristine: equals(
            ctx.activeRealms.map((r) => r.id),
            ctx.pristineActiveRealms.map((r) => r.id)
          ),
        })),

        discard: assign((ctx) => ({
          activeRealms: ctx.pristineActiveRealms,
          saveError: null,
          validationError: null,
        })),
      },

      guards: {
        hasValidationError: (ctx) => validateRealms(ctx.activeRealms) !== null,
      },

      services: {
        // load and save are overridden via useMachine/useRealmsForm
        load: () => Promise.reject(new Error('Load service not configured')),
        save: () => Promise.reject(new Error('Save service not configured')),
      },
    }
  );
}

export { validateRealms };
