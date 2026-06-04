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

import Axios, { AxiosProgressEvent } from 'axios';
import { APIConstants } from '@sonatype/nexus-ui-plugin';

/**
 * Upload progress information.
 */
export interface UploadProgress {
  /** Bytes uploaded so far */
  loaded: number;
  /** Total bytes to upload (may be 0 if unknown) */
  total: number;
  /** Upload percentage (0-100) */
  percentage: number;
  /** Estimated time remaining in milliseconds (null if unknown) */
  estimatedTimeRemaining: number | null;
  /** Upload speed in bytes per second */
  bytesPerSecond: number;
}

/**
 * Upload response from the server.
 */
export interface UploadResponse {
  success: boolean;
  data?: string;
  message?: string;
}

/**
 * Parameters for upload API call.
 */
export interface UploadParams {
  /** Repository name to upload to */
  repositoryName: string;
  /** FormData containing files and fields */
  formData: FormData;
  /** Progress callback */
  onProgress?: (progress: UploadProgress) => void;
  /** Abort signal for cancellation */
  signal?: AbortSignal;
}

/**
 * Result of an upload operation.
 */
export interface UploadResult {
  success: boolean;
  componentName?: string;
  error?: string;
}

const UPLOAD_API_BASE = APIConstants.REST.INTERNAL.UPLOAD;

/**
 * Creates an UploadProgress object from axios progress event.
 */
function createProgressInfo(
  event: AxiosProgressEvent,
  startTime: number
): UploadProgress {
  const loaded = event.loaded || 0;
  const total = event.total || 0;
  const percentage = total > 0 ? Math.round((loaded / total) * 100) : 0;

  const elapsedMs = Date.now() - startTime;
  const bytesPerSecond = elapsedMs > 0 ? Math.round((loaded / elapsedMs) * 1000) : 0;

  let estimatedTimeRemaining: number | null = null;
  if (bytesPerSecond > 0 && total > 0) {
    const remainingBytes = total - loaded;
    estimatedTimeRemaining = Math.round((remainingBytes / bytesPerSecond) * 1000);
  }

  return {
    loaded,
    total,
    percentage,
    estimatedTimeRemaining,
    bytesPerSecond,
  };
}

/**
 * Upload files to a repository.
 *
 * @param params - Upload parameters
 * @returns Upload result with success status and component name or error
 *
 * @example
 * ```ts
 * const formData = new FormData();
 * formData.append('asset0', file);
 * formData.append('maven2.groupId', 'com.example');
 *
 * const result = await uploadToRepository({
 *   repositoryName: 'maven-hosted',
 *   formData,
 *   onProgress: (progress) => console.log(`${progress.percentage}%`),
 * });
 * ```
 */
export async function uploadToRepository({
  repositoryName,
  formData,
  onProgress,
  signal,
}: UploadParams): Promise<UploadResult> {
  const startTime = Date.now();
  const url = `${UPLOAD_API_BASE}${encodeURIComponent(repositoryName)}`;

  try {
    const response = await Axios.post<UploadResponse>(url, formData, {
      signal,
      onUploadProgress: onProgress
        ? (event) => onProgress(createProgressInfo(event, startTime))
        : undefined,
    });

    // The Nexus upload API returns { success: true, data: "component-name" } on success
    // Or { success: false } with error messages
    if (response.data?.success === true) {
      return {
        success: true,
        componentName: response.data.data,
      };
    }

    // Handle error response format: [{ message: "error" }]
    const errorMessage = Array.isArray(response.data)
      ? (response.data[0] as { message?: string })?.message
      : response.data?.message;

    return {
      success: false,
      error: errorMessage || 'Upload failed',
    };
  } catch (err: unknown) {
    // Check for cancellation (AbortController or Axios CancelToken)
    if (err instanceof Error && err.name === 'CanceledError') {
      return {
        success: false,
        error: 'Upload cancelled',
      };
    }

    // Check for Axios errors (has isAxiosError property)
    if (err && typeof err === 'object' && 'isAxiosError' in err) {
      const axiosErr = err as { response?: { data?: { message?: string; 0?: { message?: string } } }; message?: string };
      const message =
        axiosErr.response?.data?.message ||
        axiosErr.response?.data?.[0]?.message ||
        axiosErr.message ||
        'Network error';
      return {
        success: false,
        error: message,
      };
    }

    return {
      success: false,
      error: err instanceof Error ? err.message : 'Unknown error',
    };
  }
}

/**
 * Create an AbortController for cancelling uploads.
 */
export function createUploadAbortController(): AbortController {
  return new AbortController();
}

/**
 * Build FormData for upload from structured data.
 *
 * @param assets - Array of asset data (file + fields)
 * @param componentFields - Component-level fields
 * @param assetFieldNames - Names of asset-level fields to include
 * @param disabledFields - Set of field names to exclude
 * @returns FormData ready for upload
 */
export function buildUploadFormData(
  assets: Array<{
    file: File | null;
    [key: string]: string | boolean | File | null;
  }>,
  componentFields: Record<string, string | boolean>,
  assetFieldNames: string[],
  disabledFields: Set<string> = new Set()
): FormData {
  const formData = new FormData();

  // Add assets
  assets.forEach((asset, assetIndex) => {
    const assetKey = `asset${assetIndex}`;

    // Add file
    if (asset.file) {
      formData.append(assetKey, asset.file);
    }

    // Add asset fields
    assetFieldNames.forEach((fieldName) => {
      const value = asset[fieldName];
      if (value !== null && value !== undefined && value !== '') {
        formData.append(`${assetKey}.${fieldName}`, String(value));
      }
    });
  });

  // Add component fields (skip disabled)
  Object.entries(componentFields).forEach(([fieldName, value]) => {
    if (!disabledFields.has(fieldName) && value !== null && value !== undefined && value !== '') {
      formData.append(fieldName, String(value));
    }
  });

  return formData;
}

/**
 * Calculate total size of files in FormData.
 */
export function calculateFormDataSize(formData: FormData): number {
  let totalSize = 0;

  formData.forEach((value) => {
    if (value instanceof File) {
      totalSize += value.size;
    } else if (typeof value === 'string') {
      totalSize += new Blob([value]).size;
    }
  });

  return totalSize;
}

/**
 * Format bytes as human-readable string.
 */
export function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';

  const units = ['B', 'KB', 'MB', 'GB'];
  const k = 1024;
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  const size = bytes / Math.pow(k, i);

  return `${size.toFixed(i > 0 ? 1 : 0)} ${units[i]}`;
}

/**
 * Format milliseconds as human-readable time string.
 */
export function formatTime(ms: number): string {
  if (ms < 1000) return 'less than a second';

  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds} second${seconds !== 1 ? 's' : ''}`;

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;

  if (minutes < 60) {
    if (remainingSeconds > 0) {
      return `${minutes}m ${remainingSeconds}s`;
    }
    return `${minutes} minute${minutes !== 1 ? 's' : ''}`;
  }

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  return `${hours}h ${remainingMinutes}m`;
}

