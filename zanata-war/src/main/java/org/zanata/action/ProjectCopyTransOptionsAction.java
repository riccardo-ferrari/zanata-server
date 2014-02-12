/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.action;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.zanata.dao.ProjectDAO;
import org.zanata.model.HProject;

/**
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Name("projectCopyTransOptionsAction")
@Scope(ScopeType.PAGE)
public class ProjectCopyTransOptionsAction implements Serializable {
    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    private String projectSlug;

    @In
    private ProjectDAO projectDAO;

    @In
    private CopyTransOptionsModel copyTransOptionsModel;

    public void initialize() {
        if (this.getProject().getDefaultCopyTransOpts() != null) {
            copyTransOptionsModel.setInstance(this.getProject()
                    .getDefaultCopyTransOpts());
        }
    }

    public HProject getProject() {
        return projectDAO.getBySlug(this.projectSlug);
    }

    @Transactional
    @Restrict("#{s:hasPermission(projectCopyTransOptionsAction.project, 'update')}")
    public
            void saveOptions() {
        copyTransOptionsModel.save();
        HProject project = getProject();
        project.setDefaultCopyTransOpts(copyTransOptionsModel.getInstance());
        projectDAO.makePersistent(project);

        FacesMessages.instance().add(StatusMessage.Severity.INFO,
                "jsf.project.CopyTransOpts.saved", null, null, null);
    }
}
