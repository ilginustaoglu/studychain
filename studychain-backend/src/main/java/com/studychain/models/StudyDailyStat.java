package com.studychain.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "study_daily_stats", uniqueConstraints = {
	@UniqueConstraint(name = "uk_study_daily_stats_user_date", columnNames = {"user_id", "stat_date"})
}, indexes = {
	@Index(name = "idx_study_daily_stats_user_date", columnList = "user_id, stat_date")
})
public class StudyDailyStat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "stat_date", nullable = false)
	private LocalDate statDate;

	@Column(name = "minutes", nullable = false)
	private long minutes = 0;

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public LocalDate getStatDate() {
		return statDate;
	}

	public void setStatDate(LocalDate statDate) {
		this.statDate = statDate;
	}

	public long getMinutes() {
		return minutes;
	}

	public void setMinutes(long minutes) {
		this.minutes = minutes;
	}
}


