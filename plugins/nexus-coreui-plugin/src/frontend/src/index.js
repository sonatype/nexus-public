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
import ContentSelectors from './components/pages/admin/ContentSelectors/ContentSelectors';
import Roles from './components/pages/admin/Roles/Roles';
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
import IqServer from './components/pages/admin/IqServer/IqServer';
import Bundles from './components/pages/admin/Bundles/Bundles';
import ProprietaryRepositories from './components/pages/admin/ProprietaryRepositories/ProprietaryRepositories';

window.ReactComponents = {
  ...window.ReactComponents,
  AnalyzeApplication
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
  }
}

window.plugins.push({
  id: 'nexus-coreui-plugin',

  features: [
    {
      mode: 'admin',
      path: '/Repository/Blobstores',
      ...UIStrings.BLOB_STORES.MENU,
      view: BlobStores,
      iconCls: 'x-fa fa-server',
      visibility: {
        permissions: ['nexus:blobstores:read']
      }
    },
    {
      mode: 'admin',
      path: '/Repository/Repositories-new',
      ...UIStrings.REPOSITORIES.MENU,
      view: Repositories,
      iconCls: 'x-fa fa-database',
      visibility: {
        featureFlags: [{key: 'nexus.react.repositories', defaultValue: false}],
        permissions: ['nexus:repository-admin:*:*:read']
      },
      weight: 10,
    },
    {
      mode: 'admin',
      path: '/Repository/Selectors',
      ...UIStrings.CONTENT_SELECTORS.MENU,
      view: ContentSelectors,
      iconCls: 'x-fa fa-layer-group',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:selectors:read']
      },
      weight: 300,
    },
    {
      mode: 'admin',
      ...UIStrings.ROUTING_RULES.MENU,
      path: '/Repository/RoutingRules',
      view: RoutingRules,
      iconCls: 'x-fa fa-map-signs',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:*']
      },
      weight: 500,
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
      path: '/Security/Anonymous',
      ...UIStrings.ANONYMOUS_SETTINGS.MENU,
      view: AnonymousSettings,
      iconCls: 'x-fa fa-user',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:settings:read']
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
        permissions: ['nexus:logging:read']
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
        permissions: ['nexus:atlas:create'],
        editions: ['PRO']
      }
    },
    {
      mode: 'admin',
      path: '/Support/SystemInformation',
      ...UIStrings.SYSTEM_INFORMATION.MENU,
      view: SystemInformation,
      iconCls: 'x-fa fa-globe',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:atlas:read']
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
        permissions: ['nexus:metrics:read']
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
        permissions: ['nexus:metrics:read']
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
        permissions: ['nexus:logging:read']
      }
    },
    {
      mode: 'admin',
      path: '/Repository/CleanupPolicies',
      ...UIStrings.CLEANUP_POLICIES.MENU,
      view: CleanupPolicies,
      iconCls: 'x-fa fa-broom',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:*']
      },
      weight: 400,
    },
    {
      mode: 'admin',
      path: '/IQ',
      ...UIStrings.IQ_SERVER.MENU,
      view: IqServer,
      iconCls: 'x-fa fa-shield-alt',
      visibility: {
        permissions: ['nexus:settings:read']
      }
    },
    {
      mode: 'admin',
      path: '/Repository/Proprietary',
      ...UIStrings.PROPRIETARY_REPOSITORIES.MENU,
      view: ProprietaryRepositories,
      iconCls: 'x-fa fa-door-closed',
      visibility: {
        permissions: ['nexus:settings:read']
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
        permissions: ['nexus:*']
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
        permissions: ['nexus:bundles:read']
      }
    },
  ]
});
