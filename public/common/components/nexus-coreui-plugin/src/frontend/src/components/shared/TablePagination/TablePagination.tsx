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

import React from 'react';
import { Flex, Text, Button, IconButton, Select } from '@radix-ui/themes';
import { ChevronsLeft, ChevronLeft, ChevronRight, ChevronsRight } from 'lucide-react';

export const PAGE_SIZE_OPTIONS = [20, 40, 50, 100, 250] as const;

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
          <Select.Root
            value={currentPage.toString()}
            onValueChange={(v) => handlePageChange(parseInt(v, 10))}
            size="2"
          >
            <Select.Trigger style={{ minWidth: 'unset' }} />
            <Select.Content>
              {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                <Select.Item key={page} value={page.toString()}>
                  {page}
                </Select.Item>
              ))}
            </Select.Content>
          </Select.Root>
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

          {/* Page number buttons */}
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
            <Button
              key={page}
              variant="outline"
              color="gray"
              size="2"
              highContrast={currentPage === page}
              onClick={() => handlePageChange(page)}
              style={{
                backgroundColor: currentPage === page ? 'var(--gray-12)' : undefined,
                color: currentPage === page ? 'var(--gray-1)' : undefined,
              }}
            >
              {page}
            </Button>
          ))}

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
