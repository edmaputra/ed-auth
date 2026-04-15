package io.github.edmaputra.enhauthserv.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

  private static final Pattern TENANT_PATH_PATTERN = Pattern.compile("^/t/([A-Za-z0-9_-]+)(/.*)?$");
  private static final Pattern TENANT_MACHINE_ENDPOINT_PATTERN =
      Pattern.compile("^/t/([A-Za-z0-9_-]+)/(oauth2/introspect|oauth2/revoke)$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    try {
      String requestUri = request.getRequestURI();
      if (requestUri != null) {
        Matcher machineEndpointMatcher = TENANT_MACHINE_ENDPOINT_PATTERN.matcher(requestUri);
        if (machineEndpointMatcher.matches()) {
          TenantContext.setCurrentTenant(machineEndpointMatcher.group(1));
          String rewrittenPath = "/" + machineEndpointMatcher.group(2);
          filterChain.doFilter(new MachineEndpointRewriteRequest(request, rewrittenPath), response);
          return;
        }
      }

      resolveTenantFromRequestPath(request).ifPresent(TenantContext::setCurrentTenant);
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private java.util.Optional<String> resolveTenantFromRequestPath(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    if (requestUri == null || requestUri.isBlank()) {
      return java.util.Optional.empty();
    }

    Matcher matcher = TENANT_PATH_PATTERN.matcher(requestUri);
    if (!matcher.matches()) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.ofNullable(matcher.group(1));
  }

  private static final class MachineEndpointRewriteRequest extends HttpServletRequestWrapper {

    private final String rewrittenPath;

    private MachineEndpointRewriteRequest(HttpServletRequest request, String rewrittenPath) {
      super(request);
      this.rewrittenPath = rewrittenPath;
    }

    @Override
    public String getRequestURI() {
      return rewrittenPath;
    }

    @Override
    public String getServletPath() {
      return rewrittenPath;
    }

    @Override
    public StringBuffer getRequestURL() {
      StringBuffer url = new StringBuffer();
      url.append(getScheme())
          .append("://")
          .append(getServerName());
      if (!(getScheme().equals("http") && getServerPort() == 80)
          && !(getScheme().equals("https") && getServerPort() == 443)) {
        url.append(':').append(getServerPort());
      }
      url.append(rewrittenPath);
      return url;
    }
  }
}