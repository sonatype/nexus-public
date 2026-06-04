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

import React, { useState, useCallback, useMemo } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import {
  Search,
  AlertCircle,
  ChevronRight,
  ChevronDown,
  Check,
  Ban,
  FolderTree,
  Database,
  Info,
  ExternalLink,
} from 'lucide-react';

import { SettingsButton, SettingsSelect } from '../../../../shared/form';
import { useRoutingRulesApi } from './useRoutingRulesApi';
import {
  RoutingRulePreviewProps,
  RepositoryPreviewItem,
  RoutingRulesPreview as RoutingRulesPreviewData,
  PreviewFilter,
  RoutingRule,
} from './types';
import { RoutingRuleMatchModal } from './RoutingRuleMatchModal';

import './RoutingRulePreview.scss';

/**
 * Tree node component for repository preview
 */
interface TreeNodeProps {
  item: RepositoryPreviewItem;
  level: number;
  onRuleClick: (ruleName: string) => void;
}

function TreeNode({ item, level, onRuleClick }: TreeNodeProps) {
  const [expanded, setExpanded] = useState(item.expanded);
  const hasChildren = item.children && item.children.length > 0;

  const handleToggle = useCallback(() => {
    if (hasChildren) {
      setExpanded(!expanded);
    }
  }, [hasChildren, expanded]);

  const handleRuleClick = useCallback((e: React.MouseEvent | React.KeyboardEvent) => {
    e.stopPropagation();
    if (item.rule && onRuleClick) {
      onRuleClick(item.rule);
    }
  }, [item.rule, onRuleClick]);

  const handleRuleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleRuleClick(e);
    }
  }, [handleRuleClick]);

  const getTypeIcon = () => {
    if (item.type === 'group') {
      return <FolderTree size={16} className="routing-rule-preview__type-icon" />;
    }
    return <Database size={16} className="routing-rule-preview__type-icon" />;
  };

  const getStatusIcon = () => {
    if (item.allowed) {
      return <Check size={14} className="routing-rule-preview__status-icon routing-rule-preview__status-icon--allowed" />;
    }
    return <Ban size={14} className="routing-rule-preview__status-icon routing-rule-preview__status-icon--blocked" />;
  };

  return (
    <Box className="routing-rule-preview__node">
      <Flex
        align="center"
        gap="2"
        className={`routing-rule-preview__row routing-rule-preview__row--level-${Math.min(level, 3)}`}
        style={{ paddingLeft: `${level * 24 + 12}px` }}
        onClick={handleToggle}
      >
        {/* Expand/Collapse icon */}
        <Box className="routing-rule-preview__expand">
          {hasChildren ? (
            expanded ? (
              <ChevronDown size={16} className="routing-rule-preview__chevron" />
            ) : (
              <ChevronRight size={16} className="routing-rule-preview__chevron" />
            )
          ) : (
            <Box className="routing-rule-preview__chevron-placeholder" />
          )}
        </Box>

        {/* Type icon */}
        {getTypeIcon()}

        {/* Repository name */}
        <Text size="2" className="routing-rule-preview__name">
          {item.repository}
        </Text>

        {/* Format badge */}
        <Text size="1" className="routing-rule-preview__format">
          {item.format}
        </Text>

        {/* Rule name - clickable if rule exists */}
        {item.rule && (
          <Text
            size="1"
            className="routing-rule-preview__rule routing-rule-preview__rule--clickable"
            onClick={handleRuleClick}
            onKeyDown={handleRuleKeyDown}
            role="button"
            tabIndex={0}
            aria-label={`View routing rule ${item.rule}`}
            title="Click to view routing rule details"
          >
            {item.rule}
          </Text>
        )}

        {/* Status */}
        <Box className="routing-rule-preview__status">
          {getStatusIcon()}
          <Text size="1" className={item.allowed ? 'routing-rule-preview__status-text--allowed' : 'routing-rule-preview__status-text--blocked'}>
            {item.allowed ? 'Allowed' : 'Blocked'}
          </Text>
        </Box>
      </Flex>

      {/* Children */}
      {expanded && hasChildren && (
        <Box className="routing-rule-preview__children">
          {item.children!.map((child, index) => (
            <TreeNode key={`${child.repository}-${index}`} item={child} level={level + 1} onRuleClick={onRuleClick} />
          ))}
        </Box>
      )}
    </Box>
  );
}

/**
 * RoutingRulePreview - Global preview for testing paths against all routing rules
 */
export function RoutingRulePreview({ onClose }: RoutingRulePreviewProps) {
  const [path, setPath] = useState('');
  const [filter, setFilter] = useState<PreviewFilter>('all');
  const [previewData, setPreviewData] = useState<RoutingRulesPreviewData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedRule, setSelectedRule] = useState<RoutingRule | null>(null);

  const { fetchRoutingRulesPreview, fetchRoutingRule } = useRoutingRulesApi();

  const handleRuleClick = useCallback((ruleName: string) => {
    fetchRoutingRule(ruleName)
      .then((rule) => {
        if (rule) {
          setSelectedRule(rule);
        } else {
          setError('Routing rule not found');
        }
      })
      .catch((err) => {
        setError(err.message || 'Failed to load routing rule details');
      });
  }, [fetchRoutingRule, setError]);

  const handleCloseModal = useCallback(() => {
    setSelectedRule(null);
  }, []);

  const handleSearch = useCallback(async () => {
    if (!path.trim()) {
      setError('Please enter a path to test');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const data = await fetchRoutingRulesPreview(path.trim(), filter);
      setPreviewData(data);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch preview');
      setPreviewData(null);
    } finally {
      setLoading(false);
    }
  }, [path, filter, fetchRoutingRulesPreview]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  }, [handleSearch]);

  const filterOptions = [
    { value: 'all', label: 'All Repositories' },
    { value: 'groups', label: 'Group Repositories' },
    { value: 'proxies', label: 'Proxy Repositories' },
  ];

  // Summary stats
  const stats = useMemo(() => {
    if (!previewData?.children) return null;

    let allowed = 0;
    let blocked = 0;

    const countItems = (items: RepositoryPreviewItem[]) => {
      for (const item of items) {
        if (item.allowed) allowed++;
        else blocked++;
        if (item.children) {
          countItems(item.children);
        }
      }
    };

    countItems(previewData.children);
    return { allowed, blocked, total: allowed + blocked };
  }, [previewData]);

  return (
    <Box className="routing-rule-preview">
      <Text size="2" className="routing-rule-preview__description">
        Test how routing rules will affect requests to a specific path across all repositories.
      </Text>

      {/* Search Section */}
      <Box className="routing-rule-preview__search-section">
        <Box className="routing-rule-preview__search-controls">
          <Box className="routing-rule-preview__path-input-wrapper">
            <Text as="label" size="2" weight="medium" className="routing-rule-preview__label">
              Test Path
            </Text>
            <Box className="routing-rule-preview__path-input-container">
              <Search size={16} className="routing-rule-preview__search-icon" />
              <input
                type="text"
                value={path}
                onChange={(e) => setPath(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="e.g., /com/example/artifact-1.0.jar"
                className="routing-rule-preview__path-input"
              />
            </Box>
          </Box>

          <Box className="routing-rule-preview__filter-wrapper">
            <SettingsSelect
              name="filter"
              label="Filter"
              value={filter}
              onChange={(value) => setFilter(value as PreviewFilter)}
              options={filterOptions}
            />
          </Box>

          <SettingsButton
            variant="primary"
            onClick={handleSearch}
            disabled={loading || !path.trim()}
            loading={loading}
            className="routing-rule-preview__test-button"
          >
            {loading ? 'Testing...' : 'Test'}
          </SettingsButton>
        </Box>
      </Box>

      {/* Error */}
      {error && (
        <Flex align="center" gap="2" className="routing-rule-preview__error">
          <AlertCircle size={16} />
          <Text size="2">{error}</Text>
        </Flex>
      )}

      {/* Results */}
      {previewData && !loading && (
        <>
          {/* Summary */}
          {stats && (
            <Flex gap="4" className="routing-rule-preview__summary">
              <Box className="routing-rule-preview__stat">
                <Text size="1" className="routing-rule-preview__stat-label">Total</Text>
                <Text size="4" weight="medium">{stats.total}</Text>
              </Box>
              <Box className="routing-rule-preview__stat routing-rule-preview__stat--allowed">
                <Text size="1" className="routing-rule-preview__stat-label">Allowed</Text>
                <Text size="4" weight="medium">{stats.allowed}</Text>
              </Box>
              <Box className="routing-rule-preview__stat routing-rule-preview__stat--blocked">
                <Text size="1" className="routing-rule-preview__stat-label">Blocked</Text>
                <Text size="4" weight="medium">{stats.blocked}</Text>
              </Box>
            </Flex>
          )}

          {/* Tree */}
          {previewData.children.length > 0 ? (
            <Box className="routing-rule-preview__tree">
              {previewData.children.map((item, index) => (
                <TreeNode key={`${item.repository}-${index}`} item={item} level={0} onRuleClick={handleRuleClick} />
              ))}
            </Box>
          ) : (
            <Box className="routing-rule-preview__empty">
              <Text size="2">No repositories found matching the filter criteria</Text>
            </Box>
          )}
        </>
      )}

      {/* Initial state / help */}
      {!previewData && !loading && !error && (
        <Box className="routing-rule-preview__help">
          <Flex align="center" gap="2" className="routing-rule-preview__help-header">
            <Info size={16} />
            <Text size="2" weight="medium">How to Use</Text>
          </Flex>
          <Text size="2" className="routing-rule-preview__help-text">
            Enter a request path (e.g., <code>/com/example/artifact-1.0.jar</code>) to see how
            routing rules will affect requests across all repositories. The preview shows which
            repositories would allow or block the request based on their assigned routing rules.
            {' '}
            <a
              href="http://links.sonatype.com/products/nxrm3/docs/routing-rule"
              target="_blank"
              rel="noopener noreferrer"
              className="routing-rule-preview__help-link"
            >
              Learn more
              <ExternalLink size={12} />
            </a>
          </Text>
        </Box>
      )}

      {/* Modal for rule details */}
      <RoutingRuleMatchModal
        isOpen={!!selectedRule}
        onClose={handleCloseModal}
        rule={selectedRule}
        path={path}
      />
    </Box>
  );
}

export default RoutingRulePreview;
