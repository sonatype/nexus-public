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
/*global Ext, NX*/

/**
 * Application strings
 *
 * @since 3.0
 */
Ext.define('NX.app.PluginStrings', {
  '@aggregate_priority': 90,

  singleton: true,

  requires: [
    'NX.I18n'
  ],

  //
  // Note: Symbols follow the following naming convention:
  // <Class>_<Name>_<Component or Attribute>
  //

  keys: {
    // Buttons
    Button_Back: 'Back',
    Button_Cancel: 'Cancel',
    Button_Close: 'Close',
    Button_Create: 'Create',
    Button_Discard: 'Discard',
    Button_Next: 'Next',
    Button_Save: 'Save',

    Column_No_Data: 'No data',

    // Header
    Header_Panel_Logo_Text: 'Sonatype Nexus Repository Manager',
    Header_BrowseMode_Title: 'Browse',
    Header_BrowseMode_Tooltip: 'Browse server contents',
    Header_AdminMode_Title: 'Administration',
    Header_AdminMode_Tooltip: 'Server administration and configuration',
    Header_Health_Tooltip: 'System Status',
    Header_QuickSearch_Empty: 'Search components',
    Header_QuickSearch_Tooltip: 'Quick component keyword search',
    Header_Refresh_Tooltip: 'Refresh current view and data',
    Refresh_Message: 'Refreshed',
    Header_UserMode_Title: 'User',
    User_Tooltip: 'Hi, {0}. Manage your user account.',
    Header_SignIn_Text: 'Sign in',
    Header_SignIn_Tooltip: 'Sign in',
    Header_SignOut_Text: 'Sign out',
    Header_SignOut_Tooltip: 'Sign out',
    Header_Help_Tooltip: 'Help',
    Help_Feature_Text: 'Help for: ',
    Header_Help_Feature_Tooltip: 'Help and documentation for the currently selected feature',
    Header_Help_About_Text: 'About',
    Header_Help_About_Tooltip: 'About Nexus Repository Manager',
    Header_Help_Documentation_Text: 'Documentation',
    Header_Help_Documentation_Tooltip: 'Product documentation',
    Header_Help_KB_Text: 'Knowledge base',
    Header_Help_KB_Tooltip: 'Knowledge base',
    Header_Help_Community_Text: 'Community',
    Header_Help_Community_Tooltip: 'Community information',
    Header_Help_Issues_Text: 'Issue tracker',
    Header_Help_Issues_Tooltip: 'Issue and bug tracker',
    Header_Help_Support_Text: 'Support',
    Header_Help_Support_Tooltip: 'Product support',

    // Footer
    Footer_Panel_HTML: 'Copyright &copy; 2008-present, Sonatype Inc. All rights reserved.',

    // Sign in
    SignIn_Title: 'Sign In',
    User_SignIn_Mask: 'Signing in&hellip;',
    SignIn_Username_Empty: 'Username',
    SignIn_Password_Empty: 'Password',
    SignIn_Submit_Button: 'Sign in',
    SignIn_Cancel_Button: '@Button_Cancel',

    // Filter box
    Grid_Plugin_FilterBox_Empty: 'Filter',

    // Dialogs
    Dialogs_Info_Title: 'Information',
    Dialogs_Error_Title: 'Error',
    Dialogs_Error_Message: 'Operation failed',
    Add_Submit_Button: '@Button_Create',
    Add_Cancel_Button: '@Button_Cancel',
    ChangeOrderWindow_Submit_Button: '@Button_Save',
    ChangeOrderWindow_Cancel_Button: '@Button_Cancel',

    // Server
    User_ConnectFailure_Message: 'Operation failed as server could not be contacted',
    State_Reconnected_Message: 'Server reconnected',
    State_Disconnected_Message: 'Server disconnected',
    UiSessionTimeout_Expire_Message: 'Session is about to expire',
    UiSessionTimeout_Expired_Message: 'Session expired after being inactive for {0} minutes',
    User_SignedIn_Message: 'User signed in: {0}',
    User_SignedOut_Message: 'User signed out',
    User_Credentials_Message: 'Incorrect username or password, or no permission to use the application.',
    Util_DownloadHelper_Download_Message: 'Download initiated',
    Windows_Popup_Message: 'Window pop-up was blocked!',

    // License
    State_Installed_Message: 'License installed',
    State_Uninstalled_Message: 'License uninstalled',
    State_License_Expiry: 'Your license will expire in {0} days. <a href="http://links.sonatype.com/products/nexus/pro/store">Contact us to renew.</a>',
    State_License_Expired: 'Your license has expired. <a href="http://links.sonatype.com/products/nexus/pro/store">Contact us to renew.</a>',
    State_License_Invalid_Message: 'Your license has been detected as missing or invalid. Upload a valid license to proceed.',

    // About modal
    AboutWindow_Title: 'About Nexus Repository Manager',
    AboutWindow_About_Title: 'Copyright',
    AboutWindow_License_Tab: 'License',

    // Authentication modal
    Authenticate_Title: 'Authenticate',
    Authenticate_Help_Text: 'You have requested an operation which requires validation of your credentials.',
    User_Controller_Authenticate_Mask: 'Authenticate&hellip;',
    User_View_Authenticate_Submit_Button: 'Authenticate',
    User_Retrieving_Mask: 'Retrieving authentication token&hellip;',
    Authenticate_Cancel_Button: '@Button_Cancel',

    // Expiry modal
    ExpireSession_Title: 'Session',
    ExpireSession_Help_Text: 'Session is about to expire',
    UiSessionTimeout_Expire_Text: 'Session will expire in {0} seconds',
    SignedOut_Text: 'Your session has expired. Please sign in.',
    ExpireSession_Cancel_Button: '@Button_Cancel',
    ExpireSession_SignIn_Button: 'Sign in',

    // Unsaved changes modal
    UnsavedChanges_Title: 'Unsaved changes',
    UnsavedChanges_Help_HTML: '<p>Do you want to discard your changes?</p>',
    UnsavedChanges_Discard_Button: 'Discard changes',
    UnsavedChanges_Back_Button: 'Go back',
    Menu_Browser_Title: 'You will lose your unsaved changes',

    // Unsupported browser
    UnsupportedBrowser_Title: 'The browser you are using is not supported',
    UnsupportedBrowser_Alternatives_Text: 'Below is a list of alternatives that are supported by this web application',
    UnsupportedBrowser_Continue_Button: 'Ignore and continue',

    // 404
    Feature_NotFoundPath_Text: 'Path "{0}" not found',
    Feature_NotFound_Text: 'Path not found',

    // Buttons
    SettingsForm_Save_Button: '@Button_Save',
    SettingsForm_Discard_Button: '@Button_Discard',
    Ldap_LdapServerConnectionAdd_Text: '@Button_Next',

    // Item selector
    Form_Field_ItemSelector_Empty: 'Filter',

    // Settings form
    SettingsForm_Load_Message: 'Loading',
    SettingsForm_Submit_Message: 'Saving',

    // Browse -> Welcome
    Dashboard_Title: 'Welcome',
    Dashboard_Description: 'Learn about Sonatype Nexus Repository Manager',

    // Field validation messages
    Util_Validator_Text: 'Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed and may not start with underscore or dot.',
    Util_Validator_Hostname: 'Hostname must be valid',
    Util_Validator_Trim: 'A Role ID may not start or end with a space.',
    Util_Validator_Url: 'This field should be a URL in the format "http://www.example.com"',

    // Wizard
    Wizard_Next: '@Button_Next',
    Wizard_Back: '@Button_Back',
    Wizard_Cancel: '@Button_Cancel',
    Wizard_Screen_Progress: '{0} of {1}',

    // SearchBoxTip
    SearchBoxTip_ExactMatch: 'Use <b>""</b> for exact match - "example"',
    SearchBoxTip_Wildcard: 'Use <b>*</b> or <b>?</b> for wildcards - ex?mpl*',
    SearchBoxTip_LearnMore: 'Learn more...',

    // DependencySnippet Panel
    DependencySnippetPanel_Title: 'Usage',
    DependencySnippetPanel_Copy_Button_Tooltip: 'Copy snippet to clipboard'

  }
}, function(obj) {
  NX.I18n.register(obj);
});

