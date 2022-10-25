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

import {ListMachineUtils} from '@sonatype/nexus-ui-plugin';

import {URL} from './LdapServersHelper';

export default ListMachineUtils.buildListMachine({
  id: 'LdapServersListMachine',
  sortableFields: ['order', 'name', 'url'],
  sortField: 'order',
}).withConfig({
  actions: {
    setData: assign((_, {data: ldapServers}) => {
      const data = ldapServers?.data?.map(server => {
        server.url = `${server.protocol}://${server.host}:${server.port}/${server.searchBase}`;
        return server;
      });

      return {
        data: data,
        pristineData: data,
      };
    }),
    filterData: assign({
      data: ({filter, pristineData}, _) => pristineData.filter(item =>
          ListMachineUtils.hasAnyMatches([
              item.order,
              item.name,
              item.url,
          ], filter)
      )}),
  },
  services: {
    fetchData: () => Axios.get(URL.ldapServersUrl)
  }
});
