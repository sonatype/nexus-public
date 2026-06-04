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
import { Theme } from '@radix-ui/themes';

import type { MergedApiEndpoint } from '../../utils/mergeSwaggerPermissions';
import { WhoHasAccessTab } from '../WhoHasAccessTab';
import { useEndpointAccess } from '../../hooks/useEndpointAccess';

jest.mock('../../hooks/useEndpointAccess');

const mockedUseEndpointAccess = useEndpointAccess as jest.MockedFunction<typeof useEndpointAccess>;

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

function baseRow(overrides: Partial<MergedApiEndpoint> = {}): MergedApiEndpoint {
  return {
    httpMethod: 'GET',
    swaggerPathKey: '/v1/foo',
    fullPath: '/service/rest/v1/foo',
    tag: 'Test',
    permission: {
      httpMethod: 'GET',
      pathPattern: '/service/rest/v1/foo',
      permissions: [{ permission: 'nexus:test:read', logical: 'AND' }],
      description: null,
      tag: null,
      authenticated: true,
    },
    ...overrides,
  };
}

const idleHook = {
  loading: false,
  error: null,
  qualifyingRoles: [],
  usersWithAccess: [],
  noMappedPermissions: false,
  refetch: jest.fn(),
};

describe('WhoHasAccessTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseEndpointAccess.mockReturnValue(idleHook);
  });

  it('shows anonymous banner when endpoint is not authenticated', () => {
    mockedUseEndpointAccess.mockReturnValue({
      ...idleHook,
      noMappedPermissions: true,
    });
    renderWithTheme(
      <WhoHasAccessTab
        active
        row={baseRow({
          permission: {
            httpMethod: 'GET',
            pathPattern: '/service/rest/v1/foo',
            permissions: [],
            description: null,
            tag: null,
            authenticated: false,
          },
        })}
      />
    );
    expect(screen.getByText(/allows anonymous access/i)).toBeInTheDocument();
  });

  it('shows unmapped permissions message from hook', () => {
    mockedUseEndpointAccess.mockReturnValue({
      ...idleHook,
      noMappedPermissions: true,
    });
    renderWithTheme(<WhoHasAccessTab active row={baseRow()} />);
    expect(screen.getByText(/No permission strings are mapped/i)).toBeInTheDocument();
  });

  it('renders role name in granting roles column', () => {
    mockedUseEndpointAccess.mockReturnValue({
      ...idleHook,
      qualifyingRoles: [
        {
          role: {
            id: 'role-a',
            version: '1',
            source: 'default',
            name: 'Auditors',
            description: '',
            readOnly: false,
            privileges: [],
            roles: [],
          },
          userCount: 1,
        },
      ],
      usersWithAccess: [
        {
          user: {
            userId: 'alice',
            realm: 'default',
            firstName: '',
            lastName: '',
            emailAddress: '',
            source: 'default',
            status: 'active',
            readOnly: false,
            roles: ['role-a'],
          },
          grantingRoleIds: ['role-a'],
        },
      ],
    });
    renderWithTheme(<WhoHasAccessTab active row={baseRow()} />);
    const auditorsLinks = screen.getAllByRole('link', { name: 'Auditors' });
    expect(auditorsLinks.length).toBeGreaterThanOrEqual(1);
    for (const link of auditorsLinks) {
      expect(link).toHaveAttribute('href', expect.stringContaining('role-a'));
    }
  });
});
