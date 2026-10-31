/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import { createMachine, assign } from 'xstate';
import type {
  UploadFormData,
  ValidationErrors,
  AssetFieldData,
  UploadComponentField,
  UploadFieldDefinition,
  UploadDefinitionExtended,
} from '../upload.types';
import { UPLOAD_FORM_STRINGS } from '../upload.types';
import { restClient } from '../../../../../interface/api';
import { APIConstants } from '../../../../../constants/APIConstants';

// =============================================================================
// FIELD VALIDATION PATTERNS (ported from useUploadForm.ts:31-48)
// =============================================================================

const FIELD_VALIDATION: Record<string, { pattern: RegExp; message: string }> = {
  groupId: {
    pattern: /^[a-zA-Z0-9._-]+$/,
    message: 'Group ID must contain only letters, numbers, dots, hyphens, and underscores (e.g., com.example.myapp)',
  },
  artifactId: {
    pattern: /^[a-zA-Z0-9._-]+$/,
    message: 'Artifact ID must contain only letters, numbers, dots, hyphens, and underscores (e.g., my-library)',
  },
  version: {
    pattern: /^[a-zA-Z0-9._+-]+$/,
    message: 'Version must contain only letters, numbers, dots, hyphens, underscores, and plus signs (e.g., 1.0.0-SNAPSHOT)',
  },
  name: {
    pattern: /^[@a-zA-Z0-9._/-]+$/,
    message: 'Package name must be a valid npm package name (e.g., @scope/my-package)',
  },
};

// =============================================================================
// PURE HELPER FUNCTIONS (ported from useUploadForm.ts)
// =============================================================================

/**
 * Creates initial asset data structure for a new asset.
 * Ported from useUploadForm.ts:53-59
 */
function createEmptyAsset(assetFields: UploadFieldDefinition[]): AssetFieldData {
  const assetData: AssetFieldData = { file: null };
  assetFields.forEach((field) => {
    assetData[field.name] = field.initialValue ?? '';
  });
  return assetData;
}

/**
 * Creates initial form data structure.
 * Ported from useUploadForm.ts:64-81
 */
export function createInitialFormData(
  componentFields: UploadComponentField[],
  assetFields: UploadFieldDefinition[]
): UploadFormData {
  const componentData: Record<string, string | boolean> = {};
  componentFields.forEach((field) => {
    if (field.type === 'BOOLEAN') {
      componentData[field.name] = field.initialValue ?? false;
    } else {
      componentData[field.name] = (field.initialValue as string) ?? '';
    }
  });

  return {
    assets: [createEmptyAsset(assetFields)],
    componentFields: componentData,
  };
}

/**
 * Extract file extension from filename.
 * Returns the extension without the leading dot, or empty string if none.
 * Ported from useUploadForm.ts:174-180
 */
function getFileExtension(filename: string): string {
  const lastDotIndex = filename.lastIndexOf('.');
  if (lastDotIndex === -1 || lastDotIndex === filename.length - 1) {
    return '';
  }
  return filename.substring(lastDotIndex + 1).toLowerCase();
}

/**
 * Apply regex map to extract field values from filename.
 * Ported from useUploadForm.ts:145-165
 * Returns the updated asset with regex-applied fields.
 */
function applyRegexMapToAsset(
  regexMap: UploadDefinitionExtended['regexMap'] | null,
  filename: string,
  asset: AssetFieldData
): AssetFieldData {
  if (!(regexMap && filename)) return asset;

  try {
    const regex = new RegExp(regexMap.regex);
    const match = filename.match(regex);

    if (match && match.length > 1) {
      const newAsset = { ...asset };
      regexMap.fieldList.forEach((fieldName, index) => {
        const value = match[index + 1] || '';
        newAsset[fieldName] = value;
      });
      return newAsset;
    }
  } catch {
    // Invalid regex - ignore silently
  }

  return asset;
}

/**
 * Validate a single component field.
 * Ported from useUploadForm.ts:322-346
 * Returns the error string or null.
 */
function validateComponentFieldValue(
  fieldName: string,
  value: string | boolean,
  componentFields: UploadComponentField[],
  disabledFields: Set<string>
): string | null {
  const field = componentFields.find(f => f.name === fieldName);
  if (!field) return null;

  let error: string | null = null;

  if (disabledFields.has(fieldName)) {
    error = null;
  } else if (!field.optional && (value === '' || value === null || value === undefined)) {
    error = UPLOAD_FORM_STRINGS.fieldRequired;
  } else if (typeof value === 'string' && value.length > 0) {
    const validation = FIELD_VALIDATION[fieldName];
    if (validation && !validation.pattern.test(value)) {
      error = validation.message;
    }
  }

  return error;
}

/**
 * Validate a single asset field.
 * Ported from useUploadForm.ts:239-260
 * Returns the error string or null.
 */
function validateAssetFieldValue(
  fieldName: string,
  value: string | boolean,
  assetFields: UploadFieldDefinition[]
): string | null {
  const field = assetFields.find(f => f.name === fieldName);
  if (!field) return null;

  let error: string | null = null;

  if (!field.optional && (value === '' || value === null || value === undefined)) {
    error = UPLOAD_FORM_STRINGS.fieldRequired;
  }

  return error;
}

/**
 * Validate all form data and return validation errors.
 * Ported from useUploadForm.ts:386-462
 */
function validateAll(
  formData: UploadFormData,
  config: UploadFormMachineInit
): ValidationErrors {
  const errors: ValidationErrors = {
    assets: [],
    componentFields: {},
  };

  // Validate assets
  formData.assets.forEach((asset) => {
    const assetErrors: Record<string, string | null> = {};

    // File is always required
    if (!asset.file) {
      assetErrors.file = UPLOAD_FORM_STRINGS.fileRequired;
    }

    // Validate asset fields (skip FILE type -- handled by the file check above)
    config.assetFields.forEach((field) => {
      if (field.type === 'FILE') return;
      if (!field.optional) {
        const value = asset[field.name];
        if (value === '' || value === null || value === undefined) {
          assetErrors[field.name] = UPLOAD_FORM_STRINGS.fieldRequired;
        }
      }
    });

    errors.assets!.push(assetErrors);
  });

  // Check for unique assets (if multiple assets with non-FILE asset fields)
  const nonFileAssetFields = config.assetFields.filter((f) => f.type !== 'FILE');
  if (formData.assets.length > 1 && nonFileAssetFields.length > 0) {
    const assetSignatures = formData.assets.map((asset) =>
      nonFileAssetFields.map((f) => asset[f.name]).join('|')
    );

    const duplicates = new Set<number>();
    assetSignatures.forEach((sig, i) => {
      assetSignatures.forEach((otherSig, j) => {
        if (i !== j && sig === otherSig) {
          duplicates.add(i);
          duplicates.add(j);
        }
      });
    });

    duplicates.forEach((index) => {
      nonFileAssetFields.forEach((field) => {
        if (!errors.assets![index][field.name]) {
          errors.assets![index][field.name] = UPLOAD_FORM_STRINGS.assetNotUnique;
        }
      });
    });
  }

  // Validate component fields
  config.componentFields.forEach((field) => {
    if (config.disabledFields.has(field.name)) return;

    const value = formData.componentFields[field.name];
    if (!field.optional && (value === '' || value === null || value === undefined)) {
      errors.componentFields![field.name] = UPLOAD_FORM_STRINGS.fieldRequired;
    } else if (typeof value === 'string' && value.length > 0) {
      const validation = FIELD_VALIDATION[field.name];
      if (validation && !validation.pattern.test(value)) {
        errors.componentFields![field.name] = validation.message;
      }
    }
  });

  return errors;
}

/**
 * Check if there are any validation errors.
 * Ported from useUploadForm.ts:576-585
 */
export function hasAnyValidationErrors(errors: ValidationErrors): boolean {
  // Check asset errors
  const hasAssetErrors = errors.assets?.some((assetErrors) =>
    Object.values(assetErrors).some((error) => error !== null)
  );

  // Check component field errors
  const hasComponentErrors = Object.values(errors.componentFields || {}).some(
    (error) => error !== null
  );

  return !!(hasAssetErrors || hasComponentErrors || errors.general);
}

/**
 * Compares two File references by identity metadata rather than reference
 * equality, since a freshly re-selected file is a new File instance.
 */
function areFilesEqual(a: File | null, b: File | null): boolean {
  if (a === b) return true;
  if (!a || !b) return false;
  return a.name === b.name && a.size === b.size && a.type === b.type && a.lastModified === b.lastModified;
}

function isAssetDirty(current: AssetFieldData, initial: AssetFieldData): boolean {
  if (!areFilesEqual(current.file, initial.file)) return true;

  const keys = new Set([...Object.keys(current), ...Object.keys(initial)]);
  keys.delete('file');
  for (const key of keys) {
    if (current[key] !== initial[key]) return true;
  }
  return false;
}

/**
 * Determines whether form data differs from its initial snapshot.
 * Unlike a JSON.stringify comparison, this correctly detects file-only
 * changes: JSON.stringify(new File(...)) serializes to "{}", so every
 * File looks identical to every other File (including no file at all)
 * under naive serialization.
 */
export function isFormDataDirty(current: UploadFormData, initial: UploadFormData): boolean {
  if (current.assets.length !== initial.assets.length) return true;
  if (current.assets.some((asset, i) => isAssetDirty(asset, initial.assets[i]))) return true;

  const componentKeys = new Set([
    ...Object.keys(current.componentFields),
    ...Object.keys(initial.componentFields),
  ]);
  for (const key of componentKeys) {
    if (current.componentFields[key] !== initial.componentFields[key]) return true;
  }
  return false;
}

// =============================================================================
// SUBMIT HELPERS (ported from useUploadForm.ts:484-545)
// =============================================================================

const UPLOAD_API_BASE = APIConstants.REST.INTERNAL.UPLOAD;

/**
 * Build FormData for upload API from form data.
 * Ported from useUploadForm.ts:484-512
 */
function buildUploadFormData(formData: UploadFormData, config: UploadFormMachineInit): FormData {
  const formDataToSubmit = new FormData();

  // Add assets
  formData.assets.forEach((asset, assetIndex) => {
    const assetKey = `asset${assetIndex}`;

    // Add file
    if (asset.file) {
      formDataToSubmit.append(assetKey, asset.file);
    }

    // Add asset fields
    config.assetFields.forEach((field) => {
      const value = asset[field.name];
      if (value !== null && value !== undefined && value !== '') {
        formDataToSubmit.append(`${assetKey}.${field.name}`, String(value));
      }
    });
  });

  // Add component fields (skip disabled)
  config.componentFields.forEach((field) => {
    if (!config.disabledFields.has(field.name)) {
      const value = formData.componentFields[field.name];
      if (value !== null && value !== undefined && value !== '') {
        formDataToSubmit.append(field.name, String(value));
      }
    }
  });

  return formDataToSubmit;
}

/**
 * Extract error message from API error response.
 * Ported from useUploadForm.ts:539-544
 * Handles NEXUS-53344 HTTP 400 envelope unwrapping.
 */
function extractUploadErrorMessage(errData: unknown): string {
  const responseData = (errData as { response?: { data?: unknown } })?.response?.data;
  const envelopeMessage =
    (Array.isArray(responseData) ? (responseData[0] as { message?: string })?.message : undefined) ??
    (responseData as { message?: string })?.message;
  return envelopeMessage ?? (errData instanceof Error ? errData.message : 'Upload failed');
}

// =============================================================================
// MACHINE TYPES
// =============================================================================

export interface UploadFormMachineInit {
  repositoryName: string;
  componentFields: UploadComponentField[];
  assetFields: UploadFieldDefinition[];
  multipleUpload: boolean;
  regexMap: UploadDefinitionExtended['regexMap'] | null;
  disabledFields: Set<string>;
}

export interface UploadFormMachineContext {
  formData: UploadFormData;
  initialFormData: UploadFormData;
  validationErrors: ValidationErrors;
  hasValidated: boolean;
  touchedFields: Set<string>;
  config: UploadFormMachineInit;
  submitResult: { componentName?: string } | null;
  submitError: string | null;
}

export type UploadFormEvent =
  | { type: 'SET_ASSET_FILE'; index: number; file: File | null }
  | { type: 'SET_ASSET_FIELD'; index: number; field: string; value: string | boolean }
  | { type: 'ADD_ASSET' }
  | { type: 'REMOVE_ASSET'; index: number }
  | { type: 'SET_COMPONENT_FIELD'; field: string; value: string | boolean }
  | { type: 'BLUR_COMPONENT_FIELD'; field: string }
  | { type: 'BLUR_ASSET_FIELD'; index: number; field: string }
  | { type: 'VALIDATE' }
  | { type: 'RESET' }
  | { type: 'SYNC_CONFIG'; config: Partial<UploadFormMachineInit> }
  | { type: 'SUBMIT' };

// =============================================================================
// MACHINE FACTORY
// =============================================================================

export function createUploadFormMachine(init: UploadFormMachineInit) {
  const initialFormData = createInitialFormData(init.componentFields, init.assetFields);

  return createMachine<UploadFormMachineContext, UploadFormEvent>(
    {
      id: 'upload-form',
      initial: 'editing',
      context: {
        formData: initialFormData,
        initialFormData,
        validationErrors: {},
        hasValidated: false,
        touchedFields: new Set<string>(),
        config: init,
        submitResult: null,
        submitError: null,
      },
      states: {
        editing: {
          on: {
            SET_ASSET_FILE: { actions: 'setAssetFile' },
            SET_ASSET_FIELD: { actions: 'setAssetField' },
            ADD_ASSET: { actions: 'addAsset' },
            REMOVE_ASSET: { actions: 'removeAsset' },
            SET_COMPONENT_FIELD: { actions: 'setComponentField' },
            BLUR_COMPONENT_FIELD: { actions: 'blurComponentField' },
            BLUR_ASSET_FIELD: { actions: 'blurAssetField' },
            VALIDATE: { actions: 'validateAll' },
            RESET: { actions: 'resetForm' },
            SYNC_CONFIG: { actions: 'syncConfig' },
            SUBMIT: { target: 'validating' },
          },
        },
        validating: {
          entry: ['validateAll'],
          always: [
            { target: 'editing', cond: 'hasValidationErrors' },
            { target: 'submitting' },
          ],
        },
        submitting: {
          entry: 'clearSubmitOutcome',
          invoke: {
            src: 'submitUpload',
            onDone: { target: 'editing', actions: 'setSubmitResult' },
            onError: { target: 'editing', actions: 'setSubmitError' },
          },
        },
      },
    },
    {
      guards: {
        hasValidationErrors: (ctx) => hasAnyValidationErrors(ctx.validationErrors),
      },
      actions: {
        /**
         * Set file for an asset.
         * Auto-fills the 'extension' field from the filename if present.
         * Ported from useUploadForm.ts:186-234
         */
        setAssetFile: assign((ctx, event) => {
          if (event.type !== 'SET_ASSET_FILE') return {};

          const { index, file } = event;
          const newAssets = [...ctx.formData.assets];

          if (newAssets[index]) {
            const updatedAsset = { ...newAssets[index], file };

            if (file) {
              // Auto-fill extension field from filename
              if ('extension' in updatedAsset) {
                const extension = getFileExtension(file.name);
                if (extension) {
                  updatedAsset.extension = extension;
                }
              }

              // Auto-fill filename field from the selected file
              if ('filename' in updatedAsset) {
                updatedAsset.filename = file.name;
              }
            }

            // Apply regex map if file is provided
            if (file && ctx.config.regexMap) {
              const assetWithRegex = applyRegexMapToAsset(ctx.config.regexMap, file.name, updatedAsset);
              newAssets[index] = assetWithRegex;
            } else {
              newAssets[index] = updatedAsset;
            }
          }

          // Clear validation for auto-filled fields when hasValidated
          let newAssetErrors = ctx.validationErrors.assets;
          if (ctx.hasValidated && newAssetErrors?.[index]) {
            newAssetErrors = [...(ctx.validationErrors.assets || [])];
            newAssetErrors[index] = {
              ...newAssetErrors[index],
              file: null,
              extension: null,
              filename: null,
            };
          }

          return {
            formData: { ...ctx.formData, assets: newAssets },
            validationErrors: newAssetErrors !== ctx.validationErrors.assets
              ? { ...ctx.validationErrors, assets: newAssetErrors }
              : ctx.validationErrors,
          };
        }),

        /**
         * Set a field value for an asset.
         * Validates on change after first validation attempt.
         * Ported from useUploadForm.ts:266-282
         */
        setAssetField: assign((ctx, event) => {
          if (event.type !== 'SET_ASSET_FIELD') return {};

          const { index, field, value } = event;
          const newAssets = [...ctx.formData.assets];

          if (newAssets[index]) {
            newAssets[index] = { ...newAssets[index], [field]: value };
          }

          // Live validation after first submit attempt
          if (ctx.hasValidated) {
            const error = validateAssetFieldValue(field, value, ctx.config.assetFields);
            const newAssetErrors = [...(ctx.validationErrors.assets || [])];
            if (!newAssetErrors[index]) {
              newAssetErrors[index] = {};
            }
            newAssetErrors[index] = { ...newAssetErrors[index], [field]: error };

            return {
              formData: { ...ctx.formData, assets: newAssets },
              validationErrors: { ...ctx.validationErrors, assets: newAssetErrors },
            };
          }

          return {
            formData: { ...ctx.formData, assets: newAssets },
          };
        }),

        /**
         * Add a new asset (for multiple upload).
         * Ported from useUploadForm.ts:287-294
         */
        addAsset: assign((ctx) => {
          if (!ctx.config.multipleUpload) return {};

          return {
            formData: {
              ...ctx.formData,
              assets: [...ctx.formData.assets, createEmptyAsset(ctx.config.assetFields)],
            },
          };
        }),

        /**
         * Remove an asset by index.
         * Ported from useUploadForm.ts:299-317
         */
        removeAsset: assign((ctx, event) => {
          if (event.type !== 'REMOVE_ASSET') return {};

          const { index } = event;

          // Don't remove the last asset
          if (ctx.formData.assets.length <= 1) return {};

          const newAssets = ctx.formData.assets.filter((_, i) => i !== index);

          // Update validation errors
          const newAssetErrors = ctx.validationErrors.assets
            ? ctx.validationErrors.assets.filter((_, i) => i !== index)
            : [];

          return {
            formData: { ...ctx.formData, assets: newAssets },
            validationErrors: { ...ctx.validationErrors, assets: newAssetErrors },
          };
        }),

        /**
         * Set a component field value.
         * Validates on change after first validation attempt.
         * Ported from useUploadForm.ts:352-364
         */
        setComponentField: assign((ctx, event) => {
          if (event.type !== 'SET_COMPONENT_FIELD') return {};

          const { field, value } = event;
          const newComponentFields = { ...ctx.formData.componentFields, [field]: value };

          // Validate if hasValidated or touchedFields.has(field)
          if (ctx.hasValidated || ctx.touchedFields.has(field)) {
            const error = validateComponentFieldValue(
              field,
              value,
              ctx.config.componentFields,
              ctx.config.disabledFields
            );

            return {
              formData: { ...ctx.formData, componentFields: newComponentFields },
              validationErrors: {
                ...ctx.validationErrors,
                componentFields: { ...(ctx.validationErrors.componentFields || {}), [field]: error },
              },
            };
          }

          return {
            formData: { ...ctx.formData, componentFields: newComponentFields },
          };
        }),

        /**
         * Blur handler for component field - marks as touched and validates.
         * Ported from useUploadForm.ts:366-372
         */
        blurComponentField: assign((ctx, event) => {
          if (event.type !== 'BLUR_COMPONENT_FIELD') return {};

          const { field } = event;
          const newTouchedFields = new Set(ctx.touchedFields).add(field);
          const error = validateComponentFieldValue(
            field,
            ctx.formData.componentFields[field],
            ctx.config.componentFields,
            ctx.config.disabledFields
          );

          return {
            touchedFields: newTouchedFields,
            validationErrors: {
              ...ctx.validationErrors,
              componentFields: { ...(ctx.validationErrors.componentFields || {}), [field]: error },
            },
          };
        }),

        /**
         * Blur handler for asset field - marks as touched and validates.
         * Ported from useUploadForm.ts:374-381
         */
        blurAssetField: assign((ctx, event) => {
          if (event.type !== 'BLUR_ASSET_FIELD') return {};

          const { index, field } = event;
          const key = `asset-${index}-${field}`;
          const newTouchedFields = new Set(ctx.touchedFields).add(key);
          const value = ctx.formData.assets[index]?.[field] as string | boolean;
          const error = validateAssetFieldValue(field, value, ctx.config.assetFields);

          const newAssetErrors = [...(ctx.validationErrors.assets || [])];
          if (!newAssetErrors[index]) {
            newAssetErrors[index] = {};
          }
          newAssetErrors[index] = { ...newAssetErrors[index], [field]: error };

          return {
            touchedFields: newTouchedFields,
            validationErrors: { ...ctx.validationErrors, assets: newAssetErrors },
          };
        }),

        /**
         * Validate the entire form.
         */
        validateAll: assign((ctx) => ({
          validationErrors: validateAll(ctx.formData, ctx.config),
          hasValidated: true,
        })),

        /**
         * Reset the form to initial state.
         */
        resetForm: assign((ctx) => {
          const fresh = createInitialFormData(ctx.config.componentFields, ctx.config.assetFields);
          return {
            formData: fresh,
            validationErrors: {},
            hasValidated: false,
            touchedFields: new Set<string>(),
          };
        }),

        /**
         * Sync config without resetting form data.
         */
        syncConfig: assign((ctx, event) => {
          if (event.type !== 'SYNC_CONFIG') return {};
          return { config: { ...ctx.config, ...event.config } };
        }),

        /**
         * Clear submit result/error before a new submission.
         */
        clearSubmitOutcome: assign({
          submitResult: null,
          submitError: null,
        }),

        /**
         * Set the submit result on success.
         */
        setSubmitResult: assign((_, event) => ({
          submitResult: { componentName: (event as any).data?.componentName },
        })),

        /**
         * Set the submit error on failure.
         */
        setSubmitError: assign((_, event) => ({
          submitError: `${UPLOAD_FORM_STRINGS.uploadError}: ${extractUploadErrorMessage((event as any).data)}`,
        })),
      },
      services: {
        /**
         * Submit the form data to the upload API.
         * Ported from useUploadForm.ts:483-546
         */
        submitUpload: async (ctx) => {
          const fd = buildUploadFormData(ctx.formData, ctx.config);
          const data = await restClient.post<{ success?: boolean; data?: string; [k: number]: { message?: string } }>(
            `${UPLOAD_API_BASE}${encodeURIComponent(ctx.config.repositoryName)}`,
            fd
          );

          if (data?.success !== true) {
            throw new Error(data?.[0]?.message ?? 'Upload failed');
          }

          return { componentName: data.data };
        },
      },
    }
  );
}
