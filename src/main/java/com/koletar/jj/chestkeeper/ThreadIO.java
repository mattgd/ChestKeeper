package com.koletar.jj.chestkeeper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jjkoletar
 */
public class ThreadIO implements Runnable {
    private final Map<String, String> ioQueue;
    private final File saveFolder;
    private boolean isRunning;

    public ThreadIO(final Map<String, String> ioQueue, final File saveFolder) {
        this.ioQueue = ioQueue;
        this.saveFolder = saveFolder;
    }

    public void run() {
        isRunning = true;
        try {
            while (isRunning()) {
                final Map<String, String> toWrite;
                synchronized (ioQueue) {
                    while (isRunning && ioQueue.isEmpty()) {
                        ioQueue.wait();
                    }
                    toWrite = new HashMap<String, String>();
                    toWrite.putAll(ioQueue);
                    ioQueue.clear();
                }
                process(toWrite);
            }
        } catch (InterruptedException e) {
            ChestKeeper.logger.severe("IO Thread was interrupted!");
            process(ioQueue);
        } finally {
            synchronized (ioQueue) {
                isRunning = false;
                ioQueue.notifyAll();
            }
        }
    }

    private void process(final Map<String, String> toWrite) {
        for (Map.Entry<String, String> entry : toWrite.entrySet()) {
            try {
                File saveFile = new File(saveFolder, ChestKeeper.getFileName(entry.getKey()));
                FileWriter fw = new FileWriter(saveFile, false);
                fw.write(entry.getValue());
                fw.close();
                toWrite.remove(entry.getKey());
                ChestKeeper.trace("saved user " + entry.getKey());
            } catch (IOException e) {
                ChestKeeper.logger.severe("Unable to save a user to disk! Username: " + entry.getKey());
                e.printStackTrace();
            }
        }
    }

    public boolean isRunning() {
        synchronized (ioQueue) {
            return isRunning;
        }
    }

    public void shutdown() {
        synchronized (ioQueue) {
            if (isRunning) {
                isRunning = false;
                ioQueue.notifyAll(); //wake up processing loop
                try {
                    ioQueue.wait();
                } catch (InterruptedException e) {
                    ChestKeeper.logger.severe("Shutdown process of IO Thread was interrupted!");
                }
            }
        }
    }
}
