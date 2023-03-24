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
  ROUTING_RULES: {
    MENU: {
      text: 'Routing Rules',
      description: 'Restrict which requests are handled by repositories'
    },

    LIST: {
      PREVIEW_BUTTON: 'Global Routing Preview',
      CREATE_BUTTON: 'Create Routing Rule',
      FILTER_PLACEHOLDER: 'Filter by Name or Description',
      NAME_LABEL: 'Name',
      DESCRIPTION_LABEL: 'Description',
      USED_BY_LABEL: 'Used By',
      NEEDS_ASSIGNMENT: '0 repositories, assign it to a repository',
      USED_BY: (count) => count === 1 ? '1 repository' : `${count} repositories`,
      EMPTY_LIST: 'There are no routing rules created yet',
      HELP_TITLE: 'What is a routing rule?',
      HELP_TEXT: <>
        Routes are like filters you can apply to groups in terms of security access and general component retrieval.
        They can be used to reduce the number of repositories within a group accessed in order to retrieve a component.
        For more information,{' '}
        <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/routing-rule">
          check the documentation
        </NxTextLink>.
      </>,
    },

    PREVIEW: {
      TITLE: 'Global Routing Preview',
      REPOSITORIES_LABEL: 'Repositories',
      REPOSITORIES_DESCRIPTION: 'Choose a set of repositories to test against',
      REPOSITORIES: {
        ALL: 'All Repositories',
        GROUPS: 'All Group Repositories',
        PROXIES: 'All Proxy Repositories'
      },
      PATH_LABEL: 'Path',
      COLUMNS: {
        REPOSITORY: 'Repository',
        TYPE: 'Type',
        FORMAT: 'Format',
        RULE: 'Routing Rule',
        STATUS: 'Status'
      },
      NO_RULE: 'None',
      EMPTY_PREVIEW: 'No results found or preview was not yet submitted',
      DETAILS_TITLE: (ruleName) => `Routing Rule Details for ${ruleName}`,
      TYPE_FILTER: {
        any: 'any',
        group: 'group',
        proxy: 'proxy'
      }
    },

    FORM: {
      CREATE_TITLE: 'Create Routing Rule',
      EDIT_TITLE: 'Edit Routing Rule',
      UNUSED: `To use this rule, <a href="#admin/repository/repositories">assign it to a repository</a>`,
      USED_BY: (repositoryNames) => {
        const repositoryLinks = repositoryNames.map(name =>
            `<a href="#admin/repository/repositories:${window.encodeURIComponent(name)}">${name}</a>`);
        const repository = repositoryNames.length === 1 ? 'repository' : 'repositories';
        return `This rule is in use by ${repositoryNames.length} ${repository} (${repositoryLinks.join(', ')})`;
      },
      SAVE_ERROR: 'An error occured while saving the routing rule',
      NAME_LABEL: 'Name',
      DESCRIPTION_LABEL: 'Description',
      MODE_LABEL: 'Mode',
      MODE: {
        ALLOW: 'Allow',
        BLOCK: 'Block'
      },
      PREVIEW: {
        ALLOWED: 'This request would be allowed',
        BLOCKED: 'This request would be blocked'
      },
      MODE_DESCRIPTION: 'Allow or block requests when their path matches any of the following matchers',
      MATCHERS_LABEL: 'Matchers',
      MATCHER_LABEL: (index) => `Matcher ${index}`,
      MATCHERS_DESCRIPTION: 'Enter regular expressions that will be used to identify request paths to allow or block (depending on above mode)',
      NAME_IS_NONE_ERROR: 'Rule must not be named None',
      DELETE_MATCHER_BUTTON: 'Delete this matcher',
      ADD_MATCHER_BUTTON: 'Add Another Matcher',
      CREATE_BUTTON: 'Create Routing Rule',
      CANNOT_DELETE: (repositoryNames) => `\
This rule is in use by ${repositoryNames.length} ${repositoryNames.length === 1 ? 'repository' : 'repositories'} \
(${repositoryNames.join(', ')})`
    },

    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete Routing Rule',
        MESSAGE: (name) => `Delete the routing rule named ${name}`,
        YES: 'Delete',
        NO: 'Cancel'
      },
      DELETE_ERROR: (name) => `Unable to delete routing rule named ${name}`
    },

    ALLOWED: 'Allowed',
    BLOCKED: 'Blocked',

    PATH_LABEL: 'Path',
    PATH_DESCRIPTION: 'Enter a request path to check if it would be blocked or allowed. Requests always start with a leading slash.',
    TEST_BUTTON: 'Test'
  }
};
