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
 * @since 3.21
 */
export default {
  SETTINGS: {
    CANCEL_BUTTON_LABEL: 'Cancel',
    DISCARD_BUTTON_LABEL: 'Discard',
    SAVE_BUTTON_LABEL: 'Save',
    DELETE_BUTTON_LABEL: 'Delete'
  },

  SAVING: 'Saving...',
  FILTER: 'Filter',
  CLOSE: 'Close',

  PRISTINE_TOOLTIP: 'There are no changes',
  INVALID_TOOLTIP: 'Validation errors are present',

  ERROR: {
    DECIMAL: 'This field must not contain decimal values',
    FIELD_REQUIRED: 'This field is required',
    NAN: 'This field must contain a numeric value',
    MIN: (min) => `The minimum value for this field is ${min}`,
    MAX: (max) => `The maximum value for this field is ${max}`,
    LOAD_ERROR: 'An error occurred while loading the form',
    SAVE_ERROR: 'An error occurred while saving the form',
    INVALID_NAME_CHARS: 'Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed and may not start with underscore or dot.',
    MAX_CHARS: (max) => `This field has a limit of ${max} characters`,
    INVALID_URI: 'URI should be in the format <protocol>:<path>',
    INVALID_EMAIL: 'This field should be an e-mail address in the format "user@example.com"',
    URL_ERROR: 'URL should be in the format "http://www.example.com"',
    URL_NEEDS_TRAILING_SLASH: 'URL should be in the format "http://www.example.com/" and must include a trailing slash'
  },

  SAVE_SUCCESS: 'The form was saved successfully',

  USER_TOKEN: {
    MESSAGES: {
      ACCESS_ERROR: 'You must authenticate successfully to access your token',
      RESET_SUCCESS: 'Your user token has been reset',
      RESET_ERROR: 'You must authenticate successfully to reset your token'
    }
  },

  MULTI_SELECT: {
    FROM_LABEL: 'Available',
    TO_LABEL: 'Selected',
    MOVE_RIGHT: 'Move Selection Right',
    MOVE_LEFT: 'Move Selection Left',
    MOVE_UP: 'Move Selection Up',
    MOVE_DOWN: 'Move Selection Down'
  }
};
