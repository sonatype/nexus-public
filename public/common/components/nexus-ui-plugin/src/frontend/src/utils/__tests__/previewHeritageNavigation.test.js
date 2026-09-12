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

describe('previewBrowsePathToHeritageBrowseParam', () => {
  it('maps repo-only path to browse param with encoded repo name', () => {
    expect(previewBrowsePathToHeritageBrowseParam('preview/browse/my-repo/'))
      .toBe('browse/browse:my-repo');
  });

  it('maps repo + node path to colon-separated browse param', () => {
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

describe('getHeritageEquivalent', () => {
  it('returns null for non-preview paths', () => {
    expect(getHeritageEquivalent('admin/repository/repositories')).toBeNull();
  });

  it('returns null for empty input', () => {
    expect(getHeritageEquivalent('')).toBeNull();
    expect(getHeritageEquivalent(null)).toBeNull();
  });

  it('maps browse/welcome page route', () => {
    expect(getHeritageEquivalent('preview/browse/welcome')).toBe('browse/welcome');
  });

  it('maps admin system tasks page route', () => {
    expect(getHeritageEquivalent('preview/admin/system/tasks')).toBe('admin/system/tasks');
  });

  it('invokes function heritage for repositories entity', () => {
    expect(getHeritageEquivalent('preview/admin/repository/repositories/my-repo'))
      .toBe('admin/repository/repositories:my-repo');
  });

  it('invokes function heritage for repositories page (no entity)', () => {
    expect(getHeritageEquivalent('preview/admin/repository/repositories'))
      .toBe('admin/repository/repositories');
  });

  it('invokes function heritage (browse repo) with query string', () => {
    expect(getHeritageEquivalent('preview/browse/my-repo/?tab=info'))
      .toBe('browse/browse:my-repo?tab=info');
  });

  it('maps sub-path via startsWith — appends to heritage', () => {
    expect(getHeritageEquivalent('preview/admin/repository/blobstores/default'))
      .toBe('admin/repository/blobstores/default');
  });

  it('preserves query string on page-level route', () => {
    expect(getHeritageEquivalent('preview/browse/welcome?tab=overview'))
      .toBe('browse/welcome?tab=overview');
  });

  it('strips leading slash', () => {
    expect(getHeritageEquivalent('/preview/admin/system/tasks')).toBe('admin/system/tasks');
  });

  it('maps recovery mode to the classic recovery route', () => {
    expect(getHeritageEquivalent('preview/admin/support/recoverymode'))
      .toBe('admin/support/recovery');
  });

  it('maps IQ Server Connected page with slash-separated path', () => {
    expect(getHeritageEquivalent('preview/admin/iq/connected')).toBe('admin/iq/connected');
  });

  it('maps Hosted Repository Evaluation to Classic sonatype-lifecycle route', () => {
    expect(getHeritageEquivalent('preview/admin/iq/hosted-repos-eval'))
      .toBe('admin/iq/sonatype-lifecycle/hosted-repos-eval');
  });

  it('preserves query string when mapping Hosted Repository Evaluation', () => {
    expect(getHeritageEquivalent('preview/admin/iq/hosted-repos-eval?configured=true'))
      .toBe('admin/iq/sonatype-lifecycle/hosted-repos-eval?configured=true');
  });
});

describe('heritageToPreviewPath', () => {
  it('maps browse/welcome', () => {
    expect(heritageToPreviewPath('browse/welcome')).toBe('preview/browse/welcome');
  });

  it('maps browse/browse to preview/browse', () => {
    expect(heritageToPreviewPath('browse/browse')).toBe('preview/browse');
  });

  it('maps browse repo-only path', () => {
    expect(heritageToPreviewPath('browse/browse:my-repo')).toBe('preview/browse/my-repo/');
  });

  it('maps browse repo+node path', () => {
    expect(heritageToPreviewPath('browse/browse:my-repo:com%2Ffoo')).toBe('preview/browse/my-repo/com/foo');
  });

  it('maps browse repo with empty node (trailing colon)', () => {
    expect(heritageToPreviewPath('browse/browse:my-repo:')).toBe('preview/browse/my-repo');
  });

  it('maps browse/search/generic=keyword= to search with q param', () => {
    expect(heritageToPreviewPath('browse/search/generic=keyword=test')).toBe('preview/browse/search?q=test');
  });

  it('maps browse/search/maven to preview search', () => {
    expect(heritageToPreviewPath('browse/search/maven')).toBe('preview/browse/search');
  });

  it('maps browse/search/generic (exact) to preview search', () => {
    expect(heritageToPreviewPath('browse/search/generic')).toBe('preview/browse/search');
  });

  // NOTE: This nexus-ui-plugin implementation maps 'browse/malwarerisk' to
  // 'preview/browse/malicious-packages' (the renamed preview route). The
  // nexus-coreui-plugin copy of this utility keeps the legacy 'malwarerisk'
  // segment. Both mappings are intentionally correct for their own module.
  it('maps browse/malwarerisk to preview malicious-packages', () => {
    expect(heritageToPreviewPath('browse/malwarerisk')).toBe('preview/browse/malicious-packages');
  });

  it('maps browse/malwarerisk sub-path to preview malicious-packages', () => {
    expect(heritageToPreviewPath('browse/malwarerisk/deep')).toBe('preview/browse/malicious-packages');
  });

  it('maps admin/hub to preview/settings', () => {
    expect(heritageToPreviewPath('admin/hub')).toBe('preview/settings');
  });

  it('maps admin (bare) to preview/settings', () => {
    expect(heritageToPreviewPath('admin')).toBe('preview/settings');
  });

  it('maps admin/ (trailing slash) to preview/settings', () => {
    expect(heritageToPreviewPath('admin/')).toBe('preview/settings');
  });

  it('maps Classic Hosted Repository Evaluation to Preview route', () => {
    expect(heritageToPreviewPath('admin/iq/sonatype-lifecycle/hosted-repos-eval'))
      .toBe('preview/admin/iq/hosted-repos-eval');
    expect(heritageToPreviewPath('admin/iq/sonatype-lifecycle/hosted-repos-eval?configured=true'))
      .toBe('preview/admin/iq/hosted-repos-eval?configured=true');
  });

  it('maps admin/security/atlassiancrowd to preview crowd route', () => {
    expect(heritageToPreviewPath('admin/security/atlassiancrowd')).toBe('preview/admin/security/crowd');
  });

  it('maps admin/security/atlassiancrowd sub-path', () => {
    expect(heritageToPreviewPath('admin/security/atlassiancrowd/sub')).toBe('preview/admin/security/crowd');
  });

  it('maps user/account (exact)', () => {
    expect(heritageToPreviewPath('user/account')).toBe('preview/user/account');
  });

  it('maps user/account sub-path', () => {
    expect(heritageToPreviewPath('user/account/settings')).toBe('preview/user/account');
  });

  it('maps user/NuGetApiToken', () => {
    expect(heritageToPreviewPath('user/NuGetApiToken')).toBe('preview/user/nugetapitoken');
  });

  it('maps user/usertoken', () => {
    expect(heritageToPreviewPath('user/usertoken')).toBe('preview/user/usertoken');
  });

  it('maps other user/ paths via fallthrough', () => {
    expect(heritageToPreviewPath('user/other')).toBe('preview/user/other');
  });

  it('maps admin/repository/cleanuppolicies sub-path', () => {
    expect(heritageToPreviewPath('admin/repository/cleanuppolicies/my-policy'))
      .toBe('preview/admin/repository/cleanup-policies/my-policy');
  });

  it('maps admin/repository/routingrules sub-path', () => {
    expect(heritageToPreviewPath('admin/repository/routingrules/my-rule'))
      .toBe('preview/admin/repository/routing-rules/my-rule');
  });

  it('maps admin/security/usertoken', () => {
    expect(heritageToPreviewPath('admin/security/usertoken'))
      .toBe('preview/admin/security/user-tokens');
  });

  it('maps admin/support/status sub-path', () => {
    expect(heritageToPreviewPath('admin/support/status/metrics'))
      .toBe('preview/admin/support/metrichealth/metrics');
  });

  it('maps the classic recovery route to the preview recovery mode page', () => {
    expect(heritageToPreviewPath('admin/support/recovery'))
      .toBe('preview/admin/support/recoverymode');
  });

  it('maps an unrecognised path via else branch', () => {
    expect(heritageToPreviewPath('some/other')).toBe('preview/some/other');
  });

  it('preserves query string', () => {
    expect(heritageToPreviewPath('browse/welcome?tab=info')).toBe('preview/browse/welcome?tab=info');
  });
});
