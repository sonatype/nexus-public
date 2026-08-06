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
 * Unit tests for workflow primitives. The full workflows themselves run inside
 * `useMaliciousPackagesData.test.ts`; these tests exercise the cancellation
 * mechanism in isolation so a regression in abort handling surfaces immediately.
 */

import { abortableDelay, WorkflowAbortError } from '../maliciousPackagesWorkflows';

describe('abortableDelay', () => {
  it('resolves after the requested delay when the signal is never aborted', async () => {
    const ac = new AbortController();
    const start = Date.now();
    await abortableDelay(20, ac.signal);
    expect(Date.now() - start).toBeGreaterThanOrEqual(15);
  });

  it('rejects with WorkflowAbortError if the signal is already aborted on entry', async () => {
    const ac = new AbortController();
    ac.abort();
    await expect(abortableDelay(1000, ac.signal)).rejects.toBeInstanceOf(WorkflowAbortError);
  });

  it('rejects with WorkflowAbortError when the signal aborts mid-delay', async () => {
    const ac = new AbortController();
    const promise = abortableDelay(1000, ac.signal);
    setTimeout(() => ac.abort(), 10);
    await expect(promise).rejects.toBeInstanceOf(WorkflowAbortError);
  });

  it('does not abort if the signal is undefined', async () => {
    const start = Date.now();
    await abortableDelay(10, undefined);
    expect(Date.now() - start).toBeGreaterThanOrEqual(5);
  });
});
