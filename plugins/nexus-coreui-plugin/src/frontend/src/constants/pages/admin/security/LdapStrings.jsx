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
  LDAP_SERVERS: {
    MENU: {
      text: 'LDAP',
      description: 'Lightweight directory access protocol setup',
    },
    LIST: {
      LABEL: 'LDAP Connections',
      EMPTY_LIST: 'There are no LDAP servers available',
      COLUMNS: {
        ORDER: 'Order',
        NAME: 'Name',
        URL: 'Url',
      },
      BUTTONS: {
        CREATE: 'Create connection',
        CHANGE_ORDER: 'Change Order',
        CLEAR_CACHE: 'Clear cache',
      },
      HELP: {
        TITLE: 'What is LDAP?',
        TEXT: <>
          You can configure your NXRM instance to use LDAP for authentication and user role mapping. The repository
          manager can cache authentication information and supports multiple LDAP servers and user/group mappings
          to take advantage of the central authentication set up across your organization in all your repository managers.
          For more information check{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/ldap">
            the documentation
          </NxTextLink>.
        </>,
      },
      MODAL: {
        LABEL: 'Change LDAP servers ordering',
        SUB_LABEL: 'Reorder servers',
        FOOTER: (value) => `${value} Items`,
      },
      MESSAGES: {
        LIST_CHANGED: 'LDAP server order changed',
        CACHE_CLEARED: 'LDAP cache has been cleared'
      }
    },
    FORM: {
      NAME: 'Name',
      CONFIGURATION: 'Configuration',
      SETTINGS: {
        LABEL: 'LDAP Settings',
        SUB_LABEL: 'The LDAP server usually listens on port 389 (ldap://) or port 636 (ldaps://)',
      },
      PROTOCOL: {
        LABEL: 'Protocol',
        OPTIONS: {
          ldap: 'LDAP',
          ldaps: 'LDAPS',
        }
      },
      HOSTNAME: 'Hostname',
      PORT: 'Port',
      SEARCH: {
        LABEL: 'Search base DN',
        SUB_LABEL: 'LDAP location to be added to the connection URL (e.g. "dc=example,dc=com")'
      },
      AUTHENTICATION: {
        LABEL: 'Authentication method',
        OPTIONS: {
          simple: {
            id: 'SIMPLE',
            label: 'Simple Authentication'
          },
          anonymous: {
            id: 'NONE',
            label: 'Anonymous Authentication'
          },
          digest: {
            id: 'DIGEST_MD5',
            label: 'DIGEST-MD5'
          },
          cram: {
            id: 'CRAM_MD5',
            label: 'CRAM-MD5'
          }
        }
      },
      SASL_REALM: {
        LABEL: 'SASL realm',
        SUB_LABEL: 'The SASL realm to bind to (e.g. mydomain.com)'
      },
      USERNAME: {
        LABEL: 'Username or DN',
        SUB_LABEL: 'This must be a fully qualified username if simple authentication is used'
      },
      PASSWORD: {
        LABEL: 'Password',
        SUB_LABEL: 'The password to bind with'
      },
      CHANGE_PASSWORD: 'Change password',
      CONNECTION_RULES: {
        LABEL: 'Connection Rules',
        SUB_LABEL:'Set timeout parameters and max connection attempts to avoid being blacklisted'
      },
      WAIT_TIMEOUT: {
        LABEL: 'Wait timeout',
        SUB_LABEL: 'Seconds to wait before timeout'
      },
      RETRY_TIMEOUT: {
        LABEL: 'Retry Timeout',
        SUB_LABEL: 'Seconds to wait before retrying'
      },
      MAX_RETRIES: {
        LABEL: 'Max Retries',
        SUB_LABEL: 'Number of retry attempts'
      },
      VERIFY_CONNECTION: 'Verify connection',
      NEXT: 'Next',
      TEMPLATE: {
        LABEL: 'Configuration template',
      },
      USER_RELATIVE_DN: {
        LABEL: 'User relative DN',
        SUB_LABEL: 'The relative DN where user objects are found (e.g. ou=people). This value will have the Search base DN value appended to form the full User search base DN'
      },
      USER_SUBTREE: {
        LABEL: 'User subtree',
        SUB_LABEL: 'Are users located in the structures below the user base DN?'
      },
      OBJECT_CLASS:{
        LABEL: 'Object class',
        SUB_LABEL: 'LDAP class for user objects (e.g. inetOrgPerson)'
      },
      USER_FILTER: {
        LABEL: 'User filter',
        SUB_LABEL: 'LDAP search filter to limit user search (e.g. "attribute=foo" or "(|(mail=*@example.com)(uid=dom*))")'
      },
      USER_ID_ATTRIBUTE:'User ID attribute',
      REAL_NAME_ATTRIBUTE: 'Real name attribute',
      EMAIL_ATTRIBUTE: 'Email attribute',
      PASSWORD_ATTRIBUTE: {
        LABEL: 'Password attribute',
        SUB_LABEL: 'If this field is left blank the user will be authenticated against a bind with the LDAP server'
      },
      MAP_LDAP:{
        LABEL: 'Map LDAP groups as roles',
        SUB_LABEL: 'ENABLE'
      },
      GROUP_TYPE: {
        LABEL: 'Group type',
        OPTIONS: {
          dynamic: {
            id: 'DYNAMIC',
            label: 'Dynamic Groups'
          },
          static: {
            id: 'STATIC',
            label: 'Static Groups'
          }
        }
      },
      GROUP_MEMBER_OF_ATTRIBUTE : {
        LABEL: 'Group member of attribute',
        SUB_LABEL: 'Set this to the attribute used to store the attribute which holds groups DN in the user object'
      },
      GROUP_DN: {
        LABEL: 'Group relative DN',
        SUB_LABEL: 'The relative DN where group objects are found (e.g. ou=Group). This value will have the Search base DN value appended to form the full group search base DN',
      },
      GROUP_SUBTREE: {
        LABEL: 'Group subtree',
        SUB_LABEL: 'Are groups located in structures below the group base DN'
      },
      GROUP_OBJECT_CLASS: {
        LABEL: 'Group object class',
        SUB_LABEL: "LDAP class for group objects (e.g. posixGroup)"
      },
      GROUP_ID_ATTRIBUTE: {
        LABEL: 'Group ID attribute'
      },
      GROUP_MEMBER_ATTRIBUTE: {
        LABEL: 'Group member attribute',
        SUB_LABEL: 'LDAP attribute containing the usernames for the group.'
      },
      GROUP_MEMBER_FORMAT: {
        LABEL: 'Group member format',
        SUB_LABEL: 'The format of user ID stored in the group member attribute (e.g. "uid=${username},ou=people,dc=example,dc=com")'
      },
      ALERT: 'Proceed to the User and Group tab to confirm changes and save.',
      DELETE_BUTTON: 'Delete Connection',
      MODAL_DELETE: {
        LABEL: 'Confirm deletion',
        CONFIRM: 'Delete Connection',
      },
      MODAL_PASSWORD: {
        TITLE: 'LDAP Server system password',
        LABEL: 'Password',
        SUB_LABEL: 'The password to bind with'
      },
      MODAL_VERIFY_LOGIN: {
        TITLE: 'Login Credentials',
        DESCRIPTION: 'Enter LDAP Username and password to verify',
        USERNAME: {
          LABEL: 'LDAP Server Username',
          PLACEHOLDER: 'Username'
        },
        PASSWORD: {
          LABEL: 'LDAP Server Password',
          PLACEHOLDER: 'Password'
        },
        TEST_CONNECTION_BUTTON: 'Test Connection',
        SUCCESS_MESSAGE: 'LDAP login completed successfully on: ',
        ERROR_MESSAGE: 'An error occurred.'
      },
      MODAL_VERIFY_USER_MAPPING: {
        LIST: {
          HEADER: 'Verify User Mapping',
          TABLE: {
            USER_ID: 'USER ID',
            NAME: 'NAME',
            EMAIL: 'EMAIL',
            ROLES: 'ROLES'
          }
        },
        ITEM: {
          BACK_BUTTON: 'Back to User Mapping List',
          LABELS: {
            USER_ID: 'User ID',
            NAME: 'Name',
            EMAIL: 'Email',
            ROLES: 'Roles'
          }
        }
      },
      TABS: {
        CONNECTION: 'Connection',
        USER_AND_GROUP: 'User and Group'
      },
      VERIFY_USER_MAPPING_BUTTON: 'Verify User Mapping',
      VERIFY_LOGIN_BUTTON: 'Verify Login',
      VERIFY_SUCCESS_MESSAGE: (url) => `Connection to LDAP server verified: ${url}`,
      VERIFY_DELETE_MESSAGE: (name) => `Are you sure you want to delete the ${name} LDAP connection?`,
      DELETE_SUCCESS_MESSAGE: (name) => `LDAP server deleted: ${name}`,
      SAVE_SUCCESS_MESSAGE: (name) => `LDAP server created: ${name}`,
      UPDATE_SUCCESS_MESSAGE: (name) => `LDAP server updated: ${name}`,
      VERIFYING_MESSAGE: (url) => `Checking connection to ${url}`,
      CHANGING_PASSWORD_MESSAGE: (url) => `Changing password to ${url}`,
    }
  }
};
