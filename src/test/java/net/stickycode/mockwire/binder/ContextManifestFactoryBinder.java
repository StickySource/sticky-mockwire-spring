package net.stickycode.mockwire.binder;

import net.stickycode.mockwire.IsolatedTestManifest;
import net.stickycode.mockwire.spring30.SpringIsolatedTestManifest;


public class ContextManifestFactoryBinder
    implements ContextManifestFactory {

  @Override
  public IsolatedTestManifest create() {
    return new SpringIsolatedTestManifest();
  }

}
