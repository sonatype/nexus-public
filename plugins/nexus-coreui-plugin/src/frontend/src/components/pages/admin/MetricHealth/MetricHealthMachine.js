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
import {toPairs, map, mergeRight, sortBy, compose, toLower, prop} from 'ramda';
import {ListMachineUtils, APIConstants} from '@sonatype/nexus-ui-plugin';
import {assign} from 'xstate';
import {URL, isClustered} from './MetricHealthHelper';

export const convert = (result) => {
  const list = map(
    ([name, metrics]) => mergeRight({name}, metrics),
    toPairs(result)
  );
  const sortByNameCaseInsensitive = sortBy(compose(toLower, prop('name')));

  return sortByNameCaseInsensitive(list);
};

const setData = (_, event) => {
  const result = isClustered() ? event.data.data.results : event.data.data;
  return convert(result);
};

export default ListMachineUtils.buildListMachine({
  id: 'MetricHealthMachineDetails',
  sortableFields: ['name', 'message', 'error'],
}).withConfig({
  actions: {
    setData: assign({
      data: setData,
      pristineData: (_, event) => event.data.data,
      name: (_, event) => event.data.data.hostname,
    }),
  },
  services: {
    fetchData: ({itemId}) => {
      return isClustered()
        ? Axios.get(URL.singleNodeUrl(itemId))
        : Axios.get(APIConstants.REST.INTERNAL.GET_STATUS);
    },
  },
});
