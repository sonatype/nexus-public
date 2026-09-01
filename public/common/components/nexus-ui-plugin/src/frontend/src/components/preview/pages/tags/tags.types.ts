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
 * Tag with component count from the new filtered API.
 */
export interface TagWithCount {
  /** Tag name */
  name: string;
  /** Number of components tagged with this tag */
  componentCount: number;
  /** When the tag was first created */
  firstCreated: string | null;
  /** When the tag was last updated */
  lastUpdated: string | null;
}

/**
 * Tag detail from the REST API (GET /service/rest/v1/tags/{tagName}).
 */
export interface TagDetail {
  /** Tag name */
  name: string;
  /** Timestamp when first component was tagged */
  firstCreated: string;
  /** Timestamp when last component was tagged */
  lastUpdated: string;
  /** Custom attributes associated with the tag */
  attributes: Record<string, unknown>;
}

/**
 * A component tagged with a given tag (from the search API).
 */
export interface TaggedComponent {
  id: string;
  repository: string;
  format: string;
  group: string | null;
  name: string;
  version: string | null;
  assets: Array<{
    id: string;
    downloadUrl: string;
    path: string;
  }>;
}

/**
 * Sort field options for the tags list.
 */
export type TagSortField = 'name' | 'componentCount' | 'firstCreated' | 'lastUpdated';

/**
 * Sort direction.
 */
export type SortDirection = 'asc' | 'desc';

/**
 * Component count filter ranges.
 */
export type ComponentCountRange = '0' | '1-10' | '11-100' | '101-1000' | '1001+';

/**
 * Activity filter options.
 */
export type ActivityFilter = '30' | '90' | '91';

/**
 * Filters for the tags list.
 * Uses componentCountRanges (not componentCounts) for backward compatibility.
 */
export interface TagsFilters {
  /** Name filter (contains) */
  nameFilter: string;
  /** Selected component count ranges */
  componentCountRanges: string[];
  /** Selected activity filters as days */
  activityDays: number[];
  /** @deprecated Use componentCountRanges instead. TODO(NEXUS-53658): remove after migration complete */
  componentCounts?: ComponentCountRange[];
}
