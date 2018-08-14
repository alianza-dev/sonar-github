/*
 * SonarQube :: GitHub Plugin
 * Copyright (C) 2015-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.github;

import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;

@Properties({
  @Property(
    key = GitHubPlugin.GITHUB_ENDPOINT,
    defaultValue = "https://api.github.com",
    name = "GitHub API Endpoint",
    description = "URL to access GitHub WS API. Default value is fine for public GitHub. Can be modified for GitHub enterprise.",
    global = true),
  @Property(
    key = GitHubPlugin.GITHUB_OAUTH,
    name = "GitHub OAuth token",
    description = "Authentication token",
    global = false,
    type = PropertyType.PASSWORD),
  @Property(
    key = GitHubPlugin.GITHUB_REPO,
    name = "GitHub repository",
    description = "GitHub repository for this project. Will be guessed from '" + CoreProperties.LINKS_SOURCES_DEV + "' if present",
    project = false,
    global = false),
  @Property(
    key = GitHubPlugin.GITHUB_PULL_REQUEST,
    name = "GitHub Pull Request Number(s)",
    description = "One or more comma separated pull request numbers",
    project = false,
    module = false,
    global = false,
    type = PropertyType.STRING),
  @Property(
    key = GitHubPlugin.GITHUB_DISABLE_INLINE_COMMENTS,
    defaultValue = "false",
    name = "Disable issue reporting as inline comments",
    description = "Issues will not be reported as inline comments but only in the global summary comment",
    project = true,
    global = true,
    type = PropertyType.BOOLEAN),
  @Property(
    key = GitHubPlugin.GITHUB_IGNORE_UNCHANGED_LINES,
    defaultValue = "false",
    name = "Ignore lines that were not changed in the PR",
    description = "Don't report issues for lines that were not changed by any of the commits in the PR being scanned",
    project = true,
    global = true,
    type = PropertyType.BOOLEAN),
  @Property(
    key = GitHubPlugin.GITHUB_ALWAYS_INCLUDE_UNUSED,
    defaultValue = "true",
    name = "Always include 'unused' issues in the PR",
    description = "Lines may become unused without being changed. This setting always reports unused issues in files touched in the PR whether the line was changed in the PR or not.",
    project = true,
    global = true,
    type = PropertyType.BOOLEAN),
  @Property(
    key = GitHubPlugin.GITHUB_USE_REVIEW,
    defaultValue = "false",
    name = "Add all comments in a review",
    description = "Use a PR review to contain the comments - so github only sends a single email per analysis",
    project = true,
    global = true,
    type = PropertyType.BOOLEAN)
})
public class GitHubPlugin implements Plugin {

  public static final String GITHUB_ENDPOINT = "sonar.github.endpoint";
  public static final String GITHUB_OAUTH = "sonar.github.oauth";
  public static final String GITHUB_REPO = "sonar.github.repository";
  public static final String GITHUB_PULL_REQUEST = "sonar.github.pullRequest";
  public static final String GITHUB_DISABLE_INLINE_COMMENTS = "sonar.github.disableInlineComments";
  public static final String GITHUB_IGNORE_UNCHANGED_LINES = "sonar.github.ignoreUnchangedLines";
  public static final String GITHUB_ALWAYS_INCLUDE_UNUSED = "sonar.github.alwaysIncludeUnused";
  public static final String GITHUB_USE_REVIEW = "sonar.github.useReview";

  @Override
  public void define(Context context) {
    context.addExtensions(
      PullRequestIssuePostJob.class,
      GitHubPluginConfiguration.class,
      PullRequestProjectBuilder.class,
      PullRequestFacades.class,
      MarkDownUtils.class);
  }

}
