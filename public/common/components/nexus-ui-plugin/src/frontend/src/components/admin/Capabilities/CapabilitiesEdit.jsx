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
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { ContentBody, Page, PageHeader, PageTitle, Section } from '../../layout';
import { useMachine } from '@xstate/react';
import CapabilitiesEditMachine from './CapabilitiesEditMachine';
import {
  NxInfoAlert,
  NxCheckbox,
  NxErrorAlert,
  NxFormGroup,
  NxH2,
  NxLoadWrapper,
  NxStatefulForm,
  NxTile,
  NxButton,
  NxFontAwesomeIcon,
  NxWarningAlert,
  NxGrid,
  NxReadOnly,
} from '@sonatype/react-shared-components';
import { faSpinner, faTrash } from '@fortawesome/free-solid-svg-icons';
import FormUtils from '../../../interface/FormUtils';
import UIStrings from '../../../constants/UIStrings';
import { AdminRouteNames } from '../../../constants/admin/AdminRouteNames';
import DynamicFormField from '../../widgets/DynamicFormField/DynamicFormField';
import CapabilitiesEditDeleteModal from './CapabilitiesEditDeleteModal';
import './Capabilities.scss';
import ExtJS from '../../../interface/ExtJS';
import classNames from 'classnames';
import './CapabilitiesEdit.scss';

const { CAPABILITIES } = UIStrings;

export default function CapabilitiesEdit() {
  const router = useRouter();
  const { params } = useCurrentStateAndParams();
  const id = params.id;

  const [current, send] = useMachine(CapabilitiesEditMachine, {
    devTools: true,
    context: { id },
    actions: {
      onSaveSuccess: () => {
        router.stateService.go(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
      },
      onDeleteSuccess: () => {
        router.stateService.go(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
      },
    },
  });

  const isLoading = current.matches('loading');
  const isLoadError = current.matches('loadError');
  const showDeleteConfirmationModal = current.matches('awaitingDeleteConfirmation');

  const { capability, capabilityType, loadError, deleteError } = current.context;

  const isDeleting = current.matches('confirmDelete') || current.matches('delete');
  const isDeleteError = !!deleteError;

  const errorMessage = isLoadError ? loadError || 'An error occurred' : null;

  const handleRetry = () => {
    send({ type: 'RETRY' });
  };

  const updateField = (name, value) => {
    send({
      type: 'UPDATE',
      data: {
        [name]: value,
      },
    });
  };

  const handleEnabledChange = value => {
    updateField('enabled', value);
  };

  const handleCancel = () => {
    router.stateService.go(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
  };

  const onDeleteClicked = () => {
    send({ type: 'SHOW_DELETE_MODAL' });
  };

  const onConfirmDelete = () => {
    send({ type: 'CONFIRM_DELETE' });
  };

  const onDeleteCanceled = () => {
    send({ type: 'CANCEL_DELETE' });
  };

  const buildTitle = () => {
    if (!capability) {
      return '';
    }

    if (!!capability.description) {
      return `${capability.typeName} - ${capability.description}`;
    } else {
      return capability.typeName;
    }
  };

  const canEdit = ExtJS.checkPermission('nexus:capabilities:edit');
  const canDelete = ExtJS.checkPermission('nexus:capabilities:delete');

  return (
    <Page className='nxrm-capabilities-edit-page'>
      <PageHeader>
        <PageTitle text={buildTitle()} description={CAPABILITIES.EDIT.DESCRIPTION} />
      </PageHeader>

      <ContentBody>
        <NxLoadWrapper loading={isLoading} error={errorMessage} retryHandler={handleRetry}>
          <CapabilitiesEditDeleteModal
            showModal={showDeleteConfirmationModal}
            deleteWarningMessage={capability?.deleteWarningMessage}
            onConfirmDelete={onConfirmDelete}
            onDeleteCanceled={onDeleteCanceled}
          />

          {isDeleteError && <NxErrorAlert>{deleteError || 'An error occurred deleting the capability.'}</NxErrorAlert>}
          <SummaryTile canEdit={canEdit} capability={capability} capabilityType={capabilityType} />
          <Section data-testid='capabilities-form-section'>
            <NxStatefulForm
              {...FormUtils.formProps(current, send)}
              onCancel={handleCancel}
              data-analytics-id='nxrm-edit-capabilities-form'
              className='nxrm-capabilities-form'
              submitBtnClasses={!canEdit && 'nx-read-only-submit-button'}
              additionalFooterBtns={canEdit && canDelete && DeleteButton({ capability, isDeleting, onDeleteClicked })}
            >
              <NxTile.Subsection>
                <NxTile.SubsectionHeader>
                  <NxH2>{CAPABILITIES.EDIT.FORM.TITLE}</NxH2>
                </NxTile.SubsectionHeader>
              </NxTile.Subsection>

              <NxTile.Subsection>
                {canEdit ? (
                  <NxFormGroup label={CAPABILITIES.EDIT.FORM.CAPABILITY_STATE}>
                    <NxCheckbox
                      {...FormUtils.checkboxProps('enabled', current, false)}
                      onChange={handleEnabledChange}
                    >
                      {CAPABILITIES.EDIT.FORM.ENABLED_LABEL}
                    </NxCheckbox>
                  </NxFormGroup>
                ) : (
                  <NxReadOnly>
                    <NxReadOnly.Label>{CAPABILITIES.EDIT.FORM.CAPABILITY_STATE}</NxReadOnly.Label>
                    <NxReadOnly.Data>
                      {capability?.enabled ? 'Enabled' : 'Disabled'}
                    </NxReadOnly.Data>
                  </NxReadOnly>
                )}
              </NxTile.Subsection>

              {capabilityType && capabilityType.formFields && capabilityType.formFields.length > 0 && (
                <NxTile.Subsection data-testid='form-fields'>
                  {capabilityType.formFields.map(field => (
                    <DynamicFormField
                      key={`${capabilityType.id}-${field.id}`}
                      id={field.id}
                      current={current}
                      initialValue={capability?.properties?.[field.id] ?? field.initialValue}
                      onChange={updateField}
                      dynamicProps={field}
                      readOnly={!canEdit}
                    />
                  ))}
                </NxTile.Subsection>
              )}
              <NxTile.Subsection>
                <DynamicFormField
                  id='notes'
                  current={current}
                  initialValue={capability?.notes ?? ''}
                  onChange={updateField}
                  dynamicProps={{
                    type: 'text-area',
                    label: CAPABILITIES.EDIT.FORM.NOTES,
                    name: 'notes',
                    required: false,
                    helpText: CAPABILITIES.EDIT.FORM.NOTES_HELP_TEXT,
                    attributes: {
                      long: true,
                    },
                  }}
                  readOnly={!canEdit}
                />
              </NxTile.Subsection>
            </NxStatefulForm>
          </Section>
        </NxLoadWrapper>
      </ContentBody>
    </Page>
  );
}

function SummaryTile({ canEdit, capability, capabilityType }) {
  const inactiveWarning = capability.enabled && !capability?.active ? capability.stateDescription : null;

  const category = capability?.tags?.Category;

  return (
    <Section>
      {!canEdit && <NxInfoAlert>{UIStrings.SETTINGS.READ_ONLY.WARNING}</NxInfoAlert>}
      <NxTile.Header>
        <div className='nx-tile-header__title'>
          <NxH2>Capability Summary</NxH2>
        </div>
      </NxTile.Header>

      {inactiveWarning && <NxWarningAlert role='alert'>{inactiveWarning}</NxWarningAlert>}

      <NxTile.Content>
        <NxGrid.Row>
          <ReadOnlyColumn label={CAPABILITIES.EDIT.LABELS.TYPE} value={capability.typeName} />

          <ReadOnlyColumn label={CAPABILITIES.EDIT.LABELS.DESCRIPTION} value={capability.description} />

          <ReadOnlyColumn
            className='summary-tile_state'
            label={CAPABILITIES.EDIT.LABELS.STATE}
            value={capability.state}
          />

          <ReadOnlyColumn label={CAPABILITIES.EDIT.LABELS.CATEGORY} value={category} />
        </NxGrid.Row>

        <NxGrid.Row>
          <ReadOnlyColumn
            gridColNum={100}
            label={CAPABILITIES.EDIT.LABELS.STATUS}
            value={capability.status}
            renderAsHtml={true}
          />
        </NxGrid.Row>

        <NxGrid.Row>
          <ReadOnlyColumn
            gridColNum={100}
            label={CAPABILITIES.EDIT.LABELS.ABOUT}
            value={capabilityType?.about}
            renderAsHtml={true}
          />
        </NxGrid.Row>
      </NxTile.Content>
    </Section>
  );
}

function ReadOnlyColumn({ label, value, gridColNum = 25, renderAsHtml = false, className = '' }) {
  value = value || 'N/A';

  return (
    <NxGrid.Column className={classNames(`nx-grid-col--${gridColNum}`, className)}>
      <NxReadOnly>
        <NxReadOnly.Item>
          <NxReadOnly.Label data-testid={label} id={label}>
            {label}
          </NxReadOnly.Label>

          {renderAsHtml ? (
            <NxReadOnly.Data aria-labelledby={label} dangerouslySetInnerHTML={{ __html: value }} />
          ) : (
            <NxReadOnly.Data aria-labelledby={label}>{value}</NxReadOnly.Data>
          )}
        </NxReadOnly.Item>
      </NxReadOnly>
    </NxGrid.Column>
  );
}

function DeleteButton({ capability, isDeleting, onDeleteClicked }) {
  const hasEditPermissions = ExtJS.checkPermission('nexus:capabilities:delete');

  // show the button disabled if the user does not have permission or a delete operation is pending
  const disabled = !hasEditPermissions || isDeleting;

  // Don't render button if capability is not loaded yet or if it's a system capability
  if (!capability || capability.isSystem) {
    // hide the button all together if it's a system capability or capability is not loaded
    return null;
  }

  return (
    <NxButton
      variant='tertiary'
      disabled={disabled}
      data-analytics-id='nxrm-capabilities-edit-page-delete-button'
      onClick={onDeleteClicked}
    >
      {isDeleting ? <NxFontAwesomeIcon icon={faSpinner} spin /> : <NxFontAwesomeIcon icon={faTrash} />}
      <span>Delete</span>
    </NxButton>
  );
}
