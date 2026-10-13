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

/**
 * Shared fixtures and render helper for the Repositories-tab suites.
 *
 * The two suites need different row data — one covers general rendering,
 * filtering and CSV export, the other needs names, types and version counts
 * that each produce a distinct ordering — so the fixtures stay separate while
 * the boilerplate around them does not.
 */

import React from 'react';
import { render } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import GARepositoriesTab from '../GARepositoriesTab';
import type { RepoRow } from '../gaRepositoriesMachine';

type TabProps = React.ComponentProps<typeof GARepositoriesTab>;

/** General-purpose fixture: three repositories, one of each type. */
export const REPO_ROWS: RepoRow[] = [
  { repositoryName: 'maven-central', type: 'proxy', versionCount: 42 },
  { repositoryName: 'maven-releases', type: 'hosted', versionCount: 3 },
  { repositoryName: 'maven-public', type: 'group', versionCount: 0 },
];

/**
 * Sorting fixture: every column yields a different, unambiguous ordering, and
 * the version counts (3 / 7 / 21) distinguish a numeric comparison from a
 * lexicographic one.
 */
export const SORTING_REPO_ROWS: RepoRow[] = [
  { repositoryName: 'alpha-proxy', type: 'proxy', versionCount: 7 },
  { repositoryName: 'beta-hosted', type: 'hosted', versionCount: 21 },
  { repositoryName: 'gamma-group', type: 'group', versionCount: 3 },
];

/** The themed element under test, for `render` and for `rerender` assertions. */
export function tabElement(rows: RepoRow[], props: Partial<TabProps> = {}): JSX.Element {
  return (
    <Theme>
      <GARepositoriesTab
        rows={rows}
        loading={false}
        error={null}
        selectedVersion="1.2.3"
        {...props}
      />
    </Theme>
  );
}

/**
 * Build a render helper bound to one fixture, so a suite can call
 * `renderTab()` for the common case and still override any prop per test.
 */
export function makeRenderTab(rows: RepoRow[]) {
  return function renderTab(props: Partial<TabProps> = {}) {
    return render(tabElement(rows, props));
  };
}
