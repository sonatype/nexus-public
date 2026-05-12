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


const navigateTo = (path: string) => {
  window.location.hash = path;
}


import React, { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { Puzzle, Plus, ArrowLeft } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';

import { SettingsButton, WizardForm } from '../../../shared/form';
import { useToast } from '../../../../shared';
import { CapabilitiesList } from './CapabilitiesList';
import { CapabilityTypeSelector } from './CapabilityTypeSelector';
import { CapabilityDetail } from './CapabilityDetail';
import { DynamicCapabilityForm } from './DynamicCapabilityForm';
import { useCapabilitiesApi } from './useCapabilitiesApi';
import {
  Capability,
  CapabilityType,
  CapabilityFormData,
  CapabilitiesViewMode,
  CapabilitiesPageProps,
  getCapabilityDescription,
} from './types';

import './CapabilitiesPage.scss';

/**
 * CapabilitiesPage - Main Capabilities management page for Preview UI
 *
 * Displays capability list with search, and allows creating, editing,
 * enabling/disabling, and deleting capabilities. Uses dynamic forms
 * based on capability type configuration.
 */
const ROUTE_BASE = 'preview.admin.system.capabilities';

export function CapabilitiesPage({ className }: CapabilitiesPageProps) {
  const {
    loading,
    error,
    setError,
    fetchCapabilities,
    fetchCapabilityTypes,
    createCapability,
    updateCapability,
    deleteCapability,
    enableCapability,
    disableCapability,
  } = useCapabilitiesApi();

  const toast = useToast();
  const router = useRouter();
  const { state: routeState, params } = useCurrentStateAndParams();

  const viewMode: CapabilitiesViewMode = useMemo(() => {
    const name = routeState?.name || '';
    if (name.endsWith('.detail')) return 'detail';
    if (name.endsWith('.createWithType')) return 'create';
    if (name.endsWith('.create')) return 'selectType';
    return 'list';
  }, [routeState?.name]);

  const [selectedCapability, setSelectedCapability] = useState<Capability | null>(null);
  const [selectedType, setSelectedType] = useState<CapabilityType | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [createStep, setCreateStep] = useState(0);
  const formSubmitRef = useRef<(() => void) | null>(null);

  const canCreate = ExtJS.checkPermission('nexus:capabilities:create');

  // Deep link: load capability or type from URL params
  useEffect(() => {
    if (viewMode === 'detail' && params.capabilityId && !selectedCapability) {
      fetchCapabilities().then((caps) => {
        const cap = caps.find((c) => c.id === params.capabilityId);
        if (cap) {
          setSelectedCapability(cap);
        } else {
          router.stateService.go(`${ROUTE_BASE}.list`);
        }
      }).catch(() => {
        router.stateService.go(`${ROUTE_BASE}.list`);
      });
    }
    if (viewMode === 'create' && params.typeId && !selectedType) {
      fetchCapabilityTypes().then((types) => {
        const type = types.find((t) => t.id === params.typeId);
        if (type) {
          setSelectedType(type);
          setCreateStep(1);
        } else {
          router.stateService.go(`${ROUTE_BASE}.create`);
        }
      }).catch(() => {
        router.stateService.go(`${ROUTE_BASE}.create`);
      });
    }
    // Reset step when entering selectType view
    if (viewMode === 'selectType') {
      setCreateStep(0);
    }
  }, [viewMode, params.capabilityId, params.typeId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSelectCapability = useCallback((capability: Capability) => {
    setSelectedCapability(capability);
    setSelectedType(null);
    setError(null);
    router.stateService.go(`${ROUTE_BASE}.detail`, { capabilityId: capability.id });
  }, [setError, router]);

  const handleStartCreate = useCallback(() => {
    setSelectedCapability(null);
    setSelectedType(null);
    setError(null);
    setCreateStep(0);
    router.stateService.go(`${ROUTE_BASE}.create`);
  }, [setError, router]);

  const handleSelectType = useCallback((type: CapabilityType) => {
    setSelectedType(type);
    setError(null);
  }, [setError]);

  const handleBack = useCallback(() => {
    setSelectedCapability(null);
    setSelectedType(null);
    setError(null);
    router.stateService.go(`${ROUTE_BASE}.list`);
  }, [setError, router]);

  const handleBackToTypeSelect = useCallback(() => {
    setSelectedType(null);
    setError(null);
    setCreateStep(0);
    router.stateService.go(`${ROUTE_BASE}.create`);
  }, [setError, router]);

  const handleWizardStepChange = useCallback((step: number) => {
    if (step === 1 && selectedType) {
      setCreateStep(1);
      router.stateService.go(`${ROUTE_BASE}.createWithType`, { typeId: selectedType.id });
    } else if (step === 0) {
      setCreateStep(0);
      setSelectedType(null);
      handleBackToTypeSelect();
    } else {
      setCreateStep(step);
    }
  }, [selectedType, router, handleBackToTypeSelect]);

  const handleWizardComplete = useCallback(() => {
    // Trigger form submission
    if (formSubmitRef.current) {
      formSubmitRef.current();
    }
  }, []);

  const handleWizardCancel = useCallback(() => {
    if (createStep === 0) {
      handleBack();
    } else {
      handleBackToTypeSelect();
    }
  }, [createStep, handleBack, handleBackToTypeSelect]);

  const handleSave = useCallback(async (data: CapabilityFormData) => {
    try {
      if (viewMode === 'create') {
        const result = await createCapability(data);
        toast.success(`Capability created: ${getCapabilityDescription(result)}`);
      } else if (selectedCapability) {
        const result = await updateCapability(data);
        toast.success(`Capability updated: ${getCapabilityDescription(result)}`);
      }
      setRefreshKey((k) => k + 1);
      handleBack();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to save capability';
      toast.error(message);
      throw err;
    }
  }, [viewMode, selectedCapability, createCapability, updateCapability, handleBack]);

  const handleDelete = useCallback(async () => {
    if (!selectedCapability) return;

    try {
      await deleteCapability(selectedCapability.id);
      toast.success(`Capability deleted: ${getCapabilityDescription(selectedCapability)}`);
      setRefreshKey((k) => k + 1);
      handleBack();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to delete capability';
      toast.error(message);
    }
  }, [selectedCapability, deleteCapability, handleBack]);

  const handleEnable = useCallback(async () => {
    if (!selectedCapability) return;

    try {
      await enableCapability(selectedCapability);
      toast.success(`Capability enabled: ${getCapabilityDescription(selectedCapability)}`);
      setRefreshKey((k) => k + 1);
      setSelectedCapability((prev) => prev ? { ...prev, enabled: true, state: 'active' } : null);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to enable capability';
      toast.error(message);
    }
  }, [selectedCapability, enableCapability]);

  const handleDisable = useCallback(async () => {
    if (!selectedCapability) return;

    try {
      await disableCapability(selectedCapability);
      toast.success(`Capability disabled: ${getCapabilityDescription(selectedCapability)}`);
      setRefreshKey((k) => k + 1);
      // Refresh detail view
      setSelectedCapability((prev) => prev ? { ...prev, enabled: false, state: 'disabled' } : null);
    } catch (err) {
      // Error is set by the API hook
    }
  }, [selectedCapability, disableCapability]);

  // Render header based on view mode
  const renderHeader = () => {
    if (viewMode === 'list') {
      return (
        <Flex justify="between" align="center" className="capabilities-page__header">
          <Flex align="center" gap="3">
            <Puzzle size={24} className="capabilities-page__icon" />
            <Box>
              <Heading as="h1" size="6" weight="medium">Capabilities</Heading>
              <Text size="2" className="capabilities-page__description">
                Manage repository manager capabilities and plugins
              </Text>
            </Box>
          </Flex>
          {canCreate && (
            <SettingsButton variant="primary" onClick={handleStartCreate}>
              <Plus size={16} />
              Create Capability
            </SettingsButton>
          )}
        </Flex>
      );
    }

    // WizardForm handles headers for selectType and create views
    if (viewMode === 'selectType' || viewMode === 'create') {
      return null;
    }

    // Detail view
    return (
      <Flex align="center" gap="3" className="capabilities-page__header">
        <SettingsButton variant="ghost" onClick={handleBack} className="capabilities-page__back">
          <ArrowLeft size={18} />
        </SettingsButton>
        <Box>
          <Heading as="h1" size="6" weight="medium">
            {selectedCapability?.typeName || 'Capability'}
          </Heading>
          <Text size="2" className="capabilities-page__description">
            {selectedCapability?.description || 'View and manage capability settings'}
          </Text>
        </Box>
      </Flex>
    );
  };

  return (
    <Box 
      className={`capabilities-page ${className || ''}`.trim()}
      data-testid="capabilities-page"
      data-view={viewMode}
      data-loading={loading ? 'true' : 'false'}
    >
      {renderHeader()}

      {/* Alerts removed - using Toast notifications instead (Sprint 15 UX fix) */}

      {/* Content */}
      <Box className="capabilities-page__content">
        {viewMode === 'list' && (
          <CapabilitiesList
            onSelect={handleSelectCapability}
            refreshKey={refreshKey}
          />
        )}

        {/* Wizard Form for Create Flow */}
        {(viewMode === 'selectType' || viewMode === 'create') && (
          <WizardForm
            steps={[
              { id: 'type', label: 'Select Type' },
              { id: 'config', label: 'Configure' },
            ]}
            currentStep={createStep}
            onStepChange={handleWizardStepChange}
            onComplete={handleWizardComplete}
            onCancel={handleWizardCancel}
            completeLabel="Create"
            dirty={createStep > 0}
            canAdvance={createStep === 0 ? !!selectedType : formSubmitRef.current !== null}
            loading={loading}
            testId="capability-wizard"
            noDirtyTracking={createStep < 1}
          >
            {createStep === 0 && (
              <CapabilityTypeSelector onSelect={handleSelectType} selectedTypeId={selectedType?.id} />
            )}
            {createStep === 1 && selectedType && (
              <DynamicCapabilityForm
                capabilityType={selectedType}
                isCreate={true}
                onSave={handleSave}
                onCancel={handleBackToTypeSelect}
                onSaveComplete={handleBack}
                loading={loading}
                embedded={true}
                onSubmitRef={(submitFn) => {
                  formSubmitRef.current = submitFn;
                }}
              />
            )}
            {createStep === 1 && !selectedType && params.typeId && (
              <Flex align="center" justify="center" p="6" gap="2">
                <Text size="2">Loading...</Text>
              </Flex>
            )}
          </WizardForm>
        )}

        {viewMode === 'detail' && !selectedCapability && params.capabilityId && (
          <Flex align="center" justify="center" p="6" gap="2">
            <Text size="2">Loading...</Text>
          </Flex>
        )}

        {viewMode === 'detail' && selectedCapability && (
          <CapabilityDetail
            capability={selectedCapability}
            onSave={handleSave}
            onDelete={handleDelete}
            onEnable={handleEnable}
            onDisable={handleDisable}
            onBack={handleBack}
            loading={loading}
            
          />
        )}
      </Box>
    </Box>
  );
}

export default CapabilitiesPage;


