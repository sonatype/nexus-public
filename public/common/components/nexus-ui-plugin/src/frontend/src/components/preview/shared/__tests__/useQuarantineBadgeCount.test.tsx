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

import { act, renderHook } from '@testing-library/react';
import { ExtJS } from '../../../../interface/ExtJS';
import { restClient } from '../../../../interface/api';
import { __resetQuarantineBadgeStoreForTests, useQuarantineBadgeCount } from '../useQuarantineBadgeCount';

const mockTier = jest.fn();

jest.mock('../useFirewallSecurityPagesTier', () => ({
  useFirewallSecurityPagesTier: () => mockTier(),
}));

jest.mock('../hooks/useHasUser', () => ({ useHasUser: () => true }));

jest.mock('../../../../interface/api', () => ({
  restClient: { get: jest.fn() },
  ENDPOINTS: { FIREWALL_QUARANTINE_SUMMARY: '/quarantine-summary' },
}));

const mockGet = restClient.get as jest.Mock;
let canRead = true;

const setVisibility = (state: 'visible' | 'hidden') => {
  Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => state });
  document.dispatchEvent(new Event('visibilitychange'));
};

const flush = () =>
  act(async () => {
    await Promise.resolve();
    await Promise.resolve();
  });

const advance = (ms: number) =>
  act(async () => {
    jest.advanceTimersByTime(ms);
    await Promise.resolve();
    await Promise.resolve();
  });

beforeEach(() => {
  jest.useFakeTimers();
  jest.clearAllMocks();
  __resetQuarantineBadgeStoreForTests();
  canRead = true;
  mockTier.mockReturnValue('live');
  mockGet.mockResolvedValue({ quarantinedComponentCount: 3 });
  jest.spyOn(ExtJS, 'usePermission').mockImplementation((getValue: () => boolean) => getValue());
  jest.spyOn(ExtJS, 'checkPermission').mockImplementation(() => canRead);
  setVisibility('visible');
});

afterEach(() => {
  __resetQuarantineBadgeStoreForTests();
  jest.useRealTimers();
});

describe('useQuarantineBadgeCount', () => {
  it('does not fetch and returns null when the tier is not live', async () => {
    mockTier.mockReturnValue('preview-pro');
    const { result } = renderHook(() => useQuarantineBadgeCount());
    await flush();

    expect(mockGet).not.toHaveBeenCalled();
    expect(result.current).toBeNull();
  });

  it('does not fetch when active is false', async () => {
    const { result } = renderHook(() => useQuarantineBadgeCount(false));
    await flush();
    await advance(120_000);

    expect(mockGet).not.toHaveBeenCalled();
    expect(result.current).toBeNull();
  });

  it('does not fetch without nexus:firewall-quarantine:read', async () => {
    canRead = false;
    const { result } = renderHook(() => useQuarantineBadgeCount());
    await flush();

    expect(ExtJS.checkPermission).toHaveBeenCalledWith('nexus:firewall-quarantine:read');
    expect(mockGet).not.toHaveBeenCalled();
    expect(result.current).toBeNull();
  });

  it('fetches once on mount and returns the count', async () => {
    const { result } = renderHook(() => useQuarantineBadgeCount());
    await flush();

    expect(mockGet).toHaveBeenCalledTimes(1);
    expect(mockGet).toHaveBeenCalledWith('/quarantine-summary');
    expect(result.current).toBe(3);
  });

  it('polls every 60 seconds and reflects the new count', async () => {
    const { result } = renderHook(() => useQuarantineBadgeCount());
    await flush();

    mockGet.mockResolvedValue({ quarantinedComponentCount: 8 });
    await advance(59_999);
    expect(mockGet).toHaveBeenCalledTimes(1);
    await advance(1);

    expect(mockGet).toHaveBeenCalledTimes(2);
    expect(result.current).toBe(8);
  });

  it('never overlaps requests: the next poll is scheduled 60 seconds after the previous one settles', async () => {
    let resolveFirst: (v: unknown) => void = () => undefined;
    mockGet.mockReturnValueOnce(new Promise((resolve) => {
      resolveFirst = resolve;
    }));
    renderHook(() => useQuarantineBadgeCount());
    await flush();

    await advance(180_000);
    expect(mockGet).toHaveBeenCalledTimes(1);

    await act(async () => {
      resolveFirst({ quarantinedComponentCount: 1 });
    });
    await advance(59_999);
    expect(mockGet).toHaveBeenCalledTimes(1);
    await advance(1);
    expect(mockGet).toHaveBeenCalledTimes(2);
  });

  it('shares one poller between subscribers', async () => {
    const first = renderHook(() => useQuarantineBadgeCount());
    const second = renderHook(() => useQuarantineBadgeCount());
    await flush();

    expect(mockGet).toHaveBeenCalledTimes(1);
    expect(first.result.current).toBe(3);
    expect(second.result.current).toBe(3);

    await advance(60_000);
    expect(mockGet).toHaveBeenCalledTimes(2);
  });

  it('keeps polling until the last subscriber leaves, then stops', async () => {
    const first = renderHook(() => useQuarantineBadgeCount());
    const second = renderHook(() => useQuarantineBadgeCount());
    await flush();

    first.unmount();
    await advance(60_000);
    expect(mockGet).toHaveBeenCalledTimes(2);

    second.unmount();
    await advance(300_000);
    expect(mockGet).toHaveBeenCalledTimes(2);
  });

  it('stops and returns null when the tier stops being live', async () => {
    const { result, rerender } = renderHook(() => useQuarantineBadgeCount());
    await flush();
    expect(result.current).toBe(3);

    mockTier.mockReturnValue('error');
    rerender();
    await advance(300_000);

    expect(result.current).toBeNull();
    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('pauses while the document is hidden and fetches immediately when it becomes visible', async () => {
    renderHook(() => useQuarantineBadgeCount());
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(1);

    act(() => setVisibility('hidden'));
    await advance(300_000);
    expect(mockGet).toHaveBeenCalledTimes(1);

    act(() => setVisibility('visible'));
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(2);

    await advance(60_000);
    expect(mockGet).toHaveBeenCalledTimes(3);
  });

  it('does not refetch on becoming visible within the poll interval, then fetches when the remainder elapses', async () => {
    renderHook(() => useQuarantineBadgeCount());
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(1);

    await advance(20_000);
    act(() => setVisibility('hidden'));
    act(() => setVisibility('visible'));
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(1);

    await advance(39_000);
    expect(mockGet).toHaveBeenCalledTimes(1);

    await advance(1_000);
    expect(mockGet).toHaveBeenCalledTimes(2);
  });

  it('fetches only once in a poll window across rapid hide/show cycles', async () => {
    renderHook(() => useQuarantineBadgeCount());
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(1);

    for (let i = 0; i < 3; i++) {
      await advance(5_000);
      act(() => setVisibility('hidden'));
      act(() => setVisibility('visible'));
      await flush();
    }
    expect(mockGet).toHaveBeenCalledTimes(1);

    await advance(45_000);
    expect(mockGet).toHaveBeenCalledTimes(2);
  });

  it('returns null on a 404 and stops polling', async () => {
    mockGet.mockRejectedValue(Object.assign(new Error('Not Found'), { response: { status: 404 } }));
    const { result } = renderHook(() => useQuarantineBadgeCount());
    await flush();

    expect(result.current).toBeNull();
    expect(mockGet).toHaveBeenCalledTimes(1);

    await advance(300_000);
    expect(mockGet).toHaveBeenCalledTimes(1);
    expect(result.current).toBeNull();
  });

  it('does not refetch on becoming visible after a 404, even once the poll interval has elapsed', async () => {
    mockGet.mockRejectedValue(Object.assign(new Error('Not Found'), { response: { status: 404 } }));
    renderHook(() => useQuarantineBadgeCount());
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(1);

    act(() => setVisibility('hidden'));
    await advance(120_000);
    act(() => setVisibility('visible'));
    await flush();
    await advance(300_000);

    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it.each([
    ['a 403', Object.assign(new Error('Forbidden'), { response: { status: 403 } })],
    ['a network error', new Error('Network Error')],
  ])('returns null on %s and keeps polling', async (_label, error) => {
    mockGet.mockRejectedValueOnce(error);
    const { result } = renderHook(() => useQuarantineBadgeCount());
    await flush();

    expect(result.current).toBeNull();

    await advance(60_000);
    expect(mockGet).toHaveBeenCalledTimes(2);
    expect(result.current).toBe(3);
  });

  it('drops a response that arrives after the tier stops being live', async () => {
    let resolveGet: (v: unknown) => void = () => undefined;
    mockGet.mockReturnValueOnce(new Promise((r) => {
      resolveGet = r;
    }));
    const { result, rerender } = renderHook(() => useQuarantineBadgeCount());
    await flush();
    mockTier.mockReturnValue('hidden');
    rerender();
    await act(async () => {
      resolveGet({ quarantinedComponentCount: 7 });
    });
    expect(result.current).toBeNull();
    await advance(300_000);
    expect(mockGet).toHaveBeenCalledTimes(1);
  });

  it('does not fetch when mounted hidden, then fetches once when shown', async () => {
    act(() => setVisibility('hidden'));
    const { result } = renderHook(() => useQuarantineBadgeCount());
    await flush();
    await advance(300_000);
    expect(mockGet).not.toHaveBeenCalled();
    act(() => setVisibility('visible'));
    await flush();
    expect(mockGet).toHaveBeenCalledTimes(1);
    expect(result.current).toBe(3);
  });

  it('returns null for a malformed response', async () => {
    mockGet.mockResolvedValue({});
    const { result } = renderHook(() => useQuarantineBadgeCount());
    await flush();

    expect(result.current).toBeNull();
  });
});
