/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React, { useState, useMemo, useCallback, useEffect } from 'react';
import { Box, Flex, Text, TextField, IconButton, Tooltip, Checkbox, Table, Button, Select, DropdownMenu, Spinner, Heading, Badge, Tabs } from '@radix-ui/themes';
import { Shield, Search, Trash2, Plus, X, Upload, Pencil, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, MoreVertical } from 'lucide-react';
import {NxTextLink} from '@sonatype/react-shared-components';
import { PageHeader } from '../../../shared';
import './IpAllowList.scss';
import { BulkImportModal } from './BulkImportModal';
import { BulkImportResultsModal } from './BulkImportResultsModal';
import { IpFilteringModeChangeModal } from './IpFilteringModeChangeModal';
import { BulkDeleteConfirmationModal } from './BulkDeleteConfirmationModal';
import { AddIpModal } from './AddIpModal';
import { EditIpModal } from './EditIpModal';
import { EmptyState } from './EmptyState';
import { TableSkeleton } from './TableSkeleton';
import { ToastNotification, ToastViewport } from './Toast';
import * as Toast from '@radix-ui/react-toast';
import { IpAllowListApi, transformEntryToUI } from './IpAllowListApi';


const navigateTo = (path) => {
  window.location.hash = path;
};

// Format date for display
const formatDate = (isoString) => {
  if (!isoString) return '—';
  const date = new Date(isoString);
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

// Maximum number of IP allowlist entries (PRD Section 3.7)
// Will be updated from API settings
const DEFAULT_MAX_ENTRIES = 256;

// Delay before firing a server-side search request after the user stops typing
const SEARCH_DEBOUNCE_MS = 300;

export default function IpAllowList() {
  const [mode, setMode] = useState('disabled');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRows, setSelectedRows] = useState([]);
  const [data, setData] = useState([]);
  const [showImportModal, setShowImportModal] = useState(false);
  const [showResultsModal, setShowResultsModal] = useState(false);
  const [importResults, setImportResults] = useState(null);
  const [showModeChangeModal, setShowModeChangeModal] = useState(false);
  const [pendingMode, setPendingMode] = useState(null);
  const [currentUserIp, setCurrentUserIp] = useState(null);
  const [isCurrentUserIpAllowed, setIsCurrentUserIpAllowed] = useState(false);
  const [showBulkDeleteModal, setShowBulkDeleteModal] = useState(false);
  const [showAddIpModal, setShowAddIpModal] = useState(false);
  const [showEditIpModal, setShowEditIpModal] = useState(false);
  const [editingIp, setEditingIp] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(20);
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const [toastType, setToastType] = useState('success');

  // API-related state
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingPage, setIsLoadingPage] = useState(false);
  const [maxEntries, setMaxEntries] = useState(DEFAULT_MAX_ENTRIES);
  const [totalEntries, setTotalEntries] = useState(0);
  const [ipv4AddressesCovered, setIpv4AddressesCovered] = useState(0);
  const [ipv6AddressesCovered, setIpv6AddressesCovered] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [apiError, setApiError] = useState(null);
  const [activeTab, setActiveTab] = useState('overview');

  // Search-related state
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState(null); // null means no active search

  const breadcrumbs = [
    {
      label: 'Settings',
      onClick: () => navigateTo('#preview/admin/settings'),
    },
    {
      label: 'IP Allow List',
    },
  ];

  // Use search results when searching, otherwise use paginated data
  const filteredData = useMemo(() => {
    // If search is active and we have results, use them
    if (searchQuery && searchResults !== null) {
      return searchResults;
    }
    // Otherwise return the current page data
    return data;
  }, [data, searchQuery, searchResults]);

  // With server-side pagination, data is already paginated
  // For search, results contain all matches (not paginated)
  const paginatedData = filteredData;
  // Use search result count when searching, otherwise total entries from server
  const totalCount = (searchQuery && searchResults !== null) ? searchResults.length : totalEntries;

  // Handle select all — merges/unmerges current page into the persistent cross-page selection set
  const handleSelectAll = () => {
    const currentPageIds = filteredData.map(item => item.id);
    const allCurrentPageSelected = currentPageIds.every(id => selectedRows.includes(id));
    if (allCurrentPageSelected) {
      // Unselect only current page rows, preserve selections from other pages
      setSelectedRows(prev => prev.filter(id => !currentPageIds.includes(id)));
    }
    else {
      // Add current page rows to existing selection (union)
      setSelectedRows(prev => [...new Set([...prev, ...currentPageIds])]);
    }
  };

  // Handle single row selection
  const handleRowSelect = (id, checked) => {
    if (checked) {
      setSelectedRows(prev => [...prev, id]);
    }
    else {
      setSelectedRows(prev => prev.filter(rowId => rowId !== id));
    }
  };

  // Helper to show toast notifications
  const showToastNotification = (message, type = 'success') => {
    setToastMessage(message);
    setToastType(type);
    setShowToast(true);
  };

  // Check selection state against current page only (not total selectedRows count)
  const currentPageIds = filteredData.map(item => item.id);
  const allSelected = currentPageIds.length > 0 && currentPageIds.every(id => selectedRows.includes(id));
  const someSelected = currentPageIds.length > 0 && currentPageIds.some(id => selectedRows.includes(id)) && !allSelected;

  const handleDelete = async (id) => {
    try {
      // Use bulkDelete API with single ID array for consistency
      const result = await IpAllowListApi.bulkDelete([id]);
      // Remove from selected rows if it was selected
      setSelectedRows(selectedRows.filter(rowId => rowId !== id));
      if (result.deleted > 0) {
        showToastNotification('IP address deleted successfully');
      } else if (result.failed && result.failed.length > 0) {
        showToastNotification(result.failed[0].error || 'Failed to delete IP address', 'error');
      }
      // Reload data from server
      await loadData();
    } catch (error) {
      console.error('Failed to delete entry:', error);
      showToastNotification(error.response?.data?.error || 'Failed to delete IP address', 'error');
    }
  };

  // Clear selection
  const handleClearSelection = () => {
    setSelectedRows([]);
  };

  // Handle Add IP confirmation
  const handleAddIp = async (ipAddresses) => {
    // Check if adding would exceed maximum entries
    if (totalEntries + ipAddresses.length > maxEntries) {
      showToastNotification(
        `Cannot add ${ipAddresses.length} ${ipAddresses.length === 1 ? 'entry' : 'entries'}. ` +
        `Maximum is ${maxEntries} entries, current count is ${totalEntries}.`,
        'error'
      );
      return;
    }

    try {
      // Add entries with descriptions using bulkAdd (supports objects with ipAddress and description)
      const results = await IpAllowListApi.bulkAdd(ipAddresses);

      if (results.failed.length > 0) {
        showToastNotification(
          `Added ${results.added} IP ${results.added === 1 ? 'address' : 'addresses'}. ${results.failed.length} failed.`,
          results.added > 0 ? 'warning' : 'error'
        );
      } else {
        showToastNotification(
          `${results.added} IP ${results.added === 1 ? 'address' : 'addresses'} added successfully`
        );
      }

      // Navigate to first page to show the newly added IPs
      setCurrentPage(1);
      // Reload data from server
      await loadData();
    } catch (error) {
      console.error('Failed to add entries:', error);
      showToastNotification(error.response?.data?.error || 'Failed to add IP addresses', 'error');
    }
  };

  // Open edit modal for an IP
  const handleEdit = (item) => {
    setEditingIp(item);
    setShowEditIpModal(true);
  };

  // Handle Edit IP confirmation
  const handleEditIp = async (updatedIp) => {
    try {
      // Call API to update the entry with description
      await IpAllowListApi.updateEntry(updatedIp.id, updatedIp.ipAddress, updatedIp.description);

      showToastNotification('IP address updated successfully');
      // Reload data from server to get updated data
      await loadData();
    } catch (error) {
      console.error('Failed to update entry:', error);
      const errorMessage = error.response?.data?.error || 'Failed to update IP address';
      showToastNotification(errorMessage, 'error');
    }
  };

  // Show bulk delete confirmation
  const handleBulkDelete = () => {
    if (selectedRows.length === 0) return;
    setShowBulkDeleteModal(true);
  };

  // Confirm bulk delete
  const confirmBulkDelete = async () => {
    try {
      const results = await IpAllowListApi.bulkDelete(selectedRows);

      if (results.failed.length > 0) {
        showToastNotification(
          `Deleted ${results.deleted} IP ${results.deleted === 1 ? 'address' : 'addresses'}. ${results.failed.length} failed.`,
          results.deleted > 0 ? 'warning' : 'error'
        );
      } else {
        showToastNotification(
          `${results.deleted} IP ${results.deleted === 1 ? 'address' : 'addresses'} deleted successfully`
        );
      }

      setSelectedRows([]);
      setShowBulkDeleteModal(false);
      // Reload data from server
      await loadData();
    } catch (error) {
      console.error('Failed to delete entries:', error);
      showToastNotification(error.response?.data?.error || 'Failed to delete IP addresses', 'error');
    }
  };

  // Handle mode change with confirmation logic
  const handleModeChange = async (newMode) => {
    const needsConfirmation =
      // Disabling from active state
      (newMode === 'disabled' && (mode === 'monitor' || mode === 'enforce')) ||
      // Enabling enforce mode
      newMode === 'enforce';

    if (needsConfirmation) {
      // Re-fetch current IP status before showing modal to ensure it's not stale
      // This handles the case where user added/removed their IP after page load
      if (newMode === 'enforce') {
        try {
          const ipData = await IpAllowListApi.getCurrentIp();
          setCurrentUserIp(ipData.ip);
          setIsCurrentUserIpAllowed(ipData.allowed === true);
        } catch (error) {
          console.error('Failed to fetch current IP status:', error);
          // Be cautious - assume not allowed if we can't verify
          setIsCurrentUserIpAllowed(false);
        }
      }
      setPendingMode(newMode);
      setShowModeChangeModal(true);
    } else {
      updateModeApi(newMode);
    }
  };

  const updateModeApi = async (newMode) => {
    try {
      await IpAllowListApi.updateMode(newMode);
      setMode(newMode);
      showToastNotification(`IP filtering mode changed to ${newMode}`);
    } catch (error) {
      console.error('Failed to update mode:', error);
      showToastNotification(error.response?.data?.error || 'Failed to update mode', 'error');
    }
  };

  const handleConfirmModeChange = async () => {
    setShowModeChangeModal(false);
    await updateModeApi(pendingMode);
    setPendingMode(null);
  };

  const handleCancelModeChange = () => {
    setShowModeChangeModal(false);
    setPendingMode(null);
  };

  // Load data from API
  const loadData = useCallback(async (isInitialLoad = false) => {
    try {
      // Use different loading state for initial load vs pagination
      if (isInitialLoad) {
        setIsLoading(true);
      } else {
        setIsLoadingPage(true);
      }
      setApiError(null);

      // Fetch settings and entries in parallel
      const [settings, entriesResponse] = await Promise.all([
        IpAllowListApi.getSettings(),
        IpAllowListApi.getEntries(currentPage - 1, itemsPerPage)
      ]);

      // Update mode from settings
      setMode(settings.mode.toLowerCase());
      setMaxEntries(settings.maxEntries);
      setTotalEntries(settings.totalEntries);
      setIpv4AddressesCovered(settings.ipv4AddressesCovered || 0);
      setIpv6AddressesCovered(settings.ipv6AddressesCovered || 0);

      // Transform and set entries
      const transformedEntries = entriesResponse.entries.map(transformEntryToUI);
      setData(transformedEntries);
      setTotalPages(entriesResponse.totalPages);
    } catch (error) {
      console.error('Failed to load IP Allow List data:', error);
      setApiError(error.response?.data?.error || error.message || 'Failed to load data');
      showToastNotification('Failed to load IP Allow List data', 'error');
    } finally {
      if (isInitialLoad) {
        setIsLoading(false);
      } else {
        setIsLoadingPage(false);
      }
    }
  }, [currentPage, itemsPerPage]);

  // Load data on mount
  useEffect(() => {
    loadData(true); // Initial load
  }, []);

  // Load data when pagination changes (after initial load)
  useEffect(() => {
    if (!isLoading) {
      loadData(false); // Page change load
    }
  }, [currentPage, itemsPerPage]);

  // Track previous search query to detect when search text changes (vs pagination changes)
  const [prevSearchQuery, setPrevSearchQuery] = useState('');

  // Debounced server-side search effect
  useEffect(() => {
    // If search query is empty, clear search results and reload original paginated data
    if (!searchQuery || searchQuery.trim() === '') {
      const wasSearchActive = prevSearchQuery !== '';
      setSearchResults(null);
      setIsSearching(false);
      setPrevSearchQuery('');
      // Reload to restore totalEntries which may have been overwritten by search results
      if (wasSearchActive) {
        loadData(false);
      }
      return;
    }

    // Reset to page 1 when search query text changes (not when pagination changes)
    const searchQueryTrimmed = searchQuery.trim();
    if (searchQueryTrimmed !== prevSearchQuery) {
      setPrevSearchQuery(searchQueryTrimmed);
      if (currentPage !== 1) {
        setCurrentPage(1);
        return; // Let the effect re-run with page 1
      }
    }

    const debounceTimer = setTimeout(async () => {
      try {
        setIsSearching(true);
        // Use consolidated getEntries with search parameter (server-side pagination with search)
        const result = await IpAllowListApi.getEntries(currentPage - 1, itemsPerPage, searchQueryTrimmed);
        // Transform entries to UI format
        const transformedEntries = result.entries.map(transformEntryToUI);
        setSearchResults(transformedEntries);
        // Update pagination based on search results
        setTotalEntries(result.totalEntries);
        setTotalPages(result.totalPages);
      } catch (error) {
        console.error('Search failed:', error);
        showToastNotification('Search failed', 'error');
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }, SEARCH_DEBOUNCE_MS);

    // Cleanup: cancel debounce timer on unmount or when query changes
    return () => clearTimeout(debounceTimer);
  }, [searchQuery, currentPage, itemsPerPage, prevSearchQuery]);

  // Fetch current user's IP on mount using internal Nexus API
  useEffect(() => {
    const fetchCurrentIp = async () => {
      try {
        const ipData = await IpAllowListApi.getCurrentIp();
        setCurrentUserIp(ipData.ip);
        setIsCurrentUserIpAllowed(ipData.allowed === true);
      } catch (error) {
        console.error('Failed to fetch current IP:', error);
        // If we can't get IP, we'll show a more cautious warning
        setCurrentUserIp(null);
        setIsCurrentUserIpAllowed(false);
      }
    };

    fetchCurrentIp();
  }, []);

  // Bulk import handler - uses backend bulk CSV endpoint for efficiency
  const handleBulkImport = async (file) => {
    try {
      // Read file content
      const content = await file.text();

      if (!content.trim()) {
        showToastNotification('Import Failed: File is empty', 'error');
        setShowImportModal(false);
        return;
      }

      // Send CSV content directly to backend bulk endpoint
      const apiResult = await IpAllowListApi.bulkUploadCsv(content);

      // Convert API result to local results format for the results modal
      const localResults = {
        totalRows: apiResult.totalRows,
        addedCount: apiResult.addedCount,
        skippedCount: apiResult.skippedCount,
        rejectedCount: apiResult.rejectedCount,
        added: [],
        skipped: [],
        rejected: (apiResult.rejectedEntries || []).map(entry => ({
          row: entry.line,
          ip: entry.value || 'Unknown',
          reason: entry.reason,
        }))
      };

      // Reload data from server
      if (apiResult.addedCount > 0) {
        await loadData();
      }

      // Show results modal
      setImportResults(localResults);
      setShowImportModal(false);
      setShowResultsModal(true);
    } catch (error) {
      const errorMessage = error.response?.data?.error || error.message;
      showToastNotification(`Import Failed: ${errorMessage}`, 'error');
      setShowImportModal(false);
    }
  };

  return (
    <Toast.Provider swipeDirection="right">
      <Box className="nxrm-ip-allowlist">
        <PageHeader
          icon={Shield}
          title="IP Allow List"
          description="Control access to your repository by specifying a list of allowed IP addresses"
          breadcrumbs={breadcrumbs}
          actions={
            <Badge
              color={mode === 'disabled' ? 'gray' : mode === 'monitor' ? 'blue' : 'red'}
              variant="soft"
              size="2"
              data-testid="mode-badge"
            >
              {mode === 'disabled' ? 'Disabled' : mode === 'monitor' ? 'Monitor' : 'Enforce'}
            </Badge>
          }
        />
        <Box className="nxrm-ip-allowlist__content">
        {isLoading ? (
          <>
            {/* Show skeleton mode selector */}
            <Box mb="5" p="4" style={{ border: '1px solid var(--gray-6)', borderRadius: 'var(--radius-3)', backgroundColor: 'var(--color-panel-solid, white)' }}>
              <Flex gap="3">
                <Box style={{ flex: '1 1 0', height: '80px', backgroundColor: 'var(--gray-4)', borderRadius: 'var(--radius-2)' }} />
                <Box style={{ flex: '1 1 0', height: '80px', backgroundColor: 'var(--gray-4)', borderRadius: 'var(--radius-2)' }} />
                <Box style={{ flex: '1 1 0', height: '80px', backgroundColor: 'var(--gray-4)', borderRadius: 'var(--radius-2)' }} />
              </Flex>
            </Box>
            <TableSkeleton rows={5} />
          </>
        ) : apiError ? (
          <Flex direction="column" align="center" py="9">
            <Text color="red" size="3">{apiError}</Text>
            <Button mt="3" onClick={loadData}>Retry</Button>
          </Flex>
        ) : (
          <Tabs.Root value={activeTab} onValueChange={setActiveTab}>
            <Tabs.List>
              <Tabs.Trigger value="overview">Overview</Tabs.Trigger>
              <Tabs.Trigger value="ip-allow-list">IP Allow List</Tabs.Trigger>
            </Tabs.List>

            {/* ── Overview tab: mode card only ── */}
            <Tabs.Content value="overview">
              <Box mt="4">
                {/* Mode Selection Card */}
                <Box mb="5">
                  <Box
                    p="4"
                    style={{
                      border: '1px solid var(--gray-6)',
                      borderRadius: 'var(--radius-3)',
                      backgroundColor: 'var(--color-panel-solid, white)'
                    }}
                  >
                    <Flex direction="column" gap="4">
                      {/* Header with title and inline learn more link */}
                      <Box>
                        <Heading as="h3" size="4" weight="medium" mb="2">
                          Control which network locations can access Nexus Repository.
                        </Heading>
                        <Text size="2" color="gray" style={{ display: 'block' }}>
                          When enabled, only requests originating from allowed IP addresses or CIDR ranges can interact with the application. Requests from non-allowed IPs can be monitored or blocked depending on the selected mode.{' '}
                          <NxTextLink
                            external
                            href="https://links.sonatype.com/products/nxrm3/docs/ip-allow-list"
                          >
                            Learn more
                          </NxTextLink>
                        </Text>
                      </Box>

                      {/* Horizontal Radio Cards */}
                      <Flex gap="3">
                        {/* Disabled Option */}
                        <Box
                          as="label"
                          p="3"
                          style={{
                            flex: '1 1 0',
                            cursor: 'pointer',
                            borderRadius: 'var(--radius-2)',
                            border: mode === 'disabled' ? '2px solid var(--blue-8)' : '1px solid var(--gray-6)',
                            backgroundColor: mode === 'disabled' ? 'var(--blue-2)' : 'var(--color-panel-solid, white)',
                            transition: 'all 0.15s ease'
                          }}
                          onClick={() => {
                            if (mode !== 'disabled') {
                              setPendingMode('disabled');
                              setShowModeChangeModal(true);
                            }
                          }}
                        >
                          <Flex align="start" gap="2">
                            <input
                              type="radio"
                              name="ipFilteringMode"
                              value="disabled"
                              checked={mode === 'disabled'}
                              onChange={() => {}}
                              style={{ marginTop: '2px', cursor: 'pointer' }}
                            />
                            <Box style={{ flex: 1 }}>
                              <Text size="2" weight="medium" style={{ display: 'block', marginBottom: '4px' }}>
                                Disabled
                              </Text>
                              <Text size="2" color="gray">
                                No IP filtering is applied.
                              </Text>
                            </Box>
                          </Flex>
                        </Box>

                        {/* Monitor Option */}
                        <Box
                          as="label"
                          p="3"
                          style={{
                            flex: '1 1 0',
                            cursor: 'pointer',
                            borderRadius: 'var(--radius-2)',
                            border: mode === 'monitor' ? '2px solid var(--blue-8)' : '1px solid var(--gray-6)',
                            backgroundColor: mode === 'monitor' ? 'var(--blue-2)' : 'var(--color-panel-solid, white)',
                            transition: 'all 0.15s ease'
                          }}
                          onClick={() => {
                            if (mode !== 'monitor') {
                              handleModeChange('monitor');
                            }
                          }}
                        >
                          <Flex align="start" gap="2">
                            <input
                              type="radio"
                              name="ipFilteringMode"
                              value="monitor"
                              checked={mode === 'monitor'}
                              onChange={() => {}}
                              style={{ marginTop: '2px', cursor: 'pointer' }}
                            />
                            <Box style={{ flex: 1 }}>
                              <Text size="2" weight="medium" style={{ display: 'block', marginBottom: '4px' }}>
                                Monitor
                              </Text>
                              <Text size="2" color="gray">
                                Log requests without blocking.
                              </Text>
                            </Box>
                          </Flex>
                        </Box>

                        {/* Enforce Option */}
                        <Box
                          as="label"
                          p="3"
                          style={{
                            flex: '1 1 0',
                            cursor: 'pointer',
                            borderRadius: 'var(--radius-2)',
                            border: mode === 'enforce' ? '2px solid var(--blue-8)' : '1px solid var(--gray-6)',
                            backgroundColor: mode === 'enforce' ? 'var(--blue-2)' : 'var(--color-panel-solid, white)',
                            transition: 'all 0.15s ease'
                          }}
                          onClick={() => {
                            if (mode !== 'enforce') {
                              handleModeChange('enforce');
                            }
                          }}
                        >
                          <Flex align="start" gap="2">
                            <input
                              type="radio"
                              name="ipFilteringMode"
                              value="enforce"
                              checked={mode === 'enforce'}
                              onChange={() => {}}
                              style={{ marginTop: '2px', cursor: 'pointer' }}
                            />
                            <Box style={{ flex: 1 }}>
                              <Text size="2" weight="medium" style={{ display: 'block', marginBottom: '4px' }}>
                                Enforce
                              </Text>
                              <Text size="2" color="gray">
                                Block requests from unlisted IPs.
                              </Text>
                            </Box>
                          </Flex>
                        </Box>
                      </Flex>
                    </Flex>
                  </Box>
                </Box>
              </Box>
            </Tabs.Content>

            {/* ── IP Allow List tab: stat cards + search + table ── */}
            <Tabs.Content value="ip-allow-list">
              <Box mt="4">
                {/* Stat Cards Banner */}
                <Flex className="nxrm-ip-allowlist__stat-cards">
                  <Box className="nxrm-ip-allowlist__stat-card" data-testid="stat-card-total">
                    <Text size="2" color="gray" style={{ display: 'block', marginBottom: '8px' }}>
                      Total IPs Covered
                    </Text>
                    <Text size="6" weight="bold" style={{ display: 'block' }}>
                      {(ipv4AddressesCovered + ipv6AddressesCovered).toLocaleString()}
                    </Text>
                  </Box>
                  <Box className="nxrm-ip-allowlist__stat-card" data-testid="stat-card-ipv4">
                    <Text size="2" color="gray" style={{ display: 'block', marginBottom: '8px' }}>
                      IPv4 IPs Covered
                    </Text>
                    <Text size="6" weight="bold" style={{ display: 'block' }}>
                      {ipv4AddressesCovered.toLocaleString()}
                    </Text>
                  </Box>
                  <Box className="nxrm-ip-allowlist__stat-card" data-testid="stat-card-ipv6">
                    <Text size="2" color="gray" style={{ display: 'block', marginBottom: '8px' }}>
                      IPv6 IPs Covered
                    </Text>
                    <Text size="6" weight="bold" style={{ display: 'block' }}>
                      {ipv6AddressesCovered.toLocaleString()}
                    </Text>
                  </Box>
                </Flex>

                {/* Table Section or Empty State */}
                {totalEntries === 0 && !searchQuery ? (
                  // No entries at all - show initial empty state
                  <EmptyState
                    heading="There are no allow list entries"
                    description="Add IP addresses or CIDR blocks to control which network locations can access Nexus Repository."
                    actions={[
                      {
                        label: 'Add Entry',
                        variant: 'solid',
                        onClick: () => setShowAddIpModal(true),
                        testId: 'empty-state-add-entry'
                      },
                      {
                        label: 'Import Entries',
                        variant: 'outline',
                        onClick: () => setShowImportModal(true),
                        testId: 'empty-state-import'
                      }
                    ]}
                  />
                ) : (
                <Box className="nxrm-ip-allowlist__table-container">

                  {/* Search Bar and Actions */}
                  <Flex justify="between" align="center" mb="3" gap="3">
                    <TextField.Root
                      placeholder="Filter by IP address or CIDR block"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      style={{ flex: 1 }}
                      data-testid="search-input"
                    >
                      <TextField.Slot>
                        {isSearching ? <Spinner size="1" /> : <Search size={16} />}
                      </TextField.Slot>
                    </TextField.Root>
                    <Flex gap="3" align="center">
                      <Button
                        variant="outline"
                        onClick={() => setShowImportModal(true)}
                        data-testid="bulk-import-button"
                      >
                        <Upload size={16} />
                        Import Entries
                      </Button>
                      <Button
                        onClick={() => setShowAddIpModal(true)}
                        data-testid="add-ip-button"
                      >
                        <Plus size={16} />
                        Add Entry
                      </Button>
                    </Flex>
                  </Flex>

                  {/* No Search Results State */}
                  {paginatedData.length === 0 ? (
                    <EmptyState
                      heading="No Results Found"
                      description={`No IP addresses match your search "${searchQuery}". Try a different search term or clear the filter.`}
                      actions={[
                        {
                          label: 'Reset Filters',
                          variant: 'solid',
                          onClick: () => {
                            setSearchQuery('');
                            setSearchResults(null);
                            setCurrentPage(1);
                            loadData(false);
                          },
                          testId: 'no-results-reset-button'
                        }
                      ]}
                    />
                  ) : (
                    <>
                      {/* Table */}
                      <Box style={{ overflowX: 'auto', position: 'relative' }}>
                        {isLoadingPage && (
                          <Box style={{
                            position: 'absolute',
                            top: 0,
                            left: 0,
                            right: 0,
                            bottom: 0,
                            backgroundColor: 'rgba(255, 255, 255, 0.7)',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            zIndex: 10
                          }}>
                            <Spinner size="3" />
                          </Box>
                        )}
                        <Table.Root variant="surface" size="2" className="nxrm-ip-allowlist__table">
                          <Table.Header>
                            <Table.Row>
                              <Table.ColumnHeaderCell width="50px">
                                <Checkbox
                                  checked={allSelected}
                                  onCheckedChange={handleSelectAll}
                                  {...(someSelected && { 'data-indeterminate': true })}
                                />
                              </Table.ColumnHeaderCell>
                              <Table.ColumnHeaderCell>IP Address</Table.ColumnHeaderCell>
                              <Table.ColumnHeaderCell>Description</Table.ColumnHeaderCell>
                              <Table.ColumnHeaderCell>Last Updated</Table.ColumnHeaderCell>
                              <Table.ColumnHeaderCell width="80px" justify="end" aria-label="Row actions" pr="2"></Table.ColumnHeaderCell>
                            </Table.Row>
                          </Table.Header>
                          <Table.Body>
                            {paginatedData.map((item) => (
                              <Table.Row key={item.id}>
                                <Table.Cell>
                                  <Checkbox
                                    checked={selectedRows.includes(item.id)}
                                    onCheckedChange={(checked) => handleRowSelect(item.id, checked)}
                                  />
                                </Table.Cell>
                                <Table.Cell>
                                  <Text weight="medium">{item.ipAddress}</Text>
                                </Table.Cell>
                                <Table.Cell>
                                  <Text>{item.description}</Text>
                                </Table.Cell>
                                <Table.Cell>
                                  <Text size="1" color="gray">{formatDate(item.lastUpdated)}</Text>
                                </Table.Cell>
                                <Table.Cell justify="end" pr="2">
                                  <DropdownMenu.Root>
                                    <DropdownMenu.Trigger>
                                      <IconButton
                                        variant="ghost"
                                        size="1"
                                        aria-label="Row actions"
                                      >
                                        <MoreVertical size={16} />
                                      </IconButton>
                                    </DropdownMenu.Trigger>
                                    <DropdownMenu.Content>
                                      <DropdownMenu.Item onClick={() => handleEdit(item)}>
                                        <Flex gap="2" align="center">
                                          <Pencil size={14} />
                                          Edit
                                        </Flex>
                                      </DropdownMenu.Item>
                                      <DropdownMenu.Item color="red" onClick={() => handleDelete(item.id)}>
                                        <Flex gap="2" align="center">
                                          <Trash2 size={14} />
                                          Delete
                                        </Flex>
                                      </DropdownMenu.Item>
                                    </DropdownMenu.Content>
                                  </DropdownMenu.Root>
                                </Table.Cell>
                              </Table.Row>
                            ))}
                          </Table.Body>
                        </Table.Root>
                      </Box>

                      {/* Bulk Actions Bar - Floating Overlay */}
                      {selectedRows.length > 0 && (
                        <Box
                          role="region"
                          aria-live="polite"
                          aria-relevant="additions text"
                          aria-label="Bulk actions"
                          className="nxrm-ip-allowlist__bulk-actions-bar"
                          style={{
                            position: 'fixed',
                            bottom: '24px',
                            left: '50%',
                            transform: 'translateX(-50%)',
                            borderRadius: '12px',
                            padding: '10px 12px',
                            zIndex: 1000,
                            minWidth: '400px',
                            maxWidth: '90vw',
                            animation: 'slideUp 0.2s ease-out',
                          }}
                        >
                          <Flex justify="between" align="center">
                            <Text size="2" weight="medium" style={{ color: 'var(--gray-12)' }}>
                              {selectedRows.length} {selectedRows.length === 1 ? 'IP address' : 'IP addresses'} selected:
                            </Text>
                            <Flex gap="2" align="center">
                              <Button
                                variant="outline"
                                size="2"
                                onClick={handleBulkDelete}
                              >
                                Delete
                              </Button>
                              <IconButton
                                variant="ghost"
                                size="2"
                                onClick={handleClearSelection}
                                aria-label="Dismiss bulk actions"
                                style={{ width: '32px', height: '32px' }}
                              >
                                <X size={18} />
                              </IconButton>
                            </Flex>
                          </Flex>
                        </Box>
                      )}

                      {/* Pagination Footer */}
                      {totalCount > 0 && (
                        <Flex justify="between" align="center" mt="4">
                          {/* Left: Showing X of Y */}
                          <Flex align="center" gap="2">
                            <Text size="2" color="gray">Showing</Text>
                            <Select.Root
                              value={itemsPerPage.toString()}
                              onValueChange={(value) => {
                                setItemsPerPage(Number(value));
                                setCurrentPage(1);
                              }}
                              size="2"
                            >
                              <Select.Trigger />
                              <Select.Content>
                                <Select.Item value="10">10</Select.Item>
                                <Select.Item value="20">20</Select.Item>
                                <Select.Item value="50">50</Select.Item>
                                <Select.Item value="100">100</Select.Item>
                              </Select.Content>
                            </Select.Root>
                            <Text size="2" color="gray">of {totalCount}</Text>
                          </Flex>

                          {/* Center: Page selector */}
                          {totalPages > 1 && (
                            <Flex align="center" gap="2">
                              <Text size="2" color="gray">Page</Text>
                              <Select.Root
                                value={currentPage.toString()}
                                onValueChange={(value) => setCurrentPage(Number(value))}
                                size="2"
                              >
                                <Select.Trigger />
                                <Select.Content>
                                  {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                                    <Select.Item key={page} value={page.toString()}>
                                      {page}
                                    </Select.Item>
                                  ))}
                                </Select.Content>
                              </Select.Root>
                              <Text size="2" color="gray">of {totalPages}</Text>
                            </Flex>
                          )}

                          {/* Right: Navigation buttons */}
                          {totalPages > 1 && (
                            <Flex as="nav" aria-label={`Pagination, page ${currentPage} of ${totalPages}`} gap="1">
                              <Tooltip content="First page">
                                <IconButton variant="soft" size="2" disabled={currentPage === 1} onClick={() => setCurrentPage(1)} aria-label="First page">
                                  <ChevronsLeft size={16} />
                                </IconButton>
                              </Tooltip>
                              <Tooltip content="Previous page">
                                <IconButton variant="soft" size="2" disabled={currentPage === 1} onClick={() => setCurrentPage(currentPage - 1)} aria-label="Previous page">
                                  <ChevronLeft size={16} />
                                </IconButton>
                              </Tooltip>

                              {/* Page number buttons (show up to 5 pages) */}
                              {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                                let pageNum;
                                if (totalPages <= 5) {
                                  pageNum = i + 1;
                                } else if (currentPage <= 3) {
                                  pageNum = i + 1;
                                } else if (currentPage >= totalPages - 2) {
                                  pageNum = totalPages - 4 + i;
                                } else {
                                  pageNum = currentPage - 2 + i;
                                }
                                return (
                                  <Button
                                    key={pageNum}
                                    variant={currentPage === pageNum ? 'solid' : 'soft'}
                                    size="2"
                                    onClick={() => setCurrentPage(pageNum)}
                                  >
                                    {pageNum}
                                  </Button>
                                );
                              })}

                              <Tooltip content="Next page">
                                <IconButton variant="soft" size="2" disabled={currentPage === totalPages} onClick={() => setCurrentPage(currentPage + 1)} aria-label="Next page">
                                  <ChevronRight size={16} />
                                </IconButton>
                              </Tooltip>
                              <Tooltip content="Last page">
                                <IconButton variant="soft" size="2" disabled={currentPage === totalPages} onClick={() => setCurrentPage(totalPages)} aria-label="Last page">
                                  <ChevronsRight size={16} />
                                </IconButton>
                              </Tooltip>
                            </Flex>
                          )}
                        </Flex>
                      )}
                    </>
                  )}
                </Box>
                )}
              </Box>
            </Tabs.Content>
          </Tabs.Root>
        )}
      </Box>

      {/* Bulk Import Modal */}
      <BulkImportModal
        isOpen={showImportModal}
        onClose={() => setShowImportModal(false)}
        onImport={handleBulkImport}
      />

      {/* Import Results Modal */}
      {importResults && (
        <BulkImportResultsModal
          isOpen={showResultsModal}
          onClose={() => setShowResultsModal(false)}
          results={importResults}
        />
      )}

      {/* Mode Change Confirmation Modal */}
      <IpFilteringModeChangeModal
        isOpen={showModeChangeModal}
        onClose={handleCancelModeChange}
        onConfirm={handleConfirmModeChange}
        fromMode={mode}
        toMode={pendingMode}
        currentUserIp={currentUserIp}
        isCurrentUserIpAllowed={isCurrentUserIpAllowed}
      />

      {/* Bulk Delete Confirmation Modal */}
      <BulkDeleteConfirmationModal
        isOpen={showBulkDeleteModal}
        onClose={() => setShowBulkDeleteModal(false)}
        onConfirm={confirmBulkDelete}
        count={selectedRows.length}
      />

      {/* Add IP Modal */}
      <AddIpModal
        isOpen={showAddIpModal}
        onClose={() => setShowAddIpModal(false)}
        onConfirm={handleAddIp}
      />

      {/* Edit IP Modal */}
      <EditIpModal
        isOpen={showEditIpModal}
        onClose={() => setShowEditIpModal(false)}
        onConfirm={handleEditIp}
        ipEntry={editingIp}
      />

      {/* Toast Notification */}
      <ToastNotification
        open={showToast}
        onOpenChange={setShowToast}
        message={toastMessage}
        type={toastType}
      />

      {/* Toast Viewport */}
      <ToastViewport />
    </Box>
    </Toast.Provider>
  );
}
