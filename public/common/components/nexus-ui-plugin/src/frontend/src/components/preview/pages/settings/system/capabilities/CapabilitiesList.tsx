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
import { Box, Flex, Text, Badge, Tooltip } from '@radix-ui/themes';
import {
  Search, CheckCircle, XCircle, AlertCircle, Circle, Loader2, Pencil, PuzzleIcon,
} from 'lucide-react';
import { TYPE_ICONS, DEFAULT_TYPE_ICON } from './capabilityConstants';
import { ActionIcons } from '../../../../shared/icons/action-icons';
import { Capability, getStateColor, getStateName } from './types';
import { useCapabilitiesApi } from './useCapabilitiesApi';
import {
  EntityTable,
  FilterSidebar,
  EmptyState,
  HelpSection,
  type TableColumn,
  type FilterSection,
} from '../../../../shared';

import './CapabilitiesList.scss';

interface CapabilitiesListProps {
  onSelect: (capability: Capability) => void;
  refreshKey?: number;
}

const CAPABILITY_CATEGORIES: Record<string, string> = {
  audit: 'Audit',
  baseurl: 'Core',
  crowd: 'Security',
  customs3regions: 'Core',
  defaultrole: 'Security',
  // 'firewall.audit' deliberately omitted: filtered out at fetchCapabilities() and not
  // advertised by /v1/capabilities/types post-migration. See useCapabilitiesApi.ts.
  healthcheck: 'Health Check',
  'license-expiration': 'Core',
  LegacyUrlCapability: 'Core',
  'node.identity': 'Core',
  outreach: 'Core',
  OutreachManagementCapability: 'Core',
  'browse.trim': 'Repository',
  rutauth: 'Security',
  'scheduling.scheduler': 'Scheduling',
  StorageSettings: 'Core',
  'rapture.branding': 'UI',
  'rapture.settings': 'UI',
  migration: 'Core',
  'webhook.global': 'Webhook',
  'webhook.repository': 'Webhook',
};

function getCategoryForType(typeId: string): string {
  return CAPABILITY_CATEGORIES[typeId] || 'Other';
}

function TypeIcon({ typeId }: { typeId: string }) {
  const Icon = TYPE_ICONS[typeId] || DEFAULT_TYPE_ICON;
  return <Icon size={16} className="capabilities-list__category-icon" />;
}

function StateIcon({ state }: { state: string }) {
  const color = getStateColor(state as 'active' | 'disabled' | 'error' | 'passive');
  const iconProps = { size: 16, style: { color } };

  switch (state) {
    case 'active':
      return <CheckCircle {...iconProps} />;
    case 'disabled':
      return <Circle {...iconProps} />;
    case 'error':
      return <XCircle {...iconProps} />;
    case 'passive':
      return <AlertCircle {...iconProps} />;
    default:
      return <Circle {...iconProps} />;
  }
}

const STATE_LABELS: Record<string, string> = {
  active: 'Active',
  disabled: 'Disabled',
  error: 'Error',
  passive: 'Passive',
};

export function CapabilitiesList({ onSelect, refreshKey = 0 }: CapabilitiesListProps) {
  const { fetchCapabilities, fetchCapabilityTypes } = useCapabilitiesApi();
  const [capabilities, setCapabilities] = useState<Capability[]>([]);
  const [typeNameMap, setTypeNameMap] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string[]>([]);
  const [categoryFilter, setCategoryFilter] = useState<string[]>([]);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [capData, typeData] = await Promise.all([
        fetchCapabilities(),
        fetchCapabilityTypes(),
      ]);
      setCapabilities(capData);
      const nameMap: Record<string, string> = {};
      typeData.forEach((t) => { nameMap[t.id] = t.name; });
      setTypeNameMap(nameMap);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load capabilities');
    } finally {
      setLoading(false);
    }
  }, [fetchCapabilities, fetchCapabilityTypes]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Calculate counts for filters
  const { statusCounts, categoryCounts } = useMemo(() => {
    const sc = new Map<string, number>();
    const cc = new Map<string, number>();
    capabilities.forEach((cap) => {
      sc.set(cap.state, (sc.get(cap.state) || 0) + 1);
      const cat = getCategoryForType(cap.typeId);
      cc.set(cat, (cc.get(cat) || 0) + 1);
    });
    return { statusCounts: sc, categoryCounts: cc };
  }, [capabilities]);

  const filteredCapabilities = useMemo(() => {
    let result = [...capabilities];

    // Apply text search
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      result = result.filter(
        (cap) => {
          const displayName = typeNameMap[cap.typeId] || cap.typeName;
          const category = getCategoryForType(cap.typeId);
          return (
            displayName.toLowerCase().includes(term) ||
            cap.description?.toLowerCase().includes(term) ||
            cap.stateDescription?.toLowerCase().includes(term) ||
            cap.notes?.toLowerCase().includes(term) ||
            cap.state.toLowerCase().includes(term) ||
            category.toLowerCase().includes(term)
          );
        }
      );
    }

    // Apply status filter
    if (statusFilter.length > 0) {
      result = result.filter((cap) => statusFilter.includes(cap.state));
    }

    // Apply category filter
    if (categoryFilter.length > 0) {
      result = result.filter((cap) => categoryFilter.includes(getCategoryForType(cap.typeId)));
    }

    // Sort by type name ascending
    result.sort((a, b) => {
      const typeA = typeNameMap[a.typeId] || a.typeName || a.typeId;
      const typeB = typeNameMap[b.typeId] || b.typeName || b.typeId;
      return typeA.localeCompare(typeB);
    });

    return result;
  }, [capabilities, searchTerm, statusFilter, categoryFilter, typeNameMap]);

  const handleFilterChange = useCallback((sectionId: string, value: string | string[]) => {
    if (sectionId === 'status') setStatusFilter(value as string[]);
    else if (sectionId === 'category') setCategoryFilter(value as string[]);
  }, []);

  const handleClearFilters = useCallback(() => {
    setSearchTerm('');
    setStatusFilter([]);
    setCategoryFilter([]);
  }, []);

  const handleRowClick = useCallback((capability: Capability) => {
    onSelect(capability);
  }, [onSelect]);

  // Filter sections for sidebar
  const filterSections = useMemo<FilterSection[]>(() => [
    {
      id: 'status',
      label: 'State',
      type: 'checkbox',
      options: Array.from(statusCounts.entries())
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([value, count]) => ({ value, label: STATE_LABELS[value] || value, count })),
      value: statusFilter,
      defaultExpanded: true,
    },
    {
      id: 'category',
      label: 'Category',
      type: 'checkbox',
      options: Array.from(categoryCounts.entries())
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([value, count]) => ({ value, label: value, count })),
      value: categoryFilter,
      defaultExpanded: true,
    },
  ], [statusCounts, categoryCounts, statusFilter, categoryFilter]);

  // Table columns
  const columns = useMemo<TableColumn<Capability>[]>(() => [
    {
      id: 'type',
      header: 'Type',
      accessor: (capability) => {
        const displayName = capability.typeName || typeNameMap[capability.typeId] || capability.typeId;
        return (
          <Flex align="center" gap="2">
            <TypeIcon typeId={capability.typeId} />
            <Text weight="medium">{displayName}</Text>
          </Flex>
        );
      },
      sortable: true,
      width: '220px',
    },
    {
      id: 'state',
      header: 'State',
      accessor: (capability) => (
        <Flex align="center" gap="2">
          <StateIcon state={capability.state} />
          <Badge
            color={
              capability.state === 'active' ? 'green'
                : capability.state === 'error' ? 'red'
                : capability.state === 'passive' ? 'orange'
                : 'gray'
            }
            variant="soft"
          >
            {getStateName(capability.state as 'active' | 'disabled' | 'error' | 'passive')}
          </Badge>
        </Flex>
      ),
      sortable: true,
      width: '120px',
    },
    {
      id: 'category',
      header: 'Category',
      accessor: (capability) => (
        <Badge variant="outline" size="1">{getCategoryForType(capability.typeId)}</Badge>
      ),
      sortable: true,
      width: '120px',
    },
    {
      id: 'description',
      header: 'Description',
      accessor: (capability) => {
        const description = capability.description || capability.stateDescription || '-';
        return (
          <Tooltip content={description}>
            <Text size="2" className="capabilities-list__description-text">{description}</Text>
          </Tooltip>
        );
      },
      sortable: true,
      width: '250px',
    },
    {
      id: 'notes',
      header: 'Notes',
      accessor: (capability) => {
        const notes = capability.notes || '-';
        return notes !== '-' ? (
          <Tooltip content={notes}>
            <Text size="2">{notes}</Text>
          </Tooltip>
        ) : (
          <Text size="2" color="gray">-</Text>
        );
      },
      width: '200px',
    },
    {
      id: 'actions',
      header: '',
      accessor: () => (
        <Flex align="center" justify="end" className="capabilities-list__row-actions">
          <Pencil size={16} className="capabilities-list__edit-icon" />
        </Flex>
      ),
      width: '40px',
      align: 'right',
    },
  ], [typeNameMap]);

  const emptyState = useMemo(() => {
    const hasFilters = searchTerm || statusFilter.length > 0 || categoryFilter.length > 0;
    if (hasFilters) {
      return (
        <EmptyState
          icon={PuzzleIcon}
          title="No Matching Capabilities"
          description="No capabilities match your current filters. Try adjusting your filter criteria."
          action={{ label: 'Clear Filters', onClick: handleClearFilters }}
        />
      );
    }
    return (
      <EmptyState
        icon={PuzzleIcon}
        title="No Capabilities"
        description="Capabilities are optional features that can be enabled to extend Nexus Repository functionality."
        secondaryAction={{
          label: 'Learn more about capabilities',
          href: 'http://links.sonatype.com/products/nxrm3/docs/capabilities',
        }}
        tip="Capabilities provide additional functionality like security integration, health checks, and custom configurations."
      />
    );
  }, [searchTerm, statusFilter, categoryFilter, handleClearFilters]);

  if (loading) {
    return (
      <Flex align="center" justify="center" className="capabilities-list__loading">
        <Loader2 size={24} className="capabilities-list__spinner" />
        <Text size="2">Loading capabilities...</Text>
      </Flex>
    );
  }

  if (error) {
    return (
      <Box className="capabilities-list__error">
        <AlertCircle size={20} />
        <Text size="2">{error}</Text>
      </Box>
    );
  }

  return (
    <Flex className="capabilities-list" gap="4" data-testid="capabilities-list">
      <FilterSidebar
        sections={filterSections}
        onFilterChange={handleFilterChange}
        onClear={handleClearFilters}
        disabled={loading}
        className="capabilities-list__sidebar"
      />

      <Box className="capabilities-list__main">
        <Box className="capabilities-list__search">
          <Box className="capabilities-list__search-wrapper">
            <Search size={16} className="capabilities-list__search-icon" />
            <input
              type="text"
              placeholder="Search capabilities..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="capabilities-list__search-input"
            />
            {searchTerm && (
              <button
                type="button"
                className="capabilities-list__search-clear"
                onClick={() => setSearchTerm('')}
                aria-label="Clear search"
              >
                <ActionIcons.Cancel size={14} />
              </button>
            )}
          </Box>
          <Text size="2" className="capabilities-list__count">
            Showing {filteredCapabilities.length} of {capabilities.length} capabilities
          </Text>
        </Box>

        <EntityTable<Capability>
          data={filteredCapabilities}
          columns={columns}
          getRowKey={(capability) => capability.id}
          onRowClick={handleRowClick}
          loading={loading}
          loadingMessage="Loading capabilities..."
          error={error || undefined}
          onRetry={() => loadData()}
          emptyState={emptyState}
          showRowArrow={true}
          clickable={true}
          ariaLabel="Capabilities list"
          className="capabilities-list__table"
        />

        <HelpSection
          title="About Capabilities"
          content="Capabilities are optional features that extend Nexus Repository Manager functionality. They can be enabled or disabled based on your needs. Some capabilities may require additional configuration."
          docLink={{
            label: 'View Documentation',
            href: 'http://links.sonatype.com/products/nxrm3/docs/capabilities',
          }}
          className="capabilities-list__help"
        />
      </Box>
    </Flex>
  );
}

export default CapabilitiesList;
