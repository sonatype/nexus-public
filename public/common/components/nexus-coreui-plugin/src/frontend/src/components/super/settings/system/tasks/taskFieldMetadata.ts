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
}

export const TASK_FIELD_UI: Record<string, TaskFieldMeta> = {
  // Repository selectors
  repositoryName: { label: 'Repository', type: 'repo', helpText: 'Select the repository to operate on' },
  moveRepositoryName: { label: 'Repository to Move', type: 'repo', helpText: 'Select the repository to move' },

  // Blob store selectors
  blobstoreName: { label: 'Blob Store', type: 'blobstore', helpText: 'Select the blob store' },
  moveTargetBlobstore: { label: 'Target Blob Store', type: 'blobstore', helpText: 'Select the destination blob store' },
  memberToRemove: { label: 'Member to Remove', type: 'blobstore', helpText: 'Select the blob store member to remove' },
  fromGroup: { label: 'From Group', type: 'blobstore', helpText: 'Select the blob store group' },

  // Days fields
  olderThanDays: { label: 'Age Threshold (days)', type: 'number', helpText: 'Number of days (0 = all files)', min: 0, max: 36500, placeholder: '0' },
  blobsOlderThan: { label: 'Blobs Older Than (days)', type: 'number', helpText: 'Number of days (0 = all blobs)', min: 0, max: 36500, placeholder: '0' },
  firstCreatedDays: { label: 'First Created (days)', type: 'number', helpText: 'Days since creation (0 = any age)', min: 0, max: 36500, placeholder: '0' },
  lastUpdatedDays: { label: 'Last Updated (days)', type: 'number', helpText: 'Days since last update (0 = any)', min: 0, max: 36500, placeholder: '0' },
  snapshotRetentionDays: { label: 'Snapshot Retention (days)', type: 'number', helpText: 'Days to retain snapshots', min: 0, max: 36500, placeholder: '30' },
  gracePeriodInDays: { label: 'Grace Period (days)', type: 'number', helpText: 'Days after release before removal (0 = immediate)', min: 0, max: 36500, placeholder: '0' },
  lastUsed: { label: 'Last Used (days)', type: 'number', helpText: 'Days since last download', min: 1, max: 36500, placeholder: '1' },
  sinceDays: { label: 'Since (days)', type: 'number', helpText: 'Number of days to look back', min: 0, max: 36500, placeholder: '0' },

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
  forceRebuild: { label: 'Force Full Rebuild', type: 'checkbox', helpText: 'Complete rebuild instead of incremental update' },
  rebuildAptMetadataFullRebuild: { label: 'Full Rebuild', type: 'checkbox', helpText: 'Force a complete APT metadata rebuild' },
  rebuildHelmMetadataFullRebuild: { label: 'Full Rebuild', type: 'checkbox', helpText: 'Force a complete Helm metadata rebuild' },
  yumMetadataCaching: { label: 'Enable Caching', type: 'checkbox', helpText: 'Cache rebuilt Yum metadata for faster access' },
  deleteAssociatedComponents: { label: 'Delete Associated Components', type: 'checkbox', helpText: 'Also delete components when removing matched tags' },
  restrictComponentDelete: { label: 'Restrict Component Delete', type: 'checkbox', helpText: 'Only delete components that have no other tags' },
  enableHardLinks: { label: 'Enable Hard Links', type: 'checkbox', helpText: 'Use hard links instead of copying files (faster, same filesystem only)' },
  enableMalwareCleanup: { label: 'Enable Auto Cleanup', type: 'checkbox', helpText: 'Automatically remove components identified as malicious' },
  enableMalwareCleanupMessage: { label: 'Show Cleanup Message', type: 'checkbox', helpText: 'Display notification when components are cleaned up' },
  onlyNotify: { label: 'Notify Only', type: 'checkbox', helpText: 'Send notifications without making changes (dry run)' },

  // Text fields with examples
  nameRegex: { label: 'Tag Name Pattern', type: 'string', helpText: 'Regular expression to match tag names', placeholder: '.+-SNAPSHOT' },
  groupId: { label: 'Group ID', type: 'string', helpText: 'Maven group ID to filter (leave empty for all)', placeholder: 'org.apache.commons' },
  artifactId: { label: 'Artifact ID', type: 'string', helpText: 'Maven artifact ID to filter (leave empty for all)', placeholder: 'commons-lang3' },
  baseVersion: { label: 'Base Version', type: 'string', helpText: 'Version to filter (leave empty for all)', placeholder: '1.0-SNAPSHOT' },
  packageName: { label: 'Package Name', type: 'string', helpText: 'npm package name to rebuild metadata for (leave empty for all)', placeholder: '@scope/package-name' },
};

/**
 * Get UI metadata for a task field by ID.
 * Returns undefined if the field is not in the known set.
 */
export function getTaskFieldMeta(fieldId: string): TaskFieldMeta | undefined {
  return TASK_FIELD_UI[fieldId];
}
