/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React, {useEffect} from 'react';

import {Select, FormUtils, useSimpleMachine} from '@sonatype/nexus-ui-plugin';

import {
  NxFormGroup,
  NxCheckbox,
  NxLoadWrapper
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export const BLOB_STORES_URL = '/service/rest/v1/blobstores';

export default function GenericStorageConfiguration({parentMachine}) {
  const {current, retry, isLoading} = useSimpleMachine(
    'GenericStorageConfigurationMachine',
    BLOB_STORES_URL,
    true
  );

  const [currentParent, sendParent] = parentMachine;

  const {data: blobStores, error} = current.context;

  const {type} = currentParent.context.data;

  useEffect(() => {
    sendParent('UPDATE', {
      data: {blobStoreName: getDefaultBlobStore(blobStores)}
    });
  }, [blobStores]);

  return (
    <>
      <h2 className="nx-h2">{EDITOR.STORAGE_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={retry}>
        <NxFormGroup
          label={EDITOR.BLOB_STORE_LABEL}
          isRequired
          className="nxrm-form-group-store"
        >
          <Select
            {...FormUtils.fieldProps('blobStoreName', currentParent)}
            name="blobStoreName"
            onChange={FormUtils.handleUpdate('blobStoreName', sendParent)}
          >
            <option value="">{EDITOR.SELECT_STORE_OPTION}</option>
            {blobStores?.map(({name}) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </Select>
        </NxFormGroup>

        {type !== 'group' && (
          <NxFormGroup
            label={EDITOR.CONTENT_VALIDATION_LABEL}
            isRequired
            className="nxrm-form-group-content-validation"
          >
            <NxCheckbox
              {...FormUtils.checkboxProps(
                'strictContentTypeValidation',
                currentParent
              )}
              onChange={FormUtils.handleUpdate(
                'strictContentTypeValidation',
                sendParent
              )}
            >
              {EDITOR.CONTENT_VALIDATION_DESCR}
            </NxCheckbox>
          </NxFormGroup>
        )}
      </NxLoadWrapper>
    </>
  );
}

const getDefaultBlobStore = (blobStores) =>
  blobStores?.length && blobStores.length === 1 ? blobStores[0].name : '';
