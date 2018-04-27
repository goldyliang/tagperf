package com.tagperf.sampler;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by goldyliang on 4/1/18.
 */
public class TagExecRecordsPerThread implements Serializable {
    private String currentTag;
    private Map<String, TagExecRecord> execRecords;

    private long threadId;

    private long lastSampledThreadCpuTime;
    private long threadCpuTimeSinceLastSample;

    private transient ThreadMXBean threadMBean;

    public TagExecRecordsPerThread deepCloneAndReset() {
        TagExecRecordsPerThread newRecPerThread = new TagExecRecordsPerThread(threadId);
        newRecPerThread.currentTag = currentTag;
        long threadCpuTime = getThreadCpuTime(threadId);
        newRecPerThread.threadCpuTimeSinceLastSample = threadCpuTime - lastSampledThreadCpuTime;
        newRecPerThread.execRecords = new HashMap<String, TagExecRecord>();

        for (String tag : execRecords.keySet()) {
            TagExecRecord rec = execRecords.get(tag);
            newRecPerThread.execRecords.put (tag, rec.clone());
            rec.reset();
        }
        this.threadCpuTimeSinceLastSample = 0;
        this.lastSampledThreadCpuTime = threadCpuTime;
        return newRecPerThread;
    }

    private long getCurrentThreadCpuTime () {
        return threadMBean.getThreadCpuTime(Thread.currentThread().getId());
    }

    private long getThreadCpuTime(long threadId) {
        return threadMBean.getThreadCpuTime(threadId);
    }

    public TagExecRecordsPerThread() {
        execRecords = new ConcurrentHashMap<String, TagExecRecord>();
        threadMBean = ManagementFactory.getThreadMXBean();
        lastSampledThreadCpuTime = getThreadCpuTime(threadId);
    }


    public TagExecRecordsPerThread(long threadId) {
        execRecords = new HashMap<String, TagExecRecord>();
        this.threadId = threadId;
        threadMBean = ManagementFactory.getThreadMXBean();
        lastSampledThreadCpuTime = getThreadCpuTime(threadId);
    }

    public long getThreadId() {
        return threadId;
    }

    public long getThreadCpuTimeSinceLastSample() {
        return threadCpuTimeSinceLastSample;
    }

/*    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }*/

    public TagExecRecord getTagExecRecord (String tag) {
        return execRecords.get(tag);
    }

    private void addTagEnter_internal (String tag, long currentThreadCpuTime) {
        synchronized (this) {
            if (currentTag != null) {
                execRecords.get(currentTag).addTagExit(currentThreadCpuTime);
            }

            currentTag = tag;
            TagExecRecord record = execRecords.get(tag);
            if (record == null) {
                record = new TagExecRecord(tag);
                execRecords.put(tag, record);
            }
            record.addTagEnter(currentThreadCpuTime);
        }
    }

    public void addTagEnter (String tag) {
        long currentThreadCpuTime = getCurrentThreadCpuTime();
        addTagEnter_internal (tag, currentThreadCpuTime);
    }

    public void addTagExit () {
        synchronized (this) {
            if (currentTag != null) {
                long currentThreadCpuTime = getCurrentThreadCpuTime();

                execRecords.get(currentTag).addTagExit(currentThreadCpuTime);
                currentTag = null;
                addTagEnter_internal("<null>", currentThreadCpuTime);
                //currentTag = null;
            }
        }
    }

    public void addTagSample () {
        synchronized (this) {
            if (currentTag != null)
                execRecords.get(currentTag).addSample(threadId, getThreadCpuTime(threadId));
        }
    }

    public Set<String> getTagSet() {
        return execRecords.keySet();
    }
}
