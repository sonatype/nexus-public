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
import { mergeDeepRight } from 'ramda';

import {ExtJS, FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const EMPTY_DATA = {
  name: '',
  notes: '',
  format: '',
  inUseCount: 0
};

const baseUrl = '/service/rest/internal/cleanup-policies';
const url = (name) => `${baseUrl}/${name}`;

function isEdit({name}) {
  return ValidationUtils.notBlank(name);
}

function validateCriteriaNumberField(enabled, field) {
  if (enabled) {
    if (ValidationUtils.isBlank(field)) {
      return UIStrings.ERROR.FIELD_REQUIRED;
    }
    else {
      return ValidationUtils.isInRange({value: field, min:1, max:24855, allowDecimals:false});
    }
  }

  return null;
}

export default FormUtils.buildFormMachine({
  id: 'CleanupPoliciesFormMachine',
  config: (config) => mergeDeepRight(config,{
    states: {
      loaded: {
        on: {
          SET_CRITERIA_LAST_DOWNLOADED_ENABLED: {
            target: 'loaded',
            actions: ['setCriteriaLastDownloadedEnabled']
          },
          SET_CRITERIA_LAST_BLOB_UPDATED_ENABLED: {
            target: 'loaded',
            actions: ['setCriteriaLastBlobUpdatedEnabled']
          },
          SET_CRITERIA_RELEASE_TYPE_ENABLED: {
            target: 'loaded',
            actions: ['setCriteriaReleaseTypeEnabled']
          },
          SET_CRITERIA_ASSET_REGEX_ENABLED: {
            target: 'loaded',
            actions: ['setCriteriaAssetRegexEnabled']
          },
          UPDATE: {
            actions: [...config.states.loaded.on.UPDATE.actions, 'clearCriteria']
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
      validationErrors: ({data, criteriaLastDownloadedEnabled, criteriaLastBlobUpdatedEnabled, criteriaReleaseTypeEnabled, criteriaAssetRegexEnabled}) => ({
        name: ValidationUtils.validateNameField(data.name),
        format: ValidationUtils.isBlank(data.format) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        criteriaLastDownloaded: validateCriteriaNumberField(criteriaLastDownloadedEnabled, data.criteriaLastDownloaded),
        criteriaLastBlobUpdated: validateCriteriaNumberField(criteriaLastBlobUpdatedEnabled, data.criteriaLastBlobUpdated),
        criteriaReleaseType: criteriaReleaseTypeEnabled && ValidationUtils.isBlank(data.criteriaReleaseType) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        criteriaAssetRegex: criteriaAssetRegexEnabled && ValidationUtils.isBlank(data.criteriaAssetRegex) ? UIStrings.ERROR.FIELD_REQUIRED : null
      })
    }),
    setCriteriaLastDownloadedEnabled: assign({
      criteriaLastDownloadedEnabled: (_, {checked}) => checked
    }),
    setCriteriaLastBlobUpdatedEnabled: assign({
      criteriaLastBlobUpdatedEnabled: (_, {checked}) => checked
    }),
    setCriteriaReleaseTypeEnabled: assign({
      criteriaReleaseTypeEnabled: (_, {checked}) => checked
    }),
    setCriteriaAssetRegexEnabled: assign({
      criteriaAssetRegexEnabled: (_, {checked}) => checked
    }),
    postProcessData: assign((_, {data: [, details]}) => ({
      criteriaLastDownloadedEnabled: details?.data?.criteriaLastDownloaded,
      criteriaLastBlobUpdatedEnabled: details?.data?.criteriaLastBlobUpdated,
      criteriaReleaseTypeEnabled: details?.data?.criteriaReleaseType,
      criteriaAssetRegexEnabled: details?.data?.criteriaAssetRegex
    })),

    clearCriteria: assign((ctx, event) => {
      if (event.data.format !== ctx.format) {
        return {
          ...ctx,
          data: {
            ...ctx.data,
            criteriaLastBlobUpdated: null,
            criteriaLastDownloaded: null,
            criteriaReleaseType: null,
            criteriaAssetRegex: null
          },
          criteriaLastDownloadedEnabled: false,
          criteriaLastBlobUpdatedEnabled: false,
          criteriaReleaseTypeEnabled: false,
          criteriaAssetRegexEnabled: false
        };
      }
      return ctx;
    }),
    setData: assign((_, {data: [criteria, details]}) => ({
      criteriaByFormat: criteria?.data,
      data: details?.data,
      pristineData: details?.data,
    })),
    onDeleteError: ({data}) => ExtJS.showErrorMessage(UIStrings.CLEANUP_POLICIES.MESSAGES.DELETE_ERROR(data.name)),
  },
  guards: {
    isEdit: ({pristineData}) => isEdit(pristineData),
    canDelete: () => true
  },
  services: {
    fetchData: ({pristineData}) => {
      return Axios.all([
        Axios.get('/service/rest/internal/cleanup-policies/criteria/formats'),
        isEdit(pristineData)
            ? Axios.get(url(pristineData.name))
            : Promise.resolve({data: EMPTY_DATA}),
      ]);
    },
    saveData: ({data, pristineData}) => {
      if (isEdit(pristineData)) {
        return Axios.put(url(data.name), {
          name: data.name,
          notes: data.notes,
          format: data.format,
          criteriaLastBlobUpdated: data.criteriaLastBlobUpdated,
          criteriaLastDownloaded: data.criteriaLastDownloaded,
          criteriaReleaseType: data.criteriaReleaseType,
          criteriaAssetRegex: data.criteriaAssetRegex
        });
      }
      else { // New
        return Axios.post(baseUrl, {
          name: data.name,
          notes: data.notes,
          format: data.format,
          criteriaLastBlobUpdated: data.criteriaLastBlobUpdated,
          criteriaLastDownloaded: data.criteriaLastDownloaded,
          criteriaReleaseType: data.criteriaReleaseType,
          criteriaAssetRegex: data.criteriaAssetRegex
        });
      }
    },

    confirmDelete: ({data}) => ExtJS.requestConfirmation({
      title: UIStrings.CLEANUP_POLICIES.MESSAGES.CONFIRM_DELETE.TITLE,
      message: UIStrings.CLEANUP_POLICIES.MESSAGES.CONFIRM_DELETE.MESSAGE(data.inUseCount),
      yesButtonText: UIStrings.CLEANUP_POLICIES.MESSAGES.CONFIRM_DELETE.YES,
      noButtonText: UIStrings.CLEANUP_POLICIES.MESSAGES.CONFIRM_DELETE.NO
    }),

    delete: ({data}) => Axios.delete(url(data.name))
  }
});
