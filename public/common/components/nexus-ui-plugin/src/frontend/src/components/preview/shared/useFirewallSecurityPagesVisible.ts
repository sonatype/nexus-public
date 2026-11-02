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

import { ExtJS } from '../../../interface/ExtJS';
import { FIREWALL_SECURITY_PERMISSION_LIST } from './firewallSecurityPermissions';
import { useHasUser } from './hooks/useHasUser';
import { useFirewallSecurityPagesTier } from './useFirewallSecurityPagesTier';

/**
 * Whether the Security nav item and page are available to the current user.
 *
 * - tier 'hidden': never
 * - tiers 'preview-no-iq', 'live', 'upgrade-iq' and 'error': only when the user holds at least one of the five tab
 *   read permissions
 * - tier 'preview-pro': yes for any signed-in user (the read privileges exist only on Pro+ and Enterprise, so a Pro
 *   user cannot hold one), never for a signed-out visitor
 *
 * Gates display only; the backend still enforces every privilege.
 */
export function useFirewallSecurityPagesVisible(): boolean {
  const tier = useFirewallSecurityPagesTier();
  const hasUser = useHasUser();
  const hasAnyTabPermission = ExtJS.usePermission(
    () => FIREWALL_SECURITY_PERMISSION_LIST.some((permission) => ExtJS.checkPermission(permission)),
    [hasUser]
  );

  if (tier === 'hidden') return false;
  if (tier === 'preview-pro') return hasUser;
  return Boolean(hasAnyTabPermission);
}
