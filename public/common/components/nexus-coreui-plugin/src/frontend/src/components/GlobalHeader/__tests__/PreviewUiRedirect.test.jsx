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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

const mockStateValues = {
  'nexus-context-path': '/',
  'anonymousEnabled': true,
  'loggedInEnabled': true,
  'defaultToPreviewUi': true,
  'disableLegacyUi': false,
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
  useSideNavbarOpenState: () => [true],
}));

jest.mock('@uirouter/react', () => ({
  useRouter: jest.fn(() => ({ stateService: { go: jest.fn() } })),
}));

jest.mock('../../../contexts/ThemeContext', () => ({
  useTheme: jest.fn(() => ({ effectiveTheme: 'light', setTheme: jest.fn() })),
}));

jest.mock('../SearchRadix', () => () => <div data-testid="search-radix" />);
jest.mock('../SystemStatusRadix', () => () => <div data-testid="system-status" />);
jest.mock('../HelpMenuRadix', () => () => <div data-testid="help-menu" />);
jest.mock('../LoginAndUserButtonRadix', () => () => <div data-testid="login-button" />);
jest.mock('../../ThemeSwitcher/ThemeSwitcher', () => () => null);

import GlobalHeaderRadix from '../GlobalHeaderRadix';

function renderHeader() {
  return render(
    <Theme>
      <GlobalHeaderRadix />
    </Theme>
  );
}

describe('GlobalHeaderRadix - Preview UI Redirect and Kill Switch', () => {
  const originalHash = window.location.hash;

  beforeEach(() => {
    jest.clearAllMocks();
    window.location.hash = '';
    sessionStorage.clear();
    mockUseUser.mockReturnValue({ authenticated: false });
    mockUseStatus.mockReturnValue({ edition: 'PRO' });
    mockCheckPermission.mockReturnValue(true);
    mockRestClientPost.mockClear();
    mockStateValues['anonymousEnabled'] = true;
    mockStateValues['loggedInEnabled'] = true;
    mockStateValues['defaultToPreviewUi'] = true;
    mockStateValues['disableLegacyUi'] = false;
    mockState.mockReturnValue({
      getValue: jest.fn((key, defaultVal) => mockStateValues[key] ?? defaultVal),
    });
  });

  afterAll(() => {
    window.location.hash = originalHash;
  });

  it('1. Redirects from empty hash to preview/browse/welcome (Soft Default)', async () => {
    window.location.hash = '';
    renderHeader();
    await waitFor(() => {
      expect(window.location.hash).toBe('#preview/browse/welcome');
    });
  });

  it('2. Redirects from / to preview/browse/welcome (Soft Default)', async () => {
    window.location.hash = '/';
    renderHeader();
    await waitFor(() => {
      expect(window.location.hash).toBe('#preview/browse/welcome');
    });
  });

  it('3. Redirects from browse/welcome to preview/browse/welcome (Soft Default)', async () => {
    window.location.hash = 'browse/welcome';
    renderHeader();
    await waitFor(() => {
      expect(window.location.hash).toBe('#preview/browse/welcome');
    });
  });

  it('4. Does not redirect if defaultToPreviewUi is false', async () => {
    mockStateValues['defaultToPreviewUi'] = false;
    window.location.hash = 'browse/welcome';
    renderHeader();
    // Wait a bit to ensure no redirect happens
    await new Promise(r => setTimeout(r, 100));
    expect(window.location.hash).toBe('#browse/welcome');
  });

  it('5. Does not redirect if user lacks permission', async () => {
    mockUseUser.mockReturnValue({ authenticated: true, userId: 'viewer' });
    mockStateValues['loggedInEnabled'] = false;
    window.location.hash = 'browse/welcome';
    renderHeader();
    await new Promise(r => setTimeout(r, 100));
    expect(window.location.hash).toBe('#browse/welcome');
  });

  it('6. Does not redirect if already in Preview UI', async () => {
    window.location.hash = 'preview/browse/welcome';
    renderHeader();
    await new Promise(r => setTimeout(r, 100));
    expect(window.location.hash).toBe('#preview/browse/welcome');
  });

  it('7. Does not redirect deep links (e.g. admin/security/users) under Soft Default', async () => {
    window.location.hash = 'admin/security/users';
    renderHeader();
    await new Promise(r => setTimeout(r, 100));
    expect(window.location.hash).toBe('#admin/security/users');
  });

  it('8. Prevents redirect loops under Soft Default', async () => {
    window.location.hash = 'browse/welcome';
    sessionStorage.setItem('user_requested_legacy', 'true');
    renderHeader();
    
    // Should NOT redirect because session storage flag is set
    await new Promise(r => setTimeout(r, 100));
    expect(window.location.hash).toBe('#browse/welcome');
  });

  it('9. Manual toggle wins over default setting (sets session flag)', async () => {
    window.location.hash = 'preview/browse/welcome';
    renderHeader();

    const toggleButton = screen.getByText('Switch to Classic UI');
    fireEvent.click(toggleButton);

    const useClassicBtn = await screen.findByText('Use Classic UI');
    fireEvent.click(useClassicBtn);

    expect(sessionStorage.getItem('user_requested_legacy')).toBe('true');
    expect(window.location.hash).toBe('#browse/welcome');
  });

  describe('Kill Switch (disableLegacyUi: true)', () => {
    beforeEach(() => {
      mockStateValues['disableLegacyUi'] = true;
    });

    it('10. Redirects even deep links to preview equivalents', async () => {
      window.location.hash = 'admin/security/users';
      renderHeader();
      await waitFor(() => {
        expect(window.location.hash).toBe('#preview/admin/security/users');
      });
    });

    it('11. Redirects legacy user/account to standalone preview.user.account (not Settings layout)', async () => {
      window.location.hash = 'user/account';
      renderHeader();
      await waitFor(() => {
        expect(window.location.hash).toBe('#preview/user/account');
      });
    });

    it('12. Redirects landing pages to preview welcome', async () => {
      window.location.hash = 'browse/welcome';
      renderHeader();
      await waitFor(() => {
        expect(window.location.hash).toBe('#preview/browse/welcome');
      });
    });

    it('13. Hides the header toggle button completely', () => {
      renderHeader();
      expect(screen.queryByText('Switch to Nexus One UI')).not.toBeInTheDocument();
      expect(screen.queryByText('Switch to Classic UI')).not.toBeInTheDocument();
    });

    it('14. Session flag user_requested_legacy is ignored', async () => {
      window.location.hash = 'browse/welcome';
      sessionStorage.setItem('user_requested_legacy', 'true');
      renderHeader();
      
      // Kill switch should still force redirect
      await waitFor(() => {
        expect(window.location.hash).toBe('#preview/browse/welcome');
      });
    });

    it('15. Direct URL entry to legacy route is forced to preview', async () => {
      window.location.hash = 'admin/system/blobstores';
      renderHeader();
      await waitFor(() => {
        expect(window.location.hash).toBe('#preview/admin/system/blobstores');
      });
    });

    it('16. Kill switch overrides defaultToPreviewUi: false', async () => {
      mockStateValues['defaultToPreviewUi'] = false;
      window.location.hash = 'browse/welcome';
      renderHeader();
      await waitFor(() => {
        expect(window.location.hash).toBe('#preview/browse/welcome');
      });
    });
  });
});
