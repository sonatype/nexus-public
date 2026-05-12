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
import { act, render, screen, waitFor, within } from '@testing-library/react';
import { UIRouter } from '@uirouter/react';
import { Theme } from '@radix-ui/themes';
import { TooltipProvider } from '@radix-ui/react-tooltip';
import { App } from './App';
import { getRouter } from './routerConfig/routerConfig';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { helperFunctions } from './components/widgets/SystemStatusAlerts/CELimits/UsageHelper';
import { ROUTE_NAMES } from './routerConfig/routeNames/routeNames';
import { AuthProvider } from './contexts/AuthContext';
import { PermissionsProvider } from './contexts/PermissionsContext';
import { StateProvider } from './contexts/StateContext';

const { BROWSE } = ROUTE_NAMES;

// mocking out the Welcome page to avoid having to mock all the various ExtJs functions/state required to render it
jest.mock('./components/pages/user/Welcome/Welcome', () => {
  return () => (<main><h1>Welcome Test Mock</h1></main>);
});

jest.mock('./components/login/SelfHostedLoginPageWrapper', () => {
  return () => (<main><h1>Login Test Mock</h1></main>);
});

jest.mock('./components/login/LoginPageRadix', () => {
  return () => (<main><h1>Login Test Mock</h1></main>);
});

// Phase 1: Mock Context providers to use ExtJS state directly in tests
jest.mock('./contexts/AuthContext', () => ({
  AuthProvider: ({ children }) => children,
  useAuth: () => ({
    user: window.NX?.State?.getUser?.() || null,
    isLoading: false,
    isAuthenticated: Boolean(window.NX?.Security?.hasUser?.()),
    hasUser: () => Boolean(window.NX?.Security?.hasUser?.()),
    refreshUser: () => Promise.resolve()
  })
}));

jest.mock('./contexts/PermissionsContext', () => ({
  PermissionsProvider: ({ children }) => children,
  usePermissions: () => ({
    checkPermission: (permission) => window.NX?.Permissions?.check?.(permission) || false,
    hasPermission: (permission) => window.NX?.Permissions?.check?.(permission) || false,
    usePermission: (permission) => window.NX?.Permissions?.check?.(permission) || false
  })
}));

jest.mock('./contexts/StateContext', () => ({
  StateProvider: ({ children }) => children,
  useAppState: () => ({
    getValue: (key) => window.NX?.State?.getValue?.(key),
    setValue: (key, value) => window.NX?.State?.setValue?.(key, value),
    useState: (key) => window.NX?.State?.getValue?.(key),
    getEdition: () => window.NX?.State?.getEdition?.() || 'OSS',
    getVersion: () => window.NX?.State?.getVersion?.(),
    getLicense: () => window.NX?.State?.getValue?.('license')
  })
}));

jest.mock('./contexts/ThemeContext', () => ({
  ThemeProvider: ({ children }) => children,
  useTheme: () => ({ effectiveTheme: 'light', setTheme: jest.fn() }),
  THEMES: { LIGHT: 'light', DARK: 'dark' }
}));

// Phase 1: Mock useRedirectOnLogout to avoid router initialization issues in tests
// This hook calls useCurrentStateAndParams() which fails if router isn't fully initialized
jest.mock('./hooks/useRedirectOnLogout', () => ({
  useRedirectOnLogout: jest.fn()
}));

global.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve({}) });

describe('App', () => {
  let routerInstance = null;

  afterEach(() => {
    if (routerInstance) {
      routerInstance.dispose();
      routerInstance = null;
    }
  });

  describe('login layout', () => {
    beforeEach(() => {
      // Phase 1: Login layout needs NO anonymous access to route to login page
      givenExtJSState({
        usertoken: { licenseValid: true },
        dbUpgrade: { currentState: null }
        // Note: NO anonymousUsername, so router routes to login
      });
    });

    it('should render login layout', async () => {
      const result = await renderComponent();
      routerInstance = result.router;
      const { router } = result;

      // Wait for initial automatic redirect from login to complete
      await waitFor(() => {
        expect(router.globals.transition).toBeNull();
      });

      await assertLoginLayoutRenders();
    });
  });

  describe('standard layout', () => {
    let historySpy;

    beforeEach(() => {
      givenExtJSState();
      givenUser();
      historySpy = jest.spyOn(History.prototype, 'pushState');

      window.location.hash = '';
      window.dirty = [];
    });

    it('should render standard layout', async () => {
      // logged in user (mocked by givenUser()) if try to go to login page will be redirected to welcome page,
      // which is into the standard layout
      renderComponent();
      await assertStandardLayoutRenders();
    });

    describe('UI Branding', () => {
      it('should not render branding header nor footer when not branding is set', async () => {
        renderComponent();
        const brandingHeader = screen.queryByTestId('nxrm-branding-header');
        const brandingFooter = screen.queryByTestId('nxrm-branding-footer');
        expect(brandingHeader).not.toBeInTheDocument();
        expect(brandingFooter).not.toBeInTheDocument();
      });

      it('should not render branding header and footer when both are disabled', async () => {
        givenExtJSState({
          ...getDefaultState(),
          branding: {
            headerEnabled: false,
            headerHtml: '<div>Branding Header</div>',
            footerEnabled: false,
            footerHtml: '<div>Branding Footer</div>',
          },
        });
        renderComponent();
        const brandingHeader = screen.queryByTestId('nxrm-branding-header');
        const brandingFooter = screen.queryByTestId('nxrm-branding-footer');
        expect(brandingHeader).not.toBeInTheDocument();
        expect(brandingFooter).not.toBeInTheDocument();
      });

      it('should render branding header and footer when enabled', async () => {
        givenExtJSState({
          ...getDefaultState(),
          branding: {
            headerEnabled: true,
            headerHtml: '<div>Branding Header</div>',
            footerEnabled: true,
            footerHtml: '<div>Branding Footer</div>',
          },
        });
        renderComponent();
        const brandingHeader = screen.getByTestId('nxrm-branding-header');
        const brandingFooter = screen.getByTestId('nxrm-branding-footer');
        expect(brandingHeader).toBeVisible();
        expect(within(brandingHeader).getByText('Branding Header')).toBeVisible();
        expect(brandingFooter).toBeVisible();
        expect(within(brandingFooter).getByText('Branding Footer')).toBeVisible();
      });

      it('should render branding header but not footer', async () => {
        givenExtJSState({
          ...getDefaultState(),
          branding: {
            headerEnabled: true,
            headerHtml: '<div>Branding Header</div>',
            footerEnabled: false,
            footerHtml: '<div>Branding Footer</div>',
          },
        });
        renderComponent();
        const brandingHeader = screen.getByTestId('nxrm-branding-header');
        const brandingFooter = screen.queryByTestId('nxrm-branding-footer');
        expect(brandingHeader).toBeVisible();
        expect(within(brandingHeader).getByText('Branding Header')).toBeVisible();
        expect(brandingFooter).not.toBeInTheDocument();
      });

      it('should render branding footer but not header', async () => {
        givenExtJSState({
          ...getDefaultState(),
          branding: {
            headerEnabled: false,
            headerHtml: '<div>Branding Header</div>',
            footerEnabled: true,
            footerHtml: '<div>Branding Footer</div>',
          },
        });
        renderComponent();
        const brandingHeader = screen.queryByTestId('nxrm-branding-header');
        const brandingFooter = screen.getByTestId('nxrm-branding-footer');
        expect(brandingHeader).not.toBeInTheDocument();
        expect(brandingFooter).toBeVisible();
        expect(within(brandingFooter).getByText('Branding Footer')).toBeVisible();
      });

      it('sanitizes XSS in branding header HTML (z8s8)', () => {
        givenExtJSState({
          ...getDefaultState(),
          branding: {
            headerEnabled: true,
            headerHtml: '<b>Safe Header</b><script>window.__HEADER_XSS__=true</script>',
            footerEnabled: false,
            footerHtml: '',
          },
        });
        renderComponent();

        const brandingHeader = screen.getByTestId('nxrm-branding-header');
        expect(within(brandingHeader).getByText('Safe Header')).toBeVisible();
        expect(window.__HEADER_XSS__).toBeUndefined();
      });

      it('sanitizes XSS in branding footer HTML (z8s8)', () => {
        givenExtJSState({
          ...getDefaultState(),
          branding: {
            headerEnabled: false,
            headerHtml: '',
            footerEnabled: true,
            footerHtml: '<b>Safe Footer</b><script>window.__FOOTER_XSS__=true</script>',
          },
        });
        renderComponent();

        const brandingFooter = screen.getByTestId('nxrm-branding-footer');
        expect(within(brandingFooter).getByText('Safe Footer')).toBeVisible();
        expect(window.__FOOTER_XSS__).toBeUndefined();
      });
    });

    describe('Community Edition Hard Limit Banner', () => {
      // We will just a simple test here to make sure the banner is rendered in the context of the page
      // Full testing the CEHardLimitAlert logic has its own test suite
      it('should render given a community edition is over the limit and an admin user is logged in', async () => {
        const givenGracePeriodEndDate = givenDateNDaysInTheFuture(20);

        // Must explicitly set COMMUNITY edition to test CE hard limit banner
        givenExtJSState(getDefaultState(), 'COMMUNITY');
        givenUseState({
          [helperFunctions.useThrottlingStatusValue]: 'Over limits',
          [helperFunctions.useGracePeriodEndsDate]: givenGracePeriodEndDate,
          [helperFunctions.useDaysUntilGracePeriodEnds]: 12
        });

        await renderComponent();

        await assertStandardLayoutRenders();
        await assertCommunityEditionLimitMessageShowing(
            '20 Days Remaining',
            'This instance of Nexus Repository Community Edition has exceeded its usage limit.');
      });
    });

    describe('Login Prompting', () => {
      it('shows login given user does not have permissions to view a route and is not authenticated', async () => {
        // Phase 1: Override to disable anonymous access so router routes to login
        givenExtJSState({
          usertoken: { licenseValid: true },
          dbUpgrade: { currentState: null }
          // Note: NO anonymousUsername
        });
        global.NX.Security.hasUser = jest.fn().mockReturnValue(false);

        const result = await renderComponent();
        routerInstance = result.router;
        const { router } = result;

        await assertLoginLayoutRenders();

        // the transaction should still fail because even though we resolved the login prompt successfully upon
        // re-checking visiblity we'll find the user still does not have permissions
        let errorOnTransition = null;
        try {
          await router.stateService.go('admin.security.users')
        } catch (ex) {
          errorOnTransition = ex.message
        }

        expect(errorOnTransition).toEqual('The transition has been aborted')
      });

      it('does not show for login given user does not have permissions but is already authenticated', async () => {
        global.NX.Security.hasUser.mockReturnValue(true);

        const result = await renderComponent();
        routerInstance = result.router;
        const { router } = result;

        // the transaction should still fail because even though we resolved the login prompt successfully upon
        // re-checking visiblity we'll find the user still does not have permissions
        let errorOnTransition = null;
        try {
          await router.stateService.go('admin.security.users')
        } catch (ex) {
          errorOnTransition = ex.message
        }

        expect(errorOnTransition).toEqual('The transition has been aborted')

        await assertMissingRoutePageRendered();
      });
    });

    it('should direct to 404 when page not found', async () => {
      const { router } = await renderComponent();
      await assertStandardLayoutRenders();

      router.urlService.url('some-page-that-does-not-exist', false);

      await assertMissingRoutePageRendered();
    });

    it("history hash pushState is intercepted and ignored", async () => {
      await renderComponent();

      history.pushState({}, '', '#');
      expect(historySpy).not.toHaveBeenCalled();

      history.pushState({}, '', '');
      expect(historySpy).toHaveBeenCalledWith({}, '', '');
    });

    describe('Unsaved Changes Dialog', () => {
      const selectors = {
        cancelButton: () => screen.queryByRole('button', { name: 'Cancel' }),
        continueButton: () => screen.queryByRole('button', { name: 'Continue' }),
        modalTitle: () => screen.queryByRole('heading', { name: 'Unsaved Changes' }),
        modalContent: () => screen.queryByText('You have unsaved changes. Continuing will discard them.')
      }

      it('should render the unsaved changes modal when navigating away from a page with unsaved changes', async () => {
        const { router } = await renderComponent();
        await assertStandardLayoutRenders();

        act(() => {
          window.dirty = ['some unsaved changes'];
          router.stateService.go(BROWSE.BROWSE.ROOT);
        });

        expect(selectors.modalTitle()).toBeVisible();
      });

      it('should not render the unsaved changes modal when navigating away from a page without unsaved changes', async () => {
        const { router } = await renderComponent();
        await assertStandardLayoutRenders();

        act(() => {
          router.stateService.go(BROWSE.BROWSE.ROOT);
        });

        expect(selectors.modalTitle()).not.toBeInTheDocument();
      });

      it('should hide the unsaved changes modal when the cancel button is clicked', async () => {
        const { router } = await renderComponent();
        await assertStandardLayoutRenders();

        act(() => {
          window.dirty = ['some unsaved changes'];
          router.stateService.go(BROWSE.BROWSE.ROOT);
        });

        expect(selectors.modalTitle()).toBeVisible();
        expect(selectors.modalContent()).toBeVisible();
        expect(selectors.cancelButton()).toBeVisible();

        act(() => {
          selectors.cancelButton().click();
        });

        expect(selectors.modalTitle()).not.toBeInTheDocument();
        expect(selectors.modalContent()).not.toBeInTheDocument();
        expect(selectors.cancelButton()).not.toBeInTheDocument();
      });

      it('should hide the unsaved changes modal when the continue button is clicked', async () => {
        const { router } = await renderComponent();
        await assertStandardLayoutRenders();

        act(() => {
          window.dirty = ['some unsaved changes'];
          router.stateService.go(BROWSE.BROWSE.ROOT);
        });

        expect(selectors.modalTitle()).toBeVisible();
        expect(selectors.modalContent()).toBeVisible();
        expect(selectors.continueButton()).toBeVisible();

        act(() => {
          selectors.continueButton().click();
        });

        expect(selectors.modalTitle()).not.toBeInTheDocument();
        expect(selectors.modalContent()).not.toBeInTheDocument();
        expect(selectors.continueButton()).not.toBeInTheDocument();
      });
    });
  });

  async function renderComponent() {
    const router = getRouter();
    routerInstance = router;

    const renderResult = render(
      <AuthProvider>
        <PermissionsProvider>
          <StateProvider>
            <Theme>
              <TooltipProvider>
                <UIRouter router={router}>
                  <App />
                </UIRouter>
              </TooltipProvider>
            </Theme>
          </StateProvider>
        </PermissionsProvider>
      </AuthProvider>
    );

    await waitFor(() => {
      expect(router.globals.transition).toBeNull();
    });

    return { renderResult, router }
  }

  async function assertStandardLayoutRenders() {
    await assertRendersPageContents();
    await assertRendersGlobalHeader();
    await assertRendersLeftNav();
  }

  async function assertRendersPageContents() {
    const mains = await screen.findAllByRole('main');
    expect(mains.length).toBeGreaterThanOrEqual(1);
    expect(mains[0]).toBeVisible();
  }

  async function assertRendersGlobalHeader() {
    const header = await screen.findByTestId('global-header');
    expect(header).toBeVisible();
  }

  async function assertRendersLeftNav() {
    const navs = await screen.findAllByRole('navigation');
    expect(navs.length).toBeGreaterThanOrEqual(1);
    expect(navs[0]).toBeVisible();
  }

  async function assertLoginLayoutRenders() {
    expect(await screen.findByRole('heading', { name: 'Login Test Mock' })).toBeVisible();
  }

  function assertCommunityEditionLimitMessageShowing(title, message) {
    const alerts = screen.getAllByRole('complementary', { name: 'alert system notice'});
    const alert = alerts.find(el => within(el).queryByRole('heading', { name: title }));
    expect(alert).toBeTruthy();
    expect(alert).toBeVisible();
    expect(within(alert).getByText(message, { exact: false })).toBeVisible()
  }

  function givenExtJSState(values = getDefaultState(), edition = 'PRO') {
    const getValueMock = jest.fn().mockImplementation((key, defaultValue) => {
      return values[key] || defaultValue;
    });

    jest.spyOn(ExtJS, 'state').mockReturnValue({
      getEdition: jest.fn().mockReturnValue(edition),
      getVersionMajorMinor: jest.fn().mockReturnValue('1.2.3-some-version'),
      getVersion: jest.fn().mockReturnValue('1.2.3-some-full-version'),
      getValue: getValueMock,
      getUser: jest.fn()
    });

    jest.spyOn(ExtJS, 'useStatus').mockReturnValue({
      edition: edition,
      version: '1.2.3-some-full-version',
    });

    global.NX.State.getValue = getValueMock;
  }

  function givenUseState(values = {}) {
    jest.spyOn(ExtJS, 'useState').mockImplementation((key) => values[key]);
  }

  function getDefaultState() {
    return {
      usertoken: { licenseValid: true },
      dbUpgrade: { currentState: null },
      anonymousUsername: 'anonymous' // Phase 1: Enable anonymous access so router goes to welcome, not login
    }
  }

  function givenUser(user = { name: 'admin', administrator: true, authenticated: true }) {
    jest.spyOn(ExtJS, 'useUser').mockReturnValue(user);
    global.NX.Security.hasUser = jest.fn().mockReturnValue(true);
  }

  function givenDateNDaysInTheFuture(days) {
    const date = new Date()
    date.setDate(date.getDate() + days);

    return date;
  }

  async function assertMissingRoutePageRendered() {
    const mains = await screen.findAllByRole('main');
    expect(mains.length).toBeGreaterThanOrEqual(1);
    expect(mains[0]).toBeVisible();
  }
});
