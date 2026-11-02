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

import { createMachine } from 'xstate';
import { NEXUS_SERVER_RESTARTED_EVENT } from '../../../../interface/api/serverRestartBroadcast';

/*
 * Model for the Preview UI server-restart prompt. The ExtJS State controller
 * detects a serverId/clusterId change and, when Preview UI is active, dispatches
 * NEXUS_SERVER_RESTARTED_EVENT on window instead of showing NX.Dialogs.showInfo.
 * This machine owns the window subscription (I/O lives in the Model) and the
 * reload side effect. States: idle -> prompting -> reloading.
 *
 * The window listener is scoped to the idle state, so once we move to prompting
 * the subscription is torn down. That gives us a natural latch: repeat restart
 * events cannot re-open or stack the modal.
 */

export type ServerRestartEvent = { type: 'SERVER_RESTARTED' } | { type: 'RELOAD' };

/** Reload the application. Isolated so tests can assert the reload intent without navigating. */
export function reloadApplication(): void {
  if (typeof window !== 'undefined') {
    window.location.reload();
  }
}

export function createServerRestartMachine() {
  return createMachine<Record<string, never>, ServerRestartEvent>(
    {
      id: 'serverRestart',
      predictableActionArguments: true,
      initial: 'idle',
      context: {},
      states: {
        idle: {
          invoke: {
            id: 'listenForServerRestart',
            src: 'listenForServerRestart',
          },
          on: { SERVER_RESTARTED: 'prompting' },
        },
        /* Latch: no SERVER_RESTARTED handler here, so repeat events are ignored. */
        prompting: {
          on: { RELOAD: 'reloading' },
        },
        /* Terminal-by-design: the reload replaces the running app. */
        reloading: {
          entry: 'reloadApplication',
        },
      },
    },
    {
      services: {
        listenForServerRestart: () => (sendBack) => {
          if (typeof window === 'undefined') {
            return () => {};
          }
          const handler = () => sendBack({ type: 'SERVER_RESTARTED' });
          window.addEventListener(NEXUS_SERVER_RESTARTED_EVENT, handler);
          return () => window.removeEventListener(NEXUS_SERVER_RESTARTED_EVENT, handler);
        },
      },
      actions: {
        reloadApplication: () => reloadApplication(),
      },
    },
  );
}
