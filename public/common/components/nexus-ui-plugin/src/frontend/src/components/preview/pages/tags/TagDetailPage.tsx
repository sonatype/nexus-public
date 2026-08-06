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

import React, { useState, useMemo, useCallback, useRef } from 'react';
import {
  Badge,
  Box,
  Button,
  Card,
  Flex,
  Heading,
  ScrollArea,
  Select,
  Spinner,
  Table,
  Text,
  TextField,
  Tooltip,
} from '@radix-ui/themes';
import {
  Calendar,
  ChevronDown,
  ChevronUp,
  Clock,
  Download,
  ExternalLink,
  Package,
  Search,
  Tag,
  Trash2,
  X,
} from 'lucide-react';
import { useRouter, useCurrentStateAndParams } from '@uirouter/react';

import { useToast } from '../../shared/Toast';
import { ConfirmDialog } from '../../shared/form';
import { exportToCsv, PageHeader, useRouteVisibility } from '../../shared';

import { useTagDetailExtended, type TaggedComponent } from './hooks';
import { useSearchNavigation } from '../search/unified/useSearchNavigation';

import './TagDetailPage.scss';

/**
 * Sort configuration.
 */
type SortField = 'name' | 'format' | 'version' | 'repository';
type SortDirection = 'asc' | 'desc';

/**
 * UI Strings.
 */
const STRINGS = {
  backToTags: 'Tags',
  loading: 'Loading tag details...',
  loadingComponents: 'Loading tagged components...',
  error: 'Failed to load tag details',
  retry: 'Retry',

  header: {
    created: 'Created',
    lastUpdated: 'Last Updated',
    totalComponents: 'Total Components',
  },

  components: {
    title: 'Tagged Components',
    searchPlaceholder: 'Search components...',
    formatFilter: 'Format',
    repositoryFilter: 'Repository',
    allFormats: 'All Formats',
    allRepositories: 'All Repositories',
    exportCsv: 'Export CSV',
    empty: 'No components tagged with this tag yet.',
    emptyFiltered: 'No components match your filters.',
    error: 'Failed to load tagged components',
    loadMoreError: 'Failed to load more components',
    retry: 'Retry',
  },

  table: {
    component: 'Component',
    format: 'Format',
    version: 'Version',
    repository: 'Repository',
    actions: '',
  },

  actions: {
    view: 'View Details',
    remove: 'Remove Tag',
    delete: 'Delete Tag',
  },

  deleteDialog: {
    title: 'Delete Tag',
    message: 'Are you sure you want to delete this tag?',
    warning: 'This will remove the tag from all associated components. This action cannot be undone.',
    cancel: 'Cancel',
    confirm: 'Delete',
  },
};

/**
 * Format date for display.
 */
function formatDate(dateString: string | null): string {
  if (!dateString) return '-';
  try {
    return new Date(dateString).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return dateString;
  }
}

/**
 * Get format badge color.
 */
function getFormatColor(format: string): 'red' | 'blue' | 'purple' | 'cyan' | 'orange' | 'green' | 'gray' {
  const colorMap: Record<string, 'red' | 'blue' | 'purple' | 'cyan' | 'orange' | 'green' | 'gray'> = {
    maven2: 'red',
    npm: 'red',
    nuget: 'blue',
    pypi: 'blue',
    docker: 'cyan',
    helm: 'purple',
    go: 'cyan',
    rubygems: 'red',
    yum: 'orange',
    apt: 'purple',
    raw: 'gray',
  };
  return colorMap[format.toLowerCase()] || 'gray';
}

/**
 * TagDetailPage - Full page view of a single tag with its components.
 *
 * This component follows the three-layer architecture:
 * - Layer 1: tagDetailMachine (XState state machine)
 * - Layer 2: useTagDetailExtended (integration hook)
 * - Layer 3: TagDetailPage (presentation component)
 *
 * The component contains only presentation logic:
 * - Rendering UI based on state
 * - Handling user input events
 * - Displaying validation errors
 * - Showing loading/error states
 */
export function TagDetailPage(): JSX.Element {
  const router = useRouter();
  const { params } = useCurrentStateAndParams();
  const tagName = params.tagName as string;
  const toast = useToast();
  const { navigateToDetail } = useSearchNavigation();

  // Check if user can navigate to Search component detail (same check the left-nav uses)
  const canOpenComponent = useRouteVisibility('browse.search');

  // Local UI state (filters, sort, etc.)
  const [searchFilter, setSearchFilter] = useState('');
  const [formatFilter, setFormatFilter] = useState('');
  const [repositoryFilter, setRepositoryFilter] = useState('');
  const [sortField, setSortField] = useState<SortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  // Tracks a confirm click so the dialog's synchronous auto-close can be ignored
  // while the delete is in flight (see handleDeleteDialogOpenChange).
  const confirmingDeleteRef = useRef(false);

  /**
   * Navigate back to tags list.
   */
  const handleBack = useCallback(() => {
    router.stateService.go('preview.browse.tags');
  }, [router]);

  // Use the XState-powered hook
  const {
    tagDetail,
    tagLoading,
    tagError,
    components,
    componentsLoading,
    componentsError,
    hasMoreComponents,
    totalComponentCount,
    loadMoreComponents,
    retry,
    deleteTag,
    deleting,
  } = useTagDetailExtended({
    tagName,
    onDeleted: useCallback(() => {
      setDeleteDialogOpen(false);
      toast.success(`Tag "${tagName}" deleted`);
      handleBack();
    }, [tagName, toast, handleBack]),
    onDeleteError: useCallback(
      (message: string) => {
        toast.error(message);
      },
      [toast]
    ),
  });

  /**
   * Get unique formats from components.
   */
  const formats = useMemo(() => {
    const formatSet = new Set(components.map((c) => c.format));
    return Array.from(formatSet).sort();
  }, [components]);

  /**
   * Get unique repositories from components.
   */
  const repositories = useMemo(() => {
    const repoSet = new Set(components.map((c) => c.repository));
    return Array.from(repoSet).sort();
  }, [components]);

  /**
   * Filter and sort components.
   */
  const filteredComponents = useMemo(() => {
    let filtered = [...components];

    // Apply search filter
    if (searchFilter) {
      const search = searchFilter.toLowerCase();
      filtered = filtered.filter(
        (c) =>
          c.name.toLowerCase().includes(search) ||
          (c.group?.toLowerCase().includes(search)) ||
          c.repository.toLowerCase().includes(search)
      );
    }

    // Apply format filter
    if (formatFilter) {
      filtered = filtered.filter((c) => c.format === formatFilter);
    }

    // Apply repository filter
    if (repositoryFilter) {
      filtered = filtered.filter((c) => c.repository === repositoryFilter);
    }

    // Sort
    filtered.sort((a, b) => {
      let comparison = 0;
      switch (sortField) {
        case 'name':
          comparison = (a.name || '').localeCompare(b.name || '');
          break;
        case 'format':
          comparison = a.format.localeCompare(b.format);
          break;
        case 'version':
          comparison = (a.version || '').localeCompare(b.version || '');
          break;
        case 'repository':
          comparison = a.repository.localeCompare(b.repository);
          break;
      }
      return sortDirection === 'desc' ? -comparison : comparison;
    });

    return filtered;
  }, [components, searchFilter, formatFilter, repositoryFilter, sortField, sortDirection]);

  /**
   * Handle sort toggle.
   */
  const handleSort = useCallback(
    (field: SortField) => {
      if (sortField === field) {
        setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
      } else {
        setSortField(field);
        setSortDirection('asc');
      }
    },
    [sortField]
  );

  /**
   * Navigate to component detail.
   */
  const handleViewComponent = useCallback(
    (component: TaggedComponent) => {
      // Safety net: navigation needs both search-route permission and a component
      // name to build the gaId. Rows without both are rendered non-interactive
      // (see isRowInteractive below), so this guard should never fire in practice.
      if (!canOpenComponent || !component.name) {
        return;
      }
      // Navigate using the unified search navigation helper, which builds the
      // correct gaId and calls the component detail route. Assets are not required.
      navigateToDetail({
        id: component.id,
        name: component.name,
        format: component.format,
        repository: component.repository,
        group: component.group ?? undefined,
        // version is a required string on SearchResult; navigateToDetail treats a
        // falsy value as "no version" (the detail page then auto-selects the first).
        version: component.version ?? '',
      });
    },
    [navigateToDetail, canOpenComponent]
  );

  /**
   * Handle keyboard navigation for component rows.
   */
  const handleRowKeyDown = useCallback(
    (component: TaggedComponent) => (e: React.KeyboardEvent) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleViewComponent(component);
      }
    },
    [handleViewComponent]
  );

  const handleExportCsv = useCallback(() => {
    exportToCsv(
      filteredComponents.map((c) => ({
        name: c.name,
        group: c.group,
        version: c.version,
        format: c.format,
        repository: c.repository,
      })),
      `${tagName}-components.csv`,
      ['name', 'group', 'version', 'format', 'repository']
    );
  }, [tagName, filteredComponents]);

  /**
   * Handle delete tag with confirmation. Fire-and-forget: the hook's onDeleted /
   * onDeleteError callbacks surface success and failure.
   */
  const handleDeleteTag = useCallback(() => {
    // ConfirmDialog fires onOpenChange(false) synchronously on confirm, before the
    // machine has entered 'deleting' (the loading prop is still false this render).
    // Flag the confirm so onOpenChange can swallow that one close and keep the
    // dialog open (with a spinner) until the delete resolves.
    confirmingDeleteRef.current = true;
    deleteTag();
  }, [deleteTag]);

  /**
   * Gate the dialog's open state: swallow the synchronous close triggered by the
   * confirm click (so the dialog stays open while deleting, and remains open on
   * error to allow a retry), but honor genuine cancels/dismissals.
   */
  const handleDeleteDialogOpenChange = useCallback((next: boolean) => {
    if (!next && confirmingDeleteRef.current) {
      confirmingDeleteRef.current = false;
      return;
    }
    setDeleteDialogOpen(next);
  }, []);

  /**
   * Render sort icon.
   */
  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) return null;
    return sortDirection === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />;
  };

  // Loading state
  if (tagLoading) {
    return (
      <Box
        className="tag-detail-page"
        px={{ initial: '4', md: '6', lg: '6' }}
        py={{ initial: '4', md: '5', lg: '6' }}
        width="100%"
        style={{ minWidth: 0, boxSizing: 'border-box' }}
      >
        <Flex align="center" justify="center" p="9" gap="3">
          <Spinner size="3" />
          <Text>{STRINGS.loading}</Text>
        </Flex>
      </Box>
    );
  }

  // Error state
  if (tagError) {
    return (
      <Box
        className="tag-detail-page"
        px={{ initial: '4', md: '6', lg: '6' }}
        py={{ initial: '4', md: '5', lg: '6' }}
        width="100%"
        style={{ minWidth: 0, boxSizing: 'border-box' }}
      >
        <PageHeader
          title={tagName || 'Tag Details'}
          breadcrumbs={[
            { label: 'Tags', onClick: handleBack },
            { label: tagName || 'Unknown' },
          ]}
        />
        <Flex direction="column" align="center" justify="center" p="9" gap="3">
          <Text color="red">{STRINGS.error}</Text>
          <Text color="gray" size="2">
            {tagError}
          </Text>
          <Button variant="soft" onClick={retry}>
            {STRINGS.retry}
          </Button>
        </Flex>
      </Box>
    );
  }

  return (
    <Box
      className="tag-detail-page"
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
      width="100%"
      style={{ minWidth: 0, boxSizing: 'border-box' }}
    >
      <PageHeader
        title={tagName || 'Tag Details'}
        breadcrumbs={[
          { label: 'Tags', onClick: handleBack },
          { label: tagName || 'Unknown' },
        ]}
      />

      <ScrollArea className="tag-detail-page__content">
        {/* Tag Info Card */}
        <Card className="tag-detail-page__info-card">
          <Flex align="center" gap="4" p="4">
            <Box className="tag-detail-page__icon-wrapper">
              <Tag size={32} />
            </Box>
            <Box style={{ flex: 1 }}>
              <Heading size="6">{tagName}</Heading>
              <Flex gap="4" mt="2" wrap="wrap">
                <Flex align="center" gap="2">
                  <Calendar size={14} className="tag-detail-page__meta-icon" />
                  <Text size="2" color="gray">
                    {STRINGS.header.created}:
                  </Text>
                  <Text size="2">{formatDate(tagDetail?.firstCreated || null)}</Text>
                </Flex>
                <Flex align="center" gap="2">
                  <Clock size={14} className="tag-detail-page__meta-icon" />
                  <Text size="2" color="gray">
                    {STRINGS.header.lastUpdated}:
                  </Text>
                  <Text size="2">{formatDate(tagDetail?.lastUpdated || null)}</Text>
                </Flex>
                <Flex align="center" gap="2">
                  <Package size={14} className="tag-detail-page__meta-icon" />
                  <Text size="2" color="gray">
                    {STRINGS.header.totalComponents}:
                  </Text>
                  {/* Show an explicit "unknown" indicator when the count could not be
                      resolved, rather than falling back to the loaded page size — that
                      fallback misrepresents a 500-component tag as its first-page count. */}
                  <Badge size="1" variant="soft" color="violet">
                    {totalComponentCount !== null ? totalComponentCount : '—'}
                  </Badge>
                </Flex>
              </Flex>
            </Box>
            <Button variant="soft" color="red" onClick={() => setDeleteDialogOpen(true)}>
              <Trash2 size={14} />
              {STRINGS.actions.delete}
            </Button>
          </Flex>
        </Card>

        {/* Components Section */}
        <Card className="tag-detail-page__components-card">
          <Box p="4">
            <Flex justify="between" align="center" mb="4">
              <Heading size="4">{STRINGS.components.title}</Heading>
              <Button variant="soft" onClick={handleExportCsv} disabled={filteredComponents.length === 0}>
                <Download size={14} />
                {STRINGS.components.exportCsv}
              </Button>
            </Flex>

            {/* Filters */}
            <Flex gap="3" mb="4" wrap="wrap" className="tag-detail-page__filters">
              <Box style={{ flex: 1, minWidth: '200px' }}>
                <TextField.Root
                  placeholder={STRINGS.components.searchPlaceholder}
                  value={searchFilter}
                  onChange={(e) => setSearchFilter(e.target.value)}
                >
                  <TextField.Slot>
                    <Search size={14} />
                  </TextField.Slot>
                  {searchFilter && (
                    <TextField.Slot>
                      <Button variant="ghost" size="1" onClick={() => setSearchFilter('')} data-testid="clear-search-btn">
                        <X size={14} />
                      </Button>
                    </TextField.Slot>
                  )}
                </TextField.Root>
              </Box>

              <Select.Root
                value={formatFilter || '__all__'}
                onValueChange={(v) => setFormatFilter(v === '__all__' ? '' : v)}
              >
                <Select.Trigger placeholder={STRINGS.components.allFormats} />
                <Select.Content>
                  <Select.Item value="__all__">{STRINGS.components.allFormats}</Select.Item>
                  {formats.map((format) => (
                    <Select.Item key={format} value={format}>
                      {format}
                    </Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>

              <Select.Root
                value={repositoryFilter || '__all__'}
                onValueChange={(v) => setRepositoryFilter(v === '__all__' ? '' : v)}
              >
                <Select.Trigger placeholder={STRINGS.components.allRepositories} />
                <Select.Content>
                  <Select.Item value="__all__">{STRINGS.components.allRepositories}</Select.Item>
                  {repositories.map((repo) => (
                    <Select.Item key={repo} value={repo}>
                      {repo}
                    </Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>
            </Flex>

            {/* Components Table */}
            {componentsLoading && components.length === 0 ? (
              <Flex align="center" justify="center" p="6" gap="2">
                <Spinner size="2" />
                <Text color="gray">{STRINGS.loadingComponents}</Text>
              </Flex>
            ) : componentsError && components.length === 0 ? (
              <Flex direction="column" align="center" justify="center" p="6" gap="3">
                <Text color="red">{STRINGS.components.error}</Text>
                <Text color="gray" size="2">
                  {componentsError}
                </Text>
                <Button variant="soft" onClick={retry}>
                  {STRINGS.components.retry}
                </Button>
              </Flex>
            ) : filteredComponents.length === 0 ? (
              <Flex direction="column" align="center" justify="center" p="6" gap="2">
                <Package size={32} color="var(--gray-9)" />
                <Text color="gray">
                  {components.length === 0 ? STRINGS.components.empty : STRINGS.components.emptyFiltered}
                </Text>
              </Flex>
            ) : (
              <Table.Root>
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell onClick={() => handleSort('name')} style={{ cursor: 'pointer' }}>
                      <Flex align="center" gap="1">
                        {STRINGS.table.component}
                        <SortIcon field="name" />
                      </Flex>
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell onClick={() => handleSort('format')} style={{ cursor: 'pointer' }}>
                      <Flex align="center" gap="1">
                        {STRINGS.table.format}
                        <SortIcon field="format" />
                      </Flex>
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell onClick={() => handleSort('version')} style={{ cursor: 'pointer' }}>
                      <Flex align="center" gap="1">
                        {STRINGS.table.version}
                        <SortIcon field="version" />
                      </Flex>
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell onClick={() => handleSort('repository')} style={{ cursor: 'pointer' }}>
                      <Flex align="center" gap="1">
                        {STRINGS.table.repository}
                        <SortIcon field="repository" />
                      </Flex>
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>{STRINGS.table.actions}</Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {filteredComponents.map((component) => {
                    // A row is interactive only when the user can open the Search
                    // component route AND the component has a name to build the gaId.
                    const isRowInteractive = canOpenComponent && Boolean(component.name);
                    return (
                    <Table.Row
                      key={component.id}
                      className={`tag-detail-page__component-row${isRowInteractive ? ' tag-detail-page__component-row--clickable' : ''}`}
                      {...(isRowInteractive && {
                        onClick: () => handleViewComponent(component),
                        onKeyDown: handleRowKeyDown(component),
                        tabIndex: 0,
                        role: 'button',
                        'aria-label': `View ${component.name}`,
                      })}
                    >
                      <Table.Cell>
                        <Flex direction="column" gap="1">
                          <Text weight="medium">{component.name}</Text>
                          {component.group && (
                            <Text size="1" color="gray" style={{ fontFamily: 'monospace' }}>
                              {component.group}
                            </Text>
                          )}
                        </Flex>
                      </Table.Cell>
                      <Table.Cell>
                        <Badge color={getFormatColor(component.format)} variant="soft">
                          {component.format}
                        </Badge>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2">{component.version || '-'}</Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2">{component.repository}</Text>
                      </Table.Cell>
                      <Table.Cell>
                        {isRowInteractive && (
                          <Tooltip content={STRINGS.actions.view}>
                            <Button
                              variant="ghost"
                              size="1"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleViewComponent(component);
                              }}
                            >
                              <ExternalLink size={14} />
                            </Button>
                          </Tooltip>
                        )}
                      </Table.Cell>
                    </Table.Row>
                    );
                  })}
                </Table.Body>
              </Table.Root>
            )}

            {/* Load-more error: components are already loaded (the empty-state banner
                above only covers the initial-load failure), so surface the failure
                inline near the table with a retry rather than swallowing it. */}
            {componentsError && components.length > 0 && (
              <Flex direction="column" align="center" mt="4" gap="2">
                <Text color="red" size="2">
                  {STRINGS.components.loadMoreError}
                </Text>
                <Text color="gray" size="1">
                  {componentsError}
                </Text>
                <Button variant="soft" onClick={loadMoreComponents}>
                  {STRINGS.components.retry}
                </Button>
              </Flex>
            )}

            {/* Load More */}
            {hasMoreComponents && !componentsLoading && !componentsError && (
              <Flex justify="center" mt="4">
                <Button variant="soft" onClick={loadMoreComponents}>
                  Load More
                </Button>
              </Flex>
            )}
          </Box>
        </Card>
      </ScrollArea>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        testId="delete-tag-dialog"
        onOpenChange={handleDeleteDialogOpenChange}
        loading={deleting}
        title={STRINGS.deleteDialog.title}
        message={STRINGS.deleteDialog.message}
        confirmLabel={STRINGS.deleteDialog.confirm}
        cancelLabel={STRINGS.deleteDialog.cancel}
        variant="danger"
        onConfirm={handleDeleteTag}
      >
        <Box mt="2">
          <Text size="2" color="red">
            {STRINGS.deleteDialog.warning}
          </Text>
        </Box>
      </ConfirmDialog>
    </Box>
  );
}

export default TagDetailPage;
