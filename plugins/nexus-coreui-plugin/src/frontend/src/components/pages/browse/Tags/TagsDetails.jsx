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
import {useMachine} from '@xstate/react';

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

import TagsDetailsMachine from './TagsDetailsMachine';
import { faTags } from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';

const {TAGS} = UIStrings;

export default function TagsDetails({itemId}) {
  const [state, send] = useMachine(TagsDetailsMachine, {
    context: {
      pristineData: {
        name: itemId
      }
    },
    devTools: true
  });
  const isLoading = state.matches('loading');
  const tagData = state.context.data;
  const hasLoadError = state.matches('loadError') ? state.context.error : null;

  function retry() {
    send({type: 'RETRY'});
  }

  return <Page className="nxrm-tags">
    <div>
      <PageTitle icon={faTags} {...TAGS.DETAILS.HEADER}/>
      <NxBackButton href="#browse/tags" text={TAGS.DETAILS.BACK_TO_TAGS_TABLE}/>
    </div>
    <ContentBody>
      <NxTile>
        <NxLoadWrapper retryHandler={retry} loading={isLoading} error={hasLoadError}>
          {() => (
            <>
              <NxTile.Header>
                <NxTile.HeaderTitle>
                  <NxH2>{tagData.name} {TAGS.DETAILS.TILE_HEADER}</NxH2>
                </NxTile.HeaderTitle>
                <NxTile.HeaderActions>
                  <NxTextLink href={'#browse/search/custom=' + encodeURIComponent(`tags="${tagData.name}"`)}>{TAGS.DETAILS.FIND_TAGGED}</NxTextLink>
                </NxTile.HeaderActions>
              </NxTile.Header>
              <NxTile.Content>
                <NxReadOnly>
                  <ReadOnlyField label={TAGS.DETAILS.FIRST_CREATED} value={new Date(tagData.firstCreated).toLocaleString()}/>
                  <ReadOnlyField label={TAGS.DETAILS.LAST_UPDATED} value={new Date(tagData.lastUpdated).toLocaleString()}/>
                </NxReadOnly>
                <NxCopyToClipboard 
                  label={TAGS.DETAILS.ATTRIBUTES}
                  content={JSON.stringify(tagData.attributes, null, 2)} 
                />
              </NxTile.Content>
            </>
          )}
        </NxLoadWrapper>
      </NxTile>
    </ContentBody>
  </Page>
}
