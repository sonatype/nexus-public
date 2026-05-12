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

import type { AuditEvent, AuditEventDisplay, AuditCategory } from './audit.types';
import { DOMAIN_CATEGORY_MAP } from './audit.constants';

/**
 * Enhance audit event with computed display fields.
 */
export function formatAuditEvent(event: AuditEvent): AuditEventDisplay {
  const category = DOMAIN_CATEGORY_MAP[event.domain] || 'configuration';
  const entityType = deriveEntityType(event.domain);
  const entityName = event.context || event.attributes?.name || 'Unknown';

  return {
    ...event,
    category,
    eventLabel: formatEventLabel(event.type),
    summary: buildSummary(event, entityType, entityName),
    entityType,
    entityName,
  };
}

/**
 * Format event type for display ("created" → "Created").
 */
function formatEventLabel(type: string): string {
  return type
    .split(/[-_]/)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Build human-readable summary string.
 */
function buildSummary(event: AuditEvent, entityType: string, entityName: string): string {
  const action = formatAction(event.type);
  const initiator = event.initiator || 'system';

  // Handle special cases
  if (event.domain.includes('login') || event.domain.includes('logout')) {
    return `User '${entityName}' ${event.type}`;
  }

  if (event.type === 'automatic-malware-removed') {
    const parts = entityName.split(':');
    if (parts.length >= 2) {
      const repo = parts[0];
      const asset = parts.slice(1).join(':');
      return `Component '${asset}' in repository '${repo}' removed (malware) by system`;
    }
    return `Component '${entityName}' removed (malware) by system`;
  }

  if (event.type === 'quarantined-new-violation' || event.type === 'blocked-download-already-quarantined') {
    return `Download blocked: ${entityName}${initiator !== 'system' ? ` (user: ${initiator})` : ''}`;
  }

  // Default format
  return `${entityType} '${entityName}' ${action}${initiator !== 'system' ? ` by ${initiator}` : ''}`;
}

/**
 * Format event type as action verb.
 */
function formatAction(type: string): string {
  const actionMap: Record<string, string> = {
    created: 'created',
    updated: 'updated',
    deleted: 'deleted',
    started: 'started',
    stopped: 'stopped',
    finished: 'finished',
    failed: 'failed',
    login: 'logged in',
    logout: 'logged out',
    scheduled: 'scheduled',
    canceled: 'canceled',
    activated: 'activated',
    passivated: 'deactivated',
    purged: 'purged',
    downloaded: 'downloaded',
    uploaded: 'uploaded',
    associated: 'tagged',
    disassociated: 'untagged',
  };

  return actionMap[type] || type.replace(/-/g, ' ');
}

/**
 * Derive entity type from domain.
 */
function deriveEntityType(domain: string): string {
  const typeMap: Record<string, string> = {
    'security.user': 'User',
    'security.role': 'Role',
    'security.privilege': 'Privilege',
    'security.user-role-mapping': 'User-Role Mapping',
    'security.anonymous': 'Anonymous Access',
    'security.realm': 'Realm Configuration',
    'security.ldap': 'LDAP Configuration',
    'security.crowd': 'Crowd Configuration',
    'security.sslcertificate': 'SSL Certificate',
    'security.secrets': 'Encryption Key',
    'security.jwt': 'JWT Token',
    'SamlRealm': 'SAML Configuration',
    'repository': 'Repository',
    'repository.component': 'Component',
    'repository.asset': 'Asset',
    'repository.component.tag': 'Component Tag',
    'blobstore': 'Blob Store',
    'tasks': 'Task',
    'capability': 'Capability',
    'cleanupPolicy': 'Cleanup Policy',
    'ContentSelector': 'Content Selector',
    'RoutingRule': 'Routing Rule',
    'email': 'Email Configuration',
    'httpclient': 'HTTP Client Configuration',
    'logging': 'Logging Configuration',
    'script': 'Script',
    'license': 'License',
    'freeze': 'Database Freeze',
    'DataStore': 'Data Store',
    'database-migration': 'Database Migration',
    'userToken': 'User Token',
    'userToken.admin': 'User Token (Admin)',
    'protection.config': 'Protection Configuration',
    'malware.removal': 'Malware Removal',
    'firewall.quarantine': 'Firewall Quarantine',
  };

  return typeMap[domain] || 'Event';
}

/**
 * Format timestamp for display.
 */
export function formatTimestamp(timestamp: string): string {
  try {
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    return timestamp;
  }
}

/**
 * Format timestamp for full display (with seconds).
 */
export function formatTimestampFull(timestamp: string): string {
  try {
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      second: '2-digit',
      hour12: true,
    });
  } catch {
    return timestamp;
  }
}
