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
import { render, screen } from '@testing-library/react';
import { PermissionsProvider, usePermissions } from '../PermissionsContext';
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
