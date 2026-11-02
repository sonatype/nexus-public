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
 * Assertion test for ForbidStubbedModulesPlugin.
 *
 * Lives outside src/frontend/ so it does NOT go through the nexus-ui-plugin
 * jest setup. It only needs the rspack plugin class; no jest or rspack
 * environment is required.
 *
 * Run with: node public/common/components/nexus-ui-plugin/stubs/rspack.forbid-stubbed-modules.test.js
 */

'use strict';

const assert = require('assert');
const path = require('path');

const ForbidStubbedModulesPlugin = require(path.resolve(__dirname, 'rspack.forbid-stubbed-modules'));
const { isStubbedModuleResource } = ForbidStubbedModulesPlugin;

const STUB_FILE = path.resolve(__dirname, 'empty-stub', 'index.js');

/**
 * Applies the plugin against a fake compiler and returns the collected
 * compilation errors for the given module list.
 */
function collectErrors(modules, moduleGraph) {
  let finishModulesHandler;
  const compilation = {
    errors: [],
    moduleGraph,
    hooks: {
      finishModules: {
        tap: (_name, handler) => {
          finishModulesHandler = handler;
        },
      },
    },
  };
  const fakeCompiler = {
    hooks: {
      compilation: {
        tap: (_name, handler) => handler(compilation),
      },
    },
  };
  new ForbidStubbedModulesPlugin().apply(fakeCompiler);
  assert.strictEqual(typeof finishModulesHandler, 'function',
    'plugin did not register a finishModules tap');
  finishModulesHandler(modules);
  return compilation.errors;
}

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

run('recognises the real stub path on this filesystem', () => {
  assert.ok(isStubbedModuleResource(STUB_FILE),
    `pattern did not match the actual stub path: ${STUB_FILE}`);
});

run('recognises a Windows-style stub path', () => {
  assert.ok(isStubbedModuleResource(
    'C:\\src\\public\\common\\components\\nexus-ui-plugin\\stubs\\empty-stub\\index.js'));
});

run('ignores unrelated module paths', () => {
  assert.ok(!isStubbedModuleResource('/src/node_modules/prismjs/prism.js'));
  assert.ok(!isStubbedModuleResource('/src/public/common/components/nexus-ui-plugin/src/index.ts'));
});

run('ignores non-string resources', () => {
  assert.ok(!isStubbedModuleResource(undefined));
  assert.ok(!isStubbedModuleResource(null));
  assert.ok(!isStubbedModuleResource(42));
});

run('reports an error when a bundled module resolves to the stub', () => {
  const errors = collectErrors([{ resource: STUB_FILE }]);
  assert.strictEqual(errors.length, 1);
  assert.match(errors[0].message, /ForbidStubbedModules/);
  assert.match(errors[0].message, /empty-stub/);
});

run('names the importing module when the module graph is available', () => {
  const stubModule = { resource: STUB_FILE };
  const moduleGraph = {
    getIncomingConnections: () => [
      { originModule: { identifier: () => '/node_modules/react-syntax-highlighter/dist/esm/prism.js' } },
    ],
  };
  const errors = collectErrors([stubModule], moduleGraph);
  assert.strictEqual(errors.length, 1);
  assert.match(errors[0].message, /react-syntax-highlighter\/dist\/esm\/prism\.js/);
});

run('does not list the stub itself as its own importer', () => {
  const stubModule = { resource: STUB_FILE, identifier: () => STUB_FILE };
  const moduleGraph = {
    getIncomingConnections: () => [
      { originModule: stubModule },
      { originModule: { identifier: () => '/src/entry.js' } },
    ],
  };
  const errors = collectErrors([stubModule], moduleGraph);
  assert.strictEqual(errors.length, 1);
  assert.match(errors[0].message, /imported by \/src\/entry\.js\./);
});

run('falls back to a placeholder importer when getIncomingConnections is missing', () => {
  const errors = collectErrors([{ resource: STUB_FILE }], {});
  assert.strictEqual(errors.length, 1);
  assert.match(errors[0].message, /<unknown importer>/);
});

run('falls back to a placeholder importer when getIncomingConnections throws', () => {
  const moduleGraph = {
    getIncomingConnections: () => {
      throw new TypeError('not supported by this rspack version');
    },
  };
  const errors = collectErrors([{ resource: STUB_FILE }], moduleGraph);
  assert.strictEqual(errors.length, 1);
  assert.match(errors[0].message, /<unknown importer>/);
});

run('skips origin modules that cannot report an identifier', () => {
  const moduleGraph = {
    getIncomingConnections: () => [
      { originModule: null },
      { originModule: {} },
      {
        originModule: {
          identifier: () => {
            throw new Error('module disposed');
          },
        },
      },
      { originModule: { identifier: () => '/src/entry.js' } },
    ],
  };
  const errors = collectErrors([{ resource: STUB_FILE }], moduleGraph);
  assert.strictEqual(errors.length, 1);
  assert.match(errors[0].message, /imported by \/src\/entry\.js\./);
});

run('still reports the violation when every issuer lookup fails', () => {
  const moduleGraph = { getIncomingConnections: () => [{ originModule: {} }] };
  const errors = collectErrors([{ resource: STUB_FILE }], moduleGraph);
  assert.strictEqual(errors.length, 1);
  assert.match(errors[0].message, /ForbidStubbedModules/);
  assert.match(errors[0].message, /<unknown importer>/);
});

run('tolerates a stub module whose own identifier() throws', () => {
  const stubModule = {
    resource: STUB_FILE,
    identifier: () => {
      throw new Error('no identifier');
    },
  };
  const moduleGraph = {
    getIncomingConnections: () => [{ originModule: { identifier: () => '/src/entry.js' } }],
  };
  const errors = collectErrors([stubModule], moduleGraph);
  assert.strictEqual(errors.length, 1);
  assert.match(errors[0].message, /imported by \/src\/entry\.js\./);
});

run('falls back to a placeholder importer when no module graph is available', () => {
  const errors = collectErrors([{ resource: STUB_FILE }]);
  assert.match(errors[0].message, /<unknown importer>/);
});

run('stays silent for a clean module graph', () => {
  const errors = collectErrors([
    { resource: '/src/node_modules/react/index.js' },
    { resource: undefined },
    null,
  ]);
  assert.deepStrictEqual(errors, []);
});

run('does not throw on a missing module list', () => {
  assert.doesNotThrow(() => collectErrors(undefined));
});

if (process.exitCode) {
  console.error('\nforbid-stubbed-modules: one or more assertions failed');
  process.exit(process.exitCode);
} else {
  console.log('\nforbid-stubbed-modules: all assertions passed');
}
