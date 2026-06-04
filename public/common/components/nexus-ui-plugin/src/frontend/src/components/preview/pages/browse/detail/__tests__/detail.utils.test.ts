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

import {
  formatFileSize,
  formatDate,
  formatRelativeDate,
  getAssetDownloadUrl,
  truncateText,
  getFilenameFromPath,
  isAssetCached,
  getLastDownloadedDisplay,
} from '../detail.utils';

describe('detail.utils', () => {
  describe('formatFileSize', () => {
    it('returns "-" for null', () => {
      expect(formatFileSize(null)).toBe('-');
    });

    it('returns "-" for undefined', () => {
      expect(formatFileSize(undefined)).toBe('-');
    });

    it('returns "-" for negative numbers', () => {
      expect(formatFileSize(-100)).toBe('-');
    });

    it('returns "0 B" for zero bytes', () => {
      expect(formatFileSize(0)).toBe('0 B');
    });

    it('formats bytes correctly', () => {
      expect(formatFileSize(100)).toBe('100 B');
      expect(formatFileSize(512)).toBe('512 B');
    });

    it('formats kilobytes correctly', () => {
      expect(formatFileSize(1024)).toBe('1.00 KB');
      expect(formatFileSize(1536)).toBe('1.50 KB');
      expect(formatFileSize(10240)).toBe('10.00 KB');
    });

    it('formats megabytes correctly', () => {
      expect(formatFileSize(1048576)).toBe('1.00 MB');
      expect(formatFileSize(1572864)).toBe('1.50 MB');
    });

    it('formats gigabytes correctly', () => {
      expect(formatFileSize(1073741824)).toBe('1.00 GB');
    });

    it('formats terabytes correctly', () => {
      expect(formatFileSize(1099511627776)).toBe('1.00 TB');
    });
  });

  describe('formatDate', () => {
    it('returns "-" for null', () => {
      expect(formatDate(null)).toBe('-');
    });

    it('returns "-" for undefined', () => {
      expect(formatDate(undefined)).toBe('-');
    });

    it('returns "-" for empty string', () => {
      expect(formatDate('')).toBe('-');
    });

    it('returns "-" for invalid date string', () => {
      expect(formatDate('not-a-date')).toBe('-');
    });

    it('formats valid ISO date string', () => {
      const result = formatDate('2024-01-15T10:30:00Z');
      // Check that it contains expected parts (format may vary by locale)
      expect(result).toMatch(/\d{4}/); // year
      expect(result).toMatch(/15/); // day
    });

    it('handles date with time zone', () => {
      const result = formatDate('2024-06-20T14:25:00-04:00');
      expect(result).not.toBe('-');
    });
  });

  describe('formatRelativeDate', () => {
    it('returns "Never" for null', () => {
      expect(formatRelativeDate(null)).toBe('Never');
    });

    it('returns "Never" for undefined', () => {
      expect(formatRelativeDate(undefined)).toBe('Never');
    });

    it('returns "Never" for empty string', () => {
      expect(formatRelativeDate('')).toBe('Never');
    });

    it('returns "Never" for invalid date', () => {
      expect(formatRelativeDate('invalid')).toBe('Never');
    });

    it('returns "Just now" for very recent dates', () => {
      const now = new Date();
      const result = formatRelativeDate(now.toISOString());
      expect(result).toBe('Just now');
    });

    it('returns minutes ago for recent dates', () => {
      const date = new Date();
      date.setMinutes(date.getMinutes() - 5);
      const result = formatRelativeDate(date.toISOString());
      expect(result).toBe('5 minutes ago');
    });

    it('returns singular minute for 1 minute ago', () => {
      const date = new Date();
      date.setMinutes(date.getMinutes() - 1);
      date.setSeconds(date.getSeconds() - 1); // Ensure we're past 60 seconds
      const result = formatRelativeDate(date.toISOString());
      expect(result).toBe('1 minute ago');
    });

    it('returns hours ago for dates within the day', () => {
      const date = new Date();
      date.setHours(date.getHours() - 3);
      const result = formatRelativeDate(date.toISOString());
      expect(result).toBe('3 hours ago');
    });

    it('returns days ago for dates within the month', () => {
      const date = new Date();
      date.setDate(date.getDate() - 5);
      const result = formatRelativeDate(date.toISOString());
      expect(result).toBe('5 days ago');
    });
  });

  describe('getAssetDownloadUrl', () => {
    it('generates correct URL without leading slash', () => {
      expect(getAssetDownloadUrl('maven-releases', 'org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar'))
        .toBe('/repository/maven-releases/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar');
    });

    it('removes leading slash from path', () => {
      expect(getAssetDownloadUrl('npm-hosted', '/lodash/-/lodash-4.17.21.tgz'))
        .toBe('/repository/npm-hosted/lodash/-/lodash-4.17.21.tgz');
    });

    it('handles empty path', () => {
      expect(getAssetDownloadUrl('raw-hosted', '')).toBe('/repository/raw-hosted/');
    });
  });

  describe('truncateText', () => {
    it('returns "-" for null', () => {
      expect(truncateText(null, 10)).toBe('-');
    });

    it('returns "-" for undefined', () => {
      expect(truncateText(undefined, 10)).toBe('-');
    });

    it('returns original text if shorter than maxLength', () => {
      expect(truncateText('short', 10)).toBe('short');
    });

    it('returns original text if equal to maxLength', () => {
      expect(truncateText('exactly10!', 10)).toBe('exactly10!');
    });

    it('truncates text with ellipsis if longer than maxLength', () => {
      expect(truncateText('this is a very long text', 10)).toBe('this is...');
    });
  });

  describe('getFilenameFromPath', () => {
    it('returns empty string for empty path', () => {
      expect(getFilenameFromPath('')).toBe('');
    });

    it('extracts filename from simple path', () => {
      expect(getFilenameFromPath('file.txt')).toBe('file.txt');
    });

    it('extracts filename from full path', () => {
      expect(getFilenameFromPath('/org/apache/commons/commons-lang3-3.14.0.jar'))
        .toBe('commons-lang3-3.14.0.jar');
    });

    it('handles path without leading slash', () => {
      expect(getFilenameFromPath('org/apache/file.jar')).toBe('file.jar');
    });

    it('handles path ending with slash', () => {
      // When path ends with /, return the last segment before the /
      expect(getFilenameFromPath('org/apache/')).toBe('apache');
    });

    it('returns empty string for root path', () => {
      expect(getFilenameFromPath('/')).toBe('');
    });
  });

  describe('isAssetCached', () => {
    it('returns false for unknown content type', () => {
      expect(isAssetCached('unknown', 100)).toBe(false);
    });

    it('returns false for null size', () => {
      expect(isAssetCached('application/java-archive', null)).toBe(false);
    });

    it('returns false for undefined size', () => {
      expect(isAssetCached('application/java-archive', undefined)).toBe(false);
    });

    it('returns false for zero size', () => {
      expect(isAssetCached('application/java-archive', 0)).toBe(false);
    });

    it('returns true for valid content type and positive size', () => {
      expect(isAssetCached('application/java-archive', 100)).toBe(true);
    });

    it('returns true for null content type with positive size', () => {
      expect(isAssetCached(null, 100)).toBe(true);
    });
  });

  describe('getLastDownloadedDisplay', () => {
    it('returns "Never" for null', () => {
      expect(getLastDownloadedDisplay(null)).toBe('Never');
    });

    it('returns "Never" for undefined', () => {
      expect(getLastDownloadedDisplay(undefined)).toBe('Never');
    });

    it('returns formatted relative date for valid date', () => {
      const date = new Date();
      date.setHours(date.getHours() - 2);
      const result = getLastDownloadedDisplay(date.toISOString());
      expect(result).toBe('2 hours ago');
    });
  });
});

