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

export const SETTINGS_SELECTION_MODAL = {
  FILTER_PLACEHOLDER: 'Filter',
  WILDCARD_HINT: 'Filter by name or description. Use * as a wildcard.',
  DEFAULT_EMPTY_MESSAGE: 'No items available',
  COLUMN_SELECT: 'Select',
  COLUMN_NAME: 'Name',
  COLUMN_DESCRIPTION: 'Description',
  SELECTED_COUNTER: (count: number) => `${count} Selected`,
  MATCHING_COUNT: (count: number) => `${count} matching item${count === 1 ? '' : 's'}`,
  SELECT_ROW_ARIA: (name: string) => `Select ${name}`,
  CLOSE_ARIA: 'Close',
  PAGE_FIRST_ARIA: 'First page',
  PAGE_PREV_ARIA: 'Previous page',
  PAGE_NEXT_ARIA: 'Next page',
  PAGE_LAST_ARIA: 'Last page',
  BUTTON_CANCEL: 'Cancel',
  BUTTON_CONFIRM: 'Confirm',
} as const;
