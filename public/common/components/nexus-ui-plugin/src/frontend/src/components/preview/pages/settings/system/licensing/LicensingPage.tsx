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
import { Box, Flex, Text, Tabs } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';

import { SettingsAlert } from '../../../../shared/form';
import { PageHeader } from '../../../../shared';
import { LicenseDetails } from './LicenseDetails';
import { LicensedUsage } from './LicensedUsage';
import { InstallLicense } from './InstallLicense';
import { LicenseExpiryAlert } from './LicenseExpiryAlert';
import { HistoricalUsagePreview } from './HistoricalUsagePreview';
import { useLicensing } from './useLicensing';

import './LicensingPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

export function LicensingPage() {
  const {
    license,
    loading,
    error,
    activeTab,
    setActiveTab,
    handleLicenseInstalled,
    dismissError,
    canViewHistoricalUsage,
  } = useLicensing();

  if (loading) {
    return (
      <Box className="licensing-page">
        <Flex align="center" justify="center" className="licensing-page__loading">
          <Loader2 size={24} className="licensing-page__spinner" />
          <Text size="2">Loading license information...</Text>
        </Flex>
      </Box>
    );
  }

  const showDetails = !error && license?.contactCompany;
  const showLicensedUsage =
    !error && license?.maxRepoRequests != null && license?.maxRepoComponents != null;

  return (
    <Box className="licensing-page">
      <PageHeader
        title="Licensing"
        description="A valid license is required for PRO features; manage it here"
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'Licensing' },
        ]}
      />

      {error && (
        <Box className="licensing-page__alerts">
          <SettingsAlert type="error" onClose={dismissError}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      <Box className="licensing-page__content">
        <LicenseExpiryAlert />
        <Tabs.Root value={activeTab} onValueChange={setActiveTab} className="licensing-page__tabs">
          <Tabs.List className="licensing-page__tab-list">
            <Tabs.Trigger value="license">License</Tabs.Trigger>
            {canViewHistoricalUsage && <Tabs.Trigger value="usage">Usage</Tabs.Trigger>}
          </Tabs.List>

          <Tabs.Content value="license" className="licensing-page__tab-content">
            {showDetails && license && <LicenseDetails license={license} />}
            {showLicensedUsage && license && (
              <LicensedUsage
                maxRepoRequests={license.maxRepoRequests.toLocaleString()}
                maxRepoComponents={license.maxRepoComponents.toLocaleString()}
              />
            )}
            <InstallLicense
              hasExistingLicense={!!license?.contactCompany}
              onLicenseInstalled={handleLicenseInstalled}
            />
          </Tabs.Content>

          {canViewHistoricalUsage && (
            <Tabs.Content value="usage" className="licensing-page__tab-content">
              <HistoricalUsagePreview />
            </Tabs.Content>
          )}
        </Tabs.Root>
      </Box>
    </Box>
  );
}

export default LicensingPage;
