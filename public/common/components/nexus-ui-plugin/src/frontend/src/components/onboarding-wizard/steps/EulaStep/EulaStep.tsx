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
import { Button, Callout, Checkbox, Spinner } from '@radix-ui/themes';
import { AlertCircle, CheckCircle2, ExternalLink } from 'lucide-react';

import ExtJS from '../../../../interface/ExtJS';
import UIStrings from '../../UIStrings';
import { useEulaStep } from './useEulaStep';

import './EulaStep.scss';

const CHECKBOX_ID = 'eula-step__accept-checkbox';

/**
 * EulaStep displays the End User License Agreement for acceptance.
 *
 * This step:
 * - Loads the license HTML in an iframe from /CE-LICENSE.html
 * - Requires the user to check a checkbox to accept the agreement
 * - Disables the Next button until accepted
 * - Submits the acceptance to the backend on form submission
 * - Handles errors inline with retry capability
 *
 * Based on the EulaAccept onboarding pattern from ExtJS.
 */
export default function EulaStep(): JSX.Element {
  const { accepted, loading, success, error, errorKind, onAcceptChange, onRetry } = useEulaStep();
  const { COMMUNITY_EULA, ACTIONS } = UIStrings.ONBOARDING_WIZARD;

  // AC #17: only surface an explicit retry button for fetch errors — on
  // submit errors the Next button remains enabled so a second click re-POSTs.
  const showFetchRetry = error !== null && errorKind === 'fetch';

  return (
    <div className="eula-step" data-testid="eula-step">
      <h2 className="eula-step__title">{COMMUNITY_EULA.TITLE}</h2>
      <p className="eula-step__subtitle" data-testid="eula-step__subtitle">
        {COMMUNITY_EULA.SUBTITLE}
      </p>

      {success && (
        <Callout.Root
          color="green"
          role="status"
          data-testid="eula-step__success"
        >
          <Callout.Icon>
            <CheckCircle2 size={16} />
          </Callout.Icon>
          <Callout.Text>{COMMUNITY_EULA.SUCCESS_MESSAGE}</Callout.Text>
        </Callout.Root>
      )}

      {error && (
        <div className="eula-step__error">
          <Callout.Root
            color="red"
            role="alert"
            data-testid="eula-step__error"
          >
            <Callout.Icon>
              <AlertCircle size={16} />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
          {showFetchRetry && (
            <Button
              variant="soft"
              color="red"
              onClick={onRetry}
              className="eula-step__retry-button"
              data-testid="eula-step__retry-button"
            >
              {ACTIONS.RETRY}
            </Button>
          )}
        </div>
      )}

      {loading ? (
        <div className="eula-step__loading" data-testid="eula-step__loading">
          <Spinner size="3" />
        </div>
      ) : (
        <>
          <div className="eula-step__license-container">
            <iframe
              className="eula-step__license-iframe"
              src={ExtJS.ceLicenseUrl()}
              title={COMMUNITY_EULA.IFRAME_TITLE}
              data-testid="eula-step__license-iframe"
            />
          </div>

          <a
            className="eula-step__read-full-license"
            href={ExtJS.ceLicenseUrl()}
            target="_blank"
            rel="noopener noreferrer"
            data-testid="eula-step__read-full-license"
          >
            {COMMUNITY_EULA.READ_FULL_LICENSE_LABEL}
            <ExternalLink size={14} aria-hidden="true" />
          </a>

          <div className="eula-step__acceptance">
            <label htmlFor={CHECKBOX_ID} className="eula-step__checkbox-label">
              <Checkbox
                id={CHECKBOX_ID}
                checked={accepted}
                onCheckedChange={(checked) => onAcceptChange(checked === true)}
                data-testid="eula-step__accept-checkbox"
              />
              <span>{COMMUNITY_EULA.CHECKBOX_LABEL}</span>
            </label>
          </div>
        </>
      )}
    </div>
  );
}
