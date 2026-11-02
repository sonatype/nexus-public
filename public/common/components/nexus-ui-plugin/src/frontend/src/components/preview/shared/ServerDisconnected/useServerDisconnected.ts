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
import { createServerDisconnectedMachine } from './serverDisconnectedMachine';

/* Immutable machine config: build once at module load and share. useMachine spins a fresh service per hook instance. */
const serverDisconnectedMachine = createServerDisconnectedMachine();

/*
 * ViewModel (thin projection) over serverDisconnectedMachine. Owns no state of its
 * own: the machine holds the window subscription and the reconnect side effect.
 * This hook only projects machine state into view-facing flags and exposes a single
 * semantic command (requestRetry) that the View fires from its Retry button.
 */

export interface UseServerDisconnectedResult {
  /** Whether the disconnect modal should be shown */
  isDisconnected: boolean;
  /** Whether a reconnect attempt is in progress (button disabled state) */
  isRetrying: boolean;
  /** Request a reconnect (fired by the Retry button) */
  requestRetry: () => void;
  /** Dismiss the modal (Escape or click outside); a later poll failure reopens it */
  dismiss: () => void;
}

export function useServerDisconnected(): UseServerDisconnectedResult {
  const [state, send] = useMachine(serverDisconnectedMachine);

  const requestRetry = useCallback(() => send({ type: 'RETRY' }), [send]);
  const dismiss = useCallback(() => send({ type: 'DISMISS' }), [send]);

  return {
    isDisconnected: state.matches('disconnected') || state.matches('retrying'),
    isRetrying: state.matches('retrying'),
    requestRetry,
    dismiss,
  };
}

export default useServerDisconnected;
