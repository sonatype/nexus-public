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

import {
  NxStatefulSearchDropdown,
  NxFormSelect,
} from '@sonatype/react-shared-components';

import FormUtils from '../../../../interface/FormUtils';
import ExtAPIUtils from '../../../../interface/ExtAPIUtils';

import {SUPPORTED_FIELD_TYPES} from '../FormFieldsFactoryConstants';

const Field = ({id, dynamicProps, current:parentCurrent, onChange}) => {
  const {storeApi, allowAutocomplete, initialValue, idMapping, nameMapping, readOnly} = dynamicProps;
  const [action, method] = storeApi?.split('.');
  const value = parentCurrent.context.data[id] || '';
  const loadOnInit = !allowAutocomplete && !readOnly;

  const [current, send] = ExtAPIUtils.useExtMachine(action, method, {
    initial: loadOnInit ? 'loading' : 'loaded',
    actions: {
      setData: assign({
        data: (_, {data}) => {
          return data?.map(item => ({
            id: item[idMapping || 'id'],
            displayName: item[nameMapping || 'name']
          }));
        }
      })
    }
  });

  const {data} = current.context;
  const isLoading = current.matches('loading');

  const loadData = query => {
    if (query) {
      send({type: 'LOAD', options: {query}});
    }
  };

  const onSelect = (item) => {
    onChange(id, item.id);
  };

  if (readOnly) {
    return value;
  }

  return <>
    {allowAutocomplete
        ? <>
            <NxStatefulSearchDropdown
                loading={isLoading}
                matches={data}
                onSearch={loadData}
                onSelect={onSelect}
            />
            <div>{value}</div>
          </>
        : <NxFormSelect
            {...FormUtils.selectProps(id, parentCurrent, initialValue || '')}
            value={value}
            onChange={event => onChange(id, event.currentTarget.value)}
        >
          <option value=""/>
          {data?.map(({id, displayName}) => <option key={id} value={id}>{displayName}</option>)}
        </NxFormSelect>
    }
  </>;
}

export default {
  types: SUPPORTED_FIELD_TYPES.COMBOBOX,
  component: Field,
};
