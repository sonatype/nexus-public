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

/**
 * SSL Certificate data model matching the backend API
 */
export interface SslCertificate {
  id: string;
  subjectCommonName: string;
  subjectOrganization: string;
  subjectOrganizationalUnit?: string;
  issuerCommonName: string;
  issuerOrganization: string;
  issuerOrganizationalUnit?: string;
  fingerprint: string;
  issuedOn?: number; // Timestamp
  expiresOn?: number; // Timestamp
  inTrustStore?: boolean;
  pem?: string; // PEM content (for adding)
}

/**
 * Certificate source type for adding certificates
 */
export type CertificateSource = 'remoteHost' | 'PEM';

/**
 * Add certificate form data
 */
export interface AddCertificateFormData {
  source: CertificateSource;
  remoteHostUrl: string;
  pemContent: string;
}

/**
 * Form validation errors
 */
export interface CertificateFormErrors {
  remoteHostUrl?: string;
  pemContent?: string;
}

/**
 * Sort direction type
 */
export type SortDirection = 'asc' | 'desc' | null;

/**
 * Sortable fields for certificates list
 */
export type CertificateSortField = 'subjectCommonName' | 'subjectOrganization' | 'issuerOrganization' | 'fingerprint';

/**
 * Props for SslCertificatesPage component
 */
export interface SslCertificatesPageProps {
  className?: string;
}

/**
 * Props for SslCertificatesList component
 */
export interface SslCertificatesListProps {
  onSelect: (certificateId: string) => void;
  onCreate: () => void;
}

/**
 * Props for SslCertificatesDetail component
 */
export interface SslCertificatesDetailProps {
  certificate: SslCertificate | null;
  loading: boolean;
  canDelete: boolean;
  onDelete: () => void;
  onCancel: () => void;
  error?: string;
}

/**
 * Props for SslCertificatesAddForm component
 */
export interface SslCertificatesAddFormProps {
  onSave: (data: AddCertificateFormData) => Promise<void>;
  onCancel: () => void;
  loading?: boolean;
  error?: string;
}

/**
 * Certificate source options
 */
export const CERTIFICATE_SOURCES = {
  REMOTE_HOST: 'remoteHost' as const,
  PEM: 'PEM' as const,
} as const;

/**
 * Format timestamp to readable date string
 */
export const formatDate = (timestamp?: number): string => {
  if (!timestamp) return '';
  return new Date(timestamp).toLocaleString();
};

/**
 * Check if certificate is expired or expiring soon
 */
export const isCertificateExpiring = (certificate: SslCertificate, daysThreshold: number = 30): boolean => {
  if (!certificate.expiresOn) return false;
  const now = Date.now();
  const threshold = daysThreshold * 24 * 60 * 60 * 1000;
  return certificate.expiresOn - now < threshold;
};

/**
 * Check if certificate is expired
 */
export const isCertificateExpired = (certificate: SslCertificate): boolean => {
  if (!certificate.expiresOn) return false;
  return certificate.expiresOn < Date.now();
};


