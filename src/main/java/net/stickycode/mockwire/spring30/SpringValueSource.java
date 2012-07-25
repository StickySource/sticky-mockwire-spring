package net.stickycode.mockwire.spring30;

import org.springframework.context.support.GenericApplicationContext;

import net.stickycode.reflector.ValueSource;


public class SpringValueSource
    implements ValueSource {

  private GenericApplicationContext context;

  public SpringValueSource(GenericApplicationContext context) {
    this.context = context;
  }

  @Override
  public Object get(Class<?> type) {
    return context.getBean(type);
  }

}
