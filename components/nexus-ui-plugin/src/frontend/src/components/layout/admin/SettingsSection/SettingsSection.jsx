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
import PropTypes from 'prop-types';
import React, {createRef} from 'react';

import './SettingsSection.scss';

import UIStrings from '../../../../constants/UIStrings';

/**
 * @since 3.21
 */
export default class SettingsSection extends React.Component {
  state = {
    loadingMaskSize: {
      height: 0,
      width: 0
    }
  };

  settingsSectionRef = createRef();

  updateLoadingMaskSize = () => {
    if (this.settingsSectionRef.current) {
      const {height, width} = this.settingsSectionRef.current.getBoundingClientRect();
      this.setState({
        loadingMaskSize: {
          height: height,
          width: width
        }
      });
    }
  };

  componentDidMount() {
    window.addEventListener('resize', this.updateLoadingMaskSize);
    this.updateLoadingMaskSize();
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.updateLoadingMaskSize);
  }

  render() {
    const loadingMask = <span style={this.state.loadingMaskSize} className="nxrm-settings-section-loading-mask">
      {UIStrings.SETTINGS.LOADING_MASK}
    </span>;

    return <>
      {this.props.isLoading && loadingMask}
      <div className='nxrm-settings-section' ref={this.settingsSectionRef}>
        {this.props.children}
      </div>
    </>;
  }
}

SettingsSection.propTypes = {
  isLoading: PropTypes.bool
};

SettingsSection.FieldWrapper = function({labelText, descriptionText, children}) {
  const WrapperElement = labelText ? 'label' : 'div';
  const fieldName = React.Children.only(children).props.name;

  return <WrapperElement className='nxrm-settings-section-field-wrapper'>
    {labelText ? <label htmlFor={fieldName} className='nxrm-settings-section-field-wrapper-label'>{labelText}</label> : null}
    {descriptionText ? <span className='nxrm-settings-section-field-wrapper-description'>{descriptionText}</span> : null}
    {children}
  </WrapperElement>;
};

SettingsSection.FieldWrapper.propTypes = {
  labelText: PropTypes.string
};

SettingsSection.Footer = function({children}) {
  return <div className='nxrm-settings-section-footer'>
    {children}
  </div>;
};
