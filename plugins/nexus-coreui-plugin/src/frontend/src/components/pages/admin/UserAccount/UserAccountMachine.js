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
import {assign, Machine} from 'xstate';
import Axios from 'axios';
import {ExtJS, FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const userAccountMachine = FormUtils.buildFormMachine({
  id: 'UserAccount'
}).withConfig({
  actions: {
    logLoadError: ({error}) => {
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(UIStrings.USER_ACCOUNT.MESSAGES.LOAD_ERROR);
    },
    logSaveSuccess: () => {
      ExtJS.showSuccessMessage(UIStrings.USER_ACCOUNT.MESSAGES.UPDATE_SUCCESS);
    },

    validate: assign({
      validationErrors: ({data}) => ({
        firstName: ValidationUtils.isBlank(data.firstName) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        lastName: ValidationUtils.isBlank(data.lastName) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        email: ValidationUtils.validateEmail(data.email)
      })
    })
  },
  services: {
    fetchData: () => Axios.get('/service/rest/internal/ui/user'),
    saveData: ({data}) => {
      return Axios.put('/service/rest/internal/ui/user', data);
    }
  }
});

export default userAccountMachine;
