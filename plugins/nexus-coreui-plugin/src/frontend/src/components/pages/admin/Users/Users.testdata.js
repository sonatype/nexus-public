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

const {EXT: {USER: {ACTION, METHODS}}} = APIConstants;

export const ROWS = {
  CROWD: [
    {
      userId: 'test #1',
      version: '0',
      realm: 'Crowd',
      firstName: 'first1',
      lastName: 'last1',
      email: 'first1last1@example.org',
      status: 'active',
      password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      roles: [],
      external: true,
      externalRoles: []
    },
    {
      userId: 'test #2',
      version: '0',
      realm: 'Crowd',
      firstName: 'first2',
      lastName: 'last2',
      email: 'first2last2@example.org',
      status: 'active',
      password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      roles: [
        'crowd-administrators'
      ],
      external: true,
      externalRoles: [
        'crowd-administrators'
      ]
    },
    {
      userId: 'test #3',
      version: '0',
      realm: 'Crowd',
      firstName: 'first3',
      lastName: 'last3',
      email: 'first3last3@example.org',
      status: 'active',
      password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      roles: [
        'group__5',
        'group_3',
      ],
      external: true,
      externalRoles: [
        'group__5',
        'group_3',
      ]
    },
    {
      userId: 'admin',
      version: '0',
      realm: 'Crowd',
      firstName: 'admin crowd',
      lastName: 'admin crowd',
      email: 'admin@example.org',
      status: 'active',
      password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      roles: [],
      external: true,
      externalRoles: []
    }
  ],
  LOCAL: [
    {
      userId: 'anonymous',
      version: '1',
      realm: 'default',
      firstName: 'Anonymous',
      lastName: 'User',
      email: 'anonymous@example.org',
      status: 'active',
      password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      roles: [
        'nx-anonymous'
      ],
      external: false,
      externalRoles: null
    },
    {
      userId: 'admin',
      version: '1',
      realm: 'default',
      firstName: 'Administrator',
      lastName: 'User',
      email: 'admin@example.org',
      status: 'active',
      password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      roles: [
        'nx-admin'
      ],
      external: false,
      externalRoles: null
    }
  ],
};

export const SOURCES = [
  {
    id: 'default',
    name: 'Local'
  },
  {
    id: 'SAML',
    name: 'SAML'
  },
  {
    id: 'Crowd',
    name: 'Crowd'
  },
  {
    id: 'LDAP',
    name: 'LDAP'
  },
  {
    id: 'allConfigured',
    name: 'All Users with Roles'
  }
];

export const SOURCES_MAP = indexBy(prop('id'), SOURCES);

export const ROLES = {
  'nx-admin': {
    description: 'Administrator Role',
    id: 'nx-admin',
    name: 'nx-admin',
    privileges: ['nx-all'],
    roles: [],
    source: 'default'
  },
  'TestRole': {
    description: 'Test role',
    id: 'TestRole',
    name: 'Test Role name',
    privileges: ['nx-healthcheck-read'],
    roles: [],
    source: 'default',
  },
  'replication-role': {
    description: 'Replication',
    id: 'replication-role',
    name: 'Replication role',
    privileges: ['nx-replication-update'],
    roles: [],
    source: 'default',
  }
};

export const FIELDS = {
  USER_ID: 'userId',
  REALM: 'realm',
  FIRST_NAME: 'firstName',
  LAST_NAME: 'lastName',
  EMAIL: 'email',
  STATUS: 'status',
};

export const DEFAULT_DATA = {
  action: ACTION,
  data: [
    {
      limit: 50,
      page: 1,
      filter: [
        {property: 'userId', value: ''},
        {property: 'source', value: 'default'},
      ],
      start: 0
    }
  ],
  method: METHODS.READ,
  tid: 1,
  type: 'rpc'
};
