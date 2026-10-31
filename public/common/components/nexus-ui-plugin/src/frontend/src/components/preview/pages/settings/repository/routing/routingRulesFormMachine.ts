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
import { restClient } from '../../../../../../interface/api';
import {
  RoutingRule,
  RoutingRuleFormData,
  INITIAL_ROUTING_RULE_FORM,
  NAME_PATTERN,
  NAME_PATTERN_MESSAGE,
} from './types';

// API endpoint for routing rules
const ROUTING_RULES_URL = '/service/rest/internal/ui/routing-rules';

/**
 * Extended form context with the loaded routing rule
 */
interface RoutingRulesFormContext extends FormContext<RoutingRuleFormData> {
  routingRule: RoutingRule | null;
}

/**
 * Validate routing rule form data.
 *
 * - name: required, must match NAME_PATTERN
 * - mode: required (ALLOW or BLOCK)
 * - matchers: at least one non-empty matcher, all must be valid regex
 */
function validateRoutingRule(data: RoutingRuleFormData): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!data.name?.trim()) {
    errors.name = 'Name is required';
  } else if (!NAME_PATTERN.test(data.name)) {
    errors.name = NAME_PATTERN_MESSAGE;
  }

  if (!data.mode) {
    errors.mode = 'Mode is required';
  }

  const validMatchers = data.matchers.filter((m) => m.trim());
  if (validMatchers.length === 0) {
    errors.matchers = 'At least one matcher is required';
  } else {
    for (const matcher of validMatchers) {
      try {
        new RegExp(matcher);
      } catch {
        errors.matchers = `Invalid regex pattern: ${matcher}`;
        break;
      }
    }
  }

  return errors;
}

/**
 * Fetch a routing rule by name from the internal UI API.
 */
async function findRoutingRule(name: string): Promise<RoutingRule | null> {
  try {
    const url = `${ROUTING_RULES_URL}/${encodeURIComponent(name)}`;
    const data = await restClient.get<RoutingRule>(url);
    return data || null;
  } catch (err) {
    console.error('Failed to load routing rule:', err);
    throw err;
  }
}

/**
 * Create a routing rules form machine.
 *
 * This form supports both create and edit modes:
 * - Create mode (no ruleName): starts with INITIAL_ROUTING_RULE_FORM defaults
 * - Edit mode (ruleName provided): loads existing rule from API
 *
 * The form validates:
 * - name: required, alphanumeric + hyphens/underscores, starts with letter
 * - mode: required (ALLOW or BLOCK)
 * - matchers: at least one non-empty pattern, all must be valid regex
 *
 * The mode field (ALLOW/BLOCK) affects help text but not which form fields
 * are visible, so no editingConfig sub-states are used.
 *
 * Delete is supported in edit mode when the rule has no assigned repositories.
 */
export function createRoutingRulesFormMachine(
  ruleName: string | undefined,
  preloadedRule?: RoutingRule
) {
  const hasExistingRule = Boolean(ruleName) || Boolean(preloadedRule);

  return createFormMachine({
    id: `routing-rule-form-${ruleName ?? 'new'}`,
    context: {
      data: { ...INITIAL_ROUTING_RULE_FORM } as RoutingRuleFormData,
      routingRule: preloadedRule ?? (null as RoutingRule | null),
    } as RoutingRulesFormContext,
    actions: {
      validate: assign((ctx: RoutingRulesFormContext) => ({
        validationErrors: validateRoutingRule(ctx.data),
      })),
    },
    services: {
      load: async () => {
        const rule = preloadedRule
          ? preloadedRule
          : ruleName
            ? await findRoutingRule(ruleName).catch((err: unknown) => {
                console.error('Failed to load routing rule:', err);
                throw err;
              })
            : null;

        const initialData: RoutingRuleFormData = rule
          ? {
              name: rule.name,
              description: rule.description || '',
              mode: rule.mode,
              matchers: rule.matchers.length > 0 ? [...rule.matchers] : [''],
            }
          : { ...INITIAL_ROUTING_RULE_FORM };

        return {
          data: initialData,
          routingRule: rule,
        };
      },
      // Delete service: only available in edit mode
      ...(hasExistingRule && {
        delete: async (ctx: RoutingRulesFormContext) => {
          const name = ctx.routingRule?.name || ctx.data.name;
          if (!name) {
            throw new Error('Cannot delete: routing rule name is missing');
          }
          const url = `${ROUTING_RULES_URL}/${encodeURIComponent(name)}`;
          await restClient.delete(url);
        },
      }),
    },
  });
}
