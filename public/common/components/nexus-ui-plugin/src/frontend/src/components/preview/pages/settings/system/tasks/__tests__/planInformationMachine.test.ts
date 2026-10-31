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
import { interpret } from 'xstate';
import { restClient } from '../../../../../../../interface/api';
import { planInformationMachine } from '../planInformationMachine';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn() },
}));

const mockGet = restClient.get as jest.Mock;

const plan = (over: Record<string, unknown>) => ({
  id: 1, repository: 'r', blobStore: 'b', state: 'PLANNED',
  configuration: { planStartDate: '2026-01-01', planEndDate: '2026-02-01' }, ...over,
});

describe('planInformationMachine', () => {
  beforeEach(() => jest.clearAllMocks());

  it('starts in loading', () => {
    expect(planInformationMachine.initialState.value).toBe('loading');
  });

  it('aggregates active plans on success (Classic parity: count per plan, not unique)', (done) => {
    // Classic counts +1 per plan that has a non-empty/non-"undefined" value,
    // NOT the number of distinct values. Two plans with the same blobStore → count 2.
    mockGet.mockResolvedValueOnce({
      items: [
        plan({ blobStore: 'b1', repository: 'r1', state: 'PLANNED',
                configuration: { planStartDate: '2026-03-01', planEndDate: '2026-03-10' } }),
        plan({ blobStore: 'b1', repository: 'r1', state: 'EXECUTED',
                configuration: { planStartDate: '2026-01-01', planEndDate: '2026-05-01' } }),
        plan({ blobStore: 'b1', repository: 'r2', state: 'CANCELLED',
                configuration: { planStartDate: '2025-01-01', planEndDate: '2027-01-01' } }),
      ],
      continuationToken: null,
    });
    const service = interpret(planInformationMachine).onTransition((state) => {
      if (state.matches('loaded')) {
        expect(state.context.planCount).toBe(2);          // CANCELLED excluded
        expect(state.context.blobStoreCount).toBe(2);     // 2 active plans with b1 (same name counts twice)
        expect(state.context.repositoryCount).toBe(2);    // 2 active plans with a repository
        expect(state.context.startDate).toBe('2026-01-01'); // earliest active
        expect(state.context.endDate).toBe('2026-05-01');   // latest active
        expect(state.context.error).toBeNull();
        service.stop(); done();
      }
    });
    service.start();
  });

  it('follows continuationToken across pages', (done) => {
    mockGet
      .mockResolvedValueOnce({ items: [plan({ blobStore: 'b1' })], continuationToken: 'tok' })
      .mockResolvedValueOnce({ items: [plan({ blobStore: 'b2' })], continuationToken: null });
    const service = interpret(planInformationMachine).onTransition((state) => {
      if (state.matches('loaded')) {
        expect(state.context.planCount).toBe(2);
        expect(mockGet).toHaveBeenCalledTimes(2);
        service.stop(); done();
      }
    });
    service.start();
  });

  it('yields empty aggregates when there are no active plans', (done) => {
    mockGet.mockResolvedValueOnce({ items: [], continuationToken: null });
    const service = interpret(planInformationMachine).onTransition((state) => {
      if (state.matches('loaded')) {
        expect(state.context.planCount).toBe(0);
        expect(state.context.startDate).toBeNull();
        expect(state.context.endDate).toBeNull();
        service.stop(); done();
      }
    });
    service.start();
  });

  it('transitions to error on fetch failure', (done) => {
    mockGet.mockRejectedValueOnce(new Error('Network failure'));
    const service = interpret(planInformationMachine).onTransition((state) => {
      if (state.matches('error')) {
        expect(state.context.error).toBe('Network failure');
        service.stop(); done();
      }
    });
    service.start();
  });

  it('ignores the literal string "undefined" in repository and blobStore fields', (done) => {
    // The backend serialises a missing value as the string "undefined" (not a JS undefined).
    // Plans with that value must not inflate the repository or blobStore counts.
    mockGet.mockResolvedValueOnce({
      items: [
        plan({ repository: 'undefined', blobStore: 'undefined', state: 'PLANNED' }),
        plan({ repository: 'npm.js-proxy', blobStore: 'default', state: 'PLANNED' }),
      ],
      continuationToken: null,
    });
    const service = interpret(planInformationMachine).onTransition((state) => {
      if (state.matches('loaded')) {
        expect(state.context.repositoryCount).toBe(1);  // only npm.js-proxy
        expect(state.context.blobStoreCount).toBe(1);   // only default
        service.stop(); done();
      }
    });
    service.start();
  });

  it('re-enters loading and recovers on RETRY after a fetch failure', (done) => {
    mockGet
      .mockRejectedValueOnce(new Error('first attempt'))
      .mockResolvedValueOnce({
        items: [plan({ blobStore: 'b1', state: 'PLANNED' })],
        continuationToken: null,
      });
    const service = interpret(planInformationMachine).onTransition((state) => {
      if (state.matches('error')) {
        service.send({ type: 'RETRY' });
      }
      if (state.matches('loaded')) {
        expect(state.context.planCount).toBe(1);
        expect(state.context.error).toBeNull();
        service.stop(); done();
      }
    });
    service.start();
  });

  it('stops following continuationToken at the MAX_PAGES safety bound and sets truncated', (done) => {
    // A backend that always returns a token must not loop forever / exhaust memory.
    // When MAX_PAGES is hit with a remaining token the truncated flag must be set so the
    // widget can warn the user that the displayed counts are incomplete.
    mockGet.mockResolvedValue({ items: [plan({})], continuationToken: 'always' });
    const service = interpret(planInformationMachine).onTransition((state) => {
      if (state.matches('loaded')) {
        expect(mockGet).toHaveBeenCalledTimes(100); // MAX_PAGES
        expect(state.context.planCount).toBe(100);
        expect(state.context.truncated).toBe(true);
        service.stop(); done();
      }
    });
    service.start();
  });

  it('sets truncated: false when all pages are consumed normally', (done) => {
    mockGet
      .mockResolvedValueOnce({ items: [plan({})], continuationToken: 'tok' })
      .mockResolvedValueOnce({ items: [plan({})], continuationToken: null });
    const service = interpret(planInformationMachine).onTransition((state) => {
      if (state.matches('loaded')) {
        expect(state.context.truncated).toBe(false);
        service.stop(); done();
      }
    });
    service.start();
  });
});
