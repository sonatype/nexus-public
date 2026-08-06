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
  createDataStoreFormMachine,
  toFormData,
  toUpdatePayload,
} from '../dataStoreFormMachine';

jest.mock('../../../../../../../interface/api', () => ({
  ENDPOINTS: { DATASTORE: '/service/rest/internal/ui/datastore' },
  restClient: { get: jest.fn(), put: jest.fn() },
  parseApiError: (err: unknown) => ({ message: err instanceof Error ? err.message : String(err) }),
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

const MOCK_CONFIG = {
  name: 'nexus', source: 'local', type: 'jdbc',
  jdbcUrl: 'jdbc:postgresql://localhost:5432/nexus',
  username: 'nexus_user', schema: 'nexus',
  maximumConnectionPool: 100,
  advanced: 'socketTimeout=30000\nconnectTimeout=5000',
};

async function startAndLoad(config = MOCK_CONFIG) {
  restClient.get.mockResolvedValue(config);
  const service = interpret(createDataStoreFormMachine()).start();
  await waitFor(service, (s) => s.matches('editing'));
  return service;
}

describe('dataStoreFormMachine', () => {
  beforeEach(() => jest.clearAllMocks());

  it('loads config into form data (pool + parsed params + read-only fields)', async () => {
    const service = await startAndLoad();
    const { data } = service.getSnapshot().context;
    expect(data.maximumConnectionPool).toBe(100);
    expect(data.jdbcUrl).toBe('jdbc:postgresql://localhost:5432/nexus');
    expect(data.jdbcParameters.map((p) => p.name)).toEqual(['socketTimeout', 'connectTimeout']);
    service.stop();
  });

  it('is pristine after load and dirty after an edit', async () => {
    const service = await startAndLoad();
    expect(service.getSnapshot().context.isPristine).toBe(true);
    service.send({ type: 'UPDATE', name: 'maximumConnectionPool', value: '200' });
    expect(service.getSnapshot().context.isPristine).toBe(false);
    service.stop();
  });

  it('reports a pool validation error when out of range', async () => {
    const service = await startAndLoad();
    service.send({ type: 'UPDATE', name: 'maximumConnectionPool', value: '5000' });
    expect(service.getSnapshot().context.validationErrors.maximumConnectionPool).toMatch(/at most 3000/i);
    service.stop();
  });

  it('SUBMIT with a valid change PUTs the serialized payload', async () => {
    restClient.put.mockResolvedValue({ ...MOCK_CONFIG, maximumConnectionPool: 200 });
    const service = await startAndLoad();
    service.send({ type: 'UPDATE', name: 'maximumConnectionPool', value: '200' });
    service.send({ type: 'SUBMIT' });
    await waitFor(service, (s) => s.matches('editing') && s.context.isPristine);
    expect(restClient.put).toHaveBeenCalledWith('/service/rest/internal/ui/datastore', {
      maximumConnectionPool: 200,
      advanced: 'socketTimeout=30000\nconnectTimeout=5000',
    });
    service.stop();
  });

  it('SUBMIT with an invalid pool does not PUT', async () => {
    const service = await startAndLoad();
    service.send({ type: 'UPDATE', name: 'maximumConnectionPool', value: '0' });
    service.send({ type: 'SUBMIT' });
    await new Promise((r) => setTimeout(r, 0));
    expect(restClient.put).not.toHaveBeenCalled();
    service.stop();
  });

  it('toUpdatePayload serializes only custom params and coerces pool', () => {
    const payload = toUpdatePayload(toFormData(MOCK_CONFIG));
    expect(payload).toEqual({ maximumConnectionPool: 100, advanced: 'socketTimeout=30000\nconnectTimeout=5000' });
  });
});
