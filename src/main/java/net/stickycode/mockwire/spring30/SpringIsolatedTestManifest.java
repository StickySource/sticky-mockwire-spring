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
package net.stickycode.mockwire.spring30;

import java.beans.Introspector;
import java.util.List;
import java.util.Map;

import net.stickycode.bootstrap.StickyBootstrap;
import net.stickycode.bootstrap.spring3.StickySpringBootstrap;
import net.stickycode.configuration.ConfigurationSource;
import net.stickycode.configured.ConfigurationSystem;
import net.stickycode.configured.spring30.ConfigurationRefresher;
import net.stickycode.exception.PermanentException;
import net.stickycode.mockwire.IsolatedTestManifest;
import net.stickycode.mockwire.MissingBeanException;
import net.stickycode.mockwire.NonUniqueBeanException;
import net.stickycode.stereotype.StickyComponent;
import net.stickycode.stereotype.StickyPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

public class SpringIsolatedTestManifest
    implements IsolatedTestManifest {

  private GenericApplicationContext context;

  private Logger log = LoggerFactory.getLogger(getClass());

  public SpringIsolatedTestManifest() {
    super();

    context = new GenericApplicationContext();

    MockwireFieldInjectingBeanPostProcessor blessInjector = new MockwireFieldInjectingBeanPostProcessor(new SpringValueSource(context));
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
  public boolean hasRegisteredType(Class<?> type) {
    return context.getBeanNamesForType(type).length > 0;
  }

  @Override
  public void prepareTest(Object testInstance) {
    refresh(testInstance.getClass());
    try {
      context.getAutowireCapableBeanFactory().autowireBean(testInstance);
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
    log.debug("registering bean '{}' of type '{}'", beanName, type.getName());
    context.getBeanFactory().initializeBean(bean, beanName);
    context.getBeanFactory().registerSingleton(beanName, bean);
    // beans that get pushed straight into the context need to be attached to destructive bean post processors
    context.getDefaultListableBeanFactory().registerDisposableBean(
        beanName, new DisposableBeanAdapter(bean, beanName, context));
  }

  @Override
  public void registerType(String beanName, Class<?> type) {
    log.debug("registering definition '{}' for type '{}'", beanName, type.getName());
    GenericBeanDefinition bd = new GenericBeanDefinition();
    bd.setBeanClass(type);
    bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
    context.getDefaultListableBeanFactory().registerBeanDefinition(beanName, bd);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getBeanOfType(Class<T> type) {
    Map<String, ?> beans = context.getBeansOfType(type);
    if (beans.size() == 1)
      return (T) beans.values().iterator().next();

    if (beans.size() == 0)
      throw new MissingBeanException(type);

    throw new NonUniqueBeanException(beans.size(), beans.keySet(), type);
  }

  @Override
  public void scanPackages(String[] scanRoots) {
    log.debug("scanning roots {}", scanRoots);
    StickySpringBootstrap bootstrap = new StickySpringBootstrap(context);
    XmlBeanDefinitionReader beanDefinitionReader = createXmlLoader();
    for (String s : scanRoots)
      if (s.endsWith(".xml"))
        beanDefinitionReader.loadBeanDefinitions(s);
      else
        bootstrap.scan(scanRoots);
  }

  private XmlBeanDefinitionReader createXmlLoader() {
    XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(context);
    beanDefinitionReader.setResourceLoader(context);
    beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(context));
    return beanDefinitionReader;
  }

  @Override
  public void startup(Class<?> testClass) {
//    refresh(testClass);
  }

  private void refresh(Class<?> testClass) {
    try {
      context.refresh();
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
    context.close();
  }

  GenericApplicationContext getContext() {
    return context;
  }

  @Override
  public void registerConfiguationSystem(List<ConfigurationSource> configurationSources) {
    for (ConfigurationSource configurationSource : configurationSources) {
      registerBean(name(configurationSource.getClass()), configurationSource, ConfigurationSource.class);
    }
  }

  public void registerType(GenericApplicationContext c, Class<?> type) {
    GenericBeanDefinition bd = new GenericBeanDefinition();
    bd.setBeanClass(type);
    bd.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
    c.getDefaultListableBeanFactory().registerBeanDefinition(name(type), bd);
  }

  private String name(Class<?> type) {
    return Introspector.decapitalize(type.getSimpleName());
  }

  @Override
  public void configure() {
    context.getBean(StickyBootstrap.class).start();
  }

  @Override
  public void initialiseFramework(List<String> frameworkPackages) {
    new StickySpringBootstrap(context)
    .scan(frameworkPackages.toArray(new String[frameworkPackages.size()]));
  }

}
