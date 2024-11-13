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

/**
 * @since 3.36
 *
 * Utilities for working with dates
 */

const ERROR_MESSAGE = value => `Unable to convert timestamp (${value}) to date string, returning null`;

export default class DateUtils {
  /**
   * @param {number} timestamp a numerical timestamp
   * @returns {string|null} a human-friendly date string
   */
  static timestampToString(timestamp) {
    try {
      return new Date(timestamp).toString();
    }
    catch (e) {
      console.debug(ERROR_MESSAGE(timestamp));
      return null;
    }
  }

  /**
   * @param {number} timestamp a numerical timestamp
   * @returns {string|null} a "ddd, MMM D, YYYY" date format string. For example "Mon, Oct 3, 2022".
   */
  static prettyDate(timestamp) {
    try {
      const options = {
        weekday: 'short',
        month: 'short',
        year: 'numeric',
        day: 'numeric',
      };

      return new Date(timestamp).toLocaleDateString("en-US", options);
    }
    catch (e) {
      console.debug(ERROR_MESSAGE(timestamp));
      return null;
    }
  }

  /**
   * @param {Date} date a Date
   * @returns {string|null} a human-friendly date time string
   */
  static prettyDateTime(date) {
    try {
      const options = {
        timeZoneName: 'longOffset',
        hour12: false,
      };
      return date.toLocaleString("en-US", options);
    }
    catch (e) {
      console.debug(ERROR_MESSAGE(date));
      return null;
    }
  }

  /**
   * @param {Date} date a Date
   * @returns {string|null} a human-friendly date time string with long format
   */
  static prettyDateTimeLong(date) {
    try {
      const options = {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
        timeZoneName: 'longOffset'
      };
      return date.toLocaleString("en-US", options);
    }
    catch (e) {
      console.debug(ERROR_MESSAGE(date));
      return null;
    }
  }
}
