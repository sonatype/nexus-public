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
import { protectDataMachine } from '../protectDataMachine';

function machineWith(services: {
  caps?: unknown | Error;
  instance?: unknown | Error;
}) {
  return protectDataMachine.withConfig({
    services: {
      fetchIqCapabilities: () =>
        services.caps instanceof Error ? Promise.reject(services.caps) : Promise.resolve(services.caps),
      fetchHcInstanceEnabled: () =>
        services.instance instanceof Error
          ? Promise.reject(services.instance)
          : Promise.resolve(services.instance),
    },
  });
}

describe('protectDataMachine', () => {
  it('stores validated iqCapabilities and hcInstanceEnabled', (done) => {
    const caps = { connected: true, hasFirewall: true, hasLifecycle: false, url: 'https://iq' };
    const service = interpret(
      machineWith({ caps, instance: true }),
    ).onTransition((state) => {
      if (state.matches('capabilities.loaded') && state.matches('instance.loaded')) {
        expect(state.context.iqCapabilities).toEqual(caps);
        expect(state.context.hcInstanceEnabled).toBe(true);
        service.stop();
        done();
      }
    });
    service.start();
  });

  it('defaults iqCapabilities to null on failure and hcInstanceEnabled false when service returns false', (done) => {
    const service = interpret(
      machineWith({ caps: new Error('nope'), instance: false }),
    ).onTransition((state) => {
      if (state.matches('capabilities.loaded') && state.matches('instance.loaded')) {
        expect(state.context.iqCapabilities).toBeNull();
        expect(state.context.hcInstanceEnabled).toBe(false);
        service.stop();
        done();
      }
    });
    service.start();
  });

  it('starts with both regions loading', () => {
    const service = interpret(machineWith({ caps: null, instance: true }));
    expect(service.initialState.matches('capabilities.loading')).toBe(true);
    expect(service.initialState.matches('instance.loading')).toBe(true);
  });
});
