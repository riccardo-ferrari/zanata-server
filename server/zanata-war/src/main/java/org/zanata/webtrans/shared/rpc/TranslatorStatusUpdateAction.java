package org.zanata.webtrans.shared.rpc;

import org.zanata.webtrans.shared.auth.SessionId;
import org.zanata.webtrans.shared.model.Person;
import org.zanata.webtrans.shared.model.TransUnit;

import net.customware.gwt.dispatch.shared.Result;


public class TranslatorStatusUpdateAction extends AbstractWorkspaceAction<TranslatorUpdateStatusResult>
{
   private static final long serialVersionUID = -9165857458963498055L;

   private Person person;
   private TransUnit selectedTransUnit;


   @SuppressWarnings("unused")
   private TranslatorStatusUpdateAction()
   {
      this(null, null);
   }

   public TranslatorStatusUpdateAction(Person person, TransUnit selectedTransUnit)
   {
      this.person = person;
      this.selectedTransUnit = selectedTransUnit;
   }

   public Person getPerson()
   {
      return person;
   }

   public TransUnit getSelectedTransUnit()
   {
      return selectedTransUnit;
   }
}