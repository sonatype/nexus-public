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
import { useService } from '@xstate/react';

import {
  ContentBody,
  Page,
  PageTitle,
  ReadOnlyField
} from '@sonatype/nexus-ui-plugin';

import {
  NxLoadWrapper,
  NxTile,
  NxH2,
  NxTextLink,
  NxBackButton,
  NxReadOnly,
  NxCopyToClipboard
} from '@sonatype/react-shared-components';

import { faTags } from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';

const {TAGS} = UIStrings;

export default function TagsDetails({itemId, service}) {
  const [state] = useService(service);
  const isLoading = state.matches('loading');
  const data = state.context.data.find(v => v.name == itemId);
  const error = state.context.error;

  return <Page className="nxrm-tags">
    <div>
      <PageTitle icon={faTags} {...TAGS.DETAILS.HEADER}/>
      <NxBackButton href="/#browse/tags" text={TAGS.DETAILS.BACK_TO_TAGS_TABLE}/>
    </div>
    <ContentBody>
      <NxLoadWrapper retryHandler={() => {}} loading={isLoading} error={error}>
        {() => (
          <NxTile>
            <NxTile.Header>
              <NxTile.HeaderTitle>
                <NxH2>{data.name} {TAGS.DETAILS.TILE_HEADER}</NxH2>
              </NxTile.HeaderTitle>
              <NxTile.HeaderActions>
                <NxTextLink href={'/#browse/search/custom=' + encodeURIComponent(`tags="${data.name}"`)}>{TAGS.DETAILS.FIND_TAGGED}</NxTextLink>
              </NxTile.HeaderActions>
            </NxTile.Header>
            <NxTile.Content>
                <NxReadOnly>
                  <ReadOnlyField label={TAGS.DETAILS.FIRST_CREATED} value={new Date(data.firstCreated).toLocaleString()}/>
                  <ReadOnlyField label={TAGS.DETAILS.LAST_UPDATED} value={new Date(data.lastUpdated).toLocaleString()}/>
                </NxReadOnly>
                <NxCopyToClipboard 
                  label={TAGS.DETAILS.ATTRIBUTES}
                  content={JSON.stringify(data.attributes, null, 2)} 
                />
            </NxTile.Content>
          </NxTile>
        )}
      </NxLoadWrapper>
    </ContentBody>
  </Page>
}
