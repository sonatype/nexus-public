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
import {render, screen, waitFor} from '@testing-library/react';
import '@testing-library/jest-dom';

import {SearchCollapsibleNav} from './LeftNavigationMenuRadix';

const mockSetFormats = jest.fn();
const mockUseIsVisible = jest.fn(() => true);

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
  useIsVisible: (...args) => mockUseIsVisible(...args),
}));

jest.mock('../super/search/unified/useRepositories', () => ({
  useRepositories: () => ({
    availableFormats: new Set(['maven2', 'nuget', 'pub']),
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
      SEARCH: {
        ROOT: 'browse.search',
        GENERIC: 'browse.search.generic',
        CUSTOM: 'browse.search.custom',
        MAVEN: 'browse.search.maven',
        NUGET: 'browse.search.nuget',
        PUB: 'browse.search.pub',
      },
    },
  },
}));

jest.mock('../shared', () => ({
  Tooltip: ({children}) => children,
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
  useRouter: () => ({
    globals: {
      current: {name: 'browse.search.generic'},
    },
    stateRegistry: {
      get: (name) => ({
        data: {
          visibilityRequirements: {
            permissionToken: name,
          },
        },
      }),
    },
  }),
  useIsActive: () => false,
  useSref: (name) => ({
    href: `#/${name}`,
    onClick: jest.fn(),
  }),
}));

describe('SearchCollapsibleNav', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('shows format routes from available repositories', async () => {
    render(<SearchCollapsibleNav isCollapsed={false} />);

    await waitFor(() => {
      expect(screen.getByText('Custom')).toBeInTheDocument();
    });

    // Formats come from useRepositories mock which returns maven2, nuget, pub
    expect(screen.getByText('Maven')).toBeInTheDocument();
    expect(screen.getByText('NuGet')).toBeInTheDocument();
    expect(screen.getByText('Pub')).toBeInTheDocument();
  });
});
