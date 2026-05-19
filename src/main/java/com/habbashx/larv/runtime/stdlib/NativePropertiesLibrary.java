package com.habbashx.larv.runtime.stdlib;

import com.habbashx.larv.error.LarvError;
import com.habbashx.larv.runtime.ExecutionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

@Native("Properties Library")
public class NativePropertiesLibrary implements NativeLibrary {

    private final Properties properties = new Properties();

    private final ExecutionContext executionContext;

    public NativePropertiesLibrary(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public void registerAll() {
        executionContext.registerNative("loadProp",    this::loadProperties);
        executionContext.registerNative("getProp",     this::getProperty);
        executionContext.registerNative("setProp",this::setProperty);
        executionContext.registerNative("saveProp",this::saveProperties);
        executionContext.registerNative("getAllProps",  this::getAllProperties);
    }

    private @NotNull String args(@NotNull List<Object> args, int index, String fnName) {
        if (args.size() <= index || !(args.get(index) instanceof String s))
            throw new LarvError(fnName + "() expects a string path as argument " + (index + 1), -1, LarvError.Kind.RUNTIME);
        return s;
    }

    private @Nullable Object loadProperties(List<Object> args) {
        final String fileName = args(args, 0, "loadProp");
        try (final Reader reader = new BufferedReader(new FileReader(fileName), 1024)) {
            properties.load(reader);
            return null;
        } catch (IOException e) {
            throw new LarvError("loadProp(): cannot read file '" + fileName + "': " + e.getMessage(),
                    -1, LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object setProperty(List<Object> args) {
        final @Nullable String propertyName = args(args,0,"setProp");
        final @Nullable String propertyValue = args(args,2,"setProp");
        properties.setProperty(propertyName,propertyValue);

        return null;
    }

    private @Nullable Object saveProperties(List<Object> args) {

        String fileName = args(args,0,"saveProp");

        try (final Writer writer =  new BufferedWriter(new FileWriter(fileName,true))) {

            properties.store(writer,"");
            return null;
        } catch (IOException e) {
            throw new LarvError("saveProp(): cannot save file '"+fileName+"': "+e.getMessage(),-1,LarvError.Kind.RUNTIME);
        }
    }

    private @Nullable Object getProperty(List<Object> args) {
        return properties.get(args(args, 0, "getProp"));
    }

    private @NotNull Object getAllProperties(List<Object> args) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            result.put(key, properties.getProperty(key));
        }
        return result;
    }
}