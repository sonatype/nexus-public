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

import { renderHook, } from '@testing-library/react';
import {
  useUnsavedChangesWarning,
  clearDirtyState,
  hasUnsavedChanges,
} from '../useUnsavedChangesWarning';

describe('useUnsavedChangesWarning', () => {
  beforeEach(() => {
    // Reset window.dirty before each test
    window.dirty = [];
  });

  afterEach(() => {
    // Cleanup
    window.dirty = [];
  });

  it('initializes window.dirty if not present', () => {
    delete (window as Record<string, any>).dirty;

    renderHook(() => useUnsavedChangesWarning(false, 'test-form'));

    expect(window.dirty).toEqual([]);
  });

  it('adds formId to window.dirty when isDirty is true', () => {
    renderHook(() => useUnsavedChangesWarning(true, 'test-form'));

    expect(window.dirty).toContain('test-form');
  });

  it('removes formId from window.dirty when isDirty becomes false', () => {
    const { rerender } = renderHook(
      ({ isDirty }) => useUnsavedChangesWarning(isDirty, 'test-form'),
      { initialProps: { isDirty: true } }
    );

    expect(window.dirty).toContain('test-form');

    rerender({ isDirty: false });

    expect(window.dirty).not.toContain('test-form');
  });

  it('removes formId from window.dirty on unmount to prevent stale entries', () => {
    const { unmount } = renderHook(() =>
      useUnsavedChangesWarning(true, 'test-form')
    );

    expect(window.dirty).toContain('test-form');

    unmount();

    expect(window.dirty).not.toContain('test-form');
  });

  it('does not duplicate formId in window.dirty', () => {
    const { rerender } = renderHook(
      ({ isDirty }) => useUnsavedChangesWarning(isDirty, 'test-form'),
      { initialProps: { isDirty: true } }
    );

    // Trigger multiple re-renders
    rerender({ isDirty: true });
    rerender({ isDirty: true });

    const count = window.dirty!.filter((id) => id === 'test-form').length;
    expect(count).toBe(1);
  });

  it('supports multiple forms', () => {
    renderHook(() => useUnsavedChangesWarning(true, 'form-1'));
    renderHook(() => useUnsavedChangesWarning(true, 'form-2'));

    expect(window.dirty).toContain('form-1');
    expect(window.dirty).toContain('form-2');
    expect(window.dirty).toHaveLength(2);
  });

  it('adds beforeunload listener when isDirty is true', () => {
    const addEventListenerSpy = jest.spyOn(window, 'addEventListener');

    renderHook(() => useUnsavedChangesWarning(true, 'test-form'));

    expect(addEventListenerSpy).toHaveBeenCalledWith(
      'beforeunload',
      expect.any(Function)
    );

    addEventListenerSpy.mockRestore();
  });

  it('removes beforeunload listener on cleanup', () => {
    const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');

    const { unmount } = renderHook(() =>
      useUnsavedChangesWarning(true, 'test-form')
    );

    unmount();

    expect(removeEventListenerSpy).toHaveBeenCalledWith(
      'beforeunload',
      expect.any(Function)
    );

    removeEventListenerSpy.mockRestore();
  });
});

describe('clearDirtyState', () => {
  beforeEach(() => {
    window.dirty = ['form-1', 'form-2', 'form-3'];
  });

  it('removes the specified formId from window.dirty', () => {
    clearDirtyState('form-2');

    expect(window.dirty).toEqual(['form-1', 'form-3']);
  });

  it('does nothing if formId is not in window.dirty', () => {
    clearDirtyState('non-existent');

    expect(window.dirty).toEqual(['form-1', 'form-2', 'form-3']);
  });

  it('handles undefined window.dirty gracefully', () => {
    delete (window as Record<string, any>).dirty;

    expect(() => clearDirtyState('form-1')).not.toThrow();
  });
});

describe('hasUnsavedChanges', () => {
  it('returns true when window.dirty has items', () => {
    window.dirty = ['form-1'];

    expect(hasUnsavedChanges()).toBe(true);
  });

  it('returns false when window.dirty is empty', () => {
    window.dirty = [];

    expect(hasUnsavedChanges()).toBe(false);
  });

  it('returns false when window.dirty is undefined', () => {
    delete (window as Record<string, any>).dirty;

    // The module initializes window.dirty at load time, but if someone
    // deletes it, hasUnsavedChanges should still return falsy
    const result = hasUnsavedChanges();
    expect(result).toBeFalsy();
  });
});
