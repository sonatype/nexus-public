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
import {ExtJS, Permissions, APIConstants} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

const {REST: {PUBLIC: {TASKS: tasksUrl}}} = APIConstants;
const {TASKS: {FORM: LABELS }} = UIStrings;

export const canCreateTask = () => ExtJS.checkPermission(Permissions.TASKS.CREATE);
export const canDeleteTask = () => ExtJS.checkPermission(Permissions.TASKS.DELETE);
export const canUpdateTask = () => ExtJS.checkPermission(Permissions.TASKS.UPDATE);
export const canRunTask = () => ExtJS.checkPermission(Permissions.TASKS.START);
export const canStopTask = () => ExtJS.checkPermission(Permissions.TASKS.STOP);

const runTaskUrl = id => `${tasksUrl}/${encodeURIComponent(id)}/run`;
const stopTaskUrl = id => `${tasksUrl}/${encodeURIComponent(id)}/stop`;

export const URLs = {runTaskUrl, stopTaskUrl};

export const NOTIFICATION_CONDITIONS = {
  FAILURE: {
    LABEL: LABELS.SEND_NOTIFICATION_ON.OPTIONS.FAILURE,
    ID: 'FAILURE'
  },
  SUCCESS_FAILURE: {
    ID: 'SUCCESS_FAILURE',
    LABEL: LABELS.SEND_NOTIFICATION_ON.OPTIONS.SUCCESS_FAILURE,
  },
};

export const INITIAL_DATA = {
  typeId: '',
  name: '',
  enabled: true,
  alertEmail: '',
  notificationCondition: NOTIFICATION_CONDITIONS.FAILURE.ID,
  properties: {},
  // TODO Remove when NEXUS-37051 is done.
  schedule: 'manual',
};
