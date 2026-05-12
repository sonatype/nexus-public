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
 * @nosc - Nexus One Shared Components
 *
 * Single entry point for all shared NOSC components, hooks, and utilities.
 *
 * Usage:
 *   import { SettingsForm, ConfirmDialog, useToast, EntityTable } from '@nosc';
 */

// ============================================
// Settings Form Components (14)
// ============================================
export {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsCheckbox,
  SettingsCheckboxGroup,
  SettingsCombobox,
  SettingsSelect,
  SettingsTextArea,
  SettingsToggle,
  SettingsAlert,
  SettingsButton,
  SettingsTransferList,
  ConfirmDialog,
  WizardForm,
} from '../components/super/shared/form';

export type { ConfirmDialogProps } from '../components/super/shared/form/ConfirmDialog';
export type { WizardFormProps, WizardStep } from '../components/super/shared/form/WizardForm';

// ============================================
// Page Components (8) + Hooks + Toast
// ============================================
export {
  FilterSidebar,
  EntityTable,
  EmptyState,
  StatusBadge,
  PageHeader,
  LoadingState,
  ErrorState,
  HelpSection,
  FormatBadge,
  TypeBadge,
  useUnsavedChangesWarning,
  clearDirtyState,
  hasUnsavedChanges,
  ToastProvider,
  useToast,
} from '../components/shared';

export type {
  FilterSidebarProps,
  FilterSection,
  FilterOption,
  EntityTableProps,
  TableColumn,
  EmptyStateProps,
  StatusBadgeProps,
  StatusType,
  PageHeaderProps,
  BreadcrumbItem,
  LoadingStateProps,
  ErrorStateProps,
  HelpSectionProps,
  DocLink,
  ToastType,
  ToastMessage,
} from '../components/shared';

// ============================================
// REST API Utilities
// ============================================
export { restClient, ENDPOINTS, parseApiError } from '../utils/api';

// ============================================
// Semantic Icons
// ============================================
export { ActionIcons, StatusIcons, NavIcons } from './icons';
export type { ActionIconName, StatusIconName, NavIconName } from './icons';

// ============================================
// Design Tokens
// ============================================
export { colors, spacing, radii, fontSizes } from './theme';
export type { ColorToken, SpacingToken, RadiusToken, FontSizeToken } from './theme';
