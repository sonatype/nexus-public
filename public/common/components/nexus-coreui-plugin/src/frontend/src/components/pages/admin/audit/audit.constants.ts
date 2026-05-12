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

import type { AuditCategory } from './audit.types';

/**
 * Maps audit event domains to UI categories for filtering and display.
 */
export const DOMAIN_CATEGORY_MAP: Record<string, AuditCategory> = {
  // Security domains
  'security.user': 'security',
  'security.role': 'security',
  'security.privilege': 'security',
  'security.user-role-mapping': 'security',
  'security.anonymous': 'security',
  'security.realm': 'security',
  'security.ldap': 'security',
  'security.crowd': 'security',
  'security.sslcertificate': 'security',
  'security.secrets': 'security',
  'security.jwt': 'security',
  'SamlRealm': 'security',

  // Repository domains
  'repository': 'repository',
  'repository.component': 'repository',
  'repository.asset': 'repository',
  'repository.component.tag': 'repository',
  'blobstore': 'repository',

  // Configuration domains
  'tasks': 'configuration',
  'capability': 'configuration',
  'cleanupPolicy': 'configuration',
  'ContentSelector': 'configuration',
  'RoutingRule': 'configuration',
  'email': 'configuration',
  'httpclient': 'configuration',
  'logging': 'configuration',
  'script': 'configuration',
  'license': 'configuration',
  'freeze': 'configuration',
  'DataStore': 'configuration',
  'database-migration': 'configuration',
  'userToken': 'configuration',
  'userToken.admin': 'configuration',

  // Protection domains
  'protection.config': 'protection',
  'malware.removal': 'protection',
  'firewall.quarantine': 'protection',
};

/**
 * Category colors for badges and icons.
 */
export const CATEGORY_COLORS: Record<AuditCategory, string> = {
  security: 'blue',
  repository: 'purple',
  configuration: 'gray',
  protection: 'amber',
};

/**
 * User-friendly labels for categories.
 */
export const CATEGORY_LABELS: Record<AuditCategory, string> = {
  security: 'Security',
  repository: 'Repository',
  configuration: 'Configuration',
  protection: 'Protection',
};

/**
 * Common event types for filtering.
 */
export const COMMON_EVENT_TYPES = [
  'created',
  'updated',
  'deleted',
  'started',
  'finished',
  'failed',
  'login',
  'logout',
] as const;

/**
 * Common domain filters grouped by category.
 */
export const COMMON_DOMAINS = [
  // Security
  { value: 'security.user', label: 'Users', category: 'security' as AuditCategory },
  { value: 'security.role', label: 'Roles', category: 'security' as AuditCategory },
  { value: 'security.privilege', label: 'Privileges', category: 'security' as AuditCategory },
  { value: 'security.ldap', label: 'LDAP', category: 'security' as AuditCategory },
  { value: 'SamlRealm', label: 'SAML', category: 'security' as AuditCategory },
  // Repository
  { value: 'repository', label: 'Repositories', category: 'repository' as AuditCategory },
  { value: 'blobstore', label: 'Blob Stores', category: 'repository' as AuditCategory },
  { value: 'repository.component', label: 'Components', category: 'repository' as AuditCategory },
  // Configuration
  { value: 'tasks', label: 'Tasks', category: 'configuration' as AuditCategory },
  { value: 'capability', label: 'Capabilities', category: 'configuration' as AuditCategory },
  { value: 'cleanupPolicy', label: 'Cleanup Policies', category: 'configuration' as AuditCategory },
  // Protection
  { value: 'protection.config', label: 'Protection Config', category: 'protection' as AuditCategory },
  { value: 'malware.removal', label: 'Malware Removal', category: 'protection' as AuditCategory },
  { value: 'firewall.quarantine', label: 'Firewall Quarantine', category: 'protection' as AuditCategory },
] as const;
