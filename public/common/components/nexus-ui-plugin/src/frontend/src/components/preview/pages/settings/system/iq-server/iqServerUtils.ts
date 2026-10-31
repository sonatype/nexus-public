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

import { IqServerConfiguration, IqValidationErrors, PASSWORD_PLACEHOLDER } from './types';

/** validateIqConfig doesn't read `properties`, so it accepts either the wire shape
 *  (IqServerConfiguration, string properties) or the form shape (IqServerFormData,
 *  array properties) — both are structurally assignable once `properties` is omitted. */
type IqConfigForValidation = Omit<IqServerConfiguration, 'properties'>;

const URL_HOSTNAME_REGEX = /^(([a-z0-9]|[a-z0-9][a-z0-9-]*[a-z0-9])\.)*([a-z0-9]|[a-z0-9][a-z0-9-]*[a-z0-9])$/i;
const URL_PATHNAME_REGEX = /^([\S]*\S)?$/i;

export function isValidUrl(url: string): boolean {
  let parsed: URL;
  try {
    parsed = new URL(url);
  } catch {
    return false;
  }
  const hostnameMatch = url.match(/^https?:\/\/([^:/?#]+)/i);
  const hostname = hostnameMatch?.[1];
  const { protocol, pathname, port } = parsed;
  return (
    (protocol === 'http:' || protocol === 'https:') &&
    Boolean(hostname) &&
    URL_HOSTNAME_REGEX.test(hostname!) &&
    port !== '0' &&
    URL_PATHNAME_REGEX.test(decodeURIComponent(pathname))
  );
}

/**
 * Validates IQ Server configuration and returns validation errors
 */
export function validateIqConfig(config: IqConfigForValidation, pristineConfig: IqConfigForValidation): IqValidationErrors {
  const errors: IqValidationErrors = {};

  if (!config.url?.trim()) {
    errors.url = 'IQ Server URL is required';
  } else if (!isValidUrl(config.url)) {
    errors.url = 'Please enter a valid URL';
  }

  if (!config.authenticationType) {
    errors.authenticationType = 'Authentication method is required';
  }

  if (config.authenticationType === 'USER') {
    if (!config.username?.trim()) {
      errors.username = 'Username is required';
    }

    // Password is required for new configs or when URL changes
    const urlChanged = pristineConfig.url && pristineConfig.url !== config.url;
    const isPlaceholder = config.password === PASSWORD_PLACEHOLDER;

    if (!config.password?.trim() && !isPlaceholder) {
      errors.password = 'Password is required';
    } else if (urlChanged && isPlaceholder) {
      errors.password = 'Password is required when changing the URL';
    }
  }

  if (config.timeoutSeconds !== null && (config.timeoutSeconds < 1 || config.timeoutSeconds > 3600)) {
    errors.timeoutSeconds = 'Timeout must be between 1 and 3600 seconds';
  }

  return errors;
}

/**
 * Parses the verification reason into application names or a status message.
 * Backend returns either comma-separated app names or messages like "No applications configured yet."
 */
export function parseApplicationReason(reason: string): { isList: boolean; items: string[] } {
  const trimmed = reason.trim();
  const appsPrefix = 'Applications: ';
  if (trimmed.includes(appsPrefix)) {
    const after = trimmed.split(appsPrefix)[1]?.trim() ?? '';
    const items = after.split(',').map((s) => s.trim()).filter(Boolean);
    return { isList: items.length > 0, items };
  }
  if (
    trimmed.toLowerCase().startsWith('no applications') ||
    (trimmed.startsWith('Connection successful') && !trimmed.includes(','))
  ) {
    return { isList: false, items: [trimmed] };
  }
  const items = trimmed.split(',').map((s) => s.trim()).filter(Boolean);
  return { isList: items.length > 0, items };
}

/** "Connected v1.2.3" -> " (v1.2.3)"; no match -> "". Mirrors the inline regex used today. */
export function parseVersion(reason?: string): string {
  const m = reason?.match(/v?(\d+\.\d+(?:\.\d+)?)/);
  return m ? ` (v${m[1]})` : '';
}


/**
 * Best-effort error-message extraction from an Axios error or REST client
 * error. Handles single strings, JSON-encoded strings, JSR-303 validation
 * arrays, and `{message}` objects. Falls back to the provided default.
 */
export function formatErrorMessage(err: any, fallback: string): string {
  const raw = err?.response?.data ?? err?.message ?? err;
  let parsed: any = raw;
  if (typeof raw === 'string') {
    const trimmed = raw.trim();
    if (trimmed.startsWith('[') || trimmed.startsWith('{')) {
      try {
        parsed = JSON.parse(trimmed);
      } catch {
        /* fall through */
      }
    }
  }
  let text: string;
  if (Array.isArray(parsed)) {
    text = parsed.map((e: any) => e?.message || JSON.stringify(e)).join(' ');
  } else if (typeof parsed === 'string') {
    text = parsed;
  } else if (parsed?.message) {
    text = parsed.message;
  } else {
    return fallback;
  }
  return humanizeIqError(text) || fallback;
}

// Peel Java toString / HttpClient noise from verify errors and map to plain user copy.
function humanizeIqError(raw: string): string {
  let text = raw.trim();

  // Peel ValidationErrorXO{id='...', message='...'} → the inner message.
  const xo = text.match(/ValidationErrorXO\{[^}]*message='([^']*)'/);
  if (xo) text = xo[1];

  // Strip the "[hostname/ip, hostname/ip]" resolver dump that
  // org.apache.http adds after the target: "Connect to localhost:8073 [localhost/127.0.0.1, ...] failed: ..."
  text = text.replace(/\s*\[[^\]]*(?:\/[^\]]*)+\]/g, '');

  // Pattern-match common root causes and rewrite as user-facing copy.
  const connectMatch = text.match(/Connect to ([^\s]+) failed: (.+)$/i);
  if (connectMatch) {
    const [, target, cause] = connectMatch;
    const causeLc = cause.toLowerCase();
    if (causeLc.includes('connection refused')) {
      return `Cannot reach ${target} — connection refused. Check the URL and that the IQ Server is running.`;
    }
    if (causeLc.includes('timed out') || causeLc.includes('timeout')) {
      return `Cannot reach ${target} — connection timed out. Check the URL, network, and firewall.`;
    }
    return `Cannot reach ${target} — ${cause}.`;
  }
  if (/UnknownHostException|unknown host|nodename nor servname/i.test(text)) {
    const host = text.match(/([\w.-]+)(?::\d+)?/)?.[1];
    return host
      ? `Host not found: ${host}. Check the URL is spelled correctly.`
      : 'Host not found. Check the URL is spelled correctly.';
  }
  if (/SSLHandshake|PKIX|certificate/i.test(text)) {
    return 'SSL certificate could not be verified. Add the IQ Server certificate to the Nexus truststore or use "Use Nexus Truststore".';
  }
  if (/401|unauthorized|invalid credentials/i.test(text)) {
    return 'Authentication failed. Check the username and password.';
  }
  if (/403|forbidden/i.test(text)) {
    return 'The IQ Server user does not have permission to read policy data.';
  }
  return text;
}

/**
 * Convert an UPPER_SNAKE_CASE policy stage value (e.g. "STAGE_RELEASE") to a
 * human-readable label ("Stage Release") for display.
 */
export function humanizeStage(stage: string | null | undefined): string {
  if (!stage) return '';
  return stage
    .split('_')
    .map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
    .join(' ');
}

