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
 * Capabilities API Hook
 *
 * Migration Status (AgentDev3):
 * - fetchCapabilities: ✅ REST (GET /v1/capabilities)
 * - fetchCapabilityTypes: ✅ REST (GET /v1/capabilities/types)
 * - createCapability: ✅ REST (POST /v1/capabilities)
 * - updateCapability: ✅ REST (PUT /v1/capabilities/{id})
 * - deleteCapability: ✅ REST (DELETE /v1/capabilities/{id})
 * - enableCapability: ✅ REST (PUT /v1/capabilities/{id} with enabled: true)
 * - disableCapability: ✅ REST (PUT /v1/capabilities/{id} with enabled: false)
 */

import { useState, useCallback } from 'react';
import { restClient, parseApiError, urlBuilder } from '@/utils/api';
import { Capability, CapabilityType, CapabilityFormData } from './types';

// =============================================================================
// REST API RESPONSE TYPES
// =============================================================================

interface RestCapability {
  id: string;
  type: string;
  typeName?: string;
  notes?: string;
  enabled: boolean;
  active?: boolean;
  error?: boolean;
  state?: string;
  stateDescription?: string;
  description?: string;
  properties: Record<string, string>;
  tags?: Record<string, string>;
}

interface RestCapabilityType {
  id: string;
  name: string;
  about?: string;
  formFields?: Array<{
    id: string;
    type: string;
    label: string;
    helpText?: string;
    required: boolean;
    initialValue?: string | boolean | number | null;
    minValue?: number | null;
    maxValue?: number | null;
    storeApi?: string | null;
    storeFilters?: Record<string, unknown>[] | null;
    attributes?: Record<string, unknown>;
  }>;
}

// =============================================================================
// TRANSFORMERS
// =============================================================================

/**
 * Transform REST capability to internal Capability type.
 * REST API now includes typeName, state, description, and tags.
 */
function restToCapability(rest: RestCapability): Capability {
  return {
    id: rest.id,
    typeId: rest.type,
    typeName: rest.typeName || rest.type,
    enabled: rest.enabled,
    active: rest.active ?? rest.enabled,
    error: rest.error ?? false,
    state: (rest.state as Capability['state']) || (rest.enabled ? 'active' : 'disabled'),
    stateDescription: rest.stateDescription,
    description: rest.description,
    notes: rest.notes,
    properties: rest.properties || {},
    tags: rest.tags,
  };
}

/**
 * Transform REST capability type to internal CapabilityType.
 */
function restToCapabilityType(rest: RestCapabilityType): CapabilityType {
  return {
    id: rest.id,
    name: rest.name,
    about: rest.about,
    formFields: rest.formFields?.map((field) => ({
      id: field.id,
      type: field.type as any,
      label: field.label,
      helpText: field.helpText,
      required: field.required,
      disabled: false,
      readOnly: false,
      initialValue: field.initialValue ?? undefined,
      minValue: field.minValue ?? undefined,
      maxValue: field.maxValue ?? undefined,
      storeApi: field.storeApi ?? undefined,
      storeFilters: field.storeFilters ?? undefined,
      attributes: field.attributes,
    })),
  };
}

/**
 * Transform form data to REST create/update payload.
 * Property values are coerced to strings — the Java backend
 * throws NullPointerException on null/undefined values (bug jqxh).
 */
function formDataToRestPayload(data: CapabilityFormData) {
  const sanitizedProperties: Record<string, string> = {};
  if (data.properties) {
    for (const [key, value] of Object.entries(data.properties)) {
      sanitizedProperties[key] = value ?? '';
    }
  }
  return {
    id: data.id,
    type: data.typeId,
    enabled: data.enabled,
    notes: data.notes || '',
    properties: sanitizedProperties,
  };
}

// =============================================================================
// HOOK
// =============================================================================

/**
 * Custom hook for Capabilities API operations
 * Uses REST API (v1/capabilities)
 */
export function useCapabilitiesApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch all capabilities
   */
  const fetchCapabilities = useCallback(async (): Promise<Capability[]> => {
    try {
      const url = urlBuilder.capabilities.list();
      const response = await restClient.get<RestCapability[]>(url);
      return Array.isArray(response) ? response.map(restToCapability) : [];
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      console.error('Failed to fetch capabilities:', err);
      throw new Error(apiError.message || 'Failed to load capabilities');
    }
  }, []);

  /**
   * Fetch all capability types
   */
  const fetchCapabilityTypes = useCallback(async (): Promise<CapabilityType[]> => {
    try {
      const url = urlBuilder.capabilities.types();
      const response = await restClient.get<RestCapabilityType[]>(url);
      return Array.isArray(response) ? response.map(restToCapabilityType) : [];
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      console.error('Failed to fetch capability types:', err);
      throw new Error(apiError.message || 'Failed to load capability types');
    }
  }, []);

  /**
   * Create a new capability
   */
  const createCapability = useCallback(async (data: CapabilityFormData): Promise<Capability> => {
    setLoading(true);
    setError(null);
    try {
      const url = urlBuilder.capabilities.create();
      const payload = formDataToRestPayload(data);
      const response = await restClient.post<RestCapability>(url, payload);
      return restToCapability(response);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to create capability');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Update an existing capability
   */
  const updateCapability = useCallback(async (data: CapabilityFormData): Promise<Capability> => {
    setLoading(true);
    setError(null);
    try {
      if (!data.id) {
        throw new Error('Capability ID is required for update');
      }
      const url = urlBuilder.capabilities.update(data.id);
      const payload = formDataToRestPayload(data);
      const response = await restClient.put<RestCapability>(url, payload);
      return restToCapability(response);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to update capability');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete a capability
   */
  const deleteCapability = useCallback(async (id: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const url = urlBuilder.capabilities.delete(id);
      await restClient.delete(url);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to delete capability');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Enable a capability
   */
  const enableCapability = useCallback(async (capability: Capability): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const updateUrl = urlBuilder.capabilities.update(capability.id);
      const payload = formDataToRestPayload({
        id: capability.id,
        typeId: capability.typeId,
        enabled: true,
        notes: capability.notes,
        properties: capability.properties,
      });
      await restClient.put(updateUrl, payload);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to enable capability');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Disable a capability
   */
  const disableCapability = useCallback(async (capability: Capability): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const updateUrl = urlBuilder.capabilities.update(capability.id);
      const payload = formDataToRestPayload({
        id: capability.id,
        typeId: capability.typeId,
        enabled: false,
        notes: capability.notes,
        properties: capability.properties,
      });
      await restClient.put(updateUrl, payload);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to disable capability');
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    setError,
    fetchCapabilities,
    fetchCapabilityTypes,
    createCapability,
    updateCapability,
    deleteCapability,
    enableCapability,
    disableCapability,
  };
}

export default useCapabilitiesApi;



