/*
 * Copyright 2013 Stanley Shyiko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.shyiko.sme;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class ServersExtension extends AbstractMavenLifecycleParticipant {

    private static final String[] FIELDS = new String[] {"username", "password", "passphrase", "privateKey",
            "filePermissions", "directoryPermissions"};

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        MojoExecution mojoExecution = new MojoExecution(new MojoDescriptor());
        ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        Map<String, String> properties = new HashMap<String, String>();
        try {
            for (Server server : session.getSettings().getServers()) {
                for (String field : FIELDS) {
                    String fieldNameWithFirstLetterCapitalized = upperCaseFirstLetter(field);
                    String fieldValue = (String) Server.class.
                            getMethod("get" + fieldNameWithFirstLetterCapitalized).invoke(server);
                    String resolvedValue = (String) expressionEvaluator.evaluate(fieldValue);
                    Server.class.getMethod("set" + fieldNameWithFirstLetterCapitalized, new Class[]{String.class}).
                            invoke(server, resolvedValue);
                    if (resolvedValue != null) {
                        properties.put("settings.servers.server." + server.getId() + "." + field,
                                resolvedValue); // legacy syntax, left for backward compatibility
                        properties.put("settings.servers." + server.getId() + "." + field, resolvedValue);
                    }
                }
            }
            for (MavenProject project : session.getProjects()) {
                project.getProperties().putAll(properties);
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Failed to expose settings.servers.*", e);
        }
    }

    private String upperCaseFirstLetter(String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }
}