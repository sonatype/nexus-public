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
 * IQ Server configuration data model
 */
export interface IqServerConfiguration {
  enabled: boolean;
  url: string;
  authenticationType: 'USER' | 'PKI' | '';
  username: string;
  password: string;
  useTrustStoreForUrl: boolean;
  timeoutSeconds: number | null;
  properties: string;
  showLink: boolean;
  // Server-returned only; not sent on writes.
  licensedSolutions?: Array<{ id: string; url: string }>;
  hasFirewall?: boolean;
}

/**
 * IQ Server connection verification result
 */
export interface IqVerificationResult {
  success: boolean;
  reason?: string;
}

/**
 * IQ Server capabilities data model
 */
export interface IqCapabilities {
  hasFirewall: boolean;
  hasLifecycle: boolean;
  connected: boolean;
  url: string | null;
}

/**
 * Default IQ Server capabilities
 */
export const DEFAULT_IQ_CAPABILITIES: IqCapabilities = {
  hasFirewall: false,
  hasLifecycle: false,
  connected: false,
  url: null,
};

/**
 * Default IQ Server configuration
 */
export const DEFAULT_IQ_CONFIGURATION: IqServerConfiguration = {
  enabled: false,
  url: '',
  authenticationType: '',
  username: '',
  password: '',
  useTrustStoreForUrl: false,
  timeoutSeconds: null,
  properties: '',
  showLink: true,
};

/**
 * Validation errors for IQ Server configuration form
 */
export interface IqValidationErrors {
  url?: string;
  authenticationType?: string;
  username?: string;
  password?: string;
  timeoutSeconds?: string;
}

// Backend accepts this on PUT to reuse the stored password without transmitting it in clear text.
export const PASSWORD_PLACEHOLDER = '#~NXRM~PLACEHOLDER~PASSWORD~#';

/**
 * A single IQ Server "properties" name/value row.
 */
export interface IqProperty {
  id: string;
  name: string;
  value: string;
}

/**
 * Validation result for a single property row.
 */
export interface PropertyValidation {
  id: string;
  error?: string;
}

/**
 * Form-shape variant of IqServerConfiguration: `properties` is a parsed row array
 * instead of the raw wire string, plus one load-time-only informational field.
 */
export interface IqServerFormData extends Omit<IqServerConfiguration, 'properties'> {
  properties: IqProperty[];
  /** Count of non-blank lines from the loaded `properties` string that weren't valid
   *  name=value pairs (comments, ':'-separated, etc.) and would be dropped on next save.
   *  Set once at load; excluded from the PUT payload. */
  propertiesDroppedLineCount: number;
}

/* ------------------------------------------------------------------
 * Hosted Repository Evaluation — Settings options
 *
 * Single source of truth for dropdown values, defaults, and types.
 * Types are derived from the option arrays so the type/data can't drift.
 * ------------------------------------------------------------------ */

export const ACTIVITY_TIME_FRAME_OPTIONS = [
  { value: 30, label: '30 Days' },
  { value: 60, label: '60 Days' },
  { value: 90, label: '90 Days' },
] as const;
export type ActivityTimeFrame = typeof ACTIVITY_TIME_FRAME_OPTIONS[number]['value'];

export const ARTIFACT_LATEST_VERSIONS_OPTIONS = [1, 2, 3, 4, 5] as const;
export type ArtifactLatestVersions = typeof ARTIFACT_LATEST_VERSIONS_OPTIONS[number];

// Uppercase/underscored form — matches the global /evaluation/settings API.
// (RepositoryEvaluationTab uses a lowercase/hyphenated variant for its override endpoint.)
export const POLICY_EVALUATION_STAGES = ['RELEASE', 'STAGE_RELEASE', 'BUILD', 'OPERATE'] as const;
export type PolicyEvaluationStage = typeof POLICY_EVALUATION_STAGES[number];

export const MONITORING_FILTER_OPTIONS = [
  { value: 'all', label: 'All monitoring' },
  { value: 'enabled', label: 'Enabled' },
  { value: 'disabled', label: 'Disabled' },
] as const;
export type MonitoringFilter = typeof MONITORING_FILTER_OPTIONS[number]['value'];

/* ------------------------------------------------------------------
 * Hosted Repository Evaluation — Domain models
 * Consumed by the useHostedRepoEvaluation hook and its callers.
 * ------------------------------------------------------------------ */

export interface GlobalEvaluationSettings {
  activityTimeFrame: ActivityTimeFrame;
  artifactLatestVersions: ArtifactLatestVersions;
  policyEvaluationStage: PolicyEvaluationStage;
  autoEnrollNewRepos: boolean;
}

export const DEFAULT_SETTINGS: GlobalEvaluationSettings = {
  activityTimeFrame: 30, // 30/60/90 days available; 30 is the Classic UI default
  // Both time-frame and latest-versions are always active. Default matches Classic.
  artifactLatestVersions: 5,
  policyEvaluationStage: 'RELEASE',
  autoEnrollNewRepos: false,
};

export interface DashboardRepository {
  id: string;
  name: string;
  format: string;
  size: number | null;
  componentCount: number | null;
  isMonitored: boolean;
  hasCustomConfig?: boolean;
}

export interface DashboardQuery {
  page: number;
  pageSize: number;
  sortBy?: 'name' | 'format' | 'size' | 'componentCount' | 'monitoring';
  sortDir?: 'asc' | 'desc';
  search?: string;
  formatFilter?: string;
  monitoringFilter?: MonitoringFilter;
}

export interface DashboardPage {
  rows: DashboardRepository[];
  totalCount: number;
  monitoredCount: number;
  page: number;
  pageSize: number;
  globalConfigAvailable: boolean;
}

export interface SettingsWithRepos {
  settings: GlobalEvaluationSettings;
  monitoredRepoIds: string[];
  totalRepoCount: number;
}

export interface SelectionDelta {
  addRepositoryIds?: string[];
  removeRepositoryIds?: string[];
}

export interface SaveResult {
  ok: boolean;
  message?: string;
}

/* ------------------------------------------------------------------
 * IQ Connected — API response models (consumed by useIqConnectedApi)
 * ------------------------------------------------------------------ */

export interface LicensedSolution {
  id: string;
  url: string;
}

export interface IqConfigResponse {
  enabled: boolean;
  showLink: boolean;
  url: string;
  authenticationType: 'USER' | 'PKI' | '';
  username: string;
  password: string;
  useTrustStoreForUrl: boolean;
  timeoutSeconds: number | null;
  properties: string;
  failOpenModeEnabled: boolean;
  licensedSolutions: LicensedSolution[];
  hasFirewall: boolean;
}

export interface IqVerifyResult {
  success: boolean;
  reason?: string;
  applicationCount?: number;
}

export interface DashboardSummary {
  numberOfMonitoredRepositories: number;
  totalRepositories: number;
  globalConfigAvailable: boolean;
  hasSelections: boolean;
}

export interface EvaluationSettingsSummary {
  activityTimeFrame: number;
  artifactLatestVersions: number;
  policyEvaluationStage: string;
  monitoredRepoCount: number;
  totalRepoCount: number;
}
