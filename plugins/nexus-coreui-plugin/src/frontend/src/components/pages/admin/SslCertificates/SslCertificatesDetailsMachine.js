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

import {ExtJS, FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import {URL} from './SslCertificatesHelper';

const {SSL_CERTIFICATES: {MESSAGES: LABELS}} = UIStrings;

const {sslCertificatesUrl, singleSslCertificatesUrl} = URL;

export default FormUtils.buildFormMachine({
  id: 'SslCertificatesDetailsMachine',
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: () => {{}}
    }),
    setData: assign(({pristineData: {id}}, {data: sslCertificates}) => {
      let certificate = sslCertificates?.data?.find(it => it.id === id);

      if (!certificate) {
        ExtJS.showErrorMessage(UIStrings.ERROR.NOT_FOUND_ERROR(id));
        certificate = {};
      }

      return {
        data: certificate,
        pristineData: certificate,
      };
    }),
    onDeleteError: (_, event) => ExtJS.showErrorMessage(event.data?.response?.data),
    logDeleteSuccess: ({data}) => ExtJS.showSuccessMessage(LABELS.DELETE_SUCCESS(data.subjectCommonName)),
  },
  services: {
    fetchData: () => Axios.get(sslCertificatesUrl),
    confirmDelete: ({data}) => ExtJS.requestConfirmation({
      title: LABELS.CONFIRM_DELETE.TITLE,
      message: LABELS.CONFIRM_DELETE.MESSAGE(data.subjectCommonName),
      yesButtonText: LABELS.CONFIRM_DELETE.YES,
      noButtonText: LABELS.CONFIRM_DELETE.NO
    }),
    delete: ({data}) => Axios.delete(singleSslCertificatesUrl(data.id)),
  }
});
