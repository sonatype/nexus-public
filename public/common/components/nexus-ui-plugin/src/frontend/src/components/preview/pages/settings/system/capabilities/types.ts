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
 * Capability state enum
 */
export type CapabilityState = 'active' | 'disabled' | 'error' | 'passive';

/**
 * Form field type for dynamic capability forms
 */
export type FormFieldType =
  | 'string'
  | 'text'
  | 'text-area'
  | 'password'
  | 'boolean'
  | 'checkbox'
  | 'number'
  | 'url'
  | 'itemselect'
  | 'combobox'
  | 'repo-or-group-target'
  | 'repo-target';

/**
 * Form field definition from capability type descriptor
 */
export interface FormField {
  id: string;
  type: FormFieldType;
  label: string;
  helpText?: string;
  required: boolean;
  disabled: boolean;
  readOnly: boolean;
  regexValidation?: string;
  initialValue?: string | boolean | number;
  attributes?: Record<string, unknown>;
  minValue?: number;
  maxValue?: number;
  storeApi?: string;
  storeFilters?: Record<string, string>;
  idMapping?: string;
  nameMapping?: string;
  allowAutocomplete?: boolean;
}

/**
 * Capability type - defines what types of capabilities can be created
 */
export interface CapabilityType {
  id: string;
  name: string;
  about?: string;
  formFields?: FormField[];
}

/**
 * Capability instance
 */
export interface Capability {
  id: string;
  typeId: string;
  typeName: string;
  enabled: boolean;
  active: boolean;
  error: boolean;
  state: CapabilityState;
  stateDescription?: string;
  description?: string;
  notes?: string;
  status?: string;
  properties: Record<string, string>;
  tags?: Record<string, string>;
  disableWarningMessage?: string;
  deleteWarningMessage?: string;
  isSystem?: boolean;
}

/**
 * Form data for creating/updating a capability
 */
export interface CapabilityFormData {
  id?: string;
  typeId: string;
  enabled: boolean;
  notes?: string;
  properties: Record<string, string>;
}

/**
 * View mode for the capabilities page
 */
export type CapabilitiesViewMode = 'list' | 'selectType' | 'create' | 'detail';

/**
 * Page props
 */
export interface CapabilitiesPageProps {
  className?: string;
}

/**
 * Helper function to get state icon color
 */
export function getStateColor(state: CapabilityState): string {
  switch (state) {
    case 'active':
      return 'var(--color-success)';
    case 'disabled':
      return 'var(--color-text-secondary)';
    case 'error':
      return 'var(--color-danger)';
    case 'passive':
      return 'var(--color-warning)';
    default:
      return 'var(--color-text-secondary)';
  }
}

/**
 * Helper function to get a displayable state name
 */
export function getStateName(state: CapabilityState): string {
  return state.charAt(0).toUpperCase() + state.slice(1);
}

/**
 * Helper function to get capability description
 */
export function getCapabilityDescription(capability: Capability): string {
  let description = capability.typeName;
  if (capability.description) {
    description += ' - ' + capability.description;
  }
  return description;
}


