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

import React, { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';
import {
  Box,
  Button,
  Flex,
  Grid,
  Heading,
  IconButton,
  Text,
  TextField,
  Tooltip,
} from '@radix-ui/themes';
import { Copy, FolderTree, Search, X } from 'lucide-react';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';

import { RepositoryListTable } from './repository-list/RepositoryListTable';
import { useRepositoryList, isIqServerEnabled } from './repository-list/useRepositoryList';
import { useRepositoryListServer, arrayToFilterString } from './repository-list/useRepositoryListServer';
import { BrowseTree } from './tree/BrowseTree';
import { DetailPanel, type ComponentData, type AssetData } from './detail/DetailPanel';
import type { BrowseNode, BrowseTreeRef } from './tree/browse-tree.types';
import { fetchAsset, fetchComponent, type SearchResultItem } from './browse.api';
import { createBrowseMachine } from './browseMachine';
import type { RepositoryReference, RepositoryPageResponse, RepositoryType } from './browse.types';
import { FORMAT_LABELS, TYPE_LABELS } from '../settings/repository/repositories/types';
import { isHealthCheckSupportedFormat } from '../../../../utils/healthCheckFormats';

import { restClient, ENDPOINTS } from '../../../../interface/api';
import { isMockMode } from '../../config/featureFlags';
import { MOCK_REPOSITORIES } from './mockData';
import { ResizablePanel } from './ResizablePanel';

// Import shared components
import {
  TablePagination,
  useToast,
} from '../../shared';
import MalwareBanner from '../../shared/security/MalwareBanner';
import { BrowseFilterSidebar } from './repository-list/BrowseFilterSidebar';
import { MobileFilterDrawer } from '../search/unified/MobileFilterDrawer';
import { Breadcrumbs } from '../search/details/Breadcrumbs';
import { useBrowseBreadcrumbs } from './useBrowseBreadcrumbs';
import { useCanDelete } from './useCanDelete';
import { InRepositorySearch } from './InRepositorySearch';

import './BrowsePage.scss';

// Header + breadcrumb + toolbar combined height. Used to size the browse content
// area so it fills the remaining viewport. When the malware banner is visible it
// adds ~40px above the header; update this value if the layout chrome changes.
const BROWSE_CONTENT_TOP_OFFSET_PX = 280;

/**
 * Filter values for Browse (multi-select).
 */
export interface BrowseFilters {
  formats: string[];
  types: string[];
  statuses: string[];
}

/**
 * Available filter options computed from repositories.
 */
export interface FilterOptions {
  formats: Array<{ value: string; label: string; count: number }>;
  types: Array<{ value: string; label: string; count: number }>;
  statuses: Array<{ value: string; label: string; count: number }>;
}

/**
 * UI Strings for the browse page.
 */
const STRINGS = {
  pageTitle: 'Browse',
  pageDescription: 'Browse assets and components',

  selectItem: 'Select an item to view details',
  copyUrlTitle: 'Copy URL to Clipboard',
  urlCopied: 'URL copied to clipboard',
  filterPlaceholder: 'Filter by name...',
  treeDrawerTitle: 'Repository contents',
  showTree: 'Show tree',
};

/** Default page size for Browse repository list (matches Search-like behavior). */
const BROWSE_DEFAULT_PAGE_SIZE = 40;

/**
 * Route state names for browse navigation.
 */
const ROUTE_STATES = {
  base: 'preview.browse.browse',
  repo: 'preview.browse.browse.repo',
  path: 'preview.browse.browse.repo.path',
};

/**
 * BrowsePage is the main entry point for the Browse functionality.
 *
 * State management is now driven by browseMachine (XState), replacing
 * the 9 useState calls for selectedRepository, selectedNode, repositoryUrl,
 * detailData (asset/component/loading/error), filters, and nameFilter.
 *
 * Independent hooks (useRepositoryListServer, useRepositoryList) remain unchanged.
 */
export function BrowsePage(): JSX.Element {
  const router = useRouter();
  const { params } = useCurrentStateAndParams();

  // Ref to BrowseTree for programmatic expansion/scrolling
  const browseTreeRef = useRef<BrowseTreeRef>(null);

  // =========================================================================
  // XState Machine — replaces 9 useState calls
  // =========================================================================

  const browseMachine = useMemo(
    () => createBrowseMachine(params.repoName || undefined),
    []
  );

  const [machineState, send] = useMachine(browseMachine, {
    services: {
      loadNodeDetail: (ctx) => {
        if (!ctx.selectedNode || !ctx.selectedRepository) {
          return Promise.resolve({ asset: null, component: null });
        }

        if (ctx.selectedNode.type === 'asset') {
          if (!ctx.selectedNode.assetId) {
            return Promise.reject(new Error(
              'Unable to load asset details. The asset identifier is missing.'
            ));
          }
          const assetPromise = fetchAsset(ctx.selectedNode.assetId, ctx.selectedRepository);
          const componentPromise = ctx.selectedNode.componentId
            ? fetchComponent(ctx.selectedNode.componentId, ctx.selectedRepository).catch(() => null)
            : Promise.resolve(null);

          return Promise.all([assetPromise, componentPromise]).then(
            ([asset, comp]) => ({
              asset: {
                id: asset.id,
                name: asset.name,
                format: asset.format,
                contentType: asset.contentType,
                size: asset.size,
                repositoryName: asset.repositoryName,
                blobCreated: asset.blobCreated || null,
                blobUpdated: asset.lastModified || null,
                lastDownloaded: asset.lastDownloaded || null,
                path: asset.path || asset.name,
                downloadUrl: asset.downloadUrl, // absolute URL from server — includes context path, preferred over client-generated URL
                blobRef: asset.blobRef,
                createdBy: asset.createdBy,
                createdByIp: asset.createdByIp, // intentionally not rendered — privacy concern
                checksum: asset.checksum,
                // Pass through format-specific attributes (e.g. attributes.docker)
                // so the Attributes tab can surface image metadata (NEXUS-51972).
                attributes: asset.attributes,
                registryUrl: asset.registryUrl,
              } as AssetData,
              component: comp ? {
                id: comp.id,
                repositoryName: comp.repositoryName,
                format: comp.format,
                group: comp.group || null,
                name: comp.name,
                version: comp.version || null,
              } as ComponentData : null,
            }),
          );
        }

        if (ctx.selectedNode.type === 'component' && ctx.selectedNode.componentId) {
          return fetchComponent(ctx.selectedNode.componentId, ctx.selectedRepository).then(
            (component) => {
              return {
                asset: null,
                component: {
                  id: component.id,
                  repositoryName: component.repositoryName,
                  format: component.format,
                  group: component.group || null,
                  name: component.name,
                  version: component.version || null,
                } as ComponentData,
              };
            },
          ).catch((err) => {
            throw err;
          });
        }

        // Folders don't need extra data fetching
        return Promise.resolve({ asset: null, component: null });
      },
    },
  });

  // ---------------------------------------------------------------------------
  // Convenience reads from machine context (replaces 9 useState variables)
  // ---------------------------------------------------------------------------
  const selectedRepository = machineState.context.selectedRepository;
  const selectedNode = machineState.context.selectedNode;
  const assetData = machineState.context.detailData.asset;
  const componentData = machineState.context.detailData.component;
  const detailLoading = machineState.context.detailData.loading;
  const detailError = machineState.context.detailData.error;

  // Server-side preflight — see useCanDelete for why this must not become a
  // client-side ExtJS.checkPermission wildcard check (NEXUS-53861).
  const canDelete = useCanDelete(selectedNode, selectedRepository, componentData);
  const filters: BrowseFilters = useMemo(
    () => ({
      formats: machineState.context.filters.formats,
      types: machineState.context.filters.types,
      statuses: machineState.context.filters.statuses,
    }),
    [
      machineState.context.filters.formats,
      machineState.context.filters.types,
      machineState.context.filters.statuses,
    ],
  );
  const nameFilter = machineState.context.filters.nameFilter;

  // Measure the sticky header so table column headers stick below it
  const stickyHeaderRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = stickyHeaderRef.current;
    if (!el) return;

    const update = () => {
      const height = el.getBoundingClientRect().height;
      el.parentElement?.style.setProperty('--sticky-table-header-offset', `${height}px`);
    };

    update();
    const observer = new ResizeObserver(update);
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  // =========================================================================
  // URL Params Sync
  // =========================================================================

  // Sync selectedRepository from URL params when they change.
  // Handles navigation from search results, header autocomplete,
  // and direct URL entry.
  useEffect(() => {
    if (params.repoName && params.repoName !== selectedRepository) {
      // If already in tree view (different repo), go back first
      if (machineState.matches('treeView')) {
        send({ type: 'BACK' });
      }
      send({ type: 'SELECT_REPO', repoName: params.repoName });
    } else if (!params.repoName && machineState.matches('treeView')) {
      // Left menu navigated to base route — go back to repo list
      // (mirrors handleBackToList breadcrumb behavior)
      send({ type: 'BACK' });
    }
  }, [params.repoName]);

  // Decode path from URL if present
  const initialPath = params.path ? decodeURIComponent(params.path) : undefined;

  // =========================================================================
  // Independent Hooks (unchanged — NOT part of the machine)
  // =========================================================================

  // Server-side repository list with filtering and pagination
  const initialFormats = params.format ? params.format.toLowerCase() : undefined;
  const {
    repositories: serverRepositories,
    loading: serverLoading,
    error: serverError,
    filterParams,
    setFilterParams,
    totalCount,
    page,
    totalPages,
    goToPage,
  } = useRepositoryListServer({
    formats: initialFormats,
    pageSize: BROWSE_DEFAULT_PAGE_SIZE,
  });

  const {
    state: repoState,
    showHealthCheckColumn,
    showIqPolicyViolationsColumn,
    enableHealthCheck,
  } = useRepositoryList();

  const toast = useToast();
  const [analyzingRepos, setAnalyzingRepos] = useState<Set<string>>(new Set());
  const [showMobileFilters, setShowMobileFilters] = useState(false);

  const handleAnalyze = useCallback(async (repositoryName: string) => {
    setAnalyzingRepos((prev) => new Set(prev).add(repositoryName));
    try {
      await enableHealthCheck(repositoryName);
      toast.success(`Health check enabled for ${repositoryName}`);
    } catch (err) {
      toast.error(`Failed to start analysis for ${repositoryName}: ${err instanceof Error ? err.message : String(err)}`);
    } finally {
      setAnalyzingRepos((prev) => {
        const next = new Set(prev);
        next.delete(repositoryName);
        return next;
      });
    }
  }, [enableHealthCheck, toast]);

  // Protection filter (client-side only; uses IQ audit data)
  const [protectionFilter, setProtectionFilter] = useState<string[]>([]);
  const [healthCheckFilter, setHealthCheckFilter] = useState<string[]>([]);
  const [iqAuditRepos, setIqAuditRepos] = useState<Array<{ repositoryName: string; enabled?: boolean; enabledQuarantine?: boolean }>>([]);

  // Fetch ALL repositories for filter options (using server-side API with large page size)
  const [allReposForFilters, setAllReposForFilters] = useState<RepositoryReference[]>([]);
  const [_filterOptionsLoading, setFilterOptionsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const fetchAllRepos = async () => {
      try {
        if (isMockMode()) {
          if (!cancelled) {
            setAllReposForFilters([...MOCK_REPOSITORIES]);
            setFilterOptionsLoading(false);
          }
          return;
        }

        const PAGE_SIZE = 100;
        let page = 1;
        let allRepos: RepositoryReference[] = [];
        let hasMore = true;

        while (hasMore && !cancelled) {
          const response = await restClient.get<RepositoryPageResponse>(
            `/service/rest/internal/ui/repositories/details/filtered?pageSize=${PAGE_SIZE}&page=${page}`,
          );
          const pageData = response?.data || [];
          allRepos = allRepos.concat(pageData);
          hasMore = pageData.length === PAGE_SIZE;
          page++;
        }

        if (!cancelled) {
          setAllReposForFilters(allRepos);
          setFilterOptionsLoading(false);
        }
      } catch (error) {
        if (!cancelled) {
          console.error('Failed to fetch repositories for filter options:', error);
          setFilterOptionsLoading(false);
        }
      }
    };

    fetchAllRepos();

    return () => {
      cancelled = true;
    };
  }, []);

  // Fetch IQ audit status for protection filter (when IQ Server is enabled)
  useEffect(() => {
    if (!isIqServerEnabled()) return;
    let cancelled = false;
    restClient
      .get<Array<{ repositoryName: string; enabled?: boolean; enabledQuarantine?: boolean }>>(ENDPOINTS.IQ_AUDIT)
      .then((data) => {
        if (!cancelled && Array.isArray(data)) setIqAuditRepos(data);
      })
      .catch(() => {
        if (!cancelled) setIqAuditRepos([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Compute filter options from ALL repositories (for sidebar counts)
  const filterOptions: FilterOptions = useMemo(() => {
    const repos = allReposForFilters;

    const formatCounts = new Map<string, number>();
    const typeCounts = new Map<string, number>();
    const statusCounts = new Map<string, number>();

    repos.forEach((repo) => {
      const format = repo.format.toLowerCase();
      formatCounts.set(format, (formatCounts.get(format) || 0) + 1);

      const type = repo.type;
      typeCounts.set(type, (typeCounts.get(type) || 0) + 1);

      const status = repo.status?.online ? 'online' : 'offline';
      statusCounts.set(status, (statusCounts.get(status) || 0) + 1);
    });

    return {
      formats: Array.from(formatCounts.entries())
        .map(([value, count]) => ({ 
          value, 
          label: FORMAT_LABELS[value] || value, 
          count 
        }))
        .sort((a, b) => a.label.localeCompare(b.label)),
      types: Array.from(typeCounts.entries())
        .map(([value, count]) => ({ 
          value, 
          label: TYPE_LABELS[value as RepositoryType] || value, 
          count 
        }))
        .sort((a, b) => a.label.localeCompare(b.label)),
      statuses: Array.from(statusCounts.entries())
        .map(([value, count]) => ({ value, label: value, count }))
        .sort((a, b) => (a.value === 'online' ? -1 : 1)),
    };
  }, [allReposForFilters]);

  // Protection options from allRepos + IQ audit (Unprotected, Audited, Protected)
  const protectionOptions = useMemo(() => {
    if (!showIqPolicyViolationsColumn) return [];
    const auditByRepo = iqAuditRepos.reduce<Record<string, { enabled?: boolean; enabledQuarantine?: boolean }>>(
      (acc, item) => {
        acc[item.repositoryName] = { enabled: item.enabled, enabledQuarantine: item.enabledQuarantine };
        return acc;
      },
      {}
    );
    let unprotected = 0;
    let audited = 0;
    let protected_ = 0;
    allReposForFilters.forEach((repo) => {
      const audit = auditByRepo[repo.name];
      if (!audit?.enabled) {
        unprotected++;
      } else if (audit.enabledQuarantine) {
        protected_++;
      } else {
        audited++;
      }
    });
    const opts: Array<{ value: string; label: string; count: number }> = [];
    if (unprotected > 0) opts.push({ value: 'unprotected', label: 'Unprotected', count: unprotected });
    if (audited > 0) opts.push({ value: 'audited', label: 'Audited', count: audited });
    if (protected_ > 0) opts.push({ value: 'protected', label: 'Protected', count: protected_ });
    return opts;
  }, [allReposForFilters, iqAuditRepos, showIqPolicyViolationsColumn]);

  // Apply client-side protection filter on top of server-side filtered repos
  const filteredRepositories = useMemo(() => {
    let repos = serverRepositories;
    if (protectionFilter.length > 0) {
      const auditByRepo = iqAuditRepos.reduce<Record<string, { enabled?: boolean; enabledQuarantine?: boolean }>>(
        (acc, item) => {
          acc[item.repositoryName] = { enabled: item.enabled, enabledQuarantine: item.enabledQuarantine };
          return acc;
        },
        {}
      );
      repos = repos.filter((repo) => {
        const audit = auditByRepo[repo.name];
        const isUnprotected = !audit?.enabled;
        const isAudited = !!(audit?.enabled && !audit?.enabledQuarantine);
        const isProtected = !!(audit?.enabledQuarantine);
        return (
          (protectionFilter.includes('unprotected') && isUnprotected) ||
          (protectionFilter.includes('audited') && isAudited) ||
          (protectionFilter.includes('protected') && isProtected)
        );
      });
    }
    if (healthCheckFilter.length > 0) {
      repos = repos.filter((repo) => {
        if (repo.type !== 'proxy') return false;
        const supported = isHealthCheckSupportedFormat(repo.format);
        const hc = repoState.healthCheck?.[repo.name];
        const isActive = supported && hc?.enabled;
        const isAnalyze = supported && !hc?.enabled;
        const isUnsupported = !supported;
        return (
          (healthCheckFilter.includes('active') && isActive) ||
          (healthCheckFilter.includes('analyze') && isAnalyze) ||
          (healthCheckFilter.includes('unsupported') && isUnsupported)
        );
      });
    }
    return repos;
  }, [serverRepositories, protectionFilter, iqAuditRepos, healthCheckFilter, repoState.healthCheck]);

  // Check if any filters are active
  const hasActiveFilters = useMemo(() => {
    return (
      nameFilter.trim() !== '' ||
      filters.formats.length > 0 ||
      filters.types.length > 0 ||
      filters.statuses.length > 0 ||
      protectionFilter.length > 0 ||
      healthCheckFilter.length > 0
    );
  }, [nameFilter, filters, protectionFilter, healthCheckFilter]);

  // =========================================================================
  // Event Handlers (dispatch machine events + side effects)
  // =========================================================================

  /**
   * Handle repository selection from the list (row click, chevron).
   * Navigate to Browse Contents (folder tree) in same tab.
   * Eye icon uses handleViewProfile in RepositoryListTable to go to repository-profile.
   */
  const handleSelectRepository = useCallback(
    (repoName: string) => {
      router.stateService.go(ROUTE_STATES.repo, { repoName });
    },
    [router]
  );

  /**
   * Handle back button — return to repository list.
   */
  const handleBackToList = useCallback(() => {
    send({ type: 'BACK' });
    router.stateService.go(ROUTE_STATES.base, {}, { location: 'replace' });
  }, [send, router]);

  /**
   * Handle node selection from the tree.
   * Tree clicks update selection state and URL with path.
   */
  const handleSelectNode = useCallback(
    (node: BrowseNode) => {
      send({ type: 'SELECT_NODE', node });

      // Update URL with path for deep linking
      if (selectedRepository && node.id) {
        router.stateService.go(ROUTE_STATES.path, {
          repoName: selectedRepository,
          path: node.id,
          tab: params.tab || 'summary',
        });
      }
    },
    [send, selectedRepository, router, params.tab],
  );

  /**
   * Handle tab changes in detail panel.
   * Uses location: 'replace' to avoid polluting browser history.
   */
  const handleTabChange = useCallback(
    (newTab: string) => {
      router.stateService.go('.', { tab: newTab }, { location: 'replace' });
    },
    [router],
  );

  /**
   * Handle item deletion.
   */
  const handleDeleted = useCallback(() => {
    const deletedNodeId = selectedNode?.id;
    send({ type: 'NODE_DELETED' });
    // Remove the deleted node from the tree in-place, preserving expanded state
    if (deletedNodeId && browseTreeRef.current) {
      browseTreeRef.current.removeNode(deletedNodeId);
    }
    // Clear path from URL so tree doesn't re-select the deleted node
    if (selectedRepository) {
      router.stateService.go(ROUTE_STATES.repo, { repoName: selectedRepository }, { location: 'replace' });
    }
  }, [send, selectedRepository, selectedNode, router]);

  /**
   * Copy the current Preview UI page URL to clipboard so that opening it in a
   * new tab re-opens the same Browse view instead of navigating to Classic UI.
   */
  const handleCopyUrl = useCallback(() => {
    if (selectedRepository) {
      navigator.clipboard.writeText(window.location.href).then(() => {
        toast.success(STRINGS.urlCopied);
      });
    }
  }, [selectedRepository, toast]);

  /**
   * Handle search result selection - navigate and potentially load component directly.
   * For hierarchical formats (Maven), let the tree handle expansion.
   * For flat formats (npm, nuget), construct node from search result and load directly.
   */
  const handleSearchResultSelect = useCallback(
    (path: string, searchResult?: SearchResultItem) => {
      if (selectedRepository && path) {
        // If we have the search result with component ID, decode it to get the raw ID
        // Search API returns base64(repositoryName:rawId), but fetchComponent expects just rawId
        if (searchResult?.id) {
          try {
            let rawId: string | undefined;

            // Try to decode as base64 first (standard format for most components)
            try {
              const decoded = atob(searchResult.id);
              const parts = decoded.split(':');
              if (parts.length >= 2) {
                rawId = parts.slice(1).join(':'); // Everything after first colon is the raw ID
              }
            } catch {
              // Not base64 encoded - use the ID directly (common for npm, etc.)
              rawId = searchResult.id;
            }

            const node: BrowseNode = {
              id: path,
              text: searchResult.name,
              type: 'component',
              leaf: true,
              componentId: rawId || searchResult.id,
            };
            send({ type: 'SELECT_NODE', node });
          } catch (_err) {
            // Fallback: select node without component ID
            const node: BrowseNode = {
              id: path,
              text: searchResult.name || path,
              type: 'component',
              leaf: true,
            };
            send({ type: 'SELECT_NODE', node });
          }
        } else {
          // No search result ID - just navigate to path
          const node: BrowseNode = {
            id: path,
            text: searchResult?.name || path,
            type: 'component',
            leaf: true,
          };
          send({ type: 'SELECT_NODE', node });
        }

        // Navigate to URL (this will also trigger tree expansion for hierarchical formats)
        router.stateService.go(ROUTE_STATES.path, {
          repoName: selectedRepository,
          path: path, // Don't encode - route param has raw: true
          tab: 'summary',
        });
      }
    },
    [selectedRepository, router, send]
  );

  const breadcrumbItems = useBrowseBreadcrumbs(selectedRepository, handleBackToList);

  /**
   * Sync local filters to server-side API.
   */
  const syncFiltersToServer = useCallback(
    (newFilters: BrowseFilters, newNameFilter: string) => {
      setFilterParams({
        formats: arrayToFilterString(newFilters.formats),
        types: arrayToFilterString(newFilters.types),
        statuses: arrayToFilterString(newFilters.statuses),
        nameFilter: newNameFilter || undefined,
      });
    },
    [setFilterParams],
  );

  /**
   * Reset all filters.
   */
  const handleResetFilters = useCallback(() => {
    send({ type: 'CLEAR_FILTERS' });
    setProtectionFilter([]);
    setHealthCheckFilter([]);
    setFilterParams({
      formats: undefined,
      types: undefined,
      statuses: undefined,
      nameFilter: undefined,
      sortField: 'name',
      sortDirection: 'asc',
      page: 1,
    });
  }, [send, setFilterParams]);

  /**
   * Handle sort change from filter bar or table header.
   */
  const handleSort = useCallback(
    (field: string, direction: 'asc' | 'desc') => {
      setFilterParams({
        sortField: field as 'name' | 'type' | 'format' | 'status',
        sortDirection: direction,
        page: 1,
      });
    },
    [setFilterParams],
  );

  /**
   * Handle name filter change.
   */
  const handleNameFilterChange = useCallback(
    (value: string) => {
      send({ type: 'SET_NAME_FILTER', value });
      syncFiltersToServer(filters, value);
    },
    [send, filters, syncFiltersToServer],
  );

  // Sync format from URL params
  useEffect(() => {
    if (params.format) {
      const urlFormat = params.format.toLowerCase();
      if (!filters.formats.includes(urlFormat)) {
        send({ type: 'SET_FILTER', section: 'formats', value: [urlFormat] });
        setFilterParams({
          formats: urlFormat,
          types: arrayToFilterString(filters.types),
          statuses: arrayToFilterString(filters.statuses),
          nameFilter: nameFilter || undefined,
        });
      }
    }
  }, [params.format]);

  // Health check filter options (computed from repos)
  const healthCheckOptions = useMemo(() => {
    if (!showHealthCheckColumn) return [];
    const allRepos = allReposForFilters.length > 0 ? allReposForFilters : serverRepositories;
    let activeCount = 0, analyzeCount = 0, unsupportedCount = 0;
    for (const repo of allRepos) {
      if (repo.type !== 'proxy') continue;
      if (!isHealthCheckSupportedFormat(repo.format)) { unsupportedCount++; continue; }
      const hc = repoState.healthCheck?.[repo.name];
      if (hc?.enabled) { activeCount++; } else { analyzeCount++; }
    }
    const opts = [
      { value: 'active', label: 'Active', count: activeCount },
      { value: 'analyze', label: 'Analyze', count: analyzeCount },
      { value: 'unsupported', label: 'Not Supported', count: unsupportedCount },
    ].filter(o => o.count > 0);
    return opts;
  }, [showHealthCheckColumn, allReposForFilters, serverRepositories, repoState.healthCheck]);

  /**
   * Handle format/type/status filter changes (sync to machine + server).
   */
  const handleFormatsChange = useCallback(
    (values: string[]) => {
      send({ type: 'SET_FILTER', section: 'formats', value: values });
      syncFiltersToServer({ ...filters, formats: values }, nameFilter);
    },
    [send, filters, nameFilter, syncFiltersToServer],
  );
  const handleTypesChange = useCallback(
    (values: string[]) => {
      send({ type: 'SET_FILTER', section: 'types', value: values });
      syncFiltersToServer({ ...filters, types: values }, nameFilter);
    },
    [send, filters, nameFilter, syncFiltersToServer],
  );
  const handleStatusesChange = useCallback(
    (values: string[]) => {
      send({ type: 'SET_FILTER', section: 'statuses', value: values });
      syncFiltersToServer({ ...filters, statuses: values }, nameFilter);
    },
    [send, filters, nameFilter, syncFiltersToServer],
  );

  // =========================================================================
  // RENDER: Repository List with Sidebar (Step 1) — Reference Table Layout
  // =========================================================================
  if (!selectedRepository) {
    const isLoading = serverLoading || repoState.loading;
    const hasError = serverError || repoState.error;
    const sortField = filterParams.sortField ?? 'name';
    const sortDirection = (filterParams.sortDirection ?? 'asc') as 'asc' | 'desc';

    const displayTotal = totalCount.toLocaleString();

    const filterBarContent = (
      <BrowseFilterSidebar
        formatOptions={filterOptions.formats}
        typeOptions={filterOptions.types}
        statusOptions={filterOptions.statuses}
        protectionOptions={protectionOptions}
        healthCheckOptions={healthCheckOptions}
        selectedFormats={filters.formats}
        selectedTypes={filters.types}
        selectedStatuses={filters.statuses}
        selectedProtection={protectionFilter}
        selectedHealthCheck={healthCheckFilter}
        sortField={sortField}
        sortDirection={sortDirection}
        onFormatsChange={handleFormatsChange}
        onTypesChange={handleTypesChange}
        onStatusesChange={handleStatusesChange}
        onProtectionChange={protectionOptions.length > 0 ? setProtectionFilter : undefined}
        onHealthCheckChange={healthCheckOptions.length > 0 ? setHealthCheckFilter : undefined}
        onSortChange={handleSort}
        onResetFilters={handleResetFilters}
        disabled={isLoading}
      />
    );

    return (
      <div className="browse-page browse-page--list" data-testid="browse-page">
        <Box
          className="browse-page__list-wrapper"
          px={{ initial: '4', md: '6', lg: '6' }}
          py={{ initial: '4', md: '5', lg: '6' }}
          width="100%"
          style={{ minWidth: 0, boxSizing: 'border-box' }}
        >
          <Flex direction="column" gap="6" width="100%" style={{ minWidth: 0 }}>
            {/* Malware Alert Banner — repository list view */}
            <MalwareBanner />
            <Grid
              columns={{ initial: '1', sm: '250px 1fr' }}
              gap="6"
              width="100%"
              style={{ minWidth: 0 }}
            >
              {/* Filter Sidebar — hidden on mobile, matches Search layout */}
              <Box
                className="filter-bar"
                display={{ initial: 'none', sm: 'block' }}
                style={{ overflow: 'visible', minWidth: 0 }}
                role="complementary"
                aria-label="Filter bar"
              >
                <aside className="search-sidebar">
                  {filterBarContent}
                </aside>
              </Box>

              {/* Main Content — shared padding for header + table alignment */}
              <Box className="browse-page__main page-content" minWidth="0" width="100%" role="main" aria-label="Page content">
                <Box className="browse-page__list-content">
                  <Flex align="baseline" gap="2" mb="4">
                    <Heading as="h1" size="6" weight="bold">Repositories</Heading>
                    <Text size="2" color="gray">{displayTotal}</Text>
                  </Flex>
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
                          value={nameFilter}
                          onChange={(e) => handleNameFilterChange(e.target.value)}
                          size="2"
                          style={{ width: '100%' }}
                        >
                          <TextField.Slot>
                            <Search size={14} />
                          </TextField.Slot>
                          {nameFilter && (
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
                    </Flex>
                  </Box>

                  <Flex direction="column" gap="3" style={{ flex: 1 }}>
                  {/* Table (Card + Inset) or Loading/Error — no ScrollArea; page scrolls like Search */}
                <RepositoryListTable
                  repositories={filteredRepositories}
                  loading={isLoading}
                  error={hasError}
                  onSelect={handleSelectRepository}
                  healthCheck={repoState.healthCheck}
                  firewallStatus={repoState.firewallStatus}
                  firewallLoaded={repoState.firewallLoaded}
                  showHealthCheck={showHealthCheckColumn}
                  showIqPolicyViolations={true}
                  hasFilters={hasActiveFilters}
                  onAnalyze={handleAnalyze}
                  analyzingRepos={analyzingRepos}
                  hasFirewallLicense={showIqPolicyViolationsColumn}
                  proxyProtectionSummary={
                    protectionOptions.length > 0
                      ? {
                          totalProxy: protectionOptions.reduce((s, o) => s + (o.count || 0), 0),
                          protectedProxy:
                            (protectionOptions.find((o) => o.value === 'protected')?.count || 0) +
                            (protectionOptions.find((o) => o.value === 'audited')?.count || 0),
                        }
                      : undefined
                  }
                  sortKey={sortField}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />

                  {/* Pagination */}
                  {totalCount > 0 && (
                    <Box className="browse-page__pagination" p="3">
                      <TablePagination
                        currentPage={page}
                        totalPages={Math.max(1, totalPages)}
                        itemsPerPage={filterParams.pageSize ?? BROWSE_DEFAULT_PAGE_SIZE}
                        totalItems={totalCount}
                        onPageChange={goToPage}
                        onItemsPerPageChange={(pageSize) => setFilterParams({ pageSize, page: 1 })}
                        mt="0"
                      />
                    </Box>
                  )}
                </Flex>
                </Box>
              </Box>
            </Grid>
          </Flex>
        </Box>

        <MobileFilterDrawer
          isOpen={showMobileFilters}
          onClose={() => setShowMobileFilters(false)}
          title="Filter"
          onClearAll={handleResetFilters}
        >
          {filterBarContent}
        </MobileFilterDrawer>
      </div>
    );
  }

  // =========================================================================
  // RENDER: Tree + Detail (Step 2) - GitHub Style Layout
  // =========================================================================

  return (
    <div className="browse-page browse-page--tree" data-testid="browse-page">
      {/* Fixed header with search - always visible */}
      <Box
        className="browse-page__header-fixed"
        px="4"
        pt="0"
        pb="3"
        style={{
          position: 'sticky',
          top: 0,
          zIndex: 10,
          backgroundColor: 'var(--color-background)',
          borderBottom: '1px solid var(--gray-5)',
        }}
      >
        <Box maxWidth="1280px" mx="auto" width="100%">
          <Breadcrumbs items={breadcrumbItems} />

          {/* Malware Alert Banner */}
          <MalwareBanner />

          {/* Search bar and actions */}
          <Box p="4">
            <Grid columns={{ initial: '1', md: '1fr auto' }} gap="6" align="start">
              <Flex direction="column" gap="3">
                <Heading size="6">{selectedRepository}</Heading>
                {/* In-Repository Search - Always visible */}
                <InRepositorySearch
                  repositoryName={selectedRepository}
                  onSelectResult={handleSearchResultSelect}
                />
                <Flex align="end" gap="4" wrap="wrap">
                  <Box display={{ initial: 'block', sm: 'none' }}>
                    <Button
                      size="2"
                      variant="soft"
                      color="gray"
                      onClick={() => setShowMobileFilters(true)}
                    >
                      <FolderTree size={14} />
                      {STRINGS.showTree}
                    </Button>
                  </Box>
                  <Tooltip content={STRINGS.copyUrlTitle}>
                    <Button
                      variant="soft"
                      size="2"
                      color="blue"
                      onClick={handleCopyUrl}
                      aria-label={STRINGS.copyUrlTitle}
                    >
                      <Copy size={14} />
                      Copy URL
                    </Button>
                  </Tooltip>
                </Flex>
              </Flex>
            </Grid>
          </Box>
        </Box>
      </Box>

      {/* Resizable panels - takes remaining viewport height */}
      <Box
        className="browse-page__panels"
        px="4"
        style={{
          height: `calc(100vh - ${BROWSE_CONTENT_TOP_OFFSET_PX}px)`,
          overflow: 'hidden',
        }}
      >
        <Box maxWidth="1280px" mx="auto" width="100%" height="100%">
          <ResizablePanel
            leftPanel={
              <BrowseTree
                ref={browseTreeRef}
                repositoryName={selectedRepository}
                initialPath={initialPath}
                onSelect={handleSelectNode}
                baseUrl="preview/browse"
              />
            }
            rightPanel={
              <DetailPanel
                node={selectedNode}
                repositoryName={selectedRepository}
                assetData={assetData}
                componentData={componentData}
                loading={detailLoading}
                error={detailError}
                onDeleted={handleDeleted}
                canDelete={canDelete}
                activeTab={params.tab || 'summary'}
                onTabChange={handleTabChange}
              />
            }
            defaultLeftWidth={400}
            minLeftWidth={250}
            maxLeftWidth={800}
            storageKey="browse-tree-width"
          />
        </Box>
      </Box>

      {/* Mobile tree drawer */}
      <MobileFilterDrawer
        isOpen={showMobileFilters}
        onClose={() => setShowMobileFilters(false)}
        title={STRINGS.treeDrawerTitle}
      >
        <BrowseTree
          ref={browseTreeRef}
          repositoryName={selectedRepository}
          initialPath={initialPath}
          onSelect={handleSelectNode}
          baseUrl="preview/browse"
        />
      </MobileFilterDrawer>
    </div>
  );
}

export default BrowsePage;
