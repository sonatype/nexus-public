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

export const GA_REPOSITORIES_STRINGS = {
  FILTER_PLACEHOLDER: 'Filter',
  CLEAR_SEARCH_ARIA: 'Clear search',
  FILTER_BUTTON: 'Filter',
  RESET_FILTERS: 'Reset filters',
  EXPORT_CSV: 'Export CSV',
  EXPORT_CSV_ARIA: 'Export all filtered results as CSV',
  EXPORT_FILENAME: 'repositories.csv',
  FILTERS: {
    SORT_LABEL: 'Sort',
    TYPE_LABEL: 'Type',
    SORT_ASC: 'Ascending',
    SORT_DESC: 'Descending',
  },
  TYPE_OPTIONS: [
    { value: 'hosted', label: 'Hosted' },
    { value: 'proxy', label: 'Proxy' },
    { value: 'group', label: 'Group' },
  ],
  COLUMNS: {
    REPOSITORY: 'Repository',
    TYPE: 'Type',
    VERSIONS_IN_REPO: 'Versions in Repo',
  },
  EMPTY: {
    // Kept separate from NO_RESULTS_TITLE: this fires when the API returned zero rows
    // (no repositories contain the selected version); NO_RESULTS_TITLE fires when the
    // user's client-side search/filter narrowed a non-empty result set to zero. The two
    // states may diverge in copy later — keep the keys distinct.
    NO_REPOS_TITLE: 'No repositories found',
    NO_REPOS_FOR_VERSION: (version: string) => `No repositories contain version ${version}.`,
    NO_REPOS_FOR_COMPONENT: 'No repositories are available for this component.',
    NO_RESULTS_TITLE: 'No repositories found',
    NO_RESULTS_DETAIL: 'Try adjusting your search terms or filters.',
  },
  VERSIONS_BADGE: (count: number) => `${count} version${count !== 1 ? 's' : ''}`,
} as const;
