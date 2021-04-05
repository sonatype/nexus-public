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
import FormUtils from './FormUtils';

describe('FormUtils', () => {
  describe('fieldProps', () => {
    it('converts the value to a string', () => {
      const {value} = FormUtils.fieldProps('test', makeContext({data: {test: 1}}));
      expect(value).toBe('1');
    });

    it('converts the default value to a string', () => {
      const defaultValue = 1;
      const {value} = FormUtils.fieldProps('test', makeContext({data: {}}), defaultValue);
      expect(value).toBe('1');
    });

    it('sets validation errors from save errors', () => {
      const context = makeContext({saveErrors: {test: 'error'}});

      expect(FormUtils.fieldProps('test', context).validationErrors).toBe('error');
      expect(FormUtils.fieldProps(['test'], context).validationErrors).toBe('error');
    });

    it('ignores validation errors if not touched', () => {
      const context = makeContext({isTouched: {test: false}});

      expect(FormUtils.fieldProps('test', context).validationErrors).toBeNull();
      expect(FormUtils.fieldProps(['test'], context).validationErrors).toBeNull();
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

      expect(FormUtils.fieldProps('test', context).validationErrors).toBe('error');
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

      expect(FormUtils.fieldProps('test', context).validationErrors).toBe('error');
      expect(FormUtils.fieldProps(['test'], context).validationErrors).toBe('error');
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

      expect(FormUtils.fieldProps('test', context).validationErrors).toBe('error');
      expect(FormUtils.fieldProps(['test'], context).validationErrors).toBe('error');
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

      expect(FormUtils.fieldProps('test', context).validationErrors).toBeNull();
      expect(FormUtils.fieldProps(['test'], context).validationErrors).toBeNull();
    });

    it('is pristine for a field not included in isTouched', () => {
      expect(FormUtils.fieldProps('test', makeContext({})).isPristine).toBe(true);
    });

    it('is pristine for an untouched field', () => {
      expect(FormUtils.fieldProps('test', makeContext({
        isTouched: {
          test: false
        }
      })).isPristine).toBe(true);
    });

    it('is not pristine for a touched field', () => {
      expect(FormUtils.fieldProps('test', makeContext({
        isTouched: {
          test: true
        }
      })).isPristine).toBe(false);
    });

    it('is pristine for a nested field that is not touched', () => {
      expect(FormUtils.fieldProps(['test', 'nested'], makeContext({
        isTouched: {
          test: {
            nested: false
          }
        }
      })).isPristine).toBe(true);
    });

    it('is not pristine for a nested field that has been touched', () => {
      expect(FormUtils.fieldProps(['test', 'nested'], makeContext({
        isTouched: {
          test: {
            nested: true
          }
        }
      })).isPristine).toBe(false);
    });
  });

  describe('isInvalid', () => {
    it('returns false for a null or undefined errors object', () => {
      expect(FormUtils.isInvalid(null)).toBe(false);
      expect(FormUtils.isInvalid(undefined)).toBe(false);
    });

    it('returns false for a key with a null or undefined value', () => {
      expect(FormUtils.isInvalid({test: null})).toBe(false);
      expect(FormUtils.isInvalid({test: null})).toBe(false);
    });

    it('returns true for a key with a string value', () => {
      expect(FormUtils.isInvalid({test: 'error message'})).toBe(true);
    });

    it('returns false for nested objects with no errors', () => {
      expect(FormUtils.isInvalid({test: {nested: null}})).toBe(false);
      expect(FormUtils.isInvalid({test: {nested: undefined}})).toBe(false);
    });

    it('returns true for nested objects with errors', () => {
      expect(FormUtils.isInvalid({test: {nested: 'error'}})).toBe(true);
    });

    it('returns false for empty arrays', () => {
      expect(FormUtils.isInvalid({test: []})).toBe(false);
    });

    it('returns true for arrays of error messages', () => {
      expect(FormUtils.isInvalid({test: ['error']})).toBe(true);
    });
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
