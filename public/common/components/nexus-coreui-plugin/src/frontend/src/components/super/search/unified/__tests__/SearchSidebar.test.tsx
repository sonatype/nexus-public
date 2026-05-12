/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { SearchSidebar } from '../SearchSidebar';

function renderWithTheme(ui) {
  return render(<Theme>{ui}</Theme>);
}

describe('SearchSidebar', () => {
  const defaultProps = {
    selectedFormat: '' as const,
    onFormatChange: jest.fn(),
    filters: {},
    onFilterChange: jest.fn(),
    onReset: jest.fn(),
    repositories: ['repo1', 'repo2', 'maven-central'],
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('reset button', () => {
    it('renders Reset filters button', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      expect(screen.getByText('Reset filters')).toBeInTheDocument();
    });

    it('calls onReset and onFormatChange when Reset filters is clicked', async () => {
      const onReset = jest.fn();
      const onFormatChange = jest.fn();
      renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          onFormatChange={onFormatChange}
          onReset={onReset}
        />,
      );
      await userEvent.click(screen.getByText('Reset filters'));
      expect(onFormatChange).toHaveBeenCalledWith('');
      expect(onReset).toHaveBeenCalled();
    });
  });

  describe('format section', () => {
    it('renders Format section title', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      expect(screen.getByText('Format')).toBeInTheDocument();
    });

    it('renders format dropdown with All formats default', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      const trigger = screen.getByTestId('format-dropdown-trigger');
      expect(trigger).toHaveTextContent('All formats');
      expect(trigger).toHaveAttribute('aria-label', 'Format: All formats');
    });

    it('shows selected format in dropdown trigger', () => {
      renderWithTheme(
        <SearchSidebar {...defaultProps} selectedFormat="maven" />,
      );
      const trigger = screen.getByTestId('format-dropdown-trigger');
      expect(trigger).toHaveTextContent('Maven');
    });

    it('shows format-specific filters when format is selected', () => {
      renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          selectedFormat="maven"
          filters={{}}
        />,
      );
      expect(screen.getByText('Filters')).toBeInTheDocument();
      expect(screen.getByLabelText(/Group ID/i)).toBeInTheDocument();
    });
  });

  describe('repository section', () => {
    it('renders Repository section title', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      expect(screen.getByText('Repository')).toBeInTheDocument();
    });

    it('renders repository dropdown with All repositories default', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} />);
      const trigger = screen.getByTestId('repository-dropdown-trigger');
      expect(trigger).toHaveTextContent('All repositories');
      expect(trigger).toHaveAttribute(
        'aria-label',
        'Repository: All repositories',
      );
    });

    it('shows selected repository in trigger when filter is set', () => {
      renderWithTheme(
        <SearchSidebar
          {...defaultProps}
          filters={{ repository: 'maven-central' }}
        />,
      );
      const trigger = screen.getByTestId('repository-dropdown-trigger');
      expect(trigger).toHaveTextContent('maven-central');
    });
  });

  describe('disabled state', () => {
    it('disables Reset filters button when disabled', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} disabled={true} />);
      expect(screen.getByText('Reset filters').closest('button')).toBeDisabled();
    });

    it('disables format dropdown when disabled', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} disabled={true} />);
      expect(screen.getByTestId('format-dropdown-trigger')).toBeDisabled();
    });

    it('disables repository dropdown when disabled', () => {
      renderWithTheme(<SearchSidebar {...defaultProps} disabled={true} />);
      expect(screen.getByTestId('repository-dropdown-trigger')).toBeDisabled();
    });
  });
});
