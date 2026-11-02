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

import { debouncedValueOf, useDebouncedWithDiscard } from '../useDebouncedWithDiscard';

const DELAY = 300;

/** Advance timers inside `act` so the resulting state update is flushed before assertions. */
function advance(ms: number): void {
  act(() => {
    jest.advanceTimersByTime(ms);
  });
}

describe('debouncedValueOf', () => {
  it('reads the value back out of a token', () => {
    const { result } = renderHook(() => useDebouncedWithDiscard('commons', DELAY));

    expect(debouncedValueOf(result.current[0])).toBe('commons');
  });

  it('keeps a value that contains the separator intact', () => {
    const { result } = renderHook(() =>
      useDebouncedWithDiscard('org.apache.commons:commons-lang3', DELAY)
    );

    expect(debouncedValueOf(result.current[0])).toBe('org.apache.commons:commons-lang3');
  });

  it('keeps an empty value empty', () => {
    const { result } = renderHook(() => useDebouncedWithDiscard('', DELAY));

    expect(debouncedValueOf(result.current[0])).toBe('');
  });
});

describe('useDebouncedWithDiscard', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('withholds a new value until the delay has elapsed', () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedWithDiscard(value, DELAY),
      { initialProps: { value: 'a' } }
    );

    rerender({ value: 'ab' });
    expect(debouncedValueOf(result.current[0])).toBe('a');

    advance(DELAY - 1);
    expect(debouncedValueOf(result.current[0])).toBe('a');

    advance(1);
    expect(debouncedValueOf(result.current[0])).toBe('ab');
  });

  it('applies only the latest value after a rapid burst of changes', () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedWithDiscard(value, DELAY),
      { initialProps: { value: '' } }
    );

    for (const value of ['c', 'co', 'com', 'comm']) {
      rerender({ value });
      advance(DELAY - 1);
    }

    advance(1);
    expect(debouncedValueOf(result.current[0])).toBe('comm');
  });

  it('issues a new token when a value is re-entered after a discard', () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedWithDiscard(value, DELAY),
      { initialProps: { value: 'commons' } }
    );

    advance(DELAY);
    const settled = result.current[0];

    // Leave the value, discard whatever was derived from it, and come back to it inside a single
    // interval. The settled value is unchanged, so only the generation can distinguish this.
    rerender({ value: 'c' });
    act(() => {
      result.current[1]();
    });
    rerender({ value: 'commons' });
    advance(DELAY);

    expect(result.current[0]).not.toBe(settled);
    expect(debouncedValueOf(result.current[0])).toBe('commons');
  });

  it('does not change the token when nothing is discarded and the value settles unchanged', () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedWithDiscard(value, DELAY),
      { initialProps: { value: 'commons' } }
    );

    advance(DELAY);
    const settled = result.current[0];

    rerender({ value: 'commons' });
    advance(DELAY);

    expect(result.current[0]).toBe(settled);
  });

  it('keeps the discard callback stable across renders', () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedWithDiscard(value, DELAY),
      { initialProps: { value: 'a' } }
    );

    const discard = result.current[1];
    rerender({ value: 'b' });
    advance(DELAY);

    expect(result.current[1]).toBe(discard);
  });

  it('does not apply a pending value after unmount', () => {
    const { result, rerender, unmount } = renderHook(
      ({ value }) => useDebouncedWithDiscard(value, DELAY),
      { initialProps: { value: 'a' } }
    );

    rerender({ value: 'b' });
    const beforeUnmount = result.current[0];
    unmount();

    expect(() => advance(DELAY)).not.toThrow();
    expect(result.current[0]).toBe(beforeUnmount);
  });
});
