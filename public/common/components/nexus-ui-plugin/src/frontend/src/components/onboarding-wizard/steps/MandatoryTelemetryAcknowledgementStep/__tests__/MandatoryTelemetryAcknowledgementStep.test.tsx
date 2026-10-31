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
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import MandatoryTelemetryAcknowledgementStep from '../MandatoryTelemetryAcknowledgementStep';
import { SUCCESS_INDICATOR_MS } from '../useMandatoryTelemetryAcknowledgementStep';
import { useWizard } from '../../../useWizard';
import { restClient } from '../../../../../interface/api';
import ExtJS from '../../../../../interface/ExtJS';

jest.mock('../../../useWizard');
jest.mock('../../../../../interface/api', () => ({
  restClient: { post: jest.fn() },
  parseApiError: (err: unknown) => ({
    message: err instanceof Error ? err.message : String(err),
  }),
}));

jest.mock('@radix-ui/themes', () => ({
  ...jest.requireActual('@radix-ui/themes'),
  Spinner: () => <div data-testid="spinner">Loading...</div>,
}));

const mockedUseWizard = jest.mocked(useWizard);
const mockedPost = jest.mocked(restClient.post);

const CHECKBOX_TESTID = 'mandatory-telemetry-acknowledgement-step__acknowledge-checkbox';
const WARNING_TESTID = 'mandatory-telemetry-acknowledgement-step__warning';
const SUCCESS_TESTID = 'mandatory-telemetry-acknowledgement-step__success';
const ERROR_TESTID = 'mandatory-telemetry-acknowledgement-step__error';
const STATUS_TESTID = 'mandatory-telemetry-acknowledgement-step__status';

const NEVER_BODY =
  "We haven't received telemetry from this instance yet. Check Repository Health Check, then review the Baseline Telemetry status again in 24 hours.";

/**
 * Noon UTC so the rendered calendar date is Sep 14 in every timezone the suite might run in --
 * `formatDateTime` deliberately formats in the viewer's own zone, so a midnight timestamp would
 * render as the 13th or the 15th depending on the machine.
 */
const LAST_UPLOAD_ISO = '2026-09-14T12:00:00Z';
const LAST_UPLOAD_DATE = 'Sep 14, 2026';

/**
 * Flush queued microtasks so an awaited POST settles and the hook's follow-up
 * state updates land before the test advances timers or asserts. Two flushes
 * cover the settle -> setState -> effect chain.
 */
async function flushMicrotasks(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
}

/**
 * The `nexus.telemetry.health` shape the step consumes. Kept as a local literal rather than
 * imported so a field the backend stops publishing shows up here as a test failure.
 */
interface MockTelemetryHealth {
  gracePeriodDays?: number;
  lastSuccessfulUpload?: string;
  readOnly?: boolean;
  showWarning?: boolean;
  lastError?: { type: string; httpStatusCode?: number; message: string };
}

/**
 * Stubs the `nexus.telemetry.health` state the step reads the grace period length and Baseline
 * Telemetry status from. Pass `undefined` to simulate a backend that reported no health state at
 * all.
 */
const mockTelemetryHealth = (health: MockTelemetryHealth | undefined) => {
  jest.spyOn(ExtJS, 'state').mockReturnValue({
    getValue: (key: string) => (key === 'nexus.telemetry.health' ? health : undefined),
  } as never);
};

describe('MandatoryTelemetryAcknowledgementStep', () => {
  const registerStep = jest.fn();
  const submit = jest.fn();

  const mostRecentRegistration = () => {
    const lastCall = registerStep.mock.calls[registerStep.mock.calls.length - 1];
    return lastCall[0] as {
      valid: boolean;
      onSubmit: () => Promise<void>;
      onError?: () => void;
    };
  };

  const wizardValue = () => ({
    state: 'stepReady' as const,
    steps: [],
    currentIndex: 0,
    currentStep: null,
    errorMessage: null,
    errorKind: null,
    isCurrentStepValid: false,
    registerStep,
    getStarted: jest.fn(),
    submit,
    skip: jest.fn(),
    finish: jest.fn(),
    actionButtonRef: { current: null },
  });

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    mockedPost.mockResolvedValue(undefined as never);
    mockedUseWizard.mockReturnValue(wizardValue());
    mockTelemetryHealth({ gracePeriodDays: 45 });
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('shouldRenderWithoutCrashing', () => {
    render(<MandatoryTelemetryAcknowledgementStep />);
    expect(
      screen.getByTestId('mandatory-telemetry-acknowledgement-step')
    ).toBeInTheDocument();
  });

  it('shouldRenderTitleAndDescription', () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    expect(
      screen.getByRole('heading', { name: 'Required base telemetry' })
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'Once a day, Nexus Repository uploads a limited set of usage and product health data to help Sonatype provide better support, improve product quality, and review license usage. Nexus Repository records the outcome of each upload attempt.'
      )
    ).toBeInTheDocument();
  });

  it('shouldLinkTheBaseTelemetryDocumentationInANewTab', () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    const link = screen.getByTestId('mandatory-telemetry-acknowledgement-step__learn-more');
    expect(link).toHaveTextContent('Learn more about required base telemetry');
    expect(link).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nxrm3/nexus-telemetry'
    );
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('shouldWarnAboutReadOnlyModeUsingTheGracePeriodReportedByTheBackend', () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    expect(screen.getByTestId(WARNING_TESTID)).toHaveTextContent(
      'If telemetry cannot be sent for 45 consecutive days, the instance will enter read-only mode until connectivity is restored or an exemption is approved.'
    );
  });

  it('shouldWarnWithTheCommunityGracePeriodWhenTheBackendReportsIt', () => {
    mockTelemetryHealth({ gracePeriodDays: 10 });

    render(<MandatoryTelemetryAcknowledgementStep />);

    expect(screen.getByTestId(WARNING_TESTID)).toHaveTextContent(
      'If telemetry cannot be sent for 10 consecutive days, the instance will enter read-only mode until connectivity is restored or an exemption is approved.'
    );
  });

  it('shouldOmitTheDayCountWhenTheBackendReportsNoGracePeriod', () => {
    mockTelemetryHealth({});

    render(<MandatoryTelemetryAcknowledgementStep />);

    expect(screen.getByTestId(WARNING_TESTID)).toHaveTextContent(
      'If telemetry cannot be sent for an extended period, the instance will enter read-only mode until connectivity is restored or an exemption is approved.'
    );
  });

  it('shouldOmitTheDayCountWhenNoTelemetryHealthStateExists', () => {
    mockTelemetryHealth(undefined);

    render(<MandatoryTelemetryAcknowledgementStep />);

    const warning = screen.getByTestId(WARNING_TESTID);
    expect(warning).toHaveTextContent(
      'If telemetry cannot be sent for an extended period, the instance will enter read-only mode until connectivity is restored or an exemption is approved.'
    );
    expect(warning).not.toHaveTextContent('{gracePeriodDays}');
  });

  describe('Baseline Telemetry status', () => {
    it('shouldReportTheLastUploadDateWhenTelemetryIsActive', () => {
      mockTelemetryHealth({ gracePeriodDays: 45, lastSuccessfulUpload: LAST_UPLOAD_ISO });

      render(<MandatoryTelemetryAcknowledgementStep />);

      const status = screen.getByTestId(STATUS_TESTID);
      expect(status).toHaveAttribute('data-status', 'active');
      expect(status).toHaveTextContent(`Last received: ${LAST_UPLOAD_DATE}`);
    });

    it('shouldNotExplainAnythingFurtherWhenTelemetryIsActive', () => {
      // Active is the only state with no body copy: the date is the whole message.
      mockTelemetryHealth({ gracePeriodDays: 45, lastSuccessfulUpload: LAST_UPLOAD_ISO });

      render(<MandatoryTelemetryAcknowledgementStep />);

      const status = screen.getByTestId(STATUS_TESTID);
      expect(status).not.toHaveTextContent(NEVER_BODY);
      expect(status).not.toHaveTextContent('Recent telemetry uploads have failed.');
    });

    it('shouldTellNewInstancesThatNoTelemetryHasArrivedYet', () => {
      mockTelemetryHealth({ gracePeriodDays: 45 });

      render(<MandatoryTelemetryAcknowledgementStep />);

      const status = screen.getByTestId(STATUS_TESTID);
      expect(status).toHaveAttribute('data-status', 'never');
      expect(status).toHaveTextContent('Last received: Never');
      expect(status).toHaveTextContent(NEVER_BODY);
    });

    it.each(['never', 'unknown'])(
      'shouldTreatTheBackendSentinel"%s"AsNoUploadOnRecord',
      (sentinel) => {
        // `TelemetryHealthCheck` reports these two words where a timestamp would go; neither is a
        // date, so both have to read as "Never" rather than leaking the sentinel to the screen.
        mockTelemetryHealth({ gracePeriodDays: 45, lastSuccessfulUpload: sentinel });

        render(<MandatoryTelemetryAcknowledgementStep />);

        const status = screen.getByTestId(STATUS_TESTID);
        expect(status).toHaveAttribute('data-status', 'never');
        expect(status).toHaveTextContent('Last received: Never');
        expect(status).not.toHaveTextContent(sentinel);
      }
    );

    it('shouldReportAnUploadIssueWhileTheGracePeriodIsRunning', () => {
      mockTelemetryHealth({
        gracePeriodDays: 45,
        lastSuccessfulUpload: LAST_UPLOAD_ISO,
        showWarning: true,
      });

      render(<MandatoryTelemetryAcknowledgementStep />);

      const status = screen.getByTestId(STATUS_TESTID);
      expect(status).toHaveAttribute('data-status', 'uploadIssue');
      expect(status).toHaveTextContent(`Last received: ${LAST_UPLOAD_DATE}`);
      expect(status).toHaveTextContent(
        'Recent telemetry uploads have failed. A 45-day grace period starts today to resolve the Baseline Telemetry issue. Check Repository Health Check again in 24 hours to confirm the status.'
      );
    });

    it('shouldFallBackToDayCountFreeCopyWhenNoGracePeriodWasReported', () => {
      mockTelemetryHealth({ lastSuccessfulUpload: LAST_UPLOAD_ISO, showWarning: true });

      render(<MandatoryTelemetryAcknowledgementStep />);

      const status = screen.getByTestId(STATUS_TESTID);
      expect(status).toHaveAttribute('data-status', 'uploadIssue');
      expect(status).toHaveTextContent(
        'Recent telemetry uploads have failed. A grace period starts today to resolve the Baseline Telemetry issue. Check Repository Health Check again in 24 hours to confirm the status.'
      );
      expect(status).not.toHaveTextContent('{gracePeriodDays}');
    });

    it('shouldReportReadOnlyModeAsAnUploadIssueToo', () => {
      // Read-only mode is the same upload failure with the grace period spent, so it does not get a
      // status of its own -- the administrator sees the same thing either way.
      mockTelemetryHealth({
        gracePeriodDays: 45,
        lastSuccessfulUpload: LAST_UPLOAD_ISO,
        readOnly: true,
      });

      render(<MandatoryTelemetryAcknowledgementStep />);

      const status = screen.getByTestId(STATUS_TESTID);
      expect(status).toHaveAttribute('data-status', 'uploadIssue');
      expect(status).toHaveTextContent(`Last received: ${LAST_UPLOAD_DATE}`);
      expect(status).toHaveTextContent(
        'Recent telemetry uploads have failed. A 45-day grace period starts today to resolve the Baseline Telemetry issue. Check Repository Health Check again in 24 hours to confirm the status.'
      );
    });

    it('shouldReportTheFailureRatherThanTheMissingDateWhenBothApply', () => {
      // An instance that has never uploaded *and* is past the threshold is a failing instance, not
      // a new one — same precedence `TelemetryHealthCheck` applies.
      mockTelemetryHealth({ gracePeriodDays: 45, showWarning: true });

      render(<MandatoryTelemetryAcknowledgementStep />);

      const status = screen.getByTestId(STATUS_TESTID);
      expect(status).toHaveAttribute('data-status', 'uploadIssue');
      expect(status).toHaveTextContent('Last received: Never');
      expect(status).toHaveTextContent('Recent telemetry uploads have failed.');
    });

    it.each([
      ['an empty health map', {}],
      ['no telemetry health state at all', undefined],
    ])('shouldReportNeverReceivedGiven%s', (_label, health) => {
      // Nothing reported means nothing received as far as this step can tell. The step is gated on
      // mandatory mode with telemetry required, so there is no opt-out instance to mis-report here.
      mockTelemetryHealth(health);

      render(<MandatoryTelemetryAcknowledgementStep />);

      const status = screen.getByTestId(STATUS_TESTID);
      expect(status).toHaveAttribute('data-status', 'never');
      expect(status).toHaveTextContent('Last received: Never');
    });

    it('shouldNotLeakTheDatePlaceholderOrAnInvalidDate', () => {
      mockTelemetryHealth({ gracePeriodDays: 45, lastSuccessfulUpload: 'not-a-timestamp' });

      render(<MandatoryTelemetryAcknowledgementStep />);

      const status = screen.getByTestId(STATUS_TESTID);
      expect(status).toHaveTextContent('Last received: Never');
      expect(status).not.toHaveTextContent('{lastSuccessfulUpload}');
      expect(status).not.toHaveTextContent('Invalid Date');
    });

    it('shouldShowTheLastClientErrorReportedByTheBackend', () => {
      mockTelemetryHealth({
        gracePeriodDays: 45,
        lastSuccessfulUpload: LAST_UPLOAD_ISO,
        showWarning: true,
        lastError: { type: 'CLIENT_ERROR', httpStatusCode: 403, message: 'Forbidden' },
      });

      render(<MandatoryTelemetryAcknowledgementStep />);

      expect(screen.getByTestId(STATUS_TESTID)).toHaveTextContent(
        'Last upload error: HTTP 403: Forbidden'
      );
      expect(screen.getByTestId(`${STATUS_TESTID}-error`)).not.toHaveTextContent(/[{}]/);
    });

    it('shouldShowASignatureFailureReportedByTheBackend', () => {
      mockTelemetryHealth({
        gracePeriodDays: 45,
        showWarning: true,
        lastError: { type: 'SIGNATURE_FAILURE', httpStatusCode: 204, message: 'Unable to verify the signature of the Telemetry Service response' },
      });

      render(<MandatoryTelemetryAcknowledgementStep />);

      expect(screen.getByTestId(`${STATUS_TESTID}-error`)).toHaveTextContent(
        /^Last upload error: HTTP 204: Unable to verify the signature of the Telemetry Service response$/
      );
    });

    it('shouldShowTheMessageOfATlsFailureWithoutStatusCode', () => {
      mockTelemetryHealth({
        gracePeriodDays: 45,
        showWarning: true,
        lastError: { type: 'TLS_FAILURE', message: 'TLS handshake failed: PKIX path building...' },
      });

      render(<MandatoryTelemetryAcknowledgementStep />);

      expect(screen.getByTestId(`${STATUS_TESTID}-error`)).toHaveTextContent(
        /^Last upload error: TLS handshake failed: PKIX path building\.\.\.$/
      );
    });

    it('shouldShowTheEllipsisOfAMessageTheBackendCutShort', () => {
      mockTelemetryHealth({
        gracePeriodDays: 45,
        showWarning: true,
        lastError: { type: 'CLIENT_ERROR', httpStatusCode: 407, message: '407 Proxy Authentication Required Proxy...' },
      });

      render(<MandatoryTelemetryAcknowledgementStep />);

      expect(screen.getByTestId(`${STATUS_TESTID}-error`)).toHaveTextContent(
        /^Last upload error: HTTP 407: 407 Proxy Authentication Required Proxy\.\.\.$/
      );
    });

    it('shouldShowTheLastClientErrorVerbatimWhenItContainsReplacementPatterns', () => {
      // String.prototype.replace reads `$&` and the like in a string replacement; the error is server text.
      mockTelemetryHealth({
        gracePeriodDays: 45,
        showWarning: true,
        lastError: { type: 'CLIENT_ERROR', httpStatusCode: 400, message: "bad $& and $' value" },
      });

      render(<MandatoryTelemetryAcknowledgementStep />);

      expect(screen.getByTestId(`${STATUS_TESTID}-error`)).toHaveTextContent(
        "Last upload error: HTTP 400: bad $& and $' value"
      );
    });

    it.each([
      ['a blank message', '  '],
      ['an empty message', ''],
    ])('shouldShowOnlyTheStatusCodeGiven%s', (_label, message) => {
      mockTelemetryHealth({ gracePeriodDays: 45, showWarning: true, lastError: { type: 'CLIENT_ERROR', httpStatusCode: 401, message } });

      render(<MandatoryTelemetryAcknowledgementStep />);

      expect(screen.getByTestId(`${STATUS_TESTID}-error`)).toHaveTextContent(/^Last upload error: HTTP 401$/);
    });

    it.each([
      ['CONNECTION_ERROR', 'Unable to send telemetry data to the Telemetry Service'],
      ['SIGNATURE_FAILURE', 'Unable to verify the signature of the Telemetry Service response'],
      ['TLS_FAILURE', 'Unable to verify the TLS certificate of the Telemetry Service'],
    ])('shouldShowTheBackendDefaultMessageForA%sWithoutStatusCode', (type, message) => {
      mockTelemetryHealth({ gracePeriodDays: 45, showWarning: true, lastError: { type, message } });

      render(<MandatoryTelemetryAcknowledgementStep />);

      expect(screen.getByTestId(`${STATUS_TESTID}-error`)).toHaveTextContent(
        new RegExp(`^Last upload error: ${message}$`)
      );
    });

    it.each([
      ['no last error', undefined],
      ['a last error that is not an object', 'HTTP 403' as unknown as MockTelemetryHealth['lastError']],
      ['a last error of an unknown type', { type: 'OTHER', httpStatusCode: 403, message: 'Forbidden' }],
      ['a last error with neither status code nor message', { type: 'CONNECTION_ERROR', message: ' ' }],
    ])('shouldNotShowAnUploadErrorGiven%s', (_label, lastError) => {
      mockTelemetryHealth({ gracePeriodDays: 45, showWarning: true, lastError });

      render(<MandatoryTelemetryAcknowledgementStep />);

      expect(screen.queryByTestId(`${STATUS_TESTID}-error`)).not.toBeInTheDocument();
      expect(screen.getByTestId(STATUS_TESTID)).not.toHaveTextContent('Last upload error');
    });
  });

  it('shouldRegisterAsInvalidUntilTheCheckboxIsTicked', async () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    await waitFor(() => expect(registerStep).toHaveBeenCalled());
    expect(mostRecentRegistration().valid).toBe(false);
    expect(screen.getByTestId(CHECKBOX_TESTID)).toHaveAttribute('data-state', 'unchecked');
  });

  it('shouldRegisterAsValidOnceTheCheckboxIsTicked', async () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    await userEvent.click(screen.getByTestId(CHECKBOX_TESTID));

    await waitFor(() => expect(mostRecentRegistration().valid).toBe(true));
  });

  it('shouldRegisterAsInvalidAgainWhenTheCheckboxIsUnticked', async () => {
    render(<MandatoryTelemetryAcknowledgementStep />);
    const checkbox = screen.getByTestId(CHECKBOX_TESTID);

    await userEvent.click(checkbox);
    await waitFor(() => expect(mostRecentRegistration().valid).toBe(true));

    await userEvent.click(checkbox);
    await waitFor(() => expect(mostRecentRegistration().valid).toBe(false));
  });

  it('shouldLabelTheCheckboxWithTheAcknowledgement', () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    expect(
      screen.getByLabelText('I understand this requirement and its impact.')
    ).toBeInTheDocument();
  });

  it('shouldRegisterAnOnErrorHandlerToKeepWizardInStepReadyState', async () => {
    // The onError handler exists to prevent transition to issueOccurred state,
    // allowing inline error recovery with retry capability.
    render(<MandatoryTelemetryAcknowledgementStep />);

    await waitFor(() => expect(registerStep).toHaveBeenCalled());
    expect(mostRecentRegistration().onError).toBeDefined();
  });

  it('shouldPostTheAcknowledgementOnSubmit', async () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    await waitFor(() => expect(registerStep).toHaveBeenCalled());
    const onSubmit = mostRecentRegistration().onSubmit;
    let submitPromise!: Promise<void>;
    await act(async () => {
      submitPromise = onSubmit();
      await flushMicrotasks();
    });
    // Skip the success indicator delay so the submit promise settles.
    await act(async () => {
      jest.advanceTimersByTime(SUCCESS_INDICATOR_MS);
      await flushMicrotasks();
    });
    await submitPromise;

    expect(mockedPost).toHaveBeenCalledTimes(1);
    expect(mockedPost).toHaveBeenCalledWith(
      '/service/rest/v1/system/mandatory-telemetry-acknowledgement',
      { acknowledgement: true }
    );
  });

  it('shouldShowSuccessIndicatorAfterSuccessfulSubmit', async () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    await waitFor(() => expect(registerStep).toHaveBeenCalled());

    let submitPromise!: Promise<void>;
    await act(async () => {
      submitPromise = mostRecentRegistration().onSubmit();
      await flushMicrotasks();
    });

    // Before the delay elapses, success should already be visible
    expect(screen.getByTestId(SUCCESS_TESTID)).toBeInTheDocument();
    expect(screen.getByTestId(SUCCESS_TESTID)).toHaveTextContent('Acknowledgement recorded');

    await act(async () => {
      jest.advanceTimersByTime(SUCCESS_INDICATOR_MS);
      await flushMicrotasks();
    });
    await submitPromise;
  });

  it('shouldShowTheBackendMessageInlineWhenPostFails', async () => {
    mockedPost.mockRejectedValue(new Error('Network error') as never);

    render(<MandatoryTelemetryAcknowledgementStep />);

    await waitFor(() => expect(registerStep).toHaveBeenCalled());
    const onSubmit = mostRecentRegistration().onSubmit;

    await act(async () => {
      await expect(onSubmit()).rejects.toThrow('Network error');
    });

    expect(screen.getByTestId(ERROR_TESTID)).toBeInTheDocument();
    expect(screen.getByTestId(ERROR_TESTID)).toHaveTextContent('Network error');
  });

  it('shouldFallBackToTheGenericMessageWhenTheFailureCarriesNoText', async () => {
    mockedPost.mockRejectedValue(new Error('') as never);

    render(<MandatoryTelemetryAcknowledgementStep />);

    await waitFor(() => expect(registerStep).toHaveBeenCalled());
    const onSubmit = mostRecentRegistration().onSubmit;

    await act(async () => {
      await expect(onSubmit()).rejects.toThrow();
    });

    expect(screen.getByTestId(ERROR_TESTID)).toHaveTextContent(
      'Failed to submit your acknowledgement. Please try again.'
    );
  });

  it('shouldNotOfferAnExplicitRetryButtonOnSubmitFailure', async () => {
    // Matches EulaStep's submit-error convention: no retry button — the wizard's
    // Next button stays enabled and a second click re-POSTs.
    mockedPost.mockRejectedValue(new Error('Network error') as never);

    render(<MandatoryTelemetryAcknowledgementStep />);

    await waitFor(() => expect(registerStep).toHaveBeenCalled());
    const onSubmit = mostRecentRegistration().onSubmit;

    await act(async () => {
      await expect(onSubmit()).rejects.toThrow();
    });

    expect(screen.getByTestId(ERROR_TESTID)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
  });

  it('shouldStayValidAfterASubmitFailureSoNextResubmits', async () => {
    mockedPost.mockRejectedValueOnce(new Error('Network error') as never);

    render(<MandatoryTelemetryAcknowledgementStep />);

    await userEvent.click(screen.getByTestId(CHECKBOX_TESTID));
    await waitFor(() => expect(mostRecentRegistration().valid).toBe(true));

    await act(async () => {
      await expect(mostRecentRegistration().onSubmit()).rejects.toThrow();
    });

    expect(mockedPost).toHaveBeenCalledTimes(1);
    // The step remains submittable, so the wizard's Next button is the retry path.
    expect(mostRecentRegistration().valid).toBe(true);

    let submitPromise!: Promise<void>;
    await act(async () => {
      submitPromise = mostRecentRegistration().onSubmit();
      await flushMicrotasks();
    });
    await act(async () => {
      jest.advanceTimersByTime(SUCCESS_INDICATOR_MS);
      await flushMicrotasks();
    });
    await submitPromise;

    expect(mockedPost).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId(SUCCESS_TESTID)).toBeInTheDocument();
    expect(screen.queryByTestId(ERROR_TESTID)).not.toBeInTheDocument();
  });

  it('shouldDisableSubmitWhileSubmitting', async () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    await userEvent.click(screen.getByTestId(CHECKBOX_TESTID));
    await waitFor(() => expect(mostRecentRegistration().valid).toBe(true));

    // Hold the POST open so the submitting state is observable
    let resolvePromise!: () => void;
    const pendingPromise = new Promise<void>((resolve) => {
      resolvePromise = resolve;
    });
    mockedPost.mockReturnValueOnce(pendingPromise as never);

    const onSubmit = mostRecentRegistration().onSubmit;
    let submitPromise!: Promise<void>;
    await act(async () => {
      submitPromise = onSubmit();
      await flushMicrotasks();
    });

    // While submitting, valid should be false
    expect(mostRecentRegistration().valid).toBe(false);

    // Complete the POST
    await act(async () => {
      resolvePromise();
      await flushMicrotasks();
    });
    await act(async () => {
      jest.advanceTimersByTime(SUCCESS_INDICATOR_MS);
      await flushMicrotasks();
    });
    await submitPromise;

    // After submit completes, valid should be true again
    expect(mostRecentRegistration().valid).toBe(true);
  });

  it('shouldCallTheWizardSubmitItself', async () => {
    render(<MandatoryTelemetryAcknowledgementStep />);

    await waitFor(() => expect(registerStep).toHaveBeenCalled());
    expect(mostRecentRegistration().onSubmit).not.toBe(submit);
    expect(submit).not.toHaveBeenCalled();
  });

  it('shouldRegisterOnceAndNotLoopOnRerender', () => {
    const { rerender } = render(<MandatoryTelemetryAcknowledgementStep />);
    rerender(<MandatoryTelemetryAcknowledgementStep />);
    rerender(<MandatoryTelemetryAcknowledgementStep />);

    expect(registerStep).toHaveBeenCalledTimes(1);
  });
});
