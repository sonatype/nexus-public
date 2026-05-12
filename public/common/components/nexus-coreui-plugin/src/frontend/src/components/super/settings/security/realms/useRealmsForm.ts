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

import { useMemo, useCallback, useEffect } from 'react';
import { useMachine } from '@xstate/react';
import { useToast } from '../../../../shared';
import { createRealmsFormMachine, RealmsFormContext } from './realmsFormMachine';
import { useRealmsApi } from './useRealmsApi';
import { Realm } from './types';
import {
  clearDirtyState,
} from '../../../../shared/hooks/useUnsavedChangesWarning';

const FORM_ID = 'realms-form';

export interface UseRealmsFormReturn {
  /** All available realms */
  availableRealms: Realm[];
  /** Inactive realms (available but not active) */
  inactiveRealms: Realm[];
  /** Currently active realms in order */
  activeRealms: Realm[];
  /** Whether the form has unsaved changes */
  isPristine: boolean;
  /** Whether data is loading */
  isLoading: boolean;
  /** Whether save is in progress */
  isSaving: boolean;
  /** Whether there was a load error */
  hasLoadError: boolean;
  /** Validation error */
  validationError: string | null;
  /** Save error */
  saveError: string | null;
  /** Load error */
  loadError: string | null;

  /** Add a realm to the active list */
  addRealm: (realm: Realm) => void;
  /** Remove a realm from the active list */
  removeRealm: (realmId: string) => void;
  /** Set the full active realms list (for drag-and-drop reorder) */
  reorder: (activeRealms: Realm[]) => void;
  /** Move a realm up in the active list */
  moveUp: (realmId: string) => void;
  /** Move a realm down in the active list */
  moveDown: (realmId: string) => void;
  /** Save the current active realms */
  submit: () => void;
  /** Discard changes and revert to pristine state */
  discard: () => void;
  /** Retry loading after an error */
  retry: () => void;
}

/**
 * Custom hook for managing Realms form state.
 *
 * Connects the realms XState machine with the REST API hooks,
 * provides dirty state tracking and unsaved changes warnings.
 */
export function useRealmsForm(): UseRealmsFormReturn {
  const toast = useToast();
  const api = useRealmsApi();

  const machine = useMemo(() => createRealmsFormMachine(), []);

  const [state, send] = useMachine(machine, {
    services: {
      load: async () => {
        const [availableRealms, activeRealmIds] = await Promise.all([
          api.fetchAvailableRealms(),
          api.fetchActiveRealmIds(),
        ]);
        return { availableRealms, activeRealmIds };
      },
      save: async (ctx: RealmsFormContext) => {
        try {
          const realmIds = ctx.activeRealms.map((r) => r.id);
          await api.updateActiveRealms(realmIds);
          toast.success('Realms configuration saved');
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Failed to save realms');
          throw err;
        }
      },
    },
  });

  const { context } = state;

  useEffect(() => {
    return () => {
      clearDirtyState(FORM_ID);
    };
  }, []);

  // Compute inactive realms (available but not currently active)
  const inactiveRealms = useMemo(() => {
    const activeIds = new Set(context.activeRealms.map((r) => r.id));
    return context.availableRealms.filter((r) => !activeIds.has(r.id));
  }, [context.availableRealms, context.activeRealms]);

  // Actions
  const addRealm = useCallback(
    (realm: Realm) => send({ type: 'ADD_REALM', realm }),
    [send]
  );

  const removeRealm = useCallback(
    (realmId: string) => send({ type: 'REMOVE_REALM', realmId }),
    [send]
  );

  const reorder = useCallback(
    (activeRealms: Realm[]) => send({ type: 'REORDER', activeRealms }),
    [send]
  );

  const moveUp = useCallback(
    (realmId: string) => send({ type: 'MOVE_UP', realmId }),
    [send]
  );

  const moveDown = useCallback(
    (realmId: string) => send({ type: 'MOVE_DOWN', realmId }),
    [send]
  );

  const submit = useCallback(() => send({ type: 'SUBMIT' }), [send]);
  const discard = useCallback(() => send({ type: 'DISCARD' }), [send]);
  const retry = useCallback(() => send({ type: 'RETRY' }), [send]);

  return {
    availableRealms: context.availableRealms,
    inactiveRealms,
    activeRealms: context.activeRealms,
    isPristine: context.isPristine,
    isLoading: state.matches('loading'),
    isSaving: state.matches('saving'),
    hasLoadError: state.matches('loadError'),
    validationError: context.validationError,
    saveError: context.saveError,
    loadError: context.loadError,
    addRealm,
    removeRealm,
    reorder,
    moveUp,
    moveDown,
    submit,
    discard,
    retry,
  };
}
