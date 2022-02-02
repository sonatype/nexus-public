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

import {Utils, FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

export const baseIntUrl = '/service/rest/internal/ui/repositories';
export const basePubUrl = '/service/rest/v1/repositories';
export const recipesUrl = baseIntUrl + '/recipes';
export const blobStoresUrl = '/service/rest/v1/blobstores';
export const repositoryUrl = (format, type) => `${basePubUrl}/${formatFormat(format)}/${type}`;

export default FormUtils.buildFormMachine({
  id: 'RepositoriesFormMachine',
  initial: 'loadingOptions',
  config: (config) => ({
    ...config,
    states: {
      ...config.states,
      loadingOptions: {
        invoke: {
          id: 'fetchOptions',
          src: 'fetchOptions',
          onDone: {
            target: 'loading',
            actions: ['setOptions']
          },
          onError: {
            target: 'loadError',
            actions: ['setLoadError', 'logLoadError']
          }
        }
      }
    },
    on: {
      'RETRY': {
        target: 'loading'
      }
    }
  })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        name: validateNameField(data.name),
        format: Utils.isBlank(data.format) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        type: Utils.isBlank(data.type) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        blobStoreName: Utils.isBlank(data.blobStoreName) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        memberNames: isGroupType(data.type) && !data.memberNames.length ? UIStrings.ERROR.FIELD_REQUIRED : null
      })
    }),
    setRepositories: assign({
      repositories: (_, event) => event.data?.data
    }),
    setOptions: assign({
      blobStores: (_, event) => event.data[1]?.data,
      formats: (_, event) => {
        const recipes = event.data[0]?.data;
        return getFormats(recipes);
      },
      types: (_, event) => {
        const recipes = event.data[0]?.data;
        return getTypes(recipes);
      }
    })
  },
  services: {
    fetchData: async ({pristineData, blobStores}) => {
      if (isEdit(pristineData)) { 
        const response = await Axios.get(repositoriesUrl(pristineData.name));
        const {
          group: {memberNames}, 
          storage: {blobStoreName, strictContentTypeValidation}, 
          ...rest
        } = response;
        return {data: {
          ...rest,
          memberNames,
          blobStoreName,
          strictContentTypeValidation
        }};
      } else {
        return {data: {
          format: '',
          type: '',
          name: '',
          online: true,
          memberNames: [],
          blobStoreName: getDefaultBlobStore(blobStores),
          strictContentTypeValidation: true
        }};
      }
    },
    fetchOptions: () => {
      return Promise.all([
        Axios.get(recipesUrl),
        Axios.get(blobStoresUrl)
      ]);
    },
    saveData: ({data, pristineData}) => {
      const {
        format, 
        type, 
        name, 
        online, 
        blobStoreName, 
        strictContentTypeValidation,
        memberNames
      } = data;
      const payload = {
        name,
        online,
        storage: {
          blobStoreName,
          strictContentTypeValidation
        },
        group: {memberNames}
      };
      return isEdit(pristineData)
        ? Axios.put(repositoryUrl(format, type), payload)
        : Axios.post(repositoryUrl(format, type), payload);
    },
  }
});

const isEdit = ({name}) => Utils.notBlank(name);

const validateNameField = (field) => {
  if (Utils.isBlank(field)) {
    return UIStrings.ERROR.FIELD_REQUIRED;
  }
  else if (field.length > 255) {
    return UIStrings.ERROR.MAX_CHARS(255);
  }
  else if (!Utils.isName(field)) {
    return UIStrings.ERROR.INVALID_NAME_CHARS;
  }

  return null;
}

const formatFormat = (format) => format === 'maven2' ? 'maven' : format

const getFormats = (recipes) => [...new Set(recipes?.map(r => r.format))].sort();

const getTypes = (recipes) => recipes?.reduce((acc, curr) => {
  const {format, type} = curr;
  acc.has(format)
    ? acc.get(format).push(type)
    : acc.set(format, [type]);
  return acc;
}, new Map());

const getDefaultBlobStore = (blobStores) => 
  blobStores?.length && blobStores.length === 1 ? blobStores[0].name : '';

const isGroupType = (type) => type === 'group';
