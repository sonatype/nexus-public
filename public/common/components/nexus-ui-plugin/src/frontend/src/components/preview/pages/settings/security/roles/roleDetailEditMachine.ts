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
import type { Role, RoleFormData } from './types';

// Model for the Role page's inline-edit surface. Owns a pending-state buffer
// separate from the persisted role, dirty tracking against the last-known
// committed snapshot, and the save lifecycle. Effective Permissions and the
// Security Tree consume `pending` from the ViewModel so they recompute live
// from local edits without a server round-trip.
//
// Injected callback keeps the machine React-free (see architecture.md line 121):
//   - save: async mutation that persists pending state (throws on failure)
//   - onSaved: side effect invoked with the fresh role after a successful save

export interface RoleDetailEditContext {
  committed: RoleFormData;
  pending: RoleFormData;
  saveError: string | null;
}

export type RoleDetailEditEvent =
  | { type: 'UPDATE_NAME'; value: string }
  | { type: 'UPDATE_DESCRIPTION'; value: string }
  | { type: 'SET_PRIVILEGES'; value: string[] }
  | { type: 'SET_ROLES'; value: string[] }
  | { type: 'SAVE' }
  | { type: 'CANCEL' };

export interface RoleDetailEditMachineOptions {
  initialRole: Role;
  save: (pending: RoleFormData) => Promise<Role>;
  onSaved: (updated: Role) => void;
}

function roleToFormData(role: Role): RoleFormData {
  return {
    id: role.id,
    name: role.name ?? '',
    description: role.description ?? '',
    privileges: [...(role.privileges ?? [])],
    roles: [...(role.roles ?? [])],
    source: role.source,
    version: role.version,
  };
}

function arraysEqualAsSets(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false;
  const sa = new Set(a);
  return b.every((x) => sa.has(x));
}

// Returns true iff any editable field on `pending` differs from `committed`.
// Exported for the ViewModel and its tests so both can compute the same signal.
export function isPending(context: RoleDetailEditContext): boolean {
  const { committed, pending } = context;
  if ((committed.name ?? '') !== (pending.name ?? '')) return true;
  if ((committed.description ?? '') !== (pending.description ?? '')) return true;
  if (!arraysEqualAsSets(committed.privileges ?? [], pending.privileges ?? [])) return true;
  if (!arraysEqualAsSets(committed.roles ?? [], pending.roles ?? [])) return true;
  return false;
}

export function createRoleDetailEditMachine(options: RoleDetailEditMachineOptions) {
  const { initialRole, save, onSaved } = options;
  const initial = roleToFormData(initialRole);

  return createMachine<RoleDetailEditContext, RoleDetailEditEvent>(
    {
      id: 'roleDetailEdit',
      initial: 'viewing',
      context: {
        committed: initial,
        pending: initial,
        saveError: null,
      },
      states: {
        viewing: {
          on: {
            UPDATE_NAME: { actions: 'assignName' },
            UPDATE_DESCRIPTION: { actions: 'assignDescription' },
            SET_PRIVILEGES: { actions: 'assignPrivileges' },
            SET_ROLES: { actions: 'assignRoles' },
            CANCEL: { actions: 'resetPending' },
            SAVE: {
              target: 'saving',
              cond: 'hasPendingChanges',
              actions: 'clearSaveError',
            },
          },
        },
        saving: {
          invoke: {
            src: 'save',
            onDone: {
              target: 'viewing',
              actions: ['commitSaved', 'triggerOnSaved'],
            },
            onError: {
              target: 'viewing',
              actions: 'assignSaveError',
            },
          },
        },
      },
    },
    {
      services: {
        save: (context) => save(context.pending),
      },
      guards: {
        hasPendingChanges: (context) => isPending(context),
      },
      actions: {
        assignName: assign<RoleDetailEditContext, RoleDetailEditEvent>({
          pending: (context, event) => ({
            ...context.pending,
            name: (event as { value: string }).value,
          }),
        }),
        assignDescription: assign<RoleDetailEditContext, RoleDetailEditEvent>({
          pending: (context, event) => ({
            ...context.pending,
            description: (event as { value: string }).value,
          }),
        }),
        assignPrivileges: assign<RoleDetailEditContext, RoleDetailEditEvent>({
          pending: (context, event) => ({
            ...context.pending,
            privileges: [...(event as { value: string[] }).value],
          }),
        }),
        assignRoles: assign<RoleDetailEditContext, RoleDetailEditEvent>({
          pending: (context, event) => ({
            ...context.pending,
            roles: [...(event as { value: string[] }).value],
          }),
        }),
        resetPending: assign<RoleDetailEditContext, RoleDetailEditEvent>({
          pending: (context) => ({ ...context.committed }),
          saveError: (_context) => null,
        }),
        clearSaveError: assign<RoleDetailEditContext, RoleDetailEditEvent>({
          saveError: (_context) => null,
        }),
        commitSaved: assign<RoleDetailEditContext, RoleDetailEditEvent>({
          committed: (_context, event) => roleToFormData((event as { data: Role }).data),
          pending: (_context, event) => roleToFormData((event as { data: Role }).data),
          saveError: (_context) => null,
        }),
        assignSaveError: assign<RoleDetailEditContext, RoleDetailEditEvent>({
          saveError: (_context, event) => {
            const err = (event as { data?: unknown }).data;
            return (err as { message?: string } | null | undefined)?.message ?? 'Failed to save role';
          },
        }),
        triggerOnSaved: (_context, event) => {
          onSaved((event as { data: Role }).data);
        },
      },
    },
  );
}
