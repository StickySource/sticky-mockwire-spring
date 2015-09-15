/**
 * Copyright (c) 2010 RedEngine Ltd, http://www.redengine.co.nz. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package net.stickycode.mockwire.spring3;

import java.beans.Introspector;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.SimpleThreadScope;

import net.stickycode.bootstrap.StickyBootstrap;
import net.stickycode.bootstrap.StickySystemStartup;
import net.stickycode.configuration.ConfigurationSource;
import net.stickycode.exception.PermanentException;
import net.stickycode.mockwire.IsolatedTestManifest;
import net.stickycode.mockwire.MissingBeanException;

public class SpringIsolatedTestManifest
    implements IsolatedTestManifest {

  private StickyBootstrap bootstrap;

  private Logger log = LoggerFactory.getLogger(getClass());

  public SpringIsolatedTestManifest() {
    super();

    bootstrap = StickyBootstrap.crank();

    GenericApplicationContext context = (GenericApplicationContext) bootstrap.getImplementation();

    MockwireFieldInjectingBeanPostProcessor blessInjector = new MockwireFieldInjectingBeanPostProcessor(
        new SpringValueSource(context));
    context.getBeanFactory().addBeanPostProcessor(blessInjector);

    AutowiredAnnotationBeanPostProcessor inject = new AutowiredAnnotationBeanPostProcessor();
    inject.setBeanFactory(context.getDefaultListableBeanFactory());
    context.getBeanFactory().addBeanPostProcessor(inject);

    CommonAnnotationBeanPostProcessor commonPostProcessor = new CommonAnnotationBeanPostProcessor();
    commonPostProcessor.setBeanFactory(context.getDefaultListableBeanFactory());
    context.getBeanFactory().addBeanPostProcessor(commonPostProcessor);

    context.getBeanFactory().registerSingleton(
        Introspector.decapitalize(getClass().getSimpleName()),
        this);
  }

  @Override
  public boolean canFind(Class<?> type) {
    return bootstrap.canFind(type);
  }

  @Override
  public void beforeTest(Object testInstance) {
    refresh(testInstance.getClass());
    try {
      bootstrap.inject(testInstance);
    }
    catch (BeansException e) {
      Throwable cause = e.getMostSpecificCause();
      if (cause instanceof NoSuchBeanDefinitionException) {
        NoSuchBeanDefinitionException n = (NoSuchBeanDefinitionException) cause;
        throw new MissingBeanException(n, testInstance, n.getBeanType());
      }
      if (cause instanceof PermanentException)
        throw (PermanentException) cause;

      throw new TestInjectionFailure(e, testInstance.getClass());
    }
  }

  @Override
  public void registerBean(String beanName, Object bean, Class<?> type) {
    bootstrap.registerSingleton(beanName, bean, type);
  }

  @Override
  public void registerType(String beanName, Class<?> type) {
    bootstrap.registerType(beanName, type);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getBeanOfType(Class<T> type) {
    return bootstrap.find(type);
  }

  @Override
  public void scanPackages(String[] scanRoots) {
    log.debug("scanning roots {}", scanRoots);
    bootstrap.scan(scanRoots);
  }

//  private XmlBeanDefinitionReader createXmlLoader() {
//    XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(context);
//    beanDefinitionReader.setResourceLoader(context);
//    beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(context));
//    return beanDefinitionReader;
//  }

  @Override
  public void startup(Class<?> testClass) {
    refresh(testClass);
  }

  private void refresh(Class<?> testClass) {
    try {
      GenericApplicationContext context = getContext();
      context.getBeanFactory().registerScope("request", new SimpleThreadScope());
//      context.refresh();
      if (context.getBeanNamesForType(StickySystemStartup.class).length > 0)
        context.getBean(StickySystemStartup.class).start();
    }
    catch (BeansException e) {
      Throwable cause = e.getMostSpecificCause();
      if (cause instanceof NoSuchBeanDefinitionException) {
        NoSuchBeanDefinitionException n = (NoSuchBeanDefinitionException) cause;
        throw new MissingBeanException(n, testClass, n.getBeanType());
      }
      if (cause instanceof PermanentException)
        throw (PermanentException) cause;

      throw new TestInjectionFailure(e, testClass);
    }
  }

  @Override
  public void shutdown() {
    bootstrap.shutdown();
  }

  GenericApplicationContext getContext() {
    return (GenericApplicationContext) bootstrap.getImplementation();
  }

  @Override
  public void registerConfiguationSystem(List<ConfigurationSource> configurationSources) {
    for (ConfigurationSource configurationSource : configurationSources) {
      registerBean(name(configurationSource.getClass()), configurationSource, ConfigurationSource.class);
    }
  }

  private String name(Class<?> type) {
    return Introspector.decapitalize(type.getSimpleName());
  }

  @Override
  public void configure() {
  }

  @Override
  public void initialiseFramework(List<String> frameworkPackages) {
    bootstrap.scan(frameworkPackages.toArray(new String[frameworkPackages.size()]));
  }

}
