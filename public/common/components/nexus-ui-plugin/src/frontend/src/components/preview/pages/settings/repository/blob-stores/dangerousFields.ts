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

import type { BlobStoreFormData } from './blobStoreFormMachine';

export interface DangerousFieldInfo {
  field: string;
  label: string;
}

const DANGEROUS_FIELDS: Record<string, DangerousFieldInfo[]> = {
  file: [
    { field: 'path', label: 'Path' },
  ],
  s3: [
    { field: 'bucketConfiguration.bucket.name', label: 'Bucket Name' },
    { field: 'bucketConfiguration.bucket.region', label: 'Region' },
    { field: 'bucketConfiguration.encryption', label: 'Encryption' },
  ],
  google: [
    { field: 'bucketConfiguration.bucket.name', label: 'Bucket Name' },
    { field: 'bucketConfiguration.bucket.projectId', label: 'Project ID' },
    { field: 'bucketConfiguration.bucket.region', label: 'Region' },
    { field: 'bucketConfiguration.encryption', label: 'Encryption' },
  ],
  azure: [
    { field: 'bucketConfiguration.accountName', label: 'Account Name' },
    { field: 'bucketConfiguration.containerName', label: 'Container Name' },
  ],
  group: [
    { field: 'members', label: 'Members' },
  ],
};

function getNestedValue(obj: Record<string, unknown>, path: string): unknown {
  return path.split('.').reduce<unknown>(
    (current, key) => (current != null && typeof current === 'object') ? (current as Record<string, unknown>)[key] : undefined,
    obj
  );
}

function valuesAreEqual(a: unknown, b: unknown): boolean {
  if (a === b) return true;
  if (a == null && b == null) return true;
  if (a == null || b == null) return false;

  if (Array.isArray(a) && Array.isArray(b)) {
    return JSON.stringify(a) === JSON.stringify(b);
  }

  if (typeof a === 'object' && typeof b === 'object') {
    return JSON.stringify(a) === JSON.stringify(b);
  }

  return false;
}

export function hasDangerousFieldChanges(
  pristineData: BlobStoreFormData,
  currentData: BlobStoreFormData,
  type: string
): boolean {
  const fields = DANGEROUS_FIELDS[type?.toLowerCase()] || [];
  return fields.some(({ field }) => {
    const pristineValue = getNestedValue(pristineData as unknown as Record<string, unknown>, field);
    const currentValue = getNestedValue(currentData as unknown as Record<string, unknown>, field);
    return !valuesAreEqual(pristineValue, currentValue);
  });
}

export function getDangerousFieldsChanged(
  pristineData: BlobStoreFormData,
  currentData: BlobStoreFormData,
  type: string
): DangerousFieldInfo[] {
  const fields = DANGEROUS_FIELDS[type?.toLowerCase()] || [];
  return fields.filter(({ field }) => {
    const pristineValue = getNestedValue(pristineData as unknown as Record<string, unknown>, field);
    const currentValue = getNestedValue(currentData as unknown as Record<string, unknown>, field);
    return !valuesAreEqual(pristineValue, currentValue);
  });
}
