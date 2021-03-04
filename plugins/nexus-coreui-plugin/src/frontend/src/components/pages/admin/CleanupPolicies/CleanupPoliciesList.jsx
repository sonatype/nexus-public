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

import {faChevronRight, faInfoCircle, faBroom} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
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
  PageActions,
  PageHeader,
  PageTitle,
  Section,
  SectionToolbar,
  Utils
} from '@sonatype/nexus-ui-plugin';

import CleanupPoliciesListMachine from './CleanupPoliciesListMachine';

import UIStrings from '../../../../constants/UIStrings';

export default function CleanupPoliciesList({onCreate, onEdit}) {
  const [current, send] = useMachine(CleanupPoliciesListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const data = current.context.data;
  const filterText = current.context.filter;
  const error = current.context.error;

  const nameSortDir = Utils.getSortDirection('name', current.context);
  const formatSortDir = Utils.getSortDirection('format', current.context);
  const notesSortDir = Utils.getSortDirection('notes', current.context);

  function filter(value) {
    send({type: 'FILTER', filter: value});
  }

  return <Page className="nxrm-cleanup-policies">
    <PageHeader>
      <PageTitle icon={faBroom} {...UIStrings.CLEANUP_POLICIES.MENU}/>
      <PageActions>
        <NxButton variant="primary" onClick={onCreate}>
          <span>{UIStrings.CLEANUP_POLICIES.CREATE_BUTTON}</span>
        </NxButton>
      </PageActions>
    </PageHeader>
    <ContentBody>
      <Section className="nxrm-cleanup-policies-list">
        <SectionToolbar>
          <div className="nxrm-spacer" />
          <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={UIStrings.CLEANUP_POLICIES.FILTER_PLACEHOLDER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={() => send({type: 'SORT_BY_NAME'})} isSortable sortDir={nameSortDir}>
                {UIStrings.CLEANUP_POLICIES.NAME_LABEL}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_FORMAT'})} isSortable sortDir={formatSortDir}>
                {UIStrings.CLEANUP_POLICIES.FORMAT_LABEL}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_NOTES'})} isSortable sortDir={notesSortDir}>
                {UIStrings.CLEANUP_POLICIES.NOTES_LABEL}
              </NxTableCell>
              <NxTableCell hasIcon/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={UIStrings.CLEANUP_POLICIES.EMPTY_MESSAGE}>
            {data.map(({name, format, notes}) => (
                <NxTableRow key={name} onClick={() => onEdit(name)} isClickable>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{format}</NxTableCell>
                  <NxTableCell>{notes}</NxTableCell>
                  <NxTableCell hasIcon><NxFontAwesomeIcon icon={faChevronRight}/></NxTableCell>
                </NxTableRow>
            ))}
          </NxTableBody>
        </NxTable>
      </Section>

      <HelpTile>
        <h3><NxFontAwesomeIcon icon={faInfoCircle}/>{UIStrings.CLEANUP_POLICIES.HELP_TITLE}</h3>
        <p dangerouslySetInnerHTML={{__html: UIStrings.CLEANUP_POLICIES.HELP_TEXT}}/>
      </HelpTile>
    </ContentBody>
  </Page>;
}
