/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.carlspring.strongbox.actuator;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.logging.LogFileWebEndpointProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link LogFileStreamWebEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Przemyslaw Fusik
 */
@Configuration
@ConditionalOnEnabledEndpoint(endpoint = LogFileStreamWebEndpoint.class)
@EnableConfigurationProperties(LogFileWebEndpointProperties.class)
public class LogFileStreamWebEndpointAutoConfiguration
{

    private final LogFileWebEndpointProperties properties;

    public LogFileStreamWebEndpointAutoConfiguration(LogFileWebEndpointProperties properties)
    {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SseEmitterAwareTailerListenerAdapter tailerListener(SseEmitter sseEmitter)
    {
        return new SseEmitterAwareTailerListenerAdapter(sseEmitter);
    }

    @Bean
    @ConditionalOnMissingBean
    @Conditional(LogFileCondition.class)
    public LogFileStreamWebEndpoint logFileStreamWebEndpoint(ApplicationContext applicationContext)
    {
        return new LogFileStreamWebEndpoint(applicationContext, this.properties.getExternalFile());
    }

    private static class LogFileCondition
            extends SpringBootCondition
    {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context,
                                                AnnotatedTypeMetadata metadata)
        {
            Environment environment = context.getEnvironment();
            String config = environment.resolvePlaceholders("${logging.file:}");
            ConditionMessage.Builder message = ConditionMessage.forCondition("Log File");
            if (StringUtils.hasText(config))
            {
                return ConditionOutcome
                               .match(message.found("logging.file").items(config));
            }
            config = environment.resolvePlaceholders("${logging.path:}");
            if (StringUtils.hasText(config))
            {
                return ConditionOutcome
                               .match(message.found("logging.path").items(config));
            }
            config = environment.getProperty("management.endpoint.logfile.external-file");
            if (StringUtils.hasText(config))
            {
                return ConditionOutcome
                               .match(message.found("management.endpoint.logfile.external-file")
                                             .items(config));
            }
            return ConditionOutcome.noMatch(message.didNotFind("logging file").atAll());
        }

    }

}