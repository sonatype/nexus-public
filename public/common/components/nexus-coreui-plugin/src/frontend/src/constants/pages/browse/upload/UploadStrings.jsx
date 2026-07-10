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
import { faUpload } from '@fortawesome/free-solid-svg-icons';

export default {
  UPLOAD: {
    MENU: {
      text: 'Upload',
      description: 'Upload content to the hosted repository',
      icon: faUpload
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
      COORDINATES_EXTRACTED_FROM_POM_MESSAGE: 'Component details will be extracted from the provided POM file.',
      ENFORCEMENT_BLOCKED: {
        // CLM-40150: TITLE and MESSAGE are unchanged. The link-label constant was removed
        // alongside the broken evaluation link in UploadDetails.jsx — the IQ-side
        // per-evaluation report UI does not exist for hosted-deployment blocks yet, so
        // surfacing the link sent users to a 404. Reference ID still rendered separately
        // (ENFORCEMENT_CORRELATION_ID_LABEL) for support correlation.
        TITLE: 'Upload blocked by Lifecycle Evaluation.',
        MESSAGE: (assetName, repositoryName) =>
            `${assetName || 'Artifact'} was not uploaded to ${repositoryName || 'the repository'} ` +
            'because it failed one or more policies configured.'
      },
      ENFORCEMENT_UNAVAILABLE: {
        TITLE: 'Policy evaluation unavailable.',
        MESSAGE: (assetName, repositoryName) =>
            `${assetName || 'Artifact'} could not be uploaded to ${repositoryName || 'the repository'} ` +
            'because policy evaluation is currently unavailable. Please retry in a few moments.'
      },
      ENFORCEMENT_CORRELATION_ID_LABEL: 'Reference ID'
    },
    URL_COPIED_MESSAGE: 'URL Copied to Clipboard',
    URL_COPY_ERROR_MESSAGE: 'Failed to copy URL to clipboard'
  }
};
