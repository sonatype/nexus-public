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

import React, { useCallback, useMemo, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Card,
  Checkbox,
  Flex,
  Heading,
  Inset,
  Link,
  Table,
  Text,
  TextField,
} from '@radix-ui/themes';
import { Search } from 'lucide-react';

import { useToast } from '../../../../shared';
import { useIsCloud } from '../../../../shared/hooks';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { useBlobStoreCdnStatuses } from './useBlobStoreCdnStatuses';
import { useRepositoryCdnList, RepositoryCdnRow } from './useRepositoryCdnList';

type CdnState = 'enabled' | 'disabled' | 'blob-store-off';

// When the user cannot read blob stores, the blob store state is unknown and only the repository's
// own setting is shown.
const STATUS_LABELS = { enabled: 'Configured', disabled: 'Not configured' } as const;

function deriveState(
  row: RepositoryCdnRow,
  blobStoreCdnMap: Record<string, { cdnEnabled: boolean; type: string }>,
): CdnState {
  const bsInfo = blobStoreCdnMap[row.blobStoreName];
  if (bsInfo && !bsInfo.cdnEnabled) return 'blob-store-off';
  return row.globalDistribution ? 'enabled' : 'disabled';
}

function repositoryNoun(count: number): string {
  return count === 1 ? 'repository' : 'repositories';
}

/**
 * Repository Distribution Status card — bulk configuration of Repository CDN Enablement.
 *
 * Renders inside GlobalDistributionPage. Reuses the page's edition, feature-flag, and permission
 * gates; adds a nexus:settings:update check that hides the bulk buttons and row selection for read-only admins.
 *
 * Distribution Status column logic:
 * - "Enabled" when repository.globalDistribution = true AND blob store CDN is enabled
 * - "Disabled" when repository.globalDistribution = false AND blob store CDN is enabled
 * - "Not enabled for blob store" when blob store supports CDN but is not configured
 *
 * Repositories whose blob stores don't support global distribution (UNSUPPORTED status) are
 * excluded from the table. Group repositories (no blob store) are always included.
 */
export function RepositoryCdnStatusCard() {
  const { rows, isLoading, error, isUpdating, applyCdnState } = useRepositoryCdnList();
  const { blobStores, isLoading: cdnLoading, error: cdnError, canReadBlobStores } = useBlobStoreCdnStatuses();
  const { success: toastSuccess, error: toastError, warning: toastWarning } = useToast();
  const canUpdate = ExtJS.checkPermission('nexus:settings:update');
  const isCloud = useIsCloud();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // Convert blobStores array to a map for O(1) lookups: blobStoreName -> { cdnEnabled, type }
  const blobStoreCdnMap = useMemo(() => {
    const map: Record<string, { cdnEnabled: boolean; type: string }> = {};
    for (const bs of blobStores) {
      map[bs.name] = { cdnEnabled: bs.cdnEnabled, type: bs.type ?? 's3' };
    }
    return map;
  }, [blobStores]);

  // Filter to only repositories whose blob stores support global distribution.
  // Group repositories have no blob store (blobStoreName === '') and are always shown.
  const supportedRows = useMemo(() => {
    return rows
      .filter((r) => {
        // Group repositories have no blob store - always include them
        if (!r.blobStoreName) return true;
        // Without blob store access we can't tell which are UNSUPPORTED, so keep everything
        if (!canReadBlobStores) return true;
        // Include if blob store is in the map (i.e., not UNSUPPORTED)
        return r.blobStoreName in blobStoreCdnMap;
      })
      .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase()));
  }, [rows, blobStoreCdnMap, canReadBlobStores]);

  const filteredRows = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return supportedRows;
    return supportedRows.filter(
      (r) =>
        r.name.toLowerCase().includes(q) ||
        r.type.toLowerCase().includes(q) ||
        r.format.toLowerCase().includes(q),
    );
  }, [supportedRows, searchQuery]);

  const allFilteredSelected =
    filteredRows.length > 0 && filteredRows.every((r) => selectedIds.has(r.id));

  const handleSearchChange = useCallback((next: string) => {
    setSearchQuery(next);
    setSelectedIds(new Set());
  }, []);

  const toggleRow = useCallback((id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }, []);

  const toggleAll = useCallback(() => {
    setSelectedIds(
      allFilteredSelected ? new Set() : new Set(filteredRows.map((r) => r.id)),
    );
  }, [allFilteredSelected, filteredRows]);

  const applyBulk = useCallback(
    async (value: boolean) => {
      const selected = filteredRows.filter((r) => selectedIds.has(r.id));
      if (selected.length === 0) return;

      const skipped = value
        ? selected.filter((r) => deriveState(r, blobStoreCdnMap) === 'blob-store-off')
        : [];
      const eligible = value
        ? selected.filter((r) => deriveState(r, blobStoreCdnMap) !== 'blob-store-off')
        : selected;

      const outcome =
        eligible.length > 0
          ? await applyCdnState(
              eligible.map((r) => r.id),
              value,
            )
          : { updated: [], failed: [] };

      const verb = value ? 'enabled' : 'disabled';
      const okCount = outcome.updated.length;
      const failCount = outcome.failed.length;
      const skipCount = skipped.length;

      if (failCount === 0 && okCount > 0) {
        const detail =
          skipCount > 0
            ? `${okCount} ${repositoryNoun(okCount)} updated. ${skipCount} skipped because Global Distribution is not enabled for their blob store.`
            : `${okCount} ${repositoryNoun(okCount)} updated.`;
        toastSuccess(`Global Distribution ${verb}`, detail);
      } else if (failCount > 0 && okCount === 0) {
        toastError(
          `Failed to ${value ? 'enable' : 'disable'} Global Distribution`,
          outcome.failed[0]?.message || 'Please try again.',
        );
      } else if (failCount > 0 && okCount > 0) {
        toastWarning(
          `${okCount} ${repositoryNoun(okCount)} ${verb}, ${failCount} failed`,
          outcome.failed[0]?.message ||
            `${failCount} ${repositoryNoun(failCount)} could not be updated.`,
        );
      } else if (okCount === 0 && skipCount > 0) {
        toastWarning(
          'Nothing to update',
          `${skipCount} ${repositoryNoun(skipCount)} skipped because Global Distribution is not enabled for their blob store.`,
        );
      }

      // Keep failed rows selected for retry; drop the rest.
      setSelectedIds(new Set(outcome.failed.map((f) => f.id)));
    },
    [filteredRows, selectedIds, blobStoreCdnMap, applyCdnState, toastSuccess, toastError, toastWarning],
  );

  const hasSelection = selectedIds.size > 0;
  const bulkDisabled = !hasSelection || isUpdating;

  if (isLoading || cdnLoading) {
    return (
      <Card size="3" variant="surface">
        <Box p="4">
          <Text size="2" color="gray">
            Loading repositories…
          </Text>
        </Box>
      </Card>
    );
  }

  if (error) {
    return (
      <Card size="3" variant="surface">
        <Box p="4">
          <Text size="2" color="red">
            Failed to load repositories: {error}
          </Text>
        </Box>
      </Card>
    );
  }

  if (cdnError) {
    return (
      <Card size="3" variant="surface">
        <Box p="4">
          <Text size="2" color="red">
            Failed to load blob store Global Distribution status: {cdnError}
          </Text>
        </Box>
      </Card>
    );
  }

  if (rows.length === 0) {
    return (
      <Card size="3" variant="surface">
        <Box p="4">
          <Heading size="3" mb="1">
            Repository Distribution Status
          </Heading>
          <Text size="2" color="gray" as="div" mb="4">
            Manage Global Distribution for repositories
          </Text>
          <Text size="2" color="gray">
            There are no repositories available
          </Text>
        </Box>
      </Card>
    );
  }

  return (
    <Card size="3" variant="surface">
      <Box p="4">
        <Heading size="3" mb="1">
          Repository Distribution Status
        </Heading>
        <Text size="2" color="gray" as="div" mb="4">
          Manage Global Distribution for repositories
        </Text>

        <Box mb="4">
          <TextField.Root
            placeholder="Filter by name"
            value={searchQuery}
            onChange={(e) => handleSearchChange(e.target.value)}
            size="2"
            aria-label="Filter repositories by name"
          >
            <TextField.Slot>
              <Search size={16} aria-hidden="true" />
            </TextField.Slot>
          </TextField.Root>
        </Box>

        {canUpdate && (
          <Flex gap="3" mb="4">
            <Button
              size="2"
              variant="soft"
              color="gray"
              disabled={bulkDisabled}
              onClick={() => void applyBulk(true)}
              aria-label="Enable Global Distribution for selected repositories"
            >
              Enable
            </Button>
            <Button
              size="2"
              variant="soft"
              color="gray"
              disabled={bulkDisabled}
              onClick={() => void applyBulk(false)}
              aria-label="Disable Global Distribution for selected repositories"
            >
              Disable
            </Button>
          </Flex>
        )}

        <Inset side="x">
          <Table.Root size="2" variant="surface">
            <Table.Header>
              <Table.Row>
                {canUpdate && (
                  <Table.ColumnHeaderCell style={{ width: 48 }}>
                    <Checkbox
                      checked={allFilteredSelected}
                      onCheckedChange={toggleAll}
                      aria-label="Select all repositories"
                      disabled={filteredRows.length === 0}
                    />
                  </Table.ColumnHeaderCell>
                )}
                <Table.ColumnHeaderCell style={{ minWidth: 180 }}>
                  <Text size="2" weight="medium">
                    Name
                  </Text>
                </Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell style={{ minWidth: 100 }}>
                  <Text size="2" weight="medium">
                    Type
                  </Text>
                </Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell style={{ minWidth: 120 }}>
                  <Text size="2" weight="medium">
                    Format
                  </Text>
                </Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell style={{ minWidth: 160 }}>
                  <Text size="2" weight="medium">
                    Distribution Status
                  </Text>
                </Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {filteredRows.length === 0 ? (
                <Table.Row>
                  <Table.Cell colSpan={canUpdate ? 5 : 4}>
                    <Text
                      size="2"
                      color="gray"
                      style={{ textAlign: 'center', display: 'block' }}
                    >
                      No repositories match the filter
                    </Text>
                  </Table.Cell>
                </Table.Row>
              ) : (
                filteredRows.map((row) => {
                  const state = deriveState(row, blobStoreCdnMap);
                  return (
                    <Table.Row key={row.id}>
                      {canUpdate && (
                        <Table.Cell>
                          <Checkbox
                            checked={selectedIds.has(row.id)}
                            onCheckedChange={() => toggleRow(row.id)}
                            aria-label={`Select ${row.name}`}
                          />
                        </Table.Cell>
                      )}
                      <Table.Cell>
                        <Link
                          href={`#preview/admin/repository/repositories/${encodeURIComponent(row.name)}`}
                          size="2"
                          weight="medium"
                        >
                          {row.name}
                        </Link>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2" style={{ textTransform: 'capitalize' }}>
                          {row.type}
                        </Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2">{row.format}</Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Flex align="center" gap="2">
                          {state === 'enabled' && (
                            <Badge size="1" variant="soft" color="green" radius="full">
                              {canReadBlobStores ? 'Enabled' : STATUS_LABELS.enabled}
                            </Badge>
                          )}
                          {state === 'disabled' && (
                            <Badge size="1" variant="soft" color="gray" radius="full">
                              {canReadBlobStores ? 'Disabled' : STATUS_LABELS.disabled}
                            </Badge>
                          )}
                          {state === 'blob-store-off' && (
                            <>
                              <Badge
                                size="1"
                                variant="soft"
                                color="orange"
                                radius="full"
                              >
                                Not enabled for blob store
                              </Badge>
                              {/* Cloud has no blob store admin pages to link to. */}
                              {!isCloud && (
                                <Link
                                  href={`#preview/admin/repository/blobstores/${encodeURIComponent(blobStoreCdnMap[row.blobStoreName].type)}/${encodeURIComponent(row.blobStoreName)}`}
                                  size="1"
                                >
                                  Configure
                                </Link>
                              )}
                            </>
                          )}
                        </Flex>
                      </Table.Cell>
                    </Table.Row>
                  );
                })
              )}
            </Table.Body>
          </Table.Root>
        </Inset>
      </Box>
    </Card>
  );
}

export default RepositoryCdnStatusCard;
