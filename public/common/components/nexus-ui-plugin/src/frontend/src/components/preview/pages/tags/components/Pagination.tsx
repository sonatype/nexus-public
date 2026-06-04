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
import { Button, Flex, Text } from '@radix-ui/themes';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

export interface PaginationProps {
  /** Current page (0-indexed) */
  currentPage: number;
  /** Total number of pages */
  pageCount: number;
  /** Callback when page changes */
  onChange: (page: number) => void;
}

/**
 * Simple pagination component with first/prev/next/last buttons.
 */
export function Pagination({
  currentPage,
  pageCount,
  onChange,
}: PaginationProps): JSX.Element | null {
  // Don't render if only one page or no pages
  if (pageCount <= 1) {
    return null;
  }

  const isFirstPage = currentPage === 0;
  const isLastPage = currentPage >= pageCount - 1;

  const goToFirst = () => onChange(0);
  const goToPrevious = () => onChange(Math.max(0, currentPage - 1));
  const goToNext = () => onChange(Math.min(pageCount - 1, currentPage + 1));
  const goToLast = () => onChange(pageCount - 1);

  return (
    <Flex align="center" justify="center" gap="2" className="pagination" data-testid="pagination">
      <Button
        variant="ghost"
        size="1"
        onClick={goToFirst}
        disabled={isFirstPage}
        aria-label="First page"
      >
        <ChevronsLeft size={16} />
      </Button>
      <Button
        variant="ghost"
        size="1"
        onClick={goToPrevious}
        disabled={isFirstPage}
        aria-label="Previous page"
      >
        <ChevronLeft size={16} />
      </Button>
      <Text size="2" color="gray">
        Page {currentPage + 1} of {pageCount}
      </Text>
      <Button
        variant="ghost"
        size="1"
        onClick={goToNext}
        disabled={isLastPage}
        aria-label="Next page"
      >
        <ChevronRight size={16} />
      </Button>
      <Button
        variant="ghost"
        size="1"
        onClick={goToLast}
        disabled={isLastPage}
        aria-label="Last page"
      >
        <ChevronsRight size={16} />
      </Button>
    </Flex>
  );
}

export default Pagination;

