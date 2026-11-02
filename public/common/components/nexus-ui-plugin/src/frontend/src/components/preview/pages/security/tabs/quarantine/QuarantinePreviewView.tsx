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


import React, { useState } from 'react';
import { Box, Button, Flex, IconButton, Table, Tabs, Text, VisuallyHidden } from '@radix-ui/themes';
import { ChevronDown, ChevronRight, Layers } from 'lucide-react';

import { ThreatLevelBadge } from '../../../../shared/ThreatLevelBadge';
import { UPGRADE_URL } from '../../banners/bannerConstants';
import './QuarantinePreviewView.scss';
import { ExampleBadge, ExampleDataCallout, ExampleFrame, UpgradeCallout } from './ExampleFrame';
import { useProUpsell } from '../ProUpsellContext';
import { CACHED_RISK_FIXTURES, CONTAINER_FIXTURES, QUARANTINE_FIXTURES } from './quarantineFixtures';

function QuarantineTab(): JSX.Element {
  const proUpsell = useProUpsell();
  return (
    <Flex direction="column" gap="4" pt="4">
      <ExampleDataCallout />
      <UpgradeCallout />
      <Flex align="center">
        <ExampleBadge />
      </Flex>
      <ExampleFrame label="Example quarantine entry">
        <Table.Root variant="ghost" size="2">
          <caption>
            <VisuallyHidden>Example quarantined components</VisuallyHidden>
          </caption>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell>Severity</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Policy Name</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Quarantine Time</Table.ColumnHeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {QUARANTINE_FIXTURES.map((item) => (
              <Table.Row key={item.id}>
                <Table.Cell>
                  <Flex align="center" gap="2">
                    <ThreatLevelBadge threatLevel={item.threatLevel} />
                    <ExampleBadge />
                  </Flex>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2" color="blue">
                    {item.component}
                  </Text>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2">{item.repository}</Text>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2">{item.policyName}</Text>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2">{item.quarantinedOn}</Text>
                </Table.Cell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table.Root>
      </ExampleFrame>
      {proUpsell && (
        <Flex direction="column" align="start" gap="2">
          <Text size="2" color="gray">
            Upgrade to Nexus Repository Pro + to quarantine malicious and policy-violating components in your own
            repositories, and review every blocked request.
          </Text>
          <Button asChild>
            <a href={UPGRADE_URL}>
              Upgrade to Nexus Repository Pro +
            </a>
          </Button>
        </Flex>
      )}
    </Flex>
  );
}

function CachedRiskTab(): JSX.Element {
  return (
    <Flex direction="column" gap="3" pt="4">
      <Flex align="center">
        <ExampleBadge />
      </Flex>
      <Text size="2" color="gray">
        Already cached in your repositories and pulled by your developers.
      </Text>
      <ExampleFrame label="Example cached component">
        <Table.Root variant="ghost" size="2">
          <caption>
            <VisuallyHidden>Example cached components</VisuallyHidden>
          </caption>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell>Downloads (30d)</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Severity</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>CVE</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {CACHED_RISK_FIXTURES.map((item) => (
              <Table.Row key={item.id}>
                <Table.Cell>
                  <Flex align="center" gap="2">
                    <Text size="2" weight="medium">
                      {item.downloads.toLocaleString('en-US')}
                    </Text>
                    <ExampleBadge />
                  </Flex>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2">{item.component}</Text>
                </Table.Cell>
                <Table.Cell>
                  <ThreatLevelBadge threatLevel={item.threatLevel} />
                </Table.Cell>
                <Table.Cell>
                  <Text size="2" color="gray">
                    {item.cve}
                  </Text>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2">{item.repository}</Text>
                </Table.Cell>
              </Table.Row>
            ))}
          </Table.Body>
        </Table.Root>
      </ExampleFrame>
    </Flex>
  );
}

function ContainersTab(): JSX.Element {
  const [expanded, setExpanded] = useState<Record<string, boolean>>(
    Object.fromEntries(CONTAINER_FIXTURES.map((item) => [item.id, true]))
  );

  return (
    <Flex direction="column" gap="4" pt="4">
      <ExampleDataCallout />
      <UpgradeCallout />
      <ExampleFrame label="Example quarantined container">
        <Table.Root variant="ghost" size="2">
          <caption>
            <VisuallyHidden>Example quarantined containers</VisuallyHidden>
          </caption>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell>
                <VisuallyHidden>Expand</VisuallyHidden>
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Image</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Digest</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Policy</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Quarantine Time</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {CONTAINER_FIXTURES.flatMap((item) => {
              const open = expanded[item.id];
              const rows = [
                <Table.Row key={item.id}>
                  <Table.Cell>
                    <IconButton
                      variant="ghost"
                      color="gray"
                      size="1"
                      aria-label={`${open ? 'Collapse' : 'Expand'} ${item.image}`}
                      aria-expanded={open}
                      onClick={() => setExpanded((prev) => ({ ...prev, [item.id]: !prev[item.id] }))}
                    >
                      {open ? <ChevronDown size={14} aria-hidden="true" /> : <ChevronRight size={14} aria-hidden="true" />}
                    </IconButton>
                  </Table.Cell>
                  <Table.Cell>
                    <Flex align="center" gap="2">
                      <Text size="2">{item.image}</Text>
                      <ExampleBadge />
                    </Flex>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2" color="gray">
                      {item.digest}
                    </Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{item.policyName}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{item.quarantinedOn}</Text>
                  </Table.Cell>
                  <Table.Cell>
                    <Text size="2">{item.repository}</Text>
                  </Table.Cell>
                </Table.Row>,
              ];
              if (open) {
                rows.push(
                  <Table.Row key={`${item.id}-layer`}>
                    <Table.Cell />
                    <Table.Cell colSpan={5}>
                      <Flex align="center" wrap="wrap" gap="3" py="1">
                        <Flex align="center" gap="2">
                          <Layers size={14} aria-hidden="true" />
                          <Text size="2" color="gray">
                            Layer {item.layer.index} of {item.layer.total}
                          </Text>
                        </Flex>
                        <ThreatLevelBadge threatLevel={item.layer.threatLevel} />
                        <Text size="2" color="blue">
                          {item.layer.component}
                        </Text>
                      </Flex>
                    </Table.Cell>
                  </Table.Row>
                );
              }
              return rows;
            })}
          </Table.Body>
        </Table.Root>
      </ExampleFrame>
    </Flex>
  );
}

export function QuarantinePreviewView(): JSX.Element {
  return (
    <Box className="firewall-security-preview" data-testid="quarantine-preview">
      <Tabs.Root defaultValue="quarantine">
        <Tabs.List>
          <Tabs.Trigger value="quarantine">Quarantine</Tabs.Trigger>
          <Tabs.Trigger value="cached-risk">Cached Risk</Tabs.Trigger>
          <Tabs.Trigger value="containers">Containers</Tabs.Trigger>
        </Tabs.List>
        <Tabs.Content value="quarantine">
          <QuarantineTab />
        </Tabs.Content>
        <Tabs.Content value="cached-risk">
          <CachedRiskTab />
        </Tabs.Content>
        <Tabs.Content value="containers">
          <ContainersTab />
        </Tabs.Content>
      </Tabs.Root>
    </Box>
  );
}
