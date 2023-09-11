package com.bdkjzx.project.i18n.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import com.bdkjzx.project.i18n.interceptor.I18nInterceptor;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class I18nFilter implements Filter, Ordered {
    private I18nInterceptor i18nInterceptor;

    public I18nInterceptor getI18nInterceptor() {
        return i18nInterceptor;
    }

    public void setI18nInterceptor(I18nInterceptor i18nInterceptor) {
        this.i18nInterceptor = i18nInterceptor;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (this.httpServlet(servletRequest, servletResponse)) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
            try {
                i18nInterceptor.preHandle(httpServletRequest, httpServletResponse, null);
            } catch (Exception e) {
                throw new IOException(e);
            }

            filterChain.doFilter(servletRequest, servletResponse);

            i18nInterceptor.afterCompletion(httpServletRequest, httpServletResponse, null, null);

        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private boolean httpServlet(ServletRequest servletRequest, ServletResponse servletResponse) {
        return servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse;
    }

    @Override
    public void destroy() {

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
