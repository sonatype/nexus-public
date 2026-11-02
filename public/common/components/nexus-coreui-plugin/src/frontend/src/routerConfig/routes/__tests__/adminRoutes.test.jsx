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
import {Permissions} from '@sonatype/nexus-ui-plugin';

import {adminRoutes} from '../adminRoutes';

describe('adminRoutes', () => {
  describe('admin.repository.globaldistribution', () => {
    const route = adminRoutes.find(r => r.name === 'admin.repository.globaldistribution');

    it('registers a route that redirects to the preview page', () => {
      expect(route).toBeDefined();
      expect(route.redirectTo).toBe('preview.admin.repository.globaldistribution');
    });

    // Must match the preview route gate in previewAdminRoutes.js.
    it('is gated by any of settings:read, repository-admin:read or blobstores:read', () => {
      expect(route.data.visibilityRequirements).toEqual({
        requiresAnyPermission: [
          Permissions.SETTINGS.READ,
          Permissions.REPOSITORY_ADMIN.READ,
          Permissions.BLOB_STORES.READ,
        ],
      });
    });
  });
});
