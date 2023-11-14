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
        text: 'Usage'
      },
      TOTAL_COMPONENTS: {
        title: 'Total components'
      },
      UNIQUE_LOGINS: {
        title: 'Unique logins',
        subTitle: 'Past 30 days'
      },
      PEAK_REQUESTS_PER_MINUTE: {
        title: 'Peak requests per minute',
        subTitle: 'Past 24 hours'
      },
      PEAK_REQUESTS_PER_DAY: {
        title: 'Peak requests per day',
        subTitle:'Past 30 days'
      },
      LEARN_ABOUT_PRO: {
        TEXT: 'Learn about Pro',
        URL: '/'
      },
      HARD_LIMIT_ALERT_CONTENT: {
        TOTAL_COMPONENTS:
          <>
            Users can not currently upload to this repository. This repository contains the maximum of 75,000
            components. Review your usage and consider removing unused components or consider upgrading to Pro for
            unlimited usage.
          </>,
        UNIQUE_LOGINS:
          <>
            Users can not currently upload to this repository. This repository has hit the maximum of 100 users
            login in the past 30 days. Review your usage and consider upgrading to Pro for unlimited usage.
          </>,
        REQUESTS_PER_DAY:
          <>
            Users can not currently upload to this repository. This repository has hit the maximum of 250,000 peak
            requests in the past 30 days. Review your usage and consider upgrading to Pro for unlimited usage.
          </>
      },
      SOFT_LIMIT_ALERT_CONTENT: {
        TOTAL_COMPONENTS:
          <>
            This repository is approaching the maximum of 75,000 components. Users will not be able to upload to this
            repository once this limit is reached. Review your usage and consider removing unused components or
            consider upgrading to Pro for unlimited usage.
          </>,
        UNIQUE_LOGINS:
          <>
            This repository is approaching the maximum of 100 users login in the past 30 days. Users will not be able
            to upload to this repository once this limit is reached. Review your usage and consider upgrading to Pro
            for unlimited usage.
          </>,
        REQUESTS_PER_DAY:
          <>
            This repository is approaching the maximum of 250,000 peak requests in the past 30 days. Users will not be
            able to upload to this repository once this limit is reached. Review your usage and consider upgrading to
            Pro for unlimited usage.
          </>
      }
    }
  }
};
