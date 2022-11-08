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
import {mergeDeepRight, whereEq} from 'ramda';
import {FormUtils, APIConstants, ValidationUtils, ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

import {readFile} from "./LicenseHelper";

const {REST: {PUBLIC: {LICENSE: licenseUrl}}} = APIConstants;
const {LICENSING: LABELS} = UIStrings;

export default FormUtils.buildFormMachine({
  id: 'LicenseMachine',
  stateAfterSave: 'loading',
  config: (config) =>
      mergeDeepRight(config, {
        states: {
          loaded: {
            on: {
              SHOW_AGREEMENT_MODAL: {
                target: 'agreement',
              },
              SET_FILES: {
                target: 'loaded',
                actions: ['reset', 'clearSaveError', 'update']
              },
            },
          },
          loading: {
            invoke: {
              onError: {
                target: 'loaded',
              }
            }
          },
          agreement: {
            on: {
              ACCEPT: {
                target: 'saving',
              },
              DECLINE: {
                target: 'loaded',
              },
            },
          },
        },
      })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        files: ValidationUtils.validateNotBlank(data.files),
      })
    }),
    logLoadError: () => {},
    logSaveError: ({saveError}) => {
      ExtJS.showErrorMessage(`${UIStrings.ERROR.SAVE_ERROR}. ${saveError || ''}`);
    },
    logSaveSuccess: () => ExtJS.showSuccessMessage(LABELS.INSTALL.MESSAGES.SUCCESS),
    setIsPristine: assign({
      isPristine: ({data, pristineData}) => whereEq(data)(pristineData),
    }),
  },
  services: {
    fetchData: () => Axios.get(licenseUrl),
    saveData: async ({data}) => {
      const file = await readFile(data.files.item(0));
      return Axios.post(licenseUrl, file, {
        headers: {
          'Content-Type': 'application/octet-stream',
        }
      });
    },
  },
});
