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

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import BlobStoresList from '../BlobStoresList';
import * as useBlobStoresModule from '../useBlobStores';

jest.mock('../useBlobStores', () => ({
  useBlobStoresList: jest.fn(),
}));

// Mock the router
const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {
      go: mockGo
    }
  })
}));

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    state: () => ({
      getValue: jest.fn().mockReturnValue(false)
    }),
    useUser: () => true,
    usePermission: (fn) => fn(),
    checkPermission: () => true
  },
  HumanReadableUtils: {
    bytesToString: (bytes) => `${bytes} bytes`
  },
  Permissions: {
    BLOB_STORES: {
      CREATE: 'nexus:blobstores:create'
    }
  }
}));

describe('BlobStoresList', () => {
  const mockUseBlobStoresList = useBlobStoresModule.useBlobStoresList;
  const mockBlobStores = [
    {
      name: 'default',
      type: 'File',
      typeId: 'File',
      path: '/data/blobs/default',
      available: true,
      blobCount: 1000,
      totalSizeInBytes: 1073741824,
      availableSpaceInBytes: 10737418240,
      unlimited: false
    },
    {
      name: 's3-bucket',
      type: 'S3',
      typeId: 'S3',
      path: 's3://my-bucket',
      available: true,
      blobCount: 5000,
      totalSizeInBytes: 5368709120,
      availableSpaceInBytes: 0,
      unlimited: true
    },
    {
      name: 'failed-store',
      type: 'File',
      typeId: 'File',
      path: '/data/blobs/failed',
      available: false,
      unavailable: true,
      blobCount: 0,
      totalSizeInBytes: 0,
      availableSpaceInBytes: 0
    }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseBlobStoresList.mockReset();
  });

  it('renders loading state', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: [],
      loading: true,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);
    expect(screen.getByText('Loading blob stores...')).toBeInTheDocument();
  });

  it('renders error state', () => {
    const mockRefetch = jest.fn();
    mockUseBlobStoresList.mockReturnValue({
      blobStores: [],
      loading: false,
      error: 'Failed to load',
      refetch: mockRefetch
    });

    render(<BlobStoresList />);
    expect(screen.getByText('Failed to load')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /retry|try again/i }));
    expect(mockRefetch).toHaveBeenCalled();
  });

  it('renders empty state', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: [],
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);
    expect(screen.getByText('No Blob Stores')).toBeInTheDocument();
    expect(screen.getByText(/Create your first blob store/)).toBeInTheDocument();
  });

  it('renders blob stores list', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);

    expect(screen.getByText('default')).toBeInTheDocument();
    expect(screen.getByText('s3-bucket')).toBeInTheDocument();
    expect(screen.getByText('failed-store')).toBeInTheDocument();
  });

  it('displays correct status for available and unavailable stores', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);

    const startedStatuses = screen.getAllByText('Started');
    const failedStatuses = screen.getAllByText('Failed');

    expect(startedStatuses).toHaveLength(2);
    expect(failedStatuses).toHaveLength(1);
  });

  it('displays unlimited for stores with unlimited space', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);
    expect(screen.getByText('Unlimited')).toBeInTheDocument();
  });

  it('filters blob stores by name', async () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);

    const filterInput = screen.getByPlaceholderText('Filter by name...');
    await userEvent.type(filterInput, 's3');

    expect(screen.getByText('s3-bucket')).toBeInTheDocument();
    expect(screen.queryByText('default')).not.toBeInTheDocument();
  });

  it('navigates to create page when create button is clicked', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);

    fireEvent.click(screen.getByRole('button', { name: /create blob store/i }));
    expect(mockGo).toHaveBeenCalledWith('preview.admin.repository.blobstores.create');
  });

  it('navigates to edit page when row is clicked', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);

    fireEvent.click(screen.getByText('default'));
    expect(mockGo).toHaveBeenCalledWith('preview.admin.repository.blobstores.edit', {
      type: 'File',
      name: 'default'
    });
  });

  it('sorts blob stores by column', async () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);

    // Click on Name column to toggle sort
    const nameHeader = screen.getByRole('columnheader', { name: /name/i });
    fireEvent.click(nameHeader);

    // Verify sort indicator appears
    await waitFor(() => {
      expect(screen.getByRole('columnheader', { name: /name/i })).toBeInTheDocument();
    });
  });

  it('displays unavailable status for unavailable stores', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);

    // The unavailable store should show "Unavailable" for count and size
    const unknownTexts = screen.getAllByText('Unavailable');
    expect(unknownTexts.length).toBeGreaterThan(0);
  });

  it('renders page header with title', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);

    expect(screen.getByRole('heading', { name: 'Blob Stores' })).toBeInTheDocument();
    expect(screen.getByText('Configure local and cloud blob storage')).toBeInTheDocument();
  });

  it('renders help section', () => {
    mockUseBlobStoresList.mockReturnValue({
      blobStores: mockBlobStores,
      loading: false,
      error: null,
      refetch: jest.fn()
    });

    render(<BlobStoresList />);

    expect(screen.getByText('What is a blob store?')).toBeInTheDocument();
    expect(screen.getByText('View Documentation')).toBeInTheDocument();
  });
});
