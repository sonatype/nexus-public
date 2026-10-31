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
import { Flex, Text, Heading, Badge, Tooltip, Link } from '@radix-ui/themes';
import { AlertCircle, ShieldOff } from 'lucide-react';

const FIREWALL_DOCS_URL = 'https://help.sonatype.com/en/repository-firewall.html';

export interface FirewallNotSupportedEmptyStateProps {
  /** The repository format (e.g. 'terraform', 'helm') */
  format: string;
  /**
   * Rendering context controls sizing and layout:
   * - 'cell'  → inline AlertCircle + "Not supported" text (repo list / quick config)
   * - 'tab'   → centered full empty state with ShieldOff icon (repo profile tab)
   */
  context?: 'cell' | 'tab';
}

/**
 * FirewallNotSupportedEmptyState — unified N/A treatment for unsupported Firewall formats.
 *
 * Used in:
 * - FirewallCell (context="cell")
 * - MalwareQuickActionsTab badge (context="cell")
 * - FirewallReportTab (context="tab")
 */
export function FirewallNotSupportedEmptyState({
  format,
  context = 'cell',
}: FirewallNotSupportedEmptyStateProps): JSX.Element {
  const tooltipText = 'Format not Supported';

  if (context === 'tab') {
    return (
      <Flex
        direction="column"
        align="center"
        justify="center"
        gap="3"
        style={{ padding: 'var(--space-8)', minHeight: 200 }}
        data-testid="firewall-not-supported-empty-state"
      >
        <ShieldOff size={48} color="var(--gray-8)" aria-hidden="true" />
        <Heading size="4" color="gray">
          Firewall Not Available
        </Heading>
        <Text size="2" color="gray" align="center" style={{ maxWidth: 400 }}>
          Repository Firewall does not support the {format} format. No malware scanning or
          component quarantine is available for this repository.
        </Text>
        <Link
          href={FIREWALL_DOCS_URL}
          target="_blank"
          rel="noopener noreferrer"
          size="2"
        >
          Learn about supported formats
        </Link>
      </Flex>
    );
  }

  return (
    <Tooltip content={tooltipText}>
      <Flex
        align="center"
        justify="center"
        gap="1"
        data-testid="firewall-not-supported-badge"
        aria-label={`Firewall: Not supported for ${format}`}
      >
        <AlertCircle size={14} color="var(--gray-8)" aria-hidden="true" />
        <Text size="1" color="gray">
          Not supported
        </Text>
      </Flex>
    </Tooltip>
  );
}

/**
 * A standalone gray Badge variant for use in table rows (Quick Config).
 * Separate from the inline cell variant to avoid nesting interactive elements.
 */
export function FirewallNotSupportedBadge({ format }: { format: string }): JSX.Element {
  const tooltipText = 'Format not Supported';

  return (
    <Tooltip content={tooltipText}>
      <Badge
        variant="soft"
        color="gray"
        size="1"
        data-testid="firewall-not-supported-badge"
        aria-label={`Firewall: Not supported for ${format}`}
      >
        Not supported
      </Badge>
    </Tooltip>
  );
}

export default FirewallNotSupportedEmptyState;
