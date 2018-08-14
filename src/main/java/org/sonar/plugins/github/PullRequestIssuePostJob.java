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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHCommitState;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Compute comments to be added on the pull request.
 */
public class PullRequestIssuePostJob implements PostJob {
  private static final Logger LOG = Loggers.get(PullRequestFacade.class);

  private static final Comparator<PostJobIssue> ISSUE_COMPARATOR = new IssueComparator();

  private final PullRequestFacades pullRequestFacades;
  private final GitHubPluginConfiguration gitHubPluginConfiguration;
  private final MarkDownUtils markDownUtils;

  public PullRequestIssuePostJob(GitHubPluginConfiguration gitHubPluginConfiguration, PullRequestFacades pullRequestFacades, MarkDownUtils markDownUtils) {
    this.gitHubPluginConfiguration = gitHubPluginConfiguration;
    this.pullRequestFacades = pullRequestFacades;
    this.markDownUtils = markDownUtils;
  }

  @Override
  public void describe(PostJobDescriptor descriptor) {
    descriptor
      .name("GitHub Pull Request Issue Publisher")
      .requireProperty(GitHubPlugin.GITHUB_PULL_REQUEST);
  }

  @Override
  public void execute(PostJobContext context) {
    for(PullRequestFacade pullRequestFacade : pullRequestFacades.getPullRequestFacades().values()) {
      GlobalReport report = new GlobalReport(markDownUtils, gitHubPluginConfiguration.tryReportIssuesInline());
      try {
        Map<InputFile, Map<Integer, StringBuilder>> commentsToBeAddedByLine = processIssues(report, pullRequestFacade, context.issues());

        updateReviewComments(pullRequestFacade, commentsToBeAddedByLine);

        pullRequestFacade.deleteOutdatedComments();

        pullRequestFacade.createOrUpdateGlobalComments(report.hasNewIssue() ? report.formatForMarkdown() : null);

        pullRequestFacade.createOrUpdateSonarQubeStatus(report.getStatus(),
                                                        report.getStatusDescription(),
                                                        report.hasNewIssue());
      } catch (Exception e) {
        LOG.error("SonarQube analysis failed to complete the review of this pull request", e);
        pullRequestFacade.createOrUpdateSonarQubeStatus(GHCommitState.ERROR,
                                                        StringUtils.abbreviate(
                                                                "SonarQube analysis failed: " + e.getMessage(), 140),
                                                        false);
      }
    }
  }

  private Map<InputFile, Map<Integer, StringBuilder>> processIssues(GlobalReport report, PullRequestFacade pullRequestFacade, Iterable<PostJobIssue> issues) {
    Map<InputFile, Map<Integer, StringBuilder>> commentToBeAddedByFileAndByLine = new HashMap<>();

    StreamSupport.stream(issues.spliterator(), false)
      .filter(PostJobIssue::isNew)
      // SONARGITUB-13 Ignore issues on files not modified by the P/R
      .filter(i -> {
        InputComponent inputComponent = i.inputComponent();
          boolean result = keepIssue(pullRequestFacade, i, inputComponent);
          if (result) {
            LOG.debug("result: {}, inputComponent: {}, file?: {}, hasFile?: {}, ignoreUnchanged?: {}, line: {}, ruleKey: {}, message: {}",
                     result,
                     inputComponent,
                     Optional.ofNullable(inputComponent).map(InputComponent::isFile).orElse(false),
                     Optional.ofNullable(inputComponent).filter(InputComponent::isFile).map(ic->pullRequestFacade.hasFile((InputFile) ic)).orElse(false),
                     gitHubPluginConfiguration.ignoreUnchangedLines(),
                     i.line(),
                     i.ruleKey(),
                     i.message());
          }
          return result;
      })
      .sorted(ISSUE_COMPARATOR)
      .forEach(i -> processIssue(report, pullRequestFacade, commentToBeAddedByFileAndByLine, i));
    return commentToBeAddedByFileAndByLine;
  }

  private boolean keepIssue(PullRequestFacade pullRequestFacade, PostJobIssue issue, InputComponent inputComponent) {
    if (inputComponent == null || !inputComponent.isFile()) {
      return true;
    }
    if (pullRequestFacade.hasFile((InputFile) inputComponent)) {
      if (gitHubPluginConfiguration.alwaysIncludeUnused() && isUnusedType(issue)){
        return true;
      }
      if (gitHubPluginConfiguration.ignoreUnchangedLines()) {
        if (issue.line() == null) {
          return true;
        }
        return pullRequestFacade.hasFileLine((InputFile) inputComponent, issue.line());
      }
      return true;
    }
    return false;
  }

  private boolean isUnusedType(PostJobIssue issue) {
    switch(issue.ruleKey().toString()){
      case "squid:S1068":
      case "squid:S1854":
      case "squid:UselessImportCheck":
        return true;
      default:
        return false;
    }
  }

  private void processIssue(GlobalReport report, PullRequestFacade pullRequestFacade, Map<InputFile, Map<Integer, StringBuilder>> commentToBeAddedByFileAndByLine, PostJobIssue issue) {
    boolean reportedInline = false;
    InputComponent inputComponent = issue.inputComponent();
    if (gitHubPluginConfiguration.tryReportIssuesInline() && inputComponent != null && inputComponent.isFile()) {
      reportedInline = tryReportInline(pullRequestFacade, commentToBeAddedByFileAndByLine, issue, (InputFile) inputComponent);
    }
    LOG.debug("reportedInLine: {}, inputComponent: {}, line: {}, message: {}", reportedInline, inputComponent, issue.line(), issue.message());
    report.process(issue, pullRequestFacade.getGithubUrl(inputComponent, issue.line()), reportedInline);
  }

  private boolean tryReportInline(PullRequestFacade pullRequestFacade, Map<InputFile, Map<Integer, StringBuilder>> commentToBeAddedByFileAndByLine, PostJobIssue issue, InputFile inputFile) {
    Integer lineOrNull = issue.line();
    if (lineOrNull != null) {
      int line = lineOrNull.intValue();
      if (pullRequestFacade.hasFileLine(inputFile, line)) {
        String message = issue.message();
        String ruleKey = issue.ruleKey().toString();
        if (!commentToBeAddedByFileAndByLine.containsKey(inputFile)) {
          commentToBeAddedByFileAndByLine.put(inputFile, new HashMap<Integer, StringBuilder>());
        }
        Map<Integer, StringBuilder> commentsByLine = commentToBeAddedByFileAndByLine.get(inputFile);
        if (!commentsByLine.containsKey(line)) {
          commentsByLine.put(line, new StringBuilder());
        }
        commentsByLine.get(line).append(markDownUtils.inlineIssue(issue.severity(), message, ruleKey)).append("\n");
        return true;
      }
    }
    return false;
  }

  private void updateReviewComments(PullRequestFacade pullRequestFacade, Map<InputFile, Map<Integer, StringBuilder>> commentsToBeAddedByLine) {
    for (Map.Entry<InputFile, Map<Integer, StringBuilder>> entry : commentsToBeAddedByLine.entrySet()) {
      for (Map.Entry<Integer, StringBuilder> entryPerLine : entry.getValue().entrySet()) {
        String body = entryPerLine.getValue().toString();
        pullRequestFacade.createOrUpdateReviewComment(entry.getKey(), entryPerLine.getKey(), body);
      }
    }
  }

}
