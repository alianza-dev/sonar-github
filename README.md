# SonarQube GitHub Plugin

[![Build Status](https://travis-ci.org/SonarSource/sonar-github.svg?branch=master)](https://travis-ci.org/SonarSource/sonar-github) [![Quality Gate](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.auth.github%3Asonar-auth-github-plugin&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.auth.github%3Asonar-auth-github-plugin)

### License

Copyright 2015-2017 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)

## Forked by Alianza to add:

1. **sonar.github.ignoreUnchangedLines** A server-side configuration option to restrict analysis comments to that are actually being changed by the pull request. The default behavior is to report on every code smell in every file that is touched by the change and was annoying when you had to change a few lines in a large file full of bad code - we wanted to be able to focus first on reducing the bad code being contributed, so this feature helps with that.
1. **sonar.github.alwaysIncludeUnused** Lines may become unused without being changed, and if you are using sonar.github.ignoreUnchangedLines these will not be reported on the pull request. This setting always reports unused issues in files touched in the PR whether the line was changed in the PR or not.
1. **sonar.github.useReview** A server-side configuration option to enable use of the new(ish) github review process. When the plugin is going to comment on the pull request, it creates a review, adds the comments to the review, adds the summary as the review body, and posts as either requests changes (if there are any blockers/criticals) or just as a comment. This means that only one email is sent per analysis. On subsequent commits the previous review is dismissed, and a new one added.
1. **sonar.github.pullRequest** Changed to support multiple comma-separated values so a scan can update more than one PR that is based on the same branch/commit. 

You can easily build the forked plugin, copy the jar file to the relevant folder in your SonarQube installation, restart your server, then configure the new features in the Administration->GitHub section on SonarQube. 
