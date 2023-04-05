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
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';

const {REST: {PUBLIC: {USERS: usersUrl, ROLES: rolesUrl}}} = APIConstants;

import UIStrings from '../../../../constants/UIStrings';

const {USERS: {FORM: LABELS}} = UIStrings;

export const DEFAULT_SOURCE = 'default';

export const STATUSES = {
  active: {
    id: 'active',
    label: LABELS.STATUS.OPTIONS.ACTIVE,
  },
  disabled: {
    id: 'disabled',
    label: LABELS.STATUS.OPTIONS.DISABLED,
  },
};

export const EMPTY_DATA = {
  userId: '',
  source: DEFAULT_SOURCE,
  status: STATUSES.active.id,
};

const findUsersUrl = (userId, source = DEFAULT_SOURCE) => {
  return `${usersUrl}?source=${encodeURIComponent(source)}&userId=${encodeURIComponent(userId)}`;
};
const singleUserUrl = (userId) => `${usersUrl}/${encodeURIComponent(userId)}`;
const createUserUrl = usersUrl;
const getRolesUrl = (source) => `${rolesUrl}?source=${encodeURIComponent(source)}`;
const defaultRolesUrl = getRolesUrl('default');
const changePasswordUrl = (userId) => `${usersUrl}/${encodeURIComponent(userId)}/change-password`;
const resetTokenUrl = (userId, realm) => `${usersUrl}/${encodeURIComponent(userId)}/${realm}/user-token-reset`;

export const URL = {
  usersUrl,
  singleUserUrl,
  createUserUrl,
  defaultRolesUrl,
  findUsersUrl,
  changePasswordUrl,
  resetTokenUrl
};

export const isAnonymousUser = (userId) => userId === ExtJS.state().getValue('anonymousUsername');

export const isExternalUser = (source) => source !== DEFAULT_SOURCE;

export const isCurrentUser = (userId) => userId === ExtJS.state().getUser().id;

export const parseIdParameter = (idString) => {
  const [firstParam, secondParam] = idString.split('/');

  const source = firstParam && secondParam ? firstParam : DEFAULT_SOURCE;
  const userId = firstParam && secondParam ? secondParam : firstParam;

  return {
    source: decodeURIComponent(source),
    userId: userId ?  decodeURIComponent(userId) : EMPTY_DATA.userId,
  };
};

export const fullName = ({firstName, lastName}) => `${firstName || ''} ${lastName || ''}`;
export const sourceLabel = (source) => isExternalUser(source) ? source : LABELS.LOCAL_USER_SOURCE;
