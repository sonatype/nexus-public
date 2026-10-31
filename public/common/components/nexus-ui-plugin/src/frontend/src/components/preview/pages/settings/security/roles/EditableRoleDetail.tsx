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

import React, { useCallback, useMemo, useState } from 'react';
import { Box, Button, Flex, Text } from '@radix-ui/themes';
import { Trash2, Pencil } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsAlert,
  SettingsTextInput,
  SettingsTextArea,
  SettingsButton,
  SettingsSelectionModal,
  useSettingsSelectionModal,
} from '../../../../shared/form';
import type { SettingsSelectionItem } from '../../../../shared/form/settingsSelectionModalMachine';
import { useRolesApi } from './useRolesApi';
import { useRoleDetailEdit } from './useRoleDetailEdit';
import { useRoleReferenceData } from './useRoleReferenceData';
import { useRoleEffectivePrivileges } from './useRoleEffectivePrivileges';
import { useAssignedUsers } from './useAssignedUsers';
import { useCombinedRoleTree } from './useRoleTree';
import { RoleExplorerTree } from './RoleExplorerTree';
import {
  EffectivePermissionsSection,
  AssignedUsersSection,
} from './RoleDetailSections';
import { ROLE_DETAIL } from './RoleDetailStrings';
import type { Role, RoleFormData } from './types';

export interface EditableRoleDetailProps {
  role: Role;
  canDelete: boolean;
  onDelete: () => void;
  onComplete?: () => void;
  error?: string;
}

// Inline-editable Role page surface. Owns a pending buffer via useRoleDetailEdit;
// Effective Permissions and Security Tree derive live from `pending` so local
// edits recompute widgets before Save. Save / Cancel / Delete live in the
// SettingsForm chrome; picker modals target Privileges + Contained Roles.
export function EditableRoleDetail({
  role,
  canDelete,
  onDelete,
  onComplete,
  error,
}: EditableRoleDetailProps) {
  const { updateRole } = useRolesApi();

  const handleSaved = useCallback(
    (_updated: Role) => {
      onComplete?.();
    },
    [onComplete],
  );

  const editSurface = useRoleDetailEdit({
    role,
    save: (pending: RoleFormData) => updateRole(pending),
    onSaved: handleSaved,
  });

  const {
    pending,
    isDirty,
    isSaving,
    saveError,
    changeName,
    changeDescription,
    applyPrivileges,
    applyRoles,
    save,
    cancel,
  } = editSurface;

  const referenceData = useRoleReferenceData(role.id);

  const privilegeItems: SettingsSelectionItem[] = useMemo(
    () =>
      referenceData.allPrivileges.map((p) => ({
        id: p.id,
        name: p.name,
        description: p.description,
      })),
    [referenceData.allPrivileges],
  );
  const roleItems: SettingsSelectionItem[] = useMemo(
    () => referenceData.allRoles.map((r) => ({ id: r.id, name: r.name })),
    [referenceData.allRoles],
  );

  const privilegesPicker = useSettingsSelectionModal({
    availableItems: privilegeItems,
    onConfirm: applyPrivileges,
  });
  const rolesPicker = useSettingsSelectionModal({
    availableItems: roleItems,
    onConfirm: applyRoles,
  });

  const openPrivilegesPicker = useCallback(
    () => privilegesPicker.open(pending.privileges ?? []),
    [privilegesPicker, pending.privileges],
  );
  const openRolesPicker = useCallback(
    () => rolesPicker.open(pending.roles ?? []),
    [rolesPicker, pending.roles],
  );

  // Effective permissions + security tree derive from the pending buffer so they
  // recompute live from local edits before Save (per AC4).
  const pendingSnapshot = useMemo(
    () => ({
      id: role.id,
      privileges: pending.privileges,
      roles: pending.roles,
    }),
    [role.id, pending.privileges, pending.roles],
  );

  const effective = useRoleEffectivePrivileges(pendingSnapshot);
  const [treeSearch, setTreeSearch] = useState('');
  const treeState = useCombinedRoleTree(pending.roles, { searchTerm: treeSearch });
  const assigned = useAssignedUsers(role.id);

  const isReadOnly = role.readOnly === true;
  const showDeleteButton = canDelete && !isReadOnly;

  return (
    <Box className="role-detail">
      {isReadOnly && (
        <Box className="role-detail__readonly-banner">
          <SettingsAlert type="info">{ROLE_DETAIL.READ_ONLY_BANNER}</SettingsAlert>
        </Box>
      )}

      <SettingsForm
        testId="role-detail-form"
        showHeader={false}
        onSubmit={save}
        onCancel={cancel}
        saving={isSaving}
        pristine={!isDirty}
        submitDisabled={isReadOnly}
        cancelDisabled={!isDirty}
        submitLabel={isSaving ? ROLE_DETAIL.BUTTON_SAVING : ROLE_DETAIL.BUTTON_SAVE}
        cancelLabel={ROLE_DETAIL.BUTTON_CANCEL}
        confirmDiscard={false}
        error={error || saveError || undefined}
        footerExtra={
          showDeleteButton ? (
            <Button
              type="button"
              variant="soft"
              color="red"
              onClick={onDelete}
              disabled={isSaving}
              data-testid="role-detail-delete"
              data-analytics-id="nxrm-role-delete"
            >
              <Trash2 size={16} />
              {ROLE_DETAIL.BUTTON_DELETE}
            </Button>
          ) : undefined
        }
      >
        <SettingsFormSection title={ROLE_DETAIL.SECTION_INFO.TITLE} defaultOpen>
          <Flex align="center" gap="2" data-testid="role-detail-id">
            <Text size="2" weight="medium">
              {ROLE_DETAIL.SECTION_INFO.ID_LABEL}
            </Text>
            <Text size="2" color="gray">
              {role.id}
            </Text>
          </Flex>
          <SettingsTextInput
            name="name"
            value={pending.name ?? ''}
            onChange={changeName}
            label={ROLE_DETAIL.SECTION_INFO.NAME_LABEL}
            placeholder={ROLE_DETAIL.SECTION_INFO.NAME_PLACEHOLDER}
            required
            disabled={isReadOnly}
          />
          <SettingsTextArea
            name="description"
            value={pending.description ?? ''}
            onChange={changeDescription}
            label={ROLE_DETAIL.SECTION_INFO.DESCRIPTION_LABEL}
            placeholder={ROLE_DETAIL.SECTION_INFO.DESCRIPTION_PLACEHOLDER}
            rows={3}
            disabled={isReadOnly}
          />
        </SettingsFormSection>

        <SettingsFormSection title={ROLE_DETAIL.SECTION_PRIVILEGES.TITLE} defaultOpen>
          <Flex align="center" gap="3" className="role-detail__picker-row">
            <Text size="2" data-testid="role-detail-privileges-count">
              {ROLE_DETAIL.SECTION_PRIVILEGES.COUNT(pending.privileges.length)}
            </Text>
            <SettingsButton
              variant="secondary"
              onClick={openPrivilegesPicker}
              disabled={isReadOnly}
              icon={Pencil}
              testId="role-detail-open-privileges"
            >
              {ROLE_DETAIL.BUTTON_EDIT_PRIVILEGES}
            </SettingsButton>
          </Flex>
        </SettingsFormSection>

        <SettingsFormSection title={ROLE_DETAIL.SECTION_CONTAINED_ROLES.TITLE} defaultOpen>
          <Flex align="center" gap="3" className="role-detail__picker-row">
            <Text size="2" data-testid="role-detail-roles-count">
              {ROLE_DETAIL.SECTION_CONTAINED_ROLES.COUNT(pending.roles.length)}
            </Text>
            <SettingsButton
              variant="secondary"
              onClick={openRolesPicker}
              disabled={isReadOnly}
              icon={Pencil}
              testId="role-detail-open-contained-roles"
            >
              {ROLE_DETAIL.BUTTON_EDIT_CONTAINED_ROLES}
            </SettingsButton>
          </Flex>
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
      </SettingsForm>

      <SettingsSelectionModal
        picker={privilegesPicker}
        title={ROLE_DETAIL.PICKERS.PRIVILEGES_TITLE}
        emptyMessage={ROLE_DETAIL.PICKERS.PRIVILEGES_EMPTY}
        testId="privileges-picker"
      />
      <SettingsSelectionModal
        picker={rolesPicker}
        title={ROLE_DETAIL.PICKERS.CONTAINED_ROLES_TITLE}
        showDescriptionColumn={false}
        emptyMessage={ROLE_DETAIL.PICKERS.CONTAINED_ROLES_EMPTY}
        testId="contained-roles-picker"
      />
    </Box>
  );
}

export default EditableRoleDetail;
