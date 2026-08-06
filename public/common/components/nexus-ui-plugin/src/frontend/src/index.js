/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import './styles/_nx-overrides.scss';
import './styles/_global.scss';

export { default as UIStrings } from './constants/UIStrings';
export { default as APIConstants } from './constants/APIConstants';
export { default as Permissions } from './constants/Permissions';
export { RouteNames } from './constants/RouteNames';
export { ROUTE_NAMES as PreviewRouteNames } from './components/preview/constants/RouteNames';
export { DeleteConfirmationModal } from './components/preview/shared/modals/DeleteConfirmationModal';

export { default as ExtJS } from './interface/ExtJS';
export { default as ExtAPIUtils } from './interface/ExtAPIUtils';
export { default as configureAxios } from './interface/configureAxios';
export { default as configureDebugLogging } from './interface/configureDebugLogging';
export { default as exposeCreateRoot } from './interface/exposeCreateRoot';
export { bootstrapFromREST } from './interface/RestBootstrap';

export { default as Utils } from './interface/Utils';
export { default as UnitUtil } from './interface/UnitUtil';
export { default as FormUtils } from './interface/FormUtils';
export { default as HumanReadableUtils } from './interface/HumanReadableUtils';
export { default as ListMachineUtils } from './interface/ListMachineUtils';
export { default as ValidationUtils } from './interface/ValidationUtils';
export { default as useSimpleMachine } from './interface/SimpleMachineUtils';
export { default as DateUtils } from './interface/DateUtils';
export { ApiUtils } from './interface/ApiUtils';

export { default as HistoricalUsage } from './components/pages/admin/Usage/HistoricalUsage';
export { default as CheckboxControlledWrapper } from './components/widgets/CheckboxControlledWrapper/CheckboxControlledWrapper';
export { default as DynamicFormField } from './components/widgets/DynamicFormField/DynamicFormField';
export { default as FormFieldsFactory } from './components/widgets/FormFieldsFactory/FormFieldsFactory';
export { default as FieldWrapper } from './components/widgets/FieldWrapper/FieldWrapper';
export { default as Information } from './components/widgets/Information/Information';
export { default as ReadOnlyField } from './components/widgets/ReadOnlyField/ReadOnlyField';
export { default as SslCertificateDetailsModal } from './components/widgets/SslCertificateDetailsModal/SslCertificateDetailsModal';
export { default as Textfield } from './components/widgets/Textfield/Textfield';
export { default as UseNexusTruststore } from './components/widgets/UseTruststoreCheckbox/UseNexusTruststore';
export { default as HelpTile } from './components/widgets/HelpTile/HelpTile';

export * from './interface/urlUtil';
export * from './interface/versionUtil';

export * from './utils/clipboardUtils';
export * from './utils/loginUtils';
export * from './utils/devModeUtils';

export * from './hooks/useSideNavbarOpenState';

export * from './interface/LocationUtils';
export * from './interface/NavigationUtils';
export { default as TokenMachine } from './interface/TokenMachine';

export { createRouter } from './router/createRouter';
export { handleExtJsUnsavedChanges, useExtJsUnsavedChangesGuard } from './router/extJsUnsavedChanges';
export { showUnsavedChangesModal, resetDialogState } from './router/unsavedChangesDialog';

export { default as RouteLoadingFallback } from './components/widgets/RouteLoadingFallback/RouteLoadingFallback';

export { ThemeSelector } from './components/widgets/ThemeSelector/ThemeSelector';

export * from './components/layout';

export { default as LoginPage } from './components/pages/login/LoginPage';
export { default as OnboardingWizardMount } from './components/onboarding-wizard/OnboardingWizardMount';

export * from './components/pages/admin/Usage/HistoricalUsageColumns';
export { default as ChangeIcon } from './components/pages/admin/Usage/ChangeIcon';
export { default as UnsavedChangesModal } from './components/widgets/UnsavedChangesModal/UnsavedChangesModal';

export { DirectoryList } from './components/widgets/DirectoryList/DirectoryList';
export { DirectoryPage } from './components/widgets/DirectoryPage/DirectoryPage';
export { NavigationLinkWithCollapsibleList } from './components/widgets/NavigationLinkWithCollapsibleList/NavigationLinkWithCollapsibleList';
export { LeftNavigationMenuItem } from './components/widgets/LeftNavigationMenuItem/LeftNavigationMenuItem';
export { LeftNavigationMenuCollapsibleItem } from './components/widgets/LeftNavigationMenuItem/LeftNavigationMenuCollapsibleItem';
export { LeftNavigationMenuCollapsibleChildItem } from './components/widgets/LeftNavigationMenuItem/LeftNavigationMenuCollapsibleChildItem';

export { default as CleanupPoliciesList } from './components/admin/CleanupPolicies/CleanupPoliciesList';
export { default as CleanupPoliciesForm } from './components/admin/CleanupPolicies/CleanupPoliciesForm';
export { default as CleanupPoliciesDryRun } from './components/admin/CleanupPolicies/CleanupPoliciesDryRun';
export { default as CleanupPoliciesPreview } from './components/admin/CleanupPolicies/CleanupPoliciesPreview';
export { default as CleanupExclusionCriteria } from './components/admin/CleanupPolicies/CleanupExclusionCriteria';
export { default as CleanupPoliciesListMachine } from './components/admin/CleanupPolicies/CleanupPoliciesListMachine';
export { default as CleanupPoliciesFormMachine } from './components/admin/CleanupPolicies/CleanupPoliciesFormMachine';
export { default as CleanupPoliciesDryRunMachine } from './components/admin/CleanupPolicies/CleanupPoliciesDryRunMachine';
export { default as CleanupPoliciesPreviewFormMachine } from './components/admin/CleanupPolicies/CleanupPoliciesPreviewFormMachine';
export { default as CleanupPoliciesPreviewListMachine } from './components/admin/CleanupPolicies/CleanupPoliciesPreviewListMachine';
export * as CleanupPoliciesHelper from './components/admin/CleanupPolicies/CleanupPoliciesHelper';
export { default as CapabilitiesList } from './components/admin/Capabilities/CapabilitiesList';
export { default as CapabilitiesEdit } from './components/admin/Capabilities/CapabilitiesEdit';
export { default as CapabilitiesCreate } from './components/admin/Capabilities/CapabilitiesCreate';
export { default as CapabilitiesListMachine } from './components/admin/Capabilities/CapabilitiesListMachine';
export { default as TasksStrings } from './constants/admin/TasksStrings';
export { default as GlobalEvaluationSettingsMachine } from './interface/GlobalEvaluationSettingsMachine';

// Preview UI: Context Providers (Sprint 13 - Shared Library Migration)
export { AuthProvider, useAuth } from './contexts/AuthContext';
export { PermissionsProvider, usePermissions, usePermissionsLoading } from './contexts/PermissionsContext';
export { ThemeProvider, useTheme, THEMES } from './contexts/ThemeContext';
export { StateProvider, useAppState } from './contexts/StateContext';

// Preview UI: ExtJS Loader Utility
export { isExtJSLoaded, onExtJSLoad, loadExtJS } from './utils/extJsLoader';

// Preview UI: Shared Components (Sprint 13 - Shared Library Migration)
// Note: PageHeader is NOT exported here to avoid collision with Classic UI PageHeader from ./components/layout
export {
  FilterSidebar,
  EntityTable,
  EmptyState,
  StatusBadge,
  LoadingState,
  ErrorState,
  HelpSection,
  useUnsavedChangesWarning,
  clearDirtyState,
  hasUnsavedChanges,
  ToastProvider,
  useToast,
  Tooltip,
  TooltipContainerProvider,
  usePortalContainer,
  ThemeSwitcher,
  // FormatBadge + FormatIcon are the public API for rendering format icons.
  // The underlying icon data (FORMAT_SVGS, FORMAT_IMAGES, FORMAT_ICONS, TYPE_ICONS,
  // DEFAULT_FORMAT_ICON, IconComponent) is intentionally NOT re-exported here — it
  // remains an internal implementation detail of the FormatIcon component. Consumers
  // who need to render a custom format icon should extend FormatIcon rather than
  // reach into the internal data maps.
  FormatBadge,
  FormatIcon,
  FORMAT_LABELS,
  SearchRadix,
  NavItem,
  NavItemBox,
  useRouteVisibility,
  useContextAwareRouteName,
  useIsPreviewUI,
  PreviewUIContext,
  SessionExpiryModal,
  useSessionExpiry,
  useUnreadStatusFailure,
  resetUnreadStatusFailure,
  STATUS_BELL_ACK_STORAGE_KEY,
  SystemStatusBell,
  SystemAlerts,
  SystemAlert,
  CELimitsAlert,
} from './components/preview/shared';

// Preview UI: REST API Utilities
export {
  restClient,
  urlBuilder,
  API_BASE,
  API_V1,
  API_INTERNAL,
  API_INTERNAL_UI,
  ENDPOINTS,
  NEXUS_SESSION_EXPIRED_EVENT,
  notifySessionExpiredFromRest,
  encodeRepositoryItemId,
  decodeRepositoryItemId,
  parseApiError,
  getErrorMessage,
  getFieldError,
  hasFieldErrors,
  fieldErrorsToMap,
  isAuthError,
  isPermissionError,
  isNotFoundError,
  isConflictError,
  isValidationError,
  isServerError,
  isNetworkError,
} from './interface/api';

// Preview UI: XState Form Utilities
export {
  createFormMachine,
  useForm,
  hasValidationErrors,
  extractErrorMessage,
  toPathArray,
} from './interface/form';

// Preview UI: XState List Utilities
export {
  createListMachine,
  useList,
} from './interface/list';

// Preview UI: Settings Form Components (Sprint 19 cache invalidation)
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
} from './components/preview/shared';

// Preview UI: Heritage Navigation Utilities (Preview/Classic route mapping)
export {
  getHeritageEquivalent,
  heritageToPreviewPath,
  previewBrowsePathToHeritageBrowseParam,
  PREVIEW_TO_HERITAGE_ROUTES,
} from './utils/previewHeritageNavigation';

// ============================================================================
// Preview UI: route-entry pages (lazy; each creates its own chunk)
// ============================================================================
import React from 'react';

// Browse pages
export const BrowsePage         = React.lazy(() => import('./components/preview/pages/browse/BrowsePage'));

// Settings hub + layout
export const SettingsHubPage         = React.lazy(() => import('./components/preview/pages/settings/SettingsHubPage'));
export const SettingsPageLayoutRadix = React.lazy(() => import('./components/preview/shared/SettingsPageLayoutRadix'));

// Settings — Repository
export const RepositoriesPage       = React.lazy(() => import('./components/preview/pages/settings/repository/repositories/RepositoriesPage'));
export const RepositoryProfilePage  = React.lazy(() => import('./components/preview/pages/settings/repository/profile/RepositoryProfilePage'));
export const BlobStoresPage         = React.lazy(() => import('./components/preview/pages/settings/repository/blob-stores/BlobStoresPage'));
export const ContentSelectorsPage   = React.lazy(() => import('./components/preview/pages/settings/repository/selectors/ContentSelectorsPage'));
export const CleanupPoliciesPage    = React.lazy(() => import('./components/preview/pages/settings/repository/cleanup/CleanupPoliciesPage'));
export const RoutingRulesPage       = React.lazy(() => import('./components/preview/pages/settings/repository/routing/RoutingRulesPage'));
export const DataStorePage          = React.lazy(() => import('./components/preview/pages/settings/repository/datastore/DataStorePage'));
export const ProprietaryPage        = React.lazy(() => import('./components/preview/pages/settings/repository/proprietary/ProprietaryPage'));

// Settings — Security
export const PrivilegesPage      = React.lazy(() => import('./components/preview/pages/settings/security/privileges/PrivilegesPage'));
export const RolesPage           = React.lazy(() => import('./components/preview/pages/settings/security/roles/RolesPage'));
export const UsersPage           = React.lazy(() => import('./components/preview/pages/settings/security/users/UsersPage'));
export const AnonymousPage       = React.lazy(() => import('./components/preview/pages/settings/security/anonymous/AnonymousPage'));
export const CrowdPage           = React.lazy(() => import('./components/preview/pages/settings/security/crowd/CrowdPage'));
export const LdapPage            = React.lazy(() => import('./components/preview/pages/settings/security/ldap/LdapPage'));
export const OAuth2Page          = React.lazy(() => import('./components/preview/pages/settings/security/oauth2/OAuth2Page'));
export const RealmsPage          = React.lazy(() => import('./components/preview/pages/settings/security/realms/RealmsPage'));
export const SamlPage            = React.lazy(() => import('./components/preview/pages/settings/security/saml/SamlPage'));
export const SslCertificatesPage = React.lazy(() => import('./components/preview/pages/settings/security/sslcertificates/SslCertificatesPage'));
export const UserTokensPage      = React.lazy(() => import('./components/preview/pages/settings/security/user-tokens/UserTokensPage'));
export const ServiceAccountTokensPage = React.lazy(() => import('./components/preview/pages/settings/security/service-account-tokens/ServiceAccountTokensPage'));

// Settings — Support
export const LogsPage           = React.lazy(() => import('./components/preview/pages/settings/support/logs/LogsPage'));
export const LoggingConfigPage  = React.lazy(() => import('./components/preview/pages/settings/support/logging-config/LoggingConfigPage'));
export const SystemInfoPage     = React.lazy(() => import('./components/preview/pages/settings/support/system-info/SystemInfoPage'));
export const MetricHealthPage   = React.lazy(() => import('./components/preview/pages/settings/support/metric-health/MetricHealthPage'));
export const RecoveryModePage   = React.lazy(() => import('./components/preview/pages/settings/support/recovery-mode/RecoveryModePage'));
export const SupportRequestPage = React.lazy(() => import('./components/preview/pages/settings/support/support-request/SupportRequestPage'));
export const SupportZipPage     = React.lazy(() => import('./components/preview/pages/settings/support/support-zip/SupportZipPage'));

// Settings — System
export const ApiPage                = React.lazy(() => import('./components/preview/pages/settings/system/api/ApiPage'));
export const TasksPage              = React.lazy(() => import('./components/preview/pages/settings/system/tasks/TasksPage'));
export const CapabilitiesPage       = React.lazy(() => import('./components/preview/pages/settings/system/capabilities/CapabilitiesPage'));
export const PreviewUiSettingsPage  = React.lazy(() => import('./components/preview/pages/settings/system/preview-ui/PreviewUiSettingsPage'));
export const EmailPage              = React.lazy(() => import('./components/preview/pages/settings/system/email/EmailPage'));
export const HttpPage               = React.lazy(() => import('./components/preview/pages/settings/system/http/HttpPage'));
export const LicensingPage          = React.lazy(() => import('./components/preview/pages/settings/system/licensing/LicensingPage'));
export const NodesPage              = React.lazy(() => import('./components/preview/pages/settings/system/nodes/NodesPage'));
export const UpgradePage            = React.lazy(() => import('./components/preview/pages/settings/system/upgrade/UpgradePage'));
export const UsagePage              = React.lazy(() => import('./components/preview/pages/settings/system/usage/UsagePage'));
export const IqServerPage           = React.lazy(() => import('./components/preview/pages/settings/system/iq-server/IqServerPage'));

// Settings — User account (shared by admin settings and user menu routes)
export const UserAccountPage = React.lazy(() => import('./components/preview/pages/settings/user-account/UserAccountPage'));

// User pages (preview.user.*)
export const UserTokenPage     = React.lazy(() => import('./components/preview/pages/User/UserTokenPage'));
export const NuGetApiTokenPage = React.lazy(() => import('./components/preview/pages/User/NuGetApiTokenPage'));

// Search pages (format-specific)
export const GASearchPage       = React.lazy(() => import('./components/preview/pages/search/results/GASearchPage'));
export const GADetailPage       = React.lazy(() => import('./components/preview/pages/search/details/GADetailPage'));
export const CustomSearchPage   = React.lazy(() => import('./components/preview/pages/search/custom/CustomSearchPage'));
export const NpmSearchPage      = React.lazy(() => import('./components/preview/pages/search/npm/NpmSearchPage'));
export const NpmDetailPage      = React.lazy(() => import('./components/preview/pages/search/npm/NpmDetailPage'));
export const NuGetSearchPage    = React.lazy(() => import('./components/preview/pages/search/nuget/NuGetSearchPage'));
export const NuGetDetailPage    = React.lazy(() => import('./components/preview/pages/search/nuget/NuGetDetailPage'));
export const DockerSearchPage   = React.lazy(() => import('./components/preview/pages/search/docker/DockerSearchPage'));
export const DockerDetailPage   = React.lazy(() => import('./components/preview/pages/search/docker/DockerDetailPage'));
export const PyPISearchPage     = React.lazy(() => import('./components/preview/pages/search/pypi/PyPISearchPage'));
export const PyPIDetailPage     = React.lazy(() => import('./components/preview/pages/search/pypi/PyPIDetailPage'));
export const HelmSearchPage     = React.lazy(() => import('./components/preview/pages/search/helm/HelmSearchPage'));
export const HelmDetailPage     = React.lazy(() => import('./components/preview/pages/search/helm/HelmDetailPage'));
export const GolangSearchPage   = React.lazy(() => import('./components/preview/pages/search/golang/GolangSearchPage'));
export const GolangDetailPage   = React.lazy(() => import('./components/preview/pages/search/golang/GolangDetailPage'));
export const RawSearchPage      = React.lazy(() => import('./components/preview/pages/search/raw/RawSearchPage'));
export const RawDetailPage      = React.lazy(() => import('./components/preview/pages/search/raw/RawDetailPage'));
export const YumSearchPage      = React.lazy(() => import('./components/preview/pages/search/yum/YumSearchPage'));
export const YumDetailPage      = React.lazy(() => import('./components/preview/pages/search/yum/YumDetailPage'));
export const AptSearchPage      = React.lazy(() => import('./components/preview/pages/search/apt/AptSearchPage'));
export const AptDetailPage      = React.lazy(() => import('./components/preview/pages/search/apt/AptDetailPage'));
export const RubyGemsSearchPage = React.lazy(() => import('./components/preview/pages/search/rubygems/RubyGemsSearchPage'));
export const RubyGemsDetailPage = React.lazy(() => import('./components/preview/pages/search/rubygems/RubyGemsDetailPage'));
export const UnifiedSearchPage  = React.lazy(() => import('./components/preview/pages/search/unified/UnifiedSearchPage'));

// Tags / Upload
export const TagsPage       = React.lazy(() => import('./components/preview/pages/tags/TagsPageRadix'));
export const TagDetailPage  = React.lazy(() => import('./components/preview/pages/tags/TagDetailPage'));
export const UploadPage     = React.lazy(() => import('./components/preview/pages/upload/UploadPage'));
export const UploadFormPage = React.lazy(() => import('./components/preview/pages/upload/UploadFormContainer'));

// Top-level preview pages
export const RemediatePage            = React.lazy(() => import('./components/preview/pages/RemediatePage'));
export const WelcomeSuper             = React.lazy(() => import('./components/preview/pages/Welcome/Welcome'));
export const ProtectHub               = React.lazy(() => import('./components/preview/pages/Protect/ProtectHub'));
export const MalwareRemediationWizard = React.lazy(() => import('./components/preview/pages/MalwareRisk/MalwareRemediationWizard'));
export const MaliciousPackagesPage    = React.lazy(() => import('./components/preview/pages/MaliciousPackages/MaliciousPackagesPage'));
export const MalwareRiskPageSuper     = React.lazy(() => import('./components/preview/pages/MalwareRisk/MalwareRiskPageSuper'));

// Security entry pages (moved to preview/shared/security in Task 2)
export const HealthReportPage   = React.lazy(() => import('./components/preview/shared/security/HealthReportPage'));
export const FirewallReportPage = React.lazy(() => import('./components/preview/shared/security/FirewallReportPage'));

// TestHub (Sonatype-internal test pages)
export const SonatypeTestHub                    = React.lazy(() => import('./components/preview/pages/TestHub/SonatypeTestHub'));
export const SonatypeInternalTestDashboard      = React.lazy(() => import('./components/preview/pages/TestHub/test-dashboard/MalwareDashboardTestPage'));
export const SonatypeInternalTestMalwareDefense = React.lazy(() => import('./components/preview/pages/TestHub/test-malware-defense/MalwareDefenseTestPage'));
export const SonatypeInternalTestSearchFw       = React.lazy(() => import('./components/preview/pages/TestHub/test-search-fw/SearchFirewallTestPage'));
export const SonatypeInternalTestSearchHc       = React.lazy(() => import('./components/preview/pages/TestHub/test-search-hc/SearchHealthCheckTestPage'));

// SonatypeInternalTestScenario wraps MalwareDashboardScenarioPage with a transition-param
// unwrapper. Consumers do their own lazyLoad with this named export.
// NOTE: Lazy export avoids breaking tests due to broken relative imports in the compiled dist.
// The dist build generates require('../../pages/Welcome/MalwareStatusCard') instead of
// the correct require('../../Welcome/MalwareStatusCard') from source.
export const MalwareDashboardScenarioPage = React.lazy(() =>
  import('./components/preview/pages/TestHub/test-dashboard/MalwareDashboardTestPage')
    .then(module => ({ default: module.MalwareDashboardScenarioPage }))
);

// PreviewUiSettings admin-gate entry (exported as raw component; consumers apply their own lazy loading)
export { default as PreviewUiSettings } from './components/preview/pages/admin/PreviewUiSettings/PreviewUiSettings';

// Cloud-only admin pages (exported as raw component; consumers apply their own lazy loading)
export { default as IpAllowList } from './components/preview/pages/admin/IpAllowList/IpAllowList';

// Preview UI config + UIStrings
export { PREVIEW_FEATURE_FLAGS, isDevelopmentMode, isFeatureEnabled } from './components/preview/config/featureFlags';
export { default as PreviewUIStrings } from './components/preview/constants/UIStrings';

// ComingSoonPage is used non-lazily in route files for stub routes
export { default as ComingSoonPage } from './components/preview/shared/ComingSoonPage';

// FeatureGate / withFeatureGate: HOC used by coreui admin routes to wrap pages
export { FeatureGate, withFeatureGate, withCloudExcluded } from './components/preview/shared/FeatureGate';

// Preview UI: audit log data/types (relocated from coreui so ui-plugin has no
// cross-package dependency on coreui). coreui's AuditLogPage and
// AuditFilterSidebar re-use these via the package-name import. TypeScript
// type exports (AuditFilters, AuditCategory, AuditEvent, AuditLogResponse)
// live in the sibling index.d.ts so consumers can import them via the
// package name: `import type { AuditFilters } from '@sonatype/nexus-ui-plugin'`.
export { useAuditLogApi } from './utils/audit/useAuditLogApi';
export { formatAuditEvent, formatTimestamp } from './utils/audit/auditEventFormatter';
export {
  CATEGORY_COLORS,
  CATEGORY_LABELS,
  COMMON_EVENT_TYPES,
  COMMON_DOMAINS,
} from './utils/audit/audit.constants';

// Repository types dictionary (used by coreui's TypeBadge; see NEXUS-51698
// scope decision C — the repository/repositories/types file is preview-owned
// but TypeBadge is a coreui Classic-UI primitive that still references the
// canonical TYPE_LABELS).
export { TYPE_LABELS, FORMAT_LABELS as REPOSITORY_FORMAT_LABELS } from './components/preview/pages/settings/repository/repositories/types';

// Shared preview primitives used by coreui's AuditLogPage
export { MobileFilterDrawer } from './components/preview/pages/search/unified/MobileFilterDrawer';
export { useRepositoriesApi } from './components/preview/pages/settings/repository/repositories/useRepositoriesApi';
export { TablePagination } from './components/preview/shared/TablePagination/TablePagination';

// Search filters + repositories hook — used by coreui's LeftNavigationMenuRadix.
export { FORMATS } from './components/preview/pages/search/unified/searchFilters';
export { useRepositories } from './components/preview/pages/search/unified/useRepositories';

// MissingRoutePage — used by coreui's routerConfig + lazyLoad fallback.
export { MissingRoutePage } from './components/preview/pages/MissingRoutePage';

// SETTINGS_SECTIONS — used by coreui's previewAdminRoutes tests.
export { SETTINGS_SECTIONS } from './components/preview/pages/settings/settingsConfig';
