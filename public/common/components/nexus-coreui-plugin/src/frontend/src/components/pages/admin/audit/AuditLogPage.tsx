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

import React, { useState, useCallback, useDeferredValue, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  Flex,
  Grid,
  Heading,
  IconButton,
  Spinner,
  Table,
  Text,
  Code,
  Badge,
  Inset,
  TextField,
} from '@radix-ui/themes';
import { RefreshCw, Download, ChevronDown, ChevronUp, Search, X, Filter } from 'lucide-react';
import { useCurrentStateAndParams } from '@uirouter/react';

import type { AuditFilters, AuditCategory } from './audit.types';
import { useAuditLogApi } from './useAuditLogApi';
import { formatAuditEvent, formatTimestamp } from './auditEventFormatter';
import { CATEGORY_COLORS, CATEGORY_LABELS } from './audit.constants';
import { AuditFilterSidebar } from './AuditFilterSidebar';
import { MobileFilterDrawer } from '../../../super/search/unified/MobileFilterDrawer';
import { PageHeader } from '../../../shared/PageHeader/PageHeader';
import { useRepositoriesApi } from '../../../super/settings/repository/repositories/useRepositoriesApi';
import '../../../super/search/unified/SearchSidebar.scss';

const DEFAULT_FILTERS: AuditFilters = {
  categories: [],
  domains: [],
  eventTypes: [],
  dateRange: 'last-30-days',
  initiator: '',
  initiators: [],
  searchQuery: '',
  repositoryName: '',
  repositoryType: '',
};

/**
 * Audit Log Page - Displays comprehensive audit events across all system activities.
 *
 * Shows security, repository, configuration, and protection events with filtering,
 * pagination, and expandable details.
 */
export function AuditLogPage() {
  const { params } = useCurrentStateAndParams();
  const [filters, setFilters] = useState<AuditFilters>({
    ...DEFAULT_FILTERS,
    repositoryName: (params?.repositoryName as string) || '',
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set());
  const [showMobileFilters, setShowMobileFilters] = useState(false);
  const [searchInput, setSearchInput] = useState('');
  const debouncedSearch = useDeferredValue(searchInput);

  const { data, loading, error, refetch } = useAuditLogApi({
    filters,
    page: currentPage,
    limit: 20,
  });

  const { fetchRepositories } = useRepositoriesApi();
  const [repositories, setRepositories] = useState<any[]>([]);

  useEffect(() => {
    fetchRepositories().then(setRepositories).catch(console.error);
  }, [fetchRepositories]);

  // Apply debounced search to filters
  useEffect(() => {
    setFilters((f) => ({ ...f, searchQuery: debouncedSearch }));
    setCurrentPage(1);
  }, [debouncedSearch]);

  const toggleRow = (id: number) => {
    const newExpanded = new Set(expandedRows);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedRows(newExpanded);
  };

  const handleExportCSV = () => {
    // TODO: Implement CSV export
  };

  const handleCategoryToggle = useCallback(
    (category: AuditCategory) => {
      const newCategories = filters.categories.includes(category)
        ? filters.categories.filter((c) => c !== category)
        : [...filters.categories, category];
      setFilters({ ...filters, categories: newCategories });
      setCurrentPage(1);
    },
    [filters]
  );

  const handleEventTypeToggle = useCallback(
    (eventType: string) => {
      const newEventTypes = filters.eventTypes.includes(eventType)
        ? filters.eventTypes.filter((t) => t !== eventType)
        : [...filters.eventTypes, eventType];
      setFilters({ ...filters, eventTypes: newEventTypes });
      setCurrentPage(1);
    },
    [filters]
  );

  const handleDomainToggle = useCallback(
    (domain: string) => {
      const newDomains = filters.domains.includes(domain)
        ? filters.domains.filter((d) => d !== domain)
        : [...filters.domains, domain];
      setFilters({ ...filters, domains: newDomains });
      setCurrentPage(1);
    },
    [filters]
  );

  const handleInitiatorChange = useCallback(
    (initiator: string) => {
      setFilters({ ...filters, initiator });
      setCurrentPage(1);
    },
    [filters]
  );

  const handleRepositoryNameChange = useCallback(
    (repositoryName: string) => {
      setFilters({ ...filters, repositoryName });
      setCurrentPage(1);
    },
    [filters]
  );

  const handleRepositoryTypeChange = useCallback(
    (repositoryType: string) => {
      setFilters({ ...filters, repositoryType });
      setCurrentPage(1);
    },
    [filters]
  );

  const handleDateRangeChange = useCallback(
    (dateRange: AuditFilters['dateRange']) => {
      setFilters({ ...filters, dateRange });
      setCurrentPage(1);
    },
    [filters]
  );

  const handleSearchChange = useCallback((searchQuery: string) => {
    setSearchInput(searchQuery);
  }, []);

  const handleClearAllFilters = useCallback(() => {
    setFilters(DEFAULT_FILTERS);
    setSearchInput('');
    setCurrentPage(1);
  }, []);

  const hasActiveFilters =
    filters.categories.length > 0 ||
    filters.domains.length > 0 ||
    filters.eventTypes.length > 0 ||
    filters.dateRange !== 'last-30-days' ||
    filters.initiator !== '' ||
    filters.initiators.length > 0;

  const filterBarContent = (
    <AuditFilterSidebar
      filters={filters}
      repositories={repositories}
      onCategoryToggle={handleCategoryToggle}
      onDomainToggle={handleDomainToggle}
      onEventTypeToggle={handleEventTypeToggle}
      onInitiatorChange={handleInitiatorChange}
      onRepositoryNameChange={handleRepositoryNameChange}
      onRepositoryTypeChange={handleRepositoryTypeChange}
      onDateRangeChange={handleDateRangeChange}
      onClearAllFilters={handleClearAllFilters}
      hasActiveFilters={hasActiveFilters}
      disabled={loading}
    />
  );

  return (
    <Box
      data-testid="audit-log-page"
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
      width="100%"
      style={{ minWidth: 0, boxSizing: 'border-box' }}
    >
      <Flex direction="column" gap="6" width="100%" style={{ minWidth: 0 }}>
        <Grid columns={{ initial: '1', sm: '250px 1fr' }} gap="6" width="100%" style={{ minWidth: 0 }}>
          {/* Filter Sidebar - hidden on mobile */}
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
            <PageHeader
              title="Audit Log"
              description={
                data
                  ? `${data.pagination.totalItems.toLocaleString()} event${
                      data.pagination.totalItems !== 1 ? 's' : ''
                    }`
                  : undefined
              }
              actions={
                <Flex gap="2">
                  <Button variant="outline" onClick={refetch} disabled={loading}>
                    <RefreshCw size={14} />
                    Refresh
                  </Button>
                  <Button variant="outline" onClick={handleExportCSV}>
                    <Download size={14} />
                    Export CSV
                  </Button>
                </Flex>
              }
            />

            {/* Search and Mobile Filter Bar */}
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
                    placeholder="Search by context (e.g., user, repository name)..."
                    value={searchInput}
                    onChange={(e) => handleSearchChange(e.target.value)}
                    size="2"
                    style={{ width: '100%' }}
                  >
                    <TextField.Slot>
                      <Search size={14} />
                    </TextField.Slot>
                    {searchInput && (
                      <TextField.Slot side="right">
                        <IconButton
                          variant="ghost"
                          color="gray"
                          size="1"
                          onClick={() => handleSearchChange('')}
                          aria-label="Clear search"
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
              </Flex>
            </Box>

            {error && (
              <Card mb="4" style={{ borderLeft: '3px solid var(--red-9)' }}>
                <Text color="red">{error}</Text>
              </Card>
            )}

            {loading ? (
              <Flex justify="center" align="center" style={{ minHeight: '400px' }}>
                <Flex direction="column" align="center" gap="3">
                  <Spinner size="3" />
                  <Text color="gray">Loading audit events...</Text>
                </Flex>
              </Flex>
            ) : data && data.items.length > 0 ? (
              <>
                <Text size="2" color="gray" mb="3">
                  Showing {(currentPage - 1) * 20 + 1}-{Math.min(currentPage * 20, data.pagination.totalItems)} of{' '}
                  {data.pagination.totalItems} events
                </Text>

                <Card size="1">
                  <Inset clip="padding-box" side="bottom">
                    <Box style={{ overflowX: 'auto' }}>
                      <Table.Root size="2">
                        <Table.Header>
                          <Table.Row>
                            <Table.ColumnHeaderCell style={{ width: '40px' }}></Table.ColumnHeaderCell>
                            <Table.ColumnHeaderCell>Timestamp</Table.ColumnHeaderCell>
                            <Table.ColumnHeaderCell>Category</Table.ColumnHeaderCell>
                            <Table.ColumnHeaderCell>Event</Table.ColumnHeaderCell>
                            <Table.ColumnHeaderCell>Summary</Table.ColumnHeaderCell>
                            <Table.ColumnHeaderCell>Initiator</Table.ColumnHeaderCell>
                          </Table.Row>
                        </Table.Header>

                        <Table.Body>
                          {data.items.map((event) => {
                            const displayEvent = formatAuditEvent(event);
                            const isExpanded = expandedRows.has(event.id);

                            return (
                              <React.Fragment key={event.id}>
                                <Table.Row
                                  style={{ cursor: 'pointer' }}
                                  onClick={() => toggleRow(event.id)}
                                >
                                  <Table.Cell>
                                    {isExpanded ? (
                                      <ChevronUp size={16} />
                                    ) : (
                                      <ChevronDown size={16} />
                                    )}
                                  </Table.Cell>
                                  <Table.Cell>
                                    <Text size="2">{formatTimestamp(event.timestamp)}</Text>
                                  </Table.Cell>
                                  <Table.Cell>
                                    <Badge
                                      color={CATEGORY_COLORS[displayEvent.category] as any}
                                      variant="soft"
                                      size="1"
                                    >
                                      {CATEGORY_LABELS[displayEvent.category]}
                                    </Badge>
                                  </Table.Cell>
                                  <Table.Cell>
                                    <Text size="2" weight="medium">
                                      {displayEvent.eventLabel}
                                    </Text>
                                  </Table.Cell>
                                  <Table.Cell>
                                    <Text size="2">{displayEvent.summary}</Text>
                                  </Table.Cell>
                                  <Table.Cell>
                                    <Text size="2" color="gray">
                                      {event.initiator || 'system'}
                                    </Text>
                                  </Table.Cell>
                                </Table.Row>

                                {isExpanded && (
                                  <Table.Row>
                                    <Table.Cell colSpan={6}>
                                      <Box p="4" style={{ backgroundColor: 'var(--gray-2)' }}>
                                        <Heading size="3" mb="2">
                                          Event Details
                                        </Heading>
                                        <Flex direction="column" gap="2" mb="3">
                                          <Text size="2">
                                            <strong>Domain:</strong> {event.domain}
                                          </Text>
                                          <Text size="2">
                                            <strong>Type:</strong> {event.type}
                                          </Text>
                                          <Text size="2">
                                            <strong>Context:</strong> {event.context || '-'}
                                          </Text>
                                          <Text size="2">
                                            <strong>Node ID:</strong> {event.nodeId}
                                          </Text>
                                        </Flex>

                                        <Text size="2" weight="medium" mb="1">
                                          Attributes:
                                        </Text>
                                        <Code
                                          size="2"
                                          style={{
                                            display: 'block',
                                            whiteSpace: 'pre',
                                            overflowX: 'auto',
                                            padding: '12px',
                                          }}
                                        >
                                          {JSON.stringify(event.attributes, null, 2)}
                                        </Code>
                                      </Box>
                                    </Table.Cell>
                                  </Table.Row>
                                )}
                              </React.Fragment>
                            );
                          })}
                        </Table.Body>
                      </Table.Root>
                    </Box>
                  </Inset>
                </Card>

                {data.pagination.totalPages > 1 && (
                  <Flex justify="between" align="center" mt="4">
                    <Button
                      variant="outline"
                      disabled={currentPage === 1}
                      onClick={() => setCurrentPage((p) => p - 1)}
                    >
                      Previous
                    </Button>
                    <Text size="2" color="gray">
                      Page {currentPage} of {data.pagination.totalPages}
                    </Text>
                    <Button
                      variant="outline"
                      disabled={currentPage === data.pagination.totalPages}
                      onClick={() => setCurrentPage((p) => p + 1)}
                    >
                      Next
                    </Button>
                  </Flex>
                )}
              </>
            ) : (
              <Card>
                <Flex direction="column" align="center" justify="center" gap="2" py="8">
                  <Text size="3" weight="medium">
                    No Audit Events Found
                  </Text>
                  <Text size="2" color="gray">
                    {hasActiveFilters || filters.searchQuery
                      ? 'No audit events match the current filters. Try adjusting your search criteria.'
                      : 'No audit events match the current filters.'}
                  </Text>
                  {(hasActiveFilters || filters.searchQuery) && (
                    <Button variant="outline" size="2" mt="2" onClick={handleClearAllFilters}>
                      Clear all filters
                    </Button>
                  )}
                </Flex>
              </Card>
            )}
          </Box>
        </Grid>
      </Flex>

      <MobileFilterDrawer
        isOpen={showMobileFilters}
        onClose={() => setShowMobileFilters(false)}
        title="Filter"
        onClearAll={handleClearAllFilters}
      >
        {filterBarContent}
      </MobileFilterDrawer>
    </Box>
  );
}

export default AuditLogPage;
