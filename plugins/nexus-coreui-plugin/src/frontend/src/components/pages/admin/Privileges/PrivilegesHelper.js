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
import {find, propEq, findIndex, indexBy, insert, prop} from 'ramda';
import {APIConstants} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {PRIVILEGES: {FORM: LABELS}} = UIStrings;
const {REST: {PUBLIC: {PRIVILEGES: privilegesUrl}}, EXT: {REPOSITORY: REPO_API}} = APIConstants;

export const TYPES = {
  SCRIPT: 'script',
  REPOSITORY_ADMIN: 'repository-admin',
  REPOSITORY_CONTENT_SELECTOR: 'repository-content-selector',
  REPOSITORY_VIEW: 'repository-view',
  APPLICATION: 'application',
  WILDCARD: 'wildcard',
};

const FORMAT_FIELD_CONFIG = {
  id: 'format',
  label: LABELS.FORMAT.LABEL,
  helpText: LABELS.FORMAT.SUB_LABEL,
};

export const FIELDS = {
  NAME: {
    NAME: 'name',
  },
  SCRIPT_NAME: {
    NAME: 'scriptName',
    LABEL: 'Script Name',
  },
  CONTENT_SELECTOR: {
    NAME: 'contentSelector',
    LABEL: 'Content Selector',
  },
  PATTERN: {
    NAME: 'pattern',
  },
  REPOSITORY: {
    NAME: 'repository',
    LABEL: 'Repository',
  },
  ACTIONS: {
    NAME: 'actions',
    LABEL: 'Actions',
  },
  FORMAT: {
    NAME: 'format',
    LABEL: 'Format',
  },
};

export const ACTIONS = {
  DISASSOCIATE: 'DISASSOCIATE',
  ADD: 'ADD',
  READ: 'READ',
  BROWSE: 'BROWSE',
  DELETE: 'DELETE',
  ASSOCIATE: 'ASSOCIATE',
  ALL: 'ALL',
  EDIT: 'EDIT',
  RUN: 'RUN',
};

export const EMPTY_DATA = {
  type: '',
  name: '',
  description: '',
};

const singlePrivilegeUrl = name => `${privilegesUrl}/${encodeURIComponent(name)}`;
const createPrivilegeUrl = type => `${privilegesUrl}/${encodeURIComponent(type)}`;
const updatePrivilegeUrl = (type, name) => `${privilegesUrl}/${encodeURIComponent(type)}/${encodeURIComponent(name)}`;

export const URL = {privilegesUrl, singlePrivilegeUrl, updatePrivilegeUrl, createPrivilegeUrl};

export const convertActionsToArray = data => {
  const fieldName = FIELDS.ACTIONS.NAME;
  if (data.hasOwnProperty(fieldName) && typeof data[fieldName] === 'string') {
    data[fieldName] = data[fieldName].split(',');
  }
  return data;
};

export const convertActionsToString = data => {
  const fieldName = FIELDS.ACTIONS.NAME;
  if (data.hasOwnProperty(fieldName)) {
    data[fieldName] = data[fieldName].join(',');
  }
  return data;
};

const renameScriptId = types => {
  const type = types[TYPES.SCRIPT];
  if (!type) return;
  const field = find(propEq('id', FIELDS.NAME.NAME))(type.formFields);
  field.id = FIELDS.SCRIPT_NAME.NAME;
};

const addFormatFieldForRepoSelectorType = types => {
  const type = types[TYPES.REPOSITORY_CONTENT_SELECTOR];
  if (!type) return;

  const formatField = {
    ...APIConstants.EXT.DEFAULT_FIELD_CONFIG,
    ...FORMAT_FIELD_CONFIG
  };
  let index = findIndex(propEq('id', FIELDS.CONTENT_SELECTOR.NAME))(type.formFields);
  type.formFields = insert(++index, formatField, type.formFields);

  const repoField = find(propEq('id', FIELDS.REPOSITORY.NAME))(type.formFields);
  repoField.storeApi = `${REPO_API.ACTION}.${REPO_API.METHODS.READ_WITH_FOR_ALL}`;
};

const renamePatternHelpText = types => {
  const type = types[TYPES.WILDCARD];
  if (!type) return;
  const field = find(propEq('id', FIELDS.PATTERN.NAME))(type.formFields);
  field.helpText = LABELS.PRIVILEGE_STRING.SUB_LABEL;
};

export const modifyFormFields = typesArr => {
  const types = indexBy(prop('id'), typesArr || []);
  renameScriptId(types);
  addFormatFieldForRepoSelectorType(types);
  renamePatternHelpText(types);

  return types;
}


