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

import { GrantAccessTab } from '../GrantAccessTab';
import type { MergedApiEndpoint } from '../../utils/mergeSwaggerPermissions';

jest.mock('../../../../../../../../interface/ExtJS', () => {
  const mockExtJS = {
    checkPermission: jest.fn().mockReturnValue(false),
  };
  return {
    __esModule: true,
    default: mockExtJS,
    ExtJS: mockExtJS,
  };
});

jest.mock('../../grant/GrantWizard', () => ({
  GrantWizard: ({ active }: any) => <div data-testid="grant-wizard" data-active={String(active)}>GrantWizard</div>,
}));

const { ExtJS } = jest.requireMock('../../../../../../../../interface/ExtJS');
const mockCheckPermission = ExtJS.checkPermission as jest.MockedFunction<(perm: string) => boolean>;

const PERM_ROLES_READ = 'nexus:roles:read';
const PERM_USERS_READ = 'nexus:users:read';
const PERM_PRIVILEGES_READ = 'nexus:privileges:read';
const PERM_ROLES_UPDATE = 'nexus:roles:update';
const PERM_USERS_UPDATE = 'nexus:users:update';
const ALL_GRANT_PERMS = [
  PERM_ROLES_READ,
  PERM_USERS_READ,
  PERM_PRIVILEGES_READ,
  PERM_ROLES_UPDATE,
  PERM_USERS_UPDATE,
];
const FALLBACK_COPY = /You need additional permissions/;

const MOCK_ROW: MergedApiEndpoint = {
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
    authenticated: true,
  },
};

function grantPermissions(granted: string[]): void {
  mockCheckPermission.mockImplementation((perm: string) => granted.includes(perm));
}

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('GrantAccessTab', () => {
  beforeEach(() => {
    mockCheckPermission.mockReset();
    mockCheckPermission.mockReturnValue(false);
  });

  it('renders the additional-permissions fallback when the user cannot grant access', () => {
    grantPermissions([]);
    renderWithTheme(<GrantAccessTab row={MOCK_ROW} active={true} />);
    expect(screen.getByText(FALLBACK_COPY)).toBeInTheDocument();
    expect(screen.queryByTestId('grant-wizard')).not.toBeInTheDocument();
  });

  it('renders the GrantWizard when the user has all required grant permissions', () => {
    grantPermissions(ALL_GRANT_PERMS);
    renderWithTheme(<GrantAccessTab row={MOCK_ROW} active={true} />);
    expect(screen.getByTestId('grant-wizard')).toBeInTheDocument();
    expect(screen.queryByText(FALLBACK_COPY)).not.toBeInTheDocument();
  });

  it('passes active=true through to the GrantWizard', () => {
    grantPermissions(ALL_GRANT_PERMS);
    renderWithTheme(<GrantAccessTab row={MOCK_ROW} active={true} />);
    expect(screen.getByTestId('grant-wizard')).toHaveAttribute('data-active', 'true');
  });

  it('passes active=false through to the GrantWizard', () => {
    grantPermissions(ALL_GRANT_PERMS);
    renderWithTheme(<GrantAccessTab row={MOCK_ROW} active={false} />);
    expect(screen.getByTestId('grant-wizard')).toHaveAttribute('data-active', 'false');
  });
});
