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

import { parseApiError, stripJavaExceptionPrefix, wrapApiError } from '../error-handler';
import { AxiosError } from 'axios';

function makeAxiosError(status: number, data: unknown): AxiosError {
  const error = new Error('Request failed') as AxiosError;
  error.isAxiosError = true;
  error.response = { status, data, headers: {}, config: {} as any, statusText: '' };
  return error;
}

describe('resolveConstraintMessage (via parseApiError)', () => {
  it('translates a known constraint key to a human-readable message', () => {
    const data = [{ id: 'PARAMETER name', message: '{org.sonatype.nexus.validation.constraint.name}' }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toContain('Only letters, digits, hyphens');
    expect(result.message).not.toContain('{org.sonatype.nexus.validation.constraint.name}');
  });

  it('passes through an unknown constraint key unchanged', () => {
    const data = [{ id: '*', message: '{org.sonatype.nexus.validation.constraint.unknown}' }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('{org.sonatype.nexus.validation.constraint.unknown}');
  });

  it('passes through a plain human-readable message unchanged', () => {
    const data = [{ id: '*', message: 'Tag name already exists' }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('Tag name already exists');
  });

  it('resolves constraint key in Hibernate Validator nested errors format', () => {
    const data = {
      errors: [
        { field: 'name', message: '{org.sonatype.nexus.validation.constraint.name}' },
      ],
    };
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toContain('Only letters, digits, hyphens');
    expect(result.message).not.toContain('{org.sonatype.nexus.validation.constraint.name}');
  });

  it('returns status 422 correctly for 422 responses', () => {
    const data = [{ id: 'PARAMETER name', message: 'Invalid value' }];
    const result = parseApiError(makeAxiosError(422, data));
    expect(result.status).toBe(422);
  });
});

// JAX-RS ConstraintViolationExceptionMapper prefixes the `id` of each error
// with `HelperBean.` plus the bean's property path (often `attributes.<field>`
// for repository configuration). Neither belongs in the user-visible field
// key, and the resulting `field` must match the dotted form field name so the
// inline error renders on the right input. (NEXUS-51599)
describe('HelperBean / attributes prefix stripping (via parseApiError)', () => {
  it('strips HelperBean. and attributes. from a Docker port-conflict response', () => {
    const data = [
      {
        id: 'HelperBean.attributes.docker.httpPort',
        message: "Port must be unique (conflicts with repository 'docker-hosted-1')",
      },
    ];
    const result = parseApiError(makeAxiosError(400, data));

    expect(result.message).toBe("Port must be unique (conflicts with repository 'docker-hosted-1')");
    expect(result.message).not.toContain('HelperBean');
    expect(result.message).not.toContain('attributes.docker.httpPort');

    expect(result.fieldErrors).toBeDefined();
    expect(result.fieldErrors).toHaveLength(1);
    expect(result.fieldErrors?.[0].field).toBe('docker.httpPort');
    expect(result.fieldErrors?.[0].message)
        .toBe("Port must be unique (conflicts with repository 'docker-hosted-1')");
  });

  it('strips HelperBean. when no attributes. segment is present', () => {
    const data = [{ id: 'HelperBean.expression', message: 'Invalid expression' }];
    const result = parseApiError(makeAxiosError(400, data));

    expect(result.fieldErrors?.[0].field).toBe('expression');
    expect(result.fieldErrors?.[0].message).toBe('Invalid expression');
  });

  it('only strips prefixes anchored to the start of the id', () => {
    // Pins the anchored-strip contract: the previous unanchored String.replace
    // would have mangled these mid-path occurrences. With startsWith+slice,
    // a property literally named `attributes.foo` nested under HelperBean is
    // left intact, and a doubled `HelperBean.` prefix only loses the outer one.
    const data = [
      { id: 'HelperBean.foo.attributes.bar', message: 'mid-path attributes' },
      { id: 'HelperBean.HelperBean.expression', message: 'doubled helperbean' },
    ];
    const result = parseApiError(makeAxiosError(400, data));

    expect(result.fieldErrors).toHaveLength(2);
    expect(result.fieldErrors?.[0].field).toBe('foo.attributes.bar');
    expect(result.fieldErrors?.[1].field).toBe('HelperBean.expression');
  });
});

describe('notnull field contextualization (via parseApiError)', () => {
  const NOTNULL_TOKEN = '{org.sonatype.nexus.validation.constraint.notnull}';
  const PLAIN_NOTNULL_MESSAGE = 'Must not be null.';

  it('prefixes the humanized field name to the notnull toast message', () => {
    const data = [{ id: 'HelperBean.attributes.storage.blobStoreName', message: NOTNULL_TOKEN }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('Blob store name must not be null.');
  });

  it('humanizes a top-level camelCase field to a readable label', () => {
    const data = [{ id: 'PARAMETER httpPort', message: NOTNULL_TOKEN }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('Http port must not be null.');
  });

  it('keeps the plain message inline on the field error, not the prefixed one', () => {
    const data = [{ id: 'HelperBean.attributes.storage.blobStoreName', message: NOTNULL_TOKEN }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.fieldErrors).toHaveLength(1);
    expect(result.fieldErrors?.[0].field).toBe('storage.blobStoreName');
    expect(result.fieldErrors?.[0].message).toBe(PLAIN_NOTNULL_MESSAGE);
  });

  it('leaves the notnull message unprefixed when no field id is present', () => {
    const data = [{ id: '*', message: NOTNULL_TOKEN }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe(PLAIN_NOTNULL_MESSAGE);
  });

  it('contextualizes notnull in the Hibernate Validator nested errors format', () => {
    const data = { errors: [{ field: 'name', message: NOTNULL_TOKEN }] };
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('Name must not be null.');
    expect(result.fieldErrors?.[0].message).toBe(PLAIN_NOTNULL_MESSAGE);
  });

  it('prefixes each field when multiple notnull errors are present', () => {
    const data = [
      { id: 'HelperBean.attributes.storage.blobStoreName', message: NOTNULL_TOKEN },
      { id: 'PARAMETER name', message: NOTNULL_TOKEN },
    ];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('Blob store name must not be null. Name must not be null.');
  });

  it('does not alter non-notnull messages that carry a field id', () => {
    const data = [{ id: 'PARAMETER name', message: '{org.sonatype.nexus.validation.constraint.name}' }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toContain('Only letters, digits, hyphens');
    expect(result.message).not.toMatch(/^Name /);
  });

  it('humanizes an underscore/hyphen separated field label', () => {
    const data = [{ id: 'PARAMETER max_connection-count', message: NOTNULL_TOKEN }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('Max connection count must not be null.');
  });

  it('falls back to the raw key when the humanized label would be empty', () => {
    const data = [{ id: 'PARAMETER ___', message: NOTNULL_TOKEN }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('___ must not be null.');
  });

  // v1 REST emits Jakarta's default "must not be null" (lowercase, no period), not the Sonatype token.
  it('contextualizes the raw Jakarta notnull message from the v1 REST endpoint', () => {
    const data = [
      {
        id: 'TerraformHostedRepositoryApiRequest.updateRepository.request.terraformSigning',
        message: 'must not be null',
      },
    ];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('Terraform signing must not be null.');
  });
});

describe('405 Method Not Allowed (via parseApiError)', () => {
  it('returns status 405 with fallback message when no body message', () => {
    const data = { 'status-code': 405, 'status-message': 'METHOD_NOT_ALLOWED' };
    const result = parseApiError(makeAxiosError(405, data));
    expect(result.status).toBe(405);
    expect(result.message).toBe('This operation is not supported for the current resource state.');
  });

  it('returns the body message when present', () => {
    const data = { message: 'Health Check is not available' };
    const result = parseApiError(makeAxiosError(405, data));
    expect(result.message).toBe('Health Check is not available');
  });
});

// WebApplicationMessageException callers on the backend commonly pass the message as a
// JSON string literal (e.g. `"\"Repository not found\""`), so the parsed `message` field
// is wrapped in a redundant pair of literal quote characters. Toasts should show the
// plain text. (NEXUS-53607 manual testing)
describe('quoted message unwrapping (via parseApiError)', () => {
  it('strips wrapping quotes from a 409 message', () => {
    const data = { id: '*', message: '"Repository Health Check instance capability is not enabled"' };
    const result = parseApiError(makeAxiosError(409, data));
    expect(result.message).toBe('Repository Health Check instance capability is not enabled');
  });

  it('strips wrapping quotes from a 403 message', () => {
    const data = { id: '*', message: '"EULA is not accepted"' };
    const result = parseApiError(makeAxiosError(403, data));
    expect(result.message).toBe('EULA is not accepted');
  });

  it('strips wrapping quotes from a 404 message', () => {
    const data = { id: '*', message: '"Repository not found"' };
    const result = parseApiError(makeAxiosError(404, data));
    expect(result.message).toBe('Repository not found');
  });

  it('leaves an unquoted message unchanged', () => {
    const data = { id: '*', message: 'Repository type is not proxy' };
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('Repository type is not proxy');
  });

  it('does not strip a single leading or trailing quote', () => {
    const data = { id: '*', message: '"unterminated' };
    const result = parseApiError(makeAxiosError(409, data));
    expect(result.message).toBe('"unterminated');
  });
});

// text/plain ValidationErrorXO#toString() body from a WebApplicationMessageException catch-all; axios leaves it a plain string. (NEXUS-54871)
describe('plain-text ValidationErrorXO body (via parseApiError)', () => {
  const SSRF_MESSAGE =
    'Host resolves to private/local IP address(es): 127.0.0.1 (loopback address). To allow ' +
    'connections, update the SSRF protection settings via the API at /v1/security/ssrf-protection.';
  const SSRF_XO_BODY = `ValidationErrorXO{id='*', message='${SSRF_MESSAGE}'}`;

  it('extracts the message from a ValidationErrorXO body on a 400 create failure', () => {
    const result = parseApiError(makeAxiosError(400, SSRF_XO_BODY));
    expect(result.message).toBe(SSRF_MESSAGE);
  });

  it('extracts the message from a ValidationErrorXO body on a 500 update failure', () => {
    const result = parseApiError(makeAxiosError(500, SSRF_XO_BODY));
    expect(result.message).toBe(SSRF_MESSAGE);
  });

  it('extracts the message from a ValidationErrorXO body on a 422 response', () => {
    const result = parseApiError(makeAxiosError(422, SSRF_XO_BODY));
    expect(result.message).toBe(SSRF_MESSAGE);
  });

  it('surfaces the ValidationErrorXO message on a 403 response', () => {
    const result = parseApiError(makeAxiosError(403, SSRF_XO_BODY));
    expect(result.message).toBe(SSRF_MESSAGE);
  });

  it('preserves quotes inside the ValidationErrorXO message', () => {
    const data = "ValidationErrorXO{id='*', message='Repository 'docker-hosted' is invalid'}";
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe("Repository 'docker-hosted' is invalid");
  });

  it('falls back to the raw string for a plain-text body that is not a ValidationErrorXO', () => {
    const result = parseApiError(makeAxiosError(400, 'Something went wrong'));
    expect(result.message).toBe('Something went wrong');
  });

  it('surfaces a non-ValidationErrorXO plain-text body verbatim on a 500 response', () => {
    const result = parseApiError(makeAxiosError(500, 'Backend exploded'));
    expect(result.message).toBe('Backend exploded');
  });

  it('falls back to the generic 400 message when the body is an empty string', () => {
    const result = parseApiError(makeAxiosError(400, ''));
    expect(result.message).toBe('Invalid request. Please check your input.');
  });

  it('falls back to the generic 500 message when the body is a whitespace-only string', () => {
    const result = parseApiError(makeAxiosError(500, '   '));
    expect(result.message).toBe('Server error (500). Please try again later.');
  });

  it('extracts the ValidationErrorXO message on an unmapped status via the default branch', () => {
    const result = parseApiError(makeAxiosError(418, SSRF_XO_BODY));
    expect(result.message).toBe(SSRF_MESSAGE);
  });

  it('falls back to the generic default message on an unmapped status with no body', () => {
    const result = parseApiError(makeAxiosError(418, undefined));
    expect(result.message).toBe('Unable to complete request. Please try again.');
  });
});

// ExceptionMapperSupport#unexpectedResponse emits `ERROR: (ID uuid) <Throwable.toString()>` text/plain; toString() leaks the exception FQCN, which must not reach a toast. (NEXUS-54871)
describe('stripJavaExceptionPrefix', () => {
  const SSRF_MESSAGE =
    'Host resolves to private/local IP address(es): 127.0.0.1 (loopback address). To allow ' +
    'connections, update the SSRF protection settings via the API at /v1/security/ssrf-protection.';

  it('strips a leaked FQCN prefix but keeps the ERROR (ID) wrapper', () => {
    const body = `ERROR: (ID abc-123) jakarta.validation.ValidationException: ${SSRF_MESSAGE}`;
    expect(stripJavaExceptionPrefix(body)).toBe(`ERROR: (ID abc-123) ${SSRF_MESSAGE}`);
  });

  it('strips a mid-string FQCN prefix inside a wrapper message', () => {
    const body =
      '(internal_error) Unexpected error: java.security.InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty';
    expect(stripJavaExceptionPrefix(body)).toBe(
      '(internal_error) Unexpected error: the trustAnchors parameter must be non-empty',
    );
  });

  it('collapses chained cause FQCNs to the innermost message', () => {
    const body =
      'java.lang.RuntimeException: com.example.FooException: java.io.IOException: disk full';
    expect(stripJavaExceptionPrefix(body)).toBe('disk full');
  });

  it('leaves a plain message with no FQCN prefix unchanged', () => {
    expect(stripJavaExceptionPrefix(SSRF_MESSAGE)).toBe(SSRF_MESSAGE);
  });

  it('does not treat a bare ClassName: prefix (no package) as an FQCN', () => {
    expect(stripJavaExceptionPrefix('ValidationException: nope')).toBe('ValidationException: nope');
  });

  it('strips a bare java.lang.Exception FQCN whose last segment is exactly Exception', () => {
    expect(stripJavaExceptionPrefix('java.lang.Exception: generic error')).toBe('generic error');
  });

  it('strips a bare java.lang.Error FQCN whose last segment is exactly Error', () => {
    expect(stripJavaExceptionPrefix('java.lang.Error: generic err')).toBe('generic err');
  });

  it('strips a leaked java.lang.Throwable FQCN', () => {
    expect(stripJavaExceptionPrefix('java.lang.Throwable: custom throwable')).toBe('custom throwable');
  });

  it('returns undefined for undefined input', () => {
    expect(stripJavaExceptionPrefix(undefined)).toBeUndefined();
  });

  it('surfaces the SSRF message with the FQCN removed through parseApiError on a 400', () => {
    const body = `ERROR: (ID abc-123) jakarta.validation.ValidationException: ${SSRF_MESSAGE}`;
    const result = parseApiError(makeAxiosError(400, body));
    expect(result.message).toBe(`ERROR: (ID abc-123) ${SSRF_MESSAGE}`);
    expect(result.message).not.toContain('jakarta.validation.ValidationException');
  });
});

describe('wrapApiError', () => {
  it('returns an Error whose message matches the parsed message', () => {
    const wrapped = wrapApiError(new Error('boom'));
    expect(wrapped).toBeInstanceOf(Error);
    expect(wrapped.message).toBe('boom');
  });

  it('preserves status/code/fieldErrors/requestId from the parsed ApiError', () => {
    const axiosError = makeAxiosError(400, [
      { id: 'PARAMETER name', message: 'Name is required' },
    ]);
    (axiosError.response as any).headers = { 'x-request-id': 'req-123' };
    const wrapped = wrapApiError(axiosError);
    expect(wrapped.status).toBe(400);
    expect(wrapped.code).toBe('VALIDATION_ERROR');
    expect(wrapped.fieldErrors).toEqual([{ field: 'name', message: 'Name is required' }]);
    expect(wrapped.requestId).toBe('req-123');
  });

  it('omits undefined optional fields rather than assigning undefined', () => {
    const wrapped = wrapApiError(new Error('plain'));
    expect(Object.prototype.hasOwnProperty.call(wrapped, 'code')).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(wrapped, 'fieldErrors')).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(wrapped, 'requestId')).toBe(false);
  });
});
