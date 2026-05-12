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

const {ERROR, OAUTH2_CONFIGURATION} = UIStrings;

const OAUTH2_API_URL = APIConstants.REST.INTERNAL.OAUTH2;

// Ensure JSON values for these fields
function parseString(val) {
  if(val) {
    const result = JSON.parse(val);
    if (typeof result === 'object') {
      return result;
    }
  }
  return {};
}

function stringifyObject(val) {
  if (val && typeof val === 'object') {
    try {
      return JSON.stringify(val, null, 2);
    } catch {
      return '';
    }
  }
  return val || '';
}

export default FormUtils.buildFormMachine({
  id: 'OAuth2ConfigurationForm'
}).withConfig({
  actions: {
    setData: assign({
      data: (_, {data}) => mapOAuth2Response(data),
      pristineData: (_, {data}) => mapOAuth2Response(data)
    }),

    logSaveSuccess: () => ExtJS.showSuccessMessage(OAUTH2_CONFIGURATION.MESSAGES.SAVE_SUCCESS),

    validate: assign({
      validationErrors: ({data}) => ({
        clientId: ValidationUtils.isBlank(data?.clientId) ? ERROR.FIELD_REQUIRED : null,
        clientSecret: ValidationUtils.isBlank(data?.clientSecret) ? ERROR.FIELD_REQUIRED : null,
        idpAuthorizationUrl: ValidationUtils.isBlank(data?.idpAuthorizationUrl) ? ERROR.FIELD_REQUIRED :
            ValidationUtils.validateIsUrl(data?.idpAuthorizationUrl),
        idpLogoutUrl: ValidationUtils.isBlank(data?.idpLogoutUrl) ? ERROR.FIELD_REQUIRED :
            ValidationUtils.validateIsUrl(data?.idpLogoutUrl),
        idpTokenUrl: ValidationUtils.isBlank(data?.idpTokenUrl) ? ERROR.FIELD_REQUIRED :
            ValidationUtils.validateIsUrl(data?.idpTokenUrl),
        idpJwksUrl: ValidationUtils.isBlank(data?.idpJwksUrl) ? ERROR.FIELD_REQUIRED :
            ValidationUtils.validateIsUrl(data?.idpJwksUrl),
        usernameClaim: ValidationUtils.isBlank(data?.usernameClaim) ? ERROR.FIELD_REQUIRED : null,
        firstNameClaim: ValidationUtils.isBlank(data?.firstNameClaim) ? ERROR.FIELD_REQUIRED : null,
        lastNameClaim: ValidationUtils.isBlank(data?.lastNameClaim) ? ERROR.FIELD_REQUIRED : null,
        emailClaim: ValidationUtils.isBlank(data?.emailClaim) ? ERROR.FIELD_REQUIRED : null,
        groupsClaim: ValidationUtils.isBlank(data?.groupsClaim) ? ERROR.FIELD_REQUIRED : null,
        idpJwsAlgorithm: ValidationUtils.isBlank(data?.idpJwsAlgorithm) ? ERROR.FIELD_REQUIRED : null
      })
    })
  },
  services:
      {
        fetchData: () => Axios.get(OAUTH2_API_URL).then(response => {
          const data = response.data;
          // Convert JSON fields back to string for the form

          return {
            ...response,
            data: {
              ...data,
              exactMatchClaims: stringifyObject(data.exactMatchClaims),
              authorizationCustomParams: stringifyObject(data.authorizationCustomParams),
              tokenRequestCustomParams: stringifyObject(data.tokenRequestCustomParams)
            }
          };
        }),
        saveData: ({data}) => {
          const {
            idpJwksUrl,
            idpJwsAlgorithm,
            usernameClaim,
            firstNameClaim,
            lastNameClaim,
            emailClaim,
            groupsClaim,
            exactMatchClaims,
            clientId,
            idpAuthorizationUrl,
            idpLogoutUrl,
            idpTokenUrl,
            authorizationCustomParams,
            tokenRequestCustomParams
          } = data;
          return Axios.put(OAUTH2_API_URL, {
            ...data,
            idpJwksUrl: idpJwksUrl?.trim(),
            idpJwsAlgorithm: idpJwsAlgorithm?.trim(),
            usernameClaim: usernameClaim?.trim(),
            firstNameClaim: firstNameClaim?.trim(),
            lastNameClaim: lastNameClaim?.trim(),
            emailClaim: emailClaim?.trim(),
            groupsClaim: groupsClaim?.trim(),
            clientId: clientId?.trim(),
            idpAuthorizationUrl: idpAuthorizationUrl?.trim(),
            idpLogoutUrl: idpLogoutUrl?.trim(),
            idpTokenUrl: idpTokenUrl?.trim(),
            exactMatchClaims: parseString(exactMatchClaims),
            authorizationCustomParams: parseString(authorizationCustomParams),
            tokenRequestCustomParams: parseString(tokenRequestCustomParams)
          });
        }
      }
});

/**
 * Returns the OAuth2 configuration data object
 */
function mapOAuth2Response({data}) {
  return data || {
    idpJwksUrl: '',
    idpJwsAlgorithm: '',
    idpJwks: '',
    usernameClaim: 'sub',
    firstNameClaim: '',
    lastNameClaim: '',
    emailClaim: '',
    groupsClaim: '',
    exactMatchClaims: {},
    clientId: '',
    clientSecret: '',
    idpAuthorizationUrl: '',
    idpLogoutUrl: '',
    idpTokenUrl: '',
    authorizationCustomParams: {},
    tokenRequestCustomParams: {},
    useTrustStore: false
  };
}
