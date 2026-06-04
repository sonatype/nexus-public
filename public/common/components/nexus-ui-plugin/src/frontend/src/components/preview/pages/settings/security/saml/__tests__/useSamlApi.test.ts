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
import { useSamlApi } from '../useSamlApi';
import { SamlConfiguration } from '../types';

const mockGet = jest.fn();
const mockPut = jest.fn();
const mockDelete = jest.fn();

// Mock the REST API from @/utils/api
jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: (...args: unknown[]) => mockGet(...args),
    put: (...args: unknown[]) => mockPut(...args),
    delete: (...args: unknown[]) => mockDelete(...args),
  },
  parseApiError: jest.fn((err: unknown) => {
    const error = err as { response?: { status?: number; data?: { message?: string } }; message?: string };
    return {
      message: error?.response?.data?.message || error?.message || 'Error',
      status: error?.response?.status,
    };
  }),
}));

describe('useSamlApi', () => {
  const mockConfiguration: SamlConfiguration = {
    entityId: 'https://nexus.example.com',
    idpMetadata: '<EntityDescriptor>...</EntityDescriptor>',
    usernameAttribute: 'email',
    firstNameAttribute: 'firstName',
    lastNameAttribute: 'lastName',
    emailAttribute: 'email',
    groupsAttribute: 'groups',
    validateResponseSignature: true,
    validateAssertionSignature: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockGet.mockReset();
    mockPut.mockReset();
    mockDelete.mockReset();
  });

  describe('initial state', () => {
    it('returns initial state with loading false', () => {
      const { result } = renderHook(() => useSamlApi());

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBe(null);
    });
  });

  describe('fetchConfiguration', () => {
    it('fetches configuration successfully', async () => {
      mockGet.mockResolvedValueOnce(mockConfiguration);

      const { result } = renderHook(() => useSamlApi());

      let config: SamlConfiguration | null = null;
      await act(async () => {
        config = await result.current.fetchConfiguration();
      });

      expect(config).toEqual(mockConfiguration);
      expect(mockGet).toHaveBeenCalledWith('service/rest/v1/security/saml');
    });

    it('returns null when configuration does not exist (404)', async () => {
      mockGet.mockRejectedValueOnce({ response: { status: 404 } });

      const { result } = renderHook(() => useSamlApi());

      let config: SamlConfiguration | null;
      await act(async () => {
        config = await result.current.fetchConfiguration();
      });

      expect(config).toBe(null);
    });

    it('sets loading state during fetch', async () => {
      let resolvePromise: (value: any) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      mockGet.mockReturnValueOnce(promise);

      const { result } = renderHook(() => useSamlApi());

      act(() => {
        result.current.fetchConfiguration();
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePromise!(mockConfiguration);
      });

      expect(result.current.loading).toBe(false);
    });

    it('sets error state on fetch failure', async () => {
      mockGet.mockRejectedValueOnce({
        response: { status: 500, data: { message: 'Internal Server Error' } },
      });

      const { result } = renderHook(() => useSamlApi());

      await act(async () => {
        try {
          await result.current.fetchConfiguration();
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBeTruthy();
    });
  });

  describe('saveConfiguration', () => {
    it('saves configuration successfully', async () => {
      mockPut.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useSamlApi());

      await act(async () => {
        await result.current.saveConfiguration(mockConfiguration);
      });

      expect(mockPut).toHaveBeenCalledWith(
        'service/rest/v1/security/saml',
        mockConfiguration
      );
    });

    it('sets loading state during save', async () => {
      let resolvePromise: (value: any) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      mockPut.mockReturnValueOnce(promise);

      const { result } = renderHook(() => useSamlApi());

      act(() => {
        result.current.saveConfiguration(mockConfiguration);
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePromise!(undefined);
      });

      expect(result.current.loading).toBe(false);
    });

    it('sets error state on save failure', async () => {
      mockPut.mockRejectedValueOnce({
        response: { data: { message: 'Validation error' } },
      });

      const { result } = renderHook(() => useSamlApi());

      await act(async () => {
        try {
          await result.current.saveConfiguration(mockConfiguration);
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBeTruthy();
    });
  });

  describe('deleteConfiguration', () => {
    it('deletes configuration successfully', async () => {
      mockDelete.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useSamlApi());

      await act(async () => {
        await result.current.deleteConfiguration();
      });

      expect(mockDelete).toHaveBeenCalledWith('service/rest/v1/security/saml');
    });

    it('sets error state on delete failure', async () => {
      mockDelete.mockRejectedValueOnce({
        response: { data: { message: 'Internal Server Error' } },
      });

      const { result } = renderHook(() => useSamlApi());

      await act(async () => {
        try {
          await result.current.deleteConfiguration();
        } catch {
          // Expected
        }
      });

      expect(result.current.error).toBeTruthy();
    });
  });

  describe('getMetadataUrl', () => {
    it('returns the metadata URL', () => {
      const { result } = renderHook(() => useSamlApi());

      const url = result.current.getMetadataUrl();

      expect(url).toBe('service/rest/v1/security/saml/metadata');
    });
  });

  describe('setError', () => {
    it('manually sets error state', () => {
      const { result } = renderHook(() => useSamlApi());

      act(() => {
        result.current.setError('Custom error message');
      });

      expect(result.current.error).toBe('Custom error message');
    });

    it('clears error when set to null', () => {
      const { result } = renderHook(() => useSamlApi());

      act(() => {
        result.current.setError('Error');
        result.current.setError(null);
      });

      expect(result.current.error).toBe(null);
    });
  });
});


