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

import React, { useCallback, useMemo } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { Badge, Box, Flex, Heading, Text, TextField, Theme } from '@radix-ui/themes';
import { ArrowDown, ArrowUp, ArrowUpDown, X, Search } from 'lucide-react';

import { SettingsButton } from './SettingsButton';
import type {
  SettingsSelectionSortField,
  SettingsSelectionSortDirection,
} from './settingsSelectionModalMachine';
import type { UseSettingsSelectionModalResult } from './useSettingsSelectionModal';
import { SETTINGS_SELECTION_MODAL } from './SettingsSelectionModalStrings';

import './SettingsSelectionModal.scss';

export interface SettingsSelectionModalProps {
  picker: UseSettingsSelectionModalResult;
  title: string;
  /** Column label for the entity name; e.g. "Name" (default). */
  nameColumnLabel?: string;
  /** Column label for the description column; e.g. "Description" (default). */
  descriptionColumnLabel?: string;
  /** Hide the Description column entirely (e.g. Contained Roles picker). */
  showDescriptionColumn?: boolean;
  /** Empty-state message when no items match the filter. */
  emptyMessage?: string;
  /** Optional data-testid on the dialog content. */
  testId?: string;
}

const MAX_PAGE_TOKENS = 7;

// Compute a compact list of page tokens: numbers plus 'gap' placeholders where
// the range is elided. Always includes first, last, current, current-1, current+1.
function computePageTokens(currentPage: number, totalPages: number): Array<number | 'gap-left' | 'gap-right'> {
  if (totalPages <= MAX_PAGE_TOKENS) {
    return Array.from({ length: totalPages }, (_, i) => i);
  }
  const tokens: Array<number | 'gap-left' | 'gap-right'> = [];
  const first = 0;
  const last = totalPages - 1;
  tokens.push(first);
  const windowStart = Math.max(1, currentPage - 1);
  const windowEnd = Math.min(last - 1, currentPage + 1);
  if (windowStart > 1) tokens.push('gap-left');
  for (let p = windowStart; p <= windowEnd; p++) tokens.push(p);
  if (windowEnd < last - 1) tokens.push('gap-right');
  tokens.push(last);
  return tokens;
}

// Shared searchable + paginated + sortable multi-select modal. Matches the
// classic UI's PrivilegesSelectionModal / RolesSelectionModal shape: 3 sortable
// columns (Select / Name / Description), wildcard filter with hint, 10 rows per
// page, page-number pagination, Cancel / Confirm footer. Consumers bind to it
// via useSettingsSelectionModal and open it through picker.open(currentSelection).
export function SettingsSelectionModal({
  picker,
  title,
  nameColumnLabel = SETTINGS_SELECTION_MODAL.COLUMN_NAME,
  descriptionColumnLabel = SETTINGS_SELECTION_MODAL.COLUMN_DESCRIPTION,
  showDescriptionColumn = true,
  emptyMessage,
  testId,
}: SettingsSelectionModalProps) {
  const {
    isOpen,
    visibleItems,
    filteredCount,
    totalPages,
    currentPage,
    filter,
    sortField,
    sortDirection,
    selectedCount,
    isSelected,
    cancelAndClose,
    confirmAndClose,
    toggleItem,
    changeFilter,
    changeSort,
    changePage,
  } = picker;

  const nextDirectionFor = useCallback(
    (field: SettingsSelectionSortField): SettingsSelectionSortDirection => {
      if (sortField !== field) return 'asc';
      return sortDirection === 'asc' ? 'desc' : 'asc';
    },
    [sortField, sortDirection],
  );

  const handleSortClick = useCallback(
    (field: SettingsSelectionSortField) => () => changeSort(field, nextDirectionFor(field)),
    [changeSort, nextDirectionFor],
  );

  const sortIconFor = useCallback(
    (field: SettingsSelectionSortField) => {
      if (sortField !== field) return <ArrowUpDown size={12} className="selection-modal__sort-icon selection-modal__sort-icon--idle" aria-hidden="true" />;
      return sortDirection === 'asc' ? (
        <ArrowUp size={12} className="selection-modal__sort-icon" aria-hidden="true" />
      ) : (
        <ArrowDown size={12} className="selection-modal__sort-icon" aria-hidden="true" />
      );
    },
    [sortField, sortDirection],
  );

  const ariaSortFor = useCallback(
    (field: SettingsSelectionSortField): 'ascending' | 'descending' | 'none' => {
      if (sortField !== field) return 'none';
      return sortDirection === 'asc' ? 'ascending' : 'descending';
    },
    [sortField, sortDirection],
  );

  const pageTokens = useMemo(() => computePageTokens(currentPage, totalPages), [currentPage, totalPages]);
  const canPrev = currentPage > 0;
  const canNext = currentPage < totalPages - 1;

  const columnCount = showDescriptionColumn ? 3 : 2;

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => !open && cancelAndClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="selection-modal__overlay" />
        <Theme accentColor="blue" hasBackground={false} asChild>
          <Dialog.Content
            className="selection-modal__content"
            data-testid={testId ?? 'selection-modal'}
            aria-describedby={undefined}
          >
            <Flex justify="between" align="center" className="selection-modal__header">
              <Dialog.Title asChild>
                <Heading size="5">{title}</Heading>
              </Dialog.Title>
              <Dialog.Close asChild>
                <button
                  type="button"
                  className="selection-modal__close"
                  aria-label={SETTINGS_SELECTION_MODAL.CLOSE_ARIA}
                  onClick={cancelAndClose}
                >
                  <X size={18} />
                </button>
              </Dialog.Close>
            </Flex>

            <Box className="selection-modal__filter-row">
              <TextField.Root
                size="2"
                placeholder={SETTINGS_SELECTION_MODAL.FILTER_PLACEHOLDER}
                value={filter}
                onChange={(e) => changeFilter(e.target.value)}
                data-testid="selection-modal-filter"
              >
                <TextField.Slot>
                  <Search size={16} />
                </TextField.Slot>
              </TextField.Root>
              <Text size="1" color="gray" className="selection-modal__wildcard-hint">
                {SETTINGS_SELECTION_MODAL.WILDCARD_HINT}
              </Text>
            </Box>

            <Box className="selection-modal__body">
              <table className="selection-modal__table" role="grid">
                <thead>
                  <tr>
                    <th
                      className="selection-modal__th selection-modal__th--select"
                      aria-sort={ariaSortFor('select')}
                    >
                      <button
                        type="button"
                        className="selection-modal__sort-btn"
                        onClick={handleSortClick('select')}
                        data-testid="selection-modal-sort-select"
                      >
                        {SETTINGS_SELECTION_MODAL.COLUMN_SELECT} {sortIconFor('select')}
                      </button>
                    </th>
                    <th className="selection-modal__th" aria-sort={ariaSortFor('name')}>
                      <button
                        type="button"
                        className="selection-modal__sort-btn"
                        onClick={handleSortClick('name')}
                        data-testid="selection-modal-sort-name"
                      >
                        {nameColumnLabel} {sortIconFor('name')}
                      </button>
                    </th>
                    {showDescriptionColumn && (
                      <th className="selection-modal__th" aria-sort={ariaSortFor('description')}>
                        <button
                          type="button"
                          className="selection-modal__sort-btn"
                          onClick={handleSortClick('description')}
                          data-testid="selection-modal-sort-description"
                        >
                          {descriptionColumnLabel} {sortIconFor('description')}
                        </button>
                      </th>
                    )}
                  </tr>
                  <tr className="selection-modal__counter-row">
                    <td colSpan={columnCount} className="selection-modal__counter-cell">
                      <Badge
                        color={selectedCount > 0 ? 'blue' : 'gray'}
                        variant={selectedCount > 0 ? 'solid' : 'soft'}
                        data-testid="selection-modal-counter"
                      >
                        {SETTINGS_SELECTION_MODAL.SELECTED_COUNTER(selectedCount)}
                      </Badge>
                    </td>
                  </tr>
                </thead>
                <tbody>
                  {visibleItems.length === 0 ? (
                    <tr>
                      <td
                        colSpan={columnCount}
                        className="selection-modal__empty"
                        data-testid="selection-modal-empty"
                      >
                        {emptyMessage ?? SETTINGS_SELECTION_MODAL.DEFAULT_EMPTY_MESSAGE}
                      </td>
                    </tr>
                  ) : (
                    visibleItems.map((item) => {
                      const checked = isSelected(item.id);
                      return (
                        <tr key={item.id} className="selection-modal__row" data-item-id={item.id}>
                          <td className="selection-modal__td selection-modal__td--select">
                            <input
                              type="checkbox"
                              checked={checked}
                              onChange={() => toggleItem(item.id)}
                              aria-label={SETTINGS_SELECTION_MODAL.SELECT_ROW_ARIA(item.name)}
                              data-testid={`selection-modal-checkbox-${item.id}`}
                            />
                          </td>
                          <td className="selection-modal__td">{item.name}</td>
                          {showDescriptionColumn && (
                            <td className="selection-modal__td selection-modal__td--description">
                              {item.description ?? ''}
                            </td>
                          )}
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </Box>

            <Flex
              justify="center"
              align="center"
              gap="1"
              className="selection-modal__pagination"
              data-testid="selection-modal-pagination"
            >
              <PageNavButton
                label={SETTINGS_SELECTION_MODAL.PAGE_FIRST_ARIA}
                disabled={!canPrev}
                onClick={() => changePage(0)}
              >
                &laquo;
              </PageNavButton>
              <PageNavButton
                label={SETTINGS_SELECTION_MODAL.PAGE_PREV_ARIA}
                disabled={!canPrev}
                onClick={() => changePage(currentPage - 1)}
              >
                &lsaquo;
              </PageNavButton>
              {pageTokens.map((token, i) =>
                token === 'gap-left' || token === 'gap-right' ? (
                  <span key={`${token}-${i}`} className="selection-modal__page-gap">
                    …
                  </span>
                ) : (
                  <button
                    key={token}
                    type="button"
                    className={`selection-modal__page-btn${
                      token === currentPage ? ' selection-modal__page-btn--active' : ''
                    }`}
                    aria-current={token === currentPage ? 'page' : undefined}
                    onClick={() => changePage(token)}
                    data-testid={`selection-modal-page-${token + 1}`}
                  >
                    {token + 1}
                  </button>
                ),
              )}
              <PageNavButton
                label={SETTINGS_SELECTION_MODAL.PAGE_NEXT_ARIA}
                disabled={!canNext}
                onClick={() => changePage(currentPage + 1)}
              >
                &rsaquo;
              </PageNavButton>
              <PageNavButton
                label={SETTINGS_SELECTION_MODAL.PAGE_LAST_ARIA}
                disabled={!canNext}
                onClick={() => changePage(totalPages - 1)}
              >
                &raquo;
              </PageNavButton>
            </Flex>

            <Flex justify="end" gap="3" className="selection-modal__footer">
              <SettingsButton
                variant="secondary"
                onClick={cancelAndClose}
                testId="selection-modal-cancel"
              >
                {SETTINGS_SELECTION_MODAL.BUTTON_CANCEL}
              </SettingsButton>
              <SettingsButton
                variant="primary"
                onClick={confirmAndClose}
                testId="selection-modal-confirm"
              >
                {SETTINGS_SELECTION_MODAL.BUTTON_CONFIRM}
              </SettingsButton>
            </Flex>

            {/* Announce the filtered count for AT users. */}
            <Text size="1" className="selection-modal__sr-only" aria-live="polite">
              {SETTINGS_SELECTION_MODAL.MATCHING_COUNT(filteredCount)}
            </Text>
          </Dialog.Content>
        </Theme>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

interface PageNavButtonProps {
  label: string;
  disabled: boolean;
  onClick: () => void;
  children: React.ReactNode;
}

function PageNavButton({ label, disabled, onClick, children }: PageNavButtonProps) {
  return (
    <button
      type="button"
      className="selection-modal__page-nav-btn"
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

export default SettingsSelectionModal;
