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

import {mergeDeepRight} from 'ramda';

import {FormUtils, ValidationUtils, ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

import {getDefaultValues, getValidators} from './RepositoryFormConfig';

const {CONFIRM_DELETE, DELETE_ERROR, DELETE_SUCCESS} = UIStrings.REPOSITORIES.EDITOR.MESSAGES;

const {INTERNAL, PUBLIC} = APIConstants.REST;

export const saveRepositoryUrl = (format, type, name) =>
  `${PUBLIC.REPOSITORIES}${encodeURIComponent(formatFormat(format))}/${encodeURIComponent(
    type
  )}/${name ? encodeURIComponent(name) : ''}`;
export const getRepositoryUrl = (name) => INTERNAL.REPOSITORIES_REPOSITORY + name;
export const deleteRepositoryUrl = (name) => PUBLIC.REPOSITORIES + name;

export default FormUtils.buildFormMachine({
  id: 'RepositoriesFormMachine',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        loaded: {
          on: {
            RESET_DATA: {
              actions: ['resetData'],
              target: 'loaded'
            },
            SET_DEFAULT_BLOB_STORE: {
              cond: 'hasNoBlobStoreName',
              target: 'loaded',
              actions: ['update'],
              internal: false
            },
            UPDATE_PREEMPTIVE_PULL: {
              target: 'loaded',
              actions: ['updatePreemptivePull']
            },
          }
        }
      }
    })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        ...getValidators(data.format, data.type)(data),
        name: ValidationUtils.validateNameField(data.name),
        format: ValidationUtils.validateNotBlank(data.format),
        type: ValidationUtils.validateNotBlank(data.type),
        storage: {
          blobStoreName: ValidationUtils.validateNotBlank(data.storage?.blobStoreName)
        }
      })
    }),
    resetData: assign({
      data: (_, {format, repoType}) => ({
        ...getDefaultValues(format, repoType),
        format
      })
    }),
    updatePreemptivePull: assign({
      data: ({data}, {checked}) => ({
        ...data,
        replication: {
          preemptivePullEnabled: checked,
          assetPathRegex: !checked ? null : data.replication.assetPathRegex
        }
      })
    }),
    onDeleteError: ({data}, event) => {
      const errorDetails = event.data?.message || '';
      ExtJS.showErrorMessage(DELETE_ERROR(data.name) + errorDetails);
    },
    logDeleteSuccess: ({data}) => {
      ExtJS.showSuccessMessage(DELETE_SUCCESS(data.name));
    },
  },
  guards: {
    hasNoBlobStoreName: ({data}) => ValidationUtils.isBlank(data.storage?.blobStoreName),
    canDelete: () => true
  },
  services: {
    fetchData: async ({pristineData}) => {
      if (isEdit(pristineData)) {
        const response = await Axios.get(getRepositoryUrl(pristineData.name));
        return mergeDeepRight(response, {
          data: {
            routingRule: response.data.routingRuleName
          }
        });
      } else {
        return {data: {name: ''}};
      }
    },
    saveData: ({data, pristineData}) => {
      const {format, type} = data;
      const {name} = pristineData;
      const payload = data;
      return isEdit(pristineData)
        ? Axios.put(saveRepositoryUrl(format, type, name), payload)
        : Axios.post(saveRepositoryUrl(format, type), payload);
    },
    confirmDelete: ({data}) =>
      ExtJS.requestConfirmation({
        title: CONFIRM_DELETE.TITLE,
        message: CONFIRM_DELETE.MESSAGE(data.name),
        yesButtonText: CONFIRM_DELETE.YES,
        noButtonText: CONFIRM_DELETE.NO
      }),
    delete: ({data}) => Axios.delete(deleteRepositoryUrl(data.name))
  }
});

const isEdit = ({name}) => ValidationUtils.notBlank(name);

const formatFormat = (format) => (format === 'maven2' ? 'maven' : format);
