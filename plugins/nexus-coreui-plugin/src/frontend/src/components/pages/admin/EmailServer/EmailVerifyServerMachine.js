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
import {
  FormUtils,
  APIConstants,
  ValidationUtils,
} from '@sonatype/nexus-ui-plugin';

const {
  REST: {
    PUBLIC: {VERIFY_EMAIL_SERVER},
  },
} = APIConstants;

export default FormUtils.buildFormMachine({
  id: 'EmailVerifyServerMachine',
  initial: 'loaded',
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
      testResult: (_, event) => {
        const result = event.data.data;
        return result.success;
      },
    }),
    setSaveError: assign({
      isTouched: () => ({}),
      testResult: () => false,
    }),
  },
  services: {
    saveData: ({data: {email}}) => Axios.post(VERIFY_EMAIL_SERVER, email),
  },
});
