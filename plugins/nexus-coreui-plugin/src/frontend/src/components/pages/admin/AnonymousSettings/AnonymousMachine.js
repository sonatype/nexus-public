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
import React, {useState, useEffect} from 'react';
import {assign} from 'xstate';
import Axios from 'axios';
import {ExtJS, Utils} from 'nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {ERROR, ANONYMOUS_SETTINGS} = UIStrings;


export default Utils.buildFormMachine({
  id: 'AnonymousSettingsForm'
}).withConfig({
  actions: {
    setData: assign({
      data: (_, {data: [realms, settings]}) => settings.data,
      pristineData: (_, {data: [realms, settings]}) => settings.data,
      realms: (_, {data: [realms]}) => realms.data
    }),

    validate: assign({
      validationErrors: ({data}) => ({
        userId: Utils.isBlank(data?.userId) ? ERROR.FIELD_REQUIRED : null
      })
    }),

    logLoadError: (_, {error}) => {
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(ANONYMOUS_SETTINGS.MESSAGES.LOAD_ERROR);
    },

    logSaveError: (_, {error}) => {
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(ANONYMOUS_SETTINGS.MESSAGES.SAVE_ERROR);
    },
    logSaveSuccess: () => ExtJS.showSuccessMessage(ANONYMOUS_SETTINGS.MESSAGES.SAVE_SUCCESS)
  },
  services: {
    fetchData: () => Axios.all([
      Axios.get('/service/rest/internal/ui/realms/types'),
      Axios.get('/service/rest/internal/ui/anonymous-settings')
    ]),
    saveData: ({data}) => Axios.put('/service/rest/internal/ui/anonymous-settings', data)
  }
});
