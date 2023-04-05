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
  LOGS: {
    MENU: {
      text: 'Logs',
      description: 'View the current log contents'
    },

    LIST: {
      FILTER_PLACEHOLDER: 'Filter by File Name',
      LOGGER_NAME_LABEL: 'Logger Name',
      FILE_NAME_LABEL: 'File Name',
      SIZE_LABEL: 'Size',
      LAST_MODIFIED_LABEL: 'Last Modified'
    },

    VIEW: {
      TITLE: (log) => `Viewing ${log}`,
      DOWNLOAD: 'Download',
      MARK: {
        LABEL: 'Marker to insert into log',
        INSERT: 'Insert'
      },
      REFRESH: {
        RATE_LABEL: 'Refresh Rate',
        SIZE_LABEL: 'Size'
      }
    },

    REFRESH: {
      MANUAL_ITEM: 'Manual',
      TWENTY_SECONDS_ITEM: 'Every 20 seconds',
      MINUTE_ITEM: 'Every minute',
      TWO_MINUTES_ITEM: 'Every 2 minutes',
      FIVE_MINUTES_ITEM: 'Every 5 minutes'
    },
    SIZE: {
      LAST25KB_ITEM: 'Last 25KB',
      LAST50KB_ITEM: 'Last 50KB',
      LAST100KB_ITEM: 'Last 100KB'
    }
  }
};