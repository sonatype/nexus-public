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
import { act, renderHook } from '@testing-library/react';

let transitionHookCallback: ((transition: { to: () => { name?: string } }) => void) | null = null;

const mockSetFormats = jest.fn();
const mockUseRepositories = jest.fn();
const mockUseIsPreviewUI = jest.fn(() => true);
const mockIsSonatypeDevMode = jest.fn(() => false);
const mockIsProEdition = jest.fn(() => true);
const mockUseDefaultAdminRouteName = jest.fn<string | null, []>(() => 'admin.system.api');
const mockUseSideNavbarOpenState = jest.fn(() => [true, jest.fn()]);

// Mock ExtJS global so the BrowseableFormats sync effect has a controller to call.
beforeAll(() => {
  (window as { Ext?: unknown }).Ext = {
    getApplication: () => ({
      getController: () => ({
        setFormats: mockSetFormats,
      }),
    }),
  };
});

afterAll(() => {
  delete (window as { Ext?: unknown }).Ext;
});

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  useRepositories: (...args: unknown[]) => mockUseRepositories(...args),
  useIsPreviewUI: (...args: unknown[]) => mockUseIsPreviewUI(...args),
  isSonatypeDevMode: (...args: unknown[]) => mockIsSonatypeDevMode(...args),
  useSideNavbarOpenState: (...args: unknown[]) => mockUseSideNavbarOpenState(...args),
  ExtJS: { isProEdition: (...args: unknown[]) => mockIsProEdition(...args) },
}));

jest.mock('@uirouter/react', () => ({
  useIsActive: () => false,
  useSref: (name: string) => ({
    href: `#/${name}`,
    onClick: jest.fn(),
  }),
  useTransitionHook: (
    _hookName: string,
    _criteria: unknown,
    callback: (transition: { to: () => { name?: string } }) => void
  ) => {
    transitionHookCallback = callback;
  },
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
    },
    ADMIN: {
      DIRECTORY: 'admin',
    },
  },
}));

jest.mock('./useDefaultAdminRouteName', () => ({
  useDefaultAdminRouteName: () => mockUseDefaultAdminRouteName(),
}));

import { useLeftNavigationMenu, useSearchCollapsibleNav } from './useLeftNavigationMenu';

const REPOSITORIES_WITH_FORMATS = {
  availableFormats: new Set(['alpine', 'maven2', 'nuget', 'pub']),
  loading: false,
  repositories: [],
  formatCounts: {},
  error: undefined,
  refetch: jest.fn(),
};

describe('useSearchCollapsibleNav', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    transitionHookCallback = null;
    mockUseRepositories.mockReturnValue(REPOSITORIES_WITH_FORMATS);
  });

  it('filters the format catalogue down to available repository formats', () => {
    const { result } = renderHook(() => useSearchCollapsibleNav(false));

    expect(result.current.visibleFormats).toEqual([
      { id: 'alpine', label: 'Alpine', routeName: 'browse.search.alpine' },
      { id: 'maven', label: 'Maven', routeName: 'browse.search.maven' },
      { id: 'nuget', label: 'NuGet', routeName: 'browse.search.nuget' },
      { id: 'pub', label: 'Pub', routeName: 'browse.search.pub' },
    ]);
  });

  it('returns no formats while repositories are still loading', () => {
    mockUseRepositories.mockReturnValue({ ...REPOSITORIES_WITH_FORMATS, loading: true });

    const { result } = renderHook(() => useSearchCollapsibleNav(false));

    expect(result.current.visibleFormats).toEqual([]);
  });

  it('exposes the custom search route name and a parent href', () => {
    const { result } = renderHook(() => useSearchCollapsibleNav(false));

    expect(result.current.customRouteName).toBe('browse.search.custom');
    expect(result.current.parentHref).toBe('#/browse.search.generic');
  });

  it('toggles expansion when the parent is clicked and not collapsed', () => {
    const { result } = renderHook(() => useSearchCollapsibleNav(false));
    const event = { preventDefault: jest.fn() } as unknown as React.MouseEvent<HTMLAnchorElement>;

    expect(result.current.isExpanded).toBe(false);

    act(() => result.current.handleParentClick(event));
    expect(result.current.isExpanded).toBe(true);
    expect(event.preventDefault).toHaveBeenCalledTimes(1);

    act(() => result.current.handleParentClick(event));
    expect(result.current.isExpanded).toBe(false);
  });

  it('does not toggle or prevent default when collapsed', () => {
    const { result } = renderHook(() => useSearchCollapsibleNav(true));
    const event = { preventDefault: jest.fn() } as unknown as React.MouseEvent<HTMLAnchorElement>;

    act(() => result.current.handleParentClick(event));

    expect(result.current.isExpanded).toBe(false);
    expect(event.preventDefault).not.toHaveBeenCalled();
  });

  it('auto-expands on transition to a search route', () => {
    const { result } = renderHook(() => useSearchCollapsibleNav(false));

    expect(result.current.isExpanded).toBe(false);

    act(() => {
      transitionHookCallback?.({ to: () => ({ name: 'browse.search.maven' }) });
    });

    expect(result.current.isExpanded).toBe(true);
  });

  it('does not auto-expand on transition to a non-search route', () => {
    const { result } = renderHook(() => useSearchCollapsibleNav(false));

    act(() => {
      transitionHookCallback?.({ to: () => ({ name: 'browse.welcome' }) });
    });

    expect(result.current.isExpanded).toBe(false);
  });

  it('syncs available formats to the ExtJS BrowseableFormats controller', () => {
    renderHook(() => useSearchCollapsibleNav(false));

    expect(mockSetFormats).toHaveBeenCalledWith([
      { id: 'alpine' },
      { id: 'maven2' },
      { id: 'nuget' },
      { id: 'pub' },
    ]);
  });

  it('does not sync to ExtJS while loading or with no formats', () => {
    mockUseRepositories.mockReturnValue({ ...REPOSITORIES_WITH_FORMATS, availableFormats: new Set(), loading: true });

    renderHook(() => useSearchCollapsibleNav(false));

    expect(mockSetFormats).not.toHaveBeenCalled();
  });
});

describe('useLeftNavigationMenu', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseIsPreviewUI.mockReturnValue(true);
    mockIsSonatypeDevMode.mockReturnValue(false);
    mockIsProEdition.mockReturnValue(true);
    mockUseDefaultAdminRouteName.mockReturnValue('admin.system.api');
    // useSideNavbarOpenState returns [isOpen, toggle]; open (true) => not collapsed.
    mockUseSideNavbarOpenState.mockReturnValue([true, jest.fn()]);
  });

  it('returns collapsed/preview state and a visible settings entry', () => {
    const { result } = renderHook(() => useLeftNavigationMenu());

    expect(result.current.isCollapsed).toBe(false);
    expect(result.current.isPreviewUI).toBe(true);
    expect(result.current.isProEdition).toBe(true);
    expect(result.current.showTestHub).toBe(false);
    expect(result.current.adminInitialRouteName).toBe('admin.system.api');
    expect(result.current.isSettingsVisible).toBe(true);
  });

  it('reflects the collapsed state from useSideNavbarOpenState', () => {
    // isOpen === false => collapsed.
    mockUseSideNavbarOpenState.mockReturnValue([false, jest.fn()]);

    const { result } = renderHook(() => useLeftNavigationMenu());

    expect(result.current.isCollapsed).toBe(true);
  });

  it('reflects Classic UI when not in Preview UI', () => {
    mockUseIsPreviewUI.mockReturnValue(false);

    const { result } = renderHook(() => useLeftNavigationMenu());

    expect(result.current.isPreviewUI).toBe(false);
  });

  it('reports CE mode (isProEdition false) when ExtJS is not Pro', () => {
    mockIsProEdition.mockReturnValue(false);

    const { result } = renderHook(() => useLeftNavigationMenu());

    expect(result.current.isProEdition).toBe(false);
  });

  it('shows the Test Hub flag when isSonatypeDevMode is true and Preview UI is active', () => {
    mockIsSonatypeDevMode.mockReturnValue(true);

    const { result } = renderHook(() => useLeftNavigationMenu());

    expect(result.current.showTestHub).toBe(true);
  });

  it('hides Test Hub when Preview UI is not active even if isSonatypeDevMode is true', () => {
    mockUseIsPreviewUI.mockReturnValue(false);
    mockIsSonatypeDevMode.mockReturnValue(true);

    const { result } = renderHook(() => useLeftNavigationMenu());

    expect(result.current.showTestHub).toBe(false);
  });

  it('hides the settings entry when no admin route is visible', () => {
    mockUseDefaultAdminRouteName.mockReturnValue(null);

    const { result } = renderHook(() => useLeftNavigationMenu());

    expect(result.current.adminInitialRouteName).toBeNull();
    expect(result.current.isSettingsVisible).toBe(false);
  });
});
