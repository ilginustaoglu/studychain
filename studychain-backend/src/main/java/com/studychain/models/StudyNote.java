package com.studychain.models;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "study_notes", indexes = {
	@Index(name = "idx_study_notes_user_updated", columnList = "ownerUserId,updatedAt")
})
public class StudyNote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long ownerUserId;

	@Column(nullable = false, length = 255)
	private String title = "Note";

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(Long ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}


