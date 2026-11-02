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
import { useRouter } from '@uirouter/react';
import { Badge, Box, Button, Callout, Card, Flex, Heading, Text } from '@radix-ui/themes';
import { CircleCheck, ExternalLink, TriangleAlert } from 'lucide-react';

import { LoadingState, ErrorState } from '../../../../shared';
import {
  useIqConnectedApi,
  IqConfigResponse,
  DashboardSummary,
  EvaluationSettingsSummary,
} from './useIqConnectedApi';
import { humanizeStage } from './iqServerUtils';
import { isEvaluationFeatureEnabled } from '../../repository/repositories/useRepoEvaluationOverride';

import './IqServerOverviewTab.scss';

/**
 * A freshly saved connection is established asynchronously, so GET /v1/iq can still report
 * enabled=false for a moment afterwards. The tab waits by re-loading a bounded number of
 * times: a save that never takes effect then settles into the normal loaded state instead of
 * spinning on a spinner forever.
 */
const SAVE_RETRY_LIMIT = 3;

const SAVE_RETRY_DELAY_MS = 2000;

export interface IqServerOverviewTabProps {
  /** True when the tab is the currently active one — used to know when to reload after a save. */
  isActive: boolean;
  /** Increments when the parent page reports a successful Save; drives a fresh reload cycle. */
  saveRevision: number;
}

/**
 * IqServerOverviewTab — the Lifecycle + Firewall summary content for the
 * IQ Server settings page. Rendered inside the "Overview" tab, which is only
 * enabled once IQ Server is configured and turned on.
 */
export function IqServerOverviewTab({ isActive, saveRevision }: IqServerOverviewTabProps) {
  const router = useRouter();
  const { fetchIq, verifyConnection, fetchDashboardSummary, fetchEvaluationSettings } = useIqConnectedApi();

  const [iqData, setIqData] = useState<IqConfigResponse | null>(null);
  const [dashboardSummary, setDashboardSummary] = useState<DashboardSummary | null>(null);
  const [evalSettings, setEvalSettings] = useState<EvaluationSettingsSummary | null>(null);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [reverifyError, setReverifyError] = useState<string | null>(null);
  const [reverifying, setReverifying] = useState(false);
  const [verifyOk, setVerifyOk] = useState<boolean | null>(null);
  // Incremented to trigger a reload (used by retry button)
  const [retryCount, setRetryCount] = useState(0);

  // Track the active AbortController so cleanup can abort it
  const abortControllerRef = useRef<AbortController | null>(null);
  // Last saveRevision we treated as a fresh save. Only a bump past this counts as
  // "just saved" — a retry-button reload (or a remount at the same revision) must go
  // through the normal verify path, not the post-save skip-and-retry path.
  const lastSaveRevisionRef = useRef(0);

  const loadData = useCallback(async (justSaved: boolean, signal: AbortSignal) => {
    setLoadingInitial(true);
    setVerifyOk(null);
    setLoadError(null);
    let internalRetryCount = 0;

    const attempt = async (): Promise<void> => {
      if (signal.aborted) return;
      const awaitingSave = justSaved || internalRetryCount > 0;
      const [iq, verify, dashboard, settings] = await Promise.all([
        fetchIq(),
        // Skip verifyConnection while awaiting a save — the connection was already verified
        // before PUT, and verify-connection can return 400 briefly after a fresh save while
        // the server finishes establishing the link.
        awaitingSave ? Promise.resolve({ success: true }) : verifyConnection(),
        fetchDashboardSummary(),
        fetchEvaluationSettings(),
      ]);
      if (signal.aborted) return;
      // Backend processes the IQ connection asynchronously — retry if GET still returns disabled after Save.
      if (awaitingSave && !iq.enabled && internalRetryCount < SAVE_RETRY_LIMIT) {
        internalRetryCount += 1;
        await new Promise(resolve => setTimeout(resolve, SAVE_RETRY_DELAY_MS));
        if (signal.aborted) return;
        return attempt();
      }
      setIqData(iq);
      setVerifyOk(awaitingSave ? true : (verify.success === true));
      setDashboardSummary(dashboard);
      setEvalSettings(settings);
      setLoadingInitial(false);
    };

    try {
      await attempt();
    } catch (err: any) {
      if (!signal.aborted) {
        setLoadError(err?.message || 'Failed to load IQ Server page');
        setLoadingInitial(false);
      }
    }
  }, [fetchIq, verifyConnection, fetchDashboardSummary, fetchEvaluationSettings]);

  // Initial load, plus a reload whenever the parent reports a save (saveRevision bumps) or retry.
  useEffect(() => {
    // Abort any in-flight request before starting a new one
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;
    // Only a genuine bump past the last-handled revision is a fresh save; a retry or
    // same-revision remount reloads through the normal verify path.
    const justSaved = saveRevision > lastSaveRevisionRef.current;
    lastSaveRevisionRef.current = saveRevision;
    loadData(justSaved, controller.signal);
    return () => controller.abort();
  }, [loadData, saveRevision, retryCount]);

  const hasLifecycle = useMemo(
    () => iqData?.licensedSolutions?.some(s => s.id === 'lifecycle') ?? false,
    [iqData]
  );
  const hasFirewall = useMemo(
    () => iqData?.hasFirewall || (iqData?.licensedSolutions?.some(s => s.id === 'firewall') ?? false),
    [iqData]
  );
  const isConfigured = Boolean(dashboardSummary?.globalConfigAvailable);
  const isEnabled = iqData?.enabled ?? false;
  const liveConnected = isEnabled && (verifyOk === true) && !reverifyError;
  // A failed verify is only an *error* worth flagging when IQ Server is actually enabled. Before
  // it has ever been configured, GET /v1/iq returns enabled=false and verify reports failure by
  // design — that is the normal "Not connected" state, not a broken connection, so no red banner.
  const showConnectionError = Boolean(reverifyError) || (isEnabled && verifyOk === false);

  const isHostedEvalFeatureEnabled = useMemo(() => isEvaluationFeatureEnabled(), []);

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

  const handleRetry = useCallback(() => {
    setRetryCount(c => c + 1);
  }, []);

  if (loadingInitial) {
    return (
      <Box mt="5" data-testid="iq-details-loading">
        <LoadingState message="Loading IQ Server configuration..." />
      </Box>
    );
  }

  if (loadError) {
    return (
      <Box mt="5" data-testid="iq-details-error">
        <ErrorState
          title="Failed to load IQ Server"
          message={loadError}
          onRetry={handleRetry}
        />
      </Box>
    );
  }

  return (
    <Box className="iq-server-overview-tab" p="4" hidden={!isActive}>
      {showConnectionError && (
        <Box mb="4" data-testid="iq-connection-error-banner">
          <Callout.Root color="red" variant="soft">
            <Callout.Icon>
              <TriangleAlert size={16} aria-hidden="true" />
            </Callout.Icon>
            <Callout.Text>
              <Text as="span" weight="medium" style={{display: 'block', marginBottom: '4px'}}>Connection Error</Text>
              <Text as="span" style={{display: 'block'}}>
                {reverifyError ?? 'IQ Server is not reachable. Check your connection settings and verify the server is running.'}
              </Text>
            </Callout.Text>
          </Callout.Root>
        </Box>
      )}

      <Box mb="6">
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
              <Button asChild variant="ghost" size="2" color="blue" className="iq-server-overview-tab__external-link">
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
            className="iq-server-overview-tab__summary-card"
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
                  <Box className="iq-server-overview-tab__metric" data-testid="lifecycle-metric-monitored">
                    <Text size="2" color="gray" as="div" mb="2">
                      Repositories Monitored
                    </Text>
                    <Heading size="7" className="iq-server-overview-tab__metric-value">
                      {dashboardSummary?.numberOfMonitoredRepositories ?? 0}
                    </Heading>
                  </Box>
                  <Box className="iq-server-overview-tab__metric" data-testid="lifecycle-metric-timeframe">
                    <Text size="2" color="gray" as="div" mb="2">
                      Activity Time Frame
                    </Text>
                    <Flex align="baseline" gap="2">
                      <Heading size="7" className="iq-server-overview-tab__metric-value">
                        {evalSettings?.activityTimeFrame ?? 30}
                      </Heading>
                      <Text size="2" color="gray">
                        days
                      </Text>
                    </Flex>
                  </Box>
                  <Box className="iq-server-overview-tab__metric" data-testid="lifecycle-metric-latest-versions">
                    <Text size="2" color="gray" as="div" mb="2">
                      Latest Deployed Versions
                    </Text>
                    <Flex align="baseline" gap="2">
                      <Heading size="7" className="iq-server-overview-tab__metric-value">
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
            <Button asChild variant="ghost" size="2" color="blue" className="iq-server-overview-tab__external-link">
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
    </Box>
  );
}

export default IqServerOverviewTab;
