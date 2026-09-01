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
  /** Visible row count for multi-line ('text') fields. Defaults to 4 when unset. */
  rows?: number;
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
  /** For type 'alertBanner': the banner copy to render (banners carry no value; copy is not
   *  available via the REST template, so it is hard-coded here keyed by task + field id). */
  bannerText?: string;
  /** For type 'alertBanner': maps to the SettingsAlert `type` (visual variant + a11y role). */
  bannerVariant?: 'info' | 'warning' | 'error' | 'success';
  /** Conditional visibility tied to the `taskScope` property: the field renders only when
   *  `values.taskScope` equals this value. Used by the Data Repair Plan task to toggle the
   *  duration fields (scope 'duration') vs the start/end date fields (scope 'dates'). */
  scope?: 'duration' | 'dates';
  /** For type 'taskScope': the scope value to assume when `values.taskScope` is unset (create flow
   *  or a legacy task missing the property). Defaults to 'duration' via resolveDefaultScope. The
   *  Execute Data Repair Plan task sets this to 'dates'. */
  defaultScope?: 'duration' | 'dates';
  /** For type 'staticInfo': alternate section label used on the cloud edition (the descriptor uses a
   *  different label per edition for the same field id, e.g. planOptionsLabelId). */
  cloudLabel?: string;
  /** Render an itemselect/repo field as a dual-list transfer selector instead of a single
   *  combobox (ItemselectFormField parity). */
  multiSelect?: boolean;
  /** For a multi-select repository field: narrow the Available column to repositories whose
   *  assigned blob store is in the current `blobstoreName` selection (mirrors the Classic
   *  `filterRepositoryBySelectedBlobstore` listener). Self-hosted only. */
  filterByBlobstore?: boolean;
  /** Render this field read-only: display the stored value(s) with no editing. Used by the Execute
   *  Data Repair Plan task, which mirrors Classic renderExecutePlanFields (taskScope, dates, blob
   *  store, and repository are all read-only, sourced from the task's stored properties). */
  readOnly?: boolean;
  /** Exclude this field entirely from POST/PUT payloads. Distinct from `readOnly` (which is a UI
   *  concern): some readOnly fields still need to be sent (e.g. taskScope is required by the REST
   *  validator), while derived-display-only fields (blobstoreName, repositoryName, dates on the
   *  Execute task) must never be persisted. */
  neverSerialize?: boolean;
  /** Marks a descriptor TemplateFormField named `name` whose initialValue is the default task
   *  name (not a persisted property). Hidden from the form, used to prefill the task name, and
   *  never serialized into the properties map. */
  isNameTemplate?: boolean;
  /** For a multi-select (itemselect) field where an empty selection means "all": the copy shown in
   *  the Selected column when nothing is selected, so it reads e.g. "All Blob Stores selected"
   *  instead of "No items selected". Does not change the serialized value. */
  selectedEmptyText?: string;
  /** Omit this property from the save payload when its value is empty (or the "(All Blob Stores)"
   *  sentinel) — Classic does not persist a blob-store/repository selector with no explicit value
   *  (its exportProperties sets null, which the backend drops). */
  omitWhenEmpty?: boolean;
  /** When the value is empty, serialize it as this literal string instead of "". Classic's ExtJS
   *  number fields export `String(field.value)`, i.e. the literal "null" for an empty duration
   *  field; this reproduces that byte-for-byte for Classic ↔ Preview payload parity. */
  serializeEmptyAs?: string;
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

  // Blob store selectors — assign an explicit order so the primary blobstore picker renders
  // before sibling configuration fields (e.g. olderThanDays, blobsOlderThan). The REST
  // template endpoint flattens descriptor form fields into a HashMap, so without this the
  // iteration order is non-deterministic and the picker may render below its companions.
  blobstoreName: { label: 'Blob Store', type: 'blobstore', helpText: 'Select the blob store', order: 1 },
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
  minimumRetained: { label: 'Minimum Versions Retained', type: 'number', helpText: 'Minimum snapshots to retain per groupId:artifactId (use -1 to delete all snapshots)', min: -1, max: 1000, placeholder: '1' },
  exportThreshold: { label: 'Threshold (in days)', type: 'number', helpText: 'Only export assets that have not been updated or downloaded in the configured number of days; leave blank to include all assets.', min: 1, required: false, placeholder: '30' },
  batchSize: { label: 'Batch Size', type: 'number', helpText: 'Enter batch size for the import', min: 1, required: false, placeholder: '100' },

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
  onlyNotify: { label: 'Notify Only', type: 'checkbox', helpText: 'Send notifications without making changes (dry run)' },
  // Cluster-only checkbox injected by TaskDescriptorSupport.newMultinodeFormField() on many
  // task types when nodeAccess.isClustered(). Label/helpText mirror MULTINODE_LABEL/MULTINODE_HELP
  // so it renders as an optional checkbox (checkboxes are never required) instead of the
  // fallback heuristic's required "Multinode *".
  multinode: { label: 'Multi node', type: 'checkbox', helpText: 'Run task on all nodes in the cluster.' },

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
  vendor: { label: 'Vendor', type: 'string', helpText: 'Scope the rebuild to packages with the specified vendor (leave empty for all)', placeholder: 'acme', required: false },
  baseUrl: { label: 'Base URL', type: 'url', helpText: 'Base URL of the Nexus Repository server. Required if Base URL Capability is not configured.', required: false },

  // Script task (ScriptTaskDescriptor) — `language` is a plain string and `source` is a
  // multi-line script body (TextAreaFormField). The REST template flattens both to
  // string-valued keys and drops the FormField type, so without these entries `source`
  // (empty initial value) is misdetected as a checkbox by the restTemplateToTaskType
  // fallback heuristic. Labels/help mirror the descriptor messages. No syntax highlighting:
  // the classic ExtJS UI renders a plain <textarea>, and parity is preferred. Both are
  // MANDATORY in the descriptor, so they stay required (the default).
  language: { label: 'Language', type: 'string', helpText: 'Script language' },
  source: { label: 'Script Source', type: 'text', helpText: 'Script source to execute', rows: 12 },
};

/**
 * Get UI metadata for a task field by ID.
 * Returns undefined if the field is not in the known set.
 */
export function getTaskFieldMeta(fieldId: string): TaskFieldMeta | undefined {
  return TASK_FIELD_UI[fieldId];
}

/**
 * FALLBACK per-task repository-selector filters.
 *
 * The Preview UI now derives each repository field's list from the backend descriptor's
 * `storeApi` + `storeFilters` (shipped on every task template's formFields via
 * `TaskTemplateXO`/`FormFieldInfo`) through the internal /repositories endpoint — see
 * `taskRepoQuery.ts` (`buildRepoQuery`) and `DynamicFormFields`. This map is consulted ONLY when a
 * template omits `storeApi` (OSS/older builds whose `restTemplateToTaskType` takes the legacy
 * properties-only path).
 *
 * Do NOT add new entries for Pro descriptors — fix the descriptor's `RepositoryCombobox` so the
 * correct `storeApi`/`storeFilters` ship automatically. The string keys must equal the descriptor's
 * `id` exactly.
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

/**
 * Per-task overrides for blobstore selector fields.
 *
 * Required because the REST `/v1/tasks/templates` endpoint flattens descriptor
 * form fields to a `Map<String, String>` and drops the backend `storeApi`
 * choice. The four blobstore-flavoured combobox endpoints used by descriptors
 * (`coreui_Blobstore.read`, `readWithAll`, `readNoneGroupEntriesIncludingEntryForAll`,
 * `readGroups`) differ in two ways relevant to the UI:
 *   - includeAll: prepend the synthetic "(All Blob Stores)" entry (readWithAll +
 *     readNoneGroupEntriesIncludingEntryForAll). The persisted value is the literal
 *     "(All Blob Stores)" string because the descriptor uses idMapping("name").
 *   - excludeGroups: drop blobstores whose type is the group type
 *     (readNoneGroupEntriesIncludingEntryForAll only).
 *
 * Maintenance: when a new task type with a blobstore field lands on the backend,
 * read the `TaskDescriptor`'s storeApi and add the matching entry here. A typo
 * silently falls through to the plain blobstore list.
 */
export interface TaskBlobstoreFilter {
  includeAll?: boolean;
  excludeGroups?: boolean;
}

export const TASK_TYPE_BLOBSTORE_FILTERS: Record<string, Record<string, TaskBlobstoreFilter>> = {
  // CompactBlobStoreTaskDescriptor -> coreui_Blobstore.readWithAll
  'blobstore.compact': {
    blobstoreName: { includeAll: true },
  },
  // RecalculateBlobStoreSizeTaskDescriptor -> coreui_Blobstore.readNoneGroupEntriesIncludingEntryForAll
  'blobstore.metrics.reconcile': {
    blobstoreName: { includeAll: true, excludeGroups: true },
  },
  // DeleteBlobstoreTempFilesTaskDescriptor uses coreui_Blobstore.read (plain list, no groups, no All entry)
  // — intentionally omitted from this map.
};

/**
 * Formats whose UploadHandler currently returns supportsExportImport() == true. The Java
 * descriptors (RepositoryExportTaskDescriptor / RepositoryImportTaskDescriptor) discover this
 * dynamically at boot via ImportExportTaskDescriptorSupport#getFormats, so this list duplicates
 * that selection by enumerating every @Qualifier(<format>.NAME) on an UploadHandler bean that
 * returns true. Brittle: when a new format adds UploadHandler#supportsExportImport(), append it
 * here. Follow-up: replace with a fetch of /v1/formats/upload-specs so the list stays in sync
 * without touching the frontend.
 */
const IMPORT_EXPORT_FORMATS = [
  'alpine',
  'ansiblegalaxy',
  'apt',
  'cargo',
  'composer',
  'conan',
  'conda',
  'docker',
  'go',
  'helm',
  'maven2',
  'npm',
  'nuget',
  'pub',
  'pypi',
  'r',
  'raw',
  'rubygems',
  'swift',
  'terraform',
  'yum',
];

export const TASK_TYPE_REPO_FILTERS: Record<string, Record<string, TaskRepoFilter>> = {
  'repository.rebuild-index': {
    repositoryName: { includeAll: true },
  },
  // RepositoryExportTaskDescriptor — RepositoryCombobox().includingAnyOfTypes(hosted, proxy)
  //   .includingAnyOfFormats(<every UploadHandler#supportsExportImport()==true>).
  // No "(All Repositories)" entry (the descriptor doesn't call includeAnEntryForAllRepositories()).
  'repository.export': {
    repositoryName: {
      formats: IMPORT_EXPORT_FORMATS,
      types: ['hosted', 'proxy'],
      includeAll: false,
    },
  },
  // RepositoryImportTaskDescriptor — RepositoryCombobox().includingAnyOfTypes(hosted)
  //   .includingAnyOfFormats(<every UploadHandler#supportsExportImport()==true>).
  'repository.import': {
    repositoryName: {
      formats: IMPORT_EXPORT_FORMATS,
      types: ['hosted'],
      includeAll: false,
    },
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
  'repository.maven.rebuild-metadata': {
    repositoryName: { formats: ['maven2'], types: ['hosted'], includeAll: true },
  },
  'repository.npm.rebuild-metadata': {
    repositoryName: { formats: ['npm'], types: ['hosted'], includeAll: true },
  },
  'repository.composer.rebuild-metadata': {
    repositoryName: { formats: ['composer'], types: ['hosted'], includeAll: true },
  },
  'repository.maven.remove-snapshots': {
    repositoryName: {
      facets: ['org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet'],
      versionPolicies: ['!RELEASE'],
      includeAll: true,
    },
  },
  // Multi-repository tasks (rendered as a transfer list — see MULTI_REPO_TASK_TYPES).
  // Descriptors exclude group repos and offer an "(All Repositories)" entry.
  'create.browse.nodes': {
    repositoryName: { types: ['hosted', 'proxy'], includeAll: true },
  },
  'repository.pypi.generate-missing-sha256-checksums': {
    repositoryName: { formats: ['pypi'], types: ['hosted', 'proxy'], includeAll: true },
  },
  // MalwareRemediatorTaskDescriptor -> proxy repos of maven2/npm/nuget/pypi, with an All entry.
  'malware.remediator': {
    repositoryName: { types: ['proxy'], formats: ['maven2', 'npm', 'nuget', 'pypi'], includeAll: true },
  },
};

/**
 * Task types whose `repositoryName` field is a multi-repository selector
 * (backed by an ItemselectFormField) and must render as a dual-list transfer
 * selector rather than a single-repository combobox. The available-repository
 * filtering and "(All Repositories)" entry for these come from
 * TASK_TYPE_REPO_FILTERS, same as the single-repository selectors.
 */
export const MULTI_REPO_TASK_TYPES = new Set<string>([
  'create.browse.nodes',
  'repository.pypi.generate-missing-sha256-checksums',
]);

export function isMultiRepoTask(taskTypeId: string | undefined): boolean {
  return !!taskTypeId && MULTI_REPO_TASK_TYPES.has(taskTypeId);
}

/**
 * Task type id for the "Repair - Data Repair Plan" task
 * (ReconcileTaskConstants.PLAN_RECONCILE_TYPE_ID on the backend). Note the backend id is
 * `blobstore.planReconciliation`, not the "blobstore.reconcile.plan" used in some prose.
 */
export const PLAN_RECONCILE_TYPE_ID = 'blobstore.planReconciliation';

/**
 * Default scope for the Data Repair Plan task's taskScope field.
 * Used when the field has no value set (create flow) and when evaluating
 * scope-conditional field visibility.
 */
export const TASK_SCOPE_DURATION = 'duration';

/**
 * TaskScope value for the date-range option (ReconcileTaskConstants.TASK_SCOPE_DATES).
 */
export const TASK_SCOPE_DATES = 'dates';

/**
 * Task type id for the "Repair - Execute Data Repair Plan" task
 * (ReconcileTaskConstants.EXECUTE_RECONCILE_PLAN_TYPE_ID on the backend).
 */
export const EXECUTE_RECONCILE_PLAN_TYPE_ID = 'blobstore.executeReconciliationPlan';

/**
 * Sentinel value persisted for the Data Repair Plan blob-store selector when no specific blob
 * store is chosen — means "all blob stores" (ReconcileTaskConstants.ENTRY_ALL_BLOB_STORES).
 */
export const ALL_BLOB_STORES = '(All Blob Stores)';

/**
 * Task types limited to a single instance: once one exists, the create flow must not offer the
 * type again — mirrors Classic `TaskSelectType.filterTasksIfCreated`, which removes the type from
 * the create grid when the Task store already holds one.
 */
export const SINGLETON_TASK_TYPES = new Set<string>([PLAN_RECONCILE_TYPE_ID, EXECUTE_RECONCILE_PLAN_TYPE_ID]);

export function isSingletonTaskType(taskTypeId: string | undefined): boolean {
  return !!taskTypeId && SINGLETON_TASK_TYPES.has(taskTypeId);
}

/**
 * Task types that may only run manually: the wizard omits the Schedule step and the persisted
 * schedule is forced to 'manual'. Mirrors Classic `TaskSettingsForm.initScheduleFieldSet`, which
 * hides the schedule fieldset and pins the schedule combo to 'manual' for this task. Scoped, not
 * generic.
 */
export const MANUAL_ONLY_TASK_TYPES = new Set<string>([PLAN_RECONCILE_TYPE_ID, EXECUTE_RECONCILE_PLAN_TYPE_ID]);

export function isManualOnlyTaskType(taskTypeId: string | undefined): boolean {
  return !!taskTypeId && MANUAL_ONLY_TASK_TYPES.has(taskTypeId);
}

/**
 * Filter the task-type list offered in the create flow: drops any singleton task type that already
 * has an instance (per `existingTypeIds`). Non-singleton types are never removed. Pure function so
 * it can be unit-tested independently of the form.
 */
export function filterCreatableTaskTypes<T extends { id: string }>(
  taskTypes: T[],
  existingTypeIds: ReadonlySet<string>
): T[] {
  return taskTypes.filter((t) => !(isSingletonTaskType(t.id) && existingTypeIds.has(t.id)));
}

// Banner copy for the Data Repair Plan task. The REST `/v1/tasks/templates` contract serializes
// only formField id -> initialValue and drops the AlertBannerFormField message (which lives in the
// field's helpText), so the copy is hard-coded here verbatim from the backend Messages.java to
// preserve parity (see the ticket: hard-code banner copy keyed on the task id).
//
// Source: com.sonatype.nexus.reconcile.task.PlanReconciliationTaskDescriptor and its Messages
// interface (planReconciliationTopPanelMessage / planReconciliationBottomPanelMessage). The copy
// must be kept in sync manually when the backend messages change.
const PLAN_RECONCILE_TOP_BANNER =
  'This task generates recovery plans to reconcile blobstores and their associated repositories. ' +
  'To apply generated plans, run the "Repair - Execute Data Repair Plan" task. ' +
  'You can view created plans using the /v1/reconcile/plan API';

const PLAN_RECONCILE_BOTTOM_BANNER =
  'Tasks do not run automatically after creation. You must manually run the task after saving.';

// Execute Data Repair Plan copy — hard-coded verbatim from com.sonatype.nexus.reconcile.task.Messages
// (the REST /v1/tasks/templates contract drops AlertBanner/StaticInfo copy). Help text is stored
// pre-stripped of the descriptor's <span class='nx-boxlabel'> wrapper.
const EXECUTE_RECONCILE_TOP_BANNER =
  'This task executes recovery plans created using the "Repair - Data Repair Plan" task. ' +
  'Execution is limited to the blob stores that were selected when creating the plan. ' +
  'It is recommended to perform a backup before executing a recovery plan. ' +
  'View available plans using the /v1/reconcile/plan-details API.';

const EXECUTE_PLAN_OPTIONS_HELP =
  'Execution details for recovery plan generated by the Data Repair Plan task.';

const EXECUTE_PLAN_INFORMATION_HELP =
  'Displays details for recovery plans generated by the "Repair - Data Repair Plan", including the ' +
  'number of plans, associated blob stores and repositories, and the date range covered.';

// Malware remediator banner copy — hardcoded verbatim from
// com.sonatype.nexus.risk.remediation.MalwareRemediatorTaskDescriptor (the REST
// /v1/tasks/templates contract drops PanelMessage copy, which lives in the field's
// helpText). The descriptor's <br/> is dropped — SettingsAlert renders text, not HTML.
//
// Source constants (private/common/components/nexus-malicious-risk-plugin):
//   MalwareRemediatorTaskDescriptor — field `malwareRemediatorTaskRequirements` helpText
//   MalwareRemediatorTaskDescriptor — field `enableMalwareCleanupMessage` helpText
//
// DRIFT RISK: if either helpText changes in the descriptor, this copy becomes stale.
// Update these strings to match. There is no compile-time guard — search for
// "MalwareRemediatorTaskDescriptor" when upgrading that plugin.
const MALWARE_REQUIREMENTS_BANNER =
  'This task requires the Repository Firewall enabled with the Security-Malicious policy set to fail to ' +
  'function as expected. Setting the connection to IQ Server is a requirement for Firewall to work and ' +
  'this task is using Firewall to generate the results.';

const MALWARE_CLEANUP_WARNING_BANNER =
  'Enabling automatic malware clean up may remove dependencies currently in use by your applications, ' +
  'this may break any automated builds when Repository Firewall quarantines these components the next ' +
  'time they are requested.';

/**
 * Per-task-type field metadata overrides, layered on top of the field-id-keyed TASK_FIELD_UI.
 *
 * Required because some field ids carry different semantics per task. `onlyNotify`, `blobstoreName`
 * and `repositoryName` mean something different for the Data Repair Plan task (custom copy, multi-
 * select transfer lists, blob-store-filtered repos) than the shared defaults, and the descriptor's
 * banner / scope / template fields have no usable entry in the flat map at all. The override map is
 * the same task-type-aware pattern as TASK_TYPE_REPO_FILTERS / MULTI_REPO_TASK_TYPES.
 *
 * The string keys must equal the descriptor `id` exactly (typeId, then fieldId).
 *
 * Note: blobstore.planReconciliation is self-hosted only. Its descriptor is
 * `@ConditionalOnEdition(pro = true, community = true)` (NOT cloud — the cloud=true attempt was
 * reverted in NEXUS-47948), so the bean is not registered in the cloud edition and the task never
 * appears in `/v1/tasks/templates` (hence the create list) in Cloud. There is therefore no
 * "cloud variant" of this form — these overrides only ever render on self-hosted.
 */
export const TASK_TYPE_FIELD_OVERRIDES: Record<string, Record<string, TaskFieldMeta>> = {
  [PLAN_RECONCILE_TYPE_ID]: {
    topAlertBanner: {
      label: '',
      type: 'alertBanner',
      bannerVariant: 'info',
      bannerText: PLAN_RECONCILE_TOP_BANNER,
      required: false,
      order: 0,
    },
    bottomAlertBanner: {
      label: '',
      type: 'alertBanner',
      bannerVariant: 'warning',
      bannerText: PLAN_RECONCILE_BOTTOM_BANNER,
      required: false,
      order: 999,
    },
    onlyNotify: {
      label: 'Keep database records when blob is missing:',
      type: 'checkbox',
      helpText:
        'Notify when records have missing blobs in the plan-details API, do not remove database records for missing blobs.',
      order: 10,
    },
    blobstoreName: {
      label: 'Blob store',
      type: 'itemselect',
      multiSelect: true,
      filterByBlobstore: true,
      // Empty selection = the "(All Blob Stores)" sentinel; show that clearly so it doesn't read as
      // "nothing will be processed" (Classic shows an all-selected state).
      selectedEmptyText: 'All Blob Stores selected',
      // ...but the saved payload must match Classic, which omits blobstoreName entirely for the
      // implicit all-blob-stores state (it never sends the sentinel as a property).
      omitWhenEmpty: true,
      helpText: 'Select the blob stores to repair. The repository list updates to match your selection',
      required: false,
      order: 20,
    },
    repositoryName: {
      label: 'Repository',
      type: 'itemselect',
      multiSelect: true,
      // Classic omits an empty repository selector from the payload (exportProperties → null).
      omitWhenEmpty: true,
      helpText: 'Select the priority order of the repositories to repair',
      required: false,
      order: 30,
    },
    taskScope: {
      label: 'Timespan:',
      type: 'taskScope',
      helpText: 'Limit this task to files added to the repository during a specific timespan.',
      required: true,
      order: 40,
    },
    // Descriptor TemplateFormField that defaults the task NAME (not a persisted property).
    name: { label: '', hidden: true, isNameTemplate: true, required: false },
    // Empty duration fields serialize as the literal "null" string (Classic parity).
    sinceDays: { label: 'Days', type: 'number', required: false, scope: 'duration', min: 0, order: 50, serializeEmptyAs: 'null' },
    sinceHours: { label: 'Hours', type: 'number', required: false, scope: 'duration', min: 0, order: 51, serializeEmptyAs: 'null' },
    sinceMinutes: { label: 'Minutes', type: 'number', required: false, scope: 'duration', min: 0, order: 52, serializeEmptyAs: 'null' },
    reconcileStartDate: { label: 'Start date', type: 'date', required: false, scope: 'dates', order: 60 },
    reconcileEndDate: { label: 'End date', type: 'date', required: false, scope: 'dates', order: 61 },
  },
  [EXECUTE_RECONCILE_PLAN_TYPE_ID]: {
    topAlertBanner: {
      label: '',
      type: 'alertBanner',
      bannerVariant: 'info',
      bannerText: EXECUTE_RECONCILE_TOP_BANNER,
      required: false,
      order: 0,
    },
    // Same descriptor field id on both editions, different label: self-hosted "Execution Information"
    // (createExecutionInformationInfo) vs cloud "Plan Options" (createPlanOptionsInfo).
    planOptionsLabelId: {
      label: 'Execution Information',
      cloudLabel: 'Plan Options',
      type: 'staticInfo',
      helpText: EXECUTE_PLAN_OPTIONS_HELP,
      required: false,
      order: 10,
    },
    planInformationLabelId: {
      label: 'Plan Information',
      type: 'staticInfo',
      helpText: EXECUTE_PLAN_INFORMATION_HELP,
      required: false,
      order: 20,
    },
    planInformation: {
      label: '',
      type: 'planInformation',
      required: false,
      order: 30,
    },
    // Read-only (Classic renderExecutePlanFields): Selected-only, no Available column, no filtering.
    // Self-hosted only — cloud template omits this field.
    blobstoreName: {
      label: 'Blob store',
      type: 'itemselect',
      multiSelect: true,
      readOnly: true,
      neverSerialize: true,
      helpText: 'Select the blob stores to repair. The repository list updates to match your selection',
      required: false,
      order: 40,
    },
    repositoryName: {
      label: 'Repository',
      type: 'itemselect',
      multiSelect: true,
      readOnly: true,
      neverSerialize: true,
      // When the Plan task has no repo filter (common), repositoryName is absent from
      // the derived properties and the list would render as a blank grey box.
      // Mirror the blob-store field's treatment so empty reads as "all selected".
      selectedEmptyText: 'All Repositories selected',
      helpText: 'Select the priority order of the repositories to repair',
      required: false,
      order: 50,
    },
    taskScope: {
      label: 'Timespan:',
      type: 'taskScope',
      defaultScope: 'dates',
      readOnly: true,
      neverSerialize: true,
      helpText: 'Limit this task to files added to the repository during a specific timespan.',
      required: true,
      order: 60,
    },
    reconcileStartDate: { label: 'Start date', type: 'date', required: false, readOnly: true, neverSerialize: true, scope: 'dates', order: 70 },
    reconcileEndDate: { label: 'End date', type: 'date', required: false, readOnly: true, neverSerialize: true, scope: 'dates', order: 71 },
    // Self-hosted only — descriptor TemplateFormField defaulting the task NAME (not a property).
    name: { label: '', hidden: true, isNameTemplate: true, required: false },
  },
  // MalwareRemediatorTaskDescriptor fields, in descriptor order. The descriptor's two
  // PanelMessageFormField instances have empty initial values, so without per-task metadata
  // the fallback heuristic misdetects them as checkboxes (isBoolean: value === ''). The
  // global TASK_FIELD_UI had stale, descriptor-less guesses for the two malware keys;
  // those have been removed and this per-task block is now the single source of truth.
  'malware.remediator': {
    malwareRemediatorTaskRequirements: {
      label: '',
      type: 'alertBanner',
      bannerVariant: 'info',
      bannerText: MALWARE_REQUIREMENTS_BANNER,
      required: false,
      neverSerialize: true,
      order: 0,
    },
    repositoryName: {
      label: 'Repository',
      type: 'repo',
      helpText: 'Select the proxy repository to remove malware from',
      allowAll: true,
      required: true,
      order: 10,
    },
    enableMalwareCleanup: {
      label: 'Enable Malware Cleanup',
      type: 'checkbox',
      helpText: 'When selected, malware found will be scheduled to be cleaned up in the target proxy repositories.',
      order: 20,
    },
    enableMalwareCleanupMessage: {
      label: '',
      type: 'alertBanner',
      bannerVariant: 'warning',
      bannerText: MALWARE_CLEANUP_WARNING_BANNER,
      required: false,
      neverSerialize: true,
      order: 30,
    },
  },
};

/**
 * Resolve UI metadata for a field, preferring a per-task-type override over the shared
 * field-id-keyed default. Returns undefined when neither declares the field, so callers keep
 * their fallback heuristic for genuinely unknown fields.
 */
export function resolveTaskFieldMeta(
  taskTypeId: string | undefined,
  fieldId: string
): TaskFieldMeta | undefined {
  const override = taskTypeId ? TASK_TYPE_FIELD_OVERRIDES[taskTypeId]?.[fieldId] : undefined;
  return override ?? TASK_FIELD_UI[fieldId];
}

/**
 * Resolve the default `taskScope` for a task type: the scope assumed when the property has no value.
 * Reads the per-task `taskScope` override's `defaultScope`; falls back to TASK_SCOPE_DURATION.
 */
export function resolveDefaultScope(taskTypeId: string | undefined): 'duration' | 'dates' {
  const meta = taskTypeId ? TASK_TYPE_FIELD_OVERRIDES[taskTypeId]?.taskScope : undefined;
  return meta?.defaultScope ?? (TASK_SCOPE_DURATION as 'duration');
}

/**
 * Convert a Classic `m/d/Y` task date string (e.g. "06/24/2026") to the `YYYY-MM-DD` value an
 * `<input type="date">` expects. Returns '' for blank/unparseable input. Already-ISO values pass
 * through so EDIT round-trips are resilient to either stored format. Out-of-range calendar values
 * (e.g. "99/99/2026") produce a lexical "YYYY-MM-DD"-shaped string rather than an error; the
 * native date input will reject the invalid date at render time.
 */
export function mdyToIso(value: string | null | undefined): string {
  if (!value) return '';
  const trimmed = value.trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed;
  const match = trimmed.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (!match) return '';
  const [, mm, dd, yyyy] = match;
  return `${yyyy}-${mm.padStart(2, '0')}-${dd.padStart(2, '0')}`;
}

/**
 * Convert a `YYYY-MM-DD` date-input value back to the Classic `m/d/Y` (leading-zero) string the
 * backend stores, so Preview-created tasks round-trip into the Classic UI unchanged. Returns ''
 * for blank input; passes non-ISO input (e.g. an already-`m/d/Y` value) through unchanged, so the
 * helper is idempotent with `mdyToIso`.
 */
export function isoToMdy(value: string | null | undefined): string {
  if (!value) return '';
  const match = value.trim().match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!match) return value.trim();
  const [, yyyy, mm, dd] = match;
  return `${mm}/${dd}/${yyyy}`;
}
