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
    it('sets validation errors from save errors', () => {
      const {validationErrors} = Utils.fieldProps('test', makeContext({
        saveErrors: {
          test: 'error'
        }
      }));

      expect(validationErrors).toBe('error');
    });

    it('ignores validation errors if not touched', () => {
      const {validationErrors} = Utils.fieldProps('test', makeContext({isTouched: {test: false}}));

      expect(validationErrors).toBeNull();
    });

    it('sets validation errors when touched', () => {
      const {validationErrors} = Utils.fieldProps('test', makeContext({
        isTouched: {
          test: true
        },
        validationErrors: {
          test: 'error'
        }
      }));

      expect(validationErrors).toBe('error');
    });

    it('prefers validation errors to saveErrors', () => {
      const {validationErrors} = Utils.fieldProps('test', makeContext({
        isTouched: {
          test: true
        },
        validationErrors: {
          test: 'error'
        },
        saveErrors: {
          test: 'saveError'
        }
      }));

      expect(validationErrors).toBe('error');
    });

    it('uses saveErrors when saveErrorData matches data', () => {
      const {validationErrors} = Utils.fieldProps('test', makeContext({
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
      }));

      expect(validationErrors).toBe('error');
    });

    it('does not use saveErrors when saveErrorData does not match data', () => {
      const {validationErrors} = Utils.fieldProps('test', makeContext({
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
      }));

      expect(validationErrors).toBe(null);
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
