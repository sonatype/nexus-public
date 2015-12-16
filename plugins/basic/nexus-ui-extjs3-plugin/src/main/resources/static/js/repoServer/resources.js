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
/*
 * Help text resource file for Nexus resource forms Dependencies:
 * Ext.form.Field.AfterRender override Instructions: To render the "?" next to
 * field label give tha associated value in this config a non-empty string
 * value. Fields with empty strings will not have the help "?" rendered next to
 * the field label.
 */

define('repoServer/resources',['Sonatype/all'], function(){
(function() {

  // Nexus default help text values for re-use in child forms
  var userAgentString = 'A custom fragment to add to the "user-agent" string used in HTTP requests.';
  var queryString = 'These are additional parameters sent along with the HTTP request. They are appended to the url along with a \'?\'. So \'foo=bar&foo2=bar2\' becomes \'HTTP://myurl?foo=bar&foo2=bar2\'';
  var connectionTimeout = 'Time Nexus will wait for a successful connection before retrying. (seconds)';
  var retrievalRetryCount = 'Nexus will make this many connection attempts before giving up.';

  var proxyHostname = 'This is the host name of the HTTP proxy used for remote connections. (no HTTP/HTTPs required...just the host or ip)';
  var proxyPort = 'This is the port number of the HTTP proxy used for remote connections.';
  var nonProxyHosts = 'List of host names to exclude from http proxy.<br/>Regular expressions are supported; example: <code>.*\\.somecompany\\.com</code>';

  var username = 'The username used for authentication to the HTTP proxy.';
  var password = 'The password used for authentication to the HTTP proxy.';
  var privateKey = 'The ssl private key used for authentication to the HTTP proxy.';
  var passphrase = 'The passphrase for the private key.';
  var ntlmHost = 'The Windows NT Lan Manager for authentication.';
  var ntlmDomain = 'The Windows NT Lan Manager domain for authentication.';

  Sonatype.repoServer.resources.help = {

    // Server Config help text
    server : {
      anonUsername : 'The username that will be used to authenticate anonymous users against the security realm.',
      anonPassword : 'The password that will be used to authenticate anonymous users against the security realm.',
      anonymousAccess : 'These fields are used to authenticate anonymous requests. When a request comes in without credentials, Nexus uses the Anonymous Username and Anonymous Password field as a substitute and pass it through the security subsystem. This is used in conjunction with third party security realm integration. For example if you were authenticating against Active Directory, the username might be "Guest" instead of anonymous. If you are using the default Nexus security realm, then these shouldn\'t be changed.',
      systemNotification: 'Nexus can be used to notify users (by email address, or users that are part of a specified role) when one of the following actions is performed:<br><ul><li>&nbsp;&nbsp;Nexus automatically blocks a proxy repository because the remote is unreachable.</li><li>&nbsp;&nbsp;Nexus automatically unblocks a proxy repository because the remote is again reachable.</li></ul>',
      baseUrl : 'This is the Base URL of the Nexus web application.  i.e. http://localhost:8081/nexus',
      forceBaseUrl : 'If checked, this will force all URLs to be built with a Base URL. Otherwise, Base URL will only be used in emails and rss feeds, with other URLs built based on the incoming request.',

      // use default nexus text
      userAgentString : userAgentString,
      queryString : queryString,
      connectionTimeout : connectionTimeout,
      retrievalRetryCount : retrievalRetryCount,

      proxyHostname : proxyHostname,
      proxyPort : proxyPort,
      nonProxyHosts : nonProxyHosts,

      username : username,
      password : password,
      privateKey : privateKey,
      passphrase : passphrase,
      ntlmHost : ntlmHost,
      ntlmDomain : ntlmDomain,

      smtphost : 'The host name of an SMTP server.',
      smtpport : 'The port the SMTP server is listening on.',
      smtpuser : 'The username used to access the SMTP server.',
      smtppass : 'The password used to access the SMTP server.',
      connection : 'Connection level security to be used with SMTP server. Use any of the SSL/TLS provided solutions for greater security.',
      smtpsysemail : 'Default System email address.  This is who the "From" address will be.',
      jiraUsername : 'The Username of a JIRA account.  If left empty default username and password will be used.',
      jiraPassword : 'The Password of a JIRA account.  If left empty default username and password will be used.',
      notificationEmailAddresses : 'A comma seperated list of email addresses to notify.',
      notificationsEnabled : 'Enable the system to send notification messages to the receipients defined below.'
    },

    // Groups Config help text
    groups : {
      id : 'The unique id for the group. This id will become part of the url so it should not contain spaces.',
      name : 'The Group Name which is referenced in the UI and Logs.',
      exposed : 'This controls if the group is published on a URL, if this field is false you will not be able to access this group remotely.'
    },

    // Routes Config help text
    routes : {
      pattern : 'A regular expression used to match the artifact path. ".*" is used to specify all paths. ".*/com/some/company/.*" will match any artifact with "com.some.company" as the group id or artifact id. "^/com/some/company/.*" will match any artifact starting with com/some/company.',
      ruleType : 'There are three types of rules: Inclusive (if the pattern matches, only use the repositories listed below), Exclusive (exclude the repositories listed below) and Blocking (block URLs matching the pattern).',
      group : 'A repository group this route applies to.'
    },

    // Scheduled Services Config help text
    schedules : {
      enabled : 'This flag determines if the task is currently active.  To disable this task for a period of time, de-select this checkbox.',
      name : 'A name for the scheduled task.',
      serviceType : 'The type of service that will be scheduled to run.',
      alertEmail : 'The email address where an email will be sent in case that task execution will fail.',
      serviceSchedule : 'The frequency this task will run.  Manual - this task can only be run manually. Once - run the task once at the specified date/time. Daily - run the task every day at the specified time. Weekly - run the task every week on the specified day at the specified time. Monthly - run the task every month on the specified day(s) and time. Advanced - run the task using the supplied cron string.',
      startDate : 'The date this task should start running.',
      startTime : 'The time this task should start running.',
      recurringTime : 'The time this task should start on days it will run.',
      cronCommand : 'A cron expression that will control the running of the task.'
    },

    // Users help
    users : {
      userId : 'The ID assigned to this user, will be used as the username.',
      firstName : 'The first name of the user.',
      lastName : 'The last name of the user.',
      email : 'Email address, to notify user when necessary.',
      status : 'The current status of the user.',
      roles : 'The roles assigned to this user.',
      password : 'The password required to log the user into the system.',
      reenterPassword : 'Re-enter the password to validate entry.'
    },

    // Roles help
    roles : {
      id : 'The id of this role.',
      name : 'The name of this role.',
      description : 'The description of this role.',
      sessionTimeout : 'The number of minutes to wait before timing out an idle user session.',
      rolesAndPrivileges : 'Roles and privileges contained in this Role.'
    },

    // Privileges help
    privileges : {
      name : 'The name of this privilege.',
      description : 'The description of this privilege.',
      type : 'The type of privilege.  Only "Repository Target" type privileges can be managed by the user.',
      repositoryOrGroup : 'The repository or repository group this privilege will be associated with.',
      repositoryTarget : 'The Repository Target that will be applied with this privilege.'
    },

    // Repositories Config help text
    repos : {
      // shared across types of repositories
      id : 'The unique id for the repository. This id will become part of the url so it should not contain spaces.',
      name : 'The Repository Name which is referenced in the UI and Logs.',
      repoType : 'Nexus supports 3 repository types: Hosted = Normal repository owned by this Nexus instance, Proxy = Retrieve artifacts from the remote repository and store them locally, Virtual = A logical view of another repository configured in Nexus (For example, to provide a Maven 1 view of an existing Maven 2 repository)',
      repoPolicy : 'Repositories can store either all Release artifacts or all Snapshot artifacts.',
      defaultLocalStorageUrl : 'This is the location on the file system used to host the artifacts. It is contained by the Working Directory set in the Server configuration.',
      overrideLocalStorageUrl : 'This is used to override the default local storage. Leave it blank to use the default. Note, file:/{drive-letter}:/ urls are supported in windows.  All other operating systems will use file:// .',
      writePolicy : 'This controls if users are allowed to deploy and/or update artifacts in this repository. (Hosted repositories only)',
      browseable : 'This controls if users can browse the contents of the repository via their web browser.',
      indexable : 'This controls if the artifacts contained by this repository are indexed and thus searchable.',
      exposed : 'This controls if the repository is published on a URL, if this field is false you will not be able to access this repository remotely.',
      notFoundCacheTTL : 'This controls how long to cache the fact that a file was not found in the repository.',
      artifactMaxAge : 'This controls how long to cache the artifacts in the repository before rechecking the remote repository. In a release repository, this value should be -1 (infinite) as release artifacts shouldn\'t change.',
      metadataMaxAge : 'This controls how long to cache the metadata in the repository before rechecking the remote repository. Unlike artifact max age, this value should not be infinite or Maven won\'t discover new artifact releases.',
      itemMaxAge : 'Repositories may contain resources that are neither artifacts identified by GAV coordinates or metadata. This value controls how long to cache such items in the repository before rechecking the remote repository.',
      provider : 'This is the content provider of the repository.',
      format : 'This is the format of the repository.  Maven1 = A Maven 1.x formatted view of the repository.  Maven2 = A Maven 2.x formatted view of the repository.',

      // virtual
      shadowOf : 'This is the id of the physical repository being presented as a logical view by this proxy.',
      syncAtStartup : 'The links are normally updated as changes are made to the repository, if changes may be made while Nexus is offline, the virtual repo should be synchronized at startup.',

      // proxy
      remoteStorageUrl : 'This is the location of the remote repository being proxied. Only HTTP/HTTPs urls are currently supported.',
      downloadRemoteIndexes : 'Indicates if the index stored on the remote repository should be downloaded and used for local searches.',
      autoBlockActive : 'Flag to enable Auto Blocking for this proxy repository. If enabled, Nexus will auto-block outbound connections on this repository if remote peer is detected as unreachable/unresponsive. Auto-blocked repositories will still try to detect remote peer availability, and will auto-unblock the proxy if remote peer detected as reachable/healthy. Auto-blocked repositories behaves exactly the same as user blocked proxy repositories, except they will auto-unblock themselves too.',
      checksumPolicy : 'The checksum policy for this repository: Ignore: Don\'t check remote checksums. Warn: Log a warning if the checksum is bad but serve the artifact anyway. (Default...there are currently known checksum errors on Central). StrictIfExists: Do not serve the artifact if the checksum exists but is invalid. Strict: Require that a checksum exists on the remote repository and that it is valid.',
      fileTypeValidation : 'Flag to check the remote file\'s content to see if it is valid. (e.g. not html error page), handy when you cannot enable strict checksum checking.',
      // remote
      remoteUsername : 'The username used for authentication to the remote repository.',
      remotePassword : 'The password used for authentication to the remote repository.',
      remotePrivateKey : 'The ssl private key used for authentication to the remote repository.',
      remotePassphrase : 'The passphase for the private key.',
      remoteNtlmHost : 'The Windows NT Lan Manager for authentication to the remote repository.',
      remoteNtlmDomain : 'The Windows NT Lan Manager domain for authentication to the remote repository.',

      // proxy override fields
      userAgentString : userAgentString,
      queryString : queryString,
      connectionTimeout : connectionTimeout,
      retrievalRetryCount : retrievalRetryCount,

      proxyHostname : proxyHostname,
      proxyPort : proxyPort,

      username : username,
      password : password,
      privateKey : privateKey,
      passphrase : passphrase,
      ntlmHost : ntlmHost,
      ntlmDomain : ntlmDomain
    },

    // artifact upload help text
    artifact : {
      groupId : 'Group ID',
      artifactId : 'Maven artifact ID',
      version : 'Artifact version',
      packaging : 'Packaging type',
      classifier : 'Classifier of the uploaded artifact.  If not supplied, no classifier will be appended to the artifact.',
      extension : 'Extension of the uploaded artifact.  If not supplied, the default extension (derived from the packaging type) will be used.',
      autoguess : 'Automatically guess the attributes by parsing the artifact file name.',
      description : 'A description of the artifact(s) that will be uploaded, be as descriptive as necessary.',
      gavDefinition : 'How the GAV parameters will be retrieved, either from a POM file or manually entered'
    },

    cronBriefHelp : {
      text : '<p>Provides a parser and evaluator for unix-like cron expressions.</p>' + '<table style="font-size: 11px">' + '<tr><td><b>Example</b></td><td><b>Description</b></td></tr>'
          + '<tr><td><code>0 0 12 * * ?</code></td><td>Fire at 12pm (noon) every day</td></tr>' + '<tr><td><code>0 15 10 ? * *</code></td><td>Fire at 10:15am every day</td></tr>'
          + '<tr><td><code>0 15 10 * * ?</code></td><td>Fire at 10:15am every day</td></tr>' + '<tr><td><code>0 15 10 * * ? *</code></td><td>Fire at 10:15am every day</td></tr>'
          + '<tr><td><code>0 15 10 * * ? 2005</code></td><td>Fire at 10:15am every day during the year 2005</td></tr>' + '<tr><td><code>0 * 14 * * ?</code></td><td>Fire every minute starting at 2pm and ending at 2:59pm, every day</td></tr>'
          + '<tr><td><code>0 0/5 14 * * ?</code></td><td>Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day</td></tr>'
          + '<tr valign="top"><td><code>0 0/5 14,18 * * ?</code></td><td>Fire every 5 minutes starting at 2pm and ending at 2:55pm, AND fire every 5 minutes starting at 6pm and ending at 6:55pm, every day</td></tr>'
          + '<tr><td><code>0 0-5 14 * * ?</code></td><td>Fire every minute starting at 2pm and ending at 2:05pm, every day</td></tr>'
          + '<tr><td><code>0 10,44 14 ? 3 WED</code></td><td>Fire at 2:10pm and at 2:44pm every Wednesday in the month of March.</td></tr>'
          + '<tr><td><code>0 15 10 ? * MON-FRI</code></td><td>Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday</td></tr>' + '<tr><td><code>0 15 10 15 * ?</code></td><td>Fire at 10:15am on the 15th day of every month</td></tr>'
          + '<tr><td><code>0 15 10 L * ?</code></td><td>Fire at 10:15am on the last day of every month</td></tr>' + '<tr><td><code>0 15 10 ? * 6L</code></td><td>Fire at 10:15am on the last Friday of every month</td></tr>'
          + '<tr valign="top"><td nowrap><code>0 15 10 ? * 6L 2002-2005 &nbsp; &nbsp;</code></td><td>Fire at 10:15am on every last friday of every month during the years 2002, 2003, 2004 and 2005</td></tr>'
          + '<tr><td><code>0 15 10 ? * 6#3</code></td><td>Fire at 10:15am on the third Friday of every month</td></tr>' + '</table>'
    },

    cronBigHelp : {
      text : '<br>Cron expressions provide the ability to specify complex time combinations such as - At 8:00am every Monday through Friday - or - At 1:30am every last Friday of the month.<br><br>Cron expressions are comprised of 6 required fields and one optional field separated by white space. The fields respectively are described as follows: <table cellspacing=&quot;8&quot;><tr><th align=&quot;left&quot;>Field Name</th><th align=&quot;left&quot;>&nbsp;</th><th align=&quot;left&quot;>Allowed Values</th><th align=&quot;left&quot;>&nbsp;</th><th align=&quot;left&quot;>Allowed Special Characters</th></tr><tr><td align=&quot;left&quot;><code>Seconds</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>0-59</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>, - * /</code></td></tr><tr><td align=&quot;left&quot;><code>Minutes</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>0-59</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>, - * /</code></td></tr><tr><td align=&quot;left&quot;><code>Hours</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>0-23</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>, - * /</code></td></tr><tr><td align=&quot;left&quot;><code>Day-of-month</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>1-31</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>, - * ? / L W</code></td></tr><tr><td align=&quot;left&quot;><code>Month</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>1-12 or JAN-DEC</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>, - * /</code></td></tr><tr><td align=&quot;left&quot;><code>Day-of-Week</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>1-7 or SUN-SAT</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>, - * ? / L #</code></td></tr><tr><td align=&quot;left&quot;><code>Year (Optional)</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>empty, 1970-2099</code></td><td align=&quot;left&quot;>&nbsp;</th><td align=&quot;left&quot;><code>, - * /</code></td></tr></table><br><br>The * character is used to specify all values. For example, * in the minute field means every minute.<br><br>The ? character is allowed for the day-of-month and day-of-week fields. It is used to specify no specific value.This is useful when you need to specify something in one of the two fields, but not the other.<br><br>The - character is used to specify ranges For example &quot;10-12&quot; in the hour field means &quot;the hours 10,11 and 12&quot;.<br><br>The , character is used to specify additional values. For example &quot;MON,WED,FRI&quot; in the day-of-week field means &quot;the days Monday, Wednesday, and Friday&quot;.<br><br>The / character is used to specify increments. For example &quot;0/15&quot; in the seconds field means &quot;the seconds 0, 15, 30, and 45&quot;. And &quot;5/15&quot; in the seconds field means &quot;the seconds 5, 20, 35, and 50&quot;. Specifying * before the / is equivalent to specifying 0 is the value to start with. Essentially, for each field in the expression, there is a set of numbers that can be turned on or off. For seconds and minutes, the numbers range from 0 to 59. For hours 0 to 23, for days of the month 0 to 31, and for months 1 to 12. The &quot;/&quot; character simply helps you turn on every &quot;nth&quot; value in the given set. Thus &quot;7/6&quot; in the month field only turns on month &quot;7&quot;, it does NOT mean every 6th month, please note that subtlety.<br><br>The L character is allowed for the day-of-month and day-of-week fields. This character is short-hand for &quot;last&quot;, but it has different meaning in each of the two fields. For example, the value &quot;L&quot; in the day-of-month field means &quot;the last day of the month&quot; - day 31 for January, day 28 for February on non-leap years. If used in the day-of-week field by itself, it simply means &quot;7&quot; or &quot;SAT&quot;. But if used in the day-of-week field after another value, it means &quot;the last xxx day of the month&quot; - for example &quot;6L&quot; means &quot;the last friday of the month&quot;. When using the L option, it is important not to specify lists, or ranges of values, as you will get confusing results.<br><br>The W character is allowed for the day-of-month field. This character is used to specify the weekday (Monday-Friday) nearest the given day. As an example, if you were to specify &quot;15W&quot; as the value for the day-of-month field, the meaning is: &quot;the nearest weekday to the 15th of the month&quot;. So if the 15th is a Saturday, the trigger will fire on Friday the 14th. If the 15th is a Sunday, the trigger will fire on Monday the 16th. If the 15th is a Tuesday, then it will fire on Tuesday the 15th. However if you specify &quot;1W&quot; as the value for day-of-month, and the 1st is a Saturday, the trigger will fire on Monday the 3rd, as it will not jump over the boundary of a months days. The W character can only be specified when the day-of-month is a single day, not a range or list of days.<br><br>The L and W characters can also be combined for the day-of-month expression to yield LW, which translates to &quot;last weekday of the month&quot;.<br><br>The # character is allowed for the day-of-week field. This character is used to specify &quot;the nth&quot; XXX day of the month. For example, the value of &quot;6#3&quot; in the day-of-week field means the third Friday of the month (day 6 = Friday and &quot;#3&quot; = the 3rd one in the month). Other examples: &quot;2#1&quot; = the first Monday of the month and &quot;4#5&quot; = the fifth Wednesday of the month. Note that if you specify &quot;#5&quot; and there is not 5 of the given day-of-week in the month, then no firing will occur that month.<br><br>The legal characters and the names of months and days of the week are not case sensitive.<br><br><b>NOTES:</b><ul><li>Support for specifying both a day-of-week and a day-of-month value is not complete (you will need to use the ? character in one of these fields). </li><li>Overflowing ranges is supported - that is, having a larger number on the left hand side than the right. You might do 22-2 to catch 10 o clock at night until 2 o clock in the morning, or you might have NOV-FEB. It is very important to note that overuse of overflowing ranges creates ranges that do not make sense and no effort has been made to determine which interpretation CronExpression chooses. An example would be 0 0 14-6 ? * FRI-MON. </li></ul></p>'
    },

    repoTargets : {
      name : 'The name of the repository target.',
      contentClass : 'The content class of the repository target. It will be matched only against repositories with the same content class.',
      pattern : 'Enter a pattern expression and click "Add" to add it to the list. Regular expressions are used to match the artifact path. ".*" is used to specify all paths. ".*/com/some/company/.*" will match any artifact with "com.some.company" as the group id or artifact id. "^/com/some/company/.*" will match any artifact starting with com/some/company.'
    },

    log : {
      rootLoggerLevel : 'The root logger level for your logging configuration.',
      rootLoggerAppenders : 'The root logger appenders for your logging configuration.',
      fileAppenderLocation : 'The file appender location for your logging configuration.',
      fileAppenderPattern : 'The file appender pattern for your logging configuration.'
    },

    repoMirrors : {
      mirrorUrl : 'A URL that acts as a mirror for this repository.'
    }
  };

})();
});

