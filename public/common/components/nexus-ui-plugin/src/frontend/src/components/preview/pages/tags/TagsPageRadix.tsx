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

import React, { useCallback, useEffect, useRef, useState } from 'react';
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
import { useToast } from '../../shared/Toast';
import { parseApiError } from '../../../../interface/api';
import { ExtJS } from '../../../../interface/ExtJS';
import Permissions from '../../../../constants/Permissions';

import { PageHeader, TablePagination } from '../../shared';
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
  learnMoreUrl: 'https://help.sonatype.com/en/tagging.html',
};

const TAG_NAME_REGEX = /^[a-zA-Z0-9-]{1}[a-zA-Z0-9_\-.]*$/;
const TAG_NAME_VALIDATION_MESSAGE =
  'Tag name is invalid. Only letters, digits, hyphens (-), underscores (_), and dots (.) are allowed and may not start with underscore or dot.';

const TAGS_SORT_OPTIONS: Array<{ value: TagSortField; label: string }> = [
  { value: 'name', label: 'Name' },
  { value: 'componentCount', label: 'Components' },
  { value: 'firstCreated', label: 'Created' },
  { value: 'lastUpdated', label: 'Last Updated' },
];

/** Delay before a typed name filter is committed to the server-side fetch. */
const NAME_FILTER_DEBOUNCE_MS = 300;

/**
 * Tags list page aligned with Browse layout: same structure, spacing, and components.
 */
export function TagsPageRadix(): JSX.Element {
  // Use the provider-independent ExtJS.usePermission (reads window.NX.Permissions.check
  // directly and stays reactive to login/permission changes). The context-based
  // usePermission returns false without a <PermissionsProvider> ancestor, which coreui
  // (self-hosted) never mounts — that disabled this button for everyone, admins included
  // (NEXUS-54212). Depend on hasUser so the check re-evaluates once the user and their
  // permissions have loaded (permissions arrive asynchronously after mount); without it a
  // non-admin who holds nexus:tags:create would stay stuck on the initial false.
  const hasUser = ExtJS.useUser() ?? false;
  const canCreateTag = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.TAGS.CREATE),
    [hasUser],
  );
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
    totalUnfilteredItems,
    setFilters,
    toggleSort,
    setPage,
    setPageSize,
    retry,
    refresh,
  } = useFilteredTags();

  const toast = useToast();
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newTagName, setNewTagName] = useState('');
  const [tagNameError, setTagNameError] = useState('');
  const [isCreating, setIsCreating] = useState(false);
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  // Local mirror of the name filter so the input stays responsive while the
  // committed value (which triggers a server-side fetch) is debounced.
  const [nameInput, setNameInput] = useState(filters.nameFilter);
  const nameDebounceRef = useRef<ReturnType<typeof setTimeout>>();
  // Latest filters, read inside the debounce timeout to avoid a stale merge.
  const filtersRef = useRef(filters);
  filtersRef.current = filters;

  // Keep the input in sync when the committed filter changes from elsewhere
  // (e.g. "clear all"), but never fight an in-flight debounce.
  useEffect(() => {
    if (!nameDebounceRef.current) {
      setNameInput(filters.nameFilter);
    }
  }, [filters.nameFilter]);

  // Clear any pending debounce on unmount.
  useEffect(
    () => () => {
      if (nameDebounceRef.current) {
        clearTimeout(nameDebounceRef.current);
      }
    },
    []
  );

  const handleTagNameChange = useCallback((value: string) => {
    setNewTagName(value);
    if (!value.trim() || TAG_NAME_REGEX.test(value.trim())) {
      setTagNameError('');
    } else {
      setTagNameError(TAG_NAME_VALIDATION_MESSAGE);
    }
  }, []);

  const handleCreateTag = useCallback(async () => {
    const trimmed = newTagName.trim();
    if (!trimmed) return;

    if (!TAG_NAME_REGEX.test(trimmed)) {
      setTagNameError(TAG_NAME_VALIDATION_MESSAGE);
      return;
    }

    setIsCreating(true);
    try {
      await createTag(trimmed);
      toast.success(`Tag "${trimmed}" created successfully`);
      setCreateDialogOpen(false);
      setNewTagName('');
      setTagNameError('');
      refresh();
    } catch (err) {
      toast.error(parseApiError(err).message);
    } finally {
      setIsCreating(false);
    }
  }, [newTagName, toast, refresh]);

  const handleNameFilterChange = useCallback(
    (value: string) => {
      setNameInput(value);
      if (nameDebounceRef.current) {
        clearTimeout(nameDebounceRef.current);
      }
      nameDebounceRef.current = setTimeout(() => {
        nameDebounceRef.current = undefined;
        setFilters({ ...filtersRef.current, nameFilter: value });
      }, NAME_FILTER_DEBOUNCE_MS);
    },
    [setFilters]
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
    if (nameDebounceRef.current) {
      clearTimeout(nameDebounceRef.current);
      nameDebounceRef.current = undefined;
    }
    setNameInput('');
    setFilters({ nameFilter: '', componentCountRanges: [], activityDays: [] });
  }, [setFilters]);

  const hasActiveFilters =
    filters.componentCountRanges.length > 0 || filters.activityDays.length > 0;

  const hasAnyFilter = hasActiveFilters || !!filters.nameFilter;
  const headerCount = hasAnyFilter ? totalItems : (totalUnfilteredItems ?? totalItems);

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
                count={loading ? '-' : headerCount.toLocaleString()}
                actions={
                  <Button
                    variant="solid"
                    size="2"
                    onClick={() => setCreateDialogOpen(true)}
                    disabled={!canCreateTag}
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
                    value={nameInput}
                    onChange={(e) => handleNameFilterChange(e.target.value)}
                    size="2"
                    style={{ width: '100%' }}
                  >
                    <TextField.Slot>
                      <Search size={14} />
                    </TextField.Slot>
                    {nameInput && (
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
                  hasFilters={hasAnyFilter}
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
      <AlertDialog.Root
        open={createDialogOpen}
        onOpenChange={(open) => {
          setCreateDialogOpen(open);
          if (!open) {
            setNewTagName('');
            setTagNameError('');
          }
        }}
      >
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
              onChange={(e) => handleTagNameChange(e.target.value)}
              onKeyDown={(e) =>
                e.key === 'Enter' && newTagName.trim() && !tagNameError && handleCreateTag()
              }
              autoFocus
            />
            {tagNameError && (
              <Text size="1" color="red" mt="1" as="p">
                {tagNameError}
              </Text>
            )}
          </Box>
          <Flex gap="3" mt="4" justify="end">
            <AlertDialog.Cancel>
              <Button variant="soft" color="gray" disabled={isCreating}>
                Cancel
              </Button>
            </AlertDialog.Cancel>
            <Button
              variant="solid"
              onClick={handleCreateTag}
              disabled={isCreating || !newTagName.trim() || !!tagNameError}
            >
              {isCreating ? 'Creating...' : 'Create'}
            </Button>
          </Flex>
        </AlertDialog.Content>
      </AlertDialog.Root>
    </Box>
  );
}

export default TagsPageRadix;
