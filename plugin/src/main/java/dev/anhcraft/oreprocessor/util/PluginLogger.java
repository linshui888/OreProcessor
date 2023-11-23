package dev.anhcraft.oreprocessor.util;

import java.io.File;

public class PluginLogger {

    public PluginLogger(File dir) {
        //
    }

    public void writeRaw(String str, Object... args) {
        //
    }

    public ScopedLog scope(String scope) {
        return new ScopedLog(this, scope);
    }

    public synchronized void flush() {
        //
    }
}
