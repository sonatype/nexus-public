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
import { Button, Flex, IconButton, Select, Text } from '@radix-ui/themes';
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

export interface ExamplePaginationProps {
  /** E.g. "1-25 of 26". */
  rangeLabel: string;
  /** Total page count; the first page is shown as current. 0 shows the arrows only. */
  pageCount?: number;
}

/** Static: the preview has no data to page through, so every control is inert. */
export function ExamplePagination({ rangeLabel, pageCount = 0 }: ExamplePaginationProps): JSX.Element {
  return (
    <Flex align="center" justify="between" pt="3" gap="3">
      <Text size="2">{rangeLabel}</Text>
      <Flex gap="1" align="center" role="navigation" aria-label="Pagination">
        <IconButton variant="outline" color="gray" size="2" disabled aria-label="First page">
          <ChevronsLeft size={14} aria-hidden="true" />
        </IconButton>
        <IconButton variant="outline" color="gray" size="2" disabled aria-label="Previous page">
          <ChevronLeft size={14} aria-hidden="true" />
        </IconButton>
        {Array.from({ length: pageCount }, (_, i) => (
          <Button
            key={i + 1}
            size="2"
            color="gray"
            variant={i === 0 ? 'solid' : 'outline'}
            highContrast={i === 0}
            disabled={i !== 0}
            aria-current={i === 0 ? 'page' : undefined}
          >
            {i + 1}
          </Button>
        ))}
        <IconButton variant="outline" color="gray" size="2" disabled aria-label="Next page">
          <ChevronRight size={14} aria-hidden="true" />
        </IconButton>
        <IconButton variant="outline" color="gray" size="2" disabled aria-label="Last page">
          <ChevronsRight size={14} aria-hidden="true" />
        </IconButton>
      </Flex>
      <Flex align="center" gap="2">
        <Text size="2">Rows per page</Text>
        <Select.Root value="25" disabled>
          <Select.Trigger aria-label="Rows per page" />
          <Select.Content>
            <Select.Item value="25">25</Select.Item>
          </Select.Content>
        </Select.Root>
      </Flex>
    </Flex>
  );
}
