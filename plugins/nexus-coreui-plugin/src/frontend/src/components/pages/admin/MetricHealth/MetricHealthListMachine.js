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
import {toPairs, map, filter} from 'ramda';
import {assign} from 'xstate';
import {ListMachineUtils} from '@sonatype/nexus-ui-plugin';
import {URL} from './MetricHealthHelper';

export const convert = (data) => {
  return map(({hostname, nodeId, results}) => {
    const errors = filter(([_, value]) => !value.healthy, toPairs(results));
    const errorsNumber = errors.length;
    const oneError = errorsNumber === 1;
    const multipleErrors = errorsNumber > 1;

    const name = hostname;
    const status = '';
    let error = '';
    let message = '';

    if (oneError) {
      const [key, value] = errors[0];
      error = key;
      message = value.message;
    }

    if (multipleErrors) {
      error = `(${errorsNumber}) Errors found`;
      message = 'Click to see details';
    }

    return {name, status, error, message, nodeId};
  }, data);
};

const setData = (_, event) => convert(event.data.data);

export default ListMachineUtils.buildListMachine({
  id: 'MetricHealthListMachine',
  sortableFields: ['name', 'status', 'error', 'message'],
}).withConfig({
  actions: {
    setData: assign({
      data: setData,
      pristineData: (_, event) => event.data.data,
    }),
  },
  services: {
    fetchData: () => Axios.get(URL.nodesUrl),
  },
});
