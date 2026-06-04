/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 */

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { ArtifactTable } from '../ArtifactTable';

describe('ArtifactTable', () => {
  const defaultProps = {
    items: [],
    loading: false,
    error: null,
    hasMore: false,
    onLoadMore: jest.fn(),
  };

  it('shows loading state when loading and no items', () => {
    render(<ArtifactTable {...defaultProps} loading={true} />);
    expect(screen.getByText(/Loading artifacts/)).toBeInTheDocument();
  });

  it('shows error message when error is set', () => {
    render(<ArtifactTable {...defaultProps} error="Failed to load" />);
    expect(screen.getByText('Failed to load')).toBeInTheDocument();
  });

  it('shows empty state when no items', () => {
    render(<ArtifactTable {...defaultProps} />);
    expect(screen.getByText(/No artifacts found/)).toBeInTheDocument();
  });

  it('renders artifact rows when items are provided', () => {
    const items = [
      { id: '1', group: 'org.example', name: 'my-lib', version: '1.0.0', format: 'maven2', criticalCount: 1 },
    ];
    render(<ArtifactTable {...defaultProps} items={items} />);
    expect(screen.getByText('org.example')).toBeInTheDocument();
    expect(screen.getByText('my-lib')).toBeInTheDocument();
    expect(screen.getByText('1.0.0')).toBeInTheDocument();
    expect(screen.getByText('maven2')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('shows Load more button when hasMore is true', () => {
    const items = [{ id: '1', name: 'foo', version: '1.0' }];
    render(<ArtifactTable {...defaultProps} items={items} hasMore={true} />);
    expect(screen.getByRole('button', { name: /Load more/i })).toBeInTheDocument();
  });

  it('calls onLoadMore when Load more is clicked', () => {
    const onLoadMore = jest.fn();
    const items = [{ id: '1', name: 'foo', version: '1.0' }];
    render(<ArtifactTable {...defaultProps} items={items} hasMore={true} onLoadMore={onLoadMore} />);
    fireEvent.click(screen.getByRole('button', { name: /Load more/i }));
    expect(onLoadMore).toHaveBeenCalled();
  });
});
