package com.studychain.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studychain.config.GoogleCalendarProperties;
import com.studychain.models.CalendarEvent;
import com.studychain.models.GoogleCalendarAccount;
import com.studychain.models.User;
import com.studychain.repositories.CalendarEventRepository;
import com.studychain.repositories.GoogleCalendarAccountRepository;
import com.studychain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Service
public class GoogleCalendarService {

	private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
	private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
	private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
	private static final String CALENDAR_LIST_URL = "https://www.googleapis.com/calendar/v3/users/me/calendarList";
	public static final String MISSING_CALENDAR_SCOPE = "missing-calendar-scope";
	private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.events";
	private static final String CALENDAR_LIST_SCOPE = "https://www.googleapis.com/auth/calendar.calendarlist.readonly";
	private static final String SCOPE = "openid email " + CALENDAR_SCOPE + " " + CALENDAR_LIST_SCOPE;
	private static final String DEFAULT_GOOGLE_COLOR = "#4285f4";

	private final GoogleCalendarProperties properties;
	private final GoogleCalendarAccountRepository accountRepository;
	private final CalendarEventRepository eventRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;
	private final RestClient restClient;

	public GoogleCalendarService(GoogleCalendarProperties properties,
	                             GoogleCalendarAccountRepository accountRepository,
	                             CalendarEventRepository eventRepository,
	                             UserRepository userRepository,
	                             ObjectMapper objectMapper,
	                             RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.accountRepository = accountRepository;
		this.eventRepository = eventRepository;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
		this.restClient = restClientBuilder.build();
	}

	public boolean isConfigured() {
		return properties.isConfigured();
	}

	public Optional<GoogleCalendarAccount> findAccount(Long userId) {
		return accountRepository.findByUserId(userId);
	}

	public boolean calendarScopeGranted(GoogleCalendarAccount account) {
		return scopeStatus(account).calendar();
	}

	public boolean calendarListScopeGranted(GoogleCalendarAccount account) {
		return scopeStatus(account).calendarList();
	}

	public ScopeStatus scopeStatus(GoogleCalendarAccount account) {
		try {
			String token = ensureAccessToken(account);
			JsonNode info = restClient.get()
				.uri("https://oauth2.googleapis.com/tokeninfo?access_token={token}", token)
				.retrieve()
				.body(JsonNode.class);
			String scope = info == null ? "" : info.path("scope").asText("");
			return new ScopeStatus(includesCalendarScope(scope), includesCalendarListScope(scope));
		} catch (Exception ex) {
			log.warn("Could not read Google Calendar permission: {}", ex.getMessage());
			return new ScopeStatus(false, false);
		}
	}

	public String authorizationUrl(String state) {
		return UriComponentsBuilder.fromUriString(AUTH_URL)
			.queryParam("client_id", properties.getClientId())
			.queryParam("redirect_uri", properties.getRedirectUri())
			.queryParam("response_type", "code")
			.queryParam("scope", SCOPE)
			.queryParam("access_type", "offline")
			.queryParam("prompt", "consent")
			.queryParam("state", state)
			.encode()
			.build()
			.toUriString();
	}

	@Transactional
	public void connect(Long userId, String code) {
		if (!isConfigured()) {
			throw new GoogleCalendarException("Google Calendar is not configured");
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GoogleCalendarException("User not found"));
		TokenResponse tokens = exchangeAuthorizationCode(code);
		if (!includesCalendarScope(tokens.scope())) {
			throw new GoogleCalendarException(MISSING_CALENDAR_SCOPE);
		}
		GoogleCalendarAccount account = accountRepository.findByUserId(userId).orElseGet(GoogleCalendarAccount::new);
		account.setUser(user);
		account.setAccessToken(tokens.accessToken());
		if (StringUtils.hasText(tokens.refreshToken())) {
			account.setRefreshToken(tokens.refreshToken());
		}
		account.setTokenExpiresAt(tokens.expiresAt());
		account.setGoogleEmail(fetchEmail(tokens.accessToken()));
		account.setConnectedAt(Instant.now());
		accountRepository.save(account);
		pushUnsynced(account);
	}

	@Transactional
	public void disconnect(Long userId) {
		accountRepository.deleteByUserId(userId);
	}

	@Transactional
	public void syncRange(Long userId, LocalDateTime start, LocalDateTime end) {
		GoogleCalendarAccount account = accountRepository.findByUserId(userId).orElse(null);
		if (account == null) {
			return;
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GoogleCalendarException("User not found"));
		Set<String> seen = new HashSet<>();
		List<CalendarSource> calendars = listCalendars(account);
		boolean complete = true;
		for (CalendarSource calendar : calendars) {
			try {
				importCalendar(account, user, calendar, start, end, seen);
			} catch (RuntimeException ex) {
				complete = false;
				log.warn("Could not import Google calendar {}: {}", calendar.id(), ex.getMessage());
			}
		}
		if (!complete) {
			return;
		}
		List<CalendarEvent> linked = eventRepository
			.findAllByUser_IdAndGoogleEventIdIsNotNullAndStartTimeBetween(userId, start, end);
		for (CalendarEvent local : linked) {
			if (!seen.contains(local.getGoogleEventId())) {
				eventRepository.delete(local);
			}
		}
	}

	private void importCalendar(GoogleCalendarAccount account, User user, CalendarSource calendar,
	                            LocalDateTime start, LocalDateTime end, Set<String> seen) {
		String pageToken = null;
		int pages = 0;
		do {
			JsonNode page = listEvents(account, calendar.id(), start, end, pageToken);
			if (page == null) {
				return;
			}
			for (JsonNode item : page.path("items")) {
				ParsedGoogleEvent parsed = parseEvent(item, calendar);
				if (parsed == null) {
					continue;
				}
				seen.add(parsed.googleEventId());
				Long userId = user.getId();
				CalendarEvent event = eventRepository.findByUser_IdAndGoogleEventId(userId, parsed.googleEventId())
					.orElseGet(() -> findByStudyChainId(userId, parsed.studyChainId()).orElse(null));
				if (event == null) {
					event = new CalendarEvent();
					event.setUser(user);
				}
				applyParsed(event, parsed);
				eventRepository.save(event);
			}
			pageToken = page.path("nextPageToken").asText(null);
			pages++;
		} while (StringUtils.hasText(pageToken) && pages < 20);
	}

	public void pushCreated(CalendarEvent event) {
		try {
			GoogleCalendarAccount account = accountFor(event);
			if (account == null) {
				return;
			}
			String googleId = insertEvent(account, event);
			event.setGoogleEventId(googleId);
			eventRepository.save(event);
		} catch (Exception ex) {
			log.warn("Could not create Google Calendar event for local id {}: {}", event.getId(), ex.getMessage());
		}
	}

	public void pushUpdated(CalendarEvent event) {
		try {
			GoogleCalendarAccount account = accountFor(event);
			if (account == null) {
				return;
			}
			if (!StringUtils.hasText(event.getGoogleEventId())) {
				String googleId = insertEvent(account, event);
				event.setGoogleEventId(googleId);
			} else {
				try {
					patchEvent(account, event);
				} catch (RestClientResponseException ex) {
					if (ex.getStatusCode().value() != 404 && ex.getStatusCode().value() != 410) {
						throw ex;
					}
					String googleId = insertEvent(account, event);
					event.setGoogleEventId(googleId);
				}
			}
			eventRepository.save(event);
		} catch (Exception ex) {
			log.warn("Could not update Google Calendar event for local id {}: {}", event.getId(), ex.getMessage());
		}
	}

	public void deleteRemote(Long userId, String googleEventId, String googleCalendarId) {
		if (!StringUtils.hasText(googleEventId)) {
			return;
		}
		GoogleCalendarAccount account = accountRepository.findByUserId(userId).orElse(null);
		if (account == null) {
			return;
		}
		String calendarId = StringUtils.hasText(googleCalendarId) ? googleCalendarId : "primary";
		try {
			withAuth(account, token -> {
				restClient.delete()
					.uri(eventUri(calendarId, googleEventId))
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.retrieve()
					.toBodilessEntity();
				return null;
			});
		} catch (RestClientResponseException ex) {
			int status = ex.getStatusCode().value();
			if (status == 404 || status == 410) {
				return;
			}
			throw new GoogleCalendarException("Could not delete the event from Google Calendar", ex);
		}
	}

	private void pushUnsynced(GoogleCalendarAccount account) {
		Long userId = account.getUser().getId();
		for (CalendarEvent event : eventRepository.findAllByUser_IdAndGoogleEventIdIsNull(userId)) {
			try {
				String googleId = insertEvent(account, event);
				event.setGoogleEventId(googleId);
				eventRepository.save(event);
			} catch (Exception ex) {
				log.warn("Could not push existing event {} to Google Calendar: {}", event.getId(), ex.getMessage());
			}
		}
	}

	private GoogleCalendarAccount accountFor(CalendarEvent event) {
		if (event.getUser() == null || event.getUser().getId() == null) {
			return null;
		}
		return accountRepository.findByUserId(event.getUser().getId()).orElse(null);
	}

	private Optional<CalendarEvent> findByStudyChainId(Long userId, String studyChainId) {
		if (!StringUtils.hasText(studyChainId)) {
			return Optional.empty();
		}
		try {
			Long id = Long.valueOf(studyChainId);
			return eventRepository.findById(id).filter(event -> event.getUser().getId().equals(userId));
		} catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

	private void applyParsed(CalendarEvent event, ParsedGoogleEvent parsed) {
		event.setTitle(parsed.title());
		event.setDescription(parsed.description());
		event.setStartTime(parsed.start());
		event.setEndTime(parsed.end());
		event.setAllDay(parsed.allDay());
		event.setGoogleEventId(parsed.googleEventId());
		event.setGoogleCalendarId(parsed.googleCalendarId());
		event.setColor(parsed.color());
	}

	private TokenResponse exchangeAuthorizationCode(String code) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", code);
		form.add("client_id", properties.getClientId());
		form.add("client_secret", properties.getClientSecret());
		form.add("redirect_uri", properties.getRedirectUri());
		form.add("grant_type", "authorization_code");
		return readTokenResponse(postToken(form));
	}

	private String refreshAccessToken(GoogleCalendarAccount account) {
		if (!StringUtils.hasText(account.getRefreshToken())) {
			throw new GoogleCalendarException("Google Calendar permission expired. Connect again.");
		}
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", properties.getClientId());
		form.add("client_secret", properties.getClientSecret());
		form.add("refresh_token", account.getRefreshToken());
		form.add("grant_type", "refresh_token");
		TokenResponse tokens = readTokenResponse(postToken(form));
		account.setAccessToken(tokens.accessToken());
		account.setTokenExpiresAt(tokens.expiresAt());
		if (StringUtils.hasText(tokens.refreshToken())) {
			account.setRefreshToken(tokens.refreshToken());
		}
		accountRepository.save(account);
		return tokens.accessToken();
	}

	private JsonNode postToken(MultiValueMap<String, String> form) {
		try {
			return restClient.post()
				.uri(TOKEN_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(JsonNode.class);
		} catch (RestClientResponseException ex) {
			throw new GoogleCalendarException(googleError(ex), ex);
		}
	}

	private TokenResponse readTokenResponse(JsonNode node) {
		if (node == null || !node.hasNonNull("access_token")) {
			throw new GoogleCalendarException("Google did not return an access token");
		}
		long expiresIn = node.path("expires_in").asLong(3600);
		String refresh = node.path("refresh_token").asText(null);
		return new TokenResponse(
			node.get("access_token").asText(),
			refresh,
			Instant.now().plusSeconds(Math.max(expiresIn - 60, 30)),
			node.path("scope").asText("")
		);
	}

	private static boolean includesCalendarScope(String scope) {
		return hasScope(scope, CALENDAR_SCOPE) || hasScope(scope, "https://www.googleapis.com/auth/calendar");
	}

	private static boolean includesCalendarListScope(String scope) {
		return hasScope(scope, CALENDAR_LIST_SCOPE)
			|| hasScope(scope, "https://www.googleapis.com/auth/calendar.calendarlist")
			|| hasScope(scope, "https://www.googleapis.com/auth/calendar.readonly")
			|| hasScope(scope, "https://www.googleapis.com/auth/calendar");
	}

	private static boolean hasScope(String scope, String expected) {
		if (scope == null || expected == null) {
			return false;
		}
		for (String part : scope.split(" ")) {
			if (expected.equals(part)) {
				return true;
			}
		}
		return false;
	}

	private String fetchEmail(String accessToken) {
		try {
			JsonNode profile = restClient.get()
				.uri(USERINFO_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.retrieve()
				.body(JsonNode.class);
			if (profile == null) {
				return null;
			}
			String email = profile.path("email").asText(null);
			return StringUtils.hasText(email) ? email : null;
		} catch (Exception ex) {
			log.warn("Could not read Google account email: {}", ex.getMessage());
			return null;
		}
	}

	private String ensureAccessToken(GoogleCalendarAccount account) {
		if (account.getTokenExpiresAt() != null && account.getTokenExpiresAt().isAfter(Instant.now().plusSeconds(30))) {
			return account.getAccessToken();
		}
		return refreshAccessToken(account);
	}

	private <T> T withAuth(GoogleCalendarAccount account, Function<String, T> call) {
		String token = ensureAccessToken(account);
		try {
			return call.apply(token);
		} catch (RestClientResponseException ex) {
			if (ex.getStatusCode().value() != 401) {
				throw ex;
			}
			return call.apply(refreshAccessToken(account));
		}
	}

	private List<CalendarSource> listCalendars(GoogleCalendarAccount account) {
		try {
			JsonNode page = withAuth(account, token -> restClient.get()
				.uri(CALENDAR_LIST_URL + "?maxResults=250")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.retrieve()
				.body(JsonNode.class));
			List<CalendarSource> calendars = new java.util.ArrayList<>();
			if (page != null) {
				for (JsonNode item : page.path("items")) {
					boolean visible = item.path("selected").asBoolean(false) || item.path("primary").asBoolean(false);
					String id = item.path("id").asText(null);
					if (!visible || !StringUtils.hasText(id)) {
						continue;
					}
					calendars.add(new CalendarSource(id, normalizeHex(item.path("backgroundColor").asText(null))));
				}
			}
			if (calendars.isEmpty()) {
				calendars.add(new CalendarSource("primary", DEFAULT_GOOGLE_COLOR));
			}
			return calendars;
		} catch (RuntimeException ex) {
			log.warn("Could not list Google calendars: {}", ex.getMessage());
			return List.of(new CalendarSource("primary", DEFAULT_GOOGLE_COLOR));
		}
	}

	private JsonNode listEvents(GoogleCalendarAccount account, String calendarId, LocalDateTime start, LocalDateTime end, String pageToken) {
		UriComponentsBuilder builder = UriComponentsBuilder
			.fromUriString("https://www.googleapis.com/calendar/v3/calendars")
			.pathSegment(calendarId, "events")
			.queryParam("singleEvents", "true")
			.queryParam("orderBy", "startTime")
			.queryParam("showDeleted", "false")
			.queryParam("maxResults", "250")
			.queryParam("timeMin", rfc3339(start.minusSeconds(1)))
			.queryParam("timeMax", rfc3339(end.plusSeconds(1)));
		if (StringUtils.hasText(pageToken)) {
			builder.queryParam("pageToken", pageToken);
		}
		URI uri = builder.build().toUri();
		try {
			return withAuth(account, token -> restClient.get()
				.uri(uri)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.retrieve()
				.body(JsonNode.class));
		} catch (RestClientResponseException ex) {
			throw new GoogleCalendarException(googleError(ex), ex);
		}
	}

	private String insertEvent(GoogleCalendarAccount account, CalendarEvent event) {
		JsonNode created = withAuth(account, token -> restClient.post()
			.uri(EVENTS_URL)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.body(eventBody(event))
			.retrieve()
			.body(JsonNode.class));
		if (created == null || !created.hasNonNull("id")) {
			throw new GoogleCalendarException("Google did not return an event id");
		}
		return created.get("id").asText();
	}

	private void patchEvent(GoogleCalendarAccount account, CalendarEvent event) {
		String calendarId = StringUtils.hasText(event.getGoogleCalendarId()) ? event.getGoogleCalendarId() : "primary";
		withAuth(account, token -> restClient.patch()
			.uri(eventUri(calendarId, event.getGoogleEventId()))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.body(eventBody(event))
			.retrieve()
			.toBodilessEntity());
	}

	private URI eventUri(String calendarId, String googleEventId) {
		return UriComponentsBuilder
			.fromUriString("https://www.googleapis.com/calendar/v3/calendars")
			.pathSegment(calendarId, "events", googleEventId)
			.build()
			.toUri();
	}

	private ObjectNode eventBody(CalendarEvent event) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("summary", event.getTitle());
		body.put("description", event.getDescription() == null ? "" : event.getDescription());
		ObjectNode start = body.putObject("start");
		ObjectNode end = body.putObject("end");
		if (event.isAllDay()) {
			LocalDate startDate = event.getStartTime().toLocalDate();
			LocalDate endDate = event.getEndTime().toLocalDate();
			if (!endDate.isAfter(startDate)) {
				endDate = startDate.plusDays(1);
			}
			start.put("date", startDate.toString());
			end.put("date", endDate.toString());
		} else {
			ZoneId zone = ZoneId.systemDefault();
			start.put("dateTime", event.getStartTime().atZone(zone).toOffsetDateTime().toString());
			start.put("timeZone", zone.getId());
			end.put("dateTime", event.getEndTime().atZone(zone).toOffsetDateTime().toString());
			end.put("timeZone", zone.getId());
		}
		ObjectNode props = body.putObject("extendedProperties").putObject("private");
		if (event.getColor() != null) {
			props.put("studychainColor", event.getColor());
		}
		if (event.getId() != null) {
			props.put("studychainId", String.valueOf(event.getId()));
		}
		return body;
	}

	private ParsedGoogleEvent parseEvent(JsonNode item, CalendarSource calendar) {
		if (item == null || !item.hasNonNull("id")) {
			return null;
		}
		if ("cancelled".equals(item.path("status").asText())) {
			return null;
		}
		JsonNode startNode = item.path("start");
		JsonNode endNode = item.path("end");
		boolean allDay = startNode.hasNonNull("date") && !startNode.hasNonNull("dateTime");
		LocalDateTime start = parseGoogleDate(startNode);
		LocalDateTime end = parseGoogleDate(endNode);
		if (start == null) {
			return null;
		}
		if (end == null || !end.isAfter(start)) {
			end = allDay ? start.plusDays(1) : start.plusHours(1);
		}
		String title = limit(item.path("summary").asText("(No title)"), 255);
		if (!StringUtils.hasText(title)) {
			title = "(No title)";
		}
		String description = item.path("description").isMissingNode() || item.path("description").isNull()
			? null
			: limit(item.path("description").asText(), 2000);
		String studyChainId = item.path("extendedProperties").path("private").path("studychainId").asText(null);
		String color = resolveColor(item, calendar.backgroundColor());
		return new ParsedGoogleEvent(item.get("id").asText(), calendar.id(), title, description, start, end, allDay, color, studyChainId);
	}

	private static String resolveColor(JsonNode item, String calendarColor) {
		String eventColor = eventColorHex(item.path("colorId").asText(null));
		if (eventColor != null) {
			return eventColor;
		}
		String appColor = normalizeHex(item.path("extendedProperties").path("private").path("studychainColor").asText(null));
		if (appColor != null) {
			return appColor;
		}
		if (calendarColor != null) {
			return calendarColor;
		}
		return DEFAULT_GOOGLE_COLOR;
	}

	private static String eventColorHex(String colorId) {
		if (!StringUtils.hasText(colorId)) {
			return null;
		}
		return switch (colorId) {
			case "1" -> "#a4bdfc";
			case "2" -> "#7ae7bf";
			case "3" -> "#dbadff";
			case "4" -> "#ff887c";
			case "5" -> "#fbd75b";
			case "6" -> "#ffb878";
			case "7" -> "#46d6db";
			case "8" -> "#e1e1e1";
			case "9" -> "#5484ed";
			case "10" -> "#51b749";
			case "11" -> "#dc2127";
			default -> null;
		};
	}

	private static String normalizeHex(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.matches("#[0-9a-fA-F]{6}") ? trimmed : null;
	}

	private LocalDateTime parseGoogleDate(JsonNode node) {
		if (node == null || node.isMissingNode()) {
			return null;
		}
		if (node.hasNonNull("dateTime")) {
			try {
				return OffsetDateTime.parse(node.get("dateTime").asText())
					.atZoneSameInstant(ZoneId.systemDefault())
					.toLocalDateTime();
			} catch (DateTimeParseException ex) {
				return null;
			}
		}
		if (node.hasNonNull("date")) {
			try {
				return LocalDate.parse(node.get("date").asText()).atStartOfDay();
			} catch (DateTimeParseException ex) {
				return null;
			}
		}
		return null;
	}

	private String googleError(RestClientResponseException ex) {
		try {
			JsonNode node = objectMapper.readTree(ex.getResponseBodyAsString());
			String description = node.path("error_description").asText("");
			if (!StringUtils.hasText(description) && node.path("error").isObject()) {
				description = node.path("error").path("message").asText("");
			}
			if (!StringUtils.hasText(description)) {
				description = node.path("error").asText("");
			}
			if (StringUtils.hasText(description)) {
				return description;
			}
		} catch (Exception ignored) {
			// Fall through to a generic message. The raw body can contain request details.
		}
		return "Google Calendar request failed";
	}

	private static String rfc3339(LocalDateTime value) {
		return value.atZone(ZoneId.systemDefault()).toInstant().toString();
	}

	private static String limit(String value, int max) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
	}

	private record TokenResponse(String accessToken, String refreshToken, Instant expiresAt, String scope) {
	}

	public record ScopeStatus(boolean calendar, boolean calendarList) {
	}

	private record CalendarSource(String id, String backgroundColor) {
	}

	private record ParsedGoogleEvent(String googleEventId, String googleCalendarId, String title, String description,
	                                 LocalDateTime start, LocalDateTime end, boolean allDay,
	                                 String color, String studyChainId) {
	}
}
