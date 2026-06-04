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

import { renderHook, act, waitFor } from '@testing-library/react';
import { useLicensingApi } from '../useLicensingApi';

const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

const mockProLicenseUrl = jest.fn();
jest.mock('../../../../../../../interface/api', () => ({
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

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    proLicenseUrl: (...args: any[]) => mockProLicenseUrl(...args),
  },
}));

describe('useLicensingApi', () => {
  const mockLicenseData = {
    contactCompany: 'Acme Corp',
    contactName: 'John Doe',
    contactEmail: 'john@acme.com',
    effectiveDate: '2024-01-01T00:00:00Z',
    expirationDate: '2025-12-31T23:59:59Z',
    licenseType: 'PRO',
    licensedUsers: 100,
    fingerprint: 'abc123',
    maxRepoRequests: 1000000,
    maxRepoComponents: 50000,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockProLicenseUrl.mockReturnValue('https://example.com/license');
  });

  describe('fetchLicense', () => {
    it('returns license data on success', async () => {
      mockRestClient.get.mockResolvedValue(mockLicenseData);

      const { result } = renderHook(() => useLicensingApi());

      let licenseData;
      await act(async () => {
        licenseData = await result.current.fetchLicense();
      });

      expect(mockRestClient.get).toHaveBeenCalledWith('service/rest/internal/ui/license');
      expect(licenseData).toEqual(mockLicenseData);
    });

    it('returns empty object when response data is missing', async () => {
      mockRestClient.get.mockResolvedValue(undefined);

      const { result } = renderHook(() => useLicensingApi());

      let licenseData;
      await act(async () => {
        licenseData = await result.current.fetchLicense();
      });

      expect(licenseData).toEqual({});
    });

    it('throws error on failure', async () => {
      const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
      mockRestClient.get.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useLicensingApi());

      await act(async () => {
        await expect(result.current.fetchLicense()).rejects.toThrow('Failed to load license information');
      });

      expect(consoleErrorSpy).toHaveBeenCalled();
      consoleErrorSpy.mockRestore();
    });
  });

  describe('uploadLicense', () => {
    it('uploads license file successfully', async () => {
      const file = new File(['license content'], 'license.lic', { type: 'application/octet-stream' });
      const arrayBuffer = new ArrayBuffer(8);
      mockRestClient.post.mockResolvedValue({});

      // Mock FileReader
      const mockFileReader = {
        readAsArrayBuffer: jest.fn(),
        result: arrayBuffer,
        onload: null as any,
        onerror: null as any,
      };
      global.FileReader = jest.fn(() => mockFileReader) as any;

      const { result } = renderHook(() => useLicensingApi());

      await act(async () => {
        // Trigger FileReader onload
        setTimeout(() => {
          if (mockFileReader.onload) {
            mockFileReader.onload({} as any);
          }
        }, 0);
        await result.current.uploadLicense(file);
      });

      await waitFor(() => {
        expect(mockRestClient.post).toHaveBeenCalledWith(
          'service/rest/internal/ui/license',
          arrayBuffer,
          {
            headers: {
              'Content-Type': 'application/octet-stream',
            },
          }
        );
      });

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('sets error state on upload failure', async () => {
      const file = new File(['license content'], 'license.lic', { type: 'application/octet-stream' });
      const arrayBuffer = new ArrayBuffer(8);
      const errorResponse = {
        response: {
          data: {
            message: 'Invalid license file',
          },
        },
      };
      mockRestClient.post.mockRejectedValue(errorResponse);

      // Mock FileReader
      const mockFileReader = {
        readAsArrayBuffer: jest.fn(),
        result: arrayBuffer,
        onload: null as any,
        onerror: null as any,
      };
      global.FileReader = jest.fn(() => mockFileReader) as any;

      const { result } = renderHook(() => useLicensingApi());

      await act(async () => {
        // Trigger FileReader onload
        setTimeout(() => {
          if (mockFileReader.onload) {
            mockFileReader.onload({} as any);
          }
        }, 0);
        await expect(result.current.uploadLicense(file)).rejects.toThrow('Invalid license file');
      });

      await waitFor(() => {
        expect(result.current.error).toBe('Invalid license file');
      });

      expect(result.current.loading).toBe(false);
    });

    it('handles upload error without response data', async () => {
      const file = new File(['license content'], 'license.lic', { type: 'application/octet-stream' });
      const arrayBuffer = new ArrayBuffer(8);
      const error = { message: 'Network error' };
      mockRestClient.post.mockRejectedValue(error);

      // Mock FileReader
      const mockFileReader = {
        readAsArrayBuffer: jest.fn(),
        result: arrayBuffer,
        onload: null as any,
        onerror: null as any,
      };
      global.FileReader = jest.fn(() => mockFileReader) as any;

      const { result } = renderHook(() => useLicensingApi());

      await act(async () => {
        // Trigger FileReader onload
        setTimeout(() => {
          if (mockFileReader.onload) {
            mockFileReader.onload({} as any);
          }
        }, 0);
        await expect(result.current.uploadLicense(file)).rejects.toThrow('Network error');
      });

      await waitFor(() => {
        expect(result.current.error).toBe('Network error');
      });
    });

    it('handles FileReader error', async () => {
      const file = new File(['license content'], 'license.lic', { type: 'application/octet-stream' });

      // Mock FileReader with error
      const mockFileReader = {
        readAsArrayBuffer: jest.fn(),
        result: null,
        onload: null as any,
        onerror: null as any,
      };
      global.FileReader = jest.fn(() => mockFileReader) as any;

      const { result } = renderHook(() => useLicensingApi());

      await act(async () => {
        // Trigger FileReader onerror
        setTimeout(() => {
          if (mockFileReader.onerror) {
            mockFileReader.onerror({} as any);
          }
        }, 0);
        await expect(result.current.uploadLicense(file)).rejects.toThrow();
      });
    });
  });

  describe('getLicenseAgreementUrl', () => {
    it('returns license agreement URL', () => {
      mockProLicenseUrl.mockReturnValue('https://example.com/license-agreement');

      const { result } = renderHook(() => useLicensingApi());

      const url = result.current.getLicenseAgreementUrl();

      expect(mockProLicenseUrl).toHaveBeenCalled();
      expect(url).toBe('https://example.com/license-agreement');
    });

    it('returns empty string when URL is not available', () => {
      mockProLicenseUrl.mockReturnValue(null);

      const { result } = renderHook(() => useLicensingApi());

      const url = result.current.getLicenseAgreementUrl();

      expect(url).toBe('');
    });

    it('returns empty string when ExtJS throws error', () => {
      mockProLicenseUrl.mockImplementation(() => {
        throw new Error('ExtJS error');
      });

      const { result } = renderHook(() => useLicensingApi());

      const url = result.current.getLicenseAgreementUrl();

      expect(url).toBe('');
    });
  });

  describe('setError', () => {
    it('allows setting and clearing error manually', () => {
      const { result } = renderHook(() => useLicensingApi());

      expect(result.current.error).toBeNull();

      act(() => {
        result.current.setError('Manual error');
      });

      expect(result.current.error).toBe('Manual error');

      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('loading state', () => {
    it('sets loading to true during upload', async () => {
      const file = new File(['license content'], 'license.lic', { type: 'application/octet-stream' });
      const arrayBuffer = new ArrayBuffer(8);

      // Mock FileReader to auto-trigger onload
      const mockFileReader = {
        readAsArrayBuffer: jest.fn(function(this: any) {
          // Auto-trigger onload after a microtask
          Promise.resolve().then(() => {
            if (this.onload) {
              this.onload({} as any);
            }
          });
        }),
        result: arrayBuffer,
        onload: null as any,
        onerror: null as any,
      };
      global.FileReader = jest.fn(() => mockFileReader) as any;

      // Track if loading was ever true during the upload
      let wasLoadingDuringUpload = false;
      mockRestClient.post.mockImplementation(() => {
        // At this point, loading should be true
        wasLoadingDuringUpload = true;
        return Promise.resolve({});
      });

      const { result } = renderHook(() => useLicensingApi());

      await act(async () => {
        await result.current.uploadLicense(file);
      });

      // Verify loading was true during the upload
      expect(wasLoadingDuringUpload).toBe(true);

      // Loading should be false after upload completes
      expect(result.current.loading).toBe(false);
    });
  });
});


