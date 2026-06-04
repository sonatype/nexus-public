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
import {createFormMachine, ExtJS, parseRetryAfter, UIStrings} from '@sonatype/nexus-ui-plugin';

const {ERRORS} = UIStrings;

function encodeCredential(str) {
  const bytes = new TextEncoder().encode(str);
  let binary = '';
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}

export function createLoginFormMachine() {
  return createFormMachine({
    id: 'login-form',
    context: {
      data: {username: '', password: ''},
      isSsoRedirecting: false,
      rateLimitWarning: false,
      retryAfterSeconds: null,
    },
    actions: {
      validate: assign((ctx) => {
        const errors = {
          username: !ctx.data.username?.trim() ? ERRORS.USERNAME_REQUIRED : null,
          password: !ctx.data.password ? ERRORS.PASSWORD_REQUIRED : null,
        };
        return { validationErrors: errors };
      }),
      ssoRedirect: assign((_ctx, event) => {
        window.location.assign(event.url);
        return {isSsoRedirecting: true};
      }),
      setSaveError: assign((_, event) => {
        const error = event.data;
        if (error?.response?.status === 429) {
          return {rateLimitWarning: true, retryAfterSeconds: error.response.retryAfter ?? null, saveError: null};
        }
        return {rateLimitWarning: false, retryAfterSeconds: null, saveError: ERRORS.WRONG_CREDENTIALS};
      }),
      clearSaveError: assign({saveError: null, rateLimitWarning: false, retryAfterSeconds: null}),
    },
    on: {
      SSO_LOGIN: {actions: 'ssoRedirect'},
    },
    services: {
      save: async (ctx) => {
        const contextPath = ExtJS.state().getValue('nexus-context-path', '');
        const contextPrefix = contextPath === '/' ? '' : contextPath;
        const url = `${contextPrefix}/service/rapture/session`;
        const body = `username=${encodeURIComponent(encodeCredential(ctx.data.username))}&password=${encodeURIComponent(encodeCredential(ctx.data.password))}`;
        const response = await fetch(url, {
          method: 'POST',
          headers: {'Content-Type': 'application/x-www-form-urlencoded'},
          body,
        });
        if (response.ok) {
          window.NX?.State?.setUser?.({id: ctx.data.username});
          window.location.hash = '#browse/welcome';
          return {username: ctx.data.username};
        }
        if (response.status === 429) {
          const retryAfter = parseRetryAfter(response.headers.get('Retry-After'));
          throw Object.assign(new Error('Rate limit exceeded'), {response: {status: 429, retryAfter}});
        }
        const data = await response.text().catch(() => '');
        throw Object.assign(new Error('Authentication failed'), {response: {status: response.status, data}});
      },
    },
  });
}
