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
import {createFormMachine, ExtJS, UIStrings} from '@sonatype/nexus-ui-plugin';

const {ERRORS} = UIStrings;

export function createLoginFormMachine() {
  return createFormMachine({
    id: 'login-form',
    context: {
      data: {username: '', password: ''},
      isSsoRedirecting: false,
    },
    actions: {
      validate: assign((ctx) => ({
        validationErrors: {
          username: !ctx.data.username?.trim() ? ERRORS.USERNAME_REQUIRED : null,
          password: !ctx.data.password ? ERRORS.PASSWORD_REQUIRED : null,
        },
      })),
      ssoRedirect: assign((_ctx, event) => {
        window.location.assign(event.url);
        return {isSsoRedirecting: true};
      }),
    },
    on: {
      SSO_LOGIN: {
        actions: 'ssoRedirect',
      },
    },
    services: {
      save: async (ctx) => {
        try {
          const result = await ExtJS.requestSession(ctx.data.username, ctx.data.password);
          if (result.response.status >= 200 && result.response.status < 300) {
            window.location.hash = '#browse/welcome';
          } else {
            throw new Error(ERRORS.WRONG_CREDENTIALS);
          }
        } catch (err) {
          throw err.message === ERRORS.WRONG_CREDENTIALS
            ? err
            : new Error(ERRORS.WRONG_CREDENTIALS);
        }
      },
    },
  });
}
