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
import {assign, createMachine} from "xstate";

export default createMachine({
  id: 'CEHardLimitAlertsMachine',
  initial: 'loaded',

  states: {
    loaded: {
      on: {
        'DISMISS': {
          target: 'loaded',
          actions: 'handleDismiss'
        }
      }
    },
    loadError: {
      on: {
        'RETRY': {
          target: 'loaded',
          actions: 'clearError'
        }
      }
    }
  },
},
{
  actions: {
    setError: assign({
      loadError: (_, event) => event.data.message
    }),

    clearError: assign({
      loadError: () => null
    }),

    handleDismiss: (context, event) => {
      const {banner} = event;
      if (banner === 'under_end_grace') {
        const expires = new Date();
        expires.setMonth(expires.getMonth() + 6);
        document.cookie = `${banner}=dismissed; expires=${expires.toUTCString()}; path=/`;
      }
      context.dismissedBanners = context.dismissedBanners || [];
      context.dismissedBanners.push(banner);
    }
  }
});
