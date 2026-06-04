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
import { compose, propOr, sortBy, toLower } from 'ramda';
import { useMachine } from '@xstate/react';
import { useRouter } from '@uirouter/react';
import { NxForm, NxFormGroup, NxFormSelect, NxH2, NxLoadWrapper, NxP, NxTile } from '@sonatype/react-shared-components';
import { ContentBody, Page, PageHeader, PageTitle, Section } from '../../layout';
import UIStrings from '../../../constants/UIStrings';
import { AdminRouteNames } from '../../../constants/admin/AdminRouteNames';
import FormUtils from '../../../interface/FormUtils';
import CapabilitiesCreateMachine from './CapabilitiesCreateMachine';
import DynamicFormField from '../../widgets/DynamicFormField/DynamicFormField';
import './Capabilities.scss';

const { CAPABILITIES } = UIStrings;

export default function CapabilitiesCreate() {
  const router = useRouter();
  const [current, send] = useMachine(CapabilitiesCreateMachine, {
    actions: {
      onSaveSuccess: () => {
        router.stateService.go(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
      },
    },
    devTools: false,
  });

  const {
    types: rawTypes = [],
    loadError,
    selectedType,
    shouldShowErrors,
    hasFields,
    allFieldsOptional,
  } = current.context;
  const isLoading = current.matches('loading');

  // Sort types alphabetically by name using Ramda
  const types = sortBy(compose(toLower, propOr('', 'name')), rawTypes);

  const handleRetry = () => {
    send({ type: 'RETRY' });
  };

  const handleTypeChange = value => {
    const type = types?.find(type => type.id === value);
    if (type) {
      send({
        type: 'SET_SELECTED_TYPE',
        selectedType: type,
      });
    } else {
      send({
        type: 'SET_SELECTED_TYPE',
        selectedType: null,
      });
    }
  };

  const updateField = (name, value) => {
    send({
      type: 'UPDATE',
      data: {
        [name]: value,
      },
    });
  };

  const handleCancel = () => {
    router.stateService.go(AdminRouteNames.SYSTEM.CAPABILITIES.LIST);
  };

  // Custom formProps that handles pristine state for forms with all optional fields
  const baseFormProps = FormUtils.formProps(current, send);
  const shouldAllowPristineSave = !hasFields || allFieldsOptional;

  const formProps = {
    ...baseFormProps,
    // Override validationErrors to not show pristine tooltip if pristine saves are allowed
    validationErrors:
      shouldAllowPristineSave && current.context.isPristine && !FormUtils.isInvalid(current.context.validationErrors)
        ? null
        : baseFormProps.validationErrors,
  };

  const handleSubmit = () => {
    formProps.onSubmit(); // Calls send({ type: 'SAVE' })
  };

  return (
    <Page>
      <PageHeader>
        <PageTitle text={CAPABILITIES.CREATE.TITLE} description={CAPABILITIES.CREATE.DESCRIPTION} />
      </PageHeader>
      <ContentBody>
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={handleRetry}>
          <Section>
            <NxForm
              {...formProps}
              showValidationErrors={shouldShowErrors}
              onSubmit={handleSubmit}
              onCancel={handleCancel}
              data-analytics-id='nxrm-create-capabilities-form'
              className='nxrm-capabilities-form'
            >
              <NxTile.Subsection>
                <NxTile.SubsectionHeader>
                  <NxH2>{CAPABILITIES.CREATE.FORM.TITLE}</NxH2>
                </NxTile.SubsectionHeader>
              </NxTile.Subsection>
              <NxTile.Subsection>
                <NxFormGroup label={CAPABILITIES.CREATE.FORM.SELECT_TYPE} isRequired>
                  <NxFormSelect value={selectedType?.id || ''} onChange={handleTypeChange}>
                    <option value='' disabled={!!selectedType}>
                      {CAPABILITIES.CREATE.FORM.SELECT_TYPE}
                    </option>
                    {types.map(type => (
                      <option key={type.id} value={type.id}>
                        {type.name}
                      </option>
                    ))}
                  </NxFormSelect>
                </NxFormGroup>
              </NxTile.Subsection>
              {selectedType && (
                <>
                  <NxTile.Subsection>
                    <NxTile.SubsectionHeader>
                      <NxH2>{selectedType.name}</NxH2>
                    </NxTile.SubsectionHeader>
                    <NxP dangerouslySetInnerHTML={{ __html: selectedType.about }} />
                  </NxTile.Subsection>
                  {selectedType.formFields && selectedType.formFields.length > 0 && (
                    <NxTile.Subsection data-testid='form-fields'>
                      {selectedType.formFields.map(field => (
                        <DynamicFormField
                          key={`${selectedType.id}-${field.id}`}
                          id={field.id}
                          current={current}
                          initialValue={field.initialValue}
                          onChange={updateField}
                          dynamicProps={field}
                        />
                      ))}
                    </NxTile.Subsection>
                  )}
                </>
              )}
            </NxForm>
          </Section>
        </NxLoadWrapper>
      </ContentBody>
    </Page>
  );
}
