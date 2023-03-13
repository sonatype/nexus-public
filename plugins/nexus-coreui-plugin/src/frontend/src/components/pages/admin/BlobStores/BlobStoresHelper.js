/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import {APIConstants} from '@sonatype/nexus-ui-plugin';

const {
  REST: {
    PUBLIC: {BLOB_STORES, BLOB_STORES_CONVERT_TO_GROUP},
    INTERNAL: {BLOB_STORES_TYPES, BLOB_STORES_QUOTA_TYPES, BLOB_STORES_USAGE},
  },
} = APIConstants;

const deleteBlobStoreUrl = (name) => `${BLOB_STORES}/${encodeURIComponent(name)}`;
const convertToGroupBlobStoreUrl = (name, newName) =>
    `${BLOB_STORES_CONVERT_TO_GROUP}/${encodeURIComponent(name)}/${encodeURIComponent(newName)}`;
const createBlobStoreUrl = (typeId) => `${BLOB_STORES}/${encodeURIComponent(typeId)}`;
const singleBlobStoreUrl = (typeId, name) => `${BLOB_STORES}/${encodeURIComponent(typeId)}/${encodeURIComponent(name)}`;
const blobStoreTypesUrl = `/${BLOB_STORES_TYPES}`;
const blobStoreQuotaTypesUrl = `/${BLOB_STORES_QUOTA_TYPES}`;
const blobStoreUsageUrl = (name) => `/${BLOB_STORES_USAGE}/${encodeURIComponent(name)}`;

export const URLs = {
  deleteBlobStoreUrl,
  convertToGroupBlobStoreUrl,
  createBlobStoreUrl,
  singleBlobStoreUrl,
  blobStoreTypesUrl,
  blobStoreQuotaTypesUrl,
  blobStoreUsageUrl,
};
