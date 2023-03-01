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

describe('Utils', () => {
    it('rejects decimal values when requested', () => {
      expect(Utils.isInRange({value: '1.0', max:2, allowDecimals: false}))
          .toBe('This field must not contain decimal values');
    });
    it('allows non decimal values', () => {
      expect(Utils.isInRange({value: '1', max:2, allowDecimals: false})).toBeNull();
    });

  describe('timeoutPromise', function() {
    it('returns a promise that resolves after the specified amount of time', async function() {
      jest.useFakeTimers();
      const delayedFn = jest.fn();

      Utils.timeoutPromise(12).then(delayedFn);

      expect(delayedFn).not.toHaveBeenCalled();

      await jest.advanceTimersByTime(11);
      expect(delayedFn).not.toHaveBeenCalled();

      await jest.advanceTimersByTime(1);
      expect(delayedFn).toHaveBeenCalled();
    });
  });

  describe('isInstanceOfAny', function() {
    function Fixture() {}
    function Fixture2() {}

    it('returns true if the passed value is an instance of one of the specified constructors', function() {
      expect(Utils.isInstanceOfAny([Object], {})).toBe(true);
      expect(Utils.isInstanceOfAny([Array], [])).toBe(true);
      expect(Utils.isInstanceOfAny([Object], [])).toBe(true);
      expect(Utils.isInstanceOfAny([Object], new Fixture())).toBe(true);
      expect(Utils.isInstanceOfAny([Fixture], new Fixture())).toBe(true);
      expect(Utils.isInstanceOfAny([Object], Fixture)).toBe(true);
      expect(Utils.isInstanceOfAny([Function], Fixture)).toBe(true);
      expect(Utils.isInstanceOfAny([Set], new Set())).toBe(true);
      expect(Utils.isInstanceOfAny([Function], Set)).toBe(true);

      expect(Utils.isInstanceOfAny([Object, Array, Set, Fixture], {})).toBe(true);
      expect(Utils.isInstanceOfAny([Array, Object, Fixture], [])).toBe(true);
      expect(Utils.isInstanceOfAny([Object, Fixture], [])).toBe(true);
      expect(Utils.isInstanceOfAny([Object, Fixture2], new Fixture())).toBe(true);
      expect(Utils.isInstanceOfAny([Fixture, Fixture2, Array], new Fixture())).toBe(true);
      expect(Utils.isInstanceOfAny([Object, Fixture], Fixture)).toBe(true);
      expect(Utils.isInstanceOfAny([Function, Array], Fixture)).toBe(true);
      expect(Utils.isInstanceOfAny([Set, Array], new Set())).toBe(true);
      expect(Utils.isInstanceOfAny([Function], Set)).toBe(true);

      expect(Utils.isInstanceOfAny([Array, Set, Fixture], {})).toBe(false);
      expect(Utils.isInstanceOfAny([Set, Fixture], [])).toBe(false);
      expect(Utils.isInstanceOfAny([Fixture], [])).toBe(false);
      expect(Utils.isInstanceOfAny([Fixture2], new Fixture())).toBe(false);
      expect(Utils.isInstanceOfAny([Fixture2, Array], new Fixture())).toBe(false);
      expect(Utils.isInstanceOfAny([Fixture], Fixture)).toBe(false);
      expect(Utils.isInstanceOfAny([Array], Fixture)).toBe(false);
      expect(Utils.isInstanceOfAny([Array], new Set())).toBe(false);
      expect(Utils.isInstanceOfAny([Set], Set)).toBe(false);
    });

    it('returns true if the passed value is a primitive whose corresponding boxed type is one of the specified ' +
        'constructors', function() {
      expect(Utils.isInstanceOfAny([Boolean], false)).toBe(true);
      expect(Utils.isInstanceOfAny([String], '')).toBe(true);
      expect(Utils.isInstanceOfAny([Number], 1)).toBe(true);
      expect(Utils.isInstanceOfAny([Number], NaN)).toBe(true);

      expect(Utils.isInstanceOfAny([Boolean, String], false)).toBe(true);
      expect(Utils.isInstanceOfAny([String, Number], '')).toBe(true);
      expect(Utils.isInstanceOfAny([Number, String], 1)).toBe(true);
      expect(Utils.isInstanceOfAny([Number, Boolean], NaN)).toBe(true);

      expect(Utils.isInstanceOfAny([String], false)).toBe(false);
      expect(Utils.isInstanceOfAny([Number], '')).toBe(false);
      expect(Utils.isInstanceOfAny([String], 1)).toBe(false);
      expect(Utils.isInstanceOfAny([Boolean], NaN)).toBe(false);
    });

    it('returns false if the passed value is a primitive whose corresponding boxed type is a only a subtype of ' +
        'the specified constructors', function() {
      expect(Utils.isInstanceOfAny([Object], false)).toBe(false);
      expect(Utils.isInstanceOfAny([Object], '')).toBe(false);
      expect(Utils.isInstanceOfAny([Object], 1)).toBe(false);
      expect(Utils.isInstanceOfAny([Object], NaN)).toBe(false);
    });

    it('returns false if the passed value is null', function() {
      expect(Utils.isInstanceOfAny([Object], null)).toBe(false);
      expect(Utils.isInstanceOfAny([Boolean], null)).toBe(false);
      expect(Utils.isInstanceOfAny([String], null)).toBe(false);
      expect(Utils.isInstanceOfAny([Number], null)).toBe(false);
    });

    it('returns false if the constructor list is empty', function() {
      expect(Utils.isInstanceOfAny([], false)).toBe(false);
      expect(Utils.isInstanceOfAny([], '')).toBe(false);
      expect(Utils.isInstanceOfAny([], 1)).toBe(false);
      expect(Utils.isInstanceOfAny([], NaN)).toBe(false);
      expect(Utils.isInstanceOfAny([], {})).toBe(false);
      expect(Utils.isInstanceOfAny([], [])).toBe(false);
      expect(Utils.isInstanceOfAny([], new Fixture())).toBe(false);
      expect(Utils.isInstanceOfAny([], Fixture)).toBe(false);
      expect(Utils.isInstanceOfAny([], new Set())).toBe(false);
      expect(Utils.isInstanceOfAny([], Set)).toBe(false);
    });
  });
});

