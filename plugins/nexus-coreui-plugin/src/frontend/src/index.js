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
import SystemInformation from './components/pages/admin/SystemInformation/SystemInformation';
import SupportRequest from './components/pages/admin/SupportRequest/SupportRequest';
import MetricHealth from "./components/pages/admin/MetricHealth/MetricHealth";

import UIStrings from './constants/UIStrings';
import UserAccount from "./components/pages/admin/UserAccount/UserAccount";
import NuGetApiToken from "./components/pages/user/NuGetApiToken/NuGetApiToken";

window.plugins.push({
  id: 'nexus-coreui-plugin',

  features: [
    {
      mode: 'admin',
      path: '/Security/Anonymous',
      text: UIStrings.ANONYMOUS_SETTINGS.MENU.text,
      description: UIStrings.ANONYMOUS_SETTINGS.MENU.description,
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
      text: UIStrings.LOGGING.MENU.text,
      description: UIStrings.LOGGING.MENU.description,
      view: LoggingConfiguration,
      iconCls: 'x-fa fa-book',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:logging:read']
      }
    },
    {
      mode: 'admin',
      path: '/Support/SupportRequest',
      text: UIStrings.SUPPORT_REQUEST.MENU.text,
      description: UIStrings.SUPPORT_REQUEST.MENU.description,
      view: SupportRequest,
      iconCls: 'x-fa fa-user-circle-o',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:atlas:create'],
        editions: ['PRO']
      }
    },
    {
      mode: 'admin',
      path: '/Support/SystemInformation',
      text: UIStrings.SYSTEM_INFORMATION.MENU.text,
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
      text: UIStrings.USER_ACCOUNT.MENU.text,
      description: UIStrings.USER_ACCOUNT.MENU.description,
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
      text: UIStrings.NUGET_API_KEY.TITLE,
      description: UIStrings.NUGET_API_KEY.DESCRIPTION,
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
      text: 'Status',
      description: 'System status checks',
      view: MetricHealth,
      iconCls: 'x-fa fa-medkit',
      visibility: {
        bundle: 'org.sonatype.nexus.plugins.nexus-coreui-plugin',
        permissions: ['nexus:metrics:read']
      }
    },
  ]
});
