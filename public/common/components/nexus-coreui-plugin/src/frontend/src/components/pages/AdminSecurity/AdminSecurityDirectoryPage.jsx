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
import { DirectoryList, DirectoryPage, ExtJS } from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../constants/UIStrings';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';

export default function AdminSecurityDirectoryPage() {
  const ADMIN = ROUTE_NAMES.ADMIN;

  return (
      <DirectoryPage routeName={ADMIN.SECURITY.DIRECTORY} {...UIStrings.ADMIN_SECURITY_DIRECTORY.MENU}>
        <DirectoryList>
          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-privleges-lnk"
              text={UIStrings.PRIVILEGES.MENU.text}
              description={UIStrings.PRIVILEGES.MENU.description}
              routeName={ADMIN.SECURITY.PRIVILEGES.LIST}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-roles-lnk"
              text={UIStrings.ROLES.MENU.text}
              description={UIStrings.ROLES.MENU.description}
              routeName={ADMIN.SECURITY.ROLES.LIST}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-users-lnk"
              text={UIStrings.USERS.MENU.text}
              description={UIStrings.USERS.MENU.description}
              routeName={ADMIN.SECURITY.USERS.ROOT}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-anonymous-lnk"
              text={UIStrings.ANONYMOUS_SETTINGS.MENU.text}
              description={UIStrings.ANONYMOUS_SETTINGS.MENU.description}
              routeName={ADMIN.SECURITY.ANONYMOUS.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-crowd-lnk"
              text={UIStrings.CROWD_SETTINGS.MENU.text}
              description={UIStrings.CROWD_SETTINGS.MENU.description}
              routeName={ADMIN.SECURITY.ATLASSIANCROWD.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-ldap-lnk"
              text={UIStrings.LDAP_SERVERS.MENU.text}
              description={UIStrings.LDAP_SERVERS.MENU.description}
              routeName={ADMIN.SECURITY.LDAP.ROOT}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-realms-lnk"
              text={UIStrings.REALMS.MENU.text}
              description={UIStrings.REALMS.MENU.description}
              routeName={ADMIN.SECURITY.REALMS.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-saml-config-lnk"
              text={UIStrings.SAML_CONFIGURATION.MENU.text}
              description={UIStrings.SAML_CONFIGURATION.MENU.description}
              routeName={ADMIN.SECURITY.SAML.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-oauth2-config-lnk"
              text={UIStrings.OAUTH2_CONFIGURATION.MENU.text}
              description={UIStrings.OAUTH2_CONFIGURATION.MENU.description}
              routeName={ADMIN.SECURITY.OAUTH2.ROOT}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-ssl-certs-lnk"
              text={UIStrings.SSL_CERTIFICATES.MENU.text}
              description={UIStrings.SSL_CERTIFICATES.MENU.description}
              routeName={ADMIN.SECURITY.SSLCERTIFICATES.LIST}
              params={{ itemId: null }}
          />

          <DirectoryList.DirectoryListItem
              data-analytics-id="nxrm-admin-security-directory-user-token-config-lnk"
              text={UIStrings.USER_TOKEN_CONFIGURATION.MENU.text}
              description={UIStrings.USER_TOKEN_CONFIGURATION.MENU.description}
              routeName={ADMIN.SECURITY.USERTOKEN.ROOT}
          />

          {ExtJS.state().getValue('serviceAccountEnabled', false) &&
            <DirectoryList.DirectoryListItem
                data-analytics-id="nxrm-admin-security-directory-service-account-tokens-lnk"
                text={UIStrings.SERVICE_ACCOUNT_TOKENS.MENU.text}
                description={UIStrings.SERVICE_ACCOUNT_TOKENS.MENU.description}
                routeName={ADMIN.SECURITY.SERVICE_ACCOUNT_TOKENS.ROOT}
            />
          }

        </DirectoryList>
      </DirectoryPage>);
}
