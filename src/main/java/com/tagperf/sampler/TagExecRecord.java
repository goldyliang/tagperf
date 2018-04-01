package com.tagperf.sampler;

import java.beans.Transient;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * Created by elnggng on 4/1/18.
 */
public class TagExecRecord implements Serializable, Cloneable {
    String tag;
    private boolean curTagOngoing;
    private long curStartedThreadTime; // the thread start time of the current onging thread which is not collected yet
                                          // (valid when curTagOngoing = true)
    private long cpuTimeUsed;
    private transient long cpuTimeUsedEver_Prev;
    private long cpuTimeUsedEver;
    private long tagEnteredCnt;
    private long tagExitCnt;

    private transient ThreadMXBean threadMBean;

    public TagExecRecord (String tag) {
        this.tag = tag;
        curTagOngoing = false;
        cpuTimeUsed = 0;
        cpuTimeUsedEver_Prev = cpuTimeUsedEver = 0;
        tagEnteredCnt = tagExitCnt = 0;

        threadMBean = ManagementFactory.getThreadMXBean();
    }

    public TagExecRecord clone() {
        TagExecRecord newRecord = new TagExecRecord(tag);
        newRecord.tag = tag;
        newRecord.curTagOngoing = curTagOngoing;
        newRecord.curStartedThreadTime = curStartedThreadTime;
        newRecord.cpuTimeUsed = cpuTimeUsed;
        newRecord.cpuTimeUsedEver = cpuTimeUsedEver;
        newRecord.tagEnteredCnt = tagEnteredCnt;
        newRecord.tagExitCnt = tagExitCnt;
        return newRecord;
    }

    public long getCpuTimeUsed () {
        return cpuTimeUsed;
    }

    public long getCpuTimeUsedEver () {
        return cpuTimeUsedEver;
    }

    public long getTagEnteredCnt() {
        return tagEnteredCnt;
    }

    public long getTagExitCnt() {
        return tagExitCnt;
    }

    public long getCurStartedThreadTime() {
        if (curTagOngoing) {
            return curStartedThreadTime;
        } else {
            return 0;
        }
    }

    private long getCurrentThreadCpuTime () {
        return threadMBean.getThreadCpuTime(Thread.currentThread().getId());
    }

    private long getThreadCpuTime(long threadId) {
        return threadMBean.getThreadCpuTime(threadId);
    }

    public void addTagEnter () {
        curTagOngoing = true;
        curStartedThreadTime = getCurrentThreadCpuTime();
        tagEnteredCnt++;
    }

    public void addTagExit () {
        if (curTagOngoing) {
            cpuTimeUsedEver += (getCurrentThreadCpuTime() - curStartedThreadTime);
            cpuTimeUsed = cpuTimeUsedEver - cpuTimeUsedEver_Prev;
            cpuTimeUsedEver_Prev = cpuTimeUsedEver;
            tagExitCnt++;
            curTagOngoing = false;
        } else {
            // Shall not happen
            throw new IllegalStateException("addTagExit when not in tag");
        }
    }

    // This is not called within the same thread who set the tags
    public void addSample (long thread) {
        if (curTagOngoing) {
            long cpuTime = getThreadCpuTime(thread);

            cpuTimeUsedEver += cpuTime - curStartedThreadTime;
            cpuTimeUsed = cpuTimeUsedEver - cpuTimeUsedEver_Prev;
            cpuTimeUsedEver_Prev = cpuTimeUsedEver;

            curStartedThreadTime = cpuTime;
        } else {
            // Shall not happen
            throw new IllegalStateException("addSample when not in tag");
        }
    }

    public void reset () {
        cpuTimeUsed = 0;
    }
}
