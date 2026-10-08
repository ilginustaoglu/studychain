package com.studychain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "google.calendar")
public class GoogleCalendarProperties {

	private String clientId = "";
	private String clientSecret = "";
	private String redirectUri = "http://localhost:8080/agenda/google/callback";

	public boolean isConfigured() {
		return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}
}
