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

import { useMemo } from 'react';
import { useForm } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createContentSelectorFormMachine } from './contentSelectorFormMachine';
import { ContentSelector, ContentSelectorFormData } from './types';

export interface UseContentSelectorFormOptions {
  /** Name of selector being edited (undefined for create mode) */
  selectorName?: string;
  /** Pre-loaded selector to avoid re-fetching */
  selector?: ContentSelector;
  /** Navigate back to list */
  onCancel: () => void;
  /** Called after successful save/delete to refresh the list */
  onComplete?: () => void;
  /** API: create a new content selector */
  createContentSelector: (data: ContentSelectorFormData) => Promise<unknown>;
  /** API: update an existing content selector */
  updateContentSelector: (name: string, data: ContentSelectorFormData) => Promise<unknown>;
  /** API: delete a content selector */
  deleteContentSelector: (name: string) => Promise<void>;
}

export interface UseContentSelectorFormReturn {
  form: ReturnType<typeof useForm>;
  selector: ContentSelector | null;
  isCreate: boolean;
}

/**
 * Custom hook for managing ContentSelectorForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. The machine loads selector data when editing
 * and validates name + expression fields.
 *
 * Handles save (create/update), delete, and toast notifications.
 */
export function useContentSelectorForm({
  selectorName,
  selector,
  onCancel,
  onComplete,
  createContentSelector,
  updateContentSelector,
  deleteContentSelector,
}: UseContentSelectorFormOptions): UseContentSelectorFormReturn {
  const toast = useToast();
  const isCreate = !(selectorName || selector);

  // Create the form machine - memoized based on selectorName and selector
  const machine = useMemo(
    () => createContentSelectorFormMachine(selectorName, selector),
    [selectorName, selector]
  );

  // Use the form machine with save/delete service overrides
  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: ContentSelectorFormData; selector: ContentSelector | null }) => {
        try {
          const selectorToUpdate = selector || ctx.selector;

          if (isCreate) {
            await createContentSelector(ctx.data);
            toast.success(`Content selector "${ctx.data.name}" created successfully`);
          } else if (selectorToUpdate) {
            await updateContentSelector(selectorToUpdate.name, ctx.data);
            toast.success(`Content selector "${ctx.data.name}" updated successfully`);
          }

          // Notify parent of successful operation
          if (onComplete) {
            onComplete();
          }
          // Navigate back after successful save
          onCancel();
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
      delete: async (ctx: { data: ContentSelectorFormData; selector: ContentSelector | null }) => {
        const selectorToDelete = selector || ctx.selector;
        if (selectorToDelete) {
          await deleteContentSelector(selectorToDelete.name);
          toast.success(`Content selector "${selectorToDelete.name}" deleted`);

          // Notify parent of successful operation
          if (onComplete) {
            onComplete();
          }
          // Navigate back after successful delete
          onCancel();
        }
      },
    },
  });

  // Access the raw state to get the extended context with the loaded selector
  const context = (form.state as { context: { selector: ContentSelector | null } }).context;
  const loadedSelector = context.selector;

  return {
    form,
    selector: loadedSelector,
    isCreate,
  };
}
