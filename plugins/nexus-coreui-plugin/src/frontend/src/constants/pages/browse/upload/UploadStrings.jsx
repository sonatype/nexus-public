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
export default {
  UPLOAD: {
    MENU: {
      text: 'Upload',
      description: 'Upload content to the hosted repository'
    },
    EMPTY_MESSAGE: 'No repositories found.',
    LIST: {
      COLUMNS: {
        NAME: 'Name',
        FORMAT: 'Format',
        URL: 'URL',
      },
      COPY_URL_TITLE: 'Copy URL to Clipboard',
      FILTER_PLACEHOLDER: 'Filter'
    },
    DETAILS: {
      TITLE: 'Upload',
      DESCRIPTION: 'Upload content to the hosted repository',
      TILE_TITLE: repoName => `Choose Assets/Components for ${repoName} Repository`,
      FILE_UPLOAD_LABEL: 'File',
      SUBMIT_BTN_LABEL: 'Upload',
      ADD_ANOTHER_ASSET_BTN_LABEL: 'Add another asset',
      ASSET_GROUP_NAME: assetNum => `Asset ${assetNum}`,
      ASSET_NOT_UNIQUE_MESSAGE: 'Asset not unique',
      COORDINATES_EXTRACTED_FROM_POM_MESSAGE: 'Component details will be extracted from the provided POM file.'
    },
    URL_COPIED_MESSAGE: 'URL Copied to Clipboard',
  }
};
