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
import {
  Box,
  Badge,
  Button,
  Callout,
  Card,
  Flex,
  Grid,
  Heading,
  Spinner,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import { AlertCircle, ExternalLink, Shield } from 'lucide-react';
import { SettingsFormSection } from '@/components/super/shared/form';
import { useComponentSecurity } from './useComponentSecurity';
import type { ComponentSecurityData, PolicyViolation } from './useComponentSecurity';

interface GASecurityTabProps {
  gaId: string;
  selectedVersion: string | null;
  /** When provided by parent, use instead of fetching */
  securityData?: ComponentSecurityData | null;
  securityLoading?: boolean;
  securityError?: string | null;
  iqConnected?: boolean | null;
  onRefetch?: () => void;
}

/** Maximum rows shown in the violations table before truncation. */
const MAX_VIOLATIONS_SHOWN = 20;

/**
 * Map a threat level (1–10) to a Radix badge color.
 */
function threatLevelColor(level: number): 'red' | 'orange' | 'amber' | 'gray' {
  if (level >= 9) return 'red';
  if (level >= 7) return 'orange';
  if (level >= 4) return 'amber';
  return 'gray';
}

// ---------------------------------------------------------------------------
// Sub-components (local — no separate files needed per spec)
// ---------------------------------------------------------------------------

const SEVERITY_CONFIG = [
  {
    key: 'critical',
    label: 'Critical',
    countKey: 'criticalCount' as const,
    activeBg: 'var(--red-3)',
    activeBorder: 'var(--red-6)',
    activeText: 'var(--red-11)',
  },
  {
    key: 'high',
    label: 'High',
    countKey: 'severeCount' as const,
    activeBg: 'var(--orange-3)',
    activeBorder: 'var(--orange-6)',
    activeText: 'var(--orange-11)',
  },
  {
    key: 'medium',
    label: 'Medium',
    countKey: 'moderateCount' as const,
    activeBg: 'var(--yellow-3)',
    activeBorder: 'var(--yellow-6)',
    activeText: 'var(--yellow-11)',
  },
  {
    key: 'low',
    label: 'Low',
    countKey: 'lowCount' as const,
    activeBg: 'var(--blue-3)',
    activeBorder: 'var(--blue-6)',
    activeText: 'var(--blue-11)',
  },
] as const;

function ViolationsTable({ violations }: { violations: PolicyViolation[] }) {
  const [showAll, setShowAll] = useState(false);
  const visible = showAll ? violations : violations.slice(0, MAX_VIOLATIONS_SHOWN);
  const hiddenCount = violations.length - MAX_VIOLATIONS_SHOWN;

  return (
    <SettingsFormSection title="Policy Violations" collapsible defaultCollapsed>
      <Table.Root variant="surface" size="1">
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Policy Name</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Threat Level</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Details</Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {visible.map((violation, idx) => {
            const firstReason =
              violation.constraintViolations[0]?.reasons[0] ?? '—';
            return (
              <Table.Row key={`${violation.policyName}-${idx}`}>
                <Table.Cell>
                  <Text size="2">{violation.policyName}</Text>
                </Table.Cell>
                <Table.Cell>
                  <Badge
                    color={threatLevelColor(violation.threatLevel)}
                    size="1"
                    variant="soft"
                  >
                    {violation.threatLevel}
                  </Badge>
                </Table.Cell>
                <Table.Cell>
                  <Tooltip content={firstReason}>
                    <Text
                      size="2"
                      style={{
                        maxWidth: '300px',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        display: 'block',
                      }}
                    >
                      {firstReason}
                    </Text>
                  </Tooltip>
                </Table.Cell>
              </Table.Row>
            );
          })}
        </Table.Body>
      </Table.Root>

      {!showAll && hiddenCount > 0 && (
        <Box mt="2">
          <Button variant="ghost" size="1" onClick={() => setShowAll(true)}>
            + {hiddenCount} more violations
          </Button>
        </Box>
      )}
    </SettingsFormSection>
  );
}

function ViolationSummaryCard({ data }: { data: ComponentSecurityData }) {
  const total =
    data.criticalCount +
    data.severeCount +
    data.moderateCount +
    data.lowCount;
  const highestThreat =
    data.violations.length > 0
      ? Math.max(...data.violations.map((v) => v.threatLevel))
      : null;

  return (
    <Card size="1">
      <Box p="2">
        <Flex align="center" justify="between" mb="3">
          <Heading size="4">Vulnerabilities</Heading>
          {total > 0 && highestThreat != null && (
            <Text size="2" color="gray">
              Highest Threat: <strong>{highestThreat}</strong>
            </Text>
          )}
        </Flex>

        <Grid columns="4" gap="2" mb="4">
          {SEVERITY_CONFIG.map(({ key, label, countKey, activeBg, activeBorder, activeText }) => {
            const count = data[countKey];
            const isActive = count > 0;
            return (
              <Box
                key={key}
                p="3"
                style={{
                  backgroundColor: isActive ? activeBg : 'var(--gray-3)',
                  border: `1px solid ${isActive ? activeBorder : 'var(--gray-6)'}`,
                  borderRadius: '6px',
                  opacity: isActive ? 1 : 0.5,
                  transition: 'all 0.2s ease',
                  cursor: 'default',
                }}
              >
                <Flex direction="column" gap="1" align="center">
                  <Text
                    size="3"
                    weight="bold"
                    style={{
                      color: isActive ? activeText : 'var(--gray-11)',
                    }}
                  >
                    {count}
                  </Text>
                  <Text
                    size="1"
                    style={{
                      color: isActive ? activeText : 'var(--gray-11)',
                    }}
                  >
                    {label}
                  </Text>
                </Flex>
              </Box>
            );
          })}
        </Grid>

        {data.reportUrl && (
          <Button
            variant="outline"
            size="2"
            onClick={() => window.open(data.reportUrl, '_blank')}
          >
            <ExternalLink size={14} />
            View Full Report in Lifecycle
          </Button>
        )}
      </Box>
    </Card>
  );
}

// ---------------------------------------------------------------------------
// Main component
// ---------------------------------------------------------------------------

interface GASecurityTabContentProps {
  selectedVersion: string | null;
  data: ComponentSecurityData | null;
  loading: boolean;
  error: string | null;
  iqConnected: boolean | null;
  refetch: () => void;
}

function GASecurityTabContent({
  selectedVersion,
  data,
  loading,
  error,
  iqConnected,
  refetch,
}: GASecurityTabContentProps) {
  // State 0: No version selected
  if (!selectedVersion) {
    return (
      <Callout.Root color="amber">
        <Callout.Icon>
          <AlertCircle size={16} />
        </Callout.Icon>
        <Callout.Text>
          Select a version from the <strong>Versions</strong> tab to view
          security information.
        </Callout.Text>
      </Callout.Root>
    );
  }

  // State 1: Loading
  if (loading) {
    return (
      <Flex
        direction="column"
        align="center"
        justify="center"
        gap="3"
        style={{ minHeight: '200px' }}
      >
        <Spinner size="3" />
        <Text size="2" color="gray">
          Evaluating component security...
        </Text>
      </Flex>
    );
  }

  // State 4: IQ Server not connected
  if (iqConnected === false) {
    return (
      <Flex
        direction="column"
        align="center"
        justify="center"
        gap="4"
        style={{ minHeight: '200px', padding: 'var(--space-8)' }}
      >
        <Shield size={48} color="var(--gray-8)" />
        <Heading size="4" color="gray">
          IQ Server Not Connected
        </Heading>
        <Text size="2" color="gray" align="center">
          Connect IQ Server to see policy violations and security analysis for
          this component.
        </Text>
        <Button
          variant="soft"
          size="2"
          onClick={() => {
            window.location.hash = '#preview/admin/iq';
          }}
        >
          Configure IQ Server
        </Button>
      </Flex>
    );
  }

  // State 5: Error
  if (error) {
    return (
      <Callout.Root color="red">
        <Callout.Icon>
          <AlertCircle size={16} />
        </Callout.Icon>
        {/* Callout.Text is always a <p> in Radix Themes (asChild ignored); use Box for block layout. */}
        <Box className="rt-CalloutText">
          <Flex direction="column" gap="2">
            <Text>Failed to load security data.</Text>
            <Text size="2">{error}</Text>
            <Box>
              <Button variant="ghost" size="1" onClick={refetch}>
                Try again
              </Button>
            </Box>
          </Flex>
        </Box>
      </Callout.Root>
    );
  }

  // Still waiting for initial connection check (iqConnected === null, not yet loading)
  if (iqConnected === null) {
    return null;
  }

  const hasViolations =
    data &&
    (data.criticalCount > 0 ||
      data.severeCount > 0 ||
      data.moderateCount > 0 ||
      data.lowCount > 0 ||
      data.violations.length > 0);

  // State 2: Connected, violations found
  if (data && hasViolations) {
    return (
      <Flex direction="column" gap="4">
        <ViolationSummaryCard data={data} />
        {data.violations.length > 0 && (
          <ViolationsTable violations={data.violations} />
        )}
      </Flex>
    );
  }

  // State 3: Connected, no violations (clean component)
  return (
    <Callout.Root color="green">
      <Callout.Icon>
        <Shield size={16} />
      </Callout.Icon>
      <Box className="rt-CalloutText">
        <Flex direction="column" gap="2">
          <Text>No policy violations found.</Text>
          <Text size="2" color="gray">
            This version passed all active policies in IQ Server.
          </Text>
          {data?.evaluationDate && (
            <Text size="1" color="gray">
              Evaluated: {data.evaluationDate}
            </Text>
          )}
          {data?.reportUrl && (
            <Box>
              <Button
                variant="ghost"
                size="1"
                onClick={() => window.open(data.reportUrl, '_blank')}
              >
                <ExternalLink size={12} />
                View in Lifecycle
              </Button>
            </Box>
          )}
        </Flex>
      </Box>
    </Callout.Root>
  );
}

/**
 * GASecurityTab — Security/policy violation information for a selected version.
 *
 * When securityData and related props are passed from parent, uses them (no duplicate fetch).
 * Otherwise fetches via useComponentSecurity.
 */
export function GASecurityTab({
  gaId,
  selectedVersion,
  securityData,
  securityLoading,
  securityError,
  iqConnected: iqConnectedProp,
  onRefetch,
}: GASecurityTabProps) {
  const hasParentData =
    securityData !== undefined &&
    securityLoading !== undefined &&
    securityError !== undefined &&
    iqConnectedProp !== undefined &&
    onRefetch !== undefined;

  if (hasParentData) {
    return (
      <GASecurityTabContent
        selectedVersion={selectedVersion}
        data={securityData ?? null}
        loading={securityLoading}
        error={securityError ?? null}
        iqConnected={iqConnectedProp}
        refetch={onRefetch}
      />
    );
  }

  const hookResult = useComponentSecurity({ gaId, version: selectedVersion });
  return (
    <GASecurityTabContent
      selectedVersion={selectedVersion}
      data={hookResult.data}
      loading={hookResult.loading}
      error={hookResult.error}
      iqConnected={hookResult.iqConnected}
      refetch={hookResult.refetch}
    />
  );
}

export default GASecurityTab;
