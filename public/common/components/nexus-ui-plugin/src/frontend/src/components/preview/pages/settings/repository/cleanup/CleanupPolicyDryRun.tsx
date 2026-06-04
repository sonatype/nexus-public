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
import { Box, Flex, Text, Tooltip } from '@radix-ui/themes';
import { Download, HelpCircle } from 'lucide-react';

import { SettingsSelect, SettingsButton, SettingsAlert } from '../../../../shared/form';
import { useCleanupPoliciesApi } from './useCleanupPoliciesApi';
import { CleanupPolicyFormData, RepositoryOption } from './types';

import './CleanupPolicyDryRun.scss';

interface CleanupPolicyDryRunProps {
  policyData: CleanupPolicyFormData;
  policyName?: string;
}

/**
 * CleanupPolicyDryRun - Generate CSV report of components that would be deleted
 */
export function CleanupPolicyDryRun({ policyData, policyName }: CleanupPolicyDryRunProps) {
  const [repositories, setRepositories] = useState<RepositoryOption[]>([]);
  const [selectedRepository, setSelectedRepository] = useState('');
  const [isLoadingRepos, setIsLoadingRepos] = useState(false);
  const [repoError, setRepoError] = useState<string | null>(null);

  const { fetchRepositories, getDryRunCsvUrl } = useCleanupPoliciesApi();

  // Load repositories when format changes
  useEffect(() => {
    if (policyData.format) {
      setIsLoadingRepos(true);
      setRepoError(null);
      setSelectedRepository('');

      fetchRepositories(policyData.format)
        .then(setRepositories)
        .catch((err) => setRepoError(err.message))
        .finally(() => setIsLoadingRepos(false));
    }
  }, [policyData.format, fetchRepositories]);

  // Check if at least one criteria is selected
  const hasCriteria = useMemo(() => {
    return (
      !!policyData.criteriaLastBlobUpdated ||
      !!policyData.criteriaLastDownloaded ||
      !!policyData.criteriaAssetRegex
    );
  }, [
    policyData.criteriaLastBlobUpdated,
    policyData.criteriaLastDownloaded,
    policyData.criteriaAssetRegex,
  ]);

  // Check if download is available
  const isDownloadAvailable = useMemo(() => {
    return !!selectedRepository && hasCriteria;
  }, [selectedRepository, hasCriteria]);

  // Generate download URL
  const downloadUrl = useMemo(() => {
    if (!isDownloadAvailable) return '';
    return getDryRunCsvUrl(selectedRepository, policyData, policyName);
  }, [isDownloadAvailable, selectedRepository, policyData, policyName, getDryRunCsvUrl]);

  const tooltipMessage = useMemo(() => {
    if (!selectedRepository && !hasCriteria) {
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
    </Box>
  );
}

export default CleanupPolicyDryRun;

