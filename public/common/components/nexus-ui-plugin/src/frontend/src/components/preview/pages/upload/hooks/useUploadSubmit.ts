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

import { useState, useCallback, useRef, useMemo } from 'react';
import { useToast } from '../../../shared';

import {
  uploadToRepository,
  buildUploadFormData,
  calculateFormDataSize,
  formatBytes,
  formatTime,
  type UploadProgress,
  type UploadResult,
} from '../upload.api';
import type { AssetFieldData, UploadFieldDefinition, UploadComponentField } from '../upload.types';

/**
 * Parameters for the upload submission.
 */
export interface UploadSubmitParams {
  /** Repository name to upload to */
  repositoryName: string;
  /** Asset data with files and metadata */
  assets: AssetFieldData[];
  /** Component-level fields */
  componentFields: Record<string, string | boolean>;
  /** Asset field definitions (for extracting field names) */
  assetFieldDefs: UploadFieldDefinition[];
  /** Component field definitions (optional, for skipping disabled) */
  componentFieldDefs?: UploadComponentField[];
  /** Set of field names to exclude from upload */
  disabledFields?: Set<string>;
}

/**
 * Options for useUploadSubmit hook.
 */
export interface UseUploadSubmitOptions {
  /** Maximum retry attempts (default: 3) */
  maxRetries?: number;
  /** Delay between retries in ms (default: 1000) */
  retryDelay?: number;
  /** Called when upload succeeds */
  onSuccess?: (componentName: string) => void;
  /** Called when upload fails */
  onError?: (error: string) => void;
  /** Called when upload progress updates */
  onProgress?: (progress: UploadProgress) => void;
}

/**
 * Result returned by useUploadSubmit hook.
 */
export interface UseUploadSubmitResult {
  /** Submit upload with parameters */
  upload: (params: UploadSubmitParams) => Promise<UploadResult>;
  /** Current upload progress (null if not uploading) */
  progress: UploadProgress | null;
  /** Whether upload is in progress */
  uploading: boolean;
  /** Error message (null if no error) */
  error: string | null;
  /** Number of retry attempts made */
  retryCount: number;
  /** Cancel the current upload */
  cancel: () => void;
  /** Reset state (clear error, progress) */
  reset: () => void;
  /** Retry the last failed upload */
  retry: () => Promise<UploadResult | null>;
  /** Formatted progress info for display */
  progressDisplay: {
    percentage: number;
    loaded: string;
    total: string;
    speed: string;
    timeRemaining: string;
  } | null;
}

const DEFAULT_MAX_RETRIES = 3;
const DEFAULT_RETRY_DELAY = 1000;

/**
 * UI strings for upload submission.
 */
export const UPLOAD_SUBMIT_STRINGS = {
  uploading: 'Uploading...',
  uploadSuccess: 'Upload successful!',
  uploadFailed: 'Upload failed',
  uploadCancelled: 'Upload cancelled',
  retrying: 'Retrying upload...',
  calculating: 'Calculating...',
  unknown: 'Unknown',
};

/**
 * Hook for handling file upload submission with progress tracking, retry logic, and cancellation.
 *
 * Features:
 * - Progress tracking with percentage, speed, and ETA
 * - Automatic retry on failure (configurable)
 * - Upload cancellation support
 * - Success/error callbacks
 * - Formatted progress display strings
 *
 * @example
 * ```tsx
 * const { upload, progress, uploading, error, cancel } = useUploadSubmit({
 *   onSuccess: (name) => navigate(`/search?q=${name}`),
 *   onError: (err) => console.error(err),
 * });
 *
 * const handleSubmit = async () => {
 *   await upload({
 *     repositoryName: 'maven-hosted',
 *     assets: [{ file: myFile, extension: 'jar' }],
 *     componentFields: { 'maven2.groupId': 'com.example' },
 *     assetFieldDefs: [{ name: 'extension', type: 'STRING' }],
 *   });
 * };
 *
 * // In JSX:
 * {uploading && <ProgressBar value={progress?.percentage} />}
 * ```
 */
export function useUploadSubmit(options: UseUploadSubmitOptions = {}): UseUploadSubmitResult {
  const {
    maxRetries = DEFAULT_MAX_RETRIES,
    retryDelay = DEFAULT_RETRY_DELAY,
    onSuccess,
    onError,
    onProgress,
  } = options;

  const toast = useToast();

  // State
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState<UploadProgress | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);

  // Refs for cancellation and retry
  const abortControllerRef = useRef<AbortController | null>(null);
  const lastParamsRef = useRef<UploadSubmitParams | null>(null);

  /**
   * Reset all state.
   */
  const reset = useCallback(() => {
    setProgress(null);
    setError(null);
    setRetryCount(0);
    lastParamsRef.current = null;
  }, []);

  /**
   * Cancel the current upload.
   */
  const cancel = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setUploading(false);
    setProgress(null);
    setError(UPLOAD_SUBMIT_STRINGS.uploadCancelled);
  }, []);

  /**
   * Handle progress updates.
   */
  const handleProgress = useCallback(
    (progressInfo: UploadProgress) => {
      setProgress(progressInfo);
      onProgress?.(progressInfo);
    },
    [onProgress]
  );

  /**
   * Execute the upload with optional retry logic.
   */
  const executeUpload = useCallback(
    async (params: UploadSubmitParams, attempt: number = 1): Promise<UploadResult> => {
      const {
        repositoryName,
        assets,
        componentFields,
        assetFieldDefs,
        disabledFields = new Set(),
      } = params;

      // Build FormData
      const assetFieldNames = assetFieldDefs.map((f) => f.name);
      const formData = buildUploadFormData(assets, componentFields, assetFieldNames, disabledFields);

      // Create abort controller
      abortControllerRef.current = new AbortController();

      try {
        const result = await uploadToRepository({
          repositoryName,
          formData,
          onProgress: handleProgress,
          signal: abortControllerRef.current.signal,
        });

        if (result.success) {
          setUploading(false);
          setProgress({ loaded: 100, total: 100, percentage: 100, estimatedTimeRemaining: 0, bytesPerSecond: 0 });
          setError(null);
          const componentLabel = result.componentName ? `"${result.componentName}" ` : '';
          toast.success(`Component ${componentLabel}uploaded to "${repositoryName}" successfully`);
          onSuccess?.(result.componentName || '');
          return result;
        }

        // Upload failed
        if (attempt < maxRetries) {
          setRetryCount(attempt);
          toast.warning(`${UPLOAD_SUBMIT_STRINGS.retrying} (attempt ${attempt + 1}/${maxRetries})`);

          // Wait before retry
          await new Promise((resolve) => setTimeout(resolve, retryDelay));

          // Retry if not cancelled
          if (abortControllerRef.current && !abortControllerRef.current.signal.aborted) {
            return executeUpload(params, attempt + 1);
          }
        }

        // All retries exhausted
        setUploading(false);
        setError(result.error || UPLOAD_SUBMIT_STRINGS.uploadFailed);
        toast.error(`${UPLOAD_SUBMIT_STRINGS.uploadFailed}: ${result.error}`);
        onError?.(result.error || UPLOAD_SUBMIT_STRINGS.uploadFailed);
        return result;
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Unknown error';
        setUploading(false);
        setError(errorMessage);
        onError?.(errorMessage);
        return { success: false, error: errorMessage };
      } finally {
        abortControllerRef.current = null;
      }
    },
    [handleProgress, maxRetries, retryDelay, onSuccess, onError]
  );

  /**
   * Start an upload.
   */
  const upload = useCallback(
    async (params: UploadSubmitParams): Promise<UploadResult> => {
      // Cancel any existing upload
      cancel();

      // Reset state
      setUploading(true);
      setProgress(null);
      setError(null);
      setRetryCount(0);

      // Store params for retry
      lastParamsRef.current = params;

      return executeUpload(params);
    },
    [cancel, executeUpload]
  );

  /**
   * Retry the last failed upload.
   */
  const retry = useCallback(async (): Promise<UploadResult | null> => {
    if (!lastParamsRef.current) {
      return null;
    }

    setUploading(true);
    setProgress(null);
    setError(null);

    return executeUpload(lastParamsRef.current);
  }, [executeUpload]);

  /**
   * Format progress for display.
   */
  const progressDisplay = useMemo(() => {
    if (!progress) return null;

    return {
      percentage: progress.percentage,
      loaded: formatBytes(progress.loaded),
      total: progress.total > 0 ? formatBytes(progress.total) : UPLOAD_SUBMIT_STRINGS.unknown,
      speed: progress.bytesPerSecond > 0 ? `${formatBytes(progress.bytesPerSecond)}/s` : UPLOAD_SUBMIT_STRINGS.calculating,
      timeRemaining:
        progress.estimatedTimeRemaining !== null
          ? formatTime(progress.estimatedTimeRemaining)
          : UPLOAD_SUBMIT_STRINGS.calculating,
    };
  }, [progress]);

  return {
    upload,
    progress,
    uploading,
    error,
    retryCount,
    cancel,
    reset,
    retry,
    progressDisplay,
  };
}

export default useUploadSubmit;

