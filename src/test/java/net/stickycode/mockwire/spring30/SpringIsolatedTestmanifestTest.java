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

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import net.stickycode.mockwire.spring3.SpringIsolatedTestManifest;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringIsolatedTestmanifestTest {

  public class Example {

    private String value;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

  }

  @Test
  public void empty() {
    SpringIsolatedTestManifest manifest = new SpringIsolatedTestManifest();
    assertThat(manifest.getContext().getBeanDefinitionCount()).isEqualTo(0);
  }

  @Test
  public void checkBeanFactoryPostProcessors() {
    SpringIsolatedTestManifest manifest = new SpringIsolatedTestManifest();
    assertThat(manifest.getContext().getBeanDefinitionCount()).isEqualTo(0);
    manifest.getContext().addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {

      @Override
      public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        beanFactory.getBeanNamesForType(Example.class);
      }
    });
    manifest.registerBean("bob", new Example(), Example.class);
    manifest.beforeTest(this);
  }
}
