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
import '@testing-library/jest-dom';

import type { UseLeftNavigationMenuResult, UseSearchCollapsibleNavResult } from './useLeftNavigationMenu';

const mockUseSearchCollapsibleNav = jest.fn<UseSearchCollapsibleNavResult, [boolean]>();
const mockUseLeftNavigationMenu = jest.fn<UseLeftNavigationMenuResult, []>();

// Component tests mock the hook layer: presentation is verified in isolation.
jest.mock('./useLeftNavigationMenu', () => ({
  useSearchCollapsibleNav: (isCollapsed: boolean) => mockUseSearchCollapsibleNav(isCollapsed),
  useLeftNavigationMenu: () => mockUseLeftNavigationMenu(),
}));

// NavItemBox is the shared Preview UI primitive; stub it to a simple node.
// useContextAwareRouteName is used by the local Classic UI SearchNavItem leaf.
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  useContextAwareRouteName: (name: string) => name,
  NavItemBox: ({ name, text, badge }: { name: string; text: string; badge?: number | null }) => (
    <div data-testid={`nav-item-${name}`}>
      {text}
      {badge != null && <span data-testid={`nav-badge-${name}`}>{badge}</span>}
    </div>
  ),
  // The Security section itself (label, links, filtering, group naming) is covered by SecurityNavSection.test.tsx
  // in nexus-ui-plugin; here we verify only how the menu wires it.
  SecurityNavSection: ({ isCollapsed, badgeCount }: { isCollapsed?: boolean; badgeCount?: number | null }) => (
    <div data-testid="security-nav-section" data-collapsed={String(!!isCollapsed)} data-badge={String(badgeCount)} />
  ),
  PreviewUIContext: {
    Provider: ({ children }: { children: React.ReactNode }) => children,
  },
}));

// The local SearchNavItem leaf binds to the router; stub those bindings.
jest.mock('@uirouter/react', () => ({
  useIsActive: () => false,
  useSref: (name: string) => ({
    href: `#/${name}`,
    onClick: jest.fn(),
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
      },
      BROWSE: { ROOT: 'browse.browse' },
      TRASH: { ROOT: 'browse.trash' },
      UPLOAD: { ROOT: 'browse.upload', LIST: 'browse.upload.list' },
      TAGS: { ROOT: 'browse.tags' },
      MALWARERISK: { ROOT: 'browse.malwarerisk' },
    },
    ADMIN: {
      DIRECTORY: 'admin',
    },
  },
}));

jest.mock('@radix-ui/themes', () => ({
  Box: ({ children, ...props }: { children?: React.ReactNode }) => <div {...props}>{children}</div>,
  Flex: ({ children, ...props }: { children?: React.ReactNode }) => <div {...props}>{children}</div>,
  ScrollArea: ({ children, ...props }: { children?: React.ReactNode }) => <div {...props}>{children}</div>,
  Separator: (props: Record<string, unknown>) => <hr {...props} />,
  Text: ({ children, ...props }: { children?: React.ReactNode }) => <span {...props}>{children}</span>,
}));

jest.mock('lucide-react', () => {
  const Icon = () => <span />;
  return {
    LayoutDashboard: Icon,
    Search: Icon,
    FolderOpen: Icon,
    HardDriveUpload: Icon,
    Tags: Icon,
    Trash2: Icon,
    Settings: Icon,
    ShieldAlert: Icon,
    FileCode: Icon,
    ChevronDown: Icon,
    ChevronUp: Icon,
  };
});

import LeftNavigationMenuRadix, { SearchCollapsibleNav } from './LeftNavigationMenuRadix';

const collapsibleResult = (
  overrides: Partial<UseSearchCollapsibleNavResult> = {}
): UseSearchCollapsibleNavResult => ({
  isExpanded: false,
  isParentActive: false,
  parentHref: '#/browse.search.generic',
  navigateToSearch: jest.fn(),
  handleParentClick: jest.fn(),
  customRouteName: 'browse.search.custom',
  visibleFormats: [],
  ...overrides,
});

const menuResult = (overrides: Partial<UseLeftNavigationMenuResult> = {}): UseLeftNavigationMenuResult => ({
  isCollapsed: false,
  isPreviewUI: false,
  adminInitialRouteName: 'admin.system.api',
  isSettingsVisible: true,
  isLicensedEdition: true,
  isSecuritySectionVisible: false,
  securityBadgeCount: null,
  ...overrides,
});

describe('SearchCollapsibleNav', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSearchCollapsibleNav.mockReturnValue(collapsibleResult());
  });

  it('renders the custom entry and filtered formats when expanded', () => {
    mockUseSearchCollapsibleNav.mockReturnValue(
      collapsibleResult({
        isExpanded: true,
        visibleFormats: [
          { id: 'alpine', label: 'Alpine', routeName: 'browse.search.alpine' },
          { id: 'maven', label: 'Maven', routeName: 'browse.search.maven' },
        ],
      })
    );

    render(<SearchCollapsibleNav isCollapsed={false} />);

    expect(screen.getByText('Custom')).toBeInTheDocument();
    expect(screen.getByText('Alpine')).toBeInTheDocument();
    expect(screen.getByText('Maven')).toBeInTheDocument();
  });

  it('does not render the submenu when collapsed', () => {
    const { container } = render(<SearchCollapsibleNav isCollapsed={false} />);

    expect(container.querySelector('.search-collapsible')).not.toHaveClass('search-collapsible--expanded');
    expect(screen.queryByText('Custom')).not.toBeInTheDocument();
  });

  it('reflects expansion state from the hook', () => {
    mockUseSearchCollapsibleNav.mockReturnValue(collapsibleResult({ isExpanded: true }));

    const { container } = render(<SearchCollapsibleNav isCollapsed={false} />);

    expect(container.querySelector('.search-collapsible')).toHaveClass('search-collapsible--expanded');
  });

  it('renders a minimal link without submenu when collapsed', () => {
    const { container } = render(<SearchCollapsibleNav isCollapsed />);

    expect(container.querySelector('.search-collapsible')).not.toBeInTheDocument();
    expect(container.querySelector('.guide-nav-item')).toBeInTheDocument();
    expect(screen.queryByText('Custom')).not.toBeInTheDocument();
  });
});

describe('LeftNavigationMenuRadix', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSearchCollapsibleNav.mockReturnValue(collapsibleResult());
  });

  it('shouldShowApiNavItemInPreviewUI', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: true }));

    render(<LeftNavigationMenuRadix />);

    expect(screen.getByTestId('nav-item-preview.browse.api')).toBeInTheDocument();
    expect(screen.getByText('API')).toBeInTheDocument();
  });

  it('shouldNotShowApiNavItemInClassicUI', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: false }));

    render(<LeftNavigationMenuRadix />);

    expect(screen.queryByTestId('nav-item-preview.browse.api')).not.toBeInTheDocument();
  });

  it('hides the settings entry when not visible', () => {
    mockUseLeftNavigationMenu.mockReturnValue(
      menuResult({ isPreviewUI: true, isSettingsVisible: false, adminInitialRouteName: null })
    );

    render(<LeftNavigationMenuRadix />);

    expect(screen.queryByTestId('nav-item-preview.settings')).not.toBeInTheDocument();
  });

  it('shouldNotShowTagsNavItemInCEMode', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isLicensedEdition: false }));

    render(<LeftNavigationMenuRadix />);

    expect(screen.queryByTestId('nav-item-browse.tags')).not.toBeInTheDocument();
  });

  it('shouldShowTagsNavItemInProEdition', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isLicensedEdition: true }));

    render(<LeftNavigationMenuRadix />);

    expect(screen.getByTestId('nav-item-browse.tags')).toBeInTheDocument();
    expect(screen.getByText('Tags')).toBeInTheDocument();
  });

  it('shouldShowTrashNavItemInClassicUI', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: false }));

    render(<LeftNavigationMenuRadix />);

    expect(screen.getByTestId('nav-item-browse.trash')).toBeInTheDocument();
    expect(screen.getByText('Trash')).toBeInTheDocument();
  });

  it('shouldShowTrashNavItemInPreviewUI', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: true }));

    render(<LeftNavigationMenuRadix />);

    expect(screen.getByTestId('nav-item-preview.browse.trash')).toBeInTheDocument();
  });
});

describe('LeftNavigationMenuRadix Security section', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseSearchCollapsibleNav.mockReturnValue(collapsibleResult());
  });

  it('does not render the Security section when it is not visible', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: true, isSecuritySectionVisible: false }));
    render(<LeftNavigationMenuRadix />);
    expect(screen.queryByTestId('security-nav-section')).not.toBeInTheDocument();
  });

  it('renders the Security section when visible, and no single Security link', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: true, isSecuritySectionVisible: true }));
    render(<LeftNavigationMenuRadix />);
    expect(screen.getByTestId('security-nav-section')).toHaveAttribute('data-collapsed', 'false');
    expect(screen.queryByTestId('nav-item-preview.security')).not.toBeInTheDocument();
  });

  it('places the Security section after Malware Risk and before API', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: true, isSecuritySectionVisible: true }));
    render(<LeftNavigationMenuRadix />);
    const before = screen.getByTestId('nav-item-preview.browse.malwarerisk');
    const section = screen.getByTestId('security-nav-section');
    const api = screen.getByTestId('nav-item-preview.browse.api');
    expect(before.compareDocumentPosition(section) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(section.compareDocumentPosition(api) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('adds no separator of its own around the section; the menu is unchanged when it is hidden', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: true, isSecuritySectionVisible: false }));
    const hidden = render(<LeftNavigationMenuRadix />);
    const separatorsWhenHidden = hidden.container.querySelectorAll('hr').length;
    const itemsWhenHidden = hidden.container.querySelectorAll('[data-testid^="nav-item-"]').length;
    hidden.unmount();

    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: true, isSecuritySectionVisible: true }));
    const shown = render(<LeftNavigationMenuRadix />);
    expect(shown.container.querySelectorAll('hr').length).toBe(separatorsWhenHidden);
    expect(shown.container.querySelectorAll('[data-testid^="nav-item-"]').length).toBe(itemsWhenHidden);
  });

  it('tells the Security section when the menu is collapsed', () => {
    mockUseLeftNavigationMenu.mockReturnValue(
      menuResult({ isPreviewUI: true, isSecuritySectionVisible: true, isCollapsed: true })
    );
    render(<LeftNavigationMenuRadix />);
    expect(screen.getByTestId('security-nav-section')).toHaveAttribute('data-collapsed', 'true');
  });

  it('leaves the Malware Risk item unchanged when the Security section is shown', () => {
    mockUseLeftNavigationMenu.mockReturnValue(menuResult({ isPreviewUI: true, isSecuritySectionVisible: true }));
    render(<LeftNavigationMenuRadix />);
    expect(screen.getByTestId('nav-item-preview.browse.malwarerisk')).toBeInTheDocument();
  });

  it('passes the quarantine count to the Security section as its badge count', () => {
    mockUseLeftNavigationMenu.mockReturnValue(
      menuResult({ isPreviewUI: true, isSecuritySectionVisible: true, securityBadgeCount: 6 })
    );
    render(<LeftNavigationMenuRadix />);
    expect(screen.getByTestId('security-nav-section')).toHaveAttribute('data-badge', '6');
  });

  it('passes a null badge count through', () => {
    mockUseLeftNavigationMenu.mockReturnValue(
      menuResult({ isPreviewUI: true, isSecuritySectionVisible: true, securityBadgeCount: null })
    );
    render(<LeftNavigationMenuRadix />);
    expect(screen.getByTestId('security-nav-section')).toHaveAttribute('data-badge', 'null');
  });
});
