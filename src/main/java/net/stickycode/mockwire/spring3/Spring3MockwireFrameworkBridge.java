package net.stickycode.mockwire.spring3;

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

  }

}
