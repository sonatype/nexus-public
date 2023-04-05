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
  TAGS: {
    MENU: {
      text: 'Tags',
      description: 'View created tags',
    },
    EMPTY_MESSAGE: 'No tags found.',
    HELP_MESSAGE: {
      TITLE: 'What is a tag?',
      TEXT: 
      <>
        Tagging is a Nexus Repository Pro feature that allows you to mark a set of components with a tag so that they can be logically associated with one another.
        You can use tags however you like, but the most common use cases are for CI build IDs for a project (e.g., project-abc-build-142) or for higher-level
        release train when coordinating multiple projects as a single unit (e.g., release-train-13). It is also extensively used by our 
        {" "}<NxTextLink href="http://links.sonatype.com/products/nxrm3/docs/staging" external>staging</NxTextLink> feature.
        Check out our
        {" "}<NxTextLink href="http://links.sonatype.com/products/nxrm3/docs/component-tag" external>help documentation</NxTextLink> for
        more information.
      </>,
    },
    LIST: {
      CAPTION: 'Available Tags',
      COLUMNS: {
        NAME: 'Tag Name',
        FIRST_CREATED: 'First created time',
        LAST_UPDATED: 'Last Updated Time',
      },
     FILTER_PLACEHOLDER: 'Filter by Name',
    },
    DETAILS: {
      HEADER: {
        text: 'Tags',
        description: 'View tag details',
      },
      TILE_HEADER: 'Tag Details',
      BACK_TO_TAGS_TABLE: 'Back to Tags Table View',
      ATTRIBUTES: 'Attributes',
      FIND_TAGGED: 'Find Tagged Components',
      FIRST_CREATED: 'First Created Time',
      LAST_UPDATED: 'Last Updated Time',
      TAG_NOT_FOUND: 'Tag was not found'
    }
  }
};
