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
import {ExtJS, FormUtils, APIConstants, ValidationUtils} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';
import {mergeDeepRight} from 'ramda';
import {assign} from 'xstate';

const API_URL = APIConstants.REST.PUBLIC.USER_TOKENS;

const {RESET_CONFIRMATION, RESET_ERROR_MSG, RESET_SUCCESS_MSG} = UIStrings.USER_TOKEN_CONFIGURATION;

export default FormUtils.buildFormMachine({
  id: 'UserTokensMachine',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        loaded: {
          on: {
            RESET_CONFIRMATION: {
              target: 'resetConfirmation',
              cond: 'canDelete'
            },
            SET_ENABLED: {
              target: 'loaded',
              actions: 'setEnabled'
            },
            SAVE: [
              {
                target: 'showUserTokenExpiryChangesModal',
                cond: 'userTokenExpirationEnabled'
              },
              {
                target: 'saving',
                cond: 'canSave'
              }
            ]
          }
        },
        resetConfirmation: {
          entry: 'setConfirmationString',
          on: {
            DELETE: {
              target: 'delete'
            },
            CANCEL_RESET_CONFIRMATION: {
              target: 'loaded'
            },
            SET_CONFIRMATION_STRING: {
              actions: 'setConfirmationString',
              target: 'resetConfirmation'
            }
          }
        },
        delete: {
          invoke: {
            src: 'delete',
            onDone: {
              target: 'loaded',
            },
            onError: {
              target: 'loaded',
            }
          }
        },
        showUserTokenExpiryChangesModal: {
          on: {
            CLOSE: {
              target: 'loaded'
            },
            SAVE: {
              target: 'saving',
              cond: 'canSave'
            }
          }
        }
      }
    })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        expirationDays: ValidationUtils.validateNotBlank(data.expirationDays) ||
          ValidationUtils.isInRange({value: data.expirationDays, min: 1, max: 999, allowDecimals: false})
      })
    }),
    setConfirmationString: assign((_, {value}) => ({
      confirmationString: value || '',
      confirmationStringValidationError:
        value === RESET_CONFIRMATION.CONFIRMATION_STRING
          ? null
          : RESET_CONFIRMATION.VALIDATION_ERROR
    })),
    onDeleteError: () => {
      ExtJS.showErrorMessage(RESET_ERROR_MSG);
    },
    logDeleteSuccess: () => {
      ExtJS.showSuccessMessage(RESET_SUCCESS_MSG);
    },
    setEnabled: assign({
      data: (_, {value}) => ({
        enabled: value,
        expirationEnabled: false,
        expirationDays: 30,
        protectContent: false
      })
    })
  },
  guards: {
    userTokenExpirationEnabled: ({isTouched}) => isTouched?.expirationEnabled
  },
  services: {
    fetchData: () => Axios.get(API_URL),
    saveData: ({data}) => Axios.put(API_URL, data),
    delete: () => Axios.delete(API_URL)
  }
});
