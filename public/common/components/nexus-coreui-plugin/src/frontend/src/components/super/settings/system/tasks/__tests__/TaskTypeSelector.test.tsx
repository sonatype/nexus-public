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
import { TaskTypeSelector } from '../TaskTypeSelector';
import { TaskType } from '../types';

jest.mock('@/utils/api', () => ({
  restClient: { get: jest.fn().mockResolvedValue([]) },
}));

jest.mock('../../../../shared/form', () => ({
  SettingsSelect: jest.fn(() => null),
  SettingsCombobox: jest.fn(() => null),
}));

jest.mock('@/components/super/settings/repository/repositories/components/FormatIcon', () => ({
  FormatIcon: ({ format }: { format: string }) => <span data-testid={`format-icon-${format}`} />,
}));

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('TaskTypeSelector', () => {
  const mockTaskTypes: TaskType[] = [
    { id: 'repository.cleanup', name: 'Cleanup repositories', exposed: true, formFields: [] },
    { id: 'repository.rebuild-index', name: 'Rebuild repository index', exposed: true, formFields: [] },
    { id: 'db.backup', name: 'Database backup', exposed: true, formFields: [] },
    { id: 'blobstore.compact', name: 'Compact blob store', exposed: true, formFields: [] },
    { id: 'repository.maven.rebuild-metadata', name: 'Rebuild Maven metadata', exposed: true, formFields: [] },
    { id: 'repository.docker.gc', name: 'Docker garbage collection', exposed: false, formFields: [] },
    { id: 'assetBlob.cleanup', name: 'Cleanup asset blobs', exposed: true, formFields: [] },
  ];

  const mockOnSelect = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('category stage', () => {
    it('renders category search input', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      expect(screen.getByPlaceholderText('Search task category...')).toBeInTheDocument();
    });

    it('renders category cards', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      expect(screen.getByText('Admin')).toBeInTheDocument();
      expect(screen.getByText('Cleanup')).toBeInTheDocument();
      expect(screen.getByText('Docker')).toBeInTheDocument();
      expect(screen.getByText('Maven')).toBeInTheDocument();
      expect(screen.getByText('Repository')).toBeInTheDocument();
    });

    it('shows category count', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      expect(screen.getByText('5 categories available')).toBeInTheDocument();
    });

    it('filters categories by search term', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const searchInput = screen.getByPlaceholderText('Search task category...');
      await userEvent.type(searchInput, 'Maven');

      await waitFor(() => {
        expect(screen.getByText('Maven')).toBeInTheDocument();
        expect(screen.queryByText('Admin')).not.toBeInTheDocument();
      });
    });

    it('is case insensitive when filtering', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      const searchInput = screen.getByPlaceholderText('Search task category...');
      await userEvent.type(searchInput, 'docker');

      await waitFor(() => {
        expect(screen.getByText('Docker')).toBeInTheDocument();
      });
    });
  });

  describe('type stage', () => {
    it('shows types after clicking a category', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      await userEvent.click(screen.getByText('Admin'));

      await waitFor(() => {
        expect(screen.getByText('Database backup')).toBeInTheDocument();
        expect(screen.getByText('Compact blob store')).toBeInTheDocument();
      });
    });

    it('shows back link when in type stage', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      await userEvent.click(screen.getByText('Admin'));

      await waitFor(() => {
        expect(screen.getByText(/Back to categories/)).toBeInTheDocument();
      });
    });

    it('returns to categories when back link is clicked', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      await userEvent.click(screen.getByText('Admin'));

      await waitFor(() => {
        expect(screen.getByText('Database backup')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByText(/Back to categories/));

      await waitFor(() => {
        expect(screen.getByPlaceholderText('Search task category...')).toBeInTheDocument();
      });
    });

    it('shows PRO badge for non-exposed types', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      await userEvent.click(screen.getByText('Docker'));

      await waitFor(() => {
        expect(screen.getByText('Docker garbage collection')).toBeInTheDocument();
        expect(screen.getByText('PRO')).toBeInTheDocument();
      });
    });

    it('shows type descriptions', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      await userEvent.click(screen.getByText('Admin'));

      await waitFor(() => {
        expect(screen.getByText(/Creates a backup of the embedded database/)).toBeInTheDocument();
      });
    });

    it('filters types by search within category', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      await userEvent.click(screen.getByText('Admin'));

      await waitFor(() => {
        expect(screen.getByText('Database backup')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText('Search Admin tasks...');
      await userEvent.type(searchInput, 'backup');

      await waitFor(() => {
        expect(screen.getByText('Database backup')).toBeInTheDocument();
        expect(screen.queryByText('Compact blob store')).not.toBeInTheDocument();
      });
    });

    it('calls onSelect when a type is clicked in uncontrolled mode', async () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={mockTaskTypes} onSelect={mockOnSelect} />
      );

      await userEvent.click(screen.getByText('Admin'));

      await waitFor(() => {
        expect(screen.getByText('Database backup')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByText('Database backup'));

      expect(mockOnSelect).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'db.backup',
          name: 'Database backup',
        })
      );
    });
  });

  describe('loading state', () => {
    it('shows loading indicator when loading', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={[]} onSelect={mockOnSelect} loading={true} />
      );

      expect(screen.getByText('Loading task types...')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shows error message when error prop is set', () => {
      renderWithTheme(
        <TaskTypeSelector taskTypes={[]} onSelect={mockOnSelect} error="Failed to load task types" />
      );

      expect(screen.getByText('Failed to load task types')).toBeInTheDocument();
    });
  });

  describe('controlled mode', () => {
    it('shows categories when selectedCategory is null', () => {
      renderWithTheme(
        <TaskTypeSelector
          taskTypes={mockTaskTypes}
          onSelect={mockOnSelect}
          selectedCategory={null}
        />
      );

      expect(screen.getByText('Admin')).toBeInTheDocument();
      expect(screen.getByText('Docker')).toBeInTheDocument();
    });

    it('calls onCategorySelect when a category is clicked', async () => {
      const mockCategorySelect = jest.fn();
      renderWithTheme(
        <TaskTypeSelector
          taskTypes={mockTaskTypes}
          onSelect={mockOnSelect}
          onCategorySelect={mockCategorySelect}
        />
      );

      await userEvent.click(screen.getByText('Admin'));

      expect(mockCategorySelect).toHaveBeenCalledWith('Admin');
    });

    it('calls onSelectionChange with type when type is selected', async () => {
      const mockSelectionChange = jest.fn();
      renderWithTheme(
        <TaskTypeSelector
          taskTypes={mockTaskTypes}
          onSelect={mockOnSelect}
          onSelectionChange={mockSelectionChange}
        />
      );

      await userEvent.click(screen.getByText('Admin'));

      expect(mockSelectionChange).toHaveBeenCalledWith(true, null);

      await waitFor(() => {
        expect(screen.getByText('Database backup')).toBeInTheDocument();
      });

      await userEvent.click(screen.getByText('Database backup'));

      expect(mockSelectionChange).toHaveBeenCalledWith(
        true,
        expect.objectContaining({ id: 'db.backup' })
      );
    });
  });
});
