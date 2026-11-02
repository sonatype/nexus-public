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

import {ExtAPIUtils, ExtJS, FormUtils} from '@sonatype/nexus-ui-plugin';

import {
  NxFormGroup,
  NxCheckbox,
  NxLoadWrapper,
  NxFieldset,
  NxFormSelect
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function GenericStorageConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;
  const [blobStoresState, blobStoresSend] = ExtAPIUtils.useExtMachine('coreui_Blobstore', 'readNames');

  const {data: blobStores, error} = blobStoresState.context;
  const {format, type} = currentParent.context.data;
  const loadBlobStores = () => blobStoresSend({type: 'LOAD'});
  const isLoading = blobStoresState.matches('loading');

  const {name} = currentParent.context.pristineData;
  const isEdit = !!name;
  const hasEnterprise = ExtJS.isEnterpriseEdition();

  // A repository can only be globally distributed when its blob store has CloudFront or S3
  // Transfer Acceleration configured.
  const {storage} = currentParent.context.data;
  const globalDistributionAvailable = Boolean(
    blobStores?.find((blobStore) => blobStore.name === storage?.blobStoreName)
      ?.globalDistributionEnabled
  );
  const showGlobalDistribution = hasEnterprise && globalDistributionAvailable;

  // NEXUS-54396: on create, seed storage.globalDistribution from the CDN default published by
  // CdnFeatureFlagStateContributor. Only fires when Enterprise and the feature flag are on AND
  // the parent has no explicit value (undefined) — so edit-mode loads and TYPE_CHANGE
  // preservations are untouched.
  const currentGlobalDistribution = storage?.globalDistribution;
  useEffect(() => {
    if (!hasEnterprise) {
      return;
    }
    if (currentGlobalDistribution !== undefined) {
      return;
    }
    const cdnDefault = Boolean(
      ExtJS.state()?.getValue('cdnDefaultForNewRepositories', false)
    );
    sendParent({
      type: 'UPDATE',
      name: 'storage.globalDistribution',
      value: cdnDefault
    });
    // Mount-only — the effect owns the initial seed. Subsequent user toggles flow through the
    // NxCheckbox onChange handler. Do NOT include currentGlobalDistribution in deps; that would
    // re-fire after our own dispatch and thrash.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    loadBlobStores();
  }, [format, type]);

  useEffect(() => {
    if (blobStores?.length && !globalDistributionAvailable && storage?.globalDistribution) {
      FormUtils.handleUpdate('storage.globalDistribution', sendParent)(false);
    }
  }, [blobStores, globalDistributionAvailable]);

  useEffect(() => {
    sendParent({
      type: 'SET_DEFAULT_BLOB_STORE',
      name: 'storage.blobStoreName',
      value: getDefaultBlobStore(blobStores)
    });
  }, [blobStores]);

  return (
    <>
      <h2 className="nx-h2">{EDITOR.STORAGE_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={loadBlobStores}>
        <NxFormGroup label={EDITOR.BLOB_STORE_LABEL} isRequired className="nxrm-form-group-store">
          <NxFormSelect
            {...FormUtils.selectProps('storage.blobStoreName', currentParent)}
            onChange={FormUtils.handleUpdate('storage.blobStoreName', sendParent)}
            disabled={isEdit}
          >
            <option value="">{EDITOR.SELECT_STORE_OPTION}</option>
            {blobStores?.map(({name}) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </NxFormSelect>
        </NxFormGroup>

        {type !== 'group' && (
          <NxFieldset
            label={EDITOR.CONTENT_VALIDATION_LABEL}
            className="nxrm-form-group-content-validation"
          >
            <NxCheckbox
              {...FormUtils.checkboxProps('storage.strictContentTypeValidation', currentParent)}
              onChange={FormUtils.handleUpdate('storage.strictContentTypeValidation', sendParent)}
            >
              {EDITOR.ENABLED_CHECKBOX_DESCR}
            </NxCheckbox>
          </NxFieldset>
        )}

        {showGlobalDistribution && (
          <NxFieldset
            label={EDITOR.GLOBAL_DISTRIBUTION_LABEL}
            className="nxrm-form-group-global-distribution"
          >
            <NxCheckbox
              {...FormUtils.checkboxProps('storage.globalDistribution', currentParent)}
              onChange={FormUtils.handleUpdate('storage.globalDistribution', sendParent)}
            >
              {EDITOR.GLOBAL_DISTRIBUTION_DESCR}
            </NxCheckbox>
          </NxFieldset>
        )}
      </NxLoadWrapper>
    </>
  );
}

const getDefaultBlobStore = (blobStores) =>
  blobStores?.length && blobStores.length === 1 ? blobStores[0].name : '';
