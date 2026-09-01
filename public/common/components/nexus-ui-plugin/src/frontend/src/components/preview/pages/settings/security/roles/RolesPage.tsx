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

import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Box, Button, Flex, Text } from '@radix-ui/themes';
import { Shield, Plus, ArrowLeft, Trash2, Pencil } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import Permissions from '../../../../../../constants/Permissions';

import { useToast, PageHeader } from '../../../../shared';
import { SettingsAlert, ConfirmDialog, WizardForm, SettingsButton } from '../../../../shared/form';
import { RolesList } from './RolesList';
import { RoleForm } from './RoleForm';
import { RoleProfilePage } from './RoleProfilePage';
import { useRolesApi } from './useRolesApi';
import { Role, isReadOnlyRole, formatRoleSourceDisplay } from './types';

import './RolesPage.scss';

// Base path for role URLs
const BASE_PATH = 'preview/admin/security/roles';

/**
 * URL-based routing patterns:
 * - /roles           → List page
 * - /roles/create    → Wizard (Step 1)
 * - /roles/{id}      → Edit form
 * - /roles/{id}/profile → Profile page
 */
type ViewMode = 'list' | 'create' | 'edit' | 'profile';

interface RouteState {
  viewMode: ViewMode;
  roleId: string | null;
}

function parseRoute(hash: string): RouteState {
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  const parts = cleanHash.split('/');
  const roleIndex = parts.indexOf('roles');
  
  if (roleIndex === -1) return { viewMode: 'list', roleId: null };

  const pathAfterRoles = parts.slice(roleIndex + 1);
  if (pathAfterRoles.length === 0) return { viewMode: 'list', roleId: null };

  if (pathAfterRoles[0] === 'create') {
    return { viewMode: 'create', roleId: null };
  }

  const roleId = decodeURIComponent(pathAfterRoles[0]);
  if (pathAfterRoles.length >= 2 && pathAfterRoles[1] === 'profile') {
    return { viewMode: 'profile', roleId };
  }

  return { 
    viewMode: 'edit', 
    roleId 
  };
}

function navigateTo(path: string) {
  window.location.hash = path;
}

/**
 * RolesPage - Main Roles management page for Preview UI
 * 
 * Displays role list with search/filter, and allows creating, editing, and deleting roles.
 * Uses a multi-step wizard for creation and editing.
 */
export function RolesPage() {
  const [routeState, setRouteState] = useState<RouteState>(() => parseRoute(window.location.hash));
  const [role, setRole] = useState<Role | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [listDeleteRoleId, setListDeleteRoleId] = useState<string | null>(null);
  const [listDeleteRoleName, setListDeleteRoleName] = useState<string | null>(null);
  const [internalWizardStep, setInternalWizardStep] = useState(0);

  // Toast notifications (app-level provider)
  const toast = useToast();

  const {
    loading,
    error,
    setError,
    findRole,
    deleteRole,
  } = useRolesApi();

  // Provider-independent, reactive permission checks (NEXUS-54212). ExtJS.checkPermission on
  // its own evaluates once at render and would briefly disable the Create/Edit/Delete controls
  // for a permitted non-admin if permissions load asynchronously after mount; ExtJS.usePermission
  // with a hasUser dependency re-evaluates once the user and their permissions arrive. Matches the
  // pattern used by ContentSelectorsPage and CapabilityDetail.
  const hasUser = ExtJS.useUser() ?? false;
  const canCreate = ExtJS.usePermission(() => ExtJS.checkPermission(Permissions.ROLES.CREATE), [hasUser]);
  const canUpdate = ExtJS.usePermission(() => ExtJS.checkPermission(Permissions.ROLES.UPDATE), [hasUser]);
  const canDelete = ExtJS.usePermission(() => ExtJS.checkPermission(Permissions.ROLES.DELETE), [hasUser]);

  // Handle hash changes for routing
  useEffect(() => {
    const handleHashChange = () => {
      const newState = parseRoute(window.location.hash);
      setRouteState(newState);
      setError(null);
      setInternalWizardStep(0);
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [setError]);

  // Load role details when in edit or profile mode
  useEffect(() => {
    if ((routeState.viewMode === 'edit' || routeState.viewMode === 'profile') && routeState.roleId) {
      findRole(routeState.roleId)
        .then((roleData) => {
          if (roleData) {
            setRole(roleData);
          } else {
            setError('Role not found');
            navigateTo(BASE_PATH);
          }
        })
        .catch((err) => {
          setError(err.message);
          navigateTo(BASE_PATH);
        });
    } else {
      setRole(null);
    }
  }, [routeState.viewMode, routeState.roleId, findRole, setError]);

  const handleCreate = useCallback(() => {
    navigateTo(`${BASE_PATH}/create`);
  }, []);

  const handleBack = useCallback(() => {
    navigateTo(BASE_PATH);
  }, []);

  const handleWizardStepChange = useCallback((step: number) => {
    setInternalWizardStep(step);
  }, []);

  const roleFormSubmitRef = useRef<(() => void) | null>(null);
  const [isRoleFormValid, setIsRoleFormValid] = useState(false);

  const handleFinalSubmit = useCallback(() => {
    if (roleFormSubmitRef.current) {
      roleFormSubmitRef.current();
    }
  }, []);

  const handleComplete = useCallback(() => {
    setRefreshKey((k) => k + 1);
    navigateTo(BASE_PATH);
  }, []);

  const handleDelete = useCallback(() => {
    if (!role) return;
    setDeleteDialogOpen(true);
  }, [role]);

  const handleDeleteConfirm = useCallback(async () => {
    const roleIdToDelete = listDeleteRoleId || role?.id;
    if (!roleIdToDelete) return;
    
    setDeleteDialogOpen(false);
    setListDeleteRoleId(null);
    setListDeleteRoleName(null);
    try {
      await deleteRole(roleIdToDelete);
      toast.success(`Role "${listDeleteRoleName || roleIdToDelete}" deleted successfully`);
      setRefreshKey((k) => k + 1);
      if (role && role.id === roleIdToDelete) handleBack();
    } catch (_err) {
      // Error is set by the API hook
    }
  }, [role, listDeleteRoleId, deleteRole, handleBack, toast, listDeleteRoleName]);

  const handleListDelete = useCallback((roleId: string, roleName?: string) => {
    setListDeleteRoleId(roleId);
    setListDeleteRoleName(roleName || null);
    setDeleteDialogOpen(true);
  }, []);

  // Determine if role can be edited/deleted
  const _roleCanEdit = canUpdate && role && !isReadOnlyRole(role);
  const roleCanDelete = canDelete && role && !isReadOnlyRole(role);

  // Header configuration
  const headerProps = useMemo(() => {
    switch (routeState.viewMode) {
      case 'list':
        return {
          icon: Shield,
          title: 'Roles',
          description: 'Manage roles and their privilege assignments',
          actions: (
            <Button variant="solid" color="blue" highContrast onClick={handleCreate} disabled={!canCreate} data-testid="create-role-button" data-analytics-id="nxrm-role-create">
              <Plus size={16} /> Create Role
            </Button>
          )
        };
      case 'create': {
        const createSteps = ['Type', 'Setup', 'Privileges', 'Contained Roles'];
        return {
          icon: Shield,
          title: 'Create Role',
          description: `Step ${internalWizardStep + 1}: ${createSteps[internalWizardStep]}`,
          actions: (
            <Button variant="soft" color="gray" onClick={handleBack}>
              <ArrowLeft size={16} /> Back to List
            </Button>
          )
        };
      }
      case 'edit': {
        // Type step is skipped in edit mode (role type is immutable once persisted),
        // so the edit wizard runs Setup → Privileges → Contained Roles.
        const editSteps = ['Setup', 'Privileges', 'Contained Roles'];
        return {
          icon: Shield,
          title: role ? `Edit ${role.name}` : 'Role Details',
          description: role
            ? `Step ${internalWizardStep + 1}: ${editSteps[internalWizardStep]} (Source: ${formatRoleSourceDisplay(role.source)})`
            : 'Loading...',
          actions: (
            <Button variant="soft" color="gray" onClick={handleBack}>
              <ArrowLeft size={16} /> Back to List
            </Button>
          )
        };
      }
      case 'profile': {
        const canEditThisRole = canUpdate && role && !isReadOnlyRole(role);
        return {
          icon: Shield,
          title: routeState.roleId || 'Role Profile',
          description: 'Role operational dashboard and impact analysis',
          actions: (
            <Flex gap="2">
              {canEditThisRole && (
                <Button
                  variant="solid"
                  color="blue"
                  highContrast
                  onClick={() => navigateTo(`${BASE_PATH}/${encodeURIComponent(routeState.roleId!)}`)}
                >
                  <Pencil size={16} /> Edit Role
                </Button>
              )}
              <Button variant="soft" color="gray" onClick={handleBack}>
                <ArrowLeft size={16} /> Back to List
              </Button>
            </Flex>
          )
        };
      }
      default:
        return {
          icon: Shield,
          title: 'Roles',
          description: 'Manage roles'
        };
    }
  }, [routeState.viewMode, role, canCreate, handleCreate, handleBack, internalWizardStep, canUpdate, routeState.roleId]);

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
            { label: 'Roles' }
          ]}
/>
            <RolesList
              key={refreshKey}
              onSelect={(roleId, mode) => {
                const path = mode === 'edit'
                  ? `${BASE_PATH}/${encodeURIComponent(roleId)}`
                  : `${BASE_PATH}/${encodeURIComponent(roleId)}/profile`;
                navigateTo(path);
              }}
              onDelete={canDelete ? handleListDelete : undefined}
              onCreate={handleCreate}
              canUpdate={canUpdate}
              canDelete={canDelete}
            />
          </>
        )}

        {routeState.viewMode === 'profile' && routeState.roleId && (
          <>
            <PageHeader
              icon={Shield}
              title={headerProps.title}
              description={headerProps.description}
              breadcrumbs={[
                { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
                { label: 'Roles', onClick: handleBack },
                { label: routeState.roleId || 'Role Profile' },
              ]}
              actions={headerProps.actions}
            />
            <RoleProfilePage 
              roleName={routeState.roleId} 
              onBack={handleBack} 
            />
          </>
        )}

        {(routeState.viewMode === 'create' || (routeState.viewMode === 'edit' && role)) && (() => {
          // Edit mode skips the Type step (role type is immutable once persisted),
          // so the wizard is 3 steps: Setup → Privileges → Contained Roles.
          // RoleForm still interprets `wizardStep` in its logical sense (0=Type, 1=Setup,
          // 2=Privileges, 3=Roles), so in edit mode we shift by +1 at the boundary.
          const isCreateView = routeState.viewMode === 'create';
          const steps = isCreateView
            ? [
                { id: 'type', label: 'Type' },
                { id: 'setup', label: 'Setup' },
                { id: 'privileges', label: 'Privileges' },
                { id: 'roles', label: 'Contained Roles' },
              ]
            : [
                { id: 'setup', label: 'Setup' },
                { id: 'privileges', label: 'Privileges' },
                { id: 'roles', label: 'Contained Roles' },
              ];
          const setupStepIndex = isCreateView ? 1 : 0;
          const finalStepIndex = isCreateView ? 3 : 2;
          const logicalWizardStep = isCreateView ? internalWizardStep : internalWizardStep + 1;
          return (
          <WizardForm
            steps={steps}
            currentStep={internalWizardStep}
            onStepChange={handleWizardStepChange}
            onComplete={handleFinalSubmit}
            onCancel={handleBack}
            completeLabel={isCreateView ? 'Create Role' : 'Save'}
            submitAnalyticsId={isCreateView ? 'nxrm-role-create' : 'nxrm-role-save'}
            dirty={false}
            canAdvance={
              internalWizardStep === setupStepIndex || internalWizardStep === finalStepIndex
                ? isRoleFormValid
                : true
            }
            loading={loading && internalWizardStep === finalStepIndex}
            noDirtyTracking={true}
            title={headerProps.title}
            description={headerProps.description}
            footerExtra={
              <Flex align="center" gap="3">
                {headerProps.actions}
                {routeState.viewMode === 'edit' && roleCanDelete && (
                  <SettingsButton variant="danger" onClick={handleDelete} disabled={loading} icon={Trash2}>
                    Delete Role
                  </SettingsButton>
                )}
              </Flex>
            }
          >
            <RoleForm
              isCreate={isCreateView}
              role={role}
              onCancel={handleBack}
              onComplete={handleComplete}
              loading={loading}
              onSubmitRef={roleFormSubmitRef}
              onValidationChange={setIsRoleFormValid}
              wizardStep={logicalWizardStep}
              hideActions
            />
          </WizardForm>
          );
        })()}
      </Box>

      {/* Delete Confirmation Dialog (edit view or list) */}
      {(roleCanDelete || listDeleteRoleId) && (
        <ConfirmDialog
          open={deleteDialogOpen}
          testId="delete-role-dialog"
          data-analytics-id="nxrm-role-delete"
          onOpenChange={(open) => {
            setDeleteDialogOpen(open);
            if (!open) setListDeleteRoleId(null);
          }}
          title="Delete Role?"
          message="This action cannot be undone."
          entityName={listDeleteRoleName || listDeleteRoleId || role?.name}
          confirmLabel="Delete"
          cancelLabel="Cancel"
          variant="danger"
          onConfirm={handleDeleteConfirm}
        >
          {role && (
            <Box mt="3">
              <Flex gap="2">
                <Text size="2" color="gray">Privileges:</Text>
                <Text size="2">{role.privileges.length === 0 ? 'None' : role.privileges.length === 1 ? '1 privilege' : `${role.privileges.length} privileges`}</Text>
              </Flex>
            </Box>
          )}
          <Box mt="3">
            <Text as="p" size="2" color="gray">
              Users assigned to this role will lose access to its privileges.
            </Text>
          </Box>
        </ConfirmDialog>
      )}
    </Box>
  );
}

export default RolesPage;
