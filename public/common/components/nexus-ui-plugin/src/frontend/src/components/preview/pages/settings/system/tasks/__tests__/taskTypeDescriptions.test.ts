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

import { getTaskTypeCategory, getTaskTypeDescription, TASK_TYPE_CATEGORIES } from '../taskTypeDescriptions';

describe('taskTypeDescriptions — NEXUS-53360 unique-UI tasks', () => {
  const GENERIC_FALLBACK = 'Configure and schedule this task type.';

  describe('h2.backup.task (H2BackupTaskDescriptor)', () => {
    // The production map intentionally keeps the legacy, non-public `h2.backup` key alongside
    // the real descriptor id `h2.backup.task`; these tests pin the real id so a future cleanup
    // can't silently drop coverage and reintroduce the legacy/non-public task type id.
    it('has a specific description keyed by the real descriptor TYPE_ID', () => {
      // The map previously only had "h2.backup"/"db.backup"; the live id is "h2.backup.task".
      const description = getTaskTypeDescription('h2.backup.task');
      expect(description).not.toBe(GENERIC_FALLBACK);
      expect(description).toMatch(/H2/);
    });

    it('is categorized under Admin via an explicit map entry, not only the h2.* pattern fallback', () => {
      // `getTaskTypeCategory` has a `typeId.startsWith('h2.')` pattern fallback that would also
      // return 'Admin'. This test pins the *explicit* entry so a future cleanup that removes the
      // entry (thinking the fallback covers it) gets caught immediately.
      expect(TASK_TYPE_CATEGORIES['h2.backup.task']).toBe('Admin');
      expect(getTaskTypeCategory('h2.backup.task')).toBe('Admin');
    });
  });

  describe('script (ScriptTaskDescriptor)', () => {
    it('has a specific description', () => {
      expect(getTaskTypeDescription('script')).not.toBe(GENERIC_FALLBACK);
    });
  });

  it('falls back to the generic description for an unknown task type', () => {
    expect(getTaskTypeDescription('totally.unknown.task')).toBe(GENERIC_FALLBACK);
  });
});

describe('Execute Data Repair Plan metadata', () => {
  const T = 'blobstore.executeReconciliationPlan';
  it('has a specific description', () => {
    expect(getTaskTypeDescription(T)).toMatch(/recovery plan/i);
    expect(getTaskTypeDescription(T)).not.toBe('Configure and schedule this task type.');
  });
  it('is categorized under Admin', () => {
    expect(getTaskTypeCategory(T)).toBe('Admin');
  });
});
