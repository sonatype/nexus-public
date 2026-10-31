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

import React, { useMemo, useState } from 'react';
import { Box, Button, Flex } from '@radix-ui/themes';
import { Trash2 } from 'lucide-react';

import { SettingsFormSection, SettingsAlert } from '../../../../shared/form';
import { MetadataGrid } from '../../../../shared';
import { useAssignedUsers } from './useAssignedUsers';
import { useRoleEffectivePrivileges } from './useRoleEffectivePrivileges';
import { useCombinedRoleTree } from './useRoleTree';
import { RoleExplorerTree } from './RoleExplorerTree';
import {
  IdChipList,
  AssignedUsersSection,
  EffectivePermissionsSection,
} from './RoleDetailSections';
import { ROLE_DETAIL } from './RoleDetailStrings';
import type { Role } from './types';

export interface ReadOnlyRoleDetailProps {
  role: Role;
  canDelete?: boolean;
  onDelete?: () => void;
  error?: string;
}

// View surface when the current user lacks edit permission on this role.
// Flat sections showing Role Information, Privileges list, Contained Roles list,
// Effective Permissions, Security Tree, and Assigned Users. Widgets derive from
// the committed role snapshot (no pending buffer, no live edits).
export function ReadOnlyRoleDetail({ role, canDelete = false, onDelete, error }: ReadOnlyRoleDetailProps) {
  const assigned = useAssignedUsers(role.id);

  const roleSnapshot = useMemo(
    () => ({
      id: role.id,
      privileges: role.privileges ?? [],
      roles: role.roles ?? [],
    }),
    [role.id, role.privileges, role.roles],
  );

  const effective = useRoleEffectivePrivileges(roleSnapshot);
  const [treeSearch, setTreeSearch] = useState('');
  const treeState = useCombinedRoleTree(role.roles ?? [], { searchTerm: treeSearch });

  return (
    <Box className="role-detail">
      {error && <SettingsAlert type="error">{error}</SettingsAlert>}

      {role.readOnly && (
        <Box className="role-detail__readonly-banner">
          <SettingsAlert type="info">{ROLE_DETAIL.READ_ONLY_BANNER}</SettingsAlert>
        </Box>
      )}

      <SettingsFormSection title={ROLE_DETAIL.SECTION_INFO.TITLE} defaultOpen>
        <MetadataGrid
          items={[
            { label: ROLE_DETAIL.SECTION_INFO.ID_LABEL, value: role.id },
            { label: ROLE_DETAIL.SECTION_INFO.NAME_LABEL, value: role.name },
            {
              label: ROLE_DETAIL.SECTION_INFO.DESCRIPTION_LABEL,
              value: role.description || ROLE_DETAIL.SECTION_INFO.DESCRIPTION_EMPTY,
            },
          ]}
        />
      </SettingsFormSection>

      <SettingsFormSection title={ROLE_DETAIL.SECTION_PRIVILEGES.TITLE}>
        <IdChipList
          ids={role.privileges}
          emptyLabel={ROLE_DETAIL.SECTION_PRIVILEGES.EMPTY}
        />
      </SettingsFormSection>

      <SettingsFormSection title={ROLE_DETAIL.SECTION_CONTAINED_ROLES.TITLE}>
        <IdChipList
          ids={role.roles}
          emptyLabel={ROLE_DETAIL.SECTION_CONTAINED_ROLES.EMPTY}
        />
      </SettingsFormSection>

      <SettingsFormSection title={ROLE_DETAIL.SECTION_EFFECTIVE.TITLE}>
        <EffectivePermissionsSection
          privileges={effective.privileges}
          roleMap={effective.roleMap}
          loading={effective.loading}
          error={effective.error}
        />
      </SettingsFormSection>

      <SettingsFormSection title={ROLE_DETAIL.SECTION_SECURITY_TREE.TITLE}>
        <RoleExplorerTree
          tree={treeState.tree}
          loading={treeState.loading}
          onToggleExpand={treeState.toggleExpand}
          onExpandAll={treeState.expandAll}
          onCollapseAll={treeState.collapseAll}
          onSearchChange={setTreeSearch}
        />
      </SettingsFormSection>

      <SettingsFormSection title={ROLE_DETAIL.SECTION_ASSIGNED_USERS.TITLE}>
        <AssignedUsersSection
          users={assigned.users}
          loading={assigned.loading}
          error={assigned.error}
        />
      </SettingsFormSection>

      {canDelete && onDelete && !role.readOnly && (
        <Flex mt="4" className="role-detail__readonly-actions">
          <Button
            type="button"
            variant="soft"
            color="red"
            onClick={onDelete}
            data-testid="role-detail-delete"
            data-analytics-id="nxrm-role-delete"
          >
            <Trash2 size={16} />
            {ROLE_DETAIL.BUTTON_DELETE}
          </Button>
        </Flex>
      )}
    </Box>
  );
}

export default ReadOnlyRoleDetail;
