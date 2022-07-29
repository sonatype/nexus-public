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
import React from 'react';
import {assign} from 'xstate';
import {sortBy, prop} from 'ramda';
import {FormUtils, ExtAPIUtils, APIConstants} from '@sonatype/nexus-ui-plugin';

const {EXT: {PROPRIETARY_REPOSITORIES: {ACTION, METHODS}, }} = APIConstants;

export default FormUtils.buildFormMachine({
  id: 'ProprietaryRepositoriesMachine',
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: () => {{}}
    }),
    setData: assign((_, event) => {
      const [selected, possible] = sortBy(prop('tid'), event.data?.data || []);
      const data = selected?.result?.data;
      return {
        data,
        pristineData: data,
        possibleRepos: possible?.result?.data,
      };
    }),
  },
  services: {
    fetchData: () => ExtAPIUtils.extAPIBulkRequest([
      {action: ACTION, method: METHODS.READ},
      {action: ACTION, method: METHODS.POSSIBLE_REPOS},
    ]),
    saveData: ({data}) => ExtAPIUtils.extAPIRequest(ACTION, METHODS.UPDATE, {
      data: [{'enabledRepositories': data.enabledRepositories}],
    }).then(v => v.data.result),
  }
});
