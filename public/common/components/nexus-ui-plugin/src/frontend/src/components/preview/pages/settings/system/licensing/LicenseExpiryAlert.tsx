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
import { Box } from '@radix-ui/themes';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { SettingsAlert } from '../../../../shared/form';

interface LicenseExpiryWarning {
  enabled: boolean;
  message: string;
}

/**
 * LicenseExpiryAlert - Displays a warning banner when the license is about to expire.
 * Uses state from LicenseExpiryWarningStateContributor (Pro edition only).
 * Only visible for administrator users.
 *
 * Note: 'licenseExpiryWarning' state is contributed only by the Pro license expiry state
 * contributor. It is absent in CE and unlicensed instances; the component no-ops silently.
 */
export function LicenseExpiryAlert() {
  const licenseExpiryWarning = ExtJS.useState?.(() =>
    ExtJS.state()?.getValue?.('licenseExpiryWarning')
  ) as LicenseExpiryWarning | undefined;

  const user = ExtJS.useUser();

  // Don't show if:
  // - User is not an admin
  // - Warning state is not available
  // - Warning is not enabled
  if (!user?.administrator || !licenseExpiryWarning?.enabled) {
    return null;
  }

  return (
    <Box className="license-expiry-alert">
      <SettingsAlert type="warning">
        {licenseExpiryWarning.message || 'Your license will expire soon. Please contact Sonatype to renew your license.'}
      </SettingsAlert>
    </Box>
  );
}

export default LicenseExpiryAlert;
