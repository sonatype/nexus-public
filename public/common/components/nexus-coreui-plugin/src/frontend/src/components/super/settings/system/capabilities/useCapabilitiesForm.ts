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

import { useMemo, useEffect } from 'react';
import { useForm } from '@sonatype/nexus-ui-plugin';
import { useToast } from '../../../../shared';
import { createCapabilityFormMachine } from './capabilitiesFormMachine';
import { Capability, CapabilityType, CapabilityFormData } from './types';

export interface UseCapabilitiesFormOptions {
  capabilityId?: string;
  capability?: Capability;
  initialTypeId?: string;
  onCancel: () => void;
  /** Called after successful save - navigates to list. Defaults to onCancel if not provided. */
  onSaveComplete?: () => void;
  createCapability: (data: CapabilityFormData) => Promise<Capability>;
  updateCapability: (data: CapabilityFormData) => Promise<Capability>;
  deleteCapability?: (id: string) => Promise<void>;
}

export interface UseCapabilitiesFormReturn {
  form: ReturnType<typeof useForm>;
  capability: Capability | null;
  capabilityTypes: CapabilityType[];
  selectedCapabilityType: CapabilityType | null;
  isCreate: boolean;
}

/**
 * Custom hook for managing CapabilityForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. The machine loads both the capability being edited
 * (if capabilityId provided) and reference data (capability types with form fields).
 *
 * Capability type changes trigger CAPABILITY_TYPE_CHANGE events (resets dynamic properties).
 */
export function useCapabilitiesForm({
  capabilityId,
  capability,
  initialTypeId,
  onCancel,
  onSaveComplete,
  createCapability,
  updateCapability,
  deleteCapability,
}: UseCapabilitiesFormOptions): UseCapabilitiesFormReturn {
  const toast = useToast();
  const isCreate = !capabilityId && !capability;
  // Use onSaveComplete for post-save navigation, fallback to onCancel
  const handleSaveComplete = onSaveComplete || onCancel;

  // Create the form machine - memoized based on capabilityId and capability
  const machine = useMemo(
    () => createCapabilityFormMachine(capabilityId, capability, initialTypeId),
    [capabilityId, capability, initialTypeId]
  );

  // Use the form machine with action/service overrides
  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: CapabilityFormData; capability: Capability | null }) => {
        try {
          const capabilityToUpdate = capability || ctx.capability;

          if (isCreate) {
            const created = await createCapability(ctx.data);
            toast.success(`Capability "${created.typeName || ctx.data.typeId}" created successfully`);
          } else if (capabilityToUpdate) {
            const updateData = {
              ...ctx.data,
              id: capabilityToUpdate.id,
            };
            const updated = await updateCapability(updateData);
            toast.success(`Capability "${updated.typeName || capabilityToUpdate.typeName || ctx.data.typeId}" updated successfully`);
          }
          // Don't call onCancel() here - let the machine transition to 'saved' state first
          // which will set isPristine=true and clear dirty tracking. Navigation happens
          // after the save completes via the onSaveComplete callback below.
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
      ...(deleteCapability && {
        delete: async (ctx: { data: CapabilityFormData; capability: Capability | null }) => {
          const capToDelete = capability || ctx.capability;
          if (capToDelete) {
            await deleteCapability(capToDelete.id);
            toast.success(`Capability "${capToDelete.typeName || capToDelete.typeId}" deleted successfully`);
            onCancel();
          }
        },
      }),
    },
  });

  // Access the raw state to get the extended context
  const context = (form.state as {
    context: {
      capability: Capability | null;
      capabilityTypes: CapabilityType[];
      selectedCapabilityType: CapabilityType | null;
    };
  }).context;

  // Navigate after save completes - the machine has already set isPristine=true
  // and cleared dirty tracking at this point
  useEffect(() => {
    if (form.isComplete) {
      handleSaveComplete();
    }
  }, [form.isComplete, handleSaveComplete]);

  return {
    form,
    capability: context.capability,
    capabilityTypes: context.capabilityTypes,
    selectedCapabilityType: context.selectedCapabilityType,
    isCreate,
  };
}
