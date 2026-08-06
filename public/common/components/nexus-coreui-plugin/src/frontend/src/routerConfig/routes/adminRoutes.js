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
import { UIView } from '@uirouter/react';
import { Permissions, CleanupPoliciesList, CleanupPoliciesForm, CapabilitiesList, CapabilitiesCreate, CapabilitiesEdit } from '@sonatype/nexus-ui-plugin';
import { ROUTE_NAMES } from '../routeNames/routeNames';
import { lazyLoad } from './lazyLoad';

// Lazy load all route components for better code splitting
const IqServer = lazyLoad(() => import('../../components/pages/admin/IqServer/IqServer'));
const IqServerConnected = lazyLoad(() => import('../../components/pages/admin/IqServer/IqServerConnected'));
const SonatypeLifecycle = lazyLoad(() => import('../../components/pages/admin/IqServer/SonatypeLifecycle'));
const HostedRepositoriesEvaluation = lazyLoad(() => import('../../components/pages/admin/HostedRepositoriesEvaluation/HostedRepositoriesEvaluation'));
const HostedRepositoryEvaluationGroup = lazyLoad(() => import('../../components/pages/admin/IqServer/HostedRepositoriesEvaluation'));
const RepositoriesExt = lazyLoad(() => import('../../components/pages/admin/Repositories/RepositoriesExt'));
const DataStoreConfiguration = lazyLoad(() => import('../../components/pages/admin/DataStoreConfiguration/DataStoreConfiguration'));
const ProprietaryRepositories = lazyLoad(() => import('../../components/pages/admin/ProprietaryRepositories/ProprietaryRepositories'));
const EmailServer = lazyLoad(() => import('../../components/pages/admin/EmailServer/EmailServer'));
const SslCertificatesList = lazyLoad(() => import('../../components/pages/admin/SslCertificates/SslCertificatesList'));
const SslCertificatesAddForm = lazyLoad(() => import('../../components/pages/admin/SslCertificates/SslCertificatesAddForm'));
const SslCertificatesDetailsForm = lazyLoad(() => import('../../components/pages/admin/SslCertificates/SslCertificatesDetailsForm'));
const UsersExt = lazyLoad(() => import('../../components/pages/admin/Users/UsersExt'));
const AnonymousSettings = lazyLoad(() => import('../../components/pages/admin/AnonymousSettings/AnonymousSettings'));
const Realms = lazyLoad(() => import('../../components/pages/admin/Realms/Realms'));
const UserTokens = lazyLoad(() => import('../../components/pages/admin/UserTokens/UserTokens'));
const ServiceAccountTokens = lazyLoad(() => import('../../components/pages/admin/ServiceAccountTokens/ServiceAccountTokens'));
const CrowdSettings = lazyLoad(() => import('../../components/pages/admin/CrowdSettings/CrowdSettings'));
const SamlConfiguration = lazyLoad(() => import('../../components/pages/admin/SamlConfiguration/SamlConfiguration'));
const OAuth2Configuration = lazyLoad(() => import('../../components/pages/admin/OAuth2Configuration/OAuth2Configuration'));
const SupportRequest = lazyLoad(() => import('../../components/pages/admin/SupportRequest/SupportRequest'));
const SystemInformation = lazyLoad(() => import('../../components/pages/admin/SystemInformation/SystemInformation'));
const MetricHealth = lazyLoad(() => import('../../components/pages/admin/MetricHealth/MetricHealth'));
const SupportZip = lazyLoad(() => import('../../components/pages/admin/SupportZip/SupportZip'));
const Logs = lazyLoad(() => import('../../components/pages/admin/Logs/Logs'));
const RecoveryMode = lazyLoad(() => import('../../components/pages/admin/RecoveryMode/RecoveryMode'));
const Api = lazyLoad(() => import('../../components/pages/admin/Api/Api'));
const HTTP = lazyLoad(() => import('../../components/pages/admin/Http/Http'));
const Licensing = lazyLoad(() => import('../../components/pages/admin/Licensing/Licensing'));
const Upgrade = lazyLoad(() => import('../../components/pages/admin/Upgrade/Upgrade'));

const PreviewUiSettings = lazyLoad(() => import('@sonatype/nexus-ui-plugin').then(m => ({ default: m.PreviewUiSettings })));
const NodesExt = lazyLoad(() => import('../../components/pages/admin/Nodes/NodesExt'));
const TasksExtJSWrapper = lazyLoad(() => import('../../components/pages/admin/Tasks/TasksExtJSWrapper'));
const Capabilities = lazyLoad(() => import('../../components/pages/admin/Capabilities/Capabilities'));
const BlobStoresList = lazyLoad(() => import('../../components/pages/admin/BlobStores/BlobStoresList'));
const BlobStoresForm = lazyLoad(() => import('../../components/pages/admin/BlobStores/BlobStoresForm'));
const LdapServersExt = lazyLoad(() => import('../../components/pages/admin/LdapServers/LdapServersExt'));
const AdminRepositoriesDirectoryPage = lazyLoad(() => import('../../components/pages/AdminRepositories/AdminRepositoriesDirectoryPage'));
const AdminSecurityDirectoryPage = lazyLoad(() => import('../../components/pages/AdminSecurity/AdminSecurityDirectoryPage'));
const AdminSystemDirectoryPage = lazyLoad(() => import('../../components/pages/AdminSystem/AdminSystemDirectoryPage'));
const AdminSupportDirectoryPage = lazyLoad(() => import('../../components/pages/AdminSupport/AdminSupportDirectoryPage'));
const SettingsHubClassicPage = lazyLoad(() => import('../../components/pages/admin/SettingsHub/SettingsHubClassicPage'));
const SettingsPageLayout = lazyLoad(() => import('../../components/LeftNavigationMenu/SettingsPageLayout/SettingsPageLayout'));
const PrivilegesList = lazyLoad(() => import('../../components/pages/admin/Privileges/PrivilegesList'));
const PrivilegesDetails = lazyLoad(() => import('../../components/pages/admin/Privileges/PrivilegesDetails'));
const ContentSelectorsList = lazyLoad(() => import('../../components/pages/admin/ContentSelectors/ContentSelectorsList'));
const ContentSelectorsDetails = lazyLoad(() => import('../../components/pages/admin/ContentSelectors/ContentSelectorsDetails'));
const RolesList = lazyLoad(() => import('../../components/pages/admin/Roles/RolesList'));
const RolesDetails = lazyLoad(() => import('../../components/pages/admin/Roles/RolesDetails'));
const RoutingRulesForm = lazyLoad(() => import('../../components/pages/admin/RoutingRules/RoutingRulesForm'));
const RoutingRulesList = lazyLoad(() => import('../../components/pages/admin/RoutingRules/RoutingRulesList'));
const RoutingRulesGlobalPreview = lazyLoad(() => import('../../components/pages/admin/RoutingRules/RoutingRulesGlobalPreview.jsx'));
const LoggingConfigurationList = lazyLoad(() => import('../../components/pages/admin/LoggingConfiguration/LoggingConfigurationList.jsx'));
const LoggingConfigurationForm = lazyLoad(() => import('../../components/pages/admin/LoggingConfiguration/LoggingConfigurationForm.jsx'));

const ADMIN = ROUTE_NAMES.ADMIN;

// for more info on how to define routes see private/developer-documentation/frontend/client-side-routing.md
export const adminRoutes = [
  {
    name: ADMIN.DIRECTORY,
    url: 'admin',
    component: SettingsPageLayout,
    abstract: true,
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
          Permissions.MIGRATION.READ,
        ],
      },
    },
  },

  {
    name: ADMIN.DIRECTORY + '.hub',
    url: '',
    component: SettingsHubClassicPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
      },
      title: 'Settings',
    },
  },

  // === admin/repository ===
  {
    name: ADMIN.REPOSITORY.DIRECTORY,
    url: '/repository',
    component: AdminRepositoriesDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true,
      },
      title: ADMIN.REPOSITORY.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.REPOSITORIES.ROOT,
    url: '/repositories:itemId',
    component: RepositoriesExt,
    data: {
      visibilityRequirements: {
        permissionPrefix: 'nexus:repository-admin',
      },
      title: ADMIN.REPOSITORY.REPOSITORIES.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.BLOB_STORES.READ],
      },
      title: ADMIN.REPOSITORY.BLOBSTORES.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.LIST,
    url: '/blobstores',
    component: BlobStoresList,
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.CREATE,
    url: '/blobstores/create',
    component: BlobStoresForm,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.BLOB_STORES.CREATE],
      },
    },
  },

  {
    name: ADMIN.REPOSITORY.BLOBSTORES.EDIT,
    url: '/blobstores/edit/{type:.+}/{name:.+}',
    component: BlobStoresForm,
  },

  {
    name: ADMIN.REPOSITORY.DATASTORE.ROOT,
    url: '/datastore',
    component: DataStoreConfiguration,
    data: {
      visibilityRequirements: {
        bundle: 'nexus-pro-datastore-plugin',
        permissions: ['nexus:*'],
        editions: ['PRO', 'COMMUNITY'],
      },
      title: ADMIN.REPOSITORY.DATASTORE.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.PROPRIETARY.ROOT,
    url: '/proprietary',
    component: ProprietaryRepositories,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.REPOSITORY.PROPRIETARY.TITLE,
    },
  },

  // abstract parent selectors state
  {
    name: ADMIN.REPOSITORY.SELECTORS.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SELECTORS.READ],
      },
      title: ADMIN.REPOSITORY.SELECTORS.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.SELECTORS.LIST,
    url: '/selectors',
    component: ContentSelectorsList,
  },

  {
    name: ADMIN.REPOSITORY.SELECTORS.EDIT,
    url: '/selectors/edit/{itemId:.+}',
    component: ContentSelectorsDetails,
  },

  {
    name: ADMIN.REPOSITORY.SELECTORS.CREATE,
    url: '/selectors/create',
    component: ContentSelectorsDetails,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SELECTORS.CREATE],
      },
      title: ADMIN.REPOSITORY.SELECTORS.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.CLEANUPPOLICIES.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
      },
      title: ADMIN.REPOSITORY.CLEANUPPOLICIES.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.CLEANUPPOLICIES.LIST,
    url: '/cleanuppolicies',
    component: CleanupPoliciesList,
  },

  {
    name: ADMIN.REPOSITORY.CLEANUPPOLICIES.CREATE,
    url: '/cleanuppolicies/create',
    component: CleanupPoliciesForm,
  },

  {
    name: ADMIN.REPOSITORY.CLEANUPPOLICIES.EDIT,
    url: '/cleanuppolicies/edit/{itemId:.+}',
    component: CleanupPoliciesForm,
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.ROOT,
    abstract: true,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
      },
      title: ADMIN.REPOSITORY.ROUTINGRULES.TITLE,
    },
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.LIST,
    url: '/routingrules',
    component: RoutingRulesList,
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.CREATE,
    url: '/routingrules/create',
    component: RoutingRulesForm,
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.EDIT,
    url: '/routingrules/edit/{itemId:.+}',
    component: RoutingRulesForm,
  },

  {
    name: ADMIN.REPOSITORY.ROUTINGRULES.PREVIEW,
    url: '/routingrules/preview',
    component: RoutingRulesGlobalPreview,
  },

  // === admin/security ===
  {
    name: ADMIN.SECURITY.DIRECTORY,
    url: '/security',
    component: AdminSecurityDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true,
      },
      title: ADMIN.SECURITY.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.PRIVILEGES.ROOT,
    abstract: true,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.PRIVILEGES.READ],
      },
      title: ADMIN.SECURITY.PRIVILEGES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.PRIVILEGES.LIST,
    url: '/privileges',
    component: PrivilegesList,
  },

  {
    name: ADMIN.SECURITY.PRIVILEGES.EDIT,
    url: '/privileges/edit/{itemId:.+}',
    component: PrivilegesDetails,
  },

  {
    name: ADMIN.SECURITY.PRIVILEGES.CREATE,
    url: '/privileges/create',
    component: PrivilegesDetails,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.PRIVILEGES.CREATE],
      },
      title: ADMIN.SECURITY.PRIVILEGES.TITLE,
    },
  },

  // abstract parent roles state
  {
    name: ADMIN.SECURITY.ROLES.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ROLES.READ, Permissions.PRIVILEGES.READ],
      },
      title: ADMIN.SECURITY.ROLES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.ROLES.LIST,
    url: '/roles',
    component: RolesList,
  },

  {
    name: ADMIN.SECURITY.ROLES.EDIT,
    url: '/roles/edit/{itemId:.+}',
    component: RolesDetails,
  },

  {
    name: ADMIN.SECURITY.ROLES.CREATE,
    url: '/roles/create',
    component: RolesDetails,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ROLES.READ, Permissions.PRIVILEGES.READ],
      },
      title: ADMIN.SECURITY.ROLES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.SSLCERTIFICATES.ROOT,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SSL_TRUSTSTORE.READ],
      },
      title: ADMIN.SECURITY.SSLCERTIFICATES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.SSLCERTIFICATES.LIST,
    url: '/sslcertificates',
    component: SslCertificatesList,
  },

  {
    name: ADMIN.SECURITY.SSLCERTIFICATES.CREATE,
    url: '/sslcertificates/create',
    component: SslCertificatesAddForm,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SSL_TRUSTSTORE.CREATE],
      },
      title: ADMIN.SECURITY.SSLCERTIFICATES.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.SSLCERTIFICATES.EDIT,
    url: '/sslcertificates/edit/{itemId:.+}',
    component: SslCertificatesDetailsForm,
  },

  {
    name: ADMIN.SECURITY.LDAP.ROOT,
    url: '/ldap:itemId',
    component: LdapServersExt,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LDAP.READ],
      },
      title: ADMIN.SECURITY.LDAP.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.SECURITY.USERS.ROOT,
    url: '/users:itemId',
    component: UsersExt,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.USERS.READ, Permissions.ROLES.READ],
      },
      title: ADMIN.SECURITY.USERS.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.SECURITY.ANONYMOUS.ROOT,
    url: '/anonymous',
    component: AnonymousSettings,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SECURITY.ANONYMOUS.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.REALMS.ROOT,
    url: '/realms',
    component: Realms,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SECURITY.REALMS.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.USERTOKEN.ROOT,
    url: '/usertoken',
    component: UserTokens,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.USER_TOKENS_SETTINGS.READ],
        editions: ['PRO'],
        licenseValid: [
          {
            key: 'usertoken',
            defaultValue: false,
          },
        ],
      },
      title: ADMIN.SECURITY.USERTOKEN.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.SERVICE_ACCOUNT_TOKENS.ROOT,
    url: '/satokens',
    component: ServiceAccountTokens,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        permissions: [Permissions.SERVICE_ACCOUNTS.READ],
        // Cloud uses Preview UI; classic UI is PRO-only. Backend allows PRO+CLOUD via @ConditionalOnEdition.
        editions: ['PRO'],
        statesEnabled: [
          {
            key: 'serviceAccountEnabled',
            defaultValue: false,
          },
        ],
      },
      title: ADMIN.SECURITY.SERVICE_ACCOUNT_TOKENS.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.ATLASSIANCROWD.ROOT,
    url: '/atlassiancrowd',
    component: CrowdSettings,
    data: {
      visibilityRequirements: {
        bundle: 'nexus-crowd-plugin',
        licenseValid: [
          {
            key: 'crowd',
            defaultValue: false,
          },
        ],
        permissions: ['nexus:crowd:read'],
      },
      title: ADMIN.SECURITY.ATLASSIANCROWD.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.SAML.ROOT,
    url: '/saml',
    component: SamlConfiguration,
    data: {
      visibilityRequirements: {
        bundle: 'nexus-saml-plugin',
        permissions: ['nexus:*'],
        editions: ['PRO'],
      },
      title: ADMIN.SECURITY.SAML.TITLE,
    },
  },

  {
    name: ADMIN.SECURITY.OAUTH2.ROOT,
    url: '/oauth2',
    component: OAuth2Configuration,
    data: {
      visibilityRequirements: {
        bundle: 'nexus-oauth2-plugin',
        permissions: ['nexus:*'],
        editions: ['PRO'],
        statesEnabled: [
          {
            key: 'oauth2Available',
            defaultValue: false,
          },
        ],
      },
      title: ADMIN.SECURITY.OAUTH2.TITLE,
    },
  },

  // === admin/support ===
  {
    name: ADMIN.SUPPORT.DIRECTORY,
    url: '/support',
    component: AdminSupportDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true,
      },
      title: ADMIN.SUPPORT.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.SUPPORTREQUEST.ROOT,
    url: '/supportrequest',
    component: SupportRequest,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ATLAS.CREATE],
        editions: ['PRO'],
      },
      title: ADMIN.SUPPORT.SUPPORTREQUEST.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.SYSTEMINFORMATION.ROOT,
    url: '/systeminformation',
    component: SystemInformation,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ATLAS.READ],
      },
      title: ADMIN.SUPPORT.SYSTEMINFORMATION.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.STATUS.ROOT,
    url: '/status:itemId',
    component: MetricHealth,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.METRICS.READ],
      },
      title: ADMIN.SUPPORT.STATUS.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.SUPPORT.SUPPORTZIP.ROOT,
    url: '/supportzip',
    component: SupportZip,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ATLAS.READ],
      },
      title: ADMIN.SUPPORT.SUPPORTZIP.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.LOGS.ROOT,
    url: '/logs:itemId',
    component: Logs,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LOGGING.READ],
        notClustered: true,
      },
      title: ADMIN.SUPPORT.LOGS.TITLE,
    },
    params: {
      itemId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
  },

  {
    name: ADMIN.SUPPORT.RECOVERY.ROOT,
    url: '/recovery',
    component: RecoveryMode,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
      },
      title: ADMIN.SUPPORT.RECOVERY.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.LOGGING.ROOT,
    abstract: true,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LOGGING.READ],
      },
      title: ADMIN.SUPPORT.LOGGING.TITLE,
    },
  },

  {
    name: ADMIN.SUPPORT.LOGGING.LIST,
    url: '/logging',
    component: LoggingConfigurationList,
  },

  {
    name: ADMIN.SUPPORT.LOGGING.EDIT,
    url: '/logging/edit/{itemId:.+}',
    component: LoggingConfigurationForm,
  },

  {
    name: ADMIN.SUPPORT.LOGGING.CREATE,
    url: '/logging/create',
    component: LoggingConfigurationForm,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LOGGING.UPDATE],
      },
      title: ADMIN.SUPPORT.LOGGING.TITLE,
    },
  },

  // === admin/system ===
  {
    name: ADMIN.SYSTEM.DIRECTORY,
    url: '/system',
    component: AdminSystemDirectoryPage,
    data: {
      visibilityRequirements: {
        requiresUser: true,
        ignoreForMenuVisibilityCheck: true,
      },
      title: ADMIN.SYSTEM.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.TASKS.ROOT,
    url: '/tasks:taskId',
    component: TasksExtJSWrapper,
    params: {
      taskId: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
    data: {
      visibilityRequirements: {
        permissions: [Permissions.TASKS.READ],
      },
      title: ADMIN.SYSTEM.TASKS.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.API.ROOT,
    url: '/api',
    component: Api,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SYSTEM.API.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.CAPABILITIES_EXTJS.ROOT,
    url: '/capabilities-extjs:id',
    component: Capabilities,
    params: {
      id: {
        value: null,
        raw: true,
        dynamic: true,
      },
    },
    data: {
      visibilityRequirements: {
        permissions: ['nexus:capabilities:read'],
      },
      title: ADMIN.SYSTEM.CAPABILITIES_EXTJS.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.CAPABILITIES.ROOT,
    abstract: true,
    component: UIView,
    data: {
      visibilityRequirements: {
        permissions: ['nexus:capabilities:read'],
      },
      title: ADMIN.SYSTEM.CAPABILITIES.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.CAPABILITIES.CREATE,
    url: '/capabilities/create',
    component: CapabilitiesCreate,
  },

  {
    name: ADMIN.SYSTEM.CAPABILITIES.EDIT,
    url: '/capabilities/edit/{id}',
    component: CapabilitiesEdit,
  },

  {
    name: ADMIN.SYSTEM.CAPABILITIES.LIST,
    url: '/capabilities',
    component: CapabilitiesList,
  },

  {
    name: ADMIN.SYSTEM.EMAILSERVER.ROOT,
    url: '/emailserver',
    component: EmailServer,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SYSTEM.EMAILSERVER.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.HTTP.ROOT,
    url: '/http',
    component: HTTP,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SYSTEM.HTTP.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.LICENSING.ROOT,
    url: '/licensing',
    component: Licensing,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.LICENSING.READ],
      },
      title: ADMIN.SYSTEM.LICENSING.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.NODES.ROOT,
    url: '/nodes',
    component: NodesExt,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.ADMIN],
      },
      title: ADMIN.SYSTEM.NODES.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.UPGRADE.ROOT,
    url: '/upgrade',
    component: Upgrade,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.MIGRATION.READ],
        capability: 'migration',
      },
      title: ADMIN.SYSTEM.UPGRADE.TITLE,
    },
  },

  {
    name: ADMIN.SYSTEM.PREVIEWUI.ROOT,
    url: '/previewui',
    component: PreviewUiSettings,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.SYSTEM.PREVIEWUI.TITLE,
    },
  },

  // === iq ===
  {
    name: ADMIN.IQ.ROOT,
    url: '/iq?fromConnected',
    component: IqServer,
    params: {
      fromConnected: {
        value: null,
        squash: true,
      },
    },
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
      },
      title: ADMIN.IQ.TITLE,
    },
  },

  {
    name: ADMIN.IQ.CONNECTED,
    url: '/iq/connected',
    component: IqServerConnected,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
        statesEnabled: [
          {
            key: 'hostedRepositoryEvaluationEnabled',
            defaultValue: false,
          },
        ],
      },
      title: ADMIN.IQ.TITLE,
    },
  },

  {
    name: ADMIN.IQ.SONATYPE_LIFECYCLE.ROOT,
    url: '/iq/sonatype-lifecycle',
    component: SonatypeLifecycle,
    data: {
      visibilityRequirements: {
        permissions: [Permissions.SETTINGS.READ],
        statesEnabled: [
          {
            key: 'hostedRepositoryEvaluationEnabled',
            defaultValue: true,
          },
        ],
      },
      title: ADMIN.IQ.SONATYPE_LIFECYCLE.TITLE,
    },
  },

  {
    name: ADMIN.IQ.HOSTED_REPOS_EVAL.ROOT,
    url: '/iq/sonatype-lifecycle/hosted-repos-eval?activeTab',
    component: HostedRepositoryEvaluationGroup,
    params: {
      activeTab: {
        value: null,
        squash: true,
        dynamic: true,
      },
    },
    data: {
      visibilityRequirements: {
        // Reached via cross-app deep-link from IQ on a fresh page load; the async permissions
        // fetch (NEXUS-52583) is often still in-flight when the route check runs, so a
        // permissions:[…] gate here would redirect to welcome before the real permission set
        // arrives. Gate on requiresUser instead — server APIs and inner components still
        // enforce nexus:settings:read, so non-admins get an empty page rather than a deep
        // link that silently bounces.
        requiresUser: true,
        statesEnabled: [
          {
            key: 'hostedRepositoryEvaluationEnabled',
            defaultValue: false,
          },
        ],
      },
      title: ADMIN.IQ.HOSTED_REPOS_EVAL.TITLE,
    },
  },
];
