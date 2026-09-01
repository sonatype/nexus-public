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
import {
  Permissions,
  withFeatureGate,
  withCloudExcluded,
  SettingsPageLayoutRadix,
  SettingsHubPage,
  SonatypeTestHub,
  SonatypeInternalTestDashboard,
  SonatypeInternalTestMalwareDefense,
  SonatypeInternalTestSearchFw,
  SonatypeInternalTestSearchHc,
  MalwareDashboardScenarioPage,
  // Raw (ungated) page components from preview. These get wrapped with
  // withFeatureGate below so each section shows ComingSoonPage in
  // production when its feature flag is off.
  RepositoriesPage as RepositoriesPageRaw,
  RepositoryProfilePage as RepositoryProfilePageRaw,
  BlobStoresPage as BlobStoresPageRaw,
  ContentSelectorsPage as ContentSelectorsPageRaw,
  CleanupPoliciesPage as CleanupPoliciesPageRaw,
  RoutingRulesPage as RoutingRulesPageRaw,
  DataStorePage as DataStorePageRaw,
  ProprietaryPage as ProprietaryPageRaw,
  PrivilegesPage as PrivilegesPageRaw,
  RolesPage as RolesPageRaw,
  UsersPage as UsersPageRaw,
  AnonymousPage as AnonymousPageRaw,
  CrowdPage as CrowdPageRaw,
  LdapPage as LdapPageRaw,
  OAuth2Page as OAuth2PageRaw,
  RealmsPage as RealmsPageRaw,
  SamlPage as SamlPageRaw,
  SslCertificatesPage as SslCertificatesPageRaw,
  UserTokensPage as UserTokensPageRaw,
  ServiceAccountTokensPage,
  LogsPage as LogsPageRaw,
  LoggingConfigPage as LoggingConfigPageRaw,
  SystemInfoPage as SystemInfoPageRaw,
  MetricHealthPage as MetricHealthPageRaw,
  RecoveryModePage as RecoveryModePageRaw,
  SupportRequestPage as SupportRequestPageRaw,
  SupportZipPage as SupportZipPageRaw,
  TasksPage as TasksPageRaw,
  CapabilitiesPage as CapabilitiesPageRaw,
  PreviewUiSettingsPage,
  EmailPage as EmailPageRaw,
  HttpPage as HttpPageRaw,
  LicensingPage as LicensingPageRaw,
  NodesPage as NodesPageRaw,
  UpgradePage as UpgradePageRaw,
  UsagePage,
  UserAccountPage as UserAccountPageRaw,
  IqServerOverviewPage,
  IqServerConnectedPage as IqServerConnectedPageImpl,
  HostedRepoEvaluationSetupPage,
} from '@sonatype/nexus-ui-plugin';
import {lazyLoad} from './lazyLoad';

const IpAllowListPage = lazyLoad(() => import('@sonatype/nexus-ui-plugin').then(m => ({ default: m.IpAllowList })));
// ui-plugin already exports these as React.lazy(); wrapping with lazyLoad() would double-wrap them.
// Two distinct wrapper functions force React to unmount→remount when transitioning
// between iqConnected and iqConnection (same underlying component, different element type).
const IqServerConnectedPage = (props) => React.createElement(IqServerConnectedPageImpl, props);
const IqServerConnectionPage = (props) => React.createElement(IqServerConnectedPageImpl, props);

// Sonatype-internal scenario route wraps MalwareDashboardScenarioPage with
// a transition-param unwrapper. The named export from ui-plugin is the
// underlying (non-default) export from MalwareDashboardTestPage.
const SonatypeInternalTestScenario = ({ transition }) => {
  const id = transition?.params?.().scenarioId ?? 'A';
  return React.createElement(MalwareDashboardScenarioPage, { scenarioId: id });
};

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
// FEATURE-GATED PAGES
// Each gated page wraps the ui-plugin export with ComingSoonPage in
// production. Raw (ungated) components are imported above as <Name>Raw.
// =============================================================================

// Repository - Gated (Coming Soon in production, WIP accessible in dev only)
const RepositoriesPage = withFeatureGate(RepositoriesPageRaw, 'repository.repositories', 'Repositories');
const RepositoryProfilePage = withFeatureGate(RepositoryProfilePageRaw, 'repository.repositories', 'Repository Profile');
const BlobStoresPage = withFeatureGate(BlobStoresPageRaw, 'repository.blobstores', 'Blob Stores');
const ContentSelectorsPage = withFeatureGate(ContentSelectorsPageRaw, 'repository.selectors', 'Content Selectors');
const CleanupPoliciesPage = withFeatureGate(CleanupPoliciesPageRaw, 'repository.cleanuppolicies', 'Cleanup Policies');
const RoutingRulesPage = withFeatureGate(RoutingRulesPageRaw, 'repository.routingrules', 'Routing Rules');
const DataStorePage = withFeatureGate(DataStorePageRaw, 'repository.datastore', 'Data Store');
const ProprietaryPage = withFeatureGate(ProprietaryPageRaw, 'repository.proprietary', 'Proprietary');

// Security - Gated
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

// Support - Gated (Logs and Logging Configuration are self-hosted only)
const LogsPage = withCloudExcluded(
  withFeatureGate(LogsPageRaw, 'support.logs', 'Logs'),
  'Logs',
);
const LoggingConfigPage = withCloudExcluded(
  withFeatureGate(LoggingConfigPageRaw, 'support.logging', 'Logging Configuration'),
  'Logging Configuration',
);
const SystemInfoPage = withFeatureGate(SystemInfoPageRaw, 'support.systeminfo', 'System Information');
const MetricHealthPage = withFeatureGate(MetricHealthPageRaw, 'support.metrics', 'Metric Health');
const RecoveryModePage = RecoveryModePageRaw;
const SupportRequestPage = withFeatureGate(SupportRequestPageRaw, 'support.supportrequest', 'Support Request');
const SupportZipPage = withFeatureGate(SupportZipPageRaw, 'support.supportzip', 'Support ZIP');

// System - Gated
const TasksPage = withFeatureGate(TasksPageRaw, 'system.tasks', 'Tasks');
const CapabilitiesPage = withFeatureGate(CapabilitiesPageRaw, 'system.capabilities', 'Capabilities');
const EmailPage = withFeatureGate(EmailPageRaw, 'system.emailserver', 'Email Server');
const HttpPage = withCloudExcluded(
  withFeatureGate(HttpPageRaw, 'system.http', 'HTTP'),
  'HTTP',
);
const LicensingPage = withCloudExcluded(
  withFeatureGate(LicensingPageRaw, 'system.licensing', 'Licensing'),
  'Licensing',
);
const NodesPage = withFeatureGate(NodesPageRaw, 'system.nodes', 'Nodes');
const UpgradePage = withFeatureGate(UpgradePageRaw, 'system.upgrade', 'Upgrade');

// User Account
const UserAccountPage = withFeatureGate(UserAccountPageRaw, 'useraccount', 'User Account');

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
    data: {
      title: 'Repositories',
      // NEXUS-54048: match Classic/Default UI (adminRoutes.js REPOSITORIES.ROOT), which uses a
      // repository-admin prefix so a user with admin on any single repo can see the menu. A specific
      // requiresPermission: 'nexus:repository-admin:*:*:read' would NOT be satisfied by a narrower
      // per-repo grant, hiding the menu from users the Default UI shows it to.
      visibilityRequirements: {
        permissionPrefix: 'nexus:repository-admin',
      },
    },
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
    url: '/:repositoryName?tab',
    component: RepositoriesPage,
    params: {
      tab: { value: null, type: 'string', dynamic: true },
    },
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
    data: {
      title: 'Blob Stores',
      visibilityRequirements: {
        requiresPermission: Permissions.BLOB_STORES.READ,
      },
    },
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
    data: {
      title: 'Content Selectors',
      visibilityRequirements: {
        requiresPermission: Permissions.SELECTORS.READ,
      },
    },
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
    url: '/cleanup-policies',
    abstract: true,
    component: UIView,
    data: {
      title: 'Cleanup Policies',
      // NEXUS-54048: match Classic/Default UI (adminRoutes.js CLEANUPPOLICIES.ROOT), which gates on
      // nexus:* — the write endpoints require admin, and the menu must not surface to a
      // settings:read-only user who would then hit a 403.
      visibilityRequirements: {
        requiresPermission: Permissions.ADMIN,
      },
    },
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
    url: '/routing-rules',
    abstract: true,
    redirectTo: 'preview.admin.repository.routingrules.list',
    component: UIView,
    data: {
      title: 'Routing Rules',
      // NEXUS-54048: match Classic/Default UI (adminRoutes.js ROUTINGRULES.ROOT). RoutingRulesResource
      // requires nexus:* on every endpoint, including the GET/list — so settings:read here would show
      // the menu but 403 on load.
      visibilityRequirements: {
        requiresPermission: Permissions.ADMIN,
      },
    },
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
    data: {
      title: 'Data Store',
      visibilityRequirements: {
        // NEXUS-54019: match Classic/Default UI (adminRoutes.js DATASTORE.ROOT). The Data Store
        // screen is admin-only (nexus:*) AND only available with the pro-datastore bundle on
        // PRO/COMMUNITY editions — without the bundle+editions gates the tile shows where the
        // feature does not exist and then dead-ends.
        bundle: 'nexus-pro-datastore-plugin',
        requiresPermission: Permissions.ADMIN,
        editions: ['PRO', 'COMMUNITY'],
      },
    },
  },
  {
    name: 'preview.admin.repository.proprietary',
    url: '/proprietary',
    component: ProprietaryPage,
    data: {
      title: 'Proprietary Repositories',
      visibilityRequirements: {
        requiresPermission: Permissions.SETTINGS.READ,
      },
    },
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
    data: {
      title: 'Privileges',
      visibilityRequirements: {
        requiresPermission: 'nexus:privileges:read',
      },
    },
  },
  {
    name: 'preview.admin.security.privileges.list',
    url: '',
    component: PrivilegesPage,
    data: {
      title: 'Privileges',
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
    // NEXUS-52167: `?tab` keeps the active profile tab in the URL so it survives
    // browser Back. `dynamic` avoids a remount on tab switch. Mirrors
    // repositories.profile above.
    name: 'preview.admin.security.privileges.profile',
    url: '/:privilegeId/profile?tab',
    component: PrivilegesPage,
    params: {
      tab: { value: null, type: 'string', dynamic: true },
    },
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
    data: {
      title: 'Roles',
      // NEXUS-54048: match Classic/Default UI (adminRoutes.js ROLES.ROOT), which requires BOTH
      // roles:read and privileges:read (permissions[] is AND).
      visibilityRequirements: {
        permissions: [Permissions.ROLES.READ, Permissions.PRIVILEGES.READ],
      },
    },
  },
  {
    name: 'preview.admin.security.roles.list',
    url: '',
    component: RolesPage,
    data: {
      title: 'Roles',
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
      // NEXUS-54048: match Classic/Default UI (adminRoutes.js USERS.ROOT), which requires BOTH
      // users:read and roles:read (permissions[] is AND).
      visibilityRequirements: {
        permissions: [Permissions.USERS.READ, Permissions.ROLES.READ],
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
    data: {
      title: 'Anonymous Access',
      visibilityRequirements: {
        requiresPermission: Permissions.SETTINGS.READ,
      },
    },
  },
  {
    name: 'preview.admin.security.ldap',
    url: '/ldap',
    abstract: true,
    component: UIView,
    data: {
      title: 'LDAP',
      // NEXUS-54019: match Classic/Default UI (adminRoutes.js LDAP.ROOT) — gated on ldap:read only,
      // with NO edition restriction. Preview previously added editions[PRO,COMMUNITY], which hid
      // LDAP on editions where Classic still shows it.
      visibilityRequirements: {
        requiresPermission: Permissions.LDAP.READ,
      },
    },
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
    data: {
      title: 'Realms',
      visibilityRequirements: {
        requiresPermission: Permissions.SETTINGS.READ,
      },
    },
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
    data: { title: 'SSL Certificates' },
  },
  {
    name: 'preview.admin.security.usertoken',
    url: '/user-tokens',
    component: UserTokensPage,
    data: {
      title: 'User Tokens',
      // NEXUS-54019: match Classic/Default UI (adminRoutes.js USERTOKEN.ROOT) — usertoken-settings:read
      // gated to PRO edition AND a valid usertoken license. Without the licenseValid gate the tile
      // shows on PRO installs whose license does not include user tokens and then dead-ends.
      visibilityRequirements: {
        requiresPermission: Permissions.USER_TOKENS_SETTINGS.READ,
        editions: ['PRO'],
        licenseValid: [{ key: 'usertoken', defaultValue: false }],
      },
    },
  },
  {
    name: 'preview.admin.security.serviceaccounttokens',
    url: '/service-account-tokens',
    component: ServiceAccountTokensPage,
    data: {
      title: 'Service Account Tokens',
      visibilityRequirements: {
        requiresPermission: Permissions.SERVICE_ACCOUNTS.READ,
        editions: ['PRO'],
        statesEnabled: [{ key: 'serviceAccountEnabled', defaultValue: false }],
      },
    },
  },
  {
    name: 'preview.admin.security.saml',
    url: '/saml',
    component: SamlPage,
    data: {
      title: 'SAML',
      visibilityRequirements: {
        // NEXUS-54019: match Classic/Default UI (adminRoutes.js SAML.ROOT). SAML is admin-only
        // (nexus:*) AND only available with the saml bundle on PRO — without the bundle+editions
        // gates the tile shows where the feature does not exist and then dead-ends.
        bundle: 'nexus-saml-plugin',
        requiresPermission: Permissions.ADMIN,
        editions: ['PRO'],
      },
    },
  },
  {
    name: 'preview.admin.security.oauth2',
    url: '/oauth2',
    component: OAuth2Page,
    data: {
      title: 'OAuth 2.0',
      visibilityRequirements: {
        // NEXUS-54019: match Classic/Default UI (adminRoutes.js OAUTH2.ROOT). OAuth 2.0 is
        // admin-only (nexus:*) AND only available with the oauth2 bundle on PRO when the
        // oauth2Available state is set — without the bundle+editions+state gates the tile shows
        // where the feature does not exist and then dead-ends.
        bundle: 'nexus-oauth2-plugin',
        requiresPermission: Permissions.ADMIN,
        editions: ['PRO'],
        statesEnabled: [{ key: 'oauth2Available', defaultValue: false }],
      },
    },
  },
  {
    name: 'preview.admin.security.crowd',
    url: '/crowd',
    component: CrowdPage,
    data: {
      title: 'Atlassian Crowd',
      visibilityRequirements: {
        // NEXUS-54019: match Classic/Default UI (adminRoutes.js ATLASSIANCROWD.ROOT). Crowd is
        // only available with the crowd bundle and a valid crowd license — without the
        // bundle+licenseValid gates the tile shows where the feature does not exist and dead-ends.
        bundle: 'nexus-crowd-plugin',
        licenseValid: [{ key: 'crowd', defaultValue: false }],
        permissions: ['nexus:crowd:read'],
      },
    },
  },
  {
    name: 'preview.admin.security.ip-allowlist',
    url: '/ip-allowlist',
    component: IpAllowListPage,
    data: {
      title: 'IP Allow List',
      // NEXUS-54048: gate on nexus:* to match the backend. IpAllowListResource requires
      // @RequiresPermissions("nexus:*") on every endpoint (admin-only by design, NEXUS-45598).
      // A settings:read gate let non-admins open the page and then hit a 403.
      visibilityRequirements: {
        requiresPermission: Permissions.ADMIN,
        editions: ['PRO'],
      },
    },
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
    data: {
      title: 'Logs',
      visibilityRequirements: {
        requiresPermission: Permissions.LOGGING.READ,
      },
    },
  },
  {
    name: 'preview.admin.support.logs.list',
    url: '',
    component: LogsPage,
    data: {
      title: 'Logs',
    },
  },
  {
    name: 'preview.admin.support.logs.detail',
    url: '/*filename',
    component: LogsPage,
    params: {
      // raw: true preserves '/' in task log paths (e.g. tasks/foo.log)
      // without it UIRouter would treat slashes as segment separators
      filename: {value: null, raw: true, dynamic: true},
    },
    data: {
      title: 'Log Viewer',
    },
  },
  {
    name: 'preview.admin.support.logging',
    url: '/logging',
    abstract: true,
    component: UIView,
    data: {
      title: 'Logging',
      visibilityRequirements: {
        requiresPermission: Permissions.LOGGING.READ,
      },
    },
  },
  {
    name: 'preview.admin.support.logging.list',
    url: '',
    component: LoggingConfigPage,
    data: {
      title: 'Logging',
      visibilityRequirements: {
        requiresPermission: Permissions.LOGGING.READ,
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
        requiresPermission: Permissions.LOGGING.UPDATE,
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
        requiresPermission: Permissions.LOGGING.READ,
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
    name: 'preview.admin.support.recoverymode',
    url: '/recoverymode',
    component: RecoveryModePage,
    data: {
      title: 'Recovery Mode',
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
      },
    },
  },
  {
    name: 'preview.admin.support.supportrequest',
    url: '/supportrequest',
    component: SupportRequestPage,
    data: {
      title: 'Support Request',
      // NEXUS-54019: match Classic/Default UI (adminRoutes.js SUPPORTREQUEST.ROOT) — atlas:create
      // gated to PRO. Preview previously gated on atlas:read, which showed the tile to users who
      // lack the create permission the page actually requires.
      visibilityRequirements: {
        requiresPermission: Permissions.ATLAS.CREATE,
        editions: ['PRO'],
      },
    },
  },
  {
    name: 'preview.admin.support.supportzip',
    url: '/supportzip',
    component: SupportZipPage,
    data: {
      title: 'Support ZIP',
      // NEXUS-54019: match Classic/Default UI (adminRoutes.js SUPPORTZIP.ROOT) — atlas:read.
      // Preview previously gated on the non-existent nexus:supportzip:read privilege.
      visibilityRequirements: {
        requiresPermission: Permissions.ATLAS.READ,
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
    data: {
      title: 'Tasks',
      visibilityRequirements: {
        requiresPermission: Permissions.TASKS.READ,
      },
    },
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
    data: {
      title: 'Capabilities',
      visibilityRequirements: {
        requiresPermission: Permissions.CAPABILITIES.READ,
      },
    },
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
    data: {
      title: 'Email Server',
      visibilityRequirements: {
        requiresPermission: Permissions.SETTINGS.READ,
      },
    },
  },
  {
    name: 'preview.admin.system.http',
    url: '/http',
    component: HttpPage,
    data: {
      title: 'HTTP',
      visibilityRequirements: {
        requiresPermission: Permissions.SETTINGS.READ,
      },
    },
  },
  {
    name: 'preview.admin.system.licensing',
    url: '/licensing',
    component: LicensingPage,
    data: {
      title: 'Licensing',
      visibilityRequirements: {
        requiresPermission: Permissions.LICENSING.READ,
      },
    },
  },
  {
    name: 'preview.admin.system.nodes',
    url: '/nodes',
    component: NodesPage,
    data: {
      title: 'Nodes',
      visibilityRequirements: {
        // NEXUS-54048: match Classic/Default UI (adminRoutes.js NODES.ROOT), which gates on
        // nexus:* — the Nodes screen is an admin-only screen.
        requiresPermission: Permissions.ADMIN,
      },
    },
  },
  {
    name: 'preview.admin.system.upgrade',
    url: '/upgrade',
    component: UpgradePage,
    data: {
      title: 'Upgrade',
      // NEXUS-54237: this page is informational only — current version, edition badge, license
      // validity, and documentation links. It has no relationship to the Nexus 2→3 migration
      // wizard. NEXUS-54019 aligned it to migration:read + the 'migration' capability to mirror
      // that wizard, but the wizard and its capability descriptor are being removed, which makes
      // the capability gate permanently unsatisfiable and the page unreachable. Back to the
      // baseline Settings gate, which is what this route used before the alignment.
      visibilityRequirements: {
        requiresPermission: Permissions.SETTINGS.READ,
      },
    },
  },
  {
    name: 'preview.admin.system.usage',
    url: '/usage',
    component: UsagePage,
    data: {
      title: 'Usage',
      // NEXUS-54048: Usage is a cloud-oriented feature — its Settings Hub card is cloudOnly, so
      // this route is NOT surfaced in the self-hosted Settings menu (the card is filtered out).
      // The gate here is therefore dormant on self-hosted. The value differs from the cloud route
      // (settings:read) and the shared card (settings:read); note the underlying monthly-/daily-
      // metrics APIs actually require nexus:*, so none of these frontend gates is backend-exact.
      // Left as METRICS.READ (no behaviour change) since the route is not user-reachable here;
      // realigning the metrics gates to nexus:* is tracked separately, out of this ticket's scope.
      visibilityRequirements: {
        permissions: [Permissions.METRICS.READ],
      },
    },
  },

  // =============================================================================
  // IQ SERVER (top-level admin route, not nested under system)
  // =============================================================================
  // Root /iq entry — disconnected entry point (overview).
  // Fetches current IQ config; redirects to iqConnected when already enabled.
  {
    name: 'preview.admin.iq',
    url: '/iq-overview',
    component: IqServerOverviewPage,
    data: {
      title: 'IQ Server',
      visibilityRequirements: {
        requiresPermission: 'nexus:settings:read',
      },
    },
  },

  // IQ Server connected view with Lifecycle + Firewall cards.
  // Redirects to iqOverview if IQ is disabled after load.
  {
    name: 'preview.admin.iqConnected',
    url: '/iq/connected',
    component: IqServerConnectedPage,
    data: {
      title: 'IQ Server',
      visibilityRequirements: {
        requiresPermission: 'nexus:settings:read',
      },
    },
  },

  // Connection settings route — distinct lazy-load wrapper so
  // React unmounts/remounts on every transition, keeping loadData() fresh.
  {
    name: 'preview.admin.iqConnection',
    url: '/iq/connection',
    component: IqServerConnectionPage,
    data: {
      title: 'IQ Server Connection Settings',
      visibilityRequirements: {
        requiresPermission: 'nexus:settings:read',
      },
    },
  },

  // Screen 3 — Hosted Repository Evaluation workflow (Repositories + Settings tabs).
  {
    name: 'preview.admin.iqHostedReposEval',
    url: '/iq/hosted-repos-eval?tab',
    component: HostedRepoEvaluationSetupPage,
    data: {
      title: 'Hosted Repository Evaluation',
      visibilityRequirements: {
        requiresPermission: 'nexus:settings:read',
        statesEnabled: [
          { key: 'hostedRepositoryEvaluationEnabled', defaultValue: false },
        ],
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
