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
import { useSideNavbarOpenState } from './useSideNavbarOpenState';

describe('useSideNavbarOpenState', () => {
  const setup = (initialOpen = true) => renderHook(() => useSideNavbarOpenState(initialOpen));

  it('should initialize correctly', () => {
    const { result } = setup(true);
    const [isOpen] = result.current;
    expect(isOpen).toBe(true);
  });

  it('should toggle open state with onToggleClick', () => {
    const { result } = setup(true);
    const [, onToggleClick] = result.current;

    act(() => {
      onToggleClick();
    });

    const [isOpen] = result.current;
    expect(isOpen).toBe(false);
  });

  describe('nx-sidebar-toggle window event', () => {
    it('should toggle isOpen when nx-sidebar-toggle event is dispatched without detail', () => {
      const { result } = setup(true);

      act(() => {
        window.dispatchEvent(new CustomEvent('nx-sidebar-toggle'));
      });

      const [isOpen] = result.current;
      expect(isOpen).toBe(false);
    });

    it('should set isOpen to true when nx-sidebar-toggle event has detail.open=true', () => {
      const { result } = setup(false);

      act(() => {
        window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: true } }));
      });

      const [isOpen] = result.current;
      expect(isOpen).toBe(true);
    });

    it('should set isOpen to false when nx-sidebar-toggle event has detail.open=false', () => {
      const { result } = setup(true);

      act(() => {
        window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: false } }));
      });

      const [isOpen] = result.current;
      expect(isOpen).toBe(false);
    });

    it('should remove the event listener on unmount', () => {
      const { result, unmount } = setup(true);

      unmount();

      expect(() => {
        window.dispatchEvent(new CustomEvent('nx-sidebar-toggle'));
      }).not.toThrow();

      const [isOpen] = result.current;
      expect(isOpen).toBe(true);
    });
  });
});
