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
import { assign, Machine } from 'xstate';
import {ExtJS, Utils} from 'nexus-ui-plugin';
import Axios from "axios";

const initialContext = {
  passwordCurrent: '',
  passwordNew: '',
  passwordNewConfirm: '',
  isEmpty: true,
  isCurrentNotEqualNew: true,
  isConfirmed: true,
};

const passwordChangeMachine = Machine({
  id: 'passwordChange',
  initial: 'empty',
  context: initialContext,
  states: {
    empty: {
      entry: assign(initialContext),
      on: {
        '': 'validating',
      },
    },
    validating: {
      id: 'validating',
      on: {
        '': [
          {
            cond: 'isValid',
            target: 'valid',
          },
          {
            target: 'invalid',
          },
        ]
      }
    },
    invalid: {
    },
    valid: {
      on: {
        SUBMIT: 'submitting',
      },
    },
    submitting: {
      invoke: {
        id: 'submitting',
        src: 'changePassword',
        onDone: 'empty',
        onError: 'validating',
      },
    },
  },
  on: {
    INPUT: {
      actions: ['capture', 'constraintCheck'],
      target: 'validating',
    },
    DISCARD: 'empty',
  }
}, {
  actions: {
    capture: assign((_, evt) => {
      return { [evt.name]: evt.value };
    }),
    constraintCheck: assign((ctx, _) => {
      const { passwordCurrent, passwordNew, passwordNewConfirm } = ctx;
      const isCurrentNotEqualNew = (passwordCurrent !== passwordNew);
      const isConfirmed = (passwordNew === passwordNewConfirm);
      const isEmpty = Utils.isBlank(passwordCurrent) && Utils.isBlank(passwordNew) && Utils.isBlank(passwordNewConfirm);
      return { isCurrentNotEqualNew, isConfirmed, isEmpty };
    }),
  },
  guards: {
    isValid: (ctx, _) => {
      const { passwordCurrent, passwordNew, passwordNewConfirm, isCurrentNotEqualNew,  isConfirmed} = ctx;
      return (
        Utils.notBlank(passwordCurrent) &&
        Utils.notBlank(passwordNew) &&
        Utils.notBlank(passwordNewConfirm) &&
        isCurrentNotEqualNew &&
        isConfirmed
      );
    },
  },
  services: {
    changePassword: (ctx, evt) =>
        ExtJS.fetchAuthenticationToken(evt.userId, ctx.passwordCurrent)
        .then((result) => {
          Axios.put(`/service/rest/internal/ui/user/${evt.userId}/password`,
              {authToken: result.data, password: ctx.passwordNew})
              .then(() => {
                ExtJS.showSuccessMessage('Password changed');
              })
              .catch((error) => {
                if (error.response?.status === 400 && Array.isArray(error.response.data)) {
                  const message = error.response.data.map(e => e.message).join('\\n');
                  ExtJS.showErrorMessage(message);
                }
                else {
                  ExtJS.showErrorMessage('Change password failed');
                  console.error(error);
                }
              });
        }),
  },
});

export default passwordChangeMachine;
