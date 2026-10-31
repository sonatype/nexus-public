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

// Public API for the Tags feature (Preview UI). Per frontend/architecture.md,
// only the page components and their data types are exported here; hooks,
// state machines, and API clients are internal implementation details and are
// imported directly by their consumers (and tests).

// Page components (Layer 3)
export { TagsPageRadix } from './TagsPageRadix';
export { default } from './TagsPageRadix';
export { TagDetailPage } from './TagDetailPage';

// Public data types
export type {
  TagDetail as TagDetailType,
  TagWithCount,
  TagSortField,
  SortDirection,
  TagsFilters,
  TaggedComponent,
} from './tags.types';
