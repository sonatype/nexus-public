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

import { interpret } from 'xstate';
import { createServerRestartMachine, reloadApplication } from '../serverRestartMachine';
import { NEXUS_SERVER_RESTARTED_EVENT } from '../../../../../interface/api/serverRestartBroadcast';

describe('createServerRestartMachine', () => {
  it('starts in the idle state', () => {
    const service = interpret(createServerRestartMachine()).start();
    expect(service.state.matches('idle')).toBe(true);
    service.stop();
  });

  it('transitions idle to prompting on a SERVER_RESTARTED event', () => {
    const service = interpret(createServerRestartMachine()).start();
    service.send({ type: 'SERVER_RESTARTED' });
    expect(service.state.matches('prompting')).toBe(true);
    service.stop();
  });

  it('transitions idle to prompting when the window event is dispatched', () => {
    const service = interpret(createServerRestartMachine()).start();
    window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RESTARTED_EVENT));
    expect(service.state.matches('prompting')).toBe(true);
    service.stop();
  });

  it('ignores repeat SERVER_RESTARTED events once prompting (latch)', () => {
    const service = interpret(createServerRestartMachine()).start();
    service.send({ type: 'SERVER_RESTARTED' });
    service.send({ type: 'SERVER_RESTARTED' });
    expect(service.state.matches('prompting')).toBe(true);
    service.stop();
  });

  it('ignores the window event once prompting (listener torn down on leaving idle)', () => {
    const service = interpret(createServerRestartMachine()).start();
    service.send({ type: 'SERVER_RESTARTED' });
    window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RESTARTED_EVENT));
    expect(service.state.matches('prompting')).toBe(true);
    service.stop();
  });

  it('transitions prompting to reloading and runs the reload action on RELOAD', () => {
    const reloadSpy = jest.fn();
    const machine = createServerRestartMachine().withConfig({
      actions: { reloadApplication: reloadSpy },
    });
    const service = interpret(machine).start();

    service.send({ type: 'SERVER_RESTARTED' });
    service.send({ type: 'RELOAD' });

    expect(service.state.matches('reloading')).toBe(true);
    expect(reloadSpy).toHaveBeenCalledTimes(1);
    service.stop();
  });

  it('does not react to the window event after the service stops', () => {
    const service = interpret(createServerRestartMachine()).start();
    service.stop();
    expect(() =>
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RESTARTED_EVENT)),
    ).not.toThrow();
  });
});

describe('reloadApplication', () => {
  const originalLocation = window.location;

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: originalLocation,
    });
  });

  it('calls window.location.reload', () => {
    const reloadMock = jest.fn();
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...originalLocation, reload: reloadMock },
    });

    reloadApplication();

    expect(reloadMock).toHaveBeenCalledTimes(1);
  });
});
