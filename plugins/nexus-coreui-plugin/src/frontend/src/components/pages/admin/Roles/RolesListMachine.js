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
import {ListMachineUtils, ExtAPIUtils, APIConstants, ExtJS, Permissions} from '@sonatype/nexus-ui-plugin';
import {URL} from './RolesHelper';

const {EXT: {CAPABILITY: {ACTION, METHODS}}} = APIConstants;

const DEFAULT_ROLE_CAPABILITY_TYPE_ID = 'defaultrole';
const DEFAULT_ROLE_STATE_KEY = 'defaultRole';

export default ListMachineUtils.buildListMachine({
  id: 'RolesListMachine',
  sortableFields: ['id', 'name', 'description'],
  sortField: 'id',
}).withConfig({
  actions: {
    filterData: assign({
      data: ({filter, pristineData}, _) => pristineData.filter(({id, name, description}) =>
          ListMachineUtils.hasAnyMatches([id, name, description], filter)
      )}),
    setData: assign((_, {data: [roles, capabilities]}) => {
      const defaultRoleState = ExtJS.state().getValue(DEFAULT_ROLE_STATE_KEY);
      const defaultRoleCapability = capabilities.find(it => it.typeId === DEFAULT_ROLE_CAPABILITY_TYPE_ID);
      const defaultRole = {
        capabilityId: defaultRoleCapability && defaultRoleCapability.enabled ? defaultRoleCapability.id : null,
        roleId: defaultRoleState ? defaultRoleState.id : null,
        roleName: defaultRoleState ? defaultRoleState.name : null,
      };

      return {
        defaultRole,
        data: roles.data,
        pristineData: roles.data,
      };
    }),
  },
  services: {
    fetchData: () => Axios.all([
      Axios.get(URL.defaultRolesUrl),
      ExtJS.state().getValue('capabilityActiveTypes').includes(DEFAULT_ROLE_CAPABILITY_TYPE_ID) &&
      ExtJS.checkPermission(Permissions.CAPABILITIES.READ)
          ? ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ).then(ExtAPIUtils.checkForErrorAndExtract)
          : Promise.resolve([]),
    ]),
  }
});
