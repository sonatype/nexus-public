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

import {Utils, FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

export const repositoryUrl = (format, type) =>
  `/service/rest/v1/repositories/${formatFormat(format)}/${type}`;

export default FormUtils.buildFormMachine({
  id: 'RepositoriesFormMachine'
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        name: validateNameField(data.name),
        format: Utils.isBlank(data.format)
          ? UIStrings.ERROR.FIELD_REQUIRED
          : null,
        type: Utils.isBlank(data.type) ? UIStrings.ERROR.FIELD_REQUIRED : null,
        blobStoreName: Utils.isBlank(data.blobStoreName)
          ? UIStrings.ERROR.FIELD_REQUIRED
          : null,
        memberNames:
          isGroupType(data.type) && !data.memberNames.length
            ? UIStrings.ERROR.FIELD_REQUIRED
            : null
      })
    })
  },
  services: {
    fetchData: async ({pristineData}) => {
      if (isEdit(pristineData)) {
        const response = await Axios.get(repositoryUrl(pristineData.name));
        const {
          group: {memberNames},
          storage: {blobStoreName, strictContentTypeValidation, writePolicy},
          component: {proprietaryComponents},
          cleanup: {policyNames},
          ...rest
        } = response;
        return {
          data: {
            ...rest,
            memberNames,
            blobStoreName,
            strictContentTypeValidation,
            writePolicy,
            proprietaryComponents,
            policyNames
          }
        };
      } else {
        return {
          data: {
            format: '',
            type: '',
            name: '',
            online: true,
            memberNames: [],
            strictContentTypeValidation: true,
            writePolicy: 'ALLOW_ONCE',
            proprietaryComponents: false,
            policyNames: []
          }
        };
      }
    },
    saveData: ({data, pristineData}) => {
      const {
        format,
        type,
        name,
        online,
        blobStoreName,
        strictContentTypeValidation,
        memberNames,
        writePolicy,
        proprietaryComponents,
        policyNames
      } = data;
      const payload = {
        name,
        online,
        storage: {
          blobStoreName,
          strictContentTypeValidation,
          writePolicy
        },
        group: {memberNames},
        component: {proprietaryComponents},
        cleanup: {policyNames}
      };
      return isEdit(pristineData)
        ? Axios.put(repositoryUrl(format, type), payload)
        : Axios.post(repositoryUrl(format, type), payload);
    }
  }
});

const isEdit = ({name}) => Utils.notBlank(name);

const validateNameField = (field) => {
  if (Utils.isBlank(field)) {
    return UIStrings.ERROR.FIELD_REQUIRED;
  } else if (field.length > 255) {
    return UIStrings.ERROR.MAX_CHARS(255);
  } else if (!Utils.isName(field)) {
    return UIStrings.ERROR.INVALID_NAME_CHARS;
  }

  return null;
};

const formatFormat = (format) => (format === 'maven2' ? 'maven' : format);

const isGroupType = (type) => type === 'group';
