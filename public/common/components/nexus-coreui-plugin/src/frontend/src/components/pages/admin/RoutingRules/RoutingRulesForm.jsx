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
import React, { useCallback } from 'react';
import {useMachine} from '@xstate/react';

import {FormUtils} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxErrorAlert,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormSelect,
  NxInfoAlert,
  NxLoadWrapper,
  NxStatefulForm,
  NxSuccessAlert,
  NxTextInput,
  NxTooltip,
} from '@sonatype/react-shared-components';

import {
  ContentBody,
  PageHeader,
  PageTitle,
  Section,
  Page
} from '@sonatype/nexus-ui-plugin';

import RoutingRuleFormMachine from './RoutingRulesFormMachine';

import UIStrings from '../../../../constants/UIStrings';
import {faPlus, faTrash} from '@fortawesome/free-solid-svg-icons';
import RoutingRulesPreview from './RoutingRulesFormPreview';
import RoutingRuleMatcherRow from './RoutingRuleMatcherRow';
import { DeleteConfirmationModal } from '../../../shared/modals/DeleteConfirmationModal';
import { isEmpty } from 'ramda';

import './RoutingRules.scss';
import { ROUTE_NAMES } from '../../../../routerConfig/routeNames/routeNames';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';


const {ROUTING_RULES, SETTINGS} = UIStrings;

export default function RoutingRulesForm() {
  const router = useRouter();
  const onDone = useCallback(() => router.stateService.go(ROUTE_NAMES.ADMIN.REPOSITORY.ROUTINGRULES.LIST));
  const { params } = useCurrentStateAndParams();
  const itemId = params?.itemId;
  const [current, send] = useMachine(RoutingRuleFormMachine, {
    context: {
      pristineData: {
        name: itemId
      }
    },

    actions: {
      onCancel: onDone,
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone
    },

    devTools: true
  });

  const {data, path, testError, testResult, showDeleteModal} = current.context;
  const isTesting = current.matches('testing');
  const hasData = data && !isEmpty(data);
  const assignedRepositoryCount = data?.assignedRepositoryCount || 0;
  const assignedRepositoryNames = data?.assignedRepositoryNames || [];
  const isEdit = Boolean(itemId);
  const hasAssignedRepositories = assignedRepositoryCount > 0;

  function addMatcher() {
    send({type: 'ADD_MATCHER'});
  }

  function removeMatcher(index) {
    send({type: 'REMOVE_MATCHER', index});
  }

  function updateMatcher(value, index) {
    send({type: 'UPDATE_MATCHER', index, value})
  }

  function update(value) {
    send({type: 'UPDATE', data: {mode: value}});
  }

  function cancel() {
    send({type: 'CANCEL'});
  }

  function remove(e) {
    if (!e.currentTarget.classList.contains('disabled')) {
      send({type: 'CONFIRM_DELETE'});
    }
  }

  function updatePath(value) {
    send({type: 'UPDATE_PATH', path: value});
  }

  function test() {
    send({type: 'TEST'});
  }

  return <Page className="nxrm-routing-rules">
    {isEdit &&
    <NxInfoAlert>
      {!hasAssignedRepositories && <span dangerouslySetInnerHTML={{__html: ROUTING_RULES.FORM.UNUSED}}/>}
      {hasAssignedRepositories && <span dangerouslySetInnerHTML={{__html: ROUTING_RULES.FORM.USED_BY(assignedRepositoryNames)}}/>}
    </NxInfoAlert>}
    <PageHeader>
      <PageTitle text={itemId ? ROUTING_RULES.FORM.EDIT_TITLE : ROUTING_RULES.FORM.CREATE_TITLE}/>
    </PageHeader>
    <ContentBody>
      <Section className="nxrm-routing-rules-form">
        <NxStatefulForm
            {...FormUtils.formProps(current, send)}
            onCancel={cancel}
            submitBtnText={itemId ? SETTINGS.SAVE_BUTTON_LABEL : ROUTING_RULES.FORM.CREATE_BUTTON}
            additionalFooterBtns={itemId &&
              <NxTooltip title={assignedRepositoryCount > 0 ? ROUTING_RULES.FORM.CANNOT_DELETE(assignedRepositoryNames) : undefined}>
                <NxButton type="button" variant="tertiary" onClick={remove} className={hasAssignedRepositories && 'disabled'}>
                  <NxFontAwesomeIcon icon={faTrash}/>
                  <span>{SETTINGS.DELETE_BUTTON_LABEL}</span>
                </NxButton>
              </NxTooltip>
            }
        >
          {hasData && <>
            <NxFormGroup label={ROUTING_RULES.FORM.NAME_LABEL} isRequired>
              <NxTextInput
                  {...FormUtils.fieldProps('name', current)}
                  onChange={FormUtils.handleUpdate('name', send)}/>
            </NxFormGroup>
            <NxFormGroup label={ROUTING_RULES.FORM.DESCRIPTION_LABEL}>
              <NxTextInput
                  className="nx-text-input--long"
                  {...FormUtils.fieldProps('description', current)}
                  onChange={FormUtils.handleUpdate('description', send)}/>
            </NxFormGroup>
            <NxFormGroup
                id="nxrm-routing-rules-mode"
                label={ROUTING_RULES.FORM.MODE_LABEL}
                sublabel={ROUTING_RULES.FORM.MODE_DESCRIPTION}>
              <NxFormSelect {...FormUtils.fieldProps('mode', current)} onChange={update}>
                <option value="ALLOW">{ROUTING_RULES.FORM.MODE.ALLOW}</option>
                <option value="BLOCK">{ROUTING_RULES.FORM.MODE.BLOCK}</option>
              </NxFormSelect>
            </NxFormGroup>
            <div className="nx-form-group">
              <div id="matchers-label" className="nx-label">
                <span className="nx-label__text">{ROUTING_RULES.FORM.MATCHERS_LABEL}</span>
              </div>
              <div className="nx-sub-label">{ROUTING_RULES.FORM.MATCHERS_DESCRIPTION}</div>

              {data.matchers?.map((value, index) =>
                  <RoutingRuleMatcherRow
                      key={`matcher-${index}`}
                      label={ROUTING_RULES.FORM.MATCHER_LABEL(index)}
                      {...FormUtils.fieldProps(`matchers[${index}]`, current)}
                      value={value}
                      onChange={(value) => updateMatcher(value, index)}
                      showRemoveButton={data.matchers.length > 1}
                      onRemove={() => removeMatcher(index)}/>)
              }
              <div className="add-matcher">
                <NxButton type="button" variant="tertiary" onClick={addMatcher}>
                  <NxFontAwesomeIcon icon={faPlus}/>
                  <span>{ROUTING_RULES.FORM.ADD_MATCHER_BUTTON}</span>
                </NxButton>
              </div>
            </div>
          </>}
        </NxStatefulForm>
      </Section>

      <Section className="nxrm-routing-rules-preview">
        <RoutingRulesPreview value={path} onChange={updatePath} onTest={test}/>
        <NxLoadWrapper className="preview-result" loading={isTesting} loadError={testError} retryHandler={test}>
          {testResult === true && <NxSuccessAlert>{ROUTING_RULES.FORM.PREVIEW.ALLOWED}</NxSuccessAlert>}
          {testResult === false && <NxErrorAlert>{ROUTING_RULES.FORM.PREVIEW.BLOCKED}</NxErrorAlert>}
        </NxLoadWrapper>
      </Section>
    </ContentBody>

    <DeleteConfirmationModal
      open={showDeleteModal}
      onClose={() => send('HIDE_DELETE_MODAL')}
      onConfirm={() => send('DELETE')}
      entityType="routing rule"
      loading={current.matches('delete')}
    />
  </Page>;
}
