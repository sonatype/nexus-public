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
      CIRCUIT_B: {
        TOTAL_COMPONENTS: {
          TITLE: 'Total Components',
          SUB_TITLE: 'Current',
          HIGHEST_RECORDED_COUNT: 'Highest Recorded Count (30 days)',
          METRIC_NAME: 'component_total_count',
          METRIC_NAME_PRO_POSTGRESQL: 'component_total_count',
          AGGREGATE_PERIOD_30_D: 'peak_recorded_count_30d',
          TOOLTIP: (edition) => {
            if (edition === 'Starter_Edition') {
              return NX.I18n.get('Total_Components_Tooltip');
            } else if (edition === 'PRO') {
              return 'Sonatype Nexus Repository Pro using an embedded database performs best when your total component counts remain under the threshold. If you are exceeding the threshold, we strongly recommend migrating to a PostgreSQL database.'
            } else {
              return 'Sonatype Nexus Repository OSS performs best when your total component counts remain under {} components across all repositories in your instance.'
            }
          }
        },
        UNIQUE_LOGINS: {
          TITLE: 'Unique Logins',
          SUB_TITLE: 'Last 24 hours',
          HIGHEST_RECORDED_COUNT: 'Last 30 days',
          METRIC_NAME: 'successful_last_24h',
          AGGREGATE_PERIOD_30_D: 'peak_recorded_count_30d',
          TOOLTIP: 'Measures unique users who login over a period of time.',
          TOOLTIP_STARTER: 'Unique successful logins to this Sonatype Nexus Repository instance in the last 30 days.'
        },
        REQUESTS_PER_MINUTE: {
          TITLE: 'Requests Per Minute',
          TITLE_PRO_POSTGRESQL: 'Peak Requests Per Minute',
          SUB_TITLE: 'Peak minute in last 24 hours',
          SUB_TITLE_PRO_POSTGRESQL: 'Past 24 hours',
          HIGHEST_RECORDED_COUNT: 'Peak minute in last 30 days',
          METRIC_NAME: 'requests_per_minute',
          METRIC_NAME_PRO_POSTGRESQL: 'requests_per_minute',
          AGGREGATE_PERIOD_24_H: 'last_24h',
          AGGREGATE_PERIOD_30_D: 'last_30d',
          TOOLTIP_PRO: 'Measures requests per minute to repository endpoints for all repositories in your Sonatype Nexus Repository Pro instance.'
        },
        REQUESTS_PER_DAY: {
          TITLE: 'Requests Per Day',
          TITLE_PRO_POSTGRESQL: 'Peak Requests Per Day',
          SUB_TITLE: 'Last 24 hours',
          SUB_TITLE_PRO_POSTGRESQL: 'Past 30 days',
          HIGHEST_RECORDED_COUNT: 'Highest Recorded Count (30 days)',
          METRIC_NAME: 'peak_requests_per_day',
          METRIC_NAME_PRO_POSTGRESQL: 'peak_requests_per_day_30d',
          AGGREGATE_PERIOD_30_D: 'peak_recorded_count_30d',
          TOOLTIP: (edition) => {
            if (edition === 'Starter_Edition') {
              return NX.I18n.get('Requests_Per_Day_Tooltip')
            } else if (edition === 'PRO') {
              return 'Sonatype Nexus Repository Pro using an embedded database performs best when your requests per day remain under the threshold. If you are exceeding the threshold, we strongly recommend migrating to a PostgreSQL database.'
            } else {
              return `Sonatype Nexus Repository OSS performs best when requests per day remain under {} requests per day to all repository endpoints across all repositories in your instance.`
            }
          }
        },
        CARD_SHARED_LABELS: {
          THRESHOLD: 'Threshold',
          THRESHOLD_NAME: 'thresholdName',
          THRESHOLD_VALUE: 'thresholdValue',
          PERIOD: 'period',
          VALUE: 'value',
        },
        PERCENTAGE: 0.75,
        SOFT_THRESHOLD: 'SOFT_THRESHOLD',
        STARTER_THRESHOLD: 'STARTER_THRESHOLD',
        PRO: 'PRO',
        OSS: 'OSS',
        STARTER: 'Starter_Edition',
      },
      CARD_LINK_OSS: {
        TEXT: 'Understand your usage',
        URL: 'https://links.sonatype.com/products/nxrm3/docs/optimize-performance-free'
      },
      CARD_LINK_PRO: {
        TEXT: 'Understand your usage',
        URL: 'https://links.sonatype.com/products/nxrm3/docs/optimize-performance-pro'
      },
      CARD_LINK_STARTER: {
        TEXT: 'Understand your usage',
        URL: 'https://links.sonatype.com/products/nxrm3/docs/review-usage'
      },
      ALERTS: {
        EXCEEDING_THRESHOLDS: {
          REQUESTS_PER_DAY: {
            PREFIX: 'Exceeding_Threshold_Requests_Per_Day_Prefix',
            MID: 'Exceeding_Threshold_Requests_Per_Day_Mid'
          },
          TOTAL_COMPONENTS: {
            PREFIX: 'Exceeding_Threshold_Total_Components_Prefix',
            MID: 'Exceeding_Threshold_Total_Components_Mid'
          }
        },
        APPROACHING_THRESHOLDS: {
          REQUESTS_PER_DAY: {
            PREFIX: 'Approaching_Threshold_Requests_Per_Day_Prefix',
            MID: 'Approaching_Threshold_Requests_Per_Day_Mid'
          },
          TOTAL_COMPONENTS: {
            PREFIX: 'Approaching_Threshold_Total_Components_Prefix',
            MID: 'Approaching_Threshold_Total_Components_Mid'
          }
        },
        SUFFIX: 'Suffix',
        LEARN_ABOUT_PRO: {
          TEXT: 'Learn about Pro',
          URL: 'https://links.sonatype.com/products/nxrm3/docs/learn-about-pro'
        },
        REVIEW_YOUR_USAGE: {
          TEXT: 'Review your usage',
          URL: 'https://links.sonatype.com/products/nxrm3/docs/review-usage'
        },
        UPGRADING_PRO: {
          TEXT: 'upgrading to Pro',
          URL: 'https://links.sonatype.com/products/nxrm3/docs/upgrade-to-pro'
        }
      }
    }
  }
};
