/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.stickycode.mockwire.spring30;

import net.stickycode.mockwire.MockwireInjectingFieldProcessor;
import net.stickycode.reflector.Reflector;
import net.stickycode.reflector.ValueSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;

public class MockwireFieldInjectingBeanPostProcessor
    extends InstantiationAwareBeanPostProcessorAdapter
{

  private Logger log = LoggerFactory.getLogger(getClass());

  private ValueSource source;

  public MockwireFieldInjectingBeanPostProcessor(ValueSource source) {
    super();
    this.source = source;
  }

  @Override
  public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
    log.debug("{} as {}", beanName, bean);
    new Reflector()
        .forEachField(
            new MockwireInjectingFieldProcessor(source)
        )
        .process(bean);

    return true;
  }
}
