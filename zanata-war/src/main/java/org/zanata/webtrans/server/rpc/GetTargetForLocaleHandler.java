package org.zanata.webtrans.server.rpc;

import lombok.extern.slf4j.Slf4j;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.ActionException;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.zanata.dao.TextFlowTargetDAO;
import org.zanata.model.HLocale;
import org.zanata.model.HTextFlowTarget;
import org.zanata.security.ZanataIdentity;
import org.zanata.webtrans.server.ActionHandlerFor;
import org.zanata.webtrans.shared.model.TextFlowTarget;
import org.zanata.webtrans.shared.model.TextFlowTargetId;
import org.zanata.webtrans.shared.rpc.GetTargetForLocale;
import org.zanata.webtrans.shared.rpc.GetTargetForLocaleResult;

@Name("webtrans.gwt.GetTargetForLocaleHandler")
@Scope(ScopeType.STATELESS)
@ActionHandlerFor(GetTargetForLocale.class)
@Slf4j
public class GetTargetForLocaleHandler extends
        AbstractActionHandler<GetTargetForLocale, GetTargetForLocaleResult> {
    @In
    private ZanataIdentity identity;
    @In
    private TextFlowTargetDAO textFlowTargetDAO;

    @Override
    public GetTargetForLocaleResult execute(GetTargetForLocale action,
    ExecutionContext context) throws ActionException {
        try {
            identity.checkLoggedIn();

            HTextFlowTarget hTextFlowTarget = textFlowTargetDAO
                    .getTextFlowTarget(
                    action.getSourceTransUnitId().getId(),
                    action.getLocale().getId().getLocaleId());

            if (hTextFlowTarget == null) {
                return new GetTargetForLocaleResult(null);
            } else {
                String displayName = retrieveDisplayName(hTextFlowTarget.getLocale());
                TextFlowTarget textFlowTarget = new TextFlowTarget(
                        new TextFlowTargetId(hTextFlowTarget.getId()),
                        action.getLocale(), hTextFlowTarget.getContents().get(0),
                        displayName);

                return new GetTargetForLocaleResult(textFlowTarget);
            }
        } catch (Exception e) {
            log.error("Exception when fetching target: ", e);
            return new GetTargetForLocaleResult(null);
        }
    }

    public String retrieveDisplayName(HLocale hLocale) {
        return hLocale.retrieveDisplayName();
    }

    @Override
    public void rollback(GetTargetForLocale action, GetTargetForLocaleResult result,
    ExecutionContext context) throws ActionException {
    }
}
