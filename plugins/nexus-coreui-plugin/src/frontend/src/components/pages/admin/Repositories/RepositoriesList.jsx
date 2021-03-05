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
  HelpTile,
  NxButton,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  Page,
  PageHeader,
  PageTitle,
  PageActions,
  Section,
  SectionToolbar,
  Utils
} from '@sonatype/nexus-ui-plugin';
import {
  faChevronRight,
  faCopy,
  faDatabase,
  faInfoCircle
} from '@fortawesome/free-solid-svg-icons';

import RepositoryStatus from './RepositoryStatus';
import RepositoriesListMachine from './RepositoriesListMachine';
import UIStrings from '../../../../constants/UIStrings';

const {REPOSITORIES} = UIStrings;
const {COLUMNS} = REPOSITORIES.LIST;

export default function RepositoriesList({onCreate, onEdit, copyUrl = doCopyUrl}) {
  const [current, send] = useMachine(RepositoriesListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const {data, error, filter: filterText} = current.context;

  const nameSortDir = Utils.getSortDirection('name', current.context);
  const typeSortDir = Utils.getSortDirection('type', current.context);
  const formatSortDir = Utils.getSortDirection('format', current.context);
  const statusSortDir = Utils.getSortDirection('status', current.context);
  const sortByName = () => send('SORT_BY_NAME');
  const sortByType = () => send('SORT_BY_TYPE');
  const sortByFormat = () => send('SORT_BY_FORMAT');
  const sortByStatus = () => send('SORT_BY_STATUS');

  const filter = (value) => send({type: 'FILTER', filter: value});

  return <Page className="nxrm-repositories">
    <PageHeader>
      <PageTitle icon={faDatabase} {...REPOSITORIES.MENU} />
      <PageActions>
        <NxButton variant="primary" onClick={onCreate}>{REPOSITORIES.LIST.CREATE_BUTTON}</NxButton>
      </PageActions>
    </PageHeader>
    <ContentBody className="nxrm-repositories-list">
      <Section>
        <SectionToolbar>
          <div className="nxrm-spacer"/>
          <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={REPOSITORIES.LIST.FILTER_PLACEHOLDER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={sortByName} isSortable sortDir={nameSortDir}>{COLUMNS.NAME}</NxTableCell>
              <NxTableCell onClick={sortByType} isSortable sortDir={typeSortDir}>{COLUMNS.TYPE}</NxTableCell>
              <NxTableCell onClick={sortByFormat} isSortable sortDir={formatSortDir}>{COLUMNS.FORMAT}</NxTableCell>
              <NxTableCell onClick={sortByStatus} isSortable sortDir={statusSortDir}>{COLUMNS.STATUS}</NxTableCell>
              <NxTableCell>{COLUMNS.URL}</NxTableCell>
              <NxTableCell hasIcon/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={REPOSITORIES.LIST.EMPTY_LIST}>
            {data.map(
                ({name, type, format, url, status}) => (
                    <NxTableRow key={name} onClick={() => onEdit(name)} isClickable>
                      <NxTableCell>{name}</NxTableCell>
                      <NxTableCell>{type}</NxTableCell>
                      <NxTableCell>{format}</NxTableCell>
                      <NxTableCell>
                        <RepositoryStatus status={status}/>
                      </NxTableCell>
                      <NxTableCell>
                        <NxButton variant="icon-only" onClick={e => copyUrl(e, url)} title={"Copy URL to Clipboard"}>
                          <NxFontAwesomeIcon icon={faCopy}/>
                        </NxButton>
                        {url}
                      </NxTableCell>
                      <NxTableCell hasIcon><NxFontAwesomeIcon icon={faChevronRight}/></NxTableCell>
                    </NxTableRow>
                ))}
          </NxTableBody>
        </NxTable>
      </Section>

      <HelpTile>
        <h3><NxFontAwesomeIcon icon={faInfoCircle}/><span>{REPOSITORIES.LIST.HELP.TITLE}</span></h3>
        <p dangerouslySetInnerHTML={{__html: REPOSITORIES.LIST.HELP.TEXT}}/>
      </HelpTile>
    </ContentBody>
  </Page>;
}

function doCopyUrl(event, url) {
  event.stopPropagation();
  navigator.clipboard.writeText(url);
  ExtJS.showSuccessMessage('URL Copied to Clipboard');
}
