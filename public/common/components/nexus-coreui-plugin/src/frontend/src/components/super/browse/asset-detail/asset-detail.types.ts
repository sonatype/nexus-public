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
 * Asset detail data from API.
 */
export interface AssetDetailData {
  id: string;
  name: string;
  path: string;
  format: string;
  contentType?: string;
  size?: number;
  blobCreated?: string;
  blobUpdated?: string;
  lastDownloaded?: string;
  locallyCached?: boolean;
  blobRef?: string;
  uploader?: string;
  uploaderIp?: string;
  downloadUrl?: string;
  checksum?: {
    sha1?: string;
    sha256?: string;
    sha512?: string;
    md5?: string;
  };
  attributes?: Record<string, unknown>;
}

/**
 * Component data associated with an asset.
 */
export interface ComponentDetailData {
  id: string;
  name: string;
  group?: string;
  version?: string;
  format: string;
  repository: string;
}

/**
 * Component tag.
 */
export interface ComponentTag {
  name: string;
  firstCreatedTime?: string;
  lastUpdatedTime?: string;
}

/**
 * Sonatype Lifecycle (IQ) component data.
 */
export interface LifecycleData {
  componentId?: string;
  policyViolations?: {
    critical: number;
    severe: number;
    moderate: number;
  };
  securityIssues?: {
    critical: number;
    severe: number;
    moderate: number;
  };
  licenseData?: {
    effectiveLicense?: string;
    declaredLicenses?: string[];
  };
  reportUrl?: string;
}

/**
 * Hook state for asset detail.
 */
export interface UseAssetDetailState {
  asset: AssetDetailData | null;
  component: ComponentDetailData | null;
  tags: ComponentTag[];
  lifecycle: LifecycleData | null;
  loading: boolean;
  tagsLoading: boolean;
  lifecycleLoading: boolean;
  error: string | null;
}

/**
 * Hook actions for asset detail.
 */
export interface UseAssetDetailActions {
  refetch: () => Promise<void>;
  addTag: (tagName: string) => Promise<void>;
  removeTag: (tagName: string) => Promise<void>;
}

/**
 * Hook params for asset detail.
 */
export interface UseAssetDetailParams {
  repositoryName: string;
  assetId: string;
  componentId?: string;
}


