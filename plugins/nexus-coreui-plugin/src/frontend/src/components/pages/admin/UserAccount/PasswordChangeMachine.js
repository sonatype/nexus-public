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
import {assign} from 'xstate';
import {ExtJS, FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';
import Axios from "axios";

import UIStrings from '../../../../constants/UIStrings';

const EMPTY_DATA = {
  passwordCurrent: '',
  passwordNew: '',
  passwordNewConfirm: ''
};

export default FormUtils.buildFormMachine({
  id: 'passwordChange',
  initial: 'loaded',
  config: (config) => ({
    ...config,
    context: {
      ...config.context,
      data: EMPTY_DATA,
      pristineData: EMPTY_DATA
    }
  })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data: {passwordCurrent, passwordNew, passwordNewConfirm}}) => {
        let passwordNewError = [];
        if (ValidationUtils.isBlank(passwordNew)) {
          passwordNewError.push(UIStrings.ERROR.FIELD_REQUIRED);
        }
        if (passwordCurrent === passwordNew) {
          passwordNewError.push(UIStrings.USER_ACCOUNT.MESSAGES.PASSWORD_MUST_DIFFER_ERROR);
        }

        let passwordNewConfirmError = [];
        if (ValidationUtils.isBlank(passwordNewConfirm)) {
          passwordNewConfirmError.push(UIStrings.ERROR.FIELD_REQUIRED);
        }
        if (passwordNew !== passwordNewConfirm) {
          passwordNewConfirmError.push(UIStrings.ERROR.PASSWORD_NO_MATCH_ERROR);
        }

        return {
          passwordCurrent: ValidationUtils.isBlank(passwordCurrent) ? UIStrings.ERROR.FIELD_REQUIRED : null,
          passwordNew: passwordNewError,
          passwordNewConfirm: passwordNewConfirmError
        };
      }
    }),
    logSaveSuccess: () => ExtJS.showSuccessMessage(UIStrings.USER_ACCOUNT.MESSAGES.PASSWORD_CHANGE_SUCCESS),
    logSaveError: ({saveError, saveErrors = {}}) => {
      let error = saveError ||
          saveErrors['*'] ||
          UIStrings.USER_ACCOUNT.MESSAGES.PASSWORD_CHANGE_ERROR

      ExtJS.showErrorMessage(error);
    },
    onSaveSuccess: assign({
      data: () => EMPTY_DATA,
      pristineData: () => EMPTY_DATA
    })
  },
  services: {
    saveData: ({data: {passwordCurrent, passwordNew}}, {userId}) =>
        ExtJS.fetchAuthenticationToken(userId, passwordCurrent).then((result) =>
            Axios.put(`/service/rest/internal/ui/user/${userId}/password`, {
              authToken: result.data,
              password: passwordNew
            }))
  }
});
