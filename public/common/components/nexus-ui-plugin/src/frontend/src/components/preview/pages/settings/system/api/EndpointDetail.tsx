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
import { Badge, Box, Flex, Tabs, Text } from '@radix-ui/themes';

import type { EndpointAccessDot } from './utils/endpointAccess';
import type { MergedApiEndpoint } from './utils/mergeSwaggerPermissions';
import { GrantAccessTab } from './tabs/GrantAccessTab';
import { TryItTab } from './tabs/TryItTab';
import { WhoHasAccessTab } from './tabs/WhoHasAccessTab';

export interface EndpointDetailProps {
  row: MergedApiEndpoint | null;
  fullSwagger: Record<string, unknown> | null;
  access: EndpointAccessDot;
}

function formatPermissions(row: MergedApiEndpoint): string {
  const reqs = row.permission?.permissions ?? [];
  if (reqs.length === 0) {
    return row.permission?.authenticated ? 'None mapped (see documentation)' : 'None (may allow anonymous)';
  }
  const logical = reqs[0]?.logical === 'OR' ? 'OR' : 'AND';
  const parts = reqs.map((r) => r.permission);
  return logical === 'OR' ? parts.join(' OR ') : parts.join(' AND ');
}

export function EndpointDetail({ row, fullSwagger, access }: EndpointDetailProps) {
  const [tab, setTab] = useState('try');

  if (!row) {
    return (
      <Box className="api-endpoint-detail api-endpoint-detail--empty" p="4" aria-live="polite">
        <Text size="3" weight="medium" mb="2" as="div">
          Choose an endpoint
        </Text>
        <Text size="2" color="gray" mb="3" as="div">
          Click any operation in the list on the left. The detail panel opens with:
        </Text>
        <Box as="ul" style={{ margin: 0, paddingLeft: '1.25rem' }} mb="3">
          <li>
            <Text size="2">
              <Text weight="bold" as="span">
                Try It
              </Text>{' '}
              — run the request in the browser
            </Text>
          </li>
          <li>
            <Text size="2">
              <Text weight="bold" as="span">
                Who Has Access
              </Text>{' '}
              — roles and users that can call it
            </Text>
          </li>
          <li>
            <Text size="2">
              <Text weight="bold" as="span">
                Grant Access
              </Text>{' '}
              — guided wizard (requires role/user admin permissions)
            </Text>
          </li>
        </Box>
        <Text size="2" color="gray">
          Access dots in the list reflect your permissions when the permission map loaded successfully.
        </Text>
      </Box>
    );
  }

  const accessDenied = access === 'denied' || access === 'partial';

  return (
    <Box className="api-endpoint-detail" aria-live="polite" data-testid="api-endpoint-detail">
      <Tabs.Root value={tab} onValueChange={setTab}>
        <Box className="api-endpoint-detail__detail-bar">
          <Box className="api-endpoint-detail__header" mb="2">
            <Flex align="center" gap="2" wrap="wrap" mb="2">
              <Badge size="2" color="blue">
                {row.httpMethod}
              </Badge>
              <Text size="3" weight="medium" className="api-endpoint-detail__path">
                {row.fullPath}
              </Text>
            </Flex>
            <Text size="2" color="gray" mb="1">
              Required: {formatPermissions(row)}
            </Text>
            {row.summary && (
              <Text size="2" mt="2">
                {row.summary}
              </Text>
            )}
          </Box>
          <Tabs.List className="api-endpoint-detail__tabs-list">
            <Tabs.Trigger value="try">Try It</Tabs.Trigger>
            <Tabs.Trigger value="who">Who Has Access</Tabs.Trigger>
            <Tabs.Trigger value="grant">Grant Access</Tabs.Trigger>
          </Tabs.List>
        </Box>
        <Tabs.Content value="try">
          <Box pt="3">
            <TryItTab fullSwagger={fullSwagger} row={row} accessDenied={accessDenied} />
          </Box>
        </Tabs.Content>
        <Tabs.Content value="who">
          <Box pt="3">
            <WhoHasAccessTab row={row} active={tab === 'who'} />
          </Box>
        </Tabs.Content>
        <Tabs.Content value="grant">
          <Box pt="3">
            <GrantAccessTab row={row} active={tab === 'grant'} />
          </Box>
        </Tabs.Content>
      </Tabs.Root>
    </Box>
  );
}
