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
import { Box, Flex, Text, } from '@radix-ui/themes';
import { Loader2, AlertTriangle, Trash2 } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsButton,
  SettingsAlert,
} from '../../../../shared/form';
import {
  SslCertificatesDetailProps,
  formatDate,
  isCertificateExpired,
  isCertificateExpiring,
} from './types';

import './SslCertificatesDetail.scss';

/**
 * SslCertificatesDetail - Detailed view of a single SSL certificate
 */
export function SslCertificatesDetail({
  certificate,
  loading,
  canDelete,
  onDelete,
  onCancel,
  error,
}: SslCertificatesDetailProps) {
  // Loading state
  if (loading) {
    return (
      <Flex align="center" justify="center" className="ssl-certificates-detail__loading">
        <Loader2 size={24} className="ssl-certificates-detail__spinner" />
        <Text size="2">Loading certificate details...</Text>
      </Flex>
    );
  }

  // No certificate found
  if (!certificate) {
    return (
      <Box className="ssl-certificates-detail__not-found">
        <AlertTriangle size={24} />
        <Text size="2">Certificate not found</Text>
      </Box>
    );
  }

  const isExpired = isCertificateExpired(certificate);
  const isExpiring = !isExpired && isCertificateExpiring(certificate);
  const issuedDate = formatDate(certificate.issuedOn);
  const expiresDate = formatDate(certificate.expiresOn);

  const deleteButton = canDelete ? (
    <SettingsButton variant="danger" onClick={onDelete} icon={Trash2}>
      Delete Certificate
    </SettingsButton>
  ) : undefined;

  return (
    <Box className="ssl-certificates-detail">
      <SettingsForm
        showHeader={false}
        showActions={true}
        onCancel={onCancel}
        cancelLabel="Back to List"
        footerExtra={deleteButton}
        noDirtyTracking
      >
        {/* Error Alert */}
        {error && (
          <Box className="ssl-certificates-detail__alerts">
            <SettingsAlert type="error" onClose={() => {}}>
              {error}
            </SettingsAlert>
          </Box>
        )}

        {/* Expiration Warnings */}
        {isExpired && (
          <Box className="ssl-certificates-detail__alerts">
            <SettingsAlert type="error">
              <Flex align="center" gap="2">
                <AlertTriangle size={16} />
                <Text size="2">This certificate has expired</Text>
              </Flex>
            </SettingsAlert>
          </Box>
        )}
        {isExpiring && (
          <Box className="ssl-certificates-detail__alerts">
            <SettingsAlert type="warning">
              <Flex align="center" gap="2">
                <AlertTriangle size={16} />
                <Text size="2">This certificate is expiring soon</Text>
              </Flex>
            </SettingsAlert>
          </Box>
        )}

        {/* Certificate Information */}
        <SettingsFormSection title="Certificate Information" defaultOpen>
          <Box className="ssl-certificates-detail__info">
            <Flex className="ssl-certificates-detail__row">
              <Text size="2" weight="medium" className="ssl-certificates-detail__label">Fingerprint</Text>
              <Text size="1" className="ssl-certificates-detail__fingerprint">{certificate.fingerprint}</Text>
            </Flex>
            <Flex className="ssl-certificates-detail__row">
              <Text size="2" weight="medium" className="ssl-certificates-detail__label">Valid Until</Text>
              <Text size="2">{expiresDate || 'N/A'}</Text>
            </Flex>
            <Flex className="ssl-certificates-detail__row">
              <Text size="2" weight="medium" className="ssl-certificates-detail__label">Issued On</Text>
              <Text size="2">{issuedDate || 'N/A'}</Text>
            </Flex>
          </Box>
        </SettingsFormSection>

        {/* Subject Information */}
        <SettingsFormSection title="Subject" defaultOpen>
          <Box className="ssl-certificates-detail__info">
            <Flex className="ssl-certificates-detail__row">
              <Text size="2" weight="medium" className="ssl-certificates-detail__label">Common Name</Text>
              <Text size="2">{certificate.subjectCommonName || 'N/A'}</Text>
            </Flex>
            <Flex className="ssl-certificates-detail__row">
              <Text size="2" weight="medium" className="ssl-certificates-detail__label">Organization</Text>
              <Text size="2">{certificate.subjectOrganization || 'N/A'}</Text>
            </Flex>
            {certificate.subjectOrganizationalUnit && (
              <Flex className="ssl-certificates-detail__row">
                <Text size="2" weight="medium" className="ssl-certificates-detail__label">Organizational Unit</Text>
                <Text size="2">{certificate.subjectOrganizationalUnit}</Text>
              </Flex>
            )}
          </Box>
        </SettingsFormSection>

        {/* Issuer Information */}
        <SettingsFormSection title="Issuer" defaultOpen>
          <Box className="ssl-certificates-detail__info">
            <Flex className="ssl-certificates-detail__row">
              <Text size="2" weight="medium" className="ssl-certificates-detail__label">Common Name</Text>
              <Text size="2">{certificate.issuerCommonName || 'N/A'}</Text>
            </Flex>
            <Flex className="ssl-certificates-detail__row">
              <Text size="2" weight="medium" className="ssl-certificates-detail__label">Organization</Text>
              <Text size="2">{certificate.issuerOrganization || 'N/A'}</Text>
            </Flex>
            {certificate.issuerOrganizationalUnit && (
              <Flex className="ssl-certificates-detail__row">
                <Text size="2" weight="medium" className="ssl-certificates-detail__label">Organizational Unit</Text>
                <Text size="2">{certificate.issuerOrganizationalUnit}</Text>
              </Flex>
            )}
          </Box>
        </SettingsFormSection>

        {/* Warning */}
        <Box className="ssl-certificates-detail__warning">
          <SettingsAlert type="warning">
            <Text size="2">
              Warning: Adding a certificate to the trust store means you trust the certificate authority (CA) that issued it.
              Only add certificates from trusted sources.
            </Text>
          </SettingsAlert>
        </Box>
      </SettingsForm>
    </Box>
  );
}

export default SslCertificatesDetail;


