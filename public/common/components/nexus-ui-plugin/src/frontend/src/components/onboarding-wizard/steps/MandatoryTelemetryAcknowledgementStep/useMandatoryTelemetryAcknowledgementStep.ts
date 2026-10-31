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

import { useCallback, useEffect, useState } from 'react';

import ExtJS from '../../../../interface/ExtJS';
import UIStrings from '../../UIStrings';
import { useWizard } from '../../useWizard';
import { restClient, parseApiError } from '../../../../interface/api';
import { NO_DATE, formatDateTime } from '../../../preview/shared/utils/dateFormat';

const MANDATORY_TELEMETRY_ACKNOWLEDGEMENT_API =
  '/service/rest/v1/system/mandatory-telemetry-acknowledgement';

const TELEMETRY_HEALTH_STATE = 'nexus.telemetry.health';

/**
 * Hold advancement for this many ms after the POST succeeds so the
 * user sees a brief success indicator before the wizard advances.
 * Matches SUCCESS_INDICATOR_DELAY_MS in EulaStep.
 */
export const SUCCESS_INDICATOR_MS = 500;

/**
 * The slice of `nexus.telemetry.health` this step needs. `gracePeriodDays` is the full grace
 * period length, which is edition-dependent (Pro and Community differ), so it has to be read
 * from the backend rather than baked into the copy.
 *
 * Only the mandatory-mode fields appear here: the step is gated on mandatory mode, so the
 * warning-only fields the state contributor can publish are never in play.
 */
interface TelemetryHealth {
  /**
   * Full grace period length in days. Omitted when the license carries the telemetry opt-out
   * entitlement. When absent, the UI shows a day-count-free warning message instead.
   */
  gracePeriodDays?: number;

  /**
   * Timestamp of the most recent successful upload, as `TelemetryHealthCheck` reports it: an
   * ISO-8601 string, or the sentinel `'never'` / `'unknown'`. Absent under an opt-out license.
   * Any value that will not parse as a date reads as "never received".
   */
  lastSuccessfulUpload?: string;

  /**
   * The instance is already in read-only mode: the grace period ran out. Reported as an upload
   * issue like `showWarning`, since the cause and the remedy are the same.
   */
  readOnly?: boolean;

  /** Uploads have failed beyond the threshold and the grace period is running. */
  showWarning?: boolean;

  /**
   * The latest upload failure when it is a client error (HTTP 4XX), a signature verification
   * failure, a TLS certificate failure or a connection error, and no later result superseded it. Absent when there is none; server errors (HTTP 5XX) are never reported.
   */
  lastError?: TelemetryUploadError;
}

/** The kind of failed telemetry upload, as reported by the backend. */
export type TelemetryUploadErrorType = 'CLIENT_ERROR' | 'SIGNATURE_FAILURE' | 'TLS_FAILURE' | 'CONNECTION_ERROR';

const UPLOAD_ERROR_TYPES: readonly string[] = ['CLIENT_ERROR', 'SIGNATURE_FAILURE', 'TLS_FAILURE', 'CONNECTION_ERROR'];

/**
 * A failed telemetry upload: its kind, status code and response message (plain text, may be empty).
 * The status code is null when none was recorded; the message is then the backend's default message
 * for the type, except for a TLS failure, which keeps its own message (cut short) when it has one.
 */
export interface TelemetryUploadError {
  type: TelemetryUploadErrorType;
  httpStatusCode: number | null;
  message: string;
}

/**
 * The three Baseline Telemetry states this step reports.
 *
 * Fewer than `TelemetryHealthCheck` distinguishes, because this step only ever renders with
 * telemetry required: `MandatoryTelemetryAcknowledgementOnboardingItem.applies()` requires
 * `isTelemetryRequired()`. That rules out the health check's "Opted out" and "Not acknowledged"
 * states, and leaves nothing to distinguish read-only mode by: read-only is the end of the same
 * upload failure `uploadIssue` already describes, and both read identically to the administrator
 * being asked to acknowledge the requirement.
 */
export type BaselineTelemetryStatus = 'active' | 'never' | 'uploadIssue';

/**
 * Formats the backend's last-successful-upload timestamp for display, or returns null when the
 * instance has no upload on record.
 *
 * `formatDateTime` absorbs the sentinels: `'never'` and `'unknown'` are not dates, so they come
 * back as NO_DATE alongside a missing or malformed value. Collapsing all of them to null keeps the
 * "no upload on record" decision in one place -- both the `never` status and the "Never" wording
 * follow from this returning null.
 */
function resolveLastSuccessfulUpload(rawTimestamp: string | undefined): string | null {
  const formatted = formatDateTime(rawTimestamp);
  return formatted === NO_DATE ? null : formatted;
}

/**
 * Resolves the displayed status from the backend health state.
 *
 * A failure outranks the date, the same precedence `TelemetryHealthCheck` applies, so an instance
 * that has never uploaded *and* is failing reports the failure rather than a benign "never received
 * yet". `readOnly` and `showWarning` both land on `uploadIssue`: read-only means the grace period
 * this warns about has run out, which is the same problem further along.
 *
 * Absent or empty health resolves to `never` — nothing has been reported, so nothing has been
 * received as far as this step can tell.
 */
function resolveStatus(
  health: TelemetryHealth | undefined,
  lastSuccessfulUpload: string | null
): BaselineTelemetryStatus {
  if (health?.readOnly === true || health?.showWarning === true) {
    return 'uploadIssue';
  }
  return lastSuccessfulUpload === null ? 'never' : 'active';
}

/**
 * The acknowledgement record, shared by the GET response and the POST request body.
 * The backend rejects anything other than `true`, and stores nothing about who acknowledged or when.
 */
interface MandatoryTelemetryAcknowledgementStatus {
  acknowledgement: boolean;
}

export interface UseMandatoryTelemetryAcknowledgementStepResult {
  acknowledged: boolean;
  success: boolean;
  /**
   * Display message for a failed submission, or null when there is nothing to show.
   * The only failure this step can hit is the POST, so — following the EulaStep
   * convention for submit errors — there is no explicit retry: the wizard's Next
   * button stays enabled and a second click re-POSTs.
   */
  error: string | null;
  gracePeriodDays: number | null;
  /** Which of the three Baseline Telemetry states to render. */
  status: BaselineTelemetryStatus;
  /**
   * The last successful upload formatted for display, or null when the instance has none on
   * record. Null is what makes the status `never`, so the view can render the "Never" wording
   * without re-deriving it.
   */
  lastSuccessfulUpload: string | null;
  /**
   * The latest client error (HTTP 4XX), signature failure or connection error -- status code and response message -- or
   * null when there is none. Mirrors the "Last upload error" Repository Health Check reports.
   */
  lastUploadError: TelemetryUploadError | null;
  onAcknowledgementChange: (checked: boolean) => void;
}

/**
 * useMandatoryTelemetryAcknowledgementStep - custom hook for the mandatory telemetry step.
 *
 * Manages the acknowledgement lifecycle:
 * 0. Resolves the Baseline Telemetry status and last successful upload date from backend state
 * 1. Reads grace period from backend state for UI display
 * 2. Tracks checkbox state for form validity
 * 3. Submits acknowledgement via POST when the wizard advances
 * 4. Surfaces submission failures inline, leaving re-submission to the wizard's Next button
 *
 * Uses the onError hook from the wizard to prevent transition
 * to issueOccurred state, allowing inline error recovery.
 */
export function useMandatoryTelemetryAcknowledgementStep(): UseMandatoryTelemetryAcknowledgementStepResult {
  const { registerStep } = useWizard();
  const [acknowledged, setAcknowledged] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const telemetryHealth = ExtJS.useState(() =>
    ExtJS.state()?.getValue?.(TELEMETRY_HEALTH_STATE)
  ) as TelemetryHealth | undefined;

  const gracePeriodDays =
    typeof telemetryHealth?.gracePeriodDays === 'number' &&
    telemetryHealth.gracePeriodDays > 0
      ? telemetryHealth.gracePeriodDays
      : null;

  const lastSuccessfulUpload = resolveLastSuccessfulUpload(telemetryHealth?.lastSuccessfulUpload);

  const status = resolveStatus(telemetryHealth, lastSuccessfulUpload);

  const lastError = telemetryHealth?.lastError;
  const lastUploadError: TelemetryUploadError | null =
    lastError != null &&
    typeof lastError === 'object' &&
    UPLOAD_ERROR_TYPES.includes(lastError.type) &&
    // Without a status code there is nothing to show unless the backend sent the default message.
    (typeof lastError.httpStatusCode === 'number' ||
      (typeof lastError.message === 'string' && lastError.message.trim() !== ''))
      ? {
          type: lastError.type,
          httpStatusCode: typeof lastError.httpStatusCode === 'number' ? lastError.httpStatusCode : null,
          message: typeof lastError.message === 'string' ? lastError.message.trim() : '',
        }
      : null;

  const submitAcknowledgement = useCallback(async (): Promise<void> => {
    setIsSubmitting(true);
    setError(null);

    try {
      const body: MandatoryTelemetryAcknowledgementStatus = { acknowledgement: true };
      await restClient.post(MANDATORY_TELEMETRY_ACKNOWLEDGEMENT_API, body);
      setSuccess(true);

      // Hold briefly so user sees the success indicator
      await new Promise((resolve) => setTimeout(resolve, SUCCESS_INDICATOR_MS));
    } catch (err: unknown) {
      // Prefer the backend's message, as EulaStep does, and fall back to the generic
      // copy when the failure carried nothing displayable.
      const message =
        parseApiError(err).message ||
        UIStrings.ONBOARDING_WIZARD.MANDATORY_TELEMETRY_ACKNOWLEDGEMENT.SUBMISSION_ERROR;
      setError(message);
      throw new Error(message);
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  const onError = useCallback(() => {
    // Empty on purpose: recoverable-error contract.
    // The hook has already captured the display message in state.error
    // and the view renders the banner from that state.
    // This registration exists so the wizard chrome stays in stepReady
    // instead of terminating via STEP_FAILED / issueOccurred.
  }, []);

  // Register the step with the wizard
  useEffect(() => {
    registerStep({
      valid: acknowledged && !isSubmitting,
      onSubmit: submitAcknowledgement,
      onError,
    });
  }, [registerStep, acknowledged, isSubmitting, submitAcknowledgement, onError]);

  const onAcknowledgementChange = useCallback((checked: boolean) => {
    setAcknowledged(checked);
    // Clear error when user interacts with checkbox
    if (checked) {
      setError(null);
    }
  }, []);

  return {
    acknowledged,
    success,
    error,
    gracePeriodDays,
    status,
    lastSuccessfulUpload,
    lastUploadError,
    onAcknowledgementChange,
  };
}

export default useMandatoryTelemetryAcknowledgementStep;
