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
import { Package, Plus } from 'lucide-react';
import { EmptyState } from '../EmptyState';

const renderWithTheme = (ui: React.ReactElement) => {
  return render(<Theme>{ui}</Theme>);
};

describe('EmptyState', () => {
  it('renders title and description', () => {
    renderWithTheme(
      <EmptyState
        icon={Package}
        title="No Repositories"
        description="Create your first repository to get started."
      />
    );

    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    expect(screen.getByText('No Repositories')).toBeInTheDocument();
    expect(screen.getByText('Create your first repository to get started.')).toBeInTheDocument();
  });

  it('renders primary action button', () => {
    const mockOnClick = jest.fn();

    renderWithTheme(
      <EmptyState
        icon={Package}
        title="No Repositories"
        description="Create your first repository."
        action={{
          label: 'Create Repository',
          onClick: mockOnClick,
          icon: Plus,
        }}
      />
    );

    const button = screen.getByRole('button', { name: /create repository/i });
    expect(button).toBeInTheDocument();

    fireEvent.click(button);
    expect(mockOnClick).toHaveBeenCalled();
  });

  it('renders secondary action link', () => {
    renderWithTheme(
      <EmptyState
        icon={Package}
        title="No Repositories"
        description="Create your first repository."
        secondaryAction={{
          label: 'Learn more',
          href: 'https://help.sonatype.com',
        }}
      />
    );

    const link = screen.getByRole('link', { name: /learn more/i });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute('href', 'https://help.sonatype.com');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('renders tip text', () => {
    renderWithTheme(
      <EmptyState
        icon={Package}
        title="No Repositories"
        description="Create your first repository."
        tip="Start with a proxy repository to cache Maven Central."
      />
    );

    expect(screen.getByText(/Start with a proxy repository/)).toBeInTheDocument();
  });

  it('applies small size class', () => {
    const { container } = renderWithTheme(
      <EmptyState
        icon={Package}
        title="No Items"
        description="No items found."
        size="small"
      />
    );

    expect(container.querySelector('.empty-state--small')).toBeInTheDocument();
  });

  it('applies large size class', () => {
    const { container } = renderWithTheme(
      <EmptyState
        icon={Package}
        title="No Items"
        description="No items found."
        size="large"
      />
    );

    expect(container.querySelector('.empty-state--large')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = renderWithTheme(
      <EmptyState
        icon={Package}
        title="No Items"
        description="No items found."
        className="custom-class"
      />
    );

    expect(container.querySelector('.custom-class')).toBeInTheDocument();
  });
});


