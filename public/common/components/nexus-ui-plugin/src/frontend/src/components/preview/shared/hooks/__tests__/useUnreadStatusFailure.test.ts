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
import {
  useUnreadStatusFailure,
  resetUnreadStatusFailure,
  STATUS_BELL_ACK_STORAGE_KEY,
} from '../useUnreadStatusFailure';

describe('useUnreadStatusFailure', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('shows the dot when health checks are failing on mount and there is no prior acknowledgement', () => {
    const { result } = renderHook(() => useUnreadStatusFailure(true));
    expect(result.current.showDot).toBe(true);
  });

  it('hides the dot when health checks are passing on mount', () => {
    const { result } = renderHook(() => useUnreadStatusFailure(false));
    expect(result.current.showDot).toBe(false);
  });

  it('hides the dot when health checks are failing on mount but the state was acknowledged in a previous session', () => {
    localStorage.setItem(STATUS_BELL_ACK_STORAGE_KEY, 'true');

    const { result } = renderHook(() => useUnreadStatusFailure(true));

    expect(result.current.showDot).toBe(false);
  });

  it('markAcknowledged hides the dot and persists the acknowledgement', () => {
    const { result } = renderHook(() => useUnreadStatusFailure(true));
    expect(result.current.showDot).toBe(true);

    act(() => {
      result.current.markAcknowledged();
    });

    expect(result.current.showDot).toBe(false);
    expect(localStorage.getItem(STATUS_BELL_ACK_STORAGE_KEY)).toBe('true');
  });

  it('after acknowledgement, the dot stays hidden across re-renders while the state remains unhealthy', () => {
    const { result, rerender } = renderHook(
      ({ failed }: { failed: boolean }) => useUnreadStatusFailure(failed),
      { initialProps: { failed: true } },
    );

    act(() => {
      result.current.markAcknowledged();
    });
    expect(result.current.showDot).toBe(false);

    rerender({ failed: true });
    expect(result.current.showDot).toBe(false);
  });

  it('transition unhealthy → healthy clears the acknowledgement', () => {
    const { result, rerender } = renderHook(
      ({ failed }: { failed: boolean }) => useUnreadStatusFailure(failed),
      { initialProps: { failed: true } },
    );

    act(() => {
      result.current.markAcknowledged();
    });
    expect(localStorage.getItem(STATUS_BELL_ACK_STORAGE_KEY)).toBe('true');

    rerender({ failed: false });

    expect(result.current.showDot).toBe(false);
    expect(localStorage.getItem(STATUS_BELL_ACK_STORAGE_KEY)).toBeNull();
  });

  it('clears a stale acknowledgement when mounting healthy so it cannot silently suppress a later, unrelated failure', () => {
    // Simulates the tab-closed-during-recovery scenario: an old failure was
    // acknowledged, the backend recovered while the tab was closed (so the
    // unhealthy→healthy transition was never observed to clear the ack), and
    // the app now mounts healthy with a stale flag in localStorage.
    localStorage.setItem(STATUS_BELL_ACK_STORAGE_KEY, 'true');

    const { result } = renderHook(() => useUnreadStatusFailure(false));

    expect(result.current.showDot).toBe(false);
    expect(localStorage.getItem(STATUS_BELL_ACK_STORAGE_KEY)).toBeNull();
  });

  it('transition healthy → unhealthy re-alerts even if acknowledgement was previously set', () => {
    localStorage.setItem(STATUS_BELL_ACK_STORAGE_KEY, 'true');

    const { result, rerender } = renderHook(
      ({ failed }: { failed: boolean }) => useUnreadStatusFailure(failed),
      { initialProps: { failed: false } },
    );

    // Baseline: state was healthy on mount, dot is hidden.
    expect(result.current.showDot).toBe(false);

    // New failure lands.
    rerender({ failed: true });

    expect(result.current.showDot).toBe(true);
    expect(localStorage.getItem(STATUS_BELL_ACK_STORAGE_KEY)).toBeNull();
  });

  it('supports the full cycle: unhealthy → ack → healthy → unhealthy re-alerts', () => {
    const { result, rerender } = renderHook(
      ({ failed }: { failed: boolean }) => useUnreadStatusFailure(failed),
      { initialProps: { failed: true } },
    );

    expect(result.current.showDot).toBe(true);

    act(() => {
      result.current.markAcknowledged();
    });
    expect(result.current.showDot).toBe(false);

    rerender({ failed: false });
    expect(result.current.showDot).toBe(false);
    expect(localStorage.getItem(STATUS_BELL_ACK_STORAGE_KEY)).toBeNull();

    rerender({ failed: true });
    expect(result.current.showDot).toBe(true);
  });

  it('resetUnreadStatusFailure clears any persisted acknowledgement', () => {
    localStorage.setItem(STATUS_BELL_ACK_STORAGE_KEY, 'true');
    resetUnreadStatusFailure();
    expect(localStorage.getItem(STATUS_BELL_ACK_STORAGE_KEY)).toBeNull();
  });

  describe('when localStorage is unavailable', () => {
    let getItemSpy: jest.SpyInstance;
    let setItemSpy: jest.SpyInstance;
    let removeItemSpy: jest.SpyInstance;

    beforeEach(() => {
      getItemSpy = jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new Error('Storage disabled');
      });
      setItemSpy = jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('Storage disabled');
      });
      removeItemSpy = jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
        throw new Error('Storage disabled');
      });
    });

    afterEach(() => {
      getItemSpy.mockRestore();
      setItemSpy.mockRestore();
      removeItemSpy.mockRestore();
    });

    it('falls back to mirroring healthChecksFailed when reads throw', () => {
      const { result } = renderHook(() => useUnreadStatusFailure(true));
      expect(result.current.showDot).toBe(true);
    });

    it('does not throw when markAcknowledged fails to persist', () => {
      const { result } = renderHook(() => useUnreadStatusFailure(true));

      expect(() => {
        act(() => {
          result.current.markAcknowledged();
        });
      }).not.toThrow();

      // showDot flips to false in memory even though persistence failed.
      expect(result.current.showDot).toBe(false);
    });
  });
});
