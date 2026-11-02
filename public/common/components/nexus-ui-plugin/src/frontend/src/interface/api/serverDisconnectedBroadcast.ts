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

/*
 * Two-way window-event contract between the ExtJS State controller and the React
 * shell for the Preview UI "server disconnected" modal. Unlike the fire-and-forget
 * server-restart event, disconnection is a recoverable loop that needs a return
 * leg: the modal's Retry has to reconnect the still-ExtJS polling provider.
 *
 * The ExtJS side (NX/controller/State.js) dispatches and listens for these same
 * string literals directly; keep the two in sync.
 *
 *   1. NEXUS_SERVER_DISCONNECTED_EVENT        ExtJS -> React: show the modal
 *   2. NEXUS_SERVER_RECONNECT_REQUESTED_EVENT  React -> ExtJS: Retry pressed, call connect()
 *   3. NEXUS_SERVER_RECONNECTED_EVENT          ExtJS -> React: poll recovered, close the modal
 */

/** ExtJS -> React: the poll gave up after repeated failures; show the disconnect modal. */
export const NEXUS_SERVER_DISCONNECTED_EVENT = 'nexus:server-disconnected';

/** React -> ExtJS: the user pressed Retry; reconnect the ExtJS status poll. */
export const NEXUS_SERVER_RECONNECT_REQUESTED_EVENT = 'nexus:server-reconnect-requested';

/** ExtJS -> React: a poll succeeded again; close the disconnect modal. */
export const NEXUS_SERVER_RECONNECTED_EVENT = 'nexus:server-reconnected';

/*
 * The ExtJS -> React legs (disconnected, reconnected) are dispatched by State.js
 * directly against the string literals above, so no React-side dispatcher is needed.
 * Only the React -> ExtJS leg has a JS caller (the machine's Retry action).
 */

/** React -> ExtJS request to reconnect the status poll (fired by the Retry button). */
export function requestServerReconnect(): void {
  if (typeof window === 'undefined') {
    return;
  }
  window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RECONNECT_REQUESTED_EVENT));
}
