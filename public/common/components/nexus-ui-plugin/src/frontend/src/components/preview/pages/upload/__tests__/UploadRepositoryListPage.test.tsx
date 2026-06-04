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
import { render, screen, fireEvent, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { UploadRepositoryListPage } from '../UploadRepositoryListPage';
import { useUploadableRepositories } from '../hooks/useUploadableRepositories';
import { UPLOAD_STRINGS } from '../upload.types';

// Mock the router
const mockGo = jest.fn();
jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {
      go: mockGo,
    },
  }),
}));

// Mock the hook
jest.mock('../hooks/useUploadableRepositories');
const mockUseUploadableRepositories = useUploadableRepositories as jest.MockedFunction<
  typeof useUploadableRepositories
>;

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
  },
  ExtAPIUtils: {
    extAPIRequest: jest.fn(),
    checkForErrorAndExtract: jest.fn(),
  },
  APIConstants: {
    EXT: {
      UPLOAD: {
        ACTION: 'coreui_Upload',
        METHODS: {
          GET_UPLOAD_DEFINITIONS: 'getUploadDefinitions',
        },
      },
      REPOSITORY: {
        ACTION: 'coreui_Repository',
        METHODS: {
          READ_REFERENCES: 'readReferences',
        },
      },
    },
  },
}));

/**
 * Renders the component wrapped in necessary providers.
 */
function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('UploadRepositoryListPage', () => {
  const mockRepositories = [
    { name: 'maven-releases', format: 'maven2', url: 'http://localhost:8081/repository/maven-releases/' },
    { name: 'npm-hosted', format: 'npm', url: 'http://localhost:8081/repository/npm-hosted/' },
    { name: 'docker-hosted', format: 'docker', url: 'http://localhost:8081/repository/docker-hosted/' },
  ];

  const defaultHookReturn = {
    repositories: mockRepositories,
    loading: false,
    error: null,
    filterText: '',
    selectedFormats: [],
    availableFormats: ['maven2', 'npm', 'docker'],
    formatOptions: [
      { value: 'maven2', label: 'maven2', count: 1 },
      { value: 'npm', label: 'npm', count: 1 },
      { value: 'docker', label: 'docker', count: 1 },
    ],
    hasActiveFilters: false,
    sortColumn: null,
    sortDirection: null,
    handleSort: jest.fn(),
    handleSortChange: jest.fn(),
    handleFilterChange: jest.fn(),
    clearFilter: jest.fn(),
    toggleFormat: jest.fn(),
    setFormats: jest.fn(),
    clearAllFilters: jest.fn(),
    refetch: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseUploadableRepositories.mockReturnValue(defaultHookReturn);
  });

  describe('Rendering', () => {
    it('renders the page with title and repository count', () => {
      renderWithTheme(<UploadRepositoryListPage />);

      expect(screen.getAllByText(UPLOAD_STRINGS.pageTitle).length).toBeGreaterThan(0);
      expect(screen.getAllByText(/3 repositories/).length).toBeGreaterThan(0);
    });

    it('renders the filter input', () => {
      renderWithTheme(<UploadRepositoryListPage />);

      expect(
        screen.getAllByPlaceholderText(UPLOAD_STRINGS.filterPlaceholder).length
      ).toBeGreaterThan(0);
    });

    it('renders the repository list', () => {
      renderWithTheme(<UploadRepositoryListPage />);

      expect(screen.getByText('maven-releases')).toBeInTheDocument();
      expect(screen.getByText('npm-hosted')).toBeInTheDocument();
      expect(screen.getByText('docker-hosted')).toBeInTheDocument();
    });

    it('has the correct test id', () => {
      renderWithTheme(<UploadRepositoryListPage />);

      expect(screen.getByTestId('upload-page')).toBeInTheDocument();
    });
  });

  describe('Loading State', () => {
    it('displays loading state when loading', () => {
      mockUseUploadableRepositories.mockReturnValue({
        ...defaultHookReturn,
        repositories: [],
        loading: true,
      });

      renderWithTheme(<UploadRepositoryListPage />);

      expect(screen.getByText(UPLOAD_STRINGS.loadingMessage)).toBeInTheDocument();
    });
  });

  describe('Error State', () => {
    it('displays error message when there is an error', () => {
      const errorMessage = 'Failed to fetch repositories';
      mockUseUploadableRepositories.mockReturnValue({
        ...defaultHookReturn,
        repositories: [],
        error: errorMessage,
      });

      renderWithTheme(<UploadRepositoryListPage />);

      expect(screen.getByText(new RegExp(errorMessage))).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('displays empty message when no repositories', () => {
      mockUseUploadableRepositories.mockReturnValue({
        ...defaultHookReturn,
        repositories: [],
      });

      renderWithTheme(<UploadRepositoryListPage />);

      expect(screen.getByText(UPLOAD_STRINGS.emptyMessage)).toBeInTheDocument();
    });
  });

  describe('Filtering', () => {
    it('calls handleFilterChange when typing in filter input', async () => {
      const mockHandleFilterChange = jest.fn();
      mockUseUploadableRepositories.mockReturnValue({
        ...defaultHookReturn,
        handleFilterChange: mockHandleFilterChange,
      });

      renderWithTheme(<UploadRepositoryListPage />);

      const filterInputs = screen.getAllByPlaceholderText(UPLOAD_STRINGS.filterPlaceholder);
      await userEvent.type(filterInputs[0], 'maven');

      expect(mockHandleFilterChange).toHaveBeenCalled();
    });

    it('calls clearFilter when Escape key is pressed', async () => {
      const mockClearFilter = jest.fn();
      mockUseUploadableRepositories.mockReturnValue({
        ...defaultHookReturn,
        filterText: 'maven',
        clearFilter: mockClearFilter,
      });

      renderWithTheme(<UploadRepositoryListPage />);

      const filterInputs = screen.getAllByPlaceholderText(UPLOAD_STRINGS.filterPlaceholder);
      fireEvent.keyDown(filterInputs[0], { key: 'Escape', code: 'Escape' });

      expect(mockClearFilter).toHaveBeenCalled();
    });

    it('shows clear button when filter has text', () => {
      mockUseUploadableRepositories.mockReturnValue({
        ...defaultHookReturn,
        filterText: 'maven',
      });

      renderWithTheme(<UploadRepositoryListPage />);

      expect(screen.getAllByLabelText('Clear filter').length).toBeGreaterThan(0);
    });

    it('calls clearFilter when clear button is clicked', async () => {
      const mockClearFilter = jest.fn();
      mockUseUploadableRepositories.mockReturnValue({
        ...defaultHookReturn,
        filterText: 'maven',
        clearFilter: mockClearFilter,
      });

      renderWithTheme(<UploadRepositoryListPage />);

      const clearButtons = screen.getAllByLabelText('Clear filter');
      await userEvent.click(clearButtons[0]);

      expect(mockClearFilter).toHaveBeenCalled();
    });
  });

  describe('Navigation', () => {
    it('navigates to upload form when repository row is clicked', async () => {
      renderWithTheme(<UploadRepositoryListPage />);

      const mavenRow = screen.getByText('maven-releases').closest('tr');
      expect(mavenRow).not.toBeNull();

      if (mavenRow) {
        await userEvent.click(mavenRow);
      }

      expect(mockGo).toHaveBeenCalledWith('preview.browse.upload.form', {
        repoName: 'maven-releases',
      });
    });
  });

  describe('Sorting', () => {
    it('calls handleSortChange when clicking name column header', async () => {
      const mockHandleSortChange = jest.fn();
      mockUseUploadableRepositories.mockReturnValue({
        ...defaultHookReturn,
        handleSortChange: mockHandleSortChange,
      });

      renderWithTheme(<UploadRepositoryListPage />);

      const table = screen.getByRole('table');
      const nameHeader = within(table).getByText(UPLOAD_STRINGS.columns.name);
      await userEvent.click(nameHeader);

      expect(mockHandleSortChange).toHaveBeenCalledWith('name', 'asc');
    });

    it('calls handleSortChange when clicking format column header', async () => {
      const mockHandleSortChange = jest.fn();
      mockUseUploadableRepositories.mockReturnValue({
        ...defaultHookReturn,
        handleSortChange: mockHandleSortChange,
      });

      renderWithTheme(<UploadRepositoryListPage />);

      const table = screen.getByRole('table');
      const formatHeader = within(table).getByText(UPLOAD_STRINGS.columns.format);
      await userEvent.click(formatHeader);

      expect(mockHandleSortChange).toHaveBeenCalledWith('format', 'asc');
    });
  });

  describe('Accessibility', () => {
    it('has accessible filter input', () => {
      renderWithTheme(<UploadRepositoryListPage />);

      const filterInputs = screen.getAllByLabelText('Filter repositories by name');
      expect(filterInputs.length).toBeGreaterThan(0);
    });
  });
});

