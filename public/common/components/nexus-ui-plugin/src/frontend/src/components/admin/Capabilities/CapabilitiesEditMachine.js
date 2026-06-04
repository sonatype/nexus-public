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
import Axios from 'axios';
import FormUtils from '../../../interface/FormUtils';
import ExtAPIUtils from '../../../interface/ExtAPIUtils';
import APIConstants from '../../../constants/APIConstants';
import ValidationUtils from '../../../interface/ValidationUtils';
import UIStrings from '../../../constants/UIStrings';
import { mergeDeepRight } from 'ramda';

const { ACTION, METHODS } = APIConstants.EXT.CAPABILITY;

const CapabilitiesEditMachine = FormUtils.buildFormMachine({
  id: 'CapabilitiesEditMachine',
  config: config =>
    mergeDeepRight(config, {
      context: {
        id: null,
        capability: null,
        types: [],
        capabilityType: null,
        deleteError: null,
      },
      states: {
        ...config.states,
        loaded: {
          ...config.states.loaded,
          on: {
            ...config.states.loaded.on,
            SAVE: [
              {
                target: 'saving',
                cond: 'canSave',
              },
              {
                target: 'loaded',
                actions: ['touchAllFields', 'validate'],
              },
            ],
            SHOW_DELETE_MODAL: {
              target: 'awaitingDeleteConfirmation',
            },
          },
        },
        awaitingDeleteConfirmation: {
          on: {
            CONFIRM_DELETE: {
              target: 'confirmDelete',
              cond: 'canDelete',
            },
            CANCEL_DELETE: {
              target: 'loaded',
            },
          },
        },
        confirmDelete: {
          invoke: {
            src: 'delete',
            onDone: {
              target: 'ended',
              actions: ['onDeleteSuccess'],
            },
            onError: {
              target: 'loaded',
              actions: ['setDeleteError'],
            },
          },
        },
      },
    }),
  options: options =>
    mergeDeepRight(options, {
      actions: {
        ...options.actions,
        setData: assign({
          capability: (_, event) => event.data?.capability || null,
          types: (_, event) => event.data?.types || [],
          capabilityType: (_, event) => event.data?.capabilityType || null,
          data: (_, event) => event.data?.data || {},
          pristineData: (_, event) => event.data?.data || {},
        }),
        setDeleteError: assign({
          deleteError: (_, event) => event.data?.message || 'An error occurred deleting the capability.',
        }),
        // Override base logLoadError: this machine surfaces load errors via the
        // loadError state rendered inline in CapabilitiesEdit, so the generic
        // toast notification would double-report the same failure.
        logLoadError: () => {},
        validate: assign({
          validationErrors: ({ data, capabilityType, isTouched }) => {
            const fullErrors = {};

            if (!capabilityType) {
              return {};
            }

            const fields = capabilityType.formFields || [];
            const currentFieldIds = new Set(fields.map(f => f.id));

            fields.forEach(field => {
              const value = data?.[field.id];

              if (field.required) {
                if (field.type === 'itemselect') {
                  if (!value || (Array.isArray(value) && value.length === 0)) {
                    fullErrors[field.id] = UIStrings.ERROR.FIELD_REQUIRED;
                    return;
                  }
                } else if (ValidationUtils.isBlank(value)) {
                  fullErrors[field.id] = UIStrings.ERROR.FIELD_REQUIRED;
                  return;
                }
              }

              if (field.type === 'number' && value !== null && value !== undefined && value !== '') {
                const min = field.minValue ?? Number.NEGATIVE_INFINITY;
                const max = field.maxValue ?? Number.POSITIVE_INFINITY;
                const numberError = ValidationUtils.isInRange({ value, min, max, allowDecimals: false });
                if (numberError) {
                  fullErrors[field.id] = numberError;
                  return;
                }
              }

              if (field.type === 'url' && value) {
                const urlError = ValidationUtils.validateIsUrl(value);
                if (urlError) {
                  fullErrors[field.id] = urlError;
                  return;
                }
              }
            });

            // Only show errors for touched fields that belong to the current type
            const visibleErrors = {};
            Object.entries(fullErrors).forEach(([key, value]) => {
              if (currentFieldIds.has(key) && isTouched?.[key]) {
                visibleErrors[key] = value;
              }
            });

            return visibleErrors;
          },
        }),
        touchAllFields: assign(({ capabilityType, isTouched = {} }) => {
          const updatedTouched = { ...isTouched };
          const fields = capabilityType?.formFields || [];
          fields.forEach(f => {
            updatedTouched[f.id] = true;
          });
          return { isTouched: updatedTouched };
        }),
      },
      guards: {
        ...options.guards,
        canSave: ({ isPristine, validationErrors, capabilityType, data }) => {
          const hasFields = capabilityType?.formFields && capabilityType.formFields.length > 0;

          let hasValidationErrors = false;
          if (hasFields && capabilityType.formFields) {
            for (const field of capabilityType.formFields) {
              if (field.required) {
                const value = data?.[field.id];
                if (field.type === 'itemselect') {
                  if (!value || (Array.isArray(value) && value.length === 0)) {
                    hasValidationErrors = true;
                    break;
                  }
                } else if (ValidationUtils.isBlank(value)) {
                  hasValidationErrors = true;
                  break;
                }
              }
            }
          }

          const isValid = !FormUtils.isInvalid(validationErrors) && !hasValidationErrors;

          if (!hasFields && capabilityType) {
            return isValid;
          }

          const allFieldsOptional =
            hasFields && capabilityType.formFields.every(field => field.required !== true || field.readOnly === true);

          if (allFieldsOptional) {
            return isValid;
          }

          const allRequiredFieldsHaveValues =
            hasFields &&
            capabilityType.formFields
              .filter(field => field.required === true && field.readOnly !== true)
              .every(field => {
                const value = data?.[field.id];
                if (field.type === 'itemselect') {
                  return value && (Array.isArray(value) ? value.length > 0 : true);
                }
                return !ValidationUtils.isBlank(value);
              });

          if (isValid && allRequiredFieldsHaveValues) {
            return true;
          }

          return !isPristine && isValid;
        },
        canDelete: () => true,
      },
      services: {
        ...options.services,
        fetchData: fetchCapabilityAndTypes,
        saveData: saveCapability,
        delete: deleteCapability,
      },
    }),
});

async function fetchCapabilityAndTypes(context) {
  const id = context.id;
  if (!id) {
    return Promise.reject(new Error('Missing capability id'));
  }

  const [capabilitiesResponse, typesResponse] = await Promise.all([
    ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ).then(ExtAPIUtils.checkForErrorAndExtract),
    ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ_TYPES).then(ExtAPIUtils.checkForErrorAndExtract),
  ]);

  const capability = capabilitiesResponse.find(c => String(c.id) === String(id));
  if (!capability) {
    throw new Error('Capability not found');
  }

  const capabilityType = typesResponse.find(t => t.id === capability.typeId);

  // Initialize data from capability.properties and enabled state
  // Transform properties to convert itemselect strings to arrays
  const properties = { ...(capability.properties || {}) };
  if (capabilityType?.formFields) {
    capabilityType.formFields.forEach(field => {
      if (field.type === 'itemselect' && properties[field.id] !== undefined) {
        const value = properties[field.id];
        // Convert comma-separated string to array if it's a string
        if (typeof value === 'string' && value !== '') {
          properties[field.id] = value.split(',');
        } else if (value === '') {
          // Empty string should be an empty array
          properties[field.id] = [];
        }
      }
    });
  }

  const data = {
    ...properties,
    enabled: capability.enabled !== undefined ? capability.enabled : true,
  };
  const pristineData = { ...data };

  return {
    capability,
    types: typesResponse,
    capabilityType,
    data,
    pristineData,
  };
}

function saveCapability({ data, capability, capabilityType }) {
  // Extract enabled and notes from data, keep the rest as properties
  const { enabled, notes, ...properties } = data || {};

  // Transform properties to match ExtJS format
  // itemselect fields need to be converted from arrays to strings
  const transformedProperties = { ...properties };
  if (capabilityType?.formFields) {
    capabilityType.formFields.forEach(field => {
      if (field.type === 'itemselect' && transformedProperties[field.id] !== undefined) {
        const value = transformedProperties[field.id];
        if (Array.isArray(value)) {
          // Convert array to string - join with comma if multiple items, or use single item
          // This matches ExtJS ItemSelector behavior when valueAsString is true
          transformedProperties[field.id] = value.join(',');
        }
      }
    });
  }

  const payload = {
    id: capability.id,
    typeId: capability.typeId,
    enabled: enabled !== undefined ? enabled : capability.enabled,
    notes: notes !== undefined ? notes : capability.notes || '',
    properties: transformedProperties || {},
  };

  return ExtAPIUtils.extAPIRequest(ACTION, METHODS.UPDATE, { data: [payload] }).then(response => {
    ExtAPIUtils.checkForError(response);
    return ExtAPIUtils.extractResult(response, {});
  });
}

function deleteCapability(context) {
  const id = context.id;

  if (!id) {
    return Promise.reject(new Error('Missing capability id'));
  }

  const url = `${APIConstants.REST.PUBLIC.BASE_URL}capabilities/${id}`;
  return Axios.delete(url);
}

export default CapabilitiesEditMachine;
