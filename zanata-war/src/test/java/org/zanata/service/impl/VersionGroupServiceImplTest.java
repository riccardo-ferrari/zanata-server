/*
 * Copyright 2013, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.zanata.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dbunit.operation.DatabaseOperation;
import org.hamcrest.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zanata.ZanataDbunitJpaTest;
import org.zanata.common.LocaleId;
import org.zanata.dao.PersonDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.dao.VersionGroupDAO;
import org.zanata.model.HIterationGroup;
import org.zanata.model.HLocale;
import org.zanata.model.HPerson;
import org.zanata.model.HProjectIteration;
import org.zanata.seam.SeamAutowire;
import org.zanata.service.VersionLocaleKey;
import org.zanata.ui.model.statistic.WordStatistic;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public class VersionGroupServiceImplTest extends ZanataDbunitJpaTest {
    private SeamAutowire seam = SeamAutowire.instance();

    private VersionGroupServiceImpl versionGroupServiceImpl;

    private final String GROUP1_SLUG = "group1";
    private final String GROUP2_SLUG = "group2";
    private final String GROUP3_SLUG = "group3";

    @Override
    protected void prepareDBUnitOperations() {
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/ClearAllTables.dbunit.xml",
                DatabaseOperation.DELETE_ALL));
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/AccountData.dbunit.xml",
                DatabaseOperation.CLEAN_INSERT));
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/ProjectsData.dbunit.xml",
                DatabaseOperation.CLEAN_INSERT));
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/LocalesData.dbunit.xml",
                DatabaseOperation.CLEAN_INSERT));
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/TextFlowTestData.dbunit.xml",
                DatabaseOperation.CLEAN_INSERT));
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/GroupsTestData.dbunit.xml",
                DatabaseOperation.CLEAN_INSERT));
    }

    @BeforeMethod
    public void initializeSeam() {
        seam.reset()
                .use("versionGroupDAO", new VersionGroupDAO(getSession()))
                .use("projectIterationDAO",
                        new ProjectIterationDAO(getSession()))
                .use("session", getSession())
                .useImpl(VersionStateCacheImpl.class).useImpl(LocaleServiceImpl.class).ignoreNonResolvable();

        versionGroupServiceImpl = seam.autowire(VersionGroupServiceImpl.class);
    }

    @Test
    public void getLocaleStatisticTest1() {
        LocaleId localeId = LocaleId.DE;

        Map<VersionLocaleKey, WordStatistic> result =
                versionGroupServiceImpl.getLocaleStatistic(GROUP1_SLUG,
                        localeId);

        // 3 versions in group1
        assertThat(result.size(), equalTo(3));
    }

    @Test
    public void getLocaleStatisticTest2() {
        LocaleId localeId = LocaleId.DE;

        Map<VersionLocaleKey, WordStatistic> result =
                versionGroupServiceImpl.getLocaleStatistic(GROUP2_SLUG,
                        localeId);

        // 2 versions in group1
        assertThat(result.size(), equalTo(2));
    }

    @Test
    public void getTotalMessageCountTest1() {
        int totalMessageCount =
                versionGroupServiceImpl.getTotalMessageCount(GROUP1_SLUG);
        assertThat(totalMessageCount, equalTo(18));
    }

    @Test
    public void getTotalMessageCountTest2() {
        int totalMessageCount =
                versionGroupServiceImpl.getTotalMessageCount(GROUP2_SLUG);
        assertThat(totalMessageCount, equalTo(0));
    }

    @Test
    public void getAllActiveAndMaintainedGroupsTest() {
        // personId = 1 is maintainers for group1 and group3(obsolote)
        PersonDAO personDAO = new PersonDAO(getSession());
        HPerson person = personDAO.findById(new Long(1));
        List<HIterationGroup> result =
                versionGroupServiceImpl.getAllActiveAndMaintainedGroups(person);

        assertThat(result.size(), equalTo(3));
    }

    @Test
    public void getMaintainersBySlugTest() {
        List<HPerson> maintainers =
                versionGroupServiceImpl.getMaintainersBySlug(GROUP1_SLUG);
        assertThat(maintainers.size(), equalTo(2));
    }

    @Test
    public void isVersionInGroupTest() {
        boolean result =
                versionGroupServiceImpl.isVersionInGroup(GROUP1_SLUG, new Long(
                        1));
        assertThat(result, equalTo(true));

        result =
                versionGroupServiceImpl.isVersionInGroup(GROUP1_SLUG, new Long(
                        3));
        assertThat(result, equalTo(false));
    }

    @Test
    public void getGroupActiveLocalesTest() {
        Set<HLocale> activeLocales =
                versionGroupServiceImpl.getGroupActiveLocales(GROUP1_SLUG);
        assertThat(activeLocales.size(), equalTo(3));

        activeLocales =
                versionGroupServiceImpl.getGroupActiveLocales(GROUP3_SLUG);
        assertThat(activeLocales.size(), equalTo(0));
    }

    @Test
    public void getMissingLocaleVersionMapTest() {
        Map<LocaleId, List<HProjectIteration>> map =
                versionGroupServiceImpl.getMissingLocaleVersionMap(GROUP1_SLUG);

        int activateLocaleSize =
                versionGroupServiceImpl.getGroupActiveLocales(GROUP1_SLUG)
                        .size();
        assertThat(map.size(), equalTo(activateLocaleSize));

        // See ProjectsData.dbunit.xml, HProjectIteration id="900" in group1
        ProjectIterationDAO projectIterationDAO =
                new ProjectIterationDAO(getSession());
        HProjectIteration version = projectIterationDAO.findById(new Long(900));

        for (List<HProjectIteration> versions : map.values()) {
            assertThat("", versions, Matchers.contains(version));
        }
    }
}
