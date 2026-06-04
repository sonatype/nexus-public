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
import { ENDPOINTS, restClient } from '../../../../../../interface/api';
import { createFormMachine, type FormContext, type ValidationErrors } from '../../../../../../interface/form';

import {
  ContentSelector,
  ContentSelectorFormData,
  CONTENT_SELECTOR_TYPE,
  validateName,
} from './types';

/**
 * Validate content selector form data.
 * Returns an object with field names as keys and error messages as values.
 *
 * Checks:
 * - name: required, max length, allowed characters
 * - expression: required, no CSEL blocking errors
 */
function validateContentSelector(
  data: ContentSelectorFormData,
  expressionHasBlockingErrors: boolean
): ValidationErrors {
  const errors: ValidationErrors = {};

  const nameError = validateName(data.name);
  if (nameError) {
    errors.name = nameError;
  }

  if (!data.expression?.trim()) {
    errors.expression = 'Expression is required';
  } else if (expressionHasBlockingErrors) {
    errors.expression = 'Expression has syntax errors';
  }

  return errors;
}

/**
 * Create a content selector form machine with XState.
 *
 * Content selectors have only one type (CSEL), so no editingConfig or
 * type variant sub-states are needed. The machine handles:
 * - Loading selector data (edit mode)
 * - Field validation (name + expression)
 * - Expression validation from the CSEL editor (via UPDATE_EXPRESSION_VALIDATION event)
 * - Save and delete (services provided via useForm options)
 *
 * @param selectorName - Name of selector to edit (undefined for create mode)
 * @param preloadedSelector - Pre-loaded selector data to avoid re-fetching
 */
export function createContentSelectorFormMachine(
  selectorName: string | undefined,
  preloadedSelector?: ContentSelector
) {
  const isEdit = Boolean(selectorName || preloadedSelector);

  return createFormMachine({
    id: `content-selector-form-${selectorName ?? 'new'}`,
    context: {
      data: {
        name: '',
        type: CONTENT_SELECTOR_TYPE,
        description: '',
        expression: '',
      } as ContentSelectorFormData,
      // Reference to the loaded selector (null in create mode)
      selector: preloadedSelector ?? (null as ContentSelector | null),
      // Tracks whether the CSEL editor reports blocking syntax errors
      expressionHasBlockingErrors: false,
    },
    actions: {
      validate: assign((ctx: FormContext<ContentSelectorFormData> & { expressionHasBlockingErrors: boolean }) => ({
        validationErrors: validateContentSelector(ctx.data, ctx.expressionHasBlockingErrors),
      })),
      // Update CSEL editor validation state
      updateExpressionValidation: assign((_context: any, event: any) => ({
        expressionHasBlockingErrors: Boolean(event.hasBlockingErrors),
      })),
    },
    services: {
      load: async () => {
        // Use preloaded data if available, otherwise fetch by name
        const selector = preloadedSelector
          ? preloadedSelector
          : selectorName
          ? await restClient.get<ContentSelector>(
              `${ENDPOINTS.CONTENT_SELECTORS}/${encodeURIComponent(selectorName)}`
            )
          : null;

        // Build initial form data from loaded selector or use defaults
        const initialData: ContentSelectorFormData = selector
          ? {
              name: selector.name,
              type: selector.type || CONTENT_SELECTOR_TYPE,
              description: selector.description || '',
              expression: selector.expression || '',
            }
          : {
              name: '',
              type: CONTENT_SELECTOR_TYPE,
              description: '',
              expression: '',
            };

        return {
          data: initialData,
          selector,
          expressionHasBlockingErrors: false,
        };
      },
      // Placeholder: enables confirmingDelete/deleting/deleted states in the machine.
      // Actual implementation is overridden via useForm options in useContentSelectorForm.
      delete: async () => {
        throw new Error('Delete service not configured');
      },
      // save service is provided via useForm options in useContentSelectorForm
    },
    // Custom event: CSEL editor reports validation changes
    on: {
      UPDATE_EXPRESSION_VALIDATION: {
        actions: ['updateExpressionValidation', 'validate', 'computePristine'],
      },
    },
  });
}
