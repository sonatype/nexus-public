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
import {useActor} from '@xstate/react';
import {NxFilterInput, NxTable} from '@sonatype/react-shared-components';
import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  Section,
  ListMachineUtils
} from '@sonatype/nexus-ui-plugin';
import {faPuzzlePiece} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';

export default function BundlesList ({ onEdit, service }) {
  const [current, send] = useActor(service);
  const isLoading = current.matches('loading');
  const data = current.context.data;
  const filterText = current.context.filter;
  const error = current.context.error;

  const idSortDir = ListMachineUtils.getSortDirection('id', current.context);
  const stateSortDir = ListMachineUtils.getSortDirection('state', current.context);
  const levelSortDir = ListMachineUtils.getSortDirection('startLevel', current.context);
  const nameSortDir = ListMachineUtils.getSortDirection('name', current.context);
  const versionSortDir = ListMachineUtils.getSortDirection('version', current.context);

  function filter(value) {
    send({type: 'FILTER', filter: value});
  }

  return (
    <Page>
      <PageHeader>
        <PageTitle icon={faPuzzlePiece} {...UIStrings.BUNDLES.MENU}/>
      </PageHeader>
      <ContentBody>
        <Section>
          <NxFilterInput placeholder="Enter a filter value" onChange={filter} value={filterText}/>
          <NxTable>
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell onClick={() => send({type: 'SORT_BY_ID'})} isSortable sortDir={idSortDir}>
                  {UIStrings.BUNDLES.LIST.ID_LABEL}
                </NxTable.Cell>
                <NxTable.Cell onClick={() => send({type: 'SORT_BY_STATE'})} isSortable sortDir={stateSortDir}>
                  {UIStrings.BUNDLES.LIST.STATE_LABEL}
                </NxTable.Cell>
                <NxTable.Cell onClick={() => send({type: 'SORT_BY_START_LEVEL'})} isSortable sortDir={levelSortDir}>
                  {UIStrings.BUNDLES.LIST.LEVEL_LABEL}
                </NxTable.Cell>
                <NxTable.Cell onClick={() => send({type: 'SORT_BY_NAME'})} isSortable sortDir={nameSortDir}>
                  {UIStrings.BUNDLES.LIST.NAME_LABEL}
                </NxTable.Cell>
                <NxTable.Cell onClick={() => send({type: 'SORT_BY_VERSION'})} isSortable sortDir={versionSortDir}>
                  {UIStrings.BUNDLES.LIST.VERSION_LABEL}
                </NxTable.Cell>
                <NxTable.Cell chevron />
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body isLoading={isLoading} error={error}>
              {data.map(({id, state, startLevel, name, version}) =>
                <NxTable.Row key={id} isClickable onClick={() => onEdit(id)}>
                  <NxTable.Cell>{id}</NxTable.Cell>
                  <NxTable.Cell>{state}</NxTable.Cell>
                  <NxTable.Cell>{startLevel}</NxTable.Cell>
                  <NxTable.Cell>{name}</NxTable.Cell>
                  <NxTable.Cell>{version}</NxTable.Cell>
                  <NxTable.Cell chevron />
                </NxTable.Row>
              )}
            </NxTable.Body>
          </NxTable>
        </Section>
      </ContentBody>
    </Page>
  );
};
