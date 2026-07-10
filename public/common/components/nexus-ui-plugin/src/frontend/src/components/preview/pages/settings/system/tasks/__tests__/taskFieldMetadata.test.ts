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

import { TASK_FIELD_UI, TASK_TYPE_REPO_FILTERS, TaskFieldMeta, TaskRepoFilter } from '../taskFieldMetadata';

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
});
