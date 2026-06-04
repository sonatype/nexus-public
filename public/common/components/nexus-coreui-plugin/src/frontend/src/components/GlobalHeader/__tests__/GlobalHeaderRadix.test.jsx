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
import { render, screen, fireEvent, act } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

const mockStateValues = {
  'nexus-context-path': '/',
  'anonymousEnabled': true,
  'loggedInEnabled': true,
  'disableSwitchFeedback': false,
};

const mockUseUser = jest.fn(() => ({ authenticated: false }));
const mockUseStatus = jest.fn(() => ({ edition: 'PRO' }));
const mockCheckPermission = jest.fn(() => true);
const mockRestClientPost = jest.fn(() => Promise.resolve({ data: {} }));
const mockState = jest.fn(() => ({
  getValue: jest.fn((key, defaultVal) => mockStateValues[key] ?? defaultVal),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    useStatus: (...args) => mockUseStatus(...args),
    useUser: (...args) => mockUseUser(...args),
    state: (...args) => mockState(...args),
    checkPermission: (...args) => mockCheckPermission(...args),
    isExtJsRendered: jest.fn(() => false),
    refresh: jest.fn(),
  },
  handleExtJsUnsavedChanges: jest.fn((_, cb) => cb()),
  restClient: {
    post: (...args) => mockRestClientPost(...args),
  },
}));

jest.mock('@uirouter/react', () => ({
  useRouter: jest.fn(() => ({ stateService: { go: jest.fn() } })),
}));

jest.mock('../../../contexts/ThemeContext', () => ({
  useTheme: jest.fn(() => ({ effectiveTheme: 'light', setTheme: jest.fn() })),
}));

jest.mock('../SearchRadix', () => () => <div data-testid="search-radix" />);
jest.mock('../HelpMenuRadix', () => () => <div data-testid="help-menu" />);
jest.mock('../LoginAndUserButtonRadix', () => () => <div data-testid="login-button" />);
jest.mock('../../ThemeSwitcher/ThemeSwitcher', () => () => null);
jest.mock('../../../hooks/useSideNavbarCollapsedState', () => () => [true]);

import GlobalHeaderRadix from '../GlobalHeaderRadix';

function renderHeader() {
  return render(
    <Theme>
      <GlobalHeaderRadix />
    </Theme>
  );
}

describe('GlobalHeaderRadix', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.location.hash = '';
    mockUseUser.mockReturnValue({ authenticated: false });
    mockUseStatus.mockReturnValue({ edition: 'PRO', version: '3.78.0' });
    mockCheckPermission.mockReturnValue(true);
    mockStateValues['anonymousEnabled'] = true;
    mockStateValues['loggedInEnabled'] = true;
    mockStateValues['disableSwitchFeedback'] = false;
    mockState.mockReturnValue({
      getValue: jest.fn((key, defaultVal) => mockStateValues[key] ?? defaultVal),
    });
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('shows UI toggle button for anonymous user when anonymousEnabled', () => {
    renderHeader();
    expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
  });

  it('hides UI toggle button when anonymousEnabled is false', () => {
    mockStateValues['anonymousEnabled'] = false;

    renderHeader();
    expect(screen.queryByText('Switch to Nexus One UI')).not.toBeInTheDocument();
  });

  it('shows UI toggle button for logged-in user with permission', () => {
    mockUseUser.mockReturnValue({ authenticated: true, userId: 'admin' });

    renderHeader();
    expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
  });

  it('does not lose UI toggle button during login transition (race condition fix)', () => {
    mockUseUser.mockReturnValue({ authenticated: false });
    const { rerender } = renderHeader();

    expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();

    mockUseUser.mockReturnValue({ authenticated: true, userId: 'admin' });

    rerender(
      <Theme>
        <GlobalHeaderRadix />
      </Theme>
    );

    expect(screen.getByText('Switch to Nexus One UI')).toBeInTheDocument();
  });

  it('hides button when logged-in user lacks permission', () => {
    mockUseUser.mockReturnValue({ authenticated: true, userId: 'viewer' });
    mockStateValues['loggedInEnabled'] = false;
    mockStateValues['anonymousEnabled'] = false;

    renderHeader();
    expect(screen.queryByText('Switch to Nexus One UI')).not.toBeInTheDocument();
  });

  it('renders logo and core UI elements', () => {
    renderHeader();
    expect(screen.getByTitle('Home')).toBeInTheDocument();
    expect(screen.getByTestId('search-radix')).toBeInTheDocument();
    expect(screen.getByTestId('help-menu')).toBeInTheDocument();
    expect(screen.getByTestId('login-button')).toBeInTheDocument();
    expect(screen.getByLabelText('Refresh')).toBeInTheDocument();
  });

  describe('Classic UI fallback (unmapped routes)', () => {
    it('shows "Not Available in Classic UI" dialog without sending feedback on unmapped Preview routes', async () => {
      window.location.hash = 'preview/experimental/feature';
      mockUseUser.mockReturnValue({ authenticated: true, userId: 'admin' });

      renderHeader();

      const toggleBtn = screen.getByText('Switch to Classic UI');
      expect(toggleBtn).toBeInTheDocument();

      fireEvent.click(toggleBtn);

      const textarea = await screen.findByLabelText('Optional feedback');
      fireEvent.change(textarea, { target: { value: 'unsupported route feedback' } });

      const useClassicBtn = await screen.findByText('Use Classic UI');
      fireEvent.click(useClassicBtn);

      expect(mockRestClientPost).not.toHaveBeenCalled();
      expect(screen.getByText('Not Available in Classic UI')).toBeInTheDocument();
      expect(screen.getByText(/This view is only available in the Nexus One UI/)).toBeInTheDocument();
      expect(screen.getByText('Stay on Nexus One UI')).toBeInTheDocument();
      expect(screen.getByText('Go to Classic Dashboard')).toBeInTheDocument();
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
      expect(screen.queryByText(/Feedback collection has been disabled by your administrator/)).not.toBeInTheDocument();
      expect(screen.queryByText('Use Classic UI')).not.toBeInTheDocument();

      expect(mockRestClientPost).not.toHaveBeenCalled();
      expect(sessionStorage.getItem('user_requested_legacy')).toBe('true');
      expect(window.location.hash).toBe('#browse/welcome');
    });
  });

  describe('accessibility: toggle button aria-label (M-11)', () => {
    it('toggle button has aria-label when in Classic UI mode', () => {
      // Default state: not in Nexus One UI path, button shows "Switch to Nexus One UI"
      render(
        <Theme>
          <GlobalHeaderRadix />
        </Theme>
      );

      const toggleBtn = screen.getByRole('button', { name: 'Switch to Nexus One UI' });
      expect(toggleBtn).toBeInTheDocument();
    });

    it('toggle button has aria-label when in Nexus One UI mode', () => {
      // Simulate being on a Nexus One UI path
      window.location.hash = 'preview/browse/welcome';

      render(
        <Theme>
          <GlobalHeaderRadix />
        </Theme>
      );

      const toggleBtn = screen.getByRole('button', { name: 'Switch to Classic UI' });
      expect(toggleBtn).toBeInTheDocument();
    });
  });

  describe('Feedback endpoint on Classic UI switch', () => {
    async function openPopoverAndClickSwitch(feedback = '') {
      window.location.hash = 'preview/browse/welcome';
      renderHeader();

      const toggleBtn = screen.getByText('Switch to Classic UI');
      fireEvent.click(toggleBtn);

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
      expect(body.edition).toBe('PRO');
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

    it('sends feedback when exactly at 500 character limit', async () => {
      const maxFeedback = 'x'.repeat(500);
      await openPopoverAndClickSwitch(maxFeedback);

      expect(mockRestClientPost).toHaveBeenCalledTimes(1);
      const [, body] = mockRestClientPost.mock.calls[0];
      expect(body.feedback).toHaveLength(500);
    });
  });

  describe('Feedback character limit validation', () => {
    beforeEach(() => {
      window.location.hash = 'preview/browse/welcome';
      mockUseUser.mockReturnValue({ authenticated: true, userId: 'admin' });
    });

    it('shows error message when feedback exceeds 500 characters', async () => {
      renderHeader();

      const toggleBtn = screen.getByText('Switch to Classic UI');
      fireEvent.click(toggleBtn);

      const textarea = await screen.findByLabelText('Optional feedback');
      const longFeedback = 'x'.repeat(501);
      fireEvent.change(textarea, { target: { value: longFeedback } });

      expect(await screen.findByText(/Feedback must be 500 characters or less/)).toBeInTheDocument();
      expect(screen.getByText(/1 characters too many/)).toBeInTheDocument();
    });

    it('disables Use Classic UI button when feedback exceeds limit', async () => {
      renderHeader();

      const toggleBtn = screen.getByText('Switch to Classic UI');
      fireEvent.click(toggleBtn);

      const textarea = await screen.findByLabelText('Optional feedback');
      const longFeedback = 'x'.repeat(501);
      fireEvent.change(textarea, { target: { value: longFeedback } });

      const useClassicBtn = await screen.findByText('Use Classic UI');
      expect(useClassicBtn).toBeDisabled();
    });

    it('enables button when feedback is within limit', async () => {
      renderHeader();

      const toggleBtn = screen.getByText('Switch to Classic UI');
      fireEvent.click(toggleBtn);

      const textarea = await screen.findByLabelText('Optional feedback');
      fireEvent.change(textarea, { target: { value: 'Valid feedback' } });

      const useClassicBtn = await screen.findByText('Use Classic UI');
      expect(useClassicBtn).not.toBeDisabled();
    });

    it('does not send feedback when exceeds limit and button is clicked', async () => {
      renderHeader();

      const toggleBtn = screen.getByText('Switch to Classic UI');
      fireEvent.click(toggleBtn);

      const textarea = await screen.findByLabelText('Optional feedback');
      const longFeedback = 'x'.repeat(501);
      fireEvent.change(textarea, { target: { value: longFeedback } });

      const useClassicBtn = await screen.findByText('Use Classic UI');
      fireEvent.click(useClassicBtn);

      expect(mockRestClientPost).not.toHaveBeenCalled();
    });
  });
});
