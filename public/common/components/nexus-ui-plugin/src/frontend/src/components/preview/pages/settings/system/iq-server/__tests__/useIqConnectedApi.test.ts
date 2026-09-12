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

import { restClient } from '../../../../../../../interface/api';
import { renderHook, act } from '@testing-library/react';
import { useIqConnectedApi } from '../useIqConnectedApi';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn(), post: jest.fn(), put: jest.fn(), patch: jest.fn(), delete: jest.fn() },
}));
const mockedAxios = restClient as jest.Mocked<typeof restClient>;

describe('useIqConnectedApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchIq', () => {
    it('returns IQ configuration on success', async () => {
      const payload = {
        enabled: true,
        showLink: true,
        url: 'http://localhost:8070',
        authenticationType: 'USER',
        username: 'admin',
        password: '',
        useTrustStoreForUrl: false,
        timeoutSeconds: null,
        properties: '',
        failOpenModeEnabled: false,
        licensedSolutions: [
          { id: 'lifecycle', url: 'http://localhost:8070/ui/links/lifecycle/dashboard' },
        ],
        hasFirewall: false,
      };
      mockedAxios.get.mockResolvedValueOnce(payload);

      const { result } = renderHook(() => useIqConnectedApi());
      let value: any;
      await act(async () => {
        value = await result.current.fetchIq();
      });

      expect(mockedAxios.get).toHaveBeenCalledWith('service/rest/v1/iq');
      expect(value).toEqual(payload);
    });

    it('throws an Error with the server message on failure', async () => {
      mockedAxios.get.mockRejectedValueOnce({
        response: { data: { message: 'IQ Server is misconfigured' } },
      });

      const { result } = renderHook(() => useIqConnectedApi());
      await expect(result.current.fetchIq()).rejects.toThrow('IQ Server is misconfigured');
    });

    it('throws an Error with a default message when no message is available', async () => {
      mockedAxios.get.mockRejectedValueOnce({});
      const { result } = renderHook(() => useIqConnectedApi());
      await expect(result.current.fetchIq()).rejects.toThrow('Failed to load IQ Server configuration');
    });
  });

  describe('verifyConnection', () => {
    it('returns success:true when the server confirms', async () => {
      mockedAxios.post.mockResolvedValueOnce({ success: true, applicationCount: 12 });
      const { result } = renderHook(() => useIqConnectedApi());

      let value: any;
      await act(async () => {
        value = await result.current.verifyConnection();
      });

      expect(mockedAxios.post).toHaveBeenCalledWith('service/rest/v1/iq/verify-connection');
      expect(value).toEqual({ success: true, reason: undefined, applicationCount: 12 });
    });

    it('returns success:false with reason on caught error (does NOT throw)', async () => {
      mockedAxios.post.mockRejectedValueOnce({
        response: { data: { reason: 'Unsupported or unrecognized SSL message' } },
      });
      const { result } = renderHook(() => useIqConnectedApi());

      let value: any;
      await act(async () => {
        value = await result.current.verifyConnection();
      });

      expect(value).toEqual({
        success: false,
        reason: 'Unsupported or unrecognized SSL message',
      });
    });

    it('falls back to a generic reason on plain network error', async () => {
      mockedAxios.post.mockRejectedValueOnce(new Error('Network Error'));
      const { result } = renderHook(() => useIqConnectedApi());

      let value: any;
      await act(async () => {
        value = await result.current.verifyConnection();
      });

      expect(value.success).toBe(false);
      expect(typeof value.reason).toBe('string');
      expect(value.reason).toBeTruthy();
    });
  });

  describe('fetchDashboardSummary', () => {
    it('returns counts on success', async () => {
      mockedAxios.get.mockResolvedValueOnce({
        numberOfMonitoredRepositories: 54,
        totalRepositories: 70,
        globalConfigAvailable: true,
        hasSelections: true,
      });

      const { result } = renderHook(() => useIqConnectedApi());
      let value: any;
      await act(async () => {
        value = await result.current.fetchDashboardSummary();
      });

      expect(mockedAxios.get).toHaveBeenCalledWith(
        'service/rest/v1/repository-dashboard',
        { params: { page: 1, pageSize: 1 } }
      );
      expect(value.numberOfMonitoredRepositories).toBe(54);
      expect(value.totalRepositories).toBe(70);
    });

    it('returns zero counts when the endpoint errors (non-fatal)', async () => {
      mockedAxios.get.mockRejectedValueOnce(new Error('500'));
      const { result } = renderHook(() => useIqConnectedApi());

      let value: any;
      await act(async () => {
        value = await result.current.fetchDashboardSummary();
      });

      expect(value).toEqual({
        numberOfMonitoredRepositories: 0,
        totalRepositories: 0,
        globalConfigAvailable: false,
        hasSelections: false,
      });
    });
  });

  describe('fetchEvaluationSettings', () => {
    it('returns settings on success', async () => {
      mockedAxios.get.mockResolvedValueOnce({
        activityTimeFrame: 60,
        artifactLatestVersions: 5,
        policyEvaluationStage: 'RELEASE',
        monitoredRepoCount: 54,
        totalRepoCount: 70,
      });

      const { result } = renderHook(() => useIqConnectedApi());
      let value: any;
      await act(async () => {
        value = await result.current.fetchEvaluationSettings();
      });

      expect(mockedAxios.get).toHaveBeenCalledWith('service/rest/v1/evaluation/settings');
      expect(value).toEqual({
        activityTimeFrame: 60,
        artifactLatestVersions: 5,
        policyEvaluationStage: 'RELEASE',
        monitoredRepoCount: 54,
        totalRepoCount: 70,
      });
    });

    it('returns null on HTTP 204', async () => {
      // restClient surfaces 204 No Content as null
      mockedAxios.get.mockResolvedValueOnce(null);

      const { result } = renderHook(() => useIqConnectedApi());
      let value: any;
      await act(async () => {
        value = await result.current.fetchEvaluationSettings();
      });

      expect(value).toBeNull();
    });

    it('returns null on error (non-fatal)', async () => {
      mockedAxios.get.mockRejectedValueOnce(new Error('Not found'));
      const { result } = renderHook(() => useIqConnectedApi());

      let value: any;
      await act(async () => {
        value = await result.current.fetchEvaluationSettings();
      });

      expect(value).toBeNull();
    });
  });

  it('each operation is a stable reference across re-renders', () => {
    const { result, rerender } = renderHook(() => useIqConnectedApi());
    const first = {
      fetchIq: result.current.fetchIq,
      verifyConnection: result.current.verifyConnection,
      fetchDashboardSummary: result.current.fetchDashboardSummary,
      fetchEvaluationSettings: result.current.fetchEvaluationSettings,
    };
    rerender();
    expect(result.current.fetchIq).toBe(first.fetchIq);
    expect(result.current.verifyConnection).toBe(first.verifyConnection);
    expect(result.current.fetchDashboardSummary).toBe(first.fetchDashboardSummary);
    expect(result.current.fetchEvaluationSettings).toBe(first.fetchEvaluationSettings);
  });
});
