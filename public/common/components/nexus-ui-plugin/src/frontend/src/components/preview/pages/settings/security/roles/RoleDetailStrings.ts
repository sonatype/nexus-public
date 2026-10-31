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

export const ROLE_DETAIL = {
  LOADING: 'Loading role details...',
  NOT_FOUND: 'Role not found',
  READ_ONLY_BANNER: 'This role is read-only and cannot be modified.',

  BUTTON_SAVE: 'Save',
  BUTTON_SAVING: 'Saving...',
  BUTTON_CANCEL: 'Cancel',
  BUTTON_DELETE: 'Delete Role',
  BUTTON_EDIT_PRIVILEGES: 'Edit Privileges',
  BUTTON_EDIT_CONTAINED_ROLES: 'Edit Contained Roles',

  SECTION_INFO: {
    TITLE: 'Role Information',
    ID_LABEL: 'ID',
    NAME_LABEL: 'Name',
    NAME_PLACEHOLDER: 'My Custom Role',
    DESCRIPTION_LABEL: 'Description',
    DESCRIPTION_PLACEHOLDER: 'Grants read access to all Maven repositories',
    DESCRIPTION_EMPTY: '-',
  },

  SECTION_PRIVILEGES: {
    TITLE: 'Privileges',
    COUNT: (count: number) => `${count} privilege${count === 1 ? '' : 's'} selected`,
    EMPTY: 'No privileges assigned',
  },

  SECTION_CONTAINED_ROLES: {
    TITLE: 'Contained Roles',
    COUNT: (count: number) => `${count} role${count === 1 ? '' : 's'} selected`,
    EMPTY: 'No contained roles',
  },

  SECTION_EFFECTIVE: {
    TITLE: 'Effective Permissions',
    LOADING: 'Computing effective permissions...',
    EMPTY: 'No effective permissions',
    COUNT: (count: number) => `${count} effective privilege${count === 1 ? '' : 's'}`,
    GRANTED_BY: (names: string) => `Granted by: ${names}`,
  },

  SECTION_SECURITY_TREE: {
    TITLE: 'Security Tree',
  },

  SECTION_ASSIGNED_USERS: {
    TITLE: 'Assigned Users',
    LOADING: 'Loading assigned users...',
    EMPTY: 'No users are assigned this role',
    COUNT: (count: number) => `${count} user${count === 1 ? '' : 's'} assigned`,
  },

  PICKERS: {
    PRIVILEGES_TITLE: 'Privileges Selection',
    PRIVILEGES_EMPTY: 'No privileges available',
    CONTAINED_ROLES_TITLE: 'Contained Roles Selection',
    CONTAINED_ROLES_EMPTY: 'No roles available',
  },
} as const;
