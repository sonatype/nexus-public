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
 * Utility functions for the Browse detail panel components.
 */

/**
 * Formats a byte count into a human-readable file size string.
 * Uses binary units (1 KB = 1024 bytes).
 *
 * @param bytes - The number of bytes
 * @returns Formatted string (e.g., "1.5 MB", "256 KB")
 */
export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined || bytes < 0) {
    return '-';
  }

  if (bytes === 0) {
    return '0 B';
  }

  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
  const base = 1024;
  const unitIndex = Math.floor(Math.log(bytes) / Math.log(base));
  const clampedIndex = Math.min(unitIndex, units.length - 1);
  const value = bytes / Math.pow(base, clampedIndex);

  // Show decimals only for larger units
  const formatted = clampedIndex === 0 ? value.toString() : value.toFixed(2);

  return `${formatted} ${units[clampedIndex]}`;
}

/**
 * Formats an ISO date string into a full display format with timezone.
 * Matches the classic UI format: "Wed Apr 15 2026 11:10:29 GMT-0500 (Colombia Standard Time)"
 *
 * @param isoDate - ISO 8601 date string or null
 * @returns Formatted date string or '-' if null
 */
export function formatDate(isoDate: string | null | undefined): string {
  if (!isoDate) {
    return '-';
  }

  try {
    const date = new Date(isoDate);

    // Check for invalid date
    if (isNaN(date.getTime())) {
      return '-';
    }

    // Use toLocaleString for consistent, portable date formatting across browsers
    return date.toLocaleString(undefined, {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      timeZoneName: 'short',
    });
  } catch {
    return '-';
  }
}

/**
 * Formats a date for relative display (e.g., "2 days ago").
 *
 * @param isoDate - ISO 8601 date string or null
 * @returns Relative date string or '-' if null
 */
export function formatRelativeDate(isoDate: string | null | undefined): string {
  if (!isoDate) {
    return 'Never';
  }

  try {
    const date = new Date(isoDate);

    // Check for invalid date
    if (isNaN(date.getTime())) {
      return 'Never';
    }

    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);
    const diffHours = Math.floor(diffMinutes / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSeconds < 60) {
      return 'Just now';
    }
    if (diffMinutes < 60) {
      return `${diffMinutes} minute${diffMinutes === 1 ? '' : 's'} ago`;
    }
    if (diffHours < 24) {
      return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;
    }
    if (diffDays < 30) {
      return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;
    }

    // Fall back to formatted date for older dates
    return formatDate(isoDate);
  } catch {
    return 'Never';
  }
}

/**
 * Generates a download URL for an asset.
 *
 * @param repositoryName - Repository name
 * @param assetPath - Asset path within the repository
 * @returns Download URL
 */
export function getAssetDownloadUrl(repositoryName: string, assetPath: string): string {
  // Remove leading slash if present
  const cleanPath = assetPath.startsWith('/') ? assetPath.slice(1) : assetPath;
  return `/repository/${repositoryName}/${cleanPath}`;
}

/**
 * Truncates text with ellipsis if it exceeds the specified length.
 *
 * @param text - Text to truncate
 * @param maxLength - Maximum length before truncation
 * @returns Truncated text with ellipsis or original text
 */
export function truncateText(text: string | null | undefined, maxLength: number): string {
  if (!text) {
    return '-';
  }

  if (text.length <= maxLength) {
    return text;
  }

  return `${text.slice(0, maxLength - 3)}...`;
}

/**
 * Extracts the filename from a path.
 *
 * @param path - Full path
 * @returns Filename (last segment of path)
 */
export function getFilenameFromPath(path: string): string {
  if (!path) {
    return '';
  }

  // Remove trailing slash if present
  const cleanPath = path.endsWith('/') ? path.slice(0, -1) : path;
  
  if (!cleanPath) {
    return '';
  }

  const segments = cleanPath.split('/');
  return segments[segments.length - 1] || '';
}

/**
 * Checks if an asset is locally cached (not a placeholder).
 *
 * @param contentType - Asset content type
 * @param size - Asset size in bytes
 * @returns True if the asset has actual content
 */
export function isAssetCached(contentType: string | null | undefined, size: number | null | undefined): boolean {
  return contentType !== 'unknown' && size !== null && size !== undefined && size > 0;
}

/** Returns the registry URL if it matches a safe host[:port][/path] pattern, otherwise undefined. */
export function sanitizeRegistryUrl(value: string | null | undefined): string | undefined {
  if (typeof value !== 'string' || value.length === 0) {
    return undefined;
  }
  return /^[A-Za-z0-9.\-]+(?::\d+)?(?:\/[A-Za-z0-9._\-/]+)?$/.test(value) ? value : undefined;
}

/**
 * Gets the display text for the last downloaded date.
 *
 * @param lastDownloaded - ISO date string or null
 * @returns Human-readable string for last downloaded
 */
export function getLastDownloadedDisplay(lastDownloaded: string | null | undefined): string {
  if (!lastDownloaded) {
    return 'Never';
  }

  return formatRelativeDate(lastDownloaded);
}

