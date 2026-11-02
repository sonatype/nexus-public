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
import { Button, Callout, Flex, Heading, Switch, Table, Text, VisuallyHidden } from '@radix-ui/themes';
import { Info } from 'lucide-react';

import { UPGRADE_URL } from '../../banners/bannerConstants';
import '../quarantine/QuarantinePreviewView.scss';
import { ExampleBadge, ExampleDataCallout, ExampleFrame } from '../quarantine/ExampleFrame';
import { useProUpsell } from '../ProUpsellContext';
import { POLICY_FIXTURE } from './policyFixtures';

/** Preview tier makes no network call, which is what makes the tab reviewable with no backend. */
export function PoliciesPreviewView(): JSX.Element {
  const policy = POLICY_FIXTURE;
  const proUpsell = useProUpsell();
  return (
    <Flex direction="column" gap="4" className="firewall-security-preview" data-testid="policies-preview">
      {proUpsell && (
        <Callout.Root color="blue" variant="soft" size="2" data-testid="policies-info-callout">
          <Callout.Icon>
            <Info size={16} aria-hidden="true" />
          </Callout.Icon>
          <Callout.Text>
            Nexus Repository Pro + adds configurable security policies, age-based rules, and quarantine — managed
            alongside your existing policy set.
          </Callout.Text>
        </Callout.Root>
      )}
      <Flex direction="column" gap="3">
        <ExampleDataCallout />
        <Flex align="center" gap="2">
          <Heading as="h2" size="5">
            Policy
          </Heading>
          <ExampleBadge />
        </Flex>
        <ExampleFrame label="Example policy">
          <Table.Root variant="ghost" size="2">
            <caption>
              <VisuallyHidden>Example policies</VisuallyHidden>
            </caption>
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell width="10%">Block</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell width="30%">Policy Name</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell width="12%">Type</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Criteria</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              <Table.Row>
                <Table.Cell>
                  {/* Read-only: disabled so the toggle cannot be mistaken for a working control. */}
                  <Switch size="1" checked={policy.blocked} disabled aria-label={`Block ${policy.name}`} />
                </Table.Cell>
                <Table.Cell>
                  <Flex align="center" gap="2">
                    <Text size="2" weight="medium">
                      {policy.name}
                    </Text>
                    <ExampleBadge />
                  </Flex>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2">{policy.type}</Text>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2" color="gray">
                    {policy.criteria}
                  </Text>
                </Table.Cell>
              </Table.Row>
            </Table.Body>
          </Table.Root>
        </ExampleFrame>
      </Flex>
      {proUpsell && (
        <Flex direction="column" align="start" gap="2">
          <Text size="2" color="gray">
            Upgrade to Nexus Repository Pro + to configure the full policy set and apply it to your own repositories.
          </Text>
          <Button asChild>
            <a href={UPGRADE_URL}>Upgrade to Nexus Repository Pro +</a>
          </Button>
        </Flex>
      )}
    </Flex>
  );
}
