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
import Axios from 'axios';
import {assign} from 'xstate';

import {ExtJS, FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {DATASTORE_CONFIGURATION} = UIStrings;

export default FormUtils.buildFormMachine({
  id: 'DataStoreConfigurationForm'
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        maximumConnectionPool: ValidationUtils.isInRange({
          value: data.maximumConnectionPool,
          min: 1,
          max: 3000,
          allowDecimals: false,
        })
      })
    }),
    setData: assign({
      data: (_, {data}) => data.data,
      pristineData: (_, {data}) => data.data
    }),

    logSaveError: (_, event) => {
      const error = event.data.data;
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(DATASTORE_CONFIGURATION.MESSAGES.SAVE_ERROR);
    },
    logSaveSuccess: () => ExtJS.showSuccessMessage(DATASTORE_CONFIGURATION.MESSAGES.SAVE_SUCCESS),

    logLoadError: (_, event) => {
      const error = event.data.data;
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(DATASTORE_CONFIGURATION.MESSAGES.LOAD_ERROR);
    }
  },
  services:
      {
        fetchData: () => Axios.get('service/rest/internal/ui/datastore'),
        saveData: ({data}) => {
          return Axios.put('service/rest/internal/ui/datastore', data);
        }
      }
});

