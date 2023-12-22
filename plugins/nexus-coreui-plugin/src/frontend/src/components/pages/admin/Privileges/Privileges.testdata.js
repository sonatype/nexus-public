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
import {indexBy, prop} from 'ramda';
import {APIConstants} from '@sonatype/nexus-ui-plugin';

import {TYPES as TYPE_IDS} from './PrivilegesHelper';

const {EXT: {DEFAULT_FIELD_CONFIG}} = APIConstants;

const PRIVILEGE_TYPES_UNIQUE_FIELDS = [
  {
    id: TYPE_IDS.SCRIPT,
    name: 'Script',
    formFields: [
      {
        id: 'name',
        type: 'string',
        label: 'Script Name',
        helpText: 'The name of the script',
      },
      {
        id: 'actions',
        type: 'setOfCheckboxes',
        label: 'Actions',
        helpText: 'The actions you wish to allow',
        attributes: {
          options: [
            "browse",
            "read",
            "edit",
            "add",
            "delete",
            "run"
          ]
        },
      }
    ]
  },
  {
    id: TYPE_IDS.REPOSITORY_ADMIN,
    name: 'Repository Admin',
    formFields: [
      {
        id: 'format',
        type: 'string',
        label: 'Format',
        helpText: 'The format(s) for the repository',
      },
      {
        id: 'repository',
        type: 'combobox',
        label: 'Repository',
        helpText: 'The repository name',
        storeApi: 'coreui_Repository.readReferencesAddingEntryForAll',
        allowAutocomplete: true,
      },
      {
        id: 'actions',
        type: 'setOfCheckboxes',
        label: 'Actions',
        helpText: 'The actions you wish to allow',
        attributes: {
          options: [
            "browse",
            "read",
            "edit",
            "add",
            "delete"
          ]
        }
      }
    ]
  },
  {
    id: TYPE_IDS.REPOSITORY_CONTENT_SELECTOR,
    name: 'Repository Content Selector',
    formFields: [
      {
        id: 'contentSelector',
        type: 'combobox',
        label: 'Content Selector',
        helpText: 'The content selector for the repository',
        storeApi: 'coreui_Selector.readReferences',
      },
      {
        id: 'repository',
        type: 'combobox',
        label: 'Repository',
        helpText: 'The repository or repositories to grant access',
        storeApi: 'coreui_Repository.readReferencesAddingEntriesForAllFormats',
        allowAutocomplete: true,
      },
      {
        id: 'actions',
        type: 'setOfCheckboxes',
        label: 'Actions',
        helpText: 'The actions you wish to allow',
        attributes: {
          options: [
            "browse",
            "read",
            "edit",
            "add",
            "delete"
          ]
        }
      }
    ]
  },
  {
    id: TYPE_IDS.REPOSITORY_VIEW,
    name: 'Repository View',
    formFields: [
      {
        id: 'format',
        type: 'string',
        label: 'Format',
        helpText: 'The format(s) for the repository',
      },
      {
        id: 'repository',
        type: 'combobox',
        label: 'Repository',
        helpText: 'The repository name',
        storeApi: 'coreui_Repository.readReferencesAddingEntryForAll',
        allowAutocomplete: true,
      },
      {
        id: 'actions',
        type: 'setOfCheckboxes',
        label: 'Actions',
        helpText: 'The actions you wish to allow',
        attributes: {
          options: [
            "browse",
            "read",
            "edit",
            "add",
            "delete"
          ]
        }
      }
    ]
  },
  {
    id: TYPE_IDS.APPLICATION,
    name: 'Application',
    formFields: [
      {
        id: 'domain',
        type: 'string',
        label: 'Domain',
        helpText: 'The domain for the privilege',
      },
      {
        id: 'actions',
        type: 'setOfCheckboxes',
        label: 'Actions',
        helpText: 'The actions you wish to allow',
        attributes: {
          options: [
            "read",
            "update",
            "create",
            "delete"
          ]
        }
      }
    ]
  },
  {
    id: TYPE_IDS.WILDCARD,
    name: 'Wildcard',
    formFields: [
      {
        id: 'pattern',
        type: 'string',
        label: 'Privilege String',
        helpText: 'The internal segment matching algorithm uses Apache Shiro wildcard permissions',
      }
    ]
  }
];

export const TYPES = PRIVILEGE_TYPES_UNIQUE_FIELDS.map(type => {
  type.formFields = type.formFields.map(field => ({...DEFAULT_FIELD_CONFIG, ...field}));
  return type;
});

export const BREADR_ACTIONS = ['Browse', 'Read', 'Edit', 'Add', 'Delete', 'Run'];

export const TYPES_MAP = indexBy(prop('id'), TYPES);

export const SELECTORS = [
  {
    id: 'Test_Selector_1',
    name: 'Test_Selector_1',
  },
  {
    id: 'Test_Selector_2',
    name: 'Test_Selector_2',
  },
];

export const SELECTORS_MAP = indexBy(prop('id'), SELECTORS);

export const REPOSITORIES = [
  {
    type: 'proxy',
    format: 'maven2',
    versionPolicy: 'RELEASE',
    url: 'http://localhost:8081/repository/maven-central/',
    sortOrder: 0,
    id: 'TestRepository',
    name: 'TestRepository'
  },
  {
    type: 'group',
    format: 'maven2',
    versionPolicy: 'MIXED',
    url: 'http://localhost:8081/repository/maven-public/',
    sortOrder: 0,
    id: 'maven-public',
    name: 'maven-public'
  },
  {
    type: null,
    format: null,
    versionPolicy: null,
    url: null,
    status: null,
    sortOrder: 1,
    id: '*',
    name: '(All Repositories)'
  }
];
