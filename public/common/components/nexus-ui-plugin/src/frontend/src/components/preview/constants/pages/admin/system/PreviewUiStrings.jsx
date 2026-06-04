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
import { faEye } from '@fortawesome/free-solid-svg-icons';

export default {
  PREVIEW_UI_SETTINGS: {
    title: 'Nexus One UI Settings',
    MENU: {
      text: 'Nexus One UI',
      description: 'Configure access to the Nexus One UI experience',
      icon: faEye,
    },
    description: 'Control access to the Nexus One UI experience. The Nexus One UI provides a modern, redesigned interface with improved usability and new features.',
    INFO: {
      reloadNote: 'Changes to these settings take effect after the page reloads.',
    },
    ACTIONS: {
      discard: 'Discard Changes',
      save: 'Save',
      unsavedChanges: 'Unsaved changes',
      loading: 'Loading...',
    },
    ACCESS_CONTROL: {
      title: 'Access Control',
    },
    ANONYMOUS: {
      label: 'Anonymous Users',
      description: 'Allow anonymous (non-authenticated) users to access the Nexus One UI.',
      property: 'nexus.preview.ui.anonymous.enabled',
    },
    LOGGEDIN: {
      label: 'Logged-in Users',
      description: 'Allow authenticated users to access the Nexus One UI.',
      property: 'nexus.preview.ui.loggedin.enabled',
    },
    ROLLOUT: {
      title: 'Rollout Control',
      DEFAULT_TO_PREVIEW: {
        label: 'Default to Nexus One UI',
        helpText: 'When enabled, all users land on the Nexus One UI after login. Users can still switch back to the Classic UI at any time using the toggle in the page header.',
        property: 'preview.ui.default.enabled',
      },
      DISABLE_SWITCH_FEEDBACK: {
        label: 'Disable Switch Feedback',
        helpText: 'Hide the feedback prompt and prevent Nexus One UI from sending feedback when users switch to the Classic UI.',
        property: 'preview.ui.switch.feedback.disabled',
      },
    },
    DISPOSAL: {
      title: 'Disable Classic UI',
      DISABLE_LEGACY: {
        label: 'Disable Classic UI',
        helpText: 'Completely remove access to the Classic UI (ExtJS). The Nexus One UI becomes the only available interface, and the header toggle to switch back is removed.',
        warningText: 'CAUTION: When enabled, all users lose access to the Classic UI immediately. If users encounter bugs or missing features in the Nexus One UI, they will have no fallback until an administrator disables this setting.',
        property: 'preview.ui.legacy.disabled',
      },
    },
  },
};
