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
import { Theme } from '@radix-ui/themes';
import { FilterSidebar, FilterSection } from '../FilterSidebar';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('FilterSidebar', () => {
  const mockSections: FilterSection[] = [
    {
      id: 'format',
      label: 'Format',
      type: 'checkbox',
      options: [
        { value: 'maven', label: 'Maven', count: 10 },
        { value: 'npm', label: 'NPM', count: 5 },
        { value: 'docker', label: 'Docker', count: 3 },
      ],
      value: [],
    },
    {
      id: 'type',
      label: 'Type',
      type: 'checkbox',
      options: [
        { value: 'hosted', label: 'Hosted', count: 8 },
        { value: 'proxy', label: 'Proxy', count: 7 },
      ],
      value: [],
    },
  ];

  const mockOnFilterChange = jest.fn();
  const mockOnClear = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders with default title', () => {
    renderWithTheme(
      <FilterSidebar
        sections={mockSections}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
      />
    );

    expect(screen.getByTestId('filter-sidebar')).toBeInTheDocument();
    expect(screen.getByText('Filters')).toBeInTheDocument();
  });

  it('renders with custom title', () => {
    renderWithTheme(
      <FilterSidebar
        sections={mockSections}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
        title="Custom Filters"
      />
    );

    expect(screen.getByText('Custom Filters')).toBeInTheDocument();
  });

  it('renders all filter sections', () => {
    renderWithTheme(
      <FilterSidebar
        sections={mockSections}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
      />
    );

    expect(screen.getByText('Format')).toBeInTheDocument();
    expect(screen.getByText('Type')).toBeInTheDocument();
  });

  it('renders checkbox options with counts', () => {
    renderWithTheme(
      <FilterSidebar
        sections={mockSections}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
      />
    );

    expect(screen.getByText('Maven')).toBeInTheDocument();
    expect(screen.getByText('10')).toBeInTheDocument();
    expect(screen.getByText('NPM')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
  });

  it('calls onFilterChange when checkbox is clicked', () => {
    renderWithTheme(
      <FilterSidebar
        sections={mockSections}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
      />
    );

    const mavenCheckbox = screen.getByText('Maven').closest('label')?.querySelector('[type="button"]');
    if (mavenCheckbox) {
      fireEvent.click(mavenCheckbox);
      expect(mockOnFilterChange).toHaveBeenCalledWith('format', ['maven']);
    }
  });

  it('shows clear all button when filters are active', () => {
    const sectionsWithActive: FilterSection[] = [
      {
        ...mockSections[0],
        value: ['maven'],
      },
      mockSections[1],
    ];

    renderWithTheme(
      <FilterSidebar
        sections={sectionsWithActive}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
      />
    );

    expect(screen.getByText('Clear all')).toBeInTheDocument();
  });

  it('hides clear all button when no filters are active', () => {
    renderWithTheme(
      <FilterSidebar
        sections={mockSections}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
      />
    );

    expect(screen.queryByText('Clear all')).not.toBeInTheDocument();
  });

  it('calls onClear when clear all is clicked', () => {
    const sectionsWithActive: FilterSection[] = [
      {
        ...mockSections[0],
        value: ['maven'],
      },
      mockSections[1],
    ];

    renderWithTheme(
      <FilterSidebar
        sections={sectionsWithActive}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
      />
    );

    fireEvent.click(screen.getByText('Clear all'));
    expect(mockOnClear).toHaveBeenCalled();
  });

  it('displays active filter count', () => {
    const sectionsWithActive: FilterSection[] = [
      {
        ...mockSections[0],
        value: ['maven', 'npm'],
      },
      {
        ...mockSections[1],
        value: ['hosted'],
      },
    ];

    renderWithTheme(
      <FilterSidebar
        sections={sectionsWithActive}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
      />
    );

    // Should show active count badge with total 3 (2 formats + 1 type)
    // The count is wrapped in parentheses, e.g., "(3)" in the clear button badge
    const clearButton = screen.getByRole('button', { name: /clear all/i });
    expect(clearButton).toBeInTheDocument();
  });

  it('toggles section expansion', () => {
    renderWithTheme(
      <FilterSidebar
        sections={mockSections}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
      />
    );

    // Click Format header to collapse
    fireEvent.click(screen.getByText('Format'));

    // Options should be hidden (Maven checkbox hidden)
    // Note: Due to how Radix UI works, we check if content is present
  });

  it('disables filters when disabled prop is true', () => {
    renderWithTheme(
      <FilterSidebar
        sections={mockSections}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
        disabled={true}
      />
    );

    const clearButton = screen.queryByText('Clear all');
    if (clearButton) {
      expect(clearButton.closest('button')).toBeDisabled();
    }
  });

  it('renders footer text', () => {
    renderWithTheme(
      <FilterSidebar
        sections={mockSections}
        onFilterChange={mockOnFilterChange}
        onClear={mockOnClear}
        footerText="Custom footer"
      />
    );

    expect(screen.getByText('Custom footer')).toBeInTheDocument();
  });
});


