package sepoa.fw.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import filters.SecurityRequst;

public final class SecurityFilter implements Filter {

	private List<String> excludePatterns = new ArrayList<String>();

	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("initialize > " + this.getClass().getSimpleName());
		String eps = filterConfig.getInitParameter("excludePatterns");
		if (eps != null) {
			this.excludePatterns = Arrays.asList(eps.split(","));
		}
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String url = ((HttpServletRequest) request).getServletPath();
		if (excludePatterns.contains(url)) {
			chain.doFilter(request, response);
		} else {
			chain.doFilter(new SecurityRequst((HttpServletRequest) request), response);
		}
	}

}
