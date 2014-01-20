package org.zanata.action;

import java.io.Serializable;
import java.util.List;
import javax.faces.model.DataModel;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.zanata.dao.ProjectDAO;
import org.zanata.model.HProject;
import org.zanata.security.ZanataIdentity;
import com.google.common.collect.Lists;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Name("projectSearch")
@Scope(ScopeType.CONVERSATION)
@AutoCreate
public class ProjectSearch implements Serializable {

    private static final long serialVersionUID = 1L;

    private final static int DEFAULT_PAGE_SIZE = 30;

    @Getter
    @Setter
    private int scrollerPage = 1;

    @Setter
    @Getter
    // project slug
    private String selectedItem;

    @In
    private ProjectDAO projectDAO;

    @In
    private ZanataIdentity identity;

    @Getter
    private final AbstractAutocomplete<SearchResult> projectAutocomplete =
            new AbstractAutocomplete<SearchResult>() {

                @Override
                public List<SearchResult> suggest() {
                    List<SearchResult> result = Lists.newArrayList();
                    if (StringUtils.isEmpty(getQuery())) {
                        return result;
                    }
                    try {
                        boolean includeObsolete =
                                identity != null
                                        && identity.hasPermission("HProject",
                                                "view-obsolete");
                        List<HProject> searchResult =
                                projectDAO.searchProjects(getQuery(),
                                        INITIAL_RESULT_COUNT, 0,
                                        includeObsolete);

                        for (HProject project : searchResult) {
                            result.add(new SearchResult(project));
                        }
                        result.add(new SearchResult());
                        return result;
                    } catch (ParseException pe) {
                        return result;
                    }
                }

                @Override
                public void onSelectItemAction() {
                    // nothing here
                }
            };

    private QueryProjectPagedListDataModel queryProjectPagedListDataModel =
            new QueryProjectPagedListDataModel(DEFAULT_PAGE_SIZE);

    // Count of result to be return as part of autocomplete
    private final static int INITIAL_RESULT_COUNT = 5;

    public int getPageSize() {
        return queryProjectPagedListDataModel.getPageSize();
    }

    public DataModel getProjectPagedListDataModel() {
        queryProjectPagedListDataModel.setIncludeObsolete(identity
                .hasPermission("HProject", "view-obsolete"));
        return queryProjectPagedListDataModel;
    }

    public void setSearchQuery(String searchQuery) {
        queryProjectPagedListDataModel.setQuery(searchQuery);
    }

    public String getSearchQuery() {
        return queryProjectPagedListDataModel.getQuery();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public class SearchResult {
        @Getter
        private HProject project;

        public boolean isProjectNull() {
            return project == null;
        }
    }
}
