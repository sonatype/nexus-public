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

import React, { useState, useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsAlert } from '../../../../shared/form';
import { PageHeader } from '../../../../shared';
import { SupportZipForm } from './SupportZipForm';
import { SupportZipResponse } from './SupportZipResponse';
import { SupportZipHA } from './SupportZipHA';
import { useSupportZipApi } from './useSupportZipApi';
import {
  SupportZipParams,
  SupportZipResponse as SupportZipResponseType,
  DEFAULT_SUPPORT_ZIP_PARAMS,
} from './types';

import './SupportZipPage.scss';

/**
 * SupportZipPage - Main Support ZIP generation page for Preview UI
 *
 * Handles both single-node and HA (clustered) support ZIP generation.
 *
 * Permission: nexus:atlas:create (to create ZIP), nexus:atlas:read (to view)
 * Route: #preview/admin/support/supportzip
 */
export function SupportZipPage() {
  const [params, setParams] = useState<SupportZipParams>(DEFAULT_SUPPORT_ZIP_PARAMS);
  const [response, setResponse] = useState<SupportZipResponseType | null>(null);

  const { createSupportZip, loading, error, setError } = useSupportZipApi();

  // Permission checks
  const canRead = ExtJS.checkPermission('nexus:atlas:read');
  const canCreate = ExtJS.checkPermission('nexus:atlas:create');

  // Check if clustered (HA) mode
  const isClustered = ExtJS.useState(() => ExtJS.state().getValue('nexus.datastore.clustered.enabled'));

  // Handle single param change
  const handleParamChange = useCallback(
    (name: keyof SupportZipParams, value: boolean | number) => {
      setParams((prev) => ({
        ...prev,
        [name]: value,
      }));
    },
    []
  );

  // Handle create support ZIP (single-node)
  const handleSubmit = useCallback(async () => {
    try {
      setError(null);
      setResponse(null);
      const result = await createSupportZip(params);
      setResponse(result);
    } catch (err) {
      // Error is handled by the hook
    }
  }, [params, createSupportZip, setError]);

  // Permission denied view
  if (!canRead) {
    return (
      <Box
        className="support-zip-page"
        data-testid="support-zip-page"
        data-permission="denied"
      >
        <PageHeader
          title="Support ZIP"
          description="Creates a ZIP file containing useful support information about your server"
          breadcrumbs={[
            { label: 'Settings', onClick: () => { window.location.hash = '#preview/admin/settings'; } },
            { label: 'Support ZIP' },
          ]}
        />

        <SettingsAlert type="warning" data-testid="support-zip-permission-warning">
          You do not have permission to view Support ZIP. Contact an administrator.
        </SettingsAlert>
      </Box>
    );
  }

  // Render header
  const renderHeader = () => (
    <PageHeader
      title="Support ZIP"
      description="Creates a ZIP file containing useful support information about your server"
      breadcrumbs={[
        { label: 'Settings', onClick: () => { window.location.hash = '#preview/admin/settings'; } },
        { label: 'Support ZIP' },
      ]}
    />
  );

  // Render content based on state
  const renderContent = () => {
    // Loading state
    if (loading) {
      return (
        <Box className="support-zip-page__loading" data-testid="support-zip-loading" aria-live="polite" aria-busy="true">
          <Flex align="center" justify="center" gap="3" py="9">
            <Loader2 size={24} className="support-zip-page__spinner" aria-hidden="true" />
            <Text size="3">Creating support ZIP...</Text>
          </Flex>
          <Text size="2" color="gray" align="center">
            This may take a few minutes to complete.
          </Text>
        </Box>
      );
    }

    // Single ZIP response
    if (response) {
      return <SupportZipResponse response={response} />;
    }

    // For clustered mode, show HA component (self-contained)
    if (isClustered) {
      return (
        <SupportZipHA
          params={params}
          onParamChange={handleParamChange}
          disabled={loading || !canCreate}
        />
      );
    }

    // Default: show form
    return (
      <SupportZipForm
        params={params}
        onParamChange={handleParamChange}
        onSubmit={handleSubmit}
        isHa={false}
        disabled={loading || !canCreate}
      />
    );
  };

  return (
    <Box
      className="support-zip-page"
      data-testid="support-zip-page"
      data-loading={loading ? 'true' : 'false'}
      data-clustered={isClustered ? 'true' : 'false'}
    >
      {renderHeader()}

      {/* Permission warning for create */}
      {!canCreate && (
        <Box className="support-zip-page__alerts">
          <SettingsAlert type="warning" data-testid="support-zip-create-permission-warning">
            You do not have permission to create Support ZIPs. You can view settings but not generate ZIPs.
          </SettingsAlert>
        </Box>
      )}

      {/* Error alert */}
      {error && (
        <Box className="support-zip-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)} data-testid="support-zip-error">
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="support-zip-page__content">
        {renderContent()}
      </Box>
    </Box>
  );
}

export default SupportZipPage;
