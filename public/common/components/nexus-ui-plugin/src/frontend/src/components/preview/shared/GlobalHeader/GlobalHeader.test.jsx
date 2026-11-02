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

// Consolidates self-hosted's GlobalHeaderRadix.test.jsx, __tests__/GlobalHeaderRadix.test.jsx,
// __tests__/PreviewUiRedirect.test.jsx and __tests__/SystemStatusRadix.test.jsx, plus cloud's
// GlobalHeader.test.jsx shell-rendering assertions that are not already covered by SearchRadix's,
// LoginAndUserMenu's, HelpMenu's or SystemStatusBell's own suites (NEXUS-54869).

import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

const mockStateValues = {
  'nexus-context-path': '/',
  anonymousEnabled: true,
  loggedInEnabled: true,
  disableSwitchFeedback: false,
  disableLegacyUi: false,
  defaultToPreviewUi: false,
  editionId: 'nexus-professional-edition',
};

const mockUseUser = jest.fn(() => ({ authenticated: false }));
const mockUseStatus = jest.fn(() => ({ edition: 'PRO', version: '3.78.0' }));
const mockRestClientPost = jest.fn(() => Promise.resolve({ data: {} }));
const mockState = jest.fn(() => ({
  getValue: jest.fn((key, defaultVal) => mockStateValues[key] ?? defaultVal),
}));
// Promoted to a jest.fn so individual tests can override behavior (e.g. simulate the
// early-bootstrap window where ExtJS.useState is not yet attached and returns undefined).
const mockUseState = jest.fn((fn) => fn());
const mockIsExtJsRendered = jest.fn(() => false);
const mockHasUnsavedChanges = jest.fn(() => false);
let mockIsCloud = false;

jest.mock('../../../../interface/ExtJS', () => ({
  ExtJS: {
    useStatus: (...args) => mockUseStatus(...args),
    useUser: (...args) => mockUseUser(...args),
    state: (...args) => mockState(...args),
    useState: (...args) => mockUseState(...args),
    isExtJsRendered: (...args) => mockIsExtJsRendered(...args),
    refresh: jest.fn(),
  },
}));

jest.mock('../../../../router/extJsUnsavedChanges', () => ({
  handleExtJsUnsavedChanges: jest.fn((_, cb) => cb()),
}));

jest.mock('../hooks/useUnsavedChangesWarning', () => ({
  hasUnsavedChanges: (...args) => mockHasUnsavedChanges(...args),
}));

jest.mock('../../../../interface/api', () => ({
  restClient: {
    post: (...args) => mockRestClientPost(...args),
  },
}));

jest.mock('../../../../contexts/ThemeContext', () => ({
  useTheme: jest.fn(() => ({ effectiveTheme: 'light', setTheme: jest.fn() })),
}));

jest.mock('../../../../hooks/useSideNavbarOpenState', () => ({
  useSideNavbarOpenState: () => [true],
}));

jest.mock('../hooks', () => ({
  useIsCloud: () => mockIsCloud,
}));

jest.mock('../HelpMenu', () => ({ HelpMenu: () => <div data-testid="help-menu" /> }));
jest.mock('../ThemeSwitcher', () => () => <div data-testid="theme-switcher" />);
jest.mock('../SearchRadix', () => ({ SearchRadix: () => <div data-testid="search-radix" /> }));
// Expose the fold props so tests can assert what the header hands the menu at each width.
jest.mock('../LoginAndUserMenu', () => ({
  LoginAndUserMenu: (props) => (
    <div
      data-testid="login-button"
      data-show-theme-item={String(Boolean(props.showThemeItem))}
      data-show-ui-switch-item={String(Boolean(props.showUiSwitchItem))}
      data-has-ui-switch-handler={String(typeof props.onUiSwitch === 'function')}
      data-show-nexus-one-switch-item={String(Boolean(props.showNexusOneSwitchItem))}
      data-has-nexus-one-switch-handler={String(typeof props.onNexusOneSwitch === 'function')}
      onClick={() => props.onUiSwitch?.()}
    />
  ),
}));
jest.mock('../SystemStatusBell/SystemStatusBell', () => (props) => (
  <div data-testid="system-status" data-classname={props.className} data-analytics-id={props.dataAnalyticsId} />
));

jest.mock('@uirouter/react', () => ({
  useRouter: jest.fn(() => ({ stateService: { go: jest.fn(), reload: jest.fn() } })),
}));

import GlobalHeader from './GlobalHeader';
import { handleExtJsUnsavedChanges } from '../../../../router/extJsUnsavedChanges';

const MD_MIN_WIDTH = 1024;
const XS_MIN_WIDTH = 768;
const WIDE_VIEWPORT = 1440;
const NARROW_VIEWPORT = 320;

// Drive useMediaQuery from an explicit width (jsdom matchMedia defaults to false = narrowest). Default wide; tests opt into narrow.
function setViewport(width) {
  window.matchMedia = jest.fn((query) => {
    const match = /min-width:\s*(\d+)px/.exec(query);
    const minWidth = match ? Number(match[1]) : 0;
    return {
      matches: width >= minWidth,
      media: query,
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      addListener: jest.fn(),
      removeListener: jest.fn(),
      dispatchEvent: jest.fn(),
    };
  });
}

function renderHeader(props) {
  return render(
    <Theme>
      <GlobalHeader {...props} />
    </Theme>
  );
}

describe.each([
  ['self-hosted', false],
  ['cloud', true],
])('GlobalHeader (%s, useIsCloud() -> %s)', (_label, isCloud) => {
  beforeEach(() => {
    jest.clearAllMocks();
    setViewport(WIDE_VIEWPORT);
    mockIsCloud = isCloud;
    window.location.hash = '';
    sessionStorage.clear();
    mockUseUser.mockReturnValue({ authenticated: false });
    mockUseStatus.mockReturnValue({ edition: 'PRO', version: '3.78.0' });
    mockStateValues['anonymousEnabled'] = true;
    mockStateValues['loggedInEnabled'] = true;
    mockStateValues['disableSwitchFeedback'] = false;
    mockStateValues['disableLegacyUi'] = false;
    mockStateValues['defaultToPreviewUi'] = false;
    mockStateValues['editionId'] = 'nexus-professional-edition';
    mockState.mockReturnValue({
      getValue: jest.fn((key, defaultVal) => mockStateValues[key] ?? defaultVal),
    });
    mockUseState.mockImplementation((fn) => fn());
    mockIsExtJsRendered.mockReturnValue(false);
    mockHasUnsavedChanges.mockReturnValue(false);
  });

  it('renders logo, search, help and login', () => {
    renderHeader();
    expect(screen.getByTitle('Home')).toBeInTheDocument();
    expect(screen.getByTestId('search-radix')).toBeInTheDocument();
    expect(screen.getByTestId('help-menu')).toBeInTheDocument();
    expect(screen.getByTestId('login-button')).toBeInTheDocument();
    expect(screen.getByLabelText('Refresh')).toBeInTheDocument();
  });

  describe('SystemStatusBell', () => {
    it('uses the same className/dataAnalyticsId on both distributions', () => {
      renderHeader();
      const bell = screen.getByTestId('system-status');
      expect(bell).toHaveAttribute('data-classname', 'nxrm-system-status');
      expect(bell).toHaveAttribute('data-analytics-id', 'nxrm-global-header-system-status');
    });
  });

  describe('edition badge', () => {
    it('renders the edition badge when editionId resolves to a known edition', () => {
      renderHeader();
      expect(screen.getByText('Pro')).toBeInTheDocument();
    });

    it('does not render an empty badge when editionId is an empty string', () => {
      mockStateValues['editionId'] = '';
      const { container } = renderHeader();
      expect(container.querySelector('.rt-Badge')).not.toBeInTheDocument();
    });
  });

  describe('bootstrap resilience', () => {
    it('does not throw when ExtJS.useState returns undefined during early boot', () => {
      mockUseState.mockImplementation(() => undefined);
      expect(() => renderHeader()).not.toThrow();
    });

    it('does not render the edition badge before editionId resolves at bootstrap', () => {
      mockUseState.mockImplementation(() => undefined);
      const { container } = renderHeader();
      expect(container.querySelector('.rt-Badge')).not.toBeInTheDocument();
    });
  });

  describe('login/logout race condition (bug gyca)', () => {
    it('keeps toggle visible when loggedInEnabled flashes false during login', () => {
      const { rerender } = renderHeader();
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();

      mockStateValues.loggedInEnabled = false;
      mockUseUser.mockReturnValue({ authenticated: true, id: 'admin' });

      rerender(<Theme><GlobalHeader /></Theme>);

      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
    });

    it('hides toggle on logout when anonymous access is disabled', () => {
      const { rerender } = renderHeader();
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();

      mockUseUser.mockReturnValue({ authenticated: false });
      mockStateValues.anonymousEnabled = false;

      rerender(<Theme><GlobalHeader /></Theme>);

      expect(screen.queryByText('Switch to Nexus One UI')).not.toBeInTheDocument();
    });

    it('keeps toggle visible on logout when anonymous access is enabled', () => {
      const { rerender } = renderHeader();
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();

      mockUseUser.mockReturnValue({ authenticated: false });
      mockStateValues.anonymousEnabled = true;

      rerender(<Theme><GlobalHeader /></Theme>);

      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
    });

    it('does not redirect out of preview during flag flaps', () => {
      window.location.hash = 'preview/browse/welcome';
      const { rerender } = renderHeader();

      mockStateValues.loggedInEnabled = false;
      rerender(<Theme><GlobalHeader /></Theme>);

      expect(window.location.hash).toBe('#preview/browse/welcome');
    });
  });

  // NEXUS-53775: this guard must be unconditional — it is not behind useIsCloud() — because
  // both distributions load the same shared ExtJS Menu controller and crash the same way when
  // it is invoked with no nx-feature-content mounted. Running this describe block for both
  // isCloud values locks that in.
  describe('onRefreshClick — no backing ExtJS controller', () => {
    beforeEach(() => {
      mockIsExtJsRendered.mockReturnValue(false);
      jest.spyOn(window, 'confirm');
    });

    it('refreshes without confirm when there are no unsaved changes', async () => {
      mockHasUnsavedChanges.mockReturnValue(false);
      renderHeader();

      await userEvent.click(screen.getByRole('button', { name: 'Refresh' }));

      expect(window.confirm).not.toHaveBeenCalled();
      expect(handleExtJsUnsavedChanges).not.toHaveBeenCalled();
    });

    it('prompts and refreshes when the user confirms with unsaved changes', async () => {
      mockHasUnsavedChanges.mockReturnValue(true);
      window.confirm.mockReturnValue(true);
      renderHeader();

      await userEvent.click(screen.getByRole('button', { name: 'Refresh' }));

      expect(window.confirm).toHaveBeenCalled();
    });

    it('does not refresh when the user cancels with unsaved changes', async () => {
      mockHasUnsavedChanges.mockReturnValue(true);
      window.confirm.mockReturnValue(false);
      const router = { stateService: { go: jest.fn(), reload: jest.fn() } };
      jest.spyOn(require('@uirouter/react'), 'useRouter').mockReturnValue(router);
      renderHeader();

      await userEvent.click(screen.getByRole('button', { name: 'Refresh' }));

      expect(window.confirm).toHaveBeenCalled();
      expect(router.stateService.reload).not.toHaveBeenCalled();
    });
  });

  describe('onRefreshClick — backed by ExtJS Menu controller', () => {
    it('delegates to handleExtJsUnsavedChanges rather than the React dirty-state bypass', async () => {
      mockIsExtJsRendered.mockReturnValue(true);
      renderHeader();

      await userEvent.click(screen.getByRole('button', { name: 'Refresh' }));

      expect(handleExtJsUnsavedChanges).toHaveBeenCalled();
    });
  });

  describe('Classic UI fallback (unmapped routes)', () => {
    it('shows "Not Available in Classic UI" dialog without sending feedback on unmapped Preview routes', async () => {
      window.location.hash = 'preview/experimental/feature';
      mockUseUser.mockReturnValue({ authenticated: true, userId: 'admin' });

      renderHeader();

      fireEvent.click(screen.getByText('Switch to Classic UI'));

      const textarea = await screen.findByLabelText('Optional feedback');
      fireEvent.change(textarea, { target: { value: 'unsupported route feedback' } });

      const useClassicBtn = await screen.findByText('Use Classic UI');
      fireEvent.click(useClassicBtn);

      expect(mockRestClientPost).not.toHaveBeenCalled();
      expect(screen.getByText('Not Available in Classic UI')).toBeInTheDocument();
      expect(screen.getByText('Stay on Nexus One UI')).toBeInTheDocument();
      expect(screen.getByText('Go to Classic Dashboard')).toBeInTheDocument();
    });
  });

  describe('switch destination', () => {
    it('lands on the Classic home page from a deep Preview route', () => {
      window.location.hash = 'preview/admin/repository/repositories/my-repo';
      mockUseUser.mockReturnValue({ authenticated: true, userId: 'admin' });
      mockStateValues['disableSwitchFeedback'] = true;

      renderHeader();
      fireEvent.click(screen.getByText('Switch to Classic UI'));

      expect(window.location.hash).toBe('#browse/welcome');
    });

    it('lands on the Nexus One home page from a deep Classic route', () => {
      window.location.hash = 'admin/repository/repositories:my-repo';
      mockUseUser.mockReturnValue({ authenticated: true, userId: 'admin' });

      renderHeader();
      fireEvent.click(screen.getByText('Switch to Nexus One UI'));

      expect(window.location.hash).toBe('#preview/browse/welcome');
    });
  });

  describe('switch feedback disabled', () => {
    it('bypasses the popup and does not POST when switch feedback is disabled', () => {
      window.location.hash = 'preview/browse/welcome';
      mockUseUser.mockReturnValue({ authenticated: true, userId: 'admin' });
      mockStateValues['disableSwitchFeedback'] = true;

      renderHeader();
      fireEvent.click(screen.getByText('Switch to Classic UI'));

      expect(screen.queryByLabelText('Optional feedback')).not.toBeInTheDocument();
      expect(mockRestClientPost).not.toHaveBeenCalled();
      expect(sessionStorage.getItem('user_requested_legacy')).toBe('true');
      expect(window.location.hash).toBe('#browse/welcome');
    });
  });

  describe('accessibility: toggle button aria-label', () => {
    it('has aria-label when in Classic UI mode', () => {
      renderHeader();
      expect(screen.getByRole('button', { name: 'Switch to Nexus One UI' })).toBeInTheDocument();
    });

    it('has aria-label when in Nexus One UI mode', () => {
      window.location.hash = 'preview/browse/welcome';
      renderHeader();
      expect(screen.getByRole('button', { name: 'Switch to Classic UI' })).toBeInTheDocument();
    });
  });

  describe('feedback endpoint on Classic UI switch', () => {
    async function openPopoverAndClickSwitch(feedback = '') {
      window.location.hash = 'preview/browse/welcome';
      renderHeader();

      fireEvent.click(screen.getByText('Switch to Classic UI'));
      const useClassicBtn = await screen.findByText('Use Classic UI');

      if (feedback) {
        const textarea = screen.getByLabelText('Optional feedback');
        fireEvent.change(textarea, { target: { value: feedback } });
      }

      await act(async () => {
        fireEvent.click(useClassicBtn);
      });
    }

    it('sends feedback text, edition, and version to backend proxy on switch', async () => {
      await openPopoverAndClickSwitch('Love the new UI!');

      expect(mockRestClientPost).toHaveBeenCalledTimes(1);
      const [url, body] = mockRestClientPost.mock.calls[0];
      expect(url).toBe('/service/rest/internal/ui/switch-feedback');
      expect(body.edition).toBe('nexus-professional-edition');
      expect(body.version).toBe('3.78.0');
      expect(body.feedback).toBe('Love the new UI!');
    });

    it('sends empty string feedback when textarea is empty', async () => {
      await openPopoverAndClickSwitch('');

      expect(mockRestClientPost).toHaveBeenCalledTimes(1);
      const [, body] = mockRestClientPost.mock.calls[0];
      expect(body.feedback).toBe('');
    });

    it('does not block navigation when restClient.post rejects', async () => {
      mockRestClientPost.mockImplementationOnce(() => Promise.reject(new Error('network error')));
      await openPopoverAndClickSwitch('test');
      expect(sessionStorage.getItem('user_requested_legacy')).toBe('true');
    });

    it('sends feedback when exactly at the 500 character limit', async () => {
      const maxFeedback = 'x'.repeat(500);
      await openPopoverAndClickSwitch(maxFeedback);

      expect(mockRestClientPost).toHaveBeenCalledTimes(1);
      const [, body] = mockRestClientPost.mock.calls[0];
      expect(body.feedback).toHaveLength(500);
    });
  });

  describe('feedback character limit validation', () => {
    beforeEach(() => {
      window.location.hash = 'preview/browse/welcome';
      mockUseUser.mockReturnValue({ authenticated: true, userId: 'admin' });
    });

    it('shows error message when feedback exceeds 500 characters', async () => {
      renderHeader();
      fireEvent.click(screen.getByText('Switch to Classic UI'));

      const textarea = await screen.findByLabelText('Optional feedback');
      fireEvent.change(textarea, { target: { value: 'x'.repeat(501) } });

      expect(await screen.findByText(/Feedback must be 500 characters or less/)).toBeInTheDocument();
      expect(screen.getByText(/1 characters too many/)).toBeInTheDocument();
    });

    it('disables Use Classic UI button when feedback exceeds limit', async () => {
      renderHeader();
      fireEvent.click(screen.getByText('Switch to Classic UI'));

      const textarea = await screen.findByLabelText('Optional feedback');
      fireEvent.change(textarea, { target: { value: 'x'.repeat(501) } });

      expect(await screen.findByText('Use Classic UI')).toBeDisabled();
    });

    it('does not send feedback when exceeding the limit and the button is clicked', async () => {
      renderHeader();
      fireEvent.click(screen.getByText('Switch to Classic UI'));

      const textarea = await screen.findByLabelText('Optional feedback');
      fireEvent.change(textarea, { target: { value: 'x'.repeat(501) } });

      fireEvent.click(await screen.findByText('Use Classic UI'));

      expect(mockRestClientPost).not.toHaveBeenCalled();
    });
  });

  // Second real bugfix ported unconditionally to both distributions — see file header
  // comment. Cloud's pre-merge header never read defaultToPreviewUi and only used
  // disableLegacyUi to gate the toggle button's visibility (not to force navigation).
  describe('Preview UI rollout redirects (defaultToPreviewUi / disableLegacyUi)', () => {
    beforeEach(() => {
      mockStateValues['defaultToPreviewUi'] = true;
      mockStateValues['disableLegacyUi'] = false;
    });

    it('redirects from an empty hash to preview/browse/welcome (soft default)', async () => {
      window.location.hash = '';
      renderHeader();
      await screen.findByText('Switch to Classic UI');
      expect(window.location.hash).toBe('#preview/browse/welcome');
    });

    it('does not redirect when defaultToPreviewUi is false', async () => {
      mockStateValues['defaultToPreviewUi'] = false;
      window.location.hash = 'browse/welcome';
      renderHeader();
      await screen.findByText('Switch to Nexus One UI');
      expect(window.location.hash).toBe('#browse/welcome');
    });

    it('does not redirect deep links under the soft default', async () => {
      window.location.hash = 'admin/security/users';
      renderHeader();
      await screen.findByText('Switch to Nexus One UI');
      expect(window.location.hash).toBe('#admin/security/users');
    });

    it('honors user_requested_legacy and does not loop back into preview', async () => {
      window.location.hash = 'browse/welcome';
      sessionStorage.setItem('user_requested_legacy', 'true');
      renderHeader();
      await screen.findByText('Switch to Nexus One UI');
      expect(window.location.hash).toBe('#browse/welcome');
    });

    describe('hard kill switch (disableLegacyUi: true)', () => {
      beforeEach(() => {
        mockStateValues['disableLegacyUi'] = true;
      });

      it('redirects deep links to their preview equivalents', async () => {
        window.location.hash = 'admin/security/users';
        renderHeader();
        await act(async () => {});
        expect(window.location.hash).toBe('#preview/admin/security/users');
      });

      it('redirects legacy user/account to the standalone preview.user.account route', async () => {
        window.location.hash = 'user/account';
        renderHeader();
        await act(async () => {});
        expect(window.location.hash).toBe('#preview/user/account');
      });

      it('hides the header toggle button completely', () => {
        renderHeader();
        expect(screen.queryByText('Switch to Nexus One UI')).not.toBeInTheDocument();
        expect(screen.queryByText('Switch to Classic UI')).not.toBeInTheDocument();
      });

      it('ignores user_requested_legacy and still forces the redirect', async () => {
        window.location.hash = 'browse/welcome';
        sessionStorage.setItem('user_requested_legacy', 'true');
        renderHeader();
        await act(async () => {});
        expect(window.location.hash).toBe('#preview/browse/welcome');
      });

      it('overrides defaultToPreviewUi: false', async () => {
        mockStateValues['defaultToPreviewUi'] = false;
        window.location.hash = 'browse/welcome';
        renderHeader();
        await act(async () => {});
        expect(window.location.hash).toBe('#preview/browse/welcome');
      });
    });
  });

  describe('sidebar toggle', () => {
    it('dispatches nx-sidebar-toggle when clicked', async () => {
      renderHeader();
      const dispatchSpy = jest.spyOn(window, 'dispatchEvent');

      await userEvent.click(screen.getByTestId('sidebar-toggle-button'));

      const dispatched = dispatchSpy.mock.calls.find(
        ([evt]) => evt instanceof CustomEvent && evt.type === 'nx-sidebar-toggle'
      );
      expect(dispatched).toBeDefined();
      dispatchSpy.mockRestore();
    });
  });

  describe('responsive reflow (NEXUS-55145)', () => {
    function renderPreview(width) {
      setViewport(width);
      window.location.hash = '#preview/browse/welcome';
      return renderHeader();
    }

    it('shouldShowRefreshAtWideWidth', () => {
      renderPreview(WIDE_VIEWPORT);
      expect(screen.getByLabelText('Refresh')).toBeInTheDocument();
    });

    it('shouldShowRefreshDownToXs', () => {
      renderPreview(XS_MIN_WIDTH);
      expect(screen.getByLabelText('Refresh')).toBeInTheDocument();
    });

    it('shouldHideRefreshBelowXs', () => {
      renderPreview(NARROW_VIEWPORT);
      expect(screen.queryByLabelText('Refresh')).not.toBeInTheDocument();
    });

    it('shouldShowThemeSwitcherInBarAtWideWidth', () => {
      renderPreview(WIDE_VIEWPORT);
      expect(screen.getByTestId('theme-switcher')).toBeInTheDocument();
    });

    it('shouldHideThemeSwitcherFromBarBelowMd', () => {
      renderPreview(MD_MIN_WIDTH - 1);
      expect(screen.queryByTestId('theme-switcher')).not.toBeInTheDocument();
    });

    it('shouldShowUiSwitchButtonInBarAtWideWidth', () => {
      renderPreview(WIDE_VIEWPORT);
      expect(screen.getByTestId('ui-toggle-button')).toBeInTheDocument();
    });

    it('shouldHideUiSwitchButtonFromBarBelowMd', () => {
      renderPreview(MD_MIN_WIDTH - 1);
      expect(screen.queryByTestId('ui-toggle-button')).not.toBeInTheDocument();
    });

    it('shouldNotFoldControlsIntoMenuAtWideWidth', () => {
      renderPreview(WIDE_VIEWPORT);
      const menu = screen.getByTestId('login-button');
      expect(menu).toHaveAttribute('data-show-theme-item', 'false');
      expect(menu).toHaveAttribute('data-show-ui-switch-item', 'false');
    });

    it('shouldFoldThemeAndUiSwitchIntoMenuBelowMd', () => {
      renderPreview(MD_MIN_WIDTH - 1);
      const menu = screen.getByTestId('login-button');
      expect(menu).toHaveAttribute('data-show-theme-item', 'true');
      expect(menu).toHaveAttribute('data-show-ui-switch-item', 'true');
      expect(menu).toHaveAttribute('data-has-ui-switch-handler', 'true');
    });

    it('shouldNotFoldUiSwitchWhenLegacyUiDisabled', () => {
      mockStateValues['disableLegacyUi'] = true;
      renderPreview(MD_MIN_WIDTH - 1);
      const menu = screen.getByTestId('login-button');
      expect(menu).toHaveAttribute('data-show-ui-switch-item', 'false');
    });

    it('shouldRenderSearchOnOwnRowBelowMd', () => {
      const { container } = renderPreview(MD_MIN_WIDTH - 1);
      expect(
        container.querySelector('.nxrm-global-header-radix__search--own-row')
      ).toBeInTheDocument();
      expect(screen.getByTestId('search-radix')).toBeInTheDocument();
    });

    it('shouldRenderSearchInlineAtWideWidth', () => {
      const { container } = renderPreview(WIDE_VIEWPORT);
      expect(
        container.querySelector('.nxrm-global-header-radix__search--own-row')
      ).not.toBeInTheDocument();
      expect(screen.getByTestId('search-radix')).toBeInTheDocument();
    });

    it('shouldRenderSingleSearchInstanceAtEveryWidth', () => {
      renderPreview(NARROW_VIEWPORT);
      expect(screen.getAllByTestId('search-radix')).toHaveLength(1);
    });

    it('shouldShowFullCommunityEditionLabelAtWideWidth', () => {
      mockStateValues['editionId'] = 'nexus-community-edition';
      renderPreview(WIDE_VIEWPORT);
      expect(screen.getByText('Community Edition')).toBeInTheDocument();
    });

    it('shouldShortenCommunityEditionLabelBelowXs', () => {
      mockStateValues['editionId'] = 'nexus-community-edition';
      renderPreview(NARROW_VIEWPORT);
      expect(screen.getByText('Community')).toBeInTheDocument();
      expect(screen.queryByText('Community Edition')).not.toBeInTheDocument();
    });

    it('shouldKeepFullEnterpriseLabelBelowXsWhenNoShortFormExists', () => {
      mockStateValues['editionId'] = 'nexus-enterprise-edition';
      renderPreview(NARROW_VIEWPORT);
      expect(screen.getByText('Enterprise')).toBeInTheDocument();
    });

    it('shouldOpenFeedbackDialogFromFoldedSwitchWithoutPosting', async () => {
      renderPreview(MD_MIN_WIDTH - 1);

      await act(async () => {
        fireEvent.click(screen.getByTestId('login-button'));
      });

      expect(await screen.findByText('Use Classic UI')).toBeInTheDocument();
      expect(mockRestClientPost).not.toHaveBeenCalled();
    });

    it('shouldSwitchDirectlyFromFoldedSwitchWhenFeedbackDisabled', async () => {
      mockStateValues['disableSwitchFeedback'] = true;
      renderPreview(MD_MIN_WIDTH - 1);

      await act(async () => {
        fireEvent.click(screen.getByTestId('login-button'));
      });

      expect(screen.queryByText('Use Classic UI')).not.toBeInTheDocument();
      expect(sessionStorage.getItem('user_requested_legacy')).toBe('true');
      expect(mockRestClientPost).not.toHaveBeenCalled();
    });

    it('shouldPostTypedFeedbackFromFoldedSwitchDialog', async () => {
      renderPreview(MD_MIN_WIDTH - 1);

      await act(async () => {
        fireEvent.click(screen.getByTestId('login-button'));
      });
      const textarea = await screen.findByLabelText('Optional feedback');
      fireEvent.change(textarea, { target: { value: 'Prefer classic' } });
      await act(async () => {
        fireEvent.click(screen.getByText('Use Classic UI'));
      });

      expect(mockRestClientPost).toHaveBeenCalledTimes(1);
      const [url, body] = mockRestClientPost.mock.calls[0];
      expect(url).toBe('/service/rest/internal/ui/switch-feedback');
      expect(body.feedback).toBe('Prefer classic');
    });
  });

  describe('nexus one switch fold on classic routes (NEXUS-54499)', () => {
    function renderClassic(width) {
      setViewport(width);
      window.location.hash = '#browse/welcome';
      return renderHeader();
    }

    it('shouldShowNexusOneSwitchButtonInBarAtWideWidth', () => {
      renderClassic(WIDE_VIEWPORT);
      expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
      expect(screen.getByTestId('login-button')).toHaveAttribute('data-show-nexus-one-switch-item', 'false');
    });

    it('shouldFoldNexusOneSwitchIntoMenuBelowMd', () => {
      renderClassic(MD_MIN_WIDTH - 1);
      expect(screen.queryByText('Switch to Nexus One UI')).not.toBeInTheDocument();
      const menu = screen.getByTestId('login-button');
      expect(menu).toHaveAttribute('data-show-nexus-one-switch-item', 'true');
      expect(menu).toHaveAttribute('data-has-nexus-one-switch-handler', 'true');
    });

    it('shouldNotFoldNexusOneSwitchWhenLegacyUiDisabled', () => {
      mockStateValues['disableLegacyUi'] = true;
      renderClassic(MD_MIN_WIDTH - 1);
      expect(screen.getByTestId('login-button')).toHaveAttribute('data-show-nexus-one-switch-item', 'false');
    });
  });
});
