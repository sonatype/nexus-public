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

// Mock the REST API at the relative path used by the source
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
  urlBuilder: {
    capabilities: {
      list: jest.fn(() => '/service/rest/v1/capabilities'),
      types: jest.fn(() => '/service/rest/v1/capabilities/types'),
      get: jest.fn((id: string) => `/service/rest/v1/capabilities/${encodeURIComponent(id)}`),
      create: jest.fn(() => '/service/rest/v1/capabilities'),
      update: jest.fn((id: string) => `/service/rest/v1/capabilities/${encodeURIComponent(id)}`),
      delete: jest.fn((id: string) => `/service/rest/v1/capabilities/${encodeURIComponent(id)}`),
    },
  },
  parseApiError: jest.fn((err: any) => ({
    message: err?.response?.data?.message || err?.message || 'Error',
    status: err?.response?.status,
  })),
}));

import { useCapabilitiesApi } from '../useCapabilitiesApi';
import { restClient, urlBuilder } from '../../../../../../../interface/api';

// Get mock references
const mockRestClient = restClient as jest.Mocked<typeof restClient>;
const mockUrlBuilder = urlBuilder as jest.Mocked<typeof urlBuilder>;

describe('useCapabilitiesApi', () => {
  // REST API response format
  const mockRestCapability = {
    id: 'cap-1',
    type: 'outreach',
    enabled: true,
    notes: 'Test capability',
    properties: { repository: 'maven-central' },
  };

  const mockRestCapabilityType = {
    id: 'outreach',
    name: 'Outreach: Management',
    about: 'Enables outreach features',
    formFields: [
      { id: 'repository', type: 'string', label: 'Repository', required: true },
    ],
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchCapabilities', () => {
    it('fetches capabilities successfully using REST API', async () => {
      mockRestClient.get.mockResolvedValue([mockRestCapability]);

      const { result } = renderHook(() => useCapabilitiesApi());

      let capabilities: any;
      await act(async () => {
        capabilities = await result.current.fetchCapabilities();
      });

      expect(capabilities).toHaveLength(1);
      expect(capabilities[0].id).toBe('cap-1');
      expect(capabilities[0].typeId).toBe('outreach'); // Transformed from 'type'
      expect(capabilities[0].enabled).toBe(true);
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/capabilities');
    });

    it('handles error when fetching capabilities fails', async () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      const error = new Error('Network error');
      mockRestClient.get.mockRejectedValue(error);

      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await expect(result.current.fetchCapabilities()).rejects.toThrow('Network error');
      });
      errorSpy.mockRestore();
    });
  });

  describe('fetchCapabilityTypes', () => {
    it('fetches capability types successfully using REST API', async () => {
      mockRestClient.get.mockResolvedValue([mockRestCapabilityType]);

      const { result } = renderHook(() => useCapabilitiesApi());

      let types: any;
      await act(async () => {
        types = await result.current.fetchCapabilityTypes();
      });

      expect(types).toHaveLength(1);
      expect(types[0].id).toBe('outreach');
      expect(types[0].name).toBe('Outreach: Management');
      expect(mockRestClient.get).toHaveBeenCalledWith('/service/rest/v1/capabilities/types');
    });

    it('handles error when fetching capability types fails', async () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      const error = new Error('Network error');
      mockRestClient.get.mockRejectedValue(error);

      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await expect(result.current.fetchCapabilityTypes()).rejects.toThrow('Network error');
      });
      errorSpy.mockRestore();
    });
  });

  describe('createCapability', () => {
    it('creates capability successfully using REST API', async () => {
      const newCapability = {
        typeId: 'outreach',
        enabled: true,
        notes: 'New capability',
        properties: { repository: 'maven-central' },
      };

      const createdRestCapability = {
        id: 'cap-new',
        type: 'outreach',
        enabled: true,
        notes: 'New capability',
        properties: { repository: 'maven-central' },
      };

      mockRestClient.post.mockResolvedValue(createdRestCapability);

      const { result } = renderHook(() => useCapabilitiesApi());

      let created: any;
      await act(async () => {
        created = await result.current.createCapability(newCapability);
      });

      expect(created.id).toBe('cap-new');
      expect(created.typeId).toBe('outreach');
      expect(mockRestClient.post).toHaveBeenCalledWith(
        '/service/rest/v1/capabilities',
        expect.objectContaining({
          type: 'outreach', // Transformed from typeId
          enabled: true,
          notes: 'New capability',
        })
      );
    });

    it('handles error when create fails', async () => {
      const error = {
        response: { data: { message: 'Capability already exists' } },
        message: 'Capability already exists',
      };
      mockRestClient.post.mockRejectedValue(error);

      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await expect(
          result.current.createCapability({
            typeId: 'outreach',
            enabled: true,
            notes: '',
            properties: {},
          })
        ).rejects.toThrow('Capability already exists');
      });
    });
  });

  describe('updateCapability', () => {
    it('updates capability successfully using REST API', async () => {
      const updatedCapability = {
        id: 'cap-1',
        typeId: 'outreach',
        enabled: false,
        notes: 'Updated notes',
        properties: { repository: 'maven-releases' },
      };

      const updatedRestCapability = {
        id: 'cap-1',
        type: 'outreach',
        enabled: false,
        notes: 'Updated notes',
        properties: { repository: 'maven-releases' },
      };

      mockRestClient.put.mockResolvedValue(updatedRestCapability);

      const { result } = renderHook(() => useCapabilitiesApi());

      let updated: any;
      await act(async () => {
        updated = await result.current.updateCapability(updatedCapability);
      });

      expect(updated.id).toBe('cap-1');
      expect(updated.enabled).toBe(false);
      expect(mockUrlBuilder.capabilities.update).toHaveBeenCalledWith('cap-1');
      expect(mockRestClient.put).toHaveBeenCalledWith(
        expect.stringContaining('/service/rest/v1/capabilities/'),
        expect.objectContaining({
          id: 'cap-1',
          type: 'outreach',
          enabled: false,
        })
      );
    });

    it('sanitizes null/undefined property values to empty strings (bug jqxh)', async () => {
      const capabilityWithNulls = {
        id: 'cap-1',
        typeId: 'outreach',
        enabled: true,
        notes: 'Test',
        properties: { repository: 'maven-central', interval: null as any, empty: undefined as any },
      };

      mockRestClient.put.mockResolvedValue({
        id: 'cap-1', type: 'outreach', enabled: true, notes: 'Test', properties: { repository: 'maven-central' },
      });

      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await result.current.updateCapability(capabilityWithNulls);
      });

      const payload = mockRestClient.put.mock.calls[0][1];
      expect(payload.properties.repository).toBe('maven-central');
      expect(payload.properties.interval).toBe('');
      expect(payload.properties.empty).toBe('');
    });

    it('throws error when ID is missing', async () => {
      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await expect(
          result.current.updateCapability({
            typeId: 'outreach',
            enabled: true,
            notes: '',
            properties: {},
          })
        ).rejects.toThrow('Capability ID is required for update');
      });
    });
  });

  describe('deleteCapability', () => {
    it('deletes capability successfully using REST API', async () => {
      mockRestClient.delete.mockResolvedValue(undefined);

      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await result.current.deleteCapability('cap-1');
      });

      expect(mockUrlBuilder.capabilities.delete).toHaveBeenCalledWith('cap-1');
      expect(mockRestClient.delete).toHaveBeenCalled();
    });

    it('handles error when delete fails', async () => {
      const error = new Error('Delete failed');
      mockRestClient.delete.mockRejectedValue(error);

      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await expect(result.current.deleteCapability('cap-1')).rejects.toThrow('Delete failed');
      });
    });
  });

  describe('enableCapability', () => {
    it('enables capability by updating with enabled=true', async () => {
      mockRestClient.put.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await result.current.enableCapability({
          id: 'cap-1',
          typeId: 'outreach',
          enabled: false,
          notes: 'Test',
          properties: { repository: 'maven-central' },
        } as any);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          id: 'cap-1',
          type: 'outreach',
          enabled: true,
        })
      );
    });
  });

  describe('disableCapability', () => {
    it('disables capability by updating with enabled=false', async () => {
      mockRestClient.put.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await result.current.disableCapability({
          id: 'cap-1',
          typeId: 'outreach',
          enabled: true,
          notes: 'Test',
          properties: { repository: 'maven-central' },
        } as any);
      });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          id: 'cap-1',
          type: 'outreach',
          enabled: false,
        })
      );
    });
  });

  describe('loading and error states', () => {
    it('sets loading state during API calls', async () => {
      let resolvePromise: (value: any) => void;
      const pendingPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      mockRestClient.post.mockReturnValueOnce(pendingPromise as any);

      const { result } = renderHook(() => useCapabilitiesApi());

      expect(result.current.loading).toBe(false);

      let createPromise: Promise<unknown>;
      await act(async () => {
        createPromise = result.current.createCapability({
          typeId: 'outreach',
          enabled: true,
          notes: 'Test',
          properties: {},
        });
      });

      await waitFor(() => {
        expect(result.current.loading).toBe(true);
      });

      await act(async () => {
        resolvePromise!({ id: 'cap-1', type: 'outreach', enabled: true, notes: 'Test', properties: {} });
        await createPromise;
      });

      expect(result.current.loading).toBe(false);
    });

    it('sets error state on failure', async () => {
      const error = new Error('Network error');
      mockRestClient.post.mockRejectedValue(error);

      const { result } = renderHook(() => useCapabilitiesApi());

      await act(async () => {
        await expect(result.current.createCapability({
          typeId: 'outreach',
          enabled: true,
          notes: 'Test',
          properties: {},
        })).rejects.toThrow('Network error');
      });

      await waitFor(() => {
        expect(result.current.error).toBe('Network error');
      });
    });

    it('clears error with setError', () => {
      const { result } = renderHook(() => useCapabilitiesApi());

      act(() => {
        result.current.setError('Test error');
      });

      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });
});
