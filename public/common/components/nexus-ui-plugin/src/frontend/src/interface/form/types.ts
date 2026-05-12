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

/**
 * Base context that all form machines have.
 * Users extend this with their own data shape via the TData generic.
 */
export interface FormContext<TData = Record<string, unknown>> {
  /** Current form data */
  data: TData;

  /** Original form data for pristine comparison */
  pristineData: TData;

  /** Whether the form has unsaved changes */
  isPristine: boolean;

  /** Track which fields have been interacted with */
  touched: Record<string, boolean>;

  /** Validation errors keyed by field name */
  validationErrors: Record<string, string | null>;

  /** Error from save operation */
  saveError: string | null;

  /** Error from load operation */
  loadError: string | null;

  /** Error from delete operation */
  deleteError: string | null;
}

/**
 * Standard form events that all form machines handle
 */
export type FormEvent =
  | { type: 'UPDATE'; name: string; value: unknown }
  | { type: 'BLUR'; name: string }
  | { type: 'SUBMIT' }
  | { type: 'RESET' }
  | { type: 'DELETE' }
  | { type: 'CONFIRM_DELETE' }
  | { type: 'CANCEL_DELETE' }
  | { type: 'CANCEL' }
  | { type: 'CONFIRM_CANCEL' }
  | { type: 'STAY' }
  | { type: 'RETRY' };

/**
 * Validation errors object - keys are field names, values are error messages or null
 */
export type ValidationErrors = Record<string, string | null>;

/**
 * Function that validates form data and returns validation errors
 */
export type ValidateFn<TData> = (context: FormContext<TData>) => ValidationErrors;

/**
 * Props returned by the field() helper for text inputs
 */
export interface FieldProps {
  name: string;
  value: string;
  error: string | undefined;
  onChange: (value: string) => void;
  onBlur: () => void;
}

/**
 * Props returned by the checkbox() helper
 */
export interface CheckboxProps {
  name: string;
  checked: boolean;
  error: string | undefined;
  onChange: (checked: boolean) => void;
}

/**
 * Props returned by the select() helper
 */
export interface SelectProps {
  name: string;
  value: string;
  error: string | undefined;
  onChange: (value: string) => void;
  onBlur: () => void;
}


/**
 * Return type of the useForm hook
 */
export interface UseFormReturn<TData> {
  // ============================================
  // State
  // ============================================

  /** Current form data */
  data: TData;

  /** Whether the form has unsaved changes */
  isPristine: boolean;

  /** Track which fields have been interacted with (blurred) */
  touched: Record<string, boolean>;

  // ============================================
  // Status Flags
  // ============================================

  /** Whether the form is loading initial data */
  isLoading: boolean;

  /** Whether the form is saving */
  isSaving: boolean;

  /** Whether the form is deleting */
  isDeleting: boolean;

  /** Whether there was a load error */
  hasLoadError: boolean;

  /** Whether the form has validation errors */
  hasValidationErrors: boolean;

  /** Whether the delete confirmation is showing */
  isConfirmingDelete: boolean;

  /** Whether the cancel confirmation is showing (unsaved changes warning) */
  isConfirmingCancel: boolean;

  /** Whether the form was cancelled (user discarded changes) */
  isCancelled: boolean;

  /** Whether the form completed successfully (saved or deleted) */
  isComplete: boolean;

  // ============================================
  // Errors
  // ============================================

  /** Error message from load operation */
  loadError: string | null;

  /** Error message from save operation */
  saveError: string | null;

  /** Error message from delete operation */
  deleteError: string | null;

  /** All validation errors */
  validationErrors: ValidationErrors;

  // ============================================
  // Field Helpers
  // ============================================

  /**
   * Get props for a text input field
   * @param name - Field name (supports dot notation for nested fields)
   */
  field: (name: string) => FieldProps;

  /**
   * Get props for a checkbox field
   * @param name - Field name (supports dot notation for nested fields)
   */
  checkbox: (name: string) => CheckboxProps;

  /**
   * Get props for a select field
   * @param name - Field name (supports dot notation for nested fields)
   */
  select: (name: string) => SelectProps;

  // ============================================
  // Actions
  // ============================================

  /** Submit the form */
  submit: () => void;

  /** Reset form to pristine state */
  reset: () => void;

  /** Retry loading after an error */
  retry: () => void;

  /** Request deletion (shows confirmation) */
  requestDelete: () => void;

  /** Confirm the delete operation */
  confirmDelete: () => void;

  /** Cancel the delete operation */
  cancelDelete: () => void;

  /** Request to cancel/close the form (shows confirmation if dirty) */
  requestCancel: () => void;

  /** Confirm cancellation (discard changes) */
  confirmCancel: () => void;

  /** Stay on form (dismiss cancel confirmation) */
  stay: () => void;

  // ============================================
  // Raw Access (escape hatch)
  // ============================================

  /** Raw XState state object */
  state: unknown;

  /** Raw XState send function */
  send: (event: FormEvent) => void;
}
