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

import React, { useCallback, useState } from 'react';
import {
  Box,
  Button,
  Flex,
  Grid,
  IconButton,
  Select,
  TextField,
} from '@radix-ui/themes';
import { Filter, Search, X } from 'lucide-react';
import { useRouter } from '@uirouter/react';

import { PageHeader } from '../../shared';
import { useUploadableRepositories } from './hooks/useUploadableRepositories';
import { UploadFilterSidebar } from './components/UploadFilterSidebar';
import { UploadRepositoryTable } from './components/UploadRepositoryTable';
import { MobileFilterDrawer } from '../search/unified/MobileFilterDrawer';
import { UPLOAD_STRINGS } from './upload.types';
import type { SortColumn } from './upload.types';

import '../search/unified/SearchSidebar.scss';

const UPLOAD_FORM_ROUTE = 'preview.browse.upload.form';

/**
 * UploadRepositoryListPage displays a list of hosted repositories that support file uploads.
 * Layout matches Browse list: Grid with filter sidebar, header, and table.
 */
export function UploadRepositoryListPage(): JSX.Element {
  const router = useRouter();
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  const {
    repositories,
    loading,
    error,
    filterText,
    selectedFormats,
    formatOptions,
    hasActiveFilters,
    sortColumn,
    sortDirection,
    handleSort,
    handleSortChange,
    handleFilterChange,
    clearFilter,
    toggleFormat,
    setFormats,
    clearAllFilters,
  } = useUploadableRepositories();

  const handleSelectRepository = useCallback(
    (repositoryName: string) => {
      router.stateService.go(UPLOAD_FORM_ROUTE, { repoName: repositoryName });
    },
    [router]
  );

  const handleInputChange = useCallback(
    (event: React.ChangeEvent<HTMLInputElement>) => {
      handleFilterChange(event.target.value);
    },
    [handleFilterChange]
  );

  const handleKeyDown = useCallback(
    (event: React.KeyboardEvent<HTMLInputElement>) => {
      if (event.key === 'Escape') {
        clearFilter();
      }
    },
    [clearFilter]
  );

  const handleFormatsChange = useCallback(
    (values: string[]) => {
      setFormats(values);
    },
    [setFormats]
  );

  const sortField = sortColumn ?? 'name';
  const sortDir = sortDirection ?? 'asc';

  const SORT_VALUES = [
    { value: 'name-asc', label: 'Name (Ascending)' },
    { value: 'name-desc', label: 'Name (Descending)' },
    { value: 'format-asc', label: 'Format (Ascending)' },
    { value: 'format-desc', label: 'Format (Descending)' },
  ] as const;

  const sortValue = `${sortField}-${sortDir}`;

  const renderSortDropdown = () => (
    <Select.Root
      value={sortValue}
      onValueChange={(v) => {
        const [col, dir] = v.split('-') as [SortColumn, 'asc' | 'desc'];
        handleSortChange(col, dir);
      }}
      size="2"
    >
      <Select.Trigger
        placeholder="Name (Ascending)"
        className="upload-list-page__sort-trigger"
      />
      <Select.Content position="popper" side="bottom" avoidCollisions={false} sideOffset={4}>
        {SORT_VALUES.map((opt) => (
          <Select.Item key={opt.value} value={opt.value}>
            {opt.label}
          </Select.Item>
        ))}
      </Select.Content>
    </Select.Root>
  );

  const filterBarContent = (
    <UploadFilterSidebar
      formatOptions={formatOptions}
      selectedFormats={selectedFormats}
      sortColumn={sortColumn}
      sortDirection={sortDirection}
      onFormatsChange={handleFormatsChange}
      onSortChange={handleSortChange}
      onResetFilters={clearAllFilters}
      disabled={loading}
    />
  );

  return (
    <Box
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
      width="100%"
      className="upload-list-page"
      data-testid="upload-page"
    >
      <Flex direction="column" gap="6" width="100%" className="upload-list-page__outer">
        <Grid
          columns={{ initial: '1', sm: '250px 1fr' }}
          gap="6"
          width="100%"
          className="upload-list-page__grid"
        >
          {/* Filter Sidebar — hidden on mobile */}
          <Box
            className="filter-bar upload-list-page__sidebar"
            display={{ initial: 'none', sm: 'block' }}
            role="complementary"
            aria-label="Filter bar"
          >
            <aside className="search-sidebar">{filterBarContent}</aside>
          </Box>

          {/* Main Content */}
          <Box
            className="page-content"
            minWidth="0"
            width="100%"
            role="main"
            aria-label="Page content"
          >
            <Box mb="4">
              <PageHeader
                title="Upload"
                description={`${repositories.length} repositories`}
              />
            </Box>
            <Box mb="4" role="toolbar" aria-label="Actions bar">
              <Flex
                className="actions-bar upload-list-page__actions-bar"
                align="center"
                gap="3"
                wrap="wrap"
              >
                <Box className="upload-list-page__filter-input-wrapper">
                  <TextField.Root
                    placeholder={UPLOAD_STRINGS.filterPlaceholder}
                    value={filterText}
                    onChange={handleInputChange}
                    onKeyDown={handleKeyDown}
                    size="2"
                    className="upload-list-page__filter-input"
                    aria-label="Filter repositories by name"
                  >
                    <TextField.Slot>
                      <Search size={14} />
                    </TextField.Slot>
                    {filterText && (
                      <TextField.Slot side="right">
                        <IconButton
                          variant="ghost"
                          color="gray"
                          size="1"
                          onClick={clearFilter}
                          aria-label="Clear filter"
                        >
                          <X size={14} />
                        </IconButton>
                      </TextField.Slot>
                    )}
                  </TextField.Root>
                </Box>
                <Button
                  variant="outline"
                  size="2"
                  color="gray"
                  onClick={() => setShowMobileFilters(true)}
                  aria-label="Open filters"
                  display={{ initial: 'flex', sm: 'none' }}
                >
                  <Filter size={14} />
                  Filter
                </Button>
                {renderSortDropdown()}
              </Flex>
            </Box>

            {/* Table */}
            <UploadRepositoryTable
              repositories={repositories}
              loading={loading}
              error={error}
              sortColumn={sortColumn}
              sortDirection={sortDirection}
              onSort={(col, dir) => handleSortChange(col, dir)}
              onSelect={handleSelectRepository}
            />
          </Box>
        </Grid>
      </Flex>

      <MobileFilterDrawer
        isOpen={showMobileFilters}
        onClose={() => setShowMobileFilters(false)}
        title="Filter"
        onClearAll={clearAllFilters}
      >
        {filterBarContent}
      </MobileFilterDrawer>
    </Box>
  );
}

export default UploadRepositoryListPage;
