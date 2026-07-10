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
 * Task field UI metadata - the single source of truth for how task
 * configuration fields render. Applied at render time in DynamicFormField.
 *
 * This file is intentionally a standalone module so it can be imported
 * without pulling in the entire TaskTypeSelector component tree.
 */

export interface TaskFieldMeta {
  label: string;
  type?: string;
  helpText?: string;
  placeholder?: string;
  min?: number;
  max?: number;
  allowAll?: boolean;
  /** When true, options come from /service/rest/internal/ui/repositories?withFormats=true&withAll=true,
   *  which adds a synthetic "(All Repositories)" entry (withAll) and per-format
   *  "(All <format> Repositories)" entries (withFormats) with IDs like "*-maven2"
   *  (parity with Java RepositoryCombobox.includeEntriesForAllFormats()). */
  includeFormatEntries?: boolean;
  /** Field ID of a repo-type field this field depends on. Keeps this field disabled
   *  until the referenced field has a value, and filters that repo's blobstore from options. */
  dependsOnRepo?: string;
  /** Internal server-managed property. Hidden from the form and excluded from PUT/POST payloads. */
  hidden?: boolean;
  /** Optional per-value validator. Return null if valid, error string if invalid.
   *  Empty input is NOT an error here — required-ness is enforced at the form level. */
  validate?: (value: string) => string | null;
  /** Display order within a task's form. Lower numbers render first. Fields without
   *  an order sort after fields that have one (treated as Infinity). */
  order?: number;
  /** Whether the field must be set before save. Defaults to true; set false to opt out.
   *  Checkboxes and hidden fields are always treated as non-required regardless of this flag. */
  required?: boolean;
  /** When set, the field is only shown when the selected repositoryName matches one of these
   *  repository types. If the selected repo is '*' (All Repositories) or no repo is selected,
   *  the field is always shown. On lookup failure, falls back to showing the field. */
  visibleForRepoTypes?: Array<'hosted' | 'proxy' | 'group'>;
}

export const TASK_FIELD_UI: Record<string, TaskFieldMeta> = {
  // Repository selectors
  repositoryName: { label: 'Repository', type: 'repo', helpText: 'Select the repository to operate on', allowAll: true },
  moveRepositoryName: { label: 'Repository to Move', type: 'repo', helpText: 'Select the repository to move', allowAll: false },
  restrictComponentDelete: {
    label: 'Restrict Delete',
    type: 'repo',
    helpText: 'Restrict the delete to the selected repository or format only',
    includeFormatEntries: true,
    order: 5,
    required: false,
  },

  // Blob store selectors
  blobstoreName: { label: 'Blob Store', type: 'blobstore', helpText: 'Select the blob store' },
  moveTargetBlobstore: { label: 'Target Blob Store', type: 'blobstore', helpText: 'Select the destination blob store', dependsOnRepo: 'moveRepositoryName' },
  // Internal property set at runtime by the move task to record the source blobstore — never user-editable
  moveInitialBlobstore: { label: '', hidden: true },
  memberToRemove: { label: 'Member to Remove', type: 'blobstore', helpText: 'Select the blob store member to remove' },
  fromGroup: { label: 'From Group', type: 'blobstore', helpText: 'Select the blob store group' },

  // Days fields — most are "0 = all/any", so optional
  olderThanDays: { label: 'Age Threshold (days)', type: 'number', helpText: 'Number of days (0 = all files)', min: 0, max: 36500, placeholder: '0', required: false },
  blobsOlderThan: { label: 'Blobs Older Than (days)', type: 'number', helpText: 'Number of days (0 = all blobs)', min: 0, max: 36500, placeholder: '0', required: false },
  firstCreatedDays: { label: 'Tag Creation Age (days)', type: 'number', helpText: 'Delete tags older than this many days (0 = any age)', min: 0, max: 36500, placeholder: '0', order: 1, required: false },
  lastUpdatedDays: { label: 'Tag Modified Age (days)', type: 'number', helpText: 'Delete tags not modified in this many days (0 = any)', min: 0, max: 36500, placeholder: '0', order: 2, required: false },
  snapshotRetentionDays: { label: 'Snapshot Retention (days)', type: 'number', helpText: 'Days to retain snapshots', min: 0, max: 36500, placeholder: '30' },
  gracePeriodInDays: { label: 'Grace Period (days)', type: 'number', helpText: 'Days after release before removal (0 = immediate)', min: 0, max: 36500, placeholder: '0', required: false },
  lastUsed: { label: 'Last Used (days)', type: 'number', helpText: 'Days since last download', min: 1, max: 36500, placeholder: '1' },
  sinceDays: { label: 'Since (days)', type: 'number', helpText: 'Number of days to look back', min: 0, max: 36500, placeholder: '0', required: false },

  // Hours fields
  age: { label: 'Age (hours)', type: 'number', helpText: 'Hours since upload', min: 1, max: 8760, placeholder: '24' },
  deployOffset: { label: 'Deploy Offset (hours)', type: 'number', helpText: 'Hours since last deploy before eligible for cleanup', min: 0, max: 8760, placeholder: '24' },
  sinceHours: { label: 'Hours', type: 'number', helpText: 'Number of hours to look back', min: 0, max: 8760, placeholder: '0' },

  // Minutes
  sinceMinutes: { label: 'Minutes', type: 'number', helpText: 'Number of minutes', min: 0, max: 525600, placeholder: '30' },

  // Count fields
  minimumRetained: { label: 'Minimum Versions Retained', type: 'number', helpText: 'Keep at least this many snapshot versions', min: 1, max: 1000, placeholder: '1' },
  exportThreshold: { label: 'Export Threshold', type: 'number', helpText: 'Number of assets per export batch', min: 1, placeholder: '100' },
  batchSize: { label: 'Batch Size', type: 'number', helpText: 'Number of items per import batch', min: 1, placeholder: '100' },

  // Path fields
  location: { label: 'Backup Location', type: 'string', helpText: 'Absolute server path for the backup file', placeholder: '/tmp/nexus-backup' },
  targetDir: { label: 'Target Directory', type: 'string', helpText: 'Absolute server path to export assets to', placeholder: '/tmp/nexus-export' },
  sourceDir: { label: 'Source Directory', type: 'string', helpText: 'Absolute server path to import files from', placeholder: '/tmp/nexus-import' },

  // Checkbox fields
  removeIfReleased: { label: 'Remove if Released', type: 'checkbox', helpText: 'Remove snapshots that have a corresponding release version' },
  rebuildChecksums: { label: 'Rebuild Checksums', type: 'checkbox', helpText: 'Recalculate checksums during the rebuild' },
  cascadeRebuild: { label: 'Cascade Rebuild', type: 'checkbox', helpText: 'Also rebuild metadata in contained repositories' },
  // RubyGems RebuildVersionsTask - matches RebuildVersionsTaskDescriptor.java
  forceRebuild: { label: 'Force rebuild', type: 'checkbox', helpText: 'Rebuilds even if not marked as out of date' },
  // APT RebuildAptMetadataTask - matches RebuildAptMetadataTaskDescriptor.java
  // These mirror the Classic/ExtJS updateAptRebuildCheckboxVisibility listener behavior.
  rebuildAptMetadataFullRebuild: { label: 'Full rebuild (hosted only)', type: 'checkbox', helpText: 'Repopulate apt_key_value table and execute full rebuild of metadata. Only applies to hosted repositories.', visibleForRepoTypes: ['hosted'] },
  resetProxyMetadata: { label: 'Reset proxy metadata', type: 'checkbox', helpText: 'Clear all generated metadata before rebuild. Use this after changing the upstream URL. Only applies to proxy repositories.', visibleForRepoTypes: ['proxy'] },
  // Helm RebuildHelmMetadataTask - matches RebuildHelmMetadataTaskDescriptor.java
  rebuildHelmMetadataFullRebuild: { label: 'Full rebuild', type: 'checkbox', helpText: 'Repopulate helm_key_value table and execute full rebuild of index.yaml' },
  // Alpine RebuildAlpineMetadataTask - matches RebuildAlpineMetadataTaskDescriptor.java
  rebuildAlpineMetadataFullRebuild: { label: 'Full rebuild', type: 'checkbox', helpText: 'Execute full rebuild of metadata. Use this if the index is out of sync with stored packages.' },
  // Yum YumCreateRepoTask - matches YumCreateRepoTaskDescriptor.java
  yumMetadataCaching: { label: 'Soft repair', type: 'checkbox', helpText: 'Only update the metadata with RPM changes since the last metadata generation' },
  deleteAssociatedComponents: { label: 'Delete Associated Components', type: 'checkbox', helpText: 'Also delete components when removing matched tags', order: 4 },
  enableHardLinks: { label: 'Enable Hard Links', type: 'checkbox', helpText: 'Use hard links instead of copying files (faster, same filesystem only)' },
  enableMalwareCleanup: { label: 'Enable Auto Cleanup', type: 'checkbox', helpText: 'Automatically remove components identified as malicious' },
  enableMalwareCleanupMessage: { label: 'Show Cleanup Message', type: 'checkbox', helpText: 'Display notification when components are cleaned up' },
  onlyNotify: { label: 'Notify Only', type: 'checkbox', helpText: 'Send notifications without making changes (dry run)' },

  // External blobstore metadata task — descriptor declares `external.metadata.repository.format`
  // as a free-form, non-required StringTextFormField. Without this entry the field id contains
  // "repository" so both the API mapper and the render heuristic auto-detect it as a repo
  // combobox, which is wrong — the classic UI renders it as a plain text input.
  'external.metadata.repository.format': {
    label: 'Repository format',
    type: 'string',
    helpText: 'Repository format to retrieve metadata for (leave empty for all formats)',
    placeholder: 'e.g. maven2',
    required: false,
  },

  // Text fields with examples
  nameRegex: {
    label: 'Tag Name Pattern',
    type: 'string',
    helpText: 'Regular expression matching tag names (validated by the server).',
    placeholder: 'release-.*',
    order: 3,
    required: false,
    validate: (value: string) => {
      if (!value) return null;
      try {
        new RegExp(value);
        return null;
      } catch {
        return 'Tag name regex is not a valid regular expression';
      }
    },
  },
  groupId: { label: 'Group ID', type: 'string', helpText: 'Maven group ID to filter (leave empty for all)', placeholder: 'org.apache.commons', required: false },
  artifactId: { label: 'Artifact ID', type: 'string', helpText: 'Maven artifact ID to filter (leave empty for all)', placeholder: 'commons-lang3', required: false },
  baseVersion: { label: 'Base Version', type: 'string', helpText: 'Version to filter (leave empty for all)', placeholder: '1.0-SNAPSHOT', required: false },
  packageName: { label: 'Package Name', type: 'string', helpText: 'npm package name to rebuild metadata for (leave empty for all)', placeholder: '@scope/package-name', required: false },
};

/**
 * Get UI metadata for a task field by ID.
 * Returns undefined if the field is not in the known set.
 */
export function getTaskFieldMeta(fieldId: string): TaskFieldMeta | undefined {
  return TASK_FIELD_UI[fieldId];
}

/**
 * Per-task overrides for repository selector fields.
 *
 * Required because the REST `/v1/tasks/templates` endpoint flattens descriptor
 * form fields to a `Map<String, String>` and drops backend filter metadata
 * (`storeFilters.format`, `storeFilters.type`, `includeAnEntryForAllRepositories`).
 * Until the REST contract exposes that info, the filters live here so the
 * dropdown matches the descriptor and the classic UI.
 *
 * Maintenance: when a new task type with repo filters lands on the backend,
 * read the task's `TaskDescriptor` (private/.../*TaskDescriptor.java —
 * `storeFilters.format`, `storeFilters.type`, `includeAnEntryForAllRepositories`)
 * and add the matching entry here. The string keys must equal the descriptor's
 * `id` exactly; a typo silently falls through to the unfiltered dropdown.
 * If the REST contract is ever extended to ship these fields, delete this map.
 */
export interface TaskRepoFilter {
  formats?: string[];
  types?: string[];
  /**
   * Fully-qualified facet class names (matches the descriptor's
   * `RepositoryCombobox#includingAnyOfFacets`). When set, the dropdown
   * fetches `/service/rest/internal/ui/repositories?facets=...` so the
   * server filters by facet — the client cannot approximate this from
   * format/type alone (e.g. `PurgeUnusedFacet` is on every proxy recipe
   * AND on AnsibleGalaxy hosted).
   */
  facets?: string[];
  /**
   * Maven version-policy filter (matches the descriptor's
   * `including/excludingAnyOfVersionPolicies`). Each entry is passed verbatim
   * to the REST endpoint, so prefix with `!` to exclude (e.g. `["!RELEASE"]`
   * to drop RELEASE-only repos like Maven Snapshot purge does). Triggers the
   * server-side fetch the same way `facets` does.
   *
   * Values must match the backend `VersionPolicy` enum (RELEASE, SNAPSHOT,
   * MIXED — case-insensitive). Unknown values aren't rejected, they just
   * never match a repository, so a typo silently empties the dropdown.
   */
  versionPolicies?: string[];
  includeAll?: boolean;
}

export const TASK_TYPE_REPO_FILTERS: Record<string, Record<string, TaskRepoFilter>> = {
  'repository.rebuild-index': {
    repositoryName: { includeAll: true },
  },
  'repository.purge-unused': {
    repositoryName: {
      facets: ['org.sonatype.nexus.repository.purge.PurgeUnusedFacet'],
      includeAll: true,
    },
  },
  'repository.maven.purge-unused-snapshots': {
    repositoryName: {
      facets: ['org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet'],
      versionPolicies: ['!RELEASE'],
      includeAll: true,
    },
  },
  'repository.docker.gc': {
    repositoryName: {
      facets: ['com.sonatype.nexus.repository.docker.DockerGCFacet'],
      includeAll: true,
    },
  },
  'repository.docker.gc.custom': {
    repositoryName: {
      facets: ['com.sonatype.nexus.repository.docker.DockerGCFacet'],
      includeAll: true,
    },
  },
  'external.blobstore.metadata': {
    // Descriptor uses a bare RepositoryCombobox (no includeAnEntryForAllRepositories()),
    // so we must explicitly opt out of the global allowAll default carried by
    // TASK_FIELD_UI.repositoryName. No facet/format/type filter — the descriptor lists
    // every repo, the user picks one and types its format manually.
    repositoryName: { includeAll: false },
  },
  'repository.maven.publish-dotindex': {
    repositoryName: { formats: ['maven2'], includeAll: true },
  },
  'repository.maven.unpublish-dotindex': {
    repositoryName: { formats: ['maven2'], includeAll: true },
  },
  'repository.maven.repair-base-version': {
    repositoryName: { formats: ['maven2'], types: ['hosted'], includeAll: true },
  },
  'pypi.mark.for.rebuild': {
    repositoryName: { formats: ['pypi'], types: ['hosted'], includeAll: false },
  },
  'repository.apt.rebuild.metadata': {
    repositoryName: { formats: ['apt'], types: ['hosted', 'proxy'], includeAll: true },
  },
  'repository.helm.rebuild.metadata': {
    repositoryName: { formats: ['helm'], types: ['hosted'], includeAll: true },
  },
  'repository.alpine.rebuild.metadata': {
    repositoryName: { formats: ['alpine'], types: ['hosted', 'proxy'], includeAll: true },
  },
  'repository.yum.rebuild.metadata': {
    repositoryName: { formats: ['yum'], types: ['hosted'], includeAll: false },
  },
  'repository.ruby.rebuild.versions': {
    repositoryName: { formats: ['rubygems'], types: ['hosted'], includeAll: false },
  },
};
