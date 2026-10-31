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
 * Shared Firewall / IQ Server links for the preview MalwareRisk and Welcome surfaces.
 *
 * These were copy-pasted across FirewallCard, ProtectOverview and the two NoFirewall scenario
 * components, each with a comment asking readers to keep them in sync. Export them once here so a
 * route rename or documentation-URL change lands in a single place.
 */

/** Preview IQ Server overview screen — the "Connect / Configure IQ Server" entry point. */
export const IQ_SERVER_SETTINGS_HREF = '#preview/admin/iq-overview';

/** Repository Firewall marketing / documentation page. */
export const FIREWALL_INFO_HREF =
  'https://links.sonatype.com/nexus-repository-firewall/malicious-risk/sonatype-repository-firewall';
