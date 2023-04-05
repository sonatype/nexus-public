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

import {ListMachineUtils} from '@sonatype/nexus-ui-plugin';
import {useMachine} from '@xstate/react';
import {useEffect} from 'react';
import {assign} from "xstate";

export default function useInsightListMachine(id, data) {
  const machine = ListMachineUtils.buildListMachine({
    id,
    initial: 'loaded',
    sortField: 'downloadCount',
    sortDirection: ListMachineUtils.DESC,
    sortableFields: ['identifier', 'downloadCount']
  }).withConfig({
    actions: {
      filterData: assign({
        data: ({pristineData, filter}) =>
            pristineData.filter(it => it.identifier.toLowerCase().includes(filter.toLowerCase()))
      })
    }
  });
  const [state, send] = useMachine(machine, {devTools: true});

  useEffect(() => {
    send({
      type: 'SET_DATA',
      data: {
        data
      }
    });
  }, [data]);

  const sortById = () => send({type: 'SORT_BY_IDENTIFIER'});
  const sortByCount = () => send({type: 'SORT_BY_DOWNLOAD_COUNT'});
  const setFilter = (filter) => send({type: 'FILTER', filter});

  const idSortDir = ListMachineUtils.getSortDirection('identifier', state.context);
  const countSortDir = ListMachineUtils.getSortDirection('downloadCount', state.context);

  return {
    filter: state.context.filter,
    data: state.context.data,
    sortById,
    sortByCount,
    setFilter,
    idSortDir,
    countSortDir
  };
}
