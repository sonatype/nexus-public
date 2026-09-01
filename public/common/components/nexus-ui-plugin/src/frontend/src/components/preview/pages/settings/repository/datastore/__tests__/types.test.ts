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
  validateConnectionPool,
  validateJdbcParameters,
  parseAdvancedString,
  serializeParameters,
  calculateEffectiveConfig,
} from '../types';
import { JdbcParameter } from '../JdbcParameterEditor';

describe('validateConnectionPool', () => {
  it('returns undefined for valid values', () => {
    expect(validateConnectionPool(1)).toBeUndefined();
    expect(validateConnectionPool(100)).toBeUndefined();
    expect(validateConnectionPool(3000)).toBeUndefined();
    expect(validateConnectionPool('50')).toBeUndefined();
  });

  it('returns error for values below minimum', () => {
    expect(validateConnectionPool(0)).toBe('Must be at least 1');
    expect(validateConnectionPool(-1)).toBe('Must be at least 1');
    expect(validateConnectionPool('0')).toBe('Must be at least 1');
  });

  it('returns error for values above maximum', () => {
    expect(validateConnectionPool(3001)).toBe('Must be at most 3000');
    expect(validateConnectionPool(5000)).toBe('Must be at most 3000');
    expect(validateConnectionPool('4000')).toBe('Must be at most 3000');
  });

  it('returns error for non-integer values', () => {
    expect(validateConnectionPool(1.5)).toBe('Must be a whole number');
    expect(validateConnectionPool('1.5')).toBe('Must be a whole number');
  });

  it('returns error for non-numeric strings', () => {
    expect(validateConnectionPool('abc')).toBe('Must be a valid number');
    expect(validateConnectionPool('')).toBe('Must be a valid number');
  });
});

describe('validateJdbcParameters', () => {
  it('returns no errors for valid parameters', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      { id: '2', name: 'connectTimeout', value: '5000', isDefault: false, isCustom: true },
    ];

    const result = validateJdbcParameters(params);
    expect(result.hasBlockingErrors).toBe(false);
    expect(result.validations).toEqual([]);
  });

  it('returns error for empty parameter name', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: '', value: '30000', isDefault: false, isCustom: true },
    ];

    const result = validateJdbcParameters(params);
    expect(result.hasBlockingErrors).toBe(true);
    expect(result.validations).toContainEqual(
      expect.objectContaining({ id: '1', error: 'Parameter name is required' })
    );
  });

  it('returns error for empty parameter value', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'socketTimeout', value: '', isDefault: false, isCustom: true },
    ];

    const result = validateJdbcParameters(params);
    expect(result.hasBlockingErrors).toBe(true);
    expect(result.validations).toContainEqual(
      expect.objectContaining({ id: '1', error: 'Value is required' })
    );
  });

  it('returns error for duplicate parameter names', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      { id: '2', name: 'socketTimeout', value: '60000', isDefault: false, isCustom: true },
    ];

    const result = validateJdbcParameters(params);
    expect(result.hasBlockingErrors).toBe(true);
    expect(result.validations).toContainEqual(
      expect.objectContaining({ id: '2', error: 'Duplicate parameter name' })
    );
  });

  it('returns warning for unknown parameters (non-blocking)', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'unknownParam', value: 'value', isDefault: false, isCustom: true },
    ];

    const result = validateJdbcParameters(params);
    expect(result.hasBlockingErrors).toBe(false);
    expect(result.validations).toContainEqual(
      expect.objectContaining({
        id: '1',
        warning: expect.stringContaining('Unknown JDBC parameter'),
      })
    );
  });

  it('skips validation for default read-only parameters', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: '', value: '', isDefault: true, isCustom: false },
    ];

    const result = validateJdbcParameters(params);
    expect(result.hasBlockingErrors).toBe(false);
    expect(result.validations).toEqual([]);
  });
});

describe('parseAdvancedString', () => {
  it('returns empty array for empty string', () => {
    expect(parseAdvancedString('')).toEqual([]);
    expect(parseAdvancedString('  ')).toEqual([]);
  });

  it('parses newline-separated parameters', () => {
    const result = parseAdvancedString('socketTimeout=30000\nconnectTimeout=5000');

    expect(result).toHaveLength(2);
    expect(result[0]).toMatchObject({ name: 'socketTimeout', value: '30000', isCustom: true });
    expect(result[1]).toMatchObject({ name: 'connectTimeout', value: '5000', isCustom: true });
  });

  it('parses CRLF-separated parameters', () => {
    const result = parseAdvancedString('socketTimeout=30000\r\nconnectTimeout=5000');

    expect(result).toHaveLength(2);
    expect(result[0]).toMatchObject({ name: 'socketTimeout', value: '30000' });
    expect(result[1]).toMatchObject({ name: 'connectTimeout', value: '5000' });
  });

  it('handles values with equals signs', () => {
    const result = parseAdvancedString('param=value=with=equals');

    expect(result).toHaveLength(1);
    expect(result[0]).toMatchObject({ name: 'param', value: 'value=with=equals' });
  });

  it('trims whitespace', () => {
    const result = parseAdvancedString('  socketTimeout = 30000 \n connectTimeout = 5000  ');

    expect(result).toHaveLength(2);
    expect(result[0]).toMatchObject({ name: 'socketTimeout', value: '30000' });
  });

  it('generates unique IDs for each parameter', () => {
    const result = parseAdvancedString('a=1\nb=2');

    expect(result[0].id).not.toBe(result[1].id);
  });
});

describe('serializeParameters', () => {
  it('returns empty string for empty array', () => {
    expect(serializeParameters([])).toBe('');
  });

  it('serializes custom parameters with newline separator', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      { id: '2', name: 'connectTimeout', value: '5000', isDefault: false, isCustom: true },
    ];

    expect(serializeParameters(params)).toBe('socketTimeout=30000\nconnectTimeout=5000');
  });

  it('excludes default parameters from serialization', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'defaultParam', value: 'value', isDefault: true, isCustom: false },
      { id: '2', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
    ];

    expect(serializeParameters(params)).toBe('socketTimeout=30000');
  });

  it('excludes parameters with empty names or values', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: '', value: '30000', isDefault: false, isCustom: true },
      { id: '2', name: 'socketTimeout', value: '', isDefault: false, isCustom: true },
      { id: '3', name: 'connectTimeout', value: '5000', isDefault: false, isCustom: true },
    ];

    expect(serializeParameters(params)).toBe('connectTimeout=5000');
  });
});

describe('calculateEffectiveConfig', () => {
  it('returns empty array for no parameters', () => {
    expect(calculateEffectiveConfig([])).toEqual([]);
  });

  it('includes default parameters with Default source', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'defaultParam', value: 'value', isDefault: true, isCustom: false },
    ];

    const result = calculateEffectiveConfig(params);
    expect(result).toContainEqual({
      name: 'defaultParam',
      value: 'value',
      source: 'Default',
    });
  });

  it('includes custom parameters with Custom source', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
    ];

    const result = calculateEffectiveConfig(params);
    expect(result).toContainEqual({
      name: 'socketTimeout',
      value: '30000',
      source: 'Custom',
    });
  });

  it('custom parameters override defaults', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'socketTimeout', value: '10000', isDefault: true, isCustom: false },
      { id: '2', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
    ];

    const result = calculateEffectiveConfig(params);
    expect(result).toHaveLength(1);
    expect(result[0]).toEqual({
      name: 'socketTimeout',
      value: '30000',
      source: 'Custom',
    });
  });

  it('handles case-insensitive override detection', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: 'SocketTimeout', value: '10000', isDefault: true, isCustom: false },
      { id: '2', name: 'sockettimeout', value: '30000', isDefault: false, isCustom: true },
    ];

    const result = calculateEffectiveConfig(params);
    expect(result).toHaveLength(1);
    // The last entry wins (custom parameter)
    expect(result[0].source).toBe('Custom');
  });

  it('excludes parameters with empty names', () => {
    const params: JdbcParameter[] = [
      { id: '1', name: '', value: '30000', isDefault: false, isCustom: true },
      { id: '2', name: 'socketTimeout', value: '5000', isDefault: false, isCustom: true },
    ];

    const result = calculateEffectiveConfig(params);
    expect(result).toHaveLength(1);
    expect(result[0].name).toBe('socketTimeout');
  });
});


