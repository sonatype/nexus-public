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

import { renderHook, act } from '@testing-library/react';
import { useServerRestart } from '../useServerRestart';
import { NEXUS_SERVER_RESTARTED_EVENT } from '../../../../../interface/api/serverRestartBroadcast';

describe('useServerRestart', () => {
  const originalLocation = window.location;

  beforeEach(() => {
    // confirmReload drives the machine into reloading, whose entry action calls
    // window.location.reload(); jsdom does not implement navigation, so stub it.
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { ...originalLocation, reload: jest.fn() },
    });
  });

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: originalLocation,
    });
  });

  it('starts with the modal hidden and not reloading', () => {
    const { result } = renderHook(() => useServerRestart());
    expect(result.current.isRestarted).toBe(false);
    expect(result.current.isReloading).toBe(false);
  });

  it('shows the modal when the server-restarted window event fires', () => {
    const { result } = renderHook(() => useServerRestart());

    act(() => {
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RESTARTED_EVENT));
    });

    expect(result.current.isRestarted).toBe(true);
    expect(result.current.isReloading).toBe(false);
  });

  it('enters the reloading state when confirmReload is called', () => {
    const { result } = renderHook(() => useServerRestart());

    act(() => {
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RESTARTED_EVENT));
    });
    act(() => {
      result.current.confirmReload();
    });

    expect(result.current.isRestarted).toBe(true);
    expect(result.current.isReloading).toBe(true);
  });

  it('ignores the window event after unmount (subscription torn down)', () => {
    const { result, unmount } = renderHook(() => useServerRestart());
    unmount();

    expect(() =>
      window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RESTARTED_EVENT)),
    ).not.toThrow();
    expect(result.current.isRestarted).toBe(false);
  });
});
