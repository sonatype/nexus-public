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

const HELPER_LINK = {
  TEXT: 'base telemetry',
  HREF: 'https://links.sonatype.com/products/nxrm3/nexus-telemetry',
}

const SHARED = {
  GENERAL_RECOMMENDATION: 'Check your network configuration and verify that the server can reach the telemetry service. If the problem persists, contact Sonatype Support.',
  RETRY_RECOMMENDATION: 'To retry the upload now, run the Upload Retry task.',
  RETRY_LINK: 'Go to Tasks.',
  HELPER_LINK,
}

export default {
  TELEMETRY: {
    INTRODUCED_WARNING_BANNER: {
      TITLE: 'Baseline Telemetry Upload Issue',
      MESSAGE: 'This instance of Nexus Repository has been unable to upload required base telemetry data for the past {failedReportDays} days. This is a warning only. Your instance will continue to operate normally.',
      GENERAL_RECOMMENDATION: 'Check that your server can reach the Sonatype telemetry service, then run the Upload Retry task.',
      RETRY_RECOMMENDATION: 'If your environment cannot send telemetry or the issue persists, contact Sonatype Support.',
      RETRY_LINK: 'Go to Tasks.',
      HELPER_LINK,
    },
    WARNING_BANNER: {
      TITLE: 'Baseline Telemetry Upload Issue',
      MESSAGE: 'Nexus Repository has been unable to upload base telemetry data. If telemetry uploads cannot be restored, Nexus Repository will enter **read-only mode in {remainingGracePeriodDays} day(s)**.',
      ...SHARED,
    },
    READONLY_BANNER: {
      TITLE: 'Read-Only Mode Enabled',
      MESSAGE: 'Nexus Repository is now in read-only mode because base telemetry data could not be uploaded within the grace period.',
      ...SHARED,
    },
  },
}
