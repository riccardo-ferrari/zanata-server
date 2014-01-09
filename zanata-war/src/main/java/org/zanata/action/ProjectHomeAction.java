/*
 *
 *  * Copyright 2014, Red Hat, Inc. and individual contributors as indicated by the
 *  * @author tags. See the copyright.txt file in the distribution for a full
 *  * listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it under the
 *  * terms of the GNU Lesser General Public License as published by the Free
 *  * Software Foundation; either version 2.1 of the License, or (at your option)
 *  * any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 *  * details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with this software; if not, write to the Free Software Foundation,
 *  * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 *  * site: http://www.fsf.org.
 */

package org.zanata.action;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.annotation.CachedMethodResult;
import org.zanata.common.EntityStatus;
import org.zanata.dao.ProjectDAO;
import org.zanata.model.Activity;
import org.zanata.model.HAccount;
import org.zanata.model.HLocale;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.ActivityService;
import org.zanata.service.LocaleService;
import org.zanata.service.VersionStateCache;
import org.zanata.ui.model.statistic.WordStatistic;
import org.zanata.util.StatisticsUtil;
import org.zanata.util.UrlUtil;
import org.zanata.util.ZanataMessages;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Name("projectHomeAction")
@Scope(ScopeType.PAGE)
public class ProjectHomeAction extends AbstractSortAction implements
        Serializable {

    @In
    private ActivityService activityServiceImpl;

    @In
    private LocaleService localeServiceImpl;

    @In
    private VersionStateCache versionStateCacheImpl;

    @In
    private UrlUtil urlUtil;

    @In
    private ProjectDAO projectDAO;

    @In(required = false, value = JpaIdentityStore.AUTHENTICATED_USER)
    private HAccount authenticatedAccount;

    @In
    private ZanataIdentity identity;

    @In
    private ZanataMessages zanataMessages;

    @Setter
    @Getter
    private String slug;

    @Getter
    private SortingType VersionSortingList = new SortingType(
            Lists.newArrayList(SortingType.SortOption.ALPHABETICAL,
                    SortingType.SortOption.HOURS,
                    SortingType.SortOption.PERCENTAGE,
                    SortingType.SortOption.WORDS,
                    SortingType.SortOption.LAST_UPDATED));

    @Getter
    private boolean pageRendered = false;

    private List<HProjectIteration> projectVersions;

    private Map<String, WordStatistic> statisticMap;

    private final VersionComparator versionComparator = new VersionComparator(
            getVersionSortingList());

    @CachedMethodResult
    public List<Activity> getProjectLatestActivity() {
        if (StringUtils.isEmpty(slug) || !identity.isLoggedIn()) {
            return Lists.newArrayList();
        }

        HProject project = projectDAO.getBySlug(slug);
        return activityServiceImpl
                .findLatestProjectActivities(authenticatedAccount.getPerson()
                        .getId(), project.getId(), 0, 1);
    }

    @CachedMethodResult
    public DisplayUnit getStatisticFigureForVersion(
            SortingType.SortOption sortOption, String versionSlug) {
        WordStatistic statistic = getStatisticForVersion(versionSlug);
        return getDisplayUnit(sortOption, statistic);
    }

    @CachedMethodResult
    public WordStatistic getStatisticForVersion(String versionSlug) {
        WordStatistic statistic = statisticMap.get(versionSlug);
        statistic
                .setRemainingHours(StatisticsUtil.getRemainingHours(statistic));
        return statistic;
    }

    /**
     * Sort version list
     */
    public void sortVersionList() {
        Collections.sort(projectVersions, versionComparator);
    }

    private class VersionComparator implements Comparator<HProjectIteration> {
        private SortingType sortingType;

        public VersionComparator(SortingType sortingType) {
            this.sortingType = sortingType;
        }

        @Override
        public int compare(HProjectIteration compareFrom,
                HProjectIteration compareTo) {
            final HProjectIteration item1, item2;

            if (sortingType.isDescending()) {
                item1 = compareFrom;
                item2 = compareTo;
            } else {
                item1 = compareTo;
                item2 = compareFrom;
            }

            SortingType.SortOption selectedSortOption =
                    sortingType.getSelectedSortOption();
            // Need to get statistic for comparison
            if (!selectedSortOption.equals(SortingType.SortOption.ALPHABETICAL)
                    && !selectedSortOption
                            .equals(SortingType.SortOption.LAST_UPDATED)) {

                WordStatistic wordStatistic1 =
                        getStatisticForVersion(item1.getSlug());
                WordStatistic wordStatistic2 =
                        getStatisticForVersion(item2.getSlug());

                if (selectedSortOption
                        .equals(SortingType.SortOption.PERCENTAGE)) {
                    return Double.compare(
                            wordStatistic1.getPercentTranslated(),
                            wordStatistic2.getPercentTranslated());
                } else if (selectedSortOption
                        .equals(SortingType.SortOption.HOURS)) {
                    return Double.compare(wordStatistic1.getRemainingHours(),
                            wordStatistic2.getRemainingHours());
                } else if (selectedSortOption
                        .equals(SortingType.SortOption.WORDS)) {
                    if (wordStatistic1.getTotal() == wordStatistic2.getTotal()) {
                        return 0;
                    }
                    return wordStatistic1.getTotal() > wordStatistic2
                            .getTotal() ? 1 : -1;
                }
            } else if (selectedSortOption
                    .equals(SortingType.SortOption.ALPHABETICAL)) {
                return item1.getSlug().toLowerCase()
                        .compareTo(item2.getSlug().toLowerCase());
            } else if (selectedSortOption
                    .equals(SortingType.SortOption.LAST_UPDATED)) {
                return item1.getLastChanged().compareTo(item2.getLastChanged());
            }
            return 0;
        }
    }

    @Override
    protected void loadStatistic() {
        statisticMap = Maps.newHashMap();

        for (HProjectIteration version : getProjectVersions()) {
            WordStatistic versionStats = new WordStatistic();
            List<HLocale> locales = getSupportedLocale(version);
            for (HLocale locale : locales) {
                versionStats.add(versionStateCacheImpl.getVersionStatistics(
                        version.getId(), locale.getLocaleId()));
            }
            statisticMap.put(version.getSlug(), versionStats);
        }
    }

    public List<HLocale> getSupportedLocale(HProjectIteration version) {
        if (version != null) {
            return localeServiceImpl.getSupportedLanguageByProjectIteration(
                    slug, version.getSlug());
        }
        return Lists.newArrayList();
    }

    public List<HProjectIteration> getProjectVersions() {
        if (projectVersions == null) {
            if (isUserAllowViewObsolete()) {
                projectVersions = projectDAO.getAllIterations(slug);
            } else {
                projectVersions = projectDAO.getActiveIterations(slug);
                projectVersions.addAll(projectDAO.getReadOnlyIterations(slug));
            }
        }
        Collections.sort(projectVersions, versionComparator);
        return projectVersions;
    }

    public boolean isUserAllowViewObsolete() {
        return identity != null
                && identity.hasPermission("HProject", "view-obsolete");
    }

    public boolean isUserAllowedToTranslateOrReview(HProjectIteration version,
            HLocale localeId) {
        return version != null
                && localeId != null
                && isIterationActive(version)
                && identity != null
                && (identity.hasPermission("add-translation",
                        version.getProject(), localeId) || identity
                        .hasPermission("translation-review",
                                version.getProject(), localeId));
    }

    private boolean isIterationActive(HProjectIteration version) {
        return version.getProject().getStatus() == EntityStatus.ACTIVE
                || version.getStatus() == EntityStatus.ACTIVE;
    }

    public void setPageRendered(boolean pageRendered) {
        if (pageRendered) {
            loadStatistic();
        }
        this.pageRendered = pageRendered;
    }

    @Override
    public void resetPageData() {
        projectVersions = null;
        loadStatistic();
    }

    @Override
    String getMessage(String key, Object... args) {
        return zanataMessages.getMessage(key, args);
    }

    public String getCreateVersionUrl(String projectSlug) {
        return urlUtil.createNewVersionUrl(projectSlug);
    }
}
