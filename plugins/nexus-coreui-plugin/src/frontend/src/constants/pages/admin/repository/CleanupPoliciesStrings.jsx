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
import React from 'react';
import {NxTextLink} from '@sonatype/react-shared-components';

export default {
  CLEANUP_POLICIES: {
    MENU: {
      text: 'Cleanup Policies',
      description: 'Manage component removal configuration'
    },

    CREATE_TITLE: 'Create Cleanup Policy',
    EDIT_TITLE: 'Edit Cleanup Policy',
    SUB_TITLE: 'Define Cleanup Policy',
    DESCRIPTION: <>
      <b>Cleanup policies</b> allow you to automatically delete unused
      components and free up storage space in your Nexus repository
      instance. You can schedule cleanup policies to run at specific
      intervals and define <b>cleanup criteria</b> for selecting
      components to delete.
    </>,

    HELP_TITLE: 'What is a cleanup policy?',
    HELP_TEXT: <>
      Cleanup policies can be used to remove content from your repositories. These policies will execute
      at the configured frequency. Once created, a cleanup policy must be assigned to a repository from{' '}
      <NxTextLink href="#admin/repository/repositories">the repository configuration screen</NxTextLink>.
      For more information, check{' '}
      <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/cleanup-policy">
        the documentation
      </NxTextLink>.
    </>,
    EMPTY_MESSAGE: 'No cleanup policies were found',
    CREATE_BUTTON: 'Create Cleanup Policy',
    FILTER_PLACEHOLDER: 'Filter',

    NAME_LABEL: 'Name',
    FORMAT_LABEL: 'Format',
    DESCRIPTION_LABEL: 'Description',
    CRITERIA_LABEL: 'Cleanup Criteria',
    CRITERIA_DESCRIPTION: 'Remove all componets that match all selected criteria',
    LAST_UPDATED_CHECKBOX_TITLE: (enabled) => `${enabled ? 'Disable' : 'Enable'} Component Age Criteria`,
    LAST_UPDATED_LABEL: 'Component Age',
    LAST_DOWNLOADED_CHECKBOX_TITLE: (enabled) => `${enabled ? 'Disable' : 'Enable'} Component Usage Criteria`,
    LAST_DOWNLOADED_LABEL: 'Component Usage',
    RELEASE_TYPE_CHECKBOX_TITLE: (enabled) => `${enabled ? 'Disable' : 'Enable'} Release Type Criteria`,
    RELEASE_TYPE_LABEL: 'Release Type',
    ASSET_NAME_CHECKBOX_TITLE: (enabled) => `${enabled ? 'Disable' : 'Enable'} Asset Name Matcher Criteria`,
    ASSET_NAME_LABEL: 'Asset Name Matcher',
    FORMAT_SELECT: 'Select a format...',
    RELEASE_TYPE_SELECT: 'Select the release type:',
    REPOSITORY_SELECT: 'Select a repository...',

    NAME_DESCRIPTION: 'Use a unique name for the cleanup policy',
    FORMAT_DESCRIPTION: 'The format that this cleanup policy can be applied to',
    RELEASE_TYPE_DESCRIPTION: 'Remove components that are of the following release type:',
    ASSET_NAME_DESCRIPTION: 'Contains at least one asset where the name matches this regular expression:',

    LAST_UPDATED_PREFIX: 'Published over:',
    LAST_UPDATED_SUFFIX: 'days ago',
    LAST_DOWNLOADED_PREFIX: 'Haven’t been downloaded in:',
    LAST_DOWNLOADED_SUFFIX: 'days',

    RELEASE_TYPE: {
      RELEASES_AND_SNAPSHOT: {
        id: '',
        label: 'Releases & Pre-Releases/Snapshots'
      },
      RELEASES: {
        id: 'RELEASES',
        label: 'Releases'
      },
      PRERELEASES: {
        id: 'PRERELEASES',
        label: 'Pre-Releases/Snapshots'
      },
    },

    PREVIEW: {
      TITLE: 'Cleanup policy preview',
      REPOSITORY_LABEL: 'Preview Repository',
      REPOSITORY_DESCRIPTION: 'Select a repository to preview what might get cleaned up if this policy was applied',
      INVALID_QUERY: 'The filter is invalid so results could not be retrieved.',
      BUTTON: 'Preview',
      RESULTS: 'Preview Results',
      NAME_COLUMN: 'Name',
      GROUP_COLUMN: 'Group',
      VERSION_COLUMN: 'Version',
      EMPTY: 'No assets in repository matched the criteria',
      COMPONENT_COUNT: (actual, total) => {
        if (total === 0) {
          return 'Criteria matched no components.';
        }
        else if (total > 0) {
          return `Component count (matching criteria) viewing ${actual} out of ${total}.`;
        }
        return `Component count (matching criteria) viewing first ${actual}`;
      },
      SAMPLE_WARNING: 'Results may only be a sample of what will be deleted using the current criteria.',
      BUTTON_TOOLTIP: 'Please select a repository and at least one cleanup criteria',
    },

    DRY_RUN: {
      BUTTON: 'Generate CSV Report',
      REPOSITORY_DESCRIPTION: 'Export a spreadsheet listing which components would be deleted today based on selected cleanup policy settings. Your export will list component namespaces, names, versions and paths.',
      REPOSITORY_SELECT: 'Select a repository',
      TITLE: 'Preview Cleanup Policy Results',
    },

    MESSAGES: {
      SAVE_ERROR: 'An error occurred while saving the cleanup policy',
      DELETE_ERROR: (name) => `Cleanup policy ${name} is in use and cannot be deleted`,

      CONFIRM_DELETE: {
        TITLE: 'Delete cleanup policy',
        MESSAGE: (inUseCount) => inUseCount ?
            `This Cleanup Policy is used by ${inUseCount} repositories` :
            'This Cleanup Policy is not used by any repository',
        YES: 'Delete',
        NO: 'Cancel'
      }
    }
  }
};
