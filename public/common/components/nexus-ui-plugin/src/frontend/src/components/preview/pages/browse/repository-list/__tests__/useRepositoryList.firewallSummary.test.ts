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

import { renderHook, waitFor } from '@testing-library/react';

import { useRepositoryList } from '../useRepositoryList';

const mockCheckPermission = jest.fn();
const mockClmState = jest.fn();
jest.mock('../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: (...args: unknown[]) => mockCheckPermission(...args),
    state: () => ({ getValue: () => mockClmState() }),
  },
}));

const mockGet = jest.fn();
jest.mock('../../../../../../interface/api', () => {
  const actual = jest.requireActual('../../../../../../interface/api');
  return { ...actual, restClient: { get: (...args: unknown[]) => mockGet(...args), put: jest.fn(), post: jest.fn() } };
});

const SUMMARY_URL_FRAGMENT = 'summary';

function stubApi(summary: () => Promise<unknown>) {
  mockGet.mockImplementation((url: string) =>
    url.includes(SUMMARY_URL_FRAGMENT) ? summary() : Promise.resolve([]),
  );
}

describe('useRepositoryList firewall summary readiness', () => {
  beforeEach(() => {
    mockGet.mockReset();
    mockCheckPermission.mockReset().mockReturnValue(true);
    mockClmState.mockReset().mockReturnValue({ enabled: true });
  });

  it('shouldMarkSummaryReadyWhenFetchSucceeds', async () => {
    stubApi(() => Promise.resolve([{ repositoryName: 'a', message: 'Quarantine enabled' }]));
    const { result } = renderHook(() => useRepositoryList());
    await waitFor(() => expect(result.current.state.firewallLoaded).toBe(true));
    expect(result.current.state.firewallSummaryReady).toBe(true);
    expect(result.current.state.firewallStatus.a.message).toBe('Quarantine enabled');
  });

  it('shouldNotMarkSummaryReadyWhenFetchFails', async () => {
    stubApi(() => Promise.reject(new Error('500')));
    const { result } = renderHook(() => useRepositoryList());
    await waitFor(() => expect(result.current.state.firewallLoaded).toBe(true));
    expect(result.current.state.firewallSummaryReady).toBe(false);
    expect(result.current.state.firewallStatusError).toBeDefined();
  });

  it('shouldNotMarkSummaryReadyWhenCallerLacksReadPermission', async () => {
    mockCheckPermission.mockReturnValue(false);
    stubApi(() => Promise.resolve([]));
    const { result } = renderHook(() => useRepositoryList());
    await waitFor(() => expect(result.current.state.firewallLoaded).toBe(true));
    expect(result.current.state.firewallSummaryReady).toBe(false);
    expect(mockGet.mock.calls.some(([url]) => String(url).includes(SUMMARY_URL_FRAGMENT))).toBe(false);
  });

  it('shouldHideFirewallColumnWithoutReadPermission', () => {
    mockCheckPermission.mockReturnValue(false);
    stubApi(() => Promise.resolve([]));
    const { result } = renderHook(() => useRepositoryList());
    expect(result.current.showFirewallColumn).toBe(false);
  });

  it('shouldShowFirewallColumnWhenIqEnabledAndReadable', () => {
    stubApi(() => Promise.resolve([]));
    const { result } = renderHook(() => useRepositoryList());
    expect(result.current.showFirewallColumn).toBe(true);
  });

  it('shouldHideFirewallColumnWhenIqServerDisabled', () => {
    mockClmState.mockReturnValue({ enabled: false });
    stubApi(() => Promise.resolve([]));
    const { result } = renderHook(() => useRepositoryList());
    expect(result.current.showFirewallColumn).toBe(false);
  });
});
