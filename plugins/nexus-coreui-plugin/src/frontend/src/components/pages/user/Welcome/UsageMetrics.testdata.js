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
export const SOFT_LIMIT_REACHED = [
  {
    "metricName": "peak_requests_per_day",
    "metricValue": 36300,
    "limits": [
      {
        "limitName": "HARD_LIMIT",
        "limitValue": 250000
      },
      {
        "limitName": "SOFT_LIMIT",
        "limitValue": 187500
      }
    ],
    "limitLevel": "FREE_TIER"
  },
  {
    "metricName": "peak_requests_per_minute_1d",
    "metricValue": 0,
    "limits": [],
    "limitLevel": "FREE_TIER"
  },
  {
    "metricName": "component_total_count",
    "metricValue": 75001,
    "limits": [
      {
        "limitName": "HARD_LIMIT",
        "limitValue": 75000
      },
      {
        "limitName": "SOFT_LIMIT",
        "limitValue": 56250
      }
    ],
    "limitLevel": "SOFT_LIMIT"
  },
  {
    "metricName": "successful_last_30d",
    "metricValue": 77,
    "limits": [
      {
        "limitName": "HARD_LIMIT",
        "limitValue": 100
      },
      {
        "limitName": "SOFT_LIMIT",
        "limitValue": 75
      }
    ],
    "limitLevel": "FREE_TIER"
  }
];

