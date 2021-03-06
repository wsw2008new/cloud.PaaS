/**
 * Copyright 2014 Telefonica Investigación y Desarrollo, S.A.U <br>
 * This file is part of FI-WARE project.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 * </p>
 * <p>
 * You may obtain a copy of the License at:<br>
 * <br>
 * http://www.apache.org/licenses/LICENSE-2.0
 * </p>
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * </p>
 * <p>
 * See the License for the specific language governing permissions and limitations under the License.
 * </p>
 * <p>
 * For those usages not covered by the Apache version 2.0 License please contact with opensource@tid.es
 * </p>
 */
package com.telefonica.euro_iaas.paasmanager.rest.auth;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.telefonica.euro_iaas.paasmanager.util.SystemPropertiesProvider;
import com.telefonica.euro_iaas.sdc.model.dto.PaasManagerUser;

public class OpenStackAuthenticationFilterTest {

    private OpenStackAuthenticationFilter openStackAuthenticationFilter;

    private AuthenticationManager authenticationManager;
    private AuthenticationEntryPoint authenticationEntryPoint;
    private SystemPropertiesProvider systemPropertiesProvider;

    @Before
    public void setup() {
        authenticationManager = mock(AuthenticationManager.class);
        authenticationEntryPoint = mock(AuthenticationEntryPoint.class);
        systemPropertiesProvider = mock(SystemPropertiesProvider.class);
        when(systemPropertiesProvider.getProperty(any(String.class))).thenReturn("keystoneURL");

        openStackAuthenticationFilter = new OpenStackAuthenticationFilter();
        openStackAuthenticationFilter.setAuthenticationManager(authenticationManager);
        openStackAuthenticationFilter.setAuthenticationEntryPoint(authenticationEntryPoint);
        openStackAuthenticationFilter.setSystemPropertiesProvider(systemPropertiesProvider);
    }

    @Test
    public void doFilterTestRootPath() throws IOException, ServletException {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpSession httpSession = mock(HttpSession.class);

        when(servletRequest.getHeader(anyString())).thenReturn("3df25213cac246f8bccad5c70cb3582e").thenReturn(
                "00000000000000000000000000000194");
        when(servletRequest.getPathInfo()).thenReturn("/");
        when(servletRequest.getRequestURI()).thenReturn("/vdc/00000000000000000000000000000194/");
        when(servletRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("1234");

        openStackAuthenticationFilter.doFilter(servletRequest, servletResponse, filterChain);
    }

    @Test
    public void doFilterTestAnyPath() throws IOException, ServletException {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpSession httpSession = mock(HttpSession.class);
        Authentication authResult = mock(Authentication.class);
        PaasManagerUser paasUser = mock(PaasManagerUser.class);

        when(servletRequest.getHeader(OpenStackAuthenticationFilter.OPENSTACK_HEADER_TOKEN))
                .thenReturn("3df25213cac246f8bccad5c70cb3582e").thenReturn("00000000000000000000000000000194")
                .thenReturn("1234");
        when(servletRequest.getRequestURI()).thenReturn("/vdc/00000000000000000000000000000194/");
        when(servletRequest.getPathInfo()).thenReturn("/path");
        when(servletRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("1234");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authResult);

        when(authResult.getPrincipal()).thenReturn(paasUser);

        openStackAuthenticationFilter.doFilter(servletRequest, servletResponse, filterChain);
    }

    @Test(expected = AccessDeniedException.class)
    public void doFilterOtherTenantAccess() throws IOException, ServletException {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpSession httpSession = mock(HttpSession.class);
        Authentication authResult = mock(Authentication.class);
        PaasManagerUser paasUser = mock(PaasManagerUser.class);

        when(servletRequest.getHeader(OpenStackAuthenticationFilter.OPENSTACK_HEADER_TOKEN)).thenReturn(
                "3df25213cac246f8bccad5c70cb3582e");
        when(servletRequest.getHeader(OpenStackAuthenticationFilter.HEADER_ACCEPT)).thenReturn("application/json");
        when(servletRequest.getHeader(OpenStackAuthenticationFilter.OPENSTACK_HEADER_TENANTID)).thenReturn(
                "00000000000000000000000000000194");

        when(servletRequest.getRequestURI()).thenReturn("/vdc/00000000000000000000000000000001/");
        when(servletRequest.getPathInfo()).thenReturn("/path");
        when(servletRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("1234");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authResult);

        when(authResult.getPrincipal()).thenReturn(paasUser);

        // when
        openStackAuthenticationFilter.doFilter(servletRequest, servletResponse, filterChain);

        // then
        verify(servletRequest).getHeader(OpenStackAuthenticationFilter.OPENSTACK_HEADER_TOKEN);
        verify(servletRequest).getHeader(OpenStackAuthenticationFilter.HEADER_ACCEPT);
        verify(httpSession).getId();

    }

    @Test
    public void doFilterOtherTennantAccessNoVDCinReq() throws IOException, ServletException {
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        HttpSession httpSession = mock(HttpSession.class);
        Authentication authResult = mock(Authentication.class);
        PaasManagerUser paasUser = mock(PaasManagerUser.class);

        when(servletRequest.getHeader(anyString())).thenReturn("3df25213cac246f8bccad5c70cb3582e")
                .thenReturn("00000000000000000000000000000194").thenReturn("1234");
        when(servletRequest.getRequestURI()).thenReturn("/catalog/application/");
        when(servletRequest.getPathInfo()).thenReturn("/path");
        when(servletRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getId()).thenReturn("1234");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authResult);

        when(authResult.getPrincipal()).thenReturn(paasUser);

        openStackAuthenticationFilter.doFilter(servletRequest, servletResponse, filterChain);
    }

    @Test
    public void shouldReturn406WhenAcceptHeaderIsTextPlain() throws IOException, ServletException {
        // given
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(servletRequest.getHeader(OpenStackAuthenticationFilter.HEADER_ACCEPT)).thenReturn("text/plain");

        // when
        openStackAuthenticationFilter.doFilter(servletRequest, servletResponse, filterChain);
        // then
        verify(servletResponse).sendError(eq(HttpServletResponse.SC_NOT_ACCEPTABLE), anyString());

    }

}
