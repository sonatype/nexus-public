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
import { DockerSearchPage } from '../DockerSearchPage';

// Mock the useDockerSearch hook
jest.mock('../useDockerSearch', () => ({
  useDockerSearch: () => ({
    state: {
      loading: false,
      error: undefined,
      results: [
        {
          id: 'docker:nginx',
          imageName: 'library/nginx',
          displayName: 'nginx',
          latestTag: 'latest',
          tagsCount: 50,
          size: 142 * 1024 * 1024,
          lastUpdated: '2024-01-01T00:00:00Z',
        },
        {
          id: 'docker:alpine',
          imageName: 'library/alpine',
          displayName: 'alpine',
          latestTag: '3.19',
          tagsCount: 30,
          size: 7.8 * 1024 * 1024,
          lastUpdated: '2024-01-01T00:00:00Z',
        },
      ],
      totalCount: 2,
    },
    search: jest.fn(),
    loadMore: jest.fn(),
    clear: jest.fn(),
    hasMore: false,
  }),
}));

describe('DockerSearchPage', () => {
  it('renders the Docker search page title', () => {
    render(<DockerSearchPage />);
    expect(screen.getByText(/Docker Search/i)).toBeInTheDocument();
  });

  it('displays search results', async () => {
    render(<DockerSearchPage />);
    
    await waitFor(() => {
      expect(screen.getByText('library/nginx')).toBeInTheDocument();
      expect(screen.getByText('library/alpine')).toBeInTheDocument();
    });
  });

  it('shows tag information in results', async () => {
    render(<DockerSearchPage />);
    
    await waitFor(() => {
      expect(screen.getByText('latest')).toBeInTheDocument();
      expect(screen.getByText('3.19')).toBeInTheDocument();
    });
  });

  it('shows image sizes', async () => {
    render(<DockerSearchPage />);
    
    await waitFor(() => {
      expect(screen.getByText('142.0 MB')).toBeInTheDocument();
      expect(screen.getByText('7.8 MB')).toBeInTheDocument();
    });
  });

  it('renders search input', () => {
    render(<DockerSearchPage />);
    
    const searchInput = screen.getByPlaceholderText(/Search Docker images/i);
    expect(searchInput).toBeInTheDocument();
  });

  it('calls onNavigateToDetail when row is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    render(<DockerSearchPage onNavigateToDetail={onNavigateToDetail} />);
    
    await waitFor(() => {
      const nginxRow = screen.getByText('library/nginx').closest('tr');
      if (nginxRow) {
        fireEvent.click(nginxRow);
      }
    });

    expect(onNavigateToDetail).toHaveBeenCalledWith('docker:nginx');
  });
});


