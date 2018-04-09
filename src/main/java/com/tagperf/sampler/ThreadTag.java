package com.tagperf.sampler;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Created by goldyliang on 3/6/18.
 */
public class ThreadTag implements ThreadTagMBean {
    public static final String MXBEAN_NAME = "com.tagperf.sampler:type=ThreadTag";

    private ThreadTagProvider threadTagProvider = ThreadTagProvider.instance();

    public void enableTagging() {
        threadTagProvider.enableTagging();
    }

    public void disableTagging() {
        threadTagProvider.disableTagging();
    }

    public boolean isTaggingEnabled() {
        return threadTagProvider.isTaggingEnabled();
    }

    public TagExecRecordsPerThread [] getAllThreadTagExecRecords() {
        //System.out.println ("Getting thread state.");
        return threadTagProvider.getAllThreadTagExecRecords();
    }

    public static void registerMBean() throws Exception{
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(MXBEAN_NAME);
        ThreadTag mbean = new ThreadTag();
        mbs.registerMBean(mbean, name);
    }
}
