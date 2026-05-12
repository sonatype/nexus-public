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

// =============================================================================
// Repository Types
// =============================================================================

/**
 * Repository type - hosted, proxy, or group
 */
export type RepositoryType = 'hosted' | 'proxy' | 'group';

/**
 * Repository online status
 */
export interface RepositoryStatus {
  online: boolean;
  description?: string;
  reason?: string;
}

/**
 * Storage configuration
 */
export interface StorageConfig {
  blobStoreName: string;
  strictContentTypeValidation: boolean;
  writePolicy?: WritePolicy;
}

/**
 * Write policy for hosted repositories
 */
export type WritePolicy = 'ALLOW' | 'ALLOW_ONCE' | 'DENY';

/**
 * Proxy configuration
 */
export interface ProxyConfig {
  remoteUrl: string;
  contentMaxAge: number;
  metadataMaxAge: number;
}

/**
 * Negative cache configuration for proxy repositories
 */
export interface NegativeCacheConfig {
  enabled: boolean;
  timeToLive: number;
}

/**
 * HTTP connection configuration
 */
export interface HttpClientConnectionConfig {
  retries?: number;
  userAgentSuffix?: string;
  timeout?: number;
  enableCircularRedirects?: boolean;
  enableCookies?: boolean;
  useTrustStore?: boolean;
}

/**
 * HTTP authentication configuration
 */
export interface HttpClientAuthenticationConfig {
  type: 'username' | 'ntlm' | 'bearer';
  username?: string;
  password?: string;
  ntlmHost?: string;
  ntlmDomain?: string;
  bearerToken?: string;
}

/**
 * HTTP client configuration for proxy repositories
 */
export interface HttpClientConfig {
  blocked: boolean;
  autoBlock: boolean;
  connection?: HttpClientConnectionConfig | null;
  authentication?: HttpClientAuthenticationConfig | null;
}

/**
 * Group configuration
 */
export interface GroupConfig {
  memberNames: string[];
  writableMember?: string | null;
}

/**
 * Cleanup policy reference
 */
export interface CleanupConfig {
  policyNames: string[];
}

/**
 * Component configuration for hosted repositories
 */
export interface ComponentConfig {
  proprietaryComponents: boolean;
}

/**
 * Replication configuration for proxy repositories
 */
export interface ReplicationConfig {
  preemptivePullEnabled: boolean;
  assetPathRegex?: string;
}

// =============================================================================
// Format-Specific Configurations
// =============================================================================

/**
 * Maven-specific configuration
 */
export interface MavenConfig {
  versionPolicy: 'RELEASE' | 'SNAPSHOT' | 'MIXED';
  layoutPolicy: 'STRICT' | 'PERMISSIVE';
  contentDisposition: 'INLINE' | 'ATTACHMENT';
}

/**
 * Docker-specific configuration
 */
export interface DockerConfig {
  httpPort?: number | null;
  httpsPort?: number | null;
  forceBasicAuth: boolean;
  v1Enabled: boolean;
  subdomain?: string | null;
}

/**
 * Docker proxy-specific configuration
 */
export interface DockerProxyConfig {
  indexType: 'REGISTRY' | 'HUB' | 'CUSTOM';
  indexUrl?: string | null;
  cacheForeignLayers: boolean;
  foreignLayerUrlWhitelist: string[];
}

/**
 * npm-specific configuration
 */
export interface NpmConfig {
  removeQuarantined?: boolean;
}

/**
 * NuGet proxy configuration
 */
export interface NugetProxyConfig {
  queryCacheItemMaxAge: number;
  nugetVersion: 'V2' | 'V3';
}

/**
 * APT-specific configuration
 */
export interface AptConfig {
  distribution: string;
  flat?: boolean;
}

/**
 * APT signing configuration
 */
export interface AptSigningConfig {
  keypair: string;
  passphrase?: string;
}

/**
 * Yum-specific configuration
 */
export interface YumConfig {
  repodataDepth: number;
  deployPolicy: 'STRICT' | 'PERMISSIVE';
}

/**
 * Raw content disposition configuration
 */
export interface RawConfig {
  contentDisposition: 'INLINE' | 'ATTACHMENT';
}

/**
 * Terraform signing configuration
 */
export interface TerraformSigningConfig {
  keypair: string;
  passphrase?: string;
}

// =============================================================================
// Repository Data Models
// =============================================================================

/**
 * Repository attributes map (used in API requests)
 */
export interface RepositoryAttributes {
  storage?: StorageConfig;
  proxy?: ProxyConfig;
  negativeCache?: NegativeCacheConfig;
  httpClient?: HttpClientConfig;
  group?: GroupConfig;
  cleanup?: CleanupConfig | null;
  component?: ComponentConfig;
  replication?: ReplicationConfig;
  maven?: MavenConfig;
  docker?: DockerConfig;
  dockerProxy?: DockerProxyConfig;
  npm?: NpmConfig;
  nugetProxy?: NugetProxyConfig;
  apt?: AptConfig;
  aptSigning?: AptSigningConfig;
  yum?: YumConfig;
  raw?: RawConfig;
  terraformSigning?: TerraformSigningConfig | null;
}

/**
 * Repository data as returned by the API (read operation)
 */
export interface Repository {
  name: string;
  type: RepositoryType;
  format: string;
  url: string;
  online: boolean;
  status: RepositoryStatus;
  recipe?: string;
  routingRuleId?: string | null;
  attributes?: RepositoryAttributes;
}

/**
 * Repository form data for create/update operations
 */
export interface RepositoryFormData {
  name: string;
  type: RepositoryType;
  format: string;
  recipe: string;
  online: boolean;
  routingRuleId?: string | null;
  storage: StorageConfig;
  proxy?: ProxyConfig;
  negativeCache?: NegativeCacheConfig;
  httpClient?: HttpClientConfig;
  group?: GroupConfig;
  cleanup?: CleanupConfig | null;
  component?: ComponentConfig;
  replication?: ReplicationConfig;
  maven?: MavenConfig;
  docker?: DockerConfig;
  dockerProxy?: DockerProxyConfig;
  npm?: NpmConfig;
  nugetProxy?: NugetProxyConfig;
  apt?: AptConfig;
  aptSigning?: AptSigningConfig;
  yum?: YumConfig;
  raw?: RawConfig;
  terraformSigning?: TerraformSigningConfig | null;
}

/**
 * Repository reference (minimal info for selects/transfers)
 */
export interface RepositoryReference {
  id: string;
  name: string;
  format: string;
  type?: RepositoryType;
  sortOrder?: number;
}

/**
 * Recipe definition (format + type combination)
 */
export interface Recipe {
  format: string;
  type: RepositoryType;
  name?: string;  // Optional - API returns only format+type, name can be constructed as `${format}-${type}`
  description?: string;
}

/**
 * Blob store reference
 */
export interface BlobStore {
  name: string;
  type?: string;
}

/**
 * Routing rule reference
 * Note: REST API uses 'name' as the identifier, not 'id'
 */
export interface RoutingRule {
  name: string;
  description?: string;
  mode?: 'ALLOW' | 'BLOCK';
  matchers?: string[];
}

/**
 * Cleanup policy reference
 */
export interface CleanupPolicy {
  name: string;
  format: string;
  notes?: string;
}

// =============================================================================
// API Response Types
// =============================================================================

/**
 * ExtDirect API response wrapper
 */
export interface ExtDirectResponse<T> {
  data: T;
  success: boolean;
  message?: string;
}

/**
 * Health check status
 */
export interface HealthCheckStatus {
  enabled?: boolean;
  analyzing?: boolean;
  detailedReport?: string | null;
  // Vulnerability counts from health check
  securityIssueCount?: number;
  licenseIssueCount?: number;
  malwareCount?: number | null;
  // Summary info
  iframeHeight?: number;
  iframeWidth?: number;
}

/**
 * Repository list item with extended display info
 * This matches the data returned by /service/rest/internal/ui/repositories/details/
 */
export interface RepositoryListItem extends Repository {
  // From the details endpoint
  inService?: boolean;
  repositoryName?: string;
  // Health check info embedded in list response
  healthCheckEnabled?: boolean;
  healthCheckAnalyzing?: boolean;
  healthCheckSecurityIssues?: number;
  healthCheckLicenseIssues?: number;
}

/**
 * Repository status with extended info
 */
export interface RepositoryStatusXO {
  repositoryName: string;
  online: boolean;
  description?: string;
  reason?: string;
}

/**
 * Firewall status data for a repository
 */
export interface FirewallStatusData {
  repositoryName: string;
  affectedComponentCount: number;
  criticalComponentCount: number;
  severeComponentCount: number;
  moderateComponentCount: number;
  quarantinedComponentCount: number;
  reportUrl?: string;
  message?: string | null;
  errorMessage?: string | null;
}

// =============================================================================
// Form Validation
// =============================================================================

/**
 * Form validation errors type
 */
export interface RepositoryFormErrors {
  name?: string;
  format?: string;
  type?: string;
  recipe?: string;
  storage?: {
    blobStoreName?: string;
    writePolicy?: string;
  };
  proxy?: {
    remoteUrl?: string;
    contentMaxAge?: string;
    metadataMaxAge?: string;
  };
  negativeCache?: {
    timeToLive?: string;
  };
  httpClient?: {
    authentication?: {
      username?: string;
      password?: string;
    };
  };
  group?: {
    memberNames?: string;
    writableMember?: string;
  };
  docker?: {
    httpPort?: string;
    httpsPort?: string;
    subdomain?: string;
  };
  dockerProxy?: {
    indexUrl?: string;
  };
  nugetProxy?: {
    queryCacheItemMaxAge?: string;
  };
  apt?: {
    distribution?: string;
  };
  aptSigning?: {
    keypair?: string;
  };
  terraformSigning?: {
    keypair?: string;
  };
}

// =============================================================================
// Component Props
// =============================================================================

/**
 * Props for RepositoriesPage component
 */
export interface RepositoriesPageProps {
  className?: string;
}

/**
 * Props for RepositoriesList component
 */
export interface RepositoriesListProps {
  onSelect: (name: string) => void;
  onCreate: () => void;
  onDelete?: (name: string) => Promise<void>;
}

/**
 * Props for RepositoryForm component
 */
export interface RepositoryFormProps {
  repository?: Repository | null;
  recipe: Recipe;
  isCreate: boolean;
  onSave: (data: RepositoryFormData) => Promise<void>;
  onCancel: () => void;
  onDelete?: () => void;
  loading?: boolean;
  error?: string;
  /** Hide SettingsForm action buttons (for use in wizard mode) */
  hideActions?: boolean;
  /** When true, form submit validates and advances wizard without creating (proxy deferred flow) */
  advanceOnly?: boolean;
  /** Callback when form validity changes (for wizard Next button enable/disable) */
  onCanAdvanceChange?: (canAdvance: boolean) => void;
}

/**
 * Props for RepositoryTypeSelector (wizard) component
 */
export interface RepositoryTypeSelectorProps {
  onSelect: (recipe: Recipe) => void;
  onCancel: () => void;
  /** Mode to display */
  mode?: 'format' | 'type';
  /** Hide action buttons (for use in wizard mode) */
  hideActions?: boolean;
  /** Currently selected format */
  selectedFormat?: string | null;
  /** Callback when format is selected */
  onFormatSelect?: (format: string | null) => void;
  /** Callback when selection changes - receives whether a valid recipe is selected */
  onSelectionChange?: (canAdvance: boolean, recipe: Recipe | null) => void;
}

/**
 * Props for facet components
 */
export interface FacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  errors?: RepositoryFormErrors;
  isEdit?: boolean;
  disabled?: boolean;
}

// =============================================================================
// Constants
// =============================================================================

/**
 * Repository format display names
 */
export const FORMAT_LABELS: Record<string, string> = {
  maven2: 'Maven',
  npm: 'npm',
  nuget: 'NuGet',
  pypi: 'PyPI',
  docker: 'Docker',
  helm: 'Helm',
  go: 'Go',
  yum: 'Yum',
  apt: 'APT',
  raw: 'Raw',
  rubygems: 'RubyGems',
  r: 'R',
  conan: 'Conan',
  conda: 'Conda',
  cocoapods: 'CocoaPods',
  gitlfs: 'Git LFS',
  p2: 'p2',
  terraform: 'Terraform',
  composer: 'Composer',
  cargo: 'Cargo (Rust)',
  huggingface: 'Hugging Face',
  alpine: 'Alpine',
  swift: 'Swift',
  pub: 'Pub (Dart)',
};

/**
 * Repository type display names
 */
export const TYPE_LABELS: Record<RepositoryType, string> = {
  hosted: 'Hosted',
  proxy: 'Proxy',
  group: 'Group',
};

/**
 * Write policy display options
 */
export const WRITE_POLICY_OPTIONS: { value: WritePolicy; label: string }[] = [
  { value: 'ALLOW', label: 'Allow redeploy' },
  { value: 'ALLOW_ONCE', label: 'Disable redeploy' },
  { value: 'DENY', label: 'Read-only' },
];

/**
 * Docker index type options
 */
export const DOCKER_INDEX_TYPES = {
  REGISTRY: 'REGISTRY',
  HUB: 'HUB',
  CUSTOM: 'CUSTOM',
} as const;

/**
 * Sort direction type
 */
export type SortDirection = 'asc' | 'desc' | null;

/**
 * Sortable fields for repository list
 */
export type RepositorySortField = 'name' | 'type' | 'format' | 'status';

/**
 * Repository name validation pattern
 */
export const NAME_PATTERN = /^[a-zA-Z0-9][-_.a-zA-Z0-9]*$/;
export const NAME_PATTERN_MESSAGE = 'Name must start with a letter or number and may contain letters, numbers, hyphens, underscores, and periods';

// =============================================================================
// Default Values
// =============================================================================

/**
 * Default values for proxy repository
 */
export const DEFAULT_PROXY_VALUES: Partial<RepositoryFormData> = {
  type: 'proxy',
  online: true,
  storage: {
    blobStoreName: '',
    strictContentTypeValidation: true,
  },
  proxy: {
    remoteUrl: '',
    contentMaxAge: -1,
    metadataMaxAge: 1440,
  },
  negativeCache: {
    enabled: true,
    timeToLive: 1440,
  },
  httpClient: {
    blocked: false,
    autoBlock: true,
    connection: null,
    authentication: null,
  },
  cleanup: null,
  replication: {
    preemptivePullEnabled: false,
    assetPathRegex: '',
  },
};

/**
 * Default values for hosted repository
 */
export const DEFAULT_HOSTED_VALUES: Partial<RepositoryFormData> = {
  type: 'hosted',
  online: true,
  storage: {
    blobStoreName: '',
    strictContentTypeValidation: true,
    writePolicy: 'ALLOW_ONCE',
  },
  component: {
    proprietaryComponents: false,
  },
  cleanup: null,
};

/**
 * Default values for group repository
 */
export const DEFAULT_GROUP_VALUES: Partial<RepositoryFormData> = {
  type: 'group',
  online: true,
  storage: {
    blobStoreName: '',
    strictContentTypeValidation: true,
  },
  group: {
    memberNames: [],
  },
};

// =============================================================================
// Validation Helpers
// =============================================================================

/**
 * Validate repository name
 */
export function validateRepositoryName(name: string | undefined): string | undefined {
  if (!name?.trim()) {
    return 'Name is required';
  }
  if (!NAME_PATTERN.test(name)) {
    return NAME_PATTERN_MESSAGE;
  }
  return undefined;
}

/**
 * Validate remote URL for proxy repositories
 */
export function validateRemoteUrl(url: string | undefined): string | undefined {
  if (!url?.trim()) {
    return 'Remote URL is required';
  }
  try {
    new URL(url);
    return undefined;
  } catch {
    return 'Invalid URL format';
  }
}

/**
 * Validate blob store selection
 */
export function validateBlobStore(blobStoreName: string | undefined): string | undefined {
  if (!blobStoreName?.trim()) {
    return 'Blob store is required';
  }
  return undefined;
}

/**
 * Validate group members
 */
export function validateGroupMembers(members: string[] | undefined): string | undefined {
  if (!members || members.length === 0) {
    return 'At least one member repository is required';
  }
  return undefined;
}

/**
 * Check if form has validation errors
 */
export function hasFormErrors(errors: RepositoryFormErrors): boolean {
  return Object.values(errors).some((value) => {
    if (typeof value === 'string') return !!value;
    if (typeof value === 'object' && value !== null) {
      return Object.values(value).some((v) => !!v);
    }
    return false;
  });
}

/**
 * Extract all error messages from form errors
 */
export function getFormErrorMessages(errors: RepositoryFormErrors): string[] {
  const messages: string[] = [];
  for (const value of Object.values(errors)) {
    if (typeof value === 'string' && value) {
      messages.push(value);
    } else if (typeof value === 'object' && value !== null) {
      for (const nestedValue of Object.values(value)) {
        if (typeof nestedValue === 'string' && nestedValue) {
          messages.push(nestedValue);
        }
      }
    }
  }
  return messages;
}