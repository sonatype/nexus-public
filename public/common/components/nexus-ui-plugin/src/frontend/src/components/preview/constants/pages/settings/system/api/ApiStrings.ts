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
 * User-facing strings for the API settings page endpoint list (NEXUS-54905).
 * Technical values (HTTP method names, raw permission strings) stay inline.
 */
export default {
  API_ENDPOINT_LIST: {
    searchPlaceholder: 'Search by method, path or summary',
    searchLabel: 'Search endpoints',
    resultCount: (shown: number, total: number): string =>
      `Showing ${shown} of ${total} endpoints`,
    toggle: {
      hide: 'Hide endpoint list',
      show: 'Show endpoint list',
    },
    access: {
      granted: 'Full access',
      denied: 'No access',
      partial: 'Partial access',
      unknown: 'Access unknown',
    },
  },
};
