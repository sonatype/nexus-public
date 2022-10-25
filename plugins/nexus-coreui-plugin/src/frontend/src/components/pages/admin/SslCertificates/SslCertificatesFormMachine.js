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
import {
  ExtJS,
  FormUtils,
  APIConstants,
  ExtAPIUtils,
  ValidationUtils
} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';
import {URLS, remoteHostRequestData} from './SslCertificatesHelper';
import {mergeDeepRight} from 'ramda';

const {
  SSL_CERTIFICATES: {MESSAGES: LABELS}
} = UIStrings;

const {EXT} = APIConstants;

const {sslCertificatesUrl, singleSslCertificatesUrl, createSslCertificatesUrl} = URLS;

export const SOURCES = {
  REMOTE_HOST: 'remoteHost',
  PEM: 'PEM'
};

export default FormUtils.buildFormMachine({
  id: 'SslCertificatesFormMachine',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        loaded: {
          on: {
            SET_SOURCE: {
              actions: 'setSource',
              target: 'loaded'
            },
            LOAD_NEW: {
              actions: 'setLoadNew',
              target: 'loading'
            }
          }
        },
        loading: {
          invoke: {
            onError: {
              target: 'loaded'
            }
          }
        }
      }
    })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data, source}) => ({
        remoteHostUrl:
          source === SOURCES.REMOTE_HOST && ValidationUtils.validateNotBlank(data.remoteHostUrl),
        pemContent: source === SOURCES.PEM && ValidationUtils.validateNotBlank(data.pemContent)
      })
    }),
    setData: assign(({id, data, pristineData}, event) => {
      const certificates = event.data?.data;

      let certificate;
      if (Array.isArray(certificates)) {
        certificate = certificates?.find((it) => it.id === id);
        if (!certificate) {
          ExtJS.showErrorMessage(UIStrings.ERROR.NOT_FOUND_ERROR(id));
          certificate = {};
        }
      } else {
        certificate = certificates;
      }

      return {
        data: {
          ...pristineData,
          ...data,
          certificate
        }
      };
    }),
    setSource: assign(({data}, {value}) => {
      let {remoteHostUrl, pemContent} = data;
      return {
        source: value,
        data: {
          ...data,
          remoteHostUrl: value === SOURCES.REMOTE_HOST ? remoteHostUrl : '',
          pemContent: value === SOURCES.PEM ? pemContent : ''
        }
      };
    }),
    setLoadNew: assign({
      shouldLoadNew: () => true
    }),
    onDeleteError: (_, event) => ExtJS.showErrorMessage(event.data?.response?.data),
    logDeleteSuccess: ({data: {certificate}}) =>
      ExtJS.showSuccessMessage(LABELS.DELETE_SUCCESS(certificate.subjectCommonName)),
    logLoadError: (_, event) => {
      const errorMessage = event.data?.message;
      if (errorMessage) {
        console.error(`Load Error: ${event.data?.message}`);
        ExtJS.showErrorMessage(errorMessage);
      } else {
        ExtJS.showErrorMessage(UIStrings.ERROR.LOAD_ERROR);
      }
    }
  },
  services: {
    fetchData: async ({id, shouldLoadNew, data}) => {
      const isCreate = !id && !shouldLoadNew;
      if (isCreate) {
        return {data: {}};
      }
      const {remoteHostUrl, pemContent} = data;
      if (id) {
        return Axios.get(sslCertificatesUrl);
      } else if (remoteHostUrl) {
        const response = await ExtAPIUtils.extAPIRequest(
          EXT.SSL.ACTION,
          EXT.SSL.METHODS.RETRIEVE_FROM_HOST,
          {data: remoteHostRequestData(remoteHostUrl)}
        );
        ExtAPIUtils.checkForError(response);
        return {data: ExtAPIUtils.extractResult(response)};
      } else if (pemContent) {
        const response = await ExtAPIUtils.extAPIRequest(EXT.SSL.ACTION, EXT.SSL.METHODS.DETAILS, {
          data: [pemContent]
        });
        ExtAPIUtils.checkForError(response);
        return {data: ExtAPIUtils.extractResult(response)};
      } else {
        return {data: {}};
      }
    },
    saveData: (ctx) => Axios.post(createSslCertificatesUrl, ctx.data.certificate.pem),
    confirmDelete: ({data: {certificate}}) =>
      ExtJS.requestConfirmation({
        title: LABELS.CONFIRM_DELETE.TITLE,
        message: LABELS.CONFIRM_DELETE.MESSAGE(certificate.subjectCommonName),
        yesButtonText: LABELS.CONFIRM_DELETE.YES,
        noButtonText: LABELS.CONFIRM_DELETE.NO
      }),
    delete: ({
      data: {
        certificate: {id}
      }
    }) => Axios.delete(singleSslCertificatesUrl(id))
  }
});
