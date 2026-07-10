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

import {
  FileSearch,
  Globe,
  Users,
  Cloud,
  UserCheck,
  HeartPulse,
  Clock,
  Link,
  Server,
  Megaphone,
  Scissors,
  KeyRound,
  Calendar,
  HardDrive,
  Palette,
  Settings,
  ArrowUpCircle,
  Webhook,
  GitBranch,
  Puzzle,
} from 'lucide-react';

// `firewall.audit` was removed from this map: post-migration the new UI never renders
// capabilities of that type (filtered at useCapabilitiesApi.fetchCapabilities() and not
// advertised by /v1/capabilities/types). Firewall configuration belongs on the repository
// config page now. The legacy REST API surface remains for hardcoded client compatibility.
// The previously-imported `Shield` icon has been pruned along with the lookup entry.

export const TYPE_ICONS: Record<string, React.ComponentType<{ size?: number; className?: string }>> = {
  audit: FileSearch,
  baseurl: Globe,
  crowd: Users,
  customs3regions: Cloud,
  defaultrole: UserCheck,
  healthcheck: HeartPulse,
  'license-expiration': Clock,
  LegacyUrlCapability: Link,
  'node.identity': Server,
  outreach: Megaphone,
  OutreachManagementCapability: Megaphone,
  'browse.trim': Scissors,
  rutauth: KeyRound,
  'scheduling.scheduler': Calendar,
  StorageSettings: HardDrive,
  'rapture.branding': Palette,
  'rapture.settings': Settings,
  migration: ArrowUpCircle,
  'webhook.global': Webhook,
  'webhook.repository': GitBranch,
};

export const DEFAULT_TYPE_ICON = Puzzle;
