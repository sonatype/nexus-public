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

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Badge,
  Box,
  Flex,
  Tabs,
  Theme,
} from '@radix-ui/themes';

import { useRouter, useCurrentStateAndParams } from '@uirouter/react';
import { PageHeader, LoadingState, ErrorState } from '../../../../shared';
import { useUnsavedChangesWarning } from '../../../../shared/hooks/useUnsavedChangesWarning';

const ROUTE_STATE = 'preview.admin.iqHostedReposEval';
import {
  DEFAULT_SETTINGS,
  GlobalEvaluationSettings,
  useHostedRepoEvaluation,
} from './useHostedRepoEvaluation';
import { RepositoriesTab } from './RepositoriesTab';
import { SettingsTab } from './SettingsTab';

import './HostedRepoEvaluationSetupPage.scss';


/**
 * HostedRepoEvaluationSetupPage — Screen 3 of the IQ Server hosted-repo eval surface.
 *
 * Two tabs: Repositories (table with bulk monitoring toggle) + Settings (global
 * form). Selection state is a single in-memory Map<string, boolean> (id →
 * isMonitored at click time) that persists across pagination/filter/search and
 * is applied atomically on bulk enable/disable.
 *
 * URL: #preview/admin/iq/hosted-repos-eval[?tab=settings]
 */
export function HostedRepoEvaluationSetupPage() {
  const { fetchSettingsWithRepos, fetchGlobalConfigStatus, fetchFormats } = useHostedRepoEvaluation();
  const router = useRouter();
  const { params: routeParams } = useCurrentStateAndParams();
  // Snapshot once at mount — used in loadAllInitial to respect a ?tab= direct
  // link even after the API response updates the default. A ref avoids adding
  // routeParams (unstable object identity) to loadAllInitial's dep array.
  const urlTabRef = useRef(routeParams?.tab as 'repositories' | 'settings' | undefined);

  // Sync ?tab= with the visible tab via UIRouter's stateService. Uses
  // location:'replace' so tab switches don't pollute browser history.
  const syncTabToRoute = useCallback((newTab: 'repositories' | 'settings') => {
    try {
      router.stateService.go(ROUTE_STATE, { tab: newTab }, { location: 'replace' });
    } catch {
      // No-op if router is unavailable (e.g. in tests without a UIRouter provider).
    }
  }, [router]);

  // Tab state — prefer the ?tab= UIRouter param; fall back to 'settings'
  // (overridden by loadAllInitial once the API response is known).
  const [tab, setTab] = useState<'repositories' | 'settings'>(
    urlTabRef.current ?? 'settings'
  );

  // Initial load
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Settings form state
  const [settingsForm, setSettingsForm] = useState<GlobalEvaluationSettings>(DEFAULT_SETTINGS);
  const [pristineSettings, setPristineSettings] = useState<GlobalEvaluationSettings>(DEFAULT_SETTINGS);
  // True once the user has saved global evaluation settings at least once;
  // gates the first-time onboarding info alert on the Settings tab.
  const [hasExistingConfig, setHasExistingConfig] = useState(false);
  // True once the user clicks "Next" on the Settings tab for the first time,
  // unlocking the Repositories tab before any API row exists.
  const [settingsStaged, setSettingsStaged] = useState(false);
  const [formats, setFormats] = useState<string[]>([]);
  const [monitoredCount, setMonitoredCount] = useState(0);

  const loadAllInitial = useCallback(async () => {
    setLoadingInitial(true);
    setLoadError(null);
    try {
      const [swr, availableFormats, status] = await Promise.all([
        fetchSettingsWithRepos(),
        fetchFormats(),
        fetchGlobalConfigStatus(),
      ]);
      setSettingsForm(swr.settings);
      setPristineSettings(swr.settings);
      setHasExistingConfig(status.globalConfigAvailable);
      setMonitoredCount(status.monitoredCount);
      setFormats(availableFormats);
      // Respect an explicit ?tab= from the URL; otherwise default by config state.
      const resolvedTab = urlTabRef.current ?? (status.globalConfigAvailable ? 'repositories' : 'settings');
      setTab(resolvedTab);
      syncTabToRoute(resolvedTab);
    } catch (err: any) {
      setLoadError(err?.message || 'Failed to load Hosted Repository Evaluation page');
    } finally {
      setLoadingInitial(false);
    }
  }, [fetchSettingsWithRepos, fetchFormats, fetchGlobalConfigStatus, syncTabToRoute]);

  useEffect(() => {
    loadAllInitial();
  }, [loadAllInitial]);

  // Settings form derived state — enables Save/Cancel and the unsaved-changes guard.
  const settingsDirty = useMemo(
    () => JSON.stringify(settingsForm) !== JSON.stringify(pristineSettings),
    [settingsForm, pristineSettings]
  );

  // Register with the dirty-tracker so router/beforeunload prompt before discarding unsaved changes.
  useUnsavedChangesWarning(settingsDirty, 'hosted-repo-eval-settings-form');

  // Gap 4: if the user goes back to Settings and changes the form after having
  // staged it, re-lock the Repos tab so they must click Next again.
  useEffect(() => {
    if (settingsStaged && settingsDirty) {
      setSettingsStaged(false);
    }
  }, [settingsDirty, settingsStaged]);

  const handleTabChange = useCallback((newTab: string) => {
    if (settingsDirty && hasExistingConfig && newTab !== tab) {
      setSettingsForm(pristineSettings);
    }
    const typedTab = newTab as 'repositories' | 'settings';
    setTab(typedTab);
    syncTabToRoute(typedTab);
  }, [settingsDirty, hasExistingConfig, tab, pristineSettings, syncTabToRoute]);

  // Render — initial loading / error gates
  if (loadingInitial) {
    return <LoadingState message="Loading Hosted Repository Evaluation..." />;
  }

  if (loadError) {
    return <ErrorState title="Failed to load" message={loadError} onRetry={loadAllInitial} />;
  }

  return (
    <Theme accentColor="blue" hasBackground={false}>
      <Box className="hosted-repo-eval-setup-page" p="4">
        <PageHeader
          title={hasExistingConfig ? 'Hosted Repository Evaluation' : 'Set up Hosted Repository Evaluation'}
          description={
            hasExistingConfig
              ? 'Configure global evaluation settings and choose the hosted repositories you want to monitor.'
              : 'Configure global evaluation settings, then choose the hosted repositories you want to monitor.'
          }
          breadcrumbs={[
            { label: 'Settings', onClick: () => router.stateService.go('preview.settings') },
            { label: 'IQ Server', onClick: () => router.stateService.go('preview.admin.iqConnected') },
            { label: hasExistingConfig ? 'Hosted Repository Evaluation' : 'Set up Hosted Repository Evaluation' },
          ]}
        />

        <Tabs.Root value={tab} onValueChange={handleTabChange} mt="4">
          <Tabs.List>
            <Tabs.Trigger value="settings">Settings</Tabs.Trigger>
            <Tabs.Trigger value="repositories" disabled={!hasExistingConfig && !settingsStaged}>
              {monitoredCount > 0
                ? <Flex align="center" gap="2">Monitored Repositories<Badge size="1" variant="soft">{monitoredCount}</Badge></Flex>
                : 'Repositories'}
            </Tabs.Trigger>
          </Tabs.List>

          {/* forceMount keeps both panels mounted so RepositoriesTab's fetched
              rows, filters, and selection survive a tab switch — otherwise
              Radix unmounts the inactive tab and every switch back triggers
              a fresh /repository-dashboard load. The inner `hidden` div is
              our own visibility gate (Radix Themes' Content doesn't add the
              hidden attribute under forceMount), and it also hides inactive
              content from a11y / testing-library queries. */}
          <Tabs.Content value="repositories" forceMount>
            <div hidden={tab !== 'repositories'}>
              <RepositoriesTab
                hasExistingConfig={hasExistingConfig}
                setHasExistingConfig={setHasExistingConfig}
                pristineSettings={pristineSettings}
                setSettingsStaged={setSettingsStaged}
                formats={formats}
                onMonitoredCountChange={setMonitoredCount}
                isActiveTab={tab === 'repositories'}
              />
            </div>
          </Tabs.Content>

          <Tabs.Content value="settings" forceMount>
            <div hidden={tab !== 'settings'}>
              <SettingsTab
                settingsForm={settingsForm}
                setSettingsForm={setSettingsForm}
                pristineSettings={pristineSettings}
                setPristineSettings={setPristineSettings}
                hasExistingConfig={hasExistingConfig}
                settingsDirty={settingsDirty}
                setSettingsStaged={setSettingsStaged}
                setTab={setTab}
              />
            </div>
          </Tabs.Content>
        </Tabs.Root>
      </Box>
    </Theme>
  );
}

export default HostedRepoEvaluationSetupPage;
