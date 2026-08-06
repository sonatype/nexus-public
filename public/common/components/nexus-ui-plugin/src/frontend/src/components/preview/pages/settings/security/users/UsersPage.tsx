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

import React, { useState, useEffect, useCallback, useMemo, } from 'react';
import { Box, } from '@radix-ui/themes';
import { Users, Plus, UserPlus } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { useToast, PageHeader } from '../../../../shared';
import { SettingsButton, SettingsAlert, WizardForm } from '../../../../shared/form';
import { DeleteConfirmationModal } from '../../../../shared/modals/DeleteConfirmationModal';
import { UsersList } from './UsersList';
import { UserDetail } from './UserDetail';
import { UserForm } from './UserForm';
import { UserProfilePage } from './UserProfilePage';
import { InviteUserForm } from './InviteUserForm';
import { useUsersApi } from './useUsersApi';
import { User, UserFormData, DEFAULT_SOURCE, getFullName } from './types';

import './UsersPage.scss';

// Base path for user URLs
const BASE_PATH = 'preview/admin/security/users';

/**
 * URL-based routing patterns:
 * - /users                    → List page
 * - /users/create             → Create form (Step 1)
 * - /users/{id}/{source}      → Edit form (Step 1)
 * - /users/{id}/{source}/profile → Profile page (read-only)
 */
type ViewMode = 'list' | 'create' | 'detail' | 'profile' | 'invite';

interface RouteState {
  viewMode: ViewMode;
  userId: string | null;
  source: string | null;
}

function parseRoute(hash: string): RouteState {
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  const parts = cleanHash.split('/');
  const userIndex = parts.indexOf('users');

  if (userIndex === -1) return { viewMode: 'list', userId: null, source: null };

  const pathAfterUsers = parts.slice(userIndex + 1);
  if (pathAfterUsers.length === 0) return { viewMode: 'list', userId: null, source: null };

  if (pathAfterUsers[0] === 'create') {
    return { viewMode: 'create', userId: null, source: null };
  }

  if (pathAfterUsers[0] === 'invite') {
    return { viewMode: 'invite', userId: null, source: null };
  }

  const userId = decodeURIComponent(pathAfterUsers[0]);
  const source = pathAfterUsers[1] ? decodeURIComponent(pathAfterUsers[1]) : DEFAULT_SOURCE;

  if (pathAfterUsers.length >= 3 && pathAfterUsers[2] === 'profile') {
    return { viewMode: 'profile', userId, source };
  }

  return { viewMode: 'detail', userId, source };
}

function navigateTo(path: string) {
  window.location.hash = path;
}

/**
 * UsersPage - Main Users management page for Preview UI
 * 
 * Displays user list with search/filter, and allows creating, editing, and deleting users.
 * Now uses a 2-step wizard for both creation and editing.
 */
export function UsersPage() {
  const [routeState, setRouteState] = useState<RouteState>(() => parseRoute(window.location.hash));
  const [user, setUser] = useState<User | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [listDeleteUserId, setListDeleteUserId] = useState<string | null>(null);
  const [listDeleteUserName, setListDeleteUserName] = useState<string | null>(null);
  const [wizardStep, setWizardStep] = useState(0);
  const [isStep1Valid, setIsStep1Valid] = useState(false);
  const [formDirty, setFormDirty] = useState(false);

  // Toast notifications (app-level provider)
  const toast = useToast();

  const {
    loading,
    error,
    setError,
    fetchUser,
    deleteUser,
  } = useUsersApi();

  const canCreate = ExtJS.checkPermission('nexus:users:create');
  const canUpdate = ExtJS.checkPermission('nexus:users:update');
  const canDelete = ExtJS.checkPermission('nexus:users:delete');
  const isCloud = ExtJS.state?.().getValue?.('isCloud', false) ?? false;

  // Handle hash changes for routing
  useEffect(() => {
    const handleHashChange = () => {
      const newState = parseRoute(window.location.hash);
      setRouteState(newState);
      setError(null);
      setWizardStep(0);
      setFormDirty(false);
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [setError]);

  // Load user details when selected (detail or profile)
  useEffect(() => {
    if ((routeState.viewMode === 'detail' || routeState.viewMode === 'profile') && routeState.userId) {
      fetchUser(routeState.userId, routeState.source || DEFAULT_SOURCE)
        .then((userData) => {
          if (userData) {
            setUser(userData);
          } else {
            setError('User not found');
            navigateTo(BASE_PATH);
          }
        })
        .catch((err) => {
          setError(err.message);
          navigateTo(BASE_PATH);
        });
    } else {
      setUser(null);
    }
  }, [routeState.viewMode, routeState.userId, routeState.source, fetchUser, setError]);

  const handleViewUser = useCallback((userId: string, realm: string) => {
    navigateTo(`${BASE_PATH}/${encodeURIComponent(userId)}/${encodeURIComponent(realm)}/profile`);
  }, []);

  const handleEditUser = useCallback((userId: string, realm: string) => {
    navigateTo(`${BASE_PATH}/${encodeURIComponent(userId)}/${encodeURIComponent(realm)}`);
  }, []);

  const handleCreate = useCallback(() => {
    navigateTo(`${BASE_PATH}/create`);
  }, []);

  const handleInvite = useCallback(() => {
    navigateTo(`${BASE_PATH}/invite`);
  }, []);

  const handleBack = useCallback(() => {
    navigateTo(BASE_PATH);
  }, []);

  const userFormSubmitRef = React.useRef<(() => void) | null>(null);

  const handleFinalSubmit = useCallback(() => {
    if (userFormSubmitRef.current) {
      userFormSubmitRef.current();
    }
  }, []);

  const handleSave = useCallback(async (_data: UserFormData) => {
    setRefreshKey((k) => k + 1);
    handleBack();
  }, [handleBack]);

  const handleDelete = useCallback(() => {
    if (!user) return;
    setDeleteDialogOpen(true);
  }, [user]);

  const handleDeleteConfirm = useCallback(async () => {
    const userIdToDelete = listDeleteUserId || user?.userId;
    if (!userIdToDelete) return;

    setIsDeleting(true);
    try {
      await deleteUser(userIdToDelete);
      toast.success(`User "${listDeleteUserName || userIdToDelete}" deleted successfully`);
      setRefreshKey((k) => k + 1);
      setDeleteDialogOpen(false);
      setListDeleteUserId(null);
      setListDeleteUserName(null);
      if (user && user.userId === userIdToDelete) handleBack();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsDeleting(false);
    }
  }, [user, listDeleteUserId, deleteUser, handleBack, toast, setError, listDeleteUserName]);

  const handleListDelete = useCallback((userId: string, userName?: string) => {
    setListDeleteUserId(userId);
    setListDeleteUserName(userName || null);
    setDeleteDialogOpen(true);
  }, []);

  // Header configuration
  const headerProps = useMemo(() => {
    switch (routeState.viewMode) {
      case 'list':
        return {
          icon: Users,
          title: 'Users',
          description: 'Manage users and their role assignments',
          actions: canCreate ? (
            isCloud ? (
              <SettingsButton testId="invite-user-button" variant="primary" onClick={handleInvite} icon={UserPlus}>
                Invite User
              </SettingsButton>
            ) : (
              <SettingsButton testId="create-user-button" variant="primary" onClick={handleCreate} icon={Plus}>
                Create Local User
              </SettingsButton>
            )
          ) : undefined
        };
      case 'create':
        return {
          icon: Users,
          title: 'Create User',
          description: wizardStep === 0 ? 'Step 1: Setup user details' : 'Step 2: Assign roles',
        };
      case 'detail':
        return {
          icon: Users,
          title: user ? `Edit ${getFullName(user)}` : 'User Details',
          description: wizardStep === 0 ? 'Step 1: Edit user details' : 'Step 2: Manage roles',
        };
      case 'invite':
        return {
          icon: Users,
          title: 'Invite User',
          description: 'Send an invitation to a new user',
        };
      default:
        return {
          icon: Users,
          title: 'Users',
          description: 'Manage users'
        };
    }
  }, [routeState.viewMode, user, canCreate, isCloud, handleCreate, handleInvite, wizardStep]);

  return (
    <Box
      className="users-page"
      data-testid="users-page"
      data-view={routeState.viewMode}
      data-loading={loading ? 'true' : 'false'}
      aria-busy={loading}
    >
      {routeState.viewMode !== 'profile' && (
        <PageHeader
          icon={headerProps.icon}
          title={headerProps.title}
          description={headerProps.description}
          actions={routeState.viewMode === 'list' ? headerProps.actions : undefined}
          breadcrumbs={routeState.viewMode === 'list'
            ? [
                { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
                { label: 'Users' }
              ]
            : [
                { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
                { label: 'Users', onClick: handleBack },
                { label: routeState.viewMode === 'create' ? 'Create'
                  : routeState.viewMode === 'invite' ? 'Invite'
                  : user?.userId || 'Loading...' }
              ]
          }
        />
      )}

      {/* Alerts */}
      {error && (
        <Box className="users-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="users-page__content">
        {routeState.viewMode === 'list' && (
          <UsersList
            key={refreshKey}
            onSelect={handleViewUser}
            onEdit={handleEditUser}
            onDelete={canDelete ? handleListDelete : undefined}
            onCreate={isCloud ? handleInvite : handleCreate}
            canEdit={canUpdate}
            canDelete={canDelete}
            isCloud={isCloud}
          />
        )}

        {(routeState.viewMode === 'create' || (routeState.viewMode === 'detail' && canUpdate)) && (
          <WizardForm
            steps={[
              { id: 'setup', label: 'Setup' },
              { id: 'roles', label: 'Roles' },
            ]}
            currentStep={wizardStep}
            onStepChange={setWizardStep}
            onComplete={handleFinalSubmit}
            onCancel={handleBack}
            completeLabel={routeState.viewMode === 'create' ? 'Create User' : 'Save'}
            submitAnalyticsId={routeState.viewMode === 'create' ? 'nxrm-user-create' : 'nxrm-user-save'}
            dirty={formDirty}
            canAdvance={wizardStep === 0 ? isStep1Valid : true}
            loading={loading && wizardStep === 1}
          >
            <UserForm
              user={user}
              userId={routeState.viewMode === 'detail' ? routeState.userId : undefined}
              userSource={routeState.viewMode === 'detail' ? (routeState.source || DEFAULT_SOURCE) : undefined}
              isCreate={routeState.viewMode === 'create'}
              onSave={handleSave}
              onCancel={handleBack}
              onDelete={canDelete && user?.userId !== 'admin' ? handleDelete : undefined}
              loading={loading}
              error={error || undefined}
              wizardStep={wizardStep}
              hideActions={true}
              onValidationChange={setIsStep1Valid}
              onDirtyChange={setFormDirty}
              onSubmitRef={userFormSubmitRef}
            />
          </WizardForm>
        )}

        {routeState.viewMode === 'profile' && (
          <UserProfilePage
            userId={routeState.userId!}
            userSource={routeState.source || DEFAULT_SOURCE}
            onBack={handleBack}
            onEdit={() =>
              navigateTo(
                `${BASE_PATH}/${encodeURIComponent(routeState.userId!)}/${encodeURIComponent(routeState.source || DEFAULT_SOURCE)}`
              )
            }
            canEdit={canUpdate}
          />
        )}

        {routeState.viewMode === 'detail' && !canUpdate && (
          <UserDetail
            user={user}
            loading={loading && !user}
            canEdit={false}
            canDelete={canDelete}
            onSave={handleSave}
            onDelete={handleDelete}
            onCancel={handleBack}
            error={error || undefined}
          />
        )}

        {routeState.viewMode === 'invite' && (
          <InviteUserForm
            onSuccess={(email) => {
              toast.success('Invitation email sent', `An invitation email has been sent to ${email}. New users will appear in the user list once they log in for the first time.`);
              handleBack();
              setRefreshKey((k) => k + 1);
            }}
            onCancel={handleBack}
            loading={loading}
            error={error || undefined}
          />
        )}
      </Box>

      {/* Delete Confirmation Modal */}
      <DeleteConfirmationModal
        open={deleteDialogOpen}
        testId="delete-user-dialog"
        data-analytics-id="nxrm-user-delete"
        onClose={() => {
          setDeleteDialogOpen(false);
          setListDeleteUserId(null);
          setListDeleteUserName(null);
        }}
        onConfirm={handleDeleteConfirm}
        entityName={listDeleteUserName || listDeleteUserId || user?.userId || ''}
        entityType="user"
        loading={isDeleting}
      />
    </Box>
  );
}

export default UsersPage;


