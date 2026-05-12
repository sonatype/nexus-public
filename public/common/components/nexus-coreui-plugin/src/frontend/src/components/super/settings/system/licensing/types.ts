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
 * License data model from the backend API
 */
export interface LicenseData {
  contactCompany?: string;
  contactName?: string;
  contactEmail?: string;
  effectiveDate?: string;
  expirationDate?: string;
  licenseType?: string;
  licensedUsers?: number;
  fingerprint?: string;
  maxRepoRequests?: number;
  maxRepoComponents?: number;
}

/**
 * License upload form data
 */
export interface LicenseUploadData {
  files: File[];
}

/**
 * API URLs
 */
export const LICENSE_API = {
  BASE_URL: 'service/rest/internal/ui/license',
};

/**
 * Format date for display
 */
export const formatDate = (dateString?: string): string => {
  if (!dateString) return '—';
  try {
    const date = new Date(dateString);
    // Check if date is valid
    if (isNaN(date.getTime())) {
      return dateString;
    }
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  } catch {
    return dateString;
  }
};

/**
 * Parse license types from comma-separated string
 */
export const parseLicenseTypes = (licenseType?: string): string[] => {
  if (!licenseType) return [];
  return licenseType.split(',').map((type) => type.trim()).filter(Boolean);
};

/**
 * Read file as ArrayBuffer
 */
export const readFileAsArrayBuffer = (file: File): Promise<ArrayBuffer> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as ArrayBuffer);
    reader.onerror = reject;
    reader.readAsArrayBuffer(file);
  });
};

