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
  EXT: {
    URL: '/service/extdirect',
    REPOSITORY: {
      ACTION: 'coreui_Repository',
      METHODS: {
        READ_WITH_FOR_ALL: 'readReferencesAddingEntryForAll',
      }
    },
    PROPRIETARY_REPOSITORIES: {
      ACTION: 'coreui_ProprietaryRepositories',
      METHODS: {
        READ: 'read',
        UPDATE: 'update',
        POSSIBLE_REPOS: 'readPossibleRepos',
      },
    },
    BLOBSTORE: {
      ACTION: 'coreui_Blobstore',
      METHODS: {
        READ_NAMES: 'readNames',
      },
    },
    HEALTH_CHECK: {
      ACTION: 'healthcheck_Status',
      METHODS: {
        READ: 'read',
        UPDATE: 'update',
        ENABLE_ALL: 'enableAll'
      }
    },
    PRIVILEGE: {
      ACTION: 'coreui_Privilege',
      METHODS: {
        READ: {
          NAME: 'read',
          DATA: [{
            page: 1,
            limit: 300,
            start: 0,
            sort: [{
              direction: 'ASC',
              property: 'name',
            }],
          }],
        },
        READ_TYPES: 'readTypes',
      },
    },
    SMALL_PAGE_SIZE: 25,
    BIG_PAGE_SIZE: 300,
    DEFAULT_FIELD_CONFIG: {
      id: 'id',
      type: 'string',
      label: 'Label',
      helpText: 'helpText',
      required: true,
      disabled: false,
      readOnly: false,
      regexValidation: null,
      initialValue: null,
      attributes: {},
      minValue: null,
      maxValue: null,
      storeApi: null,
      storeFilters: null,
      idMapping: null,
      nameMapping: null,
      allowAutocomplete: false,
    },
    HTTP: {
      ACTION: 'coreui_HttpSettings',
      METHODS: {
        READ: 'read',
        UPDATE: 'update'
      }
    },
  },
  REST: {
    INTERNAL: {
      BASE_URL: 'service/rest/internal/ui/',
      get REPOSITORIES() {
        return `${this.BASE_URL}repositories/`;
      },
      get REPOSITORIES_DETAILS() {
        return `${this.REPOSITORIES}details/`;
      },
      get REPOSITORIES_REPOSITORY() {
        return `${this.REPOSITORIES}repository/`;
      }
    },
    PUBLIC: {
      BASE_URL: '/service/rest/v1/',
      get REPOSITORIES() {
        return `${this.BASE_URL}repositories/`;
      },
      get PRIVILEGES() {
        return `${this.BASE_URL}security/privileges`;
      },
      get ACTIVE_REALMS() {
        return `${this.BASE_URL}security/realms/active`;
      },
      get AVAILABLE_REALMS() {
        return `${this.BASE_URL}security/realms/available`;
      },
      get SSL_CERTIFICATES() {
        return `${this.BASE_URL}security/ssl/truststore`;
      },
      get EMAIL_SERVER() {
        return `${this.BASE_URL}email`;
      },
      get VERIFY_EMAIL_SERVER() {
        return `${this.BASE_URL}email/verify`;
      },
    },
  },
  DEBOUNCE_DELAY: 500,
  SORT_DIRECTIONS: {
    DESC: 'desc',
    ASC: 'asc',
  },
};
