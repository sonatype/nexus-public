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
import { useRouter, useCurrentStateAndParams } from '@uirouter/react';
import { Badge, Box, Button, Callout, Card, Flex, Heading, Switch, Text, Theme } from '@radix-ui/themes';
import { CircleAlert, CircleCheck, ExternalLink, TriangleAlert } from 'lucide-react';

import { PageHeader, LoadingState, ErrorState } from '../../../../shared';
import {
  useIqConnectedApi,
  IqConfigResponse,
  DashboardSummary,
  EvaluationSettingsSummary,
} from './useIqConnectedApi';
import type { IqServerConfiguration } from './types';
import { disableIqServer } from './iqServerFormMachine';
import { ExtJS } from '../../../../../../interface/ExtJS';
import Permissions from '../../../../../../constants/Permissions';
import { IqServerConfigurationDialog } from './IqServerConfigurationDialog';

import { humanizeStage, formatErrorMessage } from './iqServerUtils';
import { isEvaluationFeatureEnabled } from '../../repository/repositories/useRepoEvaluationOverride';
import {
  getFreshIqConfigCache,
  clearFreshIqConfigCache,
  setFreshIqConfigCache,
  setPendingDisconnect,
} from './iqServerStateCache';

import './IqServerConnectedPage.scss';

/**
 * IqServerConnectedPage — the IQ Server connected view in Super UI.
 *
 * Mounted by two UIRouter states:
 *   preview.admin.iqConnected   — Lifecycle + Firewall cards (dialog closed)
 *   preview.admin.iqConnection  — same cards with configuration dialog open
 *
 * If the fetched config reports IQ as disabled and the dialog is not open,
 * this component redirects to preview.admin.iq so routing stays
 * semantically correct (disconnected state lives at /iq).
 *
 * Closing the dialog after Save → iqConnected; after Disconnect → iq.
 */

/**
 * A freshly saved connection is established asynchronously, so GET /v1/iq can still report
 * enabled=false for a moment afterwards. The page waits by re-loading a bounded number of
 * times: a save that never takes effect then settles into the normal loaded state instead of
 * spinning on a spinner forever.
 */
const SAVE_RETRY_LIMIT = 3;

const SAVE_RETRY_DELAY_MS = 2000;

// licensedSolutions can be legitimately empty (IQ < 200 answers /api/v2/solutions/licensed with 404,
// or an IOException there is swallowed into an empty list). Both are persistent, not transient — so
// we retry at most once on that signal alone, not the full SAVE_RETRY_LIMIT budget.
const EMPTY_SOLUTIONS_ONLY_RETRY_LIMIT = 1;

function configToResponse(config: IqServerConfiguration): IqConfigResponse {
  return {
    enabled: config.enabled ?? true,
    showLink: config.showLink,
    url: config.url,
    authenticationType: config.authenticationType,
    username: config.username,
    password: config.password,
    useTrustStoreForUrl: config.useTrustStoreForUrl,
    timeoutSeconds: config.timeoutSeconds,
    properties: config.properties,
    failOpenModeEnabled: config.failOpenModeEnabled ?? false,
    licensedSolutions: config.licensedSolutions ?? [],
    hasFirewall: config.hasFirewall ?? false,
  };
}

/**
 * The persisted IQ enabled flag as published by the rapture bootstrap state
 * (ClmStateContributor -> clmConnector.isActive()). It is read from state
 * already delivered with the page, so it needs no request at all — in particular
 * none to the remote IQ Server. Same accessor RepositoriesList uses.
 */
function isIqServerEnabledLocally(): boolean {
  try {
    return ExtJS.state().getValue('clm')?.enabled ?? false;
  }
  catch {
    return false;
  }
}

export function IqServerConnectedPage() {
  const router = useRouter();
  const { state: currentState } = useCurrentStateAndParams();
  const { fetchIq, verifyConnection, fetchDashboardSummary, fetchEvaluationSettings } = useIqConnectedApi();

  const [iqData, setIqData] = useState<IqConfigResponse | null>(null);
  const [dashboardSummary, setDashboardSummary] = useState<DashboardSummary | null>(null);
  const [evalSettings, setEvalSettings] = useState<EvaluationSettingsSummary | null>(null);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reverifyError, setReverifyError] = useState<string | null>(null);
  const [reverifying, setReverifying] = useState(false);
  const [verifyOk, setVerifyOk] = useState<boolean | null>(null);
  // Disabling the integration is independent of remote health, so it gets its
  // own in-flight/error state rather than sharing the verify state above.
  const [togglingEnabled, setTogglingEnabled] = useState(false);
  const [toggleError, setToggleError] = useState<string | null>(null);
  // Persisted enabled state from local bootstrap state, captured once.
  // It lets the header and the enable/disable control render before — and
  // independently of — the remote-dependent GET /v1/iq.
  const [iqEnabledLocally] = useState<boolean>(isIqServerEnabledLocally);
  // Set the moment a disable succeeds, so the control reflects the new state even
  // though the GET that would confirm it may never have completed.
  const [disabledLocally, setDisabledLocally] = useState(false);

  // The enable/disable control is a write affordance: visible to everyone who can
  // read the page, operable only with nexus:settings:update. Uses the ExtJS
  // singleton (not the context hook) because coreui never mounts a
  // <PermissionsProvider> — see NEXUS-54212.
  const hasUser = ExtJS.useUser() ?? false;
  const canEdit = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.SETTINGS.UPDATE),
    [hasUser],
  );

  // Open immediately on button click; also true when mounted on the /connection route.
  const [dialogOpen, setDialogOpen] = useState(
    () => currentState.name === 'preview.admin.iqConnection'
  );
  // Set to true by any callback that already issued a navigation, so the
  // redirect-check effect does not fire a second navigation on top of it.
  // IMPORTANT: This flag intentionally never resets — it works because previewAdminRoutes.js
  // declares IqServerConnectedPage and IqServerConnectionPage as two distinct lazy-load
  // wrappers of the same component, forcing React to remount on every state transition
  // (fresh useRef). If those wrappers are ever consolidated, the flag must be reset manually.
  const navigationHandledRef = useRef(false);
  // Post-save retry bookkeeping: attempts already spent, the one pending timer, and whether the
  // page is still mounted. Refs rather than state so waiting never re-renders or rebuilds loadData.
  const saveRetriesRef = useRef(0);
  const saveRetryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);

  // Leaves the page out of the post-save window: no budget spent, no timer pending. Every path
  // that stops waiting must call this, otherwise a later load would still compute awaitingSave
  // and suppress connection verification for the rest of the page's life.
  const resetSaveRetryState = useCallback(() => {
    saveRetriesRef.current = 0;
    if (saveRetryTimerRef.current !== null) {
      clearTimeout(saveRetryTimerRef.current);
      saveRetryTimerRef.current = null;
    }
  }, []);

  const loadData = useCallback(async () => {
    setLoadingInitial(true);
    setVerifyOk(null);
    setLoadError(null);
    try {
      const savedFromDialog = getFreshIqConfigCache();
      const justSaved = Boolean(savedFromDialog);
      if (justSaved) {
        clearFreshIqConfigCache();
        saveRetriesRef.current = 0;
      }
      const awaitingSave = justSaved || saveRetriesRef.current > 0;
      const cachedIsAuthoritative =
        justSaved && Array.isArray(savedFromDialog!.licensedSolutions)
          && savedFromDialog!.licensedSolutions.length > 0;
      const [iq, verify, dashboard, settings] = await Promise.all([
        cachedIsAuthoritative ? Promise.resolve(configToResponse(savedFromDialog!)) : fetchIq(),
        verifyConnection(),
        fetchDashboardSummary(),
        fetchEvaluationSettings(),
      ]);
      const licensedSolutionsEmpty = !(iq.licensedSolutions && iq.licensedSolutions.length > 0);
      const verifyTransientlyFailed = !verify.success;
      // Split the two signals: !enabled and !verify.success get the full retry budget (both are the
      // canonical shapes of "backend hasn't finished settling"); empty licensedSolutions is capped at
      // one retry because it is also the persistent shape for IQ < 200 and swallowed-IOException.
      const backendStillSettling = !iq.enabled || verifyTransientlyFailed;
      const budget = backendStillSettling ? SAVE_RETRY_LIMIT : EMPTY_SOLUTIONS_ONLY_RETRY_LIMIT;
      const staleResponse = backendStillSettling || licensedSolutionsEmpty;
      if (awaitingSave && staleResponse && saveRetriesRef.current < budget) {
        saveRetriesRef.current += 1;
        // One timer at a time, so a retry cannot overlap the load that scheduled it.
        if (saveRetryTimerRef.current !== null) {
          clearTimeout(saveRetryTimerRef.current);
        }
        saveRetryTimerRef.current = setTimeout(() => {
          saveRetryTimerRef.current = null;
          if (mountedRef.current) {
            loadData();
          }
        }, SAVE_RETRY_DELAY_MS);
        return; // loadingInitial stays true → spinner keeps showing
      }
      // Either the server caught up or the budget is spent: report what it says and stop waiting.
      resetSaveRetryState();
      setIqData(iq);
      setVerifyOk(verify.success === true);
      setDashboardSummary(dashboard);
      setEvalSettings(settings);
      setLoadingInitial(false);
    } catch (err: any) {
      // A failed load ends the post-save window too: the ErrorState retry below must behave like
      // a normal load and verify the connection, so a real connection error is reported instead
      // of being masked by the leftover post-save assumption that the link is good.
      resetSaveRetryState();
      setLoadError(err?.message || 'Failed to load IQ Server page');
      setLoadingInitial(false);
    }
    // NO finally block — intentional. finally always runs even on return.
  }, [fetchIq, verifyConnection, fetchDashboardSummary, fetchEvaluationSettings, resetSaveRetryState]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // A retry that fires after unmount would refetch and set state for a page that is gone.
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
      if (saveRetryTimerRef.current !== null) {
        clearTimeout(saveRetryTimerRef.current);
        saveRetryTimerRef.current = null;
      }
    };
  }, []);

  const hasLifecycle = useMemo(
    () => iqData?.licensedSolutions?.some(s => s.id === 'lifecycle') ?? false,
    [iqData]
  );
  // licensedSolutions (from /api/v2/solutions/licensed) is the only authoritative licence signal.
  // hasFirewall is a capability property set by detectFirewall() — true on any IQ >= 200 with a
  // working connection, regardless of licensing — so it cannot decide the Firewall tile alone.
  const hasFirewall = useMemo(
    () => iqData?.licensedSolutions?.some(s => s.id === 'firewall') ?? false,
    [iqData]
  );
  const isConfigured = Boolean(dashboardSummary?.globalConfigAvailable);
  // Persisted-state only: iqData when the GET succeeded, otherwise the local
  // bootstrap flag. Remote connection health never feeds into this.
  const enabled = disabledLocally ? false : (iqData?.enabled ?? iqEnabledLocally);
  const liveConnected = enabled && (verifyOk === true) && !reverifyError;

  const isHostedEvalFeatureEnabled = useMemo(() => isEvaluationFeatureEnabled(), []);

  // Redirect to overview when IQ is disabled and the dialog is not open.
  // navigationHandledRef guards against double-navigation when handleSaved /
  // handleDisconnected / handleDialogCancel already issued a go() this render.
  useEffect(() => {
    if (navigationHandledRef.current) return;
    if (!loadingInitial && !loadError && !enabled && !dialogOpen) {
      router.stateService.go('preview.admin.iq');
    }
  }, [loadingInitial, loadError, enabled, dialogOpen, router]);

  const handleConnectionSettings = useCallback(() => {
    setReverifyError(null);
    setDialogOpen(true);
    router.stateService.go('preview.admin.iqConnection');
  }, [router]);

  // After save: hand the PUT response body to the remounted Connected page as the "just saved"
  // signal so its first load can seed url / licensedSolutions without racing the async backend
  // settle. Verify still runs on mount; the retry loop covers the transient not-yet-active window.
  const handleSaved = useCallback((savedConfig: IqServerConfiguration) => {
    navigationHandledRef.current = true;
    setFreshIqConfigCache({ ...savedConfig, enabled: true });
    setDialogOpen(false);
    router.stateService.go('preview.admin.iqConnected');
  }, [router]);

  // After disconnect: send user to the overview (disconnected entry) route.
  // Set pendingDisconnect so the overview page skips its GET and shows the
  // disconnected card immediately, avoiding a race with server async processing.
  const handleDisconnected = useCallback((disconnectedConfig?: IqServerConfiguration) => {
    navigationHandledRef.current = true;
    setDisabledLocally(true);
    if (disconnectedConfig) setIqData(disconnectedConfig);
    setDialogOpen(false);
    setPendingDisconnect();
    router.stateService.go('preview.admin.iq');
  }, [router]);

  /**
   * Page-level enable/disable, driven by the persisted `enabled` value and never
   * by remote connection health.
   *
   * Turning it OFF calls POST /v1/iq/disable. That endpoint re-persists the
   * already-stored configuration with enabled=false, so every unrelated setting
   * (credentials, timeout, properties, hasFirewall and failOpenModeEnabled) is
   * carried over untouched — unlike a PUT payload rebuilt in the browser, which
   * cannot carry fields the GET never exposes. It also issues no request to the
   * remote IQ Server, and needs no `iqData`, so an unreachable host or an
   * unretrievable license cannot trap the user on this page.
   *
   * Turning it ON keeps the existing validated path: the dialog collects the
   * required fields and pre-verifies the connection before saving.
   */
  const handleEnabledChange = useCallback(
    async (next: boolean) => {
      if (next) {
        handleConnectionSettings();
        return;
      }

      setToggleError(null);
      setTogglingEnabled(true);
      try {
        await disableIqServer();
        // Reuse the disconnect path: marks the navigation as handled, records the
        // pending disconnect so the overview page skips its (still-stale) GET, and
        // unmounts this page so no further connection-status requests are issued.
        handleDisconnected(iqData ? { ...iqData, enabled: false } : undefined);
      }
      catch (err: any) {
        // Never report the integration as disabled when the request failed.
        setToggleError(formatErrorMessage(err, 'Failed to disable IQ Server'));
      }
      finally {
        setTogglingEnabled(false);
      }
    },
    [iqData, handleConnectionSettings, handleDisconnected],
  );

  // Cancel / close without action: return to whichever route makes sense.
  const handleDialogCancel = useCallback(() => {
    navigationHandledRef.current = true;
    setDialogOpen(false);
    router.stateService.go(
      enabled ? 'preview.admin.iqConnected' : 'preview.admin.iq'
    );
  }, [router, enabled]);


  const verifyThenNavigate = useCallback(async (stateName: string, params?: object) => {
    setReverifying(true);
    try {
      const result = await verifyConnection();
      if (result?.success) {
        setReverifyError(null);
        router.stateService.go(stateName, params);
      } else {
        setReverifyError('Unable to connect to IQ Server. Please check your connection settings and verify the server is reachable.');
      }
    } catch {
      setReverifyError('Unable to connect to IQ Server. Please check your connection settings and verify the server is reachable.');
    } finally {
      setReverifying(false);
    }
  }, [router, verifyConnection]);

  const handleSetUp = useCallback(() => {
    verifyThenNavigate('preview.admin.iqHostedReposEval', { tab: 'settings' });
  }, [verifyThenNavigate]);

  const handleSummaryTileClick = useCallback(() => {
    verifyThenNavigate('preview.admin.iqHostedReposEval', { tab: 'settings' });
  }, [verifyThenNavigate]);

  // While IQ is persisted as enabled the page shell — and with it the
  // enable/disable control — always renders. GET /v1/iq can hang or fail (it asks
  // IQ Server for licensed solutions), so its loading and error surfaces are scoped
  // to the detail body further down instead of replacing the whole page.
  if (!enabled) {
    if (loadingInitial && !dialogOpen) {
      return <LoadingState message="Loading IQ Server configuration..." />;
    }

    if (loadError && !dialogOpen) {
      return (
        <ErrorState
          title="Failed to load IQ Server"
          message={loadError}
          onRetry={loadData}
        />
      );
    }

    // Redirect in flight (useEffect above) — render nothing to avoid flash.
    if (!dialogOpen) {
      return null;
    }
  }

  return (
    <Theme accentColor="blue" hasBackground={false}>
    <Box className="iq-server-connected-page" p="4">
      
      <PageHeader
        title="IQ Server"
        description="Manage Sonatype Repository Firewall and Lifecycle configuration"
        descriptionSuffix={
          iqData?.url ? (
            <Badge
              variant="soft"
              color="gray"
              size="1"
              radius="full"
              data-testid="iq-server-url-chip"
            >
              {iqData?.url}
            </Badge>
          ) : undefined
        }
        breadcrumbs={[
          { label: 'Settings', onClick: () => router.stateService.go('preview.settings') },
          { label: 'IQ Server' },
        ]}
        actions={
          <Flex align="center" gap="4">
            <Flex asChild align="center" gap="2">
              {/* Rendered from the persisted config only, so a failing connection
                  can never remove the user's way out. */}
              <label>
                <Switch
                  data-testid="iq-enable-toggle"
                  aria-label="Enable IQ Server"
                  checked={enabled}
                  onCheckedChange={handleEnabledChange}
                  disabled={!canEdit || togglingEnabled}
                />
                <Text size="2">Enable IQ Server</Text>
              </label>
            </Flex>
            <Button variant="surface" size="2" onClick={handleConnectionSettings}>
              Connection Settings
            </Button>
          </Flex>
        }
      />

      {toggleError && (
        <Box mt="4" data-testid="iq-enable-error">
          <Callout.Root color="red" variant="soft" role="alert">
            <Callout.Icon>
              <CircleAlert size={16} aria-hidden="true" />
            </Callout.Icon>
            <Callout.Text>{toggleError}</Callout.Text>
          </Callout.Root>
        </Box>
      )}

      {(reverifyError || (enabled && verifyOk === false)) && (
        <Box mt="4" data-testid="iq-connection-error-banner">
          <Callout.Root color="red" variant="soft">
            <Callout.Icon>
              <TriangleAlert size={16} aria-hidden="true" />
            </Callout.Icon>
            <Callout.Text>
              <Text as="span" weight="medium" style={{display: 'block', marginBottom: '4px'}}>Connection Error</Text>
              <Text as="span" style={{display: 'block'}}>{reverifyError ?? 'IQ Server is not reachable. Check your connection settings and verify the server is running.'}</Text>
            </Callout.Text>
          </Callout.Root>
        </Box>
      )}

      {/* The detail body is what actually depends on GET /v1/iq
          (licensed solutions come from the remote IQ Server), so its loading and
          error surfaces live here rather than replacing the page and its
          enable/disable control. */}
      {loadingInitial ? (
        <Box mt="5" data-testid="iq-details-loading">
          <LoadingState message="Loading IQ Server configuration..." />
        </Box>
      ) : loadError ? (
        <Box mt="5" data-testid="iq-details-error">
          <ErrorState
            title="Failed to load IQ Server"
            message={loadError}
            onRetry={loadData}
          />
        </Box>
      ) : (
        <>
        <Box mt="5" mb="6">
          <Flex align="center" gap="3" mb="3">
            <Heading size="4" as="h2">
              Sonatype Lifecycle
            </Heading>
            {liveConnected && hasLifecycle ? (
              <Badge variant="soft" color="green" size="1" radius="full" role="status" data-testid="lifecycle-status-connected">
                <CircleCheck size={12} aria-hidden="true" />
                Connected
              </Badge>
            ) : (
              <Badge variant="soft" color="gray" size="1" radius="full" role="status" data-testid="lifecycle-status-disconnected">
                Not connected
              </Badge>
            )}
          </Flex>

          {!hasLifecycle ? (
            <Card data-testid="lifecycle-unavailable-card">
              <Flex direction="column" align="center" gap="2" p="6">
                <Text size="2" color="red" weight="medium">
                  Purchase license or contact Administrator
                </Text>
                <Button asChild variant="ghost" size="2" color="blue" className="iq-server-connected-page__external-link">
                  <a
                    href="https://links.sonatype.com/products/nxrm3/browse/lc-learn"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <ExternalLink size={14} />
                    Explore Sonatype Lifecycle
                  </a>
                </Button>
              </Flex>
            </Card>
          ) : !isHostedEvalFeatureEnabled ? (
            <Card data-testid="lifecycle-flag-off-card">
              <Flex direction="column" align="center" justify="center" gap="2" py="6" px="5">
                <Text size="2" color="gray">
                  Lifecycle is connected and evaluating repositories against IQ Server policies.
                </Text>
              </Flex>
            </Card>
          ) : isConfigured ? (
            <Card
              asChild
              className="iq-server-connected-page__summary-card"
              data-testid="lifecycle-summary-tile"
            >
              <button
                type="button"
                onClick={handleSummaryTileClick}
                aria-label="Edit Hosted Repository Evaluation configuration"
              >
                <Box p="4">
                  <Flex align="center" justify="between" mb="3" gap="3">
                    <Heading size="3" as="h3">
                      Hosted Repository Evaluation
                    </Heading>
                    <Badge variant="soft" color="gray" size="1" radius="full" data-testid="lifecycle-stage-pill">
                      Stage: {humanizeStage(evalSettings?.policyEvaluationStage ?? 'BUILD')}
                    </Badge>
                  </Flex>
                  <Flex gap="3" wrap="wrap">
                    <Box className="iq-server-connected-page__metric" data-testid="lifecycle-metric-monitored">
                      <Text size="2" color="gray" as="div" mb="2">
                        Repositories Monitored
                      </Text>
                      <Heading size="7" className="iq-server-connected-page__metric-value">
                        {dashboardSummary?.numberOfMonitoredRepositories ?? 0}
                      </Heading>
                    </Box>
                    <Box className="iq-server-connected-page__metric" data-testid="lifecycle-metric-timeframe">
                      <Text size="2" color="gray" as="div" mb="2">
                        Activity Time Frame
                      </Text>
                      <Flex align="baseline" gap="2">
                        <Heading size="7" className="iq-server-connected-page__metric-value">
                          {evalSettings?.activityTimeFrame ?? 30}
                        </Heading>
                        <Text size="2" color="gray">
                          days
                        </Text>
                      </Flex>
                    </Box>
                    <Box className="iq-server-connected-page__metric" data-testid="lifecycle-metric-latest-versions">
                      <Text size="2" color="gray" as="div" mb="2">
                        Latest Deployed Versions
                      </Text>
                      <Flex align="baseline" gap="2">
                        <Heading size="7" className="iq-server-connected-page__metric-value">
                          {evalSettings?.artifactLatestVersions ?? 5}
                        </Heading>
                        <Text size="2" color="gray">
                          versions
                        </Text>
                      </Flex>
                    </Box>
                  </Flex>
                </Box>
              </button>
            </Card>
          ) : (
            <Card data-testid="lifecycle-setup-card">
              <Flex align="center" justify="between" p="4" gap="4">
                <Box>
                  <Heading size="3" mb="1" as="h3">
                    Hosted Repository Evaluation
                  </Heading>
                  <Text size="2" color="gray">
                    Configure global evaluation settings to provide policy coverage for your hosted repositories.
                  </Text>
                </Box>
                <Button onClick={handleSetUp} disabled={reverifying} data-testid="lifecycle-setup-button">
                  {reverifying ? 'Verifying…' : 'Set up'}
                </Button>
              </Flex>
            </Card>
          )}
        </Box>

        <Box>
          <Flex align="center" gap="3" mb="3">
            <Heading size="4" as="h2">
              Sonatype Firewall
            </Heading>
            {liveConnected && hasFirewall ? (
              <Badge variant="soft" color="green" size="1" radius="full" role="status" data-testid="firewall-status-connected">
                <CircleCheck size={12} aria-hidden="true" />
                Connected
              </Badge>
            ) : (
              <Badge variant="soft" color="gray" size="1" radius="full" role="status" data-testid="firewall-status-disconnected">
                Not connected
              </Badge>
            )}
          </Flex>

          <Card data-testid="firewall-card">
            <Flex direction="column" align="center" gap="3" py="6" px="5">
              {hasFirewall ? (
                <Text size="2" color="gray">
                  No Repository Firewall configuration available yet.
                </Text>
              ) : (
                <Text size="2" color="red" weight="medium">
                  Purchase license or contact Administrator
                </Text>
              )}
              <Button asChild variant="ghost" size="2" color="blue" className="iq-server-connected-page__external-link">
                <a
                  href="https://links.sonatype.com/nexus-repository-firewall"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <ExternalLink size={14} />
                  Learn more about Repository Firewall
                </a>
              </Button>
            </Flex>
          </Card>
        </Box>
        </>
      )}

      <IqServerConfigurationDialog
        open={dialogOpen}
        onOpenChange={open => { if (!open) handleDialogCancel(); }}
        onSaved={handleSaved}
        onDisconnected={handleDisconnected}
        initiallyConnected={enabled}
      />
    </Box>
    </Theme>
  );
}

export default IqServerConnectedPage;
