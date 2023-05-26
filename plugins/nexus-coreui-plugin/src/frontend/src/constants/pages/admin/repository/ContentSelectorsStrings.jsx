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
import React from 'react';
import {NxTextLink} from '@sonatype/react-shared-components';

export default {
  CONTENT_SELECTORS: {
    MENU: {
      text: 'Content Selectors',
      description: 'Define the content that users can access using Content Selector Expression Language (CSEL)'
    },

    EDIT_TITLE: (name) => `Edit ${name}`,

    EMPTY_MESSAGE: 'No content selectors were found',

    HELP_TITLE: 'What is a content selector?',
    HELP_TEXT: <>
      Content selectors provide a means for you to select specific content from your repositories.
      Repository content is evaluated against expressions written in CSEL (Content Selector Expression Language).
      For more information,{' '}
      <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/content-selector">
        check the documentation
      </NxTextLink>.
    </>,
    CREATE_BUTTON: 'Create Selector',
    FILTER_PLACEHOLDER: 'Filter',

    NAME_LABEL: 'Name',
    TYPE_LABEL: 'Type',
    DESCRIPTION_LABEL: 'Description',
    EXPRESSION_LABEL: 'Search Expression',
    EXPRESSION_DESCRIPTION: 'Use the following query to identify repository content',
    EXPRESSION_EXAMPLES: 'Example Content Selector Expressions',
    RAW_EXPRESSION_EXAMPLE_LABEL: 'Select "raw" format content',
    MULTI_EXPRESSIONS_EXAMPLE_LABEL: 'Select "maven2" content along a path that starts with "/org/"',

    PREVIEW: {
      TITLE: 'Preview Content Selector Results',
      REPOSITORY_LABEL: 'Preview Repository',
      REPOSITORY_DESCRIPTION: 'Select a repository to evaluate the content selector and see the content that would be available',
      BUTTON: 'Preview',
      RESULTS: 'Preview Results',
      NAME_COLUMN: 'Name',
      EMPTY: 'No content in repositories matched the expression'
    },

    MESSAGES: {
      DUPLICATE_ERROR: (name) => `Another content selector named ${name} already exists`,
      NAME_TOO_LONG: 'The name of this content selector is too long, please use a shorter name',
      SAVE_ERROR: 'An error occurred while saving the content selector',
      DELETE_ERROR: (name) => `Content selector ${name} is in use and cannot be deleted`,

      CONFIRM_DELETE: {
        TITLE: 'Delete Content Selector',
        MESSAGE: (name) => `Delete the content selector named ${name}?`,
        YES: 'Delete',
        NO: 'Cancel'
      }
    }
  }
};