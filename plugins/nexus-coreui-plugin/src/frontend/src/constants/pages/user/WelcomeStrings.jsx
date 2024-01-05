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

export default {
  WELCOME: {
    MENU: {
      text: 'Welcome'
    },
    ACTIONS: {
      SYSTEM_HEALTH: {
        title: 'System Health',
        subTitle: 'View system status checks',
      },
      CLEANUP_POLICIES: {
        title: 'Cleanup Policies',
        subTitle: 'Review component removal policies',
      },
      BROWSE: {
        title: 'Browse',
        subTitle: 'Browse my repositories',
      },
      SEARCH: {
        title: 'Search',
        subTitle: 'Search for new components',
      },
      RELEASE_NOTES: {
        title: 'Release Notes',
        subTitle: 'See what\'s new',
      },
      DOCUMENTATION: {
        title: 'Documentation',
        subTitle: 'Visit our help site',
      },
      COMMUNITY: {
        title: 'Community',
        subTitle: 'Ask and answer questions',
      },
      CONNECT: {
        title: 'Connect',
        subTitle: 'Connect to a repository',
      },
    },
    CONNECT_MODAL: {
      TITLE: 'Obtaining a Repository URL',
      FIRST_STEP_TEXT: <>In the <em>Repositories</em> table, select the <em>Copy</em> button for the repository you wish to connect.</>,
      SECOND_STEP_TEXT: 'A modal with the repository\'s direct URL will appear; copy this URL to use as needed. The modal also contains a link to our help documentation to get more information on how to connect to your repository.'
    },
    FIREWALL_ALERT_CONTENT: (
        <>
          You haven't configured your repository protection. Sonatype Repository Firewall combines
          over 60 different signals used to identify potentially malicious activity and block risks before
          download. Automatically block malware from entering your code repositories.
        </>
    ),
    FIREWALL_ENABLE_BUTTON_CONTENT : 'Enable Capability',
    USAGE: {
      MENU: {
        TEXT: 'Usage'
      },
      TOTAL_COMPONENTS: {
        TITLE: 'Total components'
      },
      UNIQUE_LOGINS: {
        TITLE: 'Unique logins',
        SUB_TITLE: 'Past 30 days'
      },
      PEAK_REQUESTS_PER_MINUTE: {
        TITLE: 'Peak requests per minute',
        SUB_TITLE: 'Past 24 hours'
      },
      PEAK_REQUESTS_PER_DAY: {
        TITLE: 'Peak requests per day',
        SUB_TITLE: 'Past 30 days'
      },
      CIRCUIT_BREAKER: {
        TOTAL_COMPONENTS: {
          TITLE: 'Total Components',
          SUB_TITLE: 'Current',
          LIMIT: 'Limit',
          HIGHEST_RECORDED_COUNT: 'Highest Recorded Count (30 days)',
          METRIC_NAME: 'component_total_count',
          METRIC_NAME_PRO: 'component_total_count',
          AGGREGATE_NAME: 'component_total_count',
          TOOLTIP: 'The free version of Sonatype Nexus Repository includes up to 75,000 components across all repositories.'
        },
        UNIQUE_LOGINS: {
          TITLE: 'Unique Logins',
          SUB_TITLE: 'Current',
          LIMIT: 'Limit per 30 days',
          HIGHEST_RECORDED_COUNT: 'Highest Recorded Count (30 days)',
          METRIC_NAME: 'successful_last_24h',
          AGGREGATE_NAME: 'unique_user_count',
          TOOLTIP: 'The free version of Sonatype Nexus Repository includes up to 100 unique authentications per 30 days.'
        },
        REQUESTS_PER_DAY: {
          TITLE: 'Requests Per Day',
          SUB_TITLE: 'Current',
          LIMIT: 'Limit per 24 hours',
          HIGHEST_RECORDED_COUNT: 'Highest Recorded Count (30 days)',
          METRIC_NAME: 'peak_requests_per_day',
          METRIC_NAME_PRO: 'peak_requests_per_day_30d',
          AGGREGATE_NAME: 'content_request_count',
          TOOLTIP: 'The free version of Sonatype Nexus Repository includes up to 250,000 HTTP requests to repository endpoints per day.'
        }
      },
      UPGRADE_TO_PRO: {
        TEXT: 'Upgrade to Pro to remove limits',
        URL: '/'
      }
    }
  }
};
