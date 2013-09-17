package org.zanata.persistence;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jboss.solder.core.ExtensionManaged;

public class ManagedPersistenceContextFactory
{

   @ExtensionManaged
   @Produces
   @PersistenceUnit(unitName = "zanataDatasourcePU")
   @ConversationScoped
   @Forge
   private EntityManagerFactory producerField;
}