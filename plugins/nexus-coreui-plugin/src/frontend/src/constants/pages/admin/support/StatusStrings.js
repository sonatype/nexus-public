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
  METRIC_HEALTH: {
    MENU: {
      text: 'Status',
      description: 'System status checks',
      detailsDescription: 'View Node specific details',
    },
    NAME_HEADER: 'Name',
    MESSAGE_HEADER: 'Message',
    ERROR_HEADER: 'Error',
    STATUS_HEADER: 'Status',
    BACK_BUTTON: 'Back to Status Table View',
    EMPTY_NODE_LIST: 'There are no nodes available',
    EMPTY_NODE: 'There is no node available',
    HELP: {
      LABEL: 'What are Nodes?',
      SUB_LABEL:
        'When clustering is enabled, redundant Nexus Repository instances (i.e., nodes) run in active/active mode (i.e., both actively running Nexus Repository simultaneously) within a single cloud region or on-premises data center. This allows you to maintain Nexus Repository availability even if one node becomes unavailable. Node status displayed on this screen is updated at 5-minute intervals.',
    },
  },
};
