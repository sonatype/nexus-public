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
export default {
  HOSTED_REPOSITORIES_EVALUATION: {
    MENU: {
      text: 'Hosted Repositories Evaluation',
    },
    title: 'Hosted Repositories Evaluation',
    breadcrumb: 'Hosted Repositories Evaluation',
    tabs: {
      settings: 'SETTINGS',
      repositories: 'REPOSITORIES',
      monitoringSettings: 'Monitoring Settings',
      monitoredRepositories: 'Monitored Repositories'
    },
    monitoringSettings: {
      title: 'Monitoring Settings',
      activityTimeFrameLabel: 'Activity Time Frame',
      activityTimeFrameHelpText: 'Set the time frame for evaluating components based on recent repository activity.',
      activityTimeFramePlaceholder: 'Select Activity Time Frame',
      activityTimeFrameOptions: [
        { value: '30', label: '30 Days' },
        { value: '60', label: '60 Days' },
        { value: '90', label: '90 Days' }
      ],
      artifactLatestVersionsLabel: 'Artifact Latest Versions',
      artifactLatestVersionsHelpText: 'Set the number versions of the artifact for evaluation',
      artifactLatestVersionsPlaceholder: 'Select Artifact Latest Versions',
      artifactLatestVersionsOptions: [
        { value: '1', label: '1' },
        { value: '2', label: '2' },
        { value: '3', label: '3' },
        { value: '5', label: '5' }
      ],
      policyEvaluationStageLabel: 'Policy Evaluation Stage',
      policyEvaluationStageHelpText: 'Select the lifecycle stage for continuous policy evaluation.',
      policyEvaluationStagePlaceholder: 'Select Policy Evaluation Stage',
      policyEvaluationStageOptions: [
        { value: 'build', label: 'Build' },
        { value: 'stage-release', label: 'Stage Release' },
        { value: 'release', label: 'Release' },
        { value: 'operate', label: 'Operate' }
      ],
      newHostedReposLabel: 'New Hosted Repositories',
      newHostedReposText: 'Apply global configuration for all new hosted repositories created'
    },
    packageFilePatterns: {
      title: 'Package File Patterns',
      description: 'We\'ll focus on the files that matter most, based on known file patterns for each package format. This helps reduce noise by avoiding unnecessary scans of metadata or source files',
      maven: 'Evaluations target .jar, .war, and .ear files — the main deployable outputs of Java builds.',
      mavenLabel: 'Maven:',
      npm: 'Evaluate .tgz package files that follow versioned naming conventions',
      npmLabel: 'npm:',
      python: 'We match .whl and .tar.gz files, which are the primary distribution formats for Python packages.',
      pythonLabel: 'Python:'
    },
    buttons: {
      cancel: 'Cancel',
      next: 'Next',
      save: 'Save',
      update: 'Update'
    },
    repositoriesTable: {
      searchPlaceholder: 'Search repositories...',
      formatFilterLabel: 'Format',
      monitoringFilterLabel: 'All Monitoring',
      monitoringFilterOptions: {
        all: 'All Monitoring',
        enabled: 'Enabled',
        disabled: 'Disabled',
        custom: 'Custom'
      },
      showingText: 'Showing',
      ofText: 'of',
      repositoriesText: 'repositories',
      columnHeaders: {
        select: 'Select',
        repositoryName: 'Repository Name',
        format: 'Format',
        size: 'Size',
        components: 'No. Components'
      }
    },
    INCOMPLETE_MODAL: {
      TITLE: 'No Repositories Selected',
      MESSAGE: 'Please select at least one repository to enable monitoring.',
      CANCEL: 'Cancel',
      CONTINUE: 'Back to Settings'
    },
    UNSAVED_CHANGES_MODAL: {
      TITLE: 'Unsaved Changes',
      MESSAGE: 'You have unsaved changes. Are you sure you want to leave this page?',
      CANCEL: 'Stay on Page',
      CONTINUE: 'Leave Page'
    },
    INCOMPLETE_MODAL: {
      TITLE: 'No Repositories Selected',
      MESSAGE: 'You have not selected any repositories for monitoring. Please select at least one repository or go back to modify settings.',
      CANCEL: 'Stay on Page',
      CONTINUE: 'Go Back to Settings'
    },
    ERROR_MODAL: {
      TITLE: 'Save Failed',
      MESSAGE: 'An error occurred while saving the settings. Please try again.',
      CLOSE: 'Close'
    }
  }
};
