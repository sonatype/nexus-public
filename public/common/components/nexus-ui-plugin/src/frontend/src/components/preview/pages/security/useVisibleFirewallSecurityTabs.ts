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

import { useMemo } from 'react';
import { ExtJS } from '../../../../interface/ExtJS';
import { useHasUser } from '../../shared/hooks/useHasUser';
import type { FirewallSecurityPagesTier } from '../../shared/useFirewallSecurityPagesTier';
import { FIREWALL_SECURITY_TABS, type FirewallSecurityTab } from './firewallSecurityTabs';

/**
 * Tabs to show for a tier. 'preview-pro' shows every tab to a signed-in user so they can see what they would get: the
 * read privileges only exist on Pro+ and Enterprise, so no Pro user could hold one. A signed-out visitor sees none. Every other visible tier ('preview-no-iq',
 * 'live', 'upgrade-iq', 'error') shows only the tabs whose read permission the user holds, so a user without firewall
 * privileges never sees the section, its example data or its notice; 'hidden' renders no tabs.
 */
export function useVisibleFirewallSecurityTabs(tier: FirewallSecurityPagesTier): FirewallSecurityTab[] {
  const hasUser = useHasUser();
  // Comma-joined ids: a primitive, so usePermission's change check does not loop on a new array each call.
  const permittedIds = ExtJS.usePermission(
    () =>
      FIREWALL_SECURITY_TABS.filter((tab) => ExtJS.checkPermission(tab.permission))
        .map((tab) => tab.id)
        .join(','),
    [hasUser]
  ) as string;

  return useMemo(() => {
    if (tier === 'preview-pro') return hasUser ? FIREWALL_SECURITY_TABS : [];
    if (tier === 'preview-no-iq' || tier === 'live' || tier === 'upgrade-iq' || tier === 'error') {
      const permitted = new Set((permittedIds ?? '').split(',').filter(Boolean));
      return FIREWALL_SECURITY_TABS.filter((tab) => permitted.has(tab.id));
    }
    return [];
  }, [tier, permittedIds, hasUser]);
}
