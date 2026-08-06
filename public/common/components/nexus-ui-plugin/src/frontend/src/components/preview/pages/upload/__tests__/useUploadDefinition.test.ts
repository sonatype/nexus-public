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

import { renderHook, waitFor } from '@testing-library/react';

import { useUploadDefinition } from '../hooks/useUploadDefinition';

// Mock the REST API from the relative path that the source imports from
// Note: jest.mock is hoisted, so we use jest.fn() inside the factory
jest.mock('../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
  },
  ENDPOINTS: {
    REPOSITORIES: '/service/rest/v1/repositories',
    REPOSITORIES_DETAILS: '/service/rest/internal/ui/repositories/details',
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
  isNotFoundError: jest.fn((apiError: { status?: number }) => apiError?.status === 404),
}));

import { restClient } from '../../../../../interface/api';

// Get mock reference
const mockGet = restClient.get as jest.MockedFunction<typeof restClient.get>;

describe('useUploadDefinition', () => {
  const mockRepository = {
    name: 'maven-releases',
    format: 'maven2',
    type: 'hosted',
    url: 'http://localhost:8081/repository/maven-releases',
    status: { online: true },
  };

  const mockUploadDefinition = {
    format: 'maven2',
    uiUpload: true,
    multipleUpload: true,
    componentFields: [
      { name: 'groupId', type: 'STRING', displayName: 'Group ID', group: 'Component coordinates', optional: false },
      { name: 'artifactId', type: 'STRING', displayName: 'Artifact ID', group: 'Component coordinates', optional: false },
      { name: 'version', type: 'STRING', displayName: 'Version', group: 'Component coordinates', optional: false },
      { name: 'generate-pom', type: 'BOOLEAN', displayName: 'Generate a POM file', group: 'Options', optional: true },
    ],
    assetFields: [
      { name: 'extension', type: 'STRING', displayName: 'Extension', optional: false },
      { name: 'classifier', type: 'STRING', displayName: 'Classifier', optional: true },
    ],
    regexMap: {
      regex: '^(.+?)-(.+?)\\.(.+)$',
      fieldList: ['artifactId', 'version', 'extension'],
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns loading state initially', () => {
    mockGet.mockReturnValue(new Promise(() => {})); // Never resolves

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    expect(result.current.loading).toBe(true);
    expect(result.current.error).toBeNull();
    expect(result.current.uploadDefinition).toBeNull();
  });

  it('fetches and returns upload definition successfully', async () => {
    mockGet
      .mockResolvedValueOnce([mockRepository]) // repositories
      .mockResolvedValueOnce([mockUploadDefinition]); // upload definitions

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBeNull();
    expect(result.current.uploadDefinition).toEqual(mockUploadDefinition);
    expect(result.current.repositorySettings).toEqual(mockRepository);
    expect(result.current.componentFields).toHaveLength(4);
    expect(result.current.assetFields).toHaveLength(2);
    expect(result.current.multipleUpload).toBe(true);
    expect(result.current.regexMap).toEqual(mockUploadDefinition.regexMap);
  });

  it('groups component fields by group property', async () => {
    mockGet
      .mockResolvedValueOnce([mockRepository])
      .mockResolvedValueOnce([mockUploadDefinition]);

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.componentFieldsByGroup).toHaveProperty('Component coordinates');
    expect(result.current.componentFieldsByGroup).toHaveProperty('Options');
    expect(result.current.componentFieldsByGroup['Component coordinates']).toHaveLength(3);
    expect(result.current.componentFieldsByGroup.Options).toHaveLength(1);
  });

  it('returns error when repository is not found', async () => {
    mockGet
      .mockResolvedValueOnce([]) // empty repositories
      .mockResolvedValueOnce([mockUploadDefinition]);

    const { result } = renderHook(() => useUploadDefinition('nonexistent-repo'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe('Repository "nonexistent-repo" not found');
    expect(result.current.uploadDefinition).toBeNull();
  });

  it('returns error when upload definition is not found', async () => {
    const unsupportedRepo = { ...mockRepository, format: 'unsupported-format' };

    mockGet
      .mockResolvedValueOnce([unsupportedRepo])
      .mockResolvedValueOnce([mockUploadDefinition]);

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe('No upload definition found for format "unsupported-format"');
  });

  it('returns error when repository does not support UI upload', async () => {
    const noUiUploadDefinition = { ...mockUploadDefinition, uiUpload: false };

    mockGet
      .mockResolvedValueOnce([mockRepository])
      .mockResolvedValueOnce([noUiUploadDefinition]);

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe('Repository "maven-releases" does not support upload through the web UI');
  });

  it('returns error when repository is not hosted', async () => {
    const proxyRepo = { ...mockRepository, type: 'proxy' };

    mockGet
      .mockResolvedValueOnce([proxyRepo])
      .mockResolvedValueOnce([mockUploadDefinition]);

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe('Repository "maven-releases" is not a hosted repository');
  });

  it('returns error when repository name is empty', async () => {
    const { result } = renderHook(() => useUploadDefinition(''));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe('Repository name is required');
  });

  it('handles API errors gracefully', async () => {
    mockGet.mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBe('Network error');
    expect(result.current.uploadDefinition).toBeNull();
  });

  it('handles 404 on upload-specs gracefully (e.g. cloud deployment)', async () => {
    mockGet
      .mockResolvedValueOnce([mockRepository])
      .mockRejectedValueOnce({ response: { status: 404 } });

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBeNull();
    expect(result.current.uploadDefinition).toBeNull();
    expect(result.current.repositorySettings).toEqual(mockRepository);
  });

  it('falls back to public repos endpoint when internal endpoint returns 404 (cloud deployment)', async () => {
    mockGet.mockImplementation((url: string) => {
      if (url === '/service/rest/internal/ui/repositories/details') {
        return Promise.reject({ response: { status: 404 } });
      }
      if (url === '/service/rest/v1/repositories') {
        return Promise.resolve([mockRepository]);
      }
      if (url.includes('/formats/upload-specs')) {
        return Promise.resolve([mockUploadDefinition]);
      }
      return Promise.resolve([]);
    });

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBeNull();
    expect(result.current.uploadDefinition).toEqual(mockUploadDefinition);
    expect(result.current.repositorySettings).toEqual(mockRepository);
  });

  it('provides refetch function', async () => {
    mockGet
      .mockResolvedValueOnce([mockRepository])
      .mockResolvedValueOnce([mockUploadDefinition]);

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(typeof result.current.refetch).toBe('function');
  });

  it('returns default values for missing fields', async () => {
    const minimalDefinition = {
      format: 'maven2',
      uiUpload: true,
      multipleUpload: false,
    };

    mockGet
      .mockResolvedValueOnce([mockRepository])
      .mockResolvedValueOnce([minimalDefinition]);

    const { result } = renderHook(() => useUploadDefinition('maven-releases'));

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.componentFields).toEqual([]);
    expect(result.current.assetFields).toEqual([]);
    expect(result.current.multipleUpload).toBe(false);
    expect(result.current.regexMap).toBeNull();
  });
});
