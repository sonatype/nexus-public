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

import { act, renderHook, waitFor } from '@testing-library/react';
import { useResourceMachine } from '../useResourceMachine';

const INITIAL: number[] = [];
const LOADED_DATA = [1, 2, 3];
const ERROR_MESSAGE = 'network down';

describe('useResourceMachine', () => {
  it('starts loading with initialData and resolves to loaded', async () => {
    const fetchFn = jest.fn().mockResolvedValue(LOADED_DATA);
    const { result } = renderHook(() =>
      useResourceMachine<number[]>({ id: 'test', initialData: INITIAL, fetch: fetchFn }),
    );

    expect(result.current.loading).toBe(true);
    expect(result.current.data).toEqual(INITIAL);

    await waitFor(() => expect(result.current.loaded).toBe(true));
    expect(result.current.data).toEqual(LOADED_DATA);
    expect(result.current.error).toBeNull();
    expect(fetchFn).toHaveBeenCalledTimes(1);
  });

  it('surfaces structured error + errorMessage projection on rejection', async () => {
    const fetchFn = jest.fn().mockRejectedValue(new Error(ERROR_MESSAGE));
    const { result } = renderHook(() =>
      useResourceMachine<number[]>({ id: 'test', initialData: INITIAL, fetch: fetchFn }),
    );

    await waitFor(() => expect(result.current.errorMessage).toBe(ERROR_MESSAGE));
    expect(result.current.error).toEqual({ status: 0, message: ERROR_MESSAGE });
    expect(result.current.loading).toBe(false);
    expect(result.current.loaded).toBe(false);
  });

  it('preserves ThrownApiError fields on structured error', async () => {
    const thrown = Object.assign(new Error(ERROR_MESSAGE), {
      status: 500,
      code: 'SERVER_ERROR',
      requestId: 'req-7',
    });
    const fetchFn = jest.fn().mockRejectedValue(thrown);
    const { result } = renderHook(() =>
      useResourceMachine<number[]>({ id: 'test', initialData: INITIAL, fetch: fetchFn }),
    );

    await waitFor(() => expect(result.current.errorMessage).toBe(ERROR_MESSAGE));
    expect(result.current.error).toEqual({
      status: 500,
      message: ERROR_MESSAGE,
      code: 'SERVER_ERROR',
      requestId: 'req-7',
    });
  });

  it('retry re-fetches after error and reaches loaded', async () => {
    let calls = 0;
    const fetchFn = jest.fn(() => {
      calls += 1;
      return calls === 1
        ? Promise.reject(new Error(ERROR_MESSAGE))
        : Promise.resolve(LOADED_DATA);
    });
    const { result } = renderHook(() =>
      useResourceMachine<number[]>({ id: 'test', initialData: INITIAL, fetch: fetchFn }),
    );

    await waitFor(() => expect(result.current.errorMessage).toBe(ERROR_MESSAGE));
    act(() => result.current.retry());
    await waitFor(() => expect(result.current.loaded).toBe(true));
    expect(result.current.data).toEqual(LOADED_DATA);
    expect(fetchFn).toHaveBeenCalledTimes(2);
  });

  it('refresh triggers another fetch from loaded', async () => {
    let calls = 0;
    const fetchFn = jest.fn(() => Promise.resolve([++calls]));
    const { result } = renderHook(() =>
      useResourceMachine<number[]>({ id: 'test', initialData: INITIAL, fetch: fetchFn }),
    );

    await waitFor(() => expect(result.current.loaded).toBe(true));
    expect(result.current.data).toEqual([1]);
    act(() => result.current.refresh());
    await waitFor(() => expect(result.current.data).toEqual([2]));
    expect(fetchFn).toHaveBeenCalledTimes(2);
  });

  it('captures late fetcher swaps via ref (machine identity is stable)', async () => {
    let latest = jest.fn().mockResolvedValue([10]);
    const { result, rerender } = renderHook(
      ({ fetchFn }: { fetchFn: () => Promise<number[]> }) =>
        useResourceMachine<number[]>({ id: 'test', initialData: INITIAL, fetch: fetchFn }),
      { initialProps: { fetchFn: latest } },
    );

    await waitFor(() => expect(result.current.loaded).toBe(true));
    expect(result.current.data).toEqual([10]);

    latest = jest.fn().mockResolvedValue([20]);
    rerender({ fetchFn: latest });
    act(() => result.current.refresh());

    await waitFor(() => expect(result.current.data).toEqual([20]));
    expect(latest).toHaveBeenCalledTimes(1);
  });
});
