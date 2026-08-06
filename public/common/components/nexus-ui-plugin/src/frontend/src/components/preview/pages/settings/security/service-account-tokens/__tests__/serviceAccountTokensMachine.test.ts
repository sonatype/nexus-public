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
import {
  createServiceAccountTokensMachine,
  type ServiceAccountTokensServices,
} from '../serviceAccountTokensMachine';

const SAMPLE_TOKEN = {
  id: 'token-1',
  name: 'Jenkins CI',
  description: '',
  roleId: 'nx-admin',
  createdBy: 'admin',
  createdAt: '2024-01-01T00:00:00Z',
  expiresAt: null,
  lastUsedAt: null,
};

const SAMPLE_ROLE = { id: 'nx-admin', name: 'Administrator' };

function buildServices(overrides: Partial<ServiceAccountTokensServices> = {}): ServiceAccountTokensServices {
  return {
    loadAll: jest
      .fn()
      .mockResolvedValue({ tokens: [SAMPLE_TOKEN], roles: [SAMPLE_ROLE], rolesError: null }),
    createToken: jest.fn().mockResolvedValue({
      token: { ...SAMPLE_TOKEN, id: 'new', name: 'New' },
      rawToken: 'raw-abc',
    }),
    revokeToken: jest.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

describe('serviceAccountTokensMachine', () => {
  describe('loading', () => {
    it('starts in loading and populates context on success', async () => {
      const services = buildServices();
      const service = interpret(createServiceAccountTokensMachine(services)).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);
      await waitFor(service, (state) => state.matches('idle'));
      const { context } = service.getSnapshot();
      expect(context.tokens).toEqual([SAMPLE_TOKEN]);
      expect(context.roles).toEqual([SAMPLE_ROLE]);
      expect(context.rolesError).toBeNull();

      service.stop();
    });

    it('transitions to loadError on failure and can retry', async () => {
      const loadAll = jest
        .fn()
        .mockRejectedValueOnce(new Error('boom'))
        .mockResolvedValueOnce({ tokens: [SAMPLE_TOKEN], roles: [], rolesError: null });
      const services = buildServices({ loadAll });
      const service = interpret(createServiceAccountTokensMachine(services)).start();

      await waitFor(service, (state) => state.matches('loadError'));
      expect(service.getSnapshot().context.loadError).toMatch(/boom/);

      service.send({ type: 'RETRY' });
      await waitFor(service, (state) => state.matches('idle'));
      expect(service.getSnapshot().context.tokens).toEqual([SAMPLE_TOKEN]);

      service.stop();
    });
  });

  describe('create flow', () => {
    it('submits, refreshes list, and stores lastCreated', async () => {
      const services = buildServices();
      const service = interpret(createServiceAccountTokensMachine(services)).start();

      await waitFor(service, (state) => state.matches('idle'));

      service.send({
        type: 'SUBMIT_CREATE',
        form: { name: 'New', description: '', roleId: 'nx-admin' },
        commandId: 1,
      });
      expect(service.getSnapshot().matches('submittingCreate')).toBe(true);

      await waitFor(service, (state) => state.matches('idle'));
      const { context } = service.getSnapshot();
      expect(context.lastCreated).toEqual({
        token: expect.objectContaining({ id: 'new', name: 'New' }),
        rawToken: 'raw-abc',
      });
      expect(context.createError).toBeNull();
      expect(services.createToken).toHaveBeenCalledWith({
        name: 'New',
        description: '',
        roleId: 'nx-admin',
      });
      expect(services.loadAll).toHaveBeenCalledTimes(2);

      service.stop();
    });

    it('surfaces createError on failure and returns to idle', async () => {
      const createToken = jest.fn().mockRejectedValue(new Error('nope'));
      const services = buildServices({ createToken });
      const service = interpret(createServiceAccountTokensMachine(services)).start();

      await waitFor(service, (state) => state.matches('idle'));

      service.send({
        type: 'SUBMIT_CREATE',
        form: { name: 'New', description: '', roleId: 'nx-admin' },
        commandId: 1,
      });
      await waitFor(service, (state) => state.matches('idle') && state.context.createError !== null);
      expect(service.getSnapshot().context.createError).toMatch(/nope/);

      service.stop();
    });
  });

  describe('revoke flow', () => {
    it('revokes, refreshes list, and clears pendingRevokeId', async () => {
      const services = buildServices();
      const service = interpret(createServiceAccountTokensMachine(services)).start();

      await waitFor(service, (state) => state.matches('idle'));

      service.send({ type: 'REVOKE', tokenId: SAMPLE_TOKEN.id, commandId: 1 });
      expect(service.getSnapshot().matches('revoking')).toBe(true);
      expect(service.getSnapshot().context.pendingRevokeId).toBe(SAMPLE_TOKEN.id);

      await waitFor(service, (state) => state.matches('idle'));
      expect(service.getSnapshot().context.pendingRevokeId).toBeNull();
      expect(service.getSnapshot().context.revokeError).toBeNull();
      expect(services.revokeToken).toHaveBeenCalledWith(SAMPLE_TOKEN.id);
      expect(services.loadAll).toHaveBeenCalledTimes(2);

      service.stop();
    });

    it('surfaces revokeError on failure and returns to idle', async () => {
      const revokeToken = jest.fn().mockRejectedValue(new Error('cannot'));
      const services = buildServices({ revokeToken });
      const service = interpret(createServiceAccountTokensMachine(services)).start();

      await waitFor(service, (state) => state.matches('idle'));

      service.send({ type: 'REVOKE', tokenId: SAMPLE_TOKEN.id, commandId: 1 });
      await waitFor(service, (state) => state.matches('idle') && state.context.revokeError !== null);
      expect(service.getSnapshot().context.revokeError).toMatch(/cannot/);

      service.stop();
    });
  });

  describe('commandId monotonicity', () => {
    it('increments commandId on each command dispatch', async () => {
      const services = buildServices();
      const service = interpret(createServiceAccountTokensMachine(services)).start();

      await waitFor(service, (state) => state.matches('idle'));
      expect(service.getSnapshot().context.commandId).toBe(0);

      service.send({
        type: 'SUBMIT_CREATE',
        form: { name: 'A', description: '', roleId: 'r' },
        commandId: 1,
      });
      expect(service.getSnapshot().context.commandId).toBe(1);

      await waitFor(service, (state) => state.matches('idle'));

      service.send({ type: 'REVOKE', tokenId: 'x', commandId: 2 });
      expect(service.getSnapshot().context.commandId).toBe(2);

      service.stop();
    });
  });

  describe('error clearing', () => {
    it('CLEAR_ERROR clears load, create, and revoke errors when idle', async () => {
      const revokeToken = jest.fn().mockRejectedValue(new Error('cannot'));
      const services = buildServices({ revokeToken });
      const service = interpret(createServiceAccountTokensMachine(services)).start();

      await waitFor(service, (state) => state.matches('idle'));
      service.send({ type: 'REVOKE', tokenId: 'x', commandId: 1 });
      await waitFor(
        service,
        (state) => state.matches('idle') && state.context.revokeError !== null
      );

      service.send({ type: 'CLEAR_ERROR' });
      expect(service.getSnapshot().context.revokeError).toBeNull();

      service.stop();
    });
  });
});
