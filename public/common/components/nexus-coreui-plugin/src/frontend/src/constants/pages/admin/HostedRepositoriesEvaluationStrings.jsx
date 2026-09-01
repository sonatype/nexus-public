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
    breadcrumb: 'Hosted Repositories Evaluation Group',
    tabs: {
      settings: 'Global Evaluation Settings',
      repositories: 'REPOSITORIES',
      monitoringSettings: 'Global Evaluation Settings',
      monitoredRepositories: 'Monitored Repositories'
    },
    monitoringSettings: {
      title: 'Global Monitoring Settings',
      evaluationContextDescription: 'Components are evaluated when they match either setting: recent activity within the selected time frame, or the latest deployed versions.',
      evaluationDepthMethodLabel: 'Evaluation Depth Method',
      evaluationDepthMethodHelpText: 'Select how evaluation depth is determined for hosted repositories.',
      evaluationDepthMethodOptions: [
        { value: 'activityTimeFrame', label: 'Activity Time Frame' },
        { value: 'latestDeployedVersions', label: 'Latest Deployed Versions' }
      ],
      activityTimeFrameLabel: 'Activity Time Frame',
      activityTimeFrameHelpText: 'Set the time frame for evaluating components based on recent repository activity.',
      activityTimeFramePlaceholder: 'Select Activity Time Frame',
      activityTimeFrameOptions: [
        { value: '30', label: '30 Days' },
        { value: '60', label: '60 Days' },
        { value: '90', label: '90 Days' }
      ],
      artifactLatestVersionsLabel: 'Latest Deployed Versions',
      artifactLatestVersionsHelpText: 'Set the number of most recently deployed component versions to evaluate.',
      artifactLatestVersionsPlaceholder: 'Select Latest Deployed Versions',
      artifactLatestVersionsOptions: [
        { value: '1', label: '1' },
        { value: '2', label: '2' },
        { value: '3', label: '3' },
        { value: '4', label: '4' },
        { value: '5', label: '5' }
      ],
      policyEvaluationStageLabel: 'Policy Evaluation Stage',
      policyEvaluationStageHelpText: 'Select the policy evaluation stage used for the initial audit and ongoing monitoring.',
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
      description: 'We\'ll scan the files that matter most, based on known file patterns for each package format. Examples for some of the most common formats:',
      maven: '.jar, .war, and .ear files — the main deployable outputs of Java builds.',
      mavenLabel: 'Maven:',
      npm: '.tgz package files that follow versioned naming conventions.',
      npmLabel: 'npm:',
      python: '.whl and .tar.gz files, which are the primary distribution formats for Python.',
      pythonLabel: 'PyPI:'
    },
    buttons: {
      back: 'Back',
      cancel: 'Cancel',
      next: 'Next',
      save: 'Save',
      update: 'Update',
      enableMonitoring: 'Enable Monitoring',
      disableMonitoring: 'Disable Monitoring',
      clearSelection: 'Clear Selection'
    },
    savingMask: 'Saving…',
    repositoriesTable: {
      searchPlaceholder: 'Search repositories...',
      formatFilterLabel: 'All',
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
    UNSAVED_CHANGES_MODAL: {
      TITLE: 'Unsaved Changes',
      MESSAGE: 'You have unsaved changes. Are you sure you want to leave this page?',
      CANCEL: 'Stay on Page',
      CONTINUE: 'Leave Page'
    },
    INCOMPLETE_MODAL: {
      MESSAGE: 'You have not selected any repositories for monitoring. Please select at least one repository or go back to modify settings.',
    },
    ERROR_MODAL: {
      TITLE: 'Save Failed',
      MESSAGE: 'An error occurred while saving the settings. Please try again.',
      CLOSE: 'Close'
    },
    SETTINGS_ERROR_MODAL: {
      TITLE: 'Error',
      CLOSE: 'Close'
    }
  }
};
