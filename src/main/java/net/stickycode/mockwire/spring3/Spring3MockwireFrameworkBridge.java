package net.stickycode.mockwire.spring3;

import java.beans.Introspector;

import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;

import net.stickycode.bootstrap.StickyBootstrap;
import net.stickycode.mockwire.MockwireFrameworkBridge;

public class Spring3MockwireFrameworkBridge
    implements MockwireFrameworkBridge {

  @Override
  public void initialise(StickyBootstrap bootstrap, Class<?> metadata) {
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

}
