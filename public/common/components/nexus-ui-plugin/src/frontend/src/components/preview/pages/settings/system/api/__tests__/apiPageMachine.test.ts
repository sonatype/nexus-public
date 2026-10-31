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
import { waitFor } from 'xstate/lib/waitFor';
import { createApiPageMachine } from '../apiPageMachine';

const MOCK_SWAGGER = { openapi: '3.0.0', paths: { '/foo': {} } };

describe('apiPageMachine', () => {
  it('starts in loading and transitions to loaded on success', async () => {
    const loadSwagger = jest.fn().mockResolvedValue(MOCK_SWAGGER);
    const machine = createApiPageMachine({ swaggerUrl: '/swagger.json', loadSwagger });
    const service = interpret(machine).start();

    expect(service.getSnapshot().matches('loading')).toBe(true);

    await waitFor(service, (state) => state.matches('loaded'));
    expect(service.getSnapshot().context.swagger).toEqual(MOCK_SWAGGER);
    expect(loadSwagger).toHaveBeenCalledWith('/swagger.json');

    service.stop();
  });

  it('transitions to loadError on failure', async () => {
    const loadSwagger = jest.fn().mockRejectedValue(new Error('boom'));
    const machine = createApiPageMachine({ swaggerUrl: '/swagger.json', loadSwagger });
    const service = interpret(machine).start();

    await waitFor(service, (state) => state.matches('loadError'));
    expect(service.getSnapshot().context.swagger).toBeNull();

    service.stop();
  });

  it('retries from loadError back to loading and can succeed on retry', async () => {
    const loadSwagger = jest
      .fn()
      .mockRejectedValueOnce(new Error('once'))
      .mockResolvedValueOnce(MOCK_SWAGGER);
    const machine = createApiPageMachine({ swaggerUrl: '/swagger.json', loadSwagger });
    const service = interpret(machine).start();

    await waitFor(service, (state) => state.matches('loadError'));
    service.send({ type: 'RETRY' });

    await waitFor(service, (state) => state.matches('loaded'));
    expect(service.getSnapshot().context.swagger).toEqual(MOCK_SWAGGER);
    expect(loadSwagger).toHaveBeenCalledTimes(2);

    service.stop();
  });

  it('refreshes from loaded back to loading', async () => {
    const updated = { openapi: '3.0.0', paths: { '/bar': {} } };
    const loadSwagger = jest
      .fn()
      .mockResolvedValueOnce(MOCK_SWAGGER)
      .mockResolvedValueOnce(updated);
    const machine = createApiPageMachine({ swaggerUrl: '/swagger.json', loadSwagger });
    const service = interpret(machine).start();

    await waitFor(service, (state) => state.matches('loaded'));
    service.send({ type: 'REFRESH' });

    expect(service.getSnapshot().matches('loading')).toBe(true);
    await waitFor(service, (state) => state.matches('loaded'));
    expect(service.getSnapshot().context.swagger).toEqual(updated);
    expect(loadSwagger).toHaveBeenCalledTimes(2);

    service.stop();
  });

  it('reloads when the swagger URL changes via SET_SWAGGER_URL', async () => {
    const loadSwagger = jest.fn().mockResolvedValue(MOCK_SWAGGER);
    const machine = createApiPageMachine({ swaggerUrl: '/one.json', loadSwagger });
    const service = interpret(machine).start();

    await waitFor(service, (state) => state.matches('loaded'));
    expect(loadSwagger).toHaveBeenCalledWith('/one.json');

    service.send({ type: 'SET_SWAGGER_URL', url: '/two.json' });
    await waitFor(service, (state) => state.matches('loaded'));
    expect(loadSwagger).toHaveBeenCalledWith('/two.json');

    service.stop();
  });

  it('treats SET_SWAGGER_URL as a no-op while still loading (guards against double-fetch)', async () => {
    // Prevent the initial load from ever resolving so the machine stays in `loading`.
    const loadSwagger = jest.fn().mockImplementation(() => new Promise(() => {}));
    const machine = createApiPageMachine({ swaggerUrl: '/one.json', loadSwagger });
    const service = interpret(machine).start();

    expect(service.getSnapshot().matches('loading')).toBe(true);
    expect(loadSwagger).toHaveBeenCalledTimes(1);
    expect(loadSwagger).toHaveBeenCalledWith('/one.json');

    // Sending SET_SWAGGER_URL while `loading` should be dropped by the machine.
    // Neither `swaggerUrl` in context nor the load-service invocation count should change.
    service.send({ type: 'SET_SWAGGER_URL', url: '/two.json' });
    expect(service.getSnapshot().context.swaggerUrl).toBe('/one.json');
    expect(loadSwagger).toHaveBeenCalledTimes(1);

    service.stop();
  });

  it('clears swagger on entering loading (so stale data is not shown during refresh)', async () => {
    const loadSwagger = jest.fn()
      .mockResolvedValueOnce(MOCK_SWAGGER)
      .mockImplementationOnce(() => new Promise(() => {}));
    const machine = createApiPageMachine({ swaggerUrl: '/one.json', loadSwagger });
    const service = interpret(machine).start();

    await waitFor(service, (state) => state.matches('loaded'));
    expect(service.getSnapshot().context.swagger).toEqual(MOCK_SWAGGER);

    service.send({ type: 'REFRESH' });
    expect(service.getSnapshot().matches('loading')).toBe(true);
    expect(service.getSnapshot().context.swagger).toBeNull();

    service.stop();
  });
});
