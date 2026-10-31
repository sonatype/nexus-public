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

import React from 'react';
import { Box, Flex, Text, Card, Button, Spinner } from '@radix-ui/themes';
import { ShieldCheck, ShieldOff, ExternalLink, AlertCircle } from 'lucide-react';
import { useMalwareStatus, type MalwareStatus } from './useMalwareStatus';
import { useIqConnectionStatus } from '../../shared/security/useIqConnectionStatus';
import { FIREWALL_AVAILABLE_STATE_KEY } from '../../shared/security/firewallTier';
import { IQ_SERVER_SETTINGS_HREF, FIREWALL_INFO_HREF } from '../../constants/firewallLinks';
import { ExtJS } from '../../../../interface/ExtJS';

import './FirewallCard.scss';

/**
 * Display component for FirewallCard.
 * Exported for testing with mock state.
 */
export function FirewallCardDisplay(
  { status, includedInLicense = false }: { status: MalwareStatus; includedInLicense?: boolean }
): React.ReactElement {
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

  // Not configured. Two audiences:
  // - Pro+/Enterprise: Firewall is already included in the licence (always-on capability), so there is
  //   nothing to buy — the message is "included in your license" and the primary CTA configures IQ Server.
  // - Everyone else (plain Pro, unlicensed): keep the marketing upgrade prompt.
  const headline = includedInLicense ? `${title} included in your license` : `Upgrade to ${title}`;
  const subtext = includedInLicense
    ? 'Repository Firewall is part of your edition. Connect IQ Server to start blocking malicious components before they enter your repositories.'
    : 'Keep bad code out of your repository. Blocks malicious and vulnerable components before they ever enter your repo.';
  const configureLabel = includedInLicense ? 'Configure Firewall' : 'Configure';

  return (
    <Card className="nxrm-firewall-card nxrm-firewall-card--upsell" size="1">
      <Flex align="center" justify="between" gap="3">
        <Flex align="center" gap="3">
          <Box className="nxrm-firewall-card__icon">
            <ShieldCheck size={22} color="var(--amber-9)" aria-hidden />
          </Box>
          <Box className="nxrm-firewall-card__content">
            <Flex align="center" gap="2" mb="1">
              <Text size="3" weight="medium">{headline}</Text>
            </Flex>
            <Text size="2" color="gray" className="nxrm-firewall-card__status">
              {subtext}
            </Text>
          </Box>
        </Flex>
        <Flex gap="2" wrap="wrap" justify="end">
          {/*
            When Firewall is included in the licence, configuring IQ Server is the real next step, so
            "Configure Firewall" is the prominent primary CTA and "Learn more" is secondary. In the
            upgrade case the marketing "Learn more" leads instead. Prominence is expressed through
            variant only; both footer buttons stay size="2" on purpose so they render at the same
            height side by side — size is intentionally not conditional.
          */}
          <Button
            variant={includedInLicense ? 'solid' : 'soft'}
            size="2"
            onClick={() => {
              window.location.hash = IQ_SERVER_SETTINGS_HREF;
            }}
          >
            {configureLabel}
          </Button>
          <Button
            variant="surface"
            size="2"
            onClick={() => {
              window.open(FIREWALL_INFO_HREF, '_blank', 'noopener,noreferrer');
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

  // When Firewall is available under the current licence, the not-configured card should say so
  // instead of pitching an upgrade. Reactive read of the backend capability flag so the card updates
  // live as state changes.
  const includedInLicense = Boolean(
    ExtJS.useState(() => ExtJS.state()?.getValue?.(FIREWALL_AVAILABLE_STATE_KEY))
  );

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
  return <FirewallCardDisplay status={status} includedInLicense={includedInLicense} />;
}
