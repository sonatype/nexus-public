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
import { createEmailFormMachine } from '../emailFormMachine';

// Mock the local API module used by emailFormMachine
jest.mock('../../../../../../../interface/api', () => ({
  ENDPOINTS: {
    EMAIL: '/service/rest/v1/email',
  },
  restClient: {
    get: jest.fn(),
    put: jest.fn(),
  },
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

const MOCK_EMAIL_CONFIG = {
  enabled: true,
  host: 'smtp.example.com',
  port: 587,
  useAuthentication: true,
  username: 'user',
  password: 'pass',
  fromAddress: 'noreply@example.com',
  subjectPrefix: '[Nexus]',
  startTlsEnabled: true,
  startTlsRequired: false,
  sslOnConnectEnabled: false,
  sslServerIdentityCheckEnabled: false,
  nexusTrustStoreEnabled: false,
};

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(overrides?: Partial<typeof MOCK_EMAIL_CONFIG>) {
  restClient.get.mockResolvedValue({ ...MOCK_EMAIL_CONFIG, ...overrides });

  const machine = createEmailFormMachine();
  const service = interpret(machine).start();

  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

describe('emailFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading', () => {
    it('starts in loading state then transitions to editing', async () => {
      restClient.get.mockResolvedValue(MOCK_EMAIL_CONFIG);
      const machine = createEmailFormMachine();
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });

    it('loads email configuration from REST API', async () => {
      const service = await startAndLoad();
      const state = service.getSnapshot();

      expect(state.context.data.host).toBe('smtp.example.com');
      expect(state.context.data.port).toBe(587);
      expect(state.context.data.fromAddress).toBe('noreply@example.com');
      expect(state.context.data.enabled).toBe(true);

      service.stop();
    });

    it('transitions to loadError on API failure', async () => {
      restClient.get.mockRejectedValue(new Error('Network error'));

      const machine = createEmailFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));
      expect(service.getSnapshot().matches('loadError')).toBe(true);
      expect(service.getSnapshot().context.loadError).toBeTruthy();

      service.stop();
    });

    it('retries loading on RETRY event', async () => {
      restClient.get.mockRejectedValueOnce(new Error('Network error'));
      restClient.get.mockResolvedValueOnce(MOCK_EMAIL_CONFIG);

      const machine = createEmailFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));

      service.send({ type: 'RETRY' } as any);

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().context.data.host).toBe('smtp.example.com');

      service.stop();
    });
  });

  describe('validation', () => {
    it('requires host when enabled', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'host', value: '' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.host).toBe('SMTP host is required');

      service.stop();
    });

    it('requires port in valid range when enabled', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'port', value: 0 } as any);
      service.send({ type: 'SUBMIT' } as any);

      let state = service.getSnapshot();
      expect(state.context.validationErrors.port).toBeTruthy();

      // Test out-of-range port
      service.send({ type: 'UPDATE', name: 'port', value: 70000 } as any);
      state = service.getSnapshot();
      expect(state.context.validationErrors.port).toBe('Port must be between 1 and 65535');

      service.stop();
    });

    it('requires valid fromAddress when enabled', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'fromAddress', value: 'not-an-email' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.fromAddress).toBe('Invalid email address format');

      service.stop();
    });

    it('does not require fields when disabled', async () => {
      const service = await startAndLoad({ enabled: false });

      service.send({ type: 'UPDATE', name: 'host', value: '' } as any);
      service.send({ type: 'UPDATE', name: 'fromAddress', value: '' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.host).toBeFalsy();
      expect(state.context.validationErrors.fromAddress).toBeFalsy();

      service.stop();
    });

    it('passes validation with valid data and returns to editing', async () => {
      restClient.put.mockResolvedValue(undefined);

      const service = await startAndLoad();

      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) => state.matches('editing') && state.context.isPristine);
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates fields via UPDATE event', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'host', value: 'new-smtp.example.com' } as any);

      expect(service.getSnapshot().context.data.host).toBe('new-smtp.example.com');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const service = await startAndLoad();

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'host', value: 'changed-host' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'host', value: 'changed-host' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.host).toBe('smtp.example.com');

      service.stop();
    });

    it('updates boolean fields (enabled toggle)', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'enabled', value: false } as any);
      expect(service.getSnapshot().context.data.enabled).toBe(false);

      service.send({ type: 'UPDATE', name: 'startTlsEnabled', value: true } as any);
      expect(service.getSnapshot().context.data.startTlsEnabled).toBe(true);

      service.stop();
    });
  });

  describe('save flow', () => {
    it('transitions through validating -> saving -> saved -> editing and is pristine', async () => {
      restClient.put.mockResolvedValue(undefined);

      const service = await startAndLoad();

      service.send({ type: 'SUBMIT' } as any);

      // Machine passes through saved and auto-transitions back to editing
      await waitFor(service, (state) => state.matches('editing') && state.context.isPristine);
      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.isPristine).toBe(true);

      service.stop();
    });

    it('returns to editing on save error', async () => {
      restClient.put.mockRejectedValue(new Error('Save failed'));

      const service = await startAndLoad();

      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) =>
        state.matches('editing') && state.context.saveError !== null
      );

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.saveError).toBeTruthy();

      service.stop();
    });
  });
});
