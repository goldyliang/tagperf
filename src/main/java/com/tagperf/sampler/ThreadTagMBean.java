package com.tagperf.sampler;

/**
 * Created by goldyliang on 3/6/18.
 */
public interface ThreadTagMBean {
    public void enableTag();
    public void disableTag();

    public TagExecRecordsPerThread [] getAllThreadTagExecRecords();
}
