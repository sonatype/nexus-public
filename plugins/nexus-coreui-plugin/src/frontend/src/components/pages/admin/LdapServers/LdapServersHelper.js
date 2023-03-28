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
import {
  APIConstants,
  ExtJS,
  Permissions,
  ValidationUtils,
} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

const {
  LDAP_SERVERS: {FORM},
} = UIStrings;

const {
  REST: {
    PUBLIC: {
      LDAP_SERVERS: ldapServersUrl,
      LDAP_CHANGE_ORDER: ldapChangeOrderUrl,
    },
  },
} = APIConstants;

const singleLdapServersUrl = (name) =>
  `${ldapServersUrl}/${encodeURIComponent(name)}`;
const createLdapServersUrl = ldapServersUrl;
const changeLdapServersOrderUrl = ldapChangeOrderUrl;

export const URL = {
  ldapServersUrl,
  singleLdapServersUrl,
  createLdapServersUrl,
  changeLdapServersOrderUrl,
};

export const TABS_INDEX = {
  CREATE_CONNECTION: 0,
  USER_AND_GROUP: 1,
};

export const canCreate = () => {
  return ExtJS.checkPermission(Permissions.LDAP.CREATE);
};

export const canDelete = () => {
  return ExtJS.checkPermission(Permissions.LDAP.DELETE);
};

export const canUpdate = () => {
  return ExtJS.checkPermission(Permissions.LDAP.UPDATE);
};

export const isAnonymousAuth = (authScheme) => {
  const anonymousAuth = FORM.AUTHENTICATION.OPTIONS.anonymous.id;
  return anonymousAuth === authScheme;
};

export const isSimpleAuth = (authScheme) => {
  const simpleAuth = FORM.AUTHENTICATION.OPTIONS.simple.id;

  return simpleAuth === authScheme;
};

export const isDigestAuth = (authScheme) => {
  const digestAuth = FORM.AUTHENTICATION.OPTIONS.digest.id;
  return digestAuth === authScheme;
};

export const isCramtAuth = (authScheme) => {
  const cramAuth = FORM.AUTHENTICATION.OPTIONS.cram.id;
  return cramAuth === authScheme;
};

export const isLdapsProtocol = (protocol) => {
  return FORM.PROTOCOL.OPTIONS.ldaps === protocol;
};

export const validateUrlValues = (protocol, host, port) => {
  return (
    isLdapsProtocol(protocol) &&
    ValidationUtils.notBlank(protocol) &&
    ValidationUtils.notBlank(host) &&
    ValidationUtils.notBlank(port)
  );
};

export const generateUrl = (protocol, host, port) => {
  return `${protocol}://${host}:${port}`;
};

export const isDynamicGroup = (groupType) => {
  return groupType === FORM.GROUP_TYPE.OPTIONS.dynamic.id;
};

export const isStaticGroup = (groupType) => {
  return groupType === FORM.GROUP_TYPE.OPTIONS.static.id;
};

const findOptionById = (value, list) => {
  return Object.values(list).find((item) => item.id === value);
};

export const findAuthMethod = (value) => {
  return findOptionById(value, FORM.AUTHENTICATION.OPTIONS);
};

export const findGroupType = (value) => {
  return findOptionById(value, FORM.GROUP_TYPE.OPTIONS);
};
