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
import * as Dialog from '@radix-ui/react-dialog';
import { Box, Flex, Text } from '@radix-ui/themes';
import { X, Loader2 } from 'lucide-react';
import Axios from 'axios';
import { ExtJS } from '@sonatype/nexus-ui-plugin';


import { SettingsButton, SettingsAlert } from '../../../../../shared/form';

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
      setError('Certificate inspection is only available for HTTPS URLs.');
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
      setError(e?.response?.data?.message || e?.message || 'Failed to retrieve certificate');
    } finally {
      setLoading(false);
    }
  }, [remoteUrl]);

  useEffect(() => {
    loadCertificate();
  }, [loadCertificate]);

  const handleAddToTrustStore = async () => {
    if (!cert?.pem || !canCreate) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await Axios.post(SSL_TRUSTSTORE_URL, cert.pem, { headers: { 'Content-Type': 'text/plain' } });
      setIsInTrustStore(true);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setActionError(e?.response?.data?.message || e?.message || 'Failed to add certificate to trust store');
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemoveFromTrustStore = async () => {
    if (!cert?.id || !canDelete) return;
    setActionLoading(true);
    setActionError(null);
    try {
      await Axios.delete(`${SSL_TRUSTSTORE_URL}/${encodeURIComponent(cert.id)}`);
      setIsInTrustStore(false);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } }; message?: string };
      setActionError(e?.response?.data?.message || e?.message || 'Failed to remove certificate from trust store');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <Dialog.Root open onOpenChange={(open) => !open && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="cert-view-dialog__overlay" />
        <Dialog.Content className="cert-view-dialog__content">
          <Flex justify="between" align="center" mb="4">
            <Dialog.Title asChild>
              <Text size="4" weight="bold">Certificate Details</Text>
            </Dialog.Title>
            <Dialog.Close asChild>
              <button type="button" className="cert-view-dialog__close" aria-label="Close">
                <X size={16} />
              </button>
            </Dialog.Close>
          </Flex>

          {loading && (
            <Flex justify="center" align="center" py="6" gap="2">
              <Loader2 size={20} style={{ animation: 'spin 1s linear infinite' }} />
              <Text size="2" color="gray">Retrieving certificate…</Text>
            </Flex>
          )}

          {!loading && error && (
            <Box>
              <SettingsAlert type="error">{error}</SettingsAlert>
              <Flex justify="end" mt="4" gap="2">
                <SettingsButton variant="secondary" onClick={loadCertificate}>Retry</SettingsButton>
                <Dialog.Close asChild>
                  <SettingsButton variant="secondary">Close</SettingsButton>
                </Dialog.Close>
              </Flex>
            </Box>
          )}

          {!loading && !error && cert && (
            <>
              <SettingsAlert type="warning">
                Importing a SSL certificate is a security-sensitive operation. Make sure you trust this certificate before adding it to the trust store.
              </SettingsAlert>

              <Box className="cert-view-dialog__details" mt="4">
                <Text size="1" weight="medium" color="gray" mb="2" as="div">Subject</Text>
                <CertRow label="Common Name" value={cert.subjectCommonName} />
                <CertRow label="Organization" value={cert.subjectOrganization} />
                <CertRow label="Organizational Unit" value={cert.subjectOrganizationalUnit} />

                <Text size="1" weight="medium" color="gray" mt="4" mb="2" as="div">Issuer</Text>
                <CertRow label="Common Name" value={cert.issuerCommonName} />
                <CertRow label="Organization" value={cert.issuerOrganization} />
                <CertRow label="Organizational Unit" value={cert.issuerOrganizationalUnit} />

                <Text size="1" weight="medium" color="gray" mt="4" mb="2" as="div">Validity</Text>
                <CertRow label="Issued" value={formatDate(cert.issuedOn)} />
                <CertRow label="Expires" value={formatDate(cert.expiresOn)} />

                <Text size="1" weight="medium" color="gray" mt="4" mb="2" as="div">Fingerprint</Text>
                <Box className="cert-view-dialog__fingerprint">
                  <Text size="1" style={{ wordBreak: 'break-all' }}>{cert.fingerprint}</Text>
                </Box>
              </Box>

              {actionError && (
                <Box mt="3">
                  <SettingsAlert type="error">{actionError}</SettingsAlert>
                </Box>
              )}

              <Flex justify="between" align="center" mt="4" gap="2">
                <Box>
                  {isInTrustStore === true && (
                    <Text size="1" color="green">Certificate is in the trust store</Text>
                  )}
                </Box>
                <Flex gap="2">
                  {isInTrustStore === false && cert.pem && (
                    <SettingsButton
                      variant="primary"
                      onClick={handleAddToTrustStore}
                      disabled={!canCreate}
                      loading={actionLoading}
                    >
                      Add to Trust Store
                    </SettingsButton>
                  )}
                  {isInTrustStore === true && cert.id && (
                    <SettingsButton
                      variant="danger"
                      onClick={handleRemoveFromTrustStore}
                      disabled={!canDelete}
                      loading={actionLoading}
                    >
                      Remove from Trust Store
                    </SettingsButton>
                  )}
                  <Dialog.Close asChild>
                    <SettingsButton variant="secondary">Close</SettingsButton>
                  </Dialog.Close>
                </Flex>
              </Flex>
            </>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

export default CertificateViewDialog;
