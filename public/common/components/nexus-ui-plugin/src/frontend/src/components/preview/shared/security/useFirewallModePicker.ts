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

import { useCallback, useMemo } from 'react';
import { useMachine } from '@xstate/react';
import {
  createFirewallModePickerContext,
  firewallModePickerMachine,
} from './firewallModePickerMachine';
import { isFirewallPickerModeAllowed } from './firewallModes';
import type { FirewallMode } from './useFirewallEnable';

export type FirewallModeLoadState = 'loading' | 'ready' | 'failed';

export interface FirewallModePickerViewModel {
  loadState: FirewallModeLoadState;
  currentMode: FirewallMode | null;
  /** Modes offered for this repository's format */
  availableModes: FirewallMode[];
  selectedMode: FirewallMode | null;
  select: (mode: FirewallMode) => void;
  retry: () => void;
  /** Confirm is blocked until the current mode is known and a different mode is chosen */
  canConfirm: boolean;
}

const OFFERED_MODES: FirewallMode[] = ['AUDIT', 'QUARANTINE', 'PCCS'];

export function useFirewallModePicker(repositoryName: string, format: string): FirewallModePickerViewModel {
  const machine = useMemo(
    () => firewallModePickerMachine.withContext(createFirewallModePickerContext(repositoryName)),
    [repositoryName],
  );
  const [state, send] = useMachine(machine);
  const { currentMode, pccsFormats, selectedMode } = state.context;

  const loadState: FirewallModeLoadState = state.matches('ready')
    ? 'ready'
    : state.matches('failed')
      ? 'failed'
      : 'loading';

  return {
    loadState,
    currentMode,
    availableModes: OFFERED_MODES.filter((mode) => isFirewallPickerModeAllowed(mode, format, pccsFormats)),
    selectedMode,
    select: useCallback((mode: FirewallMode) => send({ type: 'SELECT', mode }), [send]),
    retry: useCallback(() => send({ type: 'RETRY' }), [send]),
    canConfirm: loadState === 'ready' && selectedMode !== null && selectedMode !== currentMode,
  };
}
