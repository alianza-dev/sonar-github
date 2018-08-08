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

import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.google.common.collect.ImmutableMap;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class PullRequestProjectBuilderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private PullRequestProjectBuilder pullRequestProjectBuilder;
  private PullRequestFacades facades;
  private MapSettings settings;
  private AnalysisMode mode;

  @Before
  public void prepare() {
    settings = new MapSettings(new PropertyDefinitions(GitHubPlugin.class));
    facades = mock(PullRequestFacades.class);
    mode = mock(AnalysisMode.class);
    pullRequestProjectBuilder = new PullRequestProjectBuilder(new GitHubPluginConfiguration(settings, new System2()), facades, mode);

  }

  @Test
  public void shouldDoNothing() {
    pullRequestProjectBuilder.build(null);
    verifyZeroInteractions(facades);
  }

  @Test
  public void shouldFailIfNotPreview() {
    settings.setProperty(GitHubPlugin.GITHUB_PULL_REQUEST, "1");

    thrown.expect(MessageException.class);
    thrown.expectMessage("The GitHub plugin is only intended to be used in preview or issues mode. Please set 'sonar.analysis.mode'.");

    pullRequestProjectBuilder.build(null);
  }

  @Test
  public void shouldNotFailIfIssues() {
    settings.setProperty(GitHubPlugin.GITHUB_PULL_REQUEST, "1");
    when(mode.isIssues()).thenReturn(true);

    PullRequestFacade mockFacade = mock(PullRequestFacade.class);
    when(facades.getPullRequestFacades()).thenReturn(ImmutableMap.of(1, mockFacade));

    pullRequestProjectBuilder.build(mock(ProjectBuilder.Context.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS)));

    verify(mockFacade).init(eq(1), any(File.class));
  }
}
