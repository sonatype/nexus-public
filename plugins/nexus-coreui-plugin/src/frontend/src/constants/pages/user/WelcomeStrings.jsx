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
      BANNERS: {
        NEAR_LIMITS: 'This instance of Nexus Repository Community Edition is trending toward its usage limit. Once limits are reached, new components cannot be added.',
        OVER_LIMIT_IN_GRACE: (daysLeft, endDate) => <><strong>{daysLeft} Days Remaining</strong><br/>This instance of Nexus Repository Community Edition has exceeded its usage limit. Limits will be enforced starting {endDate}, when new components can no longer be added.</>,
        OVER_LIMIT_END_GRACE: 'This instance of Nexus Repository Community Edition has exceeded its usage limit. New components can no longer be added.',
        BELOW_LIMIT_END_GRACE: 'If this instance of Nexus Repository Community Edition exceeds usage limits, you will not be able to add new components.',
        THROTTLING_NON_ADMIN: <>This instance of Nexus Repository Community Edition has exceeded its usage limit. New components can no longer be added. Talk to your repository administrator.</>,
        NEARING_NON_ADMIN: <>This instance of Nexus Repository Community Edition is trending toward its usage limit.  Once limits are reached, new components cannot be added. Talk to your repository administrator.</>
      },
      MENU: {
        TITLE: 'Usage Center',
        SUB_TITLE: 'Usage Metrics Overview',
        SUB_TEXT: 'Monitor this instance\'s usage to ensure your deployment is appropriate for your needs.'
      },
      HEADER: {
        BUTTONS: {
          LEARN_MORE: 'Learn More',
          RESTORE_USAGE: 'How to Restore Usage',
          PURCHASE_NOW: 'Purchase Now'
        },
        OVER_LIMITS: {
          STATUS_INDICATOR: 'Usage over limits',
          WARNING: (endDate) => <>Usage limits came into effect on {endDate}. As usage levels are currently higher than the Nexus Repository Community Edition maximum, new components can no longer be added to this instance.</>,
          TITLE: 'Usage Limits In Effect'
        },
        APPROACHING_LIMITS: {
          STATUS_INDICATOR: 'Usage nearing limits',
          WARNING: 'Once limits are reached, new components cannot be added.',
          TITLE: 'Instance Trending Toward Usage Limits'
        },
        UNDER_LIMITS: {
          STATUS_INDICATOR: 'Usage below limits',
          TITLE: 'Instance Trending Toward Usage Limits',
          WARNING: 'If you exceed usage limits, you will not be able to add new components.'
        },
        PRO_POSTGRES: {
          TEXT: 'Monitor this instance\'s usage to optimize your deployments.'
        },
        GRACE_PERIOD: {
          OVER_WARNING: (endDate) => <>Starting {endDate}, new components cannot be added.</>,
          UNDER_WARNING: (endDate) => <>Usage limits take effect on {endDate}. When the usage exceeds the Nexus Repository Community Edition maximum, new components can no longer be added to this instance.</>,
          TITLE: (endDate) => <>Usage Limits Will Be Enforced Starting {endDate}</>
        }
      },
      CARDS: {
        TOTAL_COMPONENTS: {
          TITLE: 'Total Components',
          SUB_TITLE: 'Current',
          HIGHEST_RECORDED_COUNT: 'Highest Recorded Count (30 days)',
          METRIC_NAME: 'component_total_count',
          METRIC_NAME_PRO_POSTGRESQL: 'component_total_count',
          AGGREGATE_PERIOD_30_D: 'peak_recorded_count_30d',
          TOOLTIP_PRO: 'Sonatype Nexus Repository Pro using an embedded database performs best when your total component counts remain under the threshold. If you are exceeding the threshold, we strongly recommend migrating to a PostgreSQL database.',
          TOOLTIP_CE: 'Community Edition tracks the total components stored in this instance. If usage exceeds the 100,000 component limit, the date will be displayed, and write restrictions will apply until usage is reduced.'
        },
        UNIQUE_LOGINS: {
          TITLE: 'Unique Logins',
          SUB_TITLE: 'Last 24 hours',
          HIGHEST_RECORDED_COUNT: 'Last 30 days',
          METRIC_NAME: 'successful_last_24h',
          AGGREGATE_PERIOD_30_D: 'peak_recorded_count_30d',
          TOOLTIP: 'Measures unique users who login over a period of time.',
          TOOLTIP_CE: 'Unique successful logins to this Sonatype Nexus Repository instance in the last 30 days.'
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
          TOOLTIP_PRO: 'Sonatype Nexus Repository Pro using an embedded database performs best when your requests per day remain under the threshold. If you are exceeding the threshold, we strongly recommend migrating to a PostgreSQL database.',
          TOOLTIP_CE: 'Community Edition tracks the total daily requests to this instance. If usage exceeds the 200,000 request limit, the date will be displayed, and write restrictions will apply until usage is reduced.'
        },
        CARD_PRO_LABELS: {
          THRESHOLD: 'Threshold',
          THRESHOLD_NAME: 'thresholdName',
          THRESHOLD_VALUE: 'thresholdValue',
        },
        CARD_CE_LABELS: {
          USAGE_LIMIT: 'Usage Limit',
        },
        CARD_SHARED_LABELS: {
          PERIOD: 'period',
          VALUE: 'value',
          LAST_EXCEEDED_DATE_LABEL: 'Last time over the usage limit'
        },
        PERCENTAGE: 0.75,
        SOFT_THRESHOLD: 'SOFT_THRESHOLD',
        HARD_THRESHOLD: 'HARD_THRESHOLD',
        PRO: 'PRO',
        COMMUNITY: 'COMMUNITY'
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
