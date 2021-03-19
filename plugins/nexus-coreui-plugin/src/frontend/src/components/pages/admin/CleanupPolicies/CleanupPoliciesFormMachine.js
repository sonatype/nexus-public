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

import {ExtJS, Utils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const baseUrl = '/service/rest/internal/cleanup-policies';
const url = (name) => `${baseUrl}/${name}`;

function isEdit({name}) {
  return Utils.notBlank(name);
}

function validateNameField(field) {
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

function validateCriteriaNumberField(enabled, field) {
  if (enabled) {
    if (Utils.isBlank(field)) {
      return UIStrings.ERROR.FIELD_REQUIRED;
    }
    else if (isNaN(field)) {
      return UIStrings.ERROR.NAN;
    }
    else if (parseInt(field) < 1) {
      return UIStrings.ERROR.MIN(1);
    }
    else if (parseInt(field) > 24855) {
      return UIStrings.ERROR.MAX(24855);
    }
  }

  return null;
}

export default Utils.buildFormMachine({
  id: 'CleanupPoliciesFormMachine',
  initial: 'loadingFormatCriteria',
  config: (config) => ({
    ...config,
    states: {
      ...config.states,
      loadingFormatCriteria: {
        invoke: {
          id: 'fetchCriteriaByFormat',
          src: 'fetchCriteriaByFormat',
          onDone: {
            target: 'loading',
            actions: ['setCriteriaByFormatData']
          },
          onError: {
            target: 'loadError',
            actions: ['setLoadError', 'logLoadError']
          }
        }
      },
      loaded: {
        ...config.states.loaded,
        on: {
          ...config.states.loaded.on,
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
        name: validateNameField(data.name),
        format: Utils.isBlank(data.format) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        criteriaLastDownloaded: validateCriteriaNumberField(criteriaLastDownloadedEnabled, data.criteriaLastDownloaded),
        criteriaLastBlobUpdated: validateCriteriaNumberField(criteriaLastBlobUpdatedEnabled, data.criteriaLastBlobUpdated),
        criteriaReleaseType: criteriaReleaseTypeEnabled && Utils.isBlank(data.criteriaReleaseType) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        criteriaAssetRegex: criteriaAssetRegexEnabled && Utils.isBlank(data.criteriaAssetRegex) ? UIStrings.ERROR.FIELD_REQUIRED : null
      })
    }),
    setCriteriaByFormatData: assign({
      criteriaByFormat: (_, event) => event.data?.data
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
    postProcessData: assign({
      criteriaLastDownloadedEnabled: (_, event) => event.data?.data.criteriaLastDownloaded,
      criteriaLastBlobUpdatedEnabled: (_, event) => event.data?.data.criteriaLastBlobUpdated,
      criteriaReleaseTypeEnabled: (_, event) => event.data?.data.criteriaReleaseType,
      criteriaAssetRegexEnabled: (_, event) => event.data?.data.criteriaAssetRegex
    }),

    onDeleteError: ({data}) => ExtJS.showErrorMessage(UIStrings.CLEANUP_POLICIES.MESSAGES.DELETE_ERROR(data.name))
  },
  guards: {
    isEdit: ({pristineData}) => isEdit(pristineData),
    canDelete: () => true
  },
  services: {
    fetchCriteriaByFormat: () => {
      return Axios.get('/service/rest/internal/cleanup-policies/criteria/formats');
    },
    fetchData: ({pristineData}) => {
      if (isEdit(pristineData)) {
        return Axios.get(url(pristineData.name));
      }
      else { // New
        return Promise.resolve({
          data: {
            name: '',
            notes: '',
            format: '',
            inUseCount: 0
          }
        });
      }
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
