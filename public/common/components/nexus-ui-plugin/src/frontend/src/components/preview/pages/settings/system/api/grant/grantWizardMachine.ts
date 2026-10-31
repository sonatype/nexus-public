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

import { assign, createMachine } from 'xstate';

export interface GrantApplyResult {
  userKey: string;
  ok: boolean;
  message?: string;
}

export interface GrantWizardContext {
  step: 1 | 2 | 3 | 4;
  mode: 'existing' | 'create';
  existingRoleId: string | null;
  newRoleId: string;
  newRoleName: string;
  newRoleDescription: string;
  selectedUserKeys: string[];
  applyError: string | null;
  applyResults: GrantApplyResult[];
}

export type GrantWizardEvent =
  | { type: 'NEXT' }
  | { type: 'BACK' }
  | { type: 'SELECT_EXISTING'; roleId: string }
  | { type: 'SELECT_CREATE' }
  | { type: 'SET_NEW_ROLE_FIELD'; field: 'newRoleId' | 'newRoleName' | 'newRoleDescription'; value: string }
  | { type: 'TOGGLE_USER'; userKey: string }
  | { type: 'APPLY' }
  | { type: 'RESET' }
  | { type: 'DONE_ANOTHER' };

export const NEW_ROLE_ID_PATTERN = /^[a-zA-Z0-9\-_.]+$/;

export function initialGrantWizardContext(): GrantWizardContext {
  return {
    step: 1,
    mode: 'existing',
    existingRoleId: null,
    newRoleId: '',
    newRoleName: '',
    newRoleDescription: '',
    selectedUserKeys: [],
    applyError: null,
    applyResults: [],
  };
}

function canAdvanceFrom2(ctx: GrantWizardContext): boolean {
  if (ctx.mode === 'existing') {
    return Boolean(ctx.existingRoleId);
  }
  const id = ctx.newRoleId.trim();
  return NEW_ROLE_ID_PATTERN.test(id) && ctx.newRoleName.trim().length > 0;
}

function canAdvanceFrom3(ctx: GrantWizardContext): boolean {
  return ctx.selectedUserKeys.length > 0;
}

export function createGrantWizardMachine() {
  return createMachine<GrantWizardContext, GrantWizardEvent>(
    {
      id: 'grant-wizard',
      initial: 'step1',
      context: initialGrantWizardContext(),
      states: {
        step1: {
          on: {
            NEXT: {
              target: 'step2',
              actions: assign({ step: 2 }),
            },
          },
        },
        step2: {
          on: {
            BACK: {
              target: 'step1',
              actions: assign({ step: 1 }),
            },
            NEXT: {
              target: 'step3',
              cond: 'canAdvanceFrom2',
              actions: assign({ step: 3 }),
            },
            SELECT_EXISTING: {
              actions: assign((_, e) =>
                e.type === 'SELECT_EXISTING'
                  ? { mode: 'existing' as const, existingRoleId: e.roleId }
                  : {}
              ),
            },
            SELECT_CREATE: {
              actions: assign({ mode: 'create' as const, existingRoleId: null }),
            },
            SET_NEW_ROLE_FIELD: {
              actions: assign((_ctx, e) => {
                if (e.type !== 'SET_NEW_ROLE_FIELD') {
                  return {};
                }
                return { [e.field]: e.value };
              }),
            },
          },
        },
        step3: {
          on: {
            BACK: {
              target: 'step2',
              actions: assign({ step: 2 }),
            },
            NEXT: {
              target: 'step4',
              cond: 'canAdvanceFrom3',
              actions: assign({ step: 4, applyError: null }),
            },
            TOGGLE_USER: {
              actions: assign((ctx, e) => {
                if (e.type !== 'TOGGLE_USER') {
                  return {};
                }
                const next = new Set(ctx.selectedUserKeys);
                if (next.has(e.userKey)) {
                  next.delete(e.userKey);
                } else {
                  next.add(e.userKey);
                }
                return { selectedUserKeys: [...next].sort() };
              }),
            },
          },
        },
        step4: {
          on: {
            BACK: {
              target: 'step3',
              actions: assign({ step: 3, applyError: null }),
            },
            APPLY: 'applying',
          },
        },
        applying: {
          invoke: {
            id: 'applyGrant',
            src: 'applyGrant',
            onDone: {
              target: 'done',
              actions: assign({
                applyError: null,
                applyResults: (_ctx, e) => (e as { data: { results: GrantApplyResult[] } }).data.results,
              }),
            },
            onError: {
              target: 'step4',
              actions: assign({
                applyError: (_ctx, e) => {
                  const data = (e as { data: unknown }).data;
                  if (data instanceof Error) {
                    return data.message;
                  }
                  return typeof data === 'string' ? data : 'Apply failed';
                },
              }),
            },
          },
        },
        done: {
          on: {
            DONE_ANOTHER: {
              target: 'step3',
              actions: assign({
                step: 3,
                selectedUserKeys: [],
                applyResults: [],
                applyError: null,
              }),
            },
            RESET: {
              target: 'step1',
              actions: assign(() => initialGrantWizardContext()),
            },
          },
        },
      },
    },
    {
      guards: {
        canAdvanceFrom2: (ctx) => canAdvanceFrom2(ctx),
        canAdvanceFrom3: (ctx) => canAdvanceFrom3(ctx),
      },
    }
  );
}
