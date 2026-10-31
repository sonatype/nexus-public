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

import { ContentSelectorsPage } from '../ContentSelectorsPage';
import * as useContentSelectorsApiModule from '../useContentSelectorsApi';
import { ToastProvider } from '../../../../../shared/Toast';
import { ExtJS } from '../../../../../../../interface/ExtJS';

// Mock the API hook
jest.mock('../useContentSelectorsApi');

// ContentSelectorsPage derives canCreate/canUpdate/canDelete from the relative
// interface/ExtJS singleton; spy on it to drive gating per test (NEXUS-54212).
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

const mockedUseContentSelectorsApi = useContentSelectorsApiModule.useContentSelectorsApi as jest.MockedFunction<
  typeof useContentSelectorsApiModule.useContentSelectorsApi
>;

// Mock child components
jest.mock('../ContentSelectorsList', () => ({
  ContentSelectorsList: function MockContentSelectorsList({
    onSelect,
    onCreate,
  }: {
    onSelect: (name: string) => void;
    onCreate: () => void;
  }) {
    return (
      <div data-testid="content-selectors-list">
        <button onClick={() => onSelect('test-selector')}>Select Selector</button>
        <button onClick={onCreate}>Create Selector</button>
      </div>
    );
  },
}));

jest.mock('../ContentSelectorForm', () => ({
  ContentSelectorForm: function MockContentSelectorForm({
    selector,
    isCreate,
    onCancel,
    onComplete,
    canEdit,
    canDelete,
  }: any) {
    return (
      <div
        data-testid="content-selector-form"
        data-can-edit={String(canEdit)}
        data-can-delete={String(canDelete)}
      >
        <span>{isCreate ? 'Create Selector' : `Edit ${selector?.name || 'Loading...'}`}</span>
        <button
          onClick={() => {
            // Simulate save: call onComplete (refresh list) + onCancel (navigate back)
            if (onComplete) onComplete();
            onCancel();
          }}
        >
          Save
        </button>
        <button onClick={onCancel}>Cancel</button>
      </div>
    );
  },
}));

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

describe('ContentSelectorsPage', () => {
  const mockFetchContentSelector = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckPermission.mockReturnValue(true);
    // Set URL hash to list view for most tests
    window.location.hash = '#preview/admin/repository/selectors';
    
    mockFetchContentSelector.mockResolvedValue({
      name: 'test-selector',
      type: 'csel',
      description: 'Test content selector',
      expression: 'format == "maven2"',
    });

    mockedUseContentSelectorsApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchContentSelectors: jest.fn().mockResolvedValue([]),
      fetchContentSelector: mockFetchContentSelector,
      fetchRepositories: jest.fn().mockResolvedValue([]),
      createContentSelector: jest.fn().mockResolvedValue({}),
      updateContentSelector: jest.fn().mockResolvedValue({}),
      deleteContentSelector: jest.fn().mockResolvedValue({}),
      previewContentSelector: jest.fn().mockResolvedValue([]),
      fetchPrivilegesForSelector: jest.fn().mockResolvedValue([]),
    });
  });

  it('renders the content selectors list by default', async () => {
    render(<ContentSelectorsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
    });
    expect(screen.getByRole('heading', { name: 'Content Selectors' })).toBeInTheDocument();
  });

  it('shows create selector form when Create Selector button is clicked', async () => {
    render(<ContentSelectorsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('create-selector-button'));

    await waitFor(() => {
      expect(screen.getByTestId('content-selector-form')).toBeInTheDocument();
      expect(screen.getByText('Create Selector')).toBeInTheDocument();
    });
  });

  it('navigates to selector detail when a selector is selected', async () => {
    render(<ContentSelectorsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Select Selector'));

    await waitFor(() => {
      expect(screen.getByTestId('content-selector-form')).toBeInTheDocument();
    });
  });

  it('returns to list view when cancel is clicked in create mode', async () => {
    render(<ContentSelectorsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
    });

    // Go to create mode
    fireEvent.click(screen.getByTestId('create-selector-button'));

    await waitFor(() => {
      expect(screen.getByTestId('content-selector-form')).toBeInTheDocument();
    });

    // Click cancel
    fireEvent.click(screen.getByText('Cancel'));

    await waitFor(() => {
      expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
    });
  });

  it('handles error state', async () => {
    mockedUseContentSelectorsApi.mockReturnValue({
      loading: false,
      error: 'Failed to load content selectors',
      setError: mockSetError,
      fetchContentSelectors: jest.fn().mockResolvedValue([]),
      fetchContentSelector: mockFetchContentSelector,
      fetchRepositories: jest.fn().mockResolvedValue([]),
      createContentSelector: jest.fn().mockResolvedValue({}),
      updateContentSelector: jest.fn().mockResolvedValue({}),
      deleteContentSelector: jest.fn().mockResolvedValue({}),
      previewContentSelector: jest.fn().mockResolvedValue([]),
      fetchPrivilegesForSelector: jest.fn().mockResolvedValue([]),
    });

    render(<ContentSelectorsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByText('Failed to load content selectors')).toBeInTheDocument();
    });
  });

  it('returns to list when save completes in create mode', async () => {
    render(<ContentSelectorsPage />, { wrapper: TestWrapper });

    await waitFor(() => {
      expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
    });

    // Go to create mode
    fireEvent.click(screen.getByTestId('create-selector-button'));

    await waitFor(() => {
      expect(screen.getByTestId('content-selector-form')).toBeInTheDocument();
    });

    // Save (mock calls onComplete + onCancel which navigates back)
    fireEvent.click(screen.getByText('Save'));

    await waitFor(() => {
      expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
    });
  });

  // NEXUS-54212: write actions are disabled (not hidden) for read-only users, and the
  // detail form receives canEdit/canDelete derived from the selectors permissions.
  describe('permission gating (NEXUS-54212)', () => {
    it('renders an enabled create button when the user has selectors:create', async () => {
      mockCheckPermission.mockReturnValue(true);
      render(<ContentSelectorsPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
      });
      expect(screen.getByTestId('create-selector-button')).toBeEnabled();
    });

    it('disables (not hides) the create button when the user lacks selectors:create', async () => {
      mockCheckPermission.mockImplementation((permission) => permission !== 'nexus:selectors:create');
      render(<ContentSelectorsPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
      });

      const createButton = screen.getByTestId('create-selector-button');
      expect(createButton).toBeInTheDocument();
      expect(createButton).toBeDisabled();
    });

    it('passes canEdit=true to the detail form when the user has selectors:update', async () => {
      mockCheckPermission.mockReturnValue(true);
      render(<ContentSelectorsPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Select Selector'));

      await waitFor(() => {
        expect(screen.getByTestId('content-selector-form')).toBeInTheDocument();
      });
      expect(screen.getByTestId('content-selector-form')).toHaveAttribute('data-can-edit', 'true');
    });

    it('passes canEdit=false to the detail form when the user lacks selectors:update', async () => {
      mockCheckPermission.mockImplementation((permission) => permission !== 'nexus:selectors:update');
      render(<ContentSelectorsPage />, { wrapper: TestWrapper });

      await waitFor(() => {
        expect(screen.getByTestId('content-selectors-list')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Select Selector'));

      await waitFor(() => {
        expect(screen.getByTestId('content-selector-form')).toBeInTheDocument();
      });
      expect(screen.getByTestId('content-selector-form')).toHaveAttribute('data-can-edit', 'false');
    });
  });
});
