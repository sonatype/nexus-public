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

import { assign, createMachine } from 'xstate';

// Model for the shared SettingsSelectionModal (searchable + paginated + sortable
// multi-select modal). Matches the classic UI's PrivilegesSelectionModal /
// RolesSelectionModal shape: 3 sortable columns (select / name / description),
// wildcard filter, 10 items per page. availableItems live outside context so
// async data sources can supply items after the machine has started.

export interface SettingsSelectionItem {
  id: string;
  name: string;
  description?: string;
}

export type SettingsSelectionSortField = 'select' | 'name' | 'description';
export type SettingsSelectionSortDirection = 'asc' | 'desc' | null;

export const SELECTION_ROWS_PER_PAGE = 10;

export interface SettingsSelectionContext {
  filter: string;
  sortField: SettingsSelectionSortField | null;
  sortDirection: SettingsSelectionSortDirection;
  page: number;
  tempSelection: string[];
}

export type SettingsSelectionEvent =
  | { type: 'OPEN'; initialSelection: string[] }
  | { type: 'CANCEL' }
  | { type: 'CONFIRM' }
  | { type: 'FILTER'; text: string }
  | {
      type: 'SORT';
      field: SettingsSelectionSortField;
      direction: SettingsSelectionSortDirection;
    }
  | { type: 'PAGE_CHANGE'; page: number }
  | { type: 'TOGGLE_ITEM'; itemId: string };

export interface SettingsSelectionMachineOptions {
  onConfirm: (selection: string[]) => void;
}

export function createSettingsSelectionModalMachine(options: SettingsSelectionMachineOptions) {
  const { onConfirm } = options;

  return createMachine<SettingsSelectionContext, SettingsSelectionEvent>(
    {
      id: 'settingsSelectionModal',
      initial: 'closed',
      context: {
        filter: '',
        sortField: 'name',
        sortDirection: 'asc',
        page: 0,
        tempSelection: [],
      },
      states: {
        closed: {
          on: {
            OPEN: { target: 'open', actions: 'resetForOpen' },
          },
        },
        open: {
          on: {
            CANCEL: 'closed',
            CONFIRM: { target: 'closed', actions: 'triggerOnConfirm' },
            FILTER: { actions: 'assignFilter' },
            SORT: { actions: 'assignSort' },
            PAGE_CHANGE: { actions: 'assignPage' },
            TOGGLE_ITEM: { actions: 'toggleItem' },
          },
        },
      },
    },
    {
      actions: {
        resetForOpen: assign<SettingsSelectionContext, SettingsSelectionEvent>({
          tempSelection: (_context, event) =>
            [...(event as { type: 'OPEN'; initialSelection: string[] }).initialSelection],
          filter: (_context) => '',
          sortField: (_context) => 'name',
          sortDirection: (_context) => 'asc',
          page: (_context) => 0,
        }),
        assignFilter: assign<SettingsSelectionContext, SettingsSelectionEvent>({
          filter: (_context, event) => (event as { type: 'FILTER'; text: string }).text,
          page: (_context) => 0,
        }),
        assignSort: assign<SettingsSelectionContext, SettingsSelectionEvent>({
          sortField: (_context, event) =>
            (event as { type: 'SORT'; field: SettingsSelectionSortField }).field,
          sortDirection: (_context, event) =>
            (event as { type: 'SORT'; direction: SettingsSelectionSortDirection }).direction,
        }),
        assignPage: assign<SettingsSelectionContext, SettingsSelectionEvent>({
          page: (_context, event) => (event as { type: 'PAGE_CHANGE'; page: number }).page,
        }),
        toggleItem: assign<SettingsSelectionContext, SettingsSelectionEvent>({
          tempSelection: (context, event) => {
            const { itemId } = event as { type: 'TOGGLE_ITEM'; itemId: string };
            const set = new Set(context.tempSelection);
            if (set.has(itemId)) {
              set.delete(itemId);
            } else {
              set.add(itemId);
            }
            return Array.from(set);
          },
        }),
        triggerOnConfirm: (context) => {
          onConfirm(context.tempSelection);
        },
      },
    },
  );
}

// Wildcard-aware substring match. `*` acts as "any characters". Case-insensitive.
// Empty filter matches everything.
function matchesFilter(text: string, rawFilter: string): boolean {
  const filter = rawFilter.trim().toLowerCase();
  if (!filter) return true;
  const haystack = text.toLowerCase();
  if (!filter.includes('*')) return haystack.includes(filter);
  const escapedButKeepStar = filter.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*');
  return new RegExp(escapedButKeepStar).test(haystack);
}

// Pure derivation helper: filter → sort → paginate. availableItems and the current
// selection are separate args (not part of machine context) so items sourced from
// async fetches can arrive after the machine is running, and sort-by-select can
// reflect the live temp selection without machine round-trips.
export function deriveVisibleSelectionItems(
  context: SettingsSelectionContext,
  availableItems: SettingsSelectionItem[],
): {
  filteredCount: number;
  totalPages: number;
  visibleItems: SettingsSelectionItem[];
} {
  const { filter, sortField, sortDirection, page, tempSelection } = context;

  const filtered = availableItems.filter(
    (item) =>
      matchesFilter(item.name, filter) || matchesFilter(item.description ?? '', filter),
  );

  let sorted: SettingsSelectionItem[];
  if (sortField && sortDirection) {
    const dir = sortDirection === 'asc' ? 1 : -1;
    if (sortField === 'select') {
      const selectedSet = new Set(tempSelection);
      sorted = [...filtered].sort((a, b) => {
        const aSelected = selectedSet.has(a.id) ? 1 : 0;
        const bSelected = selectedSet.has(b.id) ? 1 : 0;
        if (aSelected !== bSelected) return (bSelected - aSelected) * dir;
        return a.name.localeCompare(b.name);
      });
    } else {
      sorted = [...filtered].sort((a, b) => {
        const av = (a[sortField] ?? '').toString().toLowerCase();
        const bv = (b[sortField] ?? '').toString().toLowerCase();
        if (av < bv) return -1 * dir;
        if (av > bv) return 1 * dir;
        return 0;
      });
    }
  } else {
    sorted = filtered;
  }

  const totalPages = Math.max(1, Math.ceil(sorted.length / SELECTION_ROWS_PER_PAGE));
  const clampedPage = Math.min(page, totalPages - 1);
  const start = clampedPage * SELECTION_ROWS_PER_PAGE;
  const visibleItems = sorted.slice(start, start + SELECTION_ROWS_PER_PAGE);

  return { filteredCount: sorted.length, totalPages, visibleItems };
}
