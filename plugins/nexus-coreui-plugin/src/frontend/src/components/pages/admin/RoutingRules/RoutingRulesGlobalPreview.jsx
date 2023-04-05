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
import React, {Fragment} from 'react';
import {useMachine} from '@xstate/react';
import {faMinusSquare, faPlusSquare} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  FieldWrapper,
  Page,
  PageTitle,
  Section,
  SectionToolbar
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxFormSelect,
  NxLoadWrapper,
  NxModal,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import RoutingRulesPreview from './RoutingRulesFormPreview';
import RoutingRulesStatus from './RoutingRulesStatus';
import RoutingRulesGlobalPreviewMachine from './RoutingRulesGlobalPreviewMachine';

const {ROUTING_RULES} = UIStrings;

export default function RoutingRulesList() {
  const [current, send] = useMachine(RoutingRulesGlobalPreviewMachine, {devTools: true});
  const isLoading = current.matches('preview');
  const isLoadingSelectedRow = current.matches('fetchSelectedRowDetails');
  const viewSelectedRow = isLoadingSelectedRow || current.matches('viewSelectedRow');
  const {error, filter, preview, path, repositories, selectedRule, selectedRowDetails} = current.context;

  function updateRepositories(event) {
    send({type: 'UPDATE_AND_CLEAR', name: 'repositories', value: event.target.value});
  }

  function updatePath(event) {
    send({type: 'UPDATE_AND_CLEAR', name: 'path', value: event.target.value});
  }

  function updateFilter(value) {
    send({type: 'UPDATE', name: 'filter', value});
  }

  function previewHandler() {
    send({type: 'PREVIEW'});
  }

  function selectRow(parent, child) {
    send({type: 'SELECT_ROW', parent, child});
  }

  function toggle(index) {
    send({type: 'TOGGLE', index});
  }

  function closeModal() {
    send({type: 'CLOSE'});
  }

  function retryPreview() {
    send({type: 'PREVIEW'});
  }

  function retryDetails() {
    send({type: 'RETRY'});
  }

  return <Page className="nxrm-routing-rules">
    <PageTitle text={ROUTING_RULES.PREVIEW.TITLE}/>
    <ContentBody className="global-preview">
      <Section>
        <FieldWrapper labelText={ROUTING_RULES.PREVIEW.REPOSITORIES_LABEL}
                      descriptionText={ROUTING_RULES.PREVIEW.REPOSITORIES_DESCRIPTION}>
          <NxFormSelect name="repositories" value={repositories} onChange={updateRepositories}>
            <option value="all">{ROUTING_RULES.PREVIEW.REPOSITORIES.ALL}</option>
            <option value="groups">{ROUTING_RULES.PREVIEW.REPOSITORIES.GROUPS}</option>
            <option value="proxies">{ROUTING_RULES.PREVIEW.REPOSITORIES.PROXIES}</option>
          </NxFormSelect>
        </FieldWrapper>
        <RoutingRulesPreview value={path} onChange={updatePath} onTest={previewHandler} />

        <SectionToolbar>
          <div className="nxrm-spacer"/>
          <NxFilterInput
              id="filter"
              onChange={updateFilter}
              value={filter}
              placeholder={ROUTING_RULES.PREVIEW.FILTER}/>
        </SectionToolbar>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell>{ROUTING_RULES.PREVIEW.COLUMNS.REPOSITORY}</NxTableCell>
              <NxTableCell>{ROUTING_RULES.PREVIEW.COLUMNS.TYPE}</NxTableCell>
              <NxTableCell>{ROUTING_RULES.PREVIEW.COLUMNS.FORMAT}</NxTableCell>
              <NxTableCell>{ROUTING_RULES.PREVIEW.COLUMNS.RULE}</NxTableCell>
              <NxTableCell>{ROUTING_RULES.PREVIEW.COLUMNS.STATUS}</NxTableCell>
              <NxTableCell chevron/>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody isLoading={isLoading} error={error} emptyMessage={ROUTING_RULES.PREVIEW.EMPTY_PREVIEW}
                       retryHandler={retryPreview}>
            {preview?.map(({repository, type, format, rule, allowed, expanded, expandable, children}, index) =>
                <Fragment key={`${index}`}>
                  <NxTableRow isClickable={expandable || !!rule}
                              onClick={() => {
                                if (expandable) {
                                  toggle(index);
                                }
                                else if (!!rule) {
                                  selectRow(index);
                                }
                              }}>
                    <NxTableCell>
                      {expandable && expanded && <NxFontAwesomeIcon icon={faMinusSquare}/>}
                      {expandable && !expanded && <NxFontAwesomeIcon icon={faPlusSquare}/>}
                      <span>{repository}</span>
                    </NxTableCell>
                    <NxTableCell>{type}</NxTableCell>
                    <NxTableCell>{format}</NxTableCell>
                    <NxTableCell>{rule || ROUTING_RULES.PREVIEW.NO_RULE}</NxTableCell>
                    <NxTableCell><RoutingRulesStatus allowed={allowed}/></NxTableCell>
                    <NxTableCell chevron={!!rule}/>
                  </NxTableRow>
                  {expanded ? children?.map(({repository, type, format, rule, allowed}, childIndex) =>
                      <NxTableRow key={`${index}-${childIndex}`}
                                  isClickable={!!rule}
                                  onClick={() => !!rule && selectRow(index, childIndex)}>
                        <NxTableCell className="repository-child">{repository}</NxTableCell>
                        <NxTableCell>{type}</NxTableCell>
                        <NxTableCell>{format}</NxTableCell>
                        <NxTableCell>{rule || ROUTING_RULES.PREVIEW.NO_RULE}</NxTableCell>
                        <NxTableCell><RoutingRulesStatus allowed={allowed}/></NxTableCell>
                        <NxTableCell chevron={!!rule}/>
                      </NxTableRow>
                  ) : null}
                </Fragment>)}
          </NxTableBody>
        </NxTable>
      </Section>

      {viewSelectedRow && <NxModal id="view-selected-routing-rule" onClose={closeModal}>
        <header className="nx-modal-header"><h2 className="nx-h2">{ROUTING_RULES.PREVIEW.DETAILS_TITLE(selectedRule)}</h2></header>
        <div className="nx-modal-content">
          <NxLoadWrapper isLoading={isLoadingSelectedRow} retryHandler={retryDetails}>
            <div className="nx-form-group">
              <h3 id="mode-label" className="nx-label">{ROUTING_RULES.FORM.MODE_LABEL}</h3>
              <p className="nx-p" aria-labelledby="mode-label">{selectedRowDetails?.mode}</p>
            </div>
            <div className="nx-form-group">
              <h3 id="matchers-label" className="nx-label">{ROUTING_RULES.FORM.MATCHERS_LABEL}</h3>
              <ul className="nx-list">
              {selectedRowDetails?.matchers?.map(matcher =>
                  <li className="nx-list__item"><span className="nx-list__text"><code className="nx-code" aria-labelledby="matchers-label">{matcher}</code></span></li>
              )}
              </ul>
            </div>

          </NxLoadWrapper>
        </div>
        <footer className="nx-footer"><NxButton onClick={closeModal}>{UIStrings.CLOSE}</NxButton></footer>
      </NxModal>}
    </ContentBody>
  </Page>;
}
