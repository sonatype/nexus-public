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

export type FirewallSecurityTabId = 'malware' | 'waivers' | 'quarantine' | 'policies' | 'evaluations';

/**
 * Read permission for each Firewall Security tab. The domains match the backend security contributors in
 * nexus-firewall-plugin. Kept in its own module so both the tab registry and useFirewallSecurityPagesVisible
 * can import it without a circular dependency between shared/ and pages/.
 */
export const FIREWALL_SECURITY_PERMISSIONS: Record<FirewallSecurityTabId, string> = {
  malware: 'nexus:malware-findings:read',
  waivers: 'nexus:firewall-waivers:read',
  quarantine: 'nexus:firewall-quarantine:read',
  policies: 'nexus:firewall-policy:read',
  evaluations: 'nexus:firewall-evaluations:read',
};

export const FIREWALL_SECURITY_PERMISSION_LIST: string[] = Object.values(FIREWALL_SECURITY_PERMISSIONS);
