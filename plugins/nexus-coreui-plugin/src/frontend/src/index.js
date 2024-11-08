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
import {Permissions} from '@sonatype/nexus-ui-plugin';
import ContentSelectors from './components/pages/admin/ContentSelectors/ContentSelectors';
import EmailServer from './components/pages/admin/EmailServer/EmailServer';
import Privileges from './components/pages/admin/Privileges/Privileges';
import Roles from './components/pages/admin/Roles/Roles';
import Users from './components/pages/admin/Users/Users';
import SslCertificates from './components/pages/admin/SslCertificates/SslCertificates';
import LdapServers from './components/pages/admin/LdapServers/LdapServers';
import Tasks from './components/pages/admin/Tasks/Tasks';
import AnonymousSettings from './components/pages/admin/AnonymousSettings/AnonymousSettings';
import BlobStores from './components/pages/admin/BlobStores/BlobStores';
import InsightFrontend from './components/pages/admin/InsightFrontend/InsightFrontend';
import LoggingConfiguration from './components/pages/admin/LoggingConfiguration/LoggingConfiguration';
import Logs from "./components/pages/admin/Logs/Logs";
import Repositories from './components/pages/admin/Repositories/Repositories';
import RoutingRules from './components/pages/admin/RoutingRules/RoutingRules';
import SystemInformation from './components/pages/admin/SystemInformation/SystemInformation';
import SupportRequest from './components/pages/admin/SupportRequest/SupportRequest';
import MetricHealth from './components/pages/admin/MetricHealth/MetricHealth';
import SupportZip from './components/pages/admin/SupportZip/SupportZip';
import CleanupPolicies from './components/pages/admin/CleanupPolicies/CleanupPolicies';
import UIStrings from './constants/UIStrings';
import UserAccount from './components/pages/admin/UserAccount/UserAccount';
import NuGetApiToken from './components/pages/user/NuGetApiToken/NuGetApiToken';
import AnalyzeApplication from './components/pages/user/AnalyzeApplication/AnalyzeApplication';
import S3BlobStoreSettings from './components/pages/admin/BlobStores/S3/S3BlobStoreSettings';
import S3BlobStoreWarning from './components/pages/admin/BlobStores/S3/S3BlobStoreWarning';
import S3BlobStoreActions from './components/pages/admin/BlobStores/S3/S3BlobStoreActions';
import AzureBlobStoreSettings from './components/pages/admin/BlobStores/Azure/AzureBlobStoreSettings';
import AzureBlobStoreActions from './components/pages/admin/BlobStores/Azure/AzureBlobStoreActions';
import GoogleBlobStoreSettings from './components/pages/admin/BlobStores/Google/GoogleBlobStoreSettings';
import GoogleBlobStoreActions from './components/pages/admin/BlobStores/Google/GoogleBlobStoreActions';
import IqServer from './components/pages/admin/IqServer/IqServer';
import Bundles from './components/pages/admin/Bundles/Bundles';
import ProprietaryRepositories from './components/pages/admin/ProprietaryRepositories/ProprietaryRepositories';
import Api from './components/pages/admin/Api/Api';
import Realms from './components/pages/admin/Realms/Realms';
import HTTP from './components/pages/admin/Http/Http';
import Licensing from './components/pages/admin/Licensing/Licensing';
import UserTokens from './components/pages/admin/UserTokens/UserTokens';
import CrowdSettings from "./components/pages/admin/CrowdSettings/CrowdSettings";
import SamlConfiguration from "./components/pages/admin/SamlConfiguration/SamlConfiguration";
import Replication from "./components/pages/admin/Replication/Replication";
import DataStoreConfiguration from "./components/pages/admin/DataStoreConfiguration/DataStoreConfiguration";
import UserToken from "./components/pages/user/UserToken/UserToken";
import Welcome from './components/pages/user/Welcome/Welcome';
import Tags from './components/pages/browse/Tags/Tags';
import Upload from './components/pages/browse/Upload/Upload';
import Nodes from "./components/pages/admin/Nodes/NodeList";
import Browse from './components/pages/browse/Browse/Browse';
import UpgradeAlert from './components/UpgradeAlert/UpgradeAlert';
import UsageMetricsAlert from './components/pages/user/Welcome/UsageMetricsAlert';
import UpgradeModal from './components/pages/user/Welcome/UpgradeModal';
import MaliciousRisk from "./components/pages/maliciousrisk/MaliciousRisk";
import MaliciousRiskOnDisk from "./components/pages/maliciousrisk/riskondisk/MaliciousRiskOnDisk";
import FeatureFlags from './constants/FeatureFlags';

const {MALWARE_RISK_ENABLED} = FeatureFlags;

window.ReactComponents = {
  ...window.ReactComponents,
  AnalyzeApplication,
  UpgradeAlert,
  UsageMetricsAlert,
  UpgradeModal,
  MaliciousRiskOnDisk
};

window.BlobStoreTypes = {
  ...window.BlobStoreTypes,
  azure: {
    Settings: AzureBlobStoreSettings,
    Actions: AzureBlobStoreActions
  },
  s3: {
    Settings: S3BlobStoreSettings,
    Warning: S3BlobStoreWarning,
    Actions: S3BlobStoreActions
  },
  google: {
    Settings: GoogleBlobStoreSettings,
    Actions: GoogleBlobStoreActions
  }
}

window.plugins.push({
  id: 'nexus-coreui-plugin',

  features: [
    {
      mode: 'browse',
      path: '/Welcome',
      view: Welcome,
      ...UIStrings.WELCOME.MENU,
      iconConfig: {
        file: 'sonatype.png',
        variants: ['x16', 'x32']
      },
      weight: 10,
      authenticationRequired: false,
      visibility: {
        featureFlags: [{key: 'nexus.react.welcome', defaultValue: false}]
      },
    },
    {
      mode: 'admin',
      path: '/Repository/Blobstores',
      ...UIStrings.BLOB_STORES.MENU,
      view: BlobStores,
      iconCls: 'x-fa fa-hdd',
      visibility: {
        permissions: [Permissions.BLOB_STORES.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Repository/Repositories',
      ...UIStrings.REPOSITORIES.MENU,
      view: Repositories,
      iconCls: 'x-fa fa-database',
      weight: 10,
      visibility: {
        featureFlags: [{key: 'nexus.react.repositories', defaultValue: false}],
        permissions: ['nexus:repository-admin:*:*:read']
      },
    },
    {
      mode: 'admin',
      path: '/Repository/Selectors',
      ...UIStrings.CONTENT_SELECTORS.MENU,
      view: ContentSelectors,
      iconCls: 'x-fa fa-layer-group',
      weight: 300,
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.SELECTORS.READ]
      },
    },
    {
      mode: 'admin',
      ...UIStrings.ROUTING_RULES.MENU,
      path: '/Repository/RoutingRules',
      view: RoutingRules,
      iconCls: 'x-fa fa-map-signs',
      weight: 500,
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ADMIN]
      },
    },
    {
      mode: 'admin',
      path: '/Security/Privileges',
      ...UIStrings.PRIVILEGES.MENU,
      view: Privileges,
      iconCls: 'x-fa fa-id-badge',
      weight: 10,
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [{key: 'nexus.react.privileges', defaultValue: true}],
        permissions: [Permissions.PRIVILEGES.READ]
      },
    },
    {
      mode: 'admin',
      path: '/System/EmailServer',
      ...UIStrings.EMAIL_SERVER.MENU,
      view: EmailServer,
      iconCls: 'x-fa fa-envelope',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [{key: 'nexus.react.emailServer', defaultValue: true}],
        permissions: [Permissions.SETTINGS.READ]
      },
    },
    {
      mode: 'admin',
      path: '/Security/Roles',
      ...UIStrings.ROLES.MENU,
      view: Roles,
      iconCls: 'x-fa fa-user-tag',
      weight: 20,
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:roles:read', 'nexus:privileges:read']
      }
    },
    {
      mode: 'admin',
      path: '/Security/SslCertificates',
      ...UIStrings.SSL_CERTIFICATES.MENU,
      view: SslCertificates,
      iconCls: 'x-fa fa-id-card-alt',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [{key: 'nexus.react.sslCertificates', defaultValue: true}],
        permissions: [Permissions.SSL_TRUSTSTORE.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Security/LDAP',
      ...UIStrings.LDAP_SERVERS.MENU,
      view: LdapServers,
      iconCls: 'x-fa fa-book',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [{key: 'nexus.react.ldap', defaultValue: false}],
        permissions: [Permissions.LDAP.READ]
      }
    },
    {
      mode: 'admin',
      path: '/System/Tasks',
      ...UIStrings.TASKS.MENU,
      view: Tasks,
      iconCls: 'x-fa fa-clock',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [{key: 'nexus.react.tasks', defaultValue: false}],
        permissions: [Permissions.TASKS.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Security/Users',
      ...UIStrings.USERS.MENU,
      view: Users,
      iconCls: 'x-fa fa-users',
      weight: 30,
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [{key: 'nexus.react.users', defaultValue: false}],
        permissions: [Permissions.USERS.READ, Permissions.ROLES.READ]
      },
    },
    {
      mode: 'admin',
      path: '/Security/Anonymous',
      ...UIStrings.ANONYMOUS_SETTINGS.MENU,
      view: AnonymousSettings,
      iconCls: 'x-fa fa-user',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.SETTINGS.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Security/realms',
      ...UIStrings.REALMS.MENU,
      view: Realms,
      iconCls: 'x-fa fa-dungeon',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.SETTINGS.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Support/Logging',
      ...UIStrings.LOGGING.MENU,
      view: LoggingConfiguration,
      iconCls: 'x-fa fa-scroll',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.LOGGING.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Support/SupportRequest',
      ...UIStrings.SUPPORT_REQUEST.MENU,
      view: SupportRequest,
      iconCls: 'x-fa fa-user-circle',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ATLAS.CREATE],
        editions: ['PRO']
      }
    },
    {
      mode: 'admin',
      path: '/Support/SystemInformation',
      ...UIStrings.SYSTEM_INFORMATION.MENU,
      view: SystemInformation,
      iconCls: 'x-fa fa-info',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ATLAS.READ]
      }
    },
    {
      mode: 'user',
      path: '/Account',
      ...UIStrings.USER_ACCOUNT.MENU,
      view: UserAccount,
      iconCls: 'x-fa fa-user',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        requiresUser: true,
      }
    },
    {
      mode: 'user',
      path: '/NuGetApiToken',
      ...UIStrings.NUGET_API_KEY.MENU,
      view: NuGetApiToken,
      iconCls: 'x-fa fa-key',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        requiresUser: true,
      }
    },
    {
      mode: 'admin',
      path: '/Support/Status',
      ...UIStrings.METRIC_HEALTH.MENU,
      view: MetricHealth,
      iconCls: 'x-fa fa-medkit',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.METRICS.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Support/SupportZip',
      ...UIStrings.SUPPORT_ZIP.MENU,
      view: SupportZip,
      iconCls: 'x-fa fa-file-archive',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.METRICS.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Support/Logs',
      ...UIStrings.LOGS.MENU,
      view: Logs,
      iconCls: 'x-fa fa-terminal',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.LOGGING.READ],
        featureFlags: [{
          key: 'log.viewer.enabled',
          defaultValue: false
        }],
      }
    },
    {
      mode: 'admin',
      path: '/Repository/CleanupPolicies',
      ...UIStrings.CLEANUP_POLICIES.MENU,
      view: CleanupPolicies,
      iconCls: 'x-fa fa-broom',
      weight: 400,
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.ADMIN]
      },
    },
    {
      mode: 'admin',
      path: '/IQ',
      ...UIStrings.IQ_SERVER.MENU,
      view: IqServer,
      iconCls: 'x-fa fa-shield-alt',
      visibility: {
        permissions: [Permissions.SETTINGS.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Repository/Proprietary',
      ...UIStrings.PROPRIETARY_REPOSITORIES.MENU,
      view: ProprietaryRepositories,
      iconCls: 'x-fa fa-door-closed',
      visibility: {
        permissions: [Permissions.SETTINGS.READ]
      }
    },
    {
      mode: 'admin',
      path: '/Repository/InsightFrontend',
      text: 'Log4j Visualizer',
      description: 'Log4j Visualizer',
      view: InsightFrontend,
      iconCls: 'x-fa fa-binoculars',
      visibility: {
        requiresUser: true,
        statesEnabled: [
          {
            key: 'vulnerabilityCapabilityState',
            defaultValue: {enabled: false}
          }
        ],
        permissions: [Permissions.ADMIN]
      }
    },
    {
      mode: 'admin',
      path: '/System/Bundles',
      ...UIStrings.BUNDLES.MENU,
      view: Bundles,
      iconCls: 'x-fa fa-puzzle-piece',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: [Permissions.BUNDLES.READ]
      }
    },
    {
      mode: 'admin',
      path: '/System/API',
      ...UIStrings.API.MENU,
      view: Api,
      iconCls: 'x-fa fa-plug',
      visibility: {
        permissions: [Permissions.SETTINGS.READ]
      }
    },
    {
      mode: 'admin',
      path: '/System/HTTP',
      ...UIStrings.HTTP.MENU,
      view: HTTP,
      iconCls: 'x-fa fa-truck',
      visibility: {
        featureFlags: [{key: 'nexus.react.httpSettings', defaultValue: true}],
        permissions: [Permissions.SETTINGS.READ]
      },
    },
    {
      mode: 'admin',
      path: '/System/Licensing',
      ...UIStrings.LICENSING.MENU,
      view: Licensing,
      iconCls: 'x-fa fa-wallet',
      visibility: {
        permissions: [Permissions.LICENSING.READ]
      },
    },
    {
      mode: 'admin',
      path: '/System/Nodes',
      ...UIStrings.NODES.MENU,
      view: Nodes,
      iconCls: 'x-fa fa-archive',
      visibility: {  
        permissions: [Permissions.ADMIN],      
        statesEnabled: [
          {
            key: 'nexus.datastore.clustered.enabled',
            defaultValue: false
          }
        ]
      },
    },
    {
      mode: 'admin',
      path: '/Security/UserToken',
      ...UIStrings.USER_TOKEN_CONFIGURATION.MENU,
      view: UserTokens,
      iconCls: 'x-fa fa-key',
      visibility: {
        permissions: [Permissions.USER_TOKENS_SETTINGS.READ],
        editions: ['PRO'],
        licenseValid: [
          {
            key: 'usertoken',
            defaultValue: false
          }
        ],
      },
    },
    {
      mode: 'admin',
      path: '/Security/AtlassianCrowd',
      text: UIStrings.CROWD_SETTINGS.MENU.text,
      description: UIStrings.CROWD_SETTINGS.MENU.description,
      view: CrowdSettings,
      iconCls: 'x-fab fa-atlassian',
      visibility: {
        bundle: 'com.sonatype.nexus.plugins.nexus-crowd-plugin',
        licenseValid: [
          {
            key: 'crowd',
            defaultValue: false
          }
        ],
        permissions: ['nexus:crowd:read']
      }
    },
    {
      mode: 'admin',
      path: '/Security/Saml',
      text: UIStrings.SAML_CONFIGURATION.MENU.text,
      description: UIStrings.SAML_CONFIGURATION.MENU.description,
      view: SamlConfiguration,
      iconCls: 'x-fa fa-id-card',
      visibility: {
        bundle: 'com.sonatype.nexus.plugins.nexus-saml-plugin',
        permissions: ['nexus:*'],
        editions: ['PRO']
      }
    },
    {
      mode: 'user',
      path: '/User Token',
      text: UIStrings.USER_TOKEN.MENU.text,
      description: UIStrings.USER_TOKEN.MENU.description,
      view: UserToken,
      iconCls: 'x-fa fa-key',
      visibility: {
        bundle: 'com.sonatype.nexus.plugins.nexus-usertoken-plugin',
        statesEnabled: [
          {
            key: 'usertoken',
            defaultValue: {enabled: false}
          }
        ],
        permissions: ['nexus:usertoken-current:read'],
        editions: ['PRO']
      }
    },
    {
      mode: 'admin',
      path: '/Repository/DataStore',
      text: UIStrings.DATASTORE_CONFIGURATION.MENU.text,
      description: UIStrings.DATASTORE_CONFIGURATION.MENU.description,
      view: DataStoreConfiguration,
      iconCls: 'x-fa fa-database',
      visibility: {
        bundle: 'com.sonatype.nexus.plugins.nexus-pro-datastore-plugin',
        featureFlags: [{
          key: 'datastores',
          defaultValue: false
        }],
        permissions: ['nexus:*'],
        editions: ['PRO']
      }
    },
    {
      mode: 'admin',
      text: UIStrings.REPLICATION.MENU.text,
      description: UIStrings.REPLICATION.MENU.description,
      path: '/Repository/Replication',
      view: Replication,
      iconCls: 'x-fa fa-copy',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        statesEnabled: [
          {
            key: 'replicationCapabilityState',
            defaultValue: {enabled: false}
          }
        ],
        permissions: ['nexus:replication:read']
      }
    },
    {
      mode: 'browse',
      path: '/Tags',
      ...UIStrings.TAGS.MENU,
      view: Tags,
      iconCls: 'x-fa fa-tags',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [{
          key: 'nexus.react.tags',
          defaultValue: true
        }],
        permissions: [Permissions.TAGS.READ],
        editions: ['PRO']
      },
    },
    {
      mode: 'browse',
      path: '/Browse',
      ...UIStrings.BROWSE.MENU,
      view: Browse,
      iconCls: 'x-fa fa-database',
      authenticationRequired: false,
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [{
          key: 'nexus.react.browse',
          defaultValue: false
        }],
        statesEnabled: [{
          key: 'browseableformats',
          defaultValue: []
        }],
      }
    },
    {
      mode: 'browse',
      path: '/Upload',
      ...UIStrings.UPLOAD.MENU,
      view: Upload,
      iconCls: 'x-fa fa-upload',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        featureFlags: [{
          key: 'nexus.react.upload',
          defaultValue: true
        }],
        permissions: [Permissions.COMPONENT.CREATE],
      },
    },
    {
      mode: 'browse',
      path: '/MaliciousRisk',
      ...UIStrings.MALICIOUS_RISK.MENU,
      view: MaliciousRisk,
      iconCls: 'x-fa fa-exclamation-triangle malicious-risk-icon',
      weight: 101,
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        requiresUser: true,
        statesEnabled: [
          {
            key: MALWARE_RISK_ENABLED,
            defaultValue: false
          }
        ],
      }
    }
  ]
});
