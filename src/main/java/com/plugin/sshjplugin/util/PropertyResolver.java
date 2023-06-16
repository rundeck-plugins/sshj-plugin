package com.plugin.sshjplugin.util;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.utils.ResolverUtil;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.plugin.sshjplugin.SSHJNodeExecutorPlugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class PropertyResolver {
    private INodeEntry node;
    private Framework framework;
    private ExecutionContext context;
    private IRundeckProject frameworkProject;

    public PropertyResolver(INodeEntry node, Framework framework, ExecutionContext context) {
        this.node = node;
        this.framework = framework;
        this.context = context;
        this.frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(
                context.getFrameworkProject());
    }

    public String resolve(final String propName, final String defaultValue) {
        return ResolverUtil.resolveProperty(propName, defaultValue, node, frameworkProject, framework);
    }

    public String resolve(final String propName) {
        return ResolverUtil.resolveProperty(propName, null, node, frameworkProject, framework);
    }

    public Boolean resolveBoolean(final String propName) {
        return ResolverUtil.resolveBooleanProperty(
                propName,
                false,
                node,
                frameworkProject,
                framework
        );
    }

    public InputStream getPrivateKeyStorageFromProperty(String property) throws IOException {
        String path = resolve(property);
        return getPrivateKeyStorageData(path);

    }

    public InputStream getPrivateKeyStorageData(String path) throws IOException {
        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferencesInString(path, context.getDataContext());
        }
        if (null == path) {
            return null;
        }
        return context
                .getStorageTree()
                .getResource(path)
                .getContents()
                .getInputStream();

    }

    public String getPrivateKeyStorage(String path) throws IOException {
        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferencesInString(path, context.getDataContext());
        }
        if (null == path) {
            return null;
        }

        ResourceMeta contents = context.getStorageTree().getResource(path).getContents();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        contents.writeContent(byteArrayOutputStream);
        return byteArrayOutputStream.toString();

    }

    public String getStoragePath(String property) {
        String path = resolve(property);

        //expand properties in path
        if (path != null && path.contains("${")) {
            path = DataContextUtils.replaceDataReferencesInString(path, context.getDataContext());
        }
        return path;
    }

    public String getPasswordFromPath(String path) throws IOException {
        ResourceMeta contents = context.getStorageTree().getResource(path).getContents();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        contents.writeContent(byteArrayOutputStream);
        return byteArrayOutputStream.toString();
    }

    public String nonBlank(final String input) {
        if (null == input || "".equals(input.trim())) {
            return null;
        } else {
            return input.trim();
        }
    }

    /**
     * Look for a node/project/framework config property of the given key, and if not found
     * fallback to a framework property, return parsed long or the default
     *
     * @param key           key for node attribute/project/framework property
     * @param frameworkProp fallback framework property
     * @param defval        default value
     * @return parsed value or default
     */
    public Long resolveLongFwk(final String key, final String frameworkProp, final long defval) {
        long timeout = defval;
        String opt = resolve(key);
        if (opt == null && frameworkProp != null && framework.getPropertyLookup().hasProperty(frameworkProp)) {
            opt = framework.getProperty(frameworkProp);
        }
        if (opt != null) {
            try {
                timeout = Long.parseLong(opt);
            } catch (NumberFormatException ignored) {
            }
        }
        return timeout;
    }

    public String evaluateSecureOption(final String optionName, final ExecutionContext context) {
        if (null == optionName) {
            context.getExecutionListener().log(3, "option name was null");
            return null;
        }
        if (null == context.getPrivateDataContext()) {
            context.getExecutionListener().log(3, "private context was null");
            return null;
        }
        final String[] opts = optionName.split("\\.", 2);
        if (null != opts && 2 == opts.length) {
            final Map<String, String> option = context.getPrivateDataContext().get(opts[0]);
            if (null != option) {
                final String value = option.get(opts[1]);
                if (null == value) {
                    context.getExecutionListener().log(3, "private context '" + optionName + "' was null");
                }
                return value;
            } else {
                context.getExecutionListener().log(3, "private context '" + opts[0] + "' was null");
            }
        }
        return null;
    }

    public boolean hasProperty(String property){
        return framework.hasProperty(property);
    }

    public String getProperty(String property){
        return framework.getProperty(property);
    }

}
