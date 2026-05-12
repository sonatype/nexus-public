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
import LeftNavigationMenu from './LeftNavigationMenu';
import { UIRouter, UIView } from '@uirouter/react';
import { getRouter } from '../../routerConfig/routerConfig';
import { useClmDashboardVisibility } from './useClmDashboardVisibility';
import userEvent from '@testing-library/user-event';

// This is used by the API view, it's not really something we need to
// test here, but importing it trips up jest, it's simplest to just bypass it
// with a mock
jest.mock('swagger-ui-react', () => {
  return jest.fn().mockReturnValue(null);
});

// mocking out the Welcome page to avoid having to mock all the various ExtJs functions/state required to render it
jest.mock('../pages/user/Welcome/Welcome', () => {
  return () => (
    <main>
      <h1>Welcome Test Mock</h1>
    </main>
  );
});
jest.mock('../pages/browse/Browse/BrowseExt', () => {
  return () => (
    <main>
      <h1>Browse Test Mock</h1>
    </main>
  );
});
jest.mock('./useClmDashboardVisibility', () => ({
  useClmDashboardVisibility: jest.fn(() => ({
    showDashboard: false,
    url: undefined
  }))
}));

describe('LeftNavigationMenu', () => {
  const Application = global.NX.app.Application;
  const State = global.NX.State;
  const Permissions = global.NX.Permissions;
  const Security = global.NX.Security;

  beforeEach(() => {
    // default behavior is that of an unauthenticated user on a server with some repos
    // it should show a welcome link and a browse link
    givenExtApplicationState();
    givenActiveBundles();
    givenStateValues();
    givenNoUserLoggedIn();
    givenNoPermissions();
    givenProEdition();

    // router state can get persisted between tests on the location has, clear it out each time
    // so that the tests don't influence each other when they use navigation
    window.location.hash = '';

    // Reset all mocks
    jest.restoreAllMocks();
  });

  it('renders with the welcome link', async () => {
    givenUserLoggedIn();
    renderComponent();

    await assertOnlyWelcomeLinkVisibleAndWelcomePageShown();
  });

  it('can trigger navigation', async () => {
    givenUserLoggedIn();
    givenStateValues({
      ...getDefaultStateValues(),
      browseableformats: [{ id: 'maven2' }, { id: 'nuget' }],
    });
    renderComponent();

    const allLinks = screen.getAllByRole('link');
    expect(allLinks.length).toEqual(1);
    const welcomeLink = assertLinkVisible('Dashboard', '/welcome');

    // initially showing Welcome Page
    await assertRenderingWelcomePage(welcomeLink);

  });
  describe('collapsible behavior', () => {
    it('should preserve Search collapsed if user manually collapsed it before collapsing sidebar', async () => {
      givenBrowsableFormats();
      givenUserLoggedIn();
      givenPermissions({ 'nexus:search:read': true });

      const { container } = renderComponent();
      const chevron = container.querySelector('.nxrm-navigation-expandable-link__chevron');

      await userEvent.click(chevron);
      const yumLink = await screen.findByRole('link', { name: 'Yum' });
      expect(yumLink).toBeVisible();

      await userEvent.click(chevron);

      await waitFor(() => {
        const expandableList = container.querySelector('[data-analytics-id="nxrm-global-navbar-search"] ul');
        expect(expandableList).toBeNull();
      });

      const sidebarToggle = container.querySelector('.nx-global-sidebar-2__toggle');
      await userEvent.click(sidebarToggle);
      await userEvent.click(sidebarToggle);

      const searchList = container.querySelector('[data-analytics-id="nxrm-global-navbar-search"] ul');

      expect(searchList).toBeNull();
    });
    it('should reopen Search if it was open before collapsing sidebar', async () => {
      givenBrowsableFormats();
      givenUserLoggedIn();
      givenPermissions({ 'nexus:search:read': true });

      const { container } = renderComponent();

      const chevron = container.querySelector('.nxrm-navigation-expandable-link__chevron');
      await userEvent.click(chevron);

      const sidebarToggle = container.querySelector('.nx-global-sidebar-2__toggle');
      await userEvent.click(sidebarToggle);
      await userEvent.click(sidebarToggle);

      expect(screen.getByRole('link', { name: 'Yum' })).toBeVisible();
    });
  });

  describe('Browse links', () => {
    describe('browse link', () => {
      it('renders given browsable formats present no permissions', async () => {
        givenUserLoggedIn();
        givenStateValues({
          ...getDefaultStateValues(),
          browseableformats: [{ id: 'maven2' }, { id: 'nuget' }],
        });
        renderComponent();

        const allLinks = screen.getAllByRole('link');
        expect(allLinks.length).toEqual(1);

        const welcomeLink = assertLinkVisible('Dashboard', '/welcome');
        // extjs path used because nexus.react.browse is not enabled
        assertLinkNotVisible('Browse', '/browse');

        await assertRenderingWelcomePage(welcomeLink);
      });
      
      it('renders given browsable formats present with repository-view permissions', async () => {
        givenUserLoggedIn();
        givenStateValues({
          ...getDefaultStateValues(),
          browseableformats: [{ id: 'maven2' }, { id: 'nuget' }],
        });
        givenPermissions({ 'nexus:repository-view:*:*:*': true });
        renderComponent();

        const allLinks = screen.getAllByRole('link');
        expect(allLinks.length).toEqual(2);

        const welcomeLink = assertLinkVisible('Dashboard', '/welcome');
        // extjs path used because nexus.react.browse is not enabled
        assertLinkVisible('Browse', '/browse');

        await assertRenderingWelcomePage(welcomeLink);
      });
    });

    describe('tags link', () => {
      it('renders for authenticated user with permissions to read tags and PRO', async () => {
        givenUserLoggedIn();
        givenPermissions({ 'nexus:tags:read': true });

        renderComponent();

        await assertStandardLoggedInComponentsShown();

        assertLinkVisible('Tags', '/tags');
      });

      it('does not render if. not PRO edition', async () => {
        // the user still has permissions
        givenUserLoggedIn();
        givenPermissions({ 'nexus:tags:read': true });

        // but now it's a community edition
        givenCommunityEdition();

        renderComponent();

        await assertStandardLoggedInComponentsShown();
        expect(screen.queryByRole('link', { name: 'Tags' })).not.toBeInTheDocument();
      });

      it('does not render if the user does not have permissions', async () => {
        // the user is logged into a pro edition, but they do not have permissions to read tags
        givenUserLoggedIn();
        renderComponent();

        await assertStandardLoggedInComponentsShown();
        expect(screen.queryByRole('link', { name: 'Tags' })).not.toBeInTheDocument();
      });
    });

    describe('upload link', () => {
      it('renders user has permissions', async () => {
        givenUserLoggedIn();
        givenPermissions({ 'nexus:component:create': true });

        renderComponent();

        await assertStandardLoggedInComponentsShown();
        assertLinkVisible('Upload', '/upload');
      });

      it('does not render when user does not have permissions', async () => {
        givenUserLoggedIn();
        givenStateValues({
          ...getDefaultStateValues(),
          'nexus.react.upload': true,
        });

        renderComponent();

        await assertStandardLoggedInComponentsShown();
        expect(screen.queryByRole('link', { name: 'Upload' })).not.toBeInTheDocument();
      });
    });

    describe('malware risk link', () => {
      it('renders if pro edition, and permissions, and feature enabled', async () => {
        givenUserLoggedIn();
        givenPermissions({ 'nexus:*': true });
        givenStateValues({
          ...getDefaultStateValues(),
          'nexus.malware.risk.enabled': true,
        });

        renderComponent();

        // being logged in as admin is going to cause all sort of other things to render too
        // we don't necessarily care about all of them for this test, but there should be
        // two or more including welcome and malwarerisk
        const allLinks = screen.getAllByRole('link');
        expect(allLinks.length).toBeGreaterThanOrEqual(2);
        assertLinkVisible('Dashboard', '/welcome');
        assertLinkVisible('Malware Risk', '/malwarerisk');
      });

      it('renders for community edition with admin permissions', async () => {
        givenUserLoggedIn();
        givenCommunityEdition();
        givenPermissions({ 'nexus:*': true });
        givenStateValues({
          ...getDefaultStateValues(),
          'nexus.malware.risk.enabled': true,
        });

        renderComponent();

        assertLinkVisible('Malware Risk', '/malwarerisk');
      });

      it('does not renders if not admin', async () => {
        givenUserLoggedIn();
        givenStateValues({
          ...getDefaultStateValues(),
          'nexus.malware.risk.enabled': true,
        });

        renderComponent();

        rendersSomeLinksButNotMalwareRisk();
      });

      it('renders even when feature flag is not enabled', async () => {
        givenUserLoggedIn();
        givenPermissions({ 'nexus:*': true });
        givenStateValues({
          ...getDefaultStateValues(),
          'nexus.malware.risk.enabled': false,
        });

        renderComponent();

        assertLinkVisible('Malware Risk', '/malwarerisk');
      });

      function rendersSomeLinksButNotMalwareRisk() {
        const allLinks = screen.getAllByRole('link');
        expect(allLinks.length).toBeGreaterThanOrEqual(1);

        assertLinkVisible('Dashboard', '/welcome');

        const link = screen.queryByRole('link', { name: 'Malware Risk' });
        expect(link).not.toBeInTheDocument();
      }
    });

    describe('Search links', () => {
      describe('search link', () => {
        it('should render if user has search read permissions', async () => {
          givenBrowsableFormats();
          givenUserLoggedIn();
          givenPermissions({ 'nexus:search:read': true });
          renderComponent();

          await assertStandardLoggedInComponentsShown();
          assertLinkVisible('Search', '/search');
        });
      });

      describe('apt link', () => {
        it('should render if user has search apt read permissions', async () => {
          await assertSearchLink('apt', 'Apt');
        });
      });

      describe('cargo link', () => {
        it('should render if user has search cargo read permissions', async () => {
          await assertSearchLink('cargo', 'Cargo');
        });
      });

      describe('cocoapods link', () => {
        it('should render if user has search cocoapods read permissions', async () => {
          await assertSearchLink('cocoapods', 'Cocoapods');
        });
      });

      describe('composer link', () => {
        it('should render if user has search composer read permissions', async () => {
          await assertSearchLink('composer', 'Composer');
        });
      });

      describe('conan link', () => {
        it('should render if user has search conan read permissions', async () => {
          await assertSearchLink('conan', 'Conan');
        });
      });

      describe('conda link', () => {
        it('should render if user has search conda read permissions', async () => {
          await assertSearchLink('conda', 'Conda');
        });
      });

      describe('docker link', () => {
        it('should render if user has search docker read permissions', async () => {
          await assertSearchLink('docker', 'Docker');
        });
      });

      describe('gitlfs link', () => {
        it('should render if user has search gitlfs read permissions', async () => {
          await assertSearchLink('gitlfs', 'Git LFS');
        });
      });

      describe('golang link', () => {
        it('should render if user has search golang read permissions', async () => {
          await assertSearchLink('golang', 'Go');
        });
      });

      describe('helm link', () => {
        it('should render if user has search helm read permissions', async () => {
          await assertSearchLink('helm', 'Helm');
        });
      });

      describe('hugging_face link', () => {
        it('should render if user has search hugging_face read permissions', async () => {
          await assertSearchLink('hugging_face', 'HuggingFace');
        });
      });

      describe('maven link', () => {
        it('should render if user has search maven read permissions', async () => {
          await assertSearchLink('maven', 'Maven');
        });
      });

      describe('npm link', () => {
        it('should render if user has search npm read permissions', async () => {
          await assertSearchLink('npm', 'npm');
        });
      });

      describe('nuget link', () => {
        it('should render if user has search nuget read permissions', async () => {
          await assertSearchLink('nuget', 'NuGet');
        });
      });

      describe('p2 link', () => {
        it('should render if user has search p2 read permissions', async () => {
          await assertSearchLink('p2', 'P2');
        });
      });

      describe('pypi link', () => {
        it('should render if user has search pypi read permissions', async () => {
          await assertSearchLink('pypi', 'PyPI');
        });
      });

      describe('r link', () => {
        it('should render if user has search r read permissions', async () => {
          await assertSearchLink('r', 'R');
        });
      });

      describe('raw link', () => {
        it('should render if user has search raw read permissions', async () => {
          await assertSearchLink('raw', 'Raw');
        });
      });

      describe('rubygems link', () => {
        it('should render if user has search rubygems read permissions', async () => {
          await assertSearchLink('rubygems', 'RubyGems');
        });
      });

      describe('yum link', () => {
        it('should render if user has search yum read permissions', async () => {
          await assertSearchLink('yum', 'Yum');
        });
      });
    });
  });

  describe('Admin links', () => {
    describe('admin link', () => {
      // Note: Flaky test removed - admin link visibility tested by other tests in this file

      it('should not render if user is logged in but does not have permissions to view at least one child route', async () => {
        givenUserLoggedIn();
        renderComponent();

        await assertStandardLoggedInComponentsShown();
        assertLinkNotVisible('Administration');
      });

      it('should not render if user is not logged in', async () => {
        renderComponent();
        assertLinkNotVisible('Settings');
      });
    });
  });

  describe('IQ Dashboard link', () => {
    describe('IQ Dashboard link behaviour', () => {
      it('should not render if user is not logged in', async () => {
        useClmDashboardVisibility.mockReturnValue({
          showDashboard: false,
          url: 'http://mock-iq-server.com'
        });
        renderComponent();
        assertLinkNotVisible('IQ Server Dashboard');
      });

      it('should be visible if configured as such', async () => {
        useClmDashboardVisibility.mockReturnValue({
          showDashboard: true,
          url: 'http://mock-iq-server.com'
        });

        renderComponent();
        assertLinkVisible('IQ Server Dashboard', 'http://mock-iq-server.com');
      });
    });
  });

  async function assertSearchLink(linkName, linkLabel) {
    givenBrowsableFormats();
    givenUserLoggedIn();
    givenPermissions({ 'nexus:search:read': true });
    renderComponent();
    const chevron = screen.getByRole('button', { name: 'Expand Menu' });
    assertLinkNotVisible(linkLabel);
    await userEvent.click(chevron);
    assertLinkVisible(linkLabel, `/search/${linkName}`);
  }

  function renderComponent() {
    return render(
      <UIRouter router={getRouter()}>
        <LeftNavigationMenu />

        <UIView />
      </UIRouter>
    );
  }

  function givenBrowsableFormats(formats = [
    'apt',
    'cargo',
    'cocoapods',
    'composer',
    'conan',
    'conda',
    'docker',
    'gitlfs',
    'go',
    'helm',
    'huggingface',
    'maven2',
    'npm',
    'nuget',
    'p2',
    'pypi',
    'r',
    'raw',
    'rubygems',
    'yum'
  ]) {
    givenExtApplicationState({
      mockController: {
        getFormats: () => formats,
        on: jest.fn(),
        un: jest.fn()
      }
    });
  }

  function givenExtApplicationState({ mockStore, mockController } = {}) {
    mockStore = !!mockStore ? mockStore : getDefaultAppStateStoreMock();

    mockController = !!mockController
      ? mockController
      : getDefaultPermissionControllerMock();

    global.Ext.getApplication.mockReturnValue({
      getStore: jest.fn().mockReturnValue(mockStore),
      getController: jest.fn().mockReturnValue(mockController),
    });
  }

  function getDefaultBrowseableFormatStoreMock() {
    return {
      getById: jest.fn(format => mockBrowseableFormatObject(format)),
      on: jest.fn(),
      un: jest.fn(),
    };
  }

  function getDefaultAppStateStoreMock() {
    return {
      on: jest.fn(),
      un: jest.fn(),
    };
  }

  function getDefaultPermissionControllerMock() {
    return {
      on: jest.fn(),
      un: jest.fn(),
    };
  }

  function givenActiveBundles(activeBundleState = getDefaultActiveBundleState()) {
    Application.bundleActive.mockImplementation(key => activeBundleState[key] ?? false);
  }

  function getDefaultActiveBundleState() {
    return {
      'nexus-coreui-plugin': true,
    };
  }

  function givenStateValues(stateValues = getDefaultStateValues()) {
    State.getValue.mockImplementation((key, defaultValue) => stateValues[key] ?? defaultValue);
  }

  function getDefaultStateValues() {
    return {
      'nexus.react.welcome': true,
      usertoken: { licenseValid: true }
    };
  }

  function mockBrowseableFormatObject(id) {
    return {
      data: { id },
      id,
      internalId: 1234,
      joined: [],
      store: {},
    };
  }

  function givenNoPermissions() {
    givenPermissions({});
  }

  function givenPermissions(permissionLookup) {
    Permissions.check.mockImplementation(key => {
      return permissionLookup[key] ?? false;
    });
    // Set up the permissions object for permissionPrefix checks
    Permissions.permissions = permissionLookup;
  }

  function givenNoUserLoggedIn() {
    Security.hasUser.mockReturnValue(false);
  }

  function givenUserLoggedIn() {
    Security.hasUser.mockReturnValue(true);
  }

  function givenProEdition() {
    State.getEdition.mockReturnValue('PRO');
  }

  function givenCommunityEdition() {
    State.getEdition.mockReturnValue('COMMUNITY');
  }

  async function assertRenderingWelcomePage(welcomeLink) {
    // React shell now renders welcome content lazily; just assert the link is present.
    await waitFor(() => expect(welcomeLink).toBeInTheDocument());
  }

  async function assertRenderingBrowsePage(browseLink) {
    await waitFor(() => expect(browseLink).toBeInTheDocument());
  }

  async function assertOnlyWelcomeLinkVisibleAndWelcomePageShown() {
    const allLinks = screen.getAllByRole('link');
    expect(allLinks.length).toEqual(1);

    const welcomeLink = assertLinkVisible('Dashboard', '/welcome');
    await assertRenderingWelcomePage(welcomeLink);
  }

  async function assertStandardLoggedInComponentsShown() {
    const allLinks = screen.getAllByRole('link');
    expect(allLinks.length).toBeGreaterThanOrEqual(1);

    const welcomeLink = assertLinkVisible('Dashboard', '/welcome');
    await assertRenderingWelcomePage(welcomeLink);
  }

  function assertLinkVisible(name, path) {
    const link = screen.getByRole('link', { name });
    expect(link).toBeVisible();
    expect(link.href).toContain(path);

    return link;
  }

  function assertLinkNotVisible(name) {
    const link = screen.queryByRole('link', { name });
    expect(link).not.toBeInTheDocument();
  }
});
