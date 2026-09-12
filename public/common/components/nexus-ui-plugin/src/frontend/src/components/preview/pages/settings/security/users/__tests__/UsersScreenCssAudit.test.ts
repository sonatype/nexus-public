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

const USERS_DIR = path.resolve(__dirname, '..');

function readScss(filename: string): string {
  return fs.readFileSync(path.join(USERS_DIR, filename), 'utf-8');
}

describe('NEXUS-52931 Users screen CSS token and component audit', () => {
  // AC-1: All CSS custom property references resolve to defined tokens
  it('UsersPage.scss does not reference the undefined --color-text-primary token', () => {
    const content = readScss('UsersPage.scss');
    expect(content).not.toContain('--color-text-primary');
  });

  // AC-2: Border radius values match design system spec (--radius-md = 6px)
  it('UserForm.scss uses 6px fallback for --radius-md not 8px', () => {
    const content = readScss('UserForm.scss');
    expect(content).not.toContain('--radius-md, 8px');
  });

  // AC-6: No duplicated @keyframes spin - shared animation utility used
  it('UserDetail.scss does not define @keyframes spin locally', () => {
    const content = readScss('UserDetail.scss');
    expect(content).not.toContain('@keyframes spin');
  });

  it('UserDetail.scss imports the shared animations module', () => {
    const content = readScss('UserDetail.scss');
    expect(content).toContain("@use '../../../../shared/animations'");
  });

  it('UserForm.scss does not define @keyframes spin locally', () => {
    const content = readScss('UserForm.scss');
    expect(content).not.toContain('@keyframes spin');
  });

  it('UserForm.scss imports the shared animations module', () => {
    const content = readScss('UserForm.scss');
    expect(content).toContain("@use '../../../../shared/animations'");
  });
});
