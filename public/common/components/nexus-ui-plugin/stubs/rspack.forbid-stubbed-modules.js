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
 * Rspack plugin that fails the build if any module in the emitted bundles
 * resolves into stubs/empty-stub.
 *
 * The stub table in README.md only lists packages that are verifiably
 * unreachable from application code. Nothing structurally enforced that
 * claim: a dependency bump that starts importing a stubbed package would
 * bundle an empty object and ship a silently broken feature with a green
 * build. Grepping the emitted bundles for the package name does not catch
 * it either - the stub is empty, so a minified bundle contains no trace of
 * which package it stood in for.
 *
 * This plugin asserts on the module graph instead, so any path that pulls a
 * stubbed package into a bundle aborts the build and names the importer.
 */

const STUB_RESOURCE_PATTERN = /[\/\\]stubs[\/\\]empty-stub[\/\\]/;

/**
 * True when a resolved module path points inside the shared empty stub.
 *
 * @param {unknown} resource resolved absolute module path
 * @returns {boolean}
 */
function isStubbedModuleResource(resource) {
  return typeof resource === 'string' && STUB_RESOURCE_PATTERN.test(resource);
}

/**
 * A module's identifier, or undefined when the object does not expose one.
 *
 * Never throws: the identifier is only used to build a diagnostic message, so
 * an unexpected module shape must not be allowed to mask the violation itself.
 *
 * @param {unknown} module
 * @returns {string|undefined}
 */
function safeIdentifier(module) {
  try {
    return module && typeof module.identifier === 'function' ? module.identifier() : undefined;
  } catch (_) {
    return undefined;
  }
}

/**
 * Identifiers of the modules importing the given module, excluding itself.
 *
 * Defensive on purpose. The module graph API is not part of rspack's stable
 * public surface, so every step is optional: a missing moduleGraph, a missing
 * getIncomingConnections, a connection without an originModule, an
 * originModule without identifier(), or a throw from any of them degrades to
 * an empty list and an "<unknown importer>" message rather than a TypeError
 * that would swallow the violation.
 *
 * @param {object} compilation
 * @param {object} module the module resolving into the stub
 * @returns {string[]}
 */
function collectIssuers(compilation, module) {
  const moduleGraph = compilation && compilation.moduleGraph;
  if (!moduleGraph || typeof moduleGraph.getIncomingConnections !== 'function') {
    return [];
  }
  const self = safeIdentifier(module);
  try {
    return (moduleGraph.getIncomingConnections(module) || [])
      .map((connection) => safeIdentifier(connection && connection.originModule))
      .filter((identifier) => identifier && identifier !== self);
  } catch (_) {
    return [];
  }
}

class ForbidStubbedModulesPlugin {
  apply(compiler) {
    compiler.hooks.compilation.tap('ForbidStubbedModules', (compilation) => {
      compilation.hooks.finishModules.tap('ForbidStubbedModules', (modules) => {
        for (const module of modules || []) {
          const resource = module && module.resource;
          if (!isStubbedModuleResource(resource)) {
            continue;
          }
          const issuers = collectIssuers(compilation, module);
          const importedBy = issuers.length ? issuers.join(', ') : '<unknown importer>';
          compilation.errors.push(
            new Error(
              `ForbidStubbedModules: a bundled module resolved to the shared empty stub ` +
              `("${resource}"), imported by ${importedBy}. A package listed in ` +
              `nexus-ui-plugin/stubs/empty-stub/README.md is only stubbed because nothing ` +
              `imports it; something now does, so it would ship as an empty object. Either ` +
              `drop that package's row from the stub table and let it resolve from npm ` +
              `(check its advisories first), or remove the import.`
            )
          );
        }
      });
    });
  }
}

module.exports = ForbidStubbedModulesPlugin;
module.exports.isStubbedModuleResource = isStubbedModuleResource;
module.exports.STUB_RESOURCE_PATTERN = STUB_RESOURCE_PATTERN;
module.exports.collectIssuers = collectIssuers;
