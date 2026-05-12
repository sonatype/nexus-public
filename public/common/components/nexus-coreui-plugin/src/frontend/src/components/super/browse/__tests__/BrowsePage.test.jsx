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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme, ThemePanel } from '@radix-ui/themes';

import { BrowsePage } from '../BrowsePage';

// Wrapper with Radix UI providers
const renderWithProviders = (ui) => {
  return render(
    <Theme>
      {ui}
    </Theme>
  );
};

// Mock router hooks
const mockGo = jest.fn();
const mockRouter = {
  stateService: {
    go: mockGo,
  },
};

let mockParams = {};

jest.mock('@uirouter/react', () => ({
  useRouter: () => mockRouter,
  useCurrentStateAndParams: () => ({
    params: mockParams,
  }),
}));

// Mock axios
jest.mock('axios', () => ({
  get: jest.fn().mockResolvedValue({ data: { data: [] } }),
}));

// Mock repository list hooks
jest.mock('../repository-list/useRepositoryList', () => ({
  isIqServerEnabled: () => false,
  useRepositoryList: () => ({
    state: { loading: false, error: null, healthCheck: {}, firewallStatus: {} },
    showHealthCheckColumn: false,
    showIqPolicyViolationsColumn: false,
  }),
}));

jest.mock('../repository-list/useRepositoryListServer', () => ({
  useRepositoryListServer: () => ({
    repositories: [],
    loading: false,
    error: null,
    filterParams: {},
    setFilterParams: jest.fn(),
    totalCount: 0,
    page: 1,
    totalPages: 1,
    nextPage: jest.fn(),
    previousPage: jest.fn(),
  }),
  arrayToFilterString: jest.fn((arr) => arr?.join(',') || undefined),
}));

// Mock child components
jest.mock('../repository-list/RepositoryListTable', () => ({
  RepositoryListTable: ({ onSelect }) => (
    <div data-testid="mock-repository-list">
      <button
        data-testid="repo-maven-central"
        onClick={() => onSelect && onSelect('maven-central')}
      >
        maven-central
      </button>
      <button
        data-testid="repo-npm-hosted"
        onClick={() => onSelect && onSelect('npm-hosted')}
      >
        npm-hosted
      </button>
    </div>
  ),
}));

// Mock browse API
jest.mock('../browse.api', () => ({
  fetchAsset: jest.fn().mockResolvedValue({
    id: 'asset-123',
    name: 'test-asset',
    format: 'maven2',
    contentType: 'application/xml',
    size: 1024,
    repositoryName: 'maven-central',
    path: '/org/apache/maven/pom.xml',
  }),
  fetchComponent: jest.fn().mockResolvedValue({
    id: 'comp-123',
    name: 'maven',
    format: 'maven2',
    repositoryName: 'maven-central',
    group: 'org.apache',
    version: '1.0.0',
  }),
}));

// Mock shared components
const mockToast = {
  success: jest.fn(),
  error: jest.fn(),
  info: jest.fn(),
  warning: jest.fn(),
};

jest.mock('../../../../components/shared', () => {
  const actual = jest.requireActual('../../../../components/shared');
  return {
    ...actual,
    FilterSidebar: () => <div data-testid="mock-filter-sidebar">Filters</div>,
    PageHeader: ({ title, description }) => (
      <div data-testid="mock-page-header">
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
    ),
    useToast: () => mockToast,
  };
});

jest.mock('../../search/unified/MobileFilterDrawer', () => ({
  MobileFilterDrawer: ({ children, isOpen }) =>
    isOpen ? <div data-testid="mock-mobile-filter-drawer">{children}</div> : null,
}));

jest.mock('../tree/BrowseTree', () => ({
  BrowseTree: ({ repositoryName, initialPath, onSelect }) => (
    <div data-testid="mock-browse-tree">
      <span data-testid="tree-repo-name">{repositoryName}</span>
      {initialPath && <span data-testid="tree-initial-path">{initialPath}</span>}
      <button
        data-testid="tree-node-component"
        onClick={() =>
          onSelect && onSelect({
            id: 'org/apache/maven',
            text: 'maven',
            type: 'component',
            leaf: false,
            componentId: 'comp-123',
          })
        }
      >
        Component Node
      </button>
      <button
        data-testid="tree-node-asset"
        onClick={() =>
          onSelect && onSelect({
            id: 'org/apache/maven/pom.xml',
            text: 'pom.xml',
            type: 'asset',
            leaf: true,
            assetId: 'asset-456',
          })
        }
      >
        Asset Node
      </button>
      <button
        data-testid="tree-node-folder"
        onClick={() =>
          onSelect && onSelect({
            id: 'org/apache',
            text: 'apache',
            type: 'folder',
            leaf: false,
          })
        }
      >
        Folder Node
      </button>
    </div>
  ),
}));

jest.mock('../detail/DetailPanel', () => ({
  DetailPanel: ({ node, repositoryName, onDeleted }) => (
    <div data-testid="mock-detail-panel">
      {node ? (
        <>
          <span data-testid="detail-node-text">{node.text}</span>
          <span data-testid="detail-node-type">{node.type}</span>
          <span data-testid="detail-repo-name">{repositoryName}</span>
          <button data-testid="detail-delete-btn" onClick={onDeleted}>
            Delete
          </button>
        </>
      ) : (
        <span data-testid="detail-empty">Select an item</span>
      )}
    </div>
  ),
}));

describe('BrowsePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockParams = {};
  });

  describe('Layout', () => {
    it('renders list view by default (Step 1)', () => {
      renderWithProviders(<BrowsePage />);

      expect(screen.getByTestId('browse-page')).toBeInTheDocument();
      expect(screen.getByTestId('mock-repository-list')).toBeInTheDocument();
      // Detail panel is only shown in tree view (Step 2)
      expect(screen.queryByTestId('mock-detail-panel')).not.toBeInTheDocument();
    });

    it('renders tree view with detail panel when URL has repo (Step 2)', () => {
      mockParams = { repoName: 'maven-central' };
      renderWithProviders(<BrowsePage />);

      expect(screen.getByTestId('browse-page')).toBeInTheDocument();
      expect(screen.getByTestId('mock-browse-tree')).toBeInTheDocument();
      expect(screen.getByTestId('mock-detail-panel')).toBeInTheDocument();
      expect(screen.queryByTestId('mock-repository-list')).not.toBeInTheDocument();
    });

    it('renders page header with title and count', () => {
      renderWithProviders(<BrowsePage />);

      const headings = screen.getAllByRole('heading', { name: /repositories/i });
      expect(headings.length).toBeGreaterThan(0);
    });
  });

  describe('Repository selection', () => {
    it('shows repository list in initial view', () => {
      renderWithProviders(<BrowsePage />);

      expect(screen.getByTestId('mock-repository-list')).toBeInTheDocument();
      // Tree should not be visible until a repo is selected
      expect(screen.queryByTestId('mock-browse-tree')).not.toBeInTheDocument();
    });

    it('navigates to repository browse in same tab when repo clicked', async () => {
      mockGo.mockClear();
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('repo-maven-central'));

      expect(mockGo).toHaveBeenCalledWith('preview.browse.browse.repo', {
        repoName: 'maven-central',
      });
    });

    it('keeps repository list visible when repo clicked (same-tab navigation)', async () => {
      mockGo.mockClear();
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('repo-maven-central'));

      expect(screen.getByTestId('mock-repository-list')).toBeInTheDocument();
    });
  });

  describe('Tree interaction', () => {
    beforeEach(() => {
      mockParams = { repoName: 'maven-central' };
    });

    it('shows tree panel when navigating with repo in URL', async () => {
      renderWithProviders(<BrowsePage />);

      expect(screen.getByTestId('mock-browse-tree')).toBeInTheDocument();
      expect(screen.getByTestId('tree-repo-name')).toHaveTextContent('maven-central');
    });

    it('updates selected node on tree item click', async () => {
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('tree-node-component'));

      expect(screen.getByTestId('detail-node-text')).toHaveTextContent('maven');
      expect(screen.getByTestId('detail-node-type')).toHaveTextContent('component');
    });

  });

  describe('Detail panel', () => {
    beforeEach(() => {
      mockParams = { repoName: 'maven-central' };
    });

    it('shows empty state when no node selected (after navigating with repo in URL)', () => {
      renderWithProviders(<BrowsePage />);

      expect(screen.getByTestId('detail-empty')).toBeInTheDocument();
    });

    it('shows detail panel when node selected', async () => {
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('tree-node-component'));

      expect(screen.getByTestId('detail-node-text')).toBeInTheDocument();
      expect(screen.queryByTestId('detail-empty')).not.toBeInTheDocument();
    });

    it('shows correct node type in detail panel', async () => {
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('tree-node-component'));
      expect(screen.getByTestId('detail-node-type')).toHaveTextContent('component');

      await userEvent.click(screen.getByTestId('tree-node-asset'));
      expect(screen.getByTestId('detail-node-type')).toHaveTextContent('asset');

      await userEvent.click(screen.getByTestId('tree-node-folder'));
      expect(screen.getByTestId('detail-node-type')).toHaveTextContent('folder');
    });
  });

  describe('Deep linking', () => {
    it('restores repository from URL params', () => {
      mockParams = { repoName: 'maven-central' };

      renderWithProviders(<BrowsePage />);

      // Tree should be visible with the repository from URL
      expect(screen.getByTestId('mock-browse-tree')).toBeInTheDocument();
      expect(screen.getByTestId('tree-repo-name')).toHaveTextContent('maven-central');
    });

    it('restores path from URL params', () => {
      mockParams = {
        repoName: 'maven-central',
        path: encodeURIComponent('org/apache/maven'),
      };

      renderWithProviders(<BrowsePage />);

      // Tree should receive the initial path
      expect(screen.getByTestId('tree-initial-path')).toHaveTextContent('org/apache/maven');
    });

    it('expands tree to path on load (via initialPath prop)', () => {
      mockParams = {
        repoName: 'maven-central',
        path: encodeURIComponent('org/apache'),
      };

      renderWithProviders(<BrowsePage />);

      // The BrowseTree mock receives initialPath
      expect(screen.getByTestId('tree-initial-path')).toHaveTextContent('org/apache');
    });
  });

  describe('Delete handling', () => {
    beforeEach(() => {
      mockParams = { repoName: 'maven-central' };
    });

    it('clears selection after delete', async () => {
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('tree-node-component'));
      expect(screen.getByTestId('detail-node-text')).toHaveTextContent('maven');

      await userEvent.click(screen.getByTestId('detail-delete-btn'));
      expect(screen.getByTestId('detail-empty')).toBeInTheDocument();
    });
  });

  describe('Copy URL', () => {
    beforeEach(() => {
      Object.defineProperty(navigator, 'clipboard', {
        value: {
          writeText: jest.fn().mockResolvedValue(undefined),
        },
        configurable: true,
      });
    });

    it('copies repository URL to clipboard and shows success toast', async () => {
      mockParams = { repoName: 'maven-central' };
      renderWithProviders(<BrowsePage />);

      const copyBtn = screen.getByRole('button', { name: /copy url to clipboard/i });
      await userEvent.click(copyBtn);

      expect(navigator.clipboard.writeText).toHaveBeenCalled();
      expect(mockToast.success).toHaveBeenCalledWith('URL copied to clipboard');
    });
  });

  describe('Multiple repositories', () => {
    it('switches between repositories via Browse breadcrumb', async () => {
      mockParams = { repoName: 'maven-central' };
      renderWithProviders(<BrowsePage />);

      expect(screen.getByTestId('tree-repo-name')).toHaveTextContent('maven-central');

      await userEvent.click(screen.getByRole('button', { name: /^browse$/i }));

      // Should be back on list view
      expect(screen.getByTestId('mock-repository-list')).toBeInTheDocument();
    });
  });

  describe('Repository name in detail panel', () => {
    it('passes correct repository name to detail panel', async () => {
      mockParams = { repoName: 'maven-central' };
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('tree-node-component'));
      expect(screen.getByTestId('detail-repo-name')).toHaveTextContent('maven-central');
    });
  });

  describe('URL-based navigation', () => {
    beforeEach(() => {
      mockParams = { repoName: 'maven-central' };
      mockGo.mockClear();
    });

    it('updates URL with path when selecting a tree node', async () => {
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('tree-node-component'));

      expect(mockGo).toHaveBeenCalledWith('preview.browse.browse.repo.path', {
        repoName: 'maven-central',
        path: 'org/apache/maven',
        tab: 'summary',
      });
    });

    it('updates URL with path when selecting an asset node', async () => {
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('tree-node-asset'));

      expect(mockGo).toHaveBeenCalledWith('preview.browse.browse.repo.path', {
        repoName: 'maven-central',
        path: 'org/apache/maven/pom.xml',
        tab: 'summary',
      });
    });

    it('updates URL with path when selecting a folder node', async () => {
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('tree-node-folder'));

      expect(mockGo).toHaveBeenCalledWith('preview.browse.browse.repo.path', {
        repoName: 'maven-central',
        path: 'org/apache',
        tab: 'summary',
      });
    });

    it('preserves existing tab param when selecting a node', async () => {
      mockParams = { repoName: 'maven-central', tab: 'attributes' };
      renderWithProviders(<BrowsePage />);

      await userEvent.click(screen.getByTestId('tree-node-component'));

      expect(mockGo).toHaveBeenCalledWith('preview.browse.browse.repo.path', {
        repoName: 'maven-central',
        path: 'org/apache/maven',
        tab: 'attributes',
      });
    });
  });
});
