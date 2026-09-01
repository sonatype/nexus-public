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

import { renderHook, act } from '@testing-library/react';
import { useHostedRepoEvaluation, DEFAULT_SETTINGS } from '../useHostedRepoEvaluation';
import { restClient } from '../../../../../../../interface/api';

jest.mock('../../../../../../../interface/api');
const mockedRestClient = restClient as jest.Mocked<typeof restClient>;

describe('useHostedRepoEvaluation', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchSettingsWithRepos', () => {
    it('returns settings with artifactLatestVersions on success (200)', async () => {
      mockedRestClient.get.mockResolvedValueOnce({
        artifactLatestVersions: 3,
        activityTimeFrame: 60,
        policyEvaluationStage: 'RELEASE',
        autoEnrollNewRepos: true,
        monitoredRepoCount: 5,
      });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let value: any;
      await act(async () => {
        value = await result.current.fetchSettingsWithRepos();
      });

      expect(mockedRestClient.get).toHaveBeenCalledWith('/service/rest/v1/evaluation/settings');
      expect(value.settings.artifactLatestVersions).toBe(3);
      expect(value.settings.activityTimeFrame).toBe(60);
      expect(value.settings.policyEvaluationStage).toBe('RELEASE');
      expect(value.settings.autoEnrollNewRepos).toBe(true);
      expect(value.monitoredRepoIds).toEqual([]);
      expect(value.totalRepoCount).toBe(5);
    });

    it('returns DEFAULT_SETTINGS on HTTP 204 (no content)', async () => {
      mockedRestClient.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let value: any;
      await act(async () => {
        value = await result.current.fetchSettingsWithRepos();
      });

      expect(value.settings).toEqual(DEFAULT_SETTINGS);
      expect(value.monitoredRepoIds).toEqual([]);
      expect(value.totalRepoCount).toBe(0);
    });

    it('returns DEFAULT_SETTINGS on error (non-fatal)', async () => {
      mockedRestClient.get.mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let value: any;
      await act(async () => {
        value = await result.current.fetchSettingsWithRepos();
      });

      expect(value.settings).toEqual(DEFAULT_SETTINGS);
      expect(value.monitoredRepoIds).toEqual([]);
      expect(value.totalRepoCount).toBe(0);
    });
  });

  describe('fetchRepositories', () => {
    it('parses data.items with backend field names', async () => {
      mockedRestClient.get.mockResolvedValueOnce({
        items: [
          {
            repositoryId: 'repo-1',
            repositoryName: 'my-maven-releases',
            format: 'maven2',
            size: 1024000,
            numberOfComponents: 42,
            isSelected: true,
          },
          {
            repositoryId: 'repo-2',
            repositoryName: 'npm-hosted',
            format: 'npm',
            size: null,
            numberOfComponents: 0,
            isSelected: false,
          },
        ],
        totalRepositories: 2,
      });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let value: any;
      await act(async () => {
        value = await result.current.fetchRepositories({
          page: 1,
          pageSize: 25,
        });
      });

      expect(mockedRestClient.get).toHaveBeenCalledWith('/service/rest/v1/repository-dashboard', {
        params: { page: 1, pageSize: 25 },
      });
      expect(value.rows).toHaveLength(2);
      expect(value.rows[0].id).toBe('repo-1');
      expect(value.rows[0].name).toBe('my-maven-releases');
      expect(value.rows[0].format).toBe('maven2');
      expect(value.rows[0].size).toBe(1024000);
      expect(value.rows[0].componentCount).toBe(42);
      expect(value.rows[0].isMonitored).toBe(true);
      expect(value.rows[1].isMonitored).toBe(false);
      expect(value.totalCount).toBe(2);
    });

    it('applies filters as query params', async () => {
      mockedRestClient.get.mockResolvedValueOnce({ items: [], totalRepositories: 0 });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      await act(async () => {
        await result.current.fetchRepositories({
          page: 2,
          pageSize: 10,
          search: 'maven',
          formatFilter: 'maven2',
          monitoringFilter: 'enabled',
        });
      });

      expect(mockedRestClient.get).toHaveBeenCalledWith('/service/rest/v1/repository-dashboard', {
        params: {
          page: 2,
          pageSize: 10,
          search: 'maven',
          format: 'maven2',
          monitoring: 'enabled',
        },
      });
    });

    it('omits "all" filter values from query params', async () => {
      mockedRestClient.get.mockResolvedValueOnce({ items: [], totalRepositories: 0 });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      await act(async () => {
        await result.current.fetchRepositories({
          page: 1,
          pageSize: 25,
          formatFilter: 'all',
          monitoringFilter: 'all',
        });
      });

      expect(mockedRestClient.get).toHaveBeenCalledWith('/service/rest/v1/repository-dashboard', {
        params: { page: 1, pageSize: 25 },
      });
    });

    it('returns empty rows on error', async () => {
      mockedRestClient.get.mockRejectedValueOnce({ response: { data: { message: 'Server down' } } });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let value: any;
      await act(async () => {
        value = await result.current.fetchRepositories({ page: 1, pageSize: 25 });
      });

      expect(value.rows).toEqual([]);
      expect(value.totalCount).toBe(0);
      expect(result.current.error).toBe('Server down');
    });

    it('uses pagination.totalItems as the filtered total when present', async () => {
      mockedRestClient.get.mockResolvedValueOnce({
        items: [
          { repositoryId: 'r1', repositoryName: 'helm-a', format: 'helm' },
          { repositoryId: 'r2', repositoryName: 'helm-b', format: 'helm' },
        ],
        pagination: { totalItems: 2 },
        totalRepositories: 53, // unfiltered global — must not override
      });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let value: any;
      await act(async () => {
        value = await result.current.fetchRepositories({
          page: 1,
          pageSize: 25,
          search: 'helm',
        });
      });

      expect(value.totalCount).toBe(2);
    });

    it('falls back to totalRepositories when pagination is absent', async () => {
      mockedRestClient.get.mockResolvedValueOnce({
        items: new Array(25).fill(0).map((_, i) => ({ repositoryId: `r${i}`, repositoryName: `r${i}` })),
        totalRepositories: 53,
      });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let value: any;
      await act(async () => {
        value = await result.current.fetchRepositories({ page: 1, pageSize: 25 });
      });

      expect(value.totalCount).toBe(53);
    });
  });

  describe('saveSettings', () => {
    it('PATCHes /settings with field-only payload (no repo enumeration)', async () => {
      mockedRestClient.patch.mockResolvedValueOnce({ success: true, message: 'Saved' });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let saveResult: any;
      await act(async () => {
        saveResult = await result.current.saveSettings({
          activityTimeFrame: 30,
          artifactLatestVersions: 3,
          policyEvaluationStage: 'BUILD',
          autoEnrollNewRepos: false,
        });
      });

      // No repository-dashboard call before save.
      expect(mockedRestClient.get).not.toHaveBeenCalled();
      // PUT no longer used for settings save.
      expect(mockedRestClient.put).not.toHaveBeenCalled();

      expect(mockedRestClient.patch).toHaveBeenCalledWith('/service/rest/v1/evaluation/settings', {
        activityTimeFrame: 30,
        artifactLatestVersions: 3,
        policyEvaluationStage: 'BUILD',
        autoEnrollNewRepos: false,
      });

      expect(saveResult.ok).toBe(true);
      expect(saveResult.message).toBe('Saved');
    });

    it('returns ok:false with message on HTTP 200 with success:false', async () => {
      mockedRestClient.patch.mockResolvedValueOnce({
        success: false,
        message: 'Policy violation detected',
      });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let saveResult: any;
      await act(async () => {
        saveResult = await result.current.saveSettings(DEFAULT_SETTINGS);
      });

      expect(saveResult.ok).toBe(false);
      expect(saveResult.message).toBe('Policy violation detected');
    });

    it('returns ok:false with message on HTTP error', async () => {
      mockedRestClient.patch.mockRejectedValueOnce({
        response: { data: { message: 'Validation failed' } },
      });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let saveResult: any;
      await act(async () => {
        saveResult = await result.current.saveSettings(DEFAULT_SETTINGS);
      });

      expect(saveResult.ok).toBe(false);
      expect(saveResult.message).toBe('Validation failed');
    });
  });

  describe('applySelectionDelta', () => {
    it('PATCHes /evaluation/repositories with addRepositoryIds', async () => {
      mockedRestClient.patch.mockResolvedValueOnce({
        success: true,
        repositoriesAdded: 3,
      });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let patchResult: any;
      await act(async () => {
        patchResult = await result.current.applySelectionDelta({
          addRepositoryIds: ['r1', 'r2', 'r3'],
        });
      });

      expect(mockedRestClient.patch).toHaveBeenCalledWith(
        '/service/rest/v1/evaluation/repositories',
        { addRepositoryIds: ['r1', 'r2', 'r3'] }
      );
      expect(patchResult.ok).toBe(true);
    });

    it('PATCHes /evaluation/repositories with removeRepositoryIds', async () => {
      mockedRestClient.patch.mockResolvedValueOnce({
        success: true,
        repositoriesRemoved: 2,
      });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let patchResult: any;
      await act(async () => {
        patchResult = await result.current.applySelectionDelta({
          removeRepositoryIds: ['r4', 'r5'],
        });
      });

      expect(mockedRestClient.patch).toHaveBeenCalledWith(
        '/service/rest/v1/evaluation/repositories',
        { removeRepositoryIds: ['r4', 'r5'] }
      );
      expect(patchResult.ok).toBe(true);
    });

    it('returns ok:false when response has success:false even on HTTP 200', async () => {
      mockedRestClient.patch.mockResolvedValueOnce({
        success: false,
        errorCode: 'REPOSITORY_IN_USE',
      });

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let patchResult: any;
      await act(async () => {
        patchResult = await result.current.applySelectionDelta({ addRepositoryIds: ['r1'] });
      });

      expect(patchResult.ok).toBe(false);
      expect(patchResult.message).toBe('REPOSITORY_IN_USE');
    });

    it('returns ok:false with error message on network error', async () => {
      mockedRestClient.patch.mockRejectedValueOnce(new Error('Network timeout'));

      const { result } = renderHook(() => useHostedRepoEvaluation());
      let patchResult: any;
      await act(async () => {
        patchResult = await result.current.applySelectionDelta({ addRepositoryIds: ['r1'] });
      });

      expect(patchResult.ok).toBe(false);
      expect(patchResult.message).toBe('Network timeout');
    });
  });
});
