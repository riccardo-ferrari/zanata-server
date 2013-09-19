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
package org.zanata.cdi.util;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
public class ComponentLocator
{
   private ComponentLocator()
   {
   }

   @SuppressWarnings("unchecked")
   public static <T> T getObject(String name, Class<T> clazz)
   {
      final BeanManager beanManager = getBeanManager();
      final Bean<?> bean = beanManager.getBeans(name).iterator().next();
      final CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
      return (T) beanManager.getReference(bean, bean.getClass(), ctx);
   }

   @SuppressWarnings("unchecked")
   public static <T> T getSingleton(Class<T> clazz)
   {
      final BeanManager beanManager = getBeanManager();
      final Bean<?> bean = beanManager.getBeans(clazz).iterator().next();
      final CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
      return (T) beanManager.getReference(bean, bean.getClass(), ctx);
   }

   private static BeanManager getBeanManager()
   {
      Context jndiCtx = null;
      BeanManager beanManager = null;
      try
      {
         jndiCtx = new InitialContext();
         beanManager = (BeanManager) jndiCtx.lookup("java:comp/BeanManager");
      }
      catch (NamingException e)
      {
         throw new RuntimeException(e);
      }
      finally
      {
         if (jndiCtx != null) {
            try {
               jndiCtx.close();
            }
            catch (NamingException e) {
            }
         }
      }

      return beanManager;
   }
}
