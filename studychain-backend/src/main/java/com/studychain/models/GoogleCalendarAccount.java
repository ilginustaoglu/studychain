package com.studychain.models;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "google_calendar_accounts")
public class GoogleCalendarAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "google_email", length = 255)
	private String googleEmail;

	@Column(name = "access_token", nullable = false, columnDefinition = "text")
	private String accessToken;

	@Column(name = "refresh_token", columnDefinition = "text")
	private String refreshToken;

	@Column(name = "token_expires_at")
	private Instant tokenExpiresAt;

	@Column(name = "connected_at", nullable = false)
	private Instant connectedAt = Instant.now();

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getGoogleEmail() {
		return googleEmail;
	}

	public void setGoogleEmail(String googleEmail) {
		this.googleEmail = googleEmail;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Instant getTokenExpiresAt() {
		return tokenExpiresAt;
	}

	public void setTokenExpiresAt(Instant tokenExpiresAt) {
		this.tokenExpiresAt = tokenExpiresAt;
	}

	public Instant getConnectedAt() {
		return connectedAt;
	}

	public void setConnectedAt(Instant connectedAt) {
		this.connectedAt = connectedAt;
	}
}
