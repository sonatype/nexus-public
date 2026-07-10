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
import { act, render, screen } from '@testing-library/react';
import { PermissionsProvider, usePermissions, usePermission } from '../PermissionsContext';
import * as extJsLoader from '../../utils/extJsLoader';

jest.mock('../../utils/extJsLoader');

const mockedExtJsLoader = extJsLoader;

// Simple test component to access permissions context
function TestConsumer({ permission }) {
  const { checkPermission, hasPermission } = usePermissions();

  return (
    <div>
      <span data-testid="check">{String(checkPermission(permission || 'nexus:*'))}</span>
      <span data-testid="has">{String(hasPermission(permission || 'nexus:*'))}</span>
    </div>
  );
}

describe('PermissionsContext', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedExtJsLoader.isExtJSLoaded.mockReturnValue(false);
    mockedExtJsLoader.onExtJSLoad.mockImplementation(() => {});
    delete window.NX;
    delete window.Ext;
  });

  describe('PermissionsProvider', () => {
    it('renders children', () => {
      render(
        <PermissionsProvider>
          <div data-testid="child">Child Content</div>
        </PermissionsProvider>
      );

      expect(screen.getByTestId('child')).toHaveTextContent('Child Content');
    });

    it('returns false for permissions when ExtJS is not loaded', () => {
      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:repository-admin:*:*:read" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('false');
      expect(screen.getByTestId('has')).toHaveTextContent('false');
    });

    it('checks permissions via ExtJS when loaded', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {
        Permissions: {
          check: jest.fn().mockReturnValue(true),
        },
      };

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:repository-admin:*:*:read" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('true');
      expect(window.NX.Permissions.check).toHaveBeenCalledWith('nexus:repository-admin:*:*:read');
    });

    it('returns false for denied permissions via ExtJS', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {
        Permissions: {
          check: jest.fn().mockReturnValue(false),
        },
      };

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:settings:delete" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('false');
    });

    it('registers callback for ExtJS load', () => {
      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:*" />
        </PermissionsProvider>
      );

      expect(mockedExtJsLoader.onExtJSLoad).toHaveBeenCalledWith(expect.any(Function));
    });
  });

  describe('checkPermission', () => {
    it('delegates to NX.Permissions.check when ExtJS is loaded', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {
        Permissions: {
          check: jest.fn().mockImplementation((perm) => perm === 'nexus:admin'),
        },
      };

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:admin" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('true');
    });

    it('returns false when NX.Permissions is not available', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {}; // No Permissions object

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:*" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('false');
    });

    it('returns false when NX is undefined', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      // window.NX is undefined

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:*" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('false');
    });
  });

  describe('hasPermission', () => {
    it('is an alias for checkPermission', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {
        Permissions: {
          check: jest.fn().mockReturnValue(true),
        },
      };

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:*" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('true');
      expect(screen.getByTestId('has')).toHaveTextContent('true');
    });
  });

  describe('usePermissions', () => {
    it('returns default context values when used outside provider', () => {
      // Default context has restrictive defaults
      render(<TestConsumer permission="nexus:*" />);

      expect(screen.getByTestId('check')).toHaveTextContent('false');
      expect(screen.getByTestId('has')).toHaveTextContent('false');
    });
  });

  describe('usePermission hook', () => {
    function PermissionConsumer({ permission }) {
      const value = usePermission(permission);
      return <span data-testid="value">{String(value)}</span>;
    }

    function Wrapper({ children }) {
      return <PermissionsProvider>{children}</PermissionsProvider>;
    }

    it('returns false when ExtJS is not loaded', () => {
      render(<PermissionConsumer permission="nexus:admin" />, { wrapper: Wrapper });
      expect(screen.getByTestId('value')).toHaveTextContent('false');
    });

    it('returns the permission value from NX.Permissions.check when ExtJS is loaded', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = { Permissions: { check: jest.fn().mockReturnValue(true) } };

      render(<PermissionConsumer permission="nexus:admin" />, { wrapper: Wrapper });

      expect(screen.getByTestId('value')).toHaveTextContent('true');
    });

    it('re-evaluates when the ExtJS changed event fires', async () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      const checkMock = jest.fn().mockReturnValue(false);
      window.NX = { Permissions: { check: checkMock } };

      const changedListeners = [];
      window.Ext = {
        getApplication: jest.fn().mockReturnValue({
          getController: jest.fn().mockReturnValue({
            on: jest.fn((event, cb) => { if (event === 'changed') changedListeners.push(cb); }),
            un: jest.fn()
          })
        })
      };

      render(<PermissionConsumer permission="nexus:admin" />, { wrapper: Wrapper });
      expect(screen.getByTestId('value')).toHaveTextContent('false');

      // Simulate permissions becoming available and firing 'changed'
      checkMock.mockReturnValue(true);
      act(() => { changedListeners.forEach(fn => fn()); });

      expect(screen.getByTestId('value')).toHaveTextContent('true');
    });

    it('cleans up ExtJS listeners on unmount', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = { Permissions: { check: jest.fn().mockReturnValue(false) } };

      const unMock = jest.fn();
      window.Ext = {
        getApplication: jest.fn().mockReturnValue({
          getController: jest.fn().mockReturnValue({ on: jest.fn(), un: unMock })
        })
      };

      const { unmount } = render(<PermissionConsumer permission="nexus:admin" />, { wrapper: Wrapper });
      unmount();

      expect(unMock).toHaveBeenCalled();
    });

    it('registers listeners and re-evaluates when ExtJS loads after component mount', async () => {
      // Component mounts while ExtJS is not yet loaded
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(false);
      // Collect ALL onExtJSLoad callbacks — both PermissionsProvider and usePermission register one
      const extJsLoadCallbacks = [];
      mockedExtJsLoader.onExtJSLoad.mockImplementation((cb) => { extJsLoadCallbacks.push(cb); });

      window.NX = { Permissions: { check: jest.fn().mockReturnValue(false) } };

      const changedListeners = [];
      window.Ext = {
        getApplication: jest.fn().mockReturnValue({
          getController: jest.fn().mockReturnValue({
            on: jest.fn((event, cb) => { if (event === 'changed') changedListeners.push(cb); }),
            un: jest.fn()
          })
        })
      };

      render(<PermissionConsumer permission="nexus:admin" />, { wrapper: Wrapper });
      expect(screen.getByTestId('value')).toHaveTextContent('false');

      // ExtJS finishes loading — fire all queued onExtJSLoad callbacks
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      act(() => { extJsLoadCallbacks.forEach(cb => cb()); });

      // Listeners are now registered; simulate a subsequent login event
      window.NX.Permissions.check.mockReturnValue(true);
      act(() => { changedListeners.forEach(fn => fn()); });

      expect(screen.getByTestId('value')).toHaveTextContent('true');
    });
  });

  describe('permission patterns', () => {
    it('handles wildcard permissions', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {
        Permissions: {
          check: jest.fn().mockImplementation((perm) => {
            return perm === 'nexus:*' || perm.startsWith('nexus:read');
          }),
        },
      };

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:*" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('true');
    });

    it('handles repository-specific permissions', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      window.NX = {
        Permissions: {
          check: jest.fn().mockImplementation((perm) => {
            return perm === 'nexus:repository-admin:maven2:maven-central:read';
          }),
        },
      };

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:repository-admin:maven2:maven-central:read" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('true');
    });

    it('handles multiple permission checks', () => {
      mockedExtJsLoader.isExtJSLoaded.mockReturnValue(true);
      const checkMock = jest.fn().mockImplementation((perm) => {
        return perm === 'nexus:admin' || perm === 'nexus:browse:read';
      });
      window.NX = {
        Permissions: {
          check: checkMock,
        },
      };

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:admin" />
        </PermissionsProvider>
      );

      expect(screen.getByTestId('check')).toHaveTextContent('true');

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:browse:read" />
        </PermissionsProvider>
      );

      expect(screen.getAllByTestId('check')[1]).toHaveTextContent('true');

      render(
        <PermissionsProvider>
          <TestConsumer permission="nexus:delete" />
        </PermissionsProvider>
      );

      expect(screen.getAllByTestId('check')[2]).toHaveTextContent('false');
    });
  });
});
