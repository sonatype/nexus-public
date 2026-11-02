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
import { Badge, Flex, Text } from '@radix-ui/themes';

import './ThreatLevelBadge.scss';

export type ThreatLevelBucket = 'critical' | 'high' | 'medium' | 'low' | 'none';

/**
 * The one bucketing scheme used everywhere this value is shown - the badge and any filter built
 * on it - so a component can never be "High" in the table and "Severe" in the sidebar. IQ's
 * policy-severity scale is 0-10; it is not CVSS, despite widespread convention elsewhere of
 * labelling it that way.
 */
export function getThreatLevelBucket(threatLevel: number): ThreatLevelBucket {
  // Guard against NaN, Infinity, and other non-finite values
  if (!Number.isFinite(threatLevel)) return 'none';

  if (threatLevel >= 9) return 'critical';
  if (threatLevel >= 7) return 'high';
  if (threatLevel >= 4) return 'medium';
  if (threatLevel > 0) return 'low';
  return 'none';
}

export const THREAT_LEVEL_BUCKET_LABELS: Record<ThreatLevelBucket, string> = {
  critical: 'Critical',
  high: 'High',
  medium: 'Medium',
  low: 'Low',
  none: 'None',
};

const THREAT_LEVEL_BUCKET_COLORS: Record<ThreatLevelBucket, 'red' | 'orange' | 'yellow' | 'gray'> = {
  critical: 'red',
  high: 'orange',
  medium: 'yellow',
  low: 'gray',
  none: 'gray',
};

export interface ThreatLevelBadgeProps {
  /** IQ's 0-10 policy-severity scale - not CVSS */
  threatLevel: number;
  /** Size variant */
  size?: 'small' | 'medium';
  /** Custom class name */
  className?: string;
}

/**
 * ThreatLevelBadge displays IQ's policy-severity score (0-10) as a colored pill with its bucket
 * label alongside.
 *
 * @example
 * ```tsx
 * <ThreatLevelBadge threatLevel={10} />
 * ```
 */
export function ThreatLevelBadge({
  threatLevel,
  size = 'medium',
  className = '',
}: ThreatLevelBadgeProps): JSX.Element {
  const bucket = getThreatLevelBucket(threatLevel);
  const label = THREAT_LEVEL_BUCKET_LABELS[bucket];
  const color = THREAT_LEVEL_BUCKET_COLORS[bucket];
  const scoreText = Number.isInteger(threatLevel) ? String(threatLevel) : threatLevel.toFixed(1);

  // Combine class names and trim to avoid trailing space when className is empty
  const combinedClassName = `threat-level-badge threat-level-badge--${size} ${className}`.trim();

  return (
    <Flex
      align="center"
      gap="2"
      display="inline-flex"
      className={combinedClassName}
      aria-label={`Threat level ${scoreText} — ${label}`}
    >
      <Badge
        size="1"
        variant="solid"
        radius="full"
        color={color}
        className={`threat-level-badge__pill threat-level-badge__pill--${bucket}`}
        aria-hidden="true"
      >
        {scoreText}
      </Badge>
      <Text size={size === 'small' ? '1' : '2'} aria-hidden="true">
        {label}
      </Text>
    </Flex>
  );
}

export default ThreatLevelBadge;
