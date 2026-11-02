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

import { useCallback } from 'react';
import { useMachine } from '@xstate/react';
import { createServerRestartMachine } from './serverRestartMachine';

/* Immutable machine config: build once at module load and share. useMachine spins a fresh service per hook instance. */
const serverRestartMachine = createServerRestartMachine();

/*
 * ViewModel (thin projection) over serverRestartMachine. Owns no state of its own:
 * the machine holds the window subscription, the latch, and the reload side effect.
 * This hook only projects machine state into view-facing flags and exposes a single
 * semantic command (confirmReload) that the View fires from its reload button.
 */

export interface UseServerRestartResult {
  /** Whether the restart modal should be shown */
  isRestarted: boolean;
  /** Whether the reload is in progress (button disabled state) */
  isReloading: boolean;
  /** Confirm and trigger the application reload */
  confirmReload: () => void;
}

export function useServerRestart(): UseServerRestartResult {
  const [state, send] = useMachine(serverRestartMachine);

  const confirmReload = useCallback(() => send({ type: 'RELOAD' }), [send]);

  return {
    isRestarted: state.matches('prompting') || state.matches('reloading'),
    isReloading: state.matches('reloading'),
    confirmReload,
  };
}

export default useServerRestart;
