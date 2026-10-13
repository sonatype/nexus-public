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
import React, {useState, useEffect, useCallback, useRef, useMemo} from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxH2,
  NxInfoAlert,
  NxModal,
  NxTextLink,
} from '@sonatype/react-shared-components';
import {faCopy} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';
import {TOKEN_MODAL_AUTO_CLOSE_SECONDS} from './ServiceAccountTokensHelper';
import './ServiceAccountTokens.scss';

const LABELS = UIStrings.SERVICE_ACCOUNT_TOKENS.TOKEN_MODAL;

// btoa() only accepts Latin-1, so a bare btoa(`${name}:${token}`) throws a
// DOMException when the account name contains non-ASCII characters (accents,
// CJK, emoji). Round-trip through UTF-8 first so those encode correctly.
function safeBtoa(str) {
  return btoa(unescape(encodeURIComponent(str)));
}

export default function ServiceAccountTokensTokenModal({name, token, onClose}) {
  const [copiedField, setCopiedField] = useState(null);
  const [copyFailed, setCopyFailed] = useState(false);
  // Reveal-once by design: the encoded header has no "hide" control, matching
  // the modal's "you won't see these credentials again" philosophy.
  const [encodedRevealed, setEncodedRevealed] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(TOKEN_MODAL_AUTO_CLOSE_SECONDS);
  const intervalRef = useRef(null);
  const onCloseRef = useRef(onClose);

  const encodedCredential = useMemo(() => {
    try {
      return safeBtoa(`${name}:${token}`);
    } catch {
      return '';
    }
  }, [name, token]);

  const encodedHeader = LABELS.ENCODED_VALUE(encodedCredential);

  useEffect(() => {
    onCloseRef.current = onClose;
  });

  useEffect(() => {
    intervalRef.current = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(intervalRef.current);
          onCloseRef.current();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, []);

  const handleCopy = useCallback((text, field) => {
    navigator.clipboard.writeText(text)
      .then(() => {
        setCopyFailed(false);
        setCopiedField(field);
        setTimeout(() => setCopiedField((f) => (f === field ? null : f)), 2000);
      })
      .catch(() => {
        setCopiedField(null);
        setCopyFailed(true);
      });
  }, []);

  const copyAnnouncement =
    copiedField === 'username' ? LABELS.USERNAME_COPY_ANNOUNCEMENT
    : copiedField === 'password' ? LABELS.PASSWORD_COPY_ANNOUNCEMENT
    : copiedField === 'token' ? LABELS.COPY_ANNOUNCEMENT
    : copiedField === 'encoded' ? LABELS.ENCODED_COPY_ANNOUNCEMENT
    : '';

  return (
    <NxModal onCancel={onClose} aria-labelledby="token-reveal-modal">
      <NxModal.Header>
        <NxH2 id="token-reveal-modal">{LABELS.TITLE}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxInfoAlert role="alert">
          <strong>{LABELS.WARNING}</strong>
        </NxInfoAlert>

        <span className="nxrm-sa-sr-only" role="status" aria-live="polite">
          {copyAnnouncement}
        </span>

        <div className="nxrm-sa-cred-section">
          <p className="nxrm-sa-section-label">{LABELS.BASIC_SECTION_LABEL}</p>

          <p className="nxrm-sa-field-label">{LABELS.USERNAME_LABEL}</p>
          <div className="nxrm-sa-token-reveal">
            <code className="nxrm-sa-token-value" data-testid="sat-basic-username">{name}</code>
            <NxButton type="button" variant="icon-only" className="nxrm-sa-copy-btn"
                      onClick={() => handleCopy(name, 'username')}
                      data-testid="sat-basic-username-copy"
                      aria-label={LABELS.COPY_USERNAME_ARIA}
                      title={copiedField === 'username' ? LABELS.COPIED_BUTTON : LABELS.COPY_BUTTON}>
              <NxFontAwesomeIcon icon={faCopy} />
            </NxButton>
          </div>

          <p className="nxrm-sa-field-label">{LABELS.PASSWORD_LABEL}</p>
          <div className="nxrm-sa-token-reveal">
            <code className="nxrm-sa-token-value" data-testid="sat-basic-password">{token}</code>
            <NxButton type="button" variant="icon-only" className="nxrm-sa-copy-btn"
                      onClick={() => handleCopy(token, 'password')}
                      data-testid="sat-basic-password-copy"
                      aria-label={LABELS.COPY_PASSWORD_ARIA}
                      title={copiedField === 'password' ? LABELS.COPIED_BUTTON : LABELS.COPY_BUTTON}>
              <NxFontAwesomeIcon icon={faCopy} />
            </NxButton>
          </div>
        </div>

        <div className="nxrm-sa-cred-section">
          <p className="nxrm-sa-section-label">{LABELS.BEARER_SECTION_LABEL}</p>
          <p className="nxrm-sa-field-label">{LABELS.TOKEN_LABEL}</p>
          <div className="nxrm-sa-token-reveal">
            <code className="nxrm-sa-token-value" data-testid="sat-token-value">{token}</code>
            <NxButton type="button" variant="icon-only" className="nxrm-sa-copy-btn"
                      onClick={() => handleCopy(token, 'token')}
                      data-testid="sat-token-copy"
                      aria-label={LABELS.COPY_TOKEN_ARIA}
                      title={copiedField === 'token' ? LABELS.COPIED_BUTTON : LABELS.COPY_BUTTON}>
              <NxFontAwesomeIcon icon={faCopy} />
            </NxButton>
          </div>
        </div>

        {copyFailed && (
          <p className="nxrm-sa-copy-error" role="alert" data-testid="sa-token-copy-error">
            {LABELS.COPY_FAILED}
          </p>
        )}

        <div className="nxrm-sa-cred-section">
          {!encodedRevealed ? (
            <NxButton type="button" variant="tertiary"
                      onClick={() => { setEncodedRevealed(true); setCopyFailed(false); }}
                      data-testid="sat-basic-encoded-reveal">
              {LABELS.ENCODED_REVEAL_BUTTON}
            </NxButton>
          ) : (
            <>
              <p className="nxrm-sa-field-label">{LABELS.ENCODED_LABEL}</p>
              <div className="nxrm-sa-token-reveal">
                <code className="nxrm-sa-token-value" data-testid="sat-basic-encoded-value">
                  {encodedHeader}
                </code>
                <NxButton type="button" variant="icon-only" className="nxrm-sa-copy-btn"
                          onClick={() => handleCopy(encodedHeader, 'encoded')}
                          data-testid="sat-basic-encoded-copy"
                          aria-label={LABELS.COPY_ENCODED_ARIA}
                          title={copiedField === 'encoded' ? LABELS.COPIED_BUTTON : LABELS.COPY_BUTTON}>
                  <NxFontAwesomeIcon icon={faCopy} />
                </NxButton>
              </div>
              <p className="nxrm-sa-encoded-note">{LABELS.ENCODED_NOTE}</p>
            </>
          )}
        </div>

        <p className="nxrm-sa-docs-link-wrapper">
          <NxTextLink external href={LABELS.DOCS_LINK_HREF} data-testid="sat-docs-link">
            {LABELS.DOCS_LINK}
          </NxTextLink>
        </p>
      </NxModal.Content>
      <footer className="nx-footer nxrm-sa-token-footer">
        <span className="nxrm-sa-auto-close-notice">{LABELS.AUTO_CLOSE_NOTICE(secondsLeft)}</span>
        <div className="nx-btn-bar">
          <NxButton type="button" onClick={onClose}>
            {LABELS.DONE_BUTTON}
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}
