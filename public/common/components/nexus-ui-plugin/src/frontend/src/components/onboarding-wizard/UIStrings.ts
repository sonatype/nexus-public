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
      DESCRIPTION:
        'Your instance is now ready to use. Explore Sonatype Nexus Repository to unlock its full potential.',
    },
    VERIFYING: {
      MESSAGE: 'Verifying setup...',
    },
    LOADING: {
      MESSAGE: 'Loading...',
    },
    ISSUE_OCCURRED: {
      TITLE: 'Something went wrong',
      FALLBACK_MESSAGE: 'An unexpected error occurred while setting up your instance.',
      DISMISS_HELP: 'You can dismiss onboarding and finish setup later from the admin settings.',
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
      RETRY: 'Retry',
      DISMISS: 'Skip for Now',
    },
    CONFIGURE_ANONYMOUS_ACCESS: {
      TITLE: 'Configure Anonymous Access',
      ENABLE_LABEL: 'Enable anonymous access',
      DISABLE_LABEL: 'Disable anonymous access',
      ENABLE_OPTION_DESCRIPTION: 'Allow anonymous users to read from repositories',
      DISABLE_OPTION_DESCRIPTION: 'Require authentication for all repository access',
      ENABLE_DESCRIPTION_TERM: 'Enable anonymous access',
      ENABLE_DESCRIPTION_DEFINITION_START:
        'By default, users can search, browse and download components from repositories without credentials. ',
      ENABLE_DESCRIPTION_DEFINITION_EMPHASIS:
        'Please consider the security implications for your organization.',
      DISABLE_DESCRIPTION_TERM: 'Disable anonymous access',
      DISABLE_DESCRIPTION_DEFINITION_START: 'Choose with care because it will ',
      DISABLE_DESCRIPTION_DEFINITION_EMPHASIS: 'require credentials',
      DISABLE_DESCRIPTION_DEFINITION_END: ' for all users and/or build tools.',
      MORE_INFO_LABEL: 'More information',
      MORE_INFO_HREF: 'https://links.sonatype.com/products/nexus/anonymous-access/docs',
    },
  },
};
