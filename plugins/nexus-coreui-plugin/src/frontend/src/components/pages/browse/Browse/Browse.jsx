/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import {assign} from 'xstate';
import {useInterpret} from '@xstate/react';
import {pickBy} from 'ramda';
import {APIConstants, ListMachineUtils, ExtAPIUtils, Detail, Master, MasterDetail} from '@sonatype/nexus-ui-plugin';

import RepositoriesContextProvider from '../../admin/Repositories/RepositoriesContextProvider';
import RepositoriesListMachine from '../../admin/Repositories/RepositoriesListMachine';
import BrowseList from './BrowseList';
import BrowseTree from './BrowseTree';

const {EXT: {REPOSITORY: {ACTION, METHODS}}} = APIConstants;

export default function Browse() {
  const service = useInterpret(RepositoriesListMachine, {
    actions: {
      sortData: assign({
        data: ({sortField, sortDirection, data}) => (data.slice().sort((a, b) => {
          const dir = sortDirection === ListMachineUtils.ASC ? 1 : -1;
          const left = a[sortField];
          const right = b[sortField];

          function isString(val) {
            if (val === null) {
              return true;
            }
            return typeof val === 'string';
          }

          function getObjWithoutRepoName(obj) {
            return pickBy((_,k) => k!=='repositoryName', obj)
          }

          if (left === right) {
            return 0;
          }

          if (typeof (left) === 'object' && typeof (right) === 'object') {
            return JSON.stringify(getObjWithoutRepoName(a[sortField])).toLowerCase() > JSON.stringify(getObjWithoutRepoName(b[sortField])).toLowerCase() ? dir : -dir;
          }
          else if (isString(left) && isString(right)) {
            return (left || "").toLowerCase() > (right || "").toLowerCase() ? dir : -dir;
          }
          else {
            return left > right ? dir : -dir;
          }
        }))
      })
    },
    services: {
      fetchData: () => ExtAPIUtils.extAPIRequest(
        ACTION,
        METHODS.READ_REFERENCES,
        {}
        ).then(v => v.data.result),
      },
    devTools: true
  });

  return (
    <RepositoriesContextProvider service={service}>
      <MasterDetail path="browse/browse">
        <Master>
          <BrowseList/>
        </Master>
        <Detail>
          <BrowseTree/>
        </Detail>
      </MasterDetail>
    </RepositoriesContextProvider>
  );
}
