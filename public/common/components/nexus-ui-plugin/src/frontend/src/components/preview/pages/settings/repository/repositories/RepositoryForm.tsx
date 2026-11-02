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

import React, { useRef, useState, useCallback } from 'react';
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
import { ConfirmDialog } from '../../../../shared/form/ConfirmDialog';

import { isFirewallSupportedFormat } from '../../../../../../utils/firewallFormats';
import { isHostedRepoEvaluationSupportedFormat } from '../../../../../../utils/hostedRepoEvaluationFormats';

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
import { OciFacet } from './facets/OciFacet';
import { AptFacet } from './facets/AptFacet';
import { AlpineFacet } from './facets/AlpineFacet';
import { YumFacet } from './facets/YumFacet';
import { TerraformFacet } from './facets/TerraformFacet';
import { NugetFacet } from './facets/NugetFacet';
import { CargoFacet } from './facets/CargoFacet';
import { HuggingFaceFacet } from './facets/HuggingFaceFacet';
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
import UIStrings from '../../../../../../constants/pages/admin/repository/RepositoriesStrings';

import './RepositoryForm.scss';

const FORM_STRINGS = UIStrings.REPOSITORY_FORM;

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

  // Per-repo Evaluation tab — gated by hostedRepositoryEvaluationEnabled flag, and hidden for formats HRS can't
  // monitor. Already-monitored ones can still be disabled from the HRS Repositories tab.
  const showEvaluationTab =
    !isCreate && !!repository && recipe.type === 'hosted' && isEvaluationFeatureEnabled() &&
    isHostedRepoEvaluationSupportedFormat(recipe.format);

  // Evaluation tab plugs into the page-level "Save Changes" / "Cancel" toolbar
  // when active, so the buttons inside that tab's card stay hidden.
  const [evalDirty, setEvalDirty] = useState(false);
  const [evalSaving, setEvalSaving] = useState(false);
  const evalSaveRef = useRef<(() => Promise<void>) | null>(null);
  const evalCancelRef = useRef<(() => void) | null>(null);

  // Firewall tab plugs into the same toolbar (FIRE-958): selecting a protection level only
  // stages a pending change, and Save Changes / Cancel commit or discard it via these refs.
  const [firewallDirty, setFirewallDirty] = useState(false);
  const [firewallSaving, setFirewallSaving] = useState(false);
  const firewallSaveRef = useRef<((onCommitted?: () => void) => Promise<void>) | null>(null);
  const firewallCancelRef = useRef<(() => void) | null>(null);

  const [xetConfirmOpen, setXetConfirmOpen] = useState(false);

  const handleSubmit = useCallback(() => {
    const xetBeingEnabled =
      !isCreate &&
      recipe.format === 'huggingface' &&
      recipe.type === 'proxy' &&
      formData.xet?.enabled === true &&
      pristineData?.xet?.enabled !== true;
    if (xetBeingEnabled) {
      setXetConfirmOpen(true);
    } else {
      form.send('SUBMIT');
    }
  }, [isCreate, recipe.format, recipe.type, formData.xet?.enabled, pristineData?.xet?.enabled, form]);

  // Save Changes on the Firewall tab also saves pending edits from the Settings tab (e.g. turning
  // XET off). The firewall level is committed first, and the main form is submitted only once it
  // has been applied, so a cancelled confirmation or failed commit leaves both changes pending.
  // Committing first also means the form's save (which navigates away) runs last.
  const handleFirewallTabSave = () => {
    if (form.isPristine) {
      firewallSaveRef.current?.();
      return;
    }
    if (form.hasValidationErrors) {
      // Surface the Settings-tab errors instead of committing only half of the change.
      setActiveTab('settings');
      form.send('SUBMIT');
      return;
    }
    firewallSaveRef.current?.(handleSubmit);
  };

  // Lookup of per-tab dirty/saving state + save/cancel handlers for tabs that plug into the
  // page-level toolbar instead of the form machine's own SUBMIT/pristine. Keyed by activeTab
  // value; only populated for tabs that are actually rendered for this repository (mirrors the
  // Tabs.Content gating below) so a stale activeTab value can never activate a hidden tab's
  // save/cancel handlers. Tabs not present here (e.g. Settings) go through the normal form
  // machine path.
  const showFirewallTab = recipe.type === 'proxy' && isFirewallSupportedFormat(recipe.format);
  const tabControls: Record<
    string,
    { dirty: boolean; saving: boolean; onSave: () => void; onCancel: () => void }
  > = {
    ...(showEvaluationTab && {
      evaluation: {
        dirty: evalDirty,
        saving: evalSaving || !!form.isSaving,
        // With no staged evaluation change, Save Changes saves the pending Settings-tab edits.
        onSave: () => { if (evalDirty) evalSaveRef.current?.(); else handleSubmit(); },
        onCancel: () => { evalCancelRef.current?.(); },
      },
    }),
    ...(showFirewallTab && {
      firewall: {
        dirty: firewallDirty,
        saving: firewallSaving || !!form.isSaving,
        onSave: handleFirewallTabSave,
        onCancel: () => { firewallCancelRef.current?.(); },
      },
    }),
  };
  const activeTabControls = tabControls[activeTab as string];

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
        <Text size="2">{FORM_STRINGS.loading}</Text>
      </Flex>
    );
  }

  // Form content
  const formContent = (
    <Box>
      {/* Repository Info Section */}
        <SettingsFormSection title={FORM_STRINGS.SETTINGS.sectionTitle}>
          <SettingsTextInput
            {...form.field('name')}
            label={FORM_STRINGS.SETTINGS.nameLabel}
            disabled={!isCreate}
            required
            helpText={isCreate ? FORM_STRINGS.SETTINGS.nameHelpCreate : FORM_STRINGS.SETTINGS.nameHelpEdit}
          />

          <SettingsCheckbox
            name="repo-online"
            label={FORM_STRINGS.SETTINGS.onlineLabel}
            checked={formData.online ?? true}
            onChange={(checked) => handleChange({ online: checked })}
            description={FORM_STRINGS.SETTINGS.onlineDescription}
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

        {recipe.format === 'oci' && (
          <OciFacet
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

        {recipe.format === 'terraform' && (
          <TerraformFacet
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

        {/* Cargo: applies to all three types — the backend supports
            cargo.requireAuthentication for proxy/group (NEXUS-44559) and hosted
            (NEXUS-55006), so there is no type gate here. */}
        {recipe.format === 'cargo' && (
          <CargoFacet
            formData={formData}
            onNestedChange={handleNestedChange}
            isCloud={isCloud}
          />
        )}

        {/* npm proxies: no format-specific config in new UI. Use Firewall tab's PCCS button instead. */}

        {recipe.format === 'huggingface' && recipe.type === 'proxy' && (
          <HuggingFaceFacet
            formData={formData}
            onNestedChange={handleNestedChange}
          />
        )}

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
            showPreemptiveAuth={recipe.format === 'maven2' || recipe.format === 'pypi' || recipe.format === 'terraform' || recipe.format === 'conda' || recipe.format === 'openvsx'}
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
              <FileText size={16} /> {FORM_STRINGS.TABS.summary}
            </Tabs.Trigger>
            <Tabs.Trigger value="settings">
              <SettingsIcon size={16} /> {FORM_STRINGS.TABS.settings}
            </Tabs.Trigger>
            <Tabs.Trigger value="usage">
              <Activity size={16} /> {FORM_STRINGS.TABS.usage}
            </Tabs.Trigger>
            {recipe.type === 'proxy' && (
              <>
                {isFirewallSupportedFormat(recipe.format) && (
                  <Tabs.Trigger value="firewall">
                    <Shield size={16} /> {FORM_STRINGS.TABS.firewall}
                  </Tabs.Trigger>
                )}
                <Tabs.Trigger value="health-check">
                  <ShieldCheck size={16} /> {FORM_STRINGS.TABS.healthCheck}
                </Tabs.Trigger>
              </>
            )}
            {auditEnabled && hasAuditRead && (
              <Tabs.Trigger value="audit">
                <ScrollText size={16} /> {FORM_STRINGS.TABS.audit}
              </Tabs.Trigger>
            )}
            {showEvaluationTab && (
              <Tabs.Trigger value="evaluation">
                <ShieldQuestion size={16} /> {FORM_STRINGS.TABS.evaluation}
              </Tabs.Trigger>
            )}
            {hasTasksOrCapabilitiesRead && (
              <Tabs.Trigger value="tasks-capabilities">
                <ListChecks size={16} /> {FORM_STRINGS.TABS.tasksCapabilities}
              </Tabs.Trigger>
            )}
            {hasAccessSecurityRead && (
              <Tabs.Trigger value="access-security">
                <Lock size={16} /> {FORM_STRINGS.TABS.accessSecurity}
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

            <Tabs.Content value="usage">
              <RepositorySettingsUsageTab
                repositoryName={repository.name}
                repositoryType={toRepositoryUsageKind(repository.type)}
              />
            </Tabs.Content>

            {recipe.type === 'proxy' && (
              <>
                {isFirewallSupportedFormat(recipe.format) && (
                  <Tabs.Content value="firewall">
                    <RepositoryFirewallConfigTab
                      repositoryName={repository.name}
                      hasFirewallLicense={hasFirewallLicense}
                      showHealthCheck={false}
                      format={recipe.format}
                      onDirtyChange={setFirewallDirty}
                      onSavingChange={setFirewallSaving}
                      onSaveRef={firewallSaveRef}
                      onCancelRef={firewallCancelRef}
                      xetEnabled={formData.xet?.enabled}
                    />
                  </Tabs.Content>
                )}
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
  const toggleOnlineBlockedByDirtyForm = !form.isPristine || (activeTabControls?.dirty ?? false);
  const toggleOnlineDisabled = actionsDisabled || toggleOnlineBlockedByDirtyForm;
  const toggleOnlineTitle = toggleOnlineBlockedByDirtyForm
    ? FORM_STRINGS.TOOLTIPS.toggleBlockedByDirty
    : isOnlineSaved
      ? FORM_STRINGS.TOOLTIPS.takeOffline
      : FORM_STRINGS.TOOLTIPS.bringOnline;
  // Browse Repository navigates away from the edit page; unsaved edits would
  // be silently lost. Block navigation while the form is dirty for the same
  // reason we block the toggle above.
  const browseRepositoryDisabled = actionsDisabled || toggleOnlineBlockedByDirtyForm;
  const browseRepositoryTitle = toggleOnlineBlockedByDirtyForm
    ? FORM_STRINGS.TOOLTIPS.browseBlockedByDirty
    : undefined;

  return (
    <Box className="repository-form">
      <SettingsForm
        onSubmit={
          !canEdit
            ? undefined
            : activeTabControls
              ? activeTabControls.onSave
              : handleSubmit
        }
        onCancel={
          activeTabControls
            ? () => { activeTabControls.onCancel(); onCancel?.(); }
            : onCancel
        }
        loading={activeTabControls ? activeTabControls.saving || loading : form.isSaving || loading}
        pristine={activeTabControls ? !activeTabControls.dirty && form.isPristine : form.isPristine}
        error={error || form.saveError || undefined}
        submitLabel={isCreate ? FORM_STRINGS.BUTTONS.createRepository : FORM_STRINGS.BUTTONS.saveChanges}
        cancelLabel={FORM_STRINGS.BUTTONS.cancel}
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
                  {FORM_STRINGS.BUTTONS.browseRepository}
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
                  {FORM_STRINGS.BUTTONS.rebuildIndex}
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
                  {FORM_STRINGS.BUTTONS.invalidateCache}
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
                  {isOnlineSaved ? FORM_STRINGS.BUTTONS.takeOffline : FORM_STRINGS.BUTTONS.bringOnline}
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
                  {FORM_STRINGS.BUTTONS.delete}
                </SettingsButton>
              )}
            </Flex>
          ) : undefined
        }
      >
        {formBody}
      </SettingsForm>

      <ConfirmDialog
        open={xetConfirmOpen}
        onOpenChange={setXetConfirmOpen}
        title={UIStrings.HUGGINGFACE.XET_CONFIRM_TITLE}
        message={UIStrings.HUGGINGFACE.XET_CONFIRM_MESSAGE}
        confirmLabel={UIStrings.HUGGINGFACE.XET_CONFIRM_LABEL}
        cancelLabel="Cancel"
        variant="confirm"
        onConfirm={() => form.send('SUBMIT')}
        testId="xet-firewall-confirm"
      />
    </Box>
  );
}

