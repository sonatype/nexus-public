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


const navigateTo = (path: string) => {
  window.location.hash = path;
}


import React, { useState, useEffect, useCallback } from 'react';
import { Box, Flex, Text, Heading, Tabs } from '@radix-ui/themes';
import { Wallet, Loader2 } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import HistoricalUsage from '../../../../../pages/admin/Usage/HistoricalUsage';
import { historicalUsageColumns } from '../../../../../pages/admin/Usage/HistoricalUsageColumns';

import { SettingsAlert } from '../../../../shared/form';
import { LicenseDetails } from './LicenseDetails';
import { LicensedUsage } from './LicensedUsage';
import { InstallLicense } from './InstallLicense';
import { useLicensingApi } from './useLicensingApi';
import { LicenseData } from './types';

import './LicensingPage.scss';

/**
 * LicensingPage - Main Licensing management page for Preview UI
 *
 * Displays license information, allows uploading new licenses, and shows historical usage.
 */
export function LicensingPage() {
  const [license, setLicense] = useState<LicenseData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('license');

  const { fetchLicense, setError: setApiError } = useLicensingApi();

  const canViewHistoricalUsage = ExtJS.checkPermission('nexus:metrics:read');

  // Required columns for Historical Usage
  const requiredColumns = [
    historicalUsageColumns.metricDateMonth,
    historicalUsageColumns.peakComponents,
    historicalUsageColumns.percentageChangeComponent,
    historicalUsageColumns.totalRequests,
    historicalUsageColumns.percentageChangeRequests,
    historicalUsageColumns.totalEgress,
    historicalUsageColumns.peakStorage,
  ];

  // Load license data on mount
  useEffect(() => {
    const loadLicense = async () => {
      setLoading(true);
      setError(null);
      try {
        const licenseData = await fetchLicense();
        setLicense(licenseData);
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadLicense();
  }, [fetchLicense]);

  // Handle license installed - refresh data
  const handleLicenseInstalled = useCallback(async () => {
    try {
      const licenseData = await fetchLicense();
      setLicense(licenseData);
    } catch (err: any) {
      setError(err.message);
    }
  }, [fetchLicense]);

  // Loading state
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
  const showLicensedUsage = !error && license?.maxRepoRequests && license?.maxRepoComponents;

  return (
    <Box className="licensing-page">
      {/* Header */}
      <Flex align="center" gap="3" className="licensing-page__header">
        <Wallet size={24} className="licensing-page__icon" />
        <Box>
          <Heading as="h1" size="6" weight="medium">Licensing</Heading>
          <Text size="2" className="licensing-page__description">
            A valid license is required for PRO features; manage it here
          </Text>
        </Box>
      </Flex>

      {/* Error Alert */}
      {error && (
        <Box className="licensing-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="licensing-page__content">
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
              <Box className="licensing-page__historical-usage">
                <HistoricalUsage columns={requiredColumns} />
              </Box>
            </Tabs.Content>
          )}
        </Tabs.Root>
      </Box>
    </Box>
  );
}

export default LicensingPage;


