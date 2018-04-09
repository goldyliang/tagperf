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
    Pattern dynamicRegexPattern;
    String dynamicRegex;
    ThreadTagProvider threadTagProvider = ThreadTagProvider.instance();

    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            ThreadTag.registerMBean();
            inited = true;
        } catch (Exception e) {
            System.out.println("ThreadTag MBean Creation Failure");
        }

        dynamicRegex = filterConfig.getInitParameter("DynamicPartRegex");
        dynamicRegexPattern = Pattern.compile(dynamicRegex);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (!threadTagProvider.isTaggingEnabled()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (!inited) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest)servletRequest;
            String uri = req.getRequestURI();

            //String tag = dynamicRegexPattern.matcher(url).replaceAll("*");
            if (dynamicRegex != null) {
                uri = dynamicRegexPattern.matcher(uri).replaceAll("*");
                //tag = tag.replaceAll(dynamicRegex, "*");
            }
            StringBuilder builder = new StringBuilder();
            builder.append(req.getMethod());
            builder.append(" ");
            builder.append(uri);
            String tag = builder.toString();

            //System.out.println ("Tag: " + tag);

            ThreadTagProvider.instance().setTag(tag);
            filterChain.doFilter(servletRequest, servletResponse);
            ThreadTagProvider.instance().unsetTag();
        }
    }

    public void destroy() {

    }
}
