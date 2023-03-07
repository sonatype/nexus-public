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
import { assign } from 'xstate';
import axios from 'axios';
import {
  __,
  complement,
  compose,
  concat,
  defaultTo,
  flatten,
  flip,
  fromPairs,
  groupBy,
  lensPath,
  map,
  mergeDeepRight,
  pair,
  path,
  prop,
  pipe,
  filter,
  find,
  propEq,
  set,
  split,
  toPairs,
  transduce,
  values,
  view
} from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { APIConstants, ExtJS, FormUtils, ValidationUtils, ExtAPIUtils, UIStrings, Utils }
  from '@sonatype/nexus-ui-plugin';

/**
 * @param defs a list of componentField or assetField definition objects as returned by the backend
 * @return an object containing a key for each input field's name, with each key having a value of ''
 */
const mkStateObjFromFieldDefs = pipe(defaultTo([]), map(field => [field.name, '']), fromPairs);

/**
 * @param defs a list of componentField or assetField definition objects, with the assetField definitions transformed
 * to use fully-qualified dot-separated names (e.g. `asset0.pathId` rather than just `pathId`)
 * @return a list of the names of the fields which are required, as path arrays (e.g. split on the dot)
 */
const getRequiredFieldNames = pipe(filter(complement(prop('optional'))), map(pipe(prop('name'), split('.'))));

/**
 * Construct initial form field data structures for the machine's `data` field
 * from the uploadDefinition from the server
 */
function mkFieldStates(uploadDefinition) {
  return {
    // The first (of potentially several) file uploads
    asset0: {
      // we use the `_` subkey to denote the value that should be submitted as the parent key itself, e.g. the file
      // upload's FileList
      _: null,
      ...mkStateObjFromFieldDefs(uploadDefinition.assetFields)
    },
    ...mkStateObjFromFieldDefs(uploadDefinition.componentFields)
  };
}

/**
 * Transform nested data structure into flat iteration of pairs with dot-separated key strings
 */
function * flattenDataForSubmit(data) {
  for (const [key, val] of toPairs(data)) {
    if (typeof val === 'object' && !(val instanceof FileList)) {
      for (const [subkey, val] of flattenDataForSubmit(val)) {
        if (subkey === '_') {
          // The _ subkey is treated as the value of the parent key
          yield [key, val];
        }
        else {
          yield [`${key}.${subkey}`, val];
        }
      }
    }
    else {
      yield [key, val];
    }
  }
}

/**
 * Given a list of property paths, a transformation function, and an object,
 * return an object with all of the child paths listed in pathList where each corresponding leaf value is
 * the result of applying the fn to the value at the corresponding path on the input obj
 */
function applyToPaths(pathList, fn, obj) {
  const mapPathsToLenses = map(lensPath),
      mapLensesToPairs = map(lens => [lens, fn(view(lens, obj))]),

      // Note: transducers effectively compose backwards.
      // The data flows through mapPathsToLenses first followed by mapLensesToPairs
      mapPathsToPairs = compose(mapPathsToLenses, mapLensesToPairs),
      reduceToRetval = (acc, [lens, val]) => set(lens, val, acc);

  return transduce(mapPathsToPairs, reduceToRetval, {}, pathList);
}

const fileValidator = f => f === null ? UIStrings.ERROR.FIELD_REQUIRED : null;

export default FormUtils.buildFormMachine({
  id: 'UploadDetailsMachine',
  stateAfterSave: 'saved',
  config: (config) =>
      mergeDeepRight(config, {
        states: {
          cancelling: {
            type: 'final',
            invoke: {
              src: 'cancelUpload'
            }
          },
          saved: {
            type: 'final',
            invoke: {
              src: 'waitAndRedirect'
            }
          }
        },
        on: {
          CANCEL: {
            target: 'cancelling'
          }
        }
      })
}).withConfig({
  actions: {
    setData: assign({
      // The following two context fields expose the field definitions to the view logic. The actual state of those
      // field is stored in the `data` context field
      componentFieldsByGroup: flip(pipe(path(['data', 'uploadDefinition', 'componentFields']), groupBy(prop('group')))),
      assetFields: (_, { data: { uploadDefinition: { assetFields } } }) =>
          map(field => ({ ...field, name: `asset0.${field.name}` }), assetFields),

      data: (_, { data: { uploadDefinition } }) => mkFieldStates(uploadDefinition),
      repoSettings: (_, { data: { repoSettings } }) => repoSettings
    }),

    validate: assign(({ data, componentFieldsByGroup, assetFields }) => {
      const fields = flatten([values(componentFieldsByGroup), assetFields]),
          requiredFields = getRequiredFieldNames(fields),
          fieldValidationErrors = applyToPaths(requiredFields, ValidationUtils.validateNotBlank, data),
          fileUploadValidationErrors = applyToPaths([['asset0', '_']], fileValidator, data),
          validationErrors = mergeDeepRight(fieldValidationErrors, fileUploadValidationErrors);

      return { ...data, validationErrors };
    }),
    onSaveSuccess: assign({
      // The string returned from the backend upload API
      savedComponentName: (_, { data }) => data
    })
  },
  services: {
    async fetchData({pristineData: {id}}) {
      const repoSettingsPromise = axios.get(APIConstants.REST.PUBLIC.UPLOAD)
              .then(({ data }) => {
                const repoSettings = find(propEq('name', id), data);
                if (!repoSettings) {
                  throw new Error(`Unable to find repository "${id}"`);
                }
                else {
                  return repoSettings;
                }
              }),
          uploadDefinitionsPromise = ExtAPIUtils.extAPIRequest(
              APIConstants.EXT.UPLOAD.ACTION,
              APIConstants.EXT.UPLOAD.METHODS.GET_UPLOAD_DEFINITIONS
          ).then(response => {
            ExtAPIUtils.checkForError(response);
            return response;
          }).then(ExtAPIUtils.extractResult);

      const [uploadDefinitions, repoSettings] = await Promise.all([uploadDefinitionsPromise, repoSettingsPromise]),
          { format } = repoSettings,
          uploadDefinition = find(propEq('format', format), uploadDefinitions);

      return { uploadDefinition, repoSettings };
    },
    async saveData({ repoSettings: { name }, data }) {
      const formData = new FormData();
      for (const [key, value] of flattenDataForSubmit(data)) {
        if (value instanceof FileList) {
          for (const file of Array.from(value)) {
            formData.append(key, file);
          }
        }
        else {
          formData.set(key, value);
        }
      }

      const postResponse =
          await axios.post(`${APIConstants.REST.INTERNAL.UPLOAD}${encodeURIComponent(name)}`, formData);

      // This REST API reports errors in a weird way
      if (postResponse.data?.success !== true) {
        throw new Error(postResponse.data?.[0]?.message ?? 'Unknown Error');
      }

      // The Search page's backing data gets updated asynchronously and isn't necessarily
      // available yet just because the REST call above returned. Wait in the hopes that
      // it will be available by the time we redirect. Note that this wait is in addition to the wait
      // while the "Success" submit mask is shown.
      await Utils.timeoutPromise(500);

      return postResponse.data.data;
    },
    cancelUpload() {
      // remove trailing colon-separated part from URL, taking us back to the Upload List page
      window.location.hash = window.location.hash.replace(/:[^:]*$/, '');
    },
    async waitAndRedirect({ savedComponentName }) {
      await Utils.timeoutPromise(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      window.location.hash = `browse/search=${encodeURIComponent(`keyword="${savedComponentName}"`)}`;
    }
  }
});
