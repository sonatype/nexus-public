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
import { createUsageChartMachine } from '../usageChartMachine';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn() },
  parseApiError: (e: any) => ({
    message: e?.response?.data?.message ?? e?.message ?? 'err',
    status: e?.response?.status ?? e?.status ?? 0,
  }),
  isPermissionError: (apiErr: any) => apiErr?.status === 403,
}));
const { restClient } = jest.requireMock('../../../../../../../interface/api');

// egress then storage per fetch pair
function mockPair(egress: any[], storage: any[]) {
  restClient.get
    .mockResolvedValueOnce({ data: egress })
    .mockResolvedValueOnce({ data: storage });
}

describe('usageChartMachine', () => {
  beforeEach(() => jest.clearAllMocks());

  it('initializes month options and combines egress + storage by date', async () => {
    mockPair(
      [{ date: '2026-01-01', bytes: 100 }],
      [{ date: '2026-01-01', bytes: 200 }],
    );
    const service = interpret(createUsageChartMachine()).start();
    await waitFor(service, (s) => s.matches('loaded'));
    const ctx = service.getSnapshot().context;
    expect(ctx.monthOptions.length).toBeGreaterThan(0);
    expect(ctx.combinedData).toEqual([{ metricDate: '2026-01-01', egress: 100, storage: 200 }]);
    service.stop();
  });

  it('coerces non-finite byte values to 0 when combining', async () => {
    mockPair(
      [{ date: '2026-01-01', bytes: 'N/A' }],
      [{ date: '2026-01-01', bytes: '200' }],
    );
    const service = interpret(createUsageChartMachine()).start();
    await waitFor(service, (s) => s.matches('loaded'));
    expect(service.getSnapshot().context.combinedData).toEqual([
      { metricDate: '2026-01-01', egress: 0, storage: 200 },
    ]);
    service.stop();
  });

  it('SELECT_MONTH refetches for the chosen range', async () => {
    mockPair([], []);
    const service = interpret(createUsageChartMachine()).start();
    await waitFor(service, (s) => s.matches('loaded'));
    mockPair([{ date: '2025-12-01', bytes: 5 }], []);
    const month = { key: 'k', label: 'Dec', value: { dateFrom: '2025-12-01', dateTo: '2025-12-31' } };
    service.send({ type: 'SELECT_MONTH', month });
    await waitFor(service, (s) => s.matches('loaded') && s.context.selectedMonth?.key === 'k');
    expect(service.getSnapshot().context.dateFrom).toBe('2025-12-01');
    service.stop();
  });

  it('flags a 403 as permission error', async () => {
    restClient.get.mockRejectedValue({ response: { status: 403 }, message: 'forbidden' });
    const service = interpret(createUsageChartMachine()).start();
    await waitFor(service, (s) => s.matches('loadError'));
    expect(service.getSnapshot().context.isPermissionError).toBe(true);
    service.stop();
  });

  it('recovers from loadError by selecting a different month', async () => {
    restClient.get.mockRejectedValue(new Error('boom'));
    const service = interpret(createUsageChartMachine()).start();
    await waitFor(service, (s) => s.matches('loadError'));

    // Picking a different month while errored must refetch that range.
    mockPair([{ date: '2025-11-01', bytes: 9 }], []);
    const month = { key: 'nov', label: 'Nov', value: { dateFrom: '2025-11-01', dateTo: '2025-11-30' } };
    service.send({ type: 'SELECT_MONTH', month });

    await waitFor(service, (s) => s.matches('loaded') && s.context.selectedMonth?.key === 'nov');
    expect(service.getSnapshot().context.dateFrom).toBe('2025-11-01');
    expect(service.getSnapshot().context.combinedData).toEqual([
      { metricDate: '2025-11-01', egress: 9, storage: 0 },
    ]);
    service.stop();
  });
});
