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

import lombok.Getter;
import lombok.Setter;
import org.zanata.model.HDocument;

import java.util.List;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public abstract class AbstractDocumentsFilter {

    @Getter
    private static int documentCountPerPage = 20;

    @Getter
    private int currentDocumentPage = 0;

    @Setter
    @Getter
    private String documentQuery;

    abstract int getFilteredDocumentSize();

    abstract List<HDocument> getPagedFilteredDocuments();

    abstract List<HDocument> getFilteredDocuments();

    public String getDocumentsRange() {
        int totalDocuments = getFilteredDocumentSize();
        int upperBound =
                totalDocuments == 0 ? 0
                        : (currentDocumentPage * documentCountPerPage) + 1;
        int lowerBound = (currentDocumentPage + 1) * documentCountPerPage;
        lowerBound = lowerBound > totalDocuments ? totalDocuments : lowerBound;
        return upperBound + "-" + lowerBound;
    }

    public void nextPage() {
        int totalPage =
                (int) Math.ceil((double) getFilteredDocumentSize()
                        / documentCountPerPage) - 1;
        currentDocumentPage =
                (currentDocumentPage + 1) > totalPage ? totalPage
                        : currentDocumentPage + 1;
    }

    public void previousPage() {
        currentDocumentPage =
                (currentDocumentPage - 1) < 0 ? 0 : currentDocumentPage - 1;
    }

    public void resetDocumentPage() {
        currentDocumentPage = 0;
        documentQuery = "";
    }
}
