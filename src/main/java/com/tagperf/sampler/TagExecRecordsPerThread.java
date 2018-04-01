package com.tagperf.sampler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by elnggng on 4/1/18.
 */
public class TagExecRecordsPerThread implements Serializable {
    private String currentTag;
    private Map<String, TagExecRecord> execRecords;

    private long threadId;

    public TagExecRecordsPerThread deepCloneAndReset() {
        synchronized (this) {
            TagExecRecordsPerThread newRecPerThread = new TagExecRecordsPerThread(threadId);
            newRecPerThread.currentTag = currentTag;
            newRecPerThread.execRecords = new HashMap<String, TagExecRecord>();

            for (String tag : execRecords.keySet()) {
                TagExecRecord rec = execRecords.get(tag);
                newRecPerThread.execRecords.put (tag, rec.clone());
                rec.reset();
            }
            return newRecPerThread;
        }
    }

    public TagExecRecordsPerThread() {
        execRecords = new HashMap<String, TagExecRecord>();
    }


    public TagExecRecordsPerThread(long threadId) {
        execRecords = new HashMap<String, TagExecRecord>();
        this.threadId = threadId;
    }

    public long getThreadId() {
        return threadId;
    }

/*    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }*/

    public TagExecRecord getTagExecRecord (String tag) {
        return execRecords.get(tag);
    }

    public void addTagEnter (String tag) {
        synchronized (this) {
            if (currentTag != null) {
                addTagExit();
            }

            currentTag = tag;
            TagExecRecord record = execRecords.get(tag);
            if (record == null) {
                record = new TagExecRecord(tag);
                execRecords.put(tag, record);
            }
            record.addTagEnter();
        }
    }

    public void addTagExit () {
        synchronized (this) {
            if (currentTag != null) {
                execRecords.get(currentTag).addTagExit();
                currentTag = null;
            }
        }
    }

    public void addTagSample () {
        synchronized (this) {
            if (currentTag != null)
                execRecords.get(currentTag).addSample(threadId);
        }
    }

    public Set<String> getTagSet() {
        return execRecords.keySet();
    }
}
