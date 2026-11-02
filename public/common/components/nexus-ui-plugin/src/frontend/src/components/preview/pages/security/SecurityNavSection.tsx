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

import React, { useId } from 'react';
import { Box, Flex, Separator, Text } from '@radix-ui/themes';
import { ClipboardCheck, type LucideIcon, Scale, Shield, Siren } from 'lucide-react';
import { NavItemBox } from '../../shared/Navigation/NavItemBox';
import type { FirewallSecurityTabId } from '../../shared/firewallSecurityPermissions';
import { useFirewallSecurityPagesTier } from '../../shared/useFirewallSecurityPagesTier';
import { useVisibleFirewallSecurityTabs } from './useVisibleFirewallSecurityTabs';

interface SecurityNavLink {
  id: FirewallSecurityTabId;
  label: string;
  icon: LucideIcon;
}

// Kept separate from FIREWALL_SECURITY_TABS so the nav does not pull the page views into the main bundle.
const SECURITY_NAV_LINKS: SecurityNavLink[] = [
  { id: 'quarantine', label: 'Quarantine', icon: Shield },
  { id: 'waivers', label: 'Waivers', icon: ClipboardCheck },
  { id: 'malware', label: 'Malware', icon: Siren },
  // The mockup reuses the Waivers icon for Evaluations.
  { id: 'evaluations', label: 'Evaluations', icon: ClipboardCheck },
  { id: 'policies', label: 'Policy', icon: Scale },
];

const quarantineBadgeLabel = (count: number) => `${count} quarantined components`;

export interface SecurityNavSectionProps {
  isCollapsed?: boolean;
  /** Quarantined-component count, shown as a badge on the Quarantine link only. */
  badgeCount?: number | null;
}

// Uppercased in CSS so screen readers announce "Security", not the letters. The heading box takes mb="-1" to
// cancel half of the column gap, so it sits 4px above its first link, as in the design.
const HEADING_STYLE = {
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
} as const;

/**
 * Left-nav Security section: a non-clickable "SECURITY" heading, one link per section the user may see and a
 * trailing separator (so nothing is left behind when the section is hidden). Renders nothing when no link is visible. The caller decides whether the Security pages are visible
 * at all.
 */
export function SecurityNavSection({ isCollapsed, badgeCount }: SecurityNavSectionProps): JSX.Element | null {
  const labelId = useId();
  const tier = useFirewallSecurityPagesTier();
  const visibleTabs = useVisibleFirewallSecurityTabs(tier);

  const visibleIds = new Set(visibleTabs.map((tab) => tab.id));
  const links = SECURITY_NAV_LINKS.filter((link) => visibleIds.has(link.id));

  if (links.length === 0) return null;

  return (
    <>
      <Flex
        direction="column"
        gap="2"
        role="group"
        aria-labelledby={isCollapsed ? undefined : labelId}
        aria-label={isCollapsed ? 'Security' : undefined}
      >
        {!isCollapsed && (
          <Box px="2" mt="3" mb="-1">
            <Text id={labelId} size="1" weight="bold" color="gray" style={HEADING_STYLE}>
              Security
            </Text>
          </Box>
        )}
        {links.map(({ id, label, icon }) => (
          <NavItemBox
            key={id}
            name={`preview.security.${id}`}
            text={label}
            icon={icon}
            isCollapsed={isCollapsed}
            badge={id === 'quarantine' ? badgeCount : undefined}
            badgeLabel={id === 'quarantine' ? quarantineBadgeLabel : undefined}
            analyticsId={`nxrm-security-${id}-nav`}
          />
        ))}
      </Flex>
      <Box data-testid="nav-group-security-separator">
        <Separator size="4" my="2" />
      </Box>
    </>
  );
}

export default SecurityNavSection;
