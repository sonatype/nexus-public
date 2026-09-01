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
import { Badge, Box, Button, Callout, Card, Flex, Heading, Text, Theme } from '@radix-ui/themes';
import { CircleCheck, ExternalLink, TriangleAlert } from 'lucide-react';

import { PageHeader, LoadingState, ErrorState } from '../../../../shared';
import {
  useIqConnectedApi,
  IqConfigResponse,
  DashboardSummary,
  EvaluationSettingsSummary,
} from './useIqConnectedApi';
import type { IqServerConfiguration } from './types';
import { IqServerConfigurationDialog } from './IqServerConfigurationDialog';

import { humanizeStage } from './iqServerUtils';
import { isEvaluationFeatureEnabled } from '../../repository/repositories/useRepoEvaluationOverride';
import {
  freshIqConfigCache,
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

  const loadData = useCallback(async () => {
    setLoadingInitial(true);
    setVerifyOk(null);
    setLoadError(null);
    try {
      const justSaved = Boolean(freshIqConfigCache);
      if (justSaved) {
        clearFreshIqConfigCache();
      }
      // Skip verifyConnection when justSaved — the dialog already verified before
      // PUT, and verify-connection returns 400 briefly after a fresh save while
      // the server finishes establishing the link.
      const [iq, verify, dashboard, settings] = await Promise.all([
        fetchIq(),
        justSaved ? Promise.resolve({ success: true }) : verifyConnection(),
        fetchDashboardSummary(),
        fetchEvaluationSettings(),
      ]);
      // Backend processes the IQ connection asynchronously — retry if GET still returns disabled after Save.
      if (justSaved && !iq.enabled) {
        setTimeout(loadData, 2000);
        return; // loadingInitial stays true → spinner keeps showing
      }
      // For justSaved, force enabled:true — connection was verified by dialog before PUT.
      setIqData(iq);
      setVerifyOk(justSaved ? true : (verify.success === true));
      setDashboardSummary(dashboard);
      setEvalSettings(settings);
      setLoadingInitial(false);
    } catch (err: any) {
      setLoadError(err?.message || 'Failed to load IQ Server page');
      setLoadingInitial(false);
    }
    // NO finally block — intentional. finally always runs even on return.
  }, [fetchIq, verifyConnection, fetchDashboardSummary, fetchEvaluationSettings]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const hasLifecycle = useMemo(
    () => iqData?.licensedSolutions?.some(s => s.id === 'lifecycle') ?? false,
    [iqData]
  );
  const hasFirewall = useMemo(
    () => iqData?.hasFirewall || (iqData?.licensedSolutions?.some(s => s.id === 'firewall') ?? false),
    [iqData]
  );
  const isConfigured = Boolean(dashboardSummary?.globalConfigAvailable);
  const enabled = iqData?.enabled ?? false;
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

  // After save: fetch fresh config via GET so licensedSolutions is populated
  // (PUT response may omit it). The dialog already verified the connection, so
  // we know enabled=true; force it on the fetched config.
  const handleSaved = useCallback((savedConfig: IqServerConfiguration) => {
    navigationHandledRef.current = true;
    // Set cache as a "just saved" signal so the remounted component knows to
    // force enabled:true and fetch fresh data (GET + verify) on mount.
    setFreshIqConfigCache({ ...savedConfig, enabled: true });
    setDialogOpen(false);
    router.stateService.go('preview.admin.iqConnected');
  }, [router]);

  // After disconnect: send user to the overview (disconnected entry) route.
  // Set pendingDisconnect so the overview page skips its GET and shows the
  // disconnected card immediately, avoiding a race with server async processing.
  const handleDisconnected = useCallback((disconnectedConfig?: IqServerConfiguration) => {
    navigationHandledRef.current = true;
    if (disconnectedConfig) setIqData(disconnectedConfig);
    setDialogOpen(false);
    setPendingDisconnect();
    router.stateService.go('preview.admin.iq');
  }, [router]);

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
  if (!loadingInitial && !loadError && !enabled && !dialogOpen) {
    return null;
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
          <Button variant="surface" size="2" onClick={handleConnectionSettings}>
            Connection Settings
          </Button>
        }
      />

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
