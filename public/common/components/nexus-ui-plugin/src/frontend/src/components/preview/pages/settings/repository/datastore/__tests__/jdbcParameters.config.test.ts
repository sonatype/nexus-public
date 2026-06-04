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
  JDBC_PARAMETERS_CONFIG,
  getParameterDefinition,
  getAllParameterNames,
  getParametersByCategory,
  getParameterDescription,
  isKnownParameter,
  validateParameterValue,
  getAllowedValues,
  getCategoryDisplayName,
} from '../jdbcParameters.config';

describe('JDBC Parameters Config', () => {
  describe('JDBC_PARAMETERS_CONFIG', () => {
    it('contains expected number of parameters', () => {
      expect(JDBC_PARAMETERS_CONFIG.length).toBeGreaterThanOrEqual(20);
    });

    it('has all required fields for each parameter', () => {
      JDBC_PARAMETERS_CONFIG.forEach((param) => {
        expect(param.name).toBeDefined();
        expect(param.description).toBeDefined();
        expect(param.type).toBeDefined();
        expect(param.category).toBeDefined();
        expect(['string', 'number', 'boolean', 'enum']).toContain(param.type);
        expect(['connection', 'timeout', 'ssl', 'performance', 'other']).toContain(param.category);
      });
    });

    it('has unique parameter names', () => {
      const names = JDBC_PARAMETERS_CONFIG.map((p) => p.name.toLowerCase());
      const uniqueNames = new Set(names);
      expect(uniqueNames.size).toBe(names.length);
    });
  });

  describe('getParameterDefinition', () => {
    it('returns definition for known parameter', () => {
      const def = getParameterDefinition('socketTimeout');
      expect(def).toBeDefined();
      expect(def?.name).toBe('socketTimeout');
      expect(def?.type).toBe('number');
    });

    it('is case-insensitive', () => {
      expect(getParameterDefinition('SOCKETTIMEOUT')).toBeDefined();
      expect(getParameterDefinition('SocketTimeout')).toBeDefined();
    });

    it('returns undefined for unknown parameter', () => {
      expect(getParameterDefinition('unknownParam')).toBeUndefined();
    });
  });

  describe('getAllParameterNames', () => {
    it('returns array of all parameter names', () => {
      const names = getAllParameterNames();
      expect(Array.isArray(names)).toBe(true);
      expect(names.length).toBe(JDBC_PARAMETERS_CONFIG.length);
      expect(names).toContain('socketTimeout');
      expect(names).toContain('ssl');
      expect(names).toContain('sslmode');
    });
  });

  describe('getParametersByCategory', () => {
    it('returns parameters for timeout category', () => {
      const params = getParametersByCategory('timeout');
      expect(params.length).toBeGreaterThan(0);
      params.forEach((p) => expect(p.category).toBe('timeout'));
    });

    it('returns parameters for ssl category', () => {
      const params = getParametersByCategory('ssl');
      expect(params.length).toBeGreaterThan(0);
      params.forEach((p) => expect(p.category).toBe('ssl'));
    });

    it('returns empty array for non-existent category parameters', () => {
      const params = getParametersByCategory('connection');
      expect(Array.isArray(params)).toBe(true);
    });
  });

  describe('getParameterDescription', () => {
    it('returns description for known parameter', () => {
      const desc = getParameterDescription('socketTimeout');
      expect(desc).toContain('socket');
    });

    it('includes unit when present', () => {
      const desc = getParameterDescription('socketTimeout');
      expect(desc).toContain('ms');
    });

    it('includes default value when present', () => {
      const desc = getParameterDescription('socketTimeout');
      expect(desc).toContain('Default');
    });

    it('returns unknown message for unknown parameter', () => {
      const desc = getParameterDescription('unknownParam');
      expect(desc.toLowerCase()).toContain('unknown');
    });
  });

  describe('isKnownParameter', () => {
    it('returns true for known parameters', () => {
      expect(isKnownParameter('socketTimeout')).toBe(true);
      expect(isKnownParameter('ssl')).toBe(true);
      expect(isKnownParameter('sslmode')).toBe(true);
    });

    it('returns false for unknown parameters', () => {
      expect(isKnownParameter('unknownParam')).toBe(false);
      expect(isKnownParameter('randomSetting')).toBe(false);
    });

    it('is case-insensitive', () => {
      expect(isKnownParameter('SOCKETTIMEOUT')).toBe(true);
      expect(isKnownParameter('SslMode')).toBe(true);
    });
  });

  describe('validateParameterValue', () => {
    describe('boolean parameters', () => {
      it('accepts valid boolean values', () => {
        expect(validateParameterValue('ssl', 'true')).toBeUndefined();
        expect(validateParameterValue('ssl', 'false')).toBeUndefined();
      });

      it('rejects invalid boolean values', () => {
        const error = validateParameterValue('ssl', 'yes');
        expect(error).toContain('Must be one of');
      });
    });

    describe('number parameters', () => {
      it('accepts valid numbers within range', () => {
        expect(validateParameterValue('socketTimeout', '1000')).toBeUndefined();
        expect(validateParameterValue('socketTimeout', '0')).toBeUndefined();
      });

      it('rejects non-numeric values', () => {
        const error = validateParameterValue('socketTimeout', 'abc');
        expect(error).toContain('valid number');
      });

      it('rejects values below minimum', () => {
        const error = validateParameterValue('socketTimeout', '-1');
        expect(error).toContain('at least');
      });

      it('rejects values above maximum', () => {
        const error = validateParameterValue('socketTimeout', '999999999');
        expect(error).toContain('at most');
      });
    });

    describe('enum parameters', () => {
      it('accepts valid enum values', () => {
        expect(validateParameterValue('sslmode', 'disable')).toBeUndefined();
        expect(validateParameterValue('sslmode', 'require')).toBeUndefined();
        expect(validateParameterValue('sslmode', 'verify-full')).toBeUndefined();
      });

      it('rejects invalid enum values', () => {
        const error = validateParameterValue('sslmode', 'invalid');
        expect(error).toContain('Must be one of');
      });
    });

    describe('string parameters', () => {
      it('accepts any string value', () => {
        expect(validateParameterValue('ApplicationName', 'MyApp')).toBeUndefined();
        expect(validateParameterValue('sslcert', '/path/to/cert')).toBeUndefined();
      });
    });

    describe('unknown parameters', () => {
      it('returns undefined (no validation) for unknown parameters', () => {
        expect(validateParameterValue('unknownParam', 'anyValue')).toBeUndefined();
      });
    });

    describe('empty values', () => {
      it('returns undefined for empty values', () => {
        expect(validateParameterValue('ssl', '')).toBeUndefined();
        expect(validateParameterValue('ssl', '   ')).toBeUndefined();
      });
    });
  });

  describe('getAllowedValues', () => {
    it('returns allowed values for enum parameters', () => {
      const values = getAllowedValues('sslmode');
      expect(values).toContain('disable');
      expect(values).toContain('require');
      expect(values).toContain('verify-full');
    });

    it('returns allowed values for boolean parameters', () => {
      const values = getAllowedValues('ssl');
      expect(values).toContain('true');
      expect(values).toContain('false');
    });

    it('returns undefined for string/number parameters', () => {
      expect(getAllowedValues('ApplicationName')).toBeUndefined();
    });

    it('returns undefined for unknown parameters', () => {
      expect(getAllowedValues('unknownParam')).toBeUndefined();
    });
  });

  describe('getCategoryDisplayName', () => {
    it('returns display names for all categories', () => {
      expect(getCategoryDisplayName('connection')).toBe('Connection');
      expect(getCategoryDisplayName('timeout')).toBe('Timeouts');
      expect(getCategoryDisplayName('ssl')).toBe('SSL/TLS');
      expect(getCategoryDisplayName('performance')).toBe('Performance');
      expect(getCategoryDisplayName('other')).toBe('Other');
    });
  });
});


