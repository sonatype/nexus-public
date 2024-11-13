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
import UIStrings from '../../../../constants/UIStrings';
import {ExtJS, Utils} from "@sonatype/nexus-ui-plugin";
import {applicationHealthCheckUrl} from "./AnalyzeApplicationMachine";

export default Utils.buildFormMachine({
  id: 'analyzeApplicationModalMachine',
  config: (config) => ({
    ...config,
    states: {
      ...config.states,
      loaded: {
        ...config.states.loaded,
        on: {
          ...config.states.loaded.on,
          ASSET: {
            target: 'loaded',
            actions: ['selectAsset'],
            internal: false
          },
          ANALYZE: {
            target: 'analyzing'
          },
          CANCEL: {
            actions: ['onClose']
          }
        }
      },
      analyzing: {
        on: {
          CANCEL: {
            actions: ['onClose']
          }
        },
        invoke: {
          src: 'analyze',
          onDone: {
            actions: ['analyzeSuccess'],
            target: 'analyzed'
          },
          onError: {
            actions: ['analyzeError'],
            target: 'loaded'
          }
        }
      },
      analyzed: {
        'entry': 'onAnalyzed'
      }
    }
  })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data: {emailAddress, password, reportLabel}}) => {
        return {
          emailAddress: Utils.isBlank(emailAddress) ? UIStrings.ERROR.FIELD_REQUIRED : null,
          password: Utils.isBlank(password) || password === undefined ? UIStrings.ERROR.FIELD_REQUIRED : null,
          reportLabel: Utils.isBlank(reportLabel) ? UIStrings.ERROR.FIELD_REQUIRED : null
        }
      }
    }),
    analyzeError: () => ExtJS.showErrorMessage("Error analyzing the application"),
    analyzeSuccess: () => ExtJS.showSuccessMessage('Analysis in process. Email will be sent when report is ready.'),
    selectAsset: assign({
      data: ({data}, event) => ({...data, ...event.data})
    })
  },
  services: {
    fetchData: ({componentModel}) => Axios.get(applicationHealthCheckUrl, {params: {component: JSON.stringify(componentModel)}}),
    analyze: (context) => {
      let message = {
        repositoryName: context.componentModel.repositoryName,
        assetId: context.data.selectedAsset,
        emailAddress: context.data.emailAddress,
        reportPassword: context.data.password,
        proprietaryPackages: context.data.packages,
        reportLabel: context.data.reportLabel
      }
      return Axios.post(applicationHealthCheckUrl, message);
    }
  }
});
