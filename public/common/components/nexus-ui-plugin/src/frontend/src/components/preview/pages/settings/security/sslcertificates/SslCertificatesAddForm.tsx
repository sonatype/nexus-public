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
import { Box, Text } from '@radix-ui/themes';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsTextArea,
  SettingsAlert,
  ConfirmDialog,
} from '../../../../shared/form';
import { useSslForm } from './useSslForm';
import { useSslCertificatesApi } from './useSslCertificatesApi';
import {
  SslCertificatesAddFormProps,
  SslCertificate,
  CERTIFICATE_SOURCES,
} from './types';
import { SslCertificatesDetail } from './SslCertificatesDetail';

import './SslCertificatesAddForm.scss';

/**
 * SslCertificatesAddForm - Form for adding SSL certificates
 * Uses XState form machine for field state, validation, and dirty tracking.
 * Keeps the two-step (load -> preview -> add) flow using local state.
 */
export function SslCertificatesAddForm({
  onSave,
  onCancel,
  loading: externalLoading = false,
  error: externalError,
  onViewExisting,
}: SslCertificatesAddFormProps) {
  const { loading: apiLoading, error: apiError, setError, loadCertificateDetails } = useSslCertificatesApi();

  // XState form hook for field state, validation, dirty tracking, source change
  const sslForm = useSslForm({ onSave: undefined, onCancel });

  // Local state for the two-step preview flow
  const [certificateDetails, setCertificateDetails] = useState<SslCertificate | null>(null);
  const [showPreview, setShowPreview] = useState(false);
  // Raised as soon as the loaded certificate turns out to be trusted already, so the user is
  // offered the existing certificate instead of a dead-end error (Classic UI parity).
  const [duplicateDialogOpen, setDuplicateDialogOpen] = useState(false);

  const loading = externalLoading || apiLoading || sslForm.isSaving;
  const error = externalError || apiError || sslForm.saveError || undefined;

  const handleLoadDetails = useCallback(async () => {
    // Touch all relevant fields to trigger validation display
    sslForm.handleBlur('remoteHostUrl');
    sslForm.handleBlur('pemContent');

    if (sslForm.hasValidationErrors) {
      return;
    }

    try {
      const details = await loadCertificateDetails(
        sslForm.formData.source,
        sslForm.formData.remoteHostUrl,
        sslForm.formData.pemContent
      );
      setCertificateDetails(details);
      setShowPreview(true);
      setDuplicateDialogOpen(Boolean(details?.inTrustStore));
      setError(null);
    } catch (_err) {
      // Error is set by the API hook
    }
  }, [sslForm, loadCertificateDetails, setError]);

  const handleSubmit = useCallback(async () => {
    if (!certificateDetails) {
      await handleLoadDetails();
      return;
    }

    // Already trusted: re-offer the existing certificate rather than attempting an add that
    // the backend would reject with a 409.
    if (certificateDetails.inTrustStore) {
      setDuplicateDialogOpen(true);
      setError('This certificate already exists in the trust store and cannot be added again.');
      return;
    }

    // Submit with PEM content
    const pemContent = sslForm.formData.source === CERTIFICATE_SOURCES.PEM
      ? sslForm.formData.pemContent
      : certificateDetails.pem || '';

    if (!pemContent) {
      setError('Certificate PEM content is required');
      return;
    }

    await onSave({
      ...sslForm.formData,
      pemContent,
    });
  }, [certificateDetails, sslForm.formData, onSave, handleLoadDetails, setError]);

  const handleViewExisting = useCallback(() => {
    if (certificateDetails?.id) {
      onViewExisting?.(certificateDetails.id);
    }
  }, [certificateDetails, onViewExisting]);

  const handleCancel = useCallback(() => {
    if (showPreview) {
      setShowPreview(false);
      setCertificateDetails(null);
      setDuplicateDialogOpen(false);
    } else {
      onCancel();
    }
  }, [showPreview, onCancel]);

  const handleSourceChange = useCallback((source: typeof CERTIFICATE_SOURCES.REMOTE_HOST | typeof CERTIFICATE_SOURCES.PEM) => {
    sslForm.handleSourceChange(source);
    setCertificateDetails(null);
    setShowPreview(false);
    setDuplicateDialogOpen(false);
    setError(null);
  }, [sslForm, setError]);

  const handleFieldChange = useCallback((field: string, value: string) => {
    sslForm.handleChange(field, value);
    setError(null);
    setCertificateDetails(null);
    setShowPreview(false);
    setDuplicateDialogOpen(false);
  }, [sslForm, setError]);

  return (
    <Box className="ssl-certificates-add-form">
      <SettingsForm
        testId="ssl-add-form"
        title="Add SSL Certificate"
        description="Add a certificate to the trust store by loading from a server or pasting PEM content"
        onSubmit={handleSubmit}
        onCancel={handleCancel}
        loading={loading}
        pristine={sslForm.isPristine}
        error={error}
        submitLabel={showPreview ? 'Add Certificate' : 'Load Certificate'}
      >
        {!showPreview ? (
          <>
            {/* Source Selection */}
            <SettingsFormSection title="Certificate Source" defaultOpen>
              <Box className="ssl-certificates-add-form__source-options">
                <Box className="ssl-certificates-add-form__radio-group">
                  <label className="ssl-certificates-add-form__radio-label">
                    <input
                      type="radio"
                      name="source"
                      value={CERTIFICATE_SOURCES.REMOTE_HOST}
                      checked={sslForm.formData.source === CERTIFICATE_SOURCES.REMOTE_HOST}
                      onChange={() => handleSourceChange(CERTIFICATE_SOURCES.REMOTE_HOST)}
                      className="ssl-certificates-add-form__radio-input"
                    />
                    <Box className="ssl-certificates-add-form__radio-content">
                      <Text size="2" weight="medium">Load from server</Text>
                      <Text size="1" className="ssl-certificates-add-form__radio-description">
                        Enter a hostname, hostname:port, or URL to fetch an SSL certificate from
                      </Text>
                    </Box>
                  </label>

                  {sslForm.formData.source === CERTIFICATE_SOURCES.REMOTE_HOST && (
                    <Box className="ssl-certificates-add-form__field">
                      <SettingsTextInput
                        name="remoteHostUrl"
                        label="Hostname or URL"
                        value={sslForm.formData.remoteHostUrl}
                        onChange={(value: string) => handleFieldChange('remoteHostUrl', value)}
                        onBlur={() => sslForm.handleBlur('remoteHostUrl')}
                        error={sslForm.touched?.remoteHostUrl ? sslForm.errors?.remoteHostUrl : undefined}
                        placeholder="example.com or example.com:443 or https://example.com"
                        helpText="Remote server hostname to retrieve certificate from (e.g., repo.maven.apache.org)"
                        required
                      />
                    </Box>
                  )}
                </Box>

                <Box className="ssl-certificates-add-form__radio-group">
                  <label className="ssl-certificates-add-form__radio-label">
                    <input
                      type="radio"
                      name="source"
                      value={CERTIFICATE_SOURCES.PEM}
                      checked={sslForm.formData.source === CERTIFICATE_SOURCES.PEM}
                      onChange={() => handleSourceChange(CERTIFICATE_SOURCES.PEM)}
                      className="ssl-certificates-add-form__radio-input"
                    />
                    <Box className="ssl-certificates-add-form__radio-content">
                      <Text size="2" weight="medium">Paste PEM</Text>
                      <Text size="1" className="ssl-certificates-add-form__radio-description">
                        Paste certificate content in PEM format
                      </Text>
                    </Box>
                  </label>

                  {sslForm.formData.source === CERTIFICATE_SOURCES.PEM && (
                    <Box className="ssl-certificates-add-form__field">
                      <SettingsTextArea
                        name="pemContent"
                        label="Paste Certificate as PEM"
                        value={sslForm.formData.pemContent}
                        onChange={(value: string) => handleFieldChange('pemContent', value)}
                        onBlur={() => sslForm.handleBlur('pemContent')}
                        error={sslForm.touched?.pemContent ? sslForm.errors?.pemContent : undefined}
                        placeholder="-----BEGIN CERTIFICATE-----&#10;...&#10;-----END CERTIFICATE-----"
                        helpText="Paste PEM-encoded certificate content (starts with -----BEGIN CERTIFICATE-----)"
                        rows={8}
                        required
                      />
                    </Box>
                  )}
                </Box>
              </Box>
            </SettingsFormSection>
          </>
        ) : (
          <>
            {/* Certificate Preview */}
            {certificateDetails && (
              <>
                {certificateDetails.inTrustStore && (
                  <Box className="ssl-certificates-add-form__alerts">
                    <SettingsAlert type="error">
                      This certificate already exists in the trust store and cannot be added again.
                    </SettingsAlert>
                  </Box>
                )}
                {/* The embedded detail keeps its own action bar, so its cancel has to step back
                    through this form's two-step flow — preview -> form, then form -> list. Wiring it
                    to a no-op rendered a live but inert button (SettingsForm only omits the button
                    when onCancel is absent entirely). onDelete stays a no-op: canDelete is false,
                    so no delete button is rendered to reach it. */}
                <SslCertificatesDetail
                  certificate={certificateDetails}
                  loading={false}
                  canDelete={false}
                  onDelete={() => {}}
                  onCancel={handleCancel}
                  showTrustWarning
                />
              </>
            )}
          </>
        )}
      </SettingsForm>

      {/* Already-trusted certificate: offer the existing one instead of a dead-end error */}
      <ConfirmDialog
        open={duplicateDialogOpen}
        testId="certificate-already-exists-dialog"
        onOpenChange={setDuplicateDialogOpen}
        title="Certificate Already Exists"
        message="This certificate already exists and cannot be added again. Would you like to view the existing certificate?"
        confirmLabel="View Certificate"
        variant="warning"
        onConfirm={handleViewExisting}
      />
    </Box>
  );
}

export default SslCertificatesAddForm;
