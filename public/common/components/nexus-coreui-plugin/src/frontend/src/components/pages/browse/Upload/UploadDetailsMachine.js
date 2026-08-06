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
  always,
  complement,
  compose,
  concat,
  chain,
  defaultTo,
  dissoc,
  evolve,
  equals,
  filter,
  find,
  flatten,
  flip,
  fromPairs,
  groupBy,
  head,
  includes,
  is,
  isEmpty,
  isNil,
  keys,
  lensPath,
  lensProp,
  map,
  match,
  max,
  mergeDeepRight,
  mergeLeft,
  over,
  mergeDeepWith,
  pair,
  path,
  prop,
  pickBy,
  pipe,
  propEq,
  reject,
  set,
  split,
  tail,
  test,
  toPairs,
  transduce,
  values,
  view,
  zipObj
} from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS, combineValidationErrors, hasValidationErrors }
  from '@sonatype/react-shared-components';

import { APIConstants, ExtJS, FormUtils, ValidationUtils, ExtAPIUtils, UIStrings, Utils }
  from '@sonatype/nexus-ui-plugin';

import { repoSupportsUiUpload } from '../BrowseUtils';
import UploadStrings from '../../../../constants/pages/browse/upload/UploadStrings';
import {
  COMPOUND_FIELD_PARENT_NAME,
  ASSET_NUM_MATCHER,
  MAVEN_FORMAT,
  MAVEN_GENERATE_POM_FIELD_NAME,
  MAVEN_PACKAGING_FIELD_NAME,
  MAVEN_COMPONENT_COORDS_GROUP,
  TERRAFORM_FORMAT,
  TERRAFORM_MODULE_COORDS_GROUP,
  TERRAFORM_PROVIDER_COORDS_GROUP,
  TERRAFORM_MODULE_COORDS_GROUP_HELP,
  TERRAFORM_PROVIDER_COORDS_GROUP_HELP,
  TERRAFORM_UPLOAD_TYPE_FIELD_NAME,
  TERRAFORM_UPLOAD_TYPE_MODULE,
  TERRAFORM_UPLOAD_TYPE_PROVIDER
} from './UploadDetailsUtils';

// CLM-39871: structured error marker used to carry sync enforcement block/unavailable
// details from saveData() to the view layer while keeping the FormUtils generic error
// path working for every other failure mode.
export const HOSTED_ENFORCEMENT_ERROR_PREFIX = 'HOSTED_ENFORCEMENT::';

const HOSTED_ENFORCEMENT_CODES = new Set([
  'HOSTED_DEPLOYMENT_BLOCKED',
  'HOSTED_ENFORCEMENT_UNAVAILABLE'
]);

export function extractEnforcementPayload(response) {
  if (!response) {
    return null;
  }
  const status = response.status;
  if (status !== 403 && status !== 503) {
    return null;
  }
  const body = response.data;
  if (!body || typeof body !== 'object' || !HOSTED_ENFORCEMENT_CODES.has(body.errorCode)) {
    return null;
  }
  // CLM-40150: evaluationUrl is no longer carried by the BE response and the FE
  // banner no longer renders a link, so we don't include it on the parsed object
  // either. Dropping the field here keeps the parsed shape consistent with what
  // the BE actually emits and prevents future code from re-adding a stale link.
  return {
    errorCode: body.errorCode,
    assetName: body.assetName ?? null,
    repositoryName: body.repositoryName ?? null,
    reason: body.reason ?? null,
    correlationId: body.correlationId ?? null
  };
}

export function parseHostedEnforcementError(saveError) {
  if (!saveError || typeof saveError !== 'string') {
    return null;
  }
  if (!saveError.startsWith(HOSTED_ENFORCEMENT_ERROR_PREFIX)) {
    return null;
  }
  try {
    return JSON.parse(saveError.substring(HOSTED_ENFORCEMENT_ERROR_PREFIX.length));
  }
  catch (_) {
    return null;
  }
}

/**
 * @param defs a list of componentField or assetField definition objects as returned by the backend
 * @return an object containing a key for each input field's name, with each key having a value of ''
 */
const mkStateObjFromFieldDefs = pipe(defaultTo([]), map(field => [field.name, '']), fromPairs);

/**
 * @return a list of the names of the fields which are required, as path arrays (e.g. split on the dot)
 */
function getRequiredFieldNames(componentFieldsByGroup, assetFields, assetKeys, hasPomExtension) {
  // For each assetKey (e.g. "asset0") and each assetField definition object, construct an assetField
  // definition object with the fully qualified field name (e.g. "asset0.pathId")
  const assetFieldsWithFullNames = map(
          assetKey => map(evolve({ name: concat(`${assetKey}.`) }), assetFields),
          assetKeys
      ),

      // If we have a POM extension, the "Component coordinates" section is disabled and should
      // not be validated
      filteredComponentFieldsByGroup =
          hasPomExtension ? dissoc(MAVEN_COMPONENT_COORDS_GROUP, componentFieldsByGroup) : componentFieldsByGroup,

      fields = flatten([values(filteredComponentFieldsByGroup), assetFieldsWithFullNames]),
      requiredFields = filter(complement(prop('optional')), fields);

  return map(pipe(prop('name'), split('.')), requiredFields);
}

/**
 * For each file upload present on the form, an object of this structure is present
 */
function mkAssetFieldStates(assetFields) {
  return {
    [COMPOUND_FIELD_PARENT_NAME]: null,
    ...mkStateObjFromFieldDefs(assetFields)
  };
}

/**
 * Construct initial form field data structures for the machine's `data` field
 * from the uploadDefinition from the server
 */
function mkFieldStates(uploadDefinition) {
  return {
    // The first (of potentially several) file uploads
    asset0: mkAssetFieldStates(uploadDefinition.assetFields),
    ...mkStateObjFromFieldDefs(uploadDefinition.componentFields)
  };
}

/**
 * Transform nested data structure into flat iteration of pairs with dot-separated key strings
 */
function * flattenDataForSubmit(data) {
  for (const [key, value] of toPairs(data)) {
    if (typeof value === 'object' && !(value instanceof FileList)) {
      for (const [subkey, subValue] of flattenDataForSubmit(value)) {
        if (subkey === COMPOUND_FIELD_PARENT_NAME) {
          // The _ subkey is treated as the value of the parent key
          yield [key, subValue];
        }
        else {
          yield [`${key}.${subkey}`, subValue];
        }
      }
    }
    else {
      yield [key, value];
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

/**
 * Extract the asset number from the assetKey and return it as a `number`. Example: "asset0" -> 0
 */
function getAssetId(assetKey) {
  const matched = match(ASSET_NUM_MATCHER, assetKey)[1];

  return matched ? parseInt(matched) : null;
}

/**
 * In the field state object each file upload has a section named "asset..." where the ... is a sequential number.
 * This function returns the highest-such currently existing number
 */
function getMaxAssetId(data) {
  const transducer = compose(map(getAssetId), reject(isNil));

  return transduce(transducer, max, null, keys(data));
}

const fileValidator = f => f === null ? UIStrings.ERROR.FIELD_REQUIRED : null;

/**
 * @param regexMap an object containing the regex to apply and the list of fields to set based on its capture groups
 * @return a function which takes a string and returns an object with keys from the fieldList and values
 * derived from the regex capture groups. If a capture group results in undefined, the object will have that key's
 * value set to ''
 */
const mkRegexMapper = ({ regex, fieldList }) => pipe(
  match(new RegExp(regex)),
  tail,
  map(defaultTo('')),
  zipObj(fieldList)
);

/**
 * Check asset fields for uniqueness and return object containing field-level validation errors
 * for all non-unique asset fields (that is, all asset fields within assets whose asset fields are, when considered all
 * together, not different from the asset fields of some other asset)
 */
function checkAssetFieldUniqueness(data, assetKeys) {
  // Each key in this map is an object mapping field local names (e.g. `filename` not
  // `asset0.filename`) to their form values. Each value in the map is a list of asset keys (e.g. `asset0`) containing
  // that set of asset field values
  const valueMap = new Map();
  for (const assetKey of assetKeys) {
    const valueObj = dissoc(COMPOUND_FIELD_PARENT_NAME, data[assetKey]);

    let foundExisting = false;
    for (const [existingKey, existingValues] of valueMap) {
      if (equals(existingKey, valueObj)) {
        existingValues.push(assetKey);
        foundExisting = true;
      }
    }

    if (!foundExisting) {
      valueMap.set(valueObj, [assetKey]);
    }
  }

  // Build a validation errors object from valueMap entries that indicate multiple assets with matching asset field
  // values
  const retval = {};
  for (const [fields, assetKeys] of valueMap) {
    if (assetKeys.length > 1) {
      const errors = map(always(UploadStrings.UPLOAD.DETAILS.ASSET_NOT_UNIQUE_MESSAGE), fields);

      for (const assetKey of assetKeys) {
        retval[assetKey] = errors;
      }
    }
  }

  return retval;
}

/**
 * @param obj an object whose values are RSC ValidationErrors (e.g. string, string[], null, or undefined)
 * @return Whether at least one value within the object represents an _actual_ validation error (e.g. string or
 * non-empty array)
 */
const objHasValidationErrors = pipe(filter(hasValidationErrors), complement(isEmpty));

/**
 * @param validationErrors structured validation errors for the form
 * @return asset keys (e.g. `asset0` for which no sub-fields (or the _ field) have validation errors
 */
const getAssetKeysWithoutValidationErrors = pipe(filter(is(Object)), reject(objHasValidationErrors), keys);

/*
 * Special behaviors for the Maven format:
 * If any "Extension" asset field is set to "pom", disable all fields within the "Component coordinates" group.
 * If the "Generate a POM file with these coordinates" box is checked, disable the "Packaging" field.
 *
 * Special behaviors for the Terraform format:
 * If uploadType is "module", disable all fields within the "Provider Coordinates" group.
 * If uploadType is "provider", disable all fields within the "Module Coordinates" group.
 */
const setDisabledAction = assign(context => {
  const { componentFieldsByGroup, data, repoSettings } = context;

  if (repoSettings.format === MAVEN_FORMAT) {
    const extensionFieldValues =
            chain(([k, v]) => test(ASSET_NUM_MATCHER, k) ? [v.extension.trim()] : [], toPairs(data)),
        hasPomExtension = includes('pom', extensionFieldValues),
        componentCoordFieldNames = map(prop('name'), componentFieldsByGroup[MAVEN_COMPONENT_COORDS_GROUP]),
        generatePomValue = data[MAVEN_GENERATE_POM_FIELD_NAME],
        disabledByExtension = fromPairs(map(pair(__, hasPomExtension), componentCoordFieldNames));

    return {
      disabledFields: {
        ...disabledByExtension,
        [MAVEN_PACKAGING_FIELD_NAME]: disabledByExtension[MAVEN_PACKAGING_FIELD_NAME] || !generatePomValue
      },
      hasPomExtension
    };
  }
  else if (repoSettings.format === TERRAFORM_FORMAT) {
    const uploadType = data[TERRAFORM_UPLOAD_TYPE_FIELD_NAME]?.trim();
    const moduleCoordFieldNames = componentFieldsByGroup[TERRAFORM_MODULE_COORDS_GROUP] ?
        map(prop('name'), componentFieldsByGroup[TERRAFORM_MODULE_COORDS_GROUP]) : [];
    const providerCoordFieldNames = componentFieldsByGroup[TERRAFORM_PROVIDER_COORDS_GROUP] ?
        map(prop('name'), componentFieldsByGroup[TERRAFORM_PROVIDER_COORDS_GROUP]) : [];

    const disabledFields = {};
    let clearedData = data;

    // Disable and clear Provider Coordinates when uploadType is "module"
    if (uploadType === TERRAFORM_UPLOAD_TYPE_MODULE) {
      providerCoordFieldNames.forEach(fieldName => {
        disabledFields[fieldName] = true;
        // Clear the field value by removing it from data
        clearedData = dissoc(fieldName, clearedData);
      });
    }
    // Disable and clear Module Coordinates when uploadType is "provider"
    else if (uploadType === TERRAFORM_UPLOAD_TYPE_PROVIDER) {
      moduleCoordFieldNames.forEach(fieldName => {
        disabledFields[fieldName] = true;
        // Clear the field value by removing it from data
        clearedData = dissoc(fieldName, clearedData);
      });
    }

    return {
      disabledFields,
      data: clearedData
    };
  }
  else {
    return context;
  }
});

export default FormUtils.buildFormMachine({
  id: 'UploadDetailsMachine',
  stateAfterSave: 'saved',
  config: (config) =>
      mergeDeepRight(config, {
        states: {
          cancelling: {
            type: 'final'
          },
          saved: {
            type: 'final',
            invoke: {
              src: 'waitAndRedirect'
            }
          },
          loaded: {
            on: {
              UPDATE: {
                actions: ['update', 'processRegexMaps', 'setDisabled']
              }
            }
          }
        },
        on: {
          CANCEL: {
            target: 'cancelling'
          },
          ADD_ASSET: {
            actions: ['addAsset', 'validate']
          },
          DELETE_ASSET: {
            actions: ['deleteAsset', 'validate', 'setDisabled']
          }
        }
      })
}).withConfig({
  actions: {
    setData: assign({
      // The following two context fields expose the field definitions to the view logic. The actual state of those
      // field is stored in the `data` context field
      componentFieldsByGroup: flip(pipe(path(['data', 'uploadDefinition', 'componentFields']), groupBy(prop('group')))),

      groupHelpText: (_, { data: { repoSettings } }) => {
        const helpTextMap = {};
        if (repoSettings?.format === TERRAFORM_FORMAT) {
          helpTextMap[TERRAFORM_MODULE_COORDS_GROUP] = TERRAFORM_MODULE_COORDS_GROUP_HELP;
          helpTextMap[TERRAFORM_PROVIDER_COORDS_GROUP] = TERRAFORM_PROVIDER_COORDS_GROUP_HELP;
        }
        return helpTextMap;
      },

      assetFields: (_, { data: { uploadDefinition: { assetFields } } }) => assetFields,
      regexMapFields: (_, { data: { uploadDefinition: { regexMap } } }) => regexMap?.fieldList,
      regexMapper: (_, { data: { uploadDefinition: { regexMap } } }) => regexMap ? mkRegexMapper(regexMap) : null,
      data: (_, { data: { uploadDefinition } }) => mkFieldStates(uploadDefinition),
      pristineData: (_, { data: { uploadDefinition } }) => mkFieldStates(uploadDefinition),
      repoSettings: (_, { data: { repoSettings } }) => repoSettings,
      multipleUpload: (_, { data: { uploadDefinition: { multipleUpload } } }) => multipleUpload
    }),

    // CLM-39871: when the saveData service throws a HOSTED_ENFORCEMENT-tagged error,
    // suppress the generic RSC "An error occurred saving data" banner by clearing
    // saveError, and surface the parsed payload via a dedicated context field that
    // UploadDetails.jsx renders as the policy-specific banner.
    // For any other error, behave identically to the default FormUtils.setSaveError.
    setSaveError: assign({
      saveErrorData: ({data}) => data,
      saveError: (_, event) => {
        const message = event?.data?.message;
        if (typeof message === 'string' && message.startsWith(HOSTED_ENFORCEMENT_ERROR_PREFIX)) {
          // Hide the raw tagged string from the generic banner — our custom banner renders it.
          return null;
        }
        return FormUtils.extractSaveErrorMessage(event);
      },
      saveErrors: (_, event) => {
        const data = event.data?.response?.data;
        if (Array.isArray(data)) {
          const saveErrors = {};
          data.forEach(({id, message}) => {
            if (id) {
              saveErrors[id.replace(/^field:/, '').replace(/^parameter:/, '').replace(/^helperBean\./, '')] =
                  message;
            }
          });
          return saveErrors;
        }
        return {};
      },
      // CLM-39871: route through the exported parseHostedEnforcementError so the
      // prefix-strip + JSON.parse logic lives in one place; previously this re-
      // implemented the same parser inline and would silently drift if the
      // prefix or schema changed.
      hostedEnforcementError: (_, event) => parseHostedEnforcementError(event?.data?.message)
    }),

    clearSaveError: assign({
      saveErrorData: () => ({}),
      saveError: () => undefined,
      saveErrors: () => ({}),
      hostedEnforcementError: () => null
    }),

    addAsset: assign({
      data: context => {
        const { data, assetFields } = context,
            maxAssetId = getMaxAssetId(data),
            newAssetId = maxAssetId + 1,
            newAssetState = mkAssetFieldStates(assetFields);

        return set(lensProp(`asset${newAssetId}`), newAssetState, data);
      }
    }),

    deleteAsset: assign({
      data: ({ data }, { assetKey }) => {
        const assetIdToRemove = getAssetId(assetKey),

            // For each k,v pair from the old data object, do the following
            // 1. if it's not an asset key, pass it through
            // 2. if it's an asset key with a number lower than the one being removed, pass it through
            // 3. if it's the asset key being removed, null out the key so it will be removed
            // 4. otherwise, it must be an asset key with a value greater than the one being removed, decrement
            //    the key value
            dataKeyMapper = ([k, v]) => {
              const assetId = getAssetId(k),
                  newKey = assetId == null || assetId < assetIdToRemove ? k :
                      assetId === assetIdToRemove ? null :
                      `asset${assetId - 1}`;

              return [newKey, v];
            },
            isKeyNil = pipe(head, isNil),
            dataAsPairs = toPairs(data),
            dataAsNewPairs = reject(isKeyNil, map(dataKeyMapper, dataAsPairs));

        return fromPairs(dataAsNewPairs);
      },
      isTouched: ({ isTouched }, { assetKey }) => dissoc(assetKey, isTouched)
    }),

    validate: assign({
      validationErrors: ({ data, componentFieldsByGroup, assetFields, hasPomExtension }) => {
        const assetKeys = filter(test(ASSET_NUM_MATCHER), keys(data)),
            requiredFields = getRequiredFieldNames(componentFieldsByGroup, assetFields, assetKeys, hasPomExtension),
            fieldValidationErrors = applyToPaths(requiredFields, ValidationUtils.validateNotBlank, data),

            fileUploadFullNamePaths = map(pair(__, COMPOUND_FIELD_PARENT_NAME), assetKeys),
            fileUploadValidationErrors = applyToPaths(fileUploadFullNamePaths, fileValidator, data),

            assetKeysWithoutFieldValidationErrors = getAssetKeysWithoutValidationErrors(fieldValidationErrors),
            assetFieldUniquenessValidationErrors =
                checkAssetFieldUniqueness(data, assetKeysWithoutFieldValidationErrors),

            validationErrors = mergeDeepRight(
                mergeDeepWith(combineValidationErrors, fieldValidationErrors, fileUploadValidationErrors),
                assetFieldUniquenessValidationErrors
            );

        return validationErrors;
      }
    }),

    processRegexMaps: assign((context, { name, value }) => {
      const { data, regexMapper, regexMapFields, isTouched } = context,
          [assetId, fieldName] = split('.', name);
      if (regexMapper && fieldName === COMPOUND_FIELD_PARENT_NAME) {
        const filename = value?.item(0)?.name;

        if (filename) {
          const assetIdLens = lensProp(assetId),
              regexResult = regexMapper(filename),
              touchedAssets = isEmpty(regexResult) ? {} : fromPairs(map(pair(__, true), regexMapFields));

          return {
            ...context,
            data: over(assetIdLens, mergeLeft(regexResult), data),
            isTouched: over(assetIdLens, mergeLeft(touchedAssets), isTouched)
          };
        }
        else {
          return context;
        }
      }
      else {
        return context;
      }
    }),

    setDisabled: setDisabledAction,
    postProcessData: setDisabledAction,

    onSaveSuccess: assign({
      // The string returned from the backend upload API
      savedComponentName: (_, { data }) => data
    })
  },
  services: {
    async fetchData({ repoId }) {
      const repoSettingsPromise = ExtAPIUtils.extAPIRequest(
          APIConstants.EXT.REPOSITORY.ACTION,
          APIConstants.EXT.REPOSITORY.METHODS.READ_REFERENCES,
          {}
      ).then(response => {
        const data = ExtAPIUtils.checkForErrorAndExtract(response),
            repoSettings = find(propEq('name', repoId), data);

        if (!repoSettings) {
          throw new Error(`Unable to find repository "${repoId}"`);
        }
        else {
          return repoSettings;
        }
      });

      const uploadDefinitionsPromise = ExtAPIUtils.extAPIRequest(
          APIConstants.EXT.UPLOAD.ACTION,
          APIConstants.EXT.UPLOAD.METHODS.GET_UPLOAD_DEFINITIONS
      ).then(response => {
        ExtAPIUtils.checkForError(response);
        return response;
      }).then(ExtAPIUtils.extractResult);

      const [uploadDefinitions, repoSettings] = await Promise.all([uploadDefinitionsPromise, repoSettingsPromise]),
          { format } = repoSettings,
          uploadDefinition = find(propEq('format', format), uploadDefinitions);

      if (repoSupportsUiUpload(uploadDefinitions, repoSettings)) {
        return { uploadDefinition, repoSettings };
      }
      else {
        throw new Error(`Repository "${repoId}" does not support upload through the web UI`);
      }
    },
    async saveData({ repoSettings: { name }, data, disabledFields }) {
      const formData = new FormData(),
          disabledFieldNames = map(head, filter(([_, isDisabled]) => isDisabled, toPairs(disabledFields))),

          // Disabled fields should not be included in the form submission
          dataToUpload = pickBy((_, key) => !includes(key, disabledFieldNames), data);

      for (const [key, value] of flattenDataForSubmit(dataToUpload)) {
        if (value instanceof FileList) {
          for (const file of Array.from(value)) {
            formData.append(key, file);
          }
        }
        else {
          formData.set(key, value);
        }
      }

      let postResponse;
      try {
        postResponse = await axios.post(
            `${APIConstants.REST.INTERNAL.UPLOAD}${encodeURIComponent(name)}`,
            formData);
      }
      catch (err) {
        // CLM-39871: the synchronous hosted-repository enforcement path returns
        // HTTP 403 / 503 with a structured JSON body. Preserve that body so the
        // view layer can render a policy-specific banner.
        const enforcementPayload = extractEnforcementPayload(err?.response);
        if (enforcementPayload) {
          // The payload is encoded into the Error message string with the
          // HOSTED_ENFORCEMENT_ERROR_PREFIX so the existing FormUtils-shaped
          // saveError pipeline carries it through unchanged. setSaveError
          // re-parses it via parseHostedEnforcementError(); a typed sibling
          // field on the Error would be dead weight today.
          throw new Error(HOSTED_ENFORCEMENT_ERROR_PREFIX + JSON.stringify(enforcementPayload));
        }
        // NEXUS-53344: UploadResource now returns HTTP 400 (instead of 200 with
        // success=false) for deployment-policy / duplicate-asset rejections. The
        // body is still the same ExtJS-RPC envelope, so unwrap its message here
        // and forward through the standard saveError path that the 200 case below
        // also uses. Other axios failures (network, 5xx, etc.) keep propagating.
        const envelopeMessage = err?.response?.data?.[0]?.message;
        if (envelopeMessage) {
          throw new Error(envelopeMessage);
        }
        throw err;
      }

      // This REST API reports errors in a weird way: 200 OK with an array whose
      // first element has success=false and a message string. CLM-39871: for the
      // UI path, enforcement rejections arrive here with the HOSTED_ENFORCEMENT::
      // prefix already embedded in that message by the backend, so the tagged
      // error simply forwards it — parseHostedEnforcementError() in the view layer
      // then unpacks it.
      if (postResponse.data?.success !== true) {
        const rawMessage = postResponse.data?.[0]?.message ?? 'Unknown Error';
        throw new Error(rawMessage);
      }

      // The Search page's backing data gets updated asynchronously and isn't necessarily
      // available yet just because the REST call above returned. Wait in the hopes that
      // it will be available by the time we redirect. Note that this wait is in addition to the wait
      // while the "Success" submit mask is shown.
      await Utils.timeoutPromise(500);

      return postResponse.data.data;
    },
    async waitAndRedirect({ savedComponentName, repoSettings }) {
      await Utils.timeoutPromise(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

      const searchCriteria = `keyword="${savedComponentName}" AND repository_name=${repoSettings.name}`;
      window.location.hash = `browse/search/generic=${encodeURIComponent(searchCriteria)}`;
    }
  }
});
