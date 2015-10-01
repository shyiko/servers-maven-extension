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
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:stanley.shyiko@gmail.com">Stanley Shyiko</a>
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class ServersExtension extends AbstractMavenLifecycleParticipant implements Contextualizable {

    private static final String SECURITY_DISPATCHER_CLASS_NAME =
        "org.sonatype.plexus.components.sec.dispatcher.SecDispatcher";

    private static final String[] FIELDS = new String[]{"username", "password", "passphrase", "privateKey",
        "filePermissions", "directoryPermissions"};

    private PlexusContainer container;

    public void contextualize(final Context context) throws ContextException {
        container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        MojoExecution mojoExecution = new MojoExecution(new MojoDescriptor());
        ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        Properties userProperties = session.getUserProperties();

        boolean exportAsSysProp = isExtensionProperty(session, "servers.exportAsSysProp");

        Map<String, String> properties = new HashMap<String, String>();
        try {
            for (Server server : session.getSettings().getServers()) {
                String serverId = server.getId();
                for (String field : FIELDS) {
                    String[] aliases = getAliases(serverId, field);
                    String fieldNameWithFirstLetterCapitalized = upperCaseFirstLetter(field);
                    String fieldValue = (String) Server.class.
                        getMethod("get" + fieldNameWithFirstLetterCapitalized).invoke(server);
                    if (fieldValue != null) {
                        fieldValue = decryptInlinePasswords(fieldValue);
                    }
                    for (String alias : aliases) {
                        String userPropertyValue = userProperties.getProperty(alias);
                        if (userPropertyValue != null) {
                            fieldValue = userPropertyValue;
                            break;
                        }
                    }
                    String resolvedValue = (String) expressionEvaluator.evaluate(fieldValue);
                    Server.class.getMethod("set" + fieldNameWithFirstLetterCapitalized, new Class[]{String.class}).
                        invoke(server, resolvedValue);
                    if (resolvedValue != null) {
                        for (String alias : aliases) {
                            properties.put(alias, resolvedValue);
                        }
                    }
                }
            }

            if (exportAsSysProp) {
                System.getProperties().putAll(properties);
            } else {
                for (MavenProject project : session.getProjects()) {
                    Properties projectProperties = project.getProperties();
                    projectProperties.putAll(properties);
                }
            }
        } catch (Exception e) {
            throw new MavenExecutionException("Failed to expose settings.servers.*", e);
        }
    }

    /**
     * Lookup for property name in maven project.
     *
     * @param session current maven session
     * @param propName property name
     * @return true is property is set and has value "true"
     */
    private boolean isExtensionProperty(MavenSession session, String propName) {
        Properties properties = session.getUserProperties();
        String value = properties.getProperty(propName);
        if (value != null) {
            return Boolean.valueOf(value);
        }

        properties = session.getCurrentProject().getProperties();
        value = properties.getProperty(propName);
        return Boolean.valueOf(value);
    }

    private String decryptInlinePasswords(String v) {
        Pattern p = Pattern.compile("(\\{[^\\}]+\\})");
        Matcher m = p.matcher(v);
        StringBuffer s = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(s, decryptPassword(m.group(1)));
        }
        m.appendTail(s);
        return s.toString();
    }

    private String decryptPassword(String password) {
        try {
            Class<?> securityDispatcherClass = container.getClass().getClassLoader()
                .loadClass(SECURITY_DISPATCHER_CLASS_NAME);
            Object securityDispatcher = container.lookup(SECURITY_DISPATCHER_CLASS_NAME, "maven");
            Method decrypt = securityDispatcherClass.getMethod("decrypt", String.class);
            return (String) decrypt.invoke(securityDispatcher, password);
        } catch (Exception ignore) {
        }
        return password;
    }

    private String[] getAliases(String serverId, String field) {
        // replace space in serverId by "_"
        serverId = serverId.replaceAll(" +", "_");
        return new String[]{
            "settings.servers." + serverId + "." + field,
            "settings.servers.server." + serverId + "." + field, // legacy syntax, left for backward compatibility
        };
    }

    private String upperCaseFirstLetter(String string) {
        return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }
}