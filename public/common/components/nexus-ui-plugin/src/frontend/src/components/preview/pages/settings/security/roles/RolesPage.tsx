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

import React, { useMemo } from 'react';
import { Box, Button, Flex, Text } from '@radix-ui/themes';
import { Shield, Plus, RotateCcw, AlertTriangle } from 'lucide-react';

import { PageHeader } from '../../../../shared';
import { SettingsAlert, SettingsButton } from '../../../../shared/form';
import { DeleteConfirmationModal } from '../../../../shared/modals/DeleteConfirmationModal';
import { RolesList } from './RolesList';
import { RoleForm } from './RoleForm';
import { RoleDetail } from './RoleDetail';
import { formatRoleSourceDisplay } from './types';
import { BASE_PATH, navigateTo, useRolesPage } from './useRolesPage';

import './RolesPage.scss';

/**
 * URL-based routing patterns:
 * - /roles           -> List page
 * - /roles/create    -> Create wizard
 * - /roles/{id}      -> Role detail (editable or read-only, decided by permissions + role.readOnly)
 *
 * All state, effects, and side-actions live in useRolesPage (ViewModel). This component
 * is presentational: it reads from the hook and renders.
 */
export function RolesPage() {
  const {
    routeState,
    role,
    loading,
    error,
    roleLoadErrorType,
    refreshKey,
    canCreate,
    roleCanEdit,
    roleCanDelete,
    deleteDialogOpen,
    setDeleteDialogOpen,
    setError,
    handleCreate,
    handleBack,
    handleComplete,
    handleDelete,
    handleDeleteConfirm,
    retryRoleLoad,
  } = useRolesPage();

  // Header configuration
  const headerProps = useMemo(() => {
    switch (routeState.viewMode) {
      case 'list':
        return {
          icon: Shield,
          title: 'Roles',
          description: 'Manage roles and their privilege assignments',
          actions: (
            <Button variant="solid" onClick={handleCreate} disabled={!canCreate} data-testid="create-role-button" data-analytics-id="nxrm-role-create">
              <Plus size={16} /> Create Role
            </Button>
          )
        };
      case 'create':
        return {
          icon: Shield,
          title: 'Create Role',
          description: 'Add a new role with privileges and contained roles',
        };
      case 'detail':
        return {
          icon: Shield,
          title: role ? (roleCanEdit ? `Edit ${role.name}` : role.name) : 'Role Details',
          description: role ? `Source: ${formatRoleSourceDisplay(role.source)}` : 'Loading...',
        };
      default:
        return {
          icon: Shield,
          title: 'Roles',
          description: 'Manage roles'
        };
    }
  }, [routeState.viewMode, role, canCreate, handleCreate, roleCanEdit]);

  const showTransientErrorCard =
    routeState.viewMode === 'detail' && roleLoadErrorType === 'transient' && !role;

  return (
    <Box
      className="roles-page"
      data-testid="roles-page"
      data-view={routeState.viewMode}
      data-loading={loading ? 'true' : 'false'}
    >
      {/* Alerts */}
      {error && (
        <Box className="roles-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="roles-page__content">
        {routeState.viewMode === 'list' && (
          <>
            <PageHeader
              icon={headerProps.icon}
              title={headerProps.title}
              description={headerProps.description}
              actions={headerProps.actions}
              breadcrumbs={[
                { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
                { label: 'Roles' },
              ]}
            />
            <RolesList
              key={refreshKey}
              onSelect={(roleId) => navigateTo(`${BASE_PATH}/${encodeURIComponent(roleId)}`)}
              onCreate={handleCreate}
            />
          </>
        )}

        {showTransientErrorCard && (
          <>
            <PageHeader
              icon={Shield}
              title="Role Details"
              description="Failed to load"
              breadcrumbs={[
                { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
                { label: 'Roles', onClick: handleBack },
              ]}
            />
            <Flex
              direction="column"
              align="center"
              gap="3"
              className="roles-page__error-card"
              data-testid="role-load-error"
              p="6"
            >
              <AlertTriangle size={32} />
              <Text size="3" weight="medium">Unable to load role</Text>
              <Text size="2" color="gray">{error || 'An unexpected error occurred while loading this role.'}</Text>
              <Flex gap="2">
                <SettingsButton
                  variant="primary"
                  icon={RotateCcw}
                  onClick={retryRoleLoad}
                  testId="role-load-retry"
                >
                  Retry
                </SettingsButton>
                <SettingsButton variant="secondary" onClick={handleBack}>
                  Back to List
                </SettingsButton>
              </Flex>
            </Flex>
          </>
        )}

        {routeState.viewMode === 'detail' && role && (
          <>
            <PageHeader
              icon={Shield}
              title={headerProps.title}
              description={headerProps.description}
              breadcrumbs={[
                { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
                { label: 'Roles', onClick: handleBack },
                { label: role.name },
              ]}
            />
            <RoleDetail
              role={role}
              loading={loading}
              canEdit={roleCanEdit}
              canDelete={roleCanDelete}
              onDelete={handleDelete}
              onCancel={handleBack}
              onComplete={handleComplete}
              error={error || undefined}
            />
          </>
        )}

        {routeState.viewMode === 'create' && (
          <>
            <PageHeader
              icon={Shield}
              title={headerProps.title}
              description={headerProps.description}
              breadcrumbs={[
                { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
                { label: 'Roles', onClick: handleBack },
                { label: 'Create' },
              ]}
            />
            <RoleForm
              isCreate
              onCancel={handleBack}
              onComplete={handleComplete}
              loading={loading}
            />
          </>
        )}
      </Box>

      {roleCanDelete && (
        <DeleteConfirmationModal
          open={deleteDialogOpen}
          onClose={() => setDeleteDialogOpen(false)}
          onConfirm={handleDeleteConfirm}
          entityName={role?.name}
          entityType="role"
        >
          {role && (
            <Box>
              <Flex gap="2">
                <Text size="2" color="gray">Privileges:</Text>
                <Text size="2">
                  {role.privileges.length === 0
                    ? 'None'
                    : role.privileges.length === 1
                      ? '1 privilege'
                      : `${role.privileges.length} privileges`}
                </Text>
              </Flex>
              <Text as="p" size="2" color="gray" mt="2">
                Users assigned to this role will lose access to its privileges.
              </Text>
            </Box>
          )}
        </DeleteConfirmationModal>
      )}
    </Box>
  );
}

export default RolesPage;
