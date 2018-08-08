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

import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;

import java.util.HashMap;
import java.util.Map;

/**
 * Manage Pull Request Facades
 */
@ScannerSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class PullRequestFacades {
    private final GitHubPluginConfiguration config;
    private Map<Integer, PullRequestFacade> pullRequestFacades = new HashMap();

    public PullRequestFacades(GitHubPluginConfiguration config) {
        this.config = config;
        for (Integer pullRequestNumber : config.pullRequestNumbers()) {
            pullRequestFacades.put(pullRequestNumber, new PullRequestFacade(config));
        }
    }

    public Map<Integer, PullRequestFacade> getPullRequestFacades() {
        return pullRequestFacades;
    }
}
