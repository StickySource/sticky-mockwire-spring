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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import net.stickycode.mockwire.Controlled;
import net.stickycode.mockwire.Uncontrolled;
import net.stickycode.mockwire.UnderTest;

/**
 *A copy of {@link AutowiredAnnotationBeanPostProcessor} that only wires {@link UnderTest}, {@link Controlled} and {@link Uncontrolled} on fields. * @see AutowiredAnnotationBeanPostProcessor
 */
public class MockwireFieldInjectionAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
		implements MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private final Set<Class<? extends Annotation>> autowiredAnnotationTypes =
			new LinkedHashSet<Class<? extends Annotation>>();

	private String requiredParameterName = "required";

	private boolean requiredParameterValue = true;

	private int order = Ordered.LOWEST_PRECEDENCE - 2;

	private ConfigurableListableBeanFactory beanFactory;

	private final Map<Class<?>, Constructor[]> candidateConstructorsCache =
			new ConcurrentHashMap<Class<?>, Constructor[]>();

	private final Map<Class<?>, InjectionMetadata> injectionMetadataCache =
			new ConcurrentHashMap<Class<?>, InjectionMetadata>();


	/**
	 * Create a new AutowiredAnnotationBeanPostProcessor
	 * for Spring's standard {@link Autowired} annotation.
	 * <p>Also supports JSR-330's {@link javax.inject.Inject} annotation, if available.
	 */
	public MockwireFieldInjectionAnnotationBeanPostProcessor() {
		this.autowiredAnnotationTypes.add(Inject.class);
		this.autowiredAnnotationTypes.add(UnderTest.class);
		this.autowiredAnnotationTypes.add(Controlled.class);
	}


	/**
	 * Set the 'autowired' annotation type, to be used on constructors, fields,
	 * setter methods and arbitrary config methods.
	 * <p>The default autowired annotation type is the Spring-provided
	 * {@link Autowired} annotation, as well as {@link Value}.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation type to indicate that a member is
	 * supposed to be autowired.
	 */
	public void setAutowiredAnnotationType(Class<? extends Annotation> autowiredAnnotationType) {
		Assert.notNull(autowiredAnnotationType, "'autowiredAnnotationType' must not be null");
		this.autowiredAnnotationTypes.clear();
		this.autowiredAnnotationTypes.add(autowiredAnnotationType);
	}

	/**
	 * Set the 'autowired' annotation types, to be used on constructors, fields,
	 * setter methods and arbitrary config methods.
	 * <p>The default autowired annotation type is the Spring-provided
	 * {@link Autowired} annotation, as well as {@link Value}.
	 * <p>This setter property exists so that developers can provide their own
	 * (non-Spring-specific) annotation types to indicate that a member is
	 * supposed to be autowired.
	 */
	public void setAutowiredAnnotationTypes(Set<Class<? extends Annotation>> autowiredAnnotationTypes) {
		Assert.notEmpty(autowiredAnnotationTypes, "'autowiredAnnotationTypes' must not be empty");
		this.autowiredAnnotationTypes.clear();
		this.autowiredAnnotationTypes.addAll(autowiredAnnotationTypes);
	}

	/**
	 * Set the name of a parameter of the annotation that specifies
	 * whether it is required.
	 * @see #setRequiredParameterValue(boolean)
	 */
	public void setRequiredParameterName(String requiredParameterName) {
		this.requiredParameterName = requiredParameterName;
	}

	/**
	 * Set the boolean value that marks a dependency as required
	 * <p>For example if using 'required=true' (the default),
	 * this value should be <code>true</code>; but if using
	 * 'optional=false', this value should be <code>false</code>.
	 * @see #setRequiredParameterName(String)
	 */
	public void setRequiredParameterValue(boolean requiredParameterValue) {
		this.requiredParameterValue = requiredParameterValue;
	}

	public void setOrder(int order) {
	  this.order = order;
	}

	public int getOrder() {
	  return this.order;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory");
		}
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}


	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class beanType, String beanName) {
		if (beanType != null) {
			InjectionMetadata metadata = findAutowiringMetadata(beanType);
			metadata.checkConfigMembers(beanDefinition);
		}
	}

	@Override
	public Constructor[] determineCandidateConstructors(Class beanClass, String beanName) throws BeansException {
		// Quick check on the concurrent map first, with minimal locking.
		Constructor[] candidateConstructors = this.candidateConstructorsCache.get(beanClass);
		if (candidateConstructors == null) {
			synchronized (this.candidateConstructorsCache) {
				candidateConstructors = this.candidateConstructorsCache.get(beanClass);
				if (candidateConstructors == null) {
					Constructor[] rawCandidates = beanClass.getDeclaredConstructors();
					List<Constructor> candidates = new ArrayList<Constructor>(rawCandidates.length);
					Constructor requiredConstructor = null;
					Constructor defaultConstructor = null;
					for (Constructor<?> candidate : rawCandidates) {
						Annotation annotation = findAutowiredAnnotation(candidate);
						if (annotation != null) {
							if (requiredConstructor != null) {
								throw new BeanCreationException("Invalid autowire-marked constructor: " + candidate +
										". Found another constructor with 'required' Autowired annotation: " +
										requiredConstructor);
							}
							if (candidate.getParameterTypes().length == 0) {
								throw new IllegalStateException(
										"Autowired annotation requires at least one argument: " + candidate);
							}
							boolean required = determineRequiredStatus(annotation);
							if (required) {
								if (!candidates.isEmpty()) {
									throw new BeanCreationException(
											"Invalid autowire-marked constructors: " + candidates +
											". Found another constructor with 'required' Autowired annotation: " +
											requiredConstructor);
								}
								requiredConstructor = candidate;
							}
							candidates.add(candidate);
						}
						else if (candidate.getParameterTypes().length == 0) {
							defaultConstructor = candidate;
						}
					}
					if (!candidates.isEmpty()) {
						// Add default constructor to list of optional constructors, as fallback.
						if (requiredConstructor == null && defaultConstructor != null) {
							candidates.add(defaultConstructor);
						}
						candidateConstructors = candidates.toArray(new Constructor[candidates.size()]);
					}
					else {
						candidateConstructors = new Constructor[0];
					}
					this.candidateConstructorsCache.put(beanClass, candidateConstructors);
				}
			}
		}
		return (candidateConstructors.length > 0 ? candidateConstructors : null);
	}

	@Override
	public PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		InjectionMetadata metadata = findAutowiringMetadata(bean.getClass());
		try {
			metadata.inject(bean, beanName, pvs);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
		}
		return pvs;
	}

	/**
	 * 'Native' processing method for direct calls with an arbitrary target instance,
	 * resolving all of its fields and methods which are annotated with <code>@Autowired</code>.
	 * @param bean the target instance to process
	 * @throws BeansException if autowiring failed
	 */
	public void processInjection(Object bean) throws BeansException {
		Class<?> clazz = bean.getClass();
		InjectionMetadata metadata = findAutowiringMetadata(clazz);
		try {
			metadata.inject(bean, null, null);
		}
		catch (Throwable ex) {
			throw new BeanCreationException("Injection of autowired dependencies failed for class [" + clazz + "]", ex);
		}
	}


	private InjectionMetadata findAutowiringMetadata(Class clazz) {
		// Quick check on the concurrent map first, with minimal locking.
		InjectionMetadata metadata = this.injectionMetadataCache.get(clazz);
		if (metadata == null) {
			synchronized (this.injectionMetadataCache) {
				metadata = this.injectionMetadataCache.get(clazz);
				if (metadata == null) {
					metadata = buildAutowiringMetadata(clazz);
					this.injectionMetadataCache.put(clazz, metadata);
				}
			}
		}
		return metadata;
	}

	private InjectionMetadata buildAutowiringMetadata(Class clazz) {
		LinkedList<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();
		Class<?> targetClass = clazz;

		do {
			LinkedList<InjectionMetadata.InjectedElement> currElements = new LinkedList<InjectionMetadata.InjectedElement>();
			for (Field field : targetClass.getDeclaredFields()) {
				Annotation annotation = findAutowiredAnnotation(field);
				if (annotation != null) {
					if (Modifier.isStatic(field.getModifiers())) {
						if (logger.isWarnEnabled()) {
							logger.warn("Autowired annotation is not supported on static fields: " + field);
						}
						continue;
					}
					boolean required = determineRequiredStatus(annotation);
					currElements.add(new AutowiredFieldElement(field, required));
				}
			}
			elements.addAll(0, currElements);
			targetClass = targetClass.getSuperclass();
		}
		while (targetClass != null && targetClass != Object.class);

		return new InjectionMetadata(clazz, elements);
	}

	private Annotation findAutowiredAnnotation(AccessibleObject ao) {
		for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
			Annotation annotation = ao.getAnnotation(type);
			if (annotation != null) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * Obtain all beans of the given type as autowire candidates.
	 * @param type the type of the bean
	 * @return the target beans, or an empty Collection if no bean of this type is found
	 * @throws BeansException if bean retrieval failed
	 */
	protected <T> Map<String, T> findAutowireCandidates(Class<T> type) throws BeansException {
		if (this.beanFactory == null) {
			throw new IllegalStateException("No BeanFactory configured - " +
					"override the getBeanOfType method or specify the 'beanFactory' property");
		}
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type);
	}

	/**
	 * Determine if the annotated field or method requires its dependency.
	 * <p>A 'required' dependency means that autowiring should fail when no beans
	 * are found. Otherwise, the autowiring process will simply bypass the field
	 * or method when no beans are found.
	 * @param annotation the Autowired annotation
	 * @return whether the annotation indicates that a dependency is required
	 */
	protected boolean determineRequiredStatus(Annotation annotation) {
		try {
			Method method = ReflectionUtils.findMethod(annotation.annotationType(), this.requiredParameterName);
			return (this.requiredParameterValue == (Boolean) ReflectionUtils.invokeMethod(method, annotation));
		}
		catch (Exception ex) {
			// required by default
			return true;
		}
	}

	/**
	 * Register the specified bean as dependent on the autowired beans.
	 */
	private void registerDependentBeans(String beanName, Set<String> autowiredBeanNames) {
		if (beanName != null) {
			for (String autowiredBeanName : autowiredBeanNames) {
				beanFactory.registerDependentBean(autowiredBeanName, beanName);
				if (logger.isDebugEnabled()) {
					logger.debug(
							"Autowiring by type from bean name '" + beanName + "' to bean named '" + autowiredBeanName +
									"'");
				}
			}
		}
	}

	/**
	 * Resolve the specified cached method argument or field value.
	 */
	private Object resolvedCachedArgument(String beanName, Object cachedArgument) {
		if (cachedArgument instanceof DependencyDescriptor) {
			DependencyDescriptor descriptor = (DependencyDescriptor) cachedArgument;
			TypeConverter typeConverter = beanFactory.getTypeConverter();
			return beanFactory.resolveDependency(descriptor, beanName, null, typeConverter);
		}
		else if (cachedArgument instanceof RuntimeBeanReference) {
			return beanFactory.getBean(((RuntimeBeanReference) cachedArgument).getBeanName());
		}
		else {
			return cachedArgument;
		}
	}


	/**
	 * Class representing injection information about an annotated field.
	 */
	private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

		private final boolean required;

		private volatile boolean cached = false;

		private volatile Object cachedFieldValue;

		public AutowiredFieldElement(Field field, boolean required) {
			super(field, null);
			this.required = required;
		}

		@Override
		protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
			Field field = (Field) this.member;
			try {
				Object value;
				if (this.cached) {
					value = resolvedCachedArgument(beanName, this.cachedFieldValue);
				}
				else {
					synchronized (this) {
						if (!this.cached) {
							Set<String> autowiredBeanNames = new LinkedHashSet<String>(1);
							TypeConverter typeConverter = beanFactory.getTypeConverter();
							DependencyDescriptor descriptor = new DependencyDescriptor(field, this.required);
							this.cachedFieldValue = descriptor;
							value = beanFactory.resolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
							if (value != null) {
								registerDependentBeans(beanName, autowiredBeanNames);
								if (autowiredBeanNames.size() == 1) {
									String autowiredBeanName = autowiredBeanNames.iterator().next();
									if (beanFactory.containsBean(autowiredBeanName)) {
										if (beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
											this.cachedFieldValue = new RuntimeBeanReference(autowiredBeanName);
										}
									}
								}
							}
							else {
								this.cachedFieldValue = null;
							}
							this.cached = true;
						}
						else {
							// Already cached in the meantime...
							value = resolvedCachedArgument(beanName, this.cachedFieldValue);
						}
					}
				}
				if (value != null) {
					ReflectionUtils.makeAccessible(field);
					field.set(bean, value);
				}
			}
			catch (Throwable ex) {
				throw new BeanCreationException("Could not autowire field: " + field, ex);
			}
		}
	}

}
