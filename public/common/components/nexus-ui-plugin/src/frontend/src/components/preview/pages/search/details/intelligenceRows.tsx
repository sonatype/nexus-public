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
import { Badge, Flex, Text, Tooltip } from '@radix-ui/themes';

import {
  formatComponentAge,
  formatComponentPopularity,
  licenseThreatSeverity,
  type ComponentAlertsStatus,
  type ComponentIntelligenceData,
  type ComponentIntelligenceStatus,
  type LicenseThreatSeverity,
} from './useComponentIntelligence';

/** Placeholder for a detail whose value is absent, matching the Repository row's convention. */
export const UNAVAILABLE = '—';

/**
 * Tooltips for the states in which the Repository Health Check signals carry no value. These are
 * the Classic strings (`HealthCheckInfo_*_Tooltip` in the coreui and proui PluginStrings), so the
 * two UIs explain the same condition the same way.
 */
export const QUOTA_TOOLTIP =
  'The query limit for age and popularity data has been reached. Contact Sonatype support to extend current quota limits.';

export const INTELLIGENCE_STATE_TOOLTIPS: Partial<Record<ComponentIntelligenceStatus, string>> = {
  disabled:
    'The age and popularity data is only available once Repository Health Check (RHC) has been enabled.',
  error: 'Error retrieving component data',
  // Also used on its own for the alert-specific quota (`cappedAlerts`), which exhausts the alert
  // fields while the rest of the same response stays valid.
  capped: QUOTA_TOOLTIP,
};

export const NO_DATA_TOOLTIP = 'No data available for this component';

/**
 * Severity badges for the security alert counts, in Classic's order. Colors follow the Classic
 * chiclet palette (`nx-clm-chiclet-small.critical|severe|moderate`): red, orange, amber.
 */
const SECURITY_SEVERITIES: ReadonlyArray<{
  key: keyof ComponentIntelligenceData;
  label: string;
  color: 'red' | 'orange' | 'amber';
}> = [
  { key: 'criticalSecurityAlerts', label: 'critical', color: 'red' },
  { key: 'severeSecurityAlerts', label: 'severe', color: 'orange' },
  { key: 'moderateSecurityAlerts', label: 'moderate', color: 'amber' },
];

/** License threat severity to badge color, following the same Classic chiclet palette. */
const LICENSE_THREAT_COLORS: Record<LicenseThreatSeverity, 'red' | 'orange' | 'amber' | 'blue'> = {
  critical: 'red',
  severe: 'orange',
  moderate: 'amber',
  none: 'blue',
};

/** A row of the Component Details table. `node` takes precedence over `value` when present. */
export interface IntelligenceDetailRow {
  label: string;
  value: string;
  mono?: boolean;
  node?: React.ReactNode;
  /** Explains why the value is a placeholder; omitted when there is a real value. */
  tooltip?: string;
}

/**
 * Renders one Component Details value: a custom node when the row supplies one, otherwise text.
 * A row carrying a tooltip is wrapped so the reason behind a placeholder is reachable.
 */
export function renderIntelligenceValue(row: IntelligenceDetailRow) {
  const value = row.node ?? (
    <Text size="2" weight="medium" style={row.mono ? { fontFamily: 'monospace', fontSize: '11px' } : undefined}>
      {row.value}
    </Text>
  );
  if (!row.tooltip) {
    return value;
  }
  // Radix Tooltip needs a focusable, hoverable child that forwards a ref. Text renders a plain
  // span, which is neither, and a tabIndex on a non-interactive element is exactly the confusing
  // focus stop Biome's a11y rule warns about — so use a real button, unstyled so it still reads
  // as a value rather than a control.
  return (
    <Tooltip content={row.tooltip}>
      <button
        type="button"
        aria-label={`${row.label}: ${row.tooltip}`}
        style={{
          background: 'none',
          border: 0,
          padding: 0,
          font: 'inherit',
          color: 'inherit',
          cursor: 'help',
        }}
      >
        {value}
      </button>
    </Tooltip>
  );
}

/**
 * The three Repository Health Check signals available on every edition, in Classic's order.
 * Rendered whenever a request was made, so that a component with no upstream record still shows
 * why — matching Classic, which renders all three rows and marks each unavailable.
 */
const BASE_INTELLIGENCE_LABELS = ['Most popular version', 'Age', 'Popularity'] as const;

/**
 * Component Details rows for the Repository Health Check signals.
 *
 * Returns no rows at all for the statuses in which Classic shows no section: nothing requested
 * yet, a format the backend cannot answer for, and a user without `nexus:healthcheck:read`.
 */
export function buildIntelligenceRows(
  status: ComponentIntelligenceStatus,
  data: ComponentIntelligenceData | null,
  alertsStatus: ComponentAlertsStatus,
): IntelligenceDetailRow[] {
  if (status === 'idle' || status === 'unsupported-format' || status === 'forbidden') {
    return [];
  }

  if (status === 'loading') {
    return BASE_INTELLIGENCE_LABELS.map((label) => ({ label, value: 'Loading...' }));
  }

  if (status !== 'resolved' || !data) {
    // disabled / error / capped: the signals exist but carry no value, and the tooltip is the
    // only thing distinguishing the three — so the placeholder alone is never the whole message.
    const tooltip = INTELLIGENCE_STATE_TOOLTIPS[status] ?? NO_DATA_TOOLTIP;
    return BASE_INTELLIGENCE_LABELS.map((label) => ({ label, value: UNAVAILABLE, tooltip }));
  }

  const age = formatComponentAge(data.ageMillis);
  const popularity = formatComponentPopularity(data.popularity);

  const rows: IntelligenceDetailRow[] = [
    { label: 'Most popular version', value: data.mostPopularVersion ?? UNAVAILABLE },
    { label: 'Age', value: age ?? UNAVAILABLE },
    { label: 'Popularity', value: popularity ?? UNAVAILABLE },
  ];
  for (const row of rows) {
    if (row.value === UNAVAILABLE) {
      row.tooltip = NO_DATA_TOOLTIP;
    }
  }

  // The alert fields carry their own authorization and entitlement conditions, so their presence
  // in the response is not permission to show them: a caller who is not entitled or not permitted
  // gets no Security or License row at all. `unknown` is included deliberately — entitlement that
  // has not resolved yet must fail closed, or an unlicensed user would see the values in the window
  // before the state store publishes a license (and forever, if the listener never attaches).
  if (alertsStatus === 'unknown' || alertsStatus === 'unlicensed' || alertsStatus === 'forbidden') {
    return rows;
  }

  // The alert quota can be exhausted while age and popularity in the same response remain valid,
  // in which case the alert fields are absent for a reason worth stating — Classic shows the same
  // quota tooltip on both. Without this they would be indistinguishable from an unentitled edition.
  if (alertsStatus === 'capped') {
    for (const label of ['Security', 'License']) {
      rows.push({ label, value: UNAVAILABLE, tooltip: QUOTA_TOOLTIP });
    }
    return rows;
  }

  // Security and license are only present on an edition and license feature that request alerts
  // (see useComponentIntelligence). Absent means "not evaluated", not "zero findings", so the
  // rows are omitted rather than rendered as all-clear.
  if (data.securityAlerts !== null) {
    rows.push({
      label: 'Security',
      value: String(data.securityAlerts),
      node: (
        <Flex gap="1" align="center">
          {SECURITY_SEVERITIES.map(({ key, label, color }) => {
            const count = (data[key] as number | null) ?? 0;
            return (
              // The count is always shown, including 0, so severity is never conveyed by color
              // alone. Classic leaves a zero chiclet blank; the number carries the same meaning
              // and reads correctly to a screen reader and without color perception.
              <Badge
                key={label}
                color={count > 0 ? color : 'gray'}
                variant={count > 0 ? 'solid' : 'soft'}
                aria-label={`${count} ${label}`}
              >
                {count}
              </Badge>
            );
          })}
        </Flex>
      ),
    });
  }

  if (data.licenseThreat !== null) {
    const licenseName = data.licenseThreatName;
    rows.push({
      label: 'License',
      value: licenseName ?? UNAVAILABLE,
      node: licenseName ? (
        <Badge color={LICENSE_THREAT_COLORS[licenseThreatSeverity(data.licenseThreat)]} variant="solid">
          {licenseName}
        </Badge>
      ) : undefined,
      tooltip: licenseName ? undefined : NO_DATA_TOOLTIP,
    });
  }

  return rows;
}
