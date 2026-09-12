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
import { useRouter } from '@uirouter/react';
import { SettingsFormSection } from '../../../shared/form';
import {
  useComponentSecurity,
  type ComponentSecurityData,
  type ComponentSecurityStatus,
  type PolicyViolation,
} from './useComponentSecurity';

interface GASecurityTabProps {
  gaId: string;
  selectedVersion: string | null;
  /** When provided by parent, use instead of fetching */
  securityData?: ComponentSecurityData | null;
  securityLoading?: boolean;
  securityError?: string | null;
  securityStatus?: ComponentSecurityStatus;
  iqConnected?: boolean | null;
  onRefetch?: () => void;
}

/** Maximum rows shown in the violations table before truncation. */
const MAX_VIOLATIONS_SHOWN = 20;

/**
 * State that owns the IQ Server settings entry point. Navigation goes through the router by
 * state name, never by an assembled hash: `preview.admin.iq` is declared with `url:
 * '/iq-overview'`, so the `#preview/admin/iq` this used to assign matched no route and landed
 * the user on a 404.
 */
const IQ_SETTINGS_STATE = 'preview.admin.iq';

/**
 * Open a report URL in a new tab, but only when it is an absolute `http:`/`https:` URL.
 *
 * `reportUrl` arrives with the security payload, so it is server-supplied data rather than
 * something this component controls. Anything else — `javascript:`, `data:`, a relative path,
 * or an unparseable string — is discarded rather than handed to `window.open`.
 */
function openReportUrl(reportUrl: string | undefined): void {
  if (!reportUrl) return;
  let parsed: URL;
  try {
    parsed = new URL(reportUrl);
  } catch {
    return;
  }
  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return;
  window.open(reportUrl, '_blank', 'noopener,noreferrer');
}

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
            onClick={() => openReportUrl(data.reportUrl)}
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

/**
 * Centred icon + heading + body + optional action, used by every non-data state so the tab
 * always renders something recognisable rather than falling through to a blank panel.
 */
function SecurityEmptyState({
  heading,
  body,
  action,
  testId,
}: {
  heading: string;
  body: string;
  action?: React.ReactNode;
  testId: string;
}) {
  return (
    <Flex
      direction="column"
      align="center"
      justify="center"
      gap="4"
      style={{ minHeight: '200px', padding: 'var(--space-8)' }}
      data-testid={testId}
    >
      <Shield size={48} color="var(--gray-8)" />
      <Heading size="4" color="gray">
        {heading}
      </Heading>
      <Text size="2" color="gray" align="center">
        {body}
      </Text>
      {action}
    </Flex>
  );
}

interface GASecurityTabContentProps {
  selectedVersion: string | null;
  data: ComponentSecurityData | null;
  loading: boolean;
  error: string | null;
  status: ComponentSecurityStatus;
  iqConnected: boolean | null;
  refetch: () => void;
}

function GASecurityTabContent({
  selectedVersion,
  data,
  loading,
  error,
  status,
  iqConnected,
  refetch,
}: GASecurityTabContentProps) {
  const router = useRouter();

  const goToIqSettings = () => {
    router.stateService.go(IQ_SETTINGS_STATE);
  };

  // State 0: No version selected. === null, not truthiness: '' is the valid selected version
  // for versionless formats (raw), handled by State 0b.
  if (selectedVersion === null) {
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

  /*
   * State 0b: versionless format (raw). useComponentSecurity makes no request for these, because
   * IQ identifies a component by coordinates that include a version, so the hook stays `idle`.
   * Stated explicitly rather than left to fall through: State 1 below treats `idle` as loading,
   * so without this branch a versionless component would spin forever on a check that is never
   * going to run.
   */
  if (selectedVersion === '') {
    return (
      <Callout.Root color="gray">
        <Callout.Icon>
          <Shield size={16} />
        </Callout.Icon>
        <Callout.Text>
          Security evaluation is not available for components without a version.
        </Callout.Text>
      </Callout.Root>
    );
  }

  // State 1: Loading. `idle` counts as loading here: States 0 and 0b already handled the two
  // versions that produce no request, so reaching this with `idle` means a real version is
  // selected but the capabilities check has not resolved yet. Falling through would render a
  // resolved state — including the words "IQ Server is connected" — before anything was checked.
  if (loading || status === 'idle') {
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

  // State 5: Recoverable failure. `error` is always one of useComponentSecurity's own fixed
  // strings — never an API body, exception message, or IQ Server URL. Checked ahead of the
  // connectivity states: a failed capabilities call means the IQ state is unknown, and
  // claiming "not connected" there would send the user to reconfigure a working connection.
  if (error) {
    return (
      <Callout.Root color="red" data-testid="security-error">
        <Callout.Icon>
          <AlertCircle size={16} />
        </Callout.Icon>
        {/* Callout.Text is always a <p> in Radix Themes (asChild ignored); use Box for block layout. */}
        <Box className="rt-CalloutText">
          <Flex direction="column" gap="2">
            <Text>{error}</Text>
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

  // State 7: the capabilities endpoint is not part of this deployment (404). Every `/v1/iq`
  // resource lives in a `private/` module, so Nexus Repository Core ships this tab without the
  // endpoint. No CTA: there is no IQ settings page to send the user to, and no retry, because
  // the condition is permanent for the deployment.
  if (status === 'unsupported') {
    return (
      <SecurityEmptyState
        testId="security-unsupported"
        heading="Security Analysis Not Available"
        body="This deployment does not include IQ Server integration, so policy violation data cannot be shown for this component."
      />
    );
  }

  // State 8: the user lacks `nexus:settings:read`, which the IQ resource requires on every
  // method (403). Permanent for this user, so no retry — and no CTA, since they cannot reach
  // the IQ settings page either.
  if (status === 'forbidden') {
    return (
      <SecurityEmptyState
        testId="security-forbidden"
        heading="Security Analysis Not Available"
        body="Your account does not have permission to view the IQ Server connection status. Contact your administrator if you need security analysis for this component."
      />
    );
  }

  // State 4: IQ Server not configured, unreachable, or reporting connected: false.
  // `iqConnected === false` is honoured too, for callers still passing only the boolean.
  if (status === 'not-connected' || iqConnected === false) {
    return (
      <SecurityEmptyState
        testId="security-not-connected"
        heading="IQ Server Not Connected"
        body="Connect IQ Server to see policy violations and security analysis for this component."
        action={
          <Button variant="soft" size="2" onClick={goToIqSettings}>
            Connect IQ Server
          </Button>
        }
      />
    );
  }

  // State 4b: connected, but the instance has neither Lifecycle nor Firewall, so IQ can
  // return no policy data for this component however it is configured.
  if (status === 'not-entitled') {
    return (
      <SecurityEmptyState
        testId="security-not-entitled"
        heading="Security Analysis Not Available"
        body="The connected IQ Server does not include Sonatype Lifecycle or Sonatype Repository Firewall, which provide policy violation data for components."
        action={
          <Button variant="soft" size="2" onClick={goToIqSettings}>
            Connect IQ Server
          </Button>
        }
      />
    );
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

  // State 6: no evaluation data. Covers the expected `no-evaluation-data` status — IQ is
  // connected and entitled, but no per-component evaluation results can be retrieved — and
  // doubles as the terminal guard for any unforeseen state combination that produced no data.
  // Deliberately NOT rendered as a clean result: reporting zero violations for a component
  // that was never evaluated would be a false security assurance. Also the reason the tab can
  // never render blank: every path with no data ends here.
  if (!data) {
    return (
      <SecurityEmptyState
        testId="security-no-evaluation-data"
        heading="Evaluation Data Not Available"
        body="IQ Server is connected, but policy evaluation results for this component version could not be retrieved. View this component in IQ Server for its full security report."
      />
    );
  }

  // State 3: Connected, evaluated, no violations (clean component)
  return (
    <Callout.Root color="green" data-testid="security-clean">
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
                onClick={() => openReportUrl(data.reportUrl)}
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
  securityStatus,
  iqConnected: iqConnectedProp,
  onRefetch,
}: GASecurityTabProps) {
  const hasParentData =
    securityData !== undefined &&
    securityLoading !== undefined &&
    securityError !== undefined &&
    iqConnectedProp !== undefined &&
    onRefetch !== undefined;

  // Called unconditionally — an early return above a hook call breaks React's rules of hooks
  // and would crash on any render where `hasParentData` flipped. `enabled` suppresses the
  // duplicate request instead.
  const hookResult = useComponentSecurity({
    gaId,
    version: selectedVersion,
    enabled: !hasParentData,
  });

  if (hasParentData) {
    return (
      <GASecurityTabContent
        selectedVersion={selectedVersion}
        data={securityData ?? null}
        loading={securityLoading}
        error={securityError ?? null}
        // `idle` now renders as loading, so a parent that supplies the legacy props without a
        // status must not fall back to it — that would spin forever. Derive a resolved status
        // from the data it did supply instead.
        status={
          securityStatus ??
          (securityLoading ? 'checking' : securityData ? 'evaluated' : 'no-evaluation-data')
        }
        iqConnected={iqConnectedProp}
        refetch={onRefetch}
      />
    );
  }

  return (
    <GASecurityTabContent
      selectedVersion={selectedVersion}
      data={hookResult.data}
      loading={hookResult.loading}
      error={hookResult.error}
      status={hookResult.status}
      iqConnected={hookResult.iqConnected}
      refetch={hookResult.refetch}
    />
  );
}

export default GASecurityTab;
