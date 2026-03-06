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

import React from 'react';
import { DirectoryList, DirectoryPage } from '@sonatype/nexus-ui-plugin';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';
import UIStrings from '../../../constants/UIStrings';

export default function AdminSupportDirectoryPage() {
  return (
      <DirectoryPage
          routeName={ROUTE_NAMES.ADMIN.SUPPORT.DIRECTORY}
          text={UIStrings.ADMIN_SUPPORT_DIRECTORY.MENU.text}
          description={UIStrings.ADMIN_SUPPORT_DIRECTORY.MENU.description}
      >
        <DirectoryList>
          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-support-directory-logging-lnk"
              text={UIStrings.LOGGING.MENU.text}
              description={UIStrings.LOGGING.MENU.description}
              routeName={ROUTE_NAMES.ADMIN.SUPPORT.LOGGING.LIST}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-support-directory-logs-lnk"
              text={UIStrings.LOGS.MENU.text}
              description={UIStrings.LOGS.MENU.description}
              routeName={ROUTE_NAMES.ADMIN.SUPPORT.LOGS.ROOT}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-support-directory-recovery-lnk"
              text={UIStrings.RECOVERY_MODE.MENU.text}
              description={UIStrings.RECOVERY_MODE.MENU.description}
              routeName={ROUTE_NAMES.ADMIN.SUPPORT.RECOVERY.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-support-directory-status-lnk"
              text={UIStrings.METRIC_HEALTH.MENU.text}
              description={UIStrings.METRIC_HEALTH.MENU.description}
              routeName={ROUTE_NAMES.ADMIN.SUPPORT.STATUS.ROOT}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-support-directory-supportreqeuest-lnk"
              text={UIStrings.SUPPORT_REQUEST.MENU.text}
              description={UIStrings.SUPPORT_REQUEST.MENU.description}
              routeName={ROUTE_NAMES.ADMIN.SUPPORT.SUPPORTREQUEST.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-support-directory-supportzip-lnk"
              text={UIStrings.SUPPORT_ZIP.MENU.text}
              description={UIStrings.SUPPORT_ZIP.MENU.description}
              routeName={ROUTE_NAMES.ADMIN.SUPPORT.SUPPORTZIP.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-support-directory-system-info-lnk"
              text={UIStrings.SYSTEM_INFORMATION.MENU.text}
              description={UIStrings.SYSTEM_INFORMATION.MENU.description}
              routeName={ROUTE_NAMES.ADMIN.SUPPORT.SYSTEMINFORMATION.ROOT}
          />
        </DirectoryList>
      </DirectoryPage>);
}
