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
import {assign, createMachine} from 'xstate';
import Axios from 'axios';
import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {RECOVERY_MODE} = UIStrings;

const {
  RECOVERY_MODE: RECOVERY_MODE_PUBLIC_API
} = APIConstants.REST.PUBLIC;

const {
  RECOVERY_MODE: RECOVERY_MODE_UI_API
} = APIConstants.REST.INTERNAL;

export default createMachine(
{
  id: 'RecoveryMode',
  initial: 'loading',

  context: {
    data: {},
    error: null
  },

  states: {
    loading: {
      invoke: {
        src: 'fetchData',
        onDone: {
          target: 'loaded',
          actions: ['setData']
        },
        onError: {
          target: 'loadError',
          actions: ['logLoadError']
        }
      }
    },
    loaded: {
      on: {
        ENABLE: {
          target: 'setEnabled',
          actions: ['enableRecoveryMode']
        },
        DISABLE: [
          {
            target: 'confirmingDisable',
            cond: 'hasUnexecutedPlans'
          },
          {
            target: 'setDisabled',
            actions: ['disableRecoveryMode']
          }
        ]
      }
    },
    confirmingDisable: {
      on: {
        CONFIRM: {
          target: 'setDisabled',
          actions: ['disableRecoveryMode']
        },
        CANCEL: {
          target: 'loaded'
        }
      }
    }  ,
    setEnabled: {
      invoke: {
        src: 'enableRecoveryMode',
        onDone: {
          target: 'loaded',
          actions: ['logSaveSuccess']
        },
        onError: {
          target: 'loaded',
          actions: ['logSaveError']
        }
      }
    },
    setDisabled: {
      invoke: {
        src: 'disableRecoveryMode',
        onDone: {
          target: 'loaded',
          actions: ['logSaveSuccess']
        },
        onError: {
          target: 'loaded',
          actions: ['logSaveError']
        }
      }
    },
    loadError: {
      on: {
        RETRY: {
          target: 'loading'
        }
      }
    }
  },

},
{
  actions: {
    setData: assign({
      data: (_, {data}) => data.data,
      pristineData: (_, {data}) => data.data
    }),
    enableRecoveryMode: assign({
      data: (context) => ({
        ...context.data,
        enabled: true
      })
    }),
    disableRecoveryMode: assign({
      data: (context) => ({
        ...context.data,
        enabled: false
      })
    }),
    logError: (_, event) => console.error('Failed to load recovery mode', event),

    logLoadError: (_, {error}) => {
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(RECOVERY_MODE.MESSAGES.LOAD_ERROR);
    },

    logSaveError: (_, {error}) => {
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(RECOVERY_MODE.MESSAGES.SAVE_ERROR);
    },

    logSaveSuccess: (context) => {
      ExtJS.showSuccessMessage(RECOVERY_MODE.MESSAGES.SAVE_SUCCESS);
      ExtJS.state().setValue('recovery.mode.enabled', context?.data?.enabled);
    },
  },

  guards: {
    hasUnexecutedPlans: (context) => {
      return context?.data?.unexecutedPlans;
    }
  },

  services: {
    fetchData: () => Axios.get(RECOVERY_MODE_UI_API),
    enableRecoveryMode: () => Axios.post(RECOVERY_MODE_PUBLIC_API),
    disableRecoveryMode: () => Axios.delete(RECOVERY_MODE_PUBLIC_API)
  }
});
