package com.tagperf.sampler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/**
 * Tagged CPU sampler, which captures the current tag of each thread,
 * and retrieve the current status of the threads and the tag.
 *
 * Created by Goldy Liang on 2/7/18.
 */
public class ThreadTagProvider {

    // A String based tag held by each thread locally
    //private ThreadLocal<String> currentTag;

    private ThreadLocal<TagExecRecordsPerThread> tagExecRecords;

    private ThreadTagProvider() {
        initTagExecRecords();
    }

    private static Field threadLocalsField;
    private static Field valueField;
    private static Method getEntryMethod;

    private static ThreadTagProvider singletonInstance;

    private boolean taggingEnabled = false;

    static {
        try {
            threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);

            getEntryMethod = Class.forName("java.lang.ThreadLocal$ThreadLocalMap").getDeclaredMethod("getEntry", ThreadLocal.class);
            getEntryMethod.setAccessible(true);

            valueField = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry").getDeclaredField("value");
            valueField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        singletonInstance = new ThreadTagProvider();
        singletonInstance.initTagExecRecords();
    }

    public static ThreadTagProvider instance() {
        return singletonInstance;
    }

    private void initTagExecRecords () {
        tagExecRecords = new ThreadLocal<TagExecRecordsPerThread>() {
            @Override
            public TagExecRecordsPerThread initialValue() {
                return new TagExecRecordsPerThread(Thread.currentThread().getId());
            }
        };
    }

    /*private TagExecRecord getTagExecRecord (String tag) {
        TagExecRecordsPerThread execRecords = tagExecRecords.get();
        return execRecords.getTagExecRecord(tag);
    }*/

    public void enableTagging () {
        if (taggingEnabled == false) {
            initTagExecRecords();
            taggingEnabled = true;
        }
    }

    public void disableTagging () {
        taggingEnabled = false;
    }

    public boolean isTaggingEnabled() {
        return taggingEnabled;
    }

    public void setTag (String tag) {
        if (taggingEnabled) {
            tagExecRecords.get().addTagEnter(tag);
        }
    }

    public void unsetTag () {
        if (taggingEnabled) {
            tagExecRecords.get().addTagExit();
        }
    }

    private TagExecRecordsPerThread getTagExecRecordOfThread (Thread thread) {
        try {
            Object map = threadLocalsField.get(thread);
            if (map == null) {
                return null;
            }
            WeakReference entry = (WeakReference) getEntryMethod.invoke(map, tagExecRecords);
            if (entry == null) {
                return null;
            }
            return (TagExecRecordsPerThread)(valueField.get(entry));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ThreadGroup rootThreadGroup = null;

    ThreadGroup getRootThreadGroup( ) {
        if ( rootThreadGroup != null )
            return rootThreadGroup;
        ThreadGroup tg = Thread.currentThread( ).getThreadGroup( );
        ThreadGroup ptg;
        while ( (ptg = tg.getParent( )) != null )
            tg = ptg;
        return tg;
    }

    private Thread[] getAllThreads( ) {
        final ThreadGroup root = getRootThreadGroup( );
        final ThreadMXBean thbean = ManagementFactory.getThreadMXBean( );
        int nAlloc = thbean.getThreadCount( );
        int n = 0;
        Thread[] threads;
        do {
            nAlloc *= 2;
            threads = new Thread[ nAlloc ];
            n = root.enumerate( threads, true );
        } while ( n == nAlloc );
        return Arrays.copyOf( threads, n );
    }

    public TagExecRecordsPerThread [] getAllThreadTagExecRecords () {

        Thread[] allThreads = getAllThreads();

        TagExecRecordsPerThread [] recordsPerThreads = new TagExecRecordsPerThread [allThreads.length];

        for (int i = 0; i < allThreads.length; i++) {
            Thread thread = allThreads[i];
            TagExecRecordsPerThread tagExecRecordThread = getTagExecRecordOfThread(thread);
            if (tagExecRecordThread != null) { // Ignore system threads
                tagExecRecordThread.addTagSample();
                recordsPerThreads[i] = tagExecRecordThread.deepCloneAndReset();
            } else {
                recordsPerThreads[i] = new TagExecRecordsPerThread(thread.getId());
            }
        }
        return recordsPerThreads;
    }

}