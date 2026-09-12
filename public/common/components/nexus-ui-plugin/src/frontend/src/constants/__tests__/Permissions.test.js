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
import Permissions from '../Permissions';

describe('Permissions constants (NEXUS-54212)', () => {
  it('exposes tags create and delete', () => {
    expect(Permissions.TAGS.READ).toBe('nexus:tags:read');
    expect(Permissions.TAGS.CREATE).toBe('nexus:tags:create');
    expect(Permissions.TAGS.DELETE).toBe('nexus:tags:delete');
  });

  it('exposes healthcheck read and update', () => {
    expect(Permissions.HEALTHCHECK.READ).toBe('nexus:healthcheck:read');
    expect(Permissions.HEALTHCHECK.UPDATE).toBe('nexus:healthcheck:update');
  });

  it('exposes iq-violation-summary read', () => {
    expect(Permissions.IQ_VIOLATION_SUMMARY.READ).toBe('nexus:iq-violation-summary:read');
  });

  it('exposes repository-admin edit and delete wildcards', () => {
    expect(Permissions.REPOSITORY_ADMIN.READ).toBe('nexus:repository-admin:*:*:read');
    expect(Permissions.REPOSITORY_ADMIN.EDIT).toBe('nexus:repository-admin:*:*:edit');
    expect(Permissions.REPOSITORY_ADMIN.DELETE).toBe('nexus:repository-admin:*:*:delete');
  });
});
