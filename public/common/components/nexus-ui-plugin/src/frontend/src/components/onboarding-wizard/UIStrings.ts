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
  ONBOARDING_WIZARD: {
    ARIA: {
      TITLE: 'Onboarding Wizard',
      DESCRIPTION: 'Please complete the onboarding steps to configure your instance.',
    },
    WELCOME: {
      LOGO_ALT: 'Sonatype Nexus Repository',
      TITLE: `Let's Get You Set Up`,
      DESCRIPTION:
        'This wizard will guide you through a few quick steps to get your Nexus Repository ready for use.',
    },
    SETUP_COMPLETE: {
      TITLE: 'Setup Complete',
      DESCRIPTION: 'Your Nexus Repository is ready to use.',
      STILL_REQUIRED_MESSAGE:
        'Some onboarding steps still need to be completed. Please re-finalize your choices.',
      NETWORK_ERROR_MESSAGE: 'A network error occurred. Please try again.',
    },
    ISSUE_OCCURRED: {
      TITLE: 'Issue Occurred',
      MESSAGE:
        'An issue occurred during setup. You may need to re-finalize your choices or selections.',
    },
    DISMISS_TOAST: {
      MESSAGE:
        'Setup was not completed. Some features may require additional configuration.',
    },
    VERIFYING: {
      MESSAGE: 'Verifying setup...',
    },
    LOADING: {
      MESSAGE: 'Loading...',
    },
    UNKNOWN_STEP: {
      MESSAGE: 'This step type is not yet supported.',
    },
    CHANGE_ADMIN_PASSWORD: {
      TITLE: 'Set an admin password',
      DESCRIPTION:
        'You are still using the default admin password. Choose a new password to secure your instance before continuing.',
      PASSWORD_LABEL: 'New password',
      CONFIRM_LABEL: 'Confirm password',
      HELPER_TEXT: 'Enter a strong password.',
      MISMATCH_ERROR: 'Passwords do not match',
      NETWORK_ERROR_FALLBACK:
        'Could not save your password. Check your network connection and try again.',
      SUCCESS_MESSAGE: 'Password saved',
      HIDDEN_SUBMIT_LABEL: 'Submit password',
    },
    ACTIONS: {
      GET_STARTED: 'Get Started',
      NEXT: 'Next',
      FINISH: 'Finish',
      COMPLETE_SETUP: 'Complete Setup',
      DISMISS: 'Dismiss for now',
      RETRY: 'Retry',
    },
    CONFIGURE_ANONYMOUS_ACCESS: {
      TITLE: 'Configure Anonymous Access',
      SUBTITLE: 'Control access to your repositories',
      ENABLE_LABEL: 'Enable anonymous access',
      DISABLE_LABEL: 'Disable anonymous access',
      ENABLE_OPTION_DESCRIPTION: 'Allow anonymous users to read from repositories',
      DISABLE_OPTION_DESCRIPTION: 'Require authentication for all repository access',
      DESCRIPTION:
        'Anonymous access allows users to browse and download repository content without authentication. Disable anonymous access to require authentication for all users and build tools.',
      MORE_INFO_LABEL: 'Learn more',
      MORE_INFO_HREF: 'https://links.sonatype.com/products/nexus/anonymous-access/docs',
    },
    COMMUNITY_DISCOVER: {
      TITLE: 'Welcome to Nexus Repository Community Edition',
      SUBTITLE: 'Manage, store, and distribute software components.',
      LEARN_MORE_LABEL: 'Learn More',
      BENEFITS_LIST: [
        {
          label: 'Repository formats',
          description: 'Support for Docker, Maven, npm, PyPI, NuGet, Helm, and more.',
        },
        {
          label: 'Search and browse components',
          description: 'Find components across your repositories.',
        },
        {
          label: 'Security and access management',
          description: 'Integrate with LDAP and configure role-based access.',
        },
        {
          label: 'REST API and automation',
          description: 'Automate repository management and CI/CD workflows.',
        },
      ] as ReadonlyArray<{ readonly label: string; readonly description: string }>,
    },
    COMMUNITY_EULA: {
      TITLE: 'End User License Agreement',
      SUBTITLE: 'Please read and accept the terms and conditions',
      IFRAME_TITLE: 'End User License Agreement',
      CHECKBOX_LABEL: 'I accept the terms and conditions',
      READ_FULL_LICENSE_LABEL: 'Read Full License Agreement',
      SUCCESS_MESSAGE: 'Agreement accepted',
      LOADING_ERROR: 'Unable to load the End User License Agreement.',
      SUBMISSION_ERROR: 'Failed to submit your agreement. Please try again.',
    },
  },
};
