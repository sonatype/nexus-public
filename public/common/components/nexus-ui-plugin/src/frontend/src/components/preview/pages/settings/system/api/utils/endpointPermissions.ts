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

import { ExtJS } from '../../../../../../../interface/ExtJS';

// Wrap calls in ExtJS.usePermission at the call site so the check is reactive to async permission loads.
// The Who Has Access and Grant Access tabs both load the full security directory (roles + privileges +
// users), so any missing read causes a 403 and a red error banner in the tab body. Gate on all three.

export function canReadSecurityDirectory(): boolean {
  return (
    ExtJS.checkPermission('nexus:roles:read') &&
    ExtJS.checkPermission('nexus:users:read') &&
    ExtJS.checkPermission('nexus:privileges:read')
  );
}

export function canGrantAccess(): boolean {
  return (
    canReadSecurityDirectory() &&
    (ExtJS.checkPermission('nexus:roles:update') || ExtJS.checkPermission('nexus:roles:create')) &&
    ExtJS.checkPermission('nexus:users:update')
  );
}
