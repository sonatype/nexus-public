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
import React from 'react';
import {assign} from 'xstate';
import Axios from 'axios';
import {FormUtils} from '@sonatype/nexus-ui-plugin';

export default FormUtils.buildFormMachine({
  id: 'ProprietaryRepositoriesMachine',
  initial: 'loadingPossibleRepos',
  config: (config) => ({
    ...config,
    context: {
      ...config.context,
      data: {
        possibleRepos: [],
        enabledRepositories: [],
      },
      pristineData: {
        possibleRepos: [],
        enabledRepositories: [],
      }
    },
    states: {
      ...config.states,
      loadingPossibleRepos: {
        invoke: {
          id: 'fetchPossibleRepos',
          src: 'fetchPossibleRepos',
          onDone: {
            target: 'loading',
            actions: ['setPossibleRepos']
          },
          onError: {
            target: 'loadError',
            actions: ['setLoadError', 'logLoadError']
          }
        }
      },
    },
    on: {
      'RETRY': {
        target: 'loading'
      }
    }
  }),
}).withConfig({
  actions: {
    setPossibleRepos: assign({
      possibleRepos: (_, event) => {
        return event.data?.data?.result?.data;
      }
    }),
    validate: assign({
      validationErrors: () => {{}}
    }),
  },
  services: {
    fetchData: () => Axios.post('/service/extdirect', {"action":"coreui_ProprietaryRepositories","method":"read","data":null,"type":"rpc","tid":1}).then(v => v.data.result),
    fetchPossibleRepos: () => Axios.post('/service/extdirect',{"action":"coreui_ProprietaryRepositories","method":"readPossibleRepos","data":null,"type":"rpc","tid":1}),
    saveData: ({data}) => {
      return Axios.post('/service/extdirect', {
        "action": "coreui_ProprietaryRepositories",
        "method": "update",
        "data": [{"enabledRepositories": data.enabledRepositories}],
        "type": "rpc",
        "tid": 1
      }).then(res => res.data.result)
    },
  }
});
