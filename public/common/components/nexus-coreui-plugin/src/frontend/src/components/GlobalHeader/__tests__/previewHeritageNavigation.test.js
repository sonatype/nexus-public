/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {
  getHeritageEquivalent,
  heritageToPreviewPath,
  previewBrowsePathToHeritageBrowseParam,
} from '../previewHeritageNavigation';

describe('getHeritageEquivalent', () => {
  describe('page-level routes (no entity parameter)', () => {
    it('maps browse/welcome', () => {
      expect(getHeritageEquivalent('preview/browse/welcome')).toBe('browse/welcome');
    });

    it('maps repository list page', () => {
      expect(getHeritageEquivalent('preview/admin/repository/repositories')).toBe('admin/repository/repositories');
    });

    it('maps cleanup policies page', () => {
      expect(getHeritageEquivalent('preview/admin/repository/cleanup-policies')).toBe('admin/repository/cleanuppolicies');
    });

    it('maps security pages', () => {
      expect(getHeritageEquivalent('preview/admin/security/privileges')).toBe('admin/security/privileges');
      expect(getHeritageEquivalent('preview/admin/security/roles')).toBe('admin/security/roles');
      expect(getHeritageEquivalent('preview/admin/security/users')).toBe('admin/security/users');
      expect(getHeritageEquivalent('preview/admin/security/anonymous')).toBe('admin/security/anonymous');
    });

    it('maps system pages', () => {
      expect(getHeritageEquivalent('preview/admin/system/tasks')).toBe('admin/system/tasks');
      expect(getHeritageEquivalent('preview/admin/system/capabilities')).toBe('admin/system/capabilities');
      expect(getHeritageEquivalent('preview/admin/system/http')).toBe('admin/system/http');
    });

    it('maps IQ server page', () => {
      expect(getHeritageEquivalent('preview/admin/iq')).toBe('admin/iq');
    });

    it('maps metric health to Classic status', () => {
      expect(getHeritageEquivalent('preview/admin/support/metrichealth')).toBe('admin/support/status');
    });

    it('maps Crowd with different Classic name', () => {
      expect(getHeritageEquivalent('preview/admin/security/crowd')).toBe('admin/security/atlassiancrowd');
    });
  });

  describe('entity deep links (colon separator in Classic)', () => {
    it('maps repository entity to colon-separated Classic URL', () => {
      expect(getHeritageEquivalent('preview/admin/repository/repositories/npm-proxy-central'))
        .toBe('admin/repository/repositories:npm-proxy-central');
    });

    it('maps blob store entity to colon-separated Classic URL', () => {
      expect(getHeritageEquivalent('preview/admin/repository/blobstores/default'))
        .toBe('admin/repository/blobstores:default');
    });

    it('handles URL-encoded entity names', () => {
      expect(getHeritageEquivalent('preview/admin/repository/repositories/Maven%20Central'))
        .toBe('admin/repository/repositories:Maven%20Central');
    });

    it('handles entity names with special characters', () => {
      expect(getHeritageEquivalent('preview/admin/repository/repositories/my-repo_v2'))
        .toBe('admin/repository/repositories:my-repo_v2');
    });
  });

  describe('query string preservation', () => {
    it('preserves query strings on page-level routes', () => {
      expect(getHeritageEquivalent('preview/browse/welcome?tab=overview'))
        .toBe('browse/welcome?tab=overview');
    });

    it('preserves query strings on entity deep links', () => {
      expect(getHeritageEquivalent('preview/admin/repository/repositories/my-repo?tab=config'))
        .toBe('admin/repository/repositories:my-repo?tab=config');
    });
  });

  describe('edge cases', () => {
    it('returns null for non-preview paths', () => {
      expect(getHeritageEquivalent('admin/repository/repositories')).toBeNull();
    });

    it('returns null for empty string', () => {
      expect(getHeritageEquivalent('')).toBeNull();
    });

    it('returns null for null/undefined', () => {
      expect(getHeritageEquivalent(null)).toBeNull();
      expect(getHeritageEquivalent(undefined)).toBeNull();
    });

    it('strips leading slash', () => {
      expect(getHeritageEquivalent('/preview/admin/iq')).toBe('admin/iq');
    });
  });
});

describe('heritageToPreviewPath', () => {
  describe('page-level routes', () => {
    it('maps browse/welcome', () => {
      expect(heritageToPreviewPath('browse/welcome')).toBe('preview/browse/welcome');
    });

    it('maps admin hub to preview settings', () => {
      expect(heritageToPreviewPath('admin/hub')).toBe('preview/settings');
    });

    it('maps admin page-level routes', () => {
      expect(heritageToPreviewPath('admin/repository/repositories'))
        .toBe('preview/admin/repository/repositories');
      expect(heritageToPreviewPath('admin/security/privileges'))
        .toBe('preview/admin/security/privileges');
      expect(heritageToPreviewPath('admin/system/tasks'))
        .toBe('preview/admin/system/tasks');
    });

    it('maps Crowd Classic name to Preview name', () => {
      expect(heritageToPreviewPath('admin/security/atlassiancrowd'))
        .toBe('preview/admin/security/crowd');
    });

    it('maps Classic status to Preview metrichealth', () => {
      expect(heritageToPreviewPath('admin/support/status'))
        .toBe('preview/admin/support/metrichealth');
    });
  });

  describe('entity deep links (colon to slash conversion)', () => {
    it('converts colon-format repository entity to slash-format', () => {
      expect(heritageToPreviewPath('admin/repository/repositories:npm-proxy-central'))
        .toBe('preview/admin/repository/repositories/npm-proxy-central');
    });

    it('converts colon-format blob store entity to slash-format', () => {
      expect(heritageToPreviewPath('admin/repository/blobstores:default'))
        .toBe('preview/admin/repository/blobstores/default');
    });

    it('handles encoded entity names with colons', () => {
      expect(heritageToPreviewPath('admin/repository/repositories:Maven%20Central'))
        .toBe('preview/admin/repository/repositories/Maven%20Central');
    });
  });

  describe('round-trip symmetry', () => {
    it('getHeritageEquivalent -> heritageToPreviewPath returns original for entity paths', () => {
      const original = 'preview/admin/repository/repositories/npm-proxy-central';
      const classic = getHeritageEquivalent(original);
      expect(classic).toBe('admin/repository/repositories:npm-proxy-central');
      const roundTrip = heritageToPreviewPath(classic);
      expect(roundTrip).toBe(original);
    });

    it('round-trips page-level admin routes', () => {
      const original = 'preview/admin/security/privileges';
      const classic = getHeritageEquivalent(original);
      expect(classic).toBe('admin/security/privileges');
      const roundTrip = heritageToPreviewPath(classic);
      expect(roundTrip).toBe(original);
    });
  });
});

describe('previewBrowsePathToHeritageBrowseParam', () => {
  it('maps repo browse path', () => {
    expect(previewBrowsePathToHeritageBrowseParam('preview/browse/my-repo/'))
      .toBe('browse/browse:my-repo');
  });

  it('maps repo browse path with node path', () => {
    expect(previewBrowsePathToHeritageBrowseParam('preview/browse/my-repo/com/foo'))
      .toBe('browse/browse:my-repo:com%2Ffoo');
  });

  it('falls back to browse/browse for bare prefix', () => {
    expect(previewBrowsePathToHeritageBrowseParam('preview/browse/')).toBe('browse/browse');
  });

  it('falls back for non-matching prefix', () => {
    expect(previewBrowsePathToHeritageBrowseParam('other/path')).toBe('browse/browse');
  });
});
