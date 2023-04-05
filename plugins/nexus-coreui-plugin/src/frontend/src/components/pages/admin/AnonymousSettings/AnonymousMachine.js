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
import Axios from 'axios';
import {ExtJS, FormUtils, ValidationUtils, APIConstants} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {ERROR, ANONYMOUS_SETTINGS} = UIStrings;

const {
  REALMS_TYPES,
  ANONYMOUS_SETTINGS: ANONYMOUS_API
} = APIConstants.REST.INTERNAL;

export default FormUtils.buildFormMachine({
  id: 'AnonymousSettingsForm'
}).withConfig({
  actions: {
    setData: assign({
      data: (_, {data: [, settings]}) => settings.data,
      pristineData: (_, {data: [, settings]}) => settings.data,
      realms: (_, {data: [realms]}) => realms.data
    }),

    validate: assign({
      validationErrors: ({data}) => ({
        userId: ValidationUtils.isBlank(data?.userId) ? ERROR.FIELD_REQUIRED : null
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
      Axios.get(REALMS_TYPES),
      Axios.get(ANONYMOUS_API)
    ]),
    saveData: ({data}) => Axios.put(ANONYMOUS_API, {...data, userId: data.userId.trim()})
  }
});
