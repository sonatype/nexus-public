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
import { Box, Flex, Text, Heading, Card, Badge, Button, Tabs, Switch, Callout } from '@radix-ui/themes';
import { FlaskConical, ExternalLink, ToggleLeft, TestTube2, Info } from 'lucide-react';
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

  const handleToggle = (id: string, checked: boolean) => {
    setToggle(id, checked);
    setToggles({ ...toggles, [id]: checked });
  };

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
      </Flex>

      {Object.values(toggles).some(Boolean) && (
        <Callout.Root color="amber" size="1" mt="4">
          <Callout.Icon>
            <ToggleLeft size={16} />
          </Callout.Icon>
          <Callout.Text>
            <strong>Active toggles:</strong>{' '}
            {TEST_TOGGLES.filter((t) => toggles[t.id]).map((t) => t.label).join(', ')}
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
