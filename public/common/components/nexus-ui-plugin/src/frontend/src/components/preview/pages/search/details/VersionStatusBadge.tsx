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
import { Badge, Tooltip } from '@radix-ui/themes';

import type { VersionStatus } from '../core';

interface VersionStatusBadgeProps {
  status: VersionStatus;
  reason?: string;
}

/**
 * VersionStatusBadge - Colored badge indicating version status.
 * Per badge skill: text-only (no icons in descriptor/signal badges except Compliant/X, DTS).
 *
 * Colors:
 * - recommended → green
 * - quarantined → yellow
 * - malware → red
 * - not-recommended → gray
 * - none → no badge
 */
export function VersionStatusBadge({ status, reason }: VersionStatusBadgeProps) {
  if (status === 'none') {
    return null;
  }

  const config = getStatusConfig(status);
  const badge = (
    <Badge color={config.color} variant={config.variant} size="1">
      {config.label}
    </Badge>
  );

  if (reason) {
    return (
      <Tooltip content={reason}>
        {badge}
      </Tooltip>
    );
  }

  return badge;
}

function getStatusConfig(status: VersionStatus): {
  color: 'green' | 'yellow' | 'red' | 'gray';
  variant: 'solid' | 'soft' | 'outline';
  label: string;
} {
  switch (status) {
    case 'recommended':
      return { color: 'green', variant: 'soft', label: 'Recommended' };
    case 'quarantined':
      return { color: 'yellow', variant: 'soft', label: 'Quarantined' };
    case 'malware':
      return { color: 'red', variant: 'solid', label: 'Malware' };
    case 'not-recommended':
      return { color: 'gray', variant: 'soft', label: 'Not Recommended' };
    default:
      return { color: 'gray', variant: 'outline', label: status };
  }
}

export default VersionStatusBadge;

