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
import { NxH3, NxPageSidebar } from '@sonatype/react-shared-components';
import {
  ExtJS,
  LeftNavigationMenuItem,
  LeftNavigationMenuCollapsibleItem,
  LeftNavigationMenuCollapsibleChildItem,
  isExtjsCapabilitiesEnabled,
  isReactCapabilitiesEnabled
} from '@sonatype/nexus-ui-plugin';

import './SettingsPageLayout.scss';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';
import UIStrings from '../../../constants/UIStrings';

export default function SettingsSidebar() {
  const ADMIN = ROUTE_NAMES.ADMIN;

  return (
    <NxPageSidebar className="nxrm-settings">
      <NxH3>Settings</NxH3>
      {/* === Repository -- Collapsible Menu === */}
      <LeftNavigationMenuCollapsibleItem
        name={ADMIN.REPOSITORY.DIRECTORY}
        text={UIStrings.REPOSITORY_DIRECTORY.MENU.text}
        data-analytics-id="nxrm-global-secondary-navbar-repository"
      >
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.REPOSITORY.REPOSITORIES.ROOT}
          text={UIStrings.REPOSITORIES.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-repository-repositories"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.REPOSITORY.BLOBSTORES.LIST}
          text={UIStrings.BLOB_STORES.MENU.text}
          selectedState="admin.repository.blobstores"
          data-analytics-id="nxrm-global-secondary-navbar-repository-blobstores"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.REPOSITORY.DATASTORE.ROOT}
          text={UIStrings.DATASTORE_CONFIGURATION.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-repository-datastore"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.REPOSITORY.PROPRIETARY.ROOT}
          text={UIStrings.PROPRIETARY_REPOSITORIES.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-repository-proprietary"
        />
        <LeftNavigationMenuCollapsibleChildItem
          selectedState={ADMIN.REPOSITORY.SELECTORS.ROOT}
          name={ADMIN.REPOSITORY.SELECTORS.LIST}
          text={UIStrings.CONTENT_SELECTORS.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-repository-selectors"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.REPOSITORY.CLEANUPPOLICIES.LIST}
          text={UIStrings.CLEANUP_POLICIES.MENU.text}
          params={{ itemId: null }}
          selectedState={ADMIN.REPOSITORY.CLEANUPPOLICIES.ROOT}
          data-analytics-id="nxrm-global-secondary-navbar-repository-cleanuppolicies"
        />
        <LeftNavigationMenuCollapsibleChildItem
          selectedState={ADMIN.REPOSITORY.ROUTINGRULES.ROOT}
          name={ADMIN.REPOSITORY.ROUTINGRULES.LIST}
          text={UIStrings.ROUTING_RULES.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-repository-routingrules"
        />
      </LeftNavigationMenuCollapsibleItem>

      {/* === Security -- Collapsible Menu === */}
      <LeftNavigationMenuCollapsibleItem
        name={ADMIN.SECURITY.DIRECTORY}
        text={UIStrings.ADMIN_SECURITY_DIRECTORY.MENU.text}
        data-analytics-id="nxrm-global-secondary-navbar-security"
      >
        <LeftNavigationMenuCollapsibleChildItem
          selectedState={ADMIN.SECURITY.PRIVILEGES.ROOT}
          name={ADMIN.SECURITY.PRIVILEGES.LIST}
          text={UIStrings.PRIVILEGES.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-security-privileges"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SECURITY.ROLES.LIST}
          selectedState={ADMIN.SECURITY.ROLES.ROOT}
          text={UIStrings.ROLES.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-security-roles"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SECURITY.USERS.ROOT}
          text={UIStrings.USERS.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-security-users"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SECURITY.ANONYMOUS.ROOT}
          text={UIStrings.ANONYMOUS_SETTINGS.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-security-anonymous"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SECURITY.ATLASSIANCROWD.ROOT}
          text={UIStrings.CROWD_SETTINGS.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-security-atlassiancrowd"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SECURITY.LDAP.ROOT}
          text={UIStrings.LDAP_SERVERS.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-security-ldap"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SECURITY.REALMS.ROOT}
          text={UIStrings.REALMS.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-security-realms"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SECURITY.SAML.ROOT}
          text={UIStrings.SAML_CONFIGURATION.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-security-saml"
        />
        <LeftNavigationMenuCollapsibleChildItem
            name={ADMIN.SECURITY.OAUTH2.ROOT}
            text={UIStrings.OAUTH2_CONFIGURATION.MENU.text}
            data-analytics-id="nxrm-global-secondary-navbar-security-oauth2"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SECURITY.SSLCERTIFICATES.LIST}
          text={UIStrings.SSL_CERTIFICATES.MENU.text}
          params={{ itemId: null }}
          selectedState={ADMIN.SECURITY.SSLCERTIFICATES.ROOT}
          data-analytics-id="nxrm-global-secondary-navbar-security-sslcertificates"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SECURITY.USERTOKEN.ROOT}
          text={UIStrings.USER_TOKEN_CONFIGURATION.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-security-usertoken"
        />
        {ExtJS.state().getValue('serviceAccountEnabled', false) &&
          <LeftNavigationMenuCollapsibleChildItem
            name={ADMIN.SECURITY.SERVICE_ACCOUNT_TOKENS.ROOT}
            text={UIStrings.SERVICE_ACCOUNT_TOKENS.MENU.text}
            data-analytics-id="nxrm-global-secondary-navbar-security-serviceaccounttokens"
          />
        }
      </LeftNavigationMenuCollapsibleItem>

      {/* === Support -- Collapsible Menu === */}
      <LeftNavigationMenuCollapsibleItem
        name={ADMIN.SUPPORT.DIRECTORY}
        text={UIStrings.ADMIN_SUPPORT_DIRECTORY.MENU.text}
        data-analytics-id="nxrm-global-secondary-navbar-support"
      >
        <LeftNavigationMenuCollapsibleChildItem
          selectedState={ADMIN.SUPPORT.LOGGING.ROOT}
          name={ADMIN.SUPPORT.LOGGING.LIST}
          text={UIStrings.LOGGING.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-support-logging"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SUPPORT.LOGS.ROOT}
          text={UIStrings.LOGS.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-support-logs"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SUPPORT.RECOVERY.ROOT}
          text={UIStrings.RECOVERY_MODE.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-support-recovery"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SUPPORT.STATUS.ROOT}
          text={UIStrings.METRIC_HEALTH.MENU.text}
          params={{ itemId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-support-status"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SUPPORT.SUPPORTREQUEST.ROOT}
          text={UIStrings.SUPPORT_REQUEST.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-support-supportrequest"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SUPPORT.SUPPORTZIP.ROOT}
          text={UIStrings.SUPPORT_ZIP.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-support-supportzip"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SUPPORT.SYSTEMINFORMATION.ROOT}
          text={UIStrings.SYSTEM_INFORMATION.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-support-systeminformation"
        />
      </LeftNavigationMenuCollapsibleItem>

      {/* === System -- Collapsible Menu === */}
      <LeftNavigationMenuCollapsibleItem
        name={ADMIN.SYSTEM.DIRECTORY}
        text={UIStrings.ADMIN_SYSTEM_DIRECTORY.MENU.text}
        data-analytics-id="nxrm-global-secondary-navbar-system"
      >
        {isExtjsCapabilitiesEnabled() &&
          <LeftNavigationMenuCollapsibleChildItem
            name={ADMIN.SYSTEM.CAPABILITIES_EXTJS.ROOT}
            text={UIStrings.CAPABILITIES.MENU.text}
            params={{ id: null }}
            data-analytics-id='nxrm-global-secondary-navbar-system-capabilities'
          />
        }
        {isReactCapabilitiesEnabled() &&
          <LeftNavigationMenuCollapsibleChildItem
            name={ADMIN.SYSTEM.CAPABILITIES.LIST}
            text={UIStrings.CAPABILITIES.MENU.text}
            selectedState={ADMIN.SYSTEM.CAPABILITIES.ROOT}
            data-analytics-id='nxrm-global-secondary-navbar-system-capabilities'
          />
        }
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SYSTEM.API.ROOT}
          text={UIStrings.API.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-system-api"
        />
        {ExtJS.state().getValue('nexus.previewui.enabled') &&
          <LeftNavigationMenuCollapsibleChildItem
            name={ADMIN.SYSTEM.PREVIEWUI.ROOT}
            text={UIStrings.PREVIEW_UI_SETTINGS.MENU.text}
            data-analytics-id="nxrm-global-secondary-navbar-system-previewui"
          />
        }
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SYSTEM.EMAILSERVER.ROOT}
          text={UIStrings.EMAIL_SERVER.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-security-emailserver"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SYSTEM.HTTP.ROOT}
          text={UIStrings.HTTP.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-system-http"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SYSTEM.LICENSING.ROOT}
          text={UIStrings.LICENSING.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-system-licensing"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SYSTEM.NODES.ROOT}
          text={UIStrings.NODES.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-system-nodes"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SYSTEM.TASKS.ROOT}
          text={UIStrings.TASKS.MENU.text}
          params={{ taskId: null }}
          data-analytics-id="nxrm-global-secondary-navbar-system-tasks"
        />
        <LeftNavigationMenuCollapsibleChildItem
          name={ADMIN.SYSTEM.UPGRADE.ROOT}
          text={UIStrings.UPGRADE.MENU.text}
          data-analytics-id="nxrm-global-secondary-navbar-system-upgrade"
        />
      </LeftNavigationMenuCollapsibleItem>

      <LeftNavigationMenuItem
        text={UIStrings.IQ_SERVER.MENU.text}
        name={ADMIN.IQ.ROOT}
        data-analytics-id="nxrm-global-secondary-navbar-iq"
      />
    </NxPageSidebar>
  );
}
