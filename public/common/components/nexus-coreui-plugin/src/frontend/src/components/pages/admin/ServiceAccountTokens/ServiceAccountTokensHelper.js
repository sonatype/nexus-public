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
import {APIConstants, ExtJS, Permissions} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {REST: {PUBLIC: {SERVICE_ACCOUNT_TOKENS: serviceAccountTokensUrl, ROLES: rolesUrl}}} = APIConstants;
const {SERVICE_ACCOUNT_TOKENS: {CREATE_MODAL: CREATE_LABELS, MESSAGES}} = UIStrings;

const singleTokenUrl = (id) => `${serviceAccountTokensUrl}/${encodeURIComponent(id)}`;

export const URL = {serviceAccountTokensUrl, singleTokenUrl, rolesUrl};

export const TOKEN_MODAL_AUTO_CLOSE_SECONDS = 60;

export const EXPIRATION_OPTIONS = [
  {value: 30, label: CREATE_LABELS.EXPIRATION_OPTIONS.THIRTY_DAYS},
  {value: 60, label: CREATE_LABELS.EXPIRATION_OPTIONS.SIXTY_DAYS},
  {value: 90, label: CREATE_LABELS.EXPIRATION_OPTIONS.NINETY_DAYS},
  {value: 365, label: CREATE_LABELS.EXPIRATION_OPTIONS.ONE_YEAR},
  {value: -1, label: CREATE_LABELS.EXPIRATION_OPTIONS.NEVER},
];

export const canCreateToken = () => ExtJS.checkPermission(Permissions.SERVICE_ACCOUNTS.CREATE);
export const canRevokeToken = () => ExtJS.checkPermission(Permissions.SERVICE_ACCOUNTS.DELETE);

export function mapCreateError(event) {
  const status = event.data?.response?.status;
  if (status === 403) return MESSAGES.CREATE_ERROR_FORBIDDEN;
  if (status === 400) {
    const body = event.data?.response?.data;
    const detail = typeof body === 'string' ? body : body?.message;
    return detail || MESSAGES.CREATE_ERROR_INVALID;
  }
  return MESSAGES.CREATE_ERROR_GENERIC;
}

export function mapRevokeError(event) {
  const status = event.data?.response?.status;
  if (status === 404) return MESSAGES.REVOKE_ERROR_NOT_FOUND;
  if (status === 403) return MESSAGES.REVOKE_ERROR_FORBIDDEN;
  return MESSAGES.REVOKE_ERROR_GENERIC;
}

export function mapRolesError(err) {
  if (err?.response?.status === 403) return CREATE_LABELS.ROLES_LOAD_ERROR_FORBIDDEN;
  return CREATE_LABELS.ROLES_LOAD_ERROR_GENERIC;
}
