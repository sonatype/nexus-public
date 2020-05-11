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

import {faChevronRight, faPlusCircle, faRedo} from '@fortawesome/free-solid-svg-icons';

import {
  Button,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  Section,
  SectionActions,
  SectionHeader
} from 'nexus-ui-plugin';

import LoggingConfigurationListMachine, {ASC} from './LoggingConfigurationListMachine';

import UIStrings from '../../../../constants/UIStrings';

export default function LoggingConfigurationList({onCreate, onEdit}) {
  const [current, send] = useMachine(LoggingConfigurationListMachine, {devTools: true});
  const isLoading = current.matches('loading');
  const data = current.context.data;
  const filterText = current.context.filter;
  const error = current.context.error;

  function getSortDirection(fieldName, sortField, sortDirection) {
    if (sortField !== fieldName) {
      return null;
    }
    return sortDirection === ASC ? 'asc' : 'desc';
  }

  const nameSortDir = getSortDirection('name', current.context.sortField, current.context.sortDirection);
  const levelSortDir = getSortDirection('level', current.context.sortField, current.context.sortDirection);

  function filter(value) {
    send('FILTER', {filter: value});
  }

  function clearFilter() {
    send('FILTER', {filter: ''});
  }

  function reset() {
    send('RESET');
  }

  return <Section className="nxrm-logging-configuration-list">
    <SectionActions>
      <NxFilterInput
          inputId="filter"
          onChange={filter}
          onClear={clearFilter}
          value={filterText}
          placeholder={UIStrings.LOGGING.FILTER_PLACEHOLDER}/>
    </SectionActions>
    <SectionHeader>
      <Button variant="primary" onClick={onCreate}>
        <NxFontAwesomeIcon icon={faPlusCircle}/>
        <span>{UIStrings.LOGGING.CREATE_BUTTON}</span>
      </Button>
      <Button onClick={reset}>
        <NxFontAwesomeIcon icon={faRedo}/>
        <span>{UIStrings.LOGGING.RESET_ALL_BUTTON}</span>
      </Button>
    </SectionHeader>
    <NxTable>
      <NxTableHead>
        <NxTableRow>
          <NxTableCell onClick={() => send('SORT_BY_NAME')} isSortable sortDir={nameSortDir}>
            {UIStrings.LOGGING.NAME_LABEL}
          </NxTableCell>
          <NxTableCell onClick={() => send('SORT_BY_LEVEL')} isSortable sortDir={levelSortDir}>
            {UIStrings.LOGGING.LEVEL_LABEL}
          </NxTableCell>
          <NxTableCell hasIcon />
        </NxTableRow>
      </NxTableHead>
      <NxTableBody isLoading={isLoading} error={error}>
        {data.map(({name, level}) => (
            <NxTableRow key={name} onClick={() => onEdit(name)} isClickable>
              <NxTableCell>{name}</NxTableCell>
              <NxTableCell>{level}</NxTableCell>
              <NxTableCell hasIcon><NxFontAwesomeIcon icon={faChevronRight} /></NxTableCell>
            </NxTableRow>
        ))}
      </NxTableBody>
    </NxTable>
  </Section>;
}
