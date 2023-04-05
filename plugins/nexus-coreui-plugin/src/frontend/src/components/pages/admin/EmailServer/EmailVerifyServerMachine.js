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
import {mergeDeepRight} from 'ramda';
import {
  FormUtils,
  APIConstants,
  ValidationUtils,
  ExtAPIUtils,
} from '@sonatype/nexus-ui-plugin';

const {
  EXT: {
    EMAIL_SERVER: {ACTION, METHODS},
  },
} = APIConstants;

export default FormUtils.buildFormMachine({
  id: 'EmailVerifyServerMachine',
  initial: 'loaded',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        loaded: {
          on: {
            UPDATE_DATA: {
              actions: 'updateData',
            },
          },
        },
      },
    }),
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        email:
          ValidationUtils.notBlank(data.email) &&
          ValidationUtils.validateEmail(data.email),
      }),
    }),
    logSaveSuccess: () => {},
    logLoadError: () => {},
    setSavedData: assign({
      isTouched: () => ({}),
      testResult: (_, event) => event.data.data.result.success,
    }),
    setSaveError: assign({
      isTouched: () => ({}),
      testResult: () => false,
    }),
    updateData: assign((context, {data}) => mergeDeepRight(context, {data})),
  },
  services: {
    saveData: async ({data: {email, ...config}}) => {
      const request = config;

      const response = await ExtAPIUtils.extAPIRequest(ACTION, METHODS.VERIFY, {
        data: [request, email],
      });

      ExtAPIUtils.checkForError(response);

      return response;
    },
  },
});
