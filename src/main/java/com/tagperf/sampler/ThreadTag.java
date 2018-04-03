package com.tagperf.sampler;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Created by goldyliang on 3/6/18.
 */
public class ThreadTag implements ThreadTagMBean {
    public static final String MXBEAN_NAME = "com.tagperf.sampler:type=ThreadTag";

    public void enableTag() {

    }

    public void disableTag() {

    }

    public TagExecRecordsPerThread [] getAllThreadTagExecRecords() {
        //System.out.println ("Getting thread state.");
        return ThreadTagProvider.instance().getAllThreadTagExecRecords();
    }

    public static void registerMBean() throws Exception{
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(MXBEAN_NAME);
        ThreadTag mbean = new ThreadTag();
        mbs.registerMBean(mbean, name);
    }
}
