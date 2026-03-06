/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {NxTextLink} from "@sonatype/react-shared-components";
import React from "react";
import {faServer} from "@fortawesome/free-solid-svg-icons";

export default {
  HISTORICAL_USAGE: {
    MENU: 'Usage',
    MENU_DESCRIPTION: 'Monitor historical usage trends.',
    TITLE: 'Historical Usage',
    DESCRIPTION: <>Monitor your repository usage trends over time. <NxTextLink external
                                                                               href="http://links.sonatype.com/products/nxrm3/license/historical-usage">Learn
      how usage is calculated</NxTextLink></>,
    MONTH: 'Month',
    PEAK_COMPONENTS: 'Peak Components',
    COMPONENTS_CHANGE: 'Components % Change',
    COMPONENTS_CHANGE_TOOLTIP: 'Change rate of the peak component count from the previous month.',
    TOTAL_REQUESTS: 'Total Requests',
    REQUESTS_CHANGE: 'Requests % Change',
    REQUESTS_CHANGE_TOOLTIP: 'Change rate of the total monthly requests from the previous month.',
    PEAK_STORAGE: 'Peak Storage',
    STORAGE_CHANGE: '% Change Storage',
    STORAGE_CHANGE_TOOLTIP: 'Change rate of the total monthly storage from the previous month.',
    TOTAL_EGRESS: 'Total Egress',
    TOTAL_EGRESS_TOOLTIP: 'Egress is based on application-level tracking and may differ from actual network transfer measured by your cloud provider.',
    EGRESS_CHANGE: 'Egress % Change',
    EGRESS_CHANGE_TOOLTIP: 'Change rate of the total monthly egress from the previous month.',
    TOTAL_USAGE: 'Total Usage',
    TOTAL_USAGE_TOOLTIP: 'Combined total of egress and storage usage. This is your total usage value.',
    ICON: faServer,
    CHART: {
      LEGEND_EGRESS: 'Total Egress',
      LEGEND_STORAGE: 'Peak Storage',
      FILTER_OPTION_HEADER: 'Month',
      AXIS_DAYS: 'Days',
      AXIS_EGRESS_STORAGE: "Egress and Storage",
      TITLE: "Usage insights",
      SUB_TITLE: "Month"
    }
  }
};
