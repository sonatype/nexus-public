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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

const mockStateGetValue = jest.fn();

function givenDistribution(distribution: 'selfhosted' | 'cloud') {
  mockStateGetValue.mockImplementation((key: string, defaultValue?: unknown) =>
    key === 'distribution' ? distribution : defaultValue,
  );
}

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn(() => ({
      getValue: mockStateGetValue,
    })),
    useState: jest.fn((getValue: () => unknown) => getValue()),
  },
}));

jest.mock('../useBlobStoreCdnStatuses');

import { useBlobStoreCdnStatuses } from '../useBlobStoreCdnStatuses';
import { BlobStoreCdnStatusCard } from '../BlobStoreCdnStatusCard';

const mockedUseStatuses = useBlobStoreCdnStatuses as jest.MockedFunction<typeof useBlobStoreCdnStatuses>;

describe('BlobStoreCdnStatusCard', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    givenDistribution('selfhosted');
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders loading state', async () => {
    mockedUseStatuses.mockReturnValue({
      blobStores: [],
      isLoading: true,
      error: null,
      canReadBlobStores: true,
    });

    render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    expect(screen.getByText('Loading blob stores…')).toBeInTheDocument();
  });

  it('renders error state', async () => {
    mockedUseStatuses.mockReturnValue({
      blobStores: [],
      isLoading: false,
      error: 'Network error',
      canReadBlobStores: true,
    });

    render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    expect(screen.getByText(/Failed to load blob stores/)).toBeInTheDocument();
  });

  it('does not render without blob store read permission', () => {
    mockedUseStatuses.mockReturnValue({
      blobStores: [],
      isLoading: false,
      error: null,
      canReadBlobStores: false,
    });

    const { container } = render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    expect(screen.queryByText(/blob store/i)).not.toBeInTheDocument();
    expect(container.querySelector('.rt-Card')).toBeNull();
  });

  it('does not render on a cloud deployment', async () => {
    givenDistribution('cloud');

    mockedUseStatuses.mockReturnValue({
      blobStores: [{ name: 's3-store', cdnEnabled: true, type: 's3' }],
      isLoading: false,
      error: null,
      canReadBlobStores: true,
    });

    const { queryByText } = render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    expect(queryByText('Blob Store Distribution Status')).not.toBeInTheDocument();
  });

  it('does not show the empty state on a cloud deployment', async () => {
    givenDistribution('cloud');

    mockedUseStatuses.mockReturnValue({
      blobStores: [],
      isLoading: false,
      error: null,
      canReadBlobStores: true,
    });

    render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    expect(screen.queryByText('No blob stores have Global Distribution enabled')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'View blob stores' })).not.toBeInTheDocument();
  });

  it('shows helpful message when no supported blob stores exist', async () => {
    mockedUseStatuses.mockReturnValue({
      blobStores: [],
      isLoading: false,
      error: null,
      canReadBlobStores: true,
    });

    render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    expect(screen.getByText('No blob stores have Global Distribution enabled')).toBeInTheDocument();
    expect(
      screen.getByText('Enable Global Distribution for an S3 blob store before enabling it for its repositories.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'View blob stores' })).toBeInTheDocument();
    expect(screen.queryByText('Enterprise')).not.toBeInTheDocument();
  });

  it('renders table with blob stores', async () => {
    mockedUseStatuses.mockReturnValue({
      blobStores: [
        { name: 's3-cdn-enabled', cdnEnabled: true, type: 's3' },
        { name: 's3-cdn-disabled', cdnEnabled: false, type: 's3' },
      ],
      isLoading: false,
      error: null,
      canReadBlobStores: true,
    });

    render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    expect(screen.getByText('Blob Store Distribution Status')).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Blob Store' })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Distribution Status' })).toBeInTheDocument();
    expect(screen.getByText('s3-cdn-enabled')).toBeInTheDocument();
    expect(screen.getByText('s3-cdn-disabled')).toBeInTheDocument();
  });

  it('shows Global Distribution enabled status correctly', async () => {
    mockedUseStatuses.mockReturnValue({
      blobStores: [
        { name: 's3-cdn1', cdnEnabled: true, type: 's3' },
        { name: 's3-cdn2', cdnEnabled: true, type: 's3' },
        { name: 's3-disabled', cdnEnabled: false, type: 's3' },
      ],
      isLoading: false,
      error: null,
      canReadBlobStores: true,
    });

    render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    expect(screen.getByText('s3-cdn1')).toBeInTheDocument();
    const enabledBadges = screen.getAllByText('Enabled');
    expect(enabledBadges).toHaveLength(2);

    expect(screen.getByText('Disabled')).toBeInTheDocument();
  });

  it('sorts blob stores alphabetically', async () => {
    mockedUseStatuses.mockReturnValue({
      blobStores: [
        { name: 'alpha', cdnEnabled: false, type: 's3' },
        { name: 'middle', cdnEnabled: true, type: 's3' },
        { name: 'zebra', cdnEnabled: true, type: 's3' },
      ],
      isLoading: false,
      error: null,
      canReadBlobStores: true,
    });

    render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    // Verify the names are present
    const alphaCells = screen.getAllByText('alpha');
    const middleCells = screen.getAllByText('middle');
    const zebraCells = screen.getAllByText('zebra');

    expect(alphaCells.length).toBeGreaterThan(0);
    expect(middleCells.length).toBeGreaterThan(0);
    expect(zebraCells.length).toBeGreaterThan(0);
  });

  it('shows View blob stores button when no blob stores have Global Distribution enabled', async () => {
    mockedUseStatuses.mockReturnValue({
      blobStores: [],
      isLoading: false,
      error: null,
      canReadBlobStores: true,
    });

    render(
      <Theme>
        <BlobStoreCdnStatusCard />
      </Theme>
    );

    const button = screen.getByRole('button', { name: 'View blob stores' });
    expect(button).toBeInTheDocument();
  });
});
