/*
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */

import React from 'react';
import { Table, Checkbox } from '@radix-ui/themes';
import './TableSkeleton.scss';

/**
 * Table loading skeleton following NexusOne Pattern 3: Table Row Skeleton
 *
 * Design principles:
 * - backgroundColor: var(--gray-4)
 * - borderRadius: match content shape
 * - animation: pulse 2s infinite
 * - Dimensions mirror actual content
 * - Keep headers visible for context
 */
export function TableSkeleton({ rows = 5 }) {
  return (
    <Table.Root className="nxrm-ip-allowlist__table">
      <Table.Header>
        <Table.Row>
          <Table.ColumnHeaderCell width="40px">
            <Checkbox disabled />
          </Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>IP Address</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Description</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Last Updated</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell width="60px">Actions</Table.ColumnHeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {Array.from({ length: rows }).map((_, index) => (
          <Table.Row key={index} className="table-skeleton__row">
            <Table.Cell>
              <div className="table-skeleton__checkbox" />
            </Table.Cell>
            <Table.Cell>
              <div className="table-skeleton__text table-skeleton__text--short" />
            </Table.Cell>
            <Table.Cell>
              <div className="table-skeleton__text table-skeleton__text--long" />
            </Table.Cell>
            <Table.Cell>
              <div className="table-skeleton__text table-skeleton__text--medium" />
            </Table.Cell>
            <Table.Cell>
              <div className="table-skeleton__icon" />
            </Table.Cell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table.Root>
  );
}
