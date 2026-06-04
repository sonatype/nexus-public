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
import {render, screen, waitFor, act} from '@testing-library/react';
import '@testing-library/jest-dom';

let transitionHookCallback = null;

const mockSetFormats = jest.fn();
const mockUseIsVisible = jest.fn(() => true);
const mockUseIsPreviewUI = jest.fn(() => false);

// Mock ExtJS global before tests
beforeAll(() => {
  window.Ext = {
    getApplication: () => ({
      getController: () => ({
        setFormats: mockSetFormats,
      }),
    }),
  };
});

afterAll(() => {
  delete window.Ext;
});

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  useIsVisible: (...args) => mockUseIsVisible(...args),
  useIsPreviewUI: (...args) => mockUseIsPreviewUI(...args),
  useContextAwareRouteName: (name) => name,
  NavItemBox: ({ name, text }) => <div data-testid={`nav-item-${name}`}>{text}</div>,
  PreviewUIContext: {
    Provider: ({ children }) => children,
  },
  useRepositories: () => ({
    availableFormats: new Set(['alpine', 'maven2', 'nuget', 'pub']),
    loading: false,
    repositories: [],
    formatCounts: {},
    error: undefined,
    refetch: jest.fn(),
  }),
}));

jest.mock('../../routerConfig/routeNames/routeNames', () => ({
  ROUTE_NAMES: {
    BROWSE: {
      WELCOME: { ROOT: 'browse.welcome' },
      SEARCH: {
        ROOT: 'browse.search',
        UNIFIED: 'browse.search.unified',
        GENERIC: 'browse.search.generic',
        CUSTOM: 'browse.search.custom',
        ALPINE: 'browse.search.alpine',
        MAVEN: 'browse.search.maven',
        NUGET: 'browse.search.nuget',
        PUB: 'browse.search.pub',
      },
      BROWSE: { ROOT: 'browse.browse' },
      UPLOAD: { ROOT: 'browse.upload', LIST: 'browse.upload.list' },
      TAGS: { ROOT: 'browse.tags' },
      MALWARERISK: { ROOT: 'browse.malwarerisk' },
    },
    ADMIN: {
      DIRECTORY: 'admin',
    },
  },
}));

jest.mock('../shared', () => ({
  Tooltip: ({children}) => children,
}));

jest.mock('../../hooks/useSideNavbarCollapsedState', () => () => [false, jest.fn()]);

jest.mock('./useDefaultAdminRouteName', () => ({
  useDefaultAdminRouteName: () => 'admin.system.api',
}));

jest.mock('@radix-ui/themes', () => ({
  Box: ({children, ...props}) => <div {...props}>{children}</div>,
  Flex: ({children, ...props}) => <div {...props}>{children}</div>,
  ScrollArea: ({children, ...props}) => <div {...props}>{children}</div>,
  Separator: (props) => <hr {...props} />,
}));

jest.mock('lucide-react', () => {
  const Icon = () => <span />;
  return {
    LayoutDashboard: Icon,
    Search: Icon,
    FolderOpen: Icon,
    HardDriveUpload: Icon,
    Tags: Icon,
    Settings: Icon,
    ExternalLink: Icon,
    ShieldAlert: Icon,
    FlaskConical: Icon,
    FileCode: Icon,
    ScrollText: Icon,
    ChevronDown: Icon,
    ChevronUp: Icon,
  };
});

jest.mock('@uirouter/react', () => ({
  useIsActive: () => false,
  useSref: (name) => ({
    href: `#/${name}`,
    onClick: jest.fn(),
  }),
  useTransitionHook: (hookName, criteria, callback) => {
    transitionHookCallback = callback;
  },
}));

import {SearchCollapsibleNav} from './LeftNavigationMenuRadix';

describe('SearchCollapsibleNav', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    transitionHookCallback = null;
  });

  it('shows format routes from available repositories', async () => {
    const {container} = render(<SearchCollapsibleNav isCollapsed={false} />);

    // Simulate transition to a search route to expand the menu
    act(() => {
      if (transitionHookCallback) {
        transitionHookCallback({to: () => ({name: 'browse.search.maven'})});
      }
    });

    await waitFor(() => {
      expect(screen.getByText('Custom')).toBeInTheDocument();
    });

    // Formats come from useRepositories mock which returns alpine, maven2, nuget, pub
    expect(screen.getByText('Alpine')).toBeInTheDocument();
    expect(screen.getByText('Maven')).toBeInTheDocument();
    expect(screen.getByText('NuGet')).toBeInTheDocument();
    expect(screen.getByText('Pub')).toBeInTheDocument();
  });

  it('auto-expands when navigating to a search route', async () => {
    const {container} = render(<SearchCollapsibleNav isCollapsed={false} />);

    // Initially should not be expanded (no expanded class)
    const collapsible = container.querySelector('.search-collapsible');
    expect(collapsible).not.toHaveClass('search-collapsible--expanded');

    // Simulate transition to a search route
    act(() => {
      if (transitionHookCallback) {
        transitionHookCallback({to: () => ({name: 'browse.search.maven'})});
      }
    });

    await waitFor(() => {
      expect(container.querySelector('.search-collapsible')).toHaveClass('search-collapsible--expanded');
    });
  });

  it('does not auto-expand when navigating to a non-search route', async () => {
    const {container} = render(<SearchCollapsibleNav isCollapsed={false} />);

    // Simulate transition to a non-search route
    act(() => {
      if (transitionHookCallback) {
        transitionHookCallback({to: () => ({name: 'browse.welcome'})});
      }
    });

    await waitFor(() => {
      expect(container.querySelector('.search-collapsible')).not.toHaveClass('search-collapsible--expanded');
    });
  });
});

describe('LeftNavigationMenuRadix', () => {
  let LeftNavigationMenuRadix;

  beforeAll(() => {
    LeftNavigationMenuRadix = require('./LeftNavigationMenuRadix').default;
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shouldShowApiNavItemInPreviewUI', () => {
    mockUseIsPreviewUI.mockReturnValue(true);

    render(<LeftNavigationMenuRadix />);

    expect(screen.getByTestId('nav-item-preview.browse.api')).toBeInTheDocument();
    expect(screen.getByText('API')).toBeInTheDocument();
  });

  it('shouldNotShowApiNavItemInClassicUI', () => {
    mockUseIsPreviewUI.mockReturnValue(false);

    render(<LeftNavigationMenuRadix />);

    expect(screen.queryByTestId('nav-item-preview.browse.api')).not.toBeInTheDocument();
  });
});
