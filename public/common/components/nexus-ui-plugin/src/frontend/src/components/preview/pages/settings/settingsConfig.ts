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
 */

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
    id: 'security',
    label: 'Security',
    cards: [
      {
        id: 'users',
        path: 'security/users',
        label: 'Users',
        description: 'Manage user accounts and access',
        featureKey: 'security.users',
      },
      {
        id: 'roles',
        path: 'security/roles',
        label: 'Roles',
        description: 'Define user permissions and access control',
        featureKey: 'security.roles',
      },
      {
        id: 'privileges',
        path: 'security/privileges',
        label: 'Privileges',
        description: 'Configure granular permission settings',
        featureKey: 'security.privileges',
      },
      {
        id: 'realms',
        path: 'security/realms',
        label: 'Realms',
        description: 'Configure authentication sources and order',
        cloudExcluded: true,
        featureKey: 'security.realms',
      },
      {
        id: 'ldap',
        path: 'security/ldap',
        label: 'LDAP',
        description: 'Configure LDAP integration',
        searchTerms: ['active directory', 'ad', 'directory service'],
        cloudExcluded: true,
        featureKey: 'security.ldap',
      },
      {
        id: 'saml',
        path: 'security/saml',
        label: 'SAML',
        description: 'Configure SAML single sign-on',
        searchTerms: ['sso', 'single sign-on'],
        cloudExcluded: true,
        featureKey: 'security.saml',
      },
      {
        id: 'oauth2',
        path: 'security/oauth2',
        label: 'OAuth2',
        description: 'Configure OAuth 2.0 authentication',
        cloudExcluded: true,
        featureKey: 'security.oauth2',
      },
      {
        id: 'crowd',
        path: 'security/crowd',
        label: 'Crowd',
        description: 'Configure Atlassian Crowd integration',
        cloudExcluded: true,
        featureKey: 'security.crowd',
      },
      {
        id: 'anonymous',
        path: 'security/anonymous',
        label: 'Anonymous',
        description: 'Configure anonymous access settings',
        cloudExcluded: true,
        featureKey: 'security.anonymous',
      },
      {
        id: 'ssl-certificates',
        path: 'security/sslcertificates',
        label: 'SSL Certificates',
        description: 'Manage trusted SSL certificates',
        searchTerms: ['tls', 'https', 'certificate'],
        cloudExcluded: true,
        featureKey: 'security.sslcertificates',
      },
      {
        id: 'user-tokens',
        path: 'security/user-tokens',
        label: 'User Tokens',
        description: 'Manage API access tokens',
        searchTerms: ['api', 'token', 'authentication'],
        featureKey: 'security.usertokens',
      },
      {
        id: 'ip-allowlist',
        path: 'security/ip-allowlist',
        label: 'IP Allow List',
        description: 'Manage IP address allow list for access control',
        searchTerms: ['ip', 'allowlist', 'cidr', 'firewall', 'access control'],
        proOnly: true,
        adminOnly: true,
      },
    ],
  },
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
      },
      {
        id: 'blob-stores',
        path: 'repository/blobstores',
        label: 'Blob Stores',
        description: 'Configure local and cloud blob storage',
        searchTerms: ['storage', 's3', 'azure', 'google cloud'],
        cloudExcluded: true,
        featureKey: 'repository.blobstores',
      },
      {
        id: 'cleanup-policies',
        path: 'repository/cleanup-policies',
        label: 'Cleanup Policies',
        description: 'Configure automated component cleanup',
        searchTerms: ['retention', 'delete', 'purge'],
        featureKey: 'repository.cleanuppolicies',
      },
      {
        id: 'routing-rules',
        path: 'repository/routing-rules',
        label: 'Routing Rules',
        description: 'Control component access by path patterns',
        searchTerms: ['block', 'allow', 'filter'],
        featureKey: 'repository.routingrules',
      },
      {
        id: 'content-selectors',
        path: 'repository/selectors',
        label: 'Content Selectors',
        description: 'Define content filtering expressions',
        searchTerms: ['csel', 'filter', 'query'],
        featureKey: 'repository.selectors',
      },
      {
        id: 'data-store',
        path: 'repository/datastore',
        label: 'Data Store',
        description: 'Configure repository metadata database',
        searchTerms: ['database', 'postgresql', 'h2', 'jdbc'],
        cloudExcluded: true,
        featureKey: 'repository.datastore',
      },
      {
        id: 'proprietary',
        path: 'repository/proprietary',
        label: 'Proprietary',
        description: 'Manage proprietary component settings',
        featureKey: 'repository.proprietary',
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
      },
      {
        id: 'email',
        path: 'system/emailserver',
        label: 'Email',
        description: 'Configure email server and notifications',
        searchTerms: ['smtp', 'mail', 'notification'],
        featureKey: 'system.emailserver',
      },
      {
        id: 'http',
        path: 'system/http',
        label: 'HTTP',
        description: 'Configure HTTP and HTTPS settings',
        searchTerms: ['proxy', 'timeout', 'connection'],
        featureKey: 'system.http',
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
      },
      {
        id: 'licensing',
        path: 'system/licensing',
        label: 'Licensing',
        description: 'View and manage license information',
        searchTerms: ['license', 'subscription'],
        cloudExcluded: true,
        featureKey: 'system.licensing',
      },
      {
        id: 'nodes',
        path: 'system/nodes',
        label: 'Nodes',
        description: 'View cluster nodes and status',
        searchTerms: ['cluster', 'ha', 'high availability'],
        cloudExcluded: true,
        featureKey: 'system.nodes',
      },
      {
        id: 'iq-server',
        path: 'iq',
        label: 'IQ Server',
        description: 'Configure Sonatype IQ Server integration',
        searchTerms: ['lifecycle', 'policy'],
        featureKey: 'iqserver',
      },
      {
        id: 'preview-ui',
        path: 'system/previewui',
        label: 'Nexus One UI',
        description: 'Configure Nexus One UI settings and features',
        searchTerms: ['interface', 'ui', 'theme'],
        // No featureKey — always available
      },
      {
        id: 'upgrade',
        path: 'system/upgrade',
        label: 'Upgrade',
        description: 'Manage system upgrades and updates',
        searchTerms: ['update', 'version'],
        cloudExcluded: true,
        featureKey: 'system.upgrade',
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
      },
      {
        id: 'logs',
        path: 'support/logs',
        label: 'Logs',
        description: 'View application logs',
        searchTerms: ['log', 'error', 'debug'],
        featureKey: 'support.logs',
      },
      {
        id: 'logging-config',
        path: 'support/logging',
        label: 'Logging Config',
        description: 'Configure log levels and output',
        searchTerms: ['logger', 'log level', 'debug'],
        featureKey: 'support.logging',
      },
      {
        id: 'metric-health',
        path: 'support/metrichealth',
        label: 'Metric Health',
        description: 'View system metrics and health checks',
        searchTerms: ['status', 'health', 'metrics', 'monitoring'],
        featureKey: 'support.metrics',
      },
      {
        id: 'support-zip',
        path: 'support/supportzip',
        label: 'Support Zip',
        description: 'Generate support diagnostics bundle',
        searchTerms: ['diagnostic', 'troubleshoot', 'bundle'],
        featureKey: 'support.supportzip',
      },
      {
        id: 'support-request',
        path: 'support/supportrequest',
        label: 'Support Request',
        description: 'Submit a support request',
        searchTerms: ['help', 'ticket', 'contact'],
        featureKey: 'support.supportrequest',
      },
    ],
  },
];
