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
import axios from 'axios';
import {assign, createMachine} from 'xstate';
import {includes} from 'ramda';
import APIConstants from '../../../constants/APIConstants';
import FormUtils from '../../../interface/FormUtils';

const CERTIFICATE_DETAILS_URL = APIConstants.REST.PUBLIC.SSL_CERTIFICATE_DETAILS;
const TRUSTSTORE_URL = APIConstants.REST.PUBLIC.SSL_CERTIFICATES;
const REMOVE_CERTIFICATE_URL = (id) => `${TRUSTSTORE_URL}/${id}`;

/**
 * When instantiating this machine, the host for the ssl certificate must be set in the context
 */
export default createMachine(
  {
    id: 'SslCertificateDetailsModalMachine',
    initial: 'loading',

    states: {
      loading: {
        invoke: {
          src: 'loadCertificateDetails',
          onDone: {
            target: 'viewing',
            actions: ['setCertificateDetails']
          },
          onError: {
            target: 'loadError',
            actions: ['setError']
          }
        }
      },
      viewing: {
        on: {
          ADD_CERTIFICATE: {
            target: 'adding',
            guards: ['canAddCertificate']
          },
          REMOVE_CERTIFICATE: {
            target: 'removing',
            guards: ['canRemoveCertificate']
          }
        }
      },
      loadError: {
        on: {
          RETRY: {
            target: 'loading'
          }
        }
      },
      adding: {
        invoke: {
          src: 'addCertificateToTruststore',
          onDone: 'close',
          onError: {
            target: 'addCertificateError',
            actions: ['setError']
          }
        }
      },
      removing: {
        invoke: {
          src: 'removeCertificateFromTruststore',
          onDone: 'close',
          onError: {
            target: 'removeCertificateError',
            actions: ['setError']
          }
        }
      },
      addCertificateError: {
        on: {
          RETRY: {
            target: 'adding',
            actions: ['clearError']
          },
          ADD_CERTIFICATE: {
            target: 'adding',
            guards: ['canAddCertificate']
          }
        }
      },
      removeCertificateError: {
        on: {
          RETRY: {
            target: 'removing',
            actions: ['clearError']
          },
          REMOVE_CERTIFICATE: {
            target: 'removing',
            guards: ['canRemoveCertificate']
          }
        }
      },
      close: {
        entry: 'close',
        type: 'final'
      }
    },
    on: {
      CLOSE: {
        target: 'close'
      }
    }
  },
  {
    actions: {
      setCertificateDetails: assign({
        certificateDetails: (_, event) => event.data[0].data,
        isInTruststore: (_, event) => includes(event.data[0].data, event.data[1].data)
      }),
      setError: assign({
        error: (_, event) => FormUtils.extractSaveErrorMessage(event),
      }),
      clearError: assign({
        certificateError: () => null
      }),
      close: ({onCancel}) => onCancel()
    },
    guards: {
      canAddCertificateDetails: ({isInTruststore}) => !isInTruststore,
      canRemoveCertificateDetails: ({isInTruststore}) => isInTruststore
    },
    services: {
      loadCertificateDetails: ({host, port}) =>
        axios.all([
          axios.get(CERTIFICATE_DETAILS_URL + `?host=${host}&port=${port}`),
          axios.get(TRUSTSTORE_URL)
        ]),
      addCertificateToTruststore: ({certificateDetails}) =>
        axios.post(TRUSTSTORE_URL, certificateDetails.pem),
      removeCertificateFromTruststore: ({certificateDetails}) =>
        axios.delete(REMOVE_CERTIFICATE_URL(certificateDetails.id))
    }
  }
);
