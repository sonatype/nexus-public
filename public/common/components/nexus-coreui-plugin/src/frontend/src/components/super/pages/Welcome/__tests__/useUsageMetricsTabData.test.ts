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

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    state: jest.fn(() => ({getValue: (_key: string, def: unknown) => def})),
  },
}));

jest.mock('../dashboard', () => ({
  useInstanceTotals: jest.fn(() => ({data: null, loading: false})),
  useMonthlyMetrics: jest.fn(() => ({
    loading: false,
    error: null,
    history: {egress: [], storage: []},
    peakStorage: null,
    responseSize: 0,
  })),
  useInstanceStorage: jest.fn(() => ({currentStorageBytes: null})),
  formatBytesToGB: jest.fn((bytes: number, _flag?: boolean) => `${bytes}GB`),
}));

const {ExtJS} = require('@sonatype/nexus-ui-plugin');

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

  describe('monthlyMetricsFormatted', () => {
    it('returns undefined while loading', () => {
      const {useMonthlyMetrics} = require('../dashboard');
      useMonthlyMetrics.mockReturnValueOnce({loading: true, error: null, history: null, peakStorage: null, responseSize: 0});
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.monthlyMetricsFormatted).toBeUndefined();
    });

    it('uses peakStorage from monthly metrics when available', () => {
      const {useMonthlyMetrics, formatBytesToGB} = require('../dashboard');
      useMonthlyMetrics.mockReturnValueOnce({
        loading: false, error: null,
        history: {egress: [{value: 100}], storage: []},
        peakStorage: 5000,
        responseSize: 100,
      });
      formatBytesToGB.mockReturnValueOnce('5.00 GB');
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.monthlyMetricsFormatted?.peakStorageGB).toBe('5.00 GB');
    });

    it('falls back to instanceStorage when monthlyMetrics.peakStorage is null', () => {
      const {useMonthlyMetrics, useInstanceStorage, formatBytesToGB} = require('../dashboard');
      useMonthlyMetrics.mockReturnValueOnce({
        loading: false, error: null,
        history: {egress: [{value: 100}], storage: []},
        peakStorage: null,
        responseSize: 100,
      });
      useInstanceStorage.mockReturnValueOnce({currentStorageBytes: 2000});
      formatBytesToGB.mockReturnValueOnce('2.00 GB');
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.monthlyMetricsFormatted?.peakStorageGB).toBe('2.00 GB');
    });

    it('sets isEgressTbd=true and responseSizeGB="TBD" when no egress data exists', () => {
      const {useMonthlyMetrics} = require('../dashboard');
      useMonthlyMetrics.mockReturnValueOnce({
        loading: false, error: null,
        history: {egress: [], storage: []},
        peakStorage: null,
        responseSize: 0,
      });
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.monthlyMetricsFormatted?.isEgressTbd).toBe(true);
      expect(result.current.monthlyMetricsFormatted?.responseSizeGB).toBe('TBD');
    });

    it('uses responseSize as egress when available', () => {
      const {useMonthlyMetrics, formatBytesToGB} = require('../dashboard');
      useMonthlyMetrics.mockReturnValueOnce({
        loading: false, error: null,
        history: {egress: [{value: 50}], storage: []},
        peakStorage: null,
        responseSize: 300,
      });
      formatBytesToGB.mockReturnValueOnce('300GB');
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.monthlyMetricsFormatted?.responseSizeGB).toBe('300GB');
      expect(result.current.monthlyMetricsFormatted?.isEgressTbd).toBe(false);
    });

    it('falls back to last known egress from history when responseSize is 0', () => {
      const {useMonthlyMetrics, formatBytesToGB} = require('../dashboard');
      useMonthlyMetrics.mockReturnValueOnce({
        loading: false, error: null,
        history: {egress: [{value: 0}, {value: 200}], storage: []},
        peakStorage: null,
        responseSize: 0,
      });
      formatBytesToGB.mockReturnValueOnce('200GB');
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.monthlyMetricsFormatted?.responseSizeGB).toBe('200GB');
      expect(result.current.monthlyMetricsFormatted?.isEgressTbd).toBe(false);
    });

    it('passes instanceTotals through from useInstanceTotals hook', () => {
      const {useInstanceTotals} = require('../dashboard');
      useInstanceTotals.mockReturnValueOnce({data: {totalComponents: 42}, loading: false});
      const {result} = renderHook(() => useUsageMetricsTabData());
      expect(result.current.instanceTotals.data).toEqual({totalComponents: 42});
      expect(result.current.instanceTotals.loading).toBe(false);
    });
  });
});
