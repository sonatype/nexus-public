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
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { UploadRepositoryList } from '../components/UploadRepositoryList';
import { UPLOAD_STRINGS } from '../upload.types';
import { type UploadRepositoryListProps } from '../upload.types';

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
  },
}));

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: jest.fn().mockResolvedValue(undefined),
  },
});

/**
 * Renders the component wrapped in necessary providers.
 */
function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('UploadRepositoryList', () => {
  const mockRepositories = [
    { name: 'maven-releases', format: 'maven2', url: 'http://localhost:8081/repository/maven-releases/' },
    { name: 'npm-hosted', format: 'npm', url: 'http://localhost:8081/repository/npm-hosted/' },
    { name: 'docker-hosted', format: 'docker', url: 'http://localhost:8081/repository/docker-hosted/' },
  ];

  const defaultProps: UploadRepositoryListProps = {
    repositories: mockRepositories,
    loading: false,
    error: null,
    sortColumn: null,
    sortDirection: null,
    onSort: jest.fn(),
    onSelect: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('renders all repositories', () => {
      renderWithTheme(<UploadRepositoryList {...defaultProps} />);

      expect(screen.getByText('maven-releases')).toBeInTheDocument();
      expect(screen.getByText('npm-hosted')).toBeInTheDocument();
      expect(screen.getByText('docker-hosted')).toBeInTheDocument();
    });

    it('renders column headers', () => {
      renderWithTheme(<UploadRepositoryList {...defaultProps} />);

      expect(screen.getByText(UPLOAD_STRINGS.columns.name)).toBeInTheDocument();
      expect(screen.getByText(UPLOAD_STRINGS.columns.format)).toBeInTheDocument();
      expect(screen.getByText(UPLOAD_STRINGS.columns.url)).toBeInTheDocument();
    });

    it('renders format for each repository', () => {
      renderWithTheme(<UploadRepositoryList {...defaultProps} />);

      expect(screen.getByText('Maven')).toBeInTheDocument();
      expect(screen.getByText('npm')).toBeInTheDocument();
      expect(screen.getByText('Docker')).toBeInTheDocument();
    });
  });

  describe('Loading State', () => {
    it('displays loading message when loading', () => {
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} repositories={[]} loading={true} />
      );

      expect(screen.getByText(UPLOAD_STRINGS.loadingMessage)).toBeInTheDocument();
    });

    it('does not display repositories when loading', () => {
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} loading={true} />
      );

      expect(screen.queryByText('maven-releases')).not.toBeInTheDocument();
    });
  });

  describe('Error State', () => {
    it('displays error message when there is an error', () => {
      const errorMessage = 'Failed to load';
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} repositories={[]} error={errorMessage} />
      );

      expect(screen.getByText(new RegExp(errorMessage))).toBeInTheDocument();
    });

    it('displays error prefix with error message', () => {
      const errorMessage = 'Network error';
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} repositories={[]} error={errorMessage} />
      );

      expect(screen.getByText(new RegExp(UPLOAD_STRINGS.errorPrefix))).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('displays empty message when no repositories', () => {
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} repositories={[]} />
      );

      expect(screen.getByText(UPLOAD_STRINGS.emptyMessage)).toBeInTheDocument();
    });
  });

  describe('Sorting', () => {
    it('calls onSort with "name" when clicking name header', async () => {
      const mockOnSort = jest.fn();
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} onSort={mockOnSort} />
      );

      const nameHeader = screen.getByText(UPLOAD_STRINGS.columns.name);
      await userEvent.click(nameHeader);

      expect(mockOnSort).toHaveBeenCalledWith('name');
    });

    it('calls onSort with "format" when clicking format header', async () => {
      const mockOnSort = jest.fn();
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} onSort={mockOnSort} />
      );

      const formatHeader = screen.getByText(UPLOAD_STRINGS.columns.format);
      await userEvent.click(formatHeader);

      expect(mockOnSort).toHaveBeenCalledWith('format');
    });

    it('shows ascending sort indicator when sortColumn matches and direction is asc', () => {
      renderWithTheme(
        <UploadRepositoryList
          {...defaultProps}
          sortColumn="name"
          sortDirection="asc"
        />
      );

      // The ChevronUp icon should be rendered
      const sortIcons = document.querySelectorAll('.upload-repository-list__sort-icon');
      expect(sortIcons.length).toBeGreaterThan(0);
    });

    it('shows descending sort indicator when sortColumn matches and direction is desc', () => {
      renderWithTheme(
        <UploadRepositoryList
          {...defaultProps}
          sortColumn="name"
          sortDirection="desc"
        />
      );

      // The ChevronDown icon should be rendered
      const sortIcons = document.querySelectorAll('.upload-repository-list__sort-icon');
      expect(sortIcons.length).toBeGreaterThan(0);
    });
  });

  describe('Row Selection', () => {
    it('calls onSelect with repository name when row is clicked', async () => {
      const mockOnSelect = jest.fn();
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} onSelect={mockOnSelect} />
      );

      const mavenRow = screen.getByText('maven-releases').closest('tr');
      expect(mavenRow).not.toBeNull();

      if (mavenRow) {
        await userEvent.click(mavenRow);
      }

      expect(mockOnSelect).toHaveBeenCalledWith('maven-releases');
    });

    it('calls onSelect when pressing Enter on focused row', () => {
      const mockOnSelect = jest.fn();
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} onSelect={mockOnSelect} />
      );

      const mavenRow = screen.getByText('maven-releases').closest('tr');
      expect(mavenRow).not.toBeNull();

      if (mavenRow) {
        fireEvent.keyDown(mavenRow, { key: 'Enter', code: 'Enter' });
      }

      expect(mockOnSelect).toHaveBeenCalledWith('maven-releases');
    });

    it('calls onSelect when pressing Space on focused row', () => {
      const mockOnSelect = jest.fn();
      renderWithTheme(
        <UploadRepositoryList {...defaultProps} onSelect={mockOnSelect} />
      );

      const mavenRow = screen.getByText('maven-releases').closest('tr');
      expect(mavenRow).not.toBeNull();

      if (mavenRow) {
        fireEvent.keyDown(mavenRow, { key: ' ', code: 'Space' });
      }

      expect(mockOnSelect).toHaveBeenCalledWith('maven-releases');
    });
  });

  describe('Copy URL', () => {
    it('renders copy URL button for each repository', () => {
      renderWithTheme(<UploadRepositoryList {...defaultProps} />);

      // Should have 3 copy buttons (one for each repository)
      const copyButtons = screen.getAllByLabelText(UPLOAD_STRINGS.copyUrlTooltip);
      expect(copyButtons).toHaveLength(3);
    });
  });

  describe('Accessibility', () => {
    it('rows are focusable with tabIndex', () => {
      renderWithTheme(<UploadRepositoryList {...defaultProps} />);

      // Get all table rows (they have role="button" but are tr elements)
      const rows = screen.getAllByLabelText(/Upload to/);
      rows.forEach((row) => {
        expect(row).toHaveAttribute('tabindex', '0');
      });
    });

    it('rows have correct aria-label', () => {
      renderWithTheme(<UploadRepositoryList {...defaultProps} />);

      expect(screen.getByLabelText('Upload to maven-releases')).toBeInTheDocument();
      expect(screen.getByLabelText('Upload to npm-hosted')).toBeInTheDocument();
      expect(screen.getByLabelText('Upload to docker-hosted')).toBeInTheDocument();
    });

    it('sort headers have appropriate aria-label', () => {
      renderWithTheme(
        <UploadRepositoryList
          {...defaultProps}
          sortColumn="name"
          sortDirection="asc"
        />
      );

      const nameHeader = screen.getByText(UPLOAD_STRINGS.columns.name).closest('th');
      expect(nameHeader).toHaveAttribute(
        'aria-label',
        expect.stringContaining('ascending')
      );
    });
  });
});

