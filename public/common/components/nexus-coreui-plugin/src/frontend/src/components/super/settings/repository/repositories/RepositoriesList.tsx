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

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Badge, Box, Flex, Link, Text, TextField, Tooltip, Button } from '@radix-ui/themes';
import { Database, Search, Copy, CheckCircle, Plus, Pencil, Trash2, Eye, ShieldCheck, Ban, ExternalLink, Shield } from 'lucide-react';
import { ConfirmDialog } from '../../../shared/form';
import { DeleteConfirmationModal } from '../../../../shared';
import { Badge, Box, Flex, Grid, Link, Text, TextField, Tooltip, Button, IconButton, Select } from '@radix-ui/themes';
import { Database, Search, Copy, CheckCircle, Plus, Pencil, Trash2, Eye, ShieldCheck, Ban, ExternalLink, Shield, ArrowUpDown, Filter, X } from 'lucide-react';
import { DeleteConfirmationModal, PageHeader, TablePagination } from '../../../../shared';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { restClient, ENDPOINTS } from '@/utils/api';

import {
  EntityTable,
  FilterSidebar,
  EmptyState,
  StatusBadge,
  HelpSection,
  FormatBadge,
  TypeBadge,
  useToast,
  type TableColumn,
  type FilterSection,
} from '../../../../shared';

import { RepositoryListTable } from '@/components/super/browse/repository-list/RepositoryListTable';
import { BrowseFilterSidebar } from '@/components/super/browse/repository-list/BrowseFilterSidebar';
import { MobileFilterDrawer } from '@/components/super/search/unified/MobileFilterDrawer';

import { useRepositoriesApi } from './useRepositoriesApi';
import { ensureTrailingSlash } from '@/utils/url';
import {
  Repository,
  RepositoriesListProps,
  SortDirection,
  RepositorySortField,
  FORMAT_LABELS,
  TYPE_LABELS,
  HealthCheckStatus,
} from './types';
import { FormatIcon } from '@/components/super/settings/repository/repositories/components/FormatIcon';
import { HealthCheckCell } from '../../../../shared/security/HealthCheckCell';
import { FirewallCell } from '../../../../shared/security/FirewallCell';

import './RepositoriesList.scss';

interface FirewallStatusData {
  repositoryName: string;
  affectedComponentCount: number;
  criticalComponentCount: number;
  severeComponentCount: number;
  moderateComponentCount: number;
  quarantinedComponentCount: number;
  reportUrl?: string;
  message?: string | null;
  errorMessage?: string | null;
}

function isIqServerEnabled(): boolean {
  try { return ExtJS.state().getValue('clm')?.enabled ?? false; }
  catch { return false; }
}

function canUpdateHealthCheck(): boolean {
  try { return ExtJS.checkPermission('nexus:healthcheck:update'); }
  catch { return false; }
}

function canReadFirewallStatus(): boolean {
  try { return ExtJS.checkPermission('nexus:iq-violation-summary:read'); }
  catch { return false; }
}

const DEFAULT_PAGE_SIZE = 40;

/**
 * RepositoriesList - Displays repositories using shared components
 */
export function RepositoriesList({ onSelect, onCreate, onDelete }: RepositoriesListProps) {
  const [repositories, setRepositories] = useState<Repository[]>([]);
  const [healthStatus, setHealthStatus] = useState<Record<string, HealthCheckStatus>>({});
  const [filter, setFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState<string[]>([]);
  const [formatFilter, setFormatFilter] = useState<string[]>([]);
  const [sortField, setSortField] = useState<RepositorySortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [loadingRepos, setLoadingRepos] = useState(true);
  const [copiedUrl, setCopiedUrl] = useState<string | null>(null);
  const [analyzingRepos, setAnalyzingRepos] = useState<Set<string>>(new Set());
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [repoToDelete, setRepoToDelete] = useState<Repository | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [firewallStatus, setFirewallStatus] = useState<Record<string, FirewallStatusData>>({});
  const [firewallLoaded, setFirewallLoaded] = useState(false);
  const [showMobileFilters, setShowMobileFilters] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

  const { error, setError, fetchRepositories, deleteRepository } = useRepositoriesApi();

  const [showHealthCheckColumn, setShowHealthCheckColumn] = useState(() => canUpdateHealthCheck());
  const [showFirewallColumn, setShowFirewallColumn] = useState(() => isIqServerEnabled() && canReadFirewallStatus());

  // Toast notifications
  const toast = useToast();

  // Check permissions
  const canEdit = ExtJS.checkPermission('nexus:repository-admin:*:*:edit');
  const canDelete = ExtJS.checkPermission('nexus:repository-admin:*:*:delete');

  const fetchHealthCheck = useCallback(async () => {
    try {
      const data = await restClient.get<Array<HealthCheckStatus & { repositoryName: string }>>(ENDPOINTS.HEALTH_CHECK);
      return (data || []).reduce<Record<string, HealthCheckStatus>>((acc, item) => {
        acc[item.repositoryName] = item;
        return acc;
      }, {});
    } catch {
      return {};
    }
  }, []);

  const fetchFirewallStatus = useCallback(async () => {
    try {
      const data = await restClient.get<FirewallStatusData[]>(ENDPOINTS.FIREWALL_STATUS);
      return (data || []).reduce<Record<string, FirewallStatusData>>((acc, item) => {
        acc[item.repositoryName] = item;
        return acc;
      }, {});
    } catch {
      return {};
    }
  }, []);

  // Load repositories on mount
  useEffect(() => {
    const loadData = async () => {
      setLoadingRepos(true);
      try {
        const repos = await fetchRepositories();
        console.info('[RepositoriesList] Loaded', repos?.length, 'repositories');
        setRepositories(repos);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Failed to load repositories';
        setError(message);
      } finally {
        setLoadingRepos(false);
      }

      // Re-check permissions after ExtJS state may have loaded
      const healthEnabled = canUpdateHealthCheck();
      const firewallEnabled = isIqServerEnabled() && canReadFirewallStatus();
      if (healthEnabled) setShowHealthCheckColumn(true);
      if (firewallEnabled) setShowFirewallColumn(true);

      const [health, firewall] = await Promise.all([
        fetchHealthCheck(),
        fetchFirewallStatus(),
      ]);
      setHealthStatus(health);
      setFirewallStatus(firewall);
      setFirewallLoaded(true);

      if (Object.keys(health).length > 0) setShowHealthCheckColumn(true);
      if (Object.keys(firewall).length > 0) setShowFirewallColumn(true);
    };

    loadData();
  }, [fetchRepositories, fetchHealthCheck, fetchFirewallStatus, setError]);

  // Get unique formats and types for filters with counts
  const { formatOptions, typeOptions } = useMemo(() => {
    const formatCounts = new Map<string, number>();
    const typeCounts = new Map<string, number>();
    
    repositories.forEach((repo) => {
      formatCounts.set(repo.format, (formatCounts.get(repo.format) || 0) + 1);
      typeCounts.set(repo.type, (typeCounts.get(repo.type) || 0) + 1);
    });
    
    return {
      formatOptions: Array.from(formatCounts.entries())
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([value, count]) => ({
          value,
          label: FORMAT_LABELS[value] || value,
          count,
        })),
      typeOptions: Array.from(typeCounts.entries())
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([value, count]) => ({
          value,
          label: TYPE_LABELS[value as keyof typeof TYPE_LABELS] || value,
          count,
        })),
    };
  }, [repositories]);

  // Filter repositories
  const filteredRepositories = useMemo(() => {
    return repositories.filter((repo) => {
      // Text filter
      if (filter) {
        const searchLower = filter.toLowerCase();
        const matchesName = repo.name?.toLowerCase().includes(searchLower);
        const matchesFormat = repo.format?.toLowerCase().includes(searchLower);
        if (!matchesName && !matchesFormat) {
          return false;
        }
      }
      
      // Type filter (array now)
      if (typeFilter.length > 0 && !typeFilter.includes(repo.type)) {
        return false;
      }
      
      // Format filter (array now)
      if (formatFilter.length > 0 && !formatFilter.includes(repo.format)) {
        return false;
      }
      
      return true;
    });
  }, [repositories, filter, typeFilter, formatFilter]);

  // Sort repositories
  const sortedRepositories = useMemo(() => {
    if (!sortDirection) return filteredRepositories;

    return [...filteredRepositories].sort((a, b) => {
      let aVal: string | boolean = '';
      let bVal: string | boolean = '';

      switch (sortField) {
        case 'name':
          aVal = a.name || '';
          bVal = b.name || '';
          break;
        case 'type':
          aVal = a.type || '';
          bVal = b.type || '';
          break;
        case 'format':
          aVal = a.format || '';
          bVal = b.format || '';
          break;
        case 'status':
          aVal = a.status?.online ?? a.online ?? true;
          bVal = b.status?.online ?? b.online ?? true;
          if (aVal !== bVal) {
            return sortDirection === 'asc'
              ? (aVal ? -1 : 1)
              : (aVal ? 1 : -1);
          }
          aVal = a.name || '';
          bVal = b.name || '';
          break;
      }

      const comparison = String(aVal).toLowerCase().localeCompare(String(bVal).toLowerCase());
      return sortDirection === 'asc' ? comparison : -comparison;
    });
  }, [filteredRepositories, sortField, sortDirection]);

  const handleSort = useCallback((columnId: string) => {
    const field = columnId as RepositorySortField;
    if (sortField === field) {
      if (sortDirection === 'asc') {
        setSortDirection('desc');
      } else if (sortDirection === 'desc') {
        setSortDirection(null);
      } else {
        setSortDirection('asc');
      }
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  }, [sortField, sortDirection]);

  const handleRowClick = useCallback((repo: Repository) => {
    onSelect(repo.name);
  }, [onSelect]);

  const handleCopyUrl = useCallback((e: React.MouseEvent, url: string) => {
    e.stopPropagation();
    const urlWithSlash = ensureTrailingSlash(url);
    navigator.clipboard.writeText(urlWithSlash).then(() => {
      setCopiedUrl(urlWithSlash);
      setTimeout(() => setCopiedUrl(null), 2000);
      toast.success('URL copied to clipboard');
    }).catch(() => {
      toast.error('Failed to copy URL');
    });
  }, []);

  const handleFilterChange = useCallback((sectionId: string, value: string | string[]) => {
    if (sectionId === 'type') {
      setTypeFilter(value as string[]);
    } else if (sectionId === 'format') {
      setFormatFilter(value as string[]);
    } else if (sectionId === 'search') {
      setFilter(value as string);
    }
  }, []);

  const handleClearFilters = useCallback(() => {
    setFilter('');
    setTypeFilter([]);
    setFormatFilter([]);
  }, []);

  const handleRetry = useCallback(async () => {
    setError(null);
    setLoadingRepos(true);
    try {
      const repos = await fetchRepositories();
      setRepositories(repos);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to load repositories';
      setError(message);
    } finally {
      setLoadingRepos(false);
    }
    const healthEnabled = canUpdateHealthCheck();
    const firewallEnabled = isIqServerEnabled() && canReadFirewallStatus();
    if (healthEnabled) setShowHealthCheckColumn(true);
    if (firewallEnabled) setShowFirewallColumn(true);

    const [health, firewall] = await Promise.all([
      fetchHealthCheck(),
      fetchFirewallStatus(),
    ]);
    setHealthStatus(health);
    setFirewallStatus(firewall);
    setFirewallLoaded(true);
    if (Object.keys(health).length > 0) setShowHealthCheckColumn(true);
    if (Object.keys(firewall).length > 0) setShowFirewallColumn(true);
  }, [fetchRepositories, fetchHealthCheck, fetchFirewallStatus, setError]);

  const handleAnalyze = useCallback(async (repositoryName: string) => {
    setAnalyzingRepos((prev) => new Set(prev).add(repositoryName));
    try {
      await restClient.post(ENDPOINTS.HEALTH_CHECK_ANALYZE(repositoryName));
      const health = await fetchHealthCheck();
      setHealthStatus(health);
      toast.success(`Health check enabled for ${repositoryName}`);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to enable health check';
      toast.error(`Failed to start analysis for ${repositoryName}: ${message}`);
    } finally {
      setAnalyzingRepos((prev) => {
        const next = new Set(prev);
        next.delete(repositoryName);
        return next;
      });
    }
  }, [fetchHealthCheck, toast]);

  // Handle delete button click - opens confirmation modal
  const handleDeleteClick = useCallback((e: React.MouseEvent, repo: Repository) => {
    e.stopPropagation();
    setRepoToDelete(repo);
    setDeleteModalOpen(true);
  }, []);

  // Handle delete confirmation
  const handleDeleteConfirm = useCallback(async () => {
    if (!repoToDelete) return;

    setIsDeleting(true);
    try {
      if (onDelete) {
        await onDelete(repoToDelete.name);
      } else {
        await deleteRepository(repoToDelete.name);
      }
      // Remove from local state
      setRepositories((prev) => prev.filter((r) => r.name !== repoToDelete.name));
      toast.success(`Repository "${repoToDelete.name}" deleted successfully`);
      setDeleteModalOpen(false);
      setRepoToDelete(null);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to delete repository';
      toast.error(message);
    } finally {
      setIsDeleting(false);
    }
  }, [repoToDelete, onDelete, deleteRepository]);

  // Handle edit button click
  const handleEditClick = useCallback((e: React.MouseEvent, repo: Repository) => {
    e.stopPropagation();
    onSelect(repo.name);
  }, [onSelect]);

  // Handle profile button click - navigate to profile view
  const handleProfileClick = useCallback((e: React.MouseEvent, repo: Repository) => {
    e.stopPropagation();
    window.location.hash = `preview/admin/repository/repositories/${encodeURIComponent(repo.name)}/profile`;
  }, []);

  // Filter sidebar sections
  const filterSections: FilterSection[] = useMemo(() => [
    {
      id: 'type',
      label: 'Type',
      type: 'checkbox',
      options: typeOptions,
      value: typeFilter,
      defaultExpanded: true,
    },
    {
      id: 'format',
      label: 'Format',
      type: 'checkbox',
      options: formatOptions,
      value: formatFilter,
      defaultExpanded: true,
    },
  ], [typeOptions, formatOptions, typeFilter, formatFilter]);

  // Table columns
  const columns: TableColumn<Repository>[] = useMemo(() => [
    {
      id: 'name',
      header: 'Name',
      accessor: (repo) => <span>{repo.name}</span>,
      sortable: true,
      width: '200px',
    },
    {
      id: 'type',
      header: 'Type',
      accessor: (repo) => <TypeBadge type={repo.type as any} />,
      sortable: true,
      width: '80px',
    },
    {
      id: 'format',
      header: 'Format',
      accessor: (repo) => <FormatBadge format={repo.format} />,
      sortable: true,
      width: '120px',
    },
    {
      id: 'blobStore',
      header: 'Blob Store',
      accessor: (repo) => {
        // ExtDirect API returns blobStoreName in attributes.storage.blobStoreName
        const blobStoreName = repo.attributes?.storage?.blobStoreName 
          || (repo as unknown as { blobStoreName?: string }).blobStoreName 
          || (repo as unknown as { storage?: { blobStoreName?: string } }).storage?.blobStoreName
          || '';
        return blobStoreName || '-';
      },
      sortable: true,
      width: '120px',
    },
    {
      id: 'status',
      header: 'Status',
      accessor: (repo) => {
        const isOnline = repo.status?.online ?? repo.online ?? true;
        const statusDesc = repo.status?.description;
        const displayText = statusDesc || (isOnline ? 'Online' : 'Offline');
        
        // Truncate long status messages
        const isTruncated = displayText.length > 20;
        const truncatedText = isTruncated ? `${displayText.substring(0, 17)}...` : displayText;
        
        const statusContent = (
          <Flex align="center" gap="2" className="repositories-list__status">
            <StatusBadge
              status={isOnline ? 'online' : 'offline'}
              size="small"
            />
            <span className="repositories-list__status-text">{truncatedText}</span>
          </Flex>
        );
        
        if (isTruncated) {
          return (
            <Tooltip content={displayText}>
              {statusContent}
            </Tooltip>
          );
        }
        return statusContent;
      },
      sortable: true,
      width: '180px',
    },
    {
      id: 'url',
      header: 'URL',
      accessor: (repo) => repo.url ? (
        <button
          type="button"
          onClick={(e) => handleCopyUrl(e, repo.url)}
          className="repositories-list__copy-btn"
          title="Copy repository URL"
          aria-label="Copy repository URL"
        >
          {copiedUrl === ensureTrailingSlash(repo.url) ? (
            <CheckCircle size={16} className="repositories-list__copy-icon repositories-list__copy-icon--success" />
          ) : (
            <Copy size={16} className="repositories-list__copy-icon" />
          )}
        </button>
      ) : null,
      width: '50px',
      align: 'center',
    },
    ...(showHealthCheckColumn ? [{
      id: 'healthCheck',
      header: 'Health Check',
      accessor: (repo: Repository) => (
        <HealthCheckCell
          repository={repo}
          healthStatus={(() => {
            const hc = healthStatus[repo.name] as any;
            if (!hc) return undefined;
            return {
              enabled: hc.enabled,
              analyzing: hc.analyzing,
              detailedReport: hc.detailUrl || hc.summaryUrl,
              securityIssueCount: hc.securityIssueCount,
              licenseIssueCount: hc.licenseIssueCount,
            };
          })()}
          onAnalyze={handleAnalyze}
          analyzeLoading={analyzingRepos.has(repo.name)}
        />
      ),
      width: '120px',
    }] : []),
    ...(showFirewallColumn ? [{
      id: 'firewall',
      header: 'Firewall Report',
      accessor: (repo: Repository) => (
        <FirewallCell
          repository={repo}
          firewallStatus={firewallStatus[repo.name]}
          firewallLoaded={firewallLoaded}
          hasFirewallLicense={true}
          onEnableSuccess={async () => {
            const next = await fetchFirewallStatus();
            setFirewallStatus(next);
          }}
        />
      ),
      width: '160px',
    }] : []),
    {
      id: 'actions',
      header: '',
      accessor: (repo) => (
        <Flex align="center" gap="1" className="repositories-list__actions">
          <button
            type="button"
            onClick={(e) => handleProfileClick(e, repo)}
            className="repositories-list__action-btn repositories-list__action-btn--profile"
            aria-label={`View ${repo.name}`}
            title={`View ${repo.name}`}
          >
            <Eye size={16} />
          </button>
          {canEdit && (
            <button
              type="button"
              onClick={(e) => handleEditClick(e, repo)}
              className="repositories-list__action-btn repositories-list__action-btn--edit"
              aria-label={`Edit ${repo.name}`}
              title={`Edit ${repo.name}`}
            >
              <Pencil size={16} />
            </button>
          )}
          {canDelete && (
            <button
              type="button"
              onClick={(e) => handleDeleteClick(e, repo)}
              className="repositories-list__action-btn repositories-list__action-btn--delete"
              aria-label={`Delete ${repo.name}`}
              title={`Delete ${repo.name}`}
            >
              <Trash2 size={16} />
            </button>
          )}
        </Flex>
      ),
      width: '100px',
      align: 'right',
    },
  ], [copiedUrl, handleCopyUrl, healthStatus, handleAnalyze, analyzingRepos, canEdit, canDelete, handleProfileClick, handleEditClick, handleDeleteClick, showHealthCheckColumn, showFirewallColumn, firewallStatus, firewallLoaded, fetchFirewallStatus]);

  // Empty state for when no repositories exist
  const emptyState = useMemo(() => {
    const hasFilters = filter || typeFilter.length > 0 || formatFilter.length > 0;
    
    if (hasFilters) {
      return (
        <EmptyState
          icon={Database}
          title="No Matching Repositories"
          description="No repositories match your current filters. Try adjusting your filter criteria."
          action={{
            label: 'Clear Filters',
            onClick: handleClearFilters,
          }}
        />
      );
    }

    return (
      <EmptyState
        icon={Database}
        title="No Repositories"
        description="Create your first repository to start storing and managing your software components."
        action={onCreate ? {
          label: 'Create Repository',
          onClick: onCreate,
          icon: Plus,
        } : undefined}
        secondaryAction={{
          label: 'Learn more about repositories',
          href: 'https://help.sonatype.com/en/repository-administration.html',
        }}
        tip="Start with a proxy repository to cache artifacts from Maven Central or npm registry."
      />
    );
  }, [filter, typeFilter, formatFilter, handleClearFilters, onCreate]);

  console.info('[RepositoriesList] Rendering:', {
    repoCount: repositories.length,
    sortedCount: sortedRepositories.length,
    loadingRepos,
    error,
    filter,
  });

  return (
    <Flex className="repositories-list" gap="4">
      {/* Filter Sidebar */}
      <FilterSidebar
        sections={filterSections}
        onFilterChange={handleFilterChange}
        onClear={handleClearFilters}
        disabled={loadingRepos}
        className="repositories-list__sidebar"
      />

      {/* Main Content */}
      <Box className="repositories-list__main">
        {/* Search Bar */}
        <Box className="repositories-list__search-container" data-testid="repositories-search">
          <TextField.Root
            placeholder="Search repositories by name..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="repositories-list__search-input"
          >
            <TextField.Slot>
              <Search size={16} />
            </TextField.Slot>
          </TextField.Root>
        </Box>

        {/* Table */}
        <EntityTable<Repository>
          data={sortedRepositories}
          columns={columns}
          getRowKey={(repo) => repo.name}
          onRowClick={handleRowClick}
          loading={loadingRepos}
          loadingMessage="Loading repositories..."
          error={error || undefined}
          onRetry={handleRetry}
          emptyState={emptyState}
          sortBy={sortField}
          sortDirection={sortDirection || undefined}
          onSort={handleSort}
          showRowArrow={false}
          clickable={true}
          ariaLabel="Repositories list"
          className="repositories-list__table"
        />

        {/* Summary */}
        {!loadingRepos && !error && sortedRepositories.length > 0 && (
          <Box className="repositories-list__summary">
            <Text size="2" color="gray">
              Showing {sortedRepositories.length} of {repositories.length} repositories
            </Text>
          </Box>
        )}

        {/* Help Section */}
        <HelpSection
          title="About Repositories"
          content="Repositories are storage locations where software components are stored, organized, and retrieved. Nexus supports hosted repositories for internal artifacts, proxy repositories for caching remote content, and group repositories for combining multiple repositories into a single URL."
          docLink={{
            label: 'View Documentation',
            href: 'https://help.sonatype.com/en/repository-administration.html',
          }}
          className="repositories-list__help"
        />
      </Box>

      {/* Delete Confirmation Modal */}
      <DeleteConfirmationModal
        open={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        onConfirm={handleDeleteConfirm}
        entityName={repoToDelete?.name || ''}
        entityType="repository"
        loading={isDeleting}
        storageSize="1.2 GB"
        componentCount={847}
      />
    </Flex>
  );
}

export default RepositoriesList;
