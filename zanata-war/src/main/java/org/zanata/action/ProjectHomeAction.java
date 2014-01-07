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
import java.util.List;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.annotation.CachedMethodResult;
import org.zanata.dao.ProjectDAO;
import org.zanata.model.Activity;
import org.zanata.model.HAccount;
import org.zanata.model.HProject;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.ActivityService;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
@Name("projectHomeAction")
@Scope(ScopeType.PAGE)
public class ProjectHomeAction implements Serializable {

    @In
    private ActivityService activityServiceImpl;

    @In
    private ProjectDAO projectDAO;

    @In(required = false, value = JpaIdentityStore.AUTHENTICATED_USER)
    private HAccount authenticatedAccount;

    @In
    private ZanataIdentity identity;

    @Setter
    @Getter
    private String slug;

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
}
