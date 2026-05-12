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
import { Box, Flex, Text, Card } from '@radix-ui/themes';
import { ExtJS, Permissions } from '@sonatype/nexus-ui-plugin';
import { FileDropzone } from '../../../upload/components/FileDropzone';

import { SettingsFormSection, SettingsButton, SettingsAlert } from '../../../shared/form';
import { LicenseAgreementModal } from './LicenseAgreementModal';
import { useLicensingApi } from './useLicensingApi';

import './InstallLicense.scss';

interface InstallLicenseProps {
  hasExistingLicense: boolean;
  onLicenseInstalled: () => void;
}

/**
 * InstallLicense - Form for uploading license file
 */
export function InstallLicense({ hasExistingLicense, onLicenseInstalled }: InstallLicenseProps) {
  const [files, setFiles] = useState<File[]>([]);
  const [showAgreementModal, setShowAgreementModal] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const { loading, error, setError, uploadLicense, getLicenseAgreementUrl } = useLicensingApi();

  const canEdit = ExtJS.checkPermission(Permissions.LICENSING.CREATE);
  const isValid = files.length > 0;

  // Handle file selection
  const handleFilesChange = useCallback((newFiles: File[]) => {
    setFiles(newFiles);
    setError(null);
    setSuccessMessage(null);
  }, [setError]);

  // Show agreement modal
  const handleShowAgreement = useCallback(() => {
    if (isValid && !loading) {
      setShowAgreementModal(true);
    }
  }, [isValid, loading]);

  // Handle accept agreement
  const handleAccept = useCallback(async () => {
    if (files.length === 0) return;

    setShowAgreementModal(false);
    setError(null);

    try {
      await uploadLicense(files[0]);
      setSuccessMessage('License installed. Restart is only required if you are enabling new PRO features.');
      setFiles([]);
      onLicenseInstalled();
    } catch {
      // Error is set by the API hook
    }
  }, [files, uploadLicense, onLicenseInstalled]);

  // Handle decline agreement
  const handleDecline = useCallback(() => {
    setShowAgreementModal(false);
  }, []);

  // Clear messages
  const handleDismissError = useCallback(() => {
    setError(null);
  }, [setError]);

  const handleDismissSuccess = useCallback(() => {
    setSuccessMessage(null);
  }, []);

  const licenseUrl = getLicenseAgreementUrl();

  return (
    <>
      <Card className="install-license">
        <Text as="h3" size="4" weight="medium" className="install-license__title">
          Install License
        </Text>

        {!canEdit && (
          <Box className="install-license__readonly">
            <SettingsAlert type="info">
              You do not have permission to install licenses.
            </SettingsAlert>
          </Box>
        )}

        {canEdit && (
          <>
            <Text as="p" size="2" className="install-license__description">
              {hasExistingLicense
                ? 'Restart is only required if the new license enables additional features'
                : 'Installing a new license requires restarting the server to take effect'}
            </Text>

            <SettingsFormSection title="">
              <Box className="install-license__upload">
                <FileDropzone
                  files={files}
                  onChange={handleFilesChange}
                  accept=".lic"
                  disabled={loading}
                  label="License"
                  required
                />
              </Box>
            </SettingsFormSection>

            {error && (
              <Box className="install-license__error">
                <SettingsAlert type="error" onClose={handleDismissError}>
                  Unable to update license for the reason identified below. Verify that you selected the correct file.
                  If the problem persists, contact our support team. Reason: {error}
                </SettingsAlert>
              </Box>
            )}

            {successMessage && (
              <Box className="install-license__success">
                <SettingsAlert type="success" onClose={handleDismissSuccess}>
                  {successMessage}
                </SettingsAlert>
              </Box>
            )}

            <Flex justify="end" className="install-license__actions">
              <SettingsButton
                variant="primary"
                onClick={handleShowAgreement}
                disabled={loading || !isValid || !!error}
              >
                Upload License
              </SettingsButton>
            </Flex>
          </>
        )}
      </Card>

      {showAgreementModal && licenseUrl && (
        <LicenseAgreementModal
          open={showAgreementModal}
          onOpenChange={setShowAgreementModal}
          licenseUrl={licenseUrl}
          onAccept={handleAccept}
          onDecline={handleDecline}
        />
      )}
    </>
  );
}

export default InstallLicense;

