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
import { Flex, Table } from '@radix-ui/themes';
import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';
import { ArrowDown, ArrowUp } from 'lucide-react';

export type SortDirection = 'asc' | 'desc' | null;

interface SortableTableHeaderProps {
  children: React.ReactNode;
  sortKey: string;
  currentSortKey: string | null;
  currentSortDirection: SortDirection;
  onSort: (key: string, direction: SortDirection) => void;
  align?: 'left' | 'center' | 'right';
}

export function SortableTableHeader({
  children,
  sortKey,
  currentSortKey,
  currentSortDirection,
  onSort,
  align = 'left',
}: SortableTableHeaderProps) {
  const isActive = currentSortKey === sortKey;

  const handleClick = () => {
    if (!isActive) {
      onSort(sortKey, 'asc');
    } else if (currentSortDirection === 'asc') {
      onSort(sortKey, 'desc');
    } else {
      onSort(sortKey, 'asc');
    }
  };

  const getIcon = () => {
    if (isActive && currentSortDirection === 'asc') {
      return <ArrowUp size={14} />;
    }
    if (isActive && currentSortDirection === 'desc') {
      return <ArrowDown size={14} />;
    }
    return <ArrowUpDown size={14} />;
  };

  const ariaSort =
    isActive && currentSortDirection === 'asc'
      ? 'ascending'
      : isActive && currentSortDirection === 'desc'
        ? 'descending'
        : 'none';

  const justifyContent =
    align === 'center' ? 'center' : align === 'right' ? 'flex-end' : 'flex-start';

  return (
    <Table.ColumnHeaderCell align={align} aria-sort={ariaSort}>
      <div style={{ display: 'flex', justifyContent }}>
        <Flex
          align="center"
          gap="1"
          role="button"
          tabIndex={0}
          style={{
            cursor: 'pointer',
            userSelect: 'none',
            width: 'fit-content',
            whiteSpace: 'nowrap',
          }}
          onClick={handleClick}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              handleClick();
            }
          }}
        >
          {children}
          <span
            style={{
              opacity: isActive ? 1 : 0.5,
              color: 'var(--gray-12)',
              display: 'flex',
              alignItems: 'center',
            }}
          >
            {getIcon()}
          </span>
        </Flex>
      </div>
    </Table.ColumnHeaderCell>
  );
}

export function TableHeader({
  children,
  align = 'left',
}: {
  children: React.ReactNode;
  align?: 'left' | 'center' | 'right';
}) {
  return <Table.ColumnHeaderCell align={align}>{children}</Table.ColumnHeaderCell>;
}
