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
export const USAGE_CENTER_CONTENT_CE = [
  {
    "metricName": "peak_requests_per_day",
    "metricValue": 3300,
    "thresholds": [
      {
        "thresholdName": "HARD_THRESHOLD",
        "thresholdValue": 200000
      }
    ],
    "utilization": "FREE_TIER",
    "aggregates": [
      {
        "name": "content_request_count",
        "value": 75000,
        "period": "peak_recorded_count_30d"
      }
    ]
  },
  {
    "metricName": "component_total_count",
    "metricValue": 85000,
    "thresholds": [
      {
        "thresholdName": "HARD_THRESHOLD",
        "thresholdValue": 100000
      }
    ],
    "utilization": "FREE_TIER",
    "aggregates": [
      {
        "name": "component_total_count",
        "value": 12500,
        "period": "peak_recorded_count_30d"
      }
    ]
  },
  {
    "metricName": "successful_last_24h",
    "metricValue": 26,
    "thresholds": [
      {
        "thresholdName": "SOFT_THRESHOLD",
        "thresholdValue": 100
      }
    ],
    "utilization": "FREE_TIER",
    "aggregates": [
      {
        "name": "unique_user_count",
        "value": 52,
        "period": "peak_recorded_count_30d"
      }
    ]
  }
];

export const USAGE_CENTER_CONTENT_PRO = [
  {
    "metricName": "peak_requests_per_day_30d",
    "metricValue": 145302,
    "thresholds": [],
    "utilization": "FULL",
    "aggregates": []
  },
  {
    "metricName": "requests_per_minute",
    "metricValue": 1236,
    "aggregates": []
  },
  {
    "metricName": "component_total_count",
    "metricValue": 4758,
    "thresholds": [],
    "utilization": "FULL",
    "aggregates": []
  }
];
