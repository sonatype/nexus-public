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
import { Box, Flex, Text, Heading, Button } from '@radix-ui/themes';
import { Key, Plus, ArrowLeft } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import { useToast, PageHeader, type PageHeaderProps } from '../../../../shared';
import { SettingsButton, SettingsAlert, ConfirmDialog, WizardForm } from '../../../shared/form';
import { PrivilegesList } from './PrivilegesList';
import { PrivilegeDetail } from './PrivilegeDetail';
import { PrivilegeForm } from './PrivilegeForm';
import { PrivilegeProfilePage } from './PrivilegeProfilePage';
import { PrivilegeTypeSelector } from './PrivilegeTypeSelector';
import { usePrivilegesApi } from './usePrivilegesApi';
import { Privilege, PrivilegeFormData, isReadOnlyPrivilege, getPrivilegeTypeLabel } from './types';

import './PrivilegesPage.scss';

// Base path for privilege URLs
const BASE_PATH = 'preview/admin/security/privileges';

/**
 * URL-based routing patterns:
 * - /privileges           → List page
 * - /privileges/create    → Type selector (Step 1)
 * - /privileges/create/{type} → Configuration form (Step 2)
 * - /privileges/{id}      → Edit form
 * - /privileges/{id}/profile → Profile (read-only)
 */
type ViewMode = 'list' | 'select-type' | 'create' | 'edit' | 'profile' | 'detail';

interface RouteState {
  viewMode: ViewMode;
  privilegeId: string | null;
  typeId: string | null;
}

function parseRoute(hash: string): RouteState {
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  const parts = cleanHash.split('/');
  const privIndex = parts.indexOf('privileges');
  
  if (privIndex === -1) return { viewMode: 'list', privilegeId: null, typeId: null };

  const pathAfterPrivs = parts.slice(privIndex + 1);
  if (pathAfterPrivs.length === 0) return { viewMode: 'list', privilegeId: null, typeId: null };

  if (pathAfterPrivs[0] === 'create') {
    if (pathAfterPrivs.length === 1) return { viewMode: 'select-type', privilegeId: null, typeId: null };
    return {
      viewMode: 'create',
      privilegeId: null,
      typeId: decodeURIComponent(pathAfterPrivs[1]),
    };
  }

  const privilegeId = decodeURIComponent(pathAfterPrivs[0]);
  if (pathAfterPrivs.length >= 2 && pathAfterPrivs[1] === 'profile') {
    return { viewMode: 'profile', privilegeId, typeId: null };
  }

  return { 
    viewMode: 'edit', 
    privilegeId, 
    typeId: null 
  };
}

function navigateTo(path: string) {
  window.location.hash = path;
}

/**
 * PrivilegesPage - Main Privileges management page for Preview UI
 * 
 * Displays privilege list with search/filter, and allows creating, editing, and deleting privileges.
 * Uses a multi-step wizard for creation.
 */
export function PrivilegesPage() {
  const [routeState, setRouteState] = useState<RouteState>(() => parseRoute(window.location.hash));
  const [privilege, setPrivilege] = useState<Privilege | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [listDeletePrivilegeId, setListDeletePrivilegeId] = useState<string | null>(null);
  const [internalWizardStep, setInternalWizardStep] = useState(0);
  const [pendingTypeId, setPendingTypeId] = useState<string | null>(() => parseRoute(window.location.hash).typeId);

  // Toast notifications (app-level provider)
  const toast = useToast();

  const {
    loading,
    error,
    setError,
    findPrivilege,
    deletePrivilege,
  } = usePrivilegesApi();

  const canCreate = ExtJS.checkPermission('nexus:privileges:create');
  const canUpdate = ExtJS.checkPermission('nexus:privileges:update');
  const canDelete = ExtJS.checkPermission('nexus:privileges:delete');

  // Handle hash changes for routing
  useEffect(() => {
    const handleHashChange = () => {
      const newState = parseRoute(window.location.hash);
      setRouteState(newState);
      setError(null);

      // Synchronize internal wizard step with route
      if (newState.viewMode === 'create' && newState.typeId) {
        setInternalWizardStep(1);
        setPendingTypeId(newState.typeId);
      } else if (newState.viewMode === 'select-type') {
        setInternalWizardStep(0);
        setPendingTypeId(null);
      } else {
        setInternalWizardStep(0);
        setPendingTypeId(null);
      }
    };
    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [setError]);

  // Load privilege details when in edit mode (profile uses its own hook)
  useEffect(() => {
    if (routeState.viewMode === 'edit' && routeState.privilegeId) {
      findPrivilege(routeState.privilegeId)
        .then((privilegeData) => {
          if (privilegeData) {
            setPrivilege(privilegeData);
          } else {
            setError('Privilege not found');
            navigateTo(BASE_PATH);
          }
        })
        .catch((err) => {
          setError(err.message);
          navigateTo(BASE_PATH);
        });
    } else {
      setPrivilege(null);
    }
  }, [routeState.viewMode, routeState.privilegeId, findPrivilege, setError]);

  const handleSelectPrivilege = useCallback((privilegeId: string) => {
    navigateTo(`${BASE_PATH}/${encodeURIComponent(privilegeId)}/profile`);
  }, []);

  const handleEditPrivilege = useCallback((privilegeId: string) => {
    navigateTo(`${BASE_PATH}/${encodeURIComponent(privilegeId)}`);
  }, []);

  const handleCreate = useCallback(() => {
    navigateTo(`${BASE_PATH}/create`);
  }, []);

  const handleBack = useCallback(() => {
    navigateTo(BASE_PATH);
  }, []);

  const handleBackToTypeSelect = useCallback(() => {
    navigateTo(`${BASE_PATH}/create`);
  }, []);

  const handleTypeSelect = useCallback((typeId: string) => {
    setPendingTypeId(typeId);
  }, []);

  const handleWizardStepChange = useCallback((step: number) => {
    // Clear dirty state IMMEDIATELY before navigation to prevent unsaved changes dialog
    if (typeof window !== 'undefined' && window.dirty) {
      window.dirty.length = 0;
    }

    if (step === 0) {
      setPendingTypeId(null);
      setInternalWizardStep(0);
      navigateTo(`${BASE_PATH}/create`);
    } else if (step === 1) {
      setInternalWizardStep(1);
      if (pendingTypeId) {
        navigateTo(`${BASE_PATH}/create/${encodeURIComponent(pendingTypeId)}`);
      }
    } else if (step === 2) {
      setInternalWizardStep(2);
    } else if (step === 3) {
      setInternalWizardStep(3);
    }
  }, [pendingTypeId]);

  const privilegeFormSubmitRef = useRef<(() => void) | null>(null);
  const [isPrivilegeFormValid, setIsPrivilegeFormValid] = useState(false);

  const handleFinalSubmit = useCallback(() => {
    if (privilegeFormSubmitRef.current) {
      privilegeFormSubmitRef.current();
    }
  }, []);

  const handleSave = useCallback(async () => {
    setRefreshKey((k) => k + 1);
    navigateTo(BASE_PATH);
  }, []);

  const handleDelete = useCallback(() => {
    if (!privilege) return;
    setDeleteDialogOpen(true);
  }, [privilege]);

  const handleDeleteConfirm = useCallback(async () => {
    const privilegeIdToDelete = listDeletePrivilegeId || privilege?.name;
    if (!privilegeIdToDelete) return;
    
    setDeleteDialogOpen(false);
    setListDeletePrivilegeId(null);
    try {
      await deletePrivilege(privilegeIdToDelete);
      toast.success(`Privilege "${privilegeIdToDelete}" deleted successfully`);
      setRefreshKey((k) => k + 1);
      if (privilege && privilege.name === privilegeIdToDelete) handleBack();
    } catch (err) {
      // Error is set by the API hook
    }
  }, [privilege, listDeletePrivilegeId, deletePrivilege, handleBack, toast]);

  const handleListDelete = useCallback((privilegeId: string) => {
    setListDeletePrivilegeId(privilegeId);
    setDeleteDialogOpen(true);
  }, []);

  // Determine if privilege can be edited/deleted
  const privilegeCanEdit = canUpdate && privilege && !isReadOnlyPrivilege(privilege);
  const privilegeCanDelete = canDelete && privilege && !isReadOnlyPrivilege(privilege);

  // Header configuration
  const headerProps = useMemo(() => {
    switch (routeState.viewMode) {
      case 'list':
        return {
          icon: Key,
          title: 'Privileges',
          description: 'Manage privileges and their permissions',
          actions: canCreate ? (
            <Button variant="solid" onClick={handleCreate} data-analytics-id="nxrm-privilege-create">
              <Plus size={16} /> Create Privilege
            </Button>
          ) : undefined
        };
      case 'select-type':
        return {
          icon: Key,
          title: 'Create Privilege',
          description: 'Step 1: Select the type of privilege to create',
          actions: (
            <Button variant="ghost" onClick={handleBack}>
              <ArrowLeft size={16} /> Back to List
            </Button>
          )
        };
      case 'create':
        const isSetupStep = internalWizardStep === 1;
        return {
          icon: Key,
          title: `Create ${getPrivilegeTypeLabel(routeState.typeId || '')} Privilege`,
          description: isSetupStep 
            ? 'Step 2: Basic setup' 
            : 'Step 3: Configure privilege settings',
          actions: (
            <Button variant="ghost" onClick={handleBackToTypeSelect}>
              <ArrowLeft size={16} /> Back to Selection
            </Button>
          )
        };
      case 'edit':
        return {
          icon: Key,
          title: privilege ? `Edit ${privilege.name}` : 'Privilege Details',
          description: privilege 
            ? `Type: ${getPrivilegeTypeLabel(privilege.type)}${isReadOnlyPrivilege(privilege) ? ' (Read Only)' : ''}`
            : 'Loading...',
          actions: (
            <Button variant="ghost" onClick={handleBack}>
              <ArrowLeft size={16} /> Back to List
            </Button>
          )
        };
      default:
        return {
          icon: Key,
          title: 'Privileges',
          description: 'Manage privileges'
        };
    }
  }, [routeState.viewMode, routeState.typeId, privilege, canCreate, handleCreate, handleBack, handleBackToTypeSelect, internalWizardStep]);

  const wizardStep = useMemo(() => {
    if (routeState.viewMode === 'create' && routeState.typeId) {
      return internalWizardStep === 0 ? 1 : internalWizardStep;
    }
    return internalWizardStep;
  }, [routeState.viewMode, routeState.typeId, internalWizardStep]);

  return (
    <Box 
      className="privileges-page"
      data-testid="privileges-page"
      data-view={routeState.viewMode}
      data-loading={loading ? 'true' : 'false'}
    >
      {routeState.viewMode !== 'profile' && (
        <Box mb="4">
          <PageHeader
            icon={headerProps.icon}
            title={headerProps.title}
            description={headerProps.description}
            actions={headerProps.actions}

            breadcrumbs={[
              { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
              { label: 'Privileges' }
            ]}
          />
        </Box>
      )}

      {/* Alerts */}
      {error && (
        <Box className="privileges-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="privileges-page__content">
        {routeState.viewMode === 'list' && (
          <PrivilegesList
            key={refreshKey}
            onSelect={handleSelectPrivilege}
            onEdit={handleEditPrivilege}
            onDelete={canDelete ? handleListDelete : undefined}
            onCreate={handleCreate}
            canEdit={canUpdate}
            canDelete={canDelete}
          />
        )}

        {(routeState.viewMode === 'select-type' || routeState.viewMode === 'create') && (
          <WizardForm
            steps={
              routeState.typeId === 'repository-content-selector'
                ? [
                    { id: 'type', label: 'Select Type' },
                    { id: 'setup', label: 'Setup' },
                    { id: 'config', label: 'Configure' },
                    { id: 'preview', label: 'Preview' },
                  ]
                : [
                    { id: 'type', label: 'Select Type' },
                    { id: 'setup', label: 'Setup' },
                    { id: 'config', label: 'Configure' },
                  ]
            }
            currentStep={wizardStep}
            onStepChange={handleWizardStepChange}
            className=""
            onComplete={handleFinalSubmit}
            onCancel={handleBack}
            completeLabel="Create Privilege"
            submitAnalyticsId="nxrm-privilege-create"
            dirty={false}
            canAdvance={wizardStep === 0 ? !!pendingTypeId : wizardStep === 1 ? isPrivilegeFormValid : true}
            loading={loading && wizardStep >= 2}
            noDirtyTracking={true}
          >
            {wizardStep === 0 && (
              <PrivilegeTypeSelector 
                onSelect={handleTypeSelect} 
                selectedTypeId={pendingTypeId}
              />
            )}
            {wizardStep >= 1 && routeState.typeId && (
              <PrivilegeForm
                key={routeState.typeId}
                isCreate={true}
                typeId={routeState.typeId}
                onSave={handleSave}
                onCancel={handleBack}
                loading={loading}
                onSubmitRef={privilegeFormSubmitRef}
                onValidationChange={setIsPrivilegeFormValid}
                wizardStep={wizardStep}
                hideActions
              />
            )}
          </WizardForm>
        )}

        {routeState.viewMode === 'profile' && routeState.privilegeId && (
          <PrivilegeProfilePage
            privilegeId={routeState.privilegeId}
            onBack={handleBack}
            onEdit={
              canUpdate
                ? () => handleEditPrivilege(routeState.privilegeId!)
                : undefined
            }
            canEdit={canUpdate}
          />
        )}

        {routeState.viewMode === 'edit' && privilege && (
          <PrivilegeDetail
            privilege={privilege}
            loading={loading && !privilege}
            canEdit={privilegeCanEdit}
            canDelete={privilegeCanDelete}
            onSave={handleSave}
            onDelete={handleDelete}
            onCancel={handleBack}
            error={error || undefined}
          />
        )}
      </Box>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        testId="delete-privilege-dialog"
        data-analytics-id="nxrm-privilege-delete"
        onOpenChange={(open) => {
          setDeleteDialogOpen(open);
          if (!open) setListDeletePrivilegeId(null);
        }}
        title="Delete Privilege?"
        message={
          <>
            <Text as="p" size="2">This action cannot be undone.</Text>
            <Text as="p" size="2" mt="1">Roles using this privilege will lose this access.</Text>
          </>
        }
        entityName={
          privilege ? (
            <>
              <Text as="span" weight="medium">{privilege.name}</Text>
              {privilege.type && (
                <Text as="span" size="2" color="gray"> ({getPrivilegeTypeLabel(privilege.type)})</Text>
              )}
              {privilege.description && (
                <>
                  <br />
                  <Text as="span" size="2" color="gray">{privilege.description}</Text>
                </>
              )}
            </>
          ) : listDeletePrivilegeId
        }
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDeleteConfirm}
      />
    </Box>
  );
}

export default PrivilegesPage;


