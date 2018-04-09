package com.tagperf.sampler;

/**
 * Created by goldyliang on 3/6/18.
 */
public interface ThreadTagMBean {
    public void enableTagging();
    public void disableTagging();
    public boolean isTaggingEnabled();
    public TagExecRecordsPerThread [] getAllThreadTagExecRecords();
}
