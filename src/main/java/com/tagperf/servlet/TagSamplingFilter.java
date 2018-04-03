package com.tagperf.servlet;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Pattern;

import com.tagperf.sampler.ThreadTagProvider;
import com.tagperf.sampler.ThreadTag;
/**
 * Created by goldyliang on 4/2/18.
 */
public class TagSamplingFilter implements Filter {
    boolean inited;
    //Pattern dynamicRegexPattern;
    String dynamicRegex;
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            ThreadTag.registerMBean();
            inited = true;
        } catch (Exception e) {
            System.out.println("ThreadTag MBean Creation Failure");
        }

        dynamicRegex = filterConfig.getInitParameter("DynamicPartRegex");
        //dynamicRegexPattern = Pattern.compile(dynamicRegex);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!inited) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (servletRequest instanceof HttpServletRequest) {
            String url = ((HttpServletRequest)servletRequest).getRequestURL().toString();

            //String tag = dynamicRegexPattern.matcher(url).replaceAll("*");
            String tag = url;
            if (dynamicRegex != null) {
                tag = tag.replaceAll(dynamicRegex, "*");
            }

            System.out.println ("Tag: " + tag);

            ThreadTagProvider.instance().setTag(tag);
            filterChain.doFilter(servletRequest, servletResponse);
            ThreadTagProvider.instance().unsetTag();
        }


    }

    public void destroy() {

    }
}
