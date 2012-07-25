package net.stickycode.mockwire.spring30;

import net.stickycode.exception.PermanentException;

/**
 * Indicates a failure to inject the test instance before running a test method
 */
@SuppressWarnings("serial")
public class TestInjectionFailure
    extends PermanentException {

  public TestInjectionFailure(Throwable t, Class<?> testInstance) {
    super(t, "Failed to inject test {}", testInstance.getSimpleName());
  }

}
