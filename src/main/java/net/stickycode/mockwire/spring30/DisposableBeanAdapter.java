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

import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;


public class DisposableBeanAdapter
    implements DisposableBean {

  private final GenericApplicationContext context;
  private final Object bean;
  private final String beanName;

  public DisposableBeanAdapter(Object bean, String beanName, GenericApplicationContext context) {
    super();
    this.context = context;
    this.bean = bean;
    this.beanName = beanName;
  }

  @Override
  public void destroy() throws Exception {
    List<BeanPostProcessor> beanPostProcessors = context.getDefaultListableBeanFactory().getBeanPostProcessors();
    for (BeanPostProcessor p : beanPostProcessors) {
      if (p instanceof DestructionAwareBeanPostProcessor)
        ((DestructionAwareBeanPostProcessor)p).postProcessBeforeDestruction(bean, beanName);
    }
  }

}
