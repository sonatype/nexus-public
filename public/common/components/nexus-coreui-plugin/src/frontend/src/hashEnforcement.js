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
 * Extracts the nexus-context-path from the Nexus bundle script URL synchronously.
 * The bundle is always served at {contextPath}/static/rapture/resources/...
 * so we can find any script tag with that path pattern and extract the prefix.
 *
 * Returns '' for a root context path or when the script URL cannot be determined.
 */
export function getContextPath() {
  try {
    const scripts = document.currentScript
      ? [document.currentScript, ...document.scripts]
      : document.scripts;
    for (const script of scripts) {
      if (script.src && script.src.includes('/static/rapture/')) {
        return new URL(script.src).pathname.split('/static/rapture/')[0];
      }
    }
  } catch (e) {
    // ignore - contextPath stays ''
  }
  return '';
}

/**
 * Ensures hash-based routing is active. If someone navigates to a non-hash URL
 * (e.g. /nxrm/admin/system/tasks), rewrites to the equivalent hash URL
 * (e.g. /nxrm/#/admin/system/tasks) so ui-router can handle routing correctly.
 *
 * Accounts for non-root context paths by detecting the context-path from the
 * Nexus bundle script URL and preserving it in the redirected URL.
 */
export function enforceHashRouting() {
  if (typeof window === 'undefined' || window.__nxHashEnforced) {
    return;
  }
  window.__nxHashEnforced = true;

  const { hash, pathname, search, origin } = window.location;
  if (!hash && pathname && pathname !== '/') {
    const contextPath = getContextPath();
    const route = contextPath && pathname.startsWith(contextPath)
      ? pathname.slice(contextPath.length)
      : pathname;
    const cleaned = route.replace(/^\/+/, '');
    if (cleaned) {
      const newUrl = `${origin}${contextPath}/${search || ''}#/${cleaned}`;
      window.location.replace(newUrl);
    }
  }
}
