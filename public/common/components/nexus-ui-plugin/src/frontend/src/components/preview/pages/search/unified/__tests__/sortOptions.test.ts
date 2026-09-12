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

import {
  SORT_OPTIONS,
  SORT_OPTION_GROUPS,
  compareBySort,
  findSortOption,
  parseSortValue,
  toSortValue,
} from '../sortOptions';
import type { SearchResult } from '../unified.types';

function result(overrides: Partial<SearchResult> = {}): SearchResult {
  return {
    id: 'id',
    name: 'name',
    format: 'npm',
    repository: 'repo',
    version: '1.0.0',
    ...overrides,
  };
}

describe('sortOptions', () => {
  describe('SORT_OPTIONS', () => {
    it('offers both directions for every sortable field', () => {
      expect(SORT_OPTIONS).toHaveLength(6);
      const fields = new Set(SORT_OPTIONS.map((o) => o.field));
      expect([...fields].sort()).toEqual(['lastUpdated', 'name', 'repository']);
      for (const field of fields) {
        const directions = SORT_OPTIONS.filter((o) => o.field === field).map((o) => o.direction);
        expect(directions.sort()).toEqual(['asc', 'desc']);
      }
    });

    it('states field and direction in every label', () => {
      // The dropdown's closed state renders `label` on its own, so each label
      // has to identify the complete selection without a group heading.
      const byValue = Object.fromEntries(SORT_OPTIONS.map((o) => [o.value, o.label]));
      expect(byValue).toEqual({
        'lastUpdated:desc': 'Last updated — Newest first',
        'lastUpdated:asc': 'Last updated — Oldest first',
        'name:asc': 'Name — A-Z',
        'name:desc': 'Name — Z-A',
        'repository:asc': 'Repository — A-Z',
        'repository:desc': 'Repository — Z-A',
      });
    });

    it('composes each label from the field and direction labels rather than restating them', () => {
      for (const option of SORT_OPTIONS) {
        // directionLabel is what the open list shows; the trigger label must be
        // the same text, so the two can never drift apart.
        expect(option.label.endsWith(`— ${option.directionLabel}`)).toBe(true);
      }
    });

    it('does not offer sorting by version', () => {
      // The server orders versions by a normalised column, which compareBySort
      // cannot reproduce from the raw version string in mock mode. The option is
      // deliberately not offered rather than behaving differently in mock mode.
      expect(SORT_OPTIONS.some((o) => o.field === 'version')).toBe(false);
    });

    it('lists every option with a unique value', () => {
      const values = SORT_OPTIONS.map((o) => o.value);
      expect(new Set(values).size).toBe(values.length);
    });

    it('lists last-updated newest-first as the first option (the default sort)', () => {
      expect(SORT_OPTIONS[0].value).toBe('lastUpdated:desc');
    });
  });

  describe('SORT_OPTION_GROUPS', () => {
    it('groups the same options by field without dropping or duplicating any', () => {
      const grouped = SORT_OPTION_GROUPS.flatMap((g) => g.options);
      expect(grouped).toHaveLength(SORT_OPTIONS.length);
      expect(new Set(grouped.map((o) => o.value))).toEqual(
        new Set(SORT_OPTIONS.map((o) => o.value)),
      );
      for (const group of SORT_OPTION_GROUPS) {
        expect(group.options.every((o) => o.field === group.field)).toBe(true);
      }
    });
  });

  describe('toSortValue / parseSortValue', () => {
    it('round-trips every option', () => {
      for (const option of SORT_OPTIONS) {
        expect(toSortValue(option.field, option.direction)).toBe(option.value);
        expect(parseSortValue(option.value)).toEqual({
          field: option.field,
          direction: option.direction,
        });
      }
    });

    it('rejects values not offered by the dropdown', () => {
      expect(parseSortValue('')).toBeUndefined();
      expect(parseSortValue('name')).toBeUndefined();
      expect(parseSortValue('name:sideways')).toBeUndefined();
      expect(parseSortValue('downloads:asc')).toBeUndefined();
      expect(parseSortValue('name:asc:extra')).toBeUndefined();
    });
  });

  describe('findSortOption', () => {
    it('finds the option for a field/direction pair', () => {
      expect(findSortOption('name', 'asc')?.value).toBe('name:asc');
    });

    it('returns undefined for a missing or unoffered pair', () => {
      expect(findSortOption(undefined, undefined)).toBeUndefined();
      expect(findSortOption('name', undefined)).toBeUndefined();
    });
  });

  describe('compareBySort (mock mode only)', () => {
    it('sorts names ascending and descending', () => {
      const a = result({ name: 'alpha' });
      const b = result({ name: 'beta' });
      expect(compareBySort(a, b, 'name', 'asc')).toBeLessThan(0);
      expect(compareBySort(a, b, 'name', 'desc')).toBeGreaterThan(0);
    });

    it('orders ISO timestamps chronologically', () => {
      const older = result({ lastUpdated: '2024-01-10T08:00:00Z' });
      const newer = result({ lastUpdated: '2024-06-15T10:00:00Z' });
      expect(compareBySort(older, newer, 'lastUpdated', 'asc')).toBeLessThan(0);
      expect(compareBySort(older, newer, 'lastUpdated', 'desc')).toBeGreaterThan(0);
    });

    it('treats equal values as equal', () => {
      expect(compareBySort(result(), result(), 'name', 'asc')).toBe(0);
      expect(compareBySort(result(), result(), 'name', 'desc')).toBe(0);
    });

    it('sorts results missing the value last in both directions', () => {
      const withValue = result({ lastUpdated: '2024-01-10T08:00:00Z' });
      const without = result({ lastUpdated: undefined });
      expect(compareBySort(without, withValue, 'lastUpdated', 'asc')).toBeGreaterThan(0);
      expect(compareBySort(without, withValue, 'lastUpdated', 'desc')).toBeGreaterThan(0);
      expect(compareBySort(withValue, without, 'lastUpdated', 'asc')).toBeLessThan(0);
      expect(compareBySort(withValue, without, 'lastUpdated', 'desc')).toBeLessThan(0);
    });

    it('produces a stable full ordering when used as an Array#sort comparator', () => {
      const results = [
        result({ id: '1', name: 'charlie' }),
        result({ id: '2', name: 'alpha' }),
        result({ id: '3', name: 'bravo' }),
      ];
      expect(
        [...results].sort((x, y) => compareBySort(x, y, 'name', 'asc')).map((r) => r.name),
      ).toEqual(['alpha', 'bravo', 'charlie']);
      expect(
        [...results].sort((x, y) => compareBySort(x, y, 'name', 'desc')).map((r) => r.name),
      ).toEqual(['charlie', 'bravo', 'alpha']);
    });
  });
});
