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

import {INITIAL_DATA, URLs, canDeleteTask, canRunTask, canStopTask} from './TasksHelper';

import UIStrings from '../../../../constants/UIStrings';

const {EXT: {TASK: {ACTION, METHODS}}} = APIConstants;
const {TASKS: {MESSAGES: LABELS}} = UIStrings;
const {runTaskUrl, stopTaskUrl} = URLs;

const isEdit = (id) => ValidationUtils.notBlank(id);

// TODO Remove when NEXUS-37051 and NEXUS-37116 are done.
const prepareData = (task) => {
  Object.keys(task.properties).forEach(key => {
    if (key.includes('.')) delete task.properties[key];
  });
  return {
    ...task,
    timeZoneOffset: ExtJS.formatDate(new Date(), 'P'),
  };
};

export default FormUtils.buildFormMachine({
  id: 'TasksFormMachine',
  config: (config) =>
      mergeDeepRight(config, {
        states: {
          loaded: {
            on: {
              CONFIRM_RUN: {
                target: 'confirmRun',
                cond: 'canRun'
              },
              CONFIRM_STOP: {
                target: 'confirmStop',
                cond: 'canStop'
              },
            }
          },
          confirmRun: {
            invoke: {
              src: 'confirmRun',
              onDone: 'run',
              onError: 'loaded'
            }
          },
          run: {
            invoke: {
              src: 'run',
              onDone: {
                target: 'loaded',
                actions: ['logRunSuccess', 'toggleRunnableStoppable']
              },
              onError: {
                target: 'loaded',
                actions: 'onRunError'
              }
            }
          },
          confirmStop: {
            invoke: {
              src: 'confirmStop',
              onDone: 'stop',
              onError: 'loaded'
            }
          },
          stop: {
            invoke: {
              src: 'stop',
              onDone: {
                target: 'loaded',
                actions: ['logStopSuccess', 'toggleRunnableStoppable']
              },
              onError: {
                target: 'loaded',
                actions: 'onStopError'
              }
            }
          },
        },
      })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({pristineData, data}) => {
        const isCreate = !isEdit(pristineData.id);
        return {
          typeId: isCreate ? ValidationUtils.validateNotBlank(data.typeId): null,
          name: ValidationUtils.validateNotBlank(data.name),
          alertEmail: data.alertEmail ? ValidationUtils.validateEmail(data.alertEmail) : null,
        };
      },
    }),
    setData: assign(({pristineData: {id}}, {data: [tasks, types]}) => {
      let task = INITIAL_DATA;
      if (isEdit(id)) {
        task = tasks?.find(it => it.id === id);
        if (!task) {
          ExtJS.showErrorMessage(UIStrings.ERROR.NOT_FOUND_ERROR(id));
        }
      }
      return {
        runnable: task.runnable,
        stoppable: task.stoppable,
        data: task,
        pristineData: task,
        types: types.filter(type => type.exposed),
      };
    }),
    onDeleteError: (_, event) => ExtJS.showErrorMessage(event.data?.response?.data || event.data?.message),
    onRunError: (_, event) => ExtJS.showErrorMessage(event.data?.message),
    onStopError: (_, event) => ExtJS.showErrorMessage(event.data?.message),
    logDeleteSuccess: ({data}) => ExtJS.showSuccessMessage(LABELS.DELETE_SUCCESS(`${data.name} (${data.typeName})`)),
    logRunSuccess: ({data}) => ExtJS.showSuccessMessage(LABELS.RUN_SUCCESS(`${data.name} (${data.typeName})`)),
    logStopSuccess: ({data}) => ExtJS.showSuccessMessage(LABELS.STOP_SUCCESS(`${data.name} (${data.typeName})`)),
    toggleRunnableStoppable: assign(({runnable, stoppable}) => ({
      runnable: !runnable,
      stoppable: !stoppable,
    })),
  },
  guards: {
    canDelete: () => canDeleteTask(),
    canRun: ({runnable}) => runnable && canRunTask(),
    canStop: ({stoppable}) => stoppable && canStopTask(),
  },
  services: {
    fetchData: ({pristineData: {id}}) => {
      return Axios.all([
        isEdit(id)
            ? ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ).then(ExtAPIUtils.checkForErrorAndExtract)
            : Promise.resolve(INITIAL_DATA),
        ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ_TYPES).then(ExtAPIUtils.checkForErrorAndExtract)
      ]);
    },
    saveData: ({data, pristineData: {id}}) => {
      const task = prepareData(data);
      const method = isEdit(id) ? METHODS.UPDATE : METHODS.CREATE

      return ExtAPIUtils.extAPIRequest(ACTION, method, {data: [task]}).then(ExtAPIUtils.checkForError);
    },
    confirmDelete: ({data}) => ExtJS.requestConfirmation({
      title: LABELS.CONFIRM_DELETE.TITLE,
      message: LABELS.CONFIRM_DELETE.MESSAGE(data.name),
      yesButtonText: LABELS.CONFIRM_DELETE.YES,
      noButtonText: UIStrings.SETTINGS.CANCEL_BUTTON_LABEL,
    }),
    confirmRun: ({data}) => ExtJS.requestConfirmation({
      title: LABELS.CONFIRM_RUN.TITLE,
      message: LABELS.CONFIRM_RUN.MESSAGE(data.name),
      yesButtonText: LABELS.CONFIRM_RUN.YES,
      noButtonText: UIStrings.SETTINGS.CANCEL_BUTTON_LABEL,
    }),
    confirmStop: ({data}) => ExtJS.requestConfirmation({
      title: LABELS.CONFIRM_STOP.TITLE,
      message: LABELS.CONFIRM_STOP.MESSAGE(data.name),
      yesButtonText: LABELS.CONFIRM_STOP.YES,
      noButtonText: UIStrings.SETTINGS.CANCEL_BUTTON_LABEL,
    }),

    delete: ({data}) => ExtAPIUtils.extAPIRequest(ACTION, METHODS.DELETE, {data: [data.id]}).then(ExtAPIUtils.checkForError),
    run: ({data}) => Axios.post(runTaskUrl(data.id)),
    stop: ({data}) => Axios.post(stopTaskUrl(data.id)),
  }
});
