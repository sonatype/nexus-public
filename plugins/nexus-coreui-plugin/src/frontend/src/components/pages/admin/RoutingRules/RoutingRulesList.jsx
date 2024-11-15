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

import {faMapSigns} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  HelpTile,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Section,
  SectionToolbar,
  Utils
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFilterInput,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';

import RoutingRulesGlobalPreview from './RoutingRulesGlobalPreview';
import RoutingRulesListMachine from './RoutingRulesListMachine';

import UIStrings from '../../../../constants/UIStrings';

const {ROUTING_RULES: {LIST: LABELS, MENU}} = UIStrings;

export default function RoutingRulesList({onCreate, onEdit}) {
  const [current, send] = useMachine(RoutingRulesListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const data = current.context.data;
  const filterText = current.context.filter;
  const error = current.context.error;

  function getSortDirection(fieldName, sortField, sortDirection) {
    if (sortField !== fieldName) {
      return null;
    }
    return sortDirection === Utils.ASC ? 'asc' : 'desc';
  }

  const nameSortDir = getSortDirection('name', current.context.sortField, current.context.sortDirection);
  const descriptionSortDir = getSortDirection('description', current.context.sortField, current.context.sortDirection);
  const usedBySortDir = getSortDirection('assignedRepositoryCount', current.context.sortField, current.context.sortDirection);

  function filter(value) {
    send({type: 'FILTER', filter: value});
  }

  function preview() {
    send({type: 'PREVIEW'});
  }

  if (current.matches('preview')) {
    return <RoutingRulesGlobalPreview/>
  }

  return <Page className="nxrm-routing-rules">
    <PageHeader>
      <PageTitle icon={faMapSigns} {...MENU}/>
      <PageActions>
        <NxButton variant="tertiary" onClick={preview}>
          <span>{LABELS.PREVIEW_BUTTON}</span>
        </NxButton>
        <NxButton variant="primary" onClick={onCreate}>
          <span>{LABELS.CREATE_BUTTON}</span>
        </NxButton>
      </PageActions>
    </PageHeader>
    <ContentBody>
      <Section className="nxrm-routing-rules-list">
        <SectionToolbar>
          <div className="nxrm-spacer" />
          <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={UIStrings.FILTER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell onClick={() => send({type: 'SORT_BY_NAME'})} isSortable sortDir={nameSortDir}>
                {LABELS.NAME_LABEL}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_DESCRIPTION'})} isSortable sortDir={descriptionSortDir}>
                {LABELS.DESCRIPTION_LABEL}
              </NxTableCell>
              <NxTableCell onClick={() => send({type: 'SORT_BY_ASSIGNED_REPOSITORY_COUNT'})} isSortable sortDir={usedBySortDir}>
                {LABELS.USED_BY_LABEL}
              </NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={LABELS.EMPTY_LIST}>
            {data.map(({name, description, assignedRepositoryCount}) => (
                <NxTableRow key={name} onClick={() => onEdit(name)} isClickable>
                  <NxTableCell>{name}</NxTableCell>
                  <NxTableCell>{description}</NxTableCell>
                  <NxTableCell>
                    {assignedRepositoryCount === 0 ?
                        <a href="#admin/repository/repositories">{LABELS.NEEDS_ASSIGNMENT}</a> :
                        <span>{LABELS.USED_BY(assignedRepositoryCount)}</span>
                    }
                  </NxTableCell>
                  <NxTableCell chevron/>
                </NxTableRow>
            ))}
          </NxTableBody>
        </NxTable>
      </Section>
      <HelpTile header={LABELS.HELP_TITLE} body={LABELS.HELP_TEXT}/>
    </ContentBody>
  </Page>;
}
