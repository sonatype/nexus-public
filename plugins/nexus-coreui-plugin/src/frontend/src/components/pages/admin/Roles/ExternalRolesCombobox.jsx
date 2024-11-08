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
import {useActor} from '@xstate/react';

import {NxCombobox, NxFormGroup, NxTextInput} from '@sonatype/react-shared-components';
import {ExtJS, FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
const { 
  ROLES: { 
    FORM: LABELS, 
    FORM: {EXTERNAL_TYPE: {LDAP: {MORE_CHARACTERS, NO_RESULTS}}} 
  } 
} = UIStrings;

export default function ExternalRolesCombobox({actor, parentMachine}) {
  const [state, send] = useActor(actor);

  const {data, query, error, externalRoleType, ldapQueryCharacterLimit} = state.context;
  const isLoading = state.matches('loading');
  const retry = () => send({type: 'RETRY'});
  const [parentState, sendParent] = parentMachine;

  const lowercaseQuery = query.toLowerCase();
  const externalRoles = data.filter(it => it.displayName.toLowerCase().indexOf(lowercaseQuery) !== -1);

  const onChangeMappedRole = (newQuery, dataItem) => {
    sendParent({type: 'UPDATE', name: 'id', value: dataItem?.id || newQuery});
  };

  const onSearchMappedRole = (newQuery) => {
    if (error && newQuery === query) {
      retry();
    } else {
      send({type: 'SET_QUERY', query: newQuery});
      if(externalRoleType.toLowerCase() !== 'ldap') {
        onChangeMappedRole(newQuery);
      }
    }
  };

  const emptyMessage = () => {
    if (externalRoleType.toLowerCase() === 'ldap') {
      if (query.length >= ldapQueryCharacterLimit) {
        return NO_RESULTS;
      } else {
        return MORE_CHARACTERS(ldapQueryCharacterLimit);
      }
    }
  }

  return <>
    {externalRoleType.toLowerCase() !== 'ldap' &&
      <NxFormGroup label={LABELS.MAPPED_ROLE.LABEL} isRequired>
        <NxTextInput
          {...FormUtils.fieldProps('id', parentState)}
          onChange={(value) => onSearchMappedRole(value)}
          value={query}
          />
      </NxFormGroup>
    }
    {externalRoleType.toLowerCase() === 'ldap' &&
      <NxFormGroup label={LABELS.MAPPED_ROLE.LABEL} isRequired>
        <NxCombobox
            {...FormUtils.fieldProps('id', parentState)}
            onChange={onChangeMappedRole}
            onSearch={onSearchMappedRole}
            loading={isLoading}
            autoComplete={false}
            matches={externalRoles}
            loadError={error}
            aria-label="combobox"
            emptyMessage={emptyMessage()}
        />
      </NxFormGroup>
    }
  </>
}
