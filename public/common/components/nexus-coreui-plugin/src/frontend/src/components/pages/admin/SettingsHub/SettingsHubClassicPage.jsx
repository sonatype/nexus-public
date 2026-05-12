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
import { DirectoryList, DirectoryPage, isExtjsCapabilitiesEnabled, isReactCapabilitiesEnabled } from '@sonatype/nexus-ui-plugin';
import { ROUTE_NAMES } from '../../../../routerConfig/routeNames/routeNames';

const sectionHeadingStyle = { margin: '24px 0 8px', fontSize: '18px', fontWeight: 600 };

export default function SettingsHubClassicPage() {
  const ADMIN = ROUTE_NAMES.ADMIN;

  return (
    <DirectoryPage
      routeName={ADMIN.DIRECTORY}
      text="Settings"
      description="Configure security, repositories, system, and support"
    >
      <h3 style={sectionHeadingStyle}>Security</h3>
      <DirectoryList>
        <DirectoryList.DirectoryListItem text="Users" description="Manage user accounts and access" routeName={ADMIN.SECURITY.USERS.ROOT} params={{ itemId: null }} />
        <DirectoryList.DirectoryListItem text="Roles" description="Define user permissions and access control" routeName={ADMIN.SECURITY.ROLES.LIST} />
        <DirectoryList.DirectoryListItem text="Privileges" description="Configure granular permission settings" routeName={ADMIN.SECURITY.PRIVILEGES.LIST} />
        <DirectoryList.DirectoryListItem text="Realms" description="Configure authentication sources and order" routeName={ADMIN.SECURITY.REALMS.ROOT} />
        <DirectoryList.DirectoryListItem text="LDAP" description="Configure LDAP integration" routeName={ADMIN.SECURITY.LDAP.ROOT} params={{ itemId: null }} />
        <DirectoryList.DirectoryListItem text="SAML" description="Configure SAML single sign-on" routeName={ADMIN.SECURITY.SAML.ROOT} />
        <DirectoryList.DirectoryListItem text="OAuth2" description="Configure OAuth 2.0 authentication" routeName={ADMIN.SECURITY.OAUTH2.ROOT} />
        <DirectoryList.DirectoryListItem text="Atlassian Crowd" description="Configure Atlassian Crowd integration" routeName={ADMIN.SECURITY.ATLASSIANCROWD.ROOT} />
        <DirectoryList.DirectoryListItem text="Anonymous Access" description="Configure anonymous access settings" routeName={ADMIN.SECURITY.ANONYMOUS.ROOT} />
        <DirectoryList.DirectoryListItem text="SSL Certificates" description="Manage trusted SSL certificates" routeName={ADMIN.SECURITY.SSLCERTIFICATES.LIST} />
        <DirectoryList.DirectoryListItem text="User Tokens" description="Manage API access tokens" routeName={ADMIN.SECURITY.USERTOKEN.ROOT} />
      </DirectoryList>

      <h3 style={sectionHeadingStyle}>Repository</h3>
      <DirectoryList>
        <DirectoryList.DirectoryListItem text="Repositories" description="Manage Maven, npm, Docker, and other repositories" routeName={ADMIN.REPOSITORY.REPOSITORIES.ROOT} params={{ itemId: null }} />
        <DirectoryList.DirectoryListItem text="Blob Stores" description="Configure local and cloud blob storage" routeName={ADMIN.REPOSITORY.BLOBSTORES.LIST} />
        <DirectoryList.DirectoryListItem text="Cleanup Policies" description="Configure automated component cleanup" routeName={ADMIN.REPOSITORY.CLEANUPPOLICIES.LIST} />
        <DirectoryList.DirectoryListItem text="Routing Rules" description="Control component access by path patterns" routeName={ADMIN.REPOSITORY.ROUTINGRULES.LIST} />
        <DirectoryList.DirectoryListItem text="Content Selectors" description="Define content filtering expressions" routeName={ADMIN.REPOSITORY.SELECTORS.LIST} />
        <DirectoryList.DirectoryListItem text="Data Store" description="Configure repository metadata database" routeName={ADMIN.REPOSITORY.DATASTORE.ROOT} />
        <DirectoryList.DirectoryListItem text="Proprietary" description="Manage proprietary component settings" routeName={ADMIN.REPOSITORY.PROPRIETARY.ROOT} />
      </DirectoryList>

      <h3 style={sectionHeadingStyle}>System</h3>
      <DirectoryList>
        <DirectoryList.DirectoryListItem text="Tasks" description="Schedule and monitor system tasks" routeName={ADMIN.SYSTEM.TASKS.ROOT} params={{ taskId: null }} />
        <DirectoryList.DirectoryListItem text="Email Server" description="Configure email server and notifications" routeName={ADMIN.SYSTEM.EMAILSERVER.ROOT} />
        <DirectoryList.DirectoryListItem text="HTTP" description="Configure HTTP and HTTPS settings" routeName={ADMIN.SYSTEM.HTTP.ROOT} />
        <DirectoryList.DirectoryListItem text="API" description="Configure REST API settings" routeName={ADMIN.SYSTEM.API.ROOT} />
        {isExtjsCapabilitiesEnabled() &&
          <DirectoryList.DirectoryListItem text="Capabilities" description="Configure system capabilities and integrations" routeName={ADMIN.SYSTEM.CAPABILITIES_EXTJS.ROOT} params={{ id: null }} />
        }
        {isReactCapabilitiesEnabled() &&
          <DirectoryList.DirectoryListItem text="Capabilities" description="Configure system capabilities and integrations" routeName={ADMIN.SYSTEM.CAPABILITIES.LIST} />
        }
        <DirectoryList.DirectoryListItem text="Licensing" description="View and manage license information" routeName={ADMIN.SYSTEM.LICENSING.ROOT} />
        <DirectoryList.DirectoryListItem text="Nodes" description="View cluster nodes and status" routeName={ADMIN.SYSTEM.NODES.ROOT} />
        <DirectoryList.DirectoryListItem text="IQ Server" description="Configure Sonatype IQ Server integration" routeName={ADMIN.IQ.ROOT} />
        <DirectoryList.DirectoryListItem text="Nexus One UI" description="Configure Nexus One UI settings and features" routeName={ADMIN.SYSTEM.PREVIEWUI.ROOT} />
        <DirectoryList.DirectoryListItem text="Upgrade" description="Manage system upgrades and updates" routeName={ADMIN.SYSTEM.UPGRADE.ROOT} />
      </DirectoryList>

      <h3 style={sectionHeadingStyle}>Support</h3>
      <DirectoryList>
        <DirectoryList.DirectoryListItem text="System Information" description="View system information and diagnostics" routeName={ADMIN.SUPPORT.SYSTEMINFORMATION.ROOT} />
        <DirectoryList.DirectoryListItem text="Logs" description="View application logs" routeName={ADMIN.SUPPORT.LOGS.ROOT} params={{ itemId: null }} />
        <DirectoryList.DirectoryListItem text="Logging Configuration" description="Configure log levels and output" routeName={ADMIN.SUPPORT.LOGGING.LIST} />
        <DirectoryList.DirectoryListItem text="Metric Health" description="View system metrics and health checks" routeName={ADMIN.SUPPORT.STATUS.ROOT} params={{ itemId: null }} />
        <DirectoryList.DirectoryListItem text="Support ZIP" description="Generate support diagnostics bundle" routeName={ADMIN.SUPPORT.SUPPORTZIP.ROOT} />
        <DirectoryList.DirectoryListItem text="Support Request" description="Submit a support request" routeName={ADMIN.SUPPORT.SUPPORTREQUEST.ROOT} />
      </DirectoryList>
    </DirectoryPage>
  );
}
