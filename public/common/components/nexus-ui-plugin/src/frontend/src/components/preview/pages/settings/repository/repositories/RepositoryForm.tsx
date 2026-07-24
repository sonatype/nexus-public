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
import { Box, Flex, Text, Tabs } from '@radix-ui/themes';
import {
  Loader2,
  Trash2,
  FileText,
  Settings as SettingsIcon,
  Shield,
  ShieldCheck,
  Database,
  RotateCcw,
  Power,
} from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsCheckbox,
  SettingsButton,
} from '../../../../shared/form';

import { useRepositoryForm } from './useRepositoryForm';
import { RepositoryFormProps } from './types';
import { RepositorySummary } from './RepositorySummary';

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
import { PyPiFacet } from './facets/PyPiFacet';
import { RawFacet } from './facets/RawFacet';
import { RoutingRuleFacet } from './facets/RoutingRuleFacet';
import { RepositoryFirewallConfigTab } from './RepositoryFirewallConfigTab';

import './RepositoryForm.scss';

/**
 * RepositoryForm - Create/Edit form for repositories (Layer 3: Presentation)
 *
 * This component is purely presentational. All business logic, state management,
 * and integration concerns are handled by useRepositoryForm (Layer 2).
 */
export function RepositoryForm({
  repository,
  recipe,
  isCreate,
  onSave,
  onCancel,
  onDelete,
  onRebuildIndex,
  onInvalidateCache,
  onToggleOnline,
  isActionInFlight = false,
  loading,
  error,
  hideActions = false,
  onSubmitRef,
  advanceOnly = false,
  onCanAdvanceChange,
}: RepositoryFormProps & { onSubmitRef?: React.MutableRefObject<(() => void) | null> }) {
  const {
    form,
    hasFirewallLicense,
    isCloud,
    activeTab,
    setActiveTab,
    originChangeWarning,
    setOriginChangeWarning,
    formData,
    pristineData,
    errors,
    blobStores,
    routingRules,
    cleanupPolicies,
    memberRepositories,
    handleChange,
    handleNestedChange,
  } = useRepositoryForm({
    repositoryName: isCreate ? undefined : repository?.name,
    repository: repository || undefined,
    format: recipe.format,
    repositoryType: recipe.type,
    onSave,
    onCancel,
    advanceOnly,
    onSubmitRef,
    onCanAdvanceChange,
  });

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
            isCloud={isCloud}
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

        {/* npm proxies have no extra format-specific config in the new UI: the previous npm
         * Settings section contained only the legacy `removeQuarantinedVersions` checkbox,
         * which is dead post-migration STL-381 (PCCS is now expressed via firewall.mode and
         * the field is stripped by the migration step). The Firewall tab's PCCS button is
         * the canonical way to enable PCCS on npm/pypi proxies. */}

        {recipe.format === 'pypi' && recipe.type === 'proxy' && (
          <PyPiFacet
            formData={formData}
            onNestedChange={handleNestedChange}
          />
        )}

        {recipe.format === 'raw' && (
          <RawFacet
            formData={formData}
            onNestedChange={handleNestedChange}
          />
        )}

        {/* Storage Section — placed before ProxyFacet to match Classic UI order
            (e.g. maven2_proxy: VersionPolicy, LayoutPolicy, ContentDisposition,
            Storage, Proxy, Options, Cleanup, ...). This also positions
            NugetFacet's "Query Cache Item Max Age" correctly relative to
            Storage. */}
        <StorageFacet
          formData={formData}
          onChange={handleChange}
          onNestedChange={handleNestedChange}
          errors={errors}
          isEdit={!isCreate}
          isCloud={isCloud}
          blobStores={blobStores}
        />

        {/* Proxy-specific sections (remote URL, content/metadata max age) */}
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
            memberOptions={memberRepositories}
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
            showPreemptiveAuth={recipe.format === 'maven2' || recipe.format === 'pypi' || recipe.format === 'terraform'}
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
        <Tabs.Root value={activeTab} onValueChange={setActiveTab as (v: string) => void} className="repository-form__tabs">
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
                isCloud={isCloud}
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
                    hasFirewallLicense={hasFirewallLicense}
                    showHealthCheck={false}
                    format={recipe.format}
                  />
                </Tabs.Content>
                <Tabs.Content value="health-check">
                  <RepositoryFirewallConfigTab
                    repositoryName={repository.name}
                    hasFirewallLicense={hasFirewallLicense}
                    showFirewall={false}
                    format={recipe.format}
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

  if (hideActions) {
    return (
      <Box className="repository-form" data-testid="repository-form">
        {formBody}
      </Box>
    );
  }

  // Repository action buttons (edit mode only). Visibility mirrors the Classic UI
  // (RepositoryFeature.js + bindIfProxyOrHosted/Group helpers in Repositories.js):
  //   - Rebuild Index: hosted OR proxy (not group) — backend rejects others.
  //   - Invalidate Cache: proxy OR group (not hosted) — backend rejects hosted.
  //   - Toggle Online: all three repo types.
  //   - Delete: all three repo types.
  // pristineData drives visibility/disabled state because it reflects the saved
  // type from the server, not the (read-only) form value, and does not change
  // while the user edits the form.
  const isEdit = !isCreate;
  const savedType = pristineData?.type ?? recipe.type;
  const showRebuildIndex = isEdit && !!onRebuildIndex && (savedType === 'hosted' || savedType === 'proxy');
  const showInvalidateCache = isEdit && !!onInvalidateCache && (savedType === 'proxy' || savedType === 'group');
  const showToggleOnline = isEdit && !!onToggleOnline;
  const showDelete = isEdit && !!onDelete;
  const hasAnyAction = showRebuildIndex || showInvalidateCache || showToggleOnline || showDelete;

  // Use pristineData.online for the label so it reflects the *saved* status,
  // not the (potentially edited) Online checkbox value. The toggle action
  // affects the persisted state, independent of unsaved form changes.
  const isOnlineSaved = pristineData?.online ?? true;
  // Action buttons are disabled while the form itself is saving, while the
  // page is loading new data, AND while another action (rebuild / invalidate
  // / toggle) is mid-flight. The last condition prevents firing a second POST
  // before the first completes — see RepositoryProfilePage.tsx for the same
  // pattern (`disabled={isExecuting || isDeleting}`).
  const actionsDisabled = !!form.isSaving || !!loading || isActionInFlight;
  // Toggling online does a GET-then-PUT of the saved repository config, so
  // any unsaved edits in the form would be discarded by the refetch that
  // follows the PUT. Block the toggle while the form is dirty rather than
  // silently destroying user input — the user can save (or discard) first
  // and then toggle.
  const toggleOnlineBlockedByDirtyForm = !form.isPristine;
  const toggleOnlineDisabled = actionsDisabled || toggleOnlineBlockedByDirtyForm;
  const toggleOnlineTitle = toggleOnlineBlockedByDirtyForm
    ? 'Save or discard your unsaved changes before toggling system status'
    : isOnlineSaved
      ? 'Take this repository offline'
      : 'Bring this repository online';

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
          hasAnyAction ? (
            <Flex gap="2" wrap="wrap" align="center">
              {showRebuildIndex && (
                <SettingsButton
                  variant="secondary"
                  onClick={onRebuildIndex}
                  disabled={actionsDisabled}
                  icon={Database}
                  testId="form-rebuild-index"
                  data-analytics-id="nxrm-repository-rebuild-index"
                >
                  Rebuild Index
                </SettingsButton>
              )}
              {showInvalidateCache && (
                <SettingsButton
                  variant="secondary"
                  onClick={onInvalidateCache}
                  disabled={actionsDisabled}
                  icon={RotateCcw}
                  testId="form-invalidate-cache"
                  data-analytics-id="nxrm-repository-invalidate-cache"
                >
                  Invalidate Cache
                </SettingsButton>
              )}
              {showToggleOnline && (
                <SettingsButton
                  variant="secondary"
                  onClick={() => onToggleOnline?.(!isOnlineSaved)}
                  disabled={toggleOnlineDisabled}
                  icon={Power}
                  testId="form-toggle-online"
                  data-analytics-id="nxrm-repository-toggle-online"
                  title={toggleOnlineTitle}
                >
                  {isOnlineSaved ? 'Disable System Status' : 'Enable System Status'}
                </SettingsButton>
              )}
              {showDelete && (
                <SettingsButton
                  variant="danger"
                  onClick={onDelete}
                  disabled={actionsDisabled}
                  icon={Trash2}
                  testId="form-delete"
                  data-analytics-id="nxrm-repository-delete"
                >
                  Delete
                </SettingsButton>
              )}
            </Flex>
          ) : undefined
        }
      >
        {formBody}
      </SettingsForm>
    </Box>
  );
}
