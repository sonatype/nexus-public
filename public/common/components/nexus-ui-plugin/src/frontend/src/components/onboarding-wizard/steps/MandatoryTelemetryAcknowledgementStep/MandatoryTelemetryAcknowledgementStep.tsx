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
import { Callout, Checkbox } from '@radix-ui/themes';
import { AlertCircle, CheckCircle2, ExternalLink, Info } from 'lucide-react';

import UIStrings from '../../UIStrings';
import {
  type BaselineTelemetryStatus,
  type TelemetryUploadError,
  useMandatoryTelemetryAcknowledgementStep,
} from './useMandatoryTelemetryAcknowledgementStep';

import './MandatoryTelemetryAcknowledgementStep.scss';

const CHECKBOX_ID = 'mandatory-telemetry-acknowledgement-step__acknowledge-checkbox';

const STATUS_ICON_CLASS = 'mandatory-telemetry-acknowledgement-step__status-icon';

/**
 * The "Last upload error" line for a client error (HTTP 4XX), signature verification failure or
 * connection error. For a failure recorded without a status code the backend sends the default message
 * for its type, so this line matches what Repository Health Check reports.
 */
function describeUploadError({ httpStatusCode, message }: TelemetryUploadError): string {
  const { STATUS } = UIStrings.ONBOARDING_WIZARD.MANDATORY_TELEMETRY_ACKNOWLEDGEMENT;
  if (httpStatusCode === null) {
    return STATUS.LAST_ERROR_WITHOUT_STATUS_CODE.replace('{message}', () => message);
  }
  const code = String(httpStatusCode);
  if (!message) {
    return STATUS.LAST_ERROR_WITHOUT_MESSAGE.replace('{httpStatusCode}', code);
  }
  // A function replacer: the message is server text, and a string replacement would read `$&`,
  // `$'` and the like in it as substitution patterns.
  return STATUS.LAST_ERROR.replace('{httpStatusCode}', code).replace('{message}', () => message);
}

/**
 * The status callout's icon. Only the icon changes between states -- the callout keeps one neutral
 * surface throughout, so the three states read as one recurring element rather than three different
 * banners, and the reader's eye lands on the date rather than on a colour change.
 */
function statusIcon(status: BaselineTelemetryStatus): React.ReactNode {
  switch (status) {
    case 'active':
      return <CheckCircle2 size={16} className={`${STATUS_ICON_CLASS} ${STATUS_ICON_CLASS}--active`} />;
    case 'never':
      return <Info size={16} className={`${STATUS_ICON_CLASS} ${STATUS_ICON_CLASS}--never`} />;
    default:
      return <AlertCircle size={16} className={`${STATUS_ICON_CLASS} ${STATUS_ICON_CLASS}--failing`} />;
  }
}

/**
 * The status callout's body copy, or null for a state that needs none.
 *
 * Active gets nothing: the headline date says everything. The other two explain what the date means
 * and what to check next.
 */
function resolveStatusBody(
  status: BaselineTelemetryStatus,
  gracePeriodDays: number | null
): string | null {
  const { STATUS } = UIStrings.ONBOARDING_WIZARD.MANDATORY_TELEMETRY_ACKNOWLEDGEMENT;

  if (status === 'active') {
    return null;
  }

  if (status === 'never') {
    return STATUS.NEVER_BODY;
  }

  // uploadIssue. The grace period length is edition-dependent, so the day count only appears when
  // the backend reported one.
  if (gracePeriodDays === null) {
    return STATUS.FAILING_BODY_WITHOUT_GRACE_PERIOD;
  }

  return STATUS.FAILING_BODY.replace('{gracePeriodDays}', String(gracePeriodDays));
}

/**
 * MandatoryTelemetryAcknowledgementStep informs administrators that telemetry became mandatory in
 * 3.97.0 and records that they were told.
 *
 * This step:
 * - Explains what is uploaded and links the base telemetry documentation
 * - Reports the Baseline Telemetry status -- Active, Never received or Upload issue -- with the
 *   last successful upload date and the latest client error (HTTP 4XX), signature failure or connection error,
 *   mirroring what
 *   Repository Health Check shows
 * - Displays edition-specific grace period length from backend state
 * - Requires the user to check a checkbox to acknowledge the requirement
 * - Disables the Next button until acknowledged
 * - Submits the acknowledgement to the backend on form submission
 * - Shows submission failures inline; the Next button stays enabled so a second click re-POSTs
 */
export default function MandatoryTelemetryAcknowledgementStep(): JSX.Element {
  const {
    acknowledged,
    success,
    error,
    gracePeriodDays,
    status,
    lastSuccessfulUpload,
    lastUploadError,
    onAcknowledgementChange,
  } = useMandatoryTelemetryAcknowledgementStep();

  const {
    TITLE,
    DESCRIPTION,
    LEARN_MORE_LABEL,
    LEARN_MORE_HREF,
    READ_ONLY_WARNING,
    READ_ONLY_WARNING_WITHOUT_GRACE_PERIOD,
    CHECKBOX_LABEL,
    STATUS,
  } = UIStrings.ONBOARDING_WIZARD.MANDATORY_TELEMETRY_ACKNOWLEDGEMENT;

  const readOnlyWarning =
    gracePeriodDays !== null
      ? READ_ONLY_WARNING.replace('{gracePeriodDays}', String(gracePeriodDays))
      : READ_ONLY_WARNING_WITHOUT_GRACE_PERIOD;

  const statusHeadline = STATUS.HEADLINE.replace(
    '{lastSuccessfulUpload}',
    lastSuccessfulUpload ?? STATUS.NEVER_LABEL
  );

  const statusBody = resolveStatusBody(status, gracePeriodDays);

  return (
    <div
      className="mandatory-telemetry-acknowledgement-step"
      data-testid="mandatory-telemetry-acknowledgement-step"
    >
      <h2 className="mandatory-telemetry-acknowledgement-step__title">{TITLE}</h2>
      <p className="mandatory-telemetry-acknowledgement-step__description">{DESCRIPTION}</p>

      <a
        className="mandatory-telemetry-acknowledgement-step__learn-more"
        href={LEARN_MORE_HREF}
        target="_blank"
        rel="noopener noreferrer"
        data-testid="mandatory-telemetry-acknowledgement-step__learn-more"
      >
        {LEARN_MORE_LABEL}
        <ExternalLink size={14} aria-hidden="true" />
      </a>

      <Callout.Root
        color="blue"
        variant="surface"
        className="mandatory-telemetry-acknowledgement-step__status"
        data-testid="mandatory-telemetry-acknowledgement-step__status"
        data-status={status}
      >
        <Callout.Icon>{statusIcon(status)}</Callout.Icon>
        <Callout.Text>
          <span className="mandatory-telemetry-acknowledgement-step__status-headline">
            {statusHeadline}
          </span>
          {statusBody && (
            <span className="mandatory-telemetry-acknowledgement-step__status-body">
              {statusBody}
            </span>
          )}
          {lastUploadError && (
            <span
              className="mandatory-telemetry-acknowledgement-step__status-error"
              data-testid="mandatory-telemetry-acknowledgement-step__status-error"
            >
              {describeUploadError(lastUploadError)}
            </span>
          )}
        </Callout.Text>
      </Callout.Root>

      <Callout.Root
        color="amber"
        variant="surface"
        className="mandatory-telemetry-acknowledgement-step__warning"
        data-testid="mandatory-telemetry-acknowledgement-step__warning"
      >
        <Callout.Icon>
          <AlertCircle size={16} />
        </Callout.Icon>
        <Callout.Text>{readOnlyWarning}</Callout.Text>
      </Callout.Root>

      {success && (
        <Callout.Root
          color="green"
          role="status"
          data-testid="mandatory-telemetry-acknowledgement-step__success"
        >
          <Callout.Icon>
            <CheckCircle2 size={16} />
          </Callout.Icon>
          <Callout.Text>
            {UIStrings.ONBOARDING_WIZARD.MANDATORY_TELEMETRY_ACKNOWLEDGEMENT.SUCCESS_MESSAGE}
          </Callout.Text>
        </Callout.Root>
      )}
      {error && (
        <div className="mandatory-telemetry-acknowledgement-step__error">
          <Callout.Root
            color="red"
            role="alert"
            data-testid="mandatory-telemetry-acknowledgement-step__error"
          >
            <Callout.Icon>
              <AlertCircle size={16} />
            </Callout.Icon>
            <Callout.Text>{error}</Callout.Text>
          </Callout.Root>
        </div>
      )}

      <hr className="mandatory-telemetry-acknowledgement-step__divider" />

      <div className="mandatory-telemetry-acknowledgement-step__acknowledgement">
        <label
          htmlFor={CHECKBOX_ID}
          className="mandatory-telemetry-acknowledgement-step__checkbox-label"
        >
          <Checkbox
            id={CHECKBOX_ID}
            checked={acknowledged}
            onCheckedChange={(checked) => onAcknowledgementChange(checked === true)}
            data-testid="mandatory-telemetry-acknowledgement-step__acknowledge-checkbox"
          />
          <span>{CHECKBOX_LABEL}</span>
        </label>
      </div>
    </div>
  );
}
