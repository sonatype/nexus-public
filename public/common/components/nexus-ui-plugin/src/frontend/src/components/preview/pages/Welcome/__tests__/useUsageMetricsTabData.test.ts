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
import {renderHook} from '@testing-library/react';
import {useUsageMetricsTabData} from '../useUsageMetricsTabData';

// Self-hosted path now uses UsageCenter (reads its own state), so the hook only needs
// isCloud and monthlyMetrics (for the cloud path) (NEXUS-53863).
jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    state: jest.fn(() => ({getValue: (_key: string, def: unknown) => def})),
  },
}));

jest.mock('../dashboard', () => ({
  useMonthlyMetrics: jest.fn(() => ({
    loading: false,
    error: null,
    history: {egress: [], storage: []},
    peakStorage: null,
    responseSize: 0,
  })),
}));

const {ExtJS} = require('../../../../../interface/ExtJS');

describe('useUsageMetricsTabData', () => {
  beforeEach(() => {
    ExtJS.state.mockReturnValue({getValue: (_key: string, def: unknown) => def});
  });

  describe('isCloud', () => {
    it('returns false when ExtJS reports non-cloud', () => {
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.isCloud).toBe(false);
    });

    it('returns true when ExtJS reports isCloud=true', () => {
      ExtJS.state.mockReturnValue({
        getValue: (key: string, def: unknown) => (key === 'isCloud' ? true : def),
      });
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.isCloud).toBe(true);
    });
  });

  describe('monthlyMetrics', () => {
    it('passes monthlyMetrics through from useMonthlyMetrics hook', () => {
      const {useMonthlyMetrics} = require('../dashboard');
      const mockMetrics = {loading: false, error: null, history: {egress: [{value: 100}], storage: []}, peakStorage: 5000, responseSize: 300};
      useMonthlyMetrics.mockReturnValueOnce(mockMetrics);
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.monthlyMetrics).toBe(mockMetrics);
    });
  });
});
