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
    },
    VERIFICATION_FAILED: {
      TITLE: 'Verification failed',
      STILL_REQUIRED_MESSAGE:
        'Some onboarding steps still need to be completed. Please re-finalize your choices.',
      NETWORK_ERROR_MESSAGE: 'A network error occurred. Please try again.',
      GENERIC_MESSAGE: 'Setup could not be verified. Please try again.',
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
    MANDATORY_TELEMETRY_ACKNOWLEDGEMENT: {
      TITLE: 'Required base telemetry',
      DESCRIPTION:
        'Once a day, Nexus Repository uploads a limited set of usage and product health data to help Sonatype provide better support, improve product quality, and review license usage. Nexus Repository records the outcome of each upload attempt.',
      LEARN_MORE_LABEL: 'Learn more about required base telemetry',
      LEARN_MORE_HREF: 'https://links.sonatype.com/products/nxrm3/nexus-telemetry',
      // The grace period differs per edition, so the day count comes from the backend
      // (`nexus.telemetry.health.gracePeriodDays`) and is substituted at render time.
      READ_ONLY_WARNING:
        'If telemetry cannot be sent for {gracePeriodDays} consecutive days, the instance will enter read-only mode until connectivity is restored or an exemption is approved. If your environment cannot send telemetry, please contact Sonatype Support.',
      // Fallback for when the backend has not reported a grace period length.
      READ_ONLY_WARNING_WITHOUT_GRACE_PERIOD:
        'If telemetry cannot be sent for an extended period, the instance will enter read-only mode until connectivity is restored or an exemption is approved.',
      CHECKBOX_LABEL: 'I understand this requirement and its impact.',
      SUCCESS_MESSAGE: 'Acknowledgement recorded',
      SUBMISSION_ERROR: 'Failed to submit your acknowledgement. Please try again.',
      // Baseline Telemetry status: Active, Never received, or Upload issue. The states
      // `TelemetryHealthCheck` reports that can apply here, reworded for a first-run audience --
      // the health check writes for an operator reading a diagnostic list, this modal writes for an
      // administrator being told about the requirement.
      STATUS: {
        // `{lastSuccessfulUpload}` is the formatted `nexus.telemetry.health.lastSuccessfulUpload`
        // timestamp, or NEVER_LABEL when the instance has no successful upload on record.
        HEADLINE: 'Last received: {lastSuccessfulUpload}',
        NEVER_LABEL: 'Never',
        // ACTIVE has no body: the headline date is the whole message.
        NEVER_BODY:
          "We haven't received telemetry from this instance yet. Check Repository Health Check, then review the Baseline Telemetry status again in 24 hours.",
        // Covers a running grace period and an exhausted one alike -- read-only mode is the same
        // upload failure further along. `{gracePeriodDays}` comes from the backend because the
        // grace period is edition-dependent.
        FAILING_BODY:
          'Recent telemetry uploads have failed. A {gracePeriodDays}-day grace period starts today to resolve the Baseline Telemetry issue. Check Repository Health Check again in 24 hours to confirm the status.',
        // Fallback for when the backend has not reported a grace period length.
        FAILING_BODY_WITHOUT_GRACE_PERIOD:
          'Recent telemetry uploads have failed. A grace period starts today to resolve the Baseline Telemetry issue. Check Repository Health Check again in 24 hours to confirm the status.',
        // Latest client error (HTTP 4XX), signature verification failure or connection error, from
        // `nexus.telemetry.health.lastError`.
        // Server errors (HTTP 5XX) are never reported. LAST_ERROR_WITHOUT_MESSAGE is used when the
        // response carried no message.
        LAST_ERROR: 'Last upload error: HTTP {httpStatusCode}: {message}',
        LAST_ERROR_WITHOUT_MESSAGE: 'Last upload error: HTTP {httpStatusCode}',
        // A failure recorded without a status code: the backend sends the default message for its type.
        LAST_ERROR_WITHOUT_STATUS_CODE: 'Last upload error: {message}',
      },
    },
  },
};
