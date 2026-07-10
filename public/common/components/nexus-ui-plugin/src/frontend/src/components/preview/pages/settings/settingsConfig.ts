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

/**
 * Settings Hub Configuration
 *
 * Defines all settings cards and their organization into sections.
 * This file is the single source of truth for the Settings Hub.
 *
 * Section order intentionally mirrors the classic ExtJS admin navigation:
 * Repository → Security → Support → System
 */

export interface VisibilityRequirements {
  permissions?: string[];
  requiresPermission?: string;
  requiresAnyPermission?: string[];
  permissionPrefix?: string;
  permissionPrefixes?: string[];
  editions?: string[];
  requiresUser?: boolean;
}

export interface SettingCard {
  /** Unique identifier */
  id: string;
  /** URL path (appended to #preview/admin/) when fullHash is not set */
  path: string;
  /**
   * Full hash URL for routes outside #preview/admin/... (e.g. API lives under #preview/browse/api).
   */
  fullHash?: string;
  /** Display label */
  label: string;
  /** Description text */
  description: string;
  /** Optional additional search keywords for discoverability */
  searchTerms?: string[];
  /** If true, this card is hidden on Cloud deployments */
  cloudExcluded?: boolean;
  /** If true, this card is hidden on CE/OSS self-hosted (requires PRO or Cloud edition) */
  proOnly?: boolean;
  /** If true, this card is hidden from non-admin users */
  adminOnly?: boolean;
  /**
   * Feature flag key from previewFeatureFlags.ts.
   * When set, the card shows a "Coming Soon" badge if isFeatureEnabled(featureKey) is false.
   * Cards without a featureKey are always treated as available.
   */
  featureKey?: string;
  /**
   * Permission requirements for this card, matching the corresponding route's
   * visibilityRequirements. Cards without this field are always visible.
   * Uses the same structure as route data.visibilityRequirements so isVisible() can evaluate it.
   */
  visibilityRequirements?: VisibilityRequirements;
}

export interface SettingsSection {
  /** Section identifier */
  id: string;
  /** Section heading label */
  label: string;
  /** Cards in this section */
  cards: SettingCard[];
  /** If true, the entire section is hidden on Cloud deployments */
  cloudExcluded?: boolean;
}

/**
 * All settings organized by section.
 */
export const SETTINGS_SECTIONS: SettingsSection[] = [
  {
    id: 'repository',
    label: 'Repository',
    cards: [
      {
        id: 'repositories',
        path: 'repository/repositories',
        label: 'Repositories',
        description: 'Manage Maven, npm, Docker, and other repositories',
        searchTerms: ['maven', 'npm', 'docker', 'pypi', 'nuget'],
        featureKey: 'repository.repositories',
        visibilityRequirements: { requiresPermission: 'nexus:repository-admin:*:*:read' },
      },
      {
        id: 'blob-stores',
        path: 'repository/blobstores',
        label: 'Blob Stores',
        description: 'Configure local and cloud blob storage',
        searchTerms: ['storage', 's3', 'azure', 'google cloud'],
        cloudExcluded: true,
        featureKey: 'repository.blobstores',
        visibilityRequirements: { requiresPermission: 'nexus:blobstores:read' },
      },
      {
        id: 'cleanup-policies',
        path: 'repository/cleanup-policies',
        label: 'Cleanup Policies',
        description: 'Configure automated component cleanup',
        searchTerms: ['retention', 'delete', 'purge'],
        featureKey: 'repository.cleanuppolicies',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'routing-rules',
        path: 'repository/routing-rules',
        label: 'Routing Rules',
        description: 'Control component access by path patterns',
        searchTerms: ['block', 'allow', 'filter'],
        featureKey: 'repository.routingrules',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'content-selectors',
        path: 'repository/selectors',
        label: 'Content Selectors',
        description: 'Define content filtering expressions',
        searchTerms: ['csel', 'filter', 'query'],
        featureKey: 'repository.selectors',
        visibilityRequirements: { requiresPermission: 'nexus:selectors:read' },
      },
      {
        id: 'data-store',
        path: 'repository/datastore',
        label: 'Data Store',
        description: 'Configure repository metadata database',
        searchTerms: ['database', 'postgresql', 'h2', 'jdbc'],
        cloudExcluded: true,
        featureKey: 'repository.datastore',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'proprietary',
        path: 'repository/proprietary',
        label: 'Proprietary',
        description: 'Manage proprietary component settings',
        featureKey: 'repository.proprietary',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
    ],
  },
  {
    id: 'security',
    label: 'Security',
    cards: [
      {
        id: 'users',
        path: 'security/users',
        label: 'Users',
        description: 'Manage user accounts and access',
        featureKey: 'security.users',
        visibilityRequirements: { requiresPermission: 'nexus:users:read' },
      },
      {
        id: 'roles',
        path: 'security/roles',
        label: 'Roles',
        description: 'Define user permissions and access control',
        featureKey: 'security.roles',
        visibilityRequirements: { requiresPermission: 'nexus:roles:read' },
      },
      {
        id: 'privileges',
        path: 'security/privileges',
        label: 'Privileges',
        description: 'Configure granular permission settings',
        featureKey: 'security.privileges',
        visibilityRequirements: { requiresPermission: 'nexus:privileges:read' },
      },
      {
        id: 'realms',
        path: 'security/realms',
        label: 'Realms',
        description: 'Configure authentication sources and order',
        cloudExcluded: true,
        featureKey: 'security.realms',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'ldap',
        path: 'security/ldap',
        label: 'LDAP',
        description: 'Configure LDAP integration',
        searchTerms: ['active directory', 'ad', 'directory service'],
        cloudExcluded: true,
        featureKey: 'security.ldap',
        visibilityRequirements: { requiresPermission: 'nexus:ldap:read' },
      },
      {
        id: 'saml',
        path: 'security/saml',
        label: 'SAML',
        description: 'Configure SAML single sign-on',
        searchTerms: ['sso', 'single sign-on'],
        cloudExcluded: true,
        featureKey: 'security.saml',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'oauth2',
        path: 'security/oauth2',
        label: 'OAuth2',
        description: 'Configure OAuth 2.0 authentication',
        cloudExcluded: true,
        featureKey: 'security.oauth2',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'crowd',
        path: 'security/crowd',
        label: 'Crowd',
        description: 'Configure Atlassian Crowd integration',
        cloudExcluded: true,
        featureKey: 'security.crowd',
        visibilityRequirements: { requiresPermission: 'nexus:crowd:read' },
      },
      {
        id: 'anonymous',
        path: 'security/anonymous',
        label: 'Anonymous',
        description: 'Configure anonymous access settings',
        cloudExcluded: true,
        featureKey: 'security.anonymous',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'ssl-certificates',
        path: 'security/sslcertificates',
        label: 'SSL Certificates',
        description: 'Manage trusted SSL certificates',
        searchTerms: ['tls', 'https', 'certificate'],
        cloudExcluded: true,
        featureKey: 'security.sslcertificates',
        visibilityRequirements: { requiresPermission: 'nexus:ssl-truststore:read' },
      },
      {
        id: 'user-tokens',
        path: 'security/user-tokens',
        label: 'User Tokens',
        description: 'Manage API access tokens',
        searchTerms: ['api', 'token', 'authentication'],
        featureKey: 'security.usertokens',
        visibilityRequirements: { requiresPermission: 'nexus:usertoken-settings:read' },
      },
      {
        id: 'ip-allowlist',
        path: 'security/ip-allowlist',
        label: 'IP Allow List',
        description: 'Manage IP address allow list for access control',
        searchTerms: ['ip', 'allowlist', 'cidr', 'firewall', 'access control'],
        proOnly: true,
        adminOnly: true,
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
    ],
  },
  {
    id: 'support',
    label: 'Support',
    cloudExcluded: true,
    cards: [
      {
        id: 'system-info',
        path: 'support/systeminformation',
        label: 'System Info',
        description: 'View system information and diagnostics',
        searchTerms: ['information', 'version', 'diagnostics'],
        featureKey: 'support.systeminfo',
        visibilityRequirements: { requiresPermission: 'nexus:atlas:read' },
      },
      {
        id: 'logs',
        path: 'support/logs',
        label: 'Logs',
        description: 'View application logs',
        searchTerms: ['log', 'error', 'debug'],
        featureKey: 'support.logs',
        visibilityRequirements: { requiresPermission: 'nexus:logging:read' },
      },
      {
        id: 'logging-config',
        path: 'support/logging',
        label: 'Logging Config',
        description: 'Configure log levels and output',
        searchTerms: ['logger', 'log level', 'debug'],
        featureKey: 'support.logging',
        visibilityRequirements: { requiresPermission: 'nexus:logging:read' },
      },
      {
        id: 'metric-health',
        path: 'support/metrichealth',
        label: 'Metric Health',
        description: 'View system metrics and health checks',
        searchTerms: ['status', 'health', 'metrics', 'monitoring'],
        featureKey: 'support.metrics',
        visibilityRequirements: { requiresPermission: 'nexus:metrics:read' },
      },
      {
        id: 'support-zip',
        path: 'support/supportzip',
        label: 'Support Zip',
        description: 'Generate support diagnostics bundle',
        searchTerms: ['diagnostic', 'troubleshoot', 'bundle'],
        featureKey: 'support.supportzip',
        visibilityRequirements: { requiresPermission: 'nexus:supportzip:read' },
      },
      {
        id: 'support-request',
        path: 'support/supportrequest',
        label: 'Support Request',
        description: 'Submit a support request',
        searchTerms: ['help', 'ticket', 'contact'],
        featureKey: 'support.supportrequest',
        visibilityRequirements: { requiresPermission: 'nexus:atlas:read' },
      },
    ],
  },
  {
    id: 'system',
    label: 'System',
    cards: [
      {
        id: 'tasks',
        path: 'system/tasks',
        label: 'Tasks',
        description: 'Schedule and monitor system tasks',
        searchTerms: ['cron', 'schedule', 'job'],
        featureKey: 'system.tasks',
        visibilityRequirements: { requiresPermission: 'nexus:tasks:read' },
      },
      {
        id: 'email',
        path: 'system/emailserver',
        label: 'Email',
        description: 'Configure email server and notifications',
        searchTerms: ['smtp', 'mail', 'notification'],
        featureKey: 'system.emailserver',
        cloudExcluded: true,
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'http',
        path: 'system/http',
        label: 'HTTP',
        description: 'Configure HTTP and HTTPS settings',
        searchTerms: ['proxy', 'timeout', 'connection'],
        featureKey: 'system.http',
        cloudExcluded: true,
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'api',
        path: 'system/api',
        fullHash: '#preview/browse/api',
        label: 'API',
        description: 'Configure REST API settings',
        searchTerms: ['rest', 'endpoint'],
        featureKey: 'system.api',
      },
      {
        id: 'capabilities',
        path: 'system/capabilities',
        label: 'Capabilities',
        description: 'Configure system capabilities and integrations',
        featureKey: 'system.capabilities',
        visibilityRequirements: { requiresPermission: 'nexus:capabilities:read' },
      },
      {
        id: 'licensing',
        path: 'system/licensing',
        label: 'Licensing',
        description: 'View and manage license information',
        searchTerms: ['license', 'subscription'],
        cloudExcluded: true,
        featureKey: 'system.licensing',
        visibilityRequirements: { requiresPermission: 'nexus:licensing:read' },
      },
      {
        id: 'nodes',
        path: 'system/nodes',
        label: 'Nodes',
        description: 'View cluster nodes and status',
        searchTerms: ['cluster', 'ha', 'high availability'],
        cloudExcluded: true,
        featureKey: 'system.nodes',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'iq-server',
        path: 'iq',
        label: 'IQ Server',
        description: 'Configure Sonatype IQ Server integration',
        searchTerms: ['lifecycle', 'policy'],
        featureKey: 'iqserver',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'preview-ui',
        path: 'system/previewui',
        label: 'Nexus One UI',
        description: 'Configure Nexus One UI settings and features',
        searchTerms: ['interface', 'ui', 'theme'],
        // No featureKey — always available
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
      {
        id: 'upgrade',
        path: 'system/upgrade',
        label: 'Upgrade',
        description: 'Manage system upgrades and updates',
        searchTerms: ['update', 'version'],
        cloudExcluded: true,
        featureKey: 'system.upgrade',
        visibilityRequirements: { requiresPermission: 'nexus:settings:read' },
      },
    ],
  },
];
