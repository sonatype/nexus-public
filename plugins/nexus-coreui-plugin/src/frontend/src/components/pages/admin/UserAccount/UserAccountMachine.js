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
import {assign, Machine} from 'xstate';
import Axios from 'axios';
import {ExtJS, Utils} from 'nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const userAccountMachine = Utils.buildFormMachine({
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
        firstName: Utils.isBlank(data.firstName) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        lastName: Utils.isBlank(data.lastName) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        email: Utils.isBlank(data.email) ? UIStrings.ERROR.FIELD_REQUIRED : null
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
