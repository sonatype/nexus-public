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
import { Dialog, Flex, Text, Box, Button, Grid, Badge, Separator, Heading } from '@radix-ui/themes';
import { Shield, ExternalLink, ShieldCheck, X } from 'lucide-react';
import { SecurityStatusData } from './security.types';

import './SecurityReportModal.scss';

export interface SecurityReportModalProps {
  /** Repository name */
  repositoryName: string;
  /** Firewall/Security data */
  data?: SecurityStatusData;
  /** Whether the modal is open */
  isOpen: boolean;
  /** Callback when modal is closed */
  onClose: () => void;
  /** Optional title override */
  title?: string;
  /** Report type for visual differentiation and policy summary (firewall vs health-check) */
  type?: 'firewall' | 'health-check';
}

/**
 * SecurityReportModal - Displays detailed security analysis for a repository.
 * Aligned with AgentUX's design for the Protected Repo Upsell initiative.
 *
 * @deprecated Use full-page Health Report and Firewall Report routes instead.
 * HealthCheckCell and FirewallCell now navigate to preview.browse.health-report
 * and preview.browse.firewall-report rather than opening this modal.
 */
export function SecurityReportModal({
  repositoryName,
  data,
  isOpen,
  onClose,
  title = 'Security Report',
  type = 'health-check',
}: SecurityReportModalProps) {
  if (!data) return null;

  const hasIssues = data.criticalComponentCount > 0 || data.severeComponentCount > 0 || data.moderateComponentCount > 0;
  const isFirewall = type === 'firewall';
  const hasQuarantine = (data.quarantinedComponentCount ?? 0) > 0;

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <Dialog.Content
        maxWidth="800px"
        className={`security-report-modal ${isFirewall ? 'security-report-modal--firewall' : ''}`}
      >
        <Flex justify="between" align="center" mb="4">
          <Dialog.Title m="0">
            <Flex align="center" gap="3">
              <Shield size={24} color="var(--accent-9)" />
              <Box>
                <Heading size="5">{title}</Heading>
                <Text size="2" color="gray">{repositoryName}</Text>
              </Box>
            </Flex>
          </Dialog.Title>
          <Dialog.Close>
            <Button variant="ghost" color="gray" onClick={onClose}>
              <X size={20} />
            </Button>
          </Dialog.Close>
        </Flex>

        <Box className="security-report-modal__body">
          {/* Policy Summary - Firewall only */}
          {isFirewall && (
            <Box mb="6" p="4" className="security-report-modal__policy-summary">
              <Flex direction="column" gap="2">
                <Text size="2" weight="bold">Policy Summary</Text>
                <Text size="2" color="gray">
                  Quarantined components are blocked by the repository firewall policy. View the repository profile
                  in Settings → Repositories for policy configuration and auto-release settings.
                </Text>
              </Flex>
            </Box>
          )}

          {/* Quarantine Badge - prominent when components in quarantine */}
          {hasQuarantine && (
            <Box mb="6">
              <Flex
                align="center"
                gap="3"
                p="4"
                className={`security-report-modal__quarantine-badge ${isFirewall ? 'security-report-modal__quarantine-badge--firewall' : ''}`}
              >
                <Shield size={24} color="var(--orange-9)" />
                <Box>
                  <Text size="3" weight="bold">
                    {data.quarantinedComponentCount} component{(data.quarantinedComponentCount ?? 0) !== 1 ? 's' : ''} in quarantine
                  </Text>
                  <Text size="2" color="gray">
                    {isFirewall
                      ? 'Blocked by firewall policy. Configure auto-release in repository settings.'
                      : 'These components require review.'}
                  </Text>
                </Box>
              </Flex>
            </Box>
          )}

          {/* Summary Stats */}
          <Grid columns="3" gap="4" mb="6">
            <SummaryCard
              label="Critical"
              value={data.criticalComponentCount}
              color="red"
              description={isFirewall ? 'Highest risk violations' : 'Highest risk vulnerabilities'}
            />
            <SummaryCard
              label="Severe"
              value={data.severeComponentCount}
              color="orange"
              description={isFirewall ? 'High risk violations' : 'High risk vulnerabilities'}
            />
            <SummaryCard
              label="Moderate"
              value={data.moderateComponentCount}
              color="yellow"
              description={isFirewall ? 'Medium risk violations' : 'Medium risk vulnerabilities'}
            />
          </Grid>

          <Separator size="4" mb="6" />

          {/* Detailed Report Section */}
          <Box mb="6">
            <Heading size="4" mb="3">Analysis Details</Heading>
            {data.reportUrl ? (
              <Box className="security-report-modal__iframe-wrapper">
                <iframe
                  src={data.reportUrl}
                  title={`Security report for ${repositoryName}`}
                  width="100%"
                  height="450px"
                  style={{ border: 'none', borderRadius: 'var(--radius-3)' }}
                  sandbox="allow-same-origin allow-scripts"
                />
              </Box>
            ) : (
              <Flex direction="column" align="center" justify="center" p="8" className="security-report-modal__empty">
                <ShieldCheck size={48} color="var(--gray-7)" mb="3" />
                <Text size="3" weight="medium" color="gray">
                  {hasIssues ? 'Full report data is being processed.' : 'No security issues detected.'}
                </Text>
                <Text size="2" color="gray">
                  Run a new analysis to update these results.
                </Text>
              </Flex>
            )}
          </Box>
        </Box>

        <Flex gap="3" justify="end" mt="4">
          {data.reportUrl && (
            <Button variant="soft" asChild>
              <a href={data.reportUrl} target="_blank" rel="noopener noreferrer">
                <ExternalLink size={16} />
                View Full Report
              </a>
            </Button>
          )}
          <Dialog.Close>
            <Button variant="solid" onClick={onClose}>Close</Button>
          </Dialog.Close>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}

function SummaryCard({ label, value, color, description }: { label: string; value: number; color: any; description: string }) {
  return (
    <Card p="4" className={`security-report-modal__card security-report-modal__card--${color}`}>
      <Flex direction="column" align="center" gap="1">
        <Text size="2" weight="bold" color={color} style={{ textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          {label}
        </Text>
        <Heading size="8" style={{ color: `var(--${color}-9)` }}>
          {value.toLocaleString()}
        </Heading>
        <Text size="1" color="gray" align="center">
          {description}
        </Text>
      </Flex>
    </Card>
  );
}

// Internal Card helper since @radix-ui/themes Card might not be enough
function Card({ children, p, className, style }: any) {
  return (
    <Box 
      p={p} 
      className={className} 
      style={{ 
        background: 'var(--color-surface)', 
        border: '1px solid var(--gray-6)', 
        borderRadius: 'var(--radius-3)',
        ...style 
      }}
    >
      {children}
    </Box>
  );
}
