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
import UIStrings from '../../../constants/UIStrings';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';

export default function AdminRepositoriesDirectoryPage() {
  const ADMIN = ROUTE_NAMES.ADMIN;

  return (
      <DirectoryPage
        routeName={ROUTE_NAMES.ADMIN.REPOSITORY.DIRECTORY}
        text={UIStrings.REPOSITORY_DIRECTORY.MENU.text}
        description={UIStrings.REPOSITORY_DIRECTORY.MENU.description}
      >
        <DirectoryList>
          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-repository-directory-repositories-lnk"
              text={UIStrings.REPOSITORIES.MENU.text}
              description={UIStrings.REPOSITORIES.MENU.description}
              routeName={ADMIN.REPOSITORY.REPOSITORIES.ROOT}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-repository-directory-blobstores-lnk"
              text={UIStrings.BLOB_STORES.MENU.text}
              description={UIStrings.BLOB_STORES.MENU.description}
              routeName={ADMIN.REPOSITORY.BLOBSTORES.LIST}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-repository-directory-datastore-lnk"
              text={UIStrings.DATASTORE_CONFIGURATION.MENU.text}
              description={UIStrings.DATASTORE_CONFIGURATION.MENU.description}
              routeName={ADMIN.REPOSITORY.DATASTORE.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-repository-directory-proprietary-lnk"
              {...UIStrings.PROPRIETARY_REPOSITORIES.MENU}
              routeName={ADMIN.REPOSITORY.PROPRIETARY.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-repository-directory-selectors-lnk"
              text={UIStrings.CONTENT_SELECTORS.MENU.text}
              description={UIStrings.CONTENT_SELECTORS.MENU.description}
              routeName={ADMIN.REPOSITORY.SELECTORS.LIST}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-repository-directory-cleanup-policies-lnk"
              text={UIStrings.CLEANUP_POLICIES.MENU.text}
              description={UIStrings.CLEANUP_POLICIES.MENU.description}
              routeName={ADMIN.REPOSITORY.CLEANUPPOLICIES.LIST}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-repository-directory-routingrules-lnk"
              text={UIStrings.ROUTING_RULES.MENU.text}
              description={UIStrings.ROUTING_RULES.MENU.description}
              routeName={ADMIN.REPOSITORY.ROUTINGRULES.LIST}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-repository-directory-global-distribution-lnk"
              text="Global Distribution"
              description="Distribute repository content globally"
              routeName={ADMIN.REPOSITORY.GLOBAL_DISTRIBUTION.ROOT}
          />
        </DirectoryList>
      </DirectoryPage>);
}
