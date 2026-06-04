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
 * Shared UI Components
 *
 * A unified set of reusable components for the Nexus Repository Preview UI.
 * These components follow the design system defined in design-tokens.css
 * and support both light and dark modes.
 *
 * NOTE: Sprint 13 - These components are also exported from @sonatype/nexus-ui-plugin
 * for use in nexus-cloudui-plugin. This file retains local exports for backward
 * compatibility with existing imports in nexus-coreui-plugin.
 *
 * Components:
 * - FilterSidebar: Filtering sidebar for list pages
 * - EntityTable: Generic sortable table
 * - EmptyState: Empty state placeholder
 * - StatusBadge: Status indicator with colors
 * - PageHeader: Consistent page header
 * - LoadingState: Loading spinner with message
 * - ErrorState: Error display with retry
 * - HelpSection: Contextual help content
 *
 * Usage:
 * ```tsx
 * import {
 *   FilterSidebar,
 *   EntityTable,
 *   EmptyState,
 *   StatusBadge,
 *   PageHeader,
 *   LoadingState,
 *   ErrorState,
 *   HelpSection,
 * } from '@/components/shared';
 * ```
 */

// FilterSidebar - Filter controls for list pages
export {
  FilterSidebar,
  type FilterSidebarProps,
  type FilterSection,
  type FilterOption,
} from './FilterSidebar';

// EntityTable - Generic sortable data table
export {
  EntityTable,
  type EntityTableProps,
  type TableColumn,
} from './EntityTable';

// EmptyState - Placeholder for empty lists
export {
  EmptyState,
  type EmptyStateProps,
  type EmptyStateAction,
  type EmptyStateSecondaryAction,
} from './EmptyState';

// StatusBadge - Status indicator
export {
  StatusBadge,
  type StatusBadgeProps,
  type StatusType,
} from './StatusBadge';

// PageHeader - Consistent page header
export {
  PageHeader,
  type PageHeaderProps,
  type BreadcrumbItem,
} from './PageHeader';

// LoadingState - Loading spinner
export {
  LoadingState,
  type LoadingStateProps,
} from './LoadingState';

// ErrorState - Error display
export {
  ErrorState,
  type ErrorStateProps,
} from './ErrorState';

// HelpSection - Contextual help
export {
  HelpSection,
  type HelpSectionProps,
  type DocLink,
} from './HelpSection';

// The following components were moved to preview/shared in nexus-ui-plugin
// (NEXUS-51698). Import them from '@sonatype/nexus-ui-plugin' instead:
//   ErrorBoundary, MetadataGrid, TablePagination, SortableTableHeader,
//   TableHeader, DeleteConfirmationModal, DeepResearchLink

// Badges
export { FormatBadge, type FormatBadgeProps } from './Badges/FormatBadge';
export { TypeBadge, type TypeBadgeProps } from './Badges/TypeBadge';

// Hooks
export {
  useUnsavedChangesWarning,
  clearDirtyState,
  hasUnsavedChanges,
} from './hooks';

// Tooltip - Renders inside Theme subtree for proper theme variable inheritance
export {
  Tooltip,
  TooltipContainerProvider,
  usePortalContainer,
} from './Tooltip/TooltipContainerContext';

// SessionExpiryModal - User-friendly session expiration handling
export {
  SessionExpiryModal,
  type SessionExpiryModalProps,
} from './SessionExpiryModal';

// useSessionExpiry - Hook for managing session expiry state
export {
  useSessionExpiry,
  type UseSessionExpiryReturn,
} from './useSessionExpiry';
