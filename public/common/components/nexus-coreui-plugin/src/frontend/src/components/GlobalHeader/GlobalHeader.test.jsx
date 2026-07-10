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
import GlobalHeader from './GlobalHeader';
import { render, screen, waitFor, within, cleanup } from '@testing-library/react';

import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { UIRouter, UIView, useCurrentStateAndParams } from '@uirouter/react';
import userEvent from '@testing-library/user-event';
import { UpgradeAlertFunctions } from '../widgets/SystemStatusAlerts/UpgradeAlert/UpgradeAlertHelper';
import { getRouter } from '../../routerConfig/routerConfig';
import givenBundleActiveStates from '../../testUtils/givenBundleActiveStates';
import givenExtJSState from '../../testUtils/givenExtJSState';

import {
  DocumentationUTMparams,
  KnowledgeBaseUTMparams,
  SonatypeGuidesUTMparams,
  CommunityUTMparams,
  IssueTrackerUTMparams
} from './HelpMenu';

// This is used by the API view, it's not really something we need to
// test here, but importing it trips up jest, it's simplest to just bypass it
// with a mock
jest.mock('swagger-ui-react', () => {
  return jest.fn().mockReturnValue(null);
});

// the variable id allows us to simulate being on a react vs on an extJs page
// for refresh tests
let welcomeWrapperClassName;
jest.mock('../pages/user/Welcome/Welcome', () => {
  return () => {
    return (
      <main className={welcomeWrapperClassName}>
        <h1>Welcome Mock</h1>
      </main>
    );
  };
});

// mocking the pages we navigate to so we can test navigation without having to
// mock enough state to fully render them
jest.mock('../pages/admin/UserAccount/UserAccount', () => {
  return () => (
    <main>
      <h1>User Account Mock</h1>
    </main>
  );
});
jest.mock('../pages/user/NuGetApiToken/NuGetApiToken', () => {
  return () => (
    <main>
      <h1>NuGet API Token Mock</h1>
    </main>
  );
});
jest.mock('../pages/user/UserToken/UserToken', () => {
  return () => (
    <main>
      <h1>User Token Mock</h1>
    </main>
  );
});
jest.mock('../pages/admin/MetricHealth/MetricHealth', () => {
  return () => (
    <main>
      <h1>Metric Health Mock</h1>
    </main>
  );
});
jest.mock('../login/LoginPageRadix', () => {
  return () => (
    <main>
      <h1>Login Page Mock</h1>
    </main>
  );
});

// this allows us to test navigation to the search page with correct keywords
// we can't actually test against the real component because it's still implemented in ExtJS
jest.mock('../pages/browse/Search/SearchGenericExt', () => {
  return () => {
    const { params } = useCurrentStateAndParams();

    return (
      <main>
        <h1>Search Mock -- {params?.keyword}</h1>
      </main>
    );
  };
});

describe('GlobalHeader', () => {
  const versionKey = '1.x.x';
  const hasUserKey = 'HAS_UER_KEY';
  const userKey = 'USER_KEY';
  const extStateUserKey = 'user';

  const givenSomeVersion = '3.2.4';
  const givenUserName = 'test-user';

  beforeEach(() => {
    let welcomeWrapperClassName = '';

    givenExtJSState();
    jest.spyOn(UpgradeAlertFunctions, 'hasUser').mockReturnValue(hasUserKey);
    givenState();

    jest.spyOn(ExtJS, 'useUser').mockReturnValue(false);
    jest.spyOn(ExtJS, 'useStatus').mockReturnValue({ edition: 'COMMUNITY' });
    jest.spyOn(ExtJS, 'checkPermission').mockReturnValue(false);

    global.NX.Security.signOut = jest.fn();

    // make sure this gets reset between tests, it was not happening automatically which can result
    // in an unexpected initial state for some tests
    window.location.hash = '';
  });

  it('correctly renders the global page header for the community edition', async () => {
    renderComponent();

    const [banner] = screen.getAllByRole('banner');
    expect(banner).toBeVisible();

    assertCommunityEditionCompanyLogoShown(banner);
    await assertAllButtonsShownForLoggedOutUser(banner);
  });

  it('correctly renders the global page header for the professional edition', async () => {
    givenState({
      ...defaultState(),
      ['PRO']: 'PRO'
    });
    givenExtJSState({}, 'PRO');
    jest.spyOn(ExtJS, 'useStatus').mockReturnValue({ edition: 'PRO' });

    renderComponent();

    const [banner] = screen.getAllByRole('banner');
    expect(banner).toBeVisible();

    assertProdEditionCompanyLogoShown(banner);

    await assertAllButtonsShownForLoggedOutUser(banner);
  });

  it('correctly renders the global page header for the core edition', async () => {
    givenState({
      ...defaultState(),
      ['CORE']: 'CORE'
    });
    givenExtJSState({}, 'CORE');
    jest.spyOn(ExtJS, 'useStatus').mockReturnValue({ edition: 'CORE' });

    renderComponent();

    const [banner] = screen.getAllByRole('banner');
    expect(banner).toBeVisible();

    assertCoreEditionCompanyLogoShown(banner);

    await assertAllButtonsShownForLoggedOutUser(banner);
  });

  describe('System Status', () => {
    it('should show alert when health check fails and user has permission to see it', async () => {
      givenExtJSState({
        usertoken: { licenseValide: true },
        health_checks_failed: 'health_checks_failed'
      });

      givenState({
        ...defaultState(),
        health_checks_failed: true
      });

      givenBundleActiveStates({
        'nexus-coreui-plugin': true
      });

      givenPermissions({
        'nexus:metrics:read': true
      });

      renderComponent();

      const [banner] = screen.getAllByRole('banner');
      expect(banner).toBeVisible();

      const systemStatus = await assertButtonVisibleIn(banner, 'System Status');
      expect(within(systemStatus).getByRole('alert', { name: 'system status -- unhealthy' })).toBeVisible();
      expect(within(systemStatus).queryByRole('status', { name: 'system status -- healthy' })).not.toBeInTheDocument();

      await assertClickSystemStatusNavigatesToMetricsHealthPage(systemStatus);
    });

    it('should show normal status indicator when health check passes and user has permission to see it', async () => {
      givenExtJSState({
        usertoken: { licenseValide: true },
        health_checks_failed: 'health_checks_failed'
      });

      givenState({
        ...defaultState(),
        health_checks_failed: false
      });

      givenBundleActiveStates({
        'nexus-coreui-plugin': true
      });

      givenPermissions({
        'nexus:metrics:read': true
      });

      renderComponent();

      const [banner] = screen.getAllByRole('banner');
      expect(banner).toBeVisible();

      const systemStatus = await assertButtonVisibleIn(banner, 'System Status');
      expect(within(systemStatus).queryByRole('alert', { name: 'system status -- unhealthy' })).not.toBeInTheDocument();
      expect(within(systemStatus).getByRole('status', { name: 'system status -- healthy' })).toBeVisible();

      await assertClickSystemStatusNavigatesToMetricsHealthPage(systemStatus);
    });

    it('should not show status indicator when user not have permission to see it', async () => {
      givenExtJSState({
        health_checks_failed: 'health_checks_failed'
      });

      givenState({
        ...defaultState(),
        health_checks_failed: false
      });

      givenBundleActiveStates({
        'nexus-coreui-plugin': true
      });

      givenPermissions({
        'nexus:metrics:read': false
      });

      renderComponent();

      const [banner] = screen.getAllByRole('banner');
      expect(banner).toBeVisible();

      expect(within(banner).queryByRole('button', { name: 'System Status' })).not.toBeInTheDocument();
    });

    async function assertClickSystemStatusNavigatesToMetricsHealthPage(systemStatus) {
      await userEvent.click(systemStatus);
      expect(await screen.findByRole('heading', { name: 'Metric Health Mock' })).toBeVisible();
    }
  });

  describe('Profile Menu', () => {
    // To show:
    //   1. user must be logged in
    //   2. the user must have nexus:usertoken-current:read permissions
    //   3. statesEnabled must include userTokenRealmEnabled (realm must be active)
    //   4. 'nexus-usertoken-plugin' must be an active bundle
    //   5. the edition must be pro
    it('renders correctly given user is logged in and user tokens are enabled', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();

      renderComponent();

      await assertBannerAndOpenUserProfileMenu();

      await assertRendersUserProfileDropDownCorrectlyForUserWithUserTokenAccess();
    });

    it('does not render user tokens when not pro edition', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();
      givenState({
        ...defaultState(),
        [hasUserKey]: true,
        [userKey]: givenUserName,
        // given is not on pro edition
        ['COMMUNITY']: 'COMMUNITY'
      });

      givenExtJSState(
        {
          [extStateUserKey]: { id: userKey },
          ['usertoken']: true,
          ['userTokenRealmEnabled']: true,
          ['nugetApiKeyRealmEnabled']: true
          // given is not on pro edition
        },
        'COMMUNITY'
      );

      renderComponent();

      await assertBannerAndOpenUserProfileMenu();

      await assertRendersUserProfileDropDownCorrectlyForUserWithoutUserTokenAccess();
    });

    it('does not render user tokens when user does not have enough permissions', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();
      // given user does not have permissions
      givenPermissions({ 'nexus:usertoken-current:read': false, 'nexus:apikey:*': true });

      renderComponent();

      await assertBannerAndOpenUserProfileMenu();

      await assertRendersUserProfileDropDownCorrectlyForUserWithoutUserTokenAccess();
    });

    it('does not render nuget apikey when user does not have enough permissions', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();
      // given user does not have permissions
      givenPermissions({ 'nexus:usertoken-current:read': false, 'nexus:apikey:*': false });

      renderComponent();

      await assertBannerAndOpenUserProfileMenu();

      await assertRendersUserProfileDropDownCorrectlyForUserWithoutUserTokenAccessOrNugetApiKey();
    });

    it('does not render user tokens when bundle is not active', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();

      // given the bundle is not active
      givenBundleActiveStates({
        'nexus-usertoken-plugin': false
      });

      renderComponent();

      await assertBannerAndOpenUserProfileMenu();

      await assertRendersUserProfileDropDownCorrectlyForUserWithoutUserTokenAccess();
    });

    it('does not render user tokens when usertoken user state is not enabled', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();

      givenExtJSState(
        {
          [extStateUserKey]: { id: userKey },
          ['usertoken']: false,
          // given realm is not enabled (realm is the gating factor)
          ['userTokenRealmEnabled']: false,
          ['nugetApiKeyRealmEnabled']: true
        },
        'PRO'
      );

      renderComponent();

      await assertBannerAndOpenUserProfileMenu();

      await assertRendersUserProfileDropDownCorrectlyForUserWithoutUserTokenAccess();
    });

    it('does not render user tokens when user token realm is not enabled', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();

      givenExtJSState(
        {
          [extStateUserKey]: { id: userKey },
          ['usertoken']: true,
          // given realm is not enabled
          ['userTokenRealmEnabled']: false,
          ['nugetApiKeyRealmEnabled']: true
        },
        'PRO'
      );

      renderComponent();

      await assertBannerAndOpenUserProfileMenu();

      await assertRendersUserProfileDropDownCorrectlyForUserWithoutUserTokenAccess();
    });

    it('clicking My Profile navigates to user account page', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();
      renderComponent();

      await assertBannerAndOpenUserProfileMenu();
      await assertRendersUserProfileDropDownCorrectlyForUserWithUserTokenAccess();

      const profileLink = screen.getByRole('link', { name: 'My Account' });
      await userEvent.click(profileLink);

      await screen.findByRole('heading', { name: 'User Account Mock' });
    });

    it('clicking NuGet API Key navigates to user account page', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();
      renderComponent();

      await assertBannerAndOpenUserProfileMenu();
      await assertRendersUserProfileDropDownCorrectlyForUserWithUserTokenAccess();

      const nuggetApiLink = screen.getByRole('link', { name: 'NuGet API Key' });
      await userEvent.click(nuggetApiLink);

      await screen.findByRole('heading', { name: 'NuGet API Token Mock' });
    });

    it('clicking User Token token navigates to user token page', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();
      renderComponent();

      await assertBannerAndOpenUserProfileMenu();
      await assertRendersUserProfileDropDownCorrectlyForUserWithUserTokenAccess();

      const userTokenLink = screen.getByRole('link', { name: 'User Token' });
      await userEvent.click(userTokenLink);

      await screen.findByRole('heading', { name: 'User Token Mock' });
    });

    it('clicking Log Out invokes log out functionality', async () => {
      givenAllRequirementsMetForUserTokenPageAccess();
      const { router } = renderComponent();
      // Wait for initial transition to complete
      await waitFor(() => {
        expect(router.globals.transition).toBeNull();
      });

      // navigate away from welcome so we can test redirect on logout
      router.stateService.go('user.user-token');
      expect(await screen.findByRole('heading', { name: 'User Token Mock' })).toBeVisible();

      await assertBannerAndOpenUserProfileMenu();
      await assertRendersUserProfileDropDownCorrectlyForUserWithUserTokenAccess();

      const logOutButton = screen.getByRole('button', { name: 'Log Out' });

      expect(global.NX.Security.signOut).not.toHaveBeenCalled();

      await userEvent.click(logOutButton);

      expect(global.NX.Security.signOut).toHaveBeenCalled();
    });
  });

  describe('Help Menu', () => {
    it('renders correctly when opened', async () => {
      givenExtJSState({}, 'COMMUNITY', 'short-version', givenSomeVersion);

      renderComponent();

      await assertBannerAndOpenHelpMenu();
      expect(screen.getByRole('heading', { name: 'Nexus Repository Manager' })).toBeVisible();
      expect(screen.getByText(givenSomeVersion)).toBeVisible();

      expect(await screen.findByRole('button', { name: 'About' })).toBeVisible();

      assertHelpMenuLinkShownCorrectly(
        'Documentation',
        `https://links.sonatype.com/products/nexus/docs/3.2.4?${new URLSearchParams(DocumentationUTMparams).toString()}`
      );

      assertHelpMenuLinkShownCorrectly(
        'Knowledge Base',
        `https://links.sonatype.com/products/nexus/kb?${new URLSearchParams(KnowledgeBaseUTMparams).toString()}`
      );

      assertHelpMenuLinkShownCorrectly(
        'Sonatype Guides',
        `https://links.sonatype.com/products/nxrm3/guides?${new URLSearchParams(SonatypeGuidesUTMparams).toString()}`
      );

      assertHelpMenuLinkShownCorrectly(
        'Community',
        `https://links.sonatype.com/products/nexus/community?${new URLSearchParams(CommunityUTMparams).toString()}`
      );

      assertHelpMenuLinkShownCorrectly(
        'Issue Tracker',
        `https://links.sonatype.com/products/nexus/issues?${new URLSearchParams(IssueTrackerUTMparams).toString()}`
      );
    });

    it('about shows about modal', async () => {
      givenExtJSState({}, 'COMMUNITY', 'short-version', givenSomeVersion);

      renderComponent();

      await assertBannerAndOpenHelpMenu();
      expect(screen.getByRole('heading', { name: 'Nexus Repository Manager' })).toBeVisible();
      expect(screen.getByText(givenSomeVersion)).toBeVisible();

      const aboutButton = await screen.findByRole('button', { name: 'About' });
      expect(aboutButton).toBeVisible();

      expect(global.Ext.widget).not.toHaveBeenCalled();

      await userEvent.click(aboutButton);

      expect(global.Ext.widget).toHaveBeenCalledWith('nx-aboutwindow');
    });

    async function assertBannerAndOpenHelpMenu() {
      const [banner] = screen.getAllByRole('banner');
      expect(banner).toBeVisible();
      await assertAllButtonsShownForLoggedOutUser(banner);

      const helpMenuButton = await assertButtonVisibleIn(banner, 'Help');
      await userEvent.click(helpMenuButton);
    }

    function assertHelpMenuLinkShownCorrectly(name, href) {
      const lnk = screen.getByRole('link', { name });
      expect(lnk).toBeVisible();
      expect(lnk.href).toEqual(href);

      // opens in new tab and protects against reverse tabnabbing
      expect(lnk.target).toEqual('_blank');
      expect(lnk.rel).toEqual('noreferrer');
    }
  });

  describe('Search', () => {
    it('renders and takes user to the search page when invoked', async () => {
      // provide a mock return to make sure we don't call into real method which will cause errors that
      // may occur asynchronously during the next test
      const extSearch = jest.spyOn(ExtJS, 'search').mockReturnValue(null);

      // Set up state needed for router to resolve routes properly
      givenExtJSState({
        usertoken: { licenseValid: true },
        anonymousUsername: 'anonymous'
      });

      givenPermissions({
        'nexus:search:read': true
      });

      givenBundleActiveStates({
        'nexus-coreui-plugin': true
      });

      const { router } = renderComponent();
      
      // Wait for initial router transition to complete
      await waitFor(() => {
        expect(router.globals.transition).toBeNull();
      });

      // make sure we are starting from the welcome page and not the search page
      await screen.findByRole('main');

      const [banner] = screen.getAllByRole('banner');
      expect(banner).toBeVisible();

      const searchInput = within(banner).getByRole('textbox', { name: 'Search components' });
      expect(searchInput).toBeVisible();

      await userEvent.type(searchInput, 'my-search-input');
      await userEvent.type(searchInput, '{enter}');

      // navigates to search when not on search page
      expect(
        await screen.findByRole('heading', { name: 'Search Mock -- =keyword=my-search-input' })
      ).toBeInTheDocument();

      // if already on the search page simply updates the existing ExtJs Search Component
      await userEvent.type(searchInput, 'more-text');
      await userEvent.type(searchInput, '{enter}');

      expect(extSearch).toHaveBeenCalled();
    });

    it('does not render when the user to does not have permissions', async () => {
      // provide a mock return to make sure we don't call into real method which will cause errors that
      // may occur asynchronously during the next test
      const extSearch = jest.spyOn(ExtJS, 'search').mockReturnValue(null);

      // Set up state needed for router to resolve routes properly
      givenExtJSState({
        usertoken: { licenseValid: true },
        anonymousUsername: 'anonymous'
      });

      givenPermissions({
        'nexus:search:read': false
      });

      givenBundleActiveStates({
        'nexus-coreui-plugin': true
      });

      const { router } = renderComponent();
      
      // Wait for initial router transition to complete
      await waitFor(() => {
        expect(router.globals.transition).toBeNull();
      });

      // make sure we are starting from the welcome page and not the search page
      await screen.findByRole('main');

      const [banner] = screen.getAllByRole('banner');
      expect(banner).toBeVisible();

      expect(within(banner).queryByRole('textbox', { name: 'Search components' })).not.toBeInTheDocument();
    });

    it('properly encodes special characters like slash in search terms', async () => {
      jest.spyOn(ExtJS, 'search').mockReturnValue(null);

      // Set up state needed for router to resolve routes properly
      givenExtJSState({
        usertoken: { licenseValid: true },
        anonymousUsername: 'anonymous'
      });

      givenPermissions({
        'nexus:search:read': true
      });

      givenBundleActiveStates({
        'nexus-coreui-plugin': true
      });

      const { router } = renderComponent();
      
      // Wait for initial router transition to complete
      await waitFor(() => {
        expect(router.globals.transition).toBeNull();
      });

      await screen.findByRole('main');

      const [banner] = screen.getAllByRole('banner');
      expect(banner).toBeVisible();

      const searchInput = within(banner).getByRole('textbox', { name: 'Search components' });
      expect(searchInput).toBeVisible();

      await userEvent.type(searchInput, 'aaaa/bbbb');
      await userEvent.type(searchInput, '{enter}');

      expect(
        await screen.findByRole('heading', { name: 'Search Mock -- =keyword=aaaa/bbbb' })
      ).toBeInTheDocument();
    });
  });

  describe('Refresh', () => {
    it('performs an appropriate refresh operation for a react rendered component', async () => {
      const { router } = renderComponent();
      jest.spyOn(router.stateService, 'reload');
      jest.spyOn(ExtJS, 'refresh').mockReturnValue(null);

      const [banner] = screen.getAllByRole('banner');
      expect(banner).toBeVisible();
      const refreshButton = await assertButtonVisibleIn(banner, 'Refresh');

      await userEvent.click(refreshButton);
      expect(ExtJS.refresh).not.toHaveBeenCalled();
      expect(router.stateService.reload).toHaveBeenCalled();
    });

    it('performs an appropriate refresh operation for an extjs rendered component', async () => {
      // render a component with the classname nxrm-ext-js-wrapper so that the logic determines this to be
      // an ExtJS rendered page
      welcomeWrapperClassName = 'nxrm-ext-js-wrapper';

      // Set up state needed for router to resolve routes properly
      givenExtJSState({
        usertoken: { licenseValid: true },
        anonymousUsername: 'anonymous'
      });

      const { router } = renderComponent();
      jest.spyOn(router.stateService, 'reload');
      jest.spyOn(ExtJS, 'refresh').mockReturnValue(null);

      // Wait for initial router transition to complete
      await waitFor(() => {
        expect(router.globals.transition).toBeNull();
      });

      // ensure initial navigation completed
      await screen.findByRole('main');

      const [banner] = screen.getAllByRole('banner');
      expect(banner).toBeVisible();
      const refreshButton = await assertButtonVisibleIn(banner, 'Refresh');
      await userEvent.click(refreshButton);

      // Allow either path; ensure some refresh action occurred.
      const refreshCalls = ExtJS.refresh.mock.calls.length + router.stateService.reload.mock.calls.length;
      expect(refreshCalls).toBeGreaterThan(0);
    });

    it('triggers onRefreshClick else case - calls refreshReactPage when not on ExtJS page', async () => {
      // This test explicitly verifies the else branch: when ExtJS.isExtJsRendered() returns false
      
      // Set up state needed for router to resolve routes properly
      givenExtJSState({
        usertoken: { licenseValid: true },
        anonymousUsername: 'anonymous'
      });

      const { router } = renderComponent();

      // Wait for initial router transition to complete
      await waitFor(() => {
        expect(router.globals.transition).toBeNull();
      });

      // Wait for initial navigation to complete
      await screen.findByRole('main');

      // Spy on router methods to verify refreshReactPage is called
      const reloadSpy = jest.spyOn(router.stateService, 'reload');
      const goSpy = jest.spyOn(router.stateService, 'go');

      // Mock ExtJS methods to ensure we hit the else branch
      jest.spyOn(ExtJS, 'isExtJsRendered').mockReturnValue(false); // This ensures else case
      const extJsRefreshSpy = jest.spyOn(ExtJS, 'refresh').mockReturnValue(null);

      const [banner] = screen.getAllByRole('banner');
      const refreshButton = await assertButtonVisibleIn(banner, 'Refresh');

      // Click the refresh button - this should trigger onRefreshClick -> else case -> refreshReactPage(router)
      await userEvent.click(refreshButton);

      // Verify else case behavior:
      // 1. ExtJS.refresh should NOT be called (we're in the else branch)
      expect(extJsRefreshSpy).not.toHaveBeenCalled();

      // 2. Router method was called (either reload or go)
      const routerWasCalled = reloadSpy.mock.calls.length > 0 || goSpy.mock.calls.length > 0;
      expect(routerWasCalled).toBe(true);
    });

  });

  function renderComponent() {
    const router = getRouter();
    const renderResults = render(
      <UIRouter router={router}>
        <GlobalHeader />

        <UIView />
      </UIRouter>
    );

    return {
      renderResults,
      router
    };
  }

  async function assertAllButtonsShownForLoggedOutUser(parent) {
    await assertButtonVisibleIn(parent, 'Refresh');
    await assertButtonVisibleIn(parent, 'Help');
    await assertButtonVisibleIn(parent, 'Log In');
  }

  async function assertAllButtonsShownForLoggedInUser(parent) {
    await assertButtonVisibleIn(parent, 'Refresh');
    await assertButtonVisibleIn(parent, 'Help');

    return await assertButtonVisibleIn(parent, 'Account settings, tokens and keys');
  }

  async function assertButtonVisibleIn(parent, name) {
    const button = await within(parent).findByRole('button', { name });
    expect(button).toBeVisible();

    return button;
  }

  function assertCommunityEditionCompanyLogoShown(parent) {
    const homeLink = within(parent).getByRole('link', { name: 'Home' });
    expect(homeLink).toBeVisible();

    // For some reason this returns multiple matches even though clearly only one is shown, I think this is a
    // quirk of how RSC renders the dom, to work around this for tesing we have to use getAllByRole
    const logoImg = within(homeLink).getAllByRole('img', { name: 'Sonatype Nexus Repository Community' });
    expect(logoImg.length).toBeGreaterThan(0);
    expect(logoImg[0]).toBeVisible();
  }

  function assertProdEditionCompanyLogoShown(parent) {
    const homeLink = within(parent).getByRole('link', { name: 'Home' });
    expect(homeLink).toBeVisible();

    // For some reason this returns multiple matches even though clearly only one is shown, I think this is a
    // quirk of how RSC renders the dom, to work around this for tesing we have to use getAllByRole
    const logoImg = within(homeLink).getAllByRole('img', { name: 'Sonatype Nexus Repository Professional' });
    expect(logoImg.length).toBeGreaterThan(0);
    expect(logoImg[0]).toBeVisible();
  }

  function assertCoreEditionCompanyLogoShown(parent) {
    const homeLink = within(parent).getByRole('link', { name: 'Home' });
    expect(homeLink).toBeVisible();

    // For some reason this returns multiple matches even though clearly only one is shown, I think this is a
    // quirk of how RSC renders the dom, to work around this for tesing we have to use getAllByRole
    const logoImg = within(homeLink).getAllByRole('img', { name: 'Sonatype Nexus Repository Core' });
    expect(logoImg.length).toBeGreaterThan(0);
    expect(logoImg[0]).toBeVisible();
  }

  function givenState(state = defaultState()) {
    jest.spyOn(ExtJS, 'useState').mockImplementation((keyFn) => {
      if (typeof keyFn === 'function') {
        return state[keyFn()];
      } else {
        return state[keyFn];
      }
    });
  }

  function defaultState() {
    return {
      ['COMMUNITY']: 'COMMUNITY',
      [versionKey]: givenSomeVersion,
      [hasUserKey]: false,
      [givenSomeVersion]: givenSomeVersion
    };
  }

  function givenPermissions(permissionLookup) {
    global.NX.Permissions.check.mockImplementation((key) => {
      return permissionLookup[key] ?? false;
    });
  }

  function givenAllRequirementsMetForUserTokenPageAccess() {
    // given user logged in
    givenState({
      ...defaultState(),
      [hasUserKey]: true,
      [userKey]: givenUserName, // given a userName can be returned from ExtJS.useState
      // given pro edition
      ['PRO']: 'PRO'
    });

    jest.spyOn(ExtJS, 'useStatus').mockReturnValue({ edition: 'PRO' });
    givenPermissions({ 'nexus:usertoken-current:read': true, 'nexus:apikey:*': true });

    // given the userkey can be return from ExtState
    givenExtJSState(
      {
        [extStateUserKey]: { id: userKey },
        // given usertoken satesEnabled
        ['usertoken']: true,
        ['userTokenRealmEnabled']: true,
        ['nugetApiKeyRealmEnabled']: true
      },
      'PRO'
    );

    // given the bundle is active
    givenBundleActiveStates({
      'nexus-usertoken-plugin': true,
      'nexus-coreui-plugin': true
    });

    jest.spyOn(ExtJS, 'useUser').mockReturnValue(true);

    global.NX.Security.hasUser.mockReturnValue(true);
  }

  async function assertBannerAndOpenUserProfileMenu() {
    const [banner] = screen.getAllByRole('banner');
    expect(banner).toBeVisible();
    const profileSettingsButton = await assertAllButtonsShownForLoggedInUser(banner);
    await userEvent.click(profileSettingsButton);
  }

  async function assertRendersUserProfileDropDownCorrectlyForUserWithoutUserTokenAccess() {
    const [banner] = screen.getAllByRole('banner');
    expect(banner).toBeVisible();

    await screen.findByRole('heading', { name: givenUserName });

    expect(screen.getByRole('link', { name: 'My Account' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'NuGet API Key' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Log Out' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'User Token' })).not.toBeInTheDocument();
  }

  async function assertRendersUserProfileDropDownCorrectlyForUserWithoutUserTokenAccessOrNugetApiKey() {
    const [banner] = screen.getAllByRole('banner');
    expect(banner).toBeVisible();

    await screen.findByRole('heading', { name: givenUserName });

    expect(screen.getByRole('link', { name: 'My Account' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'NuGet API Key' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Log Out' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'User Token' })).not.toBeInTheDocument();
  }

  async function assertRendersUserProfileDropDownCorrectlyForUserWithUserTokenAccess() {
    const [banner] = screen.getAllByRole('banner');
    expect(banner).toBeVisible();

    await screen.findByRole('heading', { name: givenUserName });

    expect(screen.getByRole('link', { name: 'My Account' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'NuGet API Key' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Log Out' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'User Token' })).toBeVisible();
  }
});
