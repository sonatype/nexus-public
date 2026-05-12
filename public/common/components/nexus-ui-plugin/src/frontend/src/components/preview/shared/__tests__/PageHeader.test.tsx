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
import { Theme, Button } from '@radix-ui/themes';
import { Plus } from 'lucide-react';
import { PageHeader } from '../PageHeader';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('PageHeader', () => {
  it('renders title', () => {
    renderWithTheme(<PageHeader title="Repositories" />);

    expect(screen.getByTestId('page-header')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Repositories' })).toBeInTheDocument();
  });

  it('renders description', () => {
    renderWithTheme(
      <PageHeader
        title="Repositories"
        description="Manage your repositories"
      />
    );

    expect(screen.getByText('Manage your repositories')).toBeInTheDocument();
  });

  it('renders action buttons', () => {
    renderWithTheme(
      <PageHeader
        title="Repositories"
        actions={
          <Button>
            <Plus size={16} /> Create
          </Button>
        }
      />
    );

    expect(screen.getByRole('button', { name: /create/i })).toBeInTheDocument();
  });

  it('renders breadcrumbs', () => {
    const mockOnClick = jest.fn();

    renderWithTheme(
      <PageHeader
        title="Blob Stores"
        breadcrumbs={[
          { label: 'Settings', onClick: mockOnClick },
          { label: 'Repository' },
          { label: 'Blob Stores' },
        ]}
      />
    );

    expect(screen.getByText('Settings')).toBeInTheDocument();
    expect(screen.getByText('Repository')).toBeInTheDocument();
    // 'Blob Stores' appears in both title and breadcrumb
    const blobStoresElements = screen.getAllByText('Blob Stores');
    expect(blobStoresElements.length).toBeGreaterThanOrEqual(2); // title + breadcrumb
  });

  it('makes breadcrumb clickable when onClick is provided', () => {
    const mockOnClick = jest.fn();

    renderWithTheme(
      <PageHeader
        title="Blob Stores"
        breadcrumbs={[
          { label: 'Settings', onClick: mockOnClick },
          { label: 'Blob Stores' },
        ]}
      />
    );

    const settingsLink = screen.getByRole('button', { name: 'Settings' });
    fireEvent.click(settingsLink);

    expect(mockOnClick).toHaveBeenCalled();
  });

  it('marks last breadcrumb as current', () => {
    const { container } = renderWithTheme(
      <PageHeader
        title="Blob Stores"
        breadcrumbs={[
          { label: 'Settings' },
          { label: 'Blob Stores' },
        ]}
      />
    );

    // Find the current breadcrumb via aria-current attribute
    const currentBreadcrumb = container.querySelector('[aria-current="page"]');
    expect(currentBreadcrumb).toBeInTheDocument();
    expect(currentBreadcrumb?.textContent).toBe('Blob Stores');
  });

  it('applies custom className', () => {
    const { container } = renderWithTheme(
      <PageHeader title="Test" className="custom-class" />
    );

    expect(container.querySelector('.custom-class')).toBeInTheDocument();
  });

  it('renders breadcrumb navigation with proper aria-label', () => {
    const { container } = renderWithTheme(
      <PageHeader
        title="Blob Stores"
        breadcrumbs={[{ label: 'Settings' }, { label: 'Blob Stores' }]}
      />
    );

    const breadcrumbNav = container.querySelector('[aria-label="Breadcrumb"]');
    expect(breadcrumbNav).toBeInTheDocument();
  });
});


