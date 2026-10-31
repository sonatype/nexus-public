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
import { Box, Flex, } from '@radix-ui/themes';
import { 
  Search, 
  ChevronUp, 
  ChevronDown, 
  ChevronsUpDown,
  Ban,
  Check,
  Route,
  Plus,
  Pencil,
} from 'lucide-react';

import { 
  LoadingState, 
  ErrorState, 
  EmptyState, 
  HelpSection,
} from '../../../../shared';
import { useRoutingRulesApi } from './useRoutingRulesApi';
import { 
  RoutingRule, 
  SortDirection, 
  RoutingRuleSortField, 
  RoutingRulesListProps,
  ROUTING_MODE_LABELS,
} from './types';

import './RoutingRulesList.scss';

/**
 * RoutingRulesList - Displays routing rules in a searchable, sortable table
 */
export function RoutingRulesList({ onSelect, onCreate, onPreview }: RoutingRulesListProps) {
  const [rules, setRules] = useState<RoutingRule[]>([]);
  const [filter, setFilter] = useState('');
  const [sortField, setSortField] = useState<RoutingRuleSortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [loadingRules, setLoadingRules] = useState(true);

  const { error, setError, fetchRoutingRules } = useRoutingRulesApi();

  // Load rules on mount
  const loadRules = useCallback(async () => {
    setLoadingRules(true);
    setError(null);
    try {
      const data = await fetchRoutingRules(true);
      setRules(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoadingRules(false);
    }
  }, [fetchRoutingRules, setError]);

  useEffect(() => {
    loadRules();
  }, [loadRules]);

  // Filter rules by name/description
  const filteredRules = useMemo(() => {
    if (!filter) return rules;
    const searchLower = filter.toLowerCase();
    return rules.filter((rule) => 
      rule.name?.toLowerCase().includes(searchLower) ||
      rule.description?.toLowerCase().includes(searchLower)
    );
  }, [rules, filter]);

  // Sort rules
  const sortedRules = useMemo(() => {
    if (!sortDirection) return filteredRules;

    return [...filteredRules].sort((a, b) => {
      let aVal: string | number = '';
      let bVal: string | number = '';

      switch (sortField) {
        case 'name':
          aVal = a.name || '';
          bVal = b.name || '';
          break;
        case 'description':
          aVal = a.description || '';
          bVal = b.description || '';
          break;
        case 'mode':
          aVal = a.mode || '';
          bVal = b.mode || '';
          break;
        case 'assignedRepositoryCount': {
          aVal = a.assignedRepositoryCount ?? 0;
          bVal = b.assignedRepositoryCount ?? 0;
          const numComparison = (aVal as number) - (bVal as number);
          return sortDirection === 'asc' ? numComparison : -numComparison;
        }
      }

      const comparison = String(aVal).toLowerCase().localeCompare(String(bVal).toLowerCase());
      return sortDirection === 'asc' ? comparison : -comparison;
    });
  }, [filteredRules, sortField, sortDirection]);

  const handleSort = useCallback((field: RoutingRuleSortField) => {
    if (sortField === field) {
      // Cycle: asc -> desc -> null -> asc
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

  const handleRowClick = useCallback((rule: RoutingRule) => {
    onSelect(rule.name);
  }, [onSelect]);

  const renderSortIcon = (field: RoutingRuleSortField) => {
    if (sortField !== field || !sortDirection) {
      return <ChevronsUpDown size={14} className="routing-rules-list__sort-icon routing-rules-list__sort-icon--inactive" />;
    }
    return sortDirection === 'asc' 
      ? <ChevronUp size={14} className="routing-rules-list__sort-icon" />
      : <ChevronDown size={14} className="routing-rules-list__sort-icon" />;
  };

  const renderModeIcon = (mode: string) => {
    if (mode === 'BLOCK') {
      return <Ban size={14} className="routing-rules-list__mode-icon routing-rules-list__mode-icon--block" />;
    }
    return <Check size={14} className="routing-rules-list__mode-icon routing-rules-list__mode-icon--allow" />;
  };

  return (
    <Box className="routing-rules-list">
      {/* Filters */}
      <Flex gap="4" className="routing-rules-list__filters">
        <Box className="routing-rules-list__search">
          <Search size={16} className="routing-rules-list__search-icon" />
          <input
            type="text"
            aria-label="Filter routing rules by name or description"
            placeholder="Filter by name or description"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="routing-rules-list__search-input"
          />
        </Box>
      </Flex>

      {/* Error State - using shared component */}
      {error && (
        <ErrorState
          variant="inline"
          message={error}
          onRetry={loadRules}
        />
      )}

      {/* Loading State - using shared component */}
      {loadingRules && (
        <LoadingState message="Loading routing rules..." />
      )}

      {/* Empty State - using shared component */}
      {!(loadingRules || error ) && sortedRules.length === 0 && (
        <EmptyState
          icon={Route}
          title={filter ? 'No matching rules' : 'No Routing Rules'}
          description={
            filter 
              ? 'No routing rules match your filter criteria. Try adjusting your search.' 
              : 'Create routing rules to control which requests are allowed or blocked for proxy repositories.'
          }
          action={!filter ? {
            label: 'Create Routing Rule',
            onClick: onCreate,
            icon: Plus,
          } : undefined}
          secondaryAction={{
            label: 'Learn about routing rules',
            href: 'http://links.sonatype.com/products/nxrm3/docs/routing-rule',
          }}
          size="medium"
        />
      )}

      {/* Table */}
      {!(loadingRules || error ) && sortedRules.length > 0 && (
        <Box className="routing-rules-list__table-wrapper">
          <table className="routing-rules-list__table">
            <thead>
              <tr>
                <th className="routing-rules-list__th routing-rules-list__th--sortable">
                  <button
                    type="button"
                    onClick={() => handleSort('name')}
                    className="routing-rules-list__sort-button"
                    aria-label={`Sort by name${sortField === 'name' ? `, ${sortDirection === 'asc' ? 'ascending' : sortDirection === 'desc' ? 'descending' : 'not sorted'}` : ''}`}
                  >
                    <Flex align="center" gap="1">
                      Name
                      {renderSortIcon('name')}
                    </Flex>
                  </button>
                </th>
                <th className="routing-rules-list__th routing-rules-list__th--sortable">
                  <button
                    type="button"
                    onClick={() => handleSort('description')}
                    className="routing-rules-list__sort-button"
                    aria-label={`Sort by description${sortField === 'description' ? `, ${sortDirection === 'asc' ? 'ascending' : sortDirection === 'desc' ? 'descending' : 'not sorted'}` : ''}`}
                  >
                    <Flex align="center" gap="1">
                      Description
                      {renderSortIcon('description')}
                    </Flex>
                  </button>
                </th>
                <th className="routing-rules-list__th routing-rules-list__th--sortable routing-rules-list__th--mode">
                  <button
                    type="button"
                    onClick={() => handleSort('mode')}
                    className="routing-rules-list__sort-button"
                    aria-label={`Sort by mode${sortField === 'mode' ? `, ${sortDirection === 'asc' ? 'ascending' : sortDirection === 'desc' ? 'descending' : 'not sorted'}` : ''}`}
                  >
                    <Flex align="center" gap="1">
                      Mode
                      {renderSortIcon('mode')}
                    </Flex>
                  </button>
                </th>
                <th className="routing-rules-list__th routing-rules-list__th--sortable routing-rules-list__th--repos">
                  <button
                    type="button"
                    onClick={() => handleSort('assignedRepositoryCount')}
                    className="routing-rules-list__sort-button"
                    aria-label={`Sort by repositories${sortField === 'assignedRepositoryCount' ? `, ${sortDirection === 'asc' ? 'ascending' : sortDirection === 'desc' ? 'descending' : 'not sorted'}` : ''}`}
                  >
                    <Flex align="center" gap="1">
                      Repositories
                      {renderSortIcon('assignedRepositoryCount')}
                    </Flex>
                  </button>
                </th>
                <th className="routing-rules-list__th routing-rules-list__th--action"></th>
              </tr>
            </thead>
            <tbody>
              {sortedRules.map((rule) => (
                <tr
                  key={rule.name}
                  data-testid={`rule-row-${rule.name}`}
                  onClick={() => handleRowClick(rule)}
                  className="routing-rules-list__row"
                >
                  <td className="routing-rules-list__td routing-rules-list__td--name">
                    {rule.name}
                  </td>
                  <td className="routing-rules-list__td routing-rules-list__td--description">
                    {rule.description || '—'}
                  </td>
                  <td className="routing-rules-list__td routing-rules-list__td--mode">
                    <Flex align="center" gap="1">
                      {renderModeIcon(rule.mode)}
                      {ROUTING_MODE_LABELS[rule.mode]}
                    </Flex>
                  </td>
                  <td className="routing-rules-list__td routing-rules-list__td--repos">
                    {rule.assignedRepositoryCount ?? 0}
                  </td>
                  <td className="routing-rules-list__td routing-rules-list__td--action">
                    <Pencil size={16} className="routing-rules-list__row-edit-icon" aria-hidden="true" />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Box>
      )}

      {/* Help Section - using shared component */}
      <HelpSection
        title="What are Routing Rules?"
        content="Routing rules allow you to control which requests are allowed or blocked for proxy repositories based on request path patterns. Rules use regular expressions to match request paths."
        docLink={{
          label: 'View Documentation',
          href: 'http://links.sonatype.com/products/nxrm3/docs/routing-rule',
        }}
      />
    </Box>
  );
}

export default RoutingRulesList;
