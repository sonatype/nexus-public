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

import {ListMachineUtils, ExtAPIUtils, APIConstants} from '@sonatype/nexus-ui-plugin';

const {EXT: {TASK: {ACTION, METHODS}}} = APIConstants;

export default ListMachineUtils.buildListMachine({
  id: 'TasksListMachine',
  sortableFields: ['name', 'typeName', 'statusDescription', 'schedule', 'nextRun', 'lastRun', 'lastRunResult'],
  sortField: 'name',
}).withConfig({
  actions: {
    setData: assign({
      data: (_, event) => event.data,
      pristineData: (_, event) => event.data,
    }),
    filterData: assign({
      data: ({filter, pristineData}) => pristineData.filter((task) => {
        return ListMachineUtils.hasAnyMatches([
            task.name,
            task.typeName,
            task.statusDescription,
            task.schedule,
            task.nextRun,
            task.lastRun,
            task.lastRunResult,
        ], filter);
      }
      )}),
  },
  services: {
    fetchData: async () => {
      const response = await ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ);
      ExtAPIUtils.checkForError(response);
      return ExtAPIUtils.extractResult(response);
    },
  },
});
