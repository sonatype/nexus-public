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
 * Privilege data model matching the backend API (PrivilegeXO)
 */
export interface Privilege {
  id: string;
  version: string;
  name: string;
  description: string;
  type: string;
  readOnly: boolean;
  properties: Record<string, string>;
  permission: string;
}

/**
 * Privilege type with form fields
 */
export interface PrivilegeType {
  id: string;
  name: string;
  formFields: FormField[] | null;
}

/**
 * Form field definition from backend
 */
export interface FormField {
  id: string;
  type: string;
  label: string;
  helpText?: string;
  required?: boolean;
  regexValidation?: string;
  initialValue?: string;
  storeApi?: string;
  storeFilters?: Record<string, string>;
  idMapping?: string;
  nameMapping?: string;
  allowAutocomplete?: boolean;
}

/**
 * Privilege reference (id/name pair)
 */
export interface PrivilegeReference {
  id: string;
  name: string;
}

/**
 * Create/update privilege form data
 */
export interface PrivilegeFormData {
  id?: string;
  name: string;
  description: string;
  type: string;
  properties: Record<string, string>;
  version?: string;
}

/**
 * Form validation errors
 */
export interface PrivilegeFormErrors {
  name?: string;
  description?: string;
  type?: string;
  [key: string]: string | undefined; // Dynamic property errors
}

/**
 * Sort direction type
 */
export type SortDirection = 'asc' | 'desc' | null;

/**
 * Sortable fields for privileges list
 */
export type PrivilegeSortField = 'name' | 'description' | 'type' | 'permission';

/**
 * Props for PrivilegesPage component
 */
export interface PrivilegesPageProps {
  className?: string;
}

/**
 * Props for PrivilegesList component
 */
export interface PrivilegesListProps {
  /** Navigate to profile (read-only) - used by row click and View icon */
  onSelect: (privilegeId: string) => void;
  /** Navigate to edit form - used by Edit icon */
  onEdit?: (privilegeId: string) => void;
  onDelete?: (privilegeId: string, privilegeName?: string) => void;
  onCreate: () => void;
  canEdit?: boolean;
  canDelete?: boolean;
}

/**
 * Props for PrivilegeDetail component
 */
export interface PrivilegeDetailProps {
  privilege: Privilege | null;
  loading: boolean;
  canEdit: boolean;
  canDelete: boolean;
  onSave: (data: PrivilegeFormData) => Promise<void>;
  onDelete: () => void;
  onCancel: () => void;
  error?: string;
}

/**
 * Props for PrivilegeForm component
 */
export interface PrivilegeFormProps {
  privilege?: Privilege | null;
  isCreate: boolean;
  onSave: (data: PrivilegeFormData) => Promise<void>;
  onCancel: () => void;
  onDelete?: () => void;
  loading?: boolean;
  error?: string;
}

/**
 * Known privilege types
 */
export const PRIVILEGE_TYPES = {
  APPLICATION: 'application',
  REPOSITORY_ADMIN: 'repository-admin',
  REPOSITORY_CONTENT_SELECTOR: 'repository-content-selector',
  REPOSITORY_VIEW: 'repository-view',
  SCRIPT: 'script',
  WILDCARD: 'wildcard',
} as const;

/**
 * Display names for privilege types
 */
export const PRIVILEGE_TYPE_LABELS: Record<string, string> = {
  [PRIVILEGE_TYPES.APPLICATION]: 'Application',
  [PRIVILEGE_TYPES.REPOSITORY_ADMIN]: 'Repository Admin',
  [PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR]: 'Repository Content Selector',
  [PRIVILEGE_TYPES.REPOSITORY_VIEW]: 'Repository View',
  [PRIVILEGE_TYPES.SCRIPT]: 'Script',
  [PRIVILEGE_TYPES.WILDCARD]: 'Wildcard',
};

/**
 * Get display label for privilege type
 */
export const getPrivilegeTypeLabel = (type: string): string => 
  PRIVILEGE_TYPE_LABELS[type] || type;

/**
 * Check if privilege is read-only
 */
export const isReadOnlyPrivilege = (privilege: Privilege): boolean => privilege.readOnly === true;

/**
 * Option type for form fields
 */
export interface FieldOption {
  value: string;
  label: string;
}

/**
 * Available actions for Application privileges
 * These are application-specific operations beyond standard CRUD
 */
export const APPLICATION_ACTIONS: FieldOption[] = [
  { value: 'create', label: 'Create' },
  { value: 'read', label: 'Read' },
  { value: 'update', label: 'Update' },
  { value: 'delete', label: 'Delete' },
  { value: 'start', label: 'Start' },
  { value: 'stop', label: 'Stop' },
  { value: 'associate', label: 'Associate' },
  { value: 'disassociate', label: 'Disassociate' },
];

/**
 * Available actions for Repository privileges (View & Admin)
 * Standard repository operations
 */
export const REPOSITORY_ACTIONS: FieldOption[] = [
  { value: 'browse', label: 'Browse' },
  { value: 'read', label: 'Read' },
  { value: 'update', label: 'Edit' },
  { value: 'create', label: 'Add' },
  { value: 'delete', label: 'Delete' },
];

/**
 * Available actions for Content Selector privileges
 * Same as repository actions
 */
export const CONTENT_SELECTOR_ACTIONS: FieldOption[] = [
  { value: 'browse', label: 'Browse' },
  { value: 'read', label: 'Read' },
  { value: 'update', label: 'Edit' },
  { value: 'create', label: 'Add' },
  { value: 'delete', label: 'Delete' },
];

/**
 * Available actions for Script privileges
 * Operations related to script execution
 */
export const SCRIPT_ACTIONS: FieldOption[] = [
  { value: 'browse', label: 'Browse' },
  { value: 'read', label: 'Read' },
  { value: 'update', label: 'Edit' },
  { value: 'create', label: 'Add' },
  { value: 'delete', label: 'Delete' },
  { value: 'run', label: 'Run' },
];

/**
 * Get available actions for a privilege type
 */
export const getActionsForPrivilegeType = (type: string): FieldOption[] => {
  switch (type) {
    case PRIVILEGE_TYPES.APPLICATION:
      return APPLICATION_ACTIONS;
    case PRIVILEGE_TYPES.REPOSITORY_ADMIN:
    case PRIVILEGE_TYPES.REPOSITORY_VIEW:
      return REPOSITORY_ACTIONS;
    case PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR:
      return CONTENT_SELECTOR_ACTIONS;
    case PRIVILEGE_TYPES.SCRIPT:
      return SCRIPT_ACTIONS;
    case PRIVILEGE_TYPES.WILDCARD:
      return []; // Wildcard privileges don't have action checkboxes
    default:
      return [];
  }
};

/**
 * Common application domains
 * These are the known domains used throughout the application
 */
export const APPLICATION_DOMAINS: FieldOption[] = [
  { value: '*', label: '* (All)' },
  { value: 'analytics', label: 'analytics' },
  { value: 'apikey', label: 'apikey' },
  { value: 'blobstores', label: 'blobstores' },
  { value: 'bundles', label: 'bundles' },
  { value: 'capabilities', label: 'capabilities' },
  { value: 'component', label: 'component' },
  { value: 'healthcheck', label: 'healthcheck' },
  { value: 'healthchecksummary', label: 'healthchecksummary' },
  { value: 'ldap', label: 'ldap' },
  { value: 'licensing', label: 'licensing' },
  { value: 'logging', label: 'logging' },
  { value: 'metrics', label: 'metrics' },
  { value: 'privileges', label: 'privileges' },
  { value: 'realms', label: 'realms' },
  { value: 'replication', label: 'replication' },
  { value: 'repository-admin', label: 'repository-admin' },
  { value: 'roles', label: 'roles' },
  { value: 'routing-rules', label: 'routing-rules' },
  { value: 'script', label: 'script' },
  { value: 'search', label: 'search' },
  { value: 'security', label: 'security' },
  { value: 'selectors', label: 'selectors' },
  { value: 'settings', label: 'settings' },
  { value: 'ssl-truststore', label: 'ssl-truststore' },
  { value: 'status', label: 'status' },
  { value: 'support', label: 'support' },
  { value: 'tasks', label: 'tasks' },
  { value: 'users', label: 'users' },
  { value: 'userschangepw', label: 'userschangepw' },
  { value: 'wonderland', label: 'wonderland' },
];
