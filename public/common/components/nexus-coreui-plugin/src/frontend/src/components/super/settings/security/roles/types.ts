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
 * Role data model matching the backend API (RoleXO)
 */
export interface Role {
  id: string;
  version: string;
  source: string;
  name: string;
  description: string;
  readOnly: boolean;
  privileges: string[];
  roles: string[];
}

/**
 * Role reference (id/name pair)
 */
export interface RoleReference {
  id: string;
  name: string;
}

/**
 * Privilege reference (id/name pair)
 */
export interface PrivilegeReference {
  id: string;
  name: string;
  description?: string;
}

/**
 * Role source reference
 */
export interface RoleSource {
  id: string;
  name: string;
}

/**
 * Create/update role form data
 */
export interface RoleFormData {
  id: string;
  name: string;
  description: string;
  privileges: string[];
  roles: string[];
  source?: string;
  version?: string;
}

/**
 * Form validation errors
 */
export interface RoleFormErrors {
  id?: string;
  name?: string;
  description?: string;
  privileges?: string;
  roles?: string;
}

/**
 * Sort direction type
 */
export type SortDirection = 'asc' | 'desc' | null;

/**
 * Sortable fields for roles list
 */
export type RoleSortField = 'id' | 'name' | 'description' | 'source';

/**
 * Props for RolesPage component
 */
export interface RolesPageProps {
  className?: string;
}

/**
 * Props for RolesList component
 */
export interface RolesListProps {
  onSelect: (roleId: string, mode?: 'profile' | 'edit') => void;
  onDelete?: (roleId: string, roleName?: string) => void;
  onCreate: () => void;
  canDelete?: boolean;
}

/**
 * Props for RoleDetail component
 */
export interface RoleDetailProps {
  role: Role | null;
  loading: boolean;
  canEdit: boolean;
  canDelete: boolean;
  onDelete: () => void;
  onCancel: () => void;
  onComplete?: () => void;
  error?: string;
}

/**
 * Props for RoleForm component
 */
export interface RoleFormProps {
  role?: Role | null;
  isCreate: boolean;
  onCancel: () => void;
  onDelete?: () => void;
  onComplete?: () => void;
  loading?: boolean;
  error?: string;
}

/**
 * Props for PrivilegesSelectionModal
 */
export interface PrivilegesSelectionModalProps {
  isOpen: boolean;
  onClose: () => void;
  availablePrivileges: PrivilegeReference[];
  selectedPrivileges: string[];
  onSave: (privileges: string[]) => void;
  loading?: boolean;
}

/**
 * Props for RolesSelectionModal
 */
export interface RolesSelectionModalProps {
  isOpen: boolean;
  onClose: () => void;
  availableRoles: RoleReference[];
  selectedRoles: string[];
  currentRoleId?: string; // To exclude current role from selection
  onSave: (roles: string[]) => void;
  loading?: boolean;
}

/**
 * Default source for Nexus roles
 */
export const NEXUS_SOURCE = 'Default';
export const DEFAULT_SOURCE = 'default';

/**
 * Check if role is from external source.
 * Default/Nexus source (case-insensitive) is NOT external.
 */
export const isExternalRole = (source: string): boolean => {
  const s = (source || '').toLowerCase();
  return s !== '' && s !== NEXUS_SOURCE.toLowerCase() && s !== DEFAULT_SOURCE && s !== 'nexus';
};

/**
 * Format role source for display.
 * The API returns "default" (lowercase) for built-in roles; the UI should show "Default".
 */
export function formatRoleSourceDisplay(source: string): string {
  if (!source || source === DEFAULT_SOURCE || source === 'Nexus') {
    return 'Default';
  }
  return source;
}

/**
 * Check if role is read-only.
 * A role is locked if:
 * 1. readOnly: true is returned by the API
 * 2. It comes from an external source (LDAP, Crowd, etc.)
 */
export const isReadOnlyRole = (role: Role): boolean => 
  role.readOnly === true || isExternalRole(role.source);


