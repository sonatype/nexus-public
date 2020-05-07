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
import Axios from "axios";

import {ExtJS, Utils} from "nexus-ui-plugin";

import UIStrings from "../../../../constants/UIStrings";

const loggerUrl = (name) => `/service/rest/internal/ui/loggingConfiguration/${name}`;

export default Machine(
    {
      initial: 'loading',

      context: {
        data: {},
        pristineData: {},
        isPristine: true
      },

      states: {
        loading: {
          invoke: {
            src: 'fetch',
            onDone: {
              target: 'loaded',
              actions: ['setData']
            },
            onError: {
              target: 'error',
              actions: ['setLoadError']
            }
          }
        },
        loaded: {
          entry: ['validate', 'setDirtyFlag'],
          on: {
            UPDATE: {
              target: 'loaded',
              actions: ['update']
            },
            SAVE: [
              {
                target: 'confirmSave',
                cond: 'isNew'
              },
              {
                target: 'saving',
                cond: 'isEdit'
              }
            ]
          },
          exit: ['clearSaveError']
        },
        confirmReset: {
          invoke: {
            src: 'confirmReset',
            onDone: 'resetting',
            onError: 'loaded'
          }
        },
        confirmSave: {
          invoke: {
            src: 'confirmSave',
            onDone: 'saving',
            onError: 'loaded'
          }
        },
        resetting: {
          invoke: {
            src: 'reset',
            onDone: {
              actions: ['onDone', 'clearDirtyFlag']
            },
            onError: {
              target: 'loaded',
              actions: ['setSaveError']
            }
          }
        },
        saving: {
          invoke: {
            src: 'save',
            onDone: {
              actions: ['onDone', 'clearDirtyFlag']
            },
            onError: {
              target: 'loaded',
              actions: ['setSaveError']
            }
          }
        },
        error: {}
      },
      on: {
        CANCEL: {
          actions: ['onDone']
        },

        RESET: {
          target: 'confirmReset',
          cond: 'isEdit'
        }
      }
    },
    {
      actions: {
        setData: assign({
          data: (_, event) => event.data.data,
          pristineData: (_, event) => event.data.data,
          isPristine: () => true
        }),
        validate: assign({
          isValid: ({data: {name}}) => Utils.notBlank(name),
          isInvalid: ({data: {name}}) => Utils.isBlank(name),
        }),
        update: assign({
          data: ({data}, event) => ({...data, ...event.data}),
          isPristine: ({pristineData}, {data}) => Object.keys(pristineData).every(key => pristineData[key] === data[key])
        }),
        onDone: () => {},
        setSaveError: assign({
          saveError: (_, event) => event.data.response.data
        }),
        clearSaveError: assign({
          saveError: () => undefined
        }),
        setLoadError: assign({
          loadError: (_, event) => event.data.message
        }),
        setDirtyFlag: ({isPristine}) => {
          ExtJS.setDirtyStatus('EditLoggingConfiguration', !isPristine)
        },
        clearDirtyFlag: () => ExtJS.setDirtyStatus('EditLoggingConfiguration', false)
      },
      guards: {
        isNew: ({pristineData}) => Utils.isBlank(pristineData.name),
        isEdit: ({pristineData}) => Utils.notBlank(pristineData.name)
      },
      services: {
        fetch: ({pristineData}) => {
          if (Utils.notBlank(pristineData.name)) { // Edit logging configuration
            return Axios.get(loggerUrl(pristineData.name));
          }
          else { // New Logging Configuration
            return Promise.resolve({
              data: {
                name: '',
                level: 'INFO'
              }
            });
          }
        },
        confirmReset: () => ExtJS.requestConfirmation({
          title: UIStrings.LOGGING.CONFIRM_RESET.TITLE,
          message: UIStrings.LOGGING.CONFIRM_RESET.MESSAGE,
          yesButtonText: UIStrings.LOGGING.CONFIRM_RESET.CONFIRM_BUTTON,
          noButtonText: UIStrings.SETTINGS.CANCEL_BUTTON_LABEL
        }),
        confirmSave: async ({data}) => {
          const response = await Axios.get(loggerUrl(data.name));
          if (response.data.override) {
            return await ExtJS.requestConfirmation({
              title: UIStrings.LOGGING.CONFIRM_UPDATE.TITLE,
              message: UIStrings.LOGGING.CONFIRM_UPDATE.MESSAGE({name: data.name, level: data.level}),
              yesButtonText: UIStrings.LOGGING.CONFIRM_UPDATE.CONFIRM_BUTTON,
              noButtonText: UIStrings.SETTINGS.CANCEL_BUTTON_LABEL
            });
          }
          return Promise.resolve();
        },
        reset: ({data}) => Axios.post(`${loggerUrl(data.name)}/reset`),
        save: ({data}) => Axios.put(loggerUrl(data.name), data)
      }
    }
);
