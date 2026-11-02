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
import {
  NEXUS_SERVER_DISCONNECTED_EVENT,
  NEXUS_SERVER_RECONNECTED_EVENT,
  requestServerReconnect,
} from '../../../../interface/api/serverDisconnectedBroadcast';

/*
 * Model for the Preview UI server-disconnected prompt. The ExtJS State controller
 * gives up after repeated poll failures and, when Preview UI is active, dispatches
 * NEXUS_SERVER_DISCONNECTED_EVENT on window instead of showing NX.Dialogs.showError.
 * This machine owns the window subscription (I/O lives in the Model) and the
 * reconnect side effect.
 *
 * Unlike the server-restart machine (which latches, because a reload is terminal),
 * disconnection is a recoverable loop, so the window subscription lives at the root
 * and stays active across every state:
 *
 *   idle          - connected; waiting for a disconnect
 *   disconnected  - modal shown with an active Retry button
 *   retrying      - Retry pressed; reconnect requested, awaiting the outcome
 *
 * The Retry side effect (requestReconnect) dispatches back to ExtJS, which owns the
 * poll. Success arrives as SERVER_RECONNECTED (close); a failed retry re-fires
 * SERVER_DISCONNECTED (show the error again).
 *
 * Disconnection is recoverable, so the modal is dismissable. Critically, by the time
 * this modal shows, ExtJS has already stopped the poll (statusProvider.disconnect()),
 * so DISMISS must reconnect the poll too, otherwise the poll stays dead and the modal
 * never reopens. DISMISS (Escape or a click outside) therefore reconnects and returns
 * to idle, mirroring the legacy dialog, which reconnected the poll on close rather than
 * blocking until reconnect. If the reconnect fails, the poll gives up again and re-fires
 * SERVER_DISCONNECTED, reopening the modal.
 */

export type ServerDisconnectedEvent =
  | { type: 'SERVER_DISCONNECTED' }
  | { type: 'SERVER_RECONNECTED' }
  | { type: 'RETRY' }
  | { type: 'DISMISS' };

/** Ask ExtJS to reconnect the poll. Isolated so tests can assert intent without a window round-trip. */
export function requestReconnect(): void {
  requestServerReconnect();
}

export function createServerDisconnectedMachine() {
  return createMachine<Record<string, never>, ServerDisconnectedEvent>(
    {
      id: 'serverDisconnected',
      predictableActionArguments: true,
      initial: 'idle',
      context: {},
      /* Root-level invoke keeps the listener alive across states so the loop can drive transitions from anywhere. */
      invoke: {
        id: 'listenForServerConnectivity',
        src: 'listenForServerConnectivity',
      },
      states: {
        idle: {
          on: { SERVER_DISCONNECTED: 'disconnected' },
        },
        disconnected: {
          on: {
            RETRY: 'retrying',
            /* Server recovered on its own before the user pressed Retry. */
            SERVER_RECONNECTED: 'idle',
            /*
             * ExtJS already stopped the poll before this modal showed, so a bare dismiss
             * would strand it disconnected forever. Reconnect on dismiss (as the legacy
             * dialog did on close); a later poll failure then reopens the modal.
             */
            DISMISS: { target: 'idle', actions: 'requestReconnect' },
          },
        },
        retrying: {
          entry: 'requestReconnect',
          on: {
            SERVER_RECONNECTED: 'idle',
            /* Retry failed: the poll gave up again, so show the error once more. */
            SERVER_DISCONNECTED: 'disconnected',
            /* A reconnect is already in flight from the retrying entry; just close. */
            DISMISS: 'idle',
          },
        },
      },
    },
    {
      services: {
        listenForServerConnectivity: () => (sendBack) => {
          if (typeof window === 'undefined') {
            return () => {};
          }
          const onDisconnected = () => sendBack({ type: 'SERVER_DISCONNECTED' });
          const onReconnected = () => sendBack({ type: 'SERVER_RECONNECTED' });
          window.addEventListener(NEXUS_SERVER_DISCONNECTED_EVENT, onDisconnected);
          window.addEventListener(NEXUS_SERVER_RECONNECTED_EVENT, onReconnected);
          return () => {
            window.removeEventListener(NEXUS_SERVER_DISCONNECTED_EVENT, onDisconnected);
            window.removeEventListener(NEXUS_SERVER_RECONNECTED_EVENT, onReconnected);
          };
        },
      },
      actions: {
        requestReconnect: () => requestReconnect(),
      },
    },
  );
}
