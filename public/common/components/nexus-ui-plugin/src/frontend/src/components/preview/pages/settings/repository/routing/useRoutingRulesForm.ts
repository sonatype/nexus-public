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

import { useMemo, useCallback } from 'react';
import { useForm } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createRoutingRulesFormMachine } from './routingRulesFormMachine';
import { useRoutingRulesApi } from './useRoutingRulesApi';
import type { RoutingRule, RoutingRuleFormData, RoutingMode } from './types';

/**
 * Options for useRoutingRulesForm hook
 */
export interface UseRoutingRulesFormOptions {
  ruleName?: string;
  rule?: RoutingRule;
  onSave?: (data: RoutingRuleFormData) => Promise<void>;
  onCancel: () => void;
}

/**
 * Extended context for reading routing rule from machine state
 */
interface RoutingRulesExtendedContext {
  routingRule: RoutingRule | null;
}

/**
 * Return type of useRoutingRulesForm hook
 */
export interface UseRoutingRulesFormReturn {
  form: ReturnType<typeof useForm>;
  routingRule: RoutingRule | null;
  isCreate: boolean;
  canDelete: boolean;
  matchers: string[];
  handleMatchersChange: (matchers: string[]) => void;
  handleModeChange: (mode: RoutingMode) => void;
}

/**
 * Custom hook for managing RoutingRuleForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. Handles create and update operations with
 * proper toast notifications.
 */
export function useRoutingRulesForm({
  ruleName,
  rule,
  onSave,
  onCancel,
}: UseRoutingRulesFormOptions): UseRoutingRulesFormReturn {
  const toast = useToast();
  const { createRoutingRule, updateRoutingRule } = useRoutingRulesApi();
  const isCreate = !(ruleName || rule);

  // Create the form machine - memoized based on ruleName and rule
  const machine = useMemo(
    () => createRoutingRulesFormMachine(ruleName, rule),
    [ruleName, rule]
  );

  // Use the form machine with action/service overrides
  const form = useForm(machine, {
    actions: {
      onCancel,
    },
    services: {
      save: async (ctx: { data: RoutingRuleFormData } & RoutingRulesExtendedContext) => {
        try {
          const ruleToUpdate = rule || ctx.routingRule;

          if (isCreate) {
            await createRoutingRule(ctx.data);
            toast.success(`Routing rule "${ctx.data.name}" created successfully`);
          } else if (ruleToUpdate) {
            await updateRoutingRule(ruleToUpdate.name, ctx.data);
            toast.success(`Routing rule "${ctx.data.name}" updated successfully`);
          }

          if (onSave) {
            await onSave(ctx.data);
          }

          onCancel();
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
    },
  });

  // Access the raw state context
  const context = (form.state as { context: RoutingRulesExtendedContext }).context;
  const loadedRule = context.routingRule;

  // Can delete only if rule exists and has no assigned repositories
  const canDelete = Boolean(loadedRule) && (loadedRule?.assignedRepositoryCount ?? 0) === 0;

  // Get current matchers from form data
  const matchers = form.data.matchers as string[];

  // Handle matchers array update
  const handleMatchersChange = useCallback(
    (newMatchers: string[]) => {
      form.send({ type: 'UPDATE', name: 'matchers', value: newMatchers });
    },
    [form]
  );

  // Handle mode change
  const handleModeChange = useCallback(
    (mode: RoutingMode) => {
      form.send({ type: 'UPDATE', name: 'mode', value: mode });
    },
    [form]
  );

  return {
    form,
    routingRule: loadedRule,
    isCreate,
    canDelete,
    matchers,
    handleMatchersChange,
    handleModeChange,
  };
}
