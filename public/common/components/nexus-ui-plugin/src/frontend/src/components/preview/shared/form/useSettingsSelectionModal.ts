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

import { useCallback, useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';
import {
  createSettingsSelectionModalMachine,
  deriveVisibleSelectionItems,
  type SettingsSelectionItem,
  type SettingsSelectionSortField,
  type SettingsSelectionSortDirection,
} from './settingsSelectionModalMachine';

// ViewModel (thin projection) over settingsSelectionModalMachine. Owns no state
// of its own; projects state.context into a view-facing API and exposes semantic
// commands (open, confirmAndClose, cancelAndClose, toggleItem, changeFilter,
// changeSort, changePage).

export interface UseSettingsSelectionModalOptions {
  availableItems: SettingsSelectionItem[];
  onConfirm: (selection: string[]) => void;
}

export interface UseSettingsSelectionModalResult {
  isOpen: boolean;
  visibleItems: SettingsSelectionItem[];
  totalItems: number;
  filteredCount: number;
  totalPages: number;
  currentPage: number;
  filter: string;
  sortField: SettingsSelectionSortField | null;
  sortDirection: SettingsSelectionSortDirection;
  tempSelection: string[];
  selectedCount: number;
  isSelected: (itemId: string) => boolean;
  open: (initialSelection: string[]) => void;
  cancelAndClose: () => void;
  confirmAndClose: () => void;
  toggleItem: (itemId: string) => void;
  changeFilter: (text: string) => void;
  changeSort: (
    field: SettingsSelectionSortField,
    direction: SettingsSelectionSortDirection,
  ) => void;
  changePage: (page: number) => void;
}

export function useSettingsSelectionModal(
  options: UseSettingsSelectionModalOptions,
): UseSettingsSelectionModalResult {
  const { availableItems, onConfirm } = options;

  // Latest onConfirm captured via ref so the machine, created once on mount, fires
  // the current callback without needing to be rebuilt. useMachine ignores machine
  // identity changes after mount, so rebuilding the machine is ineffective.
  const onConfirmRef = useRef(onConfirm);
  onConfirmRef.current = onConfirm;

  const machine = useMemo(
    () =>
      createSettingsSelectionModalMachine({
        onConfirm: (selection) => onConfirmRef.current(selection),
      }),
    [],
  );

  const [state, send] = useMachine(machine);
  const { context } = state;

  const derived = useMemo(
    () => deriveVisibleSelectionItems(context, availableItems),
    [context, availableItems],
  );

  const tempSelectionSet = useMemo(() => new Set(context.tempSelection), [context.tempSelection]);
  const isSelected = useCallback(
    (itemId: string) => tempSelectionSet.has(itemId),
    [tempSelectionSet],
  );

  const open = useCallback(
    (initialSelection: string[]) => send({ type: 'OPEN', initialSelection }),
    [send],
  );
  const cancelAndClose = useCallback(() => send({ type: 'CANCEL' }), [send]);
  const confirmAndClose = useCallback(() => send({ type: 'CONFIRM' }), [send]);
  const toggleItem = useCallback(
    (itemId: string) => send({ type: 'TOGGLE_ITEM', itemId }),
    [send],
  );
  const changeFilter = useCallback((text: string) => send({ type: 'FILTER', text }), [send]);
  const changeSort = useCallback(
    (field: SettingsSelectionSortField, direction: SettingsSelectionSortDirection) =>
      send({ type: 'SORT', field, direction }),
    [send],
  );
  const changePage = useCallback((page: number) => send({ type: 'PAGE_CHANGE', page }), [send]);

  return {
    isOpen: state.matches('open'),
    visibleItems: derived.visibleItems,
    totalItems: availableItems.length,
    filteredCount: derived.filteredCount,
    totalPages: derived.totalPages,
    currentPage: context.page,
    filter: context.filter,
    sortField: context.sortField,
    sortDirection: context.sortDirection,
    tempSelection: context.tempSelection,
    selectedCount: context.tempSelection.length,
    isSelected,
    open,
    cancelAndClose,
    confirmAndClose,
    toggleItem,
    changeFilter,
    changeSort,
    changePage,
  };
}
