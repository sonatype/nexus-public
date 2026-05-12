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

import React, { useState, useMemo, useCallback } from 'react';
import { useRouter } from '@uirouter/react';
import { Button, Flex, TextField } from '@radix-ui/themes';
import {
  HardDrive,
  Plus,
  Search,
  Cloud,
  Database,
  Server
} from 'lucide-react';
import { ExtJS, HumanReadableUtils, Permissions } from '@sonatype/nexus-ui-plugin';

import {
  PageHeader,
  EntityTable,
  EmptyState,
  StatusBadge,
  HelpSection,
  type TableColumn
} from '../../../../shared';

import { useBlobStoresList } from './useBlobStores';
import type { BlobStore } from './types';
import './BlobStoresList.scss';

const STRINGS = {
  TITLE: 'Blob Stores',
  DESCRIPTION: 'Configure local and cloud blob storage',
  CREATE_BUTTON: 'Create Blob Store',
  FILTER_PLACEHOLDER: 'Filter by name...',
  LOADING: 'Loading blob stores...',
  ERROR_TITLE: 'Failed to Load Blob Stores',
  EMPTY: {
    TITLE: 'No Blob Stores',
    DESCRIPTION: 'Create your first blob store to configure storage for your repositories.',
    ACTION: 'Create Blob Store'
  },
  COLUMNS: {
    NAME: 'Name',
    PATH: 'Path',
    TYPE: 'Type',
    STATE: 'State',
    COUNT: 'Blob Count',
    SIZE: 'Total Size',
    SPACE: 'Available Space'
  },
  STATUS: {
    ONLINE: 'Started',
    OFFLINE: 'Failed'
  },
  UNKNOWN: 'Unavailable',
  UNLIMITED: 'Unlimited',
  CALCULATING: 'Calculating...',
  HELP: {
    TITLE: 'What is a blob store?',
    CONTENT: 'The binary assets you download via proxy repositories, or publish to hosted repositories, are stored in the blob store attached to those repositories. In traditional, single node NXRM deployments, blob stores are typically associated with a local filesystem directory, usually within the sonatype-work directory.',
    DOC_LABEL: 'View Documentation'
  }
};

function getTypeIcon(typeId: string): React.ReactNode {
  const iconClass = 'blob-stores-list__type-icon';
  switch (typeId?.toLowerCase()) {
    case 's3':
      return <Cloud className={`${iconClass} ${iconClass}--s3`} size={16} />;
    case 'azure':
      return <Cloud className={`${iconClass} ${iconClass}--azure`} size={16} />;
    case 'google':
      return <Cloud className={`${iconClass} ${iconClass}--google`} size={16} />;
    case 'group':
      return <Database className={`${iconClass} ${iconClass}--group`} size={16} />;
    default:
      return <Server className={`${iconClass} ${iconClass}--file`} size={16} />;
  }
}

function formatBytes(bytes: number): string {
  return HumanReadableUtils.bytesToString(bytes);
}

export default function BlobStoresList() {
  const router = useRouter();
  const { blobStores, loading, error, refetch } = useBlobStoresList();
  const [filterText, setFilterText] = useState('');
  const [sortBy, setSortBy] = useState<string>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  const isCalculating = ExtJS.state().getValue('nexus.datastore.blobstore.metrics.calculating') || false;
  const hasUser = ExtJS.useUser() ?? false;
  const canCreate = ExtJS.usePermission(() => ExtJS.checkPermission(Permissions.BLOB_STORES.CREATE), [hasUser]);

  // Filter data
  const filteredData = useMemo(() => {
    if (!filterText) return blobStores;
    const lowerFilter = filterText.toLowerCase();
    return blobStores.filter(store => store.name.toLowerCase().includes(lowerFilter));
  }, [blobStores, filterText]);

  // Sort data
  const sortedData = useMemo(() => {
    const sorted = [...filteredData];
    sorted.sort((a, b) => {
      const aVal = a[sortBy as keyof BlobStore];
      const bVal = b[sortBy as keyof BlobStore];

      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return sortDirection === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
      }
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDirection === 'asc' ? aVal - bVal : bVal - aVal;
      }
      if (typeof aVal === 'boolean' && typeof bVal === 'boolean') {
        return sortDirection === 'asc'
          ? (aVal === bVal ? 0 : aVal ? -1 : 1)
          : (aVal === bVal ? 0 : aVal ? 1 : -1);
      }
      return 0;
    });
    return sorted;
  }, [filteredData, sortBy, sortDirection]);

  const handleSort = useCallback((column: string) => {
    if (sortBy === column) {
      setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDirection('asc');
    }
  }, [sortBy]);

  const handleRowClick = useCallback((store: BlobStore) => {
    router.stateService.go('preview.admin.repository.blobstores.edit', {
      type: encodeURIComponent(store.typeId),
      name: encodeURIComponent(store.name)
    });
  }, [router]);

  const handleCreate = useCallback(() => {
    router.stateService.go('preview.admin.repository.blobstores.create');
  }, [router]);

  // Define table columns
  const columns: TableColumn<BlobStore>[] = useMemo(() => [
    {
      id: 'name',
      header: STRINGS.COLUMNS.NAME,
      accessor: (row) => (
        <Flex align="center" gap="2">
          {getTypeIcon(row.typeId)}
          <span>{row.name}</span>
        </Flex>
      ),
      sortable: true
    },
    {
      id: 'path',
      header: STRINGS.COLUMNS.PATH,
      accessor: (row) => row.path || '-',
      sortable: true
    },
    {
      id: 'type',
      header: STRINGS.COLUMNS.TYPE,
      accessor: 'type',
      sortable: true
    },
    {
      id: 'available',
      header: STRINGS.COLUMNS.STATE,
      accessor: (row) => (
        <StatusBadge
          status={row.available ? 'online' : 'offline'}
          label={row.available ? STRINGS.STATUS.ONLINE : STRINGS.STATUS.OFFLINE}
          size="small"
        />
      ),
      sortable: true
    },
    {
      id: 'blobCount',
      header: STRINGS.COLUMNS.COUNT,
      accessor: (row) => {
        if (row.unavailable) return STRINGS.UNKNOWN;
        if (isCalculating) return STRINGS.CALCULATING;
        return row.blobCount.toLocaleString();
      },
      sortable: true,
      align: 'right'
    },
    {
      id: 'totalSizeInBytes',
      header: STRINGS.COLUMNS.SIZE,
      accessor: (row) => {
        if (row.unavailable) return STRINGS.UNKNOWN;
        if (isCalculating) return STRINGS.CALCULATING;
        return formatBytes(row.totalSizeInBytes);
      },
      sortable: true,
      align: 'right'
    },
    {
      id: 'availableSpaceInBytes',
      header: STRINGS.COLUMNS.SPACE,
      accessor: (row) => {
        if (row.unavailable) return STRINGS.UNKNOWN;
        if (row.unlimited) return STRINGS.UNLIMITED;
        return formatBytes(row.availableSpaceInBytes);
      },
      sortable: true,
      align: 'right'
    }
  ], [isCalculating]);

  // Empty state component
  const emptyState = (
    <EmptyState
      icon={HardDrive}
      title={STRINGS.EMPTY.TITLE}
      description={STRINGS.EMPTY.DESCRIPTION}
      action={canCreate ? {
        label: STRINGS.EMPTY.ACTION,
        onClick: handleCreate,
        icon: Plus
      } : undefined}
    />
  );

  return (
    <div className="blob-stores-list">
      <PageHeader
        icon={HardDrive}
        title={STRINGS.TITLE}
        description={STRINGS.DESCRIPTION}
        actions={
          <Button
            size="2"
            variant="solid"
            onClick={handleCreate}
            disabled={!canCreate}
          >
            <Plus size={16} />
            {STRINGS.CREATE_BUTTON}
          </Button>
        }
      >
        <TextField.Root
          placeholder={STRINGS.FILTER_PLACEHOLDER}
          value={filterText}
          onChange={(e) => setFilterText(e.target.value)}
          className="blob-stores-list__filter"
        >
          <TextField.Slot>
            <Search size={16} />
          </TextField.Slot>
        </TextField.Root>
      </PageHeader>

      <Flex className="blob-stores-list__content">
        <div className="blob-stores-list__main">
          <EntityTable<BlobStore>
            data={sortedData}
            columns={columns}
            getRowKey={(row) => row.name}
            onRowClick={handleRowClick}
            loading={loading}
            loadingMessage={STRINGS.LOADING}
            error={error || undefined}
            onRetry={refetch}
            emptyState={emptyState}
            sortBy={sortBy}
            sortDirection={sortDirection}
            onSort={handleSort}
            ariaLabel="Blob stores table"
          />
        </div>

        <aside className="blob-stores-list__sidebar">
          <HelpSection
            title={STRINGS.HELP.TITLE}
            content={STRINGS.HELP.CONTENT}
            docLink={{
              label: STRINGS.HELP.DOC_LABEL,
              href: 'http://links.sonatype.com/products/nxrm3/docs/blob-store'
            }}
          />
        </aside>
      </Flex>
    </div>
  );
}
