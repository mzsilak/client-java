package eu.arrowhead.demo.hk2;

import java.util.Set;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.inject.hk2.ImmediateHk2InjectionManager;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.jvnet.hk2.spring.bridge.api.SpringBridge;
import org.jvnet.hk2.spring.bridge.api.SpringIntoHK2Bridge;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;


/**
 * Custom ComponentProvider class.
 * Responsible for 1) bootstrapping Jersey 2 Spring integration and
 * 2) making Jersey skip JAX-RS Spring component life-cycle management and leave it to us.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class SpringServiceLocator implements ComponentProvider {

    private final Logger logger = LogManager.getLogger();
    private final ApplicationContext ctx;
    private volatile InjectionManager injectionManager;

    public SpringServiceLocator(final ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void initialize(InjectionManager injectionManager) {
        this.injectionManager = injectionManager;

        // initialize HK2 spring-bridge
        ImmediateHk2InjectionManager hk2InjectionManager = (ImmediateHk2InjectionManager) injectionManager;
        SpringBridge.getSpringBridge().initializeSpringBridge(hk2InjectionManager.getServiceLocator());
        SpringIntoHK2Bridge springBridge = injectionManager.getInstance(SpringIntoHK2Bridge.class);
        springBridge.bridgeSpringBeanFactory(ctx);

        injectionManager.register(Bindings.injectionResolver(new AutowiredInjectResolver(ctx)));
        injectionManager.register(Bindings.service(ctx).to(ApplicationContext.class).named("SpringContext"));
    }

    // detect JAX-RS classes that are also Spring @Components.
    // register these with HK2 ServiceLocator to manage their lifecycle using Spring.
    @Override
    public boolean bind(Class<?> component, Set<Class<?>> providerContracts) {

        if (ctx == null) {
            return false;
        }

        if (AnnotationUtils.findAnnotation(component, Component.class) != null) {
            String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(ctx, component);
            if (beanNames == null || beanNames.length != 1) {
                logger.fatal("No or multiple beans available");
                return false;
            }
            String beanName = beanNames[0];

            Binding binding = Bindings.supplier(new SpringManagedBeanFactory(ctx, injectionManager, beanName))
                                      .to(component).to(providerContracts);
            injectionManager.register(binding);

            logger.debug("Registered bean name: {}", beanNames[0]);
            return true;
        }
        return false;
    }

    @Override
    public void done() {
    }

    private static class SpringManagedBeanFactory implements Supplier {

        private final ApplicationContext ctx;
        private final InjectionManager injectionManager;
        private final String beanName;

        private SpringManagedBeanFactory(ApplicationContext ctx, InjectionManager injectionManager, String beanName) {
            this.ctx = ctx;
            this.injectionManager = injectionManager;
            this.beanName = beanName;
        }

        @Override
        public Object get() {
            Object bean = ctx.getBean(beanName);
            if (bean instanceof Advised) {
                try {
                    // Unwrap the bean and inject the values inside of it
                    Object localBean = ((Advised) bean).getTargetSource().getTarget();
                    injectionManager.inject(localBean);
                } catch (Exception e) {
                    // Ignore and let the injection happen as it normally would.
                    injectionManager.inject(bean);
                }
            } else {
                injectionManager.inject(bean);
            }
            return bean;
        }
    }
}
