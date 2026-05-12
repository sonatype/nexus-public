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

import { TagDetail } from '../components/TagDetail';
import * as useTagDetailHook from '../hooks/useTagDetail';
import * as tagsApi from '../tags.api';
import { mockTagDetail } from './mockData';

// Mock the router
const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {
      go: mockGo,
    },
  }),
}));

// Mock the useTagDetail hook
jest.mock('../hooks/useTagDetail');
const mockedUseTagDetail = useTagDetailHook.useTagDetail as jest.MockedFunction<
  typeof useTagDetailHook.useTagDetail
>;

// Mock the tags API
jest.mock('../tags.api');
const mockedDeleteTag = tagsApi.deleteTag as jest.MockedFunction<typeof tagsApi.deleteTag>;

// Mock useToast
const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();
jest.mock('../../../../components/shared/Toast', () => ({
  useToast: () => ({
    success: mockToastSuccess,
    error: mockToastError,
    warning: jest.fn(),
    info: jest.fn(),
    showToast: jest.fn(),
  }),
}));

// Mock clipboard API
const mockWriteText = jest.fn();
Object.assign(navigator, {
  clipboard: {
    writeText: mockWriteText,
  },
});

// Wrapper with Theme for Radix components
const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('TagDetail', () => {
  const mockRetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockWriteText.mockResolvedValue(undefined);
    mockedDeleteTag.mockResolvedValue(undefined);
    mockToastSuccess.mockClear();
    mockToastError.mockClear();
  });

  const setupMock = (overrides: Partial<useTagDetailHook.UseTagDetailResult> = {}) => {
    const defaultState: useTagDetailHook.TagDetailState = {
      tagDetail: mockTagDetail,
      loading: false,
      error: null,
    };

    const defaultActions: useTagDetailHook.TagDetailActions = {
      retry: mockRetry,
    };

    mockedUseTagDetail.mockReturnValue({
      state: { ...defaultState, ...overrides.state },
      actions: { ...defaultActions, ...overrides.actions },
    });
  };

  it('should render tag detail with all information', () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.getByTestId('tag-detail')).toBeInTheDocument();
    expect(screen.getByTestId('tag-name')).toHaveTextContent('release-1.0');
    expect(screen.getByTestId('first-created')).toBeInTheDocument();
    expect(screen.getByTestId('last-updated')).toBeInTheDocument();
    expect(screen.getByTestId('attributes-json')).toBeInTheDocument();
  });

  it('should show loading state', () => {
    setupMock({
      state: {
        tagDetail: null,
        loading: true,
        error: null,
      },
    });

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.getByText('Loading tag details...')).toBeInTheDocument();
  });

  it('should show error state with retry button', () => {
    setupMock({
      state: {
        tagDetail: null,
        loading: false,
        error: 'Tag not found',
      },
    });

    renderWithTheme(<TagDetail tagName="nonexistent" />);

    expect(screen.getByText('Failed to load tag')).toBeInTheDocument();
    expect(screen.getByText('Tag not found')).toBeInTheDocument();
    expect(screen.getByText('Retry')).toBeInTheDocument();
  });

  it('should call retry when retry button is clicked', () => {
    setupMock({
      state: {
        tagDetail: null,
        loading: false,
        error: 'Network error',
      },
    });

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    fireEvent.click(screen.getByText('Retry'));
    expect(mockRetry).toHaveBeenCalledTimes(1);
  });

  it('should navigate back when back button is clicked', () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    fireEvent.click(screen.getByTestId('back-button'));
    expect(mockGo).toHaveBeenCalledWith('preview.browse.tags');
  });

  it('should call onBack callback when provided', () => {
    setupMock();
    const onBack = jest.fn();

    renderWithTheme(<TagDetail tagName="release-1.0" onBack={onBack} />);

    fireEvent.click(screen.getByTestId('back-button'));
    expect(onBack).toHaveBeenCalledTimes(1);
    expect(mockGo).not.toHaveBeenCalled();
  });

  it('should display formatted timestamps', () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    const firstCreated = screen.getByTestId('first-created');
    const lastUpdated = screen.getByTestId('last-updated');

    // Timestamps should be formatted and visible
    expect(firstCreated).toBeInTheDocument();
    expect(lastUpdated).toBeInTheDocument();
    // Content depends on locale, so just check they're not empty
    expect(firstCreated.textContent).not.toBe('');
    expect(lastUpdated.textContent).not.toBe('');
  });

  it('should display attributes as JSON', () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    const attributesJson = screen.getByTestId('attributes-json');
    expect(attributesJson).toHaveTextContent('production');
    expect(attributesJson).toHaveTextContent('1.0.0');
    expect(attributesJson).toHaveTextContent('142');
  });

  it('should display "No attributes" when attributes are empty', () => {
    setupMock({
      state: {
        tagDetail: { ...mockTagDetail, attributes: {} },
        loading: false,
        error: null,
      },
    });

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.getByTestId('no-attributes')).toHaveTextContent('No attributes');
  });

  it('should copy JSON to clipboard when copy button is clicked', async () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    const copyButton = screen.getByTestId('copy-json-button');
    fireEvent.click(copyButton);

    await waitFor(() => {
      expect(mockWriteText).toHaveBeenCalledWith(
        JSON.stringify(mockTagDetail.attributes, null, 2)
      );
    });
  });

  it('should not show copy button when attributes are empty', () => {
    setupMock({
      state: {
        tagDetail: { ...mockTagDetail, attributes: {} },
        loading: false,
        error: null,
      },
    });

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.queryByTestId('copy-json-button')).not.toBeInTheDocument();
  });

  it('should navigate to search when Find tagged items is clicked', () => {
    setupMock();

    // Mock window.location.hash
    const originalHash = window.location.hash;
    Object.defineProperty(window, 'location', {
      value: { hash: '' },
      writable: true,
    });

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    const findButton = screen.getByTestId('find-tagged-items-button');
    fireEvent.click(findButton);

    expect(window.location.hash).toContain('browse/search/custom');
    expect(window.location.hash).toContain('release-1.0');

    // Restore
    window.location.hash = originalHash;
  });

  it('should open delete confirmation dialog when delete button is clicked', async () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    const deleteButton = screen.getByTestId('delete-tag-button');
    fireEvent.click(deleteButton);

    await waitFor(() => {
      // Look for the dialog - should have alertdialog role
      const dialog = screen.getByRole('alertdialog');
      expect(dialog).toBeInTheDocument();
      expect(screen.getByText(/Are you sure you want to delete the tag/)).toBeInTheDocument();
    });
  });

  it('should close delete dialog when Cancel is clicked', async () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    // Open dialog
    fireEvent.click(screen.getByTestId('delete-tag-button'));

    await waitFor(() => {
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });

    // Click cancel
    fireEvent.click(screen.getByText('Cancel'));

    await waitFor(() => {
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });
  });

  it('should call deleteTag API, show success toast, and navigate back when delete is confirmed', async () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    // Open dialog
    fireEvent.click(screen.getByTestId('delete-tag-button'));

    await waitFor(() => {
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });

    // Confirm delete
    const deleteConfirmButton = screen.getAllByText('Delete').find(
      (el) => el.closest('[role="alertdialog"]')
    );
    if (deleteConfirmButton) {
      fireEvent.click(deleteConfirmButton);
    }

    await waitFor(() => {
      expect(mockedDeleteTag).toHaveBeenCalledWith('release-1.0');
    });

    await waitFor(() => {
      expect(mockToastSuccess).toHaveBeenCalledWith('Tag "release-1.0" deleted successfully');
    });

    await waitFor(() => {
      expect(mockGo).toHaveBeenCalledWith('preview.browse.tags');
    });
  });

  it('should show error toast when delete fails', async () => {
    setupMock();
    mockedDeleteTag.mockRejectedValue(new Error('Permission denied'));

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    // Open dialog
    fireEvent.click(screen.getByTestId('delete-tag-button'));

    await waitFor(() => {
      expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });

    // Confirm delete
    const deleteConfirmButton = screen.getAllByText('Delete').find(
      (el) => el.closest('[role="alertdialog"]')
    );
    if (deleteConfirmButton) {
      fireEvent.click(deleteConfirmButton);
    }

    await waitFor(() => {
      expect(mockedDeleteTag).toHaveBeenCalledWith('release-1.0');
    });

    await waitFor(() => {
      expect(mockToastError).toHaveBeenCalledWith('Permission denied');
    });

    expect(mockGo).not.toHaveBeenCalled();
  });

  it('should render with custom className', () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.getByTestId('tag-detail')).toHaveClass('tag-detail');
  });

  it('should display back button in loading state', () => {
    setupMock({
      state: {
        tagDetail: null,
        loading: true,
        error: null,
      },
    });

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.getByText('Back to Tags')).toBeInTheDocument();
  });

  it('should display back button in error state', () => {
    setupMock({
      state: {
        tagDetail: null,
        loading: false,
        error: 'Some error',
      },
    });

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.getByText('Back to Tags')).toBeInTheDocument();
  });

  it('should handle clipboard write failure gracefully', async () => {
    setupMock();
    mockWriteText.mockRejectedValue(new Error('Clipboard not available'));
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    const copyButton = screen.getByTestId('copy-json-button');
    fireEvent.click(copyButton);

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith(
        'Failed to copy to clipboard:',
        expect.any(Error)
      );
    });

    consoleSpy.mockRestore();
  });

  it('should render tag detail header section', () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.getByText('Tag Details')).toBeInTheDocument();
  });

  it('should render Find tagged items button', () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.getByTestId('find-tagged-items-button')).toHaveTextContent('Find tagged items');
  });

  it('should render Delete Tag button', () => {
    setupMock();

    renderWithTheme(<TagDetail tagName="release-1.0" />);

    expect(screen.getByTestId('delete-tag-button')).toHaveTextContent('Delete Tag');
  });
});

