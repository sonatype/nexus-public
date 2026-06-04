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

import './App';
import { enforceHashRouting } from './hashEnforcement';
import S3BlobStoreSettings from './components/pages/admin/BlobStores/S3/S3BlobStoreSettings';
import S3BlobStoreWarning from './components/pages/admin/BlobStores/S3/S3BlobStoreWarning';
import S3BlobStoreActions from './components/pages/admin/BlobStores/S3/S3BlobStoreActions';
import AzureBlobStoreSettings from './components/pages/admin/BlobStores/Azure/AzureBlobStoreSettings';
import AzureBlobStoreActions from './components/pages/admin/BlobStores/Azure/AzureBlobStoreActions';
import GoogleBlobStoreSettings from './components/pages/admin/BlobStores/Google/GoogleBlobStoreSettings';
import GoogleBlobStoreActions from './components/pages/admin/BlobStores/Google/GoogleBlobStoreActions';
import FileBlobStoreWarning from './components/pages/admin/BlobStores/File/FileBlobStoreWarning';
import CommunityEulaOnboarding from './components/pages/onboarding/CommunityEulaOnboarding';
import CommunityDiscoverOnboarding from './components/pages/onboarding/CommunityDiscoverOnboarding';
import TelemetryWarningBanner from './components/shared/telemetry/TelemetryWarningBanner';
import './styles/extjs.scss';
import { configureAxios, configureDebugLogging, exposeCreateRoot } from '@sonatype/nexus-ui-plugin';

enforceHashRouting();

configureAxios();
configureDebugLogging();
exposeCreateRoot();

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
  },
  google: {
    Settings: GoogleBlobStoreSettings,
    Actions: GoogleBlobStoreActions
  },
  file: {
    Warning: FileBlobStoreWarning
  }
}

window.ReactComponents = {
  ...window.ReactComponents,
  CommunityEulaOnboarding,
  CommunityDiscoverOnboarding,
  TelemetryWarningBanner,
};
