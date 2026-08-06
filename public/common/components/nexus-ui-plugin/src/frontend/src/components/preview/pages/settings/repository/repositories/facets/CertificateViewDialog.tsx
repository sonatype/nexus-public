/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. All other trademarks are the property of their respective owners.
 */

import React, { useState, useEffect, useCallback } from 'react';
import { Dialog, Box, Flex, Text, Button } from '@radix-ui/themes';
import { X, Loader2 } from 'lucide-react';
import Axios from 'axios';
import { ExtJS } from '@sonatype/nexus-ui-plugin';


import { SettingsButton, SettingsAlert } from '../../../../../shared/form';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

import './CertificateViewDialog.scss';

interface CertificateDetails {
  id?: string;
  subjectCommonName?: string;
  subjectOrganization?: string;
  subjectOrganizationalUnit?: string;
  issuerCommonName?: string;
  issuerOrganization?: string;
  issuerOrganizationalUnit?: string;
  fingerprint?: string;
  issuedOn?: number;
  expiresOn?: number;
  pem?: string;
}

interface CertificateViewDialogProps {
  remoteUrl: string;
  onClose: () => void;
}

const SSL_RETRIEVE_URL = '/service/rest/v1/security/ssl';
const SSL_TRUSTSTORE_URL = '/service/rest/v1/security/ssl/truststore';

function formatDate(timestamp?: number): string {
  if (!timestamp) return '';
  return new Date(timestamp).toLocaleDateString(undefined, {
    year: 'numeric', month: 'long', day: 'numeric',
  });
}

function CertRow({ label, value }: { label: string; value?: string }) {
  return (
    <Flex className="cert-view-dialog__row" justify="between" gap="3">
      <Text size="2" weight="medium" className="cert-view-dialog__label">{label}</Text>
      <Text size="2" className="cert-view-dialog__value">{value || '—'}</Text>
    </Flex>
  );
}

export function CertificateViewDialog({ remoteUrl, onClose }: CertificateViewDialogProps) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cert, setCert] = useState<CertificateDetails | null>(null);
  const [isInTrustStore, setIsInTrustStore] = useState<boolean | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const canCreate = ExtJS.checkPermission('nexus:ssl-truststore:create');
  const canDelete = ExtJS.checkPermission('nexus:ssl-truststore:delete');

  const loadCertificate = useCallback(async () => {
    setLoading(true);
    setError(null);

    if (!remoteUrl.startsWith('https://')) {
      setError(UIStrings.CERTIFICATE.DIALOG.httpsOnlyError);
      setLoading(false);
      return;
    }

    try {
      const { hostname, port, protocol } = new URL(remoteUrl);
      const params = new URLSearchParams({ host: hostname });
      if (port) params.append('port', port);
      if (protocol) params.append('protocolHint', protocol);

      const [certResponse, truststoreResponse] = await Promise.all([
        Axios.get<CertificateDetails>(`${SSL_RETRIEVE_URL}?${params.toString()}`),
        Axios.get<CertificateDetails[]>(SSL_TRUSTSTORE_URL),
      ]);

      const fetchedCert = certResponse.data;
      setCert(fetchedCert);

      const trustedCerts = Array.isArray(truststoreResponse.data) ? truststoreResponse.data : [];
      const alreadyTrusted = trustedCerts.some((c) => c.fingerprint === fetchedCert.fingerprint);
      setIsInTrustStore(alreadyTrusted);
      if (alreadyTrusted && fetchedCert.id == null) {
        const match = trustedCerts.find((c) => c.fingerprint === fetchedCert.fingerprint);
        if (match) setCert({ ...fetchedCert, id: match.id });
      }
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setError(e?.response?.data?.message || e?.message || UIStrings.CERTIFICATE.ERRORS.fetchFailed);
    } finally {
      setLoading(false);
    }
  }, [remoteUrl]);

  useEffect(() => {
    loadCertificate();
  }, [loadCertificate]);

  const handleAddToTrustStore = async () => {
    if (!(cert?.pem && canCreate)) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await Axios.post(SSL_TRUSTSTORE_URL, cert.pem, { headers: { 'Content-Type': 'text/plain' } });
      setIsInTrustStore(true);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setActionError(e?.response?.data?.message || e?.message || UIStrings.CERTIFICATE.ERRORS.addFailed);
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemoveFromTrustStore = async () => {
    if (!(cert?.id && canDelete)) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await Axios.delete(`${SSL_TRUSTSTORE_URL}/${encodeURIComponent(cert.id)}`);
      setIsInTrustStore(false);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setActionError(e?.response?.data?.message || e?.message || UIStrings.CERTIFICATE.ERRORS.removeFailed);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <Dialog.Root open onOpenChange={(open) => !open && onClose()}>
      <Dialog.Content aria-describedby={undefined} style={{ maxWidth: 520 }}>
        <Flex justify="between" align="center" mb="4">
          <Dialog.Title size="4" mb="0">{UIStrings.CERTIFICATE.DIALOG.title}</Dialog.Title>
          <Dialog.Close>
            <Button variant="ghost" color="gray" size="1" aria-label={UIStrings.CERTIFICATE.ACTIONS.close}>
              <X size={16} />
            </Button>
          </Dialog.Close>
        </Flex>

        {loading && (
          <Flex justify="center" align="center" py="6" gap="2">
            <Loader2 size={20} style={{ animation: 'spin 1s linear infinite' }} />
            <Text size="2" color="gray">{UIStrings.CERTIFICATE.DIALOG.retrieving}</Text>
          </Flex>
        )}

        {!loading && error && (
          <Box>
            <SettingsAlert type="error">{error}</SettingsAlert>
            <Flex justify="end" mt="4">
              <Dialog.Close>
                <Button variant="soft" color="gray">{UIStrings.CERTIFICATE.ACTIONS.close}</Button>
              </Dialog.Close>
            </Flex>
          </Box>
        )}

        {!(loading || error ) && cert && (
          <>
            <SettingsAlert type="warning">
              {UIStrings.CERTIFICATE.DIALOG.untrustedWarning}
            </SettingsAlert>

            <Box className="cert-view-dialog__details" mt="4">
              <CertRow label={UIStrings.CERTIFICATE.FIELDS.commonName} value={cert.subjectCommonName} />
              <CertRow label={UIStrings.CERTIFICATE.FIELDS.organization} value={cert.subjectOrganization} />
              <CertRow label={UIStrings.CERTIFICATE.FIELDS.unit} value={cert.subjectOrganizationalUnit} />
              <CertRow label={UIStrings.CERTIFICATE.FIELDS.issuerCommonName} value={cert.issuerCommonName} />
              <CertRow label={UIStrings.CERTIFICATE.FIELDS.issuerOrganization} value={cert.issuerOrganization} />
              <CertRow label={UIStrings.CERTIFICATE.FIELDS.issuerUnit} value={cert.issuerOrganizationalUnit} />
              <CertRow label={UIStrings.CERTIFICATE.FIELDS.issuedOn} value={formatDate(cert.issuedOn)} />
              <CertRow label={UIStrings.CERTIFICATE.FIELDS.validUntil} value={formatDate(cert.expiresOn)} />
              <CertRow label={UIStrings.CERTIFICATE.FIELDS.fingerprint} value={cert.fingerprint} />
            </Box>

            {actionError && (
              <Box mt="3">
                <SettingsAlert type="error">{actionError}</SettingsAlert>
              </Box>
            )}

            <Flex justify="end" align="center" mt="4" gap="3">
              {isInTrustStore === false && cert.pem && (
                <SettingsButton
                  variant="primary"
                  onClick={handleAddToTrustStore}
                  disabled={!canCreate}
                  loading={actionLoading}
                >
                  {UIStrings.CERTIFICATE.ACTIONS.addToTrustStore}
                </SettingsButton>
              )}
              {isInTrustStore === true && cert.id && (
                <SettingsButton
                  variant="primary"
                  onClick={handleRemoveFromTrustStore}
                  disabled={!canDelete}
                  loading={actionLoading}
                >
                  {UIStrings.CERTIFICATE.ACTIONS.removeFromTrustStore}
                </SettingsButton>
              )}
              <Dialog.Close>
                <Button variant="soft" color="gray">{UIStrings.CERTIFICATE.ACTIONS.close}</Button>
              </Dialog.Close>
            </Flex>
          </>
        )}
      </Dialog.Content>
    </Dialog.Root>
  );
}

export default CertificateViewDialog;
