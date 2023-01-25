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
  PRIVILEGES: {
    MENU: {
      text: 'Privileges',
      description: 'Manage Privileges'
    },
    LIST: {
      CREATE_BUTTON: 'Create Privilege',
      EMPTY_LIST: 'There are no privileges available',
      COLUMNS: {
        NAME: 'Name',
        DESCRIPTION: 'Description',
        TYPE: 'Type',
        PERMISSION: 'Permission',
      },
      HELP: {
        TITLE: 'What is a Privilege?',
        TEXT: <>
          Privileges are assigned to roles and control the actions that each role can perform.
          See our{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/privileges">
            documentation
          </NxTextLink>
          {' '}for more information.
        </>,
      },
    },
    FORM: {
      CREATE_TITLE: 'Create Privilege',
      EDIT_TILE: (name) => `Edit ${name}`,
      EDIT_DESCRIPTION: 'Nexus Privilege',
      DEFAULT_PRIVILEGE_WARNING: 'This is a default privilege and cannot be modified.',
      SECTIONS: {
        SETUP: 'Privilege Setup',
      },
      TYPE: {
        LABEL: 'Type',
      },
      NAME: {
        LABEL: 'Name',
      },
      DESCRIPTION: {
        LABEL: 'Description',
      },
      FORMAT: {
        LABEL: 'Format',
        SUB_LABEL: 'The format(s) for the repository',
      },
      PRIVILEGE_STRING: {
        LABEL: 'Privilege String',
        SUB_LABEL: <>
          The internal segment matching algorithm uses Apache Shiro wildcard <br />permissions; see{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/privileges">
            our documentation
          </NxTextLink>
          {' '}for more details
        </>,
      },
    },
    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete Privilege',
        MESSAGE: (name) => `Are you sure you want to delete the privilege named "${name}?"`,
        YES: 'Delete',
        NO: 'Cancel'
      },
      DELETE_SUCCESS: (name) => `Privilege deleted: ${name}`,
    },
  }
};