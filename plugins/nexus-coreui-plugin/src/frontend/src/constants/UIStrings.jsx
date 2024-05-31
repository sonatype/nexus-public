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

import {UIStrings} from '@sonatype/nexus-ui-plugin';
import AnonymousAccessStrings from './pages/admin/security/AnonymousAccessStrings';
import RealmsStrings from './pages/admin/security/RealmsStrings';
import PrivilegesStrings from './pages/admin/security/PrivilegesStrings';
import RolesStrings from './pages/admin/security/RolesStrings';
import UsersStrings from './pages/admin/security/UsersStrings';
import SslCertificatesStrings from './pages/admin/security/SslCertificatesStrings';
import LdapStrings from './pages/admin/security/LdapStrings';
import EmailServerStrings from './pages/admin/system/EmailServerStrings';
import LicensingStrings from './pages/admin/system/LicensingStrings';
import BlobStoresStrings from './pages/admin/repository/BlobStoresStrings';
import ContentSelectorsStrings from './pages/admin/repository/ContentSelectorsStrings';
import LoggingStrings from './pages/admin/support/LoggingStrings';
import LogsStrings from './pages/admin/support/LogsStrings';
import RepositoriesStrings from './pages/admin/repository/RepositoriesStrings';
import RoutingRulesStrings from './pages/admin/repository/RoutingRulesStrings';
import Log4jVisualizerStrings from './pages/browse/Log4jVisualizerStrings';
import SystemInformationStrings from './pages/admin/system/SystemInformationStrings';
import SupportRequestStrings from './pages/admin/support/SupportRequestStrings';
import AccountStrings from './pages/user/AccountStrings';
import NuGetApiKeyStrings from './pages/user/NuGetApiKeyStrings';
import StatusStrings from './pages/admin/support/StatusStrings';
import SupportZipStrings from './pages/admin/support/SupportZipStrings';
import CleanupPoliciesStrings from './pages/admin/repository/CleanupPoliciesStrings';
import IqServerStrings from './pages/admin/IqServerStrings';
import BundlesStrings from './pages/admin/system/BundlesStrings';
import ApiStrings from './pages/admin/system/ApiStrings';
import ProprietaryRepositoriesStrings from './pages/admin/repository/ProprietaryRepositoriesStrings';
import HttpStrings from './pages/admin/system/HttpStrings';
import TasksStrings from './pages/admin/system/TasksStrings';
import AtlassianCrowdStrings from './pages/admin/security/AtlassianCrowdStrings';
import SamlStrings from './pages/admin/security/SamlStrings';
import DataStoreStrings from './pages/admin/repository/DataStoreStrings';
import ReplicationStrings from './pages/admin/repository/ReplicationStrings';
import UserTokenConfigurationStrings from './pages/admin/security/UserTokenStrings';
import UserTokenStrings from './pages/user/UserTokenStrings';
import WelcomeStrings from './pages/user/WelcomeStrings';
import TagsStrings from './pages/browse/tags/TagsStrings';
import UploadStrings from './pages/browse/upload/UploadStrings';
import NodesStrings from './pages/admin/system/NodesStrings';
import BrowseStrings from './pages/browse/browse/BrowseStrings';

export default {
  ...UIStrings,

  // welcome page
  ...WelcomeStrings,

  // browse
  ...Log4jVisualizerStrings,
  ...TagsStrings,
  ...BrowseStrings,
  ...UploadStrings,

  // admin
  ...IqServerStrings,

  // admin/repository
  ...BlobStoresStrings,
  ...CleanupPoliciesStrings,
  ...ContentSelectorsStrings,
  ...DataStoreStrings,
  ...ProprietaryRepositoriesStrings,
  ...RepositoriesStrings,
  ...ReplicationStrings,
  ...RoutingRulesStrings,

  // admin/security
  ...AnonymousAccessStrings,
  ...AtlassianCrowdStrings,
  ...LdapStrings,
  ...PrivilegesStrings,
  ...RealmsStrings,
  ...RolesStrings,
  ...SamlStrings,
  ...SslCertificatesStrings,
  ...UsersStrings,
  ...UserTokenConfigurationStrings,

  // admin/support
  ...LoggingStrings,
  ...LogsStrings,
  ...StatusStrings,
  ...SupportRequestStrings,
  ...SupportZipStrings,

  // admin/system
  ...ApiStrings,
  ...BundlesStrings,
  ...EmailServerStrings,
  ...HttpStrings,
  ...TasksStrings,
  ...LicensingStrings,
  ...NodesStrings,
  ...SystemInformationStrings,

  // user
  ...AccountStrings,
  ...NuGetApiKeyStrings,
  ...UserTokenStrings,

  // other
  FORMAT_PLACEHOLDER: 'Format',

  HEALTHCHECK_EULA: {
    HEADER: 'Nexus IQ Server Terms of Use',
    BUTTONS: {
      ACCEPT: 'I accept',
      DECLINE: 'I do not accept'
    }
  },

  ANALYZE_APPLICATION: {
    HEADER: 'Analyze Application',
    MAIN: 'Application analysis performs a deep inspection of this application, identifying potential risks.<br/> More information is available here <a href=\"http://links.sonatype.com/products/insight/ac/home\">here</a>',
    EMAIL: {
      LABEL: 'Email address',
      DESCRIPTION: 'The address where the summary report will be sent'
    },
    PASSWORD: {
      LABEL: 'Report password',
      DESCRIPTION: 'A password to gain access to the detailed report'
    },
    PACKAGES: {
      LABEL: 'Proprietary packages',
      DESCRIPTION: 'A comma separated list of proprietary packages'
    },
    REPORT: {
      LABEL: 'Report label',
      DESCRIPTION: 'The name the report will be given'
    },
    SELECT_ASSET: {
      LABEL: 'Select Asset',
      DESCRIPTION: 'Select an asset to base the analysis on'
    },
    BUTTONS: {
      ANALYZE: 'Analyze',
      CANCEL: 'Cancel'
    }
  }
};
