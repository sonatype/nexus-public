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

import Axios from 'axios';

import { normalizeFormatKey } from './registry';

/**
 * Fire the dependency-snippet copy analytics event.
 *
 * Mirrors Classic NX.analytics.controller.Analytics#copySnippetClicked: a fire-and-forget
 * POST to `/service/rest/dependency_snippets` with the normalized format key and the copied
 * snippet's display name. Failures are swallowed so analytics never disrupts the copy action.
 */
export function trackSnippetCopy(format: string, snippetDisplayName: string): void {
  const formatKey = normalizeFormatKey(format);
  const url =
    `/service/rest/dependency_snippets?format=${encodeURIComponent(formatKey)}` +
    `&snippet=${encodeURIComponent(snippetDisplayName)}`;
  Axios.post(url).catch(() => {
    // fire-and-forget: analytics failures must not affect the copy UX
  });
}
