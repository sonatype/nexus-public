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
import { Box, Flex, Text, Table, TextField } from '@radix-ui/themes';
import { Search } from 'lucide-react';

import { SettingsSelect, SettingsButton, SettingsAlert } from '../../../../shared/form';
import { useContentSelectorsApi } from './useContentSelectorsApi';
import { RepositoryOption } from './types';

import './ContentSelectorPreview.scss';

interface ContentSelectorPreviewProps {
  type: string;
  expression: string;
}

/**
 * ContentSelectorPreview - Preview content that matches a content selector expression
 */
export function ContentSelectorPreview({ type, expression }: ContentSelectorPreviewProps) {
  const [repositories, setRepositories] = useState<RepositoryOption[]>([]);
  const [selectedRepository, setSelectedRepository] = useState('');
  const [previewResults, setPreviewResults] = useState<string[]>([]);
  const [filter, setFilter] = useState('');
  const [isLoadingRepos, setIsLoadingRepos] = useState(false);
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [repoError, setRepoError] = useState<string | null>(null);
  const [previewError, setPreviewError] = useState<string | null>(null);

  const { fetchRepositories, previewContentSelector } = useContentSelectorsApi();

  // Load repositories on mount
  useEffect(() => {
    setIsLoadingRepos(true);
    setRepoError(null);

    fetchRepositories()
      .then((repos) => {
        setRepositories(repos);
        // Select "All Repositories" option by default if available
        if (repos.length > 0) {
          const allReposOption = repos.find((r) => r.id === '*');
          if (allReposOption) {
            setSelectedRepository('*');
          }
        }
      })
      .catch((err) => setRepoError(err.message))
      .finally(() => setIsLoadingRepos(false));
  }, [fetchRepositories]);

  // Check if preview is available
  const isPreviewAvailable = useMemo(() => {
    return !!expression.trim();
  }, [expression]);

  // Handle preview
  const handlePreview = useCallback(async () => {
    if (!isPreviewAvailable) return;

    setIsLoadingPreview(true);
    setPreviewError(null);

    try {
      const results = await previewContentSelector(selectedRepository || '*', type, expression);
      setPreviewResults(results);
    } catch (err: any) {
      setPreviewError(err.message);
      setPreviewResults([]);
    } finally {
      setIsLoadingPreview(false);
    }
  }, [isPreviewAvailable, selectedRepository, type, expression, previewContentSelector]);

  // Filter results
  const filteredResults = useMemo(() => {
    if (!filter) return previewResults;
    const lowerFilter = filter.toLowerCase();
    return previewResults.filter((name) => name.toLowerCase().includes(lowerFilter));
  }, [previewResults, filter]);

  return (
    <Box className="content-selector-preview">
      <Text as="p" size="2" className="content-selector-preview__description">
        Select a repository to evaluate the content selector and see the content that would be
        available.
      </Text>

      {repoError && (
        <SettingsAlert type="error" className="content-selector-preview__error">
          {repoError}
        </SettingsAlert>
      )}

      {previewError && (
        <SettingsAlert type="error" className="content-selector-preview__error">
          {previewError}
        </SettingsAlert>
      )}

      <Flex gap="3" align="end" className="content-selector-preview__controls">
        <Box className="content-selector-preview__select">
          <SettingsSelect
            name="previewRepository"
            label="Preview Repository"
            value={selectedRepository}
            onChange={setSelectedRepository}
            disabled={isLoadingRepos}
            placeholder="Select an option"
            options={repositories.map((repo) => ({
              value: repo.id,
              label: repo.name,
            }))}
          />
        </Box>

        <SettingsButton
          variant="secondary"
          onClick={handlePreview}
          disabled={!isPreviewAvailable || isLoadingPreview}
          data-analytics-id="nxrm-content-selector-preview"
        >
          {isLoadingPreview ? 'Loading...' : 'Preview'}
        </SettingsButton>
      </Flex>

      {/* Search/Filter */}
      <Box className="content-selector-preview__toolbar">
        <TextField.Root
          placeholder="Filter results..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="content-selector-preview__search"
        >
          <TextField.Slot>
            <Search size={16} />
          </TextField.Slot>
        </TextField.Root>
      </Box>

      {/* Results Table */}
      <Table.Root className="content-selector-preview__table">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Name</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {isLoadingPreview ? (
            <Table.Row>
              <Table.Cell>
                <Flex align="center" justify="center" py="4">
                  <Text size="2" color="gray">
                    Loading preview...
                  </Text>
                </Flex>
              </Table.Cell>
            </Table.Row>
          ) : filteredResults.length === 0 ? (
            <Table.Row>
              <Table.Cell>
                <Flex align="center" justify="center" py="4">
                  <Text size="2" color="gray">
                    {previewResults.length === 0
                      ? 'No content in repositories matched the expression'
                      : 'No results match your filter'}
                  </Text>
                </Flex>
              </Table.Cell>
            </Table.Row>
          ) : (
            filteredResults.map((name, index) => (
              <Table.Row key={`${name}-${index}`}>
                <Table.Cell>{name}</Table.Cell>
              </Table.Row>
            ))
          )}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}

export default ContentSelectorPreview;

