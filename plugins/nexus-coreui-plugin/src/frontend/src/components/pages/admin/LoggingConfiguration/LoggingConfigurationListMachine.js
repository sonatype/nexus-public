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

import {ExtJS, ListMachineUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

export default ListMachineUtils.buildListMachine({
  id: 'LoggingConfigurationListMachine',
  sortableFields: ['name', 'level'],
  config: (config) => ({
    ...config,
    states: {
      ...config.states,
      loading: {
        ...config.states.loading,
        states: {
          ...config.states.loading.states,
          resetting: {
            invoke: {
              src: 'reset',
              onDone: 'fetch',
              onError: {
                target: 'fetch',
                actions: ['showResetError']
              }
            }
          }
        }
      },
      loaded: {
        ...config.states.loaded,
        on: {
          ...config.states.loaded.on,
          RESET: 'confirmReset'
        }
      },
      confirmReset: {
        invoke: {
          src: 'confirmReset',
          onDone: '#loading.resetting',
          onError: 'loaded'
        }
      }
    }
  })
}).withConfig({
  actions: {
    filterData: assign({
      data: ({filter, data, pristineData}, _) => pristineData.filter(
          ({name}) => name.toLowerCase().indexOf(filter.toLowerCase()) !== -1
      )
    }),
    sortData: assign({
      data: ListMachineUtils.sortDataByFieldAndDirection({useLowerCaseSorting: false})
    }),
    showResetError: (_, {data}) => {
      console.error(data.response);
      ExtJS.showErrorMessage(UIStrings.LOGGING.MESSAGES.RESET_ERROR)
    }
  },
  services: {
    fetchData: () => Axios.get('service/rest/internal/ui/loggingConfiguration'),
    confirmReset: () => ExtJS.requestConfirmation({
      title: UIStrings.LOGGING.CONFIRM_RESET_ALL.TITLE,
      message: UIStrings.LOGGING.CONFIRM_RESET_ALL.MESSAGE,
      yesButtonText: UIStrings.LOGGING.CONFIRM_RESET_ALL.CONFIRM_BUTTON,
      noButtonText: UIStrings.SETTINGS.CANCEL_BUTTON_LABEL
    }),
    reset: () => Axios.post('service/rest/internal/ui/loggingConfiguration/reset')
  }
});
