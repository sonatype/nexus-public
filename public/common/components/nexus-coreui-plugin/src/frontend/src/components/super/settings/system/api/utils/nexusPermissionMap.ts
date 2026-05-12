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
 * Effective permission flags from classic UI / Ext bootstrap ({@code NX.Permissions.permissions}).
 */
export function readNexusPermissionMap(): Record<string, boolean> {
  try {
    const w = window as unknown as { NX?: { Permissions?: { permissions?: Record<string, boolean> } } };
    const p = w.NX?.Permissions?.permissions;
    if (p && typeof p === 'object') {
      return p;
    }
  } catch {
    // ignore
  }
  return {};
}
