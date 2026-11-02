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
import { Badge, Box, Button, Flex, Heading, Table, Text, VisuallyHidden } from '@radix-ui/themes';
import { ArrowUp } from 'lucide-react';

import '../quarantine/QuarantinePreviewView.scss';
import { ExampleBadge, ExampleDataCallout, ExampleFrame, UpgradeCallout } from '../quarantine/ExampleFrame';
import { ExamplePagination } from '../ExamplePagination';
import { EVALUATION_FIXTURE } from './evaluationFixtures';

const COUNT_BADGE_STYLE = { minWidth: 35, justifyContent: 'center' } as const;

/** Preview tier makes no network call, which is what makes the tab reviewable with no backend. */
export function EvaluationsPreviewView(): JSX.Element {
  const evaluation = EVALUATION_FIXTURE;
  return (
    <Flex direction="column" gap="4" className="firewall-security-preview" data-testid="evaluations-preview">
      <ExampleDataCallout />
      <UpgradeCallout>to run security evaluations against your own repositories and see the findings.</UpgradeCallout>
      <Flex align="center" justify="between" wrap="wrap" gap="3">
        <Flex align="center" gap="2">
          <Heading as="h2" size="5">
            Security Evaluations
          </Heading>
          <ExampleBadge />
        </Flex>
        {/* Illustrative: there is nothing to run until the user upgrades. */}
        <Button variant="surface" disabled style={{ cursor: 'not-allowed' }}>
          Run Evaluation Now
        </Button>
      </Flex>
      <Box>
        <ExampleFrame label="Example evaluation">
          <Table.Root variant="ghost" size="2">
            <caption>
              <VisuallyHidden>Example security evaluations</VisuallyHidden>
            </caption>
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeaderCell>
                  <Flex align="center" gap="1">
                    Evaluation Run
                    <ArrowUp size={14} aria-hidden="true" />
                  </Flex>
                </Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Vulnerabilities</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Summary</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell>Type</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell align="center">Affected Packages</Table.ColumnHeaderCell>
                <Table.ColumnHeaderCell align="center">Affected Repositories</Table.ColumnHeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              <Table.Row>
                <Table.Cell>
                  <Flex align="center" gap="2">
                    <Text size="2">{evaluation.evaluationRun}</Text>
                    <ExampleBadge />
                  </Flex>
                </Table.Cell>
                <Table.Cell>
                  <Flex align="center" wrap="wrap" gap="1">
                    <Badge
                      color="red"
                      variant="solid"
                      radius="full"
                      size="1"
                      title={`${evaluation.criticalCount} Critical CVEs`}
                      style={COUNT_BADGE_STYLE}
                    >
                      {evaluation.criticalCount}
                    </Badge>
                    <Badge
                      color="orange"
                      variant="solid"
                      radius="full"
                      size="1"
                      title={`${evaluation.highCount} High CVEs`}
                      style={COUNT_BADGE_STYLE}
                    >
                      {evaluation.highCount}
                    </Badge>
                  </Flex>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2">{evaluation.summary}</Text>
                </Table.Cell>
                <Table.Cell>
                  <Text size="2" color="gray">
                    {evaluation.type}
                  </Text>
                </Table.Cell>
                <Table.Cell align="center">
                  <Text size="2">{evaluation.affectedPackages}</Text>
                </Table.Cell>
                <Table.Cell align="center">
                  <Text size="2">{evaluation.affectedRepositories}</Text>
                </Table.Cell>
              </Table.Row>
            </Table.Body>
          </Table.Root>
        </ExampleFrame>
        <ExamplePagination rangeLabel="1-10 of 10" />
      </Box>
    </Flex>
  );
}
