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
import { createHttpFormMachine } from '../httpFormMachine';

// Mock the local API module used by httpFormMachine
jest.mock('../../../../../../../interface/api', () => ({
  ENDPOINTS: {
    HTTP: '/service/rest/v1/http',
  },
  restClient: {
    get: jest.fn(),
    put: jest.fn(),
  },
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

const MOCK_HTTP_CONFIG = {
  userAgentSuffix: 'NexusRepo',
  timeout: 30,
  retries: 3,
  httpEnabled: false,
  httpHost: null,
  httpPort: null,
  httpAuthEnabled: false,
  httpsEnabled: false,
  httpsHost: null,
  httpsPort: null,
  httpsAuthEnabled: false,
  nonProxyHosts: [],
};

const MOCK_HTTP_WITH_PROXY = {
  ...MOCK_HTTP_CONFIG,
  httpEnabled: true,
  httpHost: 'proxy.example.com',
  httpPort: 8080,
  httpAuthEnabled: false,
};

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(overrides?: Record<string, unknown>) {
  restClient.get.mockResolvedValue({ ...MOCK_HTTP_CONFIG, ...overrides });

  const machine = createHttpFormMachine();
  const service = interpret(machine).start();

  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

describe('httpFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading', () => {
    it('starts in loading state then transitions to editing', async () => {
      restClient.get.mockResolvedValue(MOCK_HTTP_CONFIG);
      const machine = createHttpFormMachine();
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });

    it('loads HTTP configuration from REST API', async () => {
      const service = await startAndLoad();
      const state = service.getSnapshot();

      expect(state.context.data.userAgentSuffix).toBe('NexusRepo');
      expect(state.context.data.timeout).toBe(30);
      expect(state.context.data.retries).toBe(3);

      service.stop();
    });

    it('transitions to loadError on API failure', async () => {
      restClient.get.mockRejectedValue(new Error('Network error'));

      const machine = createHttpFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));
      expect(service.getSnapshot().matches('loadError')).toBe(true);

      service.stop();
    });

    it('retries loading on RETRY event', async () => {
      restClient.get.mockRejectedValueOnce(new Error('Network error'));
      restClient.get.mockResolvedValueOnce(MOCK_HTTP_CONFIG);

      const machine = createHttpFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));
      service.send({ type: 'RETRY' } as any);

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().context.data.timeout).toBe(30);

      service.stop();
    });
  });

  describe('validation - timeout and retries', () => {
    it('accepts valid timeout value', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'timeout', value: 60 } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.timeout).toBeFalsy();

      service.stop();
    });

    it('rejects negative timeout', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'timeout', value: -1 } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.timeout).toBeTruthy();

      service.stop();
    });

    it('rejects non-integer retries', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'retries', value: 2.5 } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.retries).toBeTruthy();

      service.stop();
    });
  });

  describe('validation - HTTP proxy', () => {
    it('requires httpHost when httpEnabled', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'httpEnabled', value: true } as any);
      service.send({ type: 'UPDATE', name: 'httpHost', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.httpHost).toBe('HTTP proxy host is required');

      service.stop();
    });

    it('requires httpPort when httpEnabled', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'httpEnabled', value: true } as any);
      service.send({ type: 'UPDATE', name: 'httpHost', value: 'proxy.local' } as any);
      service.send({ type: 'UPDATE', name: 'httpPort', value: 0 } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.httpPort).toBeTruthy();

      service.stop();
    });

    it('validates httpPort range', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'httpEnabled', value: true } as any);
      service.send({ type: 'UPDATE', name: 'httpPort', value: 99999 } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.httpPort).toBe('Port must be between 1 and 65535');

      service.stop();
    });

    it('requires username when httpAuthEnabled', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'httpEnabled', value: true } as any);
      service.send({ type: 'UPDATE', name: 'httpAuthEnabled', value: true } as any);
      service.send({ type: 'UPDATE', name: 'httpAuthUsername', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.httpAuthUsername).toBeTruthy();

      service.stop();
    });

    it('does not validate proxy fields when httpEnabled is false', async () => {
      const service = await startAndLoad();

      // httpEnabled is false by default in mock data
      const state = service.getSnapshot();
      expect(state.context.validationErrors.httpHost).toBeFalsy();
      expect(state.context.validationErrors.httpPort).toBeFalsy();

      service.stop();
    });
  });

  describe('validation - HTTPS proxy', () => {
    it('requires httpsHost when httpsEnabled', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'httpsEnabled', value: true } as any);
      service.send({ type: 'UPDATE', name: 'httpsHost', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.httpsHost).toBe('HTTPS proxy host is required');

      service.stop();
    });

    it('requires httpsPort when httpsEnabled', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'httpsEnabled', value: true } as any);
      service.send({ type: 'UPDATE', name: 'httpsHost', value: 'proxy.local' } as any);
      service.send({ type: 'UPDATE', name: 'httpsPort', value: null } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.httpsPort).toBeTruthy();

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates fields via UPDATE event', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'userAgentSuffix', value: 'CustomAgent' } as any);
      expect(service.getSnapshot().context.data.userAgentSuffix).toBe('CustomAgent');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const service = await startAndLoad();

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'timeout', value: 60 } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'timeout', value: 99 } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.timeout).toBe(30);

      service.stop();
    });
  });

  describe('save flow', () => {
    it('saves with valid data (no proxy)', async () => {
      restClient.put.mockResolvedValue(undefined);

      const service = await startAndLoad();

      // Make a change so form is dirty
      service.send({ type: 'UPDATE', name: 'timeout', value: 60 } as any);
      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) => state.matches('saved'));
      expect(service.getSnapshot().matches('saved')).toBe(true);

      service.stop();
    });

    it('saves with valid proxy config', async () => {
      restClient.get.mockResolvedValue(MOCK_HTTP_WITH_PROXY);
      restClient.put.mockResolvedValue(undefined);

      const machine = createHttpFormMachine();
      const service = interpret(machine).start();
      await waitFor(service, (state) => state.matches('editing'));

      service.send({ type: 'UPDATE', name: 'httpPort', value: 9090 } as any);
      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) => state.matches('saved'));
      expect(service.getSnapshot().matches('saved')).toBe(true);

      service.stop();
    });

    it('returns to editing on save error', async () => {
      restClient.put.mockRejectedValue(new Error('Save failed'));

      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'timeout', value: 60 } as any);
      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) =>
        state.matches('editing') && state.context.saveError !== null
      );

      expect(service.getSnapshot().context.saveError).toBeTruthy();

      service.stop();
    });
  });
});
