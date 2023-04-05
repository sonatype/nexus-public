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
import {mergeDeepRight} from 'ramda';

import {
  ExtJS,
  FormUtils,
  ValidationUtils,
  ExtAPIUtils,
  APIConstants,
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import {EMPTY_DATA, URL, fullName, isExternalUser, STATUSES} from './UsersHelper';
import confirmAdminPasswordMachine from './confirmAdminPasswordMachine';
import confirmNewPasswordMachine from './confirmNewPasswordMachine';
import resettingTokenMachine from './resettingTokenMachine';

const {USERS: {MESSAGES: LABELS}} = UIStrings;
const {EXT: {USER: {ACTION, METHODS}}} = APIConstants;

const {singleUserUrl, createUserUrl, defaultRolesUrl, findUsersUrl} = URL;

const isEdit = (userId) => ValidationUtils.notBlank(userId);

function validatePasswordConfirm(password, passwordConfirm) {
  let error = ValidationUtils.validateNotBlank(passwordConfirm);
  if (!error && password !== passwordConfirm) {
    error = UIStrings.ERROR.PASSWORD_NO_MATCH_ERROR;
  }
  return error;
}

export default FormUtils.buildFormMachine({
  id: 'UsersFormMachine',
  config: (config) => mergeDeepRight(config, {
    states: {
      loaded: {
        id: 'loaded',
        on: {
          CHANGE_PASSWORD: 'changingPassword',
          RESET_TOKEN: 'resetToken',
        }
      },
      changingPassword: {
        initial: 'confirmAdminPassword',

        states: {
          confirmAdminPassword: {
            invoke: {
              id: 'confirmAdminPasswordMachine',
              src: 'confirmAdminPasswordMachine',
              onDone: 'confirmNewPassword',
            }
          },
          confirmNewPassword: {
            invoke: {
              id: 'confirmNewPasswordMachine',
              src: 'confirmNewPasswordMachine',
              data: ({data: {userId}}) => ({
                userId: userId,
                data: {
                  passwordNew: '',
                  passwordNewConfirmed: ''
                },
                pristineData: {
                  passwordNew: '',
                  passwordNewConfirmed: ''
                }
              }),
              onDone: '#loaded'
            }
          }
        },

        on: {
          CANCEL: '#loaded'
        }
      },
      resetToken: {
        initial: 'confirmAdminPassword',

        states: {
          confirmAdminPassword: {
            invoke: {
              id: 'confirmAdminPasswordMachine',
              src: 'confirmAdminPasswordMachine',
              onDone: 'resetting'
            }
          },
          resetting: {
            invoke: {
              id: 'resettingTokenMachine',
              src: 'resettingTokenMachine',
              data: ({data}) => ({
                data: {
                  userId: data.userId,
                  source: data.source,
                  name: data.firstName
                }
              }),
              onDone: '#loaded'
            }
          }
        },

        on: {
          CANCEL: '#loaded'
        }
      }
    }
  })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({pristineData, data}) => {
        const isNexusUser = !isExternalUser(data.source);
        const isCreate = !isEdit(pristineData.userId);
        return {
          userId: ValidationUtils.validateNotBlank(data.userId),
          firstName: isNexusUser ? ValidationUtils.validateNotBlank(data.firstName) : null,
          lastName: isNexusUser ? ValidationUtils.validateNotBlank(data.lastName) : null,
          emailAddress: isNexusUser ? ValidationUtils.validateNotBlank(data.emailAddress) || ValidationUtils.validateEmail(data.emailAddress) : null,
          password: isCreate ? ValidationUtils.validateNotBlank(data.password): null,
          passwordConfirm: isCreate ? validatePasswordConfirm(data.password, data.passwordConfirm) : null,
          roles: !isNexusUser || data.roles?.length ? null : UIStrings.ERROR.FIELD_REQUIRED,
        }
      }
    }),
    setData: assign(({pristineData: {userId, source}}, {data: [roles, users]}) => {
      let user = users?.data?.find(it => it.userId === userId && it.source === source);

      if (!user) {
        ExtJS.showErrorMessage(UIStrings.ERROR.NOT_FOUND_ERROR(userId));
        user = EMPTY_DATA;
      }

      user = {
        ...user,
        status: user.status === STATUSES.active.id,
      };

      return {
        allRoles: roles?.data,
        data: user,
        pristineData: user,
      };
    }),
    onDeleteError: (_, event) => ExtJS.showErrorMessage(event.data?.response?.data),
    logDeleteSuccess: ({data}) => ExtJS.showSuccessMessage(LABELS.DELETE_SUCCESS(fullName(data))),
  },
  services: {
    fetchData: ({pristineData: {userId, source}}) => {
      return Axios.all([
        Axios.get(defaultRolesUrl),
        isEdit(userId)
            ? Axios.get(findUsersUrl(userId, source))
            : Promise.resolve({data: [EMPTY_DATA]}),
      ]);
    },
    saveData: async ({data, pristineData: {userId}, pristineData}) => {
      const modifiedData = {
        ...data,
        status: data.status ? STATUSES.active.id : STATUSES.disabled.id,
      };

      if (isEdit(userId)) {
        if (isExternalUser(pristineData.source)) {
          const response = await ExtAPIUtils.extAPIRequest(ACTION, METHODS.UPDATE_ROLE_MAPPINGS, {
            data: [{
              realm: pristineData.source,
              userId: pristineData.userId,
              roles: data.roles,
            }],
          });
          return ExtAPIUtils.checkForError(response) || response;
        } else {
          return Axios.put(singleUserUrl(userId), modifiedData);
        }
      } else {
        return Axios.post(createUserUrl, modifiedData);
      }
    },
    confirmDelete: ({data}) => ExtJS.requestConfirmation({
      title: LABELS.CONFIRM_DELETE.TITLE,
      message: LABELS.CONFIRM_DELETE.MESSAGE(fullName(data)),
      yesButtonText: LABELS.CONFIRM_DELETE.YES,
      noButtonText: LABELS.CONFIRM_DELETE.NO
    }),
    delete: ({data}) => Axios.delete(singleUserUrl(data.userId)),

    confirmAdminPasswordMachine,
    confirmNewPasswordMachine,
    resettingTokenMachine
  }
});
