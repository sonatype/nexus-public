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
import {sendParent} from 'xstate';
import {mergeDeepRight} from 'ramda';
import Axios from 'axios';
import {ExtJS, FormUtils} from '@sonatype/nexus-ui-plugin';
import {URL} from './UsersHelper';
import UIStrings from '../../../../constants/UIStrings';

const {
  USERS: {TOKEN: LABELS},
} = UIStrings;

export default FormUtils.buildFormMachine({
  id: 'resettingTokenMachine',
  initial: 'saving',

  stateAfterSave: 'done',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        done: {
          type: 'final',
        },
      },
      on: {
        CANCEL: {
          actions: sendParent('CANCEL'),
        },
      },
    }),
}).withConfig({
  actions: {
    logSaveSuccess: ({data}) =>
      ExtJS.showSuccessMessage(LABELS.SAVE_SUCCESS(data.name)),
  },
  services: {
    saveData: ({data}) =>
      Axios.delete(URL.resetTokenUrl(data.userId, data.source)),
  },
});
