/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. All other trademarks are the
 * property of their respective owners.
 */

import {
  TASK_FIELD_UI,
  TASK_TYPE_REPO_FILTERS,
  MULTI_REPO_TASK_TYPES,
  TaskFieldMeta,
  TaskRepoFilter,
  TASK_TYPE_FIELD_OVERRIDES,
  resolveTaskFieldMeta,
  getTaskFieldMeta,
  PLAN_RECONCILE_TYPE_ID,
  EXECUTE_RECONCILE_PLAN_TYPE_ID,
  TASK_SCOPE_DATES,
  resolveDefaultScope,
  ALL_BLOB_STORES,
  mdyToIso,
  isoToMdy,
  isSingletonTaskType,
  isManualOnlyTaskType,
  filterCreatableTaskTypes,
  SINGLETON_TASK_TYPES,
  MANUAL_ONLY_TASK_TYPES,
} from '../taskFieldMetadata';

describe('taskFieldMetadata', () => {
  describe('allowAll flag on repositoryName', () => {
    it('repositoryName has allowAll: true', () => {
      expect(TASK_FIELD_UI.repositoryName.allowAll).toBe(true);
    });

    it('moveRepositoryName has allowAll: false', () => {
      expect(TASK_FIELD_UI.moveRepositoryName.allowAll).toBe(false);
    });

    it('TaskFieldMeta interface accepts allowAll', () => {
      const meta: TaskFieldMeta = { label: 'Test', allowAll: true };
      expect(meta.allowAll).toBe(true);
    });
  });

  describe('includeFormatEntries flag', () => {
    it('TaskFieldMeta interface accepts includeFormatEntries', () => {
      const meta: TaskFieldMeta = { label: 'Test', includeFormatEntries: true };
      expect(meta.includeFormatEntries).toBe(true);
    });

    it('restrictComponentDelete renders as a repo selector with format entries', () => {
      expect(TASK_FIELD_UI.restrictComponentDelete).toEqual(
        expect.objectContaining({
          type: 'repo',
          includeFormatEntries: true,
        }),
      );
    });

    it('restrictComponentDelete is no longer a checkbox', () => {
      expect(TASK_FIELD_UI.restrictComponentDelete.type).not.toBe('checkbox');
    });
  });

  describe('nameRegex placeholder', () => {
    it('does not contain the Maven-snapshot placeholder', () => {
      expect(TASK_FIELD_UI.nameRegex.placeholder).toBeDefined();
      expect(TASK_FIELD_UI.nameRegex.placeholder).not.toContain('SNAPSHOT');
    });
  });

  describe('validate helper', () => {
    it('TaskFieldMeta interface accepts a validate function', () => {
      const meta: TaskFieldMeta = {
        label: 'Test',
        validate: (v: string) => (v ? null : 'required'),
      };
      expect(meta.validate?.('')).toBe('required');
      expect(meta.validate?.('x')).toBeNull();
    });

    it('nameRegex.validate returns an error for an unparseable regex', () => {
      const result = TASK_FIELD_UI.nameRegex.validate?.('[unclosed');
      expect(result).toBe('Tag name regex is not a valid regular expression');
    });

    it('nameRegex.validate returns null for a valid regex', () => {
      expect(TASK_FIELD_UI.nameRegex.validate?.('release-.*')).toBeNull();
    });

    it('nameRegex.validate returns null for empty input (handled at form level)', () => {
      expect(TASK_FIELD_UI.nameRegex.validate?.('')).toBeNull();
    });
  });

  describe('required flag', () => {
    it('TaskFieldMeta interface accepts an optional required flag', () => {
      const meta: TaskFieldMeta = { label: 'Test', required: false };
      expect(meta.required).toBe(false);
    });

    it('marks all tags.cleanup fields as not required', () => {
      expect(TASK_FIELD_UI.firstCreatedDays.required).toBe(false);
      expect(TASK_FIELD_UI.lastUpdatedDays.required).toBe(false);
      expect(TASK_FIELD_UI.nameRegex.required).toBe(false);
      expect(TASK_FIELD_UI.restrictComponentDelete.required).toBe(false);
    });

    it('does not mark mandatory-target fields as optional', () => {
      // Repository/blobstore/path selectors default to required (required is undefined)
      expect(TASK_FIELD_UI.repositoryName.required).toBeUndefined();
      expect(TASK_FIELD_UI.blobstoreName.required).toBeUndefined();
      expect(TASK_FIELD_UI.location.required).toBeUndefined();
    });
  });

  describe('nameRegex helpText', () => {
    it('does not claim Java regex semantics', () => {
      expect(TASK_FIELD_UI.nameRegex.helpText).not.toMatch(/java/i);
    });
  });

  describe('checkbox field metadata — labels and helpText match backend descriptors', () => {
    it('yumMetadataCaching has label "Soft repair" matching YumCreateRepoTaskDescriptor', () => {
      expect(TASK_FIELD_UI.yumMetadataCaching.label).toBe('Soft repair');
    });

    it('yumMetadataCaching has correct helpText from YumCreateRepoTaskDescriptor', () => {
      expect(TASK_FIELD_UI.yumMetadataCaching.helpText).toBe(
        'Only update the metadata with RPM changes since the last metadata generation'
      );
    });

    it('forceRebuild has label "Force rebuild" matching RebuildVersionsTaskDescriptor', () => {
      expect(TASK_FIELD_UI.forceRebuild.label).toBe('Force rebuild');
    });

    it('forceRebuild has correct helpText from RebuildVersionsTaskDescriptor', () => {
      expect(TASK_FIELD_UI.forceRebuild.helpText).toBe('Rebuilds even if not marked as out of date');
    });

    it('rebuildAptMetadataFullRebuild label includes "(hosted only)" to match RebuildAptMetadataTaskDescriptor', () => {
      expect(TASK_FIELD_UI.rebuildAptMetadataFullRebuild.label).toBe('Full rebuild (hosted only)');
    });

    it('rebuildAptMetadataFullRebuild has correct helpText from RebuildAptMetadataTaskDescriptor', () => {
      expect(TASK_FIELD_UI.rebuildAptMetadataFullRebuild.helpText).toContain('apt_key_value');
      expect(TASK_FIELD_UI.rebuildAptMetadataFullRebuild.helpText).toContain('hosted repositories');
    });

    it('resetProxyMetadata is defined with correct label', () => {
      expect(TASK_FIELD_UI.resetProxyMetadata).toBeDefined();
      expect(TASK_FIELD_UI.resetProxyMetadata.label).toBe('Reset proxy metadata');
    });

    it('resetProxyMetadata has correct helpText from RebuildAptMetadataTaskDescriptor', () => {
      expect(TASK_FIELD_UI.resetProxyMetadata.helpText).toContain('proxy repositories');
    });

    it('rebuildHelmMetadataFullRebuild has label "Full rebuild"', () => {
      expect(TASK_FIELD_UI.rebuildHelmMetadataFullRebuild.label).toBe('Full rebuild');
    });

    it('rebuildHelmMetadataFullRebuild has correct helpText from RebuildHelmMetadataTaskDescriptor', () => {
      expect(TASK_FIELD_UI.rebuildHelmMetadataFullRebuild.helpText).toContain('helm_key_value');
    });

    it('rebuildAlpineMetadataFullRebuild is defined with label "Full rebuild"', () => {
      expect(TASK_FIELD_UI.rebuildAlpineMetadataFullRebuild).toBeDefined();
      expect(TASK_FIELD_UI.rebuildAlpineMetadataFullRebuild.label).toBe('Full rebuild');
    });

    it('rebuildAlpineMetadataFullRebuild has correct helpText from RebuildAlpineMetadataTaskDescriptor', () => {
      expect(TASK_FIELD_UI.rebuildAlpineMetadataFullRebuild.helpText).toContain('out of sync');
    });

    it('all 5 target checkbox fields are typed as checkbox', () => {
      expect(TASK_FIELD_UI.yumMetadataCaching.type).toBe('checkbox');
      expect(TASK_FIELD_UI.forceRebuild.type).toBe('checkbox');
      expect(TASK_FIELD_UI.rebuildAptMetadataFullRebuild.type).toBe('checkbox');
      expect(TASK_FIELD_UI.resetProxyMetadata.type).toBe('checkbox');
      expect(TASK_FIELD_UI.rebuildHelmMetadataFullRebuild.type).toBe('checkbox');
      expect(TASK_FIELD_UI.rebuildAlpineMetadataFullRebuild.type).toBe('checkbox');
    });
  });

  describe('visibleForRepoTypes — APT checkbox conditional visibility', () => {
    it('rebuildAptMetadataFullRebuild is visible for hosted repos only', () => {
      expect(TASK_FIELD_UI.rebuildAptMetadataFullRebuild.visibleForRepoTypes).toEqual(['hosted']);
    });

    it('resetProxyMetadata is visible for proxy repos only', () => {
      expect(TASK_FIELD_UI.resetProxyMetadata.visibleForRepoTypes).toEqual(['proxy']);
    });

    it('non-APT checkbox fields do not have visibleForRepoTypes', () => {
      expect(TASK_FIELD_UI.rebuildHelmMetadataFullRebuild.visibleForRepoTypes).toBeUndefined();
      expect(TASK_FIELD_UI.rebuildAlpineMetadataFullRebuild.visibleForRepoTypes).toBeUndefined();
      expect(TASK_FIELD_UI.yumMetadataCaching.visibleForRepoTypes).toBeUndefined();
      expect(TASK_FIELD_UI.forceRebuild.visibleForRepoTypes).toBeUndefined();
    });

    it('non-checkbox fields do not have visibleForRepoTypes', () => {
      expect(TASK_FIELD_UI.repositoryName.visibleForRepoTypes).toBeUndefined();
      expect(TASK_FIELD_UI.blobstoreName.visibleForRepoTypes).toBeUndefined();
      expect(TASK_FIELD_UI.olderThanDays.visibleForRepoTypes).toBeUndefined();
    });
  });

  describe('TASK_TYPE_REPO_FILTERS — repository filter configuration', () => {
    it('defines filters for all 5 NEXUS-53043 task types', () => {
      expect(TASK_TYPE_REPO_FILTERS['repository.apt.rebuild.metadata']).toBeDefined();
      expect(TASK_TYPE_REPO_FILTERS['repository.helm.rebuild.metadata']).toBeDefined();
      expect(TASK_TYPE_REPO_FILTERS['repository.alpine.rebuild.metadata']).toBeDefined();
      expect(TASK_TYPE_REPO_FILTERS['repository.yum.rebuild.metadata']).toBeDefined();
      expect(TASK_TYPE_REPO_FILTERS['repository.ruby.rebuild.versions']).toBeDefined();
    });

    it('APT task: formats=[apt], types=[hosted,proxy], includeAll=true (mirrors RebuildAptMetadataTaskDescriptor)', () => {
      const filter: TaskRepoFilter = TASK_TYPE_REPO_FILTERS['repository.apt.rebuild.metadata'].repositoryName;
      expect(filter.formats).toEqual(['apt']);
      expect(filter.types).toEqual(['hosted', 'proxy']);
      expect(filter.includeAll).toBe(true);
    });

    it('Helm task: formats=[helm], types=[hosted], includeAll=true (mirrors RebuildHelmMetadataTaskDescriptor)', () => {
      const filter: TaskRepoFilter = TASK_TYPE_REPO_FILTERS['repository.helm.rebuild.metadata'].repositoryName;
      expect(filter.formats).toEqual(['helm']);
      expect(filter.types).toEqual(['hosted']);
      expect(filter.includeAll).toBe(true);
    });

    it('Alpine task: formats=[alpine], types=[hosted,proxy], includeAll=true (mirrors RebuildAlpineMetadataTaskDescriptor)', () => {
      const filter: TaskRepoFilter = TASK_TYPE_REPO_FILTERS['repository.alpine.rebuild.metadata'].repositoryName;
      expect(filter.formats).toEqual(['alpine']);
      expect(filter.types).toEqual(['hosted', 'proxy']);
      expect(filter.includeAll).toBe(true);
    });

    it('Yum task: formats=[yum], types=[hosted], includeAll=false (mirrors YumCreateRepoTaskDescriptor — no includeAnEntryForAllRepositories)', () => {
      const filter: TaskRepoFilter = TASK_TYPE_REPO_FILTERS['repository.yum.rebuild.metadata'].repositoryName;
      expect(filter.formats).toEqual(['yum']);
      expect(filter.types).toEqual(['hosted']);
      expect(filter.includeAll).toBe(false);
    });

    it('RubyGems task: formats=[rubygems], types=[hosted], includeAll=false (mirrors RebuildVersionsTaskDescriptor — no includeAnEntryForAllRepositories)', () => {
      const filter: TaskRepoFilter = TASK_TYPE_REPO_FILTERS['repository.ruby.rebuild.versions'].repositoryName;
      expect(filter.formats).toEqual(['rubygems']);
      expect(filter.types).toEqual(['hosted']);
      expect(filter.includeAll).toBe(false);
    });

    it('TaskRepoFilter interface is correctly typed', () => {
      const filter: TaskRepoFilter = { formats: ['test'], types: ['hosted'], includeAll: false };
      expect(filter.formats).toEqual(['test']);
    });

    it('NEXUS-53354 Maven rebuild metadata: formats=[maven2], types=[hosted], includeAll=true (mirrors RebuildMaven2MetadataTaskDescriptor)', () => {
      const filter: TaskRepoFilter = TASK_TYPE_REPO_FILTERS['repository.maven.rebuild-metadata'].repositoryName;
      expect(filter.formats).toEqual(['maven2']);
      expect(filter.types).toEqual(['hosted']);
      expect(filter.includeAll).toBe(true);
    });

    it('NEXUS-53354 npm rebuild metadata: formats=[npm], types=[hosted], includeAll=true (mirrors BaseRebuildNpmMetadataTaskDescriptor)', () => {
      const filter: TaskRepoFilter = TASK_TYPE_REPO_FILTERS['repository.npm.rebuild-metadata'].repositoryName;
      expect(filter.formats).toEqual(['npm']);
      expect(filter.types).toEqual(['hosted']);
      expect(filter.includeAll).toBe(true);
    });

    it('NEXUS-53354 Composer rebuild metadata: formats=[composer], types=[hosted], includeAll=true (mirrors RebuildComposerMetadataTaskDescriptor)', () => {
      const filter: TaskRepoFilter = TASK_TYPE_REPO_FILTERS['repository.composer.rebuild-metadata'].repositoryName;
      expect(filter.formats).toEqual(['composer']);
      expect(filter.types).toEqual(['hosted']);
      expect(filter.includeAll).toBe(true);
    });
  });

  describe('TASK_TYPE_REPO_FILTERS — facet and versionPolicy filters (NEXUS-53044)', () => {
    it('repository.purge-unused filters by PurgeUnusedFacet and includes (All Repositories)', () => {
      // The classic UI uses includingAnyOfFacets(PurgeUnusedFacet) on the descriptor; the
      // Preview UI must hit the facet-filtered endpoint to match that behavior.
      const filter = TASK_TYPE_REPO_FILTERS['repository.purge-unused']?.repositoryName;
      expect(filter).toBeDefined();
      expect(filter?.facets).toEqual(['org.sonatype.nexus.repository.purge.PurgeUnusedFacet']);
      expect(filter?.includeAll).toBe(true);
    });

    it('TaskRepoFilter interface accepts a facets array', () => {
      const filter: TaskRepoFilter = { facets: ['org.example.SomeFacet'], includeAll: false };
      expect(filter.facets).toHaveLength(1);
    });

    it('repository.maven.purge-unused-snapshots filters by PurgeUnusedSnapshotsFacet AND excludes RELEASE versionPolicy', () => {
      // Classic UI uses includingAnyOfFacets(PurgeUnusedSnapshotsFacet) +
      // excludingAnyOfVersionPolicies(RELEASE) — both must be in this filter.
      const filter = TASK_TYPE_REPO_FILTERS['repository.maven.purge-unused-snapshots']?.repositoryName;
      expect(filter).toBeDefined();
      expect(filter?.facets).toEqual(['org.sonatype.nexus.repository.maven.PurgeUnusedSnapshotsFacet']);
      expect(filter?.versionPolicies).toEqual(['!RELEASE']);
      expect(filter?.includeAll).toBe(true);
    });

    it('repository.docker.gc filters by DockerGCFacet (which lives only on hosted+proxy recipes)', () => {
      const filter = TASK_TYPE_REPO_FILTERS['repository.docker.gc']?.repositoryName;
      expect(filter).toBeDefined();
      expect(filter?.facets).toEqual(['com.sonatype.nexus.repository.docker.DockerGCFacet']);
      expect(filter?.includeAll).toBe(true);
    });

    it('repository.docker.gc.custom shares the same DockerGCFacet filter', () => {
      const filter = TASK_TYPE_REPO_FILTERS['repository.docker.gc.custom']?.repositoryName;
      expect(filter).toBeDefined();
      expect(filter?.facets).toEqual(['com.sonatype.nexus.repository.docker.DockerGCFacet']);
      expect(filter?.includeAll).toBe(true);
    });

    it('external.blobstore.metadata explicitly opts out of the (All Repositories) entry', () => {
      // The descriptor uses a bare RepositoryCombobox (no includeAnEntryForAllRepositories());
      // without this override the field falls back to TASK_FIELD_UI.repositoryName.allowAll=true
      // and wrongly prepends (All Repositories).
      const filter = TASK_TYPE_REPO_FILTERS['external.blobstore.metadata']?.repositoryName;
      expect(filter).toBeDefined();
      expect(filter?.includeAll).toBe(false);
      // No server-side filter — descriptor doesn't restrict by facet/format/type.
      expect(filter?.facets).toBeUndefined();
      expect(filter?.formats).toBeUndefined();
      expect(filter?.types).toBeUndefined();
    });

    it('NEXUS-53354 Remove SNAPSHOT: facets=[RemoveSnapshotsFacet], versionPolicies=[!RELEASE], includeAll=true (mirrors RemoveSnapshotsTaskDescriptor)', () => {
      const filter = TASK_TYPE_REPO_FILTERS['repository.maven.remove-snapshots']?.repositoryName;
      expect(filter).toBeDefined();
      expect(filter?.facets).toEqual(['org.sonatype.nexus.repository.maven.RemoveSnapshotsFacet']);
      expect(filter?.versionPolicies).toEqual(['!RELEASE']);
      expect(filter?.includeAll).toBe(true);
    });
  });

  describe('external.metadata.repository.format', () => {
    // The descriptor (ExternalMetadataTaskDescriptor) declares this as a non-required
    // StringTextFormField. The id contains "repository" so without an explicit metadata
    // entry both the API mapper and the render heuristic mistake it for a repo combobox.
    it('renders as a free-form text input labeled "Repository format"', () => {
      const meta = TASK_FIELD_UI['external.metadata.repository.format'];
      expect(meta).toBeDefined();
      expect(meta?.label).toBe('Repository format');
      expect(meta?.type).toBe('string');
    });

    it('is marked optional so the wizard does not block save when empty', () => {
      const meta = TASK_FIELD_UI['external.metadata.repository.format'];
      expect(meta?.required).toBe(false);
    });
  });

  describe('NEXUS-53354 Composer field metadata', () => {
    it('vendor renders as a non-required string with help text and placeholder', () => {
      const meta = TASK_FIELD_UI['vendor'];
      expect(meta).toBeDefined();
      expect(meta?.label).toBe('Vendor');
      expect(meta?.type).toBe('string');
      expect(meta?.required).toBe(false);
      expect(meta?.helpText).toMatch(/vendor/i);
      expect(meta?.placeholder).toBeDefined();
    });

    it('baseUrl renders as a non-required URL field with help text referencing the Base URL Capability', () => {
      const meta = TASK_FIELD_UI['baseUrl'];
      expect(meta).toBeDefined();
      expect(meta?.label).toBe('Base URL');
      expect(meta?.type).toBe('url');
      expect(meta?.required).toBe(false);
      expect(meta?.helpText).toMatch(/Base URL Capability/i);
    });
  });

  describe('NEXUS-53354 minimumRetained min boundary', () => {
    it('accepts -1 to match RemoveSnapshotsTaskDescriptor.withMinimumValue(-1)', () => {
      expect(TASK_FIELD_UI.minimumRetained.min).toBe(-1);
    });

    it('helpText explains the -1 = delete all semantic', () => {
      expect(TASK_FIELD_UI.minimumRetained.helpText).toMatch(/-1/);
      expect(TASK_FIELD_UI.minimumRetained.helpText).toMatch(/delete all/i);
    });
  });

  describe('MULTI_REPO_TASK_TYPES sync invariant', () => {
    it('every task type in MULTI_REPO_TASK_TYPES has a corresponding entry in TASK_TYPE_REPO_FILTERS', () => {
      expect([...MULTI_REPO_TASK_TYPES].every((id) => id in TASK_TYPE_REPO_FILTERS)).toBe(true);
    });
  });

  describe('NEXUS-53360 ScriptTask + H2BackupTask field metadata', () => {
    it('TaskFieldMeta interface accepts an optional rows property', () => {
      const meta: TaskFieldMeta = { label: 'Test', type: 'text', rows: 8 };
      expect(meta.rows).toBe(8);
    });

    it('language renders as a string field (matches ScriptTaskDescriptor StringTextFormField)', () => {
      const meta = TASK_FIELD_UI['language'];
      expect(meta).toBeDefined();
      expect(meta?.label).toBe('Language');
      expect(meta?.type).toBe('string');
      expect(meta?.helpText).toMatch(/script language/i);
    });

    it('language stays required by default (descriptor field is MANDATORY)', () => {
      // No explicit required flag — relies on the metadata default (required !== false).
      expect(TASK_FIELD_UI['language'].required).toBeUndefined();
    });

    it('source renders as a multi-line text area, NOT a checkbox', () => {
      // Regression: source has no initial value, so restTemplateToTaskType's fallback
      // heuristic (value === '' => checkbox) would mis-render it without this metadata.
      const meta = TASK_FIELD_UI['source'];
      expect(meta).toBeDefined();
      expect(meta?.label).toBe('Script Source');
      expect(meta?.type).toBe('text');
      expect(meta?.type).not.toBe('checkbox');
      expect(meta?.helpText).toMatch(/script source/i);
    });

    it('source declares a larger row count for proper sizing', () => {
      expect(TASK_FIELD_UI['source'].rows).toBeGreaterThan(4);
    });

    it('multinode is an optional checkbox matching TaskDescriptorSupport constants', () => {
      const meta = TASK_FIELD_UI['multinode'];
      expect(meta).toBeDefined();
      expect(meta?.type).toBe('checkbox');
      // Mirrors MULTINODE_LABEL / MULTINODE_HELP in TaskDescriptorSupport.java
      expect(meta?.label).toBe('Multi node');
      expect(meta?.helpText).toBe('Run task on all nodes in the cluster.');
    });

    it('location (H2BackupTask) remains a string path field', () => {
      const meta = TASK_FIELD_UI['location'];
      expect(meta).toBeDefined();
      expect(meta?.type).toBe('string');
    });
  });
});

describe('resolveTaskFieldMeta', () => {
  it('prefers a per-task override over the shared field-id default', () => {
    // onlyNotify has a generic "Notify Only" default but Data Repair Plan overrides the copy.
    expect(TASK_FIELD_UI.onlyNotify.label).toBe('Notify Only');
    const meta = resolveTaskFieldMeta(PLAN_RECONCILE_TYPE_ID, 'onlyNotify');
    expect(meta?.label).toBe('Keep database records when blob is missing:');
    expect(meta?.type).toBe('checkbox');
  });

  it('falls back to the shared default when no override exists for the task', () => {
    expect(resolveTaskFieldMeta('repository.cleanup', 'onlyNotify')).toBe(TASK_FIELD_UI.onlyNotify);
  });

  it('falls back to the shared default for an unknown task type', () => {
    expect(resolveTaskFieldMeta('totally.unknown.task', 'repositoryName')).toBe(TASK_FIELD_UI.repositoryName);
  });

  it('returns undefined for a genuinely unknown field (preserving the caller heuristic)', () => {
    expect(resolveTaskFieldMeta(PLAN_RECONCILE_TYPE_ID, 'doesNotExist')).toBeUndefined();
    expect(resolveTaskFieldMeta(undefined, 'doesNotExist')).toBeUndefined();
  });
});

describe('Data Repair Plan field overrides (blobstore.planReconciliation)', () => {
  const overrides = TASK_TYPE_FIELD_OVERRIDES[PLAN_RECONCILE_TYPE_ID];

  it('declares an override set keyed by the backend type id', () => {
    expect(PLAN_RECONCILE_TYPE_ID).toBe('blobstore.planReconciliation');
    expect(overrides).toBeDefined();
  });

  it('renders the top banner as an info alert and the bottom banner as a warning alert', () => {
    expect(overrides.topAlertBanner.type).toBe('alertBanner');
    expect(overrides.topAlertBanner.bannerVariant).toBe('info');
    expect(overrides.topAlertBanner.required).toBe(false);
    expect(overrides.bottomAlertBanner.type).toBe('alertBanner');
    expect(overrides.bottomAlertBanner.bannerVariant).toBe('warning');
  });

  // Freezes the banner copy verbatim against the backend source. The REST `/v1/tasks/templates`
  // contract does not carry banner text, so there is nothing to snapshot against at runtime; this
  // locks the hard-coded copy so any accidental edit (or a deliberate backend change that a dev
  // forgets to mirror) is caught in CI. Source of truth: PlanReconciliationTaskDescriptor / its
  // Messages interface (planReconciliationTopPanelMessage / planReconciliationBottomPanelMessage).
  // Detecting backend-side drift automatically would require exposing the copy via the REST
  // template — a deliberately deferred initiative (see ticket scope, NEXUS-47948 era).
  it('freezes the banner copy verbatim to match the backend Messages source', () => {
    expect(overrides.topAlertBanner.bannerText).toBe(
      'This task generates recovery plans to reconcile blobstores and their associated repositories. ' +
        'To apply generated plans, run the "Repair - Execute Data Repair Plan" task. ' +
        'You can view created plans using the /v1/reconcile/plan API'
    );
    expect(overrides.bottomAlertBanner.bannerText).toBe(
      'Tasks do not run automatically after creation. You must manually run the task after saving.'
    );
  });

  it('orders the top banner first and the bottom banner last', () => {
    expect(overrides.topAlertBanner.order).toBe(0);
    expect(overrides.bottomAlertBanner.order).toBe(999);
  });

  it('makes onlyNotify a checkbox with the missing-blob copy', () => {
    expect(overrides.onlyNotify.type).toBe('checkbox');
    expect(overrides.onlyNotify.helpText).toContain('do not remove database records');
  });

  it('makes blobstoreName a blobstore-filtering multi-select (optional)', () => {
    expect(overrides.blobstoreName.type).toBe('itemselect');
    expect(overrides.blobstoreName.multiSelect).toBe(true);
    expect(overrides.blobstoreName.filterByBlobstore).toBe(true);
    expect(overrides.blobstoreName.required).toBe(false);
  });

  it('makes repositoryName an optional multi-select without its own blob-store filter flag', () => {
    expect(overrides.repositoryName.type).toBe('itemselect');
    expect(overrides.repositoryName.multiSelect).toBe(true);
    expect(overrides.repositoryName.required).toBe(false);
    expect(overrides.repositoryName.filterByBlobstore).toBeUndefined();
  });

  it('keeps taskScope a required radio field', () => {
    expect(overrides.taskScope.type).toBe('taskScope');
    expect(overrides.taskScope.required).toBe(true);
  });

  it('hides the name template field and flags it as the task-name default', () => {
    expect(overrides.name.hidden).toBe(true);
    expect(overrides.name.isNameTemplate).toBe(true);
    expect(overrides.name.required).toBe(false);
  });

  it('groups the duration fields under scope=duration and the date fields under scope=dates', () => {
    expect(overrides.sinceDays.scope).toBe('duration');
    expect(overrides.sinceHours.scope).toBe('duration');
    expect(overrides.sinceMinutes.scope).toBe('duration');
    expect(overrides.reconcileStartDate.scope).toBe('dates');
    expect(overrides.reconcileEndDate.type).toBe('date');
    expect(overrides.reconcileEndDate.scope).toBe('dates');
    // None of the timespan fields are required (backend defaults sinceMinutes=30).
    expect(overrides.sinceMinutes.required).toBe(false);
    expect(overrides.reconcileStartDate.required).toBe(false);
  });

  it('exposes the all-blob-stores sentinel constant', () => {
    expect(ALL_BLOB_STORES).toBe('(All Blob Stores)');
  });

  it('defines no cloud-only fields — the task is self-hosted (pro + community) only', () => {
    // blobstore.planReconciliation is not registered in the cloud edition
    // (@ConditionalOnEdition(pro, community); cloud=true reverted in NEXUS-47948), so there is no
    // cloud variant and no cloud-only field (e.g. the old planOptionsLabelId static info).
    expect(overrides.planOptionsLabelId).toBeUndefined();
  });
});

describe('date helpers (Classic m/d/Y <-> input YYYY-MM-DD)', () => {
  it('converts m/d/Y to ISO for date inputs', () => {
    expect(mdyToIso('06/24/2026')).toBe('2026-06-24');
    expect(mdyToIso('6/4/2026')).toBe('2026-06-04');
  });

  it('passes already-ISO values through unchanged', () => {
    expect(mdyToIso('2026-06-24')).toBe('2026-06-24');
  });

  it('returns empty string for blank/invalid input', () => {
    expect(mdyToIso('')).toBe('');
    expect(mdyToIso(undefined)).toBe('');
    expect(mdyToIso('not-a-date')).toBe('');
  });

  it('converts ISO back to leading-zero m/d/Y for backend round-trip parity', () => {
    expect(isoToMdy('2026-06-24')).toBe('06/24/2026');
    expect(isoToMdy('')).toBe('');
  });

  it('round-trips a date without drift', () => {
    expect(isoToMdy(mdyToIso('06/24/2026'))).toBe('06/24/2026');
  });
});

describe('singleton task types', () => {
  it('treats blobstore.planReconciliation as a singleton', () => {
    expect(isSingletonTaskType(PLAN_RECONCILE_TYPE_ID)).toBe(true);
    expect(SINGLETON_TASK_TYPES.has(PLAN_RECONCILE_TYPE_ID)).toBe(true);
  });

  it('treats blobstore.executeReconciliationPlan as a singleton', () => {
    expect(isSingletonTaskType(EXECUTE_RECONCILE_PLAN_TYPE_ID)).toBe(true);
    expect(SINGLETON_TASK_TYPES.has(EXECUTE_RECONCILE_PLAN_TYPE_ID)).toBe(true);
  });

  it('does not treat unrelated task types as singletons', () => {
    expect(isSingletonTaskType('repository.cleanup')).toBe(false);
    expect(isSingletonTaskType('db.backup')).toBe(false);
    expect(isSingletonTaskType(undefined)).toBe(false);
  });

  describe('filterCreatableTaskTypes', () => {
    const types = [
      { id: PLAN_RECONCILE_TYPE_ID, name: 'Repair - Data Repair Plan' },
      { id: 'repository.cleanup', name: 'Cleanup' },
      { id: 'db.backup', name: 'Backup' },
    ];

    it('drops a singleton type once an instance already exists', () => {
      const result = filterCreatableTaskTypes(types, new Set([PLAN_RECONCILE_TYPE_ID]));
      expect(result.map((t) => t.id)).toEqual(['repository.cleanup', 'db.backup']);
    });

    it('keeps the singleton type when none exists yet', () => {
      const result = filterCreatableTaskTypes(types, new Set());
      expect(result.map((t) => t.id)).toContain(PLAN_RECONCILE_TYPE_ID);
    });

    it('never drops a non-singleton type, even when an instance already exists', () => {
      const result = filterCreatableTaskTypes(types, new Set(['repository.cleanup', 'db.backup']));
      expect(result.map((t) => t.id)).toEqual([
        PLAN_RECONCILE_TYPE_ID,
        'repository.cleanup',
        'db.backup',
      ]);
    });
  });
});

describe('manual-only task types', () => {
  it('treats blobstore.planReconciliation as manual-only', () => {
    expect(isManualOnlyTaskType(PLAN_RECONCILE_TYPE_ID)).toBe(true);
    expect(MANUAL_ONLY_TASK_TYPES.has(PLAN_RECONCILE_TYPE_ID)).toBe(true);
  });

  it('does not treat unrelated task types as manual-only', () => {
    expect(isManualOnlyTaskType('repository.cleanup')).toBe(false);
    expect(isManualOnlyTaskType('db.backup')).toBe(false);
    expect(isManualOnlyTaskType(undefined)).toBe(false);
  });
});

describe('resolveDefaultScope', () => {
  it('returns the constant for the Execute Data Repair Plan task type id', () => {
    expect(EXECUTE_RECONCILE_PLAN_TYPE_ID).toBe('blobstore.executeReconciliationPlan');
    expect(TASK_SCOPE_DATES).toBe('dates');
    // Task 2 adds the override entry with defaultScope:'dates' so this resolves to 'dates'.
    expect(resolveDefaultScope(EXECUTE_RECONCILE_PLAN_TYPE_ID)).toBe('dates');
  });

  it('defaults to "duration" for tasks without a defaultScope override', () => {
    expect(resolveDefaultScope('blobstore.planReconciliation')).toBe('duration');
    expect(resolveDefaultScope(undefined)).toBe('duration');
    expect(resolveDefaultScope('some.unknown.task')).toBe('duration');
  });
});

describe('Execute Data Repair Plan overrides', () => {
  const T = EXECUTE_RECONCILE_PLAN_TYPE_ID;

  it('registers all Execute fields with the right types and order', () => {
    expect(resolveTaskFieldMeta(T, 'topAlertBanner')?.type).toBe('alertBanner');
    expect(resolveTaskFieldMeta(T, 'topAlertBanner')?.bannerText).toContain('executes recovery plans');
    expect(resolveTaskFieldMeta(T, 'planOptionsLabelId')?.type).toBe('staticInfo');
    expect(resolveTaskFieldMeta(T, 'planOptionsLabelId')?.label).toBe('Execution Information');
    expect(resolveTaskFieldMeta(T, 'planOptionsLabelId')?.cloudLabel).toBe('Plan Options');
    expect(resolveTaskFieldMeta(T, 'planInformationLabelId')?.type).toBe('staticInfo');
    expect(resolveTaskFieldMeta(T, 'planInformationLabelId')?.label).toBe('Plan Information');
    expect(resolveTaskFieldMeta(T, 'planInformation')?.type).toBe('planInformation');
    expect(resolveTaskFieldMeta(T, 'blobstoreName')?.type).toBe('itemselect');
    expect(resolveTaskFieldMeta(T, 'blobstoreName')?.filterByBlobstore).toBeUndefined();
    expect(resolveTaskFieldMeta(T, 'blobstoreName')?.readOnly).toBe(true);
    expect(resolveTaskFieldMeta(T, 'repositoryName')?.type).toBe('itemselect');
    expect(resolveTaskFieldMeta(T, 'repositoryName')?.readOnly).toBe(true);
    expect(resolveTaskFieldMeta(T, 'taskScope')?.type).toBe('taskScope');
    expect(resolveTaskFieldMeta(T, 'taskScope')?.readOnly).toBe(true);
    expect(resolveTaskFieldMeta(T, 'reconcileStartDate')?.scope).toBe('dates');
    expect(resolveTaskFieldMeta(T, 'reconcileStartDate')?.readOnly).toBe(true);
    expect(resolveTaskFieldMeta(T, 'reconcileEndDate')?.scope).toBe('dates');
    expect(resolveTaskFieldMeta(T, 'reconcileEndDate')?.readOnly).toBe(true);
    expect(resolveTaskFieldMeta(T, 'name')?.hidden).toBe(true);
    expect(resolveTaskFieldMeta(T, 'name')?.isNameTemplate).toBe(true);
  });

  it('defaults the Execute task scope to "dates"', () => {
    expect(resolveDefaultScope(T)).toBe('dates');
  });

  it('does not declare any duration fields for the Execute task', () => {
    const entry = TASK_TYPE_FIELD_OVERRIDES[T];
    expect(entry.sinceDays).toBeUndefined();
    expect(entry.sinceHours).toBeUndefined();
    expect(entry.sinceMinutes).toBeUndefined();
  });
});

describe('malware.remediator (NEXUS-53359)', () => {
  it('renders the requirements field as an info banner, not a checkbox', () => {
    const meta = resolveTaskFieldMeta('malware.remediator', 'malwareRemediatorTaskRequirements');
    expect(meta?.type).toBe('alertBanner');
    expect(meta?.bannerVariant).toBe('info');
    expect(meta?.bannerText).toMatch(/Repository Firewall enabled with the Security-Malicious policy/);
    expect(meta?.neverSerialize).toBe(true);
  });

  it('renders the cleanup message field as a warning banner, not a checkbox', () => {
    const meta = resolveTaskFieldMeta('malware.remediator', 'enableMalwareCleanupMessage');
    expect(meta?.type).toBe('alertBanner');
    expect(meta?.bannerVariant).toBe('warning');
    expect(meta?.bannerText).toMatch(/may remove dependencies currently in use/);
    expect(meta?.neverSerialize).toBe(true);
  });

  it('maps enableMalwareCleanup to a checkbox with the descriptor label/help', () => {
    const meta = resolveTaskFieldMeta('malware.remediator', 'enableMalwareCleanup');
    expect(meta?.type).toBe('checkbox');
    expect(meta?.label).toBe('Enable Malware Cleanup');
    expect(meta?.helpText).toMatch(/scheduled to be cleaned up/);
  });

  it('maps repositoryName to a repo selector with the descriptor help text', () => {
    const meta = resolveTaskFieldMeta('malware.remediator', 'repositoryName');
    expect(meta?.type).toBe('repo');
    expect(meta?.helpText).toBe('Select the proxy repository to remove malware from');
  });

  it('filters repositoryName to proxy maven2/npm/nuget/pypi with an All entry', () => {
    expect(TASK_TYPE_REPO_FILTERS['malware.remediator']).toEqual({
      repositoryName: { types: ['proxy'], formats: ['maven2', 'npm', 'nuget', 'pypi'], includeAll: true },
    });
  });

  it('removes the stale, descriptor-less global enableMalwareCleanup* defaults', () => {
    expect(getTaskFieldMeta('enableMalwareCleanup')).toBeUndefined();
    expect(getTaskFieldMeta('enableMalwareCleanupMessage')).toBeUndefined();
  });

  it('banner fields are neverSerialize — empty values must not be sent to the backend', () => {
    // validateTask iterates data.properties and serializeProperties sends them to the API.
    // neverSerialize: true ensures both display-only fields are stripped before the payload
    // is built. If either loses the flag, the empty string would reach the backend.
    expect(resolveTaskFieldMeta('malware.remediator', 'malwareRemediatorTaskRequirements')?.neverSerialize).toBe(true);
    expect(resolveTaskFieldMeta('malware.remediator', 'enableMalwareCleanupMessage')?.neverSerialize).toBe(true);
  });
});
