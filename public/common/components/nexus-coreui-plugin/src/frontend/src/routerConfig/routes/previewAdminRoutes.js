/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/**
 * Preview Admin Routes - Phase 4.0 Fork Infrastructure
 * 
 * Admin (Settings) routes for Preview UI.
 * Uses ComingSoonPage for features being migrated.
 * Uses SettingsPageLayoutRadix for secondary sidebar navigation.
 * 
 * IMPORTANT: Route names must match the default admin route names (with preview. prefix)
 * so that useDefaultAdminRouteName() works correctly.
 * 
 * URL Structure (path-based routing):
 * - /preview/admin/repositories → preview.admin.repository.repositories
 * - /preview/admin/blobstores   → preview.admin.repository.blobstores
 * - etc.
 */

import React from 'react';
import {UIView} from '@uirouter/react';
import {Permissions} from '@sonatype/nexus-ui-plugin';
import {lazyLoad} from './lazyLoad';
import { withFeatureGate } from '../../components/super/shared/FeatureGate';

const SettingsPageLayoutRadix = lazyLoad(() => import('../../components/super/shared/SettingsPageLayoutRadix'));
const SettingsHubPage = lazyLoad(() => import('../../components/super/settings/SettingsHubPage'));

// SONATYPE INTERNAL — tree-shaken out in production (requires __SONATYPE_INTERNAL__ define)
const SonatypeTestHub = lazyLoad(() =>
  import('../../components/super/sonatype-internal/SonatypeTestHub')
);
const SonatypeInternalTestDashboard = lazyLoad(() =>
  import('../../components/super/sonatype-internal/test-dashboard/MalwareDashboardTestPage')
    .then((m) => ({ default: m.default }))
);
const SonatypeInternalTestScenario = lazyLoad(() =>
  import('../../components/super/sonatype-internal/test-dashboard/MalwareDashboardTestPage')
    .then((m) => {
      const ScenarioPage = ({ transition }) => {
        const id = transition?.params?.().scenarioId ?? 'A';
        return React.createElement(m.MalwareDashboardScenarioPage, { scenarioId: id });
      };
      return { default: ScenarioPage };
    })
);
const SonatypeInternalTestMalwareDefense = lazyLoad(() =>
  import('../../components/super/sonatype-internal/test-malware-defense/MalwareDefenseTestPage')
);
const SonatypeInternalTestSearchFw = lazyLoad(() =>
  import('../../components/super/sonatype-internal/test-search-fw/SearchFirewallTestPage')
);
const SonatypeInternalTestSearchHc = lazyLoad(() =>
  import('../../components/super/sonatype-internal/test-search-hc/SearchHealthCheckTestPage')
);

const sonatypeInternalRoutes = [
  // Hub — index of all test pages
  {
    name: 'preview.test',
    url: '/test',
    component: SonatypeTestHub,
    data: { title: '[Internal] Test Hub' },
  },
  // Dashboard / MalwareStatusCard
  {
    name: 'preview.test-dashboard',
    url: '/test-dashboard',
    component: SonatypeInternalTestDashboard,
    data: { title: '[Internal] Test — Dashboard' },
  },
  {
    name: 'preview.test-dashboard.scenario',
    url: '/test-dashboard/scenario/:scenarioId',
    component: SonatypeInternalTestScenario,
    data: { title: '[Internal] Dashboard Scenario' },
  },
  // Malware Defense page states
  {
    name: 'preview.test-malware-defense',
    url: '/test-malware-defense',
    component: SonatypeInternalTestMalwareDefense,
    data: { title: '[Internal] Test — Malware Defense' },
  },
  // Firewall cell permutations
  {
    name: 'preview.test-search-fw',
    url: '/test-search-fw',
    component: SonatypeInternalTestSearchFw,
    data: { title: '[Internal] Test — Firewall Cell' },
  },
  // Health Check cell permutations
  {
    name: 'preview.test-search-hc',
    url: '/test-search-hc',
    component: SonatypeInternalTestSearchHc,
    data: { title: '[Internal] Test — Health Check Cell' },
  },
];

// =============================================================================
// PLACEHOLDER COMPONENTS - All show ComingSoonPage with specific descriptions
// =============================================================================

// Repository Section - WIP pages wrapped with FeatureGate (Coming Soon in production)
const RepositoriesPageRaw = lazyLoad(() => import('../../components/super/settings/repository/repositories/RepositoriesPage'));
// Profile is a sub-route of repositories — gated with the same repository.repositories flag
const RepositoryProfilePageRaw = lazyLoad(() => import('../../components/super/settings/repository/profile/RepositoryProfilePage'));
const BlobStoresPageRaw = lazyLoad(() => import('../../components/super/settings/repository/blob-stores/BlobStoresPage'));
const ContentSelectorsPageRaw = lazyLoad(() => import('../../components/super/settings/repository/selectors/ContentSelectorsPage'));
const CleanupPoliciesPageRaw = lazyLoad(() => import('../../components/super/settings/repository/cleanup/CleanupPoliciesPage'));
const RoutingRulesPageRaw = lazyLoad(() => import('../../components/super/settings/repository/routing/RoutingRulesPage'));
const DataStorePageRaw = lazyLoad(() => import('../../components/super/settings/repository/datastore/DataStorePage'));
const ProprietaryPageRaw = lazyLoad(() => import('../../components/super/settings/repository/proprietary/ProprietaryPage'));

// Repository - Gated (Coming Soon in production, WIP accessible in dev only)
const RepositoriesPage = withFeatureGate(RepositoriesPageRaw, 'repository.repositories', 'Repositories');
const RepositoryProfilePage = withFeatureGate(RepositoryProfilePageRaw, 'repository.repositories', 'Repository Profile');
const BlobStoresPage = withFeatureGate(BlobStoresPageRaw, 'repository.blobstores', 'Blob Stores');
const ContentSelectorsPage = withFeatureGate(ContentSelectorsPageRaw, 'repository.selectors', 'Content Selectors');
const CleanupPoliciesPage = withFeatureGate(CleanupPoliciesPageRaw, 'repository.cleanuppolicies', 'Cleanup Policies');
const RoutingRulesPage = withFeatureGate(RoutingRulesPageRaw, 'repository.routingrules', 'Routing Rules');
const DataStorePage = withFeatureGate(DataStorePageRaw, 'repository.datastore', 'Data Store');
const ProprietaryPage = withFeatureGate(ProprietaryPageRaw, 'repository.proprietary', 'Proprietary');

// Security Section - WIP pages wrapped with FeatureGate (Coming Soon in production)
const PrivilegesPageRaw = lazyLoad(() => import('../../components/super/settings/security/privileges/PrivilegesPage'));
const RolesPageRaw = lazyLoad(() => import('../../components/super/settings/security/roles/RolesPage'));
const UsersPageRaw = lazyLoad(() => import('../../components/super/settings/security/users/UsersPage'));
const AnonymousPageRaw = lazyLoad(() => import('../../components/super/settings/security/anonymous/AnonymousPage'));
const CrowdPageRaw = lazyLoad(() => import('../../components/super/settings/security/crowd/CrowdPage'));
const LdapPageRaw = lazyLoad(() => import('../../components/super/settings/security/ldap/LdapPage'));
const OAuth2PageRaw = lazyLoad(() => import('../../components/super/settings/security/oauth2/OAuth2Page'));
const RealmsPageRaw = lazyLoad(() => import('../../components/super/settings/security/realms/RealmsPage'));
const SamlPageRaw = lazyLoad(() => import('../../components/super/settings/security/saml/SamlPage'));
const SslCertificatesPageRaw = lazyLoad(() => import('../../components/super/settings/security/sslcertificates/SslCertificatesPage'));
const UserTokensPageRaw = lazyLoad(() => import('../../components/super/settings/security/user-tokens/UserTokensPage'));

// Security - Gated (Coming Soon in production, WIP accessible in dev only)
const PrivilegesPage = withFeatureGate(PrivilegesPageRaw, 'security.privileges', 'Privileges');
const RolesPage = withFeatureGate(RolesPageRaw, 'security.roles', 'Roles');
const UsersPage = withFeatureGate(UsersPageRaw, 'security.users', 'Users');
const AnonymousPage = withFeatureGate(AnonymousPageRaw, 'security.anonymous', 'Anonymous Access');
const CrowdPage = withFeatureGate(CrowdPageRaw, 'security.crowd', 'Atlassian Crowd');
const LdapPage = withFeatureGate(LdapPageRaw, 'security.ldap', 'LDAP');
const OAuth2Page = withFeatureGate(OAuth2PageRaw, 'security.oauth2', 'OAuth 2.0');
const RealmsPage = withFeatureGate(RealmsPageRaw, 'security.realms', 'Realms');
const SamlPage = withFeatureGate(SamlPageRaw, 'security.saml', 'SAML');
const SslCertificatesPage = withFeatureGate(SslCertificatesPageRaw, 'security.sslcertificates', 'SSL Certificates');
const UserTokensPage = withFeatureGate(UserTokensPageRaw, 'security.usertokens', 'User Tokens');

// Support Section - WIP pages wrapped with FeatureGate (Coming Soon in production)
const LogsPageRaw = lazyLoad(() => import('../../components/super/settings/support/logs/LogsPage'));
const LoggingConfigPageRaw = lazyLoad(() => import('../../components/super/settings/support/logging-config/LoggingConfigPage'));
const SystemInfoPageRaw = lazyLoad(() => import('../../components/super/settings/support/system-info/SystemInfoPage'));
const MetricHealthPageRaw = lazyLoad(() => import('../../components/super/settings/support/metric-health/MetricHealthPage'));
const SupportRequestPageRaw = lazyLoad(() => import('../../components/super/settings/support/support-request/SupportRequestPage'));
const SupportZipPageRaw = lazyLoad(() => import('../../components/super/settings/support/support-zip/SupportZipPage'));

// Support - Gated (Coming Soon in production, WIP accessible in dev only)
const LogsPage = withFeatureGate(LogsPageRaw, 'support.logs', 'Logs');
const LoggingConfigPage = withFeatureGate(LoggingConfigPageRaw, 'support.logging', 'Logging Configuration');
const SystemInfoPage = withFeatureGate(SystemInfoPageRaw, 'support.systeminfo', 'System Information');
const MetricHealthPage = withFeatureGate(MetricHealthPageRaw, 'support.metrics', 'Metric Health');
const SupportRequestPage = withFeatureGate(SupportRequestPageRaw, 'support.supportrequest', 'Support Request');
const SupportZipPage = withFeatureGate(SupportZipPageRaw, 'support.supportzip', 'Support ZIP');

// System Section - WIP pages wrapped with FeatureGate (Coming Soon in production)
const TasksPageRaw = lazyLoad(() => import('../../components/super/settings/system/tasks/TasksPage'));
const CapabilitiesPageRaw = lazyLoad(() => import('../../components/super/settings/system/capabilities/CapabilitiesPage'));
const PreviewUiSettingsPage = lazyLoad(() => import('../../components/super/settings/system/preview-ui/PreviewUiSettingsPage'));
const EmailPageRaw = lazyLoad(() => import('../../components/super/settings/system/email/EmailPage'));
const HttpPageRaw = lazyLoad(() => import('../../components/super/settings/system/http/HttpPage'));
const LicensingPageRaw = lazyLoad(() => import('../../components/super/settings/system/licensing/LicensingPage'));
const NodesPageRaw = lazyLoad(() => import('../../components/super/settings/system/nodes/NodesPage'));
const UpgradePageRaw = lazyLoad(() => import('../../components/super/settings/system/upgrade/UpgradePage'));
const UsagePage = lazyLoad(() => import('../../components/super/settings/system/usage/UsagePage'));

// System - Gated (Coming Soon in production, WIP accessible in dev only)
const TasksPage = withFeatureGate(TasksPageRaw, 'system.tasks', 'Tasks');
const CapabilitiesPage = withFeatureGate(CapabilitiesPageRaw, 'system.capabilities', 'Capabilities');
const EmailPage = withFeatureGate(EmailPageRaw, 'system.emailserver', 'Email Server');
const HttpPage = withFeatureGate(HttpPageRaw, 'system.http', 'HTTP');
const LicensingPage = withFeatureGate(LicensingPageRaw, 'system.licensing', 'Licensing');
const NodesPage = withFeatureGate(NodesPageRaw, 'system.nodes', 'Nodes');
const UpgradePage = withFeatureGate(UpgradePageRaw, 'system.upgrade', 'Upgrade');

// User Section - WIP pages wrapped with FeatureGate (Coming Soon in production)
const UserAccountPageRaw = lazyLoad(() => import('../../components/super/settings/user-account/UserAccountPage'));

// IQ Server - WIP pages wrapped with FeatureGate (Coming Soon in production)
const IqServerPageRaw = lazyLoad(() => import('../../components/super/settings/system/iq-server/IqServerPage'));

// User Account, IQ Server - Gated (Coming Soon in production, WIP accessible in dev only)
const UserAccountPage = withFeatureGate(UserAccountPageRaw, 'useraccount', 'User Account');
const IqServerPage = withFeatureGate(IqServerPageRaw, 'iqserver', 'IQ Server');

// =============================================================================
// ROUTE DEFINITIONS
// Route names MUST match default admin routes (preview.admin.repository.repositories, etc.)
// URLs are flat at the admin level
// =============================================================================

// Exported separately so test pages can be registered at the preview.* level
// (not preview.admin.*) — they render standalone without the Settings sidebar.
export const sonatypeInternalTestRoutes = sonatypeInternalRoutes;

export const previewAdminRoutes = [
  // =============================================================================
  // SETTINGS HUB - Standalone at preview.settings (no sidebar)
  // =============================================================================
  {
    name: 'preview.settings',
    url: '/settings',
    component: SettingsHubPage,
    data: {
      title: 'Settings',
      visibilityRequirements: {
        requiresAnyPermission: [
          Permissions.REPOSITORY_ADMIN.READ,
          Permissions.BLOB_STORES.READ,
          Permissions.SELECTORS.READ,
          Permissions.PRIVILEGES.READ,
          Permissions.ROLES.READ,
          Permissions.USERS.READ,
          Permissions.SETTINGS.READ,
          Permissions.SSL_TRUSTSTORE.READ,
          Permissions.LDAP.READ,
          Permissions.USER_TOKENS_SETTINGS.READ,
          'nexus:crowd:read',
          Permissions.TASKS.READ,
          Permissions.CAPABILITIES.READ,
          Permissions.LOGGING.READ,
          Permissions.ATLAS.READ,
          Permissions.METRICS.READ,
          Permissions.LICENSING.READ,
        ],
      },
    },
  },
  // =============================================================================
  // ROOT ADMIN ROUTE - Abstract parent for /admin paths
  // =============================================================================
  {
    name: 'preview.admin',
    url: '/admin',
    abstract: true,
    component: UIView,
  },

  // =============================================================================
  // SETTINGS HUB REDIRECT - /admin/settings redirects to Settings Hub
  // =============================================================================
  {
    name: 'preview.admin.settings',
    url: '/settings',
    redirectTo: 'preview.settings',
  },

  // =============================================================================
  // ADMIN PAGES WRAPPER - Provides sidebar for all settings pages
  // =============================================================================
  {
    name: 'preview.admin.pages',
    url: '',
    component: SettingsPageLayoutRadix,
    data: {
      visibilityRequirements: {
        requiresAnyPermission: [
          Permissions.REPOSITORY_ADMIN.READ,
          Permissions.BLOB_STORES.READ,
          Permissions.SELECTORS.READ,
          Permissions.PRIVILEGES.READ,
          Permissions.ROLES.READ,
          Permissions.USERS.READ,
          Permissions.SETTINGS.READ,
          Permissions.SSL_TRUSTSTORE.READ,
          Permissions.LDAP.READ,
          Permissions.USER_TOKENS_SETTINGS.READ,
          'nexus:crowd:read',
          Permissions.TASKS.READ,
          Permissions.CAPABILITIES.READ,
          Permissions.LOGGING.READ,
          Permissions.ATLAS.READ,
          Permissions.METRICS.READ,
          Permissions.LICENSING.READ,
        ],
      },
    },
  },

  // =============================================================================
  // REPOSITORY SECTION
  // Names match: admin.repository.* → preview.admin.repository.*
  // =============================================================================
  {
    name: 'preview.admin.repository',
    url: '/repository',
    redirectTo: 'preview.admin.repository.repositories.list', /* DO NOT CHANGE - blobstores not on cloud (Agent0 fix) */
    component: UIView,
    data: { title: 'Repository' },
  },
  {
    name: 'preview.admin.repository.repositories',
    url: '/repositories',
    abstract: true,
    component: UIView,
    data: { title: 'Repositories' },
  },
  {
    name: 'preview.admin.repository.repositories.list',
    url: '',
    component: RepositoriesPage,
    data: { title: 'Repositories' },
  },
  {
    name: 'preview.admin.repository.repositories.create',
    url: '/create',
    component: RepositoriesPage,
    data: { title: 'Create Repository' },
  },
  {
    name: 'preview.admin.repository.repositories.createForm',
    url: '/create/:format/:type',
    component: RepositoriesPage,
    data: { title: 'Create Repository' },
  },
  {
    name: 'preview.admin.repository.repositories.detail',
    url: '/:repositoryName',
    component: RepositoriesPage,
    data: { title: 'Repository Details' },
  },
  {
    name: 'preview.admin.repository.repositories.profile',
    url: '/:repositoryName/profile?tab',
    component: RepositoryProfilePage,
    params: {
      tab: { value: null, type: 'string', dynamic: true },
    },
    data: { title: 'Repository Profile' },
    resolve: [
      {
        token: 'repositoryName',
        deps: ['$stateParams'],
        resolveFn: ($stateParams) => $stateParams.repositoryName,
      },
      {
        token: 'context',
        resolveFn: () => 'settings', // Settings context - shows Settings sidebar
      },
    ],
  },
  {
    name: 'preview.admin.repository.blobstores',
    url: '/blobstores',
    abstract: true,
    component: UIView,
    data: { title: 'Blob Stores' },
  },
  {
    name: 'preview.admin.repository.blobstores.list',
    url: '',
    component: BlobStoresPage,
    data: { title: 'Blob Stores' },
  },
  {
    name: 'preview.admin.repository.blobstores.create',
    url: '/create',
    component: BlobStoresPage,
    data: { title: 'Create Blob Store' },
  },
  {
    name: 'preview.admin.repository.blobstores.edit',
    url: '/:type/:name',
    component: BlobStoresPage,
    data: { title: 'Edit Blob Store' },
  },
  {
    name: 'preview.admin.repository.selectors',
    url: '/selectors',
    abstract: true,
    component: UIView,
    data: { title: 'Content Selectors' },
  },
  {
    name: 'preview.admin.repository.selectors.list',
    url: '',
    component: ContentSelectorsPage,
    data: { title: 'Content Selectors' },
  },
  {
    name: 'preview.admin.repository.selectors.create',
    url: '/create',
    component: ContentSelectorsPage,
    data: { title: 'Create Content Selector' },
  },
  {
    name: 'preview.admin.repository.selectors.detail',
    url: '/:name',
    component: ContentSelectorsPage,
    data: { title: 'Content Selector Details' },
  },
  {
    name: 'preview.admin.repository.cleanuppolicies',
    url: '/cleanuppolicies',
    abstract: true,
    component: UIView,
    data: { title: 'Cleanup Policies' },
  },
  {
    name: 'preview.admin.repository.cleanuppolicies.list',
    url: '',
    component: CleanupPoliciesPage,
    data: { title: 'Cleanup Policies' },
  },
  {
    name: 'preview.admin.repository.cleanuppolicies.create',
    url: '/create',
    component: CleanupPoliciesPage,
    data: { title: 'Create Cleanup Policy' },
  },
  {
    name: 'preview.admin.repository.cleanuppolicies.detail',
    url: '/:name',
    component: CleanupPoliciesPage,
    data: { title: 'Cleanup Policy Details' },
  },
  {
    name: 'preview.admin.repository.routingrules',
    url: '/routingrules',
    abstract: true,
    component: UIView,
    data: { title: 'Routing Rules' },
  },
  {
    name: 'preview.admin.repository.routingrules.list',
    url: '',
    component: RoutingRulesPage,
    data: { title: 'Routing Rules' },
  },
  {
    name: 'preview.admin.repository.routingrules.create',
    url: '/create',
    component: RoutingRulesPage,
    data: { title: 'Create Routing Rule' },
  },
  {
    name: 'preview.admin.repository.routingrules.preview',
    url: '/preview',
    component: RoutingRulesPage,
    data: { title: 'Routing Rules Preview' },
  },
  {
    name: 'preview.admin.repository.routingrules.detail',
    url: '/:name',
    component: RoutingRulesPage,
    data: { title: 'Routing Rule Details' },
  },
  {
    name: 'preview.admin.repository.datastore',
    url: '/datastore',
    component: DataStorePage,
    data: { title: 'Data Store' },
  },
  {
    name: 'preview.admin.repository.proprietary',
    url: '/proprietary',
    component: ProprietaryPage,
    data: { title: 'Proprietary Repositories' },
  },

  // =============================================================================
  // SECURITY SECTION  
  // Names match: admin.security.* → preview.admin.security.*
  // =============================================================================
  {
    name: 'preview.admin.security',
    url: '/security',
    redirectTo: 'preview.admin.security.privileges.list',
    component: UIView,
    data: { title: 'Security' },
  },
  {
    name: 'preview.admin.security.privileges',
    url: '/privileges',
    abstract: true,
    component: UIView,
    data: { title: 'Privileges' },
  },
  {
    name: 'preview.admin.security.privileges.list',
    url: '',
    component: PrivilegesPage,
    data: {
      title: 'Privileges',
      visibilityRequirements: {
        requiresPermission: 'nexus:privileges:read',
      },
    },
  },
  {
    name: 'preview.admin.security.privileges.create',
    url: '/create',
    component: PrivilegesPage,
    data: { title: 'Create Privilege' },
  },
  {
    name: 'preview.admin.security.privileges.createForm',
    url: '/create/:typeId',
    component: PrivilegesPage,
    params: { typeId: { type: 'string', value: null, dynamic: true } },
    data: { title: 'Create Privilege' },
  },
  {
    name: 'preview.admin.security.privileges.profile',
    url: '/:privilegeId/profile',
    component: PrivilegesPage,
    data: { title: 'Privilege Profile' },
  },
  {
    name: 'preview.admin.security.privileges.detail',
    url: '/:privilegeId',
    component: PrivilegesPage,
    data: { title: 'Privilege Details' },
  },
  {
    name: 'preview.admin.security.roles',
    url: '/roles',
    abstract: true,
    component: UIView,
    data: { title: 'Roles' },
  },
  {
    name: 'preview.admin.security.roles.list',
    url: '',
    component: RolesPage,
    data: {
      title: 'Roles',
      visibilityRequirements: {
        requiresPermission: 'nexus:roles:read',
      },
    },
  },
  {
    name: 'preview.admin.security.roles.create',
    url: '/create',
    component: RolesPage,
    data: { title: 'Create Role' },
  },
  {
    name: 'preview.admin.security.roles.detail',
    url: '/:roleId',
    component: RolesPage,
    data: { title: 'Role Details' },
  },
  {
    name: 'preview.admin.security.roles.profile',
    url: '/:roleId/profile',
    component: RolesPage,
    data: { title: 'Role Profile' },
  },
  {
    name: 'preview.admin.security.users',
    url: '/users',
    abstract: true,
    redirectTo: 'preview.admin.security.users.list',
    component: UIView,
    data: {
      title: 'Users',
      visibilityRequirements: {
        requiresPermission: 'nexus:users:read',
      },
    },
  },
  {
    name: 'preview.admin.security.users.list',
    url: '',
    component: UsersPage,
    data: { title: 'Users' },
  },
  {
    name: 'preview.admin.security.users.create',
    url: '/create',
    component: UsersPage,
    data: { title: 'Create User' },
  },
  {
    name: 'preview.admin.security.users.invite',
    url: '/invite',
    component: UsersPage,
    data: { title: 'Invite User' },
  },
  {
    name: 'preview.admin.security.users.detail',
    url: '/:userId/:source',
    component: UsersPage,
    params: {
      userId: { type: 'string', value: null, dynamic: true },
      source: { type: 'string', value: null, dynamic: true },
    },
    data: { title: 'User Details' },
  },
  {
    name: 'preview.admin.security.users.profile',
    url: '/:userId/:source/profile',
    component: UsersPage,
    params: {
      userId: { type: 'string', value: null, dynamic: true },
      source: { type: 'string', value: null, dynamic: true },
    },
    data: { title: 'User Profile' },
  },
  {
    name: 'preview.admin.security.anonymous',
    url: '/anonymous',
    component: AnonymousPage,
    data: { title: 'Anonymous Access' },
  },
  {
    name: 'preview.admin.security.ldap',
    url: '/ldap',
    abstract: true,
    component: UIView,
    data: { title: 'LDAP' },
  },
  {
    name: 'preview.admin.security.ldap.list',
    url: '',
    component: LdapPage,
    data: { title: 'LDAP' },
  },
  {
    name: 'preview.admin.security.ldap.create',
    url: '/create',
    component: LdapPage,
    data: { title: 'Create LDAP Server' },
  },
  {
    name: 'preview.admin.security.ldap.detail',
    url: '/:serverId',
    component: LdapPage,
    data: { title: 'LDAP Server Details' },
  },
  {
    name: 'preview.admin.security.realms',
    url: '/realms',
    component: RealmsPage,
    data: { title: 'Realms' },
  },
  {
    name: 'preview.admin.security.sslcertificates',
    url: '/sslcertificates',
    abstract: true,
    component: UIView,
    data: { 
      title: 'SSL Certificates',
      visibilityRequirements: {
        requiresPermission: 'nexus:ssl-truststore:read',
      },
    },
  },
  {
    name: 'preview.admin.security.sslcertificates.list',
    url: '',
    component: SslCertificatesPage,
    data: { 
      title: 'SSL Certificates',
      visibilityRequirements: {
        requiresPermission: 'nexus:ssl-truststore:read',
      },
    },
  },
  {
    name: 'preview.admin.security.usertoken',
    url: '/usertoken',
    component: UserTokensPage,
    data: { title: 'User Tokens' },
  },
  {
    name: 'preview.admin.security.saml',
    url: '/saml',
    component: SamlPage,
    data: { title: 'SAML' },
  },
  {
    name: 'preview.admin.security.oauth2',
    url: '/oauth2',
    component: OAuth2Page,
    data: { title: 'OAuth 2.0' },
  },
  {
    name: 'preview.admin.security.crowd',
    url: '/crowd',
    component: CrowdPage,
    data: { title: 'Atlassian Crowd' },
  },

  // =============================================================================
  // SUPPORT SECTION
  // Names match: admin.support.* → preview.admin.support.*
  // =============================================================================
  {
    name: 'preview.admin.support',
    url: '/support',
    redirectTo: 'preview.admin.support.logs.list',
    component: UIView,
    data: { title: 'Support' },
  },
  {
    name: 'preview.admin.support.logs',
    url: '/logs',
    abstract: true,
    component: UIView,
    data: { title: 'Logs' },
  },
  {
    name: 'preview.admin.support.logs.list',
    url: '',
    component: LogsPage,
    data: {
      title: 'Logs',
      visibilityRequirements: {
        requiresPermission: 'nexus:logging:read',
      },
    },
  },
  {
    name: 'preview.admin.support.logs.detail',
    url: '/:filename',
    component: LogsPage,
    data: {
      title: 'Log Viewer',
      visibilityRequirements: {
        requiresPermission: 'nexus:logging:read',
      },
    },
  },
  {
    name: 'preview.admin.support.logging',
    url: '/logging',
    abstract: true,
    component: UIView,
    data: { title: 'Logging' },
  },
  {
    name: 'preview.admin.support.logging.list',
    url: '',
    component: LoggingConfigPage,
    data: {
      title: 'Logging',
      visibilityRequirements: {
        requiresPermission: 'nexus:logconfig:read',
      },
    },
  },
  {
    name: 'preview.admin.support.logging.create',
    url: '/create',
    component: LoggingConfigPage,
    data: {
      title: 'Create Logger',
      visibilityRequirements: {
        requiresPermission: 'nexus:logconfig:create',
      },
    },
  },
  {
    name: 'preview.admin.support.logging.detail',
    url: '/:itemId',
    component: LoggingConfigPage,
    data: {
      title: 'Logger Details',
      visibilityRequirements: {
        requiresPermission: 'nexus:logconfig:read',
      },
    },
  },
  {
    name: 'preview.admin.support.systeminformation',
    url: '/systeminformation',
    component: SystemInfoPage,
    data: {
      title: 'System Information',
      visibilityRequirements: {
        requiresPermission: 'nexus:atlas:read',
      },
    },
  },
  {
    name: 'preview.admin.support.metrichealth',
    url: '/metrichealth',
    component: MetricHealthPage,
    data: {
      title: 'Metric Health',
      visibilityRequirements: {
        requiresPermission: 'nexus:metrics:read',
      },
    },
  },
  {
    name: 'preview.admin.support.supportrequest',
    url: '/supportrequest',
    component: SupportRequestPage,
    data: {
      title: 'Support Request',
      visibilityRequirements: {
        requiresPermission: 'nexus:atlas:read',
      },
    },
  },
  {
    name: 'preview.admin.support.supportzip',
    url: '/supportzip',
    component: SupportZipPage,
    data: {
      title: 'Support ZIP',
      visibilityRequirements: {
        requiresPermission: 'nexus:supportzip:read',
      },
    },
  },

  // =============================================================================
  // SYSTEM SECTION
  // Names match: admin.system.* → preview.admin.system.*
  // =============================================================================
  {
    name: 'preview.admin.system',
    url: '/system',
    redirectTo: 'preview.admin.system.tasks.list',
    component: UIView,
    data: { title: 'System' },
  },
  {
    name: 'preview.admin.system.tasks',
    url: '/tasks',
    abstract: true,
    component: UIView,
    data: { title: 'Tasks' },
  },
  {
    name: 'preview.admin.system.tasks.list',
    url: '',
    component: TasksPage,
    data: { title: 'Tasks' },
  },
  {
    name: 'preview.admin.system.tasks.create',
    url: '/create',
    component: TasksPage,
    data: { title: 'Create Task' },
  },
  {
    name: 'preview.admin.system.tasks.createWithType',
    url: '/create/:typeId',
    component: TasksPage,
    data: { title: 'Create Task' },
  },
  {
    name: 'preview.admin.system.tasks.detail',
    url: '/:taskId',
    component: TasksPage,
    data: { title: 'Task Details' },
  },
  {
    name: 'preview.admin.system.capabilities',
    url: '/capabilities',
    abstract: true,
    component: UIView,
    data: { title: 'Capabilities' },
  },
  {
    name: 'preview.admin.system.capabilities.list',
    url: '',
    component: CapabilitiesPage,
    data: { title: 'Capabilities' },
  },
  {
    name: 'preview.admin.system.capabilities.create',
    url: '/create',
    component: CapabilitiesPage,
    data: { title: 'Create Capability' },
  },
  {
    name: 'preview.admin.system.capabilities.createWithType',
    url: '/create/:typeId',
    component: CapabilitiesPage,
    params: { typeId: { type: 'string', value: null, dynamic: true } },
    data: { title: 'Create Capability' },
  },
  {
    name: 'preview.admin.system.capabilities.detail',
    url: '/:capabilityId',
    component: CapabilitiesPage,
    params: { capabilityId: { type: 'string', value: null, dynamic: true } },
    data: { title: 'Capability Details' },
  },
  {
    name: 'preview.admin.system.previewui',
    url: '/previewui',
    component: PreviewUiSettingsPage,
    data: { 
      title: 'Nexus One UI',
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
    },
  },
  {
    name: 'preview.admin.system.emailserver',
    url: '/emailserver',
    component: EmailPage,
    data: { title: 'Email Server' },
  },
  {
    name: 'preview.admin.system.http',
    url: '/http',
    component: HttpPage,
    data: { title: 'HTTP' },
  },
  {
    name: 'preview.admin.system.licensing',
    url: '/licensing',
    component: LicensingPage,
    data: { title: 'Licensing' },
  },
  {
    name: 'preview.admin.system.nodes',
    url: '/nodes',
    component: NodesPage,
    data: { title: 'Nodes' },
  },
  {
    name: 'preview.admin.system.upgrade',
    url: '/upgrade',
    component: UpgradePage,
    data: { title: 'Upgrade' },
  },
  {
    name: 'preview.admin.system.usage',
    url: '/usage',
    component: UsagePage,
    data: {
      title: 'Usage',
      visibilityRequirements: {
        permissions: [Permissions.METRICS.READ],
      },
    },
  },

  // =============================================================================
  // IQ SERVER (top-level admin route, not nested under system)
  // =============================================================================
  {
    name: 'preview.admin.iq',
    url: '/iq',
    component: IqServerPage,
    data: {
      title: 'IQ Server',
      visibilityRequirements: {
        requiresPermission: 'nexus:settings:read',
      },
    },
  },

  // =============================================================================
  // SONATYPE INTERNAL TEST PAGES — now registered at preview.test* level
  // (standalone, outside SettingsPageLayoutRadix) via sonatypeInternalTestRoutes
  // export in routerConfig.js — removed from here (xtrz).
  //
  // Redirect stubs so bookmarked #preview/admin/test* URLs still work.
  // =============================================================================
  {
    name: 'preview.admin.test',
    url: '/test',
    redirectTo: 'preview.test',
  },
  {
    name: 'preview.admin.test-dashboard',
    url: '/test-dashboard',
    redirectTo: 'preview.test-dashboard',
  },
  {
    name: 'preview.admin.test-malware-defense',
    url: '/test-malware-defense',
    redirectTo: 'preview.test-malware-defense',
  },
  {
    name: 'preview.admin.test-search-fw',
    url: '/test-search-fw',
    redirectTo: 'preview.test-search-fw',
  },
  {
    name: 'preview.admin.test-search-hc',
    url: '/test-search-hc',
    redirectTo: 'preview.test-search-hc',
  },

  // =============================================================================
  // USER ACCOUNT — redirect to standalone preview.user.account (outside Settings layout)
  // =============================================================================
  {
    name: 'preview.admin.useraccount',
    url: '/useraccount',
    // Redirect so old/bookmarked links land on the standalone page, not Settings
    redirectTo: 'preview.user.account',
    data: { title: 'User Account' },
  },
];
