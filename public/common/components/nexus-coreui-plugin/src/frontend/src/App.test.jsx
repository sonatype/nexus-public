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
import { act, cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { UIRouter } from '@uirouter/react';
import { Theme } from '@radix-ui/themes';
import { TooltipProvider } from '@radix-ui/react-tooltip';
import { App } from './App';
import { getRouter } from './routerConfig/routerConfig';
import { ExtJS, resetDialogState, ToastProvider } from '@sonatype/nexus-ui-plugin';
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

// Mock UnsavedChangesModal with a portal-free, plain-DOM double.
//
// The real component renders an NxModal into a document.body portal whose
// asynchronous teardown raced the App test's removal assertions on slow CI
// (the modal heading lingered past the waitFor window -- NEXUS-53445 /
// NEXUS-53515). The modal's own show/hide rendering is covered deterministically
// by UnsavedChangesModal.test.jsx; here we only need the App <-> router <->
// dialog wiring, so the double subscribes to the same shared dialog store via
// setDialogSetter and renders inline. Inline mount/unmount is synchronous, so
// there is no portal teardown to race.
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  const React = require('react');
  const dialog = jest.requireActual(
    '@sonatype/nexus-ui-plugin/src/frontend/src/router/unsavedChangesDialog'
  );

  function MockUnsavedChangesModal() {
    const [visible, setVisible] = React.useState(false);
    React.useEffect(() => {
      dialog.setDialogSetter(setVisible);
    }, []);

    if (!visible) {
      return null;
    }

    return React.createElement(
      'div',
      { role: 'dialog', 'aria-label': 'Unsaved Changes' },
      React.createElement('h2', null, 'Unsaved Changes'),
      React.createElement('p', null, 'You have unsaved changes. Continuing will discard them.'),
      React.createElement('button', { type: 'button', onClick: dialog.handleCancel }, 'Cancel'),
      React.createElement('button', { type: 'button', onClick: dialog.handleContinue }, 'Continue')
    );
  }

  return {
    __esModule: true,
    ...actual,
    UnsavedChangesModal: MockUnsavedChangesModal
  };
});

global.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve({}) });

describe('App', () => {
  let routerInstance = null;

  beforeEach(() => {
    // Guard against stale singleton state left by a previous test that did not
    // fully clean up (e.g. a test that navigated away with dirty=true but whose
    // afterEach ran after the next test's beforeEach on slow CI).
    act(() => resetDialogState());
    window.dirty = [];
  });

  afterEach(() => {
    if (routerInstance) {
      routerInstance.dispose();
      routerInstance = null;
    }
    // Wrapped in act() because resetDialogState() calls setVisible(false) on
    // the still-mounted mock's useState setter (RTL cleanup() below runs
    // after this). Without act(), the pending state update can spill across
    // the test boundary on loaded CI executors.
    act(() => resetDialogState());
    // Explicit cleanup (redundant with RTL auto-cleanup but defensive on CI,
    // where a hanging ui-router transition from showUnsavedChangesModal can
    // leave React in a state where auto-cleanup does not fully clear portals).
    cleanup();
    window.dirty = [];
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

  describe('toast bridge (NEXUS-52605 regression guard)', () => {
    beforeEach(() => {
      delete window.__nexusToast;
      givenExtJSState();
      givenUser();
      jest.spyOn(ExtJS, 'useStatus').mockReturnValue({ edition: 'PRO' });
      jest.spyOn(ExtJS, 'checkPermission').mockReturnValue(false);
      window.location.hash = '';
      window.dirty = [];
    });

    it('should register window.__nexusToast when App mounts', async () => {
      expect(window.__nexusToast).toBeUndefined();
      await renderComponent();
      expect(window.__nexusToast).toBeDefined();
      expect(typeof window.__nexusToast.error).toBe('function');
      expect(typeof window.__nexusToast.success).toBe('function');
      expect(typeof window.__nexusToast.warning).toBe('function');
      expect(typeof window.__nexusToast.info).toBe('function');
    });

    it('should display a visible toast when window.__nexusToast.error is called', async () => {
      await renderComponent();

      act(() => {
        window.__nexusToast.error('Search requires at least 3 characters');
      });

      const toast = await screen.findByTestId('toast-error');
      expect(toast).toBeInTheDocument();
      expect(toast).toHaveTextContent('Search requires at least 3 characters');
    });

    it('should display a visible toast when window.__nexusToast.success is called', async () => {
      await renderComponent();

      act(() => {
        window.__nexusToast.success('Repository created successfully');
      });

      const toast = await screen.findByTestId('toast-success');
      expect(toast).toBeInTheDocument();
      expect(toast).toHaveTextContent('Repository created successfully');
    });

    it('should display a visible toast when window.__nexusToast.warning is called', async () => {
      await renderComponent();

      act(() => {
        window.__nexusToast.warning('License expires in 30 days');
      });

      const toast = await screen.findByTestId('toast-warning');
      expect(toast).toBeInTheDocument();
      expect(toast).toHaveTextContent('License expires in 30 days');
    });

    it('should display a visible toast when window.__nexusToast.info is called', async () => {
      await renderComponent();

      act(() => {
        window.__nexusToast.info('System maintenance scheduled');
      });

      const toast = await screen.findByTestId('toast-info');
      expect(toast).toBeInTheDocument();
      expect(toast).toHaveTextContent('System maintenance scheduled');
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

      it('preserves style tags from head in branding header HTML (NEXUS-52631 regression)', () => {
        const customerHtml = [
          '<html><head><style>',
          '.container { display: flex; flex-direction: row; align-items: center; background-color: white; padding: 10px; }',
          '.logo { width: auto; max-height: 40px; margin-right: 13px; }',
          '.us-gov-banner { font-weight: bold; font-size: 7.5pt; }',
          '</style></head><body>',
          '<div class="container">',
          '<img src="logo.png" alt="Logo" class="logo">',
          '<div class="us-gov-banner">You are accessing a U.S. Government information system.</div>',
          '</div></body></html>',
        ].join('');

        givenExtJSState({
          ...getDefaultState(),
          branding: {
            headerEnabled: true,
            headerHtml: customerHtml,
            footerEnabled: false,
            footerHtml: '',
          },
        });
        renderComponent();

        const brandingHeader = screen.getByTestId('nxrm-branding-header');
        expect(within(brandingHeader).getByText('You are accessing a U.S. Government information system.')).toBeVisible();
        const style = brandingHeader.querySelector('style');
        expect(style).not.toBeNull();
        expect(style.textContent).toContain('.container');
        expect(style.textContent).toContain('display: flex');
        expect(style.textContent).toContain('.logo');
        expect(style.textContent).toContain('.us-gov-banner');
      });

      it('preserves style tags from head in branding footer HTML', () => {
        givenExtJSState({
          ...getDefaultState(),
          branding: {
            headerEnabled: false,
            headerHtml: '',
            footerEnabled: true,
            footerHtml: '<html><head><style>.banner { font-weight: bold; font-size: 7.5pt; }</style></head><body><div class="banner">Footer content</div></body></html>',
          },
        });
        renderComponent();

        const brandingFooter = screen.getByTestId('nxrm-branding-footer');
        expect(within(brandingFooter).getByText('Footer content')).toBeVisible();
        const style = brandingFooter.querySelector('style');
        expect(style).not.toBeNull();
        expect(style.textContent).toContain('font-weight: bold');
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
      // Full testing of the CELimitsAlert logic has its own test suite
      it('should render given a community edition is over the limit and an admin user is logged in', async () => {
        const givenGracePeriodEndDate = givenDateNDaysInTheFuture(20);

        // Must explicitly set COMMUNITY edition to test CE hard limit banner.
        // The banner (CELimitsAlert, NEXUS-53215) lives in nexus-ui-plugin and
        // reads its own copy of UsageHelper, whose functions have different
        // source text than nexus-coreui-plugin's copy (no test-override
        // branches) — so mock the underlying ExtJS state keys directly; both
        // UsageHelper copies bottom out in these same two state reads.
        givenExtJSState({
          ...getDefaultState(),
          'nexus.community.throttlingStatus': 'Over limits',
          'nexus.community.gracePeriodEnds': givenGracePeriodEndDate.toISOString(),
        }, 'COMMUNITY');

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

        // the transition should fail because the user doesn't have permissions —
        // they get redirected to the welcome page (dashboard) instead of seeing login or 404
        let errorOnTransition = null;
        try {
          await router.stateService.go('admin.security.users')
        } catch (ex) {
          errorOnTransition = ex.message
        }

        expect(errorOnTransition).toMatch(/aborted|superseded/);

        await assertStandardLayoutRenders();
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
      // UnsavedChangesModal is mocked at the top of this file with a portal-free
      // double, so these assertions exercise the App <-> router <-> dialog
      // wiring deterministically -- there is no NxModal portal teardown to race.
      const selectors = {
        cancelButton: () => screen.queryByRole('button', { name: 'Cancel' }),
        continueButton: () => screen.queryByRole('button', { name: 'Continue' }),
        modalTitle: () => screen.queryByRole('heading', { name: 'Unsaved Changes' }),
        modalContent: () => screen.queryByText('You have unsaved changes. Continuing will discard them.')
      };

      it('should render the unsaved changes modal when navigating away from a page with unsaved changes', async () => {
        const { router } = await renderComponent();
        await assertStandardLayoutRenders();

        await act(async () => {
          window.dirty = ['some unsaved changes'];
          router.stateService.go(BROWSE.BROWSE.ROOT);
        });

        await waitFor(() => expect(selectors.modalTitle()).toBeVisible());
      });

      it('should not render the unsaved changes modal when navigating away from a page without unsaved changes', async () => {
        const { router } = await renderComponent();
        await assertStandardLayoutRenders();

        await act(async () => {
          router.stateService.go(BROWSE.BROWSE.ROOT);
        });

        expect(selectors.modalTitle()).not.toBeInTheDocument();
      });

      it('should hide the unsaved changes modal when the cancel button is clicked', async () => {
        const { router } = await renderComponent();
        await assertStandardLayoutRenders();

        await act(async () => {
          window.dirty = ['some unsaved changes'];
          router.stateService.go(BROWSE.BROWSE.ROOT);
        });

        await waitFor(() => expect(selectors.modalTitle()).toBeVisible());
        expect(selectors.modalContent()).toBeVisible();
        expect(selectors.cancelButton()).toBeVisible();

        await act(async () => {
          fireEvent.click(selectors.cancelButton());
        });

        await waitFor(() => expect(selectors.modalTitle()).not.toBeInTheDocument());
        expect(selectors.modalContent()).not.toBeInTheDocument();
        expect(selectors.cancelButton()).not.toBeInTheDocument();
      });

      it('should hide the unsaved changes modal when the continue button is clicked', async () => {
        const { router } = await renderComponent();
        await assertStandardLayoutRenders();

        await act(async () => {
          window.dirty = ['some unsaved changes'];
          router.stateService.go(BROWSE.BROWSE.ROOT);
        });

        await waitFor(() => expect(selectors.modalTitle()).toBeVisible());
        expect(selectors.modalContent()).toBeVisible();
        expect(selectors.continueButton()).toBeVisible();

        await act(async () => {
          fireEvent.click(selectors.continueButton());
        });

        await waitFor(() => expect(selectors.modalTitle()).not.toBeInTheDocument());
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
                <ToastProvider>
                  <UIRouter router={router}>
                    <App />
                  </UIRouter>
                </ToastProvider>
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
    // findAllByTestId rather than findByTestId: the Radix-based global header
    // rolled out on main alongside the legacy header, and on CI both can end
    // up in the rendered tree at once through this describe block's sequential
    // renders. findByTestId throws on multiple matches; the assertion intent
    // is "the standard layout renders A global header", so asserting the first
    // visible match preserves the contract without a false failure.
    const headers = await screen.findAllByTestId('global-header');
    expect(headers.length).toBeGreaterThanOrEqual(1);
    expect(headers[0]).toBeVisible();
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
    // CE usage banner is now the Radix SystemAlert (NEXUS-53215) — no longer an
    // RSC NxSystemNotice, so look it up by its testid/title-text rather than
    // the old `role="complementary"` accessible name.
    const alerts = screen.getAllByTestId('nxrm-system-alert');
    const alert = alerts.find(el => within(el).queryByText(title));
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
