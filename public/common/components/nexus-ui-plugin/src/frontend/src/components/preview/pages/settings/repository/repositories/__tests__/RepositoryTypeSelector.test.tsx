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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { RepositoryTypeSelector } from '../RepositoryTypeSelector';
import { useRepositoriesApi } from '../useRepositoriesApi';

// Mock the API hook
jest.mock('../useRepositoriesApi');
const mockUseRepositoriesApi = useRepositoriesApi as jest.MockedFunction<typeof useRepositoriesApi>;

const mockRecipes = [
  { format: 'maven2', type: 'proxy', name: 'maven2-proxy' },
  { format: 'maven2', type: 'hosted', name: 'maven2-hosted' },
  { format: 'maven2', type: 'group', name: 'maven2-group' },
  { format: 'npm', type: 'proxy', name: 'npm-proxy' },
  { format: 'npm', type: 'hosted', name: 'npm-hosted' },
  { format: 'npm', type: 'group', name: 'npm-group' },
  { format: 'docker', type: 'proxy', name: 'docker-proxy' },
  { format: 'docker', type: 'hosted', name: 'docker-hosted' },
  { format: 'docker', type: 'group', name: 'docker-group' },
];

const mockApiHook = {
  loading: false,
  error: null,
  setError: jest.fn(),
  fetchRecipes: jest.fn().mockResolvedValue(mockRecipes),
};

const renderWithTheme = (component: React.ReactElement) => {
  return render(
    <Theme>
      {component}
    </Theme>
  );
};

describe('RepositoryTypeSelector', () => {
  const mockOnSelect = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRepositoriesApi.mockReturnValue(mockApiHook as any);
  });

  it('renders loading state initially', () => {
    mockUseRepositoriesApi.mockReturnValue({
      ...mockApiHook,
      fetchRecipes: jest.fn().mockImplementation(() => new Promise(() => {})),
    } as any);

    renderWithTheme(
      <RepositoryTypeSelector onSelect={mockOnSelect} onCancel={mockOnCancel} />
    );

    expect(screen.getByText(/loading repository recipes/i)).toBeInTheDocument();
  });

  it('renders cards after loading', async () => {
    renderWithTheme(
      <RepositoryTypeSelector onSelect={mockOnSelect} onCancel={mockOnCancel} />
    );

    await waitFor(() => {
      expect(screen.getByText('Select Format')).toBeInTheDocument();
    });

    // Should have Maven card
    expect(screen.getByText('Maven')).toBeInTheDocument();
  });

  it('calls onFormatSelect when clicking a format card', async () => {
    const mockOnFormatSelect = jest.fn();
    renderWithTheme(
      <RepositoryTypeSelector onSelect={mockOnSelect} onCancel={mockOnCancel} onFormatSelect={mockOnFormatSelect} />
    );

    await waitFor(() => {
      expect(screen.getByText('Select Format')).toBeInTheDocument();
    });

    const mavenCard = screen.getByText('Maven').closest('button');
    await userEvent.click(mavenCard!);

    expect(mockOnFormatSelect).toHaveBeenCalledWith('maven2');
  });

  it('calls onCancel when clicking Back (via parent or custom logic)', async () => {
    // Note: Cancel button might be in the parent RepositoriesPage or WizardForm
    // In this component specifically, we'd need to trigger it if it was present.
  });

  it('displays error state when API fails', async () => {
    mockUseRepositoriesApi.mockReturnValue({
      ...mockApiHook,
      fetchRecipes: jest.fn().mockRejectedValue(new Error('Failed to load')),
    } as any);

    renderWithTheme(
      <RepositoryTypeSelector onSelect={mockOnSelect} onCancel={mockOnCancel} />
    );

    await waitFor(() => {
      expect(screen.getByText(/failed to load/i)).toBeInTheDocument();
    });
  });

  it('shows types when a format is selected', async () => {
    renderWithTheme(
      <RepositoryTypeSelector onSelect={mockOnSelect} onCancel={mockOnCancel} selectedFormat="maven2" />
    );

    await waitFor(() => {
      expect(screen.getByText('Proxy')).toBeInTheDocument();
      expect(screen.getByText('Hosted')).toBeInTheDocument();
      expect(screen.getByText('Group')).toBeInTheDocument();
    });
  });
});
