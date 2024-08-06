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
  ROLES: {
    MENU: {
      text: 'Roles',
      description: 'Manage roles'
    },
    LIST: {
      CREATE_BUTTON: 'Create Role',
      EMPTY_LIST: 'There are no roles available',
      COLUMNS: {
        ID: 'Id',
        NAME: 'Name',
        DESCRIPTION: 'Description',
      },
      HELP: {
        TITLE: 'What is a role?',
        TEXT: <>
          Roles bring together multiple privileges so that, when you assign a user to the role,
          that user will automatically have all of those privileges. Roles can comprise both
          other roles and individual privileges. See our{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/roles">
            help documentation
          </NxTextLink>
          {' '}for more information.
        </>,
      },
      ALERT: {
        DEFAULT_ROLE: (roleId, roleName) => <>
          This instance is using{' '}
          <NxTextLink href={`#admin/security/roles:${roleId}`}>
            {roleName}
          </NxTextLink>
          {' '}as the default role granted to all authenticated users.
        </>,
        CAPABILITY: (capabilityId) => <>
          To manage this configuration, see the{' '}
          <NxTextLink href={`#admin/system/capabilities:${capabilityId}`}>
            Default Role capability
          </NxTextLink>
          {' '}page.
        </>
      },
    },
    FORM: {
      CREATE_TITLE: 'Create Role',
      EDIT_TILE: (name) => `Edit ${name}`,
      EDIT_DESCRIPTION: 'Nexus Role',
      DEFAULT_ROLE_WARNING: 'This is a default role and cannot be modified.',
      SECTIONS: {
        TYPE: 'Role Type',
        SETUP: 'Role Setup',
        PRIVILEGES: 'Privileges',
        ROLES: 'Roles',
      },
      TYPE: {
        LABEL: 'Type',
        OPTIONS: {
          NEXUS: 'Nexus role',
          EXTERNAL: 'External Role Mapping',
        }
      },
      EXTERNAL_TYPE: {
        LABEL: 'External Role Type', 
        LDAP: {
          MORE_CHARACTERS: 'Enter 3 or more characters',
          NO_RESULTS: 'If no results are found, you can still save this mapping and create an LDAP ' +
            'role with an identical name later. Mapping will not apply until an identically named LDAP role exists.'
        }
      },
      ID: {
        LABEL: 'Role ID',
      },
      MAPPED_ROLE: {
        LABEL: 'Mapped Role',
      },
      NAME: {
        LABEL: 'Role Name',
      },
      DESCRIPTION: {
        LABEL: 'Role Description',
      },
      PRIVILEGES: {
        LABEL: 'Privileges',
        AVAILABLE: 'Available',
        SELECTED: 'Given',
        EMPTY_LIST: 'This role does not have any privileges.',
      },
      ROLES: {
        LABEL: 'Roles',
        AVAILABLE: 'Available',
        SELECTED: 'Contained',
        EMPTY_LIST: 'This role does not contain any other roles.',
      },
    },
    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete Role',
        MESSAGE: (name) => `Are you sure you want to delete the role named "${name}?"`,
        YES: 'Delete',
        NO: 'Cancel'
      },
      DELETE_SUCCESS: (name) => `Role deleted: ${name}`,
    },
    SELECTION_MODAL: {
      WILDCARD_TEXT: <i>Use * as a wildcard</i>
    }
  }
};
