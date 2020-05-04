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
import Axios from 'axios';
import {ExtJS, Utils} from 'nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const initialContext = {
  data: {
    userId: '',
    firstName: '',
    lastName: '',
    email: '',
    external: true,
  },
  pristineData: {},
  isPristine: true,
  isEdited: false,
  isValid: true,
  error: null
};

const userAccountMachine = Machine({
  id: 'userAccount',
  initial: 'loading',
  context: initialContext,
  states: {
    loading: {
      invoke: {
        id: 'fetchData',
        src: 'fetchData',
        onDone: {
          target: 'fetched',
          actions: ['setData', 'checkEdited', 'validate'],
        },
        onError: {
          target: 'error',
          actions: ['logLoadError'],
        },
      },
    },
    fetched: {
      on: {
        'UPDATE': {
          actions: ['update', 'checkEdited', 'validate']
        },
        'DISCARD': {
          actions: ['discard', 'checkEdited', 'validate', 'clearError']
        },
        'SAVE': {
          target: 'saving'
        },
      },
    },
    saving: {
      invoke: {
        id: 'saveData',
        src: 'saveData',
        onDone: {
          target: 'loading',
          actions: ['logSaveSuccess', 'clearError']
        },
        onError: {
          target: 'fetched',
          actions: ['setError']
        },
      },
    },
    error: {
      type: 'final',
    },
  },
}, {
  actions: {
    setData: assign({
      data: (_, {data}) => data.data,
      pristineData: (_, {data}) => data.data,
    }),
    update: assign({
      data: (_, {data}) => data
    }),
    checkEdited: assign(({data, pristineData}) => {
      const isPristine = Object.keys(data).every((key) => data[key] === pristineData[key]);
      return {
        isPristine,
        isEdited: !isPristine
      };
    }),
    validate: assign((ctx, _) => {
      const {firstName, lastName, email} = ctx.data;
      const isValid = (Utils.notBlank(firstName) && Utils.notBlank(lastName) && Utils.notBlank(email));
      return {isValid};
    }),
    discard: assign({
      data: ({pristineData}) => pristineData,
    }),
    logLoadError: ({error}) => {
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(UIStrings.USER_ACCOUNT.MESSAGES.LOAD_ERROR);
    },
    logSaveSuccess: () => {
      ExtJS.showSuccessMessage(UIStrings.USER_ACCOUNT.MESSAGES.UPDATE_SUCCESS);
    },
    setError: assign({
      error: (_, evt) => evt.data.response.data
    }),
    clearError: assign({
      error: () => undefined
    }),
  },
  services: {
    fetchData: () => Axios.get('/service/rest/internal/ui/user'),
    saveData: ({data}) => {
      return Axios.put('/service/rest/internal/ui/user', data);
    },
  },
});

export default userAccountMachine;
