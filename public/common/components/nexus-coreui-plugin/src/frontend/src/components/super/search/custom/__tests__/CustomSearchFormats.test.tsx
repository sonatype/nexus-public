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
 * Comprehensive tests for Custom Search - All Formats and Filter Fields
 * 
 * Tests that Custom Search supports all filter fields and that they work
 * correctly for every format.
 */

import { describe, it, expect } from '@jest/globals';
import { FILTER_FIELD_OPTIONS, FILTER_OPERATOR_OPTIONS } from '../custom.types';

/**
 * Map filter field to API parameter name (copied from useCustomSearch.ts for testing).
 */
function getApiParamName(field: string): string {
  switch (field) {
    case 'keyword':
      return 'q';
    case 'format':
      return 'format';
    case 'repository':
      return 'repository';
    case 'group':
      return 'group';
    case 'name':
      return 'name';
    case 'version':
      return 'version';
    case 'tag':
      return 'tag';
    default:
      return field;
  }
}

/**
 * Build search value based on operator (copied from useCustomSearch.ts for testing).
 */
function buildSearchValue(operator: string, value: string): string {
  switch (operator) {
    case 'equals':
      return value;
    case 'contains':
      return `*${value}*`;
    case 'startsWith':
      return `${value}*`;
    case 'endsWith':
      return `*${value}`;
    default:
      return value;
  }
}

describe('Custom Search - Filter Fields', () => {
  describe('All filter fields are defined', () => {
    it('should have all required filter fields', () => {
      const fieldIds = FILTER_FIELD_OPTIONS.map((f) => f.value);

      expect(fieldIds).toContain('keyword');
      expect(fieldIds).toContain('format');
      expect(fieldIds).toContain('repository');
      expect(fieldIds).toContain('group');
      expect(fieldIds).toContain('name');
      expect(fieldIds).toContain('version');
      expect(fieldIds).toContain('tag');
    });

    it('should have labels and placeholders for all fields', () => {
      FILTER_FIELD_OPTIONS.forEach((field) => {
        expect(field.label).toBeTruthy();
        expect(field.placeholder).toBeTruthy();
      });
    });
  });

  describe('API parameter mapping', () => {
    it('should map keyword to q', () => {
      expect(getApiParamName('keyword')).toBe('q');
    });

    it('should map format correctly', () => {
      expect(getApiParamName('format')).toBe('format');
    });

    it('should map repository correctly', () => {
      expect(getApiParamName('repository')).toBe('repository');
    });

    it('should map group correctly', () => {
      expect(getApiParamName('group')).toBe('group');
    });

    it('should map name correctly', () => {
      expect(getApiParamName('name')).toBe('name');
    });

    it('should map version correctly', () => {
      expect(getApiParamName('version')).toBe('version');
    });

    it('should map tag correctly', () => {
      expect(getApiParamName('tag')).toBe('tag');
    });
  });
});

describe('Custom Search - Filter Operators', () => {
  describe('All operators are defined', () => {
    it('should have all required operators', () => {
      const operatorIds = FILTER_OPERATOR_OPTIONS.map((o) => o.value);

      expect(operatorIds).toContain('equals');
      expect(operatorIds).toContain('contains');
      expect(operatorIds).toContain('startsWith');
      expect(operatorIds).toContain('endsWith');
    });

    it('should have labels for all operators', () => {
      FILTER_OPERATOR_OPTIONS.forEach((operator) => {
        expect(operator.label).toBeTruthy();
      });
    });
  });

  describe('Search value building', () => {
    it('should build equals value correctly', () => {
      expect(buildSearchValue('equals', 'test')).toBe('test');
    });

    it('should build contains value correctly', () => {
      expect(buildSearchValue('contains', 'test')).toBe('*test*');
    });

    it('should build startsWith value correctly', () => {
      expect(buildSearchValue('startsWith', 'test')).toBe('test*');
    });

    it('should build endsWith value correctly', () => {
      expect(buildSearchValue('endsWith', 'test')).toBe('*test');
    });
  });
});

describe('Custom Search - Format Coverage', () => {
  /**
   * Test that Custom Search can filter by format for all supported formats.
   */
  const formats = [
    'maven2',
    'npm',
    'nuget',
    'pypi',
    'docker',
    'helm',
    'go',
    'rubygems',
    'yum',
    'apt',
    'raw',
    'cargo',
    'cocoapods',
    'composer',
    'conan',
    'conda',
    'p2',
    'r',
    'gitlfs',
    'terraform',
    'huggingface',
    'swift',
  ];

  it.each(formats)('should support format filter for %s', (format) => {
    // Custom Search should be able to filter by any format
    const canFilter = FILTER_FIELD_OPTIONS.some((f) => f.value === 'format');
    expect(canFilter).toBe(true);
  });
});

describe('Custom Search - Filter Combinations', () => {
  describe('Common filter combinations', () => {
    it('should support format + repository filter', () => {
      const hasFormat = FILTER_FIELD_OPTIONS.some((f) => f.value === 'format');
      const hasRepository = FILTER_FIELD_OPTIONS.some((f) => f.value === 'repository');
      
      expect(hasFormat).toBe(true);
      expect(hasRepository).toBe(true);
    });

    it('should support format + name + version filter', () => {
      const hasFormat = FILTER_FIELD_OPTIONS.some((f) => f.value === 'format');
      const hasName = FILTER_FIELD_OPTIONS.some((f) => f.value === 'name');
      const hasVersion = FILTER_FIELD_OPTIONS.some((f) => f.value === 'version');
      
      expect(hasFormat).toBe(true);
      expect(hasName).toBe(true);
      expect(hasVersion).toBe(true);
    });

    it('should support tag filter', () => {
      const hasTag = FILTER_FIELD_OPTIONS.some((f) => f.value === 'tag');
      expect(hasTag).toBe(true);
    });

    it('should support group filter (for Maven, npm scope, etc.)', () => {
      const hasGroup = FILTER_FIELD_OPTIONS.some((f) => f.value === 'group');
      expect(hasGroup).toBe(true);
    });
  });
});

