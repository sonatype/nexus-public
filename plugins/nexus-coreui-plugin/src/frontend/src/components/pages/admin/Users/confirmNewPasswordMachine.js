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
import {assign, sendParent} from 'xstate';
import {mergeDeepRight} from 'ramda';
import {URL} from './UsersHelper';
import {FormUtils, ValidationUtils, ExtJS} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

import Axios from 'axios';

const {
  USERS: { MODAL },
} = UIStrings;

export default FormUtils.buildFormMachine({
  id: 'confirmNewPasswordMachine',
  initial: 'loaded',

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
    validate: assign({
      validationErrors: ({data: {passwordNew, passwordNewConfirm}}) => ({
        passwordNew: ValidationUtils.validateNotBlank(passwordNew),
        passwordNewConfirm:
          ValidationUtils.validateNotBlank(passwordNewConfirm) ||
          ValidationUtils.validatePasswordsMatch(
            passwordNew,
            passwordNewConfirm
          ),
      }),
    }),
    logSaveSuccess: () =>
      ExtJS.showSuccessMessage(
        UIStrings.USER_ACCOUNT.MESSAGES.PASSWORD_CHANGE_SUCCESS
      ),

    setSaveError: assign({
      saveErrorData: ({data}) => data,
      saveError: (_, event) => {
        const status = event.data?.response?.status;

        if (status === 400) {
          return MODAL.ERROR.MISSING_PASSWORD;
        } else if (status === 403) {
          return MODAL.ERROR.PERMISSION;
        } else if (status === 404) {
          return MODAL.ERROR.NOT_FOUND;
        }

        return UIStrings.ERROR.UNKNOWN;
      },
    }),
  },
  services: {
    saveData: ({data: {passwordNew}, userId}) =>
      Axios.put(URL.changePasswordUrl(userId), passwordNew, {
        headers: {'Content-Type': 'text/plain'},
      }),
  },
});
