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
import { Box, Flex, Text, Table, TextField, Card, Callout } from '@radix-ui/themes';
import { Search, ChevronUp, ChevronDown, AlertTriangle } from 'lucide-react';

import { SettingsSelect, SettingsButton, SettingsAlert } from '../../../shared/form';
import { useCleanupPoliciesApi } from './useCleanupPoliciesApi';
import { CleanupPolicyFormData, RepositoryOption, PreviewComponent } from './types';

import './CleanupPolicyPreview.scss';

interface CleanupPolicyPreviewProps {
  policyData: CleanupPolicyFormData;
}

type SortField = 'name' | 'group' | 'version';
type SortDirection = 'asc' | 'desc';

/**
 * CleanupPolicyPreview - Preview components that would be deleted by a cleanup policy
 */
export function CleanupPolicyPreview({ policyData }: CleanupPolicyPreviewProps) {
  const [repositories, setRepositories] = useState<RepositoryOption[]>([]);
  const [selectedRepository, setSelectedRepository] = useState('');
  const [previewResults, setPreviewResults] = useState<PreviewComponent[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [filter, setFilter] = useState('');
  const [sortField, setSortField] = useState<SortField>('name');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [isLoadingRepos, setIsLoadingRepos] = useState(false);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [repoError, setRepoError] = useState<string | null>(null);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [showWarning, setShowWarning] = useState(false);

  const { fetchRepositories, previewCleanupPolicy } = useCleanupPoliciesApi();

  // Load repositories when format changes
  useEffect(() => {
    if (policyData.format) {
      setIsLoadingRepos(true);
      setRepoError(null);
      setSelectedRepository('');
      setPreviewResults([]);
      setTotalCount(0);

      fetchRepositories(policyData.format)
        .then(setRepositories)
        .catch((err) => setRepoError(err.message))
        .finally(() => setIsLoadingRepos(false));
    }
  }, [policyData.format, fetchRepositories]);

  // Check if preview is available
  const isPreviewAvailable = useMemo(() => {
    return (
      !!selectedRepository &&
      (!!policyData.criteriaLastBlobUpdated ||
        !!policyData.criteriaLastDownloaded ||
        !!policyData.criteriaReleaseType ||
        !!policyData.criteriaAssetRegex)
    );
  }, [
    selectedRepository,
    policyData.criteriaLastBlobUpdated,
    policyData.criteriaLastDownloaded,
    policyData.criteriaReleaseType,
    policyData.criteriaAssetRegex,
  ]);

  // Handle preview
  const handlePreview = useCallback(async () => {
    if (!isPreviewAvailable) return;

    // Validate regex pattern before sending request
    if (policyData.criteriaAssetRegex) {
      try {
        new RegExp(policyData.criteriaAssetRegex);
      } catch {
        setPreviewError('Invalid regular expression pattern. Use regex syntax (e.g., ".*\\.jar" instead of "*.jar")');
        return;
      }
    }

    setIsLoadingPreview(true);
    setPreviewError(null);

    try {
      const result = await previewCleanupPolicy(selectedRepository, policyData);
      setPreviewResults(result.components);
      setTotalCount(result.total);
      setShowWarning(result.components.length > 0);
    } catch (err: any) {
      setPreviewError(err.message);
    } finally {
      setIsLoadingPreview(false);
    }
  }, [isPreviewAvailable, selectedRepository, policyData, previewCleanupPolicy]);

  // Filter and sort results
  const filteredResults = useMemo(() => {
    let result = previewResults;

    // Apply filter
    if (filter) {
      const lowerFilter = filter.toLowerCase();
      result = result.filter(
        (item) =>
          item.name.toLowerCase().includes(lowerFilter) ||
          (item.group && item.group.toLowerCase().includes(lowerFilter)) ||
          (item.version && item.version.toLowerCase().includes(lowerFilter))
      );
    }

    // Apply sort
    if (sortField && sortDirection) {
      result = [...result].sort((a, b) => {
        const aVal = (a[sortField] || '').toLowerCase();
        const bVal = (b[sortField] || '').toLowerCase();
        const cmp = aVal.localeCompare(bVal);
        return sortDirection === 'asc' ? cmp : -cmp;
      });
    }

    return result;
  }, [previewResults, filter, sortField, sortDirection]);

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

  const getSortIcon = (field: SortField) => {
    if (sortField !== field) return null;
    return sortDirection === 'asc' ? (
      <ChevronUp size={14} className="sort-icon" />
    ) : (
      <ChevronDown size={14} className="sort-icon" />
    );
  };

  return (
    <Card className="cleanup-policy-preview">
      <Text as="h3" size="4" weight="medium" className="cleanup-policy-preview__title">
        Cleanup policy preview
      </Text>

      {/* Repository Selection */}
      <Box className="cleanup-policy-preview__controls">
        <SettingsSelect
          name="previewRepository"
          label="Preview Repository"
          helpText="Select a repository to preview what might get cleaned up if this policy was applied"
          value={selectedRepository}
          onChange={setSelectedRepository}
          disabled={isLoadingRepos || !policyData.format}
          placeholder="Select a repository..."
          options={repositories.map((repo) => ({
            value: repo.id,
            label: repo.name,
          }))}
        />

        <SettingsButton
          variant="secondary"
          onClick={handlePreview}
          disabled={!isPreviewAvailable || isLoadingPreview}
          className="cleanup-policy-preview__button"
        >
          {isLoadingPreview ? 'Loading...' : 'Preview'}
        </SettingsButton>
      </Box>

      {repoError && (
        <SettingsAlert type="error" className="cleanup-policy-preview__error">
          {repoError}
        </SettingsAlert>
      )}

      {previewError && (
        <SettingsAlert type="error" className="cleanup-policy-preview__error">
          {previewError}
        </SettingsAlert>
      )}

      {/* Warning Message */}
      {showWarning && (
        <Callout.Root color="amber" className="cleanup-policy-preview__warning">
          <Callout.Icon>
            <AlertTriangle size={16} />
          </Callout.Icon>
          <Callout.Text>
            <Text size="2">
              Results may only be a sample of what will be deleted using the current criteria.
            </Text>
            <Text size="2" className="cleanup-policy-preview__count">
              {totalCount === 0
                ? 'Criteria matched no components.'
                : totalCount > 0
                ? `Component count (matching criteria) viewing ${filteredResults.length} out of ${totalCount}.`
                : `Component count (matching criteria) viewing first ${filteredResults.length}`}
            </Text>
          </Callout.Text>
        </Callout.Root>
      )}

      {/* Search/Filter */}
      <Box className="cleanup-policy-preview__toolbar">
        <TextField.Root
          placeholder="Filter results..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="cleanup-policy-preview__search"
        >
          <TextField.Slot>
            <Search size={16} />
          </TextField.Slot>
        </TextField.Root>
      </Box>

      {/* Results Table */}
      <Table.Root className="cleanup-policy-preview__table">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell
              className="cleanup-policy-preview__sortable-header"
              onClick={() => handleSort('name')}
            >
              <Flex align="center" gap="1">
                Name {getSortIcon('name')}
              </Flex>
            </Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell
              className="cleanup-policy-preview__sortable-header"
              onClick={() => handleSort('group')}
            >
              <Flex align="center" gap="1">
                Group {getSortIcon('group')}
              </Flex>
            </Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell
              className="cleanup-policy-preview__sortable-header"
              onClick={() => handleSort('version')}
            >
              <Flex align="center" gap="1">
                Version {getSortIcon('version')}
              </Flex>
            </Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {isLoadingPreview ? (
            <Table.Row>
              <Table.Cell colSpan={3}>
                <Flex align="center" justify="center" py="4">
                  <Text size="2" color="gray">
                    Loading preview...
                  </Text>
                </Flex>
              </Table.Cell>
            </Table.Row>
          ) : filteredResults.length === 0 ? (
            <Table.Row>
              <Table.Cell colSpan={3}>
                <Flex align="center" justify="center" py="4">
                  <Text size="2" color="gray">
                    {previewResults.length === 0
                      ? 'No assets in repository matched the criteria'
                      : 'No results match your filter'}
                  </Text>
                </Flex>
              </Table.Cell>
            </Table.Row>
          ) : (
            filteredResults.map((item, index) => (
              <Table.Row key={`${item.name}-${item.group}-${item.version}-${index}`}>
                <Table.Cell>{item.name}</Table.Cell>
                <Table.Cell>{item.group || '—'}</Table.Cell>
                <Table.Cell>{item.version || '—'}</Table.Cell>
              </Table.Row>
            ))
          )}
        </Table.Body>
      </Table.Root>
    </Card>
  );
}

export default CleanupPolicyPreview;

