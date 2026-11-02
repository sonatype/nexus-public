/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import {
  NEXUS_SERVER_DISCONNECTED_EVENT,
  NEXUS_SERVER_RECONNECT_REQUESTED_EVENT,
  NEXUS_SERVER_RECONNECTED_EVENT,
  requestServerReconnect,
} from '../serverDisconnectedBroadcast';

describe('serverDisconnectedBroadcast', () => {
  it('exposes the event names shared with the ExtJS controller', () => {
    expect(NEXUS_SERVER_DISCONNECTED_EVENT).toBe('nexus:server-disconnected');
    expect(NEXUS_SERVER_RECONNECT_REQUESTED_EVENT).toBe(
      'nexus:server-reconnect-requested',
    );
    expect(NEXUS_SERVER_RECONNECTED_EVENT).toBe('nexus:server-reconnected');
  });

  it('dispatches the reconnect-requested event on the window', () => {
    const handler = jest.fn();
    window.addEventListener(NEXUS_SERVER_RECONNECT_REQUESTED_EVENT, handler);

    requestServerReconnect();

    expect(handler).toHaveBeenCalledTimes(1);
    window.removeEventListener(NEXUS_SERVER_RECONNECT_REQUESTED_EVENT, handler);
  });
});
