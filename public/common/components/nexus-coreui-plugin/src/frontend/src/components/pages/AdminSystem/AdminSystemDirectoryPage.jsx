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

import UIStrings from '../../../constants/UIStrings';
import { DirectoryList, DirectoryPage, ExtJS } from '@sonatype/nexus-ui-plugin';
import React from 'react';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';
import { isExtjsCapabilitiesEnabled, isReactCapabilitiesEnabled } from '@sonatype/nexus-ui-plugin';

export default function AdminSystemDirectoryPage() {
  const ADMIN = ROUTE_NAMES.ADMIN;

  return (
      <DirectoryPage
          routeName={ADMIN.SYSTEM.DIRECTORY}
          text={UIStrings.ADMIN_SYSTEM_DIRECTORY.MENU.text}
          description={UIStrings.ADMIN_SYSTEM_DIRECTORY.MENU.description}
      >
        <DirectoryList>
          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-system-directory-api-lnk"
              text={UIStrings.API.MENU.text}
              description={UIStrings.API.MENU.description}
              routeName={ADMIN.SYSTEM.API.ROOT}
          />

          {isExtjsCapabilitiesEnabled() &&
            <DirectoryList.DirectoryListItem
                data-analytics-id="nxrm-admin-system-directory-capabilities-lnk"
                text={UIStrings.CAPABILITIES.MENU.text}
                description={UIStrings.CAPABILITIES.MENU.description}
                routeName={ADMIN.SYSTEM.CAPABILITIES_EXTJS.ROOT}
                params={{ id: null }}
            />
          }

          {isReactCapabilitiesEnabled() &&
            <DirectoryList.DirectoryListItem
                data-analytics-id="nxrm-admin-system-directory-capabilities-lnk"
                text={UIStrings.CAPABILITIES.MENU.text}
                description={UIStrings.CAPABILITIES.MENU.description}
                routeName={ADMIN.SYSTEM.CAPABILITIES.LIST}
                params={{ id: null }}
            />
          }

          {ExtJS.state().getValue('nexus.previewui.enabled') &&
            <DirectoryList.DirectoryListItem
                data-analytics-id="nxrm-admin-system-directory-previewui-lnk"
                text={UIStrings.PREVIEW_UI_SETTINGS.MENU.text}
                description={UIStrings.PREVIEW_UI_SETTINGS.MENU.description}
                routeName={ADMIN.SYSTEM.PREVIEWUI.ROOT}
            />
          }

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-system-directory-email-server-lnk"
              text={UIStrings.EMAIL_SERVER.MENU.text}
              description={UIStrings.EMAIL_SERVER.MENU.description}
              routeName={ADMIN.SYSTEM.EMAILSERVER.ROOT}
          />


          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-system-directory-http-lnk"
              text={UIStrings.HTTP.MENU.text}
              description={UIStrings.HTTP.MENU.description}
              routeName={ADMIN.SYSTEM.HTTP.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-system-directory-licensing-lnk"
              text={UIStrings.LICENSING.MENU.text}
              description={UIStrings.LICENSING.MENU.description}
              routeName={ADMIN.SYSTEM.LICENSING.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-system-directory-nodes-lnk"
              text={UIStrings.NODES.MENU.text}
              description={UIStrings.NODES.MENU.description}
              routeName={ADMIN.SYSTEM.NODES.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-system-directory-tasks-lnk"
              text={UIStrings.TASKS.MENU.text}
              description={UIStrings.TASKS.MENU.description}
              routeName={ADMIN.SYSTEM.TASKS.ROOT}
              params={{ taskId: null }}
          />

        </DirectoryList>
      </DirectoryPage>);
}
