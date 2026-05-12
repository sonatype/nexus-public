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
import { createFormMachine, ENDPOINTS, restClient } from '@sonatype/nexus-ui-plugin';
import type { FormContext, ValidationErrors } from '@sonatype/nexus-ui-plugin';

import {
  Capability,
  CapabilityType,
  CapabilityFormData,
  FormField,
} from './types';

// =============================================================================
// VALIDATION
// =============================================================================

/**
 * Validate capability form data.
 * Validates common fields and delegates dynamic field validation to
 * the selected capability type's form field definitions.
 */
function validateCapability(
  data: CapabilityFormData,
  selectedCapabilityType: CapabilityType | null
): ValidationErrors {
  const errors: ValidationErrors = {};

  // Capability type is required
  if (!data.typeId) {
    errors.typeId = 'Capability type is required';
  }

  // Dynamic field validation based on the selected capability type
  if (selectedCapabilityType?.formFields) {
    for (const field of selectedCapabilityType.formFields) {
      const value = data.properties[field.id];

      // Required validation
      if (field.required && (!value || value.trim() === '')) {
        errors[`properties.${field.id}`] = `${field.label} is required`;
        continue;
      }

      // Skip further validation if empty and not required
      if (!value || value.trim() === '') continue;

      // Regex validation
      if (field.regexValidation) {
        try {
          const regex = new RegExp(field.regexValidation);
          if (!regex.test(value)) {
            errors[`properties.${field.id}`] = `${field.label} is invalid`;
          }
        } catch {
          // Invalid regex, skip validation
        }
      }

      // Number range validation
      if (field.type === 'number') {
        const numValue = parseFloat(value);
        if (isNaN(numValue)) {
          errors[`properties.${field.id}`] = `${field.label} must be a number`;
        } else {
          if (field.minValue !== undefined && numValue < field.minValue) {
            errors[`properties.${field.id}`] = `${field.label} must be at least ${field.minValue}`;
          }
          if (field.maxValue !== undefined && numValue > field.maxValue) {
            errors[`properties.${field.id}`] = `${field.label} must be at most ${field.maxValue}`;
          }
        }
      }
    }
  }

  return errors;
}

// =============================================================================
// REST HELPERS
// =============================================================================

/**
 * Fetch capability types from REST API
 */
async function fetchCapabilityTypes(): Promise<CapabilityType[]> {
  try {
    const data = await restClient.get(ENDPOINTS.CAPABILITIES_TYPES);
    if (!Array.isArray(data)) return [];
    return data.map((rest: any) => ({
      id: rest.id,
      name: rest.name,
      about: rest.about,
      formFields: rest.formFields?.map((field: any) => ({
        id: field.id,
        type: field.type,
        label: field.label,
        helpText: field.helpText,
        required: field.required,
        disabled: false,
        readOnly: false,
        initialValue: field.initialValue ?? undefined,
        regexValidation: field.regexValidation,
        minValue: field.minValue ?? undefined,
        maxValue: field.maxValue ?? undefined,
        storeApi: field.storeApi ?? undefined,
        storeFilters: field.storeFilters ?? undefined,
        attributes: field.attributes,
      })),
    })) as CapabilityType[];
  } catch (err) {
    console.error('Failed to load capability types:', err);
    return [];
  }
}

/**
 * Fetch a single capability by ID
 */
async function fetchCapability(capabilityId: string): Promise<Capability | null> {
  try {
    const data = await restClient.get(
      `${ENDPOINTS.CAPABILITIES}/${encodeURIComponent(capabilityId)}`
    );
    if (!data) return null;
    const rest = data as any;
    return {
      id: rest.id,
      typeId: rest.type,
      typeName: rest.type,
      enabled: rest.enabled,
      active: rest.enabled,
      error: false,
      state: rest.enabled ? 'active' : 'disabled',
      notes: rest.notes,
      properties: rest.properties || {},
    } as Capability;
  } catch (err) {
    console.error('Failed to load capability:', err);
    throw err;
  }
}

// =============================================================================
// MACHINE FACTORY
// =============================================================================

/**
 * Create a capability form machine with XState.
 *
 * Capability types are DYNAMIC (come from the API at runtime), each with
 * different form fields. The machine keeps the selected capability type
 * and its field metadata in context. The component reads
 * `context.selectedCapabilityType.formFields` to render dynamic fields.
 *
 * Unlike Privileges (which have a small fixed set of types with editingConfig),
 * capabilities have many possible types and their forms are entirely API-driven.
 * The machine uses a CAPABILITY_TYPE_CHANGE event to switch types and reset
 * dynamic properties.
 */
export function createCapabilityFormMachine(
  capabilityId: string | undefined,
  preloadedCapability?: Capability,
  initialTypeId?: string
) {
  // Initialize form data
  const initialFormData: CapabilityFormData = preloadedCapability
    ? {
        id: preloadedCapability.id,
        typeId: preloadedCapability.typeId,
        enabled: preloadedCapability.enabled,
        notes: preloadedCapability.notes || '',
        properties: { ...preloadedCapability.properties },
      }
    : {
        typeId: initialTypeId ?? '',
        enabled: true,
        notes: '',
        properties: {},
      };

  return createFormMachine({
    id: `capability-form-${capabilityId ?? 'new'}`,
    context: {
      data: initialFormData,
      // Reference data populated by the load service
      capability: preloadedCapability ?? (null as Capability | null),
      capabilityTypes: [] as CapabilityType[],
      selectedCapabilityType: null as CapabilityType | null,
    },
    actions: {
      validate: assign((ctx: any) => ({
        validationErrors: validateCapability(ctx.data, ctx.selectedCapabilityType),
      })),
      // Custom action: update capability type, reset properties, and select type metadata
      changeCapabilityType: assign((context: any, event: any) => {
        const typeId = event.value;
        const capType = (context.capabilityTypes as CapabilityType[]).find(
          (t) => t.id === typeId
        ) ?? null;

        // Initialize properties with defaults from the selected capability type's form fields
        const properties: Record<string, string> = {};
        capType?.formFields?.forEach((field: FormField) => {
          if (field.initialValue !== undefined && field.initialValue !== null) {
            properties[field.id] = String(field.initialValue);
          } else {
            // Default empty value based on type
            properties[field.id] = field.type === 'boolean' ? 'false' : '';
          }
        });

        return {
          data: { ...context.data, typeId, properties },
          touched: { ...context.touched, typeId: true },
          selectedCapabilityType: capType,
        };
      }),
    },
    guards: {},
    services: {
      load: async () => {
        // Load capability and capability types in parallel
        const [capability, capabilityTypes] = await Promise.all([
          preloadedCapability
            ? Promise.resolve(preloadedCapability)
            : capabilityId
            ? fetchCapability(capabilityId).catch((err: unknown) => {
                console.error('Failed to load capability:', err);
                throw err;
              })
            : Promise.resolve(null),
          fetchCapabilityTypes().catch((err: unknown) => {
            console.error('Failed to load capability types:', err);
            return [] as CapabilityType[];
          }),
        ]);

        // Find the selected capability type from the loaded types
        const selectedCapabilityType = capability
          ? capabilityTypes.find((t) => t.id === capability.typeId) ?? null
          : null;

        // Build initial form data
        const initialData: CapabilityFormData = capability
          ? {
              id: capability.id,
              typeId: capability.typeId,
              enabled: capability.enabled,
              notes: capability.notes || '',
              properties: { ...capability.properties },
            }
          : {
              typeId: initialTypeId ?? '',
              enabled: true,
              notes: '',
              properties: {},
            };

        // When creating with an initial type, pre-initialize the properties with defaults
        if (!capability && initialTypeId && initialData.typeId) {
          const capType = capabilityTypes.find((t) => t.id === initialTypeId);
          if (capType?.formFields) {
            capType.formFields.forEach((field: FormField) => {
              if (field.initialValue !== undefined && field.initialValue !== null) {
                initialData.properties[field.id] = String(field.initialValue);
              } else {
                initialData.properties[field.id] = field.type === 'boolean' ? 'false' : '';
              }
            });
          }
        }

        return {
          data: initialData,
          initialData: { ...initialData }, // Ensure machine knows this is the baseline
          capability,
          capabilityTypes,
          selectedCapabilityType: selectedCapabilityType || (initialTypeId ? capabilityTypes.find(t => t.id === initialTypeId) : null),
        };
      },
      // save and delete services are provided via useForm options
    },
    // Custom event for capability type changes
    on: {
      CAPABILITY_TYPE_CHANGE: {
        actions: ['changeCapabilityType', 'validate', 'computePristine'],
      },
    },
  });
}
