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
import {assign} from 'xstate';
import Axios from 'axios';
import {FormUtils, APIConstants, ValidationUtils} from '@sonatype/nexus-ui-plugin';

const {REST: {PUBLIC: {EMAIL_SERVER: emailServerUrl}}} = APIConstants;

export default FormUtils.buildFormMachine({
  id: 'EmailServerMachine',
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data: {host, port, fromAddress}}) => {
        return {
          host: ValidationUtils.validateNotBlank(host) || ValidationUtils.validateHost(host),
          port: ValidationUtils.validateNotBlank(port) ||
              ValidationUtils.isInRange({
                value: port,
                min: 1,
                max: 65535,
                allowDecimals: false
              }),
          fromAddress: ValidationUtils.validateNotBlank(fromAddress) || ValidationUtils.validateEmail(fromAddress),
        };
      },
    }),
  },
  services: {
    fetchData: () => Axios.get(emailServerUrl),
    saveData: ({data}) => Axios.put(emailServerUrl, data),
  },
});
