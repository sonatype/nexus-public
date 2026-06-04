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
  PREVIEW_UI_PREFIX,
  isPreviewUI,
  isGASearchContext,
  toPreviewSearchUrl,
  toDefaultSearchUrl,
  GA_SEARCH_VISIBILITY,
  GA_SEARCH_FEATURE_FLAGS,
} from '../search.flags';

describe('search.flags', () => {
  describe('PREVIEW_UI_PREFIX', () => {
    it('has correct value', () => {
      expect(PREVIEW_UI_PREFIX).toBe('preview.');
    });
  });

  describe('isPreviewUI', () => {
    it('returns true for routes starting with preview.', () => {
      expect(isPreviewUI('preview.browse.search.maven')).toBe(true);
      expect(isPreviewUI('preview.admin.settings')).toBe(true);
      expect(isPreviewUI('preview.browse')).toBe(true);
    });

    it('returns false for default UI routes', () => {
      expect(isPreviewUI('browse.search.maven')).toBe(false);
      expect(isPreviewUI('admin.settings')).toBe(false);
      expect(isPreviewUI('browse')).toBe(false);
    });

    it('returns false for empty string', () => {
      expect(isPreviewUI('')).toBe(false);
    });

    it('returns false for route containing but not starting with preview', () => {
      expect(isPreviewUI('admin.preview.settings')).toBe(false);
    });
  });

  describe('isGASearchContext', () => {
    it('returns true for preview maven search routes', () => {
      expect(isGASearchContext('preview.browse.search.maven')).toBe(true);
      expect(isGASearchContext('preview.browse.search.maven.detail')).toBe(true);
      expect(isGASearchContext('preview.browse.search.maven.results')).toBe(true);
    });

    it('returns false for non-maven preview search routes', () => {
      expect(isGASearchContext('preview.browse.search.npm')).toBe(false);
      expect(isGASearchContext('preview.browse.search')).toBe(false);
    });

    it('returns false for default UI maven search', () => {
      expect(isGASearchContext('browse.search.maven')).toBe(false);
    });

    it('returns false for other preview routes', () => {
      expect(isGASearchContext('preview.admin.settings')).toBe(false);
      expect(isGASearchContext('preview.browse')).toBe(false);
    });
  });

  describe('toPreviewSearchUrl', () => {
    it('converts default browse search URL to preview', () => {
      const result = toPreviewSearchUrl('#browse/search/maven');

      expect(result).toBe('#preview/browse/search/maven');
    });

    it('converts with query parameters', () => {
      const result = toPreviewSearchUrl('#browse/search/maven?q=test');

      expect(result).toBe('#preview/browse/search/maven?q=test');
    });

    it('converts npm search URL', () => {
      const result = toPreviewSearchUrl('#browse/search/npm');

      expect(result).toBe('#preview/browse/search/npm');
    });

    it('returns unchanged if not a browse search URL', () => {
      const adminUrl = '#admin/settings';
      expect(toPreviewSearchUrl(adminUrl)).toBe(adminUrl);

      const browseUrl = '#browse/welcome';
      expect(toPreviewSearchUrl(browseUrl)).toBe(browseUrl);
    });

    it('returns unchanged if already a preview URL', () => {
      const previewUrl = '#preview/browse/search/maven';
      expect(toPreviewSearchUrl(previewUrl)).toBe(previewUrl);
    });
  });

  describe('toDefaultSearchUrl', () => {
    it('converts preview browse search URL to default', () => {
      const result = toDefaultSearchUrl('#preview/browse/search/maven');

      expect(result).toBe('#browse/search/maven');
    });

    it('converts with query parameters', () => {
      const result = toDefaultSearchUrl('#preview/browse/search/maven?q=test');

      expect(result).toBe('#browse/search/maven?q=test');
    });

    it('converts npm search URL', () => {
      const result = toDefaultSearchUrl('#preview/browse/search/npm');

      expect(result).toBe('#browse/search/npm');
    });

    it('returns unchanged if not a preview search URL', () => {
      const adminUrl = '#admin/settings';
      expect(toDefaultSearchUrl(adminUrl)).toBe(adminUrl);

      const previewAdminUrl = '#preview/admin/settings';
      expect(toDefaultSearchUrl(previewAdminUrl)).toBe(previewAdminUrl);
    });

    it('returns unchanged if already a default URL', () => {
      const defaultUrl = '#browse/search/maven';
      expect(toDefaultSearchUrl(defaultUrl)).toBe(defaultUrl);
    });
  });

  describe('GA_SEARCH_VISIBILITY', () => {
    it('is an empty object (no special requirements)', () => {
      expect(GA_SEARCH_VISIBILITY).toEqual({});
    });
  });

  describe('GA_SEARCH_FEATURE_FLAGS', () => {
    it('is an empty object (reserved for future use)', () => {
      expect(GA_SEARCH_FEATURE_FLAGS).toEqual({});
    });
  });
});
