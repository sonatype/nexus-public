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

import type { TagDetail } from '../tags.types';

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


