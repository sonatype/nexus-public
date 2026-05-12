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

import React, { useCallback } from 'react';
import {
  Box,
  Callout,
  Card,
  DropdownMenu,
  Flex,
  IconButton,
  Inset,
  Spinner,
  Table,
  Text as RadixText,
  Tooltip,
} from '@radix-ui/themes';
import { ChevronRight, Copy, X, MoreHorizontal } from 'lucide-react';

import { CopyUrlButton } from '../../browse/actions/CopyUrlButton';
import { SortableTableHeader, type SortDirection } from '../../../../components/shared';
import type { UploadableRepository, SortColumn } from '../upload.types';
import { UPLOAD_STRINGS } from '../upload.types';
import { FORMAT_LABELS } from '../../../../components/super/settings/repository/repositories/types';

export interface UploadRepositoryTableProps {
  repositories: readonly UploadableRepository[];
  loading?: boolean;
  error?: string | null;
  sortColumn: SortColumn | null;
  sortDirection: SortDirection;
  onSort: (column: SortColumn, direction: SortDirection) => void;
  onSelect: (repositoryName: string) => void;
}

/**
 * UploadRepositoryTable - Table of uploadable repositories.
 * Matches Browse RepositoryListTable structure: Card > Inset > Table.
 */
export function UploadRepositoryTable({
  repositories,
  loading = false,
  error,
  sortColumn,
  sortDirection,
  onSort,
  onSelect,
}: UploadRepositoryTableProps): JSX.Element {
  const handleRowClick = useCallback(
    (repoName: string) => () => {
      onSelect(repoName);
    },
    [onSelect]
  );

  const handleRowKeyDown = useCallback(
    (repoName: string) => (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        onSelect(repoName);
      }
    },
    [onSelect]
  );

  const getSortDirection = (col: SortColumn): SortDirection => {
    if (sortColumn !== col) return null;
    return sortDirection;
  };

  if (loading) {
    return (
      <Card size="1">
        <Flex direction="column" align="center" justify="center" gap="3" p="9">
          <Spinner size="3" />
          <RadixText color="gray">{UPLOAD_STRINGS.loadingMessage}</RadixText>
        </Flex>
      </Card>
    );
  }

  if (error) {
    return (
      <Card size="1">
        <Box p="4">
          <Callout.Root color="red">
            <Callout.Icon>
              <X size={16} />
            </Callout.Icon>
            <Callout.Text>
              {UPLOAD_STRINGS.errorPrefix} {error}
            </Callout.Text>
          </Callout.Root>
        </Box>
      </Card>
    );
  }

  if (repositories.length === 0) {
    return (
      <Card size="1">
        <Flex justify="center" p="6">
          <RadixText color="gray">{UPLOAD_STRINGS.emptyMessage}</RadixText>
        </Flex>
      </Card>
    );
  }

  return (
    <Card size="1">
      <Inset clip="padding-box" side="bottom">
        <Box style={{ overflowX: 'auto' }}>
          <Table.Root size="2">
            <Table.Header>
              <Table.Row>
                <SortableTableHeader
                  sortKey="name"
                  currentSortKey={sortColumn}
                  currentSortDirection={getSortDirection('name')}
                  onSort={(key, dir) => {
                    if (dir) onSort(key as SortColumn, dir);
                  }}
                >
                  {UPLOAD_STRINGS.columns.name}
                </SortableTableHeader>
                <SortableTableHeader
                  sortKey="format"
                  currentSortKey={sortColumn}
                  currentSortDirection={getSortDirection('format')}
                  onSort={(key, dir) => {
                    if (dir) onSort(key as SortColumn, dir);
                  }}
                >
                  {UPLOAD_STRINGS.columns.format}
                </SortableTableHeader>
                <Table.ColumnHeaderCell justify="end" aria-label="Row actions" pr="2" />
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {repositories.map((repo) => (
                <Table.Row
                  key={repo.name}
                  onClick={handleRowClick(repo.name)}
                  onKeyDown={handleRowKeyDown(repo.name)}
                  tabIndex={0}
                  role="button"
                  aria-label={`Upload to ${repo.name}`}
                  style={{ cursor: 'pointer' }}
                >
                  <Table.Cell>
                    <RadixText weight="medium">{repo.name}</RadixText>
                  </Table.Cell>
                  <Table.Cell>
                    <RadixText size="2">{FORMAT_LABELS[repo.format] || repo.format}</RadixText>
                  </Table.Cell>
                  <Table.Cell pr="2" style={{ textAlign: 'right' }}>
                    <DropdownMenu.Root>
                      <DropdownMenu.Trigger>
                        <IconButton
                          variant="ghost"
                          color="gray"
                          size="1"
                          onClick={(e) => e.stopPropagation()}
                          aria-label="Row actions"
                        >
                          <MoreHorizontal size={16} />
                        </IconButton>
                      </DropdownMenu.Trigger>
                      <DropdownMenu.Content align="end" onClick={(e) => e.stopPropagation()}>
                        <DropdownMenu.Item onClick={(e) => {
                          e.stopPropagation();
                          onSelect(repo.name);
                        }}>
                          Upload to {repo.name}
                        </DropdownMenu.Item>
                        <DropdownMenu.Item onClick={async (e) => {
                          e.stopPropagation();
                          try {
                            await navigator.clipboard.writeText(repo.url);
                          } catch (error) {
                            console.error('Failed to copy URL:', error);
                          }
                        }}>
                          Copy URL
                        </DropdownMenu.Item>
                      </DropdownMenu.Content>
                    </DropdownMenu.Root>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        </Box>
      </Inset>
    </Card>
  );
}

export default UploadRepositoryTable;
