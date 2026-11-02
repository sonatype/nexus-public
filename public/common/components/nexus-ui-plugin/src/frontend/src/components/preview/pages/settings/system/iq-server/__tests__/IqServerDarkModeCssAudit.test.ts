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

import * as fs from 'fs';
import * as path from 'path';

/**
 * NEXUS-55151 dark-mode guard.
 *
 * The IQ Server settings surface does not ship explicit `[data-theme='dark']`
 * overrides; instead it relies on Radix Themes semantic colors and `var(--*)`
 * design tokens, which the app's ThemeContext remaps automatically under the
 * `.dark` body class on preview routes. That only holds while every colored
 * declaration in these SCSS files resolves to a Radix token. A raw `rgba()`/hex
 * color (for example a hardcoded black drop shadow) does NOT adapt and renders
 * wrong on a dark background, so these tests fail if one is reintroduced.
 *
 * A `var(--token, #fallback)` light-hex fallback is allowed: the fallback only
 * applies if the token is ever undefined, which it is not under Radix.
 */

const IQ_DIR = path.resolve(__dirname, '..');

const IQ_SCSS_FILES = [
  'IqServerPage.scss',
  'IqServerOverviewTab.scss',
  'IqServerConnectionSettingsTab.scss',
];

function readScss(filename: string): string {
  return fs.readFileSync(path.join(IQ_DIR, filename), 'utf-8');
}

/**
 * Strips SCSS content down to what could carry a raw color, so the hex/rgba
 * scan does not trip over `var(--token, #fallback)` fallbacks (allowed) or
 * comments (the license header contains no colors but keep the scan honest).
 */
function stripAllowedColorSources(scss: string): string {
  return scss
    // drop the light-hex fallback inside var(--token, #fallback)
    .replace(/var\([^)]*\)/g, '')
    // drop block and line comments
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/\/\/.*$/gm, '');
}

describe('NEXUS-55151 IQ Server dark-mode SCSS audit', () => {
  it('summary-card hover elevation uses a theme-adaptive Radix shadow token, not a hardcoded rgba shadow', () => {
    const content = readScss('IqServerOverviewTab.scss');
    // The hover/focus elevation must be a Radix --shadow-* token (auto-adapts light/dark).
    expect(content).toMatch(/box-shadow:\s*var\(--shadow-\d\)/);
    // Guard against the pre-fix hardcoded black shadow returning.
    expect(content).not.toMatch(/box-shadow:\s*0\s+2px\s+8px\s+rgba\(0,\s*0,\s*0/);
  });

  it.each(IQ_SCSS_FILES)(
    '%s contains no hardcoded rgba() colors outside of var() fallbacks',
    (filename) => {
      const scannable = stripAllowedColorSources(readScss(filename));
      expect(scannable).not.toMatch(/rgba?\(/i);
    }
  );

  it.each(IQ_SCSS_FILES)(
    '%s contains no bare hex color literals outside of var() fallbacks',
    (filename) => {
      const scannable = stripAllowedColorSources(readScss(filename));
      expect(scannable).not.toMatch(/#[0-9a-fA-F]{3,8}\b/);
    }
  );
});
