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
/**
 * Decode a URL segment; return original string if decoding fails.
 * @param {string} segment
 * @returns {string}
 */
function safeDecodeURIComponent(segment) {
  try {
    return decodeURIComponent(segment);
  } catch {
    return segment;
  }
}

/**
 * Maps Preview browse URLs under `preview/browse/...` to Classic `browse/browse:...` (repo or repo:path).
 * Matches Classic MasterDetail + BrowseTree bookmarks: `#browse/browse:<encRepo>` or
 * `#browse/browse:<encRepo>:<encNodePath>`.
 *
 * @param {string} previewCleanPath full path e.g. `preview/browse/my-repo/` or `preview/browse/my-repo/com/foo`
 * @returns {string}
 */
export function previewBrowsePathToHeritageBrowseParam(previewCleanPath) {
  const prefix = 'preview/browse/';
  if (!previewCleanPath.startsWith(prefix)) {
    return 'browse/browse';
  }
  let afterPrefix = previewCleanPath.slice(prefix.length).replace(/^\/+/g, '');
  afterPrefix = afterPrefix.replace(/\/+$/g, '');
  if (!afterPrefix) {
    return 'browse/browse';
  }
  const slashIdx = afterPrefix.indexOf('/');
  if (slashIdx === -1) {
    const repo = safeDecodeURIComponent(afterPrefix);
    if (!repo) {
      return 'browse/browse';
    }
    return `browse/browse:${encodeURIComponent(repo)}`;
  }
  const repo = safeDecodeURIComponent(afterPrefix.slice(0, slashIdx));
  let nodePath = afterPrefix.slice(slashIdx + 1).replace(/\/+$/g, '');
  nodePath = safeDecodeURIComponent(nodePath);
  if (!repo) {
    return 'browse/browse';
  }
  if (!nodePath) {
    return `browse/browse:${encodeURIComponent(repo)}`;
  }
  return `browse/browse:${encodeURIComponent(repo)}:${encodeURIComponent(nodePath)}`;
}

/**
 * Bidirectional route mapping between Preview UI and Classic UI.
 * Ordered from most specific to least specific so pattern matching works correctly.
 */
export const PREVIEW_TO_HERITAGE_ROUTES = [
  { preview: 'preview/browse/welcome', heritage: 'browse/welcome' },
  { preview: 'preview/browse/upload', heritage: 'browse/upload' },
  { preview: 'preview/browse/tags', heritage: 'browse/tags' },
  { preview: 'preview/browse/protect', heritage: 'browse/browse' },
  { preview: 'preview/browse/audit', heritage: 'browse/browse' },
  { preview: 'preview/browse/malicious-packages', heritage: 'browse/malwarerisk' },
  { preview: 'preview/browse/remediate', heritage: 'browse/malwarerisk' },
  { preview: 'preview/browse/api', heritage: 'browse/browse' },
  { preview: 'preview/browse/repository-profile', heritage: 'browse/browse' },
  { preview: 'preview/browse/health-report', heritage: 'browse/browse' },
  { preview: 'preview/browse/firewall-report', heritage: 'browse/browse' },
  { preview: 'preview/browse/search/maven', heritage: 'browse/search/maven' },
  { preview: 'preview/browse/search/npm', heritage: 'browse/search/npm' },
  { preview: 'preview/browse/search/nuget', heritage: 'browse/search/nuget' },
  { preview: 'preview/browse/search/docker', heritage: 'browse/search/docker' },
  { preview: 'preview/browse/search/pypi', heritage: 'browse/search/pypi' },
  { preview: 'preview/browse/search/rubygems', heritage: 'browse/search/rubygems' },
  { preview: 'preview/browse/search/helm', heritage: 'browse/search/helm' },
  { preview: 'preview/browse/search/yum', heritage: 'browse/search/yum' },
  { preview: 'preview/browse/search/apt', heritage: 'browse/search/apt' },
  { preview: 'preview/browse/search/raw', heritage: 'browse/search/raw' },
  { preview: 'preview/browse/search/golang', heritage: 'browse/search/golang' },
  { preview: 'preview/browse/search/cargo', heritage: 'browse/search/cargo' },
  { preview: 'preview/browse/search/cocoapods', heritage: 'browse/search/cocoapods' },
  { preview: 'preview/browse/search/composer', heritage: 'browse/search/composer' },
  { preview: 'preview/browse/search/conan', heritage: 'browse/search/conan' },
  { preview: 'preview/browse/search/conda', heritage: 'browse/search/conda' },
  { preview: 'preview/browse/search/gitlfs', heritage: 'browse/search/gitlfs' },
  { preview: 'preview/browse/search/p2', heritage: 'browse/search/p2' },
  { preview: 'preview/browse/search/r', heritage: 'browse/search/r' },
  { preview: 'preview/browse/search/terraform', heritage: 'browse/search/terraform' },
  { preview: 'preview/browse/search/swift', heritage: 'browse/search/swift' },
  { preview: 'preview/browse/search/hugging_face', heritage: 'browse/search/hugging_face' },
  { preview: 'preview/browse/search', heritage: 'browse/search/generic' },
  { preview: 'preview/browse/', heritage: (p) => previewBrowsePathToHeritageBrowseParam(p) },
  { preview: 'preview/browse', heritage: 'browse/browse' },
  { preview: 'preview/settings', heritage: 'admin/hub' },
  { preview: 'preview/admin/repository/repositories', heritage: (p) => {
    const rest = p.replace('preview/admin/repository/repositories', '');
    return rest.startsWith('/') ? `admin/repository/repositories:${rest.slice(1)}` : 'admin/repository/repositories';
  }},
  { preview: 'preview/admin/repository/blobstores', heritage: 'admin/repository/blobstores' },
  { preview: 'preview/admin/repository/selectors', heritage: 'admin/repository/selectors' },
  { preview: 'preview/admin/repository/cleanup-policies', heritage: 'admin/repository/cleanuppolicies' },
  { preview: 'preview/admin/repository/routing-rules', heritage: 'admin/repository/routingrules' },
  { preview: 'preview/admin/repository/datastore', heritage: 'admin/repository/datastore' },
  { preview: 'preview/admin/repository/proprietary', heritage: 'admin/repository/proprietary' },
  { preview: 'preview/admin/security/privileges', heritage: 'admin/security/privileges' },
  { preview: 'preview/admin/security/roles', heritage: 'admin/security/roles' },
  { preview: 'preview/admin/security/users', heritage: 'admin/security/users' },
  { preview: 'preview/admin/security/anonymous', heritage: 'admin/security/anonymous' },
  { preview: 'preview/admin/security/ldap', heritage: 'admin/security/ldap' },
  { preview: 'preview/admin/security/realms', heritage: 'admin/security/realms' },
  { preview: 'preview/admin/security/sslcertificates', heritage: 'admin/security/sslcertificates' },
  { preview: 'preview/admin/security/user-tokens', heritage: 'admin/security/usertoken' },
  { preview: 'preview/admin/security/saml', heritage: 'admin/security/saml' },
  { preview: 'preview/admin/security/oauth2', heritage: 'admin/security/oauth2' },
  { preview: 'preview/admin/security/crowd', heritage: 'admin/security/atlassiancrowd' },
  { preview: 'preview/admin/support/logs', heritage: 'admin/support/logs' },
  { preview: 'preview/admin/support/logging', heritage: 'admin/support/logging' },
  { preview: 'preview/admin/support/systeminformation', heritage: 'admin/support/systeminformation' },
  { preview: 'preview/admin/support/metrichealth', heritage: 'admin/support/status' },
  { preview: 'preview/admin/support/supportrequest', heritage: 'admin/support/supportrequest' },
  { preview: 'preview/admin/support/supportzip', heritage: 'admin/support/supportzip' },
  { preview: 'preview/admin/system/tasks', heritage: 'admin/system/tasks' },
  { preview: 'preview/admin/system/capabilities', heritage: 'admin/system/capabilities' },
  { preview: 'preview/admin/system/emailserver', heritage: 'admin/system/emailserver' },
  { preview: 'preview/admin/system/http', heritage: 'admin/system/http' },
  { preview: 'preview/admin/system/licensing', heritage: 'admin/system/licensing' },
  { preview: 'preview/admin/system/nodes', heritage: 'admin/system/nodes' },
  { preview: 'preview/admin/system/api', heritage: 'admin/system/api' },
  { preview: 'preview/admin/system/upgrade', heritage: 'admin/system/upgrade' },
  { preview: 'preview/admin/system/previewui', heritage: 'admin/system/previewui' },
  { preview: 'preview/admin/iq', heritage: 'admin/iq' },
  { preview: 'preview/user/account', heritage: 'user/account' },
  { preview: 'preview/user/nugetapitoken', heritage: 'user/NuGetApiToken' },
  { preview: 'preview/user/usertoken', heritage: 'user/usertoken' },
];

/**
 * Classic UI equivalent path for the given Preview hash path, or null if none exists.
 */
export function getHeritageEquivalent(previewPath) {
  const raw = (previewPath || '').replace(/^\//, '');
  const [clean, queryString] = raw.split('?');
  if (!clean.startsWith('preview/')) return null;

  for (const { preview, heritage } of PREVIEW_TO_HERITAGE_ROUTES) {
    if (typeof heritage === 'function') {
      if (clean.startsWith(preview)) {
        const result = heritage(clean);
        return queryString ? `${result}?${queryString}` : result;
      }
    } else if (clean === preview || clean.startsWith(preview + '/')) {
      const result = clean.replace(preview, heritage);
      return queryString ? `${result}?${queryString}` : result;
    }
  }
  return null;
}

/**
 * Preview UI hash path (no leading #) for the current Classic UI path.
 */
export function heritageToPreviewPath(currentPath) {
  const raw = currentPath.replace(/^\//, '');
  const [cleanPath, qs] = raw.split('?');
  const browseRepoMatch = cleanPath.match(/^browse\/browse:(.+)$/);
  let newPath;
  if (cleanPath === 'browse/welcome') {
    newPath = 'preview/browse/welcome';
  } else if (cleanPath === 'browse/browse') {
    newPath = 'preview/browse';
  } else if (browseRepoMatch) {
    const rest = browseRepoMatch[1];
    const colonIdx = rest.indexOf(':');
    if (colonIdx === -1) {
      const repo = safeDecodeURIComponent(rest);
      newPath = `preview/browse/${encodeURIComponent(repo)}/`;
    } else {
      const repo = safeDecodeURIComponent(rest.slice(0, colonIdx));
      const nodePath = safeDecodeURIComponent(rest.slice(colonIdx + 1));
      const pathPart = nodePath ? `/${nodePath.replace(/^\/+/, '')}` : '';
      newPath = `preview/browse/${encodeURIComponent(repo)}${pathPart}`;
    }
  } else if (cleanPath.match(/^browse\/search\/generic=keyword=(.*)$/)) {
    const query = decodeURIComponent(cleanPath.match(/^browse\/search\/generic=keyword=(.*)$/)[1]);
    newPath = `preview/browse/search?q=${encodeURIComponent(query)}`;
  } else if (cleanPath === 'browse/search/generic' || cleanPath.startsWith('browse/search/')) {
    newPath = 'preview/browse/search';
  } else if (cleanPath === 'browse/malwarerisk' || cleanPath.startsWith('browse/malwarerisk/')) {
    newPath = 'preview/browse/malicious-packages';
  } else if (cleanPath === 'admin/hub' || cleanPath === 'admin' || cleanPath === 'admin/') {
    newPath = 'preview/settings';
  } else if (cleanPath === 'admin/security/atlassiancrowd' || cleanPath.startsWith('admin/security/atlassiancrowd/')) {
    newPath = 'preview/admin/security/crowd';
  } else if (cleanPath === 'user/account' || cleanPath.startsWith('user/account/')) {
    newPath = 'preview/user/account';
  } else if (cleanPath === 'user/NuGetApiToken' || cleanPath.startsWith('user/NuGetApiToken')) {
    newPath = 'preview/user/nugetapitoken';
  } else if (cleanPath === 'user/usertoken' || cleanPath.startsWith('user/usertoken')) {
    newPath = 'preview/user/usertoken';
  } else if (cleanPath.startsWith('user/')) {
    newPath = 'preview/' + cleanPath;
  } else if (cleanPath === 'admin/repository/cleanuppolicies' || cleanPath.startsWith('admin/repository/cleanuppolicies/')) {
    newPath = cleanPath.replace('admin/repository/cleanuppolicies', 'preview/admin/repository/cleanup-policies');
  } else if (cleanPath === 'admin/repository/routingrules' || cleanPath.startsWith('admin/repository/routingrules/')) {
    newPath = cleanPath.replace('admin/repository/routingrules', 'preview/admin/repository/routing-rules');
  } else if (cleanPath === 'admin/security/usertoken' || cleanPath.startsWith('admin/security/usertoken/')) {
    newPath = cleanPath.replace('admin/security/usertoken', 'preview/admin/security/user-tokens');
  } else if (cleanPath === 'admin/support/status' || cleanPath.startsWith('admin/support/status/')) {
    newPath = cleanPath.replace('admin/support/status', 'preview/admin/support/metrichealth');
  } else {
    newPath = 'preview/' + cleanPath;
  }
  if (qs) newPath += '?' + qs;
  return newPath;
}
