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
import AnonymousSettings from './components/pages/admin/AnonymousSettings/AnonymousSettings';
import LoggingConfiguration from './components/pages/admin/LoggingConfiguration/LoggingConfiguration';
import LogViewer from "./components/pages/admin/LogViewer/LogViewer";
import SystemInformation from './components/pages/admin/SystemInformation/SystemInformation';
import SupportRequest from './components/pages/admin/SupportRequest/SupportRequest';
import MetricHealth from "./components/pages/admin/MetricHealth/MetricHealth";
import SupportZip from "./components/pages/admin/SupportZip/SupportZip";

import UIStrings from './constants/UIStrings';
import UserAccount from "./components/pages/admin/UserAccount/UserAccount";
import NuGetApiToken from "./components/pages/user/NuGetApiToken/NuGetApiToken";

window.plugins.push({
  id: 'nexus-coreui-plugin',

  features: [
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
      path: '/Support/Logging/LogViewer',
      ...UIStrings.LOG_VIEWER.MENU,
      view: LogViewer,
      iconCls: 'x-fa fa-terminal',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:logging:read']
      }
    },
  ]
});
