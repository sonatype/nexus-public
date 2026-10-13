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
import { useSettingsSelectionModal } from '../useSettingsSelectionModal';
import type { SettingsSelectionItem } from '../settingsSelectionModalMachine';

const ITEMS: SettingsSelectionItem[] = [
  { id: 'a', name: 'Alpha', description: 'first' },
  { id: 'b', name: 'Bravo', description: 'second' },
  { id: 'c', name: 'Charlie', description: 'third' },
  { id: 'd', name: 'Delta', description: 'fourth' },
];

describe('useSettingsSelectionModal', () => {
  it('starts closed with an empty tempSelection', () => {
    const { result } = renderHook(() =>
      useSettingsSelectionModal({ availableItems: ITEMS, onConfirm: jest.fn() }),
    );
    expect(result.current.isOpen).toBe(false);
    expect(result.current.tempSelection).toEqual([]);
    expect(result.current.selectedCount).toBe(0);
  });

  it('opens with the seeded initial selection', () => {
    const { result } = renderHook(() =>
      useSettingsSelectionModal({ availableItems: ITEMS, onConfirm: jest.fn() }),
    );
    act(() => result.current.open(['a', 'c']));
    expect(result.current.isOpen).toBe(true);
    expect(result.current.tempSelection).toEqual(['a', 'c']);
    expect(result.current.selectedCount).toBe(2);
  });

  it('toggling an item flips its selected state without closing the modal', () => {
    const { result } = renderHook(() =>
      useSettingsSelectionModal({ availableItems: ITEMS, onConfirm: jest.fn() }),
    );
    act(() => result.current.open([]));
    act(() => result.current.toggleItem('b'));
    expect(result.current.isSelected('b')).toBe(true);
    expect(result.current.isOpen).toBe(true);
    act(() => result.current.toggleItem('b'));
    expect(result.current.isSelected('b')).toBe(false);
  });

  it('confirm calls onConfirm with the temp selection and closes', () => {
    const onConfirm = jest.fn();
    const { result } = renderHook(() => useSettingsSelectionModal({ availableItems: ITEMS, onConfirm }));
    act(() => result.current.open(['a']));
    act(() => result.current.toggleItem('c'));
    act(() => result.current.confirmAndClose());
    expect(onConfirm).toHaveBeenCalledWith(['a', 'c']);
    expect(result.current.isOpen).toBe(false);
  });

  it('cancel discards the temp selection and does not call onConfirm', () => {
    const onConfirm = jest.fn();
    const { result } = renderHook(() => useSettingsSelectionModal({ availableItems: ITEMS, onConfirm }));
    act(() => result.current.open(['a']));
    act(() => result.current.toggleItem('c'));
    act(() => result.current.cancelAndClose());
    expect(onConfirm).not.toHaveBeenCalled();
    expect(result.current.isOpen).toBe(false);
  });

  it('changing the filter resets the current page to 0', () => {
    const { result } = renderHook(() =>
      useSettingsSelectionModal({ availableItems: ITEMS, onConfirm: jest.fn() }),
    );
    act(() => result.current.open([]));
    act(() => result.current.changePage(1));
    act(() => result.current.changeFilter('a'));
    expect(result.current.currentPage).toBe(0);
  });

  it('changing sort field updates sortField and sortDirection', () => {
    const { result } = renderHook(() =>
      useSettingsSelectionModal({ availableItems: ITEMS, onConfirm: jest.fn() }),
    );
    act(() => result.current.open([]));
    act(() => result.current.changeSort('description', 'desc'));
    expect(result.current.sortField).toBe('description');
    expect(result.current.sortDirection).toBe('desc');
  });

  it('re-opening with a different selection resets the previous tempSelection', () => {
    const { result } = renderHook(() =>
      useSettingsSelectionModal({ availableItems: ITEMS, onConfirm: jest.fn() }),
    );
    act(() => result.current.open(['a']));
    act(() => result.current.toggleItem('b'));
    act(() => result.current.cancelAndClose());
    act(() => result.current.open(['c']));
    expect(result.current.tempSelection).toEqual(['c']);
  });

  // Regression: async reference-data hooks render the modal with availableItems=[]
  // on first paint, then repopulate after fetch resolves. If availableItems is
  // baked into the machine at construction the modal stays empty forever.
  it('reflects availableItems that arrive after mount', () => {
    const { result, rerender } = renderHook(
      ({ items }: { items: SettingsSelectionItem[] }) =>
        useSettingsSelectionModal({ availableItems: items, onConfirm: jest.fn() }),
      { initialProps: { items: [] as SettingsSelectionItem[] } },
    );
    expect(result.current.visibleItems).toEqual([]);
    expect(result.current.totalItems).toBe(0);
    rerender({ items: ITEMS });
    expect(result.current.totalItems).toBe(ITEMS.length);
    expect(result.current.visibleItems.map((i) => i.id)).toEqual(['a', 'b', 'c', 'd']);
  });

  it('invokes the latest onConfirm reference when Confirm is dispatched', () => {
    const firstOnConfirm = jest.fn();
    const secondOnConfirm = jest.fn();
    const { result, rerender } = renderHook(
      ({ onConfirm }: { onConfirm: (sel: string[]) => void }) =>
        useSettingsSelectionModal({ availableItems: ITEMS, onConfirm }),
      { initialProps: { onConfirm: firstOnConfirm } },
    );
    act(() => result.current.open(['a']));
    rerender({ onConfirm: secondOnConfirm });
    act(() => result.current.confirmAndClose());
    expect(firstOnConfirm).not.toHaveBeenCalled();
    expect(secondOnConfirm).toHaveBeenCalledWith(['a']);
  });
});
