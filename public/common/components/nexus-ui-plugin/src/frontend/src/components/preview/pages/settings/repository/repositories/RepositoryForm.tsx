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

import React, { useCallback, useMemo, useEffect, useState } from 'react';
import { Box, Flex, Text, Tabs } from '@radix-ui/themes';
import { Loader2, Trash2, FileText, Settings as SettingsIcon, Shield, ShieldCheck } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsCheckbox,
  SettingsButton,
  SettingsAlert,
} from '../../../../shared/form';

import { useRepositoriesApi } from './useRepositoriesApi';
import { useRepositoryForm } from './useRepositoryForm';
import { validateRepository } from './repositoryFormMachine';
import { hasFormErrors } from './types';
import { RepositorySummary } from './RepositorySummary';
import {
  Repository,
  RepositoryFormData,
  RepositoryFormProps,
  BlobStore,
  RoutingRule,
  CleanupPolicy,
  RepositoryReference,
} from './types';

// Import facets
import { StorageFacet } from './facets/StorageFacet';
import { ProxyFacet } from './facets/ProxyFacet';
import { HostedFacet } from './facets/HostedFacet';
import { GroupFacet } from './facets/GroupFacet';
import { HttpClientFacet } from './facets/HttpClientFacet';
import { NegativeCacheFacet } from './facets/NegativeCacheFacet';
import { CleanupFacet } from './facets/CleanupFacet';
import { MavenFacet } from './facets/MavenFacet';
import { DockerFacet } from './facets/DockerFacet';
import { AptFacet } from './facets/AptFacet';
import { AlpineFacet } from './facets/AlpineFacet';
import { YumFacet } from './facets/YumFacet';
import { NugetFacet } from './facets/NugetFacet';
import { NpmFacet } from './facets/NpmFacet';
import { PyPiFacet } from './facets/PyPiFacet';
import { RawFacet } from './facets/RawFacet';
import { RoutingRuleFacet } from './facets/RoutingRuleFacet';
import { RepositoryFirewallConfigTab } from './RepositoryFirewallConfigTab';

import './RepositoryForm.scss';

function hasFirewallLicense(): boolean {
  try {
    const clm = ExtJS.state()?.getValue?.('clm');
    return !!(clm?.enabled ?? clm?.hasFirewall);
  } catch {
    return false;
  }
}

/**
 * RepositoryForm - Create/Edit form for repositories
 * Now uses XState for state management via useRepositoryForm hook
 */
export function RepositoryForm({
  repository,
  recipe,
  isCreate,
  onSave,
  onCancel,
  onDelete,
  loading,
  error,
  hideActions = false,
  onSubmitRef,
  advanceOnly = false,
  onCanAdvanceChange,
}: RepositoryFormProps & { onSubmitRef?: React.MutableRefObject<(() => void) | null> }) {
  const { createRepository, updateRepository } = useRepositoriesApi();
  const [activeTab, setActiveTab] = useState(isCreate ? 'summary' : 'settings');
  const [originChangeWarning, setOriginChangeWarning] = useState(false);

  // Use XState form hook
  const { form, repository: loadedRepository } = useRepositoryForm({
    repositoryName: isCreate ? undefined : repository?.name,
    repository: repository || undefined,
    format: recipe.format,
    repositoryType: recipe.type,
    onSave,
    onCancel,
    createRepository,
    updateRepository,
    advanceOnly,
  });

  const isCloud = ExtJS.state?.().getValue?.('isCloud', false) ?? false;

  // Expose submit function to parent via ref (for WizardForm integration)
  useEffect(() => {
    if (onSubmitRef) {
      onSubmitRef.current = () => {
        if (!form.isLoading && !form.isSaving) {
          if (advanceOnly && onSave) {
            // Bypass the machine's SUBMIT → saved (final) transition.
            // Validate manually and call onSave directly so the machine stays
            // in 'editing' state, keeping the form editable if the user navigates back.
            const errors = validateRepository(form.data as RepositoryFormData, { isCloud });
            if (!hasFormErrors(errors)) {
              onSave(form.data as RepositoryFormData);
            } else {
              // Show validation errors via normal machine flow (stays in editing due to errors)
              form.send('SUBMIT');
            }
          } else {
            form.send('SUBMIT');
          }
        }
      };
      return () => { onSubmitRef.current = null; };
    }
  }, [onSubmitRef, form, form.isLoading, form.isSaving, advanceOnly, onSave, isCloud]);

  // Report form validity for wizard Next button (step 2)
  useEffect(() => {
    if (!onCanAdvanceChange) return;
    if (form.isLoading) {
      onCanAdvanceChange(false);
      return;
    }
    if (form.data) {
      const errors = validateRepository(form.data as RepositoryFormData, { isCloud });
      onCanAdvanceChange(!hasFormErrors(errors));
    }
  }, [form.data, form.isLoading, onCanAdvanceChange, isCloud]);

  // Access extended context for reference data
  const context = form.state.context as any;
  const pristineData = context.pristineData as RepositoryFormData | undefined;
  const formData = form.data as RepositoryFormData;
  const blobStores: BlobStore[] = context.blobStores || [];
  const routingRules: RoutingRule[] = context.routingRules || [];
  const cleanupPolicies: CleanupPolicy[] = context.cleanupPolicies || [];
  const memberOptions: RepositoryReference[] = context.memberRepositories || [];

  // Bridge functions for facets - translate form.send() to the onChange/onNestedChange pattern
  const handleChange = useCallback((updates: Partial<RepositoryFormData>) => {
    Object.entries(updates).forEach(([key, value]) => {
      form.send({ type: 'UPDATE', name: key, value });
    });
  }, [form]);

  const handleNestedChange = useCallback(<K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => {
    const current = (formData as any)[key] || {};
    form.send({ type: 'UPDATE', name: key as string, value: { ...current, ...updates } });
  }, [form, formData]);

  // Validation errors from machine
  const errors = form.validationErrors || {};

  // Determine which facets to show based on repository type
  const showProxyFacets = recipe.type === 'proxy';
  const showHostedFacets = recipe.type === 'hosted';
  const showGroupFacets = recipe.type === 'group';
  const showCleanupFacet = recipe.type !== 'group';
  const showRoutingRuleFacet = recipe.type === 'proxy';

  // Loading state for reference data
  if (form.isLoading) {
    return (
      <Flex align="center" justify="center" className="repository-form__loading">
        <Loader2 size={24} className="repository-form__spinner" />
        <Text size="2">Loading form data...</Text>
      </Flex>
    );
  }

  // Form content
  const formContent = (
    <Box>
      {/* Repository Info Section */}
        <SettingsFormSection title="Repository Settings">
          <SettingsTextInput
            {...form.field('name')}
            label="Name"
            disabled={!isCreate}
            required
            helpText={isCreate ? 'A unique identifier for this repository' : 'Repository name cannot be changed'}
          />

          <SettingsCheckbox
            name="repo-online"
            label="Online"
            checked={formData.online ?? true}
            onChange={(checked) => handleChange({ online: checked })}
            description="Allow clients to access this repository"
          />
        </SettingsFormSection>

        {/* Format-Specific Sections - Rendered First to Match Classic UI */}
        {recipe.format === 'maven2' && recipe.type !== 'group' && (
          <MavenFacet
            formData={formData}
            onNestedChange={handleNestedChange}
            errors={errors}
            isEdit={!isCreate}
          />
        )}

        {recipe.format === 'docker' && (
          <DockerFacet
            formData={formData}
            onNestedChange={handleNestedChange}
            errors={errors}
            repoType={recipe.type}
          />
        )}

        {recipe.format === 'apt' && (
          <AptFacet
            formData={formData}
            onNestedChange={handleNestedChange}
            errors={errors}
            repoType={recipe.type}
          />
        )}

        {recipe.format === 'alpine' && (
          <AlpineFacet
            formData={formData}
            onNestedChange={handleNestedChange}
            errors={errors}
            repoType={recipe.type}
          />
        )}

        {recipe.format === 'yum' && (
          <YumFacet
            formData={formData}
            onNestedChange={handleNestedChange}
            errors={errors}
            repoType={recipe.type}
          />
        )}

        {recipe.format === 'nuget' && recipe.type === 'proxy' && (
          <NugetFacet
            formData={formData}
            onNestedChange={handleNestedChange}
            errors={errors}
          />
        )}

        {recipe.format === 'npm' && recipe.type === 'proxy' && (
          <NpmFacet
            formData={formData}
            onNestedChange={handleNestedChange}
            showFirewallFeatures={hasFirewallLicense()}
          />
        )}

        {recipe.format === 'pypi' && recipe.type === 'proxy' && (
          <PyPiFacet
            formData={formData}
            onNestedChange={handleNestedChange}
            showFirewallFeatures={hasFirewallLicense()}
          />
        )}

        {recipe.format === 'raw' && (
          <RawFacet
            formData={formData}
            onNestedChange={handleNestedChange}
          />
        )}

        {/* Proxy-specific sections */}
        {showProxyFacets && (
          <ProxyFacet
            formData={formData}
            onChange={handleChange}
            onNestedChange={handleNestedChange}
            errors={errors}
            format={recipe.format}
            originChangeWarning={originChangeWarning}
          />
        )}

        {/* Storage Section */}
        <StorageFacet
          formData={formData}
          onChange={handleChange}
          onNestedChange={handleNestedChange}
          errors={errors}
          isEdit={!isCreate}
          isCloud={isCloud}
          blobStores={blobStores}
        />

        {/* Hosted-specific sections - after Storage to match Classic UI order */}
        {showHostedFacets && (
          <HostedFacet
            formData={formData}
            onChange={handleChange}
            onNestedChange={handleNestedChange}
            errors={errors}
          />
        )}

        {/* Group-specific sections */}
        {showGroupFacets && (
          <GroupFacet
            formData={formData}
            onChange={handleChange}
            onNestedChange={handleNestedChange}
            errors={errors}
            memberOptions={memberOptions}
            format={recipe.format}
          />
        )}

        {/* Routing Rule + Negative Cache - after proxy/storage to match Classic UI */}
        {showRoutingRuleFacet && (
          <RoutingRuleFacet
            formData={formData}
            onChange={handleChange}
            errors={errors}
            routingRules={routingRules}
          />
        )}

        {showProxyFacets && (
          <NegativeCacheFacet
            formData={formData}
            onChange={handleChange}
            onNestedChange={handleNestedChange}
            errors={errors}
          />
        )}

        {/* Cleanup Policy Section */}
        {showCleanupFacet && (
          <CleanupFacet
            formData={formData}
            onChange={handleChange}
            onNestedChange={handleNestedChange}
            errors={errors}
            cleanupPolicies={cleanupPolicies}
          />
        )}

        {/* HTTP Client Section */}
        {showProxyFacets && (
          <HttpClientFacet
            formData={formData}
            onChange={handleChange}
            onNestedChange={handleNestedChange}
            errors={errors}
            showPreemptiveAuth={recipe.format === 'maven2'}
            originalRemoteUrl={pristineData?.proxy?.remoteUrl}
            isEdit={!isCreate}
            hadAuthOnLoad={!!pristineData?.httpClient?.authentication?.type}
            onOriginChangeWarning={setOriginChangeWarning}
            format={recipe.format}
          />
        )}
    </Box>
  );

  // Wrapped form body with tabs for edit mode
  const formBody = (
    <>
      {!isCreate && repository ? (
        <Tabs.Root value={activeTab} onValueChange={setActiveTab} className="repository-form__tabs">
          <Tabs.List>
            <Tabs.Trigger value="summary">
              <FileText size={16} /> Summary
            </Tabs.Trigger>
            <Tabs.Trigger value="settings">
              <SettingsIcon size={16} /> Settings
            </Tabs.Trigger>
            {recipe.type === 'proxy' && (
              <>
                <Tabs.Trigger value="firewall">
                  <Shield size={16} /> Firewall
                </Tabs.Trigger>
                <Tabs.Trigger value="health-check">
                  <ShieldCheck size={16} /> Health Check
                </Tabs.Trigger>
              </>
            )}
          </Tabs.List>

          <Box className="repository-form__tab-content">
            <Tabs.Content value="summary">
              <RepositorySummary
                repository={repository}
                formData={formData}
                onNavigateToTab={setActiveTab}
              />
            </Tabs.Content>

            <Tabs.Content value="settings">
              {formContent}
            </Tabs.Content>

            {recipe.type === 'proxy' && (
              <>
                <Tabs.Content value="firewall">
                  <RepositoryFirewallConfigTab
                    repositoryName={repository.name}
                    hasFirewallLicense={hasFirewallLicense()}
                    showHealthCheck={false}
                  />
                </Tabs.Content>
                <Tabs.Content value="health-check">
                  <RepositoryFirewallConfigTab
                    repositoryName={repository.name}
                    hasFirewallLicense={hasFirewallLicense()}
                    showFirewall={false}
                  />
                </Tabs.Content>
              </>
            )}
          </Box>
        </Tabs.Root>
      ) : (
        formContent
      )}
    </>
  );

  // When hideActions is true, don't wrap in SettingsForm (WizardForm provides the wrapper)
  // The form submission will be handled by WizardForm's onComplete callback
  // We expose the submit handler via a data attribute so WizardForm can trigger it
  if (hideActions) {
    return (
      <Box className="repository-form" data-testid="repository-form">
        {formBody}
      </Box>
    );
  }

  // Normal mode - wrap in SettingsForm
  return (
    <Box className="repository-form">
      <SettingsForm
        onSubmit={() => form.send('SUBMIT')}
        onCancel={onCancel}
        loading={form.isSaving || loading}
        pristine={form.isPristine}
        error={error || form.saveError || undefined}
        submitLabel={isCreate ? 'Create Repository' : 'Save Changes'}
        cancelLabel="Cancel"
        testId="repository-form"
        submitAnalyticsId={isCreate ? 'nxrm-repository-create' : 'nxrm-repository-save'}
        cancelAnalyticsId="nxrm-repository-cancel"
        footerExtra={
          !isCreate && onDelete ? (
            <SettingsButton
              variant="danger"
              onClick={onDelete}
              disabled={form.isSaving || loading}
              icon={Trash2}
              testId="form-delete"
              data-analytics-id="nxrm-repository-delete"
            >
              Delete
            </SettingsButton>
          ) : undefined
        }
      >
        {formBody}
      </SettingsForm>
    </Box>
  );
}

export default RepositoryForm;
