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
 * User data model matching the backend API
 */
export interface User {
  userId: string;
  realm: string;
  source: string;
  firstName: string;
  lastName: string;
  emailAddress: string;
  email?: string; // Alias used in some API responses
  status: 'active' | 'disabled';
  roles: string[];
  externalRoles?: string[];
  readOnly?: boolean;
}

/**
 * User source/realm information
 */
export interface UserSource {
  id: string;
  name: string;
}

/**
 * Role information for assignment
 */
export interface Role {
  id: string;
  name: string;
  description?: string;
  source: string;
  roles?: string[];
  privileges?: string[];
  readOnly?: boolean;
}

/**
 * Create/update user form data
 */
export interface UserFormData {
  userId: string;
  firstName: string;
  lastName: string;
  emailAddress: string;
  password?: string;
  passwordConfirm?: string;
  status: boolean; // true = active, false = disabled
  roles: string[];
  source?: string;
}

/**
 * Form validation errors
 */
export interface UserFormErrors {
  userId?: string;
  firstName?: string;
  lastName?: string;
  emailAddress?: string;
  password?: string;
  passwordConfirm?: string;
  roles?: string;
}

/**
 * Sort direction type
 */
export type SortDirection = 'asc' | 'desc' | null;

/**
 * Sortable fields for users list
 */
export type UserSortField = 'userId' | 'realm' | 'firstName' | 'lastName' | 'email' | 'status';

/**
 * Props for UsersPage component
 */
export interface UsersPageProps {
  className?: string;
}

/**
 * Props for UsersList component
 */
export interface UsersListProps {
  /** Row click and View (eye) icon: navigate to User Profile */
  onSelect: (userId: string, realm: string) => void;
  /** Edit (pencil) icon: navigate to Edit form */
  onEdit?: (userId: string, realm: string) => void;
  onDelete?: (userId: string, userName?: string) => void;
  onCreate: () => void;
  canEdit?: boolean;
  canDelete?: boolean;
  /** When true, fetches OAuth2 users and hides the source filter (cloud distribution only) */
  isCloud?: boolean;
}

/**
 * Props for UserDetail component
 */
export interface UserDetailProps {
  userId: string;
  realm: string;
  onClose: () => void;
  onDelete: () => void;
}

/**
 * Props for UserForm component
 */
export interface UserFormProps {
  user?: User | null;
  /** When editing, pass route params so form can load user before parent fetch completes */
  userId?: string | null;
  userSource?: string | null;
  isCreate: boolean;
  onSave: (data: UserFormData) => Promise<void>;
  onCancel: () => void;
  onDelete?: () => void;
  loading?: boolean;
  error?: string;
  wizardStep?: number;
  hideActions?: boolean;
  onValidationChange?: (isValid: boolean) => void;
  onSubmitRef?: React.MutableRefObject<(() => void) | null>;
}

/**
 * Default source for local users
 */
export const DEFAULT_SOURCE = 'default';

/**
 * User status options
 */
export const USER_STATUSES = {
  active: { id: 'active', label: 'Active' },
  disabled: { id: 'disabled', label: 'Disabled' },
} as const;

/**
 * Check if user is from external source
 */
export const isExternalUser = (source: string): boolean => source !== DEFAULT_SOURCE;

/**
 * Get full name from user
 */
export const getFullName = (user: Pick<User, 'firstName' | 'lastName'>): string => 
  `${user.firstName || ''} ${user.lastName || ''}`.trim();

/**
 * Get source label (Local vs external source name)
 */
export const getSourceLabel = (source: string): string => 
  isExternalUser(source) ? source : 'Local';


