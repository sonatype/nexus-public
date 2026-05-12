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
import { render, screen, waitFor, act } from '@testing-library/react';
import Axios from 'axios';
import { AuthProvider, useAuth } from '../AuthContext';
import * as extJsLoader from '../../utils/extJsLoader';

jest.mock('axios');
jest.mock('../../utils/extJsLoader');

const mockedAxios = Axios;
const mockedExtJsLoader = extJsLoader;

// Test component to access auth context
function TestConsumer() {
  const auth = useAuth();
  return (
    <div>
      <span data-testid="loading">{String(auth.isLoading)}</span>
      <span data-testid="authenticated">{String(auth.isAuthenticated)}</span>
      <span data-testid="user">{auth.user ? auth.user.id : 'null'}</span>
      <button data-testid="refresh" onClick={auth.refreshUser}>
        Refresh
      </button>
      <button data-testid="hasUser" onClick={() => auth.hasUser()}>
        Has User
      </button>
    </div>
  );
}

describe('AuthContext', () => {
  const mockUser = {
    id: 'admin',
    authenticatedRealms: ['NexusAuthorizingRealm'],
    administrator: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedExtJsLoader.isExtJSLoaded.mockReturnValue(false);
    mockedExtJsLoader.onExtJSLoad.mockImplementation(() => {});
    delete window.NX;
    delete window.Ext;
  });

  describe('AuthProvider', () => {
    it('renders children', async () => {
      mockedAxios.get.mockResolvedValueOnce({ data: mockUser });

      render(
        <AuthProvider>
          <div data-testid="child">Child Content</div>
        </AuthProvider>
      );

      expect(screen.getByTestId('child')).toHaveTextContent('Child Content');
    });

    it('starts with loading true', () => {
      mockedAxios.get.mockResolvedValueOnce({ data: mockUser });

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      expect(screen.getByTestId('loading')).toHaveTextContent('true');
    });

    it('fetches user from API when ExtJS is not loaded', async () => {
      mockedAxios.get.mockResolvedValueOnce({ data: mockUser });

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(mockedAxios.get).toHaveBeenCalledWith('/service/rest/internal/ui/user');
      expect(screen.getByTestId('user')).toHaveTextContent('admin');
    });

    it('sets user to null on 401 response', async () => {
      mockedAxios.get.mockRejectedValueOnce({
        response: { status: 401 },
      });

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('user')).toHaveTextContent('null');
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    });

    it('sets user to null on 403 response', async () => {
      mockedAxios.get.mockRejectedValueOnce({
        response: { status: 403 },
      });

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('user')).toHaveTextContent('null');
    });

    it('sets user to null on other API errors', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      mockedAxios.get.mockRejectedValueOnce(new Error('Network error'));

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('user')).toHaveTextContent('null');
      consoleSpy.mockRestore();
    });

    it('uses ExtJS user state when ExtJS is loaded', async () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {
        State: {
          getUser: jest.fn().mockReturnValue(mockUser),
        },
      };

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(window.NX.State.getUser).toHaveBeenCalled();
      expect(screen.getByTestId('user')).toHaveTextContent('admin');
      // Should not call API when ExtJS is loaded
      expect(mockedAxios.get).not.toHaveBeenCalled();
    });

    it('subscribes to ExtJS user changes', async () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {
        State: {
          getUser: jest.fn().mockReturnValue(mockUser),
        },
      };

      const mockOn = jest.fn();
      const mockUn = jest.fn();
      window.Ext = {
        getApplication: jest.fn().mockReturnValue({
          getController: jest.fn().mockReturnValue({
            on: mockOn,
            un: mockUn,
          }),
        }),
      };

      const { unmount } = render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(mockOn).toHaveBeenCalledWith('userchanged', expect.any(Function));

      // Unmount should unsubscribe
      unmount();
      expect(mockUn).toHaveBeenCalledWith('userchanged', expect.any(Function));
    });

    it('syncs with ExtJS when it loads later', async () => {
      mockedAxios.get.mockResolvedValueOnce({ data: null });

      let extJsLoadCallback;
      mockedExtJsLoader.onExtJSLoad.mockImplementation((cb) => {
        extJsLoadCallback = cb;
      });

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      // Simulate ExtJS loading later
      window.NX = {
        State: {
          getUser: jest.fn().mockReturnValue(mockUser),
        },
      };

      act(() => {
        extJsLoadCallback();
      });

      await waitFor(() => {
        expect(screen.getByTestId('user')).toHaveTextContent('admin');
      });
    });
  });

  describe('useAuth', () => {
    it('returns default context when used outside AuthProvider', () => {
      // The AuthContext has a default value, so it doesn't throw
      // This tests that the default context works
      render(<TestConsumer />);

      // Default context has isLoading: true, user: null
      expect(screen.getByTestId('loading')).toHaveTextContent('true');
      expect(screen.getByTestId('user')).toHaveTextContent('null');
      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    });

    it('provides hasUser function', async () => {
      mockedAxios.get.mockResolvedValueOnce({ data: mockUser });

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });

    it('provides refreshUser function', async () => {
      mockedAxios.get.mockResolvedValueOnce({ data: null });

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('user')).toHaveTextContent('null');

      // Setup for refresh
      mockedAxios.get.mockResolvedValueOnce({ data: mockUser });

      const refreshButton = screen.getByTestId('refresh');
      await act(async () => {
        refreshButton.click();
      });

      await waitFor(() => {
        expect(screen.getByTestId('user')).toHaveTextContent('admin');
      });
    });
  });

  describe('hasUser', () => {
    it('returns false when no user is loaded', async () => {
      mockedAxios.get.mockResolvedValueOnce({ data: null });

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('authenticated')).toHaveTextContent('false');
    });

    it('returns true when user is loaded', async () => {
      mockedAxios.get.mockResolvedValueOnce({ data: mockUser });

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });

    it('checks ExtJS state when ExtJS is loaded', async () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {
        State: {
          getUser: jest.fn().mockReturnValue(mockUser),
        },
      };

      render(
        <AuthProvider>
          <TestConsumer />
        </AuthProvider>
      );

      await waitFor(() => {
        expect(screen.getByTestId('loading')).toHaveTextContent('false');
      });

      expect(screen.getByTestId('authenticated')).toHaveTextContent('true');
    });
  });
});
