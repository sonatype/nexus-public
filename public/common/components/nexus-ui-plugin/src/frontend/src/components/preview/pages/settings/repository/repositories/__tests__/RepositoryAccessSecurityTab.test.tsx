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
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';
import { RepositoryAccessSecurityTab } from '../RepositoryAccessSecurityTab';
import ExtJS from '../../../../../../../interface/ExtJS';

jest.mock('../../../../../../../interface/ExtJS');

jest.mock('../useRepositoryAccessSecurity', () => ({
  useRepositoryAccessSecurity: jest.fn(),
}));

const mockUseRepositoryAccessSecurity =
  require('../useRepositoryAccessSecurity').useRepositoryAccessSecurity;

const mockExtJS = ExtJS as unknown as { checkPermission: jest.Mock };

const REPO_NAME = 'my-repo';
const REPO_FORMAT = 'maven2';

function renderTab(repositoryName = REPO_NAME, repositoryFormat: string | undefined = REPO_FORMAT) {
  return render(
    <Theme>
      <RepositoryAccessSecurityTab
        repositoryName={repositoryName}
        repositoryFormat={repositoryFormat}
      />
    </Theme>
  );
}

const mockPrivilege = {
  name: 'nx-repository-view-maven2-my-repo-read',
  description: 'Read the my-repo maven2 repository',
  type: 'repository-view',
  repository: REPO_NAME,
  format: REPO_FORMAT,
  actions: ['read'],
};

const mockWildcardPrivilege = {
  name: 'nx-repository-view-maven2-*-all',
  type: 'repository-view',
  repository: '*',
  format: REPO_FORMAT,
  actions: ['*'],
};

const mockRole = {
  id: 'nx-anonymous',
  name: 'nx-anonymous',
  description: 'Anonymous access role',
  privileges: ['nx-repository-view-maven2-my-repo-read'],
  roles: [],
};

const mockUser = {
  userId: 'jdoe',
  firstName: 'Jane',
  lastName: 'Doe',
  roles: ['nx-anonymous'],
};

const mockAnonymousEnabled = {
  enabled: true,
  userId: 'anonymous',
  realmName: 'NexusAuthorizingRealm',
};

const mockAnonymousDisabled = {
  enabled: false,
};

function makeHookReturn(overrides: Record<string, unknown> = {}) {
  return {
    privileges: [],
    roles: [],
    users: [],
    anonymousAccess: null,
    loading: false,
    error: null,
    refetch: jest.fn(),
    ...overrides,
  };
}

describe('RepositoryAccessSecurityTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockExtJS.checkPermission = jest.fn(() => true);
  });

  describe('loading state', () => {
    it('shouldRenderSpinnerWhenLoading', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(makeHookReturn({ loading: true }));

      renderTab();

      expect(screen.getByTestId('access-security-loading')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('shouldRenderErrorCardWhenErrorOccurs', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(makeHookReturn({ error: 'Fetch failed' }));

      renderTab();

      expect(screen.getByText('Failed to load access and security data.')).toBeInTheDocument();
      expect(screen.getByText('Fetch failed')).toBeInTheDocument();
    });

    it('shouldInvokeRefetchWhenRetryClicked', () => {
      const refetch = jest.fn();
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ error: 'Fetch failed', refetch })
      );

      renderTab();

      fireEvent.click(screen.getByRole('button', { name: /Retry/ }));
      expect(refetch).toHaveBeenCalledTimes(1);
    });
  });

  describe('successful data rendering', () => {
    it('shouldRenderPrivilegesSectionWhenPrivilegesAreReturned', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ privileges: [mockPrivilege] })
      );

      renderTab();

      expect(screen.getByText('Repository Privileges')).toBeInTheDocument();
      expect(screen.getByText(mockPrivilege.name)).toBeInTheDocument();
      expect(screen.getByText(`(${mockPrivilege.description})`)).toBeInTheDocument();
    });

    it('shouldRenderAllActionsBadgeInGreenForWildcardPrivileges', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ privileges: [mockWildcardPrivilege] })
      );

      renderTab();

      expect(screen.getByText('*')).toBeInTheDocument();
    });

    it('shouldRenderRolesTableWhenRolesAreReturned', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ roles: [mockRole] })
      );

      renderTab();

      expect(screen.getByText('Roles with Access')).toBeInTheDocument();
      expect(screen.getByText(mockRole.name)).toBeInTheDocument();
      expect(screen.getByText('Anonymous access role')).toBeInTheDocument();
      expect(screen.getByText('1 privilege')).toBeInTheDocument();
    });

    it('shouldRenderUsersTableWhenUsersAreReturned', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ users: [mockUser] })
      );

      renderTab();

      expect(screen.getByText('Users with Access')).toBeInTheDocument();
      expect(screen.getByText('jdoe')).toBeInTheDocument();
      expect(screen.getByText(/Jane Doe/)).toBeInTheDocument();
      expect(screen.getByText('nx-anonymous')).toBeInTheDocument();
    });

    it('shouldRenderEnabledAnonymousDetailsWhenEnabled', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ anonymousAccess: mockAnonymousEnabled })
      );

      renderTab();

      expect(screen.getByText('Anonymous Access')).toBeInTheDocument();
      expect(screen.getByText('Enabled')).toBeInTheDocument();
      expect(screen.getByText('anonymous')).toBeInTheDocument();
      expect(screen.getByText('NexusAuthorizingRealm')).toBeInTheDocument();
    });

    it('shouldRenderDisabledMessageWhenAnonymousDisabled', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ anonymousAccess: mockAnonymousDisabled })
      );

      renderTab();

      expect(screen.getByText('Disabled')).toBeInTheDocument();
      expect(
        screen.getByText('Anonymous users cannot access this repository.')
      ).toBeInTheDocument();
    });

    it('shouldRenderNotAvailableMessageWhenAnonymousResponseIsNull', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(makeHookReturn({ anonymousAccess: null }));

      renderTab();

      expect(
        screen.getByText('Anonymous access settings are not available on this instance.')
      ).toBeInTheDocument();
    });

    it('shouldRenderEmptyStateForZeroPrivileges', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(makeHookReturn());

      renderTab();

      expect(screen.getByText('No privileges target this repository.')).toBeInTheDocument();
    });

    it('shouldRenderEmptyStateForZeroRoles', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(makeHookReturn());

      renderTab();

      expect(
        screen.getByText('No roles reference the privileges for this repository.')
      ).toBeInTheDocument();
    });

    it('shouldRenderEmptyStateForZeroUsers', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(makeHookReturn());

      renderTab();

      expect(
        screen.getByText('No users have access via the roles for this repository.')
      ).toBeInTheDocument();
    });

    it('shouldSingularizePrivilegesCountLabel', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ privileges: [mockPrivilege] })
      );

      renderTab();

      expect(screen.getByText('1 privilege')).toBeInTheDocument();
    });

    it('shouldPluralizePrivilegesCountLabel', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ privileges: [mockPrivilege, { ...mockPrivilege, name: 'other-priv' }] })
      );

      renderTab();

      expect(screen.getByText('2 privileges')).toBeInTheDocument();
    });
  });

  describe('em-dash fallback rendering', () => {
    it('shouldRenderEmDashWhenUserHasNoRoles', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ users: [{ userId: 'lonely-user', roles: undefined }] })
      );

      renderTab();

      expect(screen.getByText('lonely-user')).toBeInTheDocument();
      expect(screen.getByText('—')).toBeInTheDocument();
    });

    it('shouldRenderEmDashWhenUserHasEmptyRolesArray', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({ users: [{ userId: 'roleless-user', roles: [] }] })
      );

      renderTab();

      expect(screen.getByText('roleless-user')).toBeInTheDocument();
      expect(screen.getByText('—')).toBeInTheDocument();
    });

    it('shouldRenderEmDashWhenAnonymousEnabledWithoutUserId', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({
          anonymousAccess: { enabled: true, userId: undefined, realmName: 'NexusAuthorizingRealm' },
        })
      );

      renderTab();

      expect(screen.getByText('Enabled')).toBeInTheDocument();
      expect(screen.getByText('—')).toBeInTheDocument();
    });

    it('shouldRenderEmDashWhenAnonymousEnabledWithoutRealm', () => {
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({
          anonymousAccess: { enabled: true, userId: 'anonymous', realm: undefined, realmName: undefined },
        })
      );

      renderTab();

      expect(screen.getByText('anonymous')).toBeInTheDocument();
      expect(screen.getByText('—')).toBeInTheDocument();
    });
  });

  describe('users truncation', () => {
    it('shouldTruncateUsersAtDisplayLimitAndShowExtraCount', () => {
      const many = Array.from({ length: 13 }, (_, i) => ({
        userId: `user-${i}`,
        roles: ['nx-anonymous'],
      }));
      mockUseRepositoryAccessSecurity.mockReturnValue(makeHookReturn({ users: many }));

      renderTab();

      expect(screen.getByText('user-0')).toBeInTheDocument();
      expect(screen.getByText('user-9')).toBeInTheDocument();
      expect(screen.queryByText('user-10')).not.toBeInTheDocument();
      expect(screen.getByText(/and 3 more users/)).toBeInTheDocument();
    });
  });

  describe('permission gating (per-section)', () => {
    it('shouldHidePrivilegesSectionWhenUserLacksPrivilegesReadPermission', () => {
      mockExtJS.checkPermission = jest.fn(
        (perm: string) => perm !== 'nexus:privileges:read'
      );
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({
          privileges: [mockPrivilege],
          roles: [mockRole],
          users: [mockUser],
          anonymousAccess: mockAnonymousEnabled,
        })
      );

      renderTab();

      expect(screen.queryByText('Repository Privileges')).not.toBeInTheDocument();
      expect(screen.getByText('Roles with Access')).toBeInTheDocument();
      expect(screen.getByText('Users with Access')).toBeInTheDocument();
      expect(screen.getByText('Anonymous Access')).toBeInTheDocument();
    });

    it('shouldHideRolesSectionWhenUserLacksRolesReadPermission', () => {
      mockExtJS.checkPermission = jest.fn(
        (perm: string) => perm !== 'nexus:roles:read'
      );
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({
          privileges: [mockPrivilege],
          roles: [mockRole],
          users: [mockUser],
          anonymousAccess: mockAnonymousEnabled,
        })
      );

      renderTab();

      expect(screen.getByText('Repository Privileges')).toBeInTheDocument();
      expect(screen.queryByText('Roles with Access')).not.toBeInTheDocument();
    });

    it('shouldHideUsersSectionWhenUserLacksUsersReadPermission', () => {
      mockExtJS.checkPermission = jest.fn(
        (perm: string) => perm !== 'nexus:users:read'
      );
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({
          privileges: [mockPrivilege],
          roles: [mockRole],
          users: [mockUser],
          anonymousAccess: mockAnonymousEnabled,
        })
      );

      renderTab();

      expect(screen.queryByText('Users with Access')).not.toBeInTheDocument();
    });

    it('shouldHideAnonymousSectionWhenUserLacksSettingsReadPermission', () => {
      mockExtJS.checkPermission = jest.fn(
        (perm: string) => perm !== 'nexus:settings:read'
      );
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({
          privileges: [mockPrivilege],
          roles: [mockRole],
          users: [mockUser],
          anonymousAccess: mockAnonymousEnabled,
        })
      );

      renderTab();

      expect(screen.queryByText('Anonymous Access')).not.toBeInTheDocument();
    });

    it('shouldRenderNoSectionsWhenUserLacksAllFourPermissions', () => {
      mockExtJS.checkPermission = jest.fn(() => false);
      mockUseRepositoryAccessSecurity.mockReturnValue(
        makeHookReturn({
          privileges: [mockPrivilege],
          roles: [mockRole],
          users: [mockUser],
          anonymousAccess: mockAnonymousEnabled,
        })
      );

      renderTab();

      expect(screen.queryByText('Repository Privileges')).not.toBeInTheDocument();
      expect(screen.queryByText('Roles with Access')).not.toBeInTheDocument();
      expect(screen.queryByText('Users with Access')).not.toBeInTheDocument();
      expect(screen.queryByText('Anonymous Access')).not.toBeInTheDocument();
    });
  });

  describe('hook integration', () => {
    it('shouldPassRepositoryNameFormatAndPermissionFlagsToHook', () => {
      mockExtJS.checkPermission = jest.fn(
        (perm: string) => perm === 'nexus:privileges:read'
      );
      mockUseRepositoryAccessSecurity.mockReturnValue(makeHookReturn());

      renderTab('some-other-repo', 'npm');

      expect(mockUseRepositoryAccessSecurity).toHaveBeenCalledWith('some-other-repo', {
        canReadPrivileges: true,
        canReadRoles: false,
        canReadUsers: false,
        canReadAnonymous: false,
        repositoryFormat: 'npm',
      });
    });
  });
});
