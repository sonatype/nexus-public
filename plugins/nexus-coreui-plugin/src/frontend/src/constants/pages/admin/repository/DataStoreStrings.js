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
  DATASTORE_CONFIGURATION: {
    MENU: {
      text: 'Data Store',
      description: 'Configure the connection used for the database'
    },
    FIELDS: {
      jdbcUrlLabel: 'JDBC URL',
      usernameLabel: 'Username',
      passwordLabel: 'Password',
      schemaLabel: 'Schema',
      maxConnectionPoolLabel: 'Maximum Connection Pool Size',
      advancedLabel: 'Advanced Settings'
    },

    MESSAGES: {
      LOAD_ERROR: 'An error occurred while attempting to load the data store connection configuration',
      SAVE_ERROR: 'An error occurred while attempting to save the data store connection configuration',
      SAVE_SUCCESS: 'Data store connection configuration updated'
    }
  }
};