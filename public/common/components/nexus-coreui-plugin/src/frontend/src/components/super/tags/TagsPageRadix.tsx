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
  AlertDialog,
  Box,
  Button,
  Flex,
  Grid,
  IconButton,
  Select,
  Text,
  TextField,
} from '@radix-ui/themes';
import { ArrowUpDown, ExternalLink, Filter, Info, Plus, Search, X } from 'lucide-react';
import { useFilteredTags, type TagSortField } from './hooks/useFilteredTags';
import { createTag } from './tags.api';
import { useToast } from '../../../components/shared/Toast';

import { PageHeader, TablePagination } from '../../../components/shared';
import { TagsFilterSidebar } from './TagsFilterSidebar';
import { TagsTable } from './TagsTable';
import { MobileFilterDrawer } from '../search/unified/MobileFilterDrawer';

import '../search/unified/SearchSidebar.scss';

const STRINGS = {
  pageTitle: 'Tags',
  filterPlaceholder: 'Filter tags by name...',
  createTag: 'Create Tag',
  aboutTags: 'About Tags',
  aboutTagsDescription:
    'Tags let you label and organize components across repositories. Use tags to mark release candidates, track deployments, or group related components. Tags can be applied to any component via the REST API or through automation workflows.',
  learnMore: 'Learn more about tags',
  learnMoreUrl: 'http://links.sonatype.com/products/nxrm3/docs/tags',
};

const TAGS_SORT_OPTIONS: Array<{ value: TagSortField; label: string }> = [
  { value: 'name', label: 'Name' },
  { value: 'componentCount', label: 'Components' },
  { value: 'firstCreated', label: 'Created' },
  { value: 'lastUpdated', label: 'Last Updated' },
];

/**
 * Tags list page aligned with Browse layout: same structure, spacing, and components.
 */
export function TagsPageRadix(): JSX.Element {
  const {
    tags,
    loading,
    error,
    filters,
    sortField,
    sortDirection,
    currentPage,
    pageSize,
    totalItems,
    setFilters,
    toggleSort,
    setPage,
    setPageSize,
    retry,
  } = useFilteredTags();

  const toast = useToast();
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  const handleCreateTag = useCallback(async () => {
    if (!newTagName.trim()) return;

    setIsCreating(true);
    try {
      await createTag(newTagName.trim());
      toast.success(`Tag "${newTagName.trim()}" created successfully`);
      setCreateDialogOpen(false);
      setNewTagName('');
      retry();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to create tag';
      toast.error(message);
    } finally {
      setIsCreating(false);
    }
  }, [newTagName, toast, retry]);

  const handleNameFilterChange = useCallback(
    (value: string) => {
      setFilters({ ...filters, nameFilter: value });
    },
    [filters, setFilters]
  );

  const toggleComponentCountRange = useCallback(
    (range: string) => {
      const newRanges = filters.componentCountRanges.includes(range)
        ? filters.componentCountRanges.filter((r) => r !== range)
        : [...filters.componentCountRanges, range];
      setFilters({ ...filters, componentCountRanges: newRanges });
    },
    [filters, setFilters]
  );

  const toggleActivityDays = useCallback(
    (days: number) => {
      const newDays = filters.activityDays.includes(days)
        ? filters.activityDays.filter((d) => d !== days)
        : [...filters.activityDays, days];
      setFilters({ ...filters, activityDays: newDays });
    },
    [filters, setFilters]
  );

  const clearAllFilters = useCallback(() => {
    setFilters({ nameFilter: '', componentCountRanges: [], activityDays: [] });
  }, [setFilters]);

  const hasActiveFilters =
    filters.componentCountRanges.length > 0 || filters.activityDays.length > 0;

  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

  const renderSortDropdown = (triggerStyle?: React.CSSProperties) => (
    <Select.Root
      value={sortField}
      onValueChange={(field) => toggleSort(field as TagSortField)}
      size="2"
    >
      <Select.Trigger style={{ width: 180, flexShrink: 0, ...triggerStyle }}>
        <Flex align="center" gap="2">
          <ArrowUpDown size={14} aria-hidden />
          <Text size="2">sort:</Text>
          <Text size="2">
            {TAGS_SORT_OPTIONS.find((o) => o.value === sortField)?.label ?? 'Name'}
          </Text>
        </Flex>
      </Select.Trigger>
      <Select.Content position="popper" side="bottom" avoidCollisions={false} sideOffset={4}>
        {TAGS_SORT_OPTIONS.map((opt) => (
          <Select.Item key={opt.value} value={opt.value}>
            {opt.label}
          </Select.Item>
        ))}
      </Select.Content>
    </Select.Root>
  );

  const filterBarContent = (
    <TagsFilterSidebar
      filters={filters}
      onToggleComponentCountRange={toggleComponentCountRange}
      onToggleActivityDays={toggleActivityDays}
      onClearAllFilters={clearAllFilters}
      hasActiveFilters={hasActiveFilters}
      disabled={loading}
    />
  );

  return (
    <Box
      data-testid="tags-page"
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
      width="100%"
      style={{ minWidth: 0, boxSizing: 'border-box' }}
    >
      <Flex direction="column" gap="6" width="100%" style={{ minWidth: 0 }}>
        <Grid columns={{ initial: '1', sm: '250px 1fr' }} gap="6" width="100%" style={{ minWidth: 0 }}>
          {/* Filter Sidebar — hidden on mobile */}
          <Box
            className="filter-bar"
            display={{ initial: 'none', sm: 'block' }}
            style={{ overflow: 'visible', minWidth: 0 }}
            role="complementary"
            aria-label="Filter bar"
          >
            <aside className="search-sidebar">{filterBarContent}</aside>
          </Box>

          {/* Main Content */}
          <Box className="page-content" minWidth="0" width="100%" role="main" aria-label="Page content">
            <Box mb="4">
              <PageHeader
                title={STRINGS.pageTitle}
                description={totalItems.toLocaleString()}
                actions={
                  <Button
                    variant="solid"
                    size="2"
                    onClick={() => setCreateDialogOpen(true)}
                    data-testid="create-tag-button"
                  >
                    <Plus size={14} />
                    {STRINGS.createTag}
                  </Button>
                }
              />
            </Box>
            <Box mb="4" role="toolbar" aria-label="Actions bar">
              <Flex
                className="actions-bar"
                align="center"
                gap="3"
                wrap="wrap"
                style={{ width: '100%' }}
              >
                <Box style={{ flex: 1, minWidth: 200 }}>
                  <TextField.Root
                    placeholder={STRINGS.filterPlaceholder}
                    value={filters.nameFilter}
                    onChange={(e) => handleNameFilterChange(e.target.value)}
                    size="2"
                    style={{ width: '100%' }}
                  >
                    <TextField.Slot>
                      <Search size={14} />
                    </TextField.Slot>
                    {filters.nameFilter && (
                      <TextField.Slot side="right">
                        <IconButton
                          variant="ghost"
                          color="gray"
                          size="1"
                          onClick={() => handleNameFilterChange('')}
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

            <Flex direction="column" gap="3" style={{ flex: 1 }}>
                <TagsTable
                  tags={tags}
                  loading={loading}
                  error={error}
                  hasFilters={hasActiveFilters || !!filters.nameFilter}
                  sortField={sortField}
                  sortDirection={sortDirection}
                  onSort={toggleSort}
                  onRetry={retry}
                />

                {totalItems > 0 && (
                  <Box p="3">
                    <TablePagination
                      currentPage={currentPage + 1}
                      totalPages={totalPages}
                      itemsPerPage={pageSize}
                      totalItems={totalItems}
                      onPageChange={(page) => setPage(page - 1)}
                      onItemsPerPageChange={setPageSize}
                      mt="0"
                    />
                  </Box>
                )}
              </Flex>
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

      {/* Help Section */}
      <Box mt="6" p="4">
        <Flex align="center" gap="2" mb="2">
          <Info size={16} />
          <Text size="2" weight="medium">
            {STRINGS.aboutTags}
          </Text>
        </Flex>
        <Text as="p" size="2" color="gray">
          {STRINGS.aboutTagsDescription}
        </Text>
        <Text as="p" size="2" color="gray" mt="1">
          <a
            href={STRINGS.learnMoreUrl}
            target="_blank"
            rel="noopener noreferrer"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}
          >
            {STRINGS.learnMore}
            <ExternalLink size={12} />
          </a>
        </Text>
      </Box>

      {/* Create Tag Dialog */}
      <AlertDialog.Root open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <AlertDialog.Content maxWidth="450px">
          <AlertDialog.Title>Create Tag</AlertDialog.Title>
          <AlertDialog.Description size="2">
            Enter a name for the new tag. Tags can be applied to components via the REST API.
          </AlertDialog.Description>
          <Box mt="3">
            <Text as="label" size="2" weight="medium" mb="1">
              Tag Name
            </Text>
            <TextField.Root
              size="2"
              placeholder="e.g., release-1.0, staging, approved"
              value={newTagName}
              onChange={(e) => setNewTagName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && newTagName.trim() && handleCreateTag()}
              autoFocus
            />
          </Box>
          <Flex gap="3" mt="4" justify="end">
            <AlertDialog.Cancel>
              <Button variant="soft" color="gray" disabled={isCreating}>
                Cancel
              </Button>
            </AlertDialog.Cancel>
            <AlertDialog.Action>
              <Button
                variant="solid"
                onClick={handleCreateTag}
                disabled={isCreating || !newTagName.trim()}
              >
                {isCreating ? 'Creating...' : 'Create'}
              </Button>
            </AlertDialog.Action>
          </Flex>
        </AlertDialog.Content>
      </AlertDialog.Root>
    </Box>
  );
}

export default TagsPageRadix;
