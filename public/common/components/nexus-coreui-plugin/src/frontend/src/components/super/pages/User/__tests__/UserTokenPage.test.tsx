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
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';
import { ToastProvider } from '../../../../shared';

import { UserTokenPage } from '../UserTokenPage';

// ---------------------------------------------------------------------------
// Mocks
// ---------------------------------------------------------------------------

const mockRequestAuthenticationToken = jest.fn();

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    requestAuthenticationToken: (...args: unknown[]) => mockRequestAuthenticationToken(...args),
    useUser: jest.fn(() => ({ id: 'testuser', userId: 'testuser' })),
  },
}));

jest.mock('axios', () => ({
  get: jest.fn(),
  post: jest.fn(),
  delete: jest.fn(),
}));

const mockAxios = jest.requireMock('axios');

Object.defineProperty(navigator, 'clipboard', {
  value: { writeText: jest.fn().mockResolvedValue(undefined) },
  configurable: true,
});

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme><ToastProvider>{children}</ToastProvider></Theme>;
}

function renderPage() {
  return render(<UserTokenPage />, { wrapper: TestWrapper });
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('UserTokenPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  describe('State 0: Loading', () => {
    it('shows spinner while loading', () => {
      // Make axios never resolve
      mockAxios.get.mockReturnValue(new Promise(() => {}));
      renderPage();
      expect(screen.getByTestId('user-token-page-loading')).toBeInTheDocument();
      expect(screen.getByText(/loading your user token status/i)).toBeInTheDocument();
    });
  });

  describe('State 1: Tokens Disabled', () => {
    it('shows disabled callout when tokens are off', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes('security/user-tokens')) return Promise.resolve({ data: { enabled: false } });
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('tokens-disabled-callout')).toBeInTheDocument();
      });

      expect(screen.getByText(/user tokens are not enabled/i)).toBeInTheDocument();
      expect(screen.queryByTestId('generate-token-btn')).not.toBeInTheDocument();
    });
  });

  describe('State 2: No Token Exists', () => {
    beforeEach(() => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes('security/user-tokens')) return Promise.resolve({ data: { enabled: true } });
        if (url.includes('user-token-timestamp')) return Promise.resolve({ data: null });
        return Promise.resolve({ data: null });
      });
    });

    it('renders Generate Token button when no token exists', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('no-token-state')).toBeInTheDocument();
      });

      expect(screen.getByTestId('generate-token-btn')).toBeInTheDocument();
      expect(screen.getByText(/no token generated/i)).toBeInTheDocument();
    });

    it('treats timestamp 404 as no-token state (not an error) — mrqu real fix', async () => {
      // This is what the real server returns when admin has no token yet
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes('security/user-tokens')) return Promise.resolve({ data: { enabled: true } });
        if (url.includes('user-token-timestamp')) {
          const err = { response: { status: 404 } };
          return Promise.reject(err);
        }
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('no-token-state')).toBeInTheDocument();
      });

      // No error toast should have fired
      expect(screen.queryByText(/failed to load/i)).not.toBeInTheDocument();
    });

    it('calls generate API after auth and shows reveal modal', async () => {
      const authToken = 'myauthtoken';
      mockRequestAuthenticationToken.mockResolvedValue(authToken);
      mockAxios.post.mockResolvedValue({ data: { nameCode: 'user123', passCode: 'pass456' } });

      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('generate-token-btn')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByTestId('generate-token-btn'));

      await waitFor(() => {
        expect(screen.getByTestId('token-reveal-dialog')).toBeInTheDocument();
      });

      expect(screen.getByText('user123')).toBeInTheDocument();
      expect(screen.getByText('pass456')).toBeInTheDocument();
    });
  });

  describe('State 3: Token Exists', () => {
    beforeEach(() => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes('security/user-tokens')) return Promise.resolve({ data: { enabled: true } });
        if (url.includes('user-token-timestamp')) return Promise.resolve({ data: { created: '2026-01-15T10:00:00Z' } });
        return Promise.resolve({ data: null });
      });
    });

    it('renders Access and Reset buttons when token exists', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('has-token-state')).toBeInTheDocument();
      });

      expect(screen.getByTestId('access-token-btn')).toBeInTheDocument();
      expect(screen.getByTestId('reset-token-btn')).toBeInTheDocument();
      expect(screen.getByText(/token active/i)).toBeInTheDocument();
    });

    it('Access Token calls auth then GET and shows reveal modal', async () => {
      mockRequestAuthenticationToken.mockResolvedValue('auth123');
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes('security/user-tokens')) return Promise.resolve({ data: { enabled: true } });
        if (url.includes('user-token-timestamp')) return Promise.resolve({ data: { created: '2026-01-15T10:00:00Z' } });
        if (url.includes('user-token?authToken')) return Promise.resolve({ data: { nameCode: 'uc', passCode: 'pc' } });
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => expect(screen.getByTestId('access-token-btn')).toBeInTheDocument());

      fireEvent.click(screen.getByTestId('access-token-btn'));

      await waitFor(() => {
        expect(screen.getByTestId('token-reveal-dialog')).toBeInTheDocument();
      });

      expect(mockRequestAuthenticationToken).toHaveBeenCalled();
    });
  });

  describe('State 4: Token Reveal Modal', () => {
    it('shows countdown in modal and auto-closes after 60s', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes('security/user-tokens')) return Promise.resolve({ data: { enabled: true } });
        if (url.includes('user-token-timestamp')) return Promise.resolve({ data: null });
        return Promise.resolve({ data: null });
      });
      mockRequestAuthenticationToken.mockResolvedValue('tok');
      mockAxios.post.mockResolvedValue({ data: { nameCode: 'n', passCode: 'p' } });

      renderPage();

      await waitFor(() => expect(screen.getByTestId('generate-token-btn')).toBeInTheDocument());
      fireEvent.click(screen.getByTestId('generate-token-btn'));

      await waitFor(() => expect(screen.getByTestId('token-reveal-dialog')).toBeInTheDocument());

      expect(screen.getByTestId('countdown-text')).toBeInTheDocument();

      // Advance 60 seconds — modal should close
      act(() => jest.advanceTimersByTime(60000));

      await waitFor(() => {
        expect(screen.queryByTestId('token-reveal-dialog')).not.toBeInTheDocument();
      });
    });

    it('copy buttons call clipboard.writeText', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes('security/user-tokens')) return Promise.resolve({ data: { enabled: true } });
        if (url.includes('user-token-timestamp')) return Promise.resolve({ data: null });
        return Promise.resolve({ data: null });
      });
      mockRequestAuthenticationToken.mockResolvedValue('tok');
      mockAxios.post.mockResolvedValue({ data: { nameCode: 'myname', passCode: 'mypass' } });

      renderPage();
      await waitFor(() => expect(screen.getByTestId('generate-token-btn')).toBeInTheDocument());
      fireEvent.click(screen.getByTestId('generate-token-btn'));

      await waitFor(() => expect(screen.getByTestId('token-reveal-dialog')).toBeInTheDocument());

      const copyBtn = screen.getByTestId('copy-btn-user-code-(username)');
      fireEvent.click(copyBtn);

      expect(navigator.clipboard.writeText).toHaveBeenCalledWith('myname');
    });
  });

  describe('Reset Confirmation', () => {
    beforeEach(() => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes('security/user-tokens')) return Promise.resolve({ data: { enabled: true } });
        if (url.includes('user-token-timestamp')) return Promise.resolve({ data: { created: '2026-01-15T10:00:00Z' } });
        return Promise.resolve({ data: null });
      });
    });

    it('shows ConfirmDialog when Reset Token is clicked', async () => {
      renderPage();
      await waitFor(() => expect(screen.getByTestId('reset-token-btn')).toBeInTheDocument());

      fireEvent.click(screen.getByTestId('reset-token-btn'));

      await waitFor(() => {
        expect(screen.getByTestId('reset-token-dialog-confirm')).toBeInTheDocument();
      });
    });

    it('calls DELETE API after reset confirmation', async () => {
      mockRequestAuthenticationToken.mockResolvedValue('delauth');
      mockAxios.delete.mockResolvedValue({});

      renderPage();
      await waitFor(() => expect(screen.getByTestId('reset-token-btn')).toBeInTheDocument());

      fireEvent.click(screen.getByTestId('reset-token-btn'));
      await waitFor(() => expect(screen.getByTestId('reset-token-dialog-confirm')).toBeInTheDocument());

      fireEvent.click(screen.getByTestId('reset-token-dialog-confirm'));

      await waitFor(() => {
        expect(mockAxios.delete).toHaveBeenCalledWith(
          expect.stringContaining('user-token?authToken=')
        );
      });
    });
  });
});
