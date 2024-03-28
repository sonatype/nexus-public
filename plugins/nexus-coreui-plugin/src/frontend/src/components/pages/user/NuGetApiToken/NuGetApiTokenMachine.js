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

import {ExtJS} from '@sonatype/nexus-ui-plugin';

import TokenMachine from '../../../../interfaces/TokenMachine';
import UIStrings from '../../../../constants/UIStrings';

export default TokenMachine.withConfig({
      actions: {
        showResetSuccess: () => {
          ExtJS.showSuccessMessage(UIStrings.NUGET_API_KEY.MESSAGES.RESET_SUCCESS)
        },
        showResetError: () => {
          ExtJS.showErrorMessage(UIStrings.NUGET_API_KEY.MESSAGES.RESET_ERROR)
        },
        showAccessError: () => {
          ExtJS.showErrorMessage(UIStrings.NUGET_API_KEY.MESSAGES.ACCESS_ERROR);
        }
      },
      services: {
        resetToken: () => ExtJS.requestAuthenticationToken(UIStrings.NUGET_API_KEY.AUTH_INSTRUCTIONS)
            .then(authToken =>
                Axios.delete(`/service/rest/internal/nuget-api-key?authToken=${btoa(authToken)}`)),
        accessToken: () => ExtJS.requestAuthenticationToken(UIStrings.NUGET_API_KEY.AUTH_INSTRUCTIONS)
            .then(authToken =>
                Axios.get(`/service/rest/internal/nuget-api-key?authToken=${btoa(authToken)}`))
      }
    },
);
