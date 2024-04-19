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
export const STARTER_THRESHOLD_REACHED = [
  {
    "metricName": "peak_requests_per_day",
    "metricValue": 200000,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 187500
      },
      {
        "thresholdName": "STARTER_THRESHOLD",
        "thresholdValue": 200000
      }
    ],
    "usageLevel": "STARTER_THRESHOLD"
  },
  {
    "metricName": "component_total_count",
    "metricValue": 120000,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 56250
      },
      {
        "thresholdName": "STARTER_THRESHOLD",
        "thresholdValue": 120000
      }
    ],
    "usageLevel": "STARTER_THRESHOLD"
  },
  {
    "metricName": "successful_last_24h",
    "metricValue": 26,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 75
      }
    ],
    "usageLevel": "FREE_TIER"
  }
];

export const SOFT_THRESHOLD_REACHED = [
  {
    "metricName": "peak_requests_per_day",
    "metricValue": 150000,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 20000
      },
      {
        "thresholdName": "STARTER_THRESHOLD",
        "thresholdValue": 200000
      }
    ],
    "usageLevel": "SOFT_THRESHOLD"
  },
  {
    "metricName": "component_total_count",
    "metricValue": 90000,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 100000
      },
      {
        "thresholdName": "STARTER_THRESHOLD",
        "thresholdValue": 120000
      }
    ],
    "usageLevel": "FREE_TIER"
  },
  {
    "metricName": "successful_last_24h",
    "metricValue": 26,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 75
      }
    ],
    "usageLevel": "FREE_TIER"
  }
];

export const NO_THRESHOLDS_DATA = [
  {
    "metricName": "component_total_count",
    "metricValue": 90000,
    "usageLevel": "FREE_TIER"
  },
]

export const NO_THRESHOLD_VALUE_DATA = [
  {
    "metricName": "peak_requests_per_day",
    "metricValue": 200000,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 187500
      },
      {
        "thresholdName": "STARTER_THRESHOLD",
      }
    ],
    "usageLevel": "STARTER_THRESHOLD"
  }
]

export const NO_THRESHOLD_NAME_DATA = [
  {
    "metricName": "peak_requests_per_day",
    "metricValue": 200000,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 187500
      },
      {
        "thresholdValue": 187500
      }
    ],
    "usageLevel": "STARTER_THRESHOLD"
  }
]

export const INVALID_THRESHOLD_VALUE_DATA = [
  {
    "metricName": "peak_requests_per_day",
    "metricValue": 200000,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 187500
      },
      {
        "thresholdName": "STARTER_THRESHOLD",
        "thresholdValue": "XYZ"
      }
    ],
    "usageLevel": "STARTER_THRESHOLD"
  }
]

export const NO_USAGE_LEVEL_DATA = [
  {
    "metricName": "peak_requests_per_day",
    "metricValue": 200000,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 187500
      },
      {
        "thresholdName": "STARTER_THRESHOLD",
        "thresholdValue": "XYZ"
      }
    ],
  }
]
