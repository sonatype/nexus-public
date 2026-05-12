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
import { TagsPageRadix } from '../TagsPageRadix';
import { useFilteredTags } from '../hooks/useFilteredTags';
import * as tagsApi from '../tags.api';

jest.mock('../hooks/useFilteredTags');
jest.mock('../tags.api');
jest.mock('../../../../components/shared/Toast', () => ({
  useToast: () => ({ success: jest.fn(), error: jest.fn() }),
}));

const mockUseFilteredTags = useFilteredTags as jest.MockedFunction<typeof useFilteredTags>;
const mockCreateTag = tagsApi.createTag as jest.MockedFunction<typeof tagsApi.createTag>;

const mockTags = [
  {
    name: 'release-1.0',
    attributes: null,
    firstCreated: '2026-01-15T00:00:00Z',
    lastUpdated: '2026-02-01T00:00:00Z',
    componentCount: 12,
  },
  {
    name: 'staging',
    attributes: null,
    firstCreated: '2026-01-20T00:00:00Z',
    lastUpdated: '2026-02-10T00:00:00Z',
    componentCount: 7,
  },
];

const defaultHookReturn = {
  tags: mockTags,
  loading: false,
  error: null,
  filters: { nameFilter: '', componentCountRanges: [], activityDays: [] },
  sortField: 'name' as const,
  sortDirection: 'asc' as const,
  currentPage: 0,
  pageSize: 20,
  totalItems: 2,
  setFilters: jest.fn(),
  toggleSort: jest.fn(),
  setPage: jest.fn(),
  setPageSize: jest.fn(),
  retry: jest.fn(),
};

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('TagsPageRadix', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseFilteredTags.mockReturnValue({ ...defaultHookReturn });
  });

  describe('rendering', () => {
    it('renders the page title', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('Tags')).toBeInTheDocument();
    });

    it('renders the about tags description', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText(/Tags let you label and organize/i)).toBeInTheDocument();
    });

    it('renders the filter sidebar', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('Component Count')).toBeInTheDocument();
      expect(screen.getByText('Activity')).toBeInTheDocument();
    });

    it('renders the tag table with data', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('release-1.0')).toBeInTheDocument();
      expect(screen.getByText('staging')).toBeInTheDocument();
    });

    it('renders table columns', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getAllByText('Name').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Components')).toBeInTheDocument();
      expect(screen.getByText('Created')).toBeInTheDocument();
      expect(screen.getByText('Last Updated')).toBeInTheDocument();
    });

    it('renders Create Tag button', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByTestId('create-tag-button')).toBeInTheDocument();
    });

    it('renders About Tags help section', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('About Tags')).toBeInTheDocument();
    });

    it('renders pagination info', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('Showing')).toBeInTheDocument();
      expect(screen.getByText(/of 2/)).toBeInTheDocument();
    });

    it('renders component count for each tag', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('12')).toBeInTheDocument();
      expect(screen.getByText('7')).toBeInTheDocument();
    });
  });

  describe('loading state', () => {
    it('shows loading state', () => {
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        loading: true,
        tags: [],
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('Loading tags...')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shows error state', () => {
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        error: 'Failed to load tags.',
        tags: [],
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('Failed to load tags.')).toBeInTheDocument();
    });

    it('shows retry button on error', () => {
      const mockRetry = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        error: 'Failed to load tags.',
        tags: [],
        retry: mockRetry,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      const retryButton = screen.getByRole('button', { name: /retry/i });
      fireEvent.click(retryButton);
      expect(mockRetry).toHaveBeenCalled();
    });
  });

  describe('empty state', () => {
    it('shows empty state when no tags', () => {
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        tags: [],
        totalItems: 0,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('No tags found')).toBeInTheDocument();
    });
  });

  describe('name filter', () => {
    it('calls setFilters when name filter changes', () => {
      const mockSetFilters = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        setFilters: mockSetFilters,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });

      const filterInput = screen.getByPlaceholderText(/filter tags by name/i);
      fireEvent.change(filterInput, { target: { value: 'release' } });

      expect(mockSetFilters).toHaveBeenCalledWith(
        expect.objectContaining({ nameFilter: 'release' })
      );
    });

    it('shows clear filter button when filter has value', () => {
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        filters: { ...defaultHookReturn.filters, nameFilter: 'release' },
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByRole('button', { name: /clear filter/i })).toBeInTheDocument();
    });

    it('calls setFilters with empty nameFilter when clear is clicked', () => {
      const mockSetFilters = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        filters: { ...defaultHookReturn.filters, nameFilter: 'release' },
        setFilters: mockSetFilters,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByRole('button', { name: /clear filter/i }));
      expect(mockSetFilters).toHaveBeenCalledWith(
        expect.objectContaining({ nameFilter: '' })
      );
    });
  });

  describe('sorting', () => {
    it('calls toggleSort when Name column header is clicked', () => {
      const mockToggleSort = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        toggleSort: mockToggleSort,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      const sortButtons = screen.getAllByRole('button');
      const nameSort = sortButtons.find(btn => btn.textContent?.includes('Name'));
      expect(nameSort).toBeTruthy();
      fireEvent.click(nameSort!);
      expect(mockToggleSort).toHaveBeenCalledWith('name');
    });

    it('calls toggleSort when Components column header is clicked', () => {
      const mockToggleSort = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        toggleSort: mockToggleSort,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      fireEvent.click(screen.getByText('Components'));
      expect(mockToggleSort).toHaveBeenCalledWith('componentCount');
    });

    it('calls toggleSort when Created column header is clicked', () => {
      const mockToggleSort = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        toggleSort: mockToggleSort,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      fireEvent.click(screen.getByText('Created'));
      expect(mockToggleSort).toHaveBeenCalledWith('firstCreated');
    });

    it('calls toggleSort when Last Updated column header is clicked', () => {
      const mockToggleSort = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        toggleSort: mockToggleSort,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      fireEvent.click(screen.getByText('Last Updated'));
      expect(mockToggleSort).toHaveBeenCalledWith('lastUpdated');
    });
  });

  describe('component count filter', () => {
    it('renders component count options', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('Empty (0)')).toBeInTheDocument();
      expect(screen.getByText('1-10')).toBeInTheDocument();
      expect(screen.getByText('11-100')).toBeInTheDocument();
    });

    it('calls setFilters when component count option is checked', () => {
      const mockSetFilters = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        setFilters: mockSetFilters,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });

      const checkboxes = screen.getAllByRole('checkbox');
      const textEl = screen.getByText('1-10');
      const parentRow = textEl.parentElement;
      const checkbox = parentRow?.querySelector('button[role="checkbox"]');
      expect(checkbox).toBeTruthy();
      fireEvent.click(checkbox!);

      expect(mockSetFilters).toHaveBeenCalledWith(
        expect.objectContaining({ componentCountRanges: expect.arrayContaining(['1-10']) })
      );
    });

    it('shows Reset filters button when filters are active', () => {
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        filters: { ...defaultHookReturn.filters, componentCountRanges: ['1-10'] },
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByText('Reset filters')).toBeInTheDocument();
    });

    it('calls setFilters with empty arrays when Reset filters is clicked', () => {
      const mockSetFilters = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        filters: { ...defaultHookReturn.filters, componentCountRanges: ['1-10'] },
        setFilters: mockSetFilters,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByText('Reset filters'));
      expect(mockSetFilters).toHaveBeenCalledWith({
        nameFilter: '',
        componentCountRanges: [],
        activityDays: [],
      });
    });
  });

  describe('tag row navigation', () => {
    it('renders tag rows as clickable buttons', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });

      const row = screen.getByRole('button', { name: /view tag release-1\.0/i });
      expect(row).toBeInTheDocument();
    });

    it('renders all tag rows', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });

      expect(screen.getByText('release-1.0')).toBeInTheDocument();
      expect(screen.getByText('staging')).toBeInTheDocument();
    });
  });

  describe('create tag dialog', () => {
    it('opens create tag dialog when Create Tag button is clicked', async () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByTestId('create-tag-button'));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/e\.g\., release-1\.0/i)).toBeInTheDocument();
      });
    });

    it('calls createTag API and refreshes on successful create', async () => {
      const mockRetry = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        retry: mockRetry,
      });
      mockCreateTag.mockResolvedValue({ name: 'new-tag', attributes: null } as any);

      render(<TagsPageRadix />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByTestId('create-tag-button'));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(/e\.g\., release-1\.0/i)).toBeInTheDocument();
      });

      const tagNameInput = screen.getByPlaceholderText(/e\.g\., release-1\.0/i);
      fireEvent.change(tagNameInput, { target: { value: 'new-tag' } });

      fireEvent.click(screen.getByRole('button', { name: /^create$/i }));

      await waitFor(() => {
        expect(mockCreateTag).toHaveBeenCalledWith('new-tag');
        expect(mockRetry).toHaveBeenCalled();
      });
    });

    it('disables Create button when tag name is empty', async () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });

      fireEvent.click(screen.getByTestId('create-tag-button'));

      await waitFor(() => {
        const createButton = screen.getByRole('button', { name: /^create$/i });
        expect(createButton).toBeDisabled();
      });
    });
  });

  describe('pagination', () => {
    it('disables previous button on first page', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByRole('button', { name: /previous page/i })).toBeDisabled();
    });

    it('disables next button on last page', () => {
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        currentPage: 0,
        totalItems: 2,
        pageSize: 20,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByRole('button', { name: /next page/i })).toBeDisabled();
    });

    it('calls setPage when next page button is clicked', () => {
      const mockSetPage = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        currentPage: 0,
        totalItems: 50,
        pageSize: 20,
        setPage: mockSetPage,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      fireEvent.click(screen.getByRole('button', { name: /next page/i }));
      expect(mockSetPage).toHaveBeenCalledWith(1);
    });

    it('calls setPage when previous page button is clicked', () => {
      const mockSetPage = jest.fn();
      mockUseFilteredTags.mockReturnValue({
        ...defaultHookReturn,
        currentPage: 1,
        totalItems: 50,
        pageSize: 20,
        setPage: mockSetPage,
      });

      render(<TagsPageRadix />, { wrapper: TestWrapper });
      fireEvent.click(screen.getByRole('button', { name: /previous page/i }));
      expect(mockSetPage).toHaveBeenCalledWith(0);
    });
  });

  describe('accessibility', () => {
    it('renders with a data-testid for the page root', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByTestId('tags-page')).toBeInTheDocument();
    });

    it('renders search input with placeholder', () => {
      render(<TagsPageRadix />, { wrapper: TestWrapper });
      expect(screen.getByPlaceholderText(/filter tags by name/i)).toBeInTheDocument();
    });
  });
});
