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

import React, { useState, useEffect, useMemo, } from 'react';
import { Box, Flex, Text, Tooltip, Table, Callout } from '@radix-ui/themes';
import { Download, AlertTriangle, Loader2 } from 'lucide-react';

import { SettingsSelect, SettingsButton, SettingsAlert } from '../../../../shared/form';
import { useCleanupPoliciesApi } from './useCleanupPoliciesApi';
import {
  CleanupPolicyFormData,
  RepositoryOption,
  PreviewComponent,
  isRepositoriesFieldSupportedFormat,
} from './types';

import './CleanupPolicyDryRun.scss';

interface CleanupPolicyDryRunProps {
  policyData: CleanupPolicyFormData;
  policyName?: string;
  selectedRepositories?: string[];
}

/**
 * CleanupPolicyDryRun - Generate CSV report of components that would be deleted
 */
export function CleanupPolicyDryRun({ policyData, policyName, selectedRepositories = [] }: CleanupPolicyDryRunProps) {
  const [repositories, setRepositories] = useState<RepositoryOption[]>([]);
  const [selectedRepository, setSelectedRepository] = useState('');
  const [isLoadingRepos, setIsLoadingRepos] = useState(false);
  const [repoError, setRepoError] = useState<string | null>(null);
  const [previewResults, setPreviewResults] = useState<PreviewComponent[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [previewError, setPreviewError] = useState<string | null>(null);

  const { fetchRepositories, getDryRunCsvUrl, previewCleanupPolicy } = useCleanupPoliciesApi();

  // Serialize selected repos for stable dependency comparison
  const _selectedReposKey = selectedRepositories.join(',');

  // Load repositories — restrict the dropdown to the Applied Repositories selected
  // in the dual-list selector above. For formats that don't support attaching the
  // policy to specific repositories, fall back to fetching all repos for the format.
  useEffect(() => {
    if (!policyData.format) return;

    if (selectedRepositories.length > 0) {
      setRepositories(selectedRepositories.map((name) => ({ id: name, name })));
      setSelectedRepository('');
      setRepoError(null);
      setIsLoadingRepos(false);
    } else if (isRepositoriesFieldSupportedFormat(policyData.format)) {
      // Format supports per-repository application but none are applied yet —
      // show an empty dropdown instead of listing every repo.
      setRepositories([]);
      setSelectedRepository('');
      setRepoError(null);
      setIsLoadingRepos(false);
    } else {
      setIsLoadingRepos(true);
      setRepoError(null);
      setSelectedRepository('');

      fetchRepositories(policyData.format)
        .then(setRepositories)
        .catch((err) => setRepoError(err.message))
        .finally(() => setIsLoadingRepos(false));
    }
  }, [policyData.format, fetchRepositories, _selectedReposKey]);

  // Check if at least one criteria is selected
  const hasCriteria = useMemo(() => {
    return (
      Boolean(policyData.criteriaLastBlobUpdated) ||
      Boolean(policyData.criteriaLastDownloaded) ||
      Boolean(policyData.criteriaAssetRegex)
    );
  }, [
    policyData.criteriaLastBlobUpdated,
    policyData.criteriaLastDownloaded,
    policyData.criteriaAssetRegex,
  ]);

  // Check if download is available
  const isDownloadAvailable = useMemo(() => {
    return Boolean(selectedRepository) && hasCriteria;
  }, [selectedRepository, hasCriteria]);

  // Generate download URL
  const downloadUrl = useMemo(() => {
    if (!isDownloadAvailable) return '';
    return getDryRunCsvUrl(selectedRepository, policyData, policyName);
  }, [isDownloadAvailable, selectedRepository, policyData, policyName, getDryRunCsvUrl]);

  const tooltipMessage = useMemo(() => {
    if (!(selectedRepository || hasCriteria)) {
      return 'Please select a repository and at least one cleanup criterion';
    }
    if (!selectedRepository) {
      return 'Please select a repository';
    }
    if (!hasCriteria) {
      return 'Please select at least one cleanup criterion';
    }
    return '';
  }, [selectedRepository, hasCriteria]);

  // Auto-load preview when repository is selected and criteria exist
  // Use specific policyData fields as dependencies (not the object reference)
  // to ensure re-fetch when individual criteria values change
  useEffect(() => {
    if (selectedRepository && hasCriteria) {
      setIsLoadingPreview(true);
      setPreviewError(null);
      previewCleanupPolicy(selectedRepository, policyData, policyName)
        .then((result) => {
          setPreviewResults(result.components);
          setTotalCount(result.total);
        })
        .catch((err) => {
          setPreviewError(err.message);
          setPreviewResults([]);
          setTotalCount(0);
        })
        .finally(() => setIsLoadingPreview(false));
    } else {
      setPreviewResults([]);
      setTotalCount(0);
    }
  }, [
    selectedRepository,
    hasCriteria,
    policyData.criteriaLastBlobUpdated,
    policyData.criteriaLastDownloaded,
    policyData.criteriaAssetRegex,
    policyData.criteriaReleaseType,
    policyData.retain,
    policyData.sortBy,
    policyName,
    previewCleanupPolicy,
  ]);

  return (
    <Box className="cleanup-policy-dry-run">
      <Text as="p" size="2" className="cleanup-policy-dry-run__description">
        Export a spreadsheet listing which components would be deleted today based on selected
        cleanup policy settings. Your export will list component namespaces, names, versions and
        paths.
      </Text>

      {repoError && (
        <SettingsAlert type="error" className="cleanup-policy-dry-run__error">
          {repoError}
        </SettingsAlert>
      )}

      <Flex gap="3" align="end" className="cleanup-policy-dry-run__controls">
        <Box className="cleanup-policy-dry-run__select">
          <SettingsSelect
            name="dryRunRepository"
            label=""
            value={selectedRepository}
            onChange={setSelectedRepository}
            disabled={isLoadingRepos || !policyData.format}
            placeholder="Select a repository"
            options={repositories.map((repo) => ({
              value: repo.id,
              label: repo.name,
            }))}
          />
        </Box>

        {isDownloadAvailable ? (
          <a
            href={downloadUrl}
            download
            className="cleanup-policy-dry-run__download-link"
          >
            <SettingsButton variant="secondary" icon={Download} data-analytics-id="nxrm-cleanup-policy-preview">
              Generate CSV Report
            </SettingsButton>
          </a>
        ) : (
          <Tooltip content={tooltipMessage}>
            <Box>
              <SettingsButton variant="secondary" disabled icon={Download} data-analytics-id="nxrm-cleanup-policy-preview">
                Generate CSV Report
              </SettingsButton>
            </Box>
          </Tooltip>
        )}
      </Flex>

      {/* Preview Table — shows components that would be deleted */}
      {selectedRepository && hasCriteria && (
        <Box mt="4">
          {previewError && (
            <SettingsAlert type="error">
              {previewError}
            </SettingsAlert>
          )}

          {totalCount > 0 && (
            <Callout.Root color="amber" mb="3">
              <Callout.Icon>
                <AlertTriangle size={16} />
              </Callout.Icon>
              <Callout.Text>
                <Text size="2">
                  Showing {previewResults.length} of {totalCount} components that would be deleted.
                </Text>
              </Callout.Text>
            </Callout.Root>
          )}

          <Table.Root>
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>Name</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Group</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Version</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {isLoadingPreview ? (
                <Table.Row>
                  <Table.Cell colSpan={3}>
                    <Flex align="center" justify="center" py="4" gap="2">
                      <Loader2 size={16} className="cleanup-policy-dry-run__spinner" />
                      <Text size="2" color="gray">Loading preview...</Text>
                    </Flex>
                  </Table.Cell>
                </Table.Row>
              ) : previewResults.length === 0 ? (
                <Table.Row>
                  <Table.Cell colSpan={3}>
                    <Flex align="center" justify="center" py="4">
                      <Text size="2" color="gray">
                        No components match the selected criteria
                      </Text>
                    </Flex>
                  </Table.Cell>
                </Table.Row>
              ) : (
                previewResults.map((item, index) => (
                  <Table.Row key={`${item.name}-${item.version}-${index}`}>
                    <Table.Cell>{item.name}</Table.Cell>
                    <Table.Cell>{item.group || '—'}</Table.Cell>
                    <Table.Cell>{item.version || '—'}</Table.Cell>
                  </Table.Row>
                ))
              )}
            </Table.Body>
          </Table.Root>

          {totalCount === 0 && !isLoadingPreview && !previewError && (
            <Box mt="2">
              <Text size="2" color="gray">
                No assets in this repository match the current criteria.
              </Text>
            </Box>
          )}
        </Box>
      )}
    </Box>
  );
}

export default CleanupPolicyDryRun;

