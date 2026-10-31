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

import React, {
  useEffect,
  useMemo,
  useRef,
  useState,
  type FocusEvent,
  type KeyboardEvent,
} from 'react';
import { Flex, Text, Button, IconButton, Select } from '@radix-ui/themes';
import { NxCombobox } from '@sonatype/react-shared-components';
import { ChevronsLeft, ChevronLeft, ChevronRight, ChevronsRight } from 'lucide-react';

import './TablePagination.scss';

export const PAGE_SIZE_OPTIONS = [20, 40, 50, 100, 250] as const;

export type PageToken = number | 'ellipsis-left' | 'ellipsis-right';

export interface PageMatch {
  id: number;
  displayName: string;
}

const SHOW_ALL_THRESHOLD = 10;
const LARGE_TOTAL_THRESHOLD = 1000;
const MATCH_CAP = 20;
const EMPTY_QUERY_WINDOW_RADIUS = 50;

export function computeMatches(
  query: string,
  currentPage: number,
  totalPages: number,
): PageMatch[] {
  const trimmed = query.trim();
  if (trimmed === '') {
    const start = Math.max(1, currentPage - EMPTY_QUERY_WINDOW_RADIUS);
    const end = Math.min(totalPages, currentPage + EMPTY_QUERY_WINDOW_RADIUS);
    const matches: PageMatch[] = [];
    for (let p = start; p <= end; p++) {
      matches.push({ id: p, displayName: String(p) });
    }
    return matches;
  }
  if (!/^\d+$/.test(trimmed)) {
    return [];
  }
  const matches: PageMatch[] = [];
  for (let p = 1; p <= totalPages && matches.length < MATCH_CAP; p++) {
    if (String(p).startsWith(trimmed)) {
      matches.push({ id: p, displayName: String(p) });
    }
  }
  return matches;
}

interface PageJumpControlProps {
  currentPage: number;
  totalPages: number;
  onJump: (page: number) => void;
}

function PageJumpControl({ currentPage, totalPages, onJump }: PageJumpControlProps) {
  const [draft, setDraft] = useState(String(currentPage));
  const [hasTyped, setHasTyped] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const selectedItemRef = useRef<PageMatch | null>(null);
  const justCommittedRef = useRef(false);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const scrolledForOpenRef = useRef(false);

  useEffect(() => {
    setDraft(String(currentPage));
    setHasTyped(false);
    selectedItemRef.current = null;
    justCommittedRef.current = false;
  }, [currentPage]);

  const matches = useMemo(
    () => computeMatches(hasTyped ? draft : '', currentPage, totalPages),
    [hasTyped, draft, currentPage, totalPages],
  );

  // NxCombobox renders match text only and reserves aria-selected for the
  // arrow-key cursor, so the current page is tagged on the rendered option node.
  useEffect(() => {
    if (!isOpen) return;
    const options = wrapperRef.current?.querySelectorAll<HTMLElement>('[role="option"]');
    if (!options?.length) return;
    for (const option of options) {
      option.removeAttribute('aria-current');
    }
    const currentIndex = matches.findIndex((match) => match.id === currentPage);
    if (currentIndex < 0) return;
    const currentOption = options[currentIndex];
    currentOption.setAttribute('aria-current', 'page');
    if (!scrolledForOpenRef.current) {
      currentOption.scrollIntoView({ block: 'nearest' });
      scrolledForOpenRef.current = true;
    }
  }, [isOpen, matches, currentPage]);

  const handleChange = (newValue: string, item?: PageMatch) => {
    if (item) {
      selectedItemRef.current = item;
    } else {
      const digits = newValue.replace(/\D/g, '');
      setDraft(digits);
      setHasTyped(true);
      selectedItemRef.current = null;
    }
  };

  const handleSearch = (query: string) => {
    const selected = selectedItemRef.current;
    if (selected && query === selected.displayName) {
      const clamped = Math.max(1, Math.min(selected.id, totalPages));
      if (clamped !== currentPage) {
        onJump(clamped);
        justCommittedRef.current = true;
      } else {
        justCommittedRef.current = false;
      }
      selectedItemRef.current = null;
    }
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (event.key !== 'Enter') return;
    if (justCommittedRef.current) {
      justCommittedRef.current = false;
      return;
    }
    const trimmed = draft.trim();
    if (trimmed === '') return;
    const parsed = parseInt(trimmed, 10);
    if (!Number.isFinite(parsed) || parsed < 1) return;
    const clamped = Math.max(1, Math.min(parsed, totalPages));
    if (clamped !== currentPage) {
      onJump(clamped);
    }
  };

  const handleBlur = (event: FocusEvent<HTMLDivElement>) => {
    if (event.currentTarget.contains(event.relatedTarget as Node | null)) return;
    setIsOpen(false);
    scrolledForOpenRef.current = false;
    const trimmed = draft.trim();
    const parsed = parseInt(trimmed, 10);
    if (Number.isFinite(parsed) && parsed >= 1) {
      const clamped = Math.max(1, Math.min(parsed, totalPages));
      if (clamped !== currentPage) {
        onJump(clamped);
      }
      return;
    }
    setDraft(String(currentPage));
    setHasTyped(false);
  };

  const inputWidth = `${String(totalPages).length + 3}ch`;

  return (
    <div
      ref={wrapperRef}
      onFocus={() => setIsOpen(true)}
      onBlur={handleBlur}
      style={{ display: 'inline-block' }}
    >
      <NxCombobox<PageMatch>
        value={draft}
        matches={matches}
        onChange={handleChange}
        onSearch={handleSearch}
        autoComplete={false}
        aria-label="Jump to page"
        emptyMessage={`No page starts with "${draft.trim()}"`}
        className="tablepagination-jump-combobox"
        style={{ width: inputWidth }}
        onKeyDown={handleKeyDown}
      />
    </div>
  );
}

/**
 * Middle window around currentPage, shifted inward when it would overrun a
 * boundary so the resulting range still spans (2 * siblingCount + 1) pages
 * when possible.
 */
function computeWindow(
  currentPage: number,
  totalPages: number,
  siblingCount: number,
): { start: number; end: number } {
  const rawStart = currentPage - siblingCount;
  const rawEnd = currentPage + siblingCount;
  const minStart = 2;
  const maxEnd = totalPages - 1;
  if (rawStart < minStart) {
    const shift = minStart - rawStart;
    return { start: minStart, end: Math.min(maxEnd, rawEnd + shift) };
  }
  if (rawEnd > maxEnd) {
    const shift = rawEnd - maxEnd;
    return { start: Math.max(minStart, rawStart - shift), end: maxEnd };
  }
  return { start: rawStart, end: rawEnd };
}

/** Page 1, plus page 2 (single-page gap) or an ellipsis (multi-page gap), or neither if adjacent. */
function leftBoundaryTokens(windowStart: number): PageToken[] {
  if (windowStart <= 2) return [1];
  if (windowStart === 3) return [1, 2];
  return [1, 'ellipsis-left'];
}

/** Mirror of leftBoundaryTokens, terminating with page totalPages. */
function rightBoundaryTokens(windowEnd: number, totalPages: number): PageToken[] {
  if (windowEnd >= totalPages - 1) return [totalPages];
  if (windowEnd === totalPages - 2) return [totalPages - 1, totalPages];
  return ['ellipsis-right', totalPages];
}

/**
 * Returns the bounded set of tokens to render in the pagination bar.
 * For totalPages <= 10, returns every page number (no ellipses) so medium-sized
 * tables keep every page reachable in a single click.
 * For totalPages > 10, returns [1, ...window, totalPages] with a window around
 * currentPage that shifts when near the boundaries. The window is 7 pages wide
 * for totalPages < 1000 and narrows to 5 pages at 4+ digits so the wider
 * numeric tiles still fit the row without wrapping. Ellipsis markers are only
 * inserted when they would hide at least 2 pages (otherwise the elided page
 * is shown inline).
 */
export function getPageTokens(currentPage: number, totalPages: number): PageToken[] {
  if (totalPages <= SHOW_ALL_THRESHOLD) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }
  const siblingCount = totalPages >= LARGE_TOTAL_THRESHOLD ? 2 : 3;
  const { start, end } = computeWindow(currentPage, totalPages, siblingCount);
  const middle = Array.from({ length: end - start + 1 }, (_, i) => start + i);
  return [
    ...leftBoundaryTokens(start),
    ...middle,
    ...rightBoundaryTokens(end, totalPages),
  ];
}

export interface TablePaginationProps {
  currentPage: number;
  totalPages: number;
  itemsPerPage: number;
  totalItems: number;
  onPageChange: (page: number) => void;
  /** Called when user changes items per page. When provided, shows a dropdown to select page size. */
  onItemsPerPageChange?: (itemsPerPage: number) => void;
  /** Margin above the pagination bar. Default "4". Use "0" when parent controls spacing (e.g. Flex gap). */
  mt?: '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9';
  /** Optional suffix for total display (e.g. "+" when more results available). */
  totalItemsSuffix?: string;
  /** When on last page and true, Next button calls onLoadMore instead of being disabled. */
  hasMore?: boolean;
  /** When true, Next button is disabled (e.g. loading more). */
  loadingMore?: boolean;
  /** Called when user clicks Next on last page and hasMore is true. */
  onLoadMore?: () => void;
}

/**
 * Table pagination matching Nexus One design.
 * Left: "Showing X of Y" (X = items on current page, Y = total)
 * Center: Page dropdown (jump to specific page)
 * Right: First, Previous, page number buttons, Next, Last
 */
export function TablePagination({
  currentPage,
  totalPages,
  itemsPerPage,
  totalItems,
  onPageChange,
  onItemsPerPageChange,
  mt = '4',
  totalItemsSuffix,
  hasMore,
  loadingMore,
  onLoadMore,
}: TablePaginationProps) {
  const start = (currentPage - 1) * itemsPerPage + 1;
  const end = Math.min(currentPage * itemsPerPage, totalItems);
  const showingCount = end - start + 1;

  if (totalItems === 0) {
    return null;
  }

  const handlePageChange = (page: number) => {
    onPageChange(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleItemsPerPageChange = (value: string) => {
    const newLimit = parseInt(value, 10);
    onItemsPerPageChange?.(newLimit);
    onPageChange(1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const itemsPerPageValue = PAGE_SIZE_OPTIONS.includes(itemsPerPage as number) ? itemsPerPage.toString() : '20';

  return (
    <Flex
      justify="between"
      align="center"
      width="100%"
      mt={mt}
      gap="4"
      style={{ flexWrap: 'wrap' }}
    >
      {/* Left: Showing [dropdown] of Y + Page dropdown */}
      <Flex align="center" gap="4" wrap="wrap">
        <Flex align="center" gap="2" style={{ whiteSpace: 'nowrap' }}>
          <Text size="2">Showing</Text>
          {onItemsPerPageChange ? (
            <Select.Root
              value={itemsPerPageValue}
              onValueChange={handleItemsPerPageChange}
              size="2"
            >
              <Select.Trigger style={{ minWidth: 'unset' }} />
              <Select.Content>
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <Select.Item key={size} value={size.toString()}>
                    {size}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          ) : (
            <Text size="2">{showingCount}</Text>
          )}
          <Text size="2">of {totalItems.toLocaleString()}{totalItemsSuffix ?? ''}</Text>
        </Flex>
        <Flex align="center" gap="2" style={{ whiteSpace: 'nowrap' }}>
          <Text size="2">Page</Text>
          <PageJumpControl
            currentPage={currentPage}
            totalPages={totalPages}
            onJump={handlePageChange}
          />
          <Text size="2">of {totalPages}</Text>
        </Flex>
      </Flex>

      {/* Right: Navigation buttons */}
      <nav aria-label={`Pagination, page ${currentPage} of ${totalPages}`}>
        <Flex align="center" gap="1">
          <IconButton
            variant="outline"
            color="gray"
            size="2"
            disabled={currentPage === 1}
            onClick={() => handlePageChange(1)}
            aria-label="First page"
          >
            <ChevronsLeft size={16} />
          </IconButton>
          <IconButton
            variant="outline"
            color="gray"
            size="2"
            disabled={currentPage === 1}
            onClick={() => handlePageChange(currentPage - 1)}
            aria-label="Previous page"
          >
            <ChevronLeft size={16} />
          </IconButton>

          {/* Page number buttons — windowed */}
          {getPageTokens(currentPage, totalPages).map((token) => {
            if (token === 'ellipsis-left' || token === 'ellipsis-right') {
              return (
                <Text
                  key={token}
                  size="2"
                  color="gray"
                  aria-hidden="true"
                  style={{ padding: '0 4px' }}
                >
                  …
                </Text>
              );
            }
            const page = token;
            const isCurrent = currentPage === page;
            return (
              <Button
                key={page}
                variant="outline"
                color="gray"
                size="2"
                highContrast={isCurrent}
                aria-current={isCurrent ? 'page' : undefined}
                onClick={() => handlePageChange(page)}
                style={{
                  backgroundColor: isCurrent ? 'var(--gray-12)' : undefined,
                  color: isCurrent ? 'var(--gray-1)' : undefined,
                }}
              >
                {page}
              </Button>
            );
          })}

          <IconButton
            variant="outline"
            color="gray"
            size="2"
            disabled={
              currentPage === totalPages &&
              !(hasMore && onLoadMore && !loadingMore)
            }
            onClick={() => {
              if (currentPage === totalPages && hasMore && onLoadMore && !loadingMore) {
                onLoadMore();
              } else {
                handlePageChange(currentPage + 1);
              }
            }}
            aria-label={currentPage === totalPages && hasMore ? 'Load more' : 'Next page'}
          >
            <ChevronRight size={16} />
          </IconButton>
          <IconButton
            variant="outline"
            color="gray"
            size="2"
            disabled={currentPage === totalPages}
            onClick={() => handlePageChange(totalPages)}
            aria-label="Last page"
          >
            <ChevronsRight size={16} />
          </IconButton>
        </Flex>
      </nav>
    </Flex>
  );
}
