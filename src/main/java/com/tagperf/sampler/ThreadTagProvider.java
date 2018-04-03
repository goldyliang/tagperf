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
        //currentTag = new ThreadLocal<String>();
        tagExecRecords = new ThreadLocal<TagExecRecordsPerThread>() {
            @Override
            public TagExecRecordsPerThread initialValue() {
                return new TagExecRecordsPerThread(Thread.currentThread().getId());
            }
        };
    }

    private static Field threadLocalsField;
    private static Field valueField;
    private static Method getEntryMethod;

    private static ThreadTagProvider singletonInstance;

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
    }

    public static ThreadTagProvider instance() {
        return singletonInstance;
    }

    /*private TagExecRecord getTagExecRecord (String tag) {
        TagExecRecordsPerThread execRecords = tagExecRecords.get();
        return execRecords.getTagExecRecord(tag);
    }*/

    public void setTag (String tag) {
        tagExecRecords.get().addTagEnter(tag);
    }

    public void unsetTag () {
         tagExecRecords.get().addTagExit();
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
            }
        }
        return recordsPerThreads;
    }

    private static long readInt (BufferedReader br) throws IOException {
        String input = br.readLine();
        return Long.valueOf(input);
    }

    public static void main(String[] args) throws IOException {
        final long[] ratios = new long[20];
        final long[] sleepInTags = new long[20];
        Arrays.fill (ratios, 500000);
        Arrays.fill (sleepInTags, 5);
        final long[] sleeps = {5};
        final boolean[] enableTag = new boolean[1];
        enableTag[0] = true;

        try {
            ThreadTag.registerMBean();
        } catch (Exception e) {
            System.out.println ("Can not registerer MBean for ThreadTag");
            e.printStackTrace();
            System.exit(1);
        }


        class TestThread extends Thread {

            @Override
            public void run() {
                Random r = new Random();
                boolean tagEnabled = enableTag[0];

                while (true) {
                    int i = r.nextInt(ratios.length);
                    if (tagEnabled) {
                        if (i > 0) {
                            String tag = "Tag-" + String.valueOf(i);
                            ThreadTagProvider.instance().setTag(tag);
                        }
                    }
                    //long l1 = System.currentTimeMillis();
                    double test = 0;
                    for (int n=0;n<ratios[i];n++) {
                        test = test + Math.sin(n);
                    }
                    //long l2 = System.currentTimeMillis();
                    //System.out.println (String.valueOf(l2-l1));
                    //System.out.println(tag + ":" + String.valueOf(test));
                    try {
                        Thread.sleep(sleepInTags[i]);
                    } catch (InterruptedException e) {
                        //
                    }
                    if (tagEnabled) {
                        if (i>0) {
                            ThreadTagProvider.instance().unsetTag();
                        }
                    }
                    tagEnabled = enableTag[0];
                    /*try {
                        Thread.sleep(sleeps[0]);
                    } catch (InterruptedException e) {
                        //
                    }*/
                }
            }
        }

        TestThread[] threads = new TestThread[20];
        for (int  i= 0; i<20; i++) {
            threads[i] = new TestThread();
            threads[i].start();
        }


        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {

            System.out.println("Current dummy loop counts:");
            for (int i = 0; i < ratios.length; i++) {
                System.out.print(ratios[i]);
                System.out.print(",");
            }
            System.out.println();

            System.out.println("Current sleep time:");
            for (int i = 0; i < ratios.length; i++) {
                System.out.print(sleepInTags[i]);
                System.out.print(",");
            }
            System.out.println();

            System.out.println("1. Change dummy loop count within tag.");
            System.out.println("2. Change sleep time within tag");
            if (enableTag[0]) {
                System.out.println("3. Disable tag");
            } else {
                System.out.println("3. Enable tag");
            }

            String input = br.readLine();

            if (input.equals("1")) {
                System.out.print ("Tag Num:");
                int num = (int)readInt (br);
                System.out.print ("Loop count");
                long cnt = readInt (br);
                ratios[num] = cnt;
            } else if (input.equals("2")) {
                System.out.print ("Tag Num:");
                int num = (int)readInt (br);
                System.out.print ("Sleep time");
                long tm = readInt (br);
                sleepInTags[num] = tm;
            } else if (input.equals("3")) {
                enableTag[0] = !enableTag[0];
            }
        }

        /*for (int i = 0; i < 60; i++) {
            TagExecRecordsPerThread[] recordsPerThreads = ThreadTagProvider.instance()
                    .getAllThreadTagExecRecords();
            for (TagExecRecordsPerThread recordOfThread : recordsPerThreads) {
                if (recordOfThread != null) {
                    System.out.println("ID:" + recordOfThread.getThreadId());
                    for (String tag : recordOfThread.getTagSet()) {
                        System.out.println("   Tag          :" + tag);
                        TagExecRecord record = recordOfThread.getTagExecRecord(tag);
                        System.out.println("   CurStartTime :" + record.getCurStartedThreadTime());
                        System.out.println("   CpuUsed      :" + record.getCpuTimeUsed());
                        System.out.println("   TagEntered   :" + record.getTagEnteredCnt());
                        System.out.println("   TagExit      :" + record.getTagExitCnt());
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
        }*/
    }
}