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
import UIStrings from "../constants/UIStrings";


const EMAIL_REGEX = /^(")?(?:[^\."])(?:(?:[\.])?(?:[\w\-!#$%&'*+/=?^_`{|}~]))*\1@(\w[\-\w]*\.?){1,5}([A-Za-z]){1,60}$/;

/**
 * @since 3.next
 */
export default class ValidationUtils {
  /**
   * @param {string|null|undefined}
   * @return {boolean} true if the string is blank, null, or undefined
   */
  static isBlank(str) {
    return (!str || /^\s*$/.test(str));
  }

  /**
   * @param {string|null|undefined}
   * @return {boolean} true if the string is not blank, null, or undefined
   */
  static notBlank(str) {
    return !ValidationUtils.isBlank(str);
  }

  /**
   * @param {string|null|undefined}
   * @return {boolean} true if the string appears to be a valid uri
   */
  static isUri(str) {
    return str && /^[a-z]*:.+$/i.test(str);
  }

  /**
   * @param {string}
   * @return {boolean} true if the string does not appear to be a valid uri
   */
  static notUri(str) {
    return !ValidationUtils.isUri(str);
  }

  /**
   * @param {str|null|undefined}
   * @returns {boolean} true if the string appears to be a valid url (http/https)
   */
  static isUrl(str) {
    return str && /^https?:\/\/[^"<>^`{|}]+$/i.test(str);
  }

  /**
   * @param {str|null|undefined}
   * @returns {boolean} true if the string does not appear to be a valid url (http/https)
   */
  static notUrl(str) {
    return !ValidationUtils.isUrl(str);
  }

  /**
   * @param value {string|number|null|undefined}
   * @param min - defaults to -Infinity
   * @param max - defaults to Infinity
   * @return null if the string is null, undefined, or blank
   * @return {string|null} a string error message if the number falls outside of the provided boundaries, null otherwise
   */
  static isInRange({value, min = -Infinity, max = Infinity, allowDecimals = true}) {
    if (value === null || value === undefined) {
      return null;
    }

    if (typeof value === 'string' && this.isBlank(value)) {
      return null;
    }

    const number = Number(value);
    if (isNaN(number)) {
      return UIStrings.ERROR.NAN
    }

    if (!allowDecimals && typeof value === 'string' && !/^-?[0-9]*$/.test(value)) {
      return UIStrings.ERROR.DECIMAL;
    }

    if (min > number) {
      return UIStrings.ERROR.MIN(min);
    }
    else if (max < number) {
      return UIStrings.ERROR.MAX(max);
    }
    else {
      return null;
    }
  }

  static validateNotBlank(value) {
    if (ValidationUtils.isBlank(value)) {
      return UIStrings.ERROR.FIELD_REQUIRED;
    }
  }

  static validateIsUrl(value) {
    if (ValidationUtils.notUrl(value)) {
      return UIStrings.ERROR.URL_ERROR;
    }
  }

  /**
   * Match the regex from components/nexus-validation/src/main/java/org/sonatype/nexus/validation/constraint/NamePatternConstants.java
   * @param {string}
   * @return {boolean} true if the string is a valid name
   */
  static isName(name) {
    return name.match(/^[a-zA-Z0-9\-]{1}[a-zA-Z0-9_\-\.]*$/);
  }

  /**
   * @param {string|null|undefined}
   * @return {boolean} true if the string appears to be a valid email address
   */
  static isEmail(str) {
    return str && EMAIL_REGEX.test(str);
  }

  static validateEmail(field) {
    if (ValidationUtils.isBlank(field)) {
      return UIStrings.ERROR.FIELD_REQUIRED;
    }
    else if (!ValidationUtils.isEmail(field)) {
      return UIStrings.ERROR.INVALID_EMAIL;
    }
    return null;
  }

  /**
   * Check if the errors object returned contains any error messages
   * @param errors {Object | null | undefined}
   * @return {boolean} true if there are any error messages
   */
  static isInvalid(errors) {
    if (errors === null || errors === undefined) {
      return false;
    }

    return Boolean(Object.values(errors).find(error => {
      if (error === null || error === undefined) {
        return false;
      }
      else if (error.length > 0) {
        return true;
      }
      else {
        return this.isInvalid(error);
      }
    }));
  }
}
