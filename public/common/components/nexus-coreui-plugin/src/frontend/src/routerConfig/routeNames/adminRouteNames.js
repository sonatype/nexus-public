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
export default {
  IQ: {
    ROOT: 'admin.iq',
    CONNECTED: 'admin.iqconnected',
    TITLE: 'IQ Server',
    SONATYPE_LIFECYCLE: {
      ROOT: 'admin.sonatypelifecycle',
      TITLE: 'Sonatype Lifecycle',
    },
    HOSTED_REPOS_EVAL: {
      ROOT: 'admin.hostedreposeval',
      TITLE: 'Hosted Repositories Evaluation',
    },
  },
  DIRECTORY: 'admin',
  REPOSITORY: {
    DIRECTORY: 'admin.repository',
    TITLE: 'Repository',
    REPOSITORIES: {
      ROOT: 'admin.repository.repositories',
      TITLE: 'Repositories',
    },
    BLOBSTORES: {
      ROOT: 'admin.repository.blobstores',
      TITLE: 'Blob Stores',
      LIST: 'admin.repository.blobstores.list',
      EDIT: 'admin.repository.blobstores.edit',
      CREATE: 'admin.repository.blobstores.create',
    },
    DATASTORE: {
      ROOT: 'admin.repository.datastore',
      TITLE: 'Data Store',
    },
    PROPRIETARY: {
      ROOT: 'admin.repository.proprietary',
      TITLE: 'Proprietary Repositories',
    },
    SELECTORS: {
      ROOT: 'admin.repository.selectors',
      TITLE: 'Content Selectors',
      LIST: 'admin.repository.selectors.list',
      EDIT: 'admin.repository.selectors.edit',
      CREATE: 'admin.repository.selectors.create',
    },
    ROUTINGRULES: {
      ROOT: 'admin.repository.routingrules',
      TITLE: 'Routing Rules',
      LIST: 'admin.repository.routingrules.list',
      CREATE: 'admin.repository.routingrules.create',
      EDIT: 'admin.repository.routingrules.edit',
      PREVIEW: 'admin.repository.routingrules.preview',
    },
  },
  SECURITY: {
    DIRECTORY: 'admin.security',
    TITLE: 'Security',
    PRIVILEGES: {
      ROOT: 'admin.security.privileges',
      TITLE: 'Privileges',
      LIST: 'admin.security.privileges.list',
      EDIT: 'admin.security.privileges.edit',
      CREATE: 'admin.security.privileges.create',
    },
    ROLES: {
      ROOT: 'admin.security.roles',
      TITLE: 'Roles',
      LIST: 'admin.security.roles.list',
      EDIT: 'admin.security.roles.edit',
      CREATE: 'admin.security.roles.create',
    },
    USERS: {
      ROOT: 'admin.security.users',
      TITLE: 'Users',
    },
    ANONYMOUS: {
      ROOT: 'admin.security.anonymous',
      TITLE: 'Anonymous Access',
    },
    ATLASSIANCROWD: {
      ROOT: 'admin.security.atlassiancrowd',
      TITLE: 'Atlassian Crowd',
    },
    LDAP: {
      ROOT: 'admin.security.ldap',
      TITLE: 'LDAP',
    },
    REALMS: {
      ROOT: 'admin.security.realms',
      TITLE: 'Realms',
    },
    SAML: {
      ROOT: 'admin.security.saml',
      TITLE: 'SAML',
    },
    OAUTH2: {
      ROOT: 'admin.security.oauth2',
      TITLE: 'OAuth 2.0',
    },
    SSLCERTIFICATES: {
      ROOT: 'admin.security.sslcertificates',
      TITLE: 'SSL Certificates',
      LIST: 'admin.security.sslcertificates.list',
      EDIT: 'admin.security.sslcertificates.edit',
      CREATE: 'admin.security.sslcertificates.create',
    },
    USERTOKEN: {
      ROOT: 'admin.security.usertoken',
      TITLE: 'User Tokens',
    },
  },
  SYSTEM: {
    DIRECTORY: 'admin.system',
    TITLE: 'System',
    TASKS: {
      ROOT: 'admin.system.tasks',
      TITLE: 'Tasks',
    },
    API: {
      ROOT: 'admin.system.api',
      TITLE: 'API',
    },
    EMAILSERVER: {
      ROOT: 'admin.system.emailserver',
      TITLE: 'Email Server',
    },
    HTTP: {
      ROOT: 'admin.system.http',
      TITLE: 'HTTP',
    },
    LICENSING: {
      ROOT: 'admin.system.licensing',
      TITLE: 'Licensing',
    },
    NODES: {
      ROOT: 'admin.system.nodes',
      TITLE: 'Nodes',
    },
    UPGRADE: {
      ROOT: 'admin.system.upgrade',
      TITLE: 'Upgrade',
    },
  },
  SUPPORT: {
    DIRECTORY: 'admin.support',
    TITLE: 'Support',
    SUPPORTREQUEST: {
      ROOT: 'admin.support.supportrequest',
      TITLE: 'Support Request',
    },
    LOGS: {
      ROOT: 'admin.support.logs',
      TITLE: 'Logs',
    },
    LOGGING: {
      ROOT: 'admin.support.logging',
      TITLE: 'Logging',
      LIST: 'admin.support.logging.list',
      CREATE: 'admin.support.logging.create',
      EDIT: 'admin.support.logging.edit',
    },
    RECOVERY: {
      ROOT: 'admin.support.recovery',
      TITLE: 'Recovery Mode',
    },
    STATUS: {
      ROOT: 'admin.support.status',
      TITLE: 'Status',
    },
    SUPPORTZIP: {
      ROOT: 'admin.support.supportzip',
      TITLE: 'Support Zip',
    },
    SYSTEMINFORMATION: {
      ROOT: 'admin.support.systeminformation',
      TITLE: 'System Information',
    },
  },
};
