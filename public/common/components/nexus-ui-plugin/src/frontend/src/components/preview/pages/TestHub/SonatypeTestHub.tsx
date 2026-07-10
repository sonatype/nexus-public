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

/**
 * SONATYPE INTERNAL — NOT INCLUDED IN PRODUCTION BUILDS
 *
 * SonatypeTestHub — index page for all internal test harness pages.
 * Access (append ?debug for filesystem assets): e.g. http://localhost:8081/?debug#preview/test
 * or http://localhost:7777/?debug#preview/test (cloud local).
 * 
 * Two tabs:
 * - Toggles: Feature toggles for local testing (e.g., force malware banner)
 * - Harnesses: Links to test harness pages for component states
 */

import React, { useState, useEffect } from 'react';
import { Box, Flex, Text, Heading, Card, Badge, Button, Tabs, Switch, Callout, Select } from '@radix-ui/themes';
import { FlaskConical, ExternalLink, ToggleLeft, TestTube2, Info, AlertTriangle } from 'lucide-react';
import { notifySessionExpiredFromRest } from '../../../../interface/api';

import './SonatypeTestHub.scss';

const TOGGLES_STORAGE_KEY = 'SONATYPE_TEST_TOGGLES';

interface TestToggle {
  id: string;
  label: string;
  description: string;
  defaultValue: boolean;
}

const TEST_TOGGLES: TestToggle[] = [
  {
    id: 'malwareBanner',
    label: 'Malware Alert Banner',
    description: 'Force-show the malware alert banner on the Dashboard with a fake count of 42 malicious components. Useful for testing the banner UI without actual malware data.',
    defaultValue: false,
  },
];

interface TestPage {
  id: string;
  route: string;
  title: string;
  description: string;
  tags: string[];
}

const TEST_PAGES: TestPage[] = [
  {
    id: 'test-dashboard',
    route: 'preview.test-dashboard',
    title: 'Dashboard — Protection Status Cards',
    description: 'Both dashboard cards in all scenarios: HealthCheckStatusCard (RHC states: disabled, no repos, clean, malware, issues) and FirewallStatusCard / MalwareStatusCardDisplay (states A–H).',
    tags: ['dashboard', 'health-check', 'firewall', 'protect'],
  },
  {
    id: 'test-malware-defense',
    route: 'preview.test-malware-defense',
    title: 'Protect Page (was Malware Defense)',
    description: 'Protect module tabs rendered in all 8 states — Overview, Quick Config, Audit. Includes renamed "Overview" tab (was "Main") and "Protect" nav label.',
    tags: ['protect', 'tabs', 'firewall', 'health-check'],
  },
  {
    id: 'test-search-fw',
    route: 'preview.test-search-fw',
    title: 'Browse — Firewall Cell',
    description: 'Every permutation of FirewallCell in the browse grid: loading, unsupported format, clean, critical/severe/moderate violations, quarantined, with/without report URL, error.',
    tags: ['firewall', 'browse', 'grid', 'modal'],
  },
  {
    id: 'test-search-hc',
    route: 'preview.test-search-hc',
    title: 'Browse — Health Check Cell',
    description: 'Every permutation of HealthCheckCell: loading/analyzing, not enabled, enabled clean, security issues only, license issues only, both, with/without report, error.',
    tags: ['health-check', 'browse', 'grid', 'modal'],
  },
];

// ---------------------------------------------------------------------------
// CE Hard Limit Banner Scenarios
// ---------------------------------------------------------------------------

// CE throttle status values (must match UsageHelper.js)
const OVER_LIMITS = 'Over limits';
const NEAR_LIMITS = '75% usage';
const UNDER_LIMITS = 'Under limits';

// CE threshold constants (must match UsageHelper.js)
const CE_REQUESTS_HARD_THRESHOLD = 100000;
const CE_COMPONENTS_HARD_THRESHOLD = 40000;

// Additional test-only thresholds not used in production
const CE_REQUESTS_SOFT_THRESHOLD = 20000;
const CE_COMPONENTS_SOFT_THRESHOLD = 100000;
const CE_UNIQUE_USER_SOFT_THRESHOLD = 100;

// Metric values for each usage level (test scenarios only)
const CE_COMPONENTS_NEAR = 75000;
const CE_REQUESTS_NEAR = 150000;
const CE_COMPONENTS_OVER = 40000;
const CE_REQUESTS_OVER = 100000;

const GRACE_PERIOD_DAYS_FUTURE = 7;
const GRACE_PERIOD_DAYS_PAST = -7;

interface CEScenario {
  id: string;
  label: string;
  throttlingStatus: string;
  gracePeriodDayOffset: number | null; // null = empty string (no grace period)
  components: number;
  requests: number;
}

const CE_SCENARIOS: CEScenario[] = [
  {
    id: 'underLimitsNoGrace',
    label: 'Under Limits — No Grace Period',
    throttlingStatus: UNDER_LIMITS,
    gracePeriodDayOffset: null,
    components: 0,
    requests: 0,
  },
  {
    id: 'nearLimitsNoGrace',
    label: 'Near Limits (75%) — No Grace Period',
    throttlingStatus: NEAR_LIMITS,
    gracePeriodDayOffset: null,
    components: CE_COMPONENTS_NEAR,
    requests: CE_REQUESTS_NEAR,
  },
  {
    id: 'overLimitsNoGrace',
    label: 'Over Limits — No Grace Period',
    throttlingStatus: OVER_LIMITS,
    gracePeriodDayOffset: null,
    components: CE_COMPONENTS_OVER,
    requests: CE_REQUESTS_OVER,
  },
  {
    id: 'underLimitsInGrace',
    label: 'Under Limits — In Grace Period',
    throttlingStatus: UNDER_LIMITS,
    gracePeriodDayOffset: GRACE_PERIOD_DAYS_FUTURE,
    components: 0,
    requests: 0,
  },
  {
    id: 'nearLimitsInGrace',
    label: 'Near Limits (75%) — In Grace Period',
    throttlingStatus: NEAR_LIMITS,
    gracePeriodDayOffset: GRACE_PERIOD_DAYS_FUTURE,
    components: CE_COMPONENTS_NEAR,
    requests: CE_REQUESTS_NEAR,
  },
  {
    id: 'overLimitsInGrace',
    label: 'Over Limits — In Grace Period',
    throttlingStatus: OVER_LIMITS,
    gracePeriodDayOffset: GRACE_PERIOD_DAYS_FUTURE,
    components: CE_COMPONENTS_OVER,
    requests: CE_REQUESTS_OVER,
  },
  {
    id: 'underLimitsPostGrace',
    label: 'Under Limits — Post Grace Period',
    throttlingStatus: UNDER_LIMITS,
    gracePeriodDayOffset: GRACE_PERIOD_DAYS_PAST,
    components: 0,
    requests: 0,
  },
  {
    id: 'nearLimitsPostGrace',
    label: 'Near Limits (75%) — Post Grace Period',
    throttlingStatus: NEAR_LIMITS,
    gracePeriodDayOffset: GRACE_PERIOD_DAYS_PAST,
    components: CE_COMPONENTS_NEAR,
    requests: CE_REQUESTS_NEAR,
  },
  {
    id: 'overLimitsPostGrace',
    label: 'Over Limits — Post Grace Period',
    throttlingStatus: OVER_LIMITS,
    gracePeriodDayOffset: GRACE_PERIOD_DAYS_PAST,
    components: CE_COMPONENTS_OVER,
    requests: CE_REQUESTS_OVER,
  },
];

function buildMockMetricData(components: number, requests: number) {
  return [
    {
      metricName: 'peak_requests_per_day',
      metricValue: requests,
      thresholds: [
        { thresholdName: 'HARD_THRESHOLD', thresholdValue: CE_REQUESTS_HARD_THRESHOLD },
        { thresholdName: 'SOFT_THRESHOLD', thresholdValue: CE_REQUESTS_SOFT_THRESHOLD },
      ],
      utilization: 'FREE_TIER',
      aggregates: [{ name: 'content_request_count', value: requests, period: 'peak_recorded_count_30d' }],
    },
    {
      metricName: 'component_total_count',
      metricValue: components,
      thresholds: [
        { thresholdName: 'HARD_THRESHOLD', thresholdValue: CE_COMPONENTS_HARD_THRESHOLD },
        { thresholdName: 'SOFT_THRESHOLD', thresholdValue: CE_COMPONENTS_SOFT_THRESHOLD },
      ],
      utilization: 'FREE_TIER',
      aggregates: [{ name: 'component_total_count', value: components, period: 'peak_recorded_count_30d' }],
    },
    {
      metricName: 'successful_last_24h',
      metricValue: 0,
      thresholds: [{ thresholdName: 'SOFT_THRESHOLD', thresholdValue: CE_UNIQUE_USER_SOFT_THRESHOLD }],
      utilization: 'FREE_TIER',
      aggregates: [{ name: 'unique_user_count', value: 0, period: 'peak_recorded_count_30d' }],
    },
  ];
}

function buildGracePeriodDate(dayOffset: number | null): string {
  if (dayOffset === null) return '';
  const date = new Date();
  date.setDate(date.getDate() + dayOffset);
  return date.toISOString();
}

function getExtJSState() {
  const app = (window as any).Ext?.getApplication?.();
  return {
    stateController: app?.getController?.('State'),
    stateStore: app?.getStore?.('State'),
  };
}

// Module-level variable to hold original state before CE scenario injection.
// In-memory only — lost on page refresh, which is the desired behavior.
let originalCEState: Record<string, unknown> | null = null;

function captureOriginalCEState() {
  const { stateController } = getExtJSState();
  if (!stateController?.getValue) return;

  originalCEState = {
    contentUsageEvaluationResult: stateController.getValue('contentUsageEvaluationResult'),
    'nexus.community.gracePeriodEnds': stateController.getValue('nexus.community.gracePeriodEnds'),
    'nexus.community.throttlingStatus': stateController.getValue('nexus.community.throttlingStatus'),
    status: stateController.getValue('status'),
  };
}

function applyCEScenario(scenario: CEScenario) {
  // Store test values in localStorage so components can pick them up
  // This is similar to how the malware banner test override works
  localStorage.setItem('SONATYPE_TEST_CE_THROTTLING_STATUS', scenario.throttlingStatus);
  localStorage.setItem('SONATYPE_TEST_CE_GRACE_PERIOD_ENDS', buildGracePeriodDate(scenario.gracePeriodDayOffset));
  localStorage.setItem('SONATYPE_TEST_CE_COMPONENTS', String(scenario.components));
  localStorage.setItem('SONATYPE_TEST_CE_REQUESTS', String(scenario.requests));

  // Also inject into ExtJS state for components that use it directly
  const { stateController, stateStore } = getExtJSState();
  if (stateController?.setValue) {
    // Disconnect state polling so server doesn't overwrite our mock data
    try {
      const state = (window as any).NX?.State?.controller?.();
      if (state?.statusProvider?.isConnected?.()) {
        state.statusProvider.disconnect();
      }
    } catch (e) {
      console.warn('[TestHub] Could not disconnect state updates:', e);
    }

    // Inject mock state — merge status to preserve existing fields
    const currentStatus = stateController.getValue('status') || {};
    stateController.setValue('contentUsageEvaluationResult',
      buildMockMetricData(scenario.components, scenario.requests));
    stateController.setValue('nexus.community.gracePeriodEnds',
      buildGracePeriodDate(scenario.gracePeriodDayOffset));
    stateController.setValue('nexus.community.throttlingStatus', scenario.throttlingStatus);
    stateController.setValue('status', { ...currentStatus, edition: 'COMMUNITY' });

    // Fire datachanged so ExtJS components pick up the new values
    if (stateStore?.fireEvent) {
      stateStore.fireEvent('datachanged', stateStore);
    }
  }

  // Force React components to re-render by dispatching a storage event
  window.dispatchEvent(new StorageEvent('storage', { key: 'SONATYPE_TEST_CE_THROTTLING_STATUS' }));
}

function clearCEScenario() {
  // Clear localStorage test overrides
  localStorage.removeItem('SONATYPE_TEST_CE_THROTTLING_STATUS');
  localStorage.removeItem('SONATYPE_TEST_CE_GRACE_PERIOD_ENDS');
  localStorage.removeItem('SONATYPE_TEST_CE_COMPONENTS');
  localStorage.removeItem('SONATYPE_TEST_CE_REQUESTS');

  const { stateController, stateStore } = getExtJSState();

  // Restore original state values
  try {
    if (originalCEState && stateController?.setValue) {
      stateController.setValue('contentUsageEvaluationResult',
        originalCEState.contentUsageEvaluationResult);
      stateController.setValue('nexus.community.gracePeriodEnds',
        originalCEState['nexus.community.gracePeriodEnds']);
      stateController.setValue('nexus.community.throttlingStatus',
        originalCEState['nexus.community.throttlingStatus']);
      stateController.setValue('status', originalCEState.status);

      if (stateStore?.fireEvent) {
        stateStore.fireEvent('datachanged', stateStore);
      }
    }
  } catch (e) {
    console.warn('[TestHub] Could not restore original CE state:', e);
  }

  // Reconnect state polling
  try {
    const state = (window as any).NX?.State?.controller?.();
    if (state?.statusProvider && !state.statusProvider.isConnected?.()) {
      state.statusProvider.connect();
    }
  } catch (e) {
    console.warn('[TestHub] Could not reconnect state updates:', e);
  }

  originalCEState = null;

  // Force React components to re-render by dispatching a storage event
  window.dispatchEvent(new StorageEvent('storage', { key: 'SONATYPE_TEST_CE_THROTTLING_STATUS' }));
}

function getToggles(): Record<string, boolean> {
  try {
    return JSON.parse(localStorage.getItem(TOGGLES_STORAGE_KEY) || '{}');
  } catch {
    return {};
  }
}

function setToggle(id: string, value: boolean) {
  const toggles = getToggles();
  toggles[id] = value;
  localStorage.setItem(TOGGLES_STORAGE_KEY, JSON.stringify(toggles));
  // Dispatch storage event for other tabs/components to pick up
  window.dispatchEvent(new StorageEvent('storage', { key: TOGGLES_STORAGE_KEY }));

  // Inject test values directly into ExtJS state for specific toggles
  // This ensures both Classic UI and Preview UI see the same test data
  if (id === 'malwareBanner') {
    if (value) {
      // Persist test count in localStorage so it survives ExtJS state refreshes
      localStorage.setItem('SONATYPE_TEST_MALWARE_BANNER', '42');
      // Clear any previous dismiss so the banner actually shows
      localStorage.removeItem('MALWARE_BANNER_DISMISSED');
      // Clear Classic UI dismiss cookie
      document.cookie = 'MALWARE_BANNER=; expires=Thu, 26 Feb 1950 00:00:00 UTC; path=/';
    } else {
      localStorage.removeItem('SONATYPE_TEST_MALWARE_BANNER');
    }

    try {
      const app = (window as any).Ext?.getApplication?.();
      const StateController = app?.getController?.('State');
      if (StateController?.setValue) {
        if (value) {
          const originalValue = StateController.getValue('nexus.malware.count');
          localStorage.setItem('SONATYPE_TEST_ORIGINAL_MALWARE_COUNT', JSON.stringify(originalValue));
          StateController.setValue('nexus.malware.count', { totalCount: 42 });
        } else {
          const originalStr = localStorage.getItem('SONATYPE_TEST_ORIGINAL_MALWARE_COUNT');
          const original = originalStr ? JSON.parse(originalStr) : { totalCount: 0 };
          StateController.setValue('nexus.malware.count', original);
          localStorage.removeItem('SONATYPE_TEST_ORIGINAL_MALWARE_COUNT');
        }

        const StateStore = app?.getStore?.('State');
        if (StateStore?.fireEvent) {
          StateStore.fireEvent('datachanged', StateStore);
        }
      }
    } catch (e) {
      console.warn('[TestHub] Could not inject test value into ExtJS state:', e);
    }
  }
}

function navigateTo(route: string) {
  const routeToHash: Record<string, string> = {
    'preview.test-dashboard': '#preview/test-dashboard',
    'preview.test-malware-defense': '#preview/test-malware-defense',
    'preview.test-search-fw': '#preview/test-search-fw',
    'preview.test-search-hc': '#preview/test-search-hc',
  };
  const hash = routeToHash[route];
  if (hash) window.open(`${window.location.origin}${window.location.pathname}?debug${hash}`, '_blank');
}

function TogglesTab() {
  const [toggles, setToggles] = useState<Record<string, boolean>>(getToggles);
  const [ceScenarioId, setCeScenarioId] = useState<string>('off');

  // Clear any leftover CE scenario state on mount
  // CE scenarios should not persist across page refreshes
  useEffect(() => {
    clearCEScenario();
  }, []);

  const handleToggle = (id: string, checked: boolean) => {
    setToggle(id, checked);
    setToggles({ ...toggles, [id]: checked });
  };

  const handleCEScenarioChange = (value: string) => {
    if (value === 'off') {
      clearCEScenario();
      setCeScenarioId('off');
    } else {
      const scenario = CE_SCENARIOS.find((s) => s.id === value);
      if (scenario) {
        // Capture original state only on first activation
        if (ceScenarioId === 'off') {
          captureOriginalCEState();
        }
        applyCEScenario(scenario);
        setCeScenarioId(value);
      }
    }
  };

  const activeCELabel = ceScenarioId !== 'off'
    ? CE_SCENARIOS.find((s) => s.id === ceScenarioId)?.label
    : null;

  return (
    <Box>
      <Callout.Root color="blue" size="1" mb="4">
        <Callout.Icon>
          <Info size={16} />
        </Callout.Icon>
        <Callout.Text>
          Toggles persist in localStorage and affect the live UI. Changes take effect immediately.
          Go to the Dashboard to see the Malware Banner when enabled.
        </Callout.Text>
      </Callout.Root>

      <Card className="sonatype-test-hub__toggle-card" mb="4">
        <Flex align="start" justify="between" gap="4" wrap="wrap">
          <Box>
            <Text size="3" weight="medium">Simulate session expired</Text>
            <Text size="2" color="gray" as="p" mt="1">
              Fires the same path as a REST 401 so the Preview UI session modal appears. This does not clear the
              server session; use Sign out to end a real login.
            </Text>
          </Box>
          <Button
            variant="outline"
            color="amber"
            size="2"
            type="button"
            onClick={() => notifySessionExpiredFromRest()}
          >
            Show session expired modal
          </Button>
        </Flex>
      </Card>

      <Flex direction="column" gap="3">
        {TEST_TOGGLES.map((toggle) => (
          <Card key={toggle.id} className="sonatype-test-hub__toggle-card">
            <Flex align="start" justify="between" gap="4">
              <Box>
                <Text size="3" weight="medium">{toggle.label}</Text>
                <Text size="2" color="gray" as="p" mt="1">{toggle.description}</Text>
              </Box>
              <Switch
                checked={toggles[toggle.id] ?? toggle.defaultValue}
                onCheckedChange={(checked) => handleToggle(toggle.id, checked)}
                size="2"
              />
            </Flex>
          </Card>
        ))}

        {/* CE Hard Limit Banners */}
        <Card className="sonatype-test-hub__toggle-card">
          <Flex align="start" justify="between" gap="4" wrap="wrap">
            <Box style={{ flex: 1, minWidth: 200 }}>
              <Text size="3" weight="medium">CE Hard Limit Banners</Text>
              <Text size="2" color="gray" as="p" mt="1">
                Simulate Community Edition hard limit banner states. Selecting a scenario overrides live
                server state and pauses state polling.
              </Text>
            </Box>
            <Select.Root value={ceScenarioId} onValueChange={handleCEScenarioChange}>
              <Select.Trigger
                data-testid="ce-scenario-select"
                placeholder="Select scenario…"
                style={{minWidth: 280}}
              />
              <Select.Content>
                <Select.Item value="off">Off</Select.Item>
                <Select.Group>
                  <Select.Label>No Grace Period</Select.Label>
                  {CE_SCENARIOS.filter((s) => s.gracePeriodDayOffset === null).map((s) => (
                    <Select.Item key={s.id} value={s.id}>{s.label}</Select.Item>
                  ))}
                </Select.Group>
                <Select.Group>
                  <Select.Label>In Grace Period</Select.Label>
                  {CE_SCENARIOS.filter((s) => s.gracePeriodDayOffset !== null && s.gracePeriodDayOffset > 0).map((s) => (
                    <Select.Item key={s.id} value={s.id}>{s.label}</Select.Item>
                  ))}
                </Select.Group>
                <Select.Group>
                  <Select.Label>Post Grace Period</Select.Label>
                  {CE_SCENARIOS.filter((s) => s.gracePeriodDayOffset !== null && s.gracePeriodDayOffset < 0).map((s) => (
                    <Select.Item key={s.id} value={s.id}>{s.label}</Select.Item>
                  ))}
                </Select.Group>
              </Select.Content>
            </Select.Root>
          </Flex>

          {ceScenarioId !== 'off' && (
            <Callout.Root color="amber" size="1" mt="3">
              <Callout.Icon>
                <AlertTriangle size={16} />
              </Callout.Icon>
              <Callout.Text>
                <strong>State updates paused.</strong> Server state polling is disconnected while a CE scenario
                is active. Live data will not refresh. Select "Off" to restore normal operation.{' '}
                <strong>Note:</strong> Logging out or refreshing the page will restart state updates and
                reset this selector.
              </Callout.Text>
            </Callout.Root>
          )}
        </Card>
      </Flex>

      {(Object.values(toggles).some(Boolean) || activeCELabel) && (
        <Callout.Root color="amber" size="1" mt="4">
          <Callout.Icon>
            <ToggleLeft size={16} />
          </Callout.Icon>
          <Callout.Text>
            <strong>Active toggles:</strong>{' '}
            {[
              ...TEST_TOGGLES.filter((t) => toggles[t.id]).map((t) => t.label),
              ...(activeCELabel ? [`CE Banners — ${activeCELabel}`] : []),
            ].join(', ')}
          </Callout.Text>
        </Callout.Root>
      )}
    </Box>
  );
}

function HarnessesTab() {
  return (
    <Box className="sonatype-test-hub__grid">
      {TEST_PAGES.map((page) => (
        <Card
          key={page.id}
          className="sonatype-test-hub__card"
          data-testid={`hub-card-${page.id}`}
        >
          <Heading size="4" mb="2">{page.title}</Heading>
          <Text size="2" color="gray" as="p" mb="3">{page.description}</Text>
          <Flex gap="1" wrap="wrap" mb="3">
            {page.tags.map((tag) => (
              <Badge key={tag} variant="soft" color="gray" size="1">{tag}</Badge>
            ))}
          </Flex>
          <Button
            variant="solid"
            color="amber"
            size="2"
            onClick={() => navigateTo(page.route)}
          >
            <ExternalLink size={14} /> Open
          </Button>
        </Card>
      ))}
    </Box>
  );
}

export default function SonatypeTestHub() {
  return (
    <Box className="sonatype-test-hub" p="4">
      <Flex align="center" gap="3" mb="5">
        <FlaskConical size={28} color="var(--amber-9)" />
        <Box>
          <Heading size="6">Sonatype Internal Test Harness</Heading>
          <Text size="2" color="amber" weight="medium">
            Not included in production builds · Enable with <code>localStorage.setItem('SONATYPE_INTERNAL','true')</code>
          </Text>
          <Text size="1" color="gray" mt="1" as="p">
            Sprint 19: "Malware Defense" renamed to "Protect" · RHC + Firewall unified module
          </Text>
        </Box>
      </Flex>

      <Tabs.Root defaultValue="toggles">
        <Tabs.List mb="4">
          <Tabs.Trigger value="toggles">
            <Flex align="center" gap="2">
              <ToggleLeft size={16} />
              Toggles
            </Flex>
          </Tabs.Trigger>
          <Tabs.Trigger value="harnesses">
            <Flex align="center" gap="2">
              <TestTube2 size={16} />
              Harnesses
            </Flex>
          </Tabs.Trigger>
        </Tabs.List>

        <Tabs.Content value="toggles">
          <TogglesTab />
        </Tabs.Content>

        <Tabs.Content value="harnesses">
          <HarnessesTab />
        </Tabs.Content>
      </Tabs.Root>
    </Box>
  );
}
