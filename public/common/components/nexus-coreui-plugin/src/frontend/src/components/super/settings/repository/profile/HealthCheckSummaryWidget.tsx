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
import { Box, Card, Flex, Text } from '@radix-ui/themes';
import { ShieldCheck, Shield } from 'lucide-react';
import type { HealthCheckData, FirewallData } from './hooks/useRepositoryProfile';

export interface HealthCheckSummaryWidgetProps {
  repositoryName: string;
  healthCheck: HealthCheckData | null;
  firewall: FirewallData | null;
  onSelectTab: (tab: 'health-check' | 'firewall-report') => void;
  /** Hide for group repositories */
  isGroup?: boolean;
}

/**
 * Summary widget for Health Check + Firewall metrics in repo profile header.
 * Compact, scannable; click to switch to that tab.
 */
export function HealthCheckSummaryWidget({
  repositoryName,
  healthCheck,
  firewall,
  onSelectTab,
  isGroup = false,
}: HealthCheckSummaryWidgetProps): JSX.Element | null {
  if (isGroup) return null;

  const hcSecurity = healthCheck?.securityIssueCount ?? 0;
  const hcLicense = healthCheck?.licenseIssueCount ?? 0;

  const fwQuarantine = firewall?.quarantinedComponentCount ?? 0;
  const fwEnabled = firewall?.enabled ?? false;

  return (
    <Flex gap="3" wrap="wrap">
      <Card
        onClick={() => onSelectTab('health-check')}
        style={{
          cursor: 'pointer',
          flex: 1,
          minWidth: '180px',
          padding: 'var(--space-3)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          transition: 'background-color 0.15s'
        }}
      >
        <Flex align="center" gap="2">
          <ShieldCheck size={18} color="var(--accent-9)" />
          <Box>
            <Text size="1" color="gray">Health Check</Text>
            <Flex gap="2" align="center">
              {hcSecurity > 0 && <Text size="2" weight="bold" color="red">{hcSecurity} vuln{hcSecurity !== 1 ? 's' : ''}</Text>}
              {hcLicense > 0 && <Text size="2" weight="bold" color="amber">{hcLicense} license</Text>}
              {hcSecurity === 0 && hcLicense === 0 && (
                <Text size="2" color="green">No issues</Text>
              )}
            </Flex>
          </Box>
        </Flex>
        <Text size="1" color="blue" style={{ cursor: 'pointer' }}>
          View report →
        </Text>
      </Card>
      <Card
        onClick={() => onSelectTab('firewall-report')}
        style={{
          cursor: 'pointer',
          flex: 1,
          minWidth: '180px',
          padding: 'var(--space-3)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          transition: 'background-color 0.15s'
        }}
      >
        <Flex align="center" gap="2">
          <Shield size={18} color="var(--orange-9)" />
          <Box>
            <Text size="1" color="gray">Firewall</Text>
            <Flex gap="2" align="center">
              {fwEnabled ? (
                <>
                  <Text size="2" color="green">Protected</Text>
                  {fwQuarantine > 0 && <Text size="2" color="orange">· {fwQuarantine} quarantine</Text>}
                </>
              ) : (
                <Text size="2" color="gray">Unprotected</Text>
              )}
            </Flex>
          </Box>
        </Flex>
        <Text size="1" color="blue" style={{ cursor: 'pointer' }}>
          {fwEnabled ? 'View report →' : 'Enable →'}
        </Text>
      </Card>
    </Flex>
  );
}

