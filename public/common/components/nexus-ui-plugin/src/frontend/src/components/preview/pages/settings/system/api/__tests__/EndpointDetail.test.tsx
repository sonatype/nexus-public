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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { EndpointDetail } from '../EndpointDetail';
import type { MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';

jest.mock('../../../../../../../interface/ExtJS', () => {
  const mockExtJS = {
    checkPermission: jest.fn().mockReturnValue(true),
    // usePermission delegates to the getter so tests drive behavior via checkPermission (NEXUS-54212 pattern).
    usePermission: jest.fn((getValue: () => boolean) => getValue()),
    useUser: jest.fn(() => ({ id: 'admin' })),
  };
  return {
    __esModule: true,
    default: mockExtJS,
    ExtJS: mockExtJS,
  };
});

const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');
const mockCheckPermission = ExtJS.checkPermission as jest.MockedFunction<(perm: string) => boolean>;

jest.mock('../tabs/TryItTab', () => ({
  TryItTab: ({ accessDenied }: any) => (
    <div data-testid="try-it-tab" data-denied={String(accessDenied)}>TryItTab</div>
  ),
}));

jest.mock('../tabs/WhoHasAccessTab', () => ({
  WhoHasAccessTab: ({ active }: any) => (
    <div data-testid="who-has-access-tab" data-active={String(active)}>WhoHasAccessTab</div>
  ),
}));

jest.mock('../tabs/GrantAccessTab', () => ({
  GrantAccessTab: ({ active }: any) => (
    <div data-testid="grant-access-tab" data-active={String(active)}>GrantAccessTab</div>
  ),
}));

function makeRow(overrides: Partial<MergedApiEndpoint> = {}): MergedApiEndpoint {
  return {
    httpMethod: 'GET',
    swaggerPathKey: '/v1/status',
    fullPath: '/service/rest/v1/status',
    tag: 'Status',
    summary: 'Check application health',
    permission: {
      httpMethod: 'GET',
      pathPattern: '/service/rest/v1/status',
      permissions: [],
      description: null,
      tag: null,
      authenticated: false,
    },
    ...overrides,
  };
}

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('EndpointDetail', () => {
  beforeEach(() => {
    // Default: admin (all permissions granted). Individual tests override for permission-gating cases.
    mockCheckPermission.mockReset();
    mockCheckPermission.mockReturnValue(true);
  });

  describe('Empty State', () => {
    it('renders placeholder when no row is selected', () => {
      renderWithTheme(<EndpointDetail row={null} fullSwagger={null} access="unknown" />);
      expect(screen.getByText('Choose an endpoint')).toBeInTheDocument();
      expect(screen.getByText(/Click any operation/)).toBeInTheDocument();
    });

    it('does not render detail panel when row is null', () => {
      renderWithTheme(<EndpointDetail row={null} fullSwagger={null} access="unknown" />);
      expect(screen.queryByTestId('api-endpoint-detail')).not.toBeInTheDocument();
    });
  });

  describe('With Selected Endpoint', () => {
    const row = makeRow();

    it('renders the detail panel', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByTestId('api-endpoint-detail')).toBeInTheDocument();
    });

    it('displays HTTP method and path', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText('GET')).toBeInTheDocument();
      expect(screen.getByText('/service/rest/v1/status')).toBeInTheDocument();
    });

    it('displays the summary', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText('Check application health')).toBeInTheDocument();
    });

    it('shows "None (may allow anonymous)" for unauthenticated endpoints without permissions', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText(/None \(may allow anonymous\)/)).toBeInTheDocument();
    });

    it('shows "None mapped" for authenticated endpoints without permissions', () => {
      const authRow = makeRow({
        permission: {
          httpMethod: 'GET',
          pathPattern: '/service/rest/v1/status',
          permissions: [],
          description: null,
          tag: null,
          authenticated: true,
        },
      });
      renderWithTheme(<EndpointDetail row={authRow} fullSwagger={null} access="granted" />);
      expect(screen.getByText(/None mapped/)).toBeInTheDocument();
    });

    it('displays permission strings joined by AND', () => {
      const row = makeRow({
        permission: {
          httpMethod: 'GET',
          pathPattern: '/service/rest/v1/repositories',
          permissions: [
            { permission: 'nexus:repos:read', logical: 'AND' },
            { permission: 'nexus:repos:browse', logical: 'AND' },
          ],
          description: null,
          tag: null,
          authenticated: true,
        },
      });
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText(/nexus:repos:read AND nexus:repos:browse/)).toBeInTheDocument();
    });

    it('displays permission strings joined by OR', () => {
      const row = makeRow({
        permission: {
          httpMethod: 'GET',
          pathPattern: '/service/rest/v1/repositories',
          permissions: [
            { permission: 'nexus:a:read', logical: 'OR' },
            { permission: 'nexus:b:read', logical: 'OR' },
          ],
          description: null,
          tag: null,
          authenticated: true,
        },
      });
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByText(/nexus:a:read OR nexus:b:read/)).toBeInTheDocument();
    });
  });

  describe('Tabs', () => {
    const row = makeRow();

    it('renders the Try It tab by default', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByTestId('try-it-tab')).toBeInTheDocument();
    });

    it('passes accessDenied=false to TryItTab when access is granted', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByTestId('try-it-tab')).toHaveAttribute('data-denied', 'false');
    });

    it('passes accessDenied=true to TryItTab when access is denied', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="denied" />);
      expect(screen.getByTestId('try-it-tab')).toHaveAttribute('data-denied', 'true');
    });

    it('switches to Who Has Access tab on click', async () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      await userEvent.click(screen.getByRole('tab', { name: /Who Has Access/ }));
      expect(screen.getByTestId('who-has-access-tab')).toBeInTheDocument();
    });

    it('switches to Grant Access tab on click', async () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      await userEvent.click(screen.getByRole('tab', { name: /Grant Access/ }));
      expect(screen.getByTestId('grant-access-tab')).toBeInTheDocument();
    });

    it('renders three tab triggers', () => {
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByRole('tab', { name: /Try It/ })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Who Has Access/ })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Grant Access/ })).toBeInTheDocument();
    });
  });

  describe('Permission Gating (NEXUS-54331)', () => {
    const row = makeRow();
    const PERM_ROLES_READ = 'nexus:roles:read';
    const PERM_USERS_READ = 'nexus:users:read';
    const PERM_PRIVILEGES_READ = 'nexus:privileges:read';
    const PERM_ROLES_UPDATE = 'nexus:roles:update';
    const PERM_USERS_UPDATE = 'nexus:users:update';
    const ALL_READS = [PERM_ROLES_READ, PERM_USERS_READ, PERM_PRIVILEGES_READ];
    const ALL_GRANT_PERMS = [...ALL_READS, PERM_ROLES_UPDATE, PERM_USERS_UPDATE];

    function grantPermissions(granted: string[]): void {
      mockCheckPermission.mockImplementation((perm: string) => granted.includes(perm));
    }

    it('shows only Try It when the user has no security-related permissions', () => {
      grantPermissions([]);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByRole('tab', { name: /Try It/ })).toBeInTheDocument();
      expect(screen.queryByRole('tab', { name: /Who Has Access/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('tab', { name: /Grant Access/ })).not.toBeInTheDocument();
    });

    it('hides Who Has Access when only nexus:users:read is granted (would 403 on roles or privileges fetch)', () => {
      grantPermissions([PERM_USERS_READ]);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.queryByRole('tab', { name: /Who Has Access/ })).not.toBeInTheDocument();
    });

    it('hides Who Has Access when only nexus:roles:read is granted', () => {
      grantPermissions([PERM_ROLES_READ]);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.queryByRole('tab', { name: /Who Has Access/ })).not.toBeInTheDocument();
    });

    it('hides Who Has Access when only nexus:privileges:read is granted', () => {
      grantPermissions([PERM_PRIVILEGES_READ]);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.queryByRole('tab', { name: /Who Has Access/ })).not.toBeInTheDocument();
    });

    it('hides Who Has Access when two of three security reads are granted', () => {
      grantPermissions([PERM_ROLES_READ, PERM_USERS_READ]);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.queryByRole('tab', { name: /Who Has Access/ })).not.toBeInTheDocument();
    });

    it('shows Who Has Access when all three security reads are granted', () => {
      grantPermissions(ALL_READS);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByRole('tab', { name: /Who Has Access/ })).toBeInTheDocument();
    });

    it('hides Grant Access when the user has all three reads but no write permissions', () => {
      grantPermissions(ALL_READS);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByRole('tab', { name: /Who Has Access/ })).toBeInTheDocument();
      expect(screen.queryByRole('tab', { name: /Grant Access/ })).not.toBeInTheDocument();
    });

    it('hides Grant Access when the user has writes but is missing a required read', () => {
      grantPermissions([PERM_ROLES_READ, PERM_USERS_READ, PERM_ROLES_UPDATE, PERM_USERS_UPDATE]);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.queryByRole('tab', { name: /Grant Access/ })).not.toBeInTheDocument();
    });

    it('shows Grant Access when the user has all three reads plus roles:update and users:update', () => {
      grantPermissions(ALL_GRANT_PERMS);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.getByRole('tab', { name: /Grant Access/ })).toBeInTheDocument();
    });

    it('does not render the Who Has Access tab body when the trigger is hidden', () => {
      grantPermissions([]);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.queryByTestId('who-has-access-tab')).not.toBeInTheDocument();
    });

    it('does not render the Grant Access tab body when the trigger is hidden', () => {
      grantPermissions([]);
      renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      expect(screen.queryByTestId('grant-access-tab')).not.toBeInTheDocument();
    });

    it('resets to Try It when the active tab loses visibility on re-render', async () => {
      mockCheckPermission.mockReturnValue(true);
      const { rerender } = renderWithTheme(<EndpointDetail row={row} fullSwagger={null} access="granted" />);
      await userEvent.click(screen.getByRole('tab', { name: /Who Has Access/ }));
      expect(screen.getByTestId('who-has-access-tab')).toBeInTheDocument();

      grantPermissions([]);
      rerender(
        <Theme>
          <EndpointDetail row={row} fullSwagger={null} access="granted" />
        </Theme>
      );

      expect(screen.queryByTestId('who-has-access-tab')).not.toBeInTheDocument();
      expect(screen.getByTestId('try-it-tab')).toBeInTheDocument();
    });
  });
});
