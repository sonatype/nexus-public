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

const ROLES_DIR = path.resolve(__dirname, '..');

const ROLES_PAGE_SCSS = 'RolesPage.scss';
const FIT_TO_CONTENT_COMMENT = '// Content flows naturally, parent container handles scrolling';

function readScss(filename: string): string {
  return fs.readFileSync(path.join(ROLES_DIR, filename), 'utf-8');
}

describe('NEXUS-54309 Roles table fit-to-content sizing', () => {
  it('RolesPage.scss does not force full-viewport height on the page wrapper', () => {
    const content = readScss(ROLES_PAGE_SCSS);
    expect(content).not.toContain('height: 100%');
  });

  it('RolesPage.scss does not force a flex column layout on the page wrapper', () => {
    const content = readScss(ROLES_PAGE_SCSS);
    expect(content).not.toContain('display: flex');
    expect(content).not.toContain('flex-direction: column');
  });

  it('RolesPage.scss does not use flex: 1 to force the content region to fill parent height', () => {
    const content = readScss(ROLES_PAGE_SCSS);
    expect(content).not.toContain('flex: 1');
  });

  it('RolesPage.scss documents that the parent container handles scrolling', () => {
    const content = readScss(ROLES_PAGE_SCSS);
    expect(content).toContain(FIT_TO_CONTENT_COMMENT);
  });

  it('RolesPage.scss preserves the roles-list top spacing rule', () => {
    const content = readScss(ROLES_PAGE_SCSS);
    expect(content).toMatch(/\.roles-list\s*\{[\s\S]*margin-top:\s*var\(--space-4\)/);
  });
});

describe('NEXUS-54309 (v2) Roles list fit-to-content sizing', () => {
  const ROLES_LIST_SCSS = 'RolesList.scss';

  it('RolesList.scss wrapper opts out of the viewport-height cascade', () => {
    const content = readScss(ROLES_LIST_SCSS);
    expect(content).not.toMatch(/\.roles-list\s*\{[^}]*height:\s*100%/);
  });

  it('RolesList.scss wrapper prevents cross-axis stretch of sidebar and main region', () => {
    const content = readScss(ROLES_LIST_SCSS);
    expect(content).toMatch(/\.roles-list\s*\{[^}]*align-items:\s*flex-start/);
  });

  it('RolesList.scss does not stretch the table region with flex: 1', () => {
    const content = readScss(ROLES_LIST_SCSS);
    expect(content).not.toMatch(/&__table\s*\{[^}]*flex:\s*1/);
  });
});
