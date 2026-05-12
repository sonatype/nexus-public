/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */

/**
 * FirewallCard - Firewall license and configuration status on Dashboard.
 *
 * Refactored from MalwareStatusCard to ONLY show:
 * - License status (Enterprise/Pro/None)
 * - Repository protection configuration
 * - Help modal with explanation
 *
 * Malware count is now shown in MalwareAlertCard (separate component).
 *
 * This card shows:
 * - Shield icon (green if configured, amber if partial, gray if none)
 * - "Firewall Enterprise" / "Firewall Pro" / "Repository Firewall"
 * - License status + repo config counts
 * - CTA buttons: "View in Browse" + "Protect"
 * - Help modal (?) explains status calculation
 */

import React, { useState } from 'react';
import { Box, Flex, Text, Card, Button, IconButton, Spinner } from '@radix-ui/themes';
import { ShieldCheck, ShieldOff, ExternalLink, AlertCircle } from 'lucide-react';
import { useMalwareStatus, type MalwareStatus } from './useMalwareStatus';
import { FirewallHelpModal } from './FirewallHelpModal';
import { useIqConnectionStatus } from '../../../../components/shared/security/useIqConnectionStatus';

import './FirewallCard.scss';

const IQ_SERVER_SETTINGS_HREF = '#preview/admin/iq';
const LEARN_MORE_HREF = 'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/sonatype-repository-firewall';

/**
 * Display component for FirewallCard.
 * Exported for testing with mock state.
 */
export function FirewallCardDisplay({ status }: { status: MalwareStatus }): React.ReactElement {
  // Title based on tier
  const title = status.firewallTier === 'enterprise'
    ? 'Firewall Enterprise'
    : 'Repository Firewall';

  // Determine if configured (has IQ Server connection)
  const isConfigured = status.available && status.hasFirewall;

  if (isConfigured) {
    // Configured state: show active status
    const { total, protected: p, inAudit, unprotected } = status.proxyCounts;
    const statusText = total === 0
      ? 'No proxy repositories'
      : unprotected === 0
        ? `All ${total} repos protected - Policy violations blocked`
        : `${p + inAudit} of ${total} repos protected - ${unprotected} unprotected`;

    return (
      <Card className="nxrm-firewall-card nxrm-firewall-card--active" size="1">
        <Flex align="center" justify="between" gap="3">
          <Flex align="center" gap="3">
            <Box className="nxrm-firewall-card__icon">
              <ShieldCheck size={22} color="var(--green-9)" aria-hidden />
            </Box>
            <Box className="nxrm-firewall-card__content">
              <Flex align="center" gap="2" mb="1">
                <Text size="3" weight="medium">{title}</Text>
              </Flex>
              <Text size="2" color="gray" className="nxrm-firewall-card__status">
                {statusText}
              </Text>
            </Box>
          </Flex>
          <Flex gap="2" wrap="wrap" justify="end">
            <Button
              variant="soft"
              size="1"
              onClick={() => {
                window.location.hash = IQ_SERVER_SETTINGS_HREF;
              }}
            >
              Configure
            </Button>
          </Flex>
        </Flex>
      </Card>
    );
  }

  // Not configured: show marketing/upgrade prompt
  return (
    <Card className="nxrm-firewall-card nxrm-firewall-card--upsell" size="1">
      <Flex align="center" justify="between" gap="3">
        <Flex align="center" gap="3">
          <Box className="nxrm-firewall-card__icon">
            <ShieldCheck size={22} color="var(--amber-9)" aria-hidden />
          </Box>
          <Box className="nxrm-firewall-card__content">
            <Flex align="center" gap="2" mb="1">
              <Text size="3" weight="medium">Upgrade to {title}</Text>
            </Flex>
            <Text size="2" color="gray" className="nxrm-firewall-card__status">
              Keep bad code out of your repository. Blocks malicious and vulnerable components before they ever enter your repo.
            </Text>
          </Box>
        </Flex>
        <Flex gap="2" wrap="wrap" justify="end">
          <Button
            variant="soft"
            size="1"
            onClick={() => {
              window.location.hash = IQ_SERVER_SETTINGS_HREF;
            }}
          >
            Configure
          </Button>
          <Button
            variant="surface"
            size="2"
            onClick={() => {
              window.open(LEARN_MORE_HREF, '_blank', 'noopener,noreferrer');
            }}
          >
            <Flex align="center" gap="2">
              Learn more
              <ExternalLink size={14} />
            </Flex>
          </Button>
        </Flex>
      </Flex>
    </Card>
  );
}

/**
 * IQ Server connection status card - shown when IQ is configured but disconnected
 */
function IqDisconnectedCard({ errorMessage }: { errorMessage?: string }): React.ReactElement {
  return (
    <Card className="nxrm-firewall-card nxrm-firewall-card--disconnected" size="1">
      <Flex align="center" justify="between" gap="3">
        <Flex align="center" gap="3">
          <Box className="nxrm-firewall-card__icon">
            <ShieldOff size={22} color="var(--red-9)" aria-hidden />
          </Box>
          <Box className="nxrm-firewall-card__content">
            <Flex align="center" gap="2" mb="1">
              <AlertCircle size={14} color="var(--red-9)" />
              <Text size="3" weight="medium" color="red">IQ Server: Not Connected</Text>
            </Flex>
            <Text size="2" color="gray" className="nxrm-firewall-card__status">
              {errorMessage || 'Unable to reach IQ Server. Check configuration.'}
            </Text>
          </Box>
        </Flex>
        <Button
          variant="soft"
          size="1"
          onClick={() => {
            window.location.hash = IQ_SERVER_SETTINGS_HREF;
          }}
        >
          Configure IQ Server
        </Button>
      </Flex>
    </Card>
  );
}

/**
 * IQ Server connection testing card - shown during connection test
 */
function IqTestingCard(): React.ReactElement {
  return (
    <Card className="nxrm-firewall-card nxrm-firewall-card--testing" size="1">
      <Flex align="center" gap="3">
        <Box className="nxrm-firewall-card__icon">
          <Spinner size="2" />
        </Box>
        <Box className="nxrm-firewall-card__content">
          <Text size="3" weight="medium">IQ Server</Text>
          <Text size="2" color="gray">Testing connection...</Text>
        </Box>
      </Flex>
    </Card>
  );
}

/**
 * FirewallCard - Shows firewall license status and repo configuration.
 * Now includes IQ Server connection awareness (BDD Scenario 8).
 *
 * States:
 * - Testing: Shows spinner while testing connection
 * - Disconnected: Shows error with link to configure
 * - Connected: Shows normal firewall status card
 * - Not configured: Shows upsell/upgrade prompt
 */
export default function FirewallCard(): React.ReactElement {
  // Check IQ Server connection status
  const iqConnection = useIqConnectionStatus();

  let status: MalwareStatus;
  try {
    status = useMalwareStatus();
  } catch (error) {
    console.error('[FirewallCard] Hook crashed:', error);
    // Fallback to upsell state
    status = {
      available: false,
      state: null,
      malwareCount: 0,
      statusText: 'Error loading status',
      color: 'gray',
      ctaText: 'Learn more',
      hasFirewall: false,
      firewallTier: 'none',
      proxyCounts: { total: 0, protected: 0, inAudit: 0, unprotected: 0 },
    };
  }

  // If IQ Server is configured but connection is being tested
  if (iqConnection.isConfigured && iqConnection.isTesting) {
    return <IqTestingCard />;
  }

  // If IQ Server is configured but disconnected
  if (iqConnection.isConfigured && iqConnection.state === 'disconnected') {
    return <IqDisconnectedCard errorMessage={iqConnection.errorMessage} />;
  }

  // Normal display (connected or not configured)
  return <FirewallCardDisplay status={status} />;
}
