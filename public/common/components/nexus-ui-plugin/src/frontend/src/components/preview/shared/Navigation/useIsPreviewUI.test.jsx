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
import { useIsPreviewUI } from './useIsPreviewUI';

describe('useIsPreviewUI', () => {
  beforeEach(() => {
    // Reset hash before each test
    window.location.hash = '';
  });

  afterEach(() => {
    window.location.hash = '';
  });

  it('returns false when hash does not start with #preview', () => {
    window.location.hash = '#browse/welcome';
    const { result } = renderHook(() => useIsPreviewUI());
    expect(result.current).toBe(false);
  });

  it('returns true when hash starts with #preview', () => {
    window.location.hash = '#preview/browse/welcome';
    const { result } = renderHook(() => useIsPreviewUI());
    expect(result.current).toBe(true);
  });

  it('returns false for empty hash', () => {
    window.location.hash = '';
    const { result } = renderHook(() => useIsPreviewUI());
    expect(result.current).toBe(false);
  });

  it('updates when hash changes to preview', () => {
    window.location.hash = '#browse/welcome';
    const { result } = renderHook(() => useIsPreviewUI());
    expect(result.current).toBe(false);

    act(() => {
      window.location.hash = '#preview/browse/welcome';
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });

    expect(result.current).toBe(true);
  });

  it('updates when hash changes away from preview', () => {
    window.location.hash = '#preview/browse/welcome';
    const { result } = renderHook(() => useIsPreviewUI());
    expect(result.current).toBe(true);

    act(() => {
      window.location.hash = '#browse/welcome';
      window.dispatchEvent(new HashChangeEvent('hashchange'));
    });

    expect(result.current).toBe(false);
  });

  it('cleans up event listener on unmount', () => {
    const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
    const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');

    const { unmount } = renderHook(() => useIsPreviewUI());

    expect(addEventListenerSpy).toHaveBeenCalledWith('hashchange', expect.any(Function));

    unmount();

    expect(removeEventListenerSpy).toHaveBeenCalledWith('hashchange', expect.any(Function));

    addEventListenerSpy.mockRestore();
    removeEventListenerSpy.mockRestore();
  });
});