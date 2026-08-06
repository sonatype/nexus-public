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
 * Components:
 * - FilterSidebar: Filtering sidebar for list pages
 * - EntityTable: Generic sortable table
 * - EmptyState: Empty state placeholder
 * - StatusBadge: Status indicator with colors
 * - PageHeader: Consistent page header
 * - LoadingState: Loading spinner with message
 * - ErrorState: Error display with retry
 * - HelpSection: Contextual help content
 * - StickyActionBar: Sticky header bar for form pages
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
 * } from '.';
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

// Hooks
export {
  useUnsavedChangesWarning,
  clearDirtyState,
  hasUnsavedChanges,
  useIsCloud,
  useHasFirewallLicense,
  useUnreadStatusFailure,
  resetUnreadStatusFailure,
  STATUS_BELL_ACK_STORAGE_KEY,
} from './hooks';
export type { UseUnreadStatusFailureReturn } from './hooks';

// Toast - Viewport-fixed notification system (available but not yet in use)
// Keeping for future implementation - requires app-level provider
export {
  ToastProvider,
  useToast,
  type ToastType,
  type ToastMessage,
} from './Toast';

// Tooltip - Renders inside Theme subtree for proper theme variable inheritance
export {
  Tooltip,
  TooltipContainerProvider,
  usePortalContainer,
} from './Tooltip';

// ThemeSwitcher - Icon-only toggle for dark/light mode
export { ThemeSwitcher } from './ThemeSwitcher';

// Badges - Format and type badges for repositories
export {
  FormatBadge,
  type FormatBadgeProps,
  FormatIcon,
  type FormatIconProps,
  FORMAT_LABELS,
  FORMAT_SVGS,
  FORMAT_IMAGES,
  FORMAT_ICONS,
  TYPE_ICONS,
  DEFAULT_FORMAT_ICON,
  type IconComponent,
} from './Badges';

// ConfirmDialog - Modal confirmation dialog
export { ConfirmDialog, type ConfirmDialogProps } from './ConfirmDialog';

// StickyActionBar - Sticky header bar for form pages
export { StickyActionBar, type StickyActionBarProps } from './StickyActionBar';

// Form Components - Radix UI based form inputs for Preview UI
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
} from './form';

// SearchRadix - Global header search with autocomplete
export { SearchRadix } from './SearchRadix';

// SystemStatusBell - Global header status bell shared by coreui and cloud
export { default as SystemStatusBell } from './SystemStatusBell/SystemStatusBell';

// Navigation Primitives - Reusable navigation components
export {
  NavItem,
  type NavItemProps,
  NavItemBox,
  type NavItemBoxProps,
  useRouteVisibility,
  useContextAwareRouteName,
  useIsPreviewUI,
  PreviewUIContext,
} from './Navigation';

// SessionExpiry - Session expiration handling
export {
  SessionExpiryModal,
  type SessionExpiryModalProps,
  useSessionExpiry,
  type UseSessionExpiryReturn,
} from './SessionExpiry';

// ErrorBoundary - Isolate crash-prone widgets so rest of page stays visible
export { ErrorBoundary, type ErrorBoundaryProps } from './ErrorBoundary';

// MetadataGrid - Two-column metadata display for profile pages
export {
  MetadataGrid,
  type MetadataGridProps,
  type MetadataGridItem,
} from './MetadataGrid';

// TablePagination - Page-based table pagination (Nexus One design)
export {
  TablePagination,
  PAGE_SIZE_OPTIONS,
  type TablePaginationProps,
} from './TablePagination/TablePagination';

// SortableTableHeader - Sortable column headers with aria-sort
export {
  SortableTableHeader,
  TableHeader,
  type SortDirection,
} from './SortableTableHeader';

// Modals - Reusable modal components
export {
  DeleteConfirmationModal,
  type DeleteConfirmationModalProps,
} from './modals';

// DeepResearchLink - Link to Guide component page for deep research
export {
  DeepResearchLink,
  type DeepResearchLinkProps,
} from './DeepResearchLink';

// exportToCsv - Trigger a browser CSV file download from tabular data
export { exportToCsv } from './utils/exportToCsv';

// SystemAlerts - Application-scoped banner host (Preview UI only)
export {
  SystemAlerts,
  type SystemAlertsProps,
  SystemAlert,
  type SystemAlertProps,
  type SystemAlertTier,
  type SystemAlertAction,
  CELimitsAlert,
} from './SystemAlerts';
