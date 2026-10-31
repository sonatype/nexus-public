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

import {
  TERMINAL_TASK_STATUSES,
  isTerminalStatus,
  isActiveStatus,
} from '../types';

// Single client-side source of truth for which task statuses are terminal vs
// active, reused by the polling gates on both the detail and list pages so the
// "keep polling / stop polling" decision is defined in exactly one place.
describe('task status classification', () => {
  it('defines the terminal status set as the DONE-group outcomes', () => {
    expect([...TERMINAL_TASK_STATUSES].sort()).toEqual(
      ['CANCELED', 'FAILED', 'INTERRUPTED', 'OK'],
    );
  });

  it('isTerminalStatus is true only for terminal outcomes', () => {
    expect(isTerminalStatus('OK')).toBe(true);
    expect(isTerminalStatus('FAILED')).toBe(true);
    expect(isTerminalStatus('CANCELED')).toBe(true);
    expect(isTerminalStatus('INTERRUPTED')).toBe(true);

    expect(isTerminalStatus('WAITING')).toBe(false);
    expect(isTerminalStatus('RUNNING')).toBe(false);
    expect(isTerminalStatus('BLOCKED')).toBe(false);
    expect(isTerminalStatus(null)).toBe(false);
    expect(isTerminalStatus(undefined)).toBe(false);
  });

  it('isActiveStatus is true while the task is doing work', () => {
    expect(isActiveStatus('RUNNING')).toBe(true);
    expect(isActiveStatus('BLOCKED')).toBe(true);

    expect(isActiveStatus('WAITING')).toBe(false);
    expect(isActiveStatus('OK')).toBe(false);
    expect(isActiveStatus('FAILED')).toBe(false);
    expect(isActiveStatus(null)).toBe(false);
    expect(isActiveStatus(undefined)).toBe(false);
  });
});
