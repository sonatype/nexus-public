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

import {FormUtils, UseNexusTruststore} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxFieldset,
  NxFormGroup,
  NxStatefulForm,
  NxTooltip
} from '@sonatype/react-shared-components';

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  Section
} from '@sonatype/nexus-ui-plugin';

import OAuth2ConfigurationMachine from './OAuth2ConfigurationMachine';
import UIStrings from '../../../../constants/UIStrings';
import './OAuth2Configuration.scss';

import {NxTextInput} from "@sonatype/react-shared-components";

const {FIELDS, LABELS} = UIStrings.OAUTH2_CONFIGURATION;

export default function OAuth2Configuration() {
  const [current, send] = useMachine(OAuth2ConfigurationMachine, {devTools: true});

  const {isPristine} = current.context;

  function discard() {
    send({type: 'RESET'});
  }

  return <Page>
    <PageHeader>
      <PageTitle
        icon={UIStrings.OAUTH2_CONFIGURATION.MENU.icon}
        text={UIStrings.OAUTH2_CONFIGURATION.MENU.text}
        description={UIStrings.OAUTH2_CONFIGURATION.MENU.description}
      />
    </PageHeader>
    <ContentBody className='nxrm-oauth2-configuration'>
      <Section>
        <NxStatefulForm
            {...FormUtils.formProps(current, send)}
            additionalFooterBtns={
              <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
                <NxButton type="button" className={isPristine && 'disabled'} onClick={discard}>
                  {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
                </NxButton>
              </NxTooltip>
            }>
          <NxFieldset label={LABELS.OAUTH_2_0_CONFIGURATION}>
            <div className="nx-sub-label nx-oauth2-required-advice-left">
              {LABELS.SUBLABELS.REQUIRED_FIELDS}
            </div>
          </NxFieldset>
          <NxFieldset label={LABELS.OIDC_SETTINGS}>
            <NxFormGroup label={FIELDS.CLIENT_ID} sublabel={LABELS.SUBLABELS.CLIENT_ID} isRequired>
              <NxTextInput
                  type="password"
                  {...FormUtils.fieldProps('clientId', current)}
                  onChange={FormUtils.handleUpdate('clientId', send)}
                  onBlur={FormUtils.trimOnBlur('clientId', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.CLIENT_SECRET} sublabel={LABELS.SUBLABELS.CLIENT_SECRET} isRequired>
              <NxTextInput
                  type="password"
                  autoComplete="new-password"
                  {...FormUtils.fieldProps('clientSecret', current)}
                  onChange={FormUtils.handleUpdate('clientSecret', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.IDP_AUTHORIZATION_URL} sublabel={LABELS.SUBLABELS.IDP_AUTHORIZATION_URL} isRequired>
              <NxTextInput
                  className="nx-text-input--long"
                  {...FormUtils.fieldProps('idpAuthorizationUrl', current)}
                  onChange={FormUtils.handleUpdate('idpAuthorizationUrl', send)}
                  onBlur={FormUtils.trimOnBlur('idpAuthorizationUrl', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.IDP_LOGOUT_URL} sublabel={LABELS.SUBLABELS.IDP_LOGOUT_URL} isRequired>
              <NxTextInput
                  className="nx-text-input--long"
                  {...FormUtils.fieldProps('idpLogoutUrl', current)}
                  onChange={FormUtils.handleUpdate('idpLogoutUrl', send)}
                  onBlur={FormUtils.trimOnBlur('idpLogoutUrl', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.IDP_TOKEN_URL} sublabel={LABELS.SUBLABELS.IDP_TOKEN_URL} isRequired>
              <NxTextInput
                  className="nx-text-input--long"
                  {...FormUtils.fieldProps('idpTokenUrl', current)}
                  onChange={FormUtils.handleUpdate('idpTokenUrl', send)}
                  onBlur={FormUtils.trimOnBlur('idpTokenUrl', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.IDP_JWKS_URL} sublabel={LABELS.SUBLABELS.IDP_JWKS_URL} isRequired>
              <NxTextInput
                  className="nx-text-input--long"
                  {...FormUtils.fieldProps('idpJwksUrl', current)}
                  onChange={FormUtils.handleUpdate('idpJwksUrl', send)}
                  onBlur={FormUtils.trimOnBlur('idpJwksUrl', send)}
              />
            </NxFormGroup>
            <UseNexusTruststore
                remoteUrl={current.context.data?.idpTokenUrl}
                {...FormUtils.checkboxProps('useTrustStore', current)}
                onChange={FormUtils.handleUpdate('useTrustStore', send)}
            />
          </NxFieldset>

          <NxFieldset label={LABELS.CLAIM_MAPPINGS} isRequired>
            <NxFormGroup label={FIELDS.USERNAME_CLAIM} sublabel={LABELS.SUBLABELS.USERNAME_CLAIM} isRequired>
              <NxTextInput
                  {...FormUtils.fieldProps('usernameClaim', current)}
                  onChange={FormUtils.handleUpdate('usernameClaim', send)}
                  onBlur={FormUtils.trimOnBlur('usernameClaim', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.FIRST_NAME_CLAIM} sublabel={LABELS.SUBLABELS.FIRST_NAME_CLAIM} isRequired>
              <NxTextInput
                  {...FormUtils.fieldProps('firstNameClaim', current)}
                  onChange={FormUtils.handleUpdate('firstNameClaim', send)}
                  onBlur={FormUtils.trimOnBlur('firstNameClaim', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.LAST_NAME_CLAIM} sublabel={LABELS.SUBLABELS.LAST_NAME_CLAIM} isRequired>
              <NxTextInput
                  {...FormUtils.fieldProps('lastNameClaim', current)}
                  onChange={FormUtils.handleUpdate('lastNameClaim', send)}
                  onBlur={FormUtils.trimOnBlur('lastNameClaim', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.EMAIL_CLAIM} sublabel={LABELS.SUBLABELS.EMAIL_CLAIM} isRequired>
              <NxTextInput
                  {...FormUtils.fieldProps('emailClaim', current)}
                  onChange={FormUtils.handleUpdate('emailClaim', send)}
                  onBlur={FormUtils.trimOnBlur('emailClaim', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.GROUPS_CLAIM} sublabel={LABELS.SUBLABELS.GROUPS_CLAIM} isRequired>
              <NxTextInput
                  {...FormUtils.fieldProps('groupsClaim', current)}
                  onChange={FormUtils.handleUpdate('groupsClaim', send)}
                  onBlur={FormUtils.trimOnBlur('groupsClaim', send)}
              />
            </NxFormGroup>
          </NxFieldset>

          <NxFieldset label={LABELS.JWT_SETTINGS}>
            <NxFormGroup label={FIELDS.IDP_JWS_ALGORITHM} sublabel={LABELS.SUBLABELS.IDP_JWS_ALGORITHM} isRequired>
              <NxTextInput
                {...FormUtils.fieldProps('idpJwsAlgorithm', current)}
                onChange={FormUtils.handleUpdate('idpJwsAlgorithm', send)}
                onBlur={FormUtils.trimOnBlur('idpJwsAlgorithm', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.IDP_JWKS} sublabel={LABELS.SUBLABELS.IDP_JWKS}>
              <NxTextInput
                type="textarea"
                className="nx-text-input--long nx-oauth2-text-input-medium"
                {...FormUtils.fieldProps('idpJwks', current)}
                onChange={FormUtils.handleUpdate('idpJwks', send)}
                placeholder={LABELS.OPTIONAL_JWKS}
              />
            </NxFormGroup>
          </NxFieldset>

          <NxFieldset label={LABELS.ADVANCED_SETTINGS}>
            <NxFormGroup label={FIELDS.AUTHORIZATION_CUSTOM_PARAMS} sublabel={LABELS.SUBLABELS.AUTHORIZATION_CUSTOM_PARAMS}>
              <NxTextInput
                type="textarea"
                className="nx-text-input--long nx-oauth2-text-input-medium"
                {...FormUtils.fieldProps('authorizationCustomParams', current)}
                onChange={FormUtils.handleUpdate('authorizationCustomParams', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.TOKEN_REQUEST_CUSTOM_PARAMS} sublabel={LABELS.SUBLABELS.TOKEN_REQUEST_CUSTOM_PARAMS}>
              <NxTextInput
                type="textarea"
                className="nx-text-input--long nx-oauth2-text-input-medium"
                {...FormUtils.fieldProps('tokenRequestCustomParams', current)}
                onChange={FormUtils.handleUpdate('tokenRequestCustomParams', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.EXACT_MATCH_CLAIMS} sublabel={LABELS.SUBLABELS.EXACT_MATCH_CLAIMS}>
              <NxTextInput
                  type="textarea"
                  className="nx-text-input--long nx-oauth2-text-input-medium"
                  {...FormUtils.fieldProps('exactMatchClaims', current)}
                  onChange={FormUtils.handleUpdate('exactMatchClaims', send)}
              />
            </NxFormGroup>
          </NxFieldset>
        </NxStatefulForm>
      </Section>
    </ContentBody>
  </Page>;
}
