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

import React, { useRef, useState } from 'react';
import { Box, Flex, Text, Tabs } from '@radix-ui/themes';
import {
  Loader2,
  Trash2,
  FileText,
  Settings as SettingsIcon,
  Shield,
  ShieldCheck,
  ShieldQuestion,
  Database,
  RotateCcw,
  Power,
  ScrollText,
  Activity,
  FolderTree,
  ListChecks,
  Lock,
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
import { ExtJS } from '../../../../../../interface/ExtJS';

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
import { RepositoryAuditTab } from './RepositoryAuditTab';
import { RepositoryTasksCapabilitiesTab } from './RepositoryTasksCapabilitiesTab';
import { RepositoryAccessSecurityTab } from './RepositoryAccessSecurityTab';
import { RepositoryEvaluationTab } from './RepositoryEvaluationTab';
import { isEvaluationFeatureEnabled } from './useRepoEvaluationOverride';
import { RepositorySettingsUsageTab } from './RepositorySettingsUsageTab';
import type { RepositoryUsageKind } from './repositoryUsageMachine';
import { isFeatureEnabled } from '../../../../config/featureFlags';

const USAGE_TAB_FLAG = 'repository.settings.usageTab';

import './RepositoryForm.scss';

function toRepositoryUsageKind(type: string): RepositoryUsageKind {
  if (type === 'group' || type === 'proxy' || type === 'hosted') return type;
  console.warn(`RepositoryForm: unrecognized repository type "${type}", defaulting Usage tab to hosted behavior`);
  return 'hosted';
}

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
  onBrowseRepository,
  isActionInFlight = false,
  loading,
  error,
  hideActions = false,
  onSubmitRef,
  advanceOnly = false,
  onCanAdvanceChange,
  onDirtyChange,
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
    onDirtyChange,
  });

  // Per-repo Evaluation tab — gated by hostedRepositoryEvaluationEnabled flag.
  const showEvaluationTab =
    !isCreate && !!repository && recipe.type === 'hosted' && isEvaluationFeatureEnabled();

  // Evaluation tab plugs into the page-level "Save Changes" / "Cancel" toolbar
  // when active, so the buttons inside that tab's card stay hidden.
  const [evalDirty, setEvalDirty] = useState(false);
  const [evalSaving, setEvalSaving] = useState(false);
  const evalSaveRef = useRef<(() => Promise<void>) | null>(null);
  const evalCancelRef = useRef<(() => void) | null>(null);
  const isEvalTabActive = showEvaluationTab && activeTab === 'evaluation';

  // Determine which facets to show based on repository type
  const showProxyFacets = recipe.type === 'proxy';
  const showHostedFacets = recipe.type === 'hosted';
  const showGroupFacets = recipe.type === 'group';
  const showCleanupFacet = recipe.type !== 'group';
  const showRoutingRuleFacet = recipe.type === 'proxy';
  // Provider-independent, reactive permission checks (NEXUS-54212). ExtJS.checkPermission on its
  // own evaluates once at render and would briefly hide the Save button / gated tabs for a
  // permitted user if permissions load asynchronously after mount; ExtJS.usePermission with a
  // hasUser dependency re-evaluates once the user and their permissions arrive. These hooks live
  // above the early returns below so hook order stays stable across renders.
  const hasUser = ExtJS.useUser() ?? false;
  const auditEnabled = ExtJS.state()?.getValue?.('previewAuditEnabled') || false;
  const hasAuditRead = ExtJS.usePermission(
    () => ExtJS.checkPermission('nexus:audit:read'),
    [hasUser],
  );
  // Tasks & Capabilities tab is visible if the user can read EITHER resource;
  // sections inside the tab are individually gated on their own permission.
  const hasTasksOrCapabilitiesRead = ExtJS.usePermission(
    () =>
      ExtJS.checkPermission('nexus:tasks:read') ||
      ExtJS.checkPermission('nexus:capabilities:read'),
    [hasUser],
  );
  // Access & Security tab is visible if the user can read ANY of the four
  // resources it surfaces; sections inside the tab are individually gated.
  const hasAccessSecurityRead = ExtJS.usePermission(
    () =>
      ExtJS.checkPermission('nexus:privileges:read') ||
      ExtJS.checkPermission('nexus:roles:read') ||
      ExtJS.checkPermission('nexus:users:read') ||
      ExtJS.checkPermission('nexus:settings:read'),
    [hasUser],
  );
  // NEXUS-54212: match Classic UI (RepositorySettingsForm.js), which gates the Save/Update button
  // on the *per-repository* edit permission — nexus:repository-admin:{format}:{name}:edit — not the
  // wildcard. A user with repository-admin:read (enough to open the detail) but no :edit for this
  // repo sees a read-only form with no Save button, instead of a button that 403s on submit.
  // ExtJS.checkPermission applies Shiro wildcard semantics, so a holder of the *:*:edit wildcard
  // still satisfies the concrete check. Create mode is already gated on repository-admin:*:*:add at
  // navigation, so submit stays enabled there. Computed here (with the other permission hooks)
  // rather than inline at the button, but only consumed after the loading early-return below.
  const canEdit = ExtJS.usePermission(
    () =>
      isCreate ||
      (!!repository &&
        ExtJS.checkPermission(`nexus:repository-admin:${recipe.format}:${repository.name}:edit`)),
    [hasUser, isCreate, repository?.name, recipe.format],
  );

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

        {/* npm proxies: no format-specific config in new UI. Use Firewall tab's PCCS button instead. */}

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

        {/* Storage — before ProxyFacet to match Classic UI order. */}
        <StorageFacet
          formData={formData}
          onChange={handleChange}
          onNestedChange={handleNestedChange}
          errors={errors}
          isEdit={!isCreate}
          isCloud={isCloud}
          blobStores={blobStores}
        />

        {/* Proxy sections */}
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

        {/* Hosted sections */}
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

        {/* Routing Rule + Negative Cache */}
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
            {isFeatureEnabled(USAGE_TAB_FLAG) && (
              <Tabs.Trigger value="usage">
                <Activity size={16} /> Usage
              </Tabs.Trigger>
            )}
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
            {auditEnabled && hasAuditRead && (
              <Tabs.Trigger value="audit">
                <ScrollText size={16} /> Audit
              </Tabs.Trigger>
            )}
            {showEvaluationTab && (
              <Tabs.Trigger value="evaluation">
                <ShieldQuestion size={16} /> Evaluation
              </Tabs.Trigger>
            )}
            {hasTasksOrCapabilitiesRead && (
              <Tabs.Trigger value="tasks-capabilities">
                <ListChecks size={16} /> Tasks &amp; Capabilities
              </Tabs.Trigger>
            )}
            {hasAccessSecurityRead && (
              <Tabs.Trigger value="access-security">
                <Lock size={16} /> Access &amp; Security
              </Tabs.Trigger>
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

            {isFeatureEnabled(USAGE_TAB_FLAG) && (
              <Tabs.Content value="usage">
                <RepositorySettingsUsageTab
                  repositoryName={repository.name}
                  repositoryType={toRepositoryUsageKind(repository.type)}
                />
              </Tabs.Content>
            )}

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
            {auditEnabled && hasAuditRead && (
              <Tabs.Content value="audit">
                <RepositoryAuditTab
                  repositoryName={repository.name}
                />
              </Tabs.Content>
            )}
            {showEvaluationTab && repository && (
              <Tabs.Content value="evaluation">
                <RepositoryEvaluationTab
                  repositoryName={repository.name}
                  hideActions
                  onDirtyChange={setEvalDirty}
                  onSavingChange={setEvalSaving}
                  onSaveRef={evalSaveRef}
                  onCancelRef={evalCancelRef}
                />
              </Tabs.Content>
            )}
            {hasTasksOrCapabilitiesRead && (
              <Tabs.Content value="tasks-capabilities">
                <RepositoryTasksCapabilitiesTab
                  repositoryName={repository.name}
                />
              </Tabs.Content>
            )}
            {hasAccessSecurityRead && (
              <Tabs.Content value="access-security">
                <RepositoryAccessSecurityTab
                  repositoryName={repository.name}
                  repositoryFormat={recipe.format}
                />
              </Tabs.Content>
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

  // Action buttons (edit mode): Rebuild Index (hosted/proxy), Invalidate Cache (proxy/group), Toggle/Delete (all).
  const isEdit = !isCreate;
  // canEdit is computed with the other permission hooks near the top of the component.
  const savedType = pristineData?.type ?? recipe.type;
  const showBrowseRepository = isEdit && !!onBrowseRepository;
  // Rebuild Index, Invalidate Cache, and Toggle Online mutate the repository, so gate them on the
  // same per-repo edit permission as the Save button (NEXUS-54212). Browse is read-only navigation
  // and Delete has its own delete permission, so neither is gated on canEdit here. The callers in
  // RepositoriesPage already only pass these handlers to holders of the repository-admin edit
  // wildcard; this is defense-in-depth so the component never shows a write action it can't authorize.
  const showRebuildIndex = isEdit && canEdit && !!onRebuildIndex && (savedType === 'hosted' || savedType === 'proxy');
  const showInvalidateCache = isEdit && canEdit && !!onInvalidateCache && (savedType === 'proxy' || savedType === 'group');
  const showToggleOnline = isEdit && canEdit && !!onToggleOnline;
  const showDelete = isEdit && !!onDelete;
  const hasAnyAction = showBrowseRepository || showRebuildIndex || showInvalidateCache || showToggleOnline || showDelete;

  const isOnlineSaved = pristineData?.online ?? true;
  const actionsDisabled = !!form.isSaving || !!loading || isActionInFlight;
  // Toggling online does a GET-then-PUT of the saved repository config, so
  // any unsaved edits in the form would be discarded by the refetch that
  // follows the PUT. Block the toggle while the form is dirty rather than
  // silently destroying user input — the user can save (or discard) first
  // and then toggle.
  const toggleOnlineBlockedByDirtyForm = !form.isPristine || (isEvalTabActive && evalDirty);
  const toggleOnlineDisabled = actionsDisabled || toggleOnlineBlockedByDirtyForm;
  const toggleOnlineTitle = toggleOnlineBlockedByDirtyForm
    ? 'Save or discard your unsaved changes before toggling system status'
    : isOnlineSaved
      ? 'Take this repository offline'
      : 'Bring this repository online';
  // Browse Repository navigates away from the edit page; unsaved edits would
  // be silently lost. Block navigation while the form is dirty for the same
  // reason we block the toggle above.
  const browseRepositoryDisabled = actionsDisabled || toggleOnlineBlockedByDirtyForm;
  const browseRepositoryTitle = toggleOnlineBlockedByDirtyForm
    ? 'Save or discard your unsaved changes before browsing the repository'
    : undefined;

  return (
    <Box className="repository-form">
      <SettingsForm
        onSubmit={
          !canEdit
            ? undefined
            : isEvalTabActive
              ? () => { evalSaveRef.current?.(); }
              : () => form.send('SUBMIT')
        }
        onCancel={isEvalTabActive ? () => { evalCancelRef.current?.(); onCancel?.(); } : onCancel}
        loading={isEvalTabActive ? evalSaving || loading : form.isSaving || loading}
        pristine={isEvalTabActive ? !evalDirty : form.isPristine}
        error={error || form.saveError || undefined}
        submitLabel={isCreate ? 'Create Repository' : 'Save Changes'}
        cancelLabel="Cancel"
        testId="repository-form"
        submitAnalyticsId={isCreate ? 'nxrm-repository-create' : 'nxrm-repository-save'}
        cancelAnalyticsId="nxrm-repository-cancel"
        footerExtra={
          hasAnyAction ? (
            <Flex gap="2" wrap="wrap" align="center">
              {showBrowseRepository && (
                <SettingsButton
                  variant="secondary"
                  onClick={onBrowseRepository}
                  disabled={browseRepositoryDisabled}
                  icon={FolderTree}
                  testId="form-browse-repository"
                  data-analytics-id="nxrm-repository-browse"
                  title={browseRepositoryTitle}
                >
                  Browse Repository
                </SettingsButton>
              )}
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

