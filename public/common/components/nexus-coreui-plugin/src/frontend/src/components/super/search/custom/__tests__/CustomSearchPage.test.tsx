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
import { Theme } from '@radix-ui/themes';
import { CustomSearchPage } from '../CustomSearchPage';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('CustomSearchPage', () => {
  it('renders the page title and subtitle', () => {
    renderWithTheme(<CustomSearchPage />);

    expect(screen.getByText('Custom Search')).toBeInTheDocument();
    expect(
      screen.getByText('Build custom search queries with multiple filter criteria')
    ).toBeInTheDocument();
  });

  it('renders the search builder with initial filter', () => {
    renderWithTheme(<CustomSearchPage />);

    expect(screen.getByText('Search Criteria')).toBeInTheDocument();
    expect(screen.getByText('Add Filter')).toBeInTheDocument();
    expect(screen.getByText('Search')).toBeInTheDocument();
  });

  it('shows initial state message before search', () => {
    renderWithTheme(<CustomSearchPage />);

    expect(screen.getByText('Build Your Search')).toBeInTheDocument();
    expect(
      screen.getByText('Add filter criteria above and click Search to find components.')
    ).toBeInTheDocument();
  });

  it('allows adding new filter rows', () => {
    renderWithTheme(<CustomSearchPage />);

    const addButton = screen.getByText('Add Filter');
    fireEvent.click(addButton);

    // Should now have 2 filter rows (2 remove buttons)
    const removeButtons = screen.getAllByLabelText('Remove filter');
    expect(removeButtons.length).toBe(2);
  });

  it('allows removing filter rows', () => {
    renderWithTheme(<CustomSearchPage />);

    // Add a second filter
    const addButton = screen.getByText('Add Filter');
    fireEvent.click(addButton);

    // Remove the first filter
    const removeButtons = screen.getAllByLabelText('Remove filter');
    fireEvent.click(removeButtons[0]);

    // Should be back to 1 filter row
    const remainingRemoveButtons = screen.getAllByLabelText('Remove filter');
    expect(remainingRemoveButtons.length).toBe(1);
  });

  it('keeps at least one filter row', () => {
    renderWithTheme(<CustomSearchPage />);

    // Try to remove the only filter - should be disabled
    const removeButton = screen.getByLabelText('Remove filter');
    expect(removeButton).toBeDisabled();
  });

  it('executes search when Search button is clicked', async () => {
    renderWithTheme(<CustomSearchPage />);

    // Enter a filter value
    const valueInput = screen.getByPlaceholderText(/spring-boot/i);
    fireEvent.change(valueInput, { target: { value: 'spring' } });

    // Click search
    const searchButton = screen.getByText('Search');
    fireEvent.click(searchButton);

    // Should show loading then results
    await waitFor(() => {
      expect(screen.queryByText('Build Your Search')).not.toBeInTheDocument();
    });
  });

  it('clears filters when Clear All is clicked', async () => {
    renderWithTheme(<CustomSearchPage />);

    // Enter a filter value
    const valueInput = screen.getByPlaceholderText(/spring-boot/i);
    fireEvent.change(valueInput, { target: { value: 'test' } });

    // Verify the value was set
    expect(valueInput).toHaveValue('test');

    // Execute search first to show Clear All button
    const searchButton = screen.getByText('Search');
    fireEvent.click(searchButton);

    // Wait for search to complete (loading becomes false) - Clear All should be enabled
    await waitFor(() => {
      const clearAllButton = screen.getByText('Clear All');
      expect(clearAllButton).not.toBeDisabled();
    });

    // Click Clear All
    fireEvent.click(screen.getByText('Clear All'));

    // After clear, the initial state message should reappear
    await waitFor(() => {
      expect(screen.getByText('Build Your Search')).toBeInTheDocument();
    });

    // Filter value should be cleared
    const clearedInput = screen.getByPlaceholderText(/spring-boot/i);
    expect(clearedInput).toHaveValue('');
  });
});

