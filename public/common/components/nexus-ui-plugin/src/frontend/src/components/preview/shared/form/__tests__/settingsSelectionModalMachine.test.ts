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

import { interpret } from 'xstate';
import {
  createSettingsSelectionModalMachine,
  deriveVisibleSelectionItems,
  SELECTION_ROWS_PER_PAGE,
  type SettingsSelectionItem,
} from '../settingsSelectionModalMachine';

const ITEMS: SettingsSelectionItem[] = [
  { id: 'nx-repository-view-*-*-read', name: 'nx-repository-view-*-*-read', description: 'Read all repositories' },
  { id: 'nx-repository-view-maven2-*-read', name: 'nx-repository-view-maven2-*-read', description: 'Read maven repositories' },
  { id: 'nx-repository-admin-*-*-add', name: 'nx-repository-admin-*-*-add', description: 'Admin all repos' },
  { id: 'nx-application-write', name: 'nx-application-write', description: 'Write app config' },
  { id: 'nx-blobstore-read', name: 'nx-blobstore-read', description: 'Read blob stores' },
];

function buildLargeItemSet(count: number): SettingsSelectionItem[] {
  return Array.from({ length: count }, (_, i) => ({
    id: `item-${i}`,
    name: `Item ${String(i).padStart(3, '0')}`,
    description: `Description ${i}`,
  }));
}

describe('settingsSelectionModalMachine', () => {
  describe('initial state', () => {
    it('starts closed', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      expect(service.state.matches('closed')).toBe(true);
      expect(service.state.context.tempSelection).toEqual([]);
      service.stop();
    });

    it('defaults page size to 10', () => {
      expect(SELECTION_ROWS_PER_PAGE).toBe(10);
    });
  });

  describe('open', () => {
    it('transitions to open and seeds tempSelection from event payload', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: ['nx-application-write'] });
      expect(service.state.matches('open')).toBe(true);
      expect(service.state.context.tempSelection).toEqual(['nx-application-write']);
      service.stop();
    });

    it('resets filter, sort, and page on open', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: [] });
      service.send({ type: 'FILTER', text: 'maven' });
      service.send({ type: 'PAGE_CHANGE', page: 2 });
      service.send({ type: 'CANCEL' });
      service.send({ type: 'OPEN', initialSelection: ['nx-blobstore-read'] });
      expect(service.state.context.filter).toBe('');
      expect(service.state.context.page).toBe(0);
      expect(service.state.context.sortField).toBe('name');
      expect(service.state.context.sortDirection).toBe('asc');
      service.stop();
    });

    it('clones the incoming selection so caller mutations do not leak', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      const caller = ['nx-application-write'];
      service.send({ type: 'OPEN', initialSelection: caller });
      caller.push('nx-blobstore-read');
      expect(service.state.context.tempSelection).toEqual(['nx-application-write']);
      service.stop();
    });
  });

  describe('toggle item', () => {
    it('adds an unselected item to tempSelection', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: [] });
      service.send({ type: 'TOGGLE_ITEM', itemId: 'nx-application-write' });
      expect(service.state.context.tempSelection).toEqual(['nx-application-write']);
      service.stop();
    });

    it('removes a selected item from tempSelection', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: ['nx-application-write', 'nx-blobstore-read'] });
      service.send({ type: 'TOGGLE_ITEM', itemId: 'nx-application-write' });
      expect(service.state.context.tempSelection).toEqual(['nx-blobstore-read']);
      service.stop();
    });

    it('deduplicates repeated toggle-add of the same item', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: [] });
      service.send({ type: 'TOGGLE_ITEM', itemId: 'nx-application-write' });
      service.send({ type: 'TOGGLE_ITEM', itemId: 'nx-application-write' });
      service.send({ type: 'TOGGLE_ITEM', itemId: 'nx-application-write' });
      expect(service.state.context.tempSelection).toEqual(['nx-application-write']);
      service.stop();
    });
  });

  describe('cancel', () => {
    it('discards tempSelection and does NOT call onConfirm', () => {
      const onConfirm = jest.fn();
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: ['nx-blobstore-read'] });
      service.send({ type: 'TOGGLE_ITEM', itemId: 'nx-application-write' });
      service.send({ type: 'CANCEL' });
      expect(service.state.matches('closed')).toBe(true);
      expect(onConfirm).not.toHaveBeenCalled();
      service.stop();
    });
  });

  describe('confirm', () => {
    it('fires onConfirm with tempSelection and returns to closed', () => {
      const onConfirm = jest.fn();
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: [] });
      service.send({ type: 'TOGGLE_ITEM', itemId: 'nx-application-write' });
      service.send({ type: 'TOGGLE_ITEM', itemId: 'nx-blobstore-read' });
      service.send({ type: 'CONFIRM' });
      expect(service.state.matches('closed')).toBe(true);
      expect(onConfirm).toHaveBeenCalledWith(['nx-application-write', 'nx-blobstore-read']);
      service.stop();
    });

    it('fires onConfirm with an empty array when the user cleared everything', () => {
      const onConfirm = jest.fn();
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: ['nx-application-write'] });
      service.send({ type: 'TOGGLE_ITEM', itemId: 'nx-application-write' });
      service.send({ type: 'CONFIRM' });
      expect(onConfirm).toHaveBeenCalledWith([]);
      service.stop();
    });
  });

  describe('filter', () => {
    it('stores the raw filter text', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: [] });
      service.send({ type: 'FILTER', text: 'MAVEN' });
      expect(service.state.context.filter).toBe('MAVEN');
      service.stop();
    });

    it('resets page to 0 on filter change', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: [] });
      service.send({ type: 'PAGE_CHANGE', page: 3 });
      service.send({ type: 'FILTER', text: 'anything' });
      expect(service.state.context.page).toBe(0);
      service.stop();
    });
  });

  describe('sort', () => {
    it('assigns sort field + direction (name)', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: [] });
      service.send({ type: 'SORT', field: 'description', direction: 'desc' });
      expect(service.state.context.sortField).toBe('description');
      expect(service.state.context.sortDirection).toBe('desc');
      service.stop();
    });

    it('supports sort by the Select column (matches classic UI)', () => {
      const service = interpret(
        createSettingsSelectionModalMachine({ onConfirm: jest.fn() }),
      ).start();
      service.send({ type: 'OPEN', initialSelection: [] });
      service.send({ type: 'SORT', field: 'select', direction: 'asc' });
      expect(service.state.context.sortField).toBe('select');
      service.stop();
    });
  });
});

describe('deriveVisibleSelectionItems', () => {
  const baseContext = {
    filter: '',
    sortField: 'name' as const,
    sortDirection: 'asc' as const,
    page: 0,
    tempSelection: [] as string[],
  };

  it('filters by name (case-insensitive substring)', () => {
    const result = deriveVisibleSelectionItems({ ...baseContext, filter: 'MAVEN' }, ITEMS);
    expect(result.filteredCount).toBe(1);
    expect(result.visibleItems[0].id).toBe('nx-repository-view-maven2-*-read');
  });

  it('filters by description too', () => {
    const result = deriveVisibleSelectionItems({ ...baseContext, filter: 'blob' }, ITEMS);
    expect(result.filteredCount).toBe(1);
    expect(result.visibleItems[0].id).toBe('nx-blobstore-read');
  });

  it('treats * as a wildcard in the filter (name)', () => {
    // "nx-*-write" matches any nx-<something>-write item.
    const result = deriveVisibleSelectionItems({ ...baseContext, filter: 'nx-*-write' }, ITEMS);
    expect(result.filteredCount).toBe(1);
    expect(result.visibleItems[0].id).toBe('nx-application-write');
  });

  it('treats * as a wildcard across name and description', () => {
    // "Read *" matches descriptions "Read all repositories", "Read maven repositories", "Read blob stores".
    const result = deriveVisibleSelectionItems({ ...baseContext, filter: 'Read *' }, ITEMS);
    expect(result.filteredCount).toBe(3);
  });

  it('sorts by name ascending', () => {
    const result = deriveVisibleSelectionItems(baseContext, ITEMS);
    const names = result.visibleItems.map((i) => i.name);
    expect(names).toEqual([...names].sort());
  });

  it('sorts by name descending', () => {
    const result = deriveVisibleSelectionItems(
      { ...baseContext, sortDirection: 'desc' },
      ITEMS,
    );
    const names = result.visibleItems.map((i) => i.name);
    expect(names).toEqual([...names].sort().reverse());
  });

  it('sorts by Select puts selected items first (asc)', () => {
    const result = deriveVisibleSelectionItems(
      { ...baseContext, sortField: 'select', tempSelection: ['nx-blobstore-read'] },
      ITEMS,
    );
    expect(result.visibleItems[0].id).toBe('nx-blobstore-read');
  });

  it('sorts by Select puts unselected items first (desc)', () => {
    const result = deriveVisibleSelectionItems(
      {
        ...baseContext,
        sortField: 'select',
        sortDirection: 'desc',
        tempSelection: ['nx-blobstore-read'],
      },
      ITEMS,
    );
    expect(result.visibleItems[0].id).not.toBe('nx-blobstore-read');
  });

  it('paginates: returns page 0 with 10 items', () => {
    const large = buildLargeItemSet(35);
    const result = deriveVisibleSelectionItems(baseContext, large);
    expect(result.visibleItems).toHaveLength(10);
    expect(result.totalPages).toBe(4);
    expect(result.visibleItems[0].id).toBe('item-0');
    expect(result.visibleItems[9].id).toBe('item-9');
  });

  it('paginates: returns correct slice for a non-zero page', () => {
    const large = buildLargeItemSet(35);
    const result = deriveVisibleSelectionItems({ ...baseContext, page: 2 }, large);
    expect(result.visibleItems).toHaveLength(10);
    expect(result.visibleItems[0].id).toBe('item-20');
    expect(result.visibleItems[9].id).toBe('item-29');
  });

  it('clamps out-of-range page requests to the last page', () => {
    const large = buildLargeItemSet(15);
    const result = deriveVisibleSelectionItems({ ...baseContext, page: 99 }, large);
    expect(result.totalPages).toBe(2);
    expect(result.visibleItems).toHaveLength(5);
  });

  it('returns at least 1 total page even for an empty item list', () => {
    const result = deriveVisibleSelectionItems(baseContext, []);
    expect(result.totalPages).toBe(1);
    expect(result.filteredCount).toBe(0);
    expect(result.visibleItems).toEqual([]);
  });
});
