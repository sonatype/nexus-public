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
  ExtJS,
  ListMachineUtils,
  Page,
  PageHeader,
  PageTitle,
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxButtonBar,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxTable,
  NxTile
} from '@sonatype/react-shared-components';

import {faCopy, faUpload} from '@fortawesome/free-solid-svg-icons';
import UploadListMachine from './UploadListMachine';
import UIStrings from '../../../../constants/UIStrings';
import './Upload.scss';

const {UPLOAD, FILTER} = UIStrings;
const {COLUMNS, COPY_URL_TITLE} = UPLOAD.LIST;

export default function UploadList({onEdit, copyUrl = doCopyUrl}) {
  const [state, send] = useMachine(UploadListMachine, {devTools: true});
  const {data, error, filter: filterText} = state.context;
  const isLoading = state.matches('loading');

  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const formatSortDir = ListMachineUtils.getSortDirection('format', state.context);
  const sortByName = () => send({type: 'SORT_BY_NAME'});
  const sortByFormat = () => send({type: 'SORT_BY_FORMAT'});

  const filter = (value) => send({type: 'FILTER', filter: value});

  return <Page className="nxrm-upload">
    <PageHeader>
      <PageTitle icon={faUpload} {...UPLOAD.MENU}/>
    </PageHeader>
    <ContentBody className="nxrm-upload-list">
      <NxTile>
        <NxTile.Header>
            <NxTile.HeaderActions>
              <NxFilterInput
                  id="filter"
                  onChange={filter}
                  value={filterText}
                  placeholder={FILTER}/>
            </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content>
          <NxTable id="nxrm-upload-list-repo-table">
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell onClick={sortByName} isSortable sortDir={nameSortDir}>{COLUMNS.NAME}</NxTable.Cell>
                <NxTable.Cell onClick={sortByFormat} isSortable sortDir={formatSortDir}>{COLUMNS.FORMAT}</NxTable.Cell>
                <NxTable.Cell>{COLUMNS.URL}</NxTable.Cell>
                <NxTable.Cell chevron />
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body isLoading={isLoading} error={error} emptyMessage={UPLOAD.EMPTY_MESSAGE}>
              {data.map(
                ({name, format, url}) => (
                  <NxTable.Row key={name} isClickable onClick={() => onEdit(name)}>
                    <NxTable.Cell>{name}</NxTable.Cell>
                    <NxTable.Cell>{format}</NxTable.Cell>
                    <NxTable.Cell>
                      <NxButtonBar>
                        <NxButton
                          variant="icon-only"
                          title={COPY_URL_TITLE}
                          onClick={(e) => copyUrl(e, url)}>
                          <NxFontAwesomeIcon icon={faCopy} />
                        </NxButton>
                      </NxButtonBar>
                    </NxTable.Cell>
                    <NxTable.Cell chevron />
                  </NxTable.Row>
              ))}
            </NxTable.Body>
          </NxTable>
        </NxTile.Content>
      </NxTile>
    </ContentBody>
  </Page>
}

function doCopyUrl(event, url) {
  event.stopPropagation();
  navigator.clipboard.writeText(url);
  ExtJS.showSuccessMessage(UPLOAD.URL_COPIED_MESSAGE);
}
