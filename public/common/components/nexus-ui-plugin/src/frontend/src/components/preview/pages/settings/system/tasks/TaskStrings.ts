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

export const TASK_TYPE_SELECTOR = {
  FILTER_PLACEHOLDER: 'Filter task types...',
  LOADING_MESSAGE: 'Loading task types...',
  COUNT: (count: number) => `${count} task type${count === 1 ? '' : 's'}`,
  EMPTY_FILTER: 'No task types match your filter',
  COLUMN_NAME: 'Name',
  COLUMN_CATEGORY: 'Category',
  COLUMN_DESCRIPTION: 'Description',
};

export const DYNAMIC_FORM_FIELDS = {
  ALL_REPOSITORIES_LABEL: '(All Repositories)',
  ALL_REPOSITORIES_VALUE: '*',
  // Backend BlobStoreComponent.readWithAll() / readNoneGroupEntriesIncludingEntryForAll()
  // emit a synthetic BlobStoreXO whose name is "(All Blob Stores)" and the descriptor
  // uses idMapping("name") — so the persisted value equals the label.
  // Coupling documented in NEXUS-53356 (2026-06).
  ALL_BLOB_STORES_LABEL: '(All Blob Stores)',
  ALL_BLOB_STORES_VALUE: '(All Blob Stores)',
};
