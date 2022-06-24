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
    PROPRIETARY_REPOSITORIES: {
      ACTION: 'coreui_ProprietaryRepositories',
      METHODS: {
        READ: 'read',
        UPDATE: 'update',
        POSSIBLE_REPOS: 'readPossibleRepos'
      }
    },
    BLOBSTORE: {
      ACTION: 'coreui_Blobstore',
      METHODS: {
        READ_NAMES: 'readNames'
      }
    },
    HEALTH_CHECK: {
      ACTION: 'healthcheck_Status',
      METHODS: {
        READ: 'read',
        UPDATE: 'update',
        ENABLE_ALL: 'enableAll'
      }
    }
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
      }
    }
  }
};
