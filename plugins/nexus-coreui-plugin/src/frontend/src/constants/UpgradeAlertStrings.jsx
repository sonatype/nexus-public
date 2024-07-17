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
  UPGRADE_ALERT: {
    PENDING: {
      LABEL: 'Upgrade Pending.',
      FINALIZE_BUTTON: 'Finalize Upgrade',
      STATUS_LABEL: 'Complete Nexus Repository version upgrade.',
      TEXT: 'New features and functionality will not be available until the upgrade process is complete.',
      OLDER_LABEL: 'Nexus Repository versioning is out of date.',
      YEAR_OLD_TEXT: <>You are currently running a Sonatype Nexus Repository version that is in extended maintenance.
        See our <NxTextLink external href="https://links.sonatype.com/products/sunsetting/docs/information">
        Sunsetting information</NxTextLink> for details about our product
        development lifecycle. Also see our <NxTextLink external href="https://links.sonatype.com/products/nexus/releasenotes">
        release notes</NxTextLink> to learn more about the newest Nexus Repository version available for your instance.</>,
      TEXT_18_MONTHS: <>You are currently running a sunsetted Sonatype Nexus Repository version.
        See our <NxTextLink external href="https://links.sonatype.com/products/sunsetting/docs/information">
        Sunsetting information</NxTextLink> for details about our product development
        lifecycle. Also see our <NxTextLink external href="https://links.sonatype.com/products/nexus/releasenotes">
        release notes</NxTextLink> to learn more about the newest Nexus Repository version available for your instance.</>
    },

    PROGRESS: {
      LABEL: 'Upgrade in progress'
    },

    WARN: {
      LABEL: 'Mixed Mode detected.',
      TEXT: 'Nexus Repository detected that 1 or more node(s) are running different Nexus Repository versions. ' +
        'If you are in the process of upgrading your nodes, you can ignore this alert. Otherwise, ensure all nodes are on the same version as soon as possible.'
    },

    ERROR: {
      LABEL: 'Upgrade Failed.',
      TEXT: 'error occurred while trying update the nodes in your cluster.',
      TEXT_MISMATCH: 'or more node(s) in your cluster is not running the same version of Nexus Repository.',
      CONTACT_SUPPORT: <><NxTextLink external href="https://links.sonatype.com/products/nexus/pro/support-request">
        Contact support</NxTextLink> for assistance.</>
    },

    COMPLETE: {
      LABEL: 'Upgrade Complete.',
      TEXT: 'Any new functionality included in your upgrade is now available for all nodes in your cluster.',
      DISMISS: 'Dismiss'
    },
  }
};
