/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

/**
 * User-facing strings for the unified search page, following the same
 * co-located strings-module convention as `GARepositoriesStrings.ts` on the
 * component details page. Exported so tests assert against the same values the
 * page renders rather than re-declaring literals.
 */

/**
 * Canonical Nexus documentation entry point for search.
 *
 * Deliberately the stable, version-independent `help.sonatype.com/en/<topic>.html`
 * form used by every other docs link in this UI, so it resolves identically on
 * Self-Hosted, Cloud and Community Edition and needs no edition detection. The
 * topic covers both how search works and which wildcards the query syntax
 * supports, so one destination satisfies the whole help section.
 */
export const SEARCH_DOCS_URL = 'https://help.sonatype.com/en/searching-for-components.html';

/**
 * Copy for the search help section. Kept beside the page rather than in the
 * shared component so the wording stays with the surface it describes; the page
 * has no localization layer, matching every other string here.
 */
export const SEARCH_HELP = {
  title: 'About Search',
  // HelpSection splits `content` on '\n', rendering each segment as its own
  // paragraph — the newline below is a deliberate paragraph break, not wrapping.
  content:
    'Search finds components across your repositories by name, version, and ' +
    'format-specific coordinates.\n' +
    'Learn more about how the search module works and which wildcards the ' +
    'search syntax supports.',
  // Names the destination rather than the action, so the link's accessible name
  // is meaningful on its own out of context.
  linkLabel: 'View search documentation',
  // Gainsight identifier for the docs link, per the data-analytics-id convention
  // (nxrm-[page]-[purpose]-[role]).
  linkAnalyticsId: 'nxrm-search-documentation-lnk',
} as const;
