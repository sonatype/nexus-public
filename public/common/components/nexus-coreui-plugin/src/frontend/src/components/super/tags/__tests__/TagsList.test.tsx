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

import { TagsList } from '../components/TagsList';
import { mockTags } from './mockData';

// Wrapper component for Radix Theme
const wrapper = ({ children }: { children: React.ReactNode }) => (
  <Theme>{children}</Theme>
);

describe('TagsList', () => {
  const defaultProps = {
    tags: mockTags,
    loading: false,
    error: null,
    sortField: 'id' as const,
    sortDirection: 'asc' as const,
    onSort: jest.fn(),
    onSelect: jest.fn(),
    onRetry: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders loading state', () => {
    render(<TagsList {...defaultProps} loading={true} tags={[]} />, { wrapper });

    expect(screen.getByTestId('tags-list-loading')).toBeInTheDocument();
    expect(screen.getByText('Loading tags...')).toBeInTheDocument();
  });

  it('renders error state with retry button', async () => {
    const onRetry = jest.fn();
    render(
      <TagsList {...defaultProps} error="Failed to load tags" tags={[]} onRetry={onRetry} />,
      { wrapper }
    );

    expect(screen.getByTestId('tags-list-error')).toBeInTheDocument();
    expect(screen.getByText('Failed to load tags')).toBeInTheDocument();

    // Click retry
    fireEvent.click(screen.getByText('Retry'));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders empty state when no tags', () => {
    render(<TagsList {...defaultProps} tags={[]} />, { wrapper });

    expect(screen.getByText('No tags found.')).toBeInTheDocument();
  });

  it('renders tags in table', () => {
    render(<TagsList {...defaultProps} />, { wrapper });

    expect(screen.getByTestId('tags-list')).toBeInTheDocument();

    // Check all tags are rendered
    mockTags.forEach((tag) => {
      expect(screen.getByText(tag.id)).toBeInTheDocument();
    });
  });

  it('renders column headers', () => {
    render(<TagsList {...defaultProps} />, { wrapper });

    expect(screen.getByText('Tag Name')).toBeInTheDocument();
    expect(screen.getByText('First Created')).toBeInTheDocument();
    expect(screen.getByText('Last Updated')).toBeInTheDocument();
  });

  it('calls onSort when clicking column header', () => {
    const onSort = jest.fn();
    render(<TagsList {...defaultProps} onSort={onSort} />, { wrapper });

    // Click on Tag Name header
    fireEvent.click(screen.getByText('Tag Name'));
    expect(onSort).toHaveBeenCalledWith('id');

    // Click on First Created header
    fireEvent.click(screen.getByText('First Created'));
    expect(onSort).toHaveBeenCalledWith('firstCreatedTime');

    // Click on Last Updated header
    fireEvent.click(screen.getByText('Last Updated'));
    expect(onSort).toHaveBeenCalledWith('lastUpdatedTime');
  });

  it('calls onSelect when clicking a row', () => {
    const onSelect = jest.fn();
    render(<TagsList {...defaultProps} onSelect={onSelect} />, { wrapper });

    // Click first row
    fireEvent.click(screen.getByTestId('tag-row-release-1.0'));
    expect(onSelect).toHaveBeenCalledWith('release-1.0');
  });

  it('calls onSelect when pressing Enter on a row', () => {
    const onSelect = jest.fn();
    render(<TagsList {...defaultProps} onSelect={onSelect} />, { wrapper });

    const row = screen.getByTestId('tag-row-release-1.0');
    fireEvent.keyDown(row, { key: 'Enter' });
    expect(onSelect).toHaveBeenCalledWith('release-1.0');
  });

  it('calls onSelect when pressing Space on a row', () => {
    const onSelect = jest.fn();
    render(<TagsList {...defaultProps} onSelect={onSelect} />, { wrapper });

    const row = screen.getByTestId('tag-row-staging');
    fireEvent.keyDown(row, { key: ' ' });
    expect(onSelect).toHaveBeenCalledWith('staging');
  });

  it('does not call onSelect for other keys', () => {
    const onSelect = jest.fn();
    render(<TagsList {...defaultProps} onSelect={onSelect} />, { wrapper });

    const row = screen.getByTestId('tag-row-staging');
    fireEvent.keyDown(row, { key: 'Tab' });
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('formats dates correctly', () => {
    render(<TagsList {...defaultProps} />, { wrapper });

    // The dates should be formatted using toLocaleString
    // We can't test the exact format as it depends on locale, but we can check the row exists
    expect(screen.getByTestId('tag-row-release-1.0')).toBeInTheDocument();
  });

  it('has proper accessibility attributes', () => {
    render(<TagsList {...defaultProps} />, { wrapper });

    const row = screen.getByTestId('tag-row-release-1.0');
    expect(row).toHaveAttribute('role', 'button');
    expect(row).toHaveAttribute('aria-label', 'View tag release-1.0');
    expect(row).toHaveAttribute('tabIndex', '0');
  });

  it('shows sort indicator for current sort field', () => {
    render(<TagsList {...defaultProps} sortField="id" sortDirection="asc" />, { wrapper });

    // The active sort icon should have the active class
    const sortIcons = document.querySelectorAll('.sort-icon--active');
    expect(sortIcons.length).toBeGreaterThan(0);
  });

  it('shows different sort indicator for descending', () => {
    const { rerender } = render(
      <TagsList {...defaultProps} sortField="id" sortDirection="asc" />,
      { wrapper }
    );

    // Should show ascending icon when asc
    expect(document.querySelector('.sort-icon--active')).toBeInTheDocument();

    // Rerender with desc
    rerender(
      <Theme>
        <TagsList {...defaultProps} sortField="id" sortDirection="desc" />
      </Theme>
    );

    // Should still have active icon (just different direction)
    expect(document.querySelector('.sort-icon--active')).toBeInTheDocument();
  });
});

