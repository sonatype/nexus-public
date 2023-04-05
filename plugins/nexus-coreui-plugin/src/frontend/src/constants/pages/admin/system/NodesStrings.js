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
  NODES: {
    MENU: {
      text: 'Nodes',
      description: 'See your available nodes'
    },
    READ_ONLY: {
      ENABLE: {
        TITLE: 'Enable read-only mode?',
        MESSAGE: 'Are you sure you want to reject additions of new components and changes to configuration?',
        BUTTON: 'Enable Read-only Mode',
        ERROR: 'An error occurred while enabling read-only mode. ',
        LOADING: 'Enabling...'
      },
      DISABLE: {
        FORCIBLY: {
          TITLE: 'Forcibly disable read-only mode?',
          MESSAGE: 'Warning: read-only mode has been enabled by system tasks. Releasing read-only mode before those tasks are complete may cause them to fail and/or cause data loss. Are you sure you want to forcibly release read-only mode?',
          BUTTON: 'Force Disable Read-only Mode',
        },
        TITLE: 'Disable read-only mode?',
        MESSAGE: 'Are you sure you want to stop rejecting additions of new components and changes to configuration?',
        BUTTON: 'Disable Read-only Mode',
        ERROR: 'An error occurred while disabling read-only mode. ',
        LOADING: 'Disabling...'
      }
    },
    HELP: {
      TITLE: 'What are Nodes?',
      TEXT: 'When clustering is enabled, redundant Nexus Repository instances (i.e., nodes) run in active/active mode (i.e., both actively running Nexus Repository simultaneously) within a single cloud region or on-premises data center. This allows you to maintain Nexus Repository availability even if one node becomes unavailable. Only active Nodes will appear here.'
    }
  }
};
