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

import { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import { restClient } from '../../../../../interface/api';
import { APIConstants } from '../../../../../constants/APIConstants';

import type {
  UploadFormData,
  ValidationErrors,
  UseUploadFormResult,
  AssetFieldData,
  UploadComponentField,
  UploadFieldDefinition,
  UploadDefinitionExtended,
} from '../upload.types';
import { UPLOAD_FORM_STRINGS } from '../upload.types';

const UPLOAD_API_BASE = APIConstants.REST.INTERNAL.UPLOAD;

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

/**
 * Creates initial asset data structure for a new asset.
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
 */
function createInitialFormData(
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

interface UseUploadFormProps {
  repositoryName: string;
  componentFields: UploadComponentField[];
  assetFields: UploadFieldDefinition[];
  multipleUpload: boolean;
  regexMap?: UploadDefinitionExtended['regexMap'] | null;
  disabledFields?: Set<string>;
}

/**
 * Hook to manage upload form state, validation, and submission.
 *
 * Features:
 * - Dynamic form data based on upload definition
 * - Multiple asset support
 * - Validation (required fields, unique assets)
 * - Regex-based field auto-population from filename
 * - Form submission with multipart/form-data
 *
 * @param props - Configuration from useUploadDefinition
 * @returns Form state and actions
 */
export function useUploadForm({
  repositoryName,
  componentFields,
  assetFields,
  multipleUpload,
  regexMap,
  disabledFields = new Set(),
}: UseUploadFormProps): UseUploadFormResult {
  // Form data state
  const [formData, setFormData] = useState<UploadFormData>(() =>
    createInitialFormData(componentFields, assetFields)
  );

  // Validation state
  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({});
  const [hasValidated, setHasValidated] = useState(false);
  const [touchedFields, setTouchedFields] = useState<Set<string>>(new Set());

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Track initial state for isDirty
  const initialFormDataRef = useRef<UploadFormData | null>(null);

  // Reset form when fields change (e.g., repository change)
  useEffect(() => {
    const newFormData = createInitialFormData(componentFields, assetFields);
    setFormData(newFormData);
    initialFormDataRef.current = newFormData;
    setValidationErrors({});
    setHasValidated(false);
  }, [componentFields, assetFields]);

  /**
   * Apply regex map to extract field values from filename.
   */
  const applyRegexMap = useCallback(
    (filename: string, assetIndex: number) => {
      if (!regexMap || !filename) return;

      try {
        const regex = new RegExp(regexMap.regex);
        const match = filename.match(regex);

        if (match && match.length > 1) {
          setFormData((prev) => {
            const newAssets = [...prev.assets];
            const newAsset = { ...newAssets[assetIndex] };

            regexMap.fieldList.forEach((fieldName, index) => {
              const value = match[index + 1] || '';
              newAsset[fieldName] = value;
            });

            newAssets[assetIndex] = newAsset;
            return { ...prev, assets: newAssets };
          });
        }
      } catch {
        // Invalid regex - ignore silently
      }
    },
    [regexMap]
  );

  /**
   * Extract file extension from filename.
   * Returns the extension without the leading dot, or empty string if none.
   */
  const getFileExtension = useCallback((filename: string): string => {
    const lastDotIndex = filename.lastIndexOf('.');
    if (lastDotIndex === -1 || lastDotIndex === filename.length - 1) {
      return '';
    }
    return filename.substring(lastDotIndex + 1).toLowerCase();
  }, []);

  /**
   * Set file for an asset.
   * Auto-fills the 'extension' field from the filename if present.
   */
  const setAssetFile = useCallback(
    (assetIndex: number, file: File | null) => {
      setFormData((prev) => {
        const newAssets = [...prev.assets];
        if (newAssets[assetIndex]) {
          const updatedAsset = { ...newAssets[assetIndex], file };
          
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
          
          newAssets[assetIndex] = updatedAsset;
        }
        return { ...prev, assets: newAssets };
      });

      // Apply regex map if file is provided
      if (file) {
        applyRegexMap(file.name, assetIndex);
      }

      // Clear validation for auto-filled fields
      if (hasValidated) {
        setValidationErrors((prev) => {
          if (!prev.assets?.[assetIndex]) return prev;
          const newAssetErrors = [...(prev.assets || [])];
          newAssetErrors[assetIndex] = { 
            ...newAssetErrors[assetIndex], 
            file: null,
            extension: null,
            filename: null,
          };
          return { ...prev, assets: newAssetErrors };
        });
      }
    },
    [applyRegexMap, hasValidated, getFileExtension]
  );

  /**
   * Validate a single asset field and update errors.
   */
  const validateAssetField = useCallback(
    (assetIndex: number, fieldName: string, value: string | boolean) => {
      const field = assetFields.find(f => f.name === fieldName);
      if (!field) return;

      let error: string | null = null;
      
      if (!field.optional && (value === '' || value === null || value === undefined)) {
        error = UPLOAD_FORM_STRINGS.fieldRequired;
      }

      setValidationErrors((prev) => {
        const newAssetErrors = [...(prev.assets || [])];
        if (!newAssetErrors[assetIndex]) {
          newAssetErrors[assetIndex] = {};
        }
        newAssetErrors[assetIndex] = { ...newAssetErrors[assetIndex], [fieldName]: error };
        return { ...prev, assets: newAssetErrors };
      });
    },
    [assetFields]
  );

  /**
   * Set a field value for an asset.
   * Validates on change after first validation attempt.
   */
  const setAssetField = useCallback(
    (assetIndex: number, fieldName: string, value: string | boolean) => {
      setFormData((prev) => {
        const newAssets = [...prev.assets];
        if (newAssets[assetIndex]) {
          newAssets[assetIndex] = { ...newAssets[assetIndex], [fieldName]: value };
        }
        return { ...prev, assets: newAssets };
      });

      // Live validation after first submit attempt
      if (hasValidated) {
        validateAssetField(assetIndex, fieldName, value);
      }
    },
    [hasValidated, validateAssetField]
  );

  /**
   * Add a new asset (for multiple upload).
   */
  const addAsset = useCallback(() => {
    if (!multipleUpload) return;

    setFormData((prev) => ({
      ...prev,
      assets: [...prev.assets, createEmptyAsset(assetFields)],
    }));
  }, [assetFields, multipleUpload]);

  /**
   * Remove an asset by index.
   */
  const removeAsset = useCallback(
    (assetIndex: number) => {
      setFormData((prev) => {
        // Don't remove the last asset
        if (prev.assets.length <= 1) return prev;

        const newAssets = prev.assets.filter((_, index) => index !== assetIndex);
        return { ...prev, assets: newAssets };
      });

      // Update validation errors
      setValidationErrors((prev) => {
        if (!prev.assets) return prev;
        const newAssetErrors = prev.assets.filter((_, index) => index !== assetIndex);
        return { ...prev, assets: newAssetErrors };
      });
    },
    []
  );

  /**
   * Validate a single component field and update errors.
   */
  const validateComponentField = useCallback(
    (fieldName: string, value: string | boolean) => {
      const field = componentFields.find(f => f.name === fieldName);
      if (!field) return;

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

      setValidationErrors((prev) => ({
        ...prev,
        componentFields: { ...(prev.componentFields || {}), [fieldName]: error },
      }));
    },
    [componentFields, disabledFields]
  );

  /**
   * Set a component field value.
   * Validates on change after first validation attempt.
   */
  const setComponentField = useCallback(
    (fieldName: string, value: string | boolean) => {
      setFormData((prev) => ({
        ...prev,
        componentFields: { ...prev.componentFields, [fieldName]: value },
      }));

      if (hasValidated || touchedFields.has(fieldName)) {
        validateComponentField(fieldName, value);
      }
    },
    [hasValidated, touchedFields, validateComponentField]
  );

  const blurComponentField = useCallback(
    (fieldName: string) => {
      setTouchedFields((prev) => new Set(prev).add(fieldName));
      validateComponentField(fieldName, formData.componentFields[fieldName]);
    },
    [formData.componentFields, validateComponentField]
  );

  const blurAssetField = useCallback(
    (assetIndex: number, fieldName: string) => {
      const key = `asset-${assetIndex}-${fieldName}`;
      setTouchedFields((prev) => new Set(prev).add(key));
      validateAssetField(assetIndex, fieldName, formData.assets[assetIndex]?.[fieldName] as string | boolean);
    },
    [formData.assets, validateAssetField]
  );

  /**
   * Validate the form and return whether it's valid.
   */
  const validate = useCallback((): boolean => {
    const errors: ValidationErrors = {
      assets: [],
      componentFields: {},
    };

    let isValid = true;

    // Validate assets
    formData.assets.forEach((asset, index) => {
      const assetErrors: Record<string, string | null> = {};

      // File is always required
      if (!asset.file) {
        assetErrors.file = UPLOAD_FORM_STRINGS.fileRequired;
        isValid = false;
      }

      // Validate asset fields (skip FILE type -- handled by the file check above)
      assetFields.forEach((field) => {
        if (field.type === 'FILE') return;
        if (!field.optional) {
          const value = asset[field.name];
          if (value === '' || value === null || value === undefined) {
            assetErrors[field.name] = UPLOAD_FORM_STRINGS.fieldRequired;
            isValid = false;
          }
        }
      });

      errors.assets!.push(assetErrors);
    });

    // Check for unique assets (if multiple assets with non-FILE asset fields)
    const nonFileAssetFields = assetFields.filter((f) => f.type !== 'FILE');
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
        isValid = false;
      });
    }

    componentFields.forEach((field) => {
      if (disabledFields.has(field.name)) return;

      const value = formData.componentFields[field.name];
      if (!field.optional && (value === '' || value === null || value === undefined)) {
        errors.componentFields![field.name] = UPLOAD_FORM_STRINGS.fieldRequired;
        isValid = false;
      } else if (typeof value === 'string' && value.length > 0) {
        const validation = FIELD_VALIDATION[field.name];
        if (validation && !validation.pattern.test(value)) {
          errors.componentFields![field.name] = validation.message;
          isValid = false;
        }
      }
    });

    setValidationErrors(errors);
    setHasValidated(true);

    return isValid;
  }, [formData, assetFields, componentFields, disabledFields]);

  /**
   * Submit the form.
   */
  const submit = useCallback(async (): Promise<{
    success: boolean;
    error?: string;
    componentName?: string;
  }> => {
    // Validate first
    if (!validate()) {
      return { success: false, error: 'Please fix validation errors' };
    }

    setIsSubmitting(true);

    try {
      const formDataToSubmit = new FormData();

      // Add assets
      formData.assets.forEach((asset, assetIndex) => {
        const assetKey = `asset${assetIndex}`;

        // Add file
        if (asset.file) {
          formDataToSubmit.append(assetKey, asset.file);
        }

        // Add asset fields
        assetFields.forEach((field) => {
          const value = asset[field.name];
          if (value !== null && value !== undefined && value !== '') {
            formDataToSubmit.append(`${assetKey}.${field.name}`, String(value));
          }
        });
      });

      // Add component fields (skip disabled)
      componentFields.forEach((field) => {
        if (!disabledFields.has(field.name)) {
          const value = formData.componentFields[field.name];
          if (value !== null && value !== undefined && value !== '') {
            formDataToSubmit.append(field.name, String(value));
          }
        }
      });

      const data = await restClient.post<{success?: boolean; data?: string; [key: number]: {message?: string}}>(
        `${UPLOAD_API_BASE}${encodeURIComponent(repositoryName)}`,
        formDataToSubmit
      );

      // Check for success
      if (data?.success !== true) {
        const errorMessage = data?.[0]?.message ?? 'Upload failed';
        throw new Error(errorMessage);
      }

      // Success! Navigation to search results will be handled by the caller
      // No legacy ExtJS toast - the search results showing the uploaded component is the success indicator
      return {
        success: true,
        componentName: data.data,
      };
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Upload failed';
      return { success: false, error: `${UPLOAD_FORM_STRINGS.uploadError}: ${errorMessage}` };
    } finally {
      setIsSubmitting(false);
    }
  }, [
    formData,
    repositoryName,
    assetFields,
    componentFields,
    disabledFields,
    validate,
  ]);

  /**
   * Reset the form to initial state.
   */
  const reset = useCallback(() => {
    const newFormData = createInitialFormData(componentFields, assetFields);
    setFormData(newFormData);
    setValidationErrors({});
    setHasValidated(false);
    setTouchedFields(new Set());
  }, [componentFields, assetFields]);

  /**
   * Check if form is valid (no errors).
   */
  const isValid = useMemo(() => {
    if (!hasValidated) return true;

    // Check asset errors
    const hasAssetErrors = validationErrors.assets?.some((assetErrors) =>
      Object.values(assetErrors).some((error) => error !== null)
    );

    // Check component field errors
    const hasComponentErrors = Object.values(validationErrors.componentFields || {}).some(
      (error) => error !== null
    );

    return !hasAssetErrors && !hasComponentErrors && !validationErrors.general;
  }, [validationErrors, hasValidated]);

  /**
   * Check if form has been modified.
   */
  const isDirty = useMemo(() => {
    if (!initialFormDataRef.current) return false;

    // Simple comparison - in production you might want deep comparison
    return JSON.stringify(formData) !== JSON.stringify(initialFormDataRef.current);
  }, [formData]);

  return {
    formData,
    validationErrors,
    isSubmitting,
    isValid,
    isDirty,
    touchedFields,

    setAssetFile,
    setAssetField,
    addAsset,
    removeAsset,
    setComponentField,
    blurComponentField,
    blurAssetField,

    validate,
    submit,
    reset,
  };
}

export default useUploadForm;

