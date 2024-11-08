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
import {assign, actions, send, createMachine} from 'xstate';
import {useMachine} from '@xstate/react';

import {
  NxFormSelect,
  NxCombobox,
} from '@sonatype/react-shared-components';

import FormUtils from '../../../../interface/FormUtils';
import ExtAPIUtils from '../../../../interface/ExtAPIUtils';

import {SUPPORTED_FIELD_TYPES} from '../FormFieldsFactoryConstants';
import UIStrings from '../../../../constants/UIStrings';
import APIConstants from '../../../../constants/APIConstants';

const Field = ({id, dynamicProps, current:parentState, onChange}) => {
  const {storeApi, allowAutocomplete, initialValue, idMapping, nameMapping, readOnly} = dynamicProps;
  const [action, method] = storeApi?.split('.');
  const value = parentState.context.data[id] || '';
  const loadOnInit = !allowAutocomplete || value;

  const [state, sendEvent] = useMachine(() =>
      createMachine({
        id: 'ComboboxMachine',
        initial: loadOnInit ? 'fetching' : 'loaded',
        states: {
          fetching: {
            invoke: {
              src: 'fetch',
              onDone: {
                target: 'loaded',
                actions: ['setData'],
              },
              onError: {
                target: 'error',
                actions: ['setError'],
              }
            }
          },
          loading: {},
          loaded: {},
          error: {},
        },
        on: {
          LOAD: {
            target: 'loading',
          },
          FETCH: {
            target: 'fetching',
          },
          LOAD_WITH_DEBOUNCE: {
            target: 'loading',
            actions: ['setQuery', 'resetError', 'debounceApiCall', 'doFetch'],
          },
          RESET_MATCHES: {
            target: 'loaded',
            actions: ['debounceApiCall', 'resetMatches'],
          },
        }
      }, {
        actions: {
          setData: assign({
            data: (_, {data}) => {
              return data?.map(item => ({
                id: item[idMapping || 'id'],
                displayName: item[nameMapping || 'name']
              }));
            }
          }),
          setQuery: assign({
            query: (_, {query}) => query,
          }),
          setError: assign({
            error: (_, event) => event?.data?.message || UIStrings.ERROR.UNKNOWN
          }),
          resetMatches: assign({
            data: [],
          }),
          resetError: assign({
            error: null,
          }),
          debounceApiCall: actions.cancel('debounced-api-call'),
          doFetch: send({
            type: 'FETCH',
            id: 'debounced-api-call',
            delay: APIConstants.DEBOUNCE_DELAY,
          }),
        },
        services: {
          fetch: async ({query}) => {
            const options = allowAutocomplete ? {query} : null;
            const response = await ExtAPIUtils.extAPIRequest(action, method, options);
            ExtAPIUtils.checkForError(response);
            return ExtAPIUtils.extractResult(response, []);
          }
        }
      }), {
        context: {
          query: value,
          data: [],
        },
        devTools: true,
      },
  );

  const {data, error} = state.context;
  const selectedItem = data.find(item => item.id === value);
  const isLoading = state.matches('loading') || state.matches('fetching');

  const loadData = (query) => {
    if (query) {
      sendEvent({type: 'LOAD_WITH_DEBOUNCE', query});
    } else {
      sendEvent({type: 'RESET_MATCHES'});
    }
  };

  const onChangeCombobox = (query, dataItem) => onChange(id, dataItem?.id || query)

  if (readOnly) {
    return selectedItem?.displayName || value;
  }

  return <>
    {allowAutocomplete
        ? <NxCombobox
            {...FormUtils.fieldProps(id, parentState)}
            value={selectedItem?.displayName || value}
            onChange={onChangeCombobox}
            onSearch={loadData}
            loading={isLoading}
            autoComplete={true}
            matches={data}
            loadError={error}
            aria-label="combobox"
          />
        : <NxFormSelect
            {...FormUtils.fieldProps(id, parentState, initialValue || '')}
            value={value}
            onChange={value => onChange(id, value)}
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
