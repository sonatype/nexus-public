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

import { assign } from 'xstate';
import { createFormMachine, type FormContext, type ValidationErrors } from '../../../../../../interface/form';

import {
  CleanupPolicy,
  CleanupPolicyFormData,
  FormatCriteria,
  EMPTY_CLEANUP_POLICY,
  NOTES_MAX_LENGTH,
  isValidCriteriaNumber,
  isRetainSupportedFormat,
  isReleaseType,
  getDefaultSortBy,
} from './types';

/**
 * Extended context for the cleanup policy form machine.
 * Tracks which criteria checkboxes are enabled and
 * which format criteria are available.
 */
export interface CleanupPolicyMachineContext {
  data: CleanupPolicyFormData;
  /** Reference data: available format criteria from the server */
  formatCriteria: FormatCriteria[];
  /** The original policy being edited (null for create) */
  policy: CleanupPolicy | null;
  /** Criteria enable/disable flags tracked in machine context */
  criteriaEnabled: {
    lastBlobUpdated: boolean;
    lastDownloaded: boolean;
    assetRegex: boolean;
    retain: boolean;
  };
}

/**
 * Get available criteria for the currently selected format.
 */
function getAvailableCriteria(
  format: string,
  formatCriteria: FormatCriteria[]
): string[] {
  const fc = formatCriteria.find((f) => f.id === format);
  return fc?.availableCriteria ?? [];
}

/**
 * Validate cleanup policy form data.
 *
 * Criteria validation depends on which checkboxes are enabled,
 * which is tracked via the criteriaEnabled flags in context.
 */
export function validateCleanupPolicy(
  data: CleanupPolicyFormData,
  criteriaEnabled: CleanupPolicyMachineContext['criteriaEnabled'],
  formatCriteria: FormatCriteria[]
): ValidationErrors {
  const errors: ValidationErrors = {};

  // Name validation
  if (!data.name?.trim()) {
    errors.name = 'Name is required';
  } else if (!/^[a-zA-Z0-9\-_.]+$/.test(data.name)) {
    errors.name = 'Name must contain only letters, digits, underscores, hyphens, and periods';
  }

  // Format validation
  if (!data.format) {
    errors.format = 'Format is required';
  }

  // Notes length
  if (data.notes && data.notes.length > NOTES_MAX_LENGTH) {
    errors.notes = `Description must be ${NOTES_MAX_LENGTH} characters or less`;
  }

  // Criteria number fields - only validate when enabled
  if (criteriaEnabled.lastBlobUpdated) {
    if (!data.criteriaLastBlobUpdated) {
      errors.criteriaLastBlobUpdated = 'Component age is required';
    } else if (!isValidCriteriaNumber(data.criteriaLastBlobUpdated)) {
      errors.criteriaLastBlobUpdated = 'Must be a whole number between 1 and 24855';
    }
  }

  if (criteriaEnabled.lastDownloaded) {
    if (!data.criteriaLastDownloaded) {
      errors.criteriaLastDownloaded = 'Component usage is required';
    } else if (!isValidCriteriaNumber(data.criteriaLastDownloaded)) {
      errors.criteriaLastDownloaded = 'Must be a whole number between 1 and 24855';
    }
  }

  if (criteriaEnabled.assetRegex) {
    if (!data.criteriaAssetRegex?.trim()) {
      errors.criteriaAssetRegex = 'Asset name pattern is required';
    } else {
      try {
        new RegExp(data.criteriaAssetRegex);
      } catch {
        errors.criteriaAssetRegex = 'Invalid regular expression pattern';
      }
    }
  }

  if (criteriaEnabled.retain) {
    if (!data.retain) {
      errors.retain = 'Number of versions is required';
    } else if (!isValidCriteriaNumber(data.retain)) {
      errors.retain = 'Must be a whole number between 1 and 24855';
    }
  }

  // At least one criteria must be enabled when a format is selected
  if (
    data.format &&
    !criteriaEnabled.lastBlobUpdated &&
    !criteriaEnabled.lastDownloaded &&
    !criteriaEnabled.assetRegex &&
    !criteriaEnabled.retain
  ) {
    errors.criteriaSelected = 'At least one criterion must be selected';
  }

  return errors;
}

/**
 * Create a cleanup policy form machine with XState.
 *
 * The cleanup policy form models format-aware criteria:
 * - Format selection determines which criteria checkboxes are available
 * - Each criterion has an enable/disable toggle tracked in context
 * - Changing format resets all criteria values and flags
 * - Retain/exclusion criteria is only available for specific formats + release type
 *
 * Custom events:
 * - FORMAT_CHANGE: changes format and resets criteria
 * - TOGGLE_CRITERIA: enables/disables a specific criterion checkbox
 * - RELEASE_TYPE_CHANGE: changes release type, may disable retain
 */
export function createCleanupPolicyFormMachine(
  policyName: string | undefined,
  preloadedPolicy?: CleanupPolicy | null,
  formatCriteria: FormatCriteria[] = []
) {
  const initialCriteriaEnabled = preloadedPolicy
    ? {
        lastBlobUpdated: !!preloadedPolicy.criteriaLastBlobUpdated,
        lastDownloaded: !!preloadedPolicy.criteriaLastDownloaded,
        assetRegex: !!preloadedPolicy.criteriaAssetRegex,
        retain: !!preloadedPolicy.retain || !!preloadedPolicy.sortBy,
      }
    : {
        lastBlobUpdated: false,
        lastDownloaded: false,
        assetRegex: false,
        retain: false,
      };

  return createFormMachine({
    id: `cleanup-policy-form-${policyName ?? 'new'}`,
    context: {
      data: { ...EMPTY_CLEANUP_POLICY } as CleanupPolicyFormData,
      formatCriteria,
      policy: preloadedPolicy ?? (null as CleanupPolicy | null),
      criteriaEnabled: initialCriteriaEnabled,
    },
    actions: {
      validate: assign((ctx: FormContext<CleanupPolicyFormData>) => {
        const extCtx = ctx as unknown as FormContext<CleanupPolicyFormData> & CleanupPolicyMachineContext;
        return {
          validationErrors: validateCleanupPolicy(
            ctx.data,
            extCtx.criteriaEnabled,
            extCtx.formatCriteria
          ),
        };
      }),
      // Change format and reset all criteria
      changeFormat: assign((context: any, event: any) => {
        const format = event.value as string;
        return {
          data: {
            ...context.data,
            format,
            criteriaLastBlobUpdated: null,
            criteriaLastDownloaded: null,
            criteriaReleaseType: null,
            criteriaAssetRegex: null,
            retain: null,
            sortBy: null,
          },
          criteriaEnabled: {
            lastBlobUpdated: false,
            lastDownloaded: false,
            assetRegex: false,
            retain: false,
          },
          touched: { ...context.touched, format: true },
        };
      }),
      // Toggle a criteria checkbox on/off
      toggleCriteria: assign((context: any, event: any) => {
        const { criteria, enabled } = event as { type: string; criteria: string; enabled: boolean };
        const newCriteriaEnabled = {
          ...context.criteriaEnabled,
          [criteria]: enabled,
        };

        // When disabling a criteria, clear its data value
        const newData = { ...context.data };
        if (!enabled) {
          switch (criteria) {
            case 'lastBlobUpdated':
              newData.criteriaLastBlobUpdated = null;
              break;
            case 'lastDownloaded':
              newData.criteriaLastDownloaded = null;
              break;
            case 'assetRegex':
              newData.criteriaAssetRegex = null;
              break;
            case 'retain':
              newData.retain = null;
              newData.sortBy = null;
              break;
          }
        }

        // When enabling retain, set default sortBy for the format
        if (enabled && criteria === 'retain') {
          newData.sortBy = getDefaultSortBy(context.data.format);
        }

        // For docker: disable retain when no other criteria remain selected
        if (!enabled && criteria !== 'retain' && newData.format === 'docker') {
          const hasOtherCriteria = !!(
            newData.criteriaLastBlobUpdated ||
            newData.criteriaLastDownloaded ||
            newData.criteriaAssetRegex
          );
          if (!hasOtherCriteria && newCriteriaEnabled.retain) {
            newCriteriaEnabled.retain = false;
            newData.retain = null;
            newData.sortBy = null;
          }
        }

        return {
          data: newData,
          criteriaEnabled: newCriteriaEnabled,
        };
      }),
      // Change release type, may disable retain criteria
      changeReleaseType: assign((context: any, event: any) => {
        const releaseType = event.value as string;
        const shouldDisableRetain = !isReleaseType(releaseType);
        const newCriteriaEnabled = shouldDisableRetain
          ? { ...context.criteriaEnabled, retain: false }
          : context.criteriaEnabled;
        const newData = {
          ...context.data,
          criteriaReleaseType: releaseType || null,
        };
        if (shouldDisableRetain) {
          newData.retain = null;
          newData.sortBy = null;
        }
        return {
          data: newData,
          criteriaEnabled: newCriteriaEnabled,
          touched: { ...context.touched, criteriaReleaseType: true },
        };
      }),
    },
    services: {
      load: async () => {
        const initialData: CleanupPolicyFormData = preloadedPolicy
          ? {
              name: preloadedPolicy.name,
              format: preloadedPolicy.format,
              notes: preloadedPolicy.notes || '',
              criteriaLastBlobUpdated: preloadedPolicy.criteriaLastBlobUpdated,
              criteriaLastDownloaded: preloadedPolicy.criteriaLastDownloaded,
              criteriaReleaseType: preloadedPolicy.criteriaReleaseType,
              criteriaAssetRegex: preloadedPolicy.criteriaAssetRegex,
              retain: preloadedPolicy.retain,
              sortBy: preloadedPolicy.sortBy,
            }
          : { ...EMPTY_CLEANUP_POLICY };

        return {
          data: initialData,
          policy: preloadedPolicy ?? null,
          formatCriteria,
          criteriaEnabled: initialCriteriaEnabled,
        };
      },
      // save service is provided via useForm options
    },
    on: {
      FORMAT_CHANGE: {
        actions: ['changeFormat', 'validate', 'computePristine'],
      },
      TOGGLE_CRITERIA: {
        actions: ['toggleCriteria', 'validate', 'computePristine'],
      },
      RELEASE_TYPE_CHANGE: {
        actions: ['changeReleaseType', 'validate', 'computePristine'],
      },
    },
  });
}
