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
import { Badge, Box, Button, Callout, Flex, Heading, Table, Tabs, Text, VisuallyHidden } from '@radix-ui/themes';
import { Info } from 'lucide-react';

import { ThreatLevelBadge } from '../../../../shared/ThreatLevelBadge';
import '../quarantine/QuarantinePreviewView.scss';
import { ExampleBadge, ExampleDataCallout, ExampleFrame } from '../quarantine/ExampleFrame';
import { EXAMPLE_WAIVER, EXAMPLE_WAIVER_REQUEST } from './waiverExamples';

// Plain anchors: App.scss resets link colour, so the accent colour is set inline.
const NOTE_LINK_STYLE = { color: 'var(--accent-11)', textDecoration: 'none' };

function ExistingTab(): JSX.Element {
  const waiver = EXAMPLE_WAIVER;
  return (
    <Flex direction="column" gap="4" pt="4">
      <ExampleDataCallout />
      <Flex align="center" gap="2">
        <Heading as="h2" size="5">
          Existing Waivers
        </Heading>
        <ExampleBadge />
      </Flex>
      <ExampleFrame label="Example waiver">
        <Table.Root variant="ghost" size="2">
          <caption>
            <VisuallyHidden>Example waivers</VisuallyHidden>
          </caption>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Date Created</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Expiration</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Policy</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Scope</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Components</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Upgrade</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Actions</Table.ColumnHeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              <Table.Cell>
                <Flex align="center" gap="2">
                  <ThreatLevelBadge threatLevel={waiver.threatLevel} />
                  <ExampleBadge />
                </Flex>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{waiver.createdOn}</Text>
              </Table.Cell>
              <Table.Cell>
                <Badge color="green" variant="soft" size="1">
                  {waiver.expiresOn}
                </Badge>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{waiver.policyName}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{waiver.scope}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">
                  {waiver.component}
                </Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">
                  {waiver.upgradeVersion}
                </Text>
              </Table.Cell>
              <Table.Cell>
                {/* Illustrative row: revoking it would mislead. */}
                <Button size="1" variant="surface" color="gray" disabled>
                  Revoke
                </Button>
              </Table.Cell>
            </Table.Row>
          </Table.Body>
        </Table.Root>
      </ExampleFrame>
    </Flex>
  );
}

function RequestedTab(): JSX.Element {
  const request = EXAMPLE_WAIVER_REQUEST;
  return (
    <Flex direction="column" gap="4" pt="4">
      <Callout.Root color="gray" variant="soft" size="1" data-testid="waiver-permission-note">
        <Callout.Icon>
          <Info size={16} aria-hidden="true" />
        </Callout.Icon>
        <Callout.Text>
          No role currently carries the Apply Waivers permission. Approving a waiver in a deployed instance requires
          a role with that privilege. <a href="#preview/admin/security/roles" style={NOTE_LINK_STYLE}>Firewall access</a>
          {' · '}
          <a href="#preview/admin/security/users" style={NOTE_LINK_STYLE}>Manage users</a>
        </Callout.Text>
      </Callout.Root>
      <ExampleFrame label="Example waiver request">
        <Table.Root variant="ghost" size="2">
          <caption>
            <VisuallyHidden>Example waiver requests</VisuallyHidden>
          </caption>
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Requested by</Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell>Reason</Table.ColumnHeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              <Table.Cell>
                <Flex align="center" gap="2">
                  <ThreatLevelBadge threatLevel={request.threatLevel} />
                  <ExampleBadge />
                </Flex>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{request.component}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{request.repository}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">
                  {request.requestedBy}
                </Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">
                  {request.reason}
                </Text>
              </Table.Cell>
            </Table.Row>
          </Table.Body>
        </Table.Root>
      </ExampleFrame>
    </Flex>
  );
}

/** Preview tier makes no network call, which is what makes the tab reviewable with no backend. */
export function WaiversPreviewView(): JSX.Element {
  return (
    <Box className="firewall-security-preview" data-testid="waivers-preview">
      <Tabs.Root defaultValue="existing">
        <Tabs.List>
          <Tabs.Trigger value="existing">Existing</Tabs.Trigger>
          <Tabs.Trigger value="requested">Requested</Tabs.Trigger>
        </Tabs.List>
        <Tabs.Content value="existing">
          <ExistingTab />
        </Tabs.Content>
        <Tabs.Content value="requested">
          <RequestedTab />
        </Tabs.Content>
      </Tabs.Root>
    </Box>
  );
}
