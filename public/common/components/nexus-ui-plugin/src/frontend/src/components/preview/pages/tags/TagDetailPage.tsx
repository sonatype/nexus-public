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

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Badge,
  Box,
  Button,
  Card,
  Flex,
  Heading,
  ScrollArea,
  Select,
  Separator,
  Spinner,
  Table,
  Text,
  TextField,
  Tooltip,
} from '@radix-ui/themes';
import {
  ArrowLeft,
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
import { restClient, parseApiError } from '../../../../interface/api';
import { APIConstants } from '../../../../constants/APIConstants';
import { useToast } from '../../shared/Toast';
import { ConfirmDialog } from '../../shared/form';

import { fetchTagDetail } from './tags.api';
import type { TagDetail } from './tags.types';

import './TagDetailPage.scss';

/**
 * Tagged component data from search API.
 */
interface TaggedComponent {
  id: string;
  repository: string;
  format: string;
  group: string | null;
  name: string;
  version: string | null;
  assets: Array<{
    id: string;
    downloadUrl: string;
    path: string;
  }>;
}

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
 */
export function TagDetailPage(): JSX.Element {
  const router = useRouter();
  const { params } = useCurrentStateAndParams();
  const tagName = params.tagName as string;
  const toast = useToast();

  // Tag detail state
  const [tagDetail, setTagDetail] = useState<TagDetail | null>(null);
  const [tagLoading, setTagLoading] = useState(true);
  const [tagError, setTagError] = useState<string | null>(null);
  
  // Components state
  const [components, setComponents] = useState<TaggedComponent[]>([]);
  const [componentsLoading, setComponentsLoading] = useState(true);
  const [componentsError, setComponentsError] = useState<string | null>(null);
  const [continuationToken, setContinuationToken] = useState<string | null>(null);
  
  // Filter state
  const [searchFilter, setSearchFilter] = useState('');
  const [formatFilter, setFormatFilter] = useState('');
  const [repositoryFilter, setRepositoryFilter] = useState('');
  
  // Sort state
  const [sortField, setSortField] = useState<SortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  
  // Delete dialog
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  /**
   * Fetch tag details.
   */
  const loadTagDetail = useCallback(async () => {
    if (!tagName) return;
    
    setTagLoading(true);
    setTagError(null);
    
    try {
      const detail = await fetchTagDetail(tagName);
      setTagDetail(detail);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load tag';
      setTagError(message);
    } finally {
      setTagLoading(false);
    }
  }, [tagName]);

  /**
   * Fetch tagged components using search API.
   */
  const loadComponents = useCallback(async (append = false) => {
    if (!tagName) return;
    
    if (!append) {
      setComponentsLoading(true);
    }
    setComponentsError(null);
    
    try {
      const params = new URLSearchParams();
      params.set('tag', tagName);
      
      if (append && continuationToken) {
        params.set('continuationToken', continuationToken);
      }
      
      const data = await restClient.get<{items?: unknown[]; continuationToken?: string}>(
        `/service/rest/v1/search?${params.toString()}`
      );
      
      const newComponents = data.items || [];
      setComponents(prev => append ? [...prev, ...newComponents] : newComponents);
      setContinuationToken(data.continuationToken || null);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to load components';
      setComponentsError(message);
    } finally {
      setComponentsLoading(false);
    }
  }, [tagName, continuationToken]);

  // Load data on mount
  useEffect(() => {
    loadTagDetail();
    loadComponents();
  }, [tagName]); // eslint-disable-line react-hooks/exhaustive-deps

  /**
   * Get unique formats from components.
   */
  const formats = useMemo(() => {
    const formatSet = new Set(components.map(c => c.format));
    return Array.from(formatSet).sort();
  }, [components]);

  /**
   * Get unique repositories from components.
   */
  const repositories = useMemo(() => {
    const repoSet = new Set(components.map(c => c.repository));
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
      filtered = filtered.filter(c => 
        c.name.toLowerCase().includes(search) ||
        (c.group?.toLowerCase().includes(search)) ||
        c.repository.toLowerCase().includes(search)
      );
    }
    
    // Apply format filter
    if (formatFilter) {
      filtered = filtered.filter(c => c.format === formatFilter);
    }
    
    // Apply repository filter
    if (repositoryFilter) {
      filtered = filtered.filter(c => c.repository === repositoryFilter);
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
  const handleSort = useCallback((field: SortField) => {
    if (sortField === field) {
      setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  }, [sortField]);

  /**
   * Navigate back to tags list.
   */
  const handleBack = useCallback(() => {
    router.stateService.go('preview.browse.tags');
  }, [router]);

  /**
   * Navigate to component detail.
   */
  const handleViewComponent = useCallback((component: TaggedComponent) => {
    if (component.assets?.[0]?.id) {
      router.stateService.go('preview.browse.search.asset', {
        repositoryName: component.repository,
        assetId: btoa(component.assets[0].id),
        componentId: component.id,
      });
    }
  }, [router]);

  /**
   * Export components to CSV.
   */
  const handleExportCsv = useCallback(() => {
    const headers = ['Name', 'Group', 'Version', 'Format', 'Repository'];
    const rows = filteredComponents.map(c => [
      c.name,
      c.group || '',
      c.version || '',
      c.format,
      c.repository,
    ]);
    
    const csv = [headers, ...rows].map(row => row.map(cell => `"${cell}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    
    const a = document.createElement('a');
    a.href = url;
    a.download = `${tagName}-components.csv`;
    a.click();
    
    URL.revokeObjectURL(url);
  }, [tagName, filteredComponents]);

  /**
   * Handle delete tag.
   */
  const handleDeleteTag = useCallback(async () => {
    try {
      await restClient.delete(`${APIConstants.REST.PUBLIC.TAGS}/${encodeURIComponent(tagName)}`);
      toast.success(`Tag "${tagName}" deleted`);
      setDeleteDialogOpen(false);
      handleBack();
    } catch (err) {
      toast.error(parseApiError(err).message);
    }
  }, [tagName, handleBack]);

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
      <Box className="tag-detail-page">
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
      <Box className="tag-detail-page">
        <Box className="tag-detail-page__header">
          <Button variant="ghost" onClick={handleBack}>
            <ArrowLeft size={16} />
            {STRINGS.backToTags}
          </Button>
        </Box>
        <Flex direction="column" align="center" justify="center" p="9" gap="3">
          <Text color="red">{STRINGS.error}</Text>
          <Text color="gray" size="2">{tagError}</Text>
          <Button variant="soft" onClick={loadTagDetail}>
            {STRINGS.retry}
          </Button>
        </Flex>
      </Box>
    );
  }

  return (
    <Box className="tag-detail-page">
      {/* Breadcrumb Header */}
      <Box className="tag-detail-page__header">
        <Flex align="center" gap="2">
          <Button variant="ghost" onClick={handleBack} className="tag-detail-page__back-btn">
            <ArrowLeft size={16} />
            {STRINGS.backToTags}
          </Button>
          <Text color="gray">/</Text>
          <Text weight="medium">{tagName}</Text>
        </Flex>
      </Box>

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
                  <Text size="2" color="gray">{STRINGS.header.created}:</Text>
                  <Text size="2">{formatDate(tagDetail?.firstCreated || null)}</Text>
                </Flex>
                <Flex align="center" gap="2">
                  <Clock size={14} className="tag-detail-page__meta-icon" />
                  <Text size="2" color="gray">{STRINGS.header.lastUpdated}:</Text>
                  <Text size="2">{formatDate(tagDetail?.lastUpdated || null)}</Text>
                </Flex>
                <Flex align="center" gap="2">
                  <Package size={14} className="tag-detail-page__meta-icon" />
                  <Text size="2" color="gray">{STRINGS.header.totalComponents}:</Text>
                  <Badge size="1" variant="soft" color="violet">{components.length}</Badge>
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
                      <Button variant="ghost" size="1" onClick={() => setSearchFilter('')}>
                        <X size={14} />
                      </Button>
                    </TextField.Slot>
                  )}
                </TextField.Root>
              </Box>
              
              <Select.Root value={formatFilter || '__all__'} onValueChange={(v) => setFormatFilter(v === '__all__' ? '' : v)}>
                <Select.Trigger placeholder={STRINGS.components.allFormats} />
                <Select.Content>
                  <Select.Item value="__all__">{STRINGS.components.allFormats}</Select.Item>
                  {formats.map(format => (
                    <Select.Item key={format} value={format}>{format}</Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>
              
              <Select.Root value={repositoryFilter || '__all__'} onValueChange={(v) => setRepositoryFilter(v === '__all__' ? '' : v)}>
                <Select.Trigger placeholder={STRINGS.components.allRepositories} />
                <Select.Content>
                  <Select.Item value="__all__">{STRINGS.components.allRepositories}</Select.Item>
                  {repositories.map(repo => (
                    <Select.Item key={repo} value={repo}>{repo}</Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>
            </Flex>

            {/* Components Table */}
            {componentsLoading ? (
              <Flex align="center" justify="center" p="6" gap="2">
                <Spinner size="2" />
                <Text color="gray">{STRINGS.loadingComponents}</Text>
              </Flex>
            ) : filteredComponents.length === 0 ? (
              <Flex direction="column" align="center" justify="center" p="6" gap="2">
                <Package size={32} color="var(--gray-9)" />
                <Text color="gray">
                  {components.length === 0 
                    ? STRINGS.components.empty 
                    : STRINGS.components.emptyFiltered}
                </Text>
              </Flex>
            ) : (
              <Table.Root>
                <Table.Header>
                  <Table.Row>
                    <Table.ColumnHeaderCell 
                      onClick={() => handleSort('name')}
                      style={{ cursor: 'pointer' }}
                    >
                      <Flex align="center" gap="1">
                        {STRINGS.table.component}
                        <SortIcon field="name" />
                      </Flex>
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell
                      onClick={() => handleSort('format')}
                      style={{ cursor: 'pointer' }}
                    >
                      <Flex align="center" gap="1">
                        {STRINGS.table.format}
                        <SortIcon field="format" />
                      </Flex>
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell
                      onClick={() => handleSort('version')}
                      style={{ cursor: 'pointer' }}
                    >
                      <Flex align="center" gap="1">
                        {STRINGS.table.version}
                        <SortIcon field="version" />
                      </Flex>
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell
                      onClick={() => handleSort('repository')}
                      style={{ cursor: 'pointer' }}
                    >
                      <Flex align="center" gap="1">
                        {STRINGS.table.repository}
                        <SortIcon field="repository" />
                      </Flex>
                    </Table.ColumnHeaderCell>
                    <Table.ColumnHeaderCell>
                      {STRINGS.table.actions}
                    </Table.ColumnHeaderCell>
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {filteredComponents.map((component) => (
                    <Table.Row 
                      key={component.id}
                      className="tag-detail-page__component-row"
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
                        <Tooltip content={STRINGS.actions.view}>
                          <Button 
                            variant="ghost" 
                            size="1"
                            onClick={() => handleViewComponent(component)}
                          >
                            <ExternalLink size={14} />
                          </Button>
                        </Tooltip>
                      </Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table.Root>
            )}

            {/* Load More */}
            {continuationToken && !componentsLoading && (
              <Flex justify="center" mt="4">
                <Button variant="soft" onClick={() => loadComponents(true)}>
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
        onOpenChange={setDeleteDialogOpen}
        title={STRINGS.deleteDialog.title}
        message={STRINGS.deleteDialog.message}
        confirmLabel={STRINGS.deleteDialog.confirm}
        cancelLabel={STRINGS.deleteDialog.cancel}
        variant="danger"
        onConfirm={handleDeleteTag}
      >
        <Box mt="2">
          <Text size="2" color="red">{STRINGS.deleteDialog.warning}</Text>
        </Box>
      </ConfirmDialog>
    </Box>
  );
}

export default TagDetailPage;

