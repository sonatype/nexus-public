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

import { interpret } from 'xstate';
import {
  createServerDisconnectedMachine,
  requestReconnect,
} from '../serverDisconnectedMachine';
import {
  NEXUS_SERVER_DISCONNECTED_EVENT,
  NEXUS_SERVER_RECONNECTED_EVENT,
  NEXUS_SERVER_RECONNECT_REQUESTED_EVENT,
} from '../../../../../interface/api/serverDisconnectedBroadcast';

describe('createServerDisconnectedMachine', () => {
  it('starts in the idle state', () => {
    const service = interpret(createServerDisconnectedMachine()).start();
    expect(service.state.matches('idle')).toBe(true);
    service.stop();
  });

  it('transitions idle to disconnected on a SERVER_DISCONNECTED event', () => {
    const service = interpret(createServerDisconnectedMachine()).start();
    service.send({ type: 'SERVER_DISCONNECTED' });
    expect(service.state.matches('disconnected')).toBe(true);
    service.stop();
  });

  it('transitions idle to disconnected when the disconnect window event is dispatched', () => {
    const service = interpret(createServerDisconnectedMachine()).start();
    window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT));
    expect(service.state.matches('disconnected')).toBe(true);
    service.stop();
  });

  it('transitions disconnected to retrying and runs requestReconnect on RETRY', () => {
    const reconnectSpy = jest.fn();
    const machine = createServerDisconnectedMachine().withConfig({
      actions: { requestReconnect: reconnectSpy },
    });
    const service = interpret(machine).start();

    service.send({ type: 'SERVER_DISCONNECTED' });
    service.send({ type: 'RETRY' });

    expect(service.state.matches('retrying')).toBe(true);
    expect(reconnectSpy).toHaveBeenCalledTimes(1);
    service.stop();
  });

  it('returns to idle when the server recovers on its own before Retry', () => {
    const service = interpret(createServerDisconnectedMachine()).start();

    service.send({ type: 'SERVER_DISCONNECTED' });
    service.send({ type: 'SERVER_RECONNECTED' });

    expect(service.state.matches('idle')).toBe(true);
    service.stop();
  });

  it('returns to idle when a retry succeeds (SERVER_RECONNECTED while retrying)', () => {
    const machine = createServerDisconnectedMachine().withConfig({
      actions: { requestReconnect: jest.fn() },
    });
    const service = interpret(machine).start();

    service.send({ type: 'SERVER_DISCONNECTED' });
    service.send({ type: 'RETRY' });
    service.send({ type: 'SERVER_RECONNECTED' });

    expect(service.state.matches('idle')).toBe(true);
    service.stop();
  });

  it('re-shows the error when a retry fails (SERVER_DISCONNECTED while retrying)', () => {
    const machine = createServerDisconnectedMachine().withConfig({
      actions: { requestReconnect: jest.fn() },
    });
    const service = interpret(machine).start();

    service.send({ type: 'SERVER_DISCONNECTED' });
    service.send({ type: 'RETRY' });
    service.send({ type: 'SERVER_DISCONNECTED' });

    expect(service.state.matches('disconnected')).toBe(true);
    service.stop();
  });

  it('reconnects the poll and returns to idle when dismissed from disconnected', () => {
    const reconnectSpy = jest.fn();
    const machine = createServerDisconnectedMachine().withConfig({
      actions: { requestReconnect: reconnectSpy },
    });
    const service = interpret(machine).start();

    service.send({ type: 'SERVER_DISCONNECTED' });
    service.send({ type: 'DISMISS' });

    expect(service.state.matches('idle')).toBe(true);
    // The poll was already stopped before the modal showed; dismiss must revive it.
    expect(reconnectSpy).toHaveBeenCalledTimes(1);
    service.stop();
  });

  it('does not reconnect again when dismissed while retrying (retry already reconnected)', () => {
    const reconnectSpy = jest.fn();
    const machine = createServerDisconnectedMachine().withConfig({
      actions: { requestReconnect: reconnectSpy },
    });
    const service = interpret(machine).start();

    service.send({ type: 'SERVER_DISCONNECTED' });
    service.send({ type: 'RETRY' });
    service.send({ type: 'DISMISS' });

    expect(service.state.matches('idle')).toBe(true);
    // Only the retrying-entry reconnect ran; dismiss must not fire a second one.
    expect(reconnectSpy).toHaveBeenCalledTimes(1);
    service.stop();
  });

  it('reopens the modal when a disconnect fires again after a dismiss', () => {
    const service = interpret(createServerDisconnectedMachine()).start();

    service.send({ type: 'SERVER_DISCONNECTED' });
    service.send({ type: 'DISMISS' });
    window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT));

    expect(service.state.matches('disconnected')).toBe(true);
    service.stop();
  });

  it('keeps listening across states so a later disconnect still shows the modal (loop, not latch)', () => {
    const machine = createServerDisconnectedMachine().withConfig({
      actions: { requestReconnect: jest.fn() },
    });
    const service = interpret(machine).start();

    window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT));
    window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RECONNECTED_EVENT));
    expect(service.state.matches('idle')).toBe(true);

    window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT));
    expect(service.state.matches('disconnected')).toBe(true);
    service.stop();
  });

  it('does not react to the window event after the service stops', () => {
    const service = interpret(createServerDisconnectedMachine()).start();
    service.stop();
    expect(() =>
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT)),
    ).not.toThrow();
  });
});

describe('requestReconnect', () => {
  it('dispatches the reconnect-requested event on the window', () => {
    const handler = jest.fn();
    window.addEventListener(NEXUS_SERVER_RECONNECT_REQUESTED_EVENT, handler);

    requestReconnect();

    expect(handler).toHaveBeenCalledTimes(1);
    window.removeEventListener(NEXUS_SERVER_RECONNECT_REQUESTED_EVENT, handler);
  });
});
