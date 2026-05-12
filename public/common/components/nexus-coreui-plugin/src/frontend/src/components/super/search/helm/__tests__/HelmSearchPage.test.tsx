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
import { HelmSearchPage } from '../HelmSearchPage';

// Mock the useHelmSearch hook
jest.mock('../useHelmSearch', () => ({
  useHelmSearch: () => ({
    state: {
      loading: false,
      error: undefined,
      results: [
        {
          id: 'helm:nginx-ingress',
          name: 'nginx-ingress',
          displayName: 'nginx-ingress',
          latestVersion: '4.9.0',
          appVersion: '3.4.0',
          versionsCount: 87,
          description: 'NGINX Ingress Controller for Kubernetes',
          repositoriesCount: 2,
          lastUpdated: '2024-01-20T10:30:00Z',
        },
        {
          id: 'helm:prometheus',
          name: 'prometheus',
          displayName: 'prometheus',
          latestVersion: '25.8.2',
          appVersion: '2.48.1',
          versionsCount: 156,
          description: 'Prometheus monitoring system',
          repositoriesCount: 3,
          lastUpdated: '2024-01-18T14:22:00Z',
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

describe('HelmSearchPage', () => {
  it('renders the Helm search page title', () => {
    render(<HelmSearchPage />);
    expect(screen.getByText(/Helm Search/i)).toBeInTheDocument();
  });

  it('displays search results', async () => {
    render(<HelmSearchPage />);
    
    await waitFor(() => {
      expect(screen.getByText('nginx-ingress')).toBeInTheDocument();
      expect(screen.getByText('prometheus')).toBeInTheDocument();
    });
  });

  it('shows chart version in results', async () => {
    render(<HelmSearchPage />);
    
    await waitFor(() => {
      expect(screen.getByText('4.9.0')).toBeInTheDocument();
      expect(screen.getByText('25.8.2')).toBeInTheDocument();
    });
  });

  it('shows app version in results', async () => {
    render(<HelmSearchPage />);
    
    await waitFor(() => {
      expect(screen.getByText('3.4.0')).toBeInTheDocument();
      expect(screen.getByText('2.48.1')).toBeInTheDocument();
    });
  });

  it('calls onNavigateToDetail when row is clicked', async () => {
    const onNavigateToDetail = jest.fn();
    render(<HelmSearchPage onNavigateToDetail={onNavigateToDetail} />);
    
    await waitFor(() => {
      const nginxRow = screen.getByText('nginx-ingress').closest('tr');
      if (nginxRow) {
        fireEvent.click(nginxRow);
      }
    });
    
    expect(onNavigateToDetail).toHaveBeenCalledWith('helm:nginx-ingress');
  });

  it('renders filter inputs', () => {
    render(<HelmSearchPage />);

    fireEvent.click(screen.getByRole('button', { name: /show/i }));

    expect(screen.getByLabelText(/Chart Name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Chart Version/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/App Version/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
  });
});


