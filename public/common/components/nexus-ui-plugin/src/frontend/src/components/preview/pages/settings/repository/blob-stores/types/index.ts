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

export type BlobStoreType = 'File' | 'S3' | 'Azure' | 'Google Cloud' | 'Group';

export interface BlobStore {
  name: string;
  type: BlobStoreType;
  typeId: string;
  path?: string;
  available: boolean;
  unavailable?: boolean;
  blobCount: number;
  totalSizeInBytes: number;
  availableSpaceInBytes: number;
  unlimited?: boolean;
}

export interface BlobStoreTypeDescriptor {
  id: string;
  name: string;
  fields?: BlobStoreField[];
}

export interface BlobStoreField {
  id: string;
  type: string;
  label: string;
  helpText?: string;
  required?: boolean;
  initialValue?: string | boolean | number;
  attributes?: Record<string, string>;
}

export interface QuotaType {
  id: string;
  name: string;
}

export interface SoftQuota {
  enabled: boolean;
  type?: string;
  limit?: number;
}

// File Blob Store specific types
export interface FileBlobStoreConfig {
  path: string;
}

// S3 Blob Store specific types
export interface S3BucketConfig {
  region: string;
  name: string;
  prefix?: string;
}

export interface S3SecurityConfig {
  accessKeyId?: string;
  secretAccessKey?: string;
  role?: string;
  sessionToken?: string;
}

export interface S3EncryptionConfig {
  encryptionType?: string;
  encryptionKey?: string;
}

export interface S3AdvancedConfig {
  endpoint?: string;
  maxConnectionPoolSize?: number;
  forcePathStyle?: boolean;
}

export interface S3FailoverBucket {
  region: string;
  bucketName: string;
}

export interface S3BlobStoreConfig {
  bucket: S3BucketConfig;
  bucketSecurity?: S3SecurityConfig;
  encryption?: S3EncryptionConfig;
  advancedBucketConnection?: S3AdvancedConfig;
  failoverBuckets?: S3FailoverBucket[];
  preSignedUrlEnabled?: boolean;
}

// Azure Blob Store specific types
export interface AzureAuthConfig {
  authenticationMethod: 'ENVIRONMENTVARIABLE' | 'MANAGEDIDENTITY' | 'ACCOUNTKEY';
  accountKey?: string;
}

export interface AzureBlobStoreConfig {
  accountName: string;
  containerName: string;
  authentication: AzureAuthConfig;
  preSignedUrlEnabled?: boolean;
}

// Google Cloud Blob Store specific types
export interface GoogleBucketConfig {
  projectId?: string;
  name: string;
  prefix?: string;
}

export interface GoogleSecurityConfig {
  authenticationMethod: 'applicationDefault' | 'accountKey';
  file?: File;
}

export interface GoogleEncryptionConfig {
  encryptionType: 'default' | 'kmsManagedEncryption';
  encryptionKey?: string;
}

export interface GoogleBlobStoreConfig {
  bucket: GoogleBucketConfig;
  bucketSecurity: GoogleSecurityConfig;
  encryption?: GoogleEncryptionConfig;
}

// Form data types
export interface BlobStoreFormData {
  name: string;
  type?: string;
  softQuota?: SoftQuota;
  // Type-specific configurations
  path?: string; // File
  bucketConfiguration?: S3BlobStoreConfig | AzureBlobStoreConfig | GoogleBlobStoreConfig;
  members?: string[]; // Group
  fillPolicy?: string; // Group
}

// API Response types
export interface BlobStoreListResponse {
  data: BlobStore[];
}

export interface BlobStoreTypesResponse {
  data: BlobStoreTypeDescriptor[];
}

export interface QuotaTypesResponse {
  data: QuotaType[];
}

// Dropdown values for S3
export interface S3DropdownValues {
  regions: Array<{ id: string; name: string }>;
  encryptionTypes: Array<{ id: string; name: string }>;
}

