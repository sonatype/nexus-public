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

const {DELETE_ERROR, DELETE_SUCCESS} = UIStrings.REPOSITORIES.EDITOR.MESSAGES;

const {INTERNAL, PUBLIC} = APIConstants.REST;

export const saveRepositoryUrl = (format, type, name) =>
  `${PUBLIC.REPOSITORIES}${encodeURIComponent(formatFormat(format))}/${encodeURIComponent(
    type
  )}/${name ? encodeURIComponent(name) : ''}`;
export const getRepositoryUrl = (name) => INTERNAL.REPOSITORIES_REPOSITORY + name;
export const deleteRepositoryUrl = (name) => PUBLIC.REPOSITORIES + name;
// No leading slash — Axios prepends baseURL (which includes the context path).
export const evaluationSettingsUrl = (repositoryId) =>
  `service/rest/v1/repositories/${encodeURIComponent(repositoryId)}/evaluation-settings`;

export default FormUtils.buildFormMachine({
  id: 'RepositoriesFormMachine',
  initial: 'loading',
  context: (context) => ({
    ...context,
    showDeleteModal: false
  }),
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
            CONFIRM_DELETE: {
              actions: assign({showDeleteModal: true})
            },
            HIDE_DELETE_MODAL: {
              actions: assign({showDeleteModal: false})
            },
            DELETE: {
              target: 'delete',
              actions: assign({showDeleteModal: false}),
              cond: 'canDelete'
            }
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

        // Fetch evaluation settings for hosted repositories if feature is enabled
        const isEvaluationEnabled = ExtJS.state().getValue('hostedRepositoryEvaluationEnabled');

        if (response.data.type === 'hosted' && isEvaluationEnabled) {
          try {
            const evalResponse = await Axios.get(evaluationSettingsUrl(response.data.name));
            // Store in attributes.evaluation to match ExtJS structure
            if (!response.data.attributes) {
              response.data.attributes = {};
            }
            // Defensive normalisation: the per-repo evaluation-settings resource is expected to
            // emit lowercase-kebab (e.g. "stage-release"), but matches the same defensive
            // normalisation used in EvaluationConfiguration.jsx for the global-settings GET.
            // Without this, any path that returns the DB format (e.g. "STAGE_RELEASE") would
            // not match any <option value> in the dropdown and the UI would render a stale
            // value or fall through to the first option.
            const rawStage = evalResponse.data.policyEvaluationStage;
            // Match EvaluationConfiguration.jsx's pattern: null/empty falls back to 'build'
            // so the dropdown always renders a matching <option>. Without this, an INHERIT-mode
            // repo with no override and no global setting would receive null, leaving the
            // select with no selection.
            const normalizedStage = rawStage
              ? rawStage.toLowerCase().replace(/_/g, '-')
              : 'build';
            response.data.attributes.evaluation = {
              mode: evalResponse.data.mode,
              activityTimeFrame: evalResponse.data.activityTimeFrame,
              artifactLatestVersions: evalResponse.data.artifactLatestVersions,
              policyEvaluationStage: normalizedStage
            };
          } catch (error) {
            if (error.response?.status === 404) {
              // No evaluation settings exist yet - use defaults from attributes or INHERIT mode
              if (!response.data.attributes) {
                response.data.attributes = {};
              }
              if (!response.data.attributes.evaluation) {
                response.data.attributes.evaluation = {
                  mode: 'INHERIT',
                  activityTimeFrame: null,
                  artifactLatestVersions: null,
                  policyEvaluationStage: null
                };
              }
            }
          }
        }

        return mergeDeepRight(response, {
          data: {
            routingRule: response.data.routingRuleName
          }
        });
      } else {
        return {data: {name: ''}};
      }
    },
    saveData: async ({data, pristineData}) => {
      const evaluation = data.attributes?.evaluation;
      const {format, type} = data;
      const {name} = pristineData;

      // Save repository with attributes.evaluation included
      const repoResponse = isEdit(pristineData)
        ? await Axios.put(saveRepositoryUrl(format, type, name), data)
        : await Axios.post(saveRepositoryUrl(format, type), data);

      // Also save evaluation settings via dedicated API for hosted repositories if feature is enabled
      const isEvaluationEnabled = ExtJS.state().getValue('hostedRepositoryEvaluationEnabled');

      if (type === 'hosted' && evaluation && isEvaluationEnabled) {
        const repositoryId = isEdit(pristineData) ? name : repoResponse.data.name;

        try {
          await Axios.put(evaluationSettingsUrl(repositoryId), {
            mode: evaluation.mode,
            activityTimeFrame: evaluation.activityTimeFrame || null,
            artifactLatestVersions: evaluation.artifactLatestVersions || null,
            policyEvaluationStage: evaluation.policyEvaluationStage || null
          });
        } catch (error) {
          // Don't fail the whole save if evaluation API call fails
          // The data is already saved in repository attributes
        }
      }

      return repoResponse;
    },
    delete: ({data}) => Axios.delete(deleteRepositoryUrl(data.name))
  }
});

const isEdit = ({name}) => ValidationUtils.notBlank(name);

const formatFormat = (format) => (format === 'maven2' ? 'maven' : format);
