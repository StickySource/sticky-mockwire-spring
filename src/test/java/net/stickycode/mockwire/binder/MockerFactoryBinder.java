package net.stickycode.mockwire.binder;

import net.stickycode.mockwire.Mocker;
import net.stickycode.mockwire.mockito.MockitoMocker;


public class MockerFactoryBinder
    implements MockerFactory {

  @Override
  public Mocker create() {
    return new MockitoMocker();
  }

}
