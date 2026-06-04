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
import { Popover, Flex, Text, Box, Button, Separator, Heading } from '@radix-ui/themes';
import { Shield, ShieldCheck, Scale, ExternalLink, ChevronRight } from 'lucide-react';
import { SecurityStatusData } from './security.types';

import './SecurityPopover.scss';

export interface SecurityPopoverProps {
  /** The clickable element that triggers the popover */
  children: React.ReactNode;
  /** Repository name for context */
  repositoryName: string;
  /** Security data summary */
  data: SecurityStatusData;
  /** Callback to open the full report modal */
  onViewFullReport: () => void;
  /** Type of security data (Firewall or Health Check) */
  type: 'firewall' | 'health-check';
}

/**
 * SecurityPopover - Shared component for the "Popover + Modal" pattern.
 * Displays a quick summary of vulnerabilities and an action to see the full report.
 */
export function SecurityPopover({
  children,
  repositoryName,
  data,
  onViewFullReport,
  type
}: SecurityPopoverProps) {
  const { criticalComponentCount, severeComponentCount, moderateComponentCount } = data;
  const isHealthy = criticalComponentCount === 0 && severeComponentCount === 0 && moderateComponentCount === 0;

  const title = type === 'firewall' ? 'Firewall Protection' : 'Health Check Summary';

  return (
    <Popover.Root>
      <Popover.Trigger>
        {children}
      </Popover.Trigger>
      <Popover.Content
        size="2"
        width="320px"
        className={`security-popover ${type === 'firewall' ? 'security-popover--firewall' : ''}`}
      >
        <Flex direction="column" gap="3">
          <Box>
            <Flex align="center" gap="2" mb="1">
              <Shield size={16} color="var(--accent-9)" />
              <Heading size="3">{title}</Heading>
            </Flex>
            <Text size="1" color="gray">{repositoryName}</Text>
          </Box>

          <Separator size="4" />

          {isHealthy ? (
            <Flex align="center" gap="2" p="2" className="security-popover__status--safe">
              <ShieldCheck size={20} color="var(--green-9)" />
              <Box>
                <Text size="2" weight="bold" color="green">Secure</Text>
                <Text size="1" color="gray" as="p">No malicious components detected.</Text>
              </Box>
            </Flex>
          ) : (
            <Flex direction="column" gap="2">
              <MetricRow label="Critical" count={criticalComponentCount} color="red" />
              <MetricRow label="Severe" count={severeComponentCount} color="orange" />
              <MetricRow label="Moderate" count={moderateComponentCount} color="yellow" />
            </Flex>
          )}

          {data.quarantinedComponentCount > 0 && (
            <Flex
              align="center"
              gap="2"
              className={`security-popover__quarantine-alert ${type === 'firewall' ? 'security-popover__quarantine-alert--badge' : ''}`}
              p="2"
            >
              <Shield size={14} />
              <Text size="1" weight="medium">
                {data.quarantinedComponentCount} component{data.quarantinedComponentCount !== 1 ? 's' : ''} in quarantine
              </Text>
            </Flex>
          )}
          {type === 'firewall' && (
            <Box className="security-popover__policy-summary" p="2">
              <Text size="1" color="gray">
                Policy details in repository profile (Settings → Repositories)
              </Text>
            </Box>
          )}

          <Button 
            variant="soft" 
            size="2" 
            onClick={(e) => {
              e.stopPropagation();
              onViewFullReport();
            }}
            className="security-popover__action"
          >
            <ExternalLink size={14} />
            View Detailed Report
            <ChevronRight size={14} style={{ marginLeft: 'auto' }} />
          </Button>
        </Flex>
      </Popover.Content>
    </Popover.Root>
  );
}

function MetricRow({ label, count, color }: { label: string; count: number; color: any }) {
  if (count === 0) return null;
  return (
    <Flex justify="between" align="center" className={`security-popover__metric security-popover__metric--${color}`}>
      <Text size="2" weight="medium">{label}</Text>
      <Flex align="center" gap="2">
        <Text size="2" weight="bold" color={color}>{count}</Text>
        <Box className={`security-popover__dot security-popover__dot--${color}`} />
      </Flex>
    </Flex>
  );
}
