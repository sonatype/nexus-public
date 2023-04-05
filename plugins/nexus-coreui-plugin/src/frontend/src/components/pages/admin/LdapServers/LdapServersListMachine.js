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
import Axios from 'axios';
import {assign} from 'xstate';
import {mergeDeepRight, lensProp, set, update, move} from 'ramda';

import {
  ListMachineUtils,
  APIConstants,
  ExtAPIUtils,
  ExtJS,
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

import {URL} from './LdapServersHelper';

const {EXT, ERROR} = APIConstants;

const {
  LDAP_SERVERS: {LIST: LABELS},
} = UIStrings;

export default ListMachineUtils.buildListMachine({
  id: 'LdapServersListMachine',
  sortableFields: ['order', 'name', 'url'],
  sortField: 'order',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        loaded: {
          on: {
            REORDER: {
              target: 'loaded',
              actions: 'reorderLdapList',
            },
            FILTER_ORDER_LIST: {
              target: 'loaded',
              actions: 'setTransferListFilter',
            },
            TOGGLE_ORDER_MODAL: {
              target: 'loaded',
              actions: 'toggleModal',
            },
            SAVE_ORDER: {
              target: 'saving',
            },
            CLEAR_CACHE: {
              target: 'clearing',
            },
          },
        },
        saving: {
          invoke: {
            src: 'saveData',
            onDone: {
              target: 'loading',
              actions: ['toggleModal', 'onSaveSuccess'],
            },
            onError: {
              target: 'loaded',
              actions: 'onSaveError',
            },
          },
        },
        clearing: {
          invoke: {
            src: 'clearCache',
            onDone: {
              target: 'loaded',
              actions: 'onClearSuccess',
            },
            onError: {
              target: 'loaded',
              actions: 'onSaveError',
            },
          },
        },
      },
    }),
}).withConfig({
  actions: {
    setData: assign((_, {data: ldapServers}) => {
      const data = ldapServers?.data?.map((server) => {
        server.url = `${server.protocol}://${server.host}:${server.port}/${server.searchBase}`;
        return server;
      });

      return {
        data: data,
        pristineData: data,
        transferListData: data,
      };
    }),
    filterData: assign({
      data: ({filter, pristineData}, _) =>
        pristineData.filter((item) =>
          ListMachineUtils.hasAnyMatches(
            [item.order, item.name, item.url],
            filter
          )
        ),
    }),
    reorderLdapList: assign(({transferListData, ...rest}, {direction, id}) => {
      let items = [...transferListData];
      const index = items.findIndex(({order: itemId}) => itemId === id);
      const destIndex = index + direction;

      const itemAtIndex = items[index];
      const itemAtDest = items[destIndex];

      const orderItemAtIndex = itemAtIndex.order;
      const orderItemAtDest = items[destIndex].order;

      // Updates order property for each item
      const orderLens = lensProp('order');
      const itemUpdated = set(orderLens, orderItemAtDest, itemAtIndex);
      const itemDestUpdated = set(orderLens, orderItemAtIndex, itemAtDest);

      // inserts the updated items
      items = update(index, itemUpdated, items);
      items = update(destIndex, itemDestUpdated, items);

      return {
        ...rest,
        transferListData: move(index, destIndex, items),
      };
    }),
    setTransferListFilter: assign({
      filterTransferList: (_, {value}) => value,
    }),
    toggleModal: assign({
      modal: (_, {value}) => value,
    }),
    onSaveError: assign(() => {
      ExtJS.showErrorMessage(ERROR.SAVE_ERROR);
    }),
    onSaveSuccess: assign(() => {
      ExtJS.showSuccessMessage(LABELS.MESSAGES.LIST_CHANGED);
    }),
    onClearSuccess: assign((_, {data}) => {
      const success = data.data.result.success;

      if (success) {
        ExtJS.showSuccessMessage(LABELS.MESSAGES.CACHE_CLEARED);
      }
    }),
  },
  services: {
    fetchData: () => Axios.get(URL.ldapServersUrl),
    saveData: async ({transferListData}) => {
      const list = transferListData.map((item) => item.name);
      return Axios.post(URL.changeLdapServersOrderUrl, list);
    },
    clearCache: async () => {
      const response = await ExtAPIUtils.extAPIRequest(
        EXT.LDAP.ACTION,
        EXT.LDAP.METHODS.CLEAR_CACHE
      );

      ExtAPIUtils.checkForError(response);

      return response;
    },
  },
});
