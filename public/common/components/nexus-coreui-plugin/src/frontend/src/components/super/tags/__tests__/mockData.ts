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

import type { Tag, TagDetail } from '../tags.types';

/**
 * Mock tags for testing.
 */
export const mockTags: Tag[] = [
  {
    id: 'release-1.0',
    firstCreatedTime: '2026-01-15T10:30:45Z',
    lastUpdatedTime: '2026-01-18T14:15:22Z',
  },
  {
    id: 'staging',
    firstCreatedTime: '2026-01-10T09:00:00Z',
    lastUpdatedTime: '2026-01-20T11:45:00Z',
  },
  {
    id: 'beta-2.0',
    firstCreatedTime: '2026-01-12T15:20:30Z',
    lastUpdatedTime: '2026-01-19T08:30:15Z',
  },
  {
    id: 'alpha-test',
    firstCreatedTime: '2026-01-05T08:00:00Z',
    lastUpdatedTime: '2026-01-06T09:00:00Z',
  },
];

/**
 * Mock tag detail for testing.
 */
export const mockTagDetail: TagDetail = {
  name: 'release-1.0',
  firstCreated: '2026-01-15T10:30:45Z',
  lastUpdated: '2026-01-18T14:15:22Z',
  attributes: {
    env: 'production',
    version: '1.0.0',
    buildId: '142',
  },
};

/**
 * Generate a large list of mock tags for pagination testing.
 */
export function generateManyTags(count: number): Tag[] {
  return Array.from({ length: count }, (_, i) => ({
    id: `tag-${String(i + 1).padStart(3, '0')}`,
    firstCreatedTime: new Date(2026, 0, 1 + i).toISOString(),
    lastUpdatedTime: new Date(2026, 0, 15 + i).toISOString(),
  }));
}

