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
  flatten,
  flip,
  fromPairs,
  groupBy,
  map,
  mergeDeepRight,
  pair,
  path,
  prop,
  pipe,
  filter,
  find,
  propEq,
  toPairs,
  values
} from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { APIConstants, ExtJS, FormUtils, ValidationUtils, ExtAPIUtils, UIStrings, Utils }
  from '@sonatype/nexus-ui-plugin';

/**
 * Construct initial form field data structures for the machine's `data` field
 * from the uploadDefinition from the server
 */
function mkFieldStates(uploadDefinition) {
  return {
    // The first (of potentially several) file uploads
    asset0: null,
    ...fromPairs(map(({name}) => [name, ''], uploadDefinition.componentFields ?? []))
  };
}

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
      componentFieldsByGroup: flip(pipe(path(['data', 'uploadDefinition', 'componentFields']), groupBy(prop('group')))),
      data: (_, { data: { uploadDefinition } }) => mkFieldStates(uploadDefinition),
      repoSettings: (_, { data: { repoSettings } }) => repoSettings
    }),

    // TODO: this code presumably implements required field validation, but I cannot test it in NEXUS-35097
    // because none of the formats implemented in that ticket actually have required fields. Revisit when needed
    // in a future ticket
    validate: assign(({ data, componentFieldsByGroup }) => {
      const fields = flatten(values(componentFieldsByGroup)),
          requiredFields = map(prop('name'), filter(complement(prop('optional')), fields)),
          missingRequiredFields = filter(f => ValidationUtils.isBlank(data[f]), requiredFields),
          missingFileUploads = data.asset0 === null ? { asset0: UIStrings.ERROR.FIELD_REQUIRED } : null,
          validationErrors = {
            ...fromPairs(map(pair(__, UIStrings.ERROR.FIELD_REQUIRED), missingRequiredFields)),
            ...missingFileUploads
          };

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
              .then(pipe(prop('data'), find(propEq('name', id)))),
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
      for (const [key, value] of toPairs(data)) {
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
