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

import { useCallback, useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';
import {
  createRoleDetailEditMachine,
  isPending,
} from './roleDetailEditMachine';
import type { Role, RoleFormData } from './types';

// ViewModel (thin projection) over roleDetailEditMachine. Owns no state of its
// own; projects state.context into a view-facing API and exposes semantic
// commands. The machine is instantiated once per role.id so opening a
// different role builds a fresh buffer.

export interface UseRoleDetailEditOptions {
  role: Role;
  save: (pending: RoleFormData) => Promise<Role>;
  onSaved: (updated: Role) => void;
}

export interface UseRoleDetailEditResult {
  pending: RoleFormData;
  committed: RoleFormData;
  isDirty: boolean;
  isSaving: boolean;
  saveError: string | null;
  changeName: (value: string) => void;
  changeDescription: (value: string) => void;
  applyPrivileges: (ids: string[]) => void;
  applyRoles: (ids: string[]) => void;
  save: () => void;
  cancel: () => void;
}

export function useRoleDetailEdit(options: UseRoleDetailEditOptions): UseRoleDetailEditResult {
  const { role, save, onSaved } = options;

  // Refs so late callback swaps reach the machine (useMachine caches machine identity from mount).
  const saveRef = useRef(save);
  saveRef.current = save;
  const onSavedRef = useRef(onSaved);
  onSavedRef.current = onSaved;

  const machine = useMemo(
    () =>
      createRoleDetailEditMachine({
        initialRole: role,
        save: (pending) => saveRef.current(pending),
        onSaved: (updated) => onSavedRef.current(updated),
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [role.id],
  );

  const [state, send] = useMachine(machine);

  const changeName = useCallback((value: string) => send({ type: 'UPDATE_NAME', value }), [send]);
  const changeDescription = useCallback(
    (value: string) => send({ type: 'UPDATE_DESCRIPTION', value }),
    [send],
  );
  const applyPrivileges = useCallback(
    (ids: string[]) => send({ type: 'SET_PRIVILEGES', value: ids }),
    [send],
  );
  const applyRoles = useCallback(
    (ids: string[]) => send({ type: 'SET_ROLES', value: ids }),
    [send],
  );
  const saveCmd = useCallback(() => send({ type: 'SAVE' }), [send]);
  const cancel = useCallback(() => send({ type: 'CANCEL' }), [send]);

  return {
    pending: state.context.pending,
    committed: state.context.committed,
    isDirty: isPending(state.context),
    isSaving: state.matches('saving'),
    saveError: state.context.saveError,
    changeName,
    changeDescription,
    applyPrivileges,
    applyRoles,
    save: saveCmd,
    cancel,
  };
}
