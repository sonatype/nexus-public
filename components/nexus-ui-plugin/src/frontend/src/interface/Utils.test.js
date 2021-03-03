/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import Utils from './Utils';
import UIStrings from '../constants/UIStrings';

describe('Utils', () => {
  describe('fieldProps', () => {
    it('converts the value to a string', () => {
      const {value} = Utils.fieldProps('test', makeContext({data: {test: 1}}));
      expect(value).toBe('1');
    });

    it('converts the default value to a string', () => {
      const defaultValue = 1;
      const {value} = Utils.fieldProps('test', makeContext({data: {}}), defaultValue);
      expect(value).toBe('1');
    });

    it('sets validation errors from save errors', () => {
      const context = makeContext({saveErrors: {test: 'error'}});

      expect(Utils.fieldProps('test', context).validationErrors).toBe('error');
      expect(Utils.fieldProps(['test'], context).validationErrors).toBe('error');
    });

    it('ignores validation errors if not touched', () => {
      const context = makeContext({isTouched: {test: false}});

      expect(Utils.fieldProps('test', context).validationErrors).toBeNull();
      expect(Utils.fieldProps(['test'], context).validationErrors).toBeNull();
    });

    it('sets validation errors when touched', () => {
      const context = makeContext({
        isTouched: {
          test: true
        },
        validationErrors: {
          test: 'error'
        }
      });

      expect(Utils.fieldProps('test', context).validationErrors).toBe('error');
    });

    it('prefers validation errors to saveErrors', () => {
      const context = makeContext({
        isTouched: {
          test: true
        },
        validationErrors: {
          test: 'error'
        },
        saveErrors: {
          test: 'saveError'
        }
      });

      expect(Utils.fieldProps('test', context).validationErrors).toBe('error');
      expect(Utils.fieldProps(['test'], context).validationErrors).toBe('error');
    });

    it('uses saveErrors when saveErrorData matches data', () => {
      const context = makeContext({
        isTouched: {
          test: true
        },
        data: {
          test: 'test'
        },
        saveErrorData: {
          test: 'test'
        },
        saveErrors: {
          test: 'error'
        }
      });

      expect(Utils.fieldProps('test', context).validationErrors).toBe('error');
      expect(Utils.fieldProps(['test'], context).validationErrors).toBe('error');
    });

    it('does not use saveErrors when saveErrorData does not match data', () => {
      const context = makeContext({
        isTouched: {
          test: true
        },
        data: {
          test: 'test'
        },
        saveErrorData: {},
        saveErrors: {
          test: 'error'
        }
      });

      expect(Utils.fieldProps('test', context).validationErrors).toBeNull();
      expect(Utils.fieldProps(['test'], context).validationErrors).toBeNull();
    });

    it('is pristine for a field not included in isTouched', () => {
      expect(Utils.fieldProps('test', makeContext({})).isPristine).toBe(true);
    });

    it('is pristine for an untouched field', () => {
      expect(Utils.fieldProps('test', makeContext({
        isTouched: {
          test: false
        }
      })).isPristine).toBe(true);
    });

    it('is not pristine for a touched field', () => {
      expect(Utils.fieldProps('test', makeContext({
        isTouched: {
          test: true
        }
      })).isPristine).toBe(false);
    });

    it('is pristine for a nested field that is not touched', () => {
      expect(Utils.fieldProps(['test', 'nested'], makeContext({
        isTouched: {
          test: {
            nested: false
          }
        }
      })).isPristine).toBe(true);
    });

    it('is not pristine for a nested field that has been touched', () => {
      expect(Utils.fieldProps(['test', 'nested'], makeContext({
        isTouched: {
          test: {
            nested: true
          }
        }
      })).isPristine).toBe(false);
    });
  });

  describe('nextSortDirection', () => {
    it('defaults to ASC for other fields', () => {
      const sortDirection = Utils.nextSortDirection('name')({
        sortField: '',
        sortDirection: null
      });

      expect(sortDirection).toBe(Utils.ASC);
    });

    it('defaults to ASC for this field', () => {
      const sortDirection = Utils.nextSortDirection('name')({
        sortField: 'name',
        sortDirection: null
      });

      expect(sortDirection).toBe(Utils.ASC);
    });

    it('switches from ASC to DESC for this field', () => {
      const sortDirection = Utils.nextSortDirection('name')({
        sortField: 'name',
        sortDirection: Utils.ASC
      });

      expect(sortDirection).toBe(Utils.DESC);
    });

    it('switches from DESC to ASC for this field', () => {
      const sortDirection = Utils.nextSortDirection('name')({
        sortField: 'name',
        sortDirection: Utils.DESC
      });

      expect(sortDirection).toBe(Utils.ASC);
    });
  });

  describe('sortDataByFieldAndDirection', () => {
    it('sorts ascending', () => {
      const sortedData = Utils.sortDataByFieldAndDirection({
        sortField: 'name',
        sortDirection: Utils.ASC,
        data: [{name: 'c'}, {name: 'a'}, {name: 'b'}]
      });

      expect(sortedData).toStrictEqual([{name: 'a'}, {name: 'b'}, {name: 'c'}]);
    });

    it('sorts descending', () => {
      const sortedData = Utils.sortDataByFieldAndDirection({
        sortField: 'name',
        sortDirection: Utils.DESC,
        data: [{name: 'c'}, {name: 'a'}, {name: 'b'}]
      });

      expect(sortedData).toStrictEqual([{name: 'c'}, {name: 'b'}, {name: 'a'}]);
    });

    it('does no sorting on equal values', () => {
      expect(Utils.sortDataByFieldAndDirection({
        sortField: 'blobCount',
        sortDirection: Utils.DESC,
        data: [{name: 'a', blobCount: 0}, {name: 'b', blobCount: 0}, {name: 'c', blobCount: 0}]
      })).toStrictEqual([{name: 'a', blobCount: 0}, {name: 'b', blobCount: 0}, {name: 'c', blobCount: 0}])

      expect(Utils.sortDataByFieldAndDirection({
        sortField: 'blobCount',
        sortDirection: Utils.ASC,
        data: [{name: 'a', blobCount: 0}, {name: 'b', blobCount: 0}, {name: 'c', blobCount: 0}]
      })).toStrictEqual([{name: 'a', blobCount: 0}, {name: 'b', blobCount: 0}, {name: 'c', blobCount: 0}])
    })
  });

  describe('isInvalid', () => {
    it('returns false for a null or undefined errors object', () => {
      expect(Utils.isInvalid(null)).toBe(false);
      expect(Utils.isInvalid(undefined)).toBe(false);
    });

    it('returns false for a key with a null or undefined value', () => {
      expect(Utils.isInvalid({test: null})).toBe(false);
      expect(Utils.isInvalid({test: null})).toBe(false);
    });

    it('returns true for a key with a string value', () => {
      expect(Utils.isInvalid({test: 'error message'})).toBe(true);
    });

    it('returns false for nested objects with no errors', () => {
      expect(Utils.isInvalid({test: {nested: null}})).toBe(false);
      expect(Utils.isInvalid({test: {nested: undefined}})).toBe(false);
    });

    it('returns true for nested objects with errors', () => {
      expect(Utils.isInvalid({test: {nested: 'error'}})).toBe(true);
    });

    it('returns false for empty arrays', () => {
      expect(Utils.isInvalid({test: []})).toBe(false);
    });

    it('returns true for arrays of error messages', () => {
      expect(Utils.isInvalid({test: ['error']})).toBe(true);
    });
  });

  describe('isInRange', () => {
    it('ignores null', () => {
      expect(Utils.isInRange({value: null})).toBeNull();
    });
    it('ignores undefined', () => {
      expect(Utils.isInRange({value: undefined})).toBeNull();
    });
    it('ignores empty string', () => {
      expect(Utils.isInRange({value: ''})).toBeNull();
    });
    it('handles numbers equal to the max range', () => {
      expect(Utils.isInRange({value: 10, max: 10})).toBeNull();
    });
    it('handles numbers equal to the min range', () => {
      expect(Utils.isInRange({value: 0, min: 0})).toBeNull();
    });
    it('handles a string number in the range', () => {
      expect(Utils.isInRange({value: '5', min: 0, max: 10})).toBeNull();
    });
    it('rejects numbers greater than the max', () => {
      expect(Utils.isInRange({value: 1, max: 0})).toBe('The maximum value for this field is 0');
    });
    it('rejects numbers less than the min', () => {
      expect(Utils.isInRange({value: 0, min: 1})).toBe('The minimum value for this field is 1');
    });
    it('rejects non-numeric values', () => {
      expect(Utils.isInRange({value: '1xx', min: 0})).toBe('This field must contain a numeric value');
    })
  });
});

function makeContext({...ctx}) {
  return {
    context: {
      data: {},
      isTouched: {},
      ...ctx
    }
  }
}
