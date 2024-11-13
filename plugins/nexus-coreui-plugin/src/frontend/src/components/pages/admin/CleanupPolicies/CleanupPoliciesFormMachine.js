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
import {isEmpty, mergeDeepRight} from 'ramda';
import {URL, isReleaseType} from './CleanupPoliciesHelper';
import {ExtJS, FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {CLEANUP_POLICIES: LABELS} = UIStrings;

const EMPTY_DATA = {
  name: '',
  notes: '',
  format: '',
  inUseCount: 0,
};

const retainSortByFormat = new Map([
  ['maven2', LABELS.EXCLUSION_CRITERIA.SORT_BY.VERSION.id],
  ['docker', LABELS.EXCLUSION_CRITERIA.SORT_BY.DATE.id]
]);

const NOTES_FIELD_MAX_LENGTH = 400;

function isEdit({name}) {
  return ValidationUtils.notBlank(name);
}

function validateCriteriaNumberField(enabled, field) {
  if (enabled) {
    if (ValidationUtils.isBlank(field)) {
      return UIStrings.ERROR.FIELD_REQUIRED;
    } else {
      return ValidationUtils.isInRange({
        value: field,
        min: 1,
        max: 24855,
        allowDecimals: false,
      });
    }
  }

  return null;
}

function validateCriteriaSelected({
  criteriaLastDownloaded,
  criteriaLastBlobUpdated,
  criteriaAssetRegex,
  retain
}) {
  if (!criteriaLastDownloaded && !criteriaLastBlobUpdated && !criteriaAssetRegex && !retain) {
    return LABELS.MESSAGES.NO_CRITERIA_ERROR;
  }

  return null;
}

export default FormUtils.buildFormMachine({
  id: 'CleanupPoliciesFormMachine',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        loaded: {
          on: {
            SET_CRITERIA_LAST_DOWNLOADED_ENABLED: {
              target: 'loaded',
              actions: 'setCriteriaLastDownloadedEnabled',
            },
            SET_CRITERIA_LAST_BLOB_UPDATED_ENABLED: {
              target: 'loaded',
              actions: 'setCriteriaLastBlobUpdatedEnabled',
            },
            SET_CRITERIA_ASSET_REGEX_ENABLED: {
              target: 'loaded',
              actions: 'setCriteriaAssetRegexEnabled',
            },
            SET_EXCLUSION_CRITERIA_ENABLED: {
              target: 'loaded',
              actions: 'setExclusionCriteriaEnabled',
            },
            UPDATE_RETAIN: {
              target: 'loaded',
              actions: ['update', 'setCachedRetain'],
            },
            UPDATE_RELEASE_TYPE: {
              target: 'loaded',
              actions: 'setReleaseType',
            },
            UPDATE: {
              actions: [
                ...config.states.loaded.on.UPDATE.actions,
                'clearCriteria',
              ],
            },
          },
        },
      },
    }),
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({
        data,
        criteriaLastDownloadedEnabled,
        criteriaLastBlobUpdatedEnabled,
        criteriaAssetRegexEnabled,
        exclusionCriteriaEnabled,
      }) => ({
        name: ValidationUtils.validateNameField(data.name),
        format: ValidationUtils.validateNotBlank(data.format),
        notes: ValidationUtils.validateLength(data.notes, NOTES_FIELD_MAX_LENGTH),
        criteriaLastDownloaded: validateCriteriaNumberField(
          criteriaLastDownloadedEnabled,
          data.criteriaLastDownloaded
        ),
        criteriaLastBlobUpdated: validateCriteriaNumberField(
          criteriaLastBlobUpdatedEnabled,
          data.criteriaLastBlobUpdated
        ),
        criteriaAssetRegex:
          criteriaAssetRegexEnabled &&
          ValidationUtils.validateNotBlank(data.criteriaAssetRegex),
        retain: validateCriteriaNumberField(
          exclusionCriteriaEnabled,
          data.retain
        ),
        criteriaSelected: validateCriteriaSelected(data),
      }),
    }),
    setCriteriaLastDownloadedEnabled: assign({
      criteriaLastDownloadedEnabled: (_, {checked}) => checked,
      exclusionCriteriaEnabled: ({
        criteriaLastBlobUpdatedEnabled,
        criteriaAssetRegexEnabled,
        exclusionCriteriaEnabled
      }, { checked }) => {
        if (criteriaLastBlobUpdatedEnabled || criteriaAssetRegexEnabled || checked) {
          return exclusionCriteriaEnabled;
        } 
        return false;
      },
      isTouched: ({isTouched}) =>
          mergeDeepRight(isTouched, {criteriaLastDownloaded:  true}),
      data: ({data, criteriaLastBlobUpdatedEnabled, criteriaAssetRegexEnabled}, {checked}) =>
        mergeDeepRight(data, {
          criteriaLastDownloaded: checked ? data.criteriaLastDownloaded : null,
          retain: !checked || !criteriaLastBlobUpdatedEnabled || !criteriaAssetRegexEnabled ? null : data.retain,
        }),
    }),
    setCriteriaLastBlobUpdatedEnabled: assign({
      criteriaLastBlobUpdatedEnabled: (_, {checked}) => checked,
      exclusionCriteriaEnabled: ({
        criteriaLastDownloadedEnabled,
        criteriaAssetRegexEnabled,
        exclusionCriteriaEnabled
      }, {checked}) => {
        if (criteriaLastDownloadedEnabled || criteriaAssetRegexEnabled || checked){
          return exclusionCriteriaEnabled;
        } 
        return false;
      },
      isTouched: ({isTouched}) =>
          mergeDeepRight(isTouched, {criteriaLastBlobUpdated:  true}),
      data: ({data, criteriaLastDownloadedEnabled, criteriaAssetRegexEnabled}, {checked}) =>
        mergeDeepRight(data, {
          criteriaLastBlobUpdated: checked ? data.criteriaLastBlobUpdated : null,
          retain: !checked || !criteriaLastDownloadedEnabled || !criteriaAssetRegexEnabled ? null : data.retain,
        }),
    }),
    setCriteriaAssetRegexEnabled: assign({
      criteriaAssetRegexEnabled: (_, {checked}) => checked,
      exclusionCriteriaEnabled: ({
        criteriaLastDownloadedEnabled,
        criteriaLastBlobUpdatedEnabled,
        exclusionCriteriaEnabled
      }, {checked}) => {
        if (criteriaLastDownloadedEnabled || criteriaLastBlobUpdatedEnabled || checked){
          return exclusionCriteriaEnabled;
        } 
        return false;
      },
      isTouched: ({isTouched}) =>
          mergeDeepRight(isTouched, {criteriaAssetRegex: true}),
      data: ({data, criteriaLastDownloadedEnabled, criteriaLastBlobUpdatedEnabled}, {checked}) =>
        mergeDeepRight(data, {
          criteriaAssetRegex: checked ? data.criteriaAssetRegex : null,
          retain: !checked || !criteriaLastBlobUpdatedEnabled || !criteriaLastDownloadedEnabled ? null : data.retain,
        }),
    }),
    setExclusionCriteriaEnabled: assign({
      exclusionCriteriaEnabled: (_, {checked}) => checked,
      isTouched: ({isTouched}) =>
          mergeDeepRight(isTouched, {retain: true}),
      data: ({data, cachedRetain}, {checked}) => 
        mergeDeepRight(data, {
          retain: checked ? cachedRetain : null,
          sortBy: checked ? retainSortByFormat.get(data.format) : null,
        }),
    }),
    setReleaseType: assign((ctx, {value}) => {
      const exclusionCriteriaEnabled =
        isReleaseType(value) && ctx.exclusionCriteriaEnabled;

      let data = {criteriaReleaseType: value};

      if (!exclusionCriteriaEnabled) {
        data.retain = null;
        data.sortBy = null;
      }

      return mergeDeepRight(ctx, {
        exclusionCriteriaEnabled,
        data,
      });
    }),
    postProcessData: assign((_, {data: [, details]}) => ({
      criteriaLastDownloadedEnabled: Boolean(
        details?.data?.criteriaLastDownloaded
      ),
      criteriaLastBlobUpdatedEnabled: Boolean(
        details?.data?.criteriaLastBlobUpdated
      ),
      criteriaAssetRegexEnabled: Boolean(details?.data?.criteriaAssetRegex),
      exclusionCriteriaEnabled:
        Boolean(details?.data?.retain) || Boolean(details?.data?.sortBy),
      exclusionSortBy: retainSortByFormat.get(details?.data?.format),
      cachedRetain: details.data.retain,
    })),
    clearCriteria: assign((ctx, event) => {
      if (event.name === 'format' && event.value !== ctx.format) {
        return mergeDeepRight(ctx, {
          data: {
            criteriaLastBlobUpdated: null,
            criteriaLastDownloaded: null,
            criteriaReleaseType: null,
            criteriaAssetRegex: null,
            retain: null,
            sortBy: null,
          },
          isTouched : {
            criteriaLastBlobUpdated: false,
            criteriaLastDownloaded: false,
            criteriaAssetRegex: false,
            retain: false
          },
          criteriaLastDownloadedEnabled: false,
          criteriaLastBlobUpdatedEnabled: false,
          criteriaAssetRegexEnabled: false,
          exclusionCriteriaEnabled: false,
          exclusionSortBy: retainSortByFormat.get(event.value),
        });
      }
      return ctx;
    }),
    setData: assign((_, {data: [criteria, details]}) => ({
      criteriaByFormat: criteria?.data,
      data: details?.data,
      pristineData: details?.data,
    })),
    onDeleteError: ({data}) =>
      ExtJS.showErrorMessage(LABELS.MESSAGES.DELETE_ERROR(data.name)),
    setCachedRetain: assign({
      cachedRetain: (_, event) => {
        return event.value
      }, 
    }),
    onLoadedEntry: assign({
      data: ({data}) => ({
        ...data,
        exclusionCriteriaEnabled:
        Boolean(data?.criteriaLastDownloaded) ||
        Boolean(data?.criteriaLastBlobUpdated) ||
        Boolean(data?.criteriaAssetRegex)
      }),
    }),
  },
  guards: {
    isEdit: ({pristineData}) => isEdit(pristineData),
    canDelete: () => true,
  },
  services: {
    fetchData: ({pristineData}) => {
      return Axios.all([
        Axios.get(`${URL.baseUrl}/criteria/formats`),
        isEdit(pristineData)
          ? Axios.get(URL.singleCleanupPolicyUrl(pristineData.name))
          : Promise.resolve({data: EMPTY_DATA}),
      ]);
    },
    saveData: ({data, pristineData}) => {
      const getCriteriaReleaseType = () => {
        if (isEmpty(data.criteriaReleaseType)) {
          return null;
        }

        return data.criteriaReleaseType;
      };

      const payload = {
        name: data.name,
        notes: data.notes,
        format: data.format,
        criteriaLastBlobUpdated: data.criteriaLastBlobUpdated,
        criteriaLastDownloaded: data.criteriaLastDownloaded,
        criteriaReleaseType: getCriteriaReleaseType(),
        criteriaAssetRegex: data.criteriaAssetRegex,
        retain: data.retain,
        sortBy: data.sortBy,
      };

      return isEdit(pristineData)
        ? Axios.put(URL.singleCleanupPolicyUrl(data.name), payload)
        : Axios.post(URL.baseUrl, payload);
    },

    confirmDelete: ({data}) =>
      ExtJS.requestConfirmation({
        title: LABELS.MESSAGES.CONFIRM_DELETE.TITLE,
        message: LABELS.MESSAGES.CONFIRM_DELETE.MESSAGE(data.inUseCount),
        yesButtonText: LABELS.MESSAGES.CONFIRM_DELETE.YES,
        noButtonText: LABELS.MESSAGES.CONFIRM_DELETE.NO,
      }),

    delete: ({data}) => Axios.delete(URL.singleCleanupPolicyUrl(data.name)),
  },
});
