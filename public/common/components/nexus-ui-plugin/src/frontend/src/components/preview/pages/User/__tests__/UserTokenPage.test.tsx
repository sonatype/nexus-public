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
import { ToastProvider } from '../../../shared';

import { UserTokenPage } from '../UserTokenPage';
import { FEATURE_DISABLED_MESSAGE } from '../useUserToken';
import { APIConstants } from '../../../../../constants/APIConstants';

// ---------------------------------------------------------------------------
// Mocks
// ---------------------------------------------------------------------------

const mockRequestAuthenticationToken = jest.fn();

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    requestAuthenticationToken: (...args: unknown[]) => mockRequestAuthenticationToken(...args),
    useUser: jest.fn(() => ({ id: 'testuser', userId: 'testuser' })),
  },
}));

jest.mock('axios', () => ({
  get: jest.fn(),
  post: jest.fn(),
  delete: jest.fn(),
  isAxiosError: (err: unknown) => typeof err === 'object' && err !== null && (err as {isAxiosError?: boolean}).isAxiosError === true,
}));

const mockAxios = jest.requireMock('axios');

Object.defineProperty(navigator, 'clipboard', {
  value: { writeText: jest.fn().mockResolvedValue(undefined) },
  configurable: true,
});

// Epoch-ms for 2027-01-15 — used in has-token tests (future date)
const FUTURE_EPOCH_MS = String(new Date('2027-01-15T10:00:00Z').getTime());

// Real attributes endpoint path (without leading slash, as defined in APIConstants)
const ATTRIBUTES_PATH = APIConstants.REST.USER_TOKEN_TIMESTAMP;

// Admin-only endpoint that the Preview UI page must NOT call. Regression sentinel for
// NEXUS-54023: the page used to eagerly fire this on load, which broke every non-admin.
const ADMIN_SETTINGS_URL_FRAGMENT = 'security/user-tokens';

function attributesReject(status: number, data: unknown = '') {
  return Promise.reject({ isAxiosError: true, response: { status, data } });
}

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

  // BDD-52136-005 — regression guard: must call the real endpoint, never the old one
  it('calls the URL from APIConstants.REST.USER_TOKEN_TIMESTAMP, not a hard-coded path', async () => {
    mockAxios.get.mockImplementation((url: string) => {
      if (url.includes(ATTRIBUTES_PATH)) return attributesReject(404);
      return Promise.resolve({ data: null });
    });

    renderPage();

    await waitFor(() => expect(screen.getByTestId('no-token-state')).toBeInTheDocument());

    const calls: string[] = mockAxios.get.mock.calls.map((c: unknown[]) => c[0] as string);
    expect(calls.some((u) => u.includes(ATTRIBUTES_PATH))).toBe(true);
    expect(calls.every((u) => !u.includes('user-token-timestamp'))).toBe(true);
  });

  // NEXUS-54023 — regression: never touch the admin usertoken-settings endpoint from the
  // per-user page. That endpoint is guarded by nexus:usertoken-settings:read, so eagerly
  // calling it 403'd every non-admin user and dropped the page into loadError.
  describe('NEXUS-54023 non-admin regression', () => {
    it('never calls the admin security/user-tokens endpoint on page load', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return attributesReject(404);
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => expect(screen.getByTestId('no-token-state')).toBeInTheDocument());

      const calls: string[] = mockAxios.get.mock.calls.map((c: unknown[]) => c[0] as string);
      expect(calls.every((u) => !u.includes(ADMIN_SETTINGS_URL_FRAGMENT))).toBe(true);
    });

    it('renders normally when the admin endpoint would have returned 403', async () => {
      // Simulate the non-admin runtime: any call to the admin endpoint would 403.
      // The test fails loudly (rejection surfaces as loadError) if the page ever calls it.
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ADMIN_SETTINGS_URL_FRAGMENT)) {
          return Promise.reject({
            isAxiosError: true,
            response: { status: 403, data: 'Subject does not have permission [nexus:usertoken-settings:read]' },
          });
        }
        if (url.includes(ATTRIBUTES_PATH)) return attributesReject(404);
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => expect(screen.getByTestId('no-token-state')).toBeInTheDocument());
      expect(screen.queryByText(/failed to load/i)).not.toBeInTheDocument();
    });
  });

  describe('State 0: Loading', () => {
    it('shows spinner while loading', () => {
      mockAxios.get.mockReturnValue(new Promise(() => {}));
      renderPage();
      expect(screen.getByTestId('user-token-page-loading')).toBeInTheDocument();
      expect(screen.getByText(/loading your user token status/i)).toBeInTheDocument();
    });
  });

  // State 1: derived from a 400 on the /attributes endpoint with the well-known
  // "feature is not enabled" body. Two branches for the response shape because
  // WebApplicationMessageException(Status, String) defaults to text/plain but the
  // ValidationErrorXO entity commonly serializes to JSON in this codebase.
  describe('State 1: Tokens Disabled', () => {
    it('shows disabled callout when attributes returns 400 with plain-text disabled body', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return attributesReject(400, FEATURE_DISABLED_MESSAGE);
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('tokens-disabled-callout')).toBeInTheDocument();
      });

      expect(screen.getByText(/user tokens are not enabled/i)).toBeInTheDocument();
      expect(screen.queryByTestId('generate-token-btn')).not.toBeInTheDocument();
    });

    it('shows disabled callout when attributes returns 400 with JSON disabled body', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) {
          return attributesReject(400, { id: '*', message: FEATURE_DISABLED_MESSAGE });
        }
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('tokens-disabled-callout')).toBeInTheDocument();
      });

      expect(screen.queryByTestId('generate-token-btn')).not.toBeInTheDocument();
    });
  });

  // BDD-52136-003
  describe('State 2: No Token Exists', () => {
    beforeEach(() => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return attributesReject(404);
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

    it('treats 404 as no-token state, not an error', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('no-token-state')).toBeInTheDocument();
      });

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

  // BDD-52136-001: token with expiration
  describe('State 3a: Token Exists (with expiration)', () => {
    beforeEach(() => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return Promise.resolve({ data: { expirationTimeTimestamp: FUTURE_EPOCH_MS } });
        if (url.includes('user-token?authToken')) return Promise.resolve({ data: { nameCode: 'uc', passCode: 'pc' } });
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

    it('shows Expires row when expirationTimeTimestamp is present', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('has-token-state')).toBeInTheDocument();
      });

      expect(screen.getByTestId('expires-row')).toBeInTheDocument();
      expect(screen.queryByTestId('created-row')).not.toBeInTheDocument();
    });

    it('Access Token calls auth then GET and shows reveal modal', async () => {
      mockRequestAuthenticationToken.mockResolvedValue('auth123');

      renderPage();

      await waitFor(() => expect(screen.getByTestId('access-token-btn')).toBeInTheDocument());

      fireEvent.click(screen.getByTestId('access-token-btn'));

      await waitFor(() => {
        expect(screen.getByTestId('token-reveal-dialog')).toBeInTheDocument();
      });

      expect(mockRequestAuthenticationToken).toHaveBeenCalled();
    });
  });

  // BDD-52136-002: token without expiration
  describe('State 3b: Token Exists (no expiration)', () => {
    beforeEach(() => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return Promise.resolve({ data: {} });
        return Promise.resolve({ data: null });
      });
    });

    it('renders has-token state with Access and Reset buttons, no Expires row', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('has-token-state')).toBeInTheDocument();
      });

      expect(screen.getByTestId('access-token-btn')).toBeInTheDocument();
      expect(screen.getByTestId('reset-token-btn')).toBeInTheDocument();
      expect(screen.queryByTestId('expires-row')).not.toBeInTheDocument();
      expect(screen.queryByTestId('created-row')).not.toBeInTheDocument();
    });
  });

  // BDD-52136-004 / BDD-52749-004
  describe('State 4: Expired Token', () => {
    beforeEach(() => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return attributesReject(410);
        return Promise.resolve({ data: null });
      });
    });

    it('shows expired callout and Generate + Reset buttons', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('expired-token-state')).toBeInTheDocument();
      });

      expect(screen.getByTestId('generate-token-btn')).toBeInTheDocument();
      expect(screen.getByTestId('reset-token-btn')).toBeInTheDocument();
      expect(screen.getByText(/token has expired/i)).toBeInTheDocument();
    });

    it('does not show Access Token button for expired token', async () => {
      renderPage();

      await waitFor(() => {
        expect(screen.getByTestId('expired-token-state')).toBeInTheDocument();
      });

      expect(screen.queryByTestId('access-token-btn')).not.toBeInTheDocument();
    });
  });

  describe('State 5: Token Reveal Modal', () => {
    it('shows countdown in modal and auto-closes after 60s', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return attributesReject(404);
        return Promise.resolve({ data: null });
      });
      mockRequestAuthenticationToken.mockResolvedValue('tok');
      mockAxios.post.mockResolvedValue({ data: { nameCode: 'n', passCode: 'p' } });

      renderPage();

      await waitFor(() => expect(screen.getByTestId('generate-token-btn')).toBeInTheDocument());
      fireEvent.click(screen.getByTestId('generate-token-btn'));

      await waitFor(() => expect(screen.getByTestId('token-reveal-dialog')).toBeInTheDocument());

      expect(screen.getByTestId('countdown-text')).toBeInTheDocument();

      act(() => jest.advanceTimersByTime(60000));

      await waitFor(() => {
        expect(screen.queryByTestId('token-reveal-dialog')).not.toBeInTheDocument();
      });
    });

    it('copy buttons call clipboard.writeText', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return attributesReject(404);
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

  describe('State: API Error', () => {
    it('shows error state and Retry button on 500 response', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return attributesReject(500, 'Internal Server Error');
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => expect(screen.getByTestId('error-state')).toBeInTheDocument());
      expect(screen.getByTestId('retry-btn')).toBeInTheDocument();
      expect(screen.queryByTestId('generate-token-btn')).not.toBeInTheDocument();
    });

    it('shows error state on network error', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return Promise.reject(new Error('Network Error'));
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => expect(screen.getByTestId('error-state')).toBeInTheDocument());
      expect(screen.queryByTestId('no-token-state')).not.toBeInTheDocument();
    });

    // Brittleness guard: only 400s carrying the well-known "not enabled" body should be
    // treated as disabled. Any other 400 must fall through to the generic error path so
    // future validation errors are not silently misclassified.
    it('treats an unrelated 400 as an error, not as disabled', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) {
          return attributesReject(400, { id: 'someField', message: 'Some other validation error' });
        }
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => expect(screen.getByTestId('error-state')).toBeInTheDocument());
      expect(screen.queryByTestId('tokens-disabled-callout')).not.toBeInTheDocument();
    });

    // Tightening guard: JSON-shape message match is exact-equality, not substring, so a
    // hypothetical superset message ("...not enabled for this realm") is not silently
    // misclassified as disabled.
    it('does not treat a JSON 400 whose message is a superset of the disabled string as disabled', async () => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) {
          return attributesReject(400, { id: '*', message: `${FEATURE_DISABLED_MESSAGE} for this realm` });
        }
        return Promise.resolve({ data: null });
      });

      renderPage();

      await waitFor(() => expect(screen.getByTestId('error-state')).toBeInTheDocument());
      expect(screen.queryByTestId('tokens-disabled-callout')).not.toBeInTheDocument();
    });
  });

  describe('Reset Confirmation', () => {
    beforeEach(() => {
      mockAxios.get.mockImplementation((url: string) => {
        if (url.includes(ATTRIBUTES_PATH)) return Promise.resolve({ data: { expirationTimeTimestamp: FUTURE_EPOCH_MS } });
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
