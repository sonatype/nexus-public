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

jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
  },
  parseApiError: jest.fn((err) => err.message || 'Error'),
}));

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn(),
  },
}));

import { restClient, parseApiError } from '../../../../../../../interface/api';
import { ExtJS } from '../../../../../../../interface/ExtJS';
import { useBlobStoreCdnStatuses } from '../useBlobStoreCdnStatuses';

const mockedRestClient = restClient as jest.Mocked<typeof restClient>;
const mockedCheckPermission = ExtJS.checkPermission as jest.Mock;
const mockedParseApiError = parseApiError as jest.Mocked<typeof parseApiError>;

describe('useBlobStoreCdnStatuses', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedCheckPermission.mockReturnValue(true);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('returns loading state initially', () => {
    mockedRestClient.get.mockImplementation(() => new Promise(() => {}));

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    expect(result.current.isLoading).toBe(true);
    expect(result.current.blobStores).toEqual([]);
    expect(result.current.error).toBeNull();
  });

  it('fetches and filters S3 blob stores', async () => {
    mockedRestClient.get.mockResolvedValue([
      {
        name: 's3-store-1',
        typeId: 's3',
        globalDistributionStatus: 'CONFIGURED',
      },
      {
        name: 's3-store-2',
        typeId: 's3',
        globalDistributionStatus: 'NOT_CONFIGURED',
      },
      {
        name: 'file-store',
        typeId: 'file',
        globalDistributionStatus: 'UNSUPPORTED',
      },
    ]);

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.blobStores).toHaveLength(2);
    expect(result.current.blobStores[0]).toEqual({
      name: 's3-store-1',
      cdnEnabled: true,
      type: 's3',
    });
    expect(result.current.blobStores[1]).toEqual({
      name: 's3-store-2',
      cdnEnabled: false,
      type: 's3',
    });
  });

  it('detects CloudFront CDN type', async () => {
    mockedRestClient.get.mockResolvedValue([
      {
        name: 'cloudfront-store',
        typeId: 's3',
        globalDistributionStatus: 'CONFIGURED',
      },
    ]);

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.blobStores[0].cdnEnabled).toBe(true);
    expect(result.current.blobStores[0].type).toBe('s3');
  });

  it('detects Transfer Acceleration CDN type', async () => {
    mockedRestClient.get.mockResolvedValue([
      {
        name: 'transfer-store',
        typeId: 's3',
        globalDistributionStatus: 'CONFIGURED',
      },
    ]);

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.blobStores[0].cdnEnabled).toBe(true);
    expect(result.current.blobStores[0].type).toBe('s3');
  });

  it('detects disabled CDN', async () => {
    mockedRestClient.get.mockResolvedValue([
      {
        name: 'disabled-store',
        typeId: 's3',
        globalDistributionStatus: 'NOT_CONFIGURED',
      },
    ]);

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.blobStores[0].cdnEnabled).toBe(false);
    expect(result.current.blobStores[0].type).toBe('s3');
  });

  it('sorts blob stores alphabetically by name', async () => {
    mockedRestClient.get.mockResolvedValue([
      { name: 'zebra', typeId: 's3', globalDistributionStatus: 'CONFIGURED' },
      { name: 'alpha', typeId: 's3', globalDistributionStatus: 'NOT_CONFIGURED' },
      { name: 'middle', typeId: 's3', globalDistributionStatus: 'CONFIGURED' },
    ]);

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.blobStores[0].name).toBe('alpha');
    expect(result.current.blobStores[1].name).toBe('middle');
    expect(result.current.blobStores[2].name).toBe('zebra');
  });

  it('handles API errors', async () => {
    const error = new Error('Network error');
    mockedRestClient.get.mockRejectedValue(error);
    mockedParseApiError.mockReturnValue('Network error');

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.error).toBe('Network error');
    expect(result.current.blobStores).toEqual([]);
  });

  it('handles empty blob store list', async () => {
    mockedRestClient.get.mockResolvedValue([]);

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.blobStores).toEqual([]);
    expect(result.current.error).toBeNull();
  });

  it('handles missing data field', async () => {
    mockedRestClient.get.mockResolvedValue({});

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.blobStores).toEqual([]);
  });

  it('does not call the API when the user lacks nexus:blobstores:read', async () => {
    mockedCheckPermission.mockImplementation((p: string) => p !== 'nexus:blobstores:read');

    const { result } = renderHook(() => useBlobStoreCdnStatuses());

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(mockedRestClient.get).not.toHaveBeenCalled();
    expect(result.current.canReadBlobStores).toBe(false);
    expect(result.current.blobStores).toEqual([]);
    expect(result.current.error).toBeNull();
  });
});
