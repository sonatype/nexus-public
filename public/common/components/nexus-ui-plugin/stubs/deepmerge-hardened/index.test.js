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

/**
 * Assertion test for the hardened deepmerge drop-in (see README.md).
 *
 * Lives outside src/frontend/ so it does NOT go through the nexus-ui-plugin
 * jest setup, and so that it exercises exactly the file that swagger-client
 * resolves to.
 *
 * Run with: node public/common/components/nexus-ui-plugin/stubs/deepmerge-hardened/index.test.js
 *
 * Set DEEPMERGE_IMPL to an absolute path to run the same assertions against
 * another implementation, e.g. the vulnerable upstream 4.3.1, to confirm the
 * security cases fail there:
 *   DEEPMERGE_IMPL=$PWD/node_modules/deepmerge/dist/cjs.js node <this file>
 */

'use strict';

const assert = require('assert');
const path = require('path');

const deepmerge = require(process.env.DEEPMERGE_IMPL || path.resolve(__dirname, 'index.cjs'));

function run(name, fn) {
  try {
    fn();
    console.log(`  ok  ${name}`);
  } catch (err) {
    console.error(`  FAIL ${name}`);
    console.error(err.stack || err.message);
    process.exitCode = 1;
  }
}

function assertPlainPrototype(value, label) {
  assert.strictEqual(Object.getPrototypeOf(value), Object.prototype,
    `${label}: result prototype was replaced`);
}

function assertGlobalPrototypeClean() {
  assert.strictEqual({}.polluted, undefined, 'Object.prototype was polluted');
  assert.strictEqual({}.isAdmin, undefined, 'Object.prototype was polluted');
}

// --- CVE-2026-93753: keys that must never be written to the result ---------

run('ignores an own __proto__ key on the target', () => {
  const result = deepmerge(JSON.parse('{"__proto__":{"polluted":1},"a":1}'), { b: 2 });
  assertPlainPrototype(result, 'target __proto__');
  assert.strictEqual(result.polluted, undefined);
  assert.deepStrictEqual(result, { a: 1, b: 2 });
  assertGlobalPrototypeClean();
});

run('ignores an own __proto__ key on the source', () => {
  const result = deepmerge({ a: 1 }, JSON.parse('{"__proto__":{"polluted":1},"b":2}'));
  assertPlainPrototype(result, 'source __proto__');
  assert.strictEqual(result.polluted, undefined);
  assert.deepStrictEqual(result, { a: 1, b: 2 });
  assertGlobalPrototypeClean();
});

run('ignores a source __proto__ key when the target has a null prototype', () => {
  const target = Object.create(null);
  target.a = 1;
  const result = deepmerge(target, JSON.parse('{"__proto__":{"polluted":1}}'));
  assertPlainPrototype(result, 'null-prototype target');
  assert.strictEqual(result.polluted, undefined);
  assertGlobalPrototypeClean();
});

run('ignores __proto__ keys nested inside merged and cloned values', () => {
  const result = deepmerge(
    { s: { x: 1 }, list: [JSON.parse('{"__proto__":{"isAdmin":true}}')] },
    JSON.parse('{"s":{"__proto__":{"isAdmin":true}},"t":{"__proto__":{"isAdmin":true}}}')
  );
  assertPlainPrototype(result.s, 'merged nested value');
  assertPlainPrototype(result.t, 'cloned nested value');
  assertPlainPrototype(result.list[0], 'cloned array element');
  assert.strictEqual(result.s.isAdmin, undefined);
  assert.strictEqual(result.t.isAdmin, undefined);
  assert.strictEqual(result.list[0].isAdmin, undefined);
  assertGlobalPrototypeClean();
});

run('keeps constructor and prototype as data keys without touching any prototype', () => {
  const result = deepmerge(
    JSON.parse('{"constructor":{"a":1},"properties":{"prototype":{"type":"string"}}}'),
    JSON.parse('{"constructor":{"b":2},"prototype":{"polluted":1},"properties":{"constructor":{"type":"integer"}}}')
  );
  assertPlainPrototype(result, 'constructor/prototype data keys');
  assert.deepStrictEqual(result.constructor, { a: 1, b: 2 });
  assert.deepStrictEqual(result.prototype, { polluted: 1 });
  // Upstream's own guard skips a source key the target only inherits (plain {} inherits
  // constructor); that behaviour is preserved rather than widened.
  assert.deepStrictEqual(result.properties, { prototype: { type: 'string' } });
  assert.strictEqual(Object.prototype.constructor, Object);
  assertGlobalPrototypeClean();
});

run('ignores unsafe keys in deepmerge.all', () => {
  const result = deepmerge.all([{ a: 1 }, JSON.parse('{"__proto__":{"polluted":1}}'), { b: 2 }]);
  assertPlainPrototype(result, 'deepmerge.all');
  assert.deepStrictEqual(result, { a: 1, b: 2 });
  assertGlobalPrototypeClean();
});

// --- Parity with upstream deepmerge 4.3.1 behaviour --------------------------

run('merges nested objects without mutating either input', () => {
  const target = { a: { x: 1 }, keep: true };
  const source = { a: { y: 2 }, add: 'z' };
  const result = deepmerge(target, source);
  assert.deepStrictEqual(result, { a: { x: 1, y: 2 }, keep: true, add: 'z' });
  assert.deepStrictEqual(target, { a: { x: 1 }, keep: true });
  assert.deepStrictEqual(source, { a: { y: 2 }, add: 'z' });
  assert.notStrictEqual(result.a, target.a);
});

run('concatenates arrays and clones their elements by default', () => {
  const element = { v: 1 };
  const result = deepmerge({ list: [element] }, { list: [{ v: 2 }] });
  assert.deepStrictEqual(result, { list: [{ v: 1 }, { v: 2 }] });
  assert.notStrictEqual(result.list[0], element);
});

run('lets the source win when target and source types differ', () => {
  assert.deepStrictEqual(deepmerge({ a: [1] }, { a: { b: 1 } }), { a: { b: 1 } });
  assert.deepStrictEqual(deepmerge({ a: { b: 1 } }, { a: [1] }), { a: [1] });
  assert.deepStrictEqual(deepmerge({ a: 1 }, { a: { b: 1 } }), { a: { b: 1 } });
  assert.deepStrictEqual(deepmerge([1], [2]), [1, 2]);
});

run('keeps Date, RegExp and React elements by reference', () => {
  const date = new Date(0);
  const regexp = /x/;
  const element = { $$typeof: Symbol.for('react.element') };
  const result = deepmerge({}, { date, regexp, element });
  assert.strictEqual(result.date, date);
  assert.strictEqual(result.regexp, regexp);
  assert.strictEqual(result.element, element);
});

run('honours the clone: false option', () => {
  const nested = { x: 1 };
  const result = deepmerge({}, { nested }, { clone: false });
  assert.strictEqual(result.nested, nested);
});

run('honours custom arrayMerge and isMergeableObject options', () => {
  const overwrite = (_target, source) => source;
  assert.deepStrictEqual(deepmerge({ a: [1] }, { a: [2] }, { arrayMerge: overwrite }), { a: [2] });

  const onlyPlain = (value) => Object.prototype.toString.call(value) === '[object Object]';
  const map = new Map();
  const result = deepmerge({}, { map }, { isMergeableObject: onlyPlain });
  assert.strictEqual(result.map, map);
});

run('exposes cloneUnlessOtherwiseSpecified to custom arrayMerge implementations', () => {
  let exposed;
  deepmerge({ a: [1] }, { a: [2] }, {
    arrayMerge: (target, source, options) => {
      exposed = options.cloneUnlessOtherwiseSpecified;
      return source;
    }
  });
  assert.strictEqual(typeof exposed, 'function');
});

run('merges enumerable own symbol keys', () => {
  const symbol = Symbol('s');
  const result = deepmerge({ [symbol]: { a: 1 } }, { [symbol]: { b: 2 } });
  assert.deepStrictEqual(result[symbol], { a: 1, b: 2 });
});

run('does not merge a key that only exists up the target prototype chain', () => {
  const target = Object.create({ inherited: { a: 1 } });
  const result = deepmerge(target, { inherited: { b: 2 } });
  assert.strictEqual(Object.prototype.hasOwnProperty.call(result, 'inherited'), false);
});

// The exact call swagger-client makes for a "mergeDeep" patch while resolving allOf
// (swagger-client/es/resolver/specmap/lib/index.js).
run('supports swagger-client mergeDeep with an enum customMerge', () => {
  const customMerge = (key) => {
    if (key === 'enum') {
      return (targetElement, sourceElement) => {
        if (Array.isArray(targetElement) && Array.isArray(sourceElement)) {
          return [...new Set([...targetElement, ...sourceElement])];
        }
        return deepmerge(targetElement, sourceElement);
      };
    }
    return undefined;
  };
  const result = deepmerge(
    { type: 'object', properties: { a: { type: 'string', enum: ['x', 'y'] } } },
    { properties: { a: { enum: ['y', 'z'] }, b: { type: 'integer' } } },
    { customMerge }
  );
  assert.deepStrictEqual(result, {
    type: 'object',
    properties: { a: { type: 'string', enum: ['x', 'y', 'z'] }, b: { type: 'integer' } }
  });
});

run('deepmerge.all rejects a non-array argument and merges in order', () => {
  assert.throws(() => deepmerge.all({}), /first argument should be an array/);
  assert.deepStrictEqual(deepmerge.all([{ a: 1 }, { a: 2, b: 1 }, { c: 1 }]), { a: 2, b: 1, c: 1 });
});

if (process.exitCode) {
  console.error('\ndeepmerge-hardened: one or more assertions failed');
  process.exit(process.exitCode);
} else {
  console.log('\ndeepmerge-hardened: all assertions passed');
}
