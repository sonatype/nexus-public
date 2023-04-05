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
const NAME_REGEX = /^[a-zA-Z0-9\-]{1}[a-zA-Z0-9_\-\.]*$/;
const URI_REGEX = /^[a-z]*:.+$/i;
const SECURE_URL_REGEX = /^(https|ldaps):\/\/[^"<>^`{|}]+$/i;
const URL_HOSTNAME_REGEX = /^(([a-z0-9]|[a-z0-9][a-z0-9\-]*[a-z0-9])\.)*([a-z0-9]|[a-z0-9][a-z0-9\-]*[a-z0-9])$/i;
const URL_PATHNAME_REGEX = /^([\S]*\S)?$/i;
const RFC_1123_HOST_REGEX = new RegExp(
    "^(((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))|" +
    "(\\[(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\])|" +
    "(\\[((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)::((?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?)\\])|" +
    "(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|" +
    "[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9]))(:([0-9]+))?$"
);

/**
 * @since 3.31
 */
export default class ValidationUtils {
  /**
   * @param str {string|null|undefined}
   * @return {boolean} true if the string is blank, null, or undefined
   */
  static isBlank(str) {
    return (!str || /^\s*$/.test(str));
  }

  /**
   * @param str {string|null|undefined}
   * @return {boolean} true if the string is not blank, null, or undefined
   */
  static notBlank(str) {
    return !ValidationUtils.isBlank(str);
  }

  /**
   * @param str {string|null|undefined}
   * @return {boolean} true if the string appears to be a valid uri
   */
  static isUri(str) {
    return str && URI_REGEX.test(str);
  }

  /**
   * @param str {string}
   * @return {boolean} true if the string does not appear to be a valid uri
   */
  static notUri(str) {
    return !ValidationUtils.isUri(str);
  }

  /**
   * @param str {string|null|undefined}
   * @returns {boolean} true if the string appears to be a valid url (http/https)
   */
  static isUrl(str) {
    let url;

    try {
      url = new URL(str);
    } catch (_) {
      return false;
    }

    // Need to extract hostname manually because URL object has encoded hostname
    // that cannot be decoded to original form.
    // For exampel 'http://fo£o.bar' encodes to 'xn--foo-cea.bar'.
    const matches = str.match(/^https?:\/\/([^:/?#]+)/i);
    const hostname = matches && matches[1];

    // Port is checked by URL function itself, except 0.
    const {protocol, pathname, port} = url;

    const isProtocolValid = protocol === 'http:' || protocol === 'https:';

    const isHostnameValid = hostname && URL_HOSTNAME_REGEX.test(hostname);

    const isPortValid = port !== '0';

    const isPathnameValid = URL_PATHNAME_REGEX.test(decodeURIComponent(pathname));

    return isProtocolValid && isHostnameValid && isPortValid && isPathnameValid;
  }

  /**
   * @param str {string|null|undefined}
   * @returns {boolean} true if the string does not appear to be a valid url (http/https)
   */
  static notUrl(str) {
    return !ValidationUtils.isUrl(str);
  }

  /**
   * @param str {string|null|undefined}
   * @returns {boolean} true if the str is an https url
   */
  static isSecureUrl(str) {
    return str && SECURE_URL_REGEX.test(str);
  }

  /**
   * @param str {string|null|undefined}
   * @returns {boolean} true if the string does not appear to be a valid secure url (https)
   */
  static notSecureUrl(str) {
    return !ValidationUtils.isSecureUrl(str);
  }

  /**
   * @param value {string|null|undefined}
   * @returns {boolean} true if the value is a valid hostname
   */
  static isHost(value) {
    return value && RFC_1123_HOST_REGEX.test(value);
  }

  /**
   * @param value {string|number|null|undefined}
   * @param min - minimum allowed value, defaults to -Infinity
   * @param max - maximum allowed value, defaults to Infinity
   * @return {string|null} a string error message if the number falls outside of the range; null if the string is blank or within the boundaries
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

  /**
   * @param value {string}
   * @returns {boolean} true if the value has white spaces.
   */
  static hasWhiteSpace (value){
    return  /\s/g.test(value);
  }

  static validateLength(value, length) {
    if (value.length > length) {
      return UIStrings.ERROR.MAX_CHARS(length);
    }
    return null;
  }

  /**
   * @param value
   * @returns {string|null} an error string if name does not meet the requirements
   */
  static validateNameField(value) {
    return this.validateNotBlank(value) || this.validateLength(value, 255) || this.validateName(value);
  }

  /**
   * @param value
   * @returns {string|null} an error string if name fails {@link ValidationUtils.isName} or null
   */
  static validateName(value) {
    if (!this.isName(value)) {
      return UIStrings.ERROR.INVALID_NAME_CHARS;
    }
    return null;
  }

  static validateNotBlank(value) {
    if (ValidationUtils.isBlank(value)) {
      return UIStrings.ERROR.FIELD_REQUIRED;
    }
  }

  static validateHost(value) {
    if (!ValidationUtils.isHost(value)) {
      return UIStrings.ERROR.HOSTNAME;
    }
  }

  static validateIsUri(value) {
    if (ValidationUtils.notUri(value)) {
      return UIStrings.ERROR.INVALID_URI;
    }
  }

  static validateIsUrl(value) {
    if (ValidationUtils.notUrl(value)) {
      return UIStrings.ERROR.URL_ERROR;
    }
  }

  static validateLeadingOrTrailingSpace(value) {
    if (/^\s/.test(value) || /\s$/.test(value)) {
      return UIStrings.ERROR.TRIM_ERROR;
    }
  }


  static validateWhiteSpace(value) {
    if(ValidationUtils.hasWhiteSpace(value)) {
      return UIStrings.ERROR.WHITE_SPACE_ERROR;
    }
  }

  /**
   * Match the regex from components/nexus-validation/src/main/java/org/sonatype/nexus/validation/constraint/NamePatternConstants.java
   * @param {string}
   * @return {boolean} true if the string is a valid name
   */
  static isName(name) {
    return name?.match(NAME_REGEX);
  }

  /**
   * @param {string|null|undefined}
   * @return {boolean} true if the string appears to be a valid email address
   */
  static isEmail(str) {
    return str && EMAIL_REGEX.test(str);
  }

  static validateEmail(value) {
    if (!ValidationUtils.isEmail(value)) {
      return UIStrings.ERROR.INVALID_EMAIL;
    }
  }

  /**
   * Checks if the errors object returned contains any error messages
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

  /**
   * Checks if two passwords match
   * @param password
   * @param passwordConfirmation
   * @returns {string|null} UIStrings.ERROR.PASSWORD_NO_MATCH_ERROR if the two values differ
   */
  static validatePasswordsMatch(password, passwordConfirmation) {
    if (password !== passwordConfirmation) {
      return UIStrings.ERROR.PASSWORD_NO_MATCH_ERROR;
    }
  }
}
