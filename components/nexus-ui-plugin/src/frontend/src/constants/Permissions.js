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
import {pick, toUpper, toLower} from 'ramda';

const CREATE = 'CREATE';
const READ = 'READ';
const UPDATE = 'UPDATE';
const DELETE = 'DELETE';
const BROWSE = 'BROWSE';
const EDIT = 'EDIT';
const ADD = 'ADD';
const START = 'START';
const STOP = 'STOP';
const UNINSTALL = 'UNINSTALL';

// default options.
const crudOptions = [CREATE, READ, UPDATE, DELETE];
const breadOptions = [BROWSE, READ, EDIT, ADD, DELETE];

function generatePermissions(permission, options = crudOptions) {
  const base = 'nexus';

  const _ = (value) => `${base}:${permission}:${toLower(value)}`;

  const entries = options.map((value) => [toUpper(value), _(value)]);

  const permissions = Object.fromEntries(entries);

  return pick(options, permissions);
}

export default {
  ADMIN: 'nexus:*',
  SSL_TRUSTSTORE: generatePermissions('ssl-truststore'),
  BLOB_STORES: generatePermissions('blobstores', [READ]),
  SELECTORS: generatePermissions('selectors', [READ]),
  PRIVILEGES: generatePermissions('privileges', [READ]),
  SETTINGS: generatePermissions('settings', [READ, UPDATE]),
  USERS: generatePermissions('users', [READ]),
  ROLES: generatePermissions('roles'),
  TASKS: generatePermissions('tasks', [...crudOptions, START, STOP]),
  LOGGING: generatePermissions('logging', [READ]),
  ATLAS: generatePermissions('atlas', [READ, CREATE]),
  METRICS: generatePermissions('metrics', [READ]),
  BUNDLES: generatePermissions('bundles', [READ]),
  LICENSING: generatePermissions('licensing', [READ, CREATE, UNINSTALL]),
  LDAP: generatePermissions('ldap'),
  USER_TOKENS_SETTINGS: generatePermissions('usertoken-settings', [READ, UPDATE]),
  USER_TOKENS_USERS: generatePermissions('usertoken-users', [DELETE]),
  TAGS: generatePermissions('tags', [READ]),
  COMPONENT: generatePermissions('component', [CREATE]),
  SEARCH: generatePermissions('search', [READ]),
  CAPABILITIES: generatePermissions('capabilities'),
};
