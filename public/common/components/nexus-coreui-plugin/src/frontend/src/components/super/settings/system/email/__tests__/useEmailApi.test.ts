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
import { useEmailApi } from '../useEmailApi';

// Mock the REST API from @sonatype/nexus-ui-plugin
const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err: any) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
  })),
}));

describe('useEmailApi', () => {
  const mockSettings = {
    enabled: true,
    host: 'smtp.example.com',
    port: 587,
    username: 'user',
    password: '',
    fromAddress: 'noreply@example.com',
    subjectPrefix: '[Nexus]',
    startTlsEnabled: true,
    startTlsRequired: false,
    sslOnConnectEnabled: false,
    sslServerIdentityCheckEnabled: false,
    nexusTrustStoreEnabled: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchSettings', () => {
    it('returns email settings on success', async () => {
      mockRestClient.get.mockResolvedValue(mockSettings);

      const { result } = renderHook(() => useEmailApi());

      let settings;
      await act(async () => {
        settings = await result.current.fetchSettings();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/email');
      expect(settings).toEqual(mockSettings);
    });

    it('throws error on failure', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValue({ message: 'API Error' });

      const { result } = renderHook(() => useEmailApi());

      await act(async () => {
        await expect(result.current.fetchSettings()).rejects.toThrow('API Error');
      });

      consoleSpy.mockRestore();
    });
  });

  describe('saveSettings', () => {
    it('saves settings successfully', async () => {
      mockRestClient.put.mockResolvedValue(undefined);
      mockRestClient.get.mockResolvedValue(mockSettings);

      const { result } = renderHook(() => useEmailApi());

      let savedSettings;
      await act(async () => {
        savedSettings = await result.current.saveSettings(mockSettings);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith('/service/rest/v1/email', mockSettings);
      expect(savedSettings).toEqual(mockSettings);
      expect(result.current.loading).toBe(false);
    });

    it('sets loading state during save', async () => {
      let resolvePut: () => void;
      mockRestClient.put.mockReturnValue(
        new Promise<void>((resolve) => {
          resolvePut = resolve;
        })
      );
      mockRestClient.get.mockResolvedValue(mockSettings);

      const { result } = renderHook(() => useEmailApi());

      act(() => {
        result.current.saveSettings(mockSettings);
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePut!();
      });

      expect(result.current.loading).toBe(false);
    });

    it('sets error on failure', async () => {
      mockRestClient.put.mockRejectedValue({ message: 'Save failed' });

      const { result } = renderHook(() => useEmailApi());

      await act(async () => {
        await expect(result.current.saveSettings(mockSettings)).rejects.toThrow('Save failed');
      });

      expect(result.current.error).toBe('Save failed');
      expect(result.current.loading).toBe(false);
    });
  });

  describe('sendVerificationEmail', () => {
    it('sends verification email successfully', async () => {
      mockRestClient.post.mockResolvedValue({ success: true });

      const { result } = renderHook(() => useEmailApi());

      let verificationResult;
      await act(async () => {
        verificationResult = await result.current.sendVerificationEmail('test@example.com');
      });

      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/email/verify',
        'test@example.com',
        { headers: { 'Content-Type': 'text/plain' } }
      );
      expect(verificationResult).toEqual({ success: true });
      expect(result.current.verifying).toBe(false);
    });

    it('handles verification failure', async () => {
      mockRestClient.post.mockResolvedValue({ success: false, reason: 'SMTP error' });

      const { result } = renderHook(() => useEmailApi());

      let verificationResult;
      await act(async () => {
        verificationResult = await result.current.sendVerificationEmail('test@example.com');
      });

      expect(verificationResult).toEqual({ success: false, reason: 'SMTP error' });
    });

    it('handles network error gracefully', async () => {
      mockRestClient.post.mockRejectedValue({ message: 'Network error' });

      const { result } = renderHook(() => useEmailApi());

      let verificationResult;
      await act(async () => {
        verificationResult = await result.current.sendVerificationEmail('test@example.com');
      });

      // Verification failures are returned as success=false, not thrown
      expect(verificationResult).toEqual({ success: false, reason: 'Network error' });
      expect(result.current.verifying).toBe(false);
    });

    it('sets verifying state during verification', async () => {
      let resolvePost: (value: unknown) => void;
      mockRestClient.post.mockReturnValue(
        new Promise((resolve) => {
          resolvePost = resolve;
        })
      );

      const { result } = renderHook(() => useEmailApi());

      act(() => {
        result.current.sendVerificationEmail('test@example.com');
      });

      expect(result.current.verifying).toBe(true);

      await act(async () => {
        resolvePost!({ success: true });
      });

      expect(result.current.verifying).toBe(false);
    });
  });
});
