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
  FILTER_FIELD_OPTIONS,
  FILTER_OPERATOR_OPTIONS,
  createEmptyFilter,
} from '../custom.types';

describe('custom.types', () => {
  describe('FILTER_FIELD_OPTIONS', () => {
    it('contains all expected fields', () => {
      const fieldValues = FILTER_FIELD_OPTIONS.map((opt) => opt.value);

      expect(fieldValues).toContain('keyword');
      expect(fieldValues).toContain('format');
      expect(fieldValues).toContain('repository');
      expect(fieldValues).toContain('group');
      expect(fieldValues).toContain('name');
      expect(fieldValues).toContain('version');
      expect(fieldValues).toContain('tag');
    });

    it('has exactly 7 field options', () => {
      expect(FILTER_FIELD_OPTIONS).toHaveLength(7);
    });

    it('each option has required properties', () => {
      FILTER_FIELD_OPTIONS.forEach((option) => {
        expect(option.value).toBeTruthy();
        expect(option.label).toBeTruthy();
        expect(option.placeholder).toBeTruthy();
      });
    });

    it('keyword is the first option', () => {
      expect(FILTER_FIELD_OPTIONS[0].value).toBe('keyword');
    });

    it('has meaningful placeholders', () => {
      const keywordOption = FILTER_FIELD_OPTIONS.find((o) => o.value === 'keyword');
      expect(keywordOption?.placeholder).toContain('spring-boot');

      const formatOption = FILTER_FIELD_OPTIONS.find((o) => o.value === 'format');
      expect(formatOption?.placeholder).toMatch(/maven|npm|docker/i);

      const versionOption = FILTER_FIELD_OPTIONS.find((o) => o.value === 'version');
      expect(versionOption?.placeholder).toMatch(/\d+\.\d+\.\d+/);
    });
  });

  describe('FILTER_OPERATOR_OPTIONS', () => {
    it('contains all expected operators', () => {
      const operatorValues = FILTER_OPERATOR_OPTIONS.map((opt) => opt.value);

      expect(operatorValues).toContain('equals');
      expect(operatorValues).toContain('contains');
      expect(operatorValues).toContain('startsWith');
      expect(operatorValues).toContain('endsWith');
    });

    it('has exactly 4 operator options', () => {
      expect(FILTER_OPERATOR_OPTIONS).toHaveLength(4);
    });

    it('each option has required properties', () => {
      FILTER_OPERATOR_OPTIONS.forEach((option) => {
        expect(option.value).toBeTruthy();
        expect(option.label).toBeTruthy();
      });
    });

    it('equals is the first option', () => {
      expect(FILTER_OPERATOR_OPTIONS[0].value).toBe('equals');
    });

    it('has human-readable labels', () => {
      const equalsOption = FILTER_OPERATOR_OPTIONS.find((o) => o.value === 'equals');
      expect(equalsOption?.label).toBe('Equals');

      const containsOption = FILTER_OPERATOR_OPTIONS.find((o) => o.value === 'contains');
      expect(containsOption?.label).toBe('Contains');

      const startsWithOption = FILTER_OPERATOR_OPTIONS.find((o) => o.value === 'startsWith');
      expect(startsWithOption?.label).toBe('Starts with');

      const endsWithOption = FILTER_OPERATOR_OPTIONS.find((o) => o.value === 'endsWith');
      expect(endsWithOption?.label).toBe('Ends with');
    });
  });

  describe('createEmptyFilter', () => {
    it('creates a filter with unique id', () => {
      const filter = createEmptyFilter();

      expect(filter.id).toBeTruthy();
      expect(filter.id).toMatch(/^filter-\d+-[a-z0-9]+$/);
    });

    it('creates filters with different ids on each call', () => {
      const filter1 = createEmptyFilter();
      const filter2 = createEmptyFilter();

      expect(filter1.id).not.toBe(filter2.id);
    });

    it('sets default field to keyword', () => {
      const filter = createEmptyFilter();

      expect(filter.field).toBe('keyword');
    });

    it('sets default operator to contains', () => {
      const filter = createEmptyFilter();

      expect(filter.operator).toBe('contains');
    });

    it('sets default value to empty string', () => {
      const filter = createEmptyFilter();

      expect(filter.value).toBe('');
    });

    it('returns a complete filter object', () => {
      const filter = createEmptyFilter();

      expect(filter).toHaveProperty('id');
      expect(filter).toHaveProperty('field');
      expect(filter).toHaveProperty('operator');
      expect(filter).toHaveProperty('value');
    });

    it('creates many unique filters rapidly', () => {
      const filters = Array.from({ length: 100 }, () => createEmptyFilter());
      const uniqueIds = new Set(filters.map((f) => f.id));

      expect(uniqueIds.size).toBe(100);
    });
  });
});
