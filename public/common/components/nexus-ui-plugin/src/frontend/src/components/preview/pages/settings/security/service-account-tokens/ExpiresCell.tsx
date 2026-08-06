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
import { Text, Badge } from '@radix-ui/themes';
import * as Tooltip from '@radix-ui/react-tooltip';
import { AlertTriangle } from 'lucide-react';

import { SERVICE_ACCOUNT_TOKENS_STRINGS } from './strings';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.EXPIRES_CELL;

interface ExpiresCellProps {
  expiresAt: string | null;
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export function ExpiresCell({ expiresAt }: ExpiresCellProps) {
  if (!expiresAt) {
    return (
      <Text size="2" color="gray">
        {LABELS.NEVER}
      </Text>
    );
  }

  const expiresDate = new Date(expiresAt);
  const now = new Date();
  const isExpired = expiresDate <= now;

  if (isExpired) {
    return (
      <Tooltip.Provider delayDuration={200}>
        <Tooltip.Root>
          <Tooltip.Trigger asChild>
            <Badge color="orange" size="1" className="sat-expired-badge">
              <AlertTriangle size={12} aria-hidden="true" className="sat-expired-badge__icon" />
              {LABELS.EXPIRED}
            </Badge>
          </Tooltip.Trigger>
          <Tooltip.Portal>
            <Tooltip.Content className="sat-expired-badge__tooltip" sideOffset={6}>
              {LABELS.EXPIRED_ON(formatDate(expiresAt))}
              <Tooltip.Arrow className="sat-expired-badge__tooltip-arrow" />
            </Tooltip.Content>
          </Tooltip.Portal>
        </Tooltip.Root>
      </Tooltip.Provider>
    );
  }

  return <Text size="2">{formatDate(expiresAt)}</Text>;
}
