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
import Axios from 'axios';
import {assign} from 'xstate';

import {ExtJS, FormUtils, ValidationUtils, APIConstants} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {ERROR, SAML_CONFIGURATION} = UIStrings;

const SAML_API_URL = APIConstants.REST.INTERNAL.SAML; 

export default FormUtils.buildFormMachine({
  id: 'SamlConfigurationForm'
}).withConfig({
  actions: {
    setData: assign({
      data: (_, {data}) => mapSamlResponse(data),
      pristineData: (_, {data}) => mapSamlResponse(data)
    }),

    logSaveSuccess: () => ExtJS.showSuccessMessage(SAML_CONFIGURATION.MESSAGES.SAVE_SUCCESS),

    validate: assign({
      validationErrors: ({data}) => ({
        idpMetadata: ValidationUtils.isBlank(data?.idpMetadata) ? ERROR.FIELD_REQUIRED : null,
        entityIdUri: ValidationUtils.notUri(data?.entityIdUri) ? SAML_CONFIGURATION.FIELDS.entityIdUriValidationError : null,
        usernameAttr: ValidationUtils.isBlank(data?.usernameAttr) ? ERROR.FIELD_REQUIRED : null
      })
    })
  },
  services:
      {
        fetchData: () => Axios.get(SAML_API_URL),
        saveData: ({data}) => {
          const {
            validateResponseSignature,
            validateAssertionSignature,
            usernameAttr,
            firstNameAttr,
            lastNameAttr,
            emailAttr,
            roleAttr
          } = data;
          return Axios.put(SAML_API_URL, {
            ...data,
            validateResponseSignature: validateResponseSignature === 'default' ? null : data.validateResponseSignature,
            validateAssertionSignature: validateAssertionSignature === 'default' ? null : data.validateAssertionSignature,
            usernameAttr: usernameAttr.trim(),
            firstNameAttr: firstNameAttr.trim(),
            lastNameAttr: lastNameAttr.trim(),
            emailAttr: emailAttr.trim(),
            roleAttr: roleAttr.trim()
          });
        }
      }
});

/**
 * Returns a modified data object where Boolean properties validateResponseSignature and validateAssertionSignature are
 * handled as strings
 */
function mapSamlResponse({data}) {
  ['validateResponseSignature', 'validateAssertionSignature'].forEach(function(item) {
    data[item] = data[item] === null ? 'default' : data[item].toString();
  });
  return data;
}
