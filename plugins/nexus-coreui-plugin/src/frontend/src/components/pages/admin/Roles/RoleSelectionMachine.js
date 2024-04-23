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
import { mergeDeepRight } from 'ramda';
import { assign } from 'xstate';
import { ListMachineUtils } from '@sonatype/nexus-ui-plugin';

const RoleSelectionMachine = ListMachineUtils.buildListMachine({
  id: 'RoleSelectionMachine',
  sortableFields: ['select', 'name', 'description'],
  sortField: 'name',
  initial: 'loaded',
  config: config => mergeDeepRight(config, {
    states: {
      loaded: {
        on: {
          SELECT_ROLE: {
            target: 'loaded',
            actions: ['selectRole']
          },
          ON_CONFIRM: {
            target: 'loaded',
            actions: ['onConfirm']
          },
          CHANGE_PAGE: {
            target: 'loaded',
            actions: ['changePage']
          },
        },
      },
    }
  }),
}).withConfig({
  actions: {
    selectRole: assign({
      data: ({ data }, { role }) => data.map(oldRole => oldRole === role ?
        { ...role, isSelected: !role.isSelected } : oldRole),
      tempSelectedRoles: ({ data }, { role }) =>
        data.filter(oldRole => oldRole === role ? !oldRole.isSelected : oldRole.isSelected).map(role => role.id)
    }),

    onConfirm: assign({
      selectedRoles: ({ tempSelectedRoles }) => tempSelectedRoles
    }),

    changePage: assign({
      offsetPage: (_, { offsetPage }) => offsetPage
    }),

    setFilter: assign({
      filter: (_, { filter }) => filter,
      offsetPage: 0,
    }),

    filterData: assign({
      filteredData: ({ filter, data }, _) => {
        const regExp = new RegExp('[-]?' + filter.toLowerCase().replace(/\*/g, '.*')) ;
        return data.filter(({ name, description }) => regExp.test(name.toLowerCase()) || regExp.test(description.toLowerCase()));
      },
      numberOfRoles: ({ filter, data }, _) => {
        const regExp = new RegExp('[-]?' + filter.toLowerCase().replace(/\*/g, '.*'));
        return data.filter(({ name, description }) => regExp.test(name.toLowerCase()) ||
          regExp.test(description.toLowerCase())).length;
      }
    }),
    sortData: assign({
      filteredData: ({ sortField, sortDirection, filteredData }) => {
        if (sortField === 'select') {
          return sortDirection === 'asc' ? filteredData.sort((a, b) =>
            Number(b.isSelected) - Number(a.isSelected) || a.name.localeCompare(b.name))
            : filteredData.sort((a, b) => Number(a.isSelected) - Number(b.isSelected) || a.name.localeCompare(b.name));
        } else {
          return ListMachineUtils.sortDataByFieldAndDirection({ useLowerCaseSorting: true })
            ({ sortField, sortDirection, data: filteredData });
        }
      }
    })
  }
});

export default RoleSelectionMachine;
