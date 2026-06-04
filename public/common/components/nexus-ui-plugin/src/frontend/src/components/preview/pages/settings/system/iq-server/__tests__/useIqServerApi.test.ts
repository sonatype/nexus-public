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
import Axios from 'axios';
import { useIqServerApi } from '../useIqServerApi';
import { IqServerConfiguration, DEFAULT_IQ_CONFIGURATION } from '../types';

// Mock Axios
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('useIqServerApi', () => {
  const mockApiResponse: IqServerConfiguration = {
    enabled: true,
    url: 'https://iq.example.com',
    authenticationType: 'USER',
    username: 'iq-user',
    password: '',
    useTrustStoreForUrl: false,
    timeoutSeconds: 60,
    properties: '',
    showLink: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('initial state', () => {
    it('should return initial state', () => {
      const { result } = renderHook(() => useIqServerApi());

      expect(result.current.loading).toBe(false);
      expect(result.current.verifying).toBe(false);
      expect(result.current.error).toBeNull();
      expect(typeof result.current.fetchSettings).toBe('function');
      expect(typeof result.current.saveSettings).toBe('function');
      expect(typeof result.current.verifyConnection).toBe('function');
      expect(typeof result.current.setError).toBe('function');
    });
  });

  describe('fetchSettings', () => {
    it('should fetch and normalize settings successfully', async () => {
      mockedAxios.get.mockResolvedValue({ data: mockApiResponse });

      const { result } = renderHook(() => useIqServerApi());

      let settings: IqServerConfiguration | undefined;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(mockedAxios.get).toHaveBeenCalledWith('service/rest/v1/iq');
      expect(settings).toEqual({
        ...DEFAULT_IQ_CONFIGURATION,
        ...mockApiResponse,
      });
    });

    it('should merge partial response with defaults', async () => {
      const partialResponse = {
        enabled: true,
        url: 'https://iq.example.com',
      };
      mockedAxios.get.mockResolvedValue({ data: partialResponse });

      const { result } = renderHook(() => useIqServerApi());

      let settings: IqServerConfiguration | undefined;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(settings).toEqual({
        ...DEFAULT_IQ_CONFIGURATION,
        ...partialResponse,
      });
    });

    it('should throw error with API message', async () => {
      mockedAxios.get.mockRejectedValue({
        response: { data: { message: 'Access denied' } },
      });

      const { result } = renderHook(() => useIqServerApi());

      await expect(result.current.fetchSettings()).rejects.toThrow('Access denied');
    });

    it('should throw error with error message', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useIqServerApi());

      await expect(result.current.fetchSettings()).rejects.toThrow('Network error');
    });

    it('should throw generic error when no message available', async () => {
      mockedAxios.get.mockRejectedValue({});

      const { result } = renderHook(() => useIqServerApi());

      await expect(result.current.fetchSettings()).rejects.toThrow('Failed to load IQ Server settings');
    });
  });

  describe('saveSettings', () => {
    it('should save settings successfully', async () => {
      mockedAxios.put.mockResolvedValue({});
      mockedAxios.get.mockResolvedValue({ data: mockApiResponse });

      const { result } = renderHook(() => useIqServerApi());

      let savedSettings: IqServerConfiguration | undefined;
      await act(async () => {
        savedSettings = await result.current.saveSettings(mockApiResponse);
      });

      expect(mockedAxios.put).toHaveBeenCalledWith('service/rest/v1/iq', mockApiResponse);
      expect(mockedAxios.get).toHaveBeenCalledWith('service/rest/v1/iq');
      expect(savedSettings).toEqual({
        ...DEFAULT_IQ_CONFIGURATION,
        ...mockApiResponse,
      });
      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should set loading state during save', async () => {
      let resolvePromise: (value: any) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      mockedAxios.put.mockReturnValue(promise);
      mockedAxios.get.mockResolvedValue({ data: mockApiResponse });

      const { result } = renderHook(() => useIqServerApi());

      let savePromise: Promise<IqServerConfiguration>;
      act(() => {
        savePromise = result.current.saveSettings(mockApiResponse);
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePromise!({});
        await savePromise;
      });

      expect(result.current.loading).toBe(false);
    });

    it('should set error state when save fails', async () => {
      const errorMessage = 'Permission denied';
      mockedAxios.put.mockRejectedValue(new Error(errorMessage));

      const { result } = renderHook(() => useIqServerApi());

      await act(async () => {
        try {
          await result.current.saveSettings(mockApiResponse);
        } catch {
          // Expected error
        }
      });

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBe(errorMessage);
    });

    it('should set error from API response', async () => {
      mockedAxios.put.mockRejectedValue({
        response: { data: { message: 'Invalid URL format' } },
      });

      const { result } = renderHook(() => useIqServerApi());

      await act(async () => {
        try {
          await result.current.saveSettings(mockApiResponse);
        } catch {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Invalid URL format');
    });

    it('should use generic error when no message available', async () => {
      mockedAxios.put.mockRejectedValue({});

      const { result } = renderHook(() => useIqServerApi());

      await act(async () => {
        try {
          await result.current.saveSettings(mockApiResponse);
        } catch {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Failed to save IQ Server settings');
    });
  });

  describe('verifyConnection', () => {
    it('should verify connection successfully', async () => {
      mockedAxios.post.mockResolvedValue({
        data: { reason: 'Connection successful' },
      });

      const { result } = renderHook(() => useIqServerApi());

      let verifyResult: { success: boolean; reason?: string } | undefined;
      await act(async () => {
        verifyResult = await result.current.verifyConnection(mockApiResponse);
      });

      expect(mockedAxios.post).toHaveBeenCalledWith(
        'service/rest/internal/ui/iq/verify-connection',
        mockApiResponse
      );
      expect(verifyResult).toEqual({
        success: true,
        reason: 'Connection successful',
      });
      expect(result.current.verifying).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should set verifying state during verification', async () => {
      let resolvePromise: (value: any) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      mockedAxios.post.mockReturnValue(promise);

      const { result } = renderHook(() => useIqServerApi());

      let verifyPromise: Promise<{ success: boolean; reason?: string }>;
      act(() => {
        verifyPromise = result.current.verifyConnection(mockApiResponse);
      });

      expect(result.current.verifying).toBe(true);

      await act(async () => {
        resolvePromise!({ data: { reason: 'OK' } });
        await verifyPromise;
      });

      expect(result.current.verifying).toBe(false);
    });

    it('should return failure with string error response', async () => {
      mockedAxios.post.mockRejectedValue({
        response: { data: 'Connection refused' },
      });

      const { result } = renderHook(() => useIqServerApi());

      let verifyResult: { success: boolean; reason?: string } | undefined;
      await act(async () => {
        verifyResult = await result.current.verifyConnection(mockApiResponse);
      });

      expect(verifyResult).toEqual({
        success: false,
        reason: 'Connection refused',
      });
      expect(result.current.verifying).toBe(false);
    });

    it('should return failure with object error response', async () => {
      mockedAxios.post.mockRejectedValue({
        response: { data: { error: 'timeout', code: 504 } },
      });

      const { result } = renderHook(() => useIqServerApi());

      let verifyResult: { success: boolean; reason?: string } | undefined;
      await act(async () => {
        verifyResult = await result.current.verifyConnection(mockApiResponse);
      });

      expect(verifyResult).toEqual({
        success: false,
        reason: JSON.stringify({ error: 'timeout', code: 504 }),
      });
    });

    it('should return failure with error message', async () => {
      mockedAxios.post.mockRejectedValue(new Error('Network timeout'));

      const { result } = renderHook(() => useIqServerApi());

      let verifyResult: { success: boolean; reason?: string } | undefined;
      await act(async () => {
        verifyResult = await result.current.verifyConnection(mockApiResponse);
      });

      expect(verifyResult).toEqual({
        success: false,
        reason: 'Network timeout',
      });
    });

    it('should return generic failure message when no details', async () => {
      mockedAxios.post.mockRejectedValue({});

      const { result } = renderHook(() => useIqServerApi());

      let verifyResult: { success: boolean; reason?: string } | undefined;
      await act(async () => {
        verifyResult = await result.current.verifyConnection(mockApiResponse);
      });

      expect(verifyResult).toEqual({
        success: false,
        reason: 'Connection verification failed',
      });
    });
  });

  describe('setError', () => {
    it('should allow setting error manually', () => {
      const { result } = renderHook(() => useIqServerApi());

      act(() => {
        result.current.setError('Custom error message');
      });

      expect(result.current.error).toBe('Custom error message');
    });

    it('should allow clearing error state', async () => {
      mockedAxios.put.mockRejectedValue(new Error('Some error'));

      const { result } = renderHook(() => useIqServerApi());

      // First, trigger an error
      await act(async () => {
        try {
          await result.current.saveSettings(mockApiResponse);
        } catch {
          // Expected error
        }
      });

      expect(result.current.error).toBe('Some error');

      // Clear the error
      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });
});
