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
import React from 'react';
import {NxBackButton, NxH3, NxLoadWrapper} from '@sonatype/react-shared-components';
import {ContentBody, Page, Section} from '@sonatype/nexus-ui-plugin';
import {useActor} from '@xstate/react';
import './BundleDetail.scss';
import UIStrings from '../../../../constants/UIStrings';


export default function BundleDetail({itemId, service}) {
  const [current] = useActor(service);
  const isLoading = current.matches('loading');
  const data = current.context.data.find(v => v.id == itemId);
  const error = current.context.error;

  return (
    <Page>
      <NxBackButton href="#admin/system/bundles" targetPageTitle="Bundles" />
      <ContentBody>
        <Section>
          <NxH3>{UIStrings.BUNDLES.BUNDLES_DETAIL.MENU.text}</NxH3>
          <NxLoadWrapper retryHandler={() => {}} loading={isLoading} error={error}>
              {() => (
                <dl className="nx-list nx-list--description-list nxrm-desc-list">
                  <div className="nx-list__item">
                    <dt className="nx-list__term">{UIStrings.BUNDLES.BUNDLES_DETAIL.LIST.ID_LABEL}</dt>
                    <dd className="nx-list__description">{data.id}</dd>
                  </div>
                  <div className="nx-list__item">
                    <dt className="nx-list__term">{UIStrings.BUNDLES.BUNDLES_DETAIL.LIST.NAME_LABEL}</dt>
                    <dd className="nx-list__description">{data.name}</dd>
                  </div>
                  <div className="nx-list__item">
                    <dt className="nx-list__term">{UIStrings.BUNDLES.BUNDLES_DETAIL.LIST.SYMBOLIC_NAME_LABEL}</dt>
                    <dd className="nx-list__description">{data.symbolicName}</dd>
                  </div>
                  <div className="nx-list__item">
                    <dt className="nx-list__term">{UIStrings.BUNDLES.BUNDLES_DETAIL.LIST.VERSION_LABEL}</dt>
                    <dd className="nx-list__description">{data.version}</dd>
                  </div>
                  <div className="nx-list__item">
                    <dt className="nx-list__term">{UIStrings.BUNDLES.BUNDLES_DETAIL.LIST.STATE_LABEL}</dt>
                    <dd className="nx-list__description">{data.state}</dd>
                  </div>
                  <div className="nx-list__item">
                    <dt className="nx-list__term">{UIStrings.BUNDLES.BUNDLES_DETAIL.LIST.LOCATION_LABEL}</dt>
                    <dd className="nx-list__description">{data.location}</dd>
                  </div>
                  <div className="nx-list__item">
                    <dt className="nx-list__term">{UIStrings.BUNDLES.BUNDLES_DETAIL.LIST.START_LEVEL_LABEL}</dt>
                    <dd className="nx-list__description">{data.startLevel}</dd>
                  </div>
                  <div className="nx-list__item">
                    <dt className="nx-list__term">{UIStrings.BUNDLES.BUNDLES_DETAIL.LIST.LAST_MODIFIED_LABEL}</dt>
                    <dd className="nx-list__description">{data.lastModified}</dd>
                  </div>
                  <div className="nx-list__item">
                    <dt className="nx-list__term">{UIStrings.BUNDLES.BUNDLES_DETAIL.LIST.FRAGMENT_LABEL}</dt>
                    <dd className="nx-list__description">{data.fragment?.toString()}</dd>
                  </div>
                  {
                    Object.entries(data.headers).map(([headerTitle, headerDesc]) => {
                      return <div className="nx-list__item" key={headerTitle}>
                        <dt className="nx-list__term">{headerTitle}</dt>
                        <dd className="nx-list__description">{headerDesc}</dd>
                      </div>
                    })
                  }
                </dl>
              )}
          </NxLoadWrapper>
        </Section>
      </ContentBody>
    </Page>
  );
}
